package com.onandor.peripheryapp.kbm.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.onandor.peripheryapp.kbm.bluetooth.reports.BatteryReport
import com.onandor.peripheryapp.kbm.bluetooth.reports.KeyboardReport
import com.onandor.peripheryapp.kbm.bluetooth.reports.MouseReport
import com.onandor.peripheryapp.utils.PermissionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class BluetoothController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val hidDeviceProfile: HidDeviceProfile,
    private val permissionChecker: PermissionChecker
) {

    interface HidProfileListener : HidDeviceProfile.ServiceStateListener {

        fun onConnectionStateChanged(device: BluetoothDevice?, state: Int)
        fun onAppStatusChanged(registered: Boolean)
    }

    private val batteryReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                onBatteryChanged(it)
            }
        }
    }

    private val lock: Any = Any()
    private var isAppRegistered: Boolean = false
    private val listeners: MutableSet<HidProfileListener> = mutableSetOf()
    private var connectedDevice: BluetoothDevice? = null
    private var waitingForDevice: BluetoothDevice? = null

    private var hidServiceProxy: BluetoothHidDevice? = null
    private val mouseReport = MouseReport()
    private val keyboardReport = KeyboardReport()
    private val batteryReport = BatteryReport()
    private var lastReportEmpty = false

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
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            super.onSetReport(device, type, id, data)
        }
    }

    private val hidServiceStateListener = object : HidDeviceProfile.ServiceStateListener {

        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            synchronized (lock) {
                if (proxy == null) {
                    if (isAppRegistered) {
                        onAppStatusChanged(false)
                    }
                } else {
                    registerApp(proxy)
                }
                updateDeviceList()
                listeners.forEach { listener ->
                    listener.onServiceStateChanged(proxy)
                }
            }
        }
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

    fun registerListener(listener: HidProfileListener): HidDeviceProfile {
        synchronized (lock) {
            if (!listeners.add(listener)) {
                return hidDeviceProfile
            }
            if (listeners.size > 1) {
                return hidDeviceProfile
            }

            hidDeviceProfile.registerServiceListener(hidServiceStateListener)
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
        return hidDeviceProfile
    }

    fun unregisterListener(listener: HidProfileListener) {
        synchronized (lock) {
            if (!listeners.remove(listener)) {
                return
            }
            if (listeners.isNotEmpty()) {
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

                listeners.forEach { listener ->
                    listener.onAppStatusChanged(registered)
                }
                if (registered && waitingForDevice != null) {
                    requestConnect(waitingForDevice)
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
                listeners.forEach { listener ->
                    listener.onConnectionStateChanged(device, state)
                }
            }
        }
    }

    fun isConnected() = connectedDevice != null

    fun requestConnect(device: BluetoothDevice?) {
        synchronized (lock) {
            waitingForDevice = device
            if (!isAppRegistered) {
                return
            }

            connectedDevice = null
            updateDeviceList()

            if (device != null && device == connectedDevice) {
                listeners.forEach { listener ->
                    listener.onConnectionStateChanged(device, BluetoothProfile.STATE_CONNECTED)
                }
            }
        }
    }

    fun sendMouse(
        left: Boolean,
        right: Boolean,
        middle: Boolean,
        dX: Int,
        dY: Int,
        dWheel: Int
    ) {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        if (hidServiceProxy == null || connectedDevice == null) {
            return
        }
        synchronized(lock) {
            val report = mouseReport.setValue(left, right, middle, dX, dY, dWheel)
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
        TODO("Not yet implemented")
    }

    fun sendBatteryLevel(batteryLevel: Float) {
        // TODO
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
            } else if (connectedDevice != null && _connectedDevice == null) {
                connectedDevice = null
            }
        }
    }

    private fun onBatteryChanged(intent: Intent) {
        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale >= 0) {
            val batteryLevel = level.toFloat() / scale.toFloat()
            sendBatteryLevel(batteryLevel)
        }
    }
}