package com.onandor.peripheryapp.webcam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.ui.components.SettingItem
import com.onandor.peripheryapp.ui.components.SettingsDropdownMenu
import com.onandor.peripheryapp.utils.DropdownItem
import com.onandor.peripheryapp.webcam.SettingOptions
import com.onandor.peripheryapp.webcam.viewmodels.WebcamSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebcamSettingsScreen(
    viewModel: WebcamSettingsViewModel = hiltViewModel()
) {

    val resolutionItems = SettingOptions
        .resolution
        .map { option ->
            DropdownItem(
                text = { Text(text = stringResource(id = option.resourceId)) },
                onClick = { viewModel.onResolutionChanged(option.value) }
            )
        }

    val frameRateItems = SettingOptions
        .frameRate
        .map { option ->
            DropdownItem(
                text = { Text(text = stringResource(id = option.resourceId)) },
                onClick = { viewModel.onFrameRateChanged(option.value) }
            )
        }
    val cameraSelectionItems = SettingOptions
        .cameraSelection
        .map { option ->
            DropdownItem(
                text = { Text(text = stringResource(id = option.resourceId)) },
                onClick = { viewModel.onCameraSelectionChanged(option.value) }
            )
        }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.webcam_settings)) },
                navigationIcon = {
                    IconButton(onClick = viewModel::navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val uiState by viewModel.uiState.collectAsState()
        
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            SettingItem(
                text = stringResource(id = R.string.webcam_settings_resolution),
                action = {
                    SettingsDropdownMenu(
                        textToTheLeft = {
                            Text(text = stringResource(id = uiState.resolution.resourceId))
                        },
                        items = resolutionItems
                    )
                }
            )
            SettingItem(
                text = stringResource(id = R.string.webcam_settings_frame_rate),
                action = {
                    SettingsDropdownMenu(
                        textToTheLeft = {
                            Text(text = stringResource(id = uiState.frameRate.resourceId))
                        },
                        items = frameRateItems
                    )
                }
            )
            SettingItem(
                text = stringResource(id = R.string.webcam_settings_camera),
                action = {
                    SettingsDropdownMenu(
                        textToTheLeft = {
                            Text(text = stringResource(id = uiState.cameraSelection.resourceId))
                        },
                        items = cameraSelectionItems
                    )
                }
            )
        }
    }
}