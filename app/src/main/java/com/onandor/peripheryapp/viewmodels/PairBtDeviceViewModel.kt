package com.onandor.peripheryapp.viewmodels

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.IBluetoothController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PairBtDeviceUiState(
    val foundDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val isBluetoothEnabled: Boolean = false,
    val searchForDevicesDialogShown: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class PairBtDeviceViewModel @Inject constructor(
    private val bluetoothController: IBluetoothController
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairBtDeviceUiState())
    val uiState = combine(
        bluetoothController.foundDevices,
        bluetoothController.bondedDevices,
        bluetoothController.isBluetoothEnabled,
        _uiState
    ) { foundDevices, pairedDevices, isBluetoothEnabled, uiState ->
        uiState.copy(
            foundDevices = foundDevices,
            pairedDevices = pairedDevices,
            isBluetoothEnabled = isBluetoothEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _uiState.value
    )

    init {
        /*
        bluetoothController.isConnected.onEach { isConnected ->
            _uiState.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _uiState.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
         */
    }

    fun showSearchForDevicesDialog() {
        bluetoothController.startDiscovery()
        _uiState.update {
            it.copy(
                searchForDevicesDialogShown = true
            )
        }
    }

    fun dismissSearchForDevicesDialog() {
        _uiState.update {
            it.copy(
                searchForDevicesDialogShown = false
            )
        }
        bluetoothController.stopDiscovery()
    }

    fun updateBondedDevices() {
        bluetoothController.updateBondedDevices()
    }

    fun requestPair(device: BluetoothDevice) {
        bluetoothController.pair(device)
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}