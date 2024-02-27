package com.onandor.peripheryapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.IBluetoothController
import com.onandor.peripheryapp.kbm.data.BtDevice
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PairedBtDevicesUiState(
    val scannedDevices: List<BtDevice> = emptyList(),
    val pairedDevices: List<BtDevice> = emptyList(),
    val isBluetoothEnabled: Boolean = false,
    val canUseBluetooth: Boolean = false
)

@HiltViewModel
class PairedBtDevicesViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val bluetoothController: IBluetoothController
) : ViewModel() {

    private val _uiState = MutableStateFlow(PairedBtDevicesUiState())
    val uiState = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        bluetoothController.isBluetoothEnabled,
        _uiState
    ) { scannedDevices, pairedDevices, isBluetoothEnabled, uiState ->
        uiState.copy(
            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            isBluetoothEnabled = isBluetoothEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _uiState.value
    )

    fun pairNewDevice() {
        navManager.navigateTo(NavActions.newBtConnection())
    }

    fun updatePairedDevices() {
        bluetoothController.updatePairedDevices()
    }
}