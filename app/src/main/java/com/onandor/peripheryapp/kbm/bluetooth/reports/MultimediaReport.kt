package com.onandor.peripheryapp.kbm.bluetooth.reports

import java.util.Arrays

class MultimediaReport {

    private val multimediaData: ByteArray = "B0".toByteArray()

    init {
        Arrays.fill(multimediaData, 0.toByte())
    }

    fun setValue(key: Int): ByteArray {
        multimediaData[0] = key.toByte()
        return multimediaData
    }

    fun getValue(): ByteArray = multimediaData
}