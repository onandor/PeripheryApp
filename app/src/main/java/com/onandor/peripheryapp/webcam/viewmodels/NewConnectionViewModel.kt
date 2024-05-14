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
import com.onandor.peripheryapp.webcam.stream.DCStreamer
import com.onandor.peripheryapp.webcam.stream.ClientEncoder
import com.onandor.peripheryapp.webcam.stream.ClientStreamer
import com.onandor.peripheryapp.webcam.stream.StreamerType
import com.onandor.peripheryapp.webcam.stream.Utils
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
    val clientStreamerConnEvent: ClientStreamer.ConnectionEvent? = null,
    val dcStreamerConnEvent: DCStreamer.ConnectionEvent? = null,
    val streamerType: Int = StreamerType.CLIENT,

    val cameraInfos: List<CameraInfo> = emptyList(),
    val cameraId: String = "",
    val resolutionIdx: Int = 0,
    val frameRateRangeIdx: Int = 0,
    val bitRate: Int = ClientEncoder.DEFAULT_BIT_RATE,
    val bitRates: List<Int> = ClientEncoder.BIT_RATES
)

@HiltViewModel
class NewConnectionViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val clientStreamer: ClientStreamer,
    private val settings: Settings,
    cameraController: CameraController,
    private val dcStreamer: DCStreamer
) : ViewModel() {

    private val portPattern = Regex("^\\d+\$")

    private val _uiState = MutableStateFlow(NewConnectionUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val streamerType = settings.get(WebcamSettingKeys.STREAMER_TYPE)
            if (streamerType == StreamerType.CLIENT) {
                loadClientStreamerValues()
            } else {
                loadDCStreamerValues()
                dcStreamer.startServer()
            }
        }

        viewModelScope.launch {
            clientStreamer.connectionEventFlow.collect {
                onClientConnectionEvent(it)
            }
        }
        viewModelScope.launch {
            dcStreamer.connectionEventFlow.collect {
                onDCConnectionEvent(it)
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
        dcStreamer.stopServer()
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
        clientStreamer.connect(uiState.value.address, uiState.value.port.toInt())
            .thenAccept { result ->
                _uiState.update { it.copy(connecting = false) }
                onClientConnectionEvent(result)
            }
        /*
        val navArgs = CameraNavArgs(
            cameraId = uiState.value.cameraId,
            resolutionIdx = uiState.value.resolutionIdx,
            frameRateRangeIdx = uiState.value.frameRateRangeIdx,
            bitRate = uiState.value.bitRate,
            streamerType = StreamerType.CLIENT
        )
        navManager.navigateTo(NavActions.Webcam.camera(navArgs))
         */
    }

    private fun onClientConnectionEvent(event: ClientStreamer.ConnectionEvent) {
        when (event) {
            ClientStreamer.ConnectionEvent.CONNECTION_SUCCESS ->  {
                viewModelScope.launch {
                    settings.save(WebcamSettingKeys.PREVIOUS_ADDRESS, uiState.value.address)
                    settings.save(WebcamSettingKeys.PREVIOUS_PORT, uiState.value.port)
                }
                val navArgs = CameraNavArgs(
                    cameraId = uiState.value.cameraId,
                    resolutionIdx = uiState.value.resolutionIdx,
                    frameRateRangeIdx = uiState.value.frameRateRangeIdx,
                    bitRate = uiState.value.bitRate,
                    streamerType = StreamerType.CLIENT
                )
                navManager.navigateTo(NavActions.Webcam.camera(navArgs))
            }
            ClientStreamer.ConnectionEvent.UNKNOWN_HOST_FAILURE -> {
                _uiState.update { it.copy(clientStreamerConnEvent = event) }
            }
            ClientStreamer.ConnectionEvent.TIMEOUT_FAILURE -> {
                _uiState.update { it.copy(clientStreamerConnEvent = event) }
            }
            ClientStreamer.ConnectionEvent.HOST_UNREACHABLE_FAILURE -> {
                _uiState.update { it.copy(clientStreamerConnEvent = event) }
            }
        }
    }

    private fun onDCConnectionEvent(event: DCStreamer.ConnectionEvent) {
        when (event) {
            DCStreamer.ConnectionEvent.CLIENT_CONNECTED -> {
                val navArgs = CameraNavArgs(
                    cameraId = uiState.value.cameraId,
                    resolutionIdx = uiState.value.resolutionIdx,
                    frameRateRangeIdx = uiState.value.frameRateRangeIdx,
                    bitRate = uiState.value.bitRate,
                    streamerType = StreamerType.DC
                )
                navManager.navigateTo(NavActions.Webcam.camera(navArgs))
            }
            DCStreamer.ConnectionEvent.CLIENT_DISCONNECTED -> {

            }
            DCStreamer.ConnectionEvent.CONNECTION_LOST -> {

            }
            DCStreamer.ConnectionEvent.PORT_IN_USE -> {
                _uiState.update { it.copy() }
            }
        }
    }

    fun onToastShown() {
        _uiState.update {
            it.copy(
                clientStreamerConnEvent = null,
                dcStreamerConnEvent = null
            )
        }
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
        if (uiState.value.streamerType == type) {
            return
        }
        if (type == StreamerType.CLIENT) {
            loadClientStreamerValues()
            dcStreamer.stopServer()
        } else {
            loadDCStreamerValues()
            dcStreamer.startServer()
        }
        viewModelScope.launch {
            settings.save(WebcamSettingKeys.STREAMER_TYPE, uiState.value.streamerType)
        }
    }

    private fun loadClientStreamerValues() {
        viewModelScope.launch {
            val previousAddress = settings.get(WebcamSettingKeys.PREVIOUS_ADDRESS)
            val previousPort = settings.get(WebcamSettingKeys.PREVIOUS_PORT)
            val canConnect = previousAddress.isNotEmpty() && previousPort.isNotEmpty()
            _uiState.update {
                it.copy(
                    address = previousAddress,
                    port = previousPort,
                    canConnect = canConnect,
                    streamerType = StreamerType.CLIENT
                )
            }
        }
    }

    private fun loadDCStreamerValues() {
        _uiState.update {
            it.copy(
                address = Utils.getIPAddress(),
                port = DCStreamer.PORT.toString(),
                streamerType = StreamerType.DC
            )
        }
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