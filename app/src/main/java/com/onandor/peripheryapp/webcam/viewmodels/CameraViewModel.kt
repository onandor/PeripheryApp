package com.onandor.peripheryapp.webcam.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val navManager: INavigationManager
) : ViewModel() {

    fun navigateBack() {
        navManager.navigateBack()
    }
}