package com.onandor.peripheryapp.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.kbm.BtConnectionResult
import com.onandor.peripheryapp.kbm.IBluetoothController
import com.onandor.peripheryapp.kbm.data.BtDevice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NewBtConnectionUiState(
    val scannedDevices: List<BtDevice> = emptyList(),
    val pairedDevices: List<BtDevice> = emptyList(),
    val isBluetoothEnabled: Boolean = false,
    val searchForDevicesDialogShown: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class NewBtConnectionViewModel @Inject constructor(
    private val bluetoothController: IBluetoothController
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewBtConnectionUiState())
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

    private var deviceConnection: Job? = null

    init {
        bluetoothController.isConnected.onEach { isConnected ->
            _uiState.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _uiState.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
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

    fun updatePairedDevices() {
        bluetoothController.updatePairedDevices()
    }

    fun connectToDevice(device: BtDevice) {
        _uiState.update { it.copy(isConnecting = true) }
        deviceConnection = bluetoothController
            .connectToDevice(device)
            .listen()
    }

    fun errorMessageShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun Flow<BtConnectionResult>.listen(): Job {
        return onEach { result ->
            when (result) {
                BtConnectionResult.ConnectionEstablished -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = true,
                            errorMessage = null
                        )
                    }
                }
                is BtConnectionResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
            .catch { throwable ->
                bluetoothController.closeConnection()
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        isConnected = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}