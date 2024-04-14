package com.onandor.peripheryapp.webcam.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.utils.IntSettingOption
import com.onandor.peripheryapp.utils.Settings
import com.onandor.peripheryapp.utils.WebcamSettingKeys
import com.onandor.peripheryapp.webcam.SettingOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WebcamSettingsUiState(
    val resolution: IntSettingOption = SettingOptions.RESOLUTION_DEFAULT,
    val frameRate: IntSettingOption = SettingOptions.FRAME_RATE_DEFAULT,
    val cameraSelection: IntSettingOption = SettingOptions.CAMERA_SELECTION_DEFAULT
)

@HiltViewModel
class WebcamSettingsViewModel @Inject constructor(
    private val settings: Settings,
    private val navManager: INavigationManager
): ViewModel() {

    private val resolutionFlow = settings
        .observe(WebcamSettingKeys.RESOLUTION)
        .map { resolution ->
            SettingOptions
                .resolution
                .find { option -> option.value == resolution }
                ?: SettingOptions.RESOLUTION_DEFAULT
        }
    private val frameRateFlow = settings
        .observe(WebcamSettingKeys.FRAME_RATE)
        .map { frameRate ->
            SettingOptions
                .frameRate
                .find { option -> option.value == frameRate }
                ?: SettingOptions.FRAME_RATE_DEFAULT
        }
    private val cameraSelection = settings
        .observe(WebcamSettingKeys.CAMERA)
        .map { cameraSelection ->
            SettingOptions
                .cameraSelection
                .find { option -> option.value == cameraSelection }
                ?: SettingOptions.CAMERA_SELECTION_DEFAULT
        }

    val uiState = combine(
        resolutionFlow, frameRateFlow, cameraSelection
    ) { resolution, frameRate, cameraSelection ->
        WebcamSettingsUiState(
            resolution = resolution,
            frameRate = frameRate,
            cameraSelection = cameraSelection
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            WebcamSettingsUiState()
        )

    fun onResolutionChanged(resolution: Int) {
        viewModelScope.launch {
            settings.save(WebcamSettingKeys.RESOLUTION, resolution)
        }
    }

    fun onFrameRateChanged(frameRate: Int) {
        viewModelScope.launch {
            settings.save(WebcamSettingKeys.FRAME_RATE, frameRate)
        }
    }

    fun onCameraSelectionChanged(cameraSelection: Int) {
        viewModelScope.launch {
            settings.save(WebcamSettingKeys.CAMERA, cameraSelection)
        }
    }

    fun navigateBack() {
        navManager.navigateBack()
    }
}