package com.onandor.peripheryapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.viewmodels.MainViewmodel

@Composable
fun MainScreen(
    viewmodel: MainViewmodel = hiltViewModel()
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            TextButton(onClick = viewmodel::navigateToBtKbmScreen) {
                Text("Bluetooth keyboard and mouse")
            }
            TextButton(onClick = viewmodel::navigateToWifiWebcamScreen) {
                Text("Wifi webcam")
            }
        }
    }
}