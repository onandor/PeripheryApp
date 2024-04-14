package com.onandor.peripheryapp.webcam.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.utils.WebcamSettingKeys
import com.onandor.peripheryapp.webcam.stream.Streamer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewConnectionUiState(
    val isCameraPermissionGranted: Boolean = false,
    val address: String = "",
    val port: String = "",
    val connecting: Boolean = false,
    val canConnect: Boolean = false,
    val connectionEvent: Streamer.ConnectionEvent? = null
)

@HiltViewModel
class NewConnectionViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val streamer: Streamer,
    private val settings: Settings
) : ViewModel() {

    private val portPattern = Regex("^\\d+\$")

    private val _uiState = MutableStateFlow(NewConnectionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val previousAddress = settings.get(WebcamSettingKeys.PREVIOUS_ADDRESS)
            val previousPort = settings.get(WebcamSettingKeys.PREVIOUS_PORT)
            val canConnect = previousAddress.isNotEmpty() && previousPort.isNotEmpty()
            _uiState.update {
                it.copy(
                    address = previousAddress,
                    port = previousPort,
                    canConnect = canConnect
                )
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            streamer.connectionEventFlow.collect {
                onConnectionEvent(it)
            }
        }
    }

    fun onCameraPermissionGranted() {
        _uiState.update { it.copy(isCameraPermissionGranted = true) }
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    fun onAddressChanged(value: String) {
        val canConnect = value.isNotEmpty() && uiState.value.port.isNotEmpty()
        _uiState.update {
            it.copy(
                address = value,
                canConnect = canConnect
            )
        }
    }

    fun onPortChanged(value: String) {
        val canConnect = uiState.value.address.isNotEmpty() && value.isNotEmpty()
        if (value.isEmpty() || value.matches(portPattern)) {
            _uiState.update {
                it.copy(
                    port = value,
                    canConnect = canConnect
                )
            }
        }
    }

    fun onConnect() {
        _uiState.update { it.copy(connecting = true) }
        streamer.connect(uiState.value.address, uiState.value.port.toInt())
            .thenAccept { result ->
                _uiState.update { it.copy(connecting = false) }
                onConnectionEvent(result)
            }
    }


    private fun onConnectionEvent(event: Streamer.ConnectionEvent) {
        when (event) {
            Streamer.ConnectionEvent.ConnectionSuccess ->  {
                CoroutineScope(Dispatchers.IO).launch {
                    settings.save(WebcamSettingKeys.PREVIOUS_ADDRESS, uiState.value.address)
                    settings.save(WebcamSettingKeys.PREVIOUS_PORT, uiState.value.port)
                }
                navManager.navigateTo(NavActions.Webcam.camera())
            }
            Streamer.ConnectionEvent.UnknownHostFailure -> {
                _uiState.update { it.copy(connectionEvent = event) }
            }
            Streamer.ConnectionEvent.TimeoutFailure -> {
                _uiState.update { it.copy(connectionEvent = event) }
            }
            Streamer.ConnectionEvent.HostUnreachableFailure -> {
                _uiState.update { it.copy(connectionEvent = event) }
            }
        }
    }

    fun onToastShown() {
        _uiState.update { it.copy(connectionEvent = null) }
    }
}