package com.onandor.peripheryapp.kbm

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HidDeviceProfile(private val context: Context) {

    private val HOGP_UUID = ParcelUuid.fromString("00001812-0000-1000-8000-00805f9b34fb")
    private val HID_UUID = ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb")

    interface ServiceStateListener {

        fun onServiceStateChanged(proxy: BluetoothProfile?)
    }

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private var hidServiceProxy: BluetoothHidDevice? = null
    private var serviceStateListener: ServiceStateListener? = null

    private val hidServiceListener = object : ServiceListener {

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HID_DEVICE) {
                return
            }
            hidServiceProxy = proxy as BluetoothHidDevice
            if (serviceStateListener != null) {
                serviceStateListener?.onServiceStateChanged(hidServiceProxy)
            } else {
                bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, proxy)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            hidServiceProxy = null
            if (serviceStateListener != null) {
                serviceStateListener?.onServiceStateChanged(null)
            }
        }
    }

    fun isHidHostProfileSupported(device: BluetoothDevice): Boolean {
        val uuids = device.uuids ?: return true
        uuids.forEach { uuid ->
            if (HID_UUID == uuid || HOGP_UUID == uuid) {
                return false
            }
        }
        return true
    }

    fun registerServiceListener(context: Context, listener: ServiceStateListener) {
        val applicationContext = context.applicationContext
        serviceStateListener = listener
        bluetoothAdapter?.getProfileProxy(
            applicationContext,
            hidServiceListener,
            BluetoothProfile.HID_DEVICE
        )
    }

    fun unregisterServiceListener() {
        if (hidServiceProxy != null) {
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidServiceProxy)
            hidServiceProxy = null
        }
        serviceStateListener = null
    }

    fun getConnectionState(device: BluetoothDevice): Int {
        if (hidServiceProxy == null) {
            return BluetoothProfile.STATE_DISCONNECTED
        }
        return hidServiceProxy!!.getConnectionState(device)
    }

    fun connectToHost(device: BluetoothDevice) {
        if (hidServiceProxy != null && isHidHostProfileSupported(device)) {
            hidServiceProxy!!.connect(device)
        }
    }

    fun disconnectFromHost(device: BluetoothDevice) {
        if (hidServiceProxy != null && isHidHostProfileSupported(device)) {
            hidServiceProxy!!.disconnect(device)
        }
    }

    fun getConnectedDevices(): List<BluetoothDevice> {
        if (hidServiceProxy == null) {
            return emptyList()
        }
        return hidServiceProxy!!.connectedDevices
    }

    fun getDevicesMatchingConnectionStates(states: IntArray): List<BluetoothDevice> {
        if (hidServiceProxy == null) {
            return emptyList()
        }
        return hidServiceProxy.getDevicesMatchingConnectionStates(states)
    }
}