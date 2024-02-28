package com.onandor.peripheryapp.viewmodels

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.IBluetoothController
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BondedBtDevicesUiState(
    val foundDevices: List<BluetoothDevice> = emptyList(),
    val bondedDevices: List<BluetoothDevice> = emptyList(),
    val isBluetoothEnabled: Boolean = false,
    val canUseBluetooth: Boolean = false
)

@HiltViewModel
class BondedBtDevicesViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val bluetoothController: IBluetoothController
) : ViewModel() {

    private val _uiState = MutableStateFlow(BondedBtDevicesUiState())
    val uiState = combine(
        bluetoothController.foundDevices,
        bluetoothController.bondedDevices,
        bluetoothController.isBluetoothEnabled,
        _uiState
    ) { foundDevices, bondedDevices, isBluetoothEnabled, uiState ->
        uiState.copy(
            foundDevices = foundDevices,
            bondedDevices = bondedDevices,
            isBluetoothEnabled = isBluetoothEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = _uiState.value
    )

    init {
        bluetoothController.init()
    }

    fun pairNewDevice() {
        navManager.navigateTo(NavActions.pairBtDevice())
    }

    fun updateBondedDevices() {
        bluetoothController.updateBondedDevices()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}