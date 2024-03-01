package com.onandor.peripheryapp.kbm

import java.util.Arrays

class MouseReport {

    private val mouseData: ByteArray = "BXYW".toByteArray()

    init {
        Arrays.fill(mouseData, 0.toByte())
    }

    fun setValue(
        left: Boolean,
        right: Boolean,
        middle: Boolean,
        x: Int,
        y: Int,
        wheel: Int
    ): ByteArray {
        val buttons: Int = (if (left) 1 else 0) or (if (right) 2 else 0) or if (middle) 4 else 0
        mouseData[0] = buttons.toByte()
        mouseData[1] = x.toByte()
        mouseData[2] = y.toByte()
        mouseData[3] = wheel.toByte()
        return mouseData
    }

    fun getReport(): ByteArray = mouseData

    interface MouseDataSender {

        fun sendMouse(left: Boolean, right: Boolean, middle: Boolean, dX: Int, dY: Int, dWheel: Int)
    }
}