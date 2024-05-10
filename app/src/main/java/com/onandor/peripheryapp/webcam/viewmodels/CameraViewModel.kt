package com.onandor.peripheryapp.webcam.viewmodels

import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.navargs.CameraNavArgs
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.webcam.stream.CameraController
import com.onandor.peripheryapp.webcam.stream.CameraOption
import com.onandor.peripheryapp.webcam.stream.Encoder
import com.onandor.peripheryapp.webcam.stream.Streamer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class CameraUiState(
    val width: Int = 640,
    val height: Int = 480
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

    private val camera: CameraOption
    private val resolution: Size
    private val frameRateRange: Range<Int>

    private var previewSurface: Surface? = null

    private val encoder: Encoder

    init {
        val cameraOptions = cameraController.getCameraOptions()

        val navArgs = navManager.getCurrentNavAction()?.navArgs as CameraNavArgs?
        if (navArgs != null) {
            camera = cameraOptions.first { it.id == navArgs.cameraId }
            resolution = camera.resolutions[navArgs.resolutionIdx]
            frameRateRange = camera.frameRateRanges[navArgs.frameRateRangeIdx]
        } else {
            camera = cameraOptions.first()
            resolution = camera.resolutions.first()
            frameRateRange = camera.frameRateRanges.first()
        }

        _uiState.update { it.copy(width = resolution.width, height = resolution.height) }

        encoder = Encoder(resolution.width, resolution.height, 2500, frameRateRange.upper) {
            streamer.queueData(it)
        }
    }

    fun onPreviewSurfaceCreated(previewSurface: Surface) {
        this.previewSurface = previewSurface
        cameraController.addCaptureTargets(listOf(previewSurface, encoder.inputSurface!!))
        cameraController.start(camera, frameRateRange)
        encoder.start()
        streamer.startStream()
    }

    fun navigateBack() {
        streamer.disconnect()
        encoder.release()
        cameraController.stop()
        navManager.navigateBack()
    }
}