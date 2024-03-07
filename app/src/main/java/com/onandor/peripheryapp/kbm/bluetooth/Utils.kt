package com.onandor.peripheryapp.kbm.bluetooth

import android.bluetooth.BluetoothDevice
import android.util.Log
import java.lang.Exception
import java.lang.reflect.Method

class BluetoothUtils {

    companion object {

        private val TAG = "BluetoothUtils"

        fun cancelBondProcess(device: BluetoothDevice): Boolean {
            val cancelBondProcessMethod: Method = try {
                BluetoothDevice::class.java.getMethod("cancelBondProcess")
            } catch (_: Exception) {
                Log.e(TAG, "Error getting method 'cancelBondProcess'")
                null
            } ?: return false
            return try {
                cancelBondProcessMethod.invoke(device) as Boolean
            } catch (_: Exception) {
                Log.e(TAG, "Error invoking method 'cancelBondProcess'")
                false
            }
        }

        fun removeBond(device: BluetoothDevice): Boolean {
            val removeBondMethod: Method = try {
                BluetoothDevice::class.java.getMethod("removeBond")
            } catch (_: Exception) {
                Log.e(TAG, "Error getting method 'removeBond'")
                null
            } ?: return false
            return try {
                removeBondMethod.invoke(device) as Boolean
            } catch (_: Exception) {
                Log.e(TAG, "Error invoking method 'removeBond'")
                false
            }
        }
    }
}