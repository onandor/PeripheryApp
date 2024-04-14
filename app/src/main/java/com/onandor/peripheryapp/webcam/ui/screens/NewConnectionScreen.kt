package com.onandor.peripheryapp.webcam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.webcam.ui.components.PermissionRequest
import com.onandor.peripheryapp.webcam.viewmodels.NewConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConnectionScreen(
    viewModel: NewConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.webcam_establish_connection)) },
                navigationIcon = {
                    IconButton(onClick = viewModel::navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!uiState.isCameraPermissionGranted) {
            PermissionRequest(
                onCameraPermissionGranted = viewModel::onCameraPermissionGranted
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 10.dp, start = 20.dp, end = 20.dp)
                        .fillMaxWidth()
                ) {
                    Text(stringResource(R.string.webcam_ip_address))
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.address,
                        onValueChange = viewModel::onAddressChanged,
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(stringResource(R.string.webcam_port))
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = uiState.port,
                        onValueChange = viewModel::onPortChanged,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = viewModel::navigateToCamera) {
                            Text(stringResource(id = R.string.webcam_connect))
                        }
                    }
                }
            }
        }
    }
}