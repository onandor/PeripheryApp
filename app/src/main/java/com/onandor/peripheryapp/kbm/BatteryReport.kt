package com.onandor.peripheryapp.kbm

import kotlin.math.ceil

class BatteryReport {

    private val batteryData: ByteArray = ByteArray(1)

    fun setValue(batteryLevel: Float): ByteArray {
        val value = ceil(batteryLevel * 255).toInt()
        batteryData[0] = (value and 0xFF).toByte()
        return batteryData
    }

    fun getReport(): ByteArray = batteryData

    interface BatteryDataSender {

        fun sendBatteryLevel(batteryLevel: Float)
    }
}