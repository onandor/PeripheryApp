package com.onandor.peripheryapp.webcam

import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.utils.IntSettingOption
import com.onandor.peripheryapp.webcam.stream.CameraSelection
import com.onandor.peripheryapp.webcam.stream.Encoder
import com.onandor.peripheryapp.webcam.stream.StreamVideoOutput

class SettingOptions {

    companion object {

        val resolution = listOf(
            IntSettingOption(
                value = StreamVideoOutput.Resolutions.LOW,
                resourceId = R.string.webcam_settings_resolution_low
            ),
            IntSettingOption(
                value = StreamVideoOutput.Resolutions.HIGH,
                resourceId = R.string.webcam_settings_resolution_high
            )
        )

        val RESOLUTION_DEFAULT = resolution[0]

        val frameRate = listOf(
            IntSettingOption(
                value = StreamVideoOutput.FrameRates.LOW,
                resourceId = R.string.webcam_settings_frame_rate_low
            ),
            IntSettingOption(
                value = StreamVideoOutput.FrameRates.HIGH,
                resourceId = R.string.webcam_settings_frame_rate_high
            )
        )

        val FRAME_RATE_DEFAULT = frameRate[0]
        
        val cameraSelection = listOf(
            IntSettingOption(
                value = CameraSelection.FRONT.value,
                resourceId = R.string.webcam_settings_camera_front
            ),
            IntSettingOption(
                value = CameraSelection.BACK.value,
                resourceId = R.string.webcam_settings_camera_back
            )
        )

        val CAMERA_SELECTION_DEFAULT = cameraSelection[0]
    }
}