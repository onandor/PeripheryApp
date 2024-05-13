package com.onandor.peripheryapp.navigation.navargs

import com.onandor.peripheryapp.webcam.stream.StreamerType

data class CameraNavArgs(
    val cameraId: String,
    val resolutionIdx: Int,
    val frameRateRangeIdx: Int,
    val bitRate: Int,
    val streamerType: StreamerType
) : NavArgs
