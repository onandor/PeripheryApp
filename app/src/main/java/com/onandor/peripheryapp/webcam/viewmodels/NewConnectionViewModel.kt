package com.onandor.peripheryapp.webcam.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NewConnectionViewModel @Inject constructor(
    private val navManager: INavigationManager
) : ViewModel() {

    fun navigateBack() {
        navManager.navigateBack()
    }

    fun navigateToCamera() {
        navManager.navigateTo(NavActions.Webcam.camera())
    }
}