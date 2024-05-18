package com.onandor.peripheryapp.webcam.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import com.onandor.peripheryapp.navigation.navargs.CameraNavArgs
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.utils.WebcamSettingKeys
import com.onandor.peripheryapp.webcam.video.CameraController
import com.onandor.peripheryapp.webcam.video.CameraInfo
import com.onandor.peripheryapp.webcam.video.encoders.H264Encoder
import com.onandor.peripheryapp.webcam.video.streamers.StreamerType
import com.onandor.peripheryapp.webcam.video.Utils
import com.onandor.peripheryapp.webcam.network.TcpServer
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
    val tcpServerEvent: TcpServer.Event? = null,
    val streamerType: Int = StreamerType.CLIENT,

    val cameraInfos: List<CameraInfo> = emptyList(),
    val cameraId: String = "",
    val resolutionIdx: Int = 0,
    val frameRateRangeIdx: Int = 0,
    val bitRate: Int = H264Encoder.DEFAULT_BIT_RATE,
    val bitRates: List<Int> = H264Encoder.BIT_RATES,
    val noCameras: Boolean = false
)

@HiltViewModel
class NewConnectionViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val settings: Settings,
    cameraController: CameraController,
    private val tcpServer: TcpServer
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewConnectionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val streamerType = settings.get(WebcamSettingKeys.STREAMER_TYPE)
            _uiState.update {
                it.copy(
                    address = Utils.getIPAddress(),
                    port = TcpServer.PORT.toString(),
                    streamerType = streamerType
                )
            }
        }

        tcpServer.start()
        viewModelScope.launch {
            tcpServer.eventFlow.collect { onTcpServerEvent(it) }
        }

        val cameraInfos = cameraController.getCameraInfos()
        if (cameraInfos.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    cameraInfos = cameraInfos,
                    cameraId = cameraInfos.first().id,
                )
            }
        } else {
            _uiState.update { it.copy(noCameras = true) }
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
        tcpServer.stop()
        navManager.navigateBack()
    }

    private fun onTcpServerEvent(event: TcpServer.Event) {
        when (event) {
            is TcpServer.Event.ClientConnected -> {
                val navArgs = CameraNavArgs(
                    cameraId = uiState.value.cameraId,
                    resolutionIdx = uiState.value.resolutionIdx,
                    frameRateRangeIdx = uiState.value.frameRateRangeIdx,
                    bitRate = uiState.value.bitRate,
                    streamerType = uiState.value.streamerType
                )
                navManager.navigateTo(NavActions.Webcam.camera(navArgs))
            }
            is TcpServer.Event.ClientDisconnected -> {}
            TcpServer.Event.ClientCannotConnect -> {}
            else -> { _uiState.update { it.copy(tcpServerEvent = event) }}
        }
    }

    fun onToastShown() {
        _uiState.update { it.copy(tcpServerEvent = null) }
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

    fun onStreamerTypeChanged(type: Int) {
        _uiState.update { it.copy(streamerType = type) }
        viewModelScope.launch { settings.save(WebcamSettingKeys.STREAMER_TYPE, type) }
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