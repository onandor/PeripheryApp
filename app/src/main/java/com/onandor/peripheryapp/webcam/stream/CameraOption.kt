package com.onandor.peripheryapp.webcam.stream

import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.media.MediaRecorder
import android.util.Range
import android.util.Size

class CameraOption(
    val id: String,
    characteristics: CameraCharacteristics
) {

    val lensFacing: Int
    val zoomRange: Range<Float>
    val aeRange: Range<Int>
    val aeStep: Float
    val frameRateRanges: Array<Range<Int>>
    val resolutions: List<Size>
    val sensorSize: Rect
    val cropRegion: Rect = Rect()

    init {
        lensFacing = characteristics[CameraCharacteristics.LENS_FACING]!!
        zoomRange = Range(
            1f,
            characteristics[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM]!!
        )
        aeRange = characteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]!!
        aeStep = characteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP]!!.toFloat()
        frameRateRanges = characteristics[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]!!
        //characteristics[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES]
        sensorSize = characteristics[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!

        val cameraConfig = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!
        resolutions = cameraConfig.getOutputSizes(MediaRecorder::class.java)
            .filter { size -> size.width <= 1280 }
            .reversed()
    }
}