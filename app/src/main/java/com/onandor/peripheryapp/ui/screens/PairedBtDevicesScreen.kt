package com.onandor.peripheryapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.viewmodels.PairedBtDevicesViewmodel

@Composable
fun PairedBtDevicesScreen(
    viewmodel: PairedBtDevicesViewmodel = hiltViewModel()
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text("PairedBtDevices")
            Button(onClick = viewmodel::pairNewDevice) {
                Text(text = "Pair new device")
            }
        }
    }
}