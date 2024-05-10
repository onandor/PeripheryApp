package com.onandor.peripheryapp.webcam.viewmodels

import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.navargs.CameraNavArgs
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.webcam.stream.CameraController
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

    private val cameraId: String
    private val resolution: Size
    private val frameRate: Range<Int>

    private var previewSurface: Surface? = null

    private val encoder: Encoder = Encoder(640, 480, 2500, 15) { streamer.queueData(it) }

    init {
        val cameraOptions = cameraController.getCameraOptions()
        val navArgs = navManager.getCurrentNavAction()?.navArgs as CameraNavArgs?
        if (navArgs != null) {
            cameraId = navArgs.cameraId
            resolution = cameraOptions.first { it.id == cameraId }
                .resolutions[navArgs.resolutionIdx]
            frameRate = cameraOptions.first { it.id == cameraId }
                .frameRateRanges[navArgs.frameRateIdx]
        } else {
            cameraId = cameraOptions.first().id
            resolution = cameraOptions.first().resolutions.first()
            frameRate = cameraOptions.first().frameRateRanges.first()
        }
    }

    fun onPreviewSurfaceCreated(previewSurface: Surface) {
        this.previewSurface = previewSurface
        val cameraOptions = cameraController.getCameraOptions()
        cameraController.addCaptureTargets(listOf(previewSurface, encoder.inputSurface!!))
        cameraController.start(cameraOptions[1], 0)
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