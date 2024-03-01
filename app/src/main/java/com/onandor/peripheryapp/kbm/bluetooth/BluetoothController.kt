package com.onandor.peripheryapp.kbm.bluetooth

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
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
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

    private val _bluetoothState = MutableStateFlow(
        bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF
    )
    override val bluetoothState: StateFlow<Int> = _bluetoothState.asStateFlow()

    private val _waitingForDeviceBonding = MutableStateFlow<BluetoothDevice?>(null)
    override val waitingForDeviceBonding: StateFlow<BluetoothDevice?> = _waitingForDeviceBonding.asStateFlow()

    private val _waitingForDeviceConnecting = MutableStateFlow<BluetoothDevice?>(null)
    override val waitingForDeviceConnecting: StateFlow<BluetoothDevice?> = _waitingForDeviceConnecting.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    override val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()


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
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    onDeviceBondStateChanged(device)
                }
            }
        }
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _bluetoothState.update { state }
                    when(state) {
                        BluetoothAdapter.STATE_ON -> {
                            updateBondedDevices()
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            _foundDevices.update { emptyList() }
                            _bondedDevices.update { emptyList() }
                        }
                    }
                }
            }
        }
    }

    private val profileListener = object : HidDataSender.ProfileListener {

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _waitingForDeviceConnecting.update { null }
                    _connectedDevice.update { device }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _waitingForDeviceConnecting.update { device }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectedDevice.update { null }
                }
            }
        }

        override fun onAppStatusChanged(registered: Boolean) {
            println("BluetoothController: onAppStatusChanged: $registered")
        }

        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            println("BluetoothController: onServiceStateChanged: ${proxy.toString()}")
        }
    }

    override fun startDiscovery(clearFoundDevices: Boolean) {
        if (!isPermissionGranted(context, Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        val scanIntentFilter = IntentFilter()
        scanIntentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        scanIntentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        scanIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bluetoothScanReceiver, scanIntentFilter)

        updateBondedDevices()
        if (clearFoundDevices) {
            _foundDevices.update { emptyList() }
        }
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!isPermissionGranted(context, Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        context.unregisterReceiver(bluetoothScanReceiver)
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun updateBondedDevices() {
        if (!isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.also { devices -> _bondedDevices.update { devices.toList() } }
    }

    override fun init() {
        hidDeviceProfile = hidDataSender.register(context, profileListener)
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        updateBondedDevices()
    }

    override fun release() {
        context.unregisterReceiver(bluetoothStateReceiver)
        hidDataSender.unregister(context, profileListener)
    }

    override fun pair(device: BluetoothDevice) {
        if (!isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        device.createBond()
    }

    override fun unpair(device: BluetoothDevice) {
        if (!isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        if (device.bondState == BluetoothDevice.BOND_BONDING) {
            BluetoothUtils.cancelBondProcess(device)
        } else if (device.bondState == BluetoothDevice.BOND_BONDED) {
            BluetoothUtils.removeBond(device)
        }
    }

    override fun connect(device: BluetoothDevice) {
        hidDataSender.requestConnect(device)
    }

    override fun disconnect(device: BluetoothDevice) {
        hidDataSender.requestConnect(null)
    }

    private fun onDeviceBondStateChanged(device: BluetoothDevice) {
        if (!isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT) ||
            !isPermissionGranted(context, Manifest.permission.BLUETOOTH_SCAN)
        ) {
            return
        }
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> {
                _foundDevices.update { devices -> devices.filterNot { it == device } }
                _waitingForDeviceBonding.update { null }
                updateBondedDevices()
            }
            BluetoothDevice.BOND_BONDING -> {
                _waitingForDeviceBonding.update { device }
            }
            BluetoothDevice.BOND_NONE -> {
                _waitingForDeviceBonding.update { null }
                updateBondedDevices()
                if (bluetoothAdapter?.isDiscovering == true) {
                    stopDiscovery()
                }
                startDiscovery(false)
            }
        }
    }
}