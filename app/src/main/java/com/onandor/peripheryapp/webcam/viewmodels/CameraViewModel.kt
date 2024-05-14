package com.onandor.peripheryapp.webcam.viewmodels

import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.navargs.CameraNavArgs
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.webcam.stream.CameraController
import com.onandor.peripheryapp.webcam.stream.CameraInfo
import com.onandor.peripheryapp.webcam.stream.DCEncoder
import com.onandor.peripheryapp.webcam.stream.DCStreamer
import com.onandor.peripheryapp.webcam.stream.Encoder
import com.onandor.peripheryapp.webcam.stream.ClientStreamer
import com.onandor.peripheryapp.webcam.stream.StreamerType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.inject.Inject

data class CameraUiState(
    val previewAspectRatio: Float = 1f,
    val showControls: Boolean = false,
    val zoom: Float = CameraController.DEFAULT_ZOOM,
    val zoomRange: ClosedFloatingPointRange<Float> = zoom..zoom,
    val aeCompensation: Float = CameraController.DEFAULT_AE_COMPENSATION.toFloat(),
    val aeRange: ClosedFloatingPointRange<Float> = aeCompensation..aeCompensation,
    val aeCompensationEV: Float = CameraController.DEFAULT_AE_COMPENSATION.toFloat(),
    val currentCamera: CameraViewModel.CameraOption = CameraViewModel.CameraOption(),
    val cameras: List<CameraViewModel.CameraOption> = emptyList()
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val clientStreamer: ClientStreamer,
    private val settings: Settings,
    private val cameraController: CameraController,
    private val dcStreamer: DCStreamer
): ViewModel() {

    data class CameraOption(
        val id: String = "",
        val lensFacing: Int = -1
    )

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    private var camera: CameraInfo
    private val resolution: Size
    private val frameRateRange: Range<Int>
    private val bitRate: Int
    private var previewSurface: Surface? = null
    private val cameraInfos: List<CameraInfo> = cameraController.getCameraInfos()

    private val encoder: Encoder
    private val dcEncoder: DCEncoder

    init {
        val navArgs = navManager.getCurrentNavAction()?.navArgs as CameraNavArgs?
        if (navArgs != null) {
            camera = cameraInfos.first { it.id == navArgs.cameraId }
            resolution = camera.resolutions[navArgs.resolutionIdx]
            frameRateRange = camera.frameRateRanges[navArgs.frameRateRangeIdx]
            bitRate = navArgs.bitRate
        } else {
            camera = cameraInfos.first()
            resolution = camera.resolutions.first()
            frameRateRange = camera.frameRateRanges.first()
            bitRate = Encoder.DEFAULT_BIT_RATE
        }

        val compatibleCameras = cameraInfos
            .filter {
                it.frameRateRanges.contains(frameRateRange) && it.resolutions.contains(resolution)
            }.map {
                CameraOption(
                    id = it.id,
                    lensFacing = it.lensFacing
                )
            }

        _uiState.update {
            it.copy(
                previewAspectRatio = resolution.width.toFloat() / resolution.height.toFloat(),
                zoomRange = camera.zoomRange.lower..camera.zoomRange.upper,
                aeRange = camera.aeRange.lower.toFloat()..camera.aeRange.upper.toFloat(),
                currentCamera = compatibleCameras.first { option -> option.id == camera.id },
                cameras = compatibleCameras
            )
        }

        encoder = if (navArgs?.streamerType == StreamerType.DC) {
            Encoder(resolution.width, resolution.height, bitRate, frameRateRange.upper) {
                //dcStreamer.queueData(it)
            }
        } else {
            Encoder(resolution.width, resolution.height, bitRate, frameRateRange.upper) {
                clientStreamer.queueData(it)
            }
        }
        dcEncoder = DCEncoder(resolution.width, resolution.height, frameRateRange.upper) {
            dcStreamer.queueData(it)
        }

        viewModelScope.launch {
            dcStreamer.connectionEventFlow.collect { event ->
                when (event) {
                    DCStreamer.ConnectionEvent.CLIENT_DISCONNECTED -> {
                        navigateBack()
                    }
                    else -> { /* TODO */ }
                }
            }
        }
    }

    fun onPreviewSurfaceCreated(previewSurface: Surface) {
        if (this.previewSurface != null) {
            this.previewSurface = previewSurface
            cameraController.updateCaptureTargets(listOf(previewSurface, encoder.inputSurface!!))
            return
        }
        this.previewSurface = previewSurface
        cameraController.start(camera, frameRateRange, listOf(previewSurface, dcEncoder.inputSurface))
        encoder.start()
        clientStreamer.startStream()
    }

    fun onPause() {
        cameraController.updateCaptureTargets(listOf(encoder.inputSurface!!))
        this.previewSurface!!.release()
    }

    fun onShowControls() {
        _uiState.update { it.copy(showControls = true) }
    }

    fun onHideControls() {
        _uiState.update { it.copy(showControls = false) }
    }

    fun onZoomChanged(value: Float) {
        val newZoom = roundTo1Decimal(value)
        _uiState.update { it.copy(zoom = newZoom) }
        cameraController.zoom(newZoom)
    }

    fun onAeCompensationChanged(value: Float) {
        val newAeCompensation = roundTo1Decimal(value)
        _uiState.update {
            it.copy(
                aeCompensation = newAeCompensation,
                aeCompensationEV = roundTo2Decimals(newAeCompensation * 0.16666667f)
            )
        }
        cameraController.adjustExposure(newAeCompensation.toInt())
    }

    fun onCameraChanged(option: CameraOption) {
        camera = cameraInfos.first { it.id == option.id }
        _uiState.update {
            it.copy(
                zoom = CameraController.DEFAULT_ZOOM,
                zoomRange = camera.zoomRange.lower..camera.zoomRange.upper,
                aeCompensation = CameraController.DEFAULT_AE_COMPENSATION.toFloat(),
                aeCompensationEV = CameraController.DEFAULT_AE_COMPENSATION.toFloat(),
                aeRange = camera.aeRange.lower.toFloat()..camera.aeRange.upper.toFloat(),
                currentCamera = option
            )
        }
        encoder.flush()
        cameraController.reset()
        cameraController.start(
            camera, frameRateRange, listOf(previewSurface!!, encoder.inputSurface!!))
    }

    private fun roundTo1Decimal(value: Float): Float {
        val df = DecimalFormat("#.#")
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(value).toFloat()
    }

    private fun roundTo2Decimals(value: Float): Float {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(value).toFloat()
    }

    fun navigateBack() {
        dcStreamer.disconnect()
        clientStreamer.disconnect() // TODO
        encoder.release()
        cameraController.stop()
        navManager.navigateBack()
    }
}