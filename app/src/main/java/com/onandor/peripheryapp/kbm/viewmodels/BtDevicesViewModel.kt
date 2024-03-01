package com.onandor.peripheryapp.kbm.viewmodels

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
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothUtils
import com.onandor.peripheryapp.kbm.bluetooth.HidDataSender
import com.onandor.peripheryapp.kbm.bluetooth.HidDeviceProfile
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BtDevicesUiState(
    val foundDevices: List<BluetoothDevice> = emptyList(),
    val bondedDevices: List<BluetoothDevice> = emptyList(),
    val bluetoothState: Int = BluetoothAdapter.STATE_OFF,
    val waitingForDeviceBonding: BluetoothDevice? = null,
    val waitingForDeviceConnecting: BluetoothDevice? = null,
    val connectedDevice: BluetoothDevice? = null
)

@SuppressLint("MissingPermission")
@HiltViewModel
class BtDevicesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val navManager: INavigationManager,
    private val hidDataSender: HidDataSender
) : ViewModel() {

    private data class BluetoothControllerFlows(
        val foundDevices: List<BluetoothDevice>,
        val bondedDevices: List<BluetoothDevice>,
        val bluetoothState: Int,
        val waitingForDeviceBonding: BluetoothDevice?,
        val waitingForDeviceConnecting: BluetoothDevice?,
        val connectedDevice: BluetoothDevice?
    )

    private val _uiState = MutableStateFlow(BtDevicesUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var hidDeviceProfile: HidDeviceProfile

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

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
                        _uiState.update {
                            val foundDevices = if (device in it.foundDevices ) {
                                it.foundDevices
                            } else {
                                it.foundDevices + device
                            }
                            it.copy(foundDevices = foundDevices)
                        }
                    }
                }
                BluetoothDevice.ACTION_NAME_CHANGED -> {
                    _uiState.update { uiState ->
                        val foundDevices = if (device in uiState.foundDevices) {
                            uiState.foundDevices.filterNot { it.address == device.address } + device
                        } else {
                            uiState.foundDevices + device
                        }
                        uiState.copy(foundDevices = foundDevices)
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
                    _uiState.update { it.copy(bluetoothState = state) }
                    when(state) {
                        BluetoothAdapter.STATE_ON -> {
                            updateBondedDevices()
                            startDiscovery(true)
                        }
                        BluetoothAdapter.STATE_OFF -> {
                            _uiState.update {
                                it.copy(
                                    foundDevices = emptyList(),
                                    bondedDevices = emptyList()
                                )
                            }
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {
                            stopDiscovery()
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
                    _uiState.update {
                        it.copy(
                            waitingForDeviceConnecting = null,
                            connectedDevice = device
                        )
                    }
                    if (device != null) {
                        navigateToInput()
                    }
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _uiState.update { it.copy(waitingForDeviceConnecting = device) }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _uiState.update { it.copy(connectedDevice = null) }
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

    init {
        _uiState.update {
            it.copy(bluetoothState = bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF)
        }
        hidDeviceProfile = hidDataSender.register(context, profileListener)
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
        updateBondedDevices()
        startDiscovery(false)
    }

    fun updateBondedDevices() {
        if (!BluetoothUtils.isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.also { devices -> _uiState.update { it.copy(bondedDevices = devices.toList()) } }
    }

    fun connect(device: BluetoothDevice) {
        hidDataSender.requestConnect(device)
    }

    fun disconnect(device: BluetoothDevice) {
        hidDataSender.requestConnect(null)
    }

    fun pair(device: BluetoothDevice) {
        if (!BluetoothUtils.isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        device.createBond()
    }

    fun forget(device: BluetoothDevice) {
        if (!BluetoothUtils.isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        if (device.bondState == BluetoothDevice.BOND_BONDING) {
            BluetoothUtils.cancelBondProcess(device)
        } else if (device.bondState == BluetoothDevice.BOND_BONDED) {
            BluetoothUtils.removeBond(device)
        }
    }

    fun navigateToInput() {
        navManager.navigateTo(NavActions.input())
    }

    private fun startDiscovery(clearFoundDevices: Boolean) {
        if (!BluetoothUtils.isPermissionGranted(context, Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        val scanIntentFilter = IntentFilter()
        scanIntentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        scanIntentFilter.addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        scanIntentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        context.registerReceiver(bluetoothScanReceiver, scanIntentFilter)

        updateBondedDevices()
        if (clearFoundDevices) {
            _uiState.update { it.copy(foundDevices = emptyList()) }
        }
        bluetoothAdapter?.startDiscovery()
    }

    private fun stopDiscovery() {
        if (!BluetoothUtils.isPermissionGranted(context, Manifest.permission.BLUETOOTH_SCAN)) {
            return
        }
        context.unregisterReceiver(bluetoothScanReceiver)
        bluetoothAdapter?.cancelDiscovery()
    }

    private fun onDeviceBondStateChanged(device: BluetoothDevice) {
        if (!BluetoothUtils.isPermissionGranted(context, Manifest.permission.BLUETOOTH_CONNECT) ||
            !BluetoothUtils.isPermissionGranted(context, Manifest.permission.BLUETOOTH_SCAN)
        ) {
            return
        }
        when (device.bondState) {
            BluetoothDevice.BOND_BONDED -> {
                _uiState.update { uiState ->
                    uiState.copy(
                        foundDevices = uiState.foundDevices.filterNot { it == device },
                        waitingForDeviceBonding = null
                    )
                }
                updateBondedDevices()
            }
            BluetoothDevice.BOND_BONDING -> {
                _uiState.update { it.copy(waitingForDeviceBonding = device) }
            }
            BluetoothDevice.BOND_NONE -> {
                _uiState.update { it.copy(waitingForDeviceBonding = null) }
                updateBondedDevices()
                if (bluetoothAdapter?.isDiscovering == true) {
                    stopDiscovery()
                }
                startDiscovery(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
        context.unregisterReceiver(bluetoothStateReceiver)
        hidDataSender.unregister(context, profileListener)
    }
}