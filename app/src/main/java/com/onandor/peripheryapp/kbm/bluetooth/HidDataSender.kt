package com.onandor.peripheryapp.kbm.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HidDataSender @Inject constructor(
    private val hidDeviceApp: HidDeviceApp,
    private val hidDeviceProfile: HidDeviceProfile
) : MouseReport.MouseDataSender, KeyboardReport.KeyboardDataSender {

    interface ProfileListener: HidDeviceApp.DeviceStateListener,
        HidDeviceProfile.ServiceStateListener {}

    private val batteryReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                onBatteryChanged(it)
            }
        }
    }

    private val lock: Any = Any()

    private val listeners: MutableSet<ProfileListener> = mutableSetOf()

    private var connectedDevice: BluetoothDevice? = null
    private var waitingForDevice: BluetoothDevice? = null

    private var isAppRegistered: Boolean = false

    private val profileListener = object : ProfileListener {

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            synchronized (lock) {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    waitingForDevice = device
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    if (device == waitingForDevice) {
                        waitingForDevice = null
                    }
                }
                updateDeviceList()
                listeners.forEach { listener ->
                    listener.onConnectionStateChanged(device ,state)
                }
            }
        }

        override fun onAppStatusChanged(registered: Boolean) {
            synchronized (lock) {
                if (isAppRegistered == registered) {
                    return
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

        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            synchronized (lock) {
                if (proxy == null) {
                    if (isAppRegistered) {
                        onAppStatusChanged(false)
                    }
                } else {
                    hidDeviceApp.registerApp(proxy)
                }
                updateDeviceList()
                listeners.forEach { listener ->
                    listener.onServiceStateChanged(proxy)
                }
            }
        }
    }

    fun register(context: Context, listener: ProfileListener): HidDeviceProfile {
        synchronized (lock) {
            if (!listeners.add(listener)) {
                return hidDeviceProfile
            }
            if (listeners.size > 1) {
                return hidDeviceProfile
            }

            val appContext = context.applicationContext
            hidDeviceProfile.registerServiceListener(appContext, profileListener)
            hidDeviceApp.registerDeviceListener(profileListener)
            appContext.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }
        return hidDeviceProfile
    }

    fun unregister(context: Context, listener: ProfileListener) {
        synchronized (lock) {
            if (!listeners.remove(listener)) {
                return
            }
            if (listeners.isNotEmpty()) {
                return
            }

            val appContext = context.applicationContext
            appContext.unregisterReceiver(batteryReceiver)
            hidDeviceApp.unregisterDeviceListener()

            hidDeviceProfile.getConnectedDevices().forEach { device ->
                hidDeviceProfile.disconnectFromHost(device)
            }

            hidDeviceApp.setDevice(null)
            hidDeviceApp.unregisterApp()

            hidDeviceProfile.unregisterServiceListener()

            connectedDevice = null
            waitingForDevice = null
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

    override fun sendKeyboard(
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

    override fun sendMouse(
        left: Boolean,
        right: Boolean,
        middle: Boolean,
        dX: Int,
        dY: Int,
        dWheel: Int
    ) {
        TODO("Not yet implemented")
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
            hidDeviceApp.setDevice(connectedDevice)
        }
    }

    private fun onBatteryChanged(intent: Intent) {
        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale >= 0) {
            val batteryLevel = level.toFloat() / scale.toFloat()
            hidDeviceApp.sendBatteryLevel(batteryLevel)
        }
    }
}