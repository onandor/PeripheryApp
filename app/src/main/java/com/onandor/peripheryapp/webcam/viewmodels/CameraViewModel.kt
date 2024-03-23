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
    private var mediaCodec: MediaCodec? = null
    private var encoder: Encoder? = null
    private val streamer = Streamer()

    private val mediaCodecCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            println("onInputBufferAvaiable")
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            val data = encoder?.encode(codec, index, info)
            data?.let { streamer.queueData(it) }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            println("onError")
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            println("onOutputFormatChanged")
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
        this.camera = camera
        mediaCodec = streamVideoOutput.mediaCodec
        mediaCodec?.setCallback(mediaCodecCallback)
        mediaCodec?.start()
        encoder = Encoder(mediaCodec!!) { onDataEncoded(it) }
        streamer.startStream("192.168.0.47", 7220)
        //encoder?.start()
    }

    private fun onDataEncoded(data: ByteArray) {
        streamer.queueData(data)
    }

    fun navigateBack() {
        //encoder?.stop()
        streamer.stopStream()
        mediaCodec?.stop()
        mediaCodec?.release()
        streamVideoOutput.release()
        navManager.navigateBack()
    }
}