package com.onandor.peripheryapp.kbm

import com.onandor.peripheryapp.kbm.data.BtDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IBluetoothController {

    val scannedDevices: StateFlow<List<BtDevice>>
    val pairedDevices: StateFlow<List<BtDevice>>
    val isBluetoothEnabled: StateFlow<Boolean>
    val isConnected: StateFlow<Boolean>
    val errors: SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()
    fun updatePairedDevices()

    fun startServer(): Flow<BtConnectionResult>
    fun connectToDevice(device: BtDevice): Flow<BtConnectionResult>
    fun closeConnection()

    fun release()
}