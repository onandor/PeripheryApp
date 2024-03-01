package com.onandor.peripheryapp.kbm.viewmodels

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.bluetooth.IBluetoothController
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BtDevicesUiState(
    val foundDevices: List<BluetoothDevice> = emptyList(),
    val bondedDevices: List<BluetoothDevice> = emptyList(),
    val bluetoothState: Int = BluetoothAdapter.STATE_OFF,
    val waitingForDeviceBonding: BluetoothDevice? = null,
    val waitingForDeviceConnecting: BluetoothDevice? = null,
    val connectedDevice: BluetoothDevice? = null
)

@HiltViewModel
class BtDevicesViewModel @Inject constructor(
    private val bluetoothController: IBluetoothController,
    private val navManager: INavigationManager
) : ViewModel() {

    private data class BluetoothControllerFlows(
        val foundDevices: List<BluetoothDevice>,
        val bondedDevices: List<BluetoothDevice>,
        val bluetoothState: Int,
        val waitingForDeviceBonding: BluetoothDevice?,
        val waitingForDeviceConnecting: BluetoothDevice?,
        val connectedDevice: BluetoothDevice?
    )

    private val bluetoothControllerFlows = com.onandor.peripheryapp.utils.combine(
        bluetoothController.foundDevices,
        bluetoothController.bondedDevices,
        bluetoothController.bluetoothState,
        bluetoothController.waitingForDeviceBonding,
        bluetoothController.waitingForDeviceConnecting,
        bluetoothController.connectedDevice
    ) { foundDevices,
        bondedDevices,
        bluetoothState,
        waitingForDeviceBonding,
        waitingForDeviceConnecting,
        connectedDevice ->
        BluetoothControllerFlows(
            foundDevices = foundDevices,
            bondedDevices = bondedDevices,
            bluetoothState = bluetoothState,
            waitingForDeviceBonding = waitingForDeviceBonding,
            waitingForDeviceConnecting = waitingForDeviceConnecting,
            connectedDevice = connectedDevice
        )
    }

    private val _uiState = MutableStateFlow(BtDevicesUiState())
    val uiState = combine(
        bluetoothControllerFlows,
        _uiState
    ) { bluetoothControllerFlows, uiState ->
        uiState.copy(
            foundDevices = bluetoothControllerFlows.foundDevices,
            bondedDevices = bluetoothControllerFlows.bondedDevices,
            bluetoothState = bluetoothControllerFlows.bluetoothState,
            waitingForDeviceBonding = bluetoothControllerFlows.waitingForDeviceBonding,
            waitingForDeviceConnecting = bluetoothControllerFlows.waitingForDeviceConnecting,
            connectedDevice = bluetoothControllerFlows.connectedDevice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _uiState.value
    )

    private var bluetoothStateJob: Job? = null
    private var connectedDeviceJob: Job? = null

    init {
        bluetoothStateJob = bluetoothController.bluetoothState.onEach { state ->
            when (state) {
                BluetoothAdapter.STATE_ON -> {
                    updateBondedDevices()
                    bluetoothController.startDiscovery(true)
                }
                BluetoothAdapter.STATE_TURNING_OFF -> {
                    bluetoothController.stopDiscovery()
                }
            }
        }.launchIn(viewModelScope)

        connectedDeviceJob = bluetoothController.connectedDevice.onEach { device ->
            if (device != null) {
                navigateToInput()
            }
        }.launchIn(viewModelScope)
    }

    fun updateBondedDevices() {
        bluetoothController.updateBondedDevices()
    }

    fun requestConnect(device: BluetoothDevice) {
        bluetoothController.connect(device)
    }

    fun requestPair(device: BluetoothDevice) {
        bluetoothController.pair(device)
    }

    fun disconnect(device: BluetoothDevice) {
        bluetoothController.disconnect(device)
    }

    fun forget(device: BluetoothDevice) {
        bluetoothController.unpair(device)
    }

    fun navigateToInput() {
        navManager.navigateTo(NavActions.input())
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.stopDiscovery()
        bluetoothStateJob?.cancel()
        connectedDeviceJob?.cancel()
    }
}