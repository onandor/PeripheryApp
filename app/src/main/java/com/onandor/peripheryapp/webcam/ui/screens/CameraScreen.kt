package com.onandor.peripheryapp.webcam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.webcam.viewmodels.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    BackHandler {
        viewModel.navigateBack()
    }
}