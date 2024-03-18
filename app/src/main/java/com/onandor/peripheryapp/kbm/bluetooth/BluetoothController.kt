package com.onandor.peripheryapp.kbm.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import com.onandor.peripheryapp.kbm.bluetooth.events.BluetoothScanEvent
import com.onandor.peripheryapp.kbm.bluetooth.events.BluetoothStateEvent
import com.onandor.peripheryapp.kbm.bluetooth.reports.BatteryReport
import com.onandor.peripheryapp.kbm.bluetooth.reports.KeyboardReport
import com.onandor.peripheryapp.kbm.bluetooth.reports.MouseReport
import com.onandor.peripheryapp.kbm.bluetooth.reports.MultimediaReport
import com.onandor.peripheryapp.kbm.bluetooth.services.BtNotificationService
import com.onandor.peripheryapp.utils.BtSettingKeys
import com.onandor.peripheryapp.utils.PermissionChecker
import com.onandor.peripheryapp.utils.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class BluetoothController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hidDeviceProfile: HidDeviceProfile,
    private val permissionChecker: PermissionChecker,
    private val settings: Settings
) {

    interface HidProfileListener : HidDeviceProfile.ServiceStateListener {

        fun onConnectionStateChanged(device: BluetoothDevice?, state: Int)
        fun onAppStatusChanged(registered: Boolean)
    }

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val lock: Any = Any()
    private var isAppRegistered: Boolean = false
    private val hidProfileListeners: MutableSet<HidProfileListener> = mutableSetOf()
    private var connectedDevice: BluetoothDevice? = null
    private var waitingForDevice: BluetoothDevice? = null

    private var hidServiceProxy: BluetoothHidDevice? = null
    private val mouseReport = MouseReport()
    private val keyboardReport = KeyboardReport()
    private val batteryReport = BatteryReport()
    private val multimediaReport = MultimediaReport()
    private var lastReportEmpty = false

    var deviceName: String = ""
        private set

    private var keyboardReportModeJob: Job? = null

    private val hidServiceProxyCallback = object : BluetoothHidDevice.Callback() {

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            this@BluetoothController.onAppStatusChanged(registered)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            this@BluetoothController.onConnectionStateChanged(device, state)
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            super.onGetReport(device, type, id, bufferSize)
            if (hidServiceProxy == null) {
                return
            }
            if (type != BluetoothHidDevice.REPORT_TYPE_INPUT) {
                hidServiceProxy!!.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
            } else if (!replyReport(device, type, id)) {
                hidServiceProxy!!.reportError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
            }
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            super.onSetReport(device, type, id, data)
            hidServiceProxy?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }
    }

    private val hidServiceStateListener = object : HidDeviceProfile.ServiceStateListener {

        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            synchronized(lock) {
                if (proxy == null) {
                    if (isAppRegistered) {
                        onAppStatusChanged(false)
                    }
                } else {
                    registerApp(proxy)
                }
                updateDeviceList()
                hidProfileListeners.forEach { listener ->
                    listener.onServiceStateChanged(proxy)
                }
            }
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) {
                    val batteryLevel = level.toFloat() / scale.toFloat()
                    sendBatteryLevel(batteryLevel)
                }
            }
        }
    }

    init {
        keyboardReportModeJob = settings
            .observe(BtSettingKeys.KEYBOARD_REPORT_MODE)
            .onEach { mode -> keyboardReport.changeMode(KeyboardReport.ReportMode.fromInt(mode)) }
            .launchIn(CoroutineScope(Dispatchers.Main))
    }

    private fun registerApp(proxy: BluetoothProfile?) {
        if (proxy == null ||
            !permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        hidServiceProxy = proxy as BluetoothHidDevice
        this.hidServiceProxy!!.registerApp(
            Constants.SDP_RECORD,
            null,
            Constants.QOS_OUT,
            Dispatchers.Main.asExecutor(),
            hidServiceProxyCallback
        )
    }

    private fun unregisterApp() {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        if (isAppRegistered) {
            hidServiceProxy?.unregisterApp()
        }
        hidServiceProxy = null
    }

    fun getBluetoothScanEventFlow(): Flow<BluetoothScanEvent> = callbackFlow {
        val bluetoothScanReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                if (device == null) {
                    return
                }
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        trySendBlocking(BluetoothScanEvent.DeviceFound(device))
                    }
                    BluetoothDevice.ACTION_NAME_CHANGED -> {
                        trySendBlocking(BluetoothScanEvent.DeviceNameChanged(device))
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        trySendBlocking(BluetoothScanEvent.DeviceBondStateChanged(device))
                    }
                }
            }
        }

        val scanIntentFilter = IntentFilter()
        scanIntentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        scanIntentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        scanIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bluetoothScanReceiver, scanIntentFilter)

        awaitClose {
            context.unregisterReceiver(bluetoothScanReceiver)
        }
    }

    fun getBluetoothStateEventFlow(): Flow<BluetoothStateEvent> = callbackFlow {
        val bluetoothStateReceiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )
                        if (state != BluetoothAdapter.ERROR) {
                            trySendBlocking(BluetoothStateEvent.StateChanged(state))
                        }
                    }
                    BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                        val scanMode = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_SCAN_MODE,
                            BluetoothAdapter.ERROR
                        )
                        if (scanMode != BluetoothAdapter.ERROR) {
                            trySendBlocking(BluetoothStateEvent.ScanModeChanged(scanMode))
                        }
                    }
                }
            }
        }

        val stateIntentFilter = IntentFilter()
        stateIntentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        stateIntentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, stateIntentFilter)

        awaitClose {
            context.unregisterReceiver(bluetoothStateReceiver)
        }
    }

    fun registerProfileListener(listener: HidProfileListener): HidDeviceProfile {
        synchronized (lock) {
            if (!hidProfileListeners.add(listener)) {
                return hidDeviceProfile
            }
            if (hidProfileListeners.size > 1) {
                return hidDeviceProfile
            }

            hidDeviceProfile.registerServiceListener(hidServiceStateListener)
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
        return hidDeviceProfile
    }

    fun unregisterProfileListener(listener: HidProfileListener) {
        synchronized (lock) {
            if (!hidProfileListeners.remove(listener)) {
                return
            }
            if (hidProfileListeners.isNotEmpty()) {
                return
            }

            context.unregisterReceiver(batteryReceiver)

            hidDeviceProfile.getConnectedDevices().forEach { device ->
                hidDeviceProfile.disconnectFromHost(device)
            }

            unregisterApp()

            hidDeviceProfile.unregisterServiceListener()

            connectedDevice = null
            waitingForDevice = null
        }
    }

    private fun onAppStatusChanged(registered: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            synchronized (lock) {
                if (isAppRegistered == registered) {
                    return@launch
                }
                isAppRegistered = registered

                hidProfileListeners.forEach { listener ->
                    listener.onAppStatusChanged(registered)
                }
                if (registered && waitingForDevice != null) {
                    requestConnect(waitingForDevice)
                }

                if (registered) {
                    updateNotificationService(null, BluetoothProfile.STATE_DISCONNECTED)
                } else {
                    stopNotificationService()
                }
            }
        }
    }

    private fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            synchronized (lock) {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    waitingForDevice = device
                } else if (state == BluetoothProfile.STATE_DISCONNECTED &&
                    device == waitingForDevice) {
                    waitingForDevice = null
                }
                updateDeviceList()
                hidProfileListeners.forEach { listener ->
                    listener.onConnectionStateChanged(device, state)
                }
                if (isAppRegistered) {
                    updateNotificationService(device?.name, state)
                }
            }
        }
    }

    fun requestConnect(device: BluetoothDevice?) {
        synchronized (lock) {
            waitingForDevice = device
            if (!isAppRegistered) {
                return
            }

            connectedDevice = null
            updateDeviceList()

            if (device != null && device == connectedDevice) {
                onConnectionStateChanged(device, BluetoothProfile.STATE_CONNECTED)
            }
        }
    }

    fun startDiscovery() {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.startDiscovery()
    }

    fun stopDiscovery() {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    fun sendMouse(
        left: Boolean,
        right: Boolean,
        middle: Boolean,
        dX: Int,
        dY: Int,
        dWheelHorizontal: Int,
        dWheelVertical: Int
    ) {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        if (hidServiceProxy == null || connectedDevice == null) {
            return
        }
        synchronized(lock) {
            val report = mouseReport.setValue(
                left = left,
                right = right,
                middle = middle,
                x = dX,
                y = dY,
                wheelHorizontal = dWheelHorizontal,
                wheelVertical = dWheelVertical
            )
            if (report.all { it == 0.toByte() }) {
                if (lastReportEmpty) {
                    return
                }
                lastReportEmpty = true
            } else {
                lastReportEmpty = false
            }
            hidServiceProxy!!.sendReport(connectedDevice, Constants.ID_MOUSE, report)
        }
    }

    fun sendKeyboard(
        modifier: Int,
        key1: Int,
        key2: Int,
        key3: Int,
        key4: Int,
        key5: Int,
        key6: Int
    ) {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        if (hidServiceProxy == null || connectedDevice == null) {
            return
        }
        synchronized(lock) {
            val report = keyboardReport.setValue(
                modifier = modifier,
                key1 = key1,
                key2 = key2,
                key3 = key3,
                key4 = key4,
                key5 = key5,
                key6 = key6
            )
            hidServiceProxy!!.sendReport(connectedDevice, Constants.ID_KEYBOARD, report)
        }
    }

    fun sendBatteryLevel(batteryLevel: Float) {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        if (hidServiceProxy == null || connectedDevice == null) {
            return
        }
        val report = batteryReport.setValue(batteryLevel)
        hidServiceProxy!!.sendReport(connectedDevice, Constants.ID_BATTERY, report)
    }

    fun sendMultimedia(key: Int) {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        if (hidServiceProxy == null || connectedDevice == null) {
            return
        }
        synchronized(lock) {
            val report = multimediaReport.setValue(key)
            hidServiceProxy!!.sendReport(connectedDevice, Constants.ID_MULTIMEDIA, report)
        }
    }

    private fun updateNotificationService(deviceName: String?, connectionState: Int) {
        val intent = BtNotificationService
            .buildIntent(deviceName, connectionState)
            .setClass(context, BtNotificationService::class.java)
        context.startForegroundService(intent)
    }

    private fun stopNotificationService() {
        val intent = Intent().setClass(context, BtNotificationService::class.java)
        context.stopService(intent)
    }

    private fun updateDeviceList() {
        synchronized (lock) {
            var _connectedDevice: BluetoothDevice? = null

            hidDeviceProfile.getConnectedDevices().forEach { device ->
                if (device == waitingForDevice || device == connectedDevice) {
                    _connectedDevice = device
                } else {
                    hidDeviceProfile.disconnectFromHost(device)
                }
            }

            val connectionStates = intArrayOf(
                BluetoothProfile.STATE_CONNECTED,
                BluetoothProfile.STATE_CONNECTING,
                BluetoothProfile.STATE_DISCONNECTING
            )
            if (hidDeviceProfile
                .getDevicesMatchingConnectionStates(connectionStates)
                .isEmpty() && waitingForDevice != null) {
                hidDeviceProfile.connectToHost(waitingForDevice!!)
            }

            if (connectedDevice == null && _connectedDevice != null) {
                connectedDevice = _connectedDevice
                waitingForDevice = null
                deviceName = connectedDevice!!.name
            } else if (connectedDevice != null && _connectedDevice == null) {
                connectedDevice = null
                deviceName = ""
            }
        }
    }

    private fun getReport(id: Byte): ByteArray? {
        when (id) {
            Constants.ID_KEYBOARD.toByte() -> {
                return keyboardReport.getValue()
            }
            Constants.ID_MOUSE.toByte() -> {
                return mouseReport.getValue()
            }
            Constants.ID_MULTIMEDIA.toByte() -> {
                return multimediaReport.getValue()
            }
            Constants.ID_BATTERY.toByte() -> {
                return batteryReport.getValue()
            }
        }
        return null
    }

    private fun replyReport(device: BluetoothDevice?, type: Byte, id: Byte): Boolean {
        val report: ByteArray = getReport(id) ?: return false
        hidServiceProxy?.replyReport(device, type, id, report)
        return true
    }
}