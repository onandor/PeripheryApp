package com.onandor.peripheryapp.webcam.viewmodels

import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.navargs.CameraNavArgs
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.webcam.stream.CameraController
import com.onandor.peripheryapp.webcam.stream.CameraInfo
import com.onandor.peripheryapp.webcam.stream.Encoder
import com.onandor.peripheryapp.webcam.stream.Streamer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val streamer: Streamer,
    private val settings: Settings,
    private val cameraController: CameraController
): ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    private val camera: CameraInfo
    private val resolution: Size
    private val frameRateRange: Range<Int>
    private val bitRate: Int
    private var previewSurface: Surface? = null

    private val encoder: Encoder

    init {
        val cameraInfos = cameraController.getCameraInfos()

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

        _uiState.update {
            it.copy(
                previewAspectRatio = resolution.width.toFloat() / resolution.height.toFloat(),
                zoomRange = camera.zoomRange.lower..camera.zoomRange.upper,
                aeRange = camera.aeRange.lower.toFloat()..camera.aeRange.upper.toFloat()
            )
        }

        encoder = Encoder(resolution.width, resolution.height, bitRate, frameRateRange.upper) {
            streamer.queueData(it)
        }
    }

    fun onPreviewSurfaceCreated(previewSurface: Surface) {
        if (this.previewSurface != null) {
            this.previewSurface = previewSurface
            cameraController.updateCaptureTargets(listOf(previewSurface, encoder.inputSurface!!))
            return
        }
        this.previewSurface = previewSurface
        cameraController.start(camera, frameRateRange, listOf(previewSurface, encoder.inputSurface!!))
        encoder.start()
        streamer.startStream()
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
        streamer.disconnect()
        encoder.release()
        cameraController.stop()
        navManager.navigateBack()
    }
}