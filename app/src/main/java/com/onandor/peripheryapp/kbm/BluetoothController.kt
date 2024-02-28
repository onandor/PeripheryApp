package com.onandor.peripheryapp.kbm

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@SuppressLint("MissingPermission")
class BluetoothController @Inject constructor(
    private val context: Context,
    private val hidDataSender: HidDataSender
): IBluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private lateinit var hidDeviceProfile: HidDeviceProfile

    private val _foundDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val foundDevices: StateFlow<List<BluetoothDevice>> = _foundDevices.asStateFlow()

    private val _bondedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    override val bondedDevices: StateFlow<List<BluetoothDevice>> = _bondedDevices.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled ?: false)
    override val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val bluetoothScanReceiver = object : BroadcastReceiver() {

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
                    if (hidDeviceProfile.isHidHostProfileSupported(device) &&
                        device.bondState == BluetoothDevice.BOND_NONE) {
                        _foundDevices.update { devices ->
                            if (device in devices ) devices
                            else devices + device
                        }
                    }
                }
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    _foundDevices.update { devices ->
                        if (device in devices) {
                            devices.filterNot { it.address == device.address } + device
                        } else {
                            devices + device
                        }
                    }
                }
            }
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> {
                            _isBluetoothEnabled.update { false }
                            _foundDevices.update { emptyList() }
                            _bondedDevices.update { emptyList() }
                        }
                        BluetoothAdapter.STATE_ON -> {
                            _isBluetoothEnabled.update { true }
                            updateBondedDevices()
                        }
                    }
                }
            }
        }
    }

    private val profileListener = object : HidDataSender.ProfileListener {

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            TODO("Not yet implemented")
        }

        override fun onAppStatusChanged(registered: Boolean) {
            TODO("Not yet implemented")
        }

        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            TODO("Not yet implemented")
        }
    }

    init {
        hidDeviceProfile = hidDataSender.register(context, profileListener)
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        updateBondedDevices()
    }

    override fun startDiscovery() {
        if (!isPermissionGranted(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        val scanIntentFilter = IntentFilter()
        scanIntentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        scanIntentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        context.registerReceiver(bluetoothScanReceiver, scanIntentFilter)

        updateBondedDevices()
        _foundDevices.update { emptyList() }
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!isPermissionGranted(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun updateBondedDevices() {
        if (!isPermissionGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.also { devices -> _bondedDevices.update { devices.toList() } }
    }

    override fun release() {
        context.unregisterReceiver(bluetoothScanReceiver)
        context.unregisterReceiver(bluetoothStateReceiver)
        hidDataSender.unregister(context, profileListener)
    }

    private fun isPermissionGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}