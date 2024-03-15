package com.onandor.peripheryapp.kbm.bluetooth.reports

import java.util.Arrays

class KeyboardReport {

    enum class ReportMode(val value: Int) {
        KEY_6(0),
        KEY_5(1);

        companion object {

            fun fromInt(value: Int) = entries.toList().first { it.value == value }
        }
    }

    private val keyboardData5: ByteArray = "M0ABCDE".toByteArray()
    private val keyboardData6: ByteArray = "M0ABCDEF".toByteArray()
    private var keyboardData: ByteArray = keyboardData6

    init {
        Arrays.fill(keyboardData5, 0.toByte())
        Arrays.fill(keyboardData6, 0.toByte())
        Arrays.fill(keyboardData, 0.toByte())
    }

    fun changeMode(mode: ReportMode) {
        keyboardData = when (mode) {
            ReportMode.KEY_6 -> keyboardData6
            ReportMode.KEY_5 -> keyboardData5
        }
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
        if (keyboardData.size == 8) {
            keyboardData[7] = key6.toByte()
        }
        return keyboardData
    }

    fun getValue(): ByteArray = keyboardData
}