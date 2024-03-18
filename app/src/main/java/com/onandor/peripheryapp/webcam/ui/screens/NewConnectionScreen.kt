package com.onandor.peripheryapp.webcam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.webcam.viewmodels.NewConnectionViewModel

@Composable
fun NewConnectionScreen(
    viewModel: NewConnectionViewModel = hiltViewModel()
) {
    BackHandler {
        viewModel.navigateBack()
    }
}