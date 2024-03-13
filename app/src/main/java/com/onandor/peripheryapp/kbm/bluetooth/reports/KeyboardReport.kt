package com.onandor.peripheryapp.kbm.bluetooth.reports

import java.util.Arrays

class KeyboardReport {

    private val keyboardData: ByteArray = "M0ABCDEF".toByteArray()

    init {
        Arrays.fill(keyboardData, 0.toByte())
    }

    fun setValue(
        modifier: Int,
        key1: Int,
        key2: Int,
        key3: Int,
        key4: Int,
        key5: Int,
        key6: Int
    ): ByteArray {
        keyboardData[0] = modifier.toByte()
        keyboardData[1] = 0.toByte()
        keyboardData[2] = key1.toByte()
        keyboardData[3] = key2.toByte()
        keyboardData[4] = key3.toByte()
        keyboardData[5] = key4.toByte()
        keyboardData[6] = key5.toByte()
        keyboardData[7] = key6.toByte()
        return keyboardData
    }

    fun getValue(): ByteArray = keyboardData
}