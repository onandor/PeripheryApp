package com.onandor.peripheryapp.webcam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.webcam.ui.components.PermissionRequest
import com.onandor.peripheryapp.webcam.viewmodels.NewConnectionViewModel

@Composable
fun NewConnectionScreen(
    viewModel: NewConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold { innerPadding ->
        if (!uiState.isCameraPermissionGranted) {
            PermissionRequest(
                onCameraPermissionGranted = viewModel::onCameraPermissionGranted
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Button(onClick = viewModel::navigateToCamera) {
                    Text(text = "Camera")
                }
            }
        }
    }
}