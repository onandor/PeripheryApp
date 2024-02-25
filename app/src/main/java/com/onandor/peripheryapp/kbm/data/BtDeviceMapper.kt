package com.onandor.peripheryapp.kbm.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBtDevice(): BtDevice {
    return BtDevice(
        name = name,
        address = address
    )
}