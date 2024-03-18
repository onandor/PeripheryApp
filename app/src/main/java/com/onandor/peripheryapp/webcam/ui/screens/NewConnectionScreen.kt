package com.onandor.peripheryapp.webcam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.webcam.viewmodels.NewConnectionViewModel

@Composable
fun NewConnectionScreen(
    viewModel: NewConnectionViewModel = hiltViewModel()
) {
    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold { innerPadding ->
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