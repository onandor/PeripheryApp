package com.onandor.peripheryapp.webcam.viewmodels

import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.NavActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class NewConnectionUiState(
    val isCameraPermissionGranted: Boolean = false
)

@HiltViewModel
class NewConnectionViewModel @Inject constructor(
    private val navManager: INavigationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewConnectionUiState())
    val uiState = _uiState.asStateFlow()

    fun onCameraPermissionGranted() {
        _uiState.update { it.copy(isCameraPermissionGranted = true) }
    }

    fun navigateBack() {
        navManager.navigateBack()
    }

    fun navigateToCamera() {
        navManager.navigateTo(NavActions.Webcam.camera())
    }
}