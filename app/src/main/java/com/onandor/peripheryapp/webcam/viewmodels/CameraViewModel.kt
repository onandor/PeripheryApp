package com.onandor.peripheryapp.webcam.viewmodels

import android.content.Context
import android.media.MediaCodec
import android.media.MediaFormat
import androidx.camera.core.Camera
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.VideoCapture
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.webcam.stream.Encoder
import com.onandor.peripheryapp.webcam.stream.StreamVideoOutput
import com.onandor.peripheryapp.webcam.stream.Streamer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val navManager: INavigationManager
) : ViewModel() {

    private var cameraProvider: ProcessCameraProvider? = null
    private val streamVideoOutput = StreamVideoOutput()
    val videoCapture = VideoCapture.withOutput(streamVideoOutput)
    private var camera: Camera? = null
    private var encoder: Encoder? = null
    private val streamer = Streamer()

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
        this.camera = camera
        encoder = Encoder(streamVideoOutput.mediaCodec!!) { streamer.queueData(it) }
        streamer.startStream("192.168.0.47", 7220)
    }

    fun navigateBack() {
        streamer.stopStream()
        encoder?.release()
        streamVideoOutput.release()
        navManager.navigateBack()
    }
}