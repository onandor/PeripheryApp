package com.onandor.peripheryapp.webcam.video.encoders

import android.view.Surface

interface Encoder {

    val inputSurface: Surface
    fun start()
    fun flush()
    fun release()
}