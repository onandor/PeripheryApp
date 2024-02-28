package com.onandor.peripheryapp.ui.screens

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onandor.peripheryapp.viewmodels.BtDevicesViewModel

@Composable
fun BtDevicesScreen(
    viewModel: BtDevicesViewModel = hiltViewModel()
) {
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.updateBondedDevices()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (uiState.bluetoothState != BluetoothAdapter.STATE_ON) {
                Text("Bluetooth is disabled. Enable it to access the functionalities.")
                Button(
                    onClick = {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                ) {
                    Text(text = "Enable Bluetooth")
                }
            } else {
                LazyColumn {
                    item {
                        Text(text = "Paired devices:")
                    }
                    uiState.bondedDevices.forEach { device ->
                        item {
                            Text(
                                text = device.name ?: device.address,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.requestConnect(device) }
                                    .padding(16.dp)
                            )
                        }
                    }
                    item {
                        Text(text = "Available devices:")
                    }
                    uiState.foundDevices.forEach { device ->
                        item {
                            Text(
                                text = device.name ?: device.address,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.requestPair(device) }
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}