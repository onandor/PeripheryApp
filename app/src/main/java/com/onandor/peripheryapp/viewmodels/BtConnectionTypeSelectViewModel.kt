package com.onandor.peripheryapp.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.kbm.IBluetoothController
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BtConnectionTypeSelectViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val bluetoothController: IBluetoothController
) : ViewModel() {

    val bluetoothState = bluetoothController.bluetoothState

    init {
        bluetoothController.init()
    }

    fun navigateToBtDevices(){
        navManager.navigateTo(NavActions.btDevices())
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}