package com.onandor.peripheryapp.navigation.navargs

data class CameraNavArgs(
    val cameraId: String,
    val resolutionIdx: Int,
    val frameRateRangeIdx: Int,
    val bitRate: Int,
    val streamerType: Int
) : NavArgs
