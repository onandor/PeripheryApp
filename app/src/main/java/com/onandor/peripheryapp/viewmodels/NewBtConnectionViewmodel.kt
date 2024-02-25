package com.onandor.peripheryapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.IBluetoothController
import com.onandor.peripheryapp.kbm.data.BtDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NewBtConnectionUiState(
    val scannedDevices: List<BtDevice> = emptyList(),
    val pairedDevices: List<BtDevice> = emptyList(),
    val searchForDevicesDialogShown: Boolean = false
)

@HiltViewModel
class NewBtConnectionViewmodel @Inject constructor(
    private val bluetoothController: IBluetoothController
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewBtConnectionUiState())
    val uiState = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _uiState
    ) { scannedDevices, pairedDevices, uiState ->
        uiState.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _uiState.value
    )

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
}