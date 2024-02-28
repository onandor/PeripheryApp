package com.onandor.peripheryapp.kbm

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.launch
import javax.inject.Singleton

@Singleton
class HidDeviceApp : MouseReport.MouseDataSender, KeyboardReport.KeyboardDataSender, BatteryReport.BatteryDataSender {

    interface DeviceStateListener {

        fun onConnectionStateChanged(device: BluetoothDevice?, state: Int)
        fun onAppStatusChanged(registered: Boolean)
    }

    private var inputHost: BluetoothHidDevice? = null
    private val mouseReport = MouseReport()
    private val keyboardReport = KeyboardReport()
    private val batteryReport = BatteryReport()

    private var device: BluetoothDevice? = null
    private var deviceStateListener: DeviceStateListener? = null

    private var registered: Boolean = false

    private val callback = object : BluetoothHidDevice.Callback() {

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            this@HidDeviceApp.registered = registered
            this@HidDeviceApp.onAppStatusChanged(registered)
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            this@HidDeviceApp.onConnectionStateChanged(device, state)
        }

        override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
            super.onGetReport(device, type, id, bufferSize)
            // TODO
        }

        override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
            super.onSetReport(device, type, id, data)
            // TODO
        }
    }

    private fun onAppStatusChanged(registered: Boolean) {
        CoroutineScope(Dispatchers.Main).launch {
            deviceStateListener?.onAppStatusChanged(registered)
        }
    }

    private fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            deviceStateListener?.onConnectionStateChanged(device, state)
        }
    }

    fun registerApp(inputHost: BluetoothProfile?) {
        if (inputHost == null) {
            return
        }
        this.inputHost = inputHost as BluetoothHidDevice
        this.inputHost!!.registerApp(
            Constants.SDP_RECORD,
            null,
            Constants.QOS_OUT,
            Dispatchers.Main.asExecutor(),
            callback
        )
    }

    fun unregisterApp() {
        if (registered) {
            inputHost?.unregisterApp()
        }
        inputHost = null
    }

    fun registerDeviceListener(listener: DeviceStateListener) {
        deviceStateListener = listener
    }

    fun unregisterDeviceListener() {
        deviceStateListener = null
    }

    fun setDevice(device: BluetoothDevice?) {
        this.device = device
    }

    override fun sendBatteryLevel(batteryLevel: Float) {
        // TODO
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

    private fun replyReport(device: BluetoothDevice, type: Byte, id: Byte): Boolean {
        // TODO
        return true
    }

    private fun getReport(id: Byte): ByteArray? {
        // TODO
        return null
    }
}