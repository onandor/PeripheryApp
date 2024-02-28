package com.onandor.peripheryapp.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewmodel @Inject constructor(
    private val navManager: INavigationManager
) : ViewModel() {

    fun navigateToBtKbmScreen() {
        navManager.navigateTo(NavActions.bondedBtDevices())
    }

    fun navigateToWifiWebcamScreen() {
        // TODO
    }
}