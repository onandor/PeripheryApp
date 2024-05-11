package com.onandor.peripheryapp.webcam.viewmodels


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import com.onandor.peripheryapp.navigation.navargs.CameraNavArgs
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.utils.WebcamSettingKeys
import com.onandor.peripheryapp.webcam.stream.CameraController
import com.onandor.peripheryapp.webcam.stream.CameraInfo
import com.onandor.peripheryapp.webcam.stream.Encoder
import com.onandor.peripheryapp.webcam.stream.Streamer
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val connectionEvent: Streamer.ConnectionEvent? = null,

    val cameraInfos: List<CameraInfo> = emptyList(),
    val cameraId: String = "",
    val resolutionIdx: Int = 0,
    val frameRateRangeIdx: Int = 0,
    val bitRate: Int = Encoder.DEFAULT_BIT_RATE,
    val bitRates: List<Int> = Encoder.BIT_RATES
)

@HiltViewModel
class NewConnectionViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val streamer: Streamer,
    private val settings: Settings,
    cameraController: CameraController
) : ViewModel() {

    private val portPattern = Regex("^\\d+\$")

    private val _uiState = MutableStateFlow(NewConnectionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
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
        viewModelScope.launch {
            streamer.connectionEventFlow.collect {
                onConnectionEvent(it)
            }
        }

        val cameraInfos = cameraController.getCameraInfos()
        _uiState.update {
            it.copy(
                cameraInfos = cameraInfos,
                cameraId = cameraInfos.first().id,
            )
        }
        viewModelScope.launch {
            val cameraId = settings.get(WebcamSettingKeys.CAMERA_ID)
            if (cameraId.isEmpty()) {
                return@launch
            }
            val resolutionIdx = settings.get(WebcamSettingKeys.RESOLUTION_IDX)
            val frameRateRangeIdx = settings.get(WebcamSettingKeys.FRAME_RATE_IDX)
            val bitRate = settings.get(WebcamSettingKeys.BIT_RATE)

            _uiState.update {
                it.copy(
                    cameraId = cameraId,
                    resolutionIdx = resolutionIdx,
                    frameRateRangeIdx = frameRateRangeIdx,
                    bitRate = bitRate
                )
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
        /*
        val navArgs = CameraNavArgs(
            cameraId = uiState.value.cameraId,
            resolutionIdx = uiState.value.resolutionIdx,
            frameRateRangeIdx = uiState.value.frameRateRangeIdx,
            bitRate = uiState.value.bitRate
        )
        navManager.navigateTo(NavActions.Webcam.camera(navArgs))
         */
    }


    private fun onConnectionEvent(event: Streamer.ConnectionEvent) {
        when (event) {
            Streamer.ConnectionEvent.CONNECTION_SUCCESS ->  {
                viewModelScope.launch {
                    settings.save(WebcamSettingKeys.PREVIOUS_ADDRESS, uiState.value.address)
                    settings.save(WebcamSettingKeys.PREVIOUS_PORT, uiState.value.port)
                }
                val navArgs = CameraNavArgs(
                    cameraId = uiState.value.cameraId,
                    resolutionIdx = uiState.value.resolutionIdx,
                    frameRateRangeIdx = uiState.value.frameRateRangeIdx,
                    bitRate = uiState.value.bitRate
                )
                navManager.navigateTo(NavActions.Webcam.camera(navArgs))
            }
            Streamer.ConnectionEvent.UNKNOWN_HOST_FAILURE -> {
                _uiState.update { it.copy(connectionEvent = event) }
            }
            Streamer.ConnectionEvent.TIMEOUT_FAILURE -> {
                _uiState.update { it.copy(connectionEvent = event) }
            }
            Streamer.ConnectionEvent.HOST_UNREACHABLE_FAILURE -> {
                _uiState.update { it.copy(connectionEvent = event) }
            }
        }
    }

    fun onToastShown() {
        _uiState.update { it.copy(connectionEvent = null) }
    }

    fun onCameraIdChanged(id: String) {
        _uiState.update {
            it.copy(
                cameraId = id,
                resolutionIdx = 0,
                frameRateRangeIdx = 0
            )
        }
        saveCameraSettings()
    }

    fun onResolutionIdxChanged(idx: Int) {
        _uiState.update { it.copy(resolutionIdx = idx) }
        saveCameraSettings()
    }

    fun onFrameRateRangeIdxChanged(idx: Int) {
        _uiState.update { it.copy(frameRateRangeIdx = idx) }
        saveCameraSettings()
    }

    fun onBitRateChanged(bitRate: Int) {
        _uiState.update { it.copy(bitRate = bitRate) }
        saveCameraSettings()
    }

    private fun saveCameraSettings() {
        viewModelScope.launch {
            settings.save(WebcamSettingKeys.CAMERA_ID, uiState.value.cameraId)
            settings.save(WebcamSettingKeys.RESOLUTION_IDX, uiState.value.resolutionIdx)
            settings.save(WebcamSettingKeys.FRAME_RATE_IDX, uiState.value.frameRateRangeIdx)
            settings.save(WebcamSettingKeys.BIT_RATE, uiState.value.bitRate)
        }
    }
}