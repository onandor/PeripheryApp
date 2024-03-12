package com.onandor.peripheryapp.kbm.viewmodels

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BtConnectionTypeSelectViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val navManager: INavigationManager
) : ViewModel() {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _bluetoothState = MutableStateFlow(
        bluetoothAdapter?.state ?: BluetoothAdapter.STATE_OFF
    )
    val bluetoothState = _bluetoothState.asStateFlow()

    private val bluetoothStateReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _bluetoothState.update { state }
                }
            }
        }
    }

    fun navigateToBtDevices(){
        context.unregisterReceiver(bluetoothStateReceiver)
        navManager.navigateTo(NavActions.btDevices())
    }

    fun permissionsGranted() {
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        )
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    override fun onCleared() {
        super.onCleared()
        context.unregisterReceiver(bluetoothStateReceiver)
    }
}