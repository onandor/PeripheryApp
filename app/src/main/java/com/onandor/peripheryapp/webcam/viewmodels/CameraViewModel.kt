package com.onandor.peripheryapp.webcam.viewmodels

import android.content.Context
import android.media.MediaCodec
import android.provider.MediaStore.Audio.Media
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.VideoCapture
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import com.google.common.util.concurrent.ListenableFuture
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.webcam.stream.Encoder
import com.onandor.peripheryapp.webcam.stream.StreamVideoOutput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val navManager: INavigationManager
) : ViewModel() {

    private var cameraProvider: ProcessCameraProvider? = null
    private val streamVideoOutput = StreamVideoOutput()
    val videoCapture = VideoCapture.withOutput(streamVideoOutput)
    private var camera: Camera? = null
    private var mediaCodec: MediaCodec? = null
    private var encoder: Encoder? = null

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
        mediaCodec = streamVideoOutput.mediaCodec
        mediaCodec?.start()
        encoder = Encoder(mediaCodec!!)
        encoder?.start()
    }

    fun navigateBack() {
        encoder?.stop()
        mediaCodec?.stop()
        mediaCodec?.release()
        streamVideoOutput.release()
        navManager.navigateBack()
    }
}