package com.onandor.peripheryapp.kbm.viewmodels

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.os.CountDownTimer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothUtils
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.kbm.bluetooth.HidDeviceProfile
import com.onandor.peripheryapp.kbm.bluetooth.events.BluetoothScanEvent
import com.onandor.peripheryapp.kbm.bluetooth.events.BluetoothStateEvent
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import com.onandor.peripheryapp.utils.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BtDevicesUiState(
    val foundDevices: List<BluetoothDevice> = emptyList(),
    val bondedDevices: List<BluetoothDevice> = emptyList(),
    val bluetoothState: Int = BluetoothAdapter.STATE_OFF,
    val waitingForDeviceBonding: BluetoothDevice? = null,
    val waitingForDeviceConnecting: BluetoothDevice? = null,
    val connectedDevice: BluetoothDevice? = null,
    val arePermissionsGranted: Boolean = false,
    val expandedBondedDevice: BluetoothDevice? = null,
    val remainingDiscoverable: Int = 0
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

    private val discoverabilityTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
        var running = false

        fun reset() {
            _uiState.update { it.copy(remainingDiscoverable = 120) }
            if (!running) {
                running = true
                this.start()
            }
        }

        fun stop() {
            running = false
            this.cancel()
        }

        override fun onTick(millisUntilFinished: Long) {
            val remaining = _uiState.value.remainingDiscoverable - 1
            _uiState.update { it.copy(remainingDiscoverable = remaining) }
            if (remaining == 0) {
                stop()
            }
        }

        override fun onFinish() { }
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
                    _uiState.update {
                        it.copy(
                            connectedDevice = null,
                            waitingForDeviceConnecting = null
                        )
                    }
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

    private fun updateBondedDevices() {
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

    private fun onBluetoothStateEvent(event: BluetoothStateEvent) {
        when (event) {
            is BluetoothStateEvent.StateChanged -> {
                _uiState.update { it.copy(bluetoothState = event.state) }
                when (event.state) {
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
            is BluetoothStateEvent.ScanModeChanged -> {
                val discoverable =
                    event.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                if (discoverable) {
                    _uiState.update { it.copy(remainingDiscoverable = 120) }
                    discoverabilityTimer.reset()
                } else {
                    _uiState.update { it.copy(remainingDiscoverable = 0) }
                    discoverabilityTimer.stop()
                }
            }
        }
    }

    private fun onBluetoothScanEvent(event: BluetoothScanEvent) {
        when (event) {
            is BluetoothScanEvent.DeviceFound -> {
                if (hidDeviceProfile.isHidHostProfileSupported(event.device) &&
                    event.device.bondState == BluetoothDevice.BOND_NONE) {
                    _uiState.update {
                        val foundDevices = if (event.device in it.foundDevices ) {
                            it.foundDevices
                        } else {
                            it.foundDevices + event.device
                        }
                        it.copy(foundDevices = foundDevices)
                    }
                }
            }
            is BluetoothScanEvent.DeviceNameChanged -> {
                _uiState.update { uiState ->
                    val foundDevices = if (event.device in uiState.foundDevices) {
                        uiState.foundDevices.filterNot {
                            it.address == event.device.address
                        } + event.device
                    } else {
                        uiState.foundDevices + event.device
                    }
                    uiState.copy(foundDevices = foundDevices)
                }
            }
            is BluetoothScanEvent.DeviceBondStateChanged -> {
                onDeviceBondStateChanged(event.device)
            }
        }
    }

    private fun collectBluetoothStateEvents() {
        viewModelScope.launch {
            bluetoothController
                .getBluetoothStateEventFlow()
                .collect { onBluetoothStateEvent(it)}
        }
    }

    private fun collectBluetoothScanEvents() {
        viewModelScope.launch {
            bluetoothController
                .getBluetoothScanEventFlow()
                .collect { onBluetoothScanEvent(it) }
        }
    }

    fun onPermissionsGranted() {
        hidDeviceProfile = bluetoothController.registerProfileListener(hidProfileListener)
        bluetoothAdapter = hidDeviceProfile.bluetoothAdapter
        collectBluetoothScanEvents()
        collectBluetoothStateEvents()

        _uiState.update {
            it.copy(
                bluetoothState = bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF,
                arePermissionsGranted = true
            )
        }

        updateBondedDevices()
        bluetoothController.startDiscovery()
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    fun onBondedDeviceClick(device: BluetoothDevice) {
        _uiState.update {
            it.copy(
                expandedBondedDevice = if (it.expandedBondedDevice == device) {
                    null
                } else {
                    device
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.stopDiscovery()
        bluetoothController.unregisterProfileListener(hidProfileListener)
    }
}