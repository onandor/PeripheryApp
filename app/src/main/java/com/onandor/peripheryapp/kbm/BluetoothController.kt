package com.onandor.peripheryapp.kbm

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.onandor.peripheryapp.kbm.data.BtDevice
import com.onandor.peripheryapp.kbm.data.toBtDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@SuppressLint("MissingPermission")
class BluetoothController(private val context: Context) : IBluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _scannedDevices = MutableStateFlow<List<BtDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BtDevice>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BtDevice>>(emptyList())
    override val pairedDevices: StateFlow<List<BtDevice>> = _pairedDevices.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled ?: false)
    override val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val bluetoothStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            _isBluetoothEnabled.update { false }
                        }
                        BluetoothAdapter.STATE_ON -> {
                            _isBluetoothEnabled.update { true }
                        }
                    }
                }
            }
        }
    }

    private val foundBtDeviceReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when(intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        onDeviceFound(device)
                    }
                }
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    if (device != null) {
                        onDeviceNameChanged(device)
                    }
                }
            }
        }
    }

    init {
        context.registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        updatePairedDevices()
    }

    override fun startDiscovery() {
        if (!permissionGranted(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        context.registerReceiver(foundBtDeviceReceiver, filter)
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!permissionGranted(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.cancelDiscovery()
        _scannedDevices.update { emptyList() }
    }

    override fun updatePairedDevices() {
        if (!permissionGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBtDevice() }
            ?.also { devices -> _pairedDevices.update { devices } }
    }

    override fun release() {
        context.unregisterReceiver(bluetoothStateReceiver)
        context.unregisterReceiver(foundBtDeviceReceiver)
    }

    private fun onDeviceFound(foundDevice: BluetoothDevice) {
        val newDevice = foundDevice.toBtDevice()
        _scannedDevices.update { devices ->
            if (newDevice in devices ) devices
            else devices + newDevice
        }
    }

    private fun onDeviceNameChanged(device: BluetoothDevice) {
        val changedDevice = device.toBtDevice()
        _scannedDevices.update { devices ->
            if (changedDevice in devices) {
                devices.filterNot { it.address == changedDevice.address } + changedDevice
            } else {
                devices + changedDevice
            }
        }
    }

    private fun permissionGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}