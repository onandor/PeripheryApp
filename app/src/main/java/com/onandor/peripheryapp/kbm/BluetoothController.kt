package com.onandor.peripheryapp.kbm

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import com.onandor.peripheryapp.kbm.data.BtDevice
import com.onandor.peripheryapp.kbm.data.toBtDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

@SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
class BluetoothController(private val context: Context) : IBluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }
    private var bluetoothHidDevice: BluetoothHidDevice? = null

    private val _scannedDevices = MutableStateFlow<List<BtDevice>>(emptyList())
    override val scannedDevices: StateFlow<List<BtDevice>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BtDevice>>(emptyList())
    override val pairedDevices: StateFlow<List<BtDevice>> = _pairedDevices.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled ?: false)
    override val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String> = _errors.asSharedFlow()

    private var clientSocket: BluetoothSocket? = null

    private val bluetoothStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
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
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    if (device != null) {
                        val newDevice = device.toBtDevice()
                        _scannedDevices.update { devices ->
                            if (newDevice in devices ) devices
                            else devices + newDevice
                        }
                    }
                }
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    if (device != null) {
                        val changedDevice = device.toBtDevice()
                        _scannedDevices.update { devices ->
                            if (changedDevice in devices) {
                                devices.filterNot { it.address == changedDevice.address } + changedDevice
                            } else {
                                devices + changedDevice
                            }
                        }
                    }
                }
            }
        }
    }

    private val btConnectionStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }
            if (device == null) {
                return
            }

            var connected: Boolean? = null
            when (intent?.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    connected = true
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    connected = false
                }
            }

            if (connected == null) {
                return
            }
            if (bluetoothAdapter?.bondedDevices?.contains(device) == true) {
                _isConnected.update { connected }
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    _errors.tryEmit("Cannot connect to a non-paired device")
                }
            }
        }
    }

    private val hidDeviceListener = object : BluetoothProfile.ServiceListener {

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HID_DEVICE) {
                return
            }
            bluetoothHidDevice = proxy as BluetoothHidDevice
            //bluetoothHidDevice.registerApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = null
            }
        }
    }

    companion object {
        const val BT_NAME = "PeripheryApp"
        const val BT_UUID = "00001124-0000-1000-8000-00805f9b34fb"
    }

    init {
        context.registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        context.registerReceiver(
            btConnectionStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
        updatePairedDevices()
    }

    override fun startDiscovery() {
        if (!permissionGranted(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        context.registerReceiver(
            foundBtDeviceReceiver,
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            }
        )

        updatePairedDevices()
        _scannedDevices.update { emptyList() }
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!permissionGranted(Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun connectToDevice(btDevice: BtDevice): Flow<BtConnectionResult> {
        return flow {
            if (!permissionGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
                throw SecurityException(
                    "BluetoothController.connectToDevice: BLUETOOTH_CONNECT" +
                            "permission denied"
                )
            }

            clientSocket = bluetoothAdapter
                ?.getRemoteDevice(btDevice.address)
                ?.createRfcommSocketToServiceRecord(UUID.fromString(BT_UUID))
            stopDiscovery()

            clientSocket?.let { _clientSocket ->
                try {
                    _clientSocket.connect()
                    emit(BtConnectionResult.ConnectionEstablished)
                } catch (e: IOException) {
                    e.printStackTrace()
                    _clientSocket.close()
                    clientSocket = null
                    emit(BtConnectionResult.Error("Bluetooth connection to remote " +
                            "device interrupted"))
                }
            }
        }.onCompletion {
            closeConnection()
        }.flowOn(Dispatchers.IO)
    }

    override fun closeConnection() {
        clientSocket?.close()
        clientSocket = null
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
        context.unregisterReceiver(btConnectionStateReceiver)
        closeConnection()
    }

    private fun permissionGranted(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}