package com.onandor.peripheryapp.webcam.stream

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CameraController @Inject constructor(context: Context) {

    private class HandlerExecutor(private val handler: Handler): Executor {

        override fun execute(command: Runnable) {
            if (!handler.post(command)) {
                throw RejectedExecutionException("" + handler + " is shutting down")
            }
        }
    }

    private val mCameraManager: CameraManager
    private var mCameraThread: HandlerThread? = null
    private var mCameraHandler: Handler? = null
    private var mCamera: CameraDevice? = null
    private var mCaptureSession: CameraCaptureSession? = null

    private val mCaptureTargets: MutableList<Surface> = mutableListOf()
    private val mCameraOptions: MutableList<CameraOption> = mutableListOf()

    init {
        mCameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val cameraIdList = try {
            mCameraManager.cameraIdList
        } catch (e: CameraAccessException) {
            emptyArray<String>()
            // TODO: error handling
        }
        cameraIdList.forEach { id ->
            val cameraChars = mCameraManager.getCameraCharacteristics(id)
            mCameraOptions.add(CameraOption(id, cameraChars))
        }
    }

    fun start(cameraOption: CameraOption, frameRateIdx: Int) = CoroutineScope(Dispatchers.IO).launch {
        mCameraThread = HandlerThread("CameraThread").apply { start() }
        mCameraHandler = Handler(mCameraThread!!.looper)

        if (mCaptureTargets.isEmpty()) {
            return@launch
        }
        mCamera = openCamera(cameraOption.id)
        // TODO: handle error
        mCaptureSession = createCaptureSession()

        val captureRequest = createCaptureRequest(cameraOption.frameRateRanges[frameRateIdx])
        mCaptureSession!!.setRepeatingRequest(captureRequest, null, mCameraHandler)
    }

    fun stop() {
        mCaptureTargets.clear()

        mCaptureSession?.stopRepeating()
        mCaptureSession?.close()
        mCaptureSession = null

        mCamera?.close()
        mCamera = null

        mCameraThread?.quitSafely()
        mCameraThread = null
        mCameraHandler?.looper?.quitSafely()
        mCameraHandler = null

    }

    fun addCaptureTargets(targets: List<Surface>) {
        mCaptureTargets.addAll(targets)
    }

    fun getCameraOptions(): List<CameraOption> {
        return mCameraOptions.toList()
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(cameraId: String) = suspendCancellableCoroutine { continuation ->
        mCameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = continuation.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                // TODO: signal that camera has been disconnected
            }

            override fun onError(device: CameraDevice, error: Int) {
                // TODO
                val exception = RuntimeException("Camera $cameraId error: $error")
                if (continuation.isActive) {
                    continuation.resumeWithException(exception)
                }
            }

        }, mCameraHandler)
    }

    private suspend fun createCaptureSession(): CameraCaptureSession = suspendCoroutine { continuation ->
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = continuation.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exception = RuntimeException("Camera ${mCamera?.id} session configuration failed")
                Log.e("CameraViewModel", exception.message, exception)
                continuation.resumeWithException(exception)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val outputConfigs = mutableListOf<OutputConfiguration>()
            for (surface in mCaptureTargets) {
                val outputConfig = OutputConfiguration(surface)
                outputConfig.dynamicRangeProfile = DynamicRangeProfiles.STANDARD
                outputConfigs.add(outputConfig)
            }

            val sessionConfig = SessionConfiguration(
                /* sessionType = */ SessionConfiguration.SESSION_REGULAR,
                /* outputs = */ outputConfigs,
                /* executor = */ HandlerExecutor(mCameraHandler!!),
                /* cb = */ stateCallback
            )
            mCamera!!.createCaptureSession(sessionConfig)
        } else {
            mCamera!!.createCaptureSession(mCaptureTargets, stateCallback, mCameraHandler)
        }
    }

    private fun createCaptureRequest(frameRateRange: Range<Int>): CaptureRequest {
        return mCaptureSession!!.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            mCaptureTargets.forEach { surface ->
                addTarget(surface)
            }
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, frameRateRange)
        }.build()
    }
}