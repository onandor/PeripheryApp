package com.onandor.peripheryapp.kbm

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.StateFlow

interface IBluetoothController {

    val foundDevices: StateFlow<List<BluetoothDevice>>
    val bondedDevices: StateFlow<List<BluetoothDevice>>
    val bluetoothState: StateFlow<Int>
    val waitingForDeviceBonding: StateFlow<BluetoothDevice?>
    val waitingForDeviceConnecting: StateFlow<BluetoothDevice?>
    val connectedDevice: StateFlow<BluetoothDevice?>

    fun startDiscovery(clearFoundDevices: Boolean)
    fun stopDiscovery()
    fun updateBondedDevices()
    fun init()
    fun release()
    fun pair(device: BluetoothDevice)
    fun unpair(device: BluetoothDevice)
    fun connect(device: BluetoothDevice)
    fun disconnect(device: BluetoothDevice)
}