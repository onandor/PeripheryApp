package com.onandor.peripheryapp.kbm.bluetooth.events

sealed interface BatteryEvent {

    data class LevelChanged(val level: Float) : BatteryEvent
}