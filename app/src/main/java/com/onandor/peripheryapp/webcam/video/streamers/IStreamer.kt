package com.onandor.peripheryapp.webcam.video.streamers

import kotlinx.coroutines.flow.SharedFlow

interface IStreamer {

    val eventFlow: SharedFlow<StreamerEvent>

    fun queueFrame(frame: ByteArray)
    fun start()
    fun stop()
}