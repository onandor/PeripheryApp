package com.onandor.peripheryapp.kbm

import com.onandor.peripheryapp.kbm.data.BtDevice
import kotlinx.coroutines.flow.StateFlow

interface IBluetoothController {

    val scannedDevices: StateFlow<List<BtDevice>>
    val pairedDevices: StateFlow<List<BtDevice>>
    val isBluetoothEnabled: StateFlow<Boolean>

    fun startDiscovery()
    fun stopDiscovery()
    fun updatePairedDevices()

    fun release()
}