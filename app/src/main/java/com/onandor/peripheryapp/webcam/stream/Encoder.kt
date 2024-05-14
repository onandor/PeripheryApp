package com.onandor.peripheryapp.webcam.stream

import android.view.Surface

interface Encoder {

    val inputSurface: Surface
    fun start()
    fun flush()
    fun release()
}