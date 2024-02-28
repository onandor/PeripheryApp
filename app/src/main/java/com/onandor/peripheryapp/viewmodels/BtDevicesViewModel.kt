package com.onandor.peripheryapp.viewmodels

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.IBluetoothController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class BtDevicesUiState(
    val foundDevices: List<BluetoothDevice> = emptyList(),
    val bondedDevices: List<BluetoothDevice> = emptyList(),
    val bluetoothState: Int = BluetoothAdapter.STATE_OFF
)

@HiltViewModel
class BtDevicesViewModel @Inject constructor(
    private val bluetoothController: IBluetoothController
) : ViewModel() {

    private val _uiState = MutableStateFlow(BtDevicesUiState())
    val uiState = combine(
        bluetoothController.foundDevices,
        bluetoothController.bondedDevices,
        bluetoothController.bluetoothState,
        _uiState
    ) { foundDevices, bondedDevices, bluetoothState, uiState ->
        uiState.copy(
            foundDevices = foundDevices,
            bondedDevices = bondedDevices,
            bluetoothState = bluetoothState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _uiState.value
    )

    private var bluetoothStateJob: Job? = null

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

    override fun onCleared() {
        super.onCleared()
        bluetoothController.stopDiscovery()
        bluetoothStateJob?.cancel()
    }
}