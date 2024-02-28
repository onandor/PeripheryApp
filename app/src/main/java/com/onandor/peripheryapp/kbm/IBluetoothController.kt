package com.onandor.peripheryapp.kbm

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow

interface IBluetoothController {

    val foundDevices: StateFlow<List<BluetoothDevice>>
    val bondedDevices: StateFlow<List<BluetoothDevice>>
    val isBluetoothEnabled: StateFlow<Boolean>

    fun startDiscovery()
    fun stopDiscovery()
    fun updateBondedDevices()
    fun init()
    fun release()
}