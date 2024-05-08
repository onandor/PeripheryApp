package com.onandor.peripheryapp.webcam.viewmodels

import android.content.Context
import android.util.Range
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.VideoCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.utils.WebcamSettingKeys
import com.onandor.peripheryapp.webcam.stream.CameraSelection
import com.onandor.peripheryapp.webcam.stream.Encoder
import com.onandor.peripheryapp.webcam.stream.StreamVideoOutput
import com.onandor.peripheryapp.webcam.stream.Streamer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val streamer: Streamer,
    private val settings: Settings
) : ViewModel() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var streamVideoOutput: StreamVideoOutput? = null
    private var camera: Camera? = null
    private var encoder: Encoder? = null
    var videoCapture: VideoCapture<StreamVideoOutput>? = null

    private var resolutionIdx: Int = StreamVideoOutput.Resolutions.LOW
    private var frameRate: Int = StreamVideoOutput.FrameRates.LOW
    private var cameraSelection: CameraSelection = CameraSelection.FRONT

    init {
        viewModelScope.launch {
            resolutionIdx = settings.get(WebcamSettingKeys.RESOLUTION)
            frameRate = settings.get(WebcamSettingKeys.FRAME_RATE)
            cameraSelection = CameraSelection.fromInt(settings.get(WebcamSettingKeys.CAMERA))

            streamVideoOutput = StreamVideoOutput(resolutionIdx, frameRate)
            videoCapture = VideoCapture.Builder(streamVideoOutput!!)
                .setTargetFrameRate(Range(frameRate, frameRate))
                .build()

            streamer.connectionEventFlow.collect {
                if (it != Streamer.ConnectionEvent.CONNECTION_SUCCESS) {
                    navigateBack()
                }
            }
        }
    }

    fun getCameraProvider(context: Context): ProcessCameraProvider {
        if (cameraProvider != null) {
            return cameraProvider!!
        }
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
        return cameraProvider!!
    }

    fun onToggleCamera() {
        /*
        controller?.cameraSelector =
            if (controller?.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
         */
    }

    fun onCameraGot(camera: Camera) {
        if (this.camera != null) {
            return
        }
        this.camera = camera
        encoder = Encoder(streamVideoOutput?.mediaCodec!!) { streamer.queueData(it) }
        streamer.startStream()
    }

    fun navigateBack() {
        streamer.disconnect()
        encoder?.release()
        streamVideoOutput?.release()
        navManager.navigateBack()
    }
}