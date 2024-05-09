package com.onandor.peripheryapp.webcam.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ColorSpace
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.ColorSpaceProfiles
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.webcam.stream.Encoder2
import com.onandor.peripheryapp.webcam.stream.Streamer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.RuntimeException
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class CameraViewModel2 @Inject constructor(
    private val navManager: INavigationManager,
    private val streamer: Streamer,
    private val settings: Settings
): ViewModel() {

    private class HandlerExecutor(_handler: Handler): Executor {
        private val handler = _handler

        override fun execute(command: Runnable) {
            if (!handler.post(command)) {
                throw RejectedExecutionException("" + handler + " is shutting down")
            }
        }
    }

    private var cameraManager: CameraManager? = null
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    private var captureSession: CameraCaptureSession? = null
    private var camera: CameraDevice? = null
    private var previewSurface: Surface? = null

    private val encoder: Encoder2 = Encoder2 { streamer.queueData(it) }

    fun onPreviewSurfaceCreated(previewSurface: Surface, cameraManager: CameraManager) {
        this.cameraManager = cameraManager
        this.previewSurface = previewSurface
        initializeCamera()
    }

    private fun initializeCamera() = viewModelScope.launch {
        camera = openCamera(cameraManager!!.cameraIdList[1], cameraHandler)

        val previewTargets = listOf(previewSurface!!, encoder.inputSurface!!)
        captureSession = createCaptureSession(camera!!, previewTargets, cameraHandler)

        val recordRequest = createRecordRequest()
        captureSession!!.setRepeatingRequest(recordRequest!!, null, cameraHandler)

        encoder.start()
        streamer.startStream()
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(cameraId: String, handler: Handler) = suspendCancellableCoroutine { continuation ->
        if (cameraManager == null) {
            return@suspendCancellableCoroutine
        }
        cameraManager!!.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = continuation.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                // TODO
            }

            override fun onError(device: CameraDevice, error: Int) {
                // TODO
            }

        }, handler)
    }

    private suspend fun createCaptureSession(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler
    ): CameraCaptureSession = suspendCoroutine { continuation ->
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = continuation.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val ex = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e("CameraViewModel", ex.message, ex)
                continuation.resumeWithException(ex)
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val outputConfigs = mutableListOf<OutputConfiguration>()
            for (target in targets) {
                println("valid: ${target.isValid}")
                val outputConfig = OutputConfiguration(target)
                outputConfig.dynamicRangeProfile = DynamicRangeProfiles.STANDARD
                outputConfigs.add(outputConfig)
            }

            val sessionConfig = SessionConfiguration(
                /* sessionType = */SessionConfiguration.SESSION_REGULAR,
                /* outputs = */ outputConfigs,
                /* executor = */ HandlerExecutor(handler),
                /* cb = */ stateCallback
            )
            device.createCaptureSession(sessionConfig)
        } else {
            device.createCaptureSession(targets, stateCallback, handler)
        }
    }

    private fun createPreviewRequest(): CaptureRequest? {
        if (previewSurface == null) {
            return null
        }
        return captureSession!!.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(previewSurface!!)
        }.build()
    }

    private fun createRecordRequest(): CaptureRequest? {
        if (previewSurface == null) {
            return null
        }
        return captureSession!!.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            addTarget(previewSurface!!)
            addTarget(encoder.inputSurface!!)
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(encoder.frameRate, encoder.frameRate))
        }.build()
    }

    fun navigateBack() {
        streamer.disconnect()
        encoder.release()
        cameraThread.quitSafely()
        navManager.navigateBack()
    }
}