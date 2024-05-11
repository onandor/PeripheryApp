package com.onandor.peripheryapp.webcam.stream

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureRequest.Builder
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

class CameraController @Inject constructor(private val context: Context) {

    private class HandlerExecutor(private val handler: Handler): Executor {

        override fun execute(command: Runnable) {
            if (!handler.post(command)) {
                throw RejectedExecutionException("" + handler + " is shutting down")
            }
        }
    }

    companion object {
        private const val DEFAULT_ZOOM = 1f
        private const val DEFAULT_AE_COMPENSATION = 0
    }

    private val mCameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var mCameraThread: HandlerThread? = null
    private var mCameraHandler: Handler? = null
    private var mCamera: CameraDevice? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var mCameraInfo: CameraInfo? = null
    private var mFrameRateRange: Range<Int> = Range(15, 15)

    private val mCaptureTargets: MutableList<Surface> = mutableListOf()
    private val mCameraInfos: MutableList<CameraInfo> = mutableListOf()

    private var mZoom: Float = DEFAULT_ZOOM
    private var mAeCompensation: Int = DEFAULT_AE_COMPENSATION

    init {
        val cameraIdList = try {
            mCameraManager.cameraIdList
        } catch (e: CameraAccessException) {
            emptyArray<String>()
            // TODO: error handling
        }
        cameraIdList.forEach { id ->
            val cameraChars = mCameraManager.getCameraCharacteristics(id)
            mCameraInfos.add(CameraInfo(id, cameraChars))
        }
    }

    fun start(cameraInfo: CameraInfo, frameRateRange: Range<Int>, targets: List<Surface>) = CoroutineScope(Dispatchers.IO).launch {
        if (mCaptureSession != null || targets.isEmpty()) {
            return@launch
        }
        mCameraThread = HandlerThread("CameraThread").apply { start() }
        mCameraHandler = Handler(mCameraThread!!.looper)
        mCameraInfo = cameraInfo
        mFrameRateRange = frameRateRange
        mCaptureTargets.addAll(targets)

        mCamera = openCamera()
        mCaptureSession = createCaptureSession() // TODO: handle error

        val crb = createBuilder()
        mCaptureSession!!.setRepeatingRequest(crb.build(), null, mCameraHandler)

        startNotificationService()
    }

    fun stop() {
        stopNotificationService()
        mCaptureTargets.clear()

        mZoom = DEFAULT_ZOOM
        mAeCompensation = DEFAULT_AE_COMPENSATION

        mCaptureSession?.stopRepeating()
        mCaptureSession?.close()
        mCaptureSession = null

        mCamera?.close()
        mCamera = null

        mCameraInfo = null

        mCameraThread?.quitSafely()
        mCameraThread = null
        mCameraHandler?.looper?.quitSafely()
        mCameraHandler = null
    }

    fun updateCaptureTargets(targets: List<Surface>) = CoroutineScope(Dispatchers.IO).launch {
        if (mCaptureSession == null || targets.isEmpty()) {
            return@launch
        }
        mCaptureTargets.clear()
        mCaptureTargets.addAll(targets)

        mCaptureSession?.stopRepeating()
        mCaptureSession?.close()
        mCaptureSession = null

        mCaptureSession = createCaptureSession() // TODO: handle error

        val builder = createBuilder()
        if (mZoom != DEFAULT_ZOOM) {
            mCameraInfo!!.sensorSize.set(Rect())
            builder.setZoom(mZoom)
        }
        if (mAeCompensation != DEFAULT_AE_COMPENSATION) {
            builder.setAeCompensation(mAeCompensation)
        }
        mCaptureSession!!.setRepeatingRequest(builder.build(), null, mCameraHandler)
    }

    fun getCameraInfos(): List<CameraInfo> {
        return mCameraInfos.toList()
    }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera() = suspendCancellableCoroutine { continuation ->
        mCameraManager.openCamera(mCameraInfo!!.id, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = continuation.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                // TODO: signal that camera has been disconnected
            }

            override fun onError(device: CameraDevice, error: Int) {
                // TODO
                val exception = RuntimeException("Camera ${mCameraInfo!!.id} error: $error")
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

    private fun createBuilder(): Builder {
        return mCaptureSession!!.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
            mCaptureTargets.forEach { surface ->
                addTarget(surface)
            }
            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mFrameRateRange)
        }
    }

    private fun startNotificationService() {
        val intent = WebcamNotificationService.buildIntent()
            .setClass(context, WebcamNotificationService::class.java)
        context.startForegroundService(intent)
    }

    private fun stopNotificationService() {
        val intent = Intent().setClass(context, WebcamNotificationService::class.java)
        context.stopService(intent)
    }

    fun zoom(value: Float) {
        mZoom = mCameraInfo!!.zoomRange.clamp(value)
        val zoomRequest = createBuilder().setZoom(value).build()
        mCaptureSession!!.setRepeatingRequest(zoomRequest, null, mCameraHandler)
    }

    fun adjustExposure(value: Int) {
        mAeCompensation = mCameraInfo!!.aeRange.clamp(value)
        val aeCompRequest = createBuilder().setAeCompensation(mAeCompensation).build()
        mCaptureSession!!.setRepeatingRequest(aeCompRequest, null, mCameraHandler)
    }

    private fun Builder.setZoom(zoom: Float): Builder {
        return this.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                set(CaptureRequest.CONTROL_ZOOM_RATIO, zoom)
            } else {
                val centerX = mCameraInfo!!.sensorSize.width() / 2
                val centerY = mCameraInfo!!.sensorSize.height() / 2
                val deltaX = (0.5f * mCameraInfo!!.sensorSize.width() / zoom).toInt()
                val deltaY = (0.5f * mCameraInfo!!.sensorSize.height() / zoom).toInt()

                mCameraInfo!!.cropRegion.set(
                    /* left = */ centerX - deltaX,
                    /* top = */ centerY - deltaY,
                    /* right = */ centerX + deltaX,
                    /* bottom = */ centerY + deltaY
                )
                set(CaptureRequest.SCALER_CROP_REGION, mCameraInfo!!.cropRegion)
            }
        }
    }

    private fun Builder.setAeCompensation(aeCompensation: Int): Builder {
        return this.apply {
            set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, aeCompensation)
        }
    }
}