package com.onandor.peripheryapp.ui.screens

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onandor.peripheryapp.kbm.data.BtDevice
import com.onandor.peripheryapp.viewmodels.NewBtConnectionViewmodel

@Composable
fun NewBtConnectionScreen(
    viewmodel: NewBtConnectionViewmodel = hiltViewModel()
) {
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewmodel.updatePairedDevices()
        }
    }

    val uiState by viewmodel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(uiState.isConnected) {
        if (uiState.isConnected) {
            Toast.makeText(context, "Connected", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewmodel.errorMessageShown()
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!uiState.isBluetoothEnabled) {
                Text("Bluetooth is disabled. Enable it to access the functionalities.")
                Button(
                    onClick = {
                        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                ) {
                    Text(text = "Enable Bluetooth")
                }
            } else {
                Button(onClick = viewmodel::showSearchForDevicesDialog) {
                    Text(text = "Search for devices")
                }
                Button(onClick = { /*TODO*/ }) {
                    Text("Turn on discoverability")
                }
            }
        }
    }

    if (uiState.searchForDevicesDialogShown) {
        SearchForDevicesDialog(
            onDeviceClick = viewmodel::connectToDevice,
            onDismissRequest = viewmodel::dismissSearchForDevicesDialog,
            scannedDevices = uiState.scannedDevices
        )
    }
}

@Composable
private fun SearchForDevicesDialog(
    onDeviceClick: (BtDevice) -> Unit,
    onDismissRequest: () -> Unit,
    scannedDevices: List<BtDevice>
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "Choose a device to pair")
                LazyColumn {
                    scannedDevices.forEach { device ->
                        item { 
                            Text(
                                text = device.name ?: device.address,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable { onDeviceClick(device) }
                            )
                        }
                    }
                }
                Button(onClick = onDismissRequest) {
                    Text(text = "Cancel")
                }
            }
        }
    }
}