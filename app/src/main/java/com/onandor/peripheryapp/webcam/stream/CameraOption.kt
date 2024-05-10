package com.onandor.peripheryapp.webcam.stream

import android.hardware.camera2.CameraCharacteristics
import android.util.Range

class CameraOption(
    val id: String,
    characteristics: CameraCharacteristics
) {

    val lensFacing: Int
    val zoomRange: Range<Float>
    val aeRange: Range<Int>
    val aeStep: Float
    val frameRateRanges: Array<Range<Int>>

    init {
        lensFacing = characteristics[CameraCharacteristics.LENS_FACING]!!
        zoomRange = Range(
            0f,
            characteristics[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM]!!
        )
        aeRange = characteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]!!
        aeStep = characteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP]!!.toFloat()
        frameRateRanges = characteristics[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]!!
        //characteristics[CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES]
    }
}