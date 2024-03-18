package com.onandor.peripheryapp.kbm.bluetooth.events

import android.bluetooth.BluetoothDevice

sealed interface BluetoothScanEvent {
    data class DeviceFound(val device: BluetoothDevice) : BluetoothScanEvent
    data class DeviceNameChanged(val device: BluetoothDevice) : BluetoothScanEvent
    data class DeviceBondStateChanged(val device: BluetoothDevice) : BluetoothScanEvent
}