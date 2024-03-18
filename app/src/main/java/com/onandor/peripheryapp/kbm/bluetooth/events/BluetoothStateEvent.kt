package com.onandor.peripheryapp.kbm.bluetooth.events

sealed interface BluetoothStateEvent {

    data class StateChanged(val state: Int) : BluetoothStateEvent
    data class ScanModeChanged(val scanMode: Int) : BluetoothStateEvent
}