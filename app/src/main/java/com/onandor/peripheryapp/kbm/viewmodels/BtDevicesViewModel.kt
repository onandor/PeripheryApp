package com.onandor.peripheryapp.kbm.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothUtils
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.kbm.bluetooth.HidDeviceProfile
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import com.onandor.peripheryapp.utils.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val navManager: INavigationManager,
    private val bluetoothController: BluetoothController,
    private val permissionChecker: PermissionChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(BtDevicesUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var hidDeviceProfile: HidDeviceProfile
    private var bluetoothAdapter: BluetoothAdapter? = null

    private val bluetoothScanListener = object : BluetoothController.BluetoothScanListener {

        override fun onDeviceFound(device: BluetoothDevice) {
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

        override fun onDeviceNameChanged(device: BluetoothDevice) {
            _uiState.update { uiState ->
                val foundDevices = if (device in uiState.foundDevices) {
                    uiState.foundDevices.filterNot { it.address == device.address } + device
                } else {
                    uiState.foundDevices + device
                }
                uiState.copy(foundDevices = foundDevices)
            }
        }

        override fun onDeviceBondStateChanged(device: BluetoothDevice) {
            this@BtDevicesViewModel.onDeviceBondStateChanged(device)
        }
    }

    private val bluetoothStateListener = object : BluetoothController.BluetoothStateListener {

        override fun onStateChanged(state: Int) {
            _uiState.update { it.copy(bluetoothState = state) }
            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    updateBondedDevices()
                    bluetoothController.startDiscovery()
                }
                BluetoothAdapter.STATE_OFF -> {
                    _uiState.update {
                        it.copy(
                            bluetoothState = BluetoothAdapter.STATE_OFF,
                            foundDevices = emptyList(),
                            bondedDevices = emptyList()
                        )
                    }
                }
                BluetoothAdapter.STATE_TURNING_OFF -> {
                    bluetoothController.stopDiscovery()
                }
            }
        }
    }

    private val hidProfileListener = object : BluetoothController.HidProfileListener {

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
            if (registered) {
                return
            }
            _uiState.update {
                it.copy(
                    foundDevices = emptyList(),
                    bondedDevices = emptyList(),
                    connectedDevice = null,
                    waitingForDeviceBonding = null,
                    waitingForDeviceConnecting = null
                )
            }
        }

        override fun onServiceStateChanged(proxy: BluetoothProfile?) {
            if (proxy != null) {
                return
            }
            _uiState.update {
                it.copy(
                    foundDevices = emptyList(),
                    bondedDevices = emptyList(),
                    connectedDevice = null,
                    waitingForDeviceBonding = null,
                    waitingForDeviceConnecting = null
                )
            }
        }
    }

    init {
        hidDeviceProfile = bluetoothController.registerProfileListener(hidProfileListener)
        bluetoothAdapter = hidDeviceProfile.bluetoothAdapter
        bluetoothController.registerScanListener(bluetoothScanListener)
        bluetoothController.registerStateListener(bluetoothStateListener)

        _uiState.update {
            it.copy(bluetoothState = bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF)
        }
        updateBondedDevices()
        bluetoothController.startDiscovery()
    }

    fun updateBondedDevices() {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        bluetoothAdapter
            ?.bondedDevices
            ?.also { devices -> _uiState.update { it.copy(bondedDevices = devices.toList()) } }
    }

    fun connect(device: BluetoothDevice) {
        bluetoothController.requestConnect(device)
    }

    fun disconnect() {
        bluetoothController.requestConnect(null)
    }

    fun pair(device: BluetoothDevice) {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
            return
        }
        device.createBond()
    }

    fun forget(device: BluetoothDevice) {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT)) {
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

    private fun onDeviceBondStateChanged(device: BluetoothDevice) {
        if (!permissionChecker.isGranted(Manifest.permission.BLUETOOTH_CONNECT) ||
            !permissionChecker.isGranted(Manifest.permission.BLUETOOTH_SCAN)
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
                bluetoothController.stopDiscovery()
                bluetoothController.startDiscovery()
            }
        }
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.stopDiscovery()
        bluetoothController.unregisterScanListener(bluetoothScanListener)
        bluetoothController.unregisterStateListener(bluetoothStateListener)
        bluetoothController.unregisterProfileListener(hidProfileListener)
    }
}