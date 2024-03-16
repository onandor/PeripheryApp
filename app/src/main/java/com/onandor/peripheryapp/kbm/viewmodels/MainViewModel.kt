package com.onandor.peripheryapp.kbm.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val navManager: INavigationManager
) : ViewModel() {

    fun navigateToBtKbmScreen() {
        navManager.navigateTo(NavActions.btDevices())
    }

    fun navigateToWifiWebcamScreen() {
        // TODO
    }
}