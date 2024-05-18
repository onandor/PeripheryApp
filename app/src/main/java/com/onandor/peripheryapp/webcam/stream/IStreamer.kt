package com.onandor.peripheryapp.webcam.stream

import kotlinx.coroutines.flow.SharedFlow

interface IStreamer {

    val eventFlow: SharedFlow<StreamerEvent>

    fun queueFrame(frame: ByteArray)
    fun start()
    fun stop()
}