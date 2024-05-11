package com.onandor.peripheryapp.webcam.ui.screens

import android.hardware.camera2.CameraMetadata
import android.util.Range
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.ui.components.SettingItem
import com.onandor.peripheryapp.ui.components.SettingsDropdownMenu
import com.onandor.peripheryapp.utils.DropdownItem
import com.onandor.peripheryapp.webcam.stream.CameraInfo
import com.onandor.peripheryapp.webcam.stream.Streamer
import com.onandor.peripheryapp.webcam.ui.components.PermissionRequest
import com.onandor.peripheryapp.webcam.viewmodels.NewConnectionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConnectionScreen(
    viewModel: NewConnectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateBack()
    }

    if (uiState.connectionEvent != null) {
        val toastText = when(uiState.connectionEvent) {
            Streamer.ConnectionEvent.TIMEOUT_FAILURE ->
                stringResource(R.string.webcam_timeout)
            Streamer.ConnectionEvent.UNKNOWN_HOST_FAILURE ->
                stringResource(R.string.webcam_unknown_host)
            Streamer.ConnectionEvent.HOST_UNREACHABLE_FAILURE ->
                stringResource(R.string.webcam_host_unreachable)
            else -> ""
        }
        LaunchedEffect(uiState.connectionEvent) {
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
            viewModel.onToastShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.webcam_establish_connection)) },
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
                ConnectionSettings(
                    address = uiState.address,
                    port = uiState.port,
                    connecting = uiState.connecting,
                    canConnect = uiState.canConnect,
                    onAddressChanged = viewModel::onAddressChanged,
                    onPortChanged = viewModel::onPortChanged,
                    onConnect = viewModel::onConnect
                )
                CameraSettings(
                    cameraInfos = uiState.cameraInfos,
                    cameraId = uiState.cameraId,
                    resolutionIdx = uiState.resolutionIdx,
                    frameRateRangeIdx = uiState.frameRateRangeIdx,
                    onCameraIdChanged = viewModel::onCameraIdChanged,
                    onResolutionIdxChanged = viewModel::onResolutionIdxChanged,
                    onFrameRateRangeIdxChanged = viewModel::onFrameRateRangeIdxChanged
                )
            }
        }
    }
}

@Composable
fun ConnectionSettings(
    address: String,
    port: String,
    connecting: Boolean,
    canConnect: Boolean,
    onAddressChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onConnect: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(top = 10.dp, start = 20.dp, end = 20.dp)
            .fillMaxWidth()
    ) {
        Text(stringResource(R.string.webcam_ip_address))
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = address,
            onValueChange = onAddressChanged,
            singleLine = true
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(stringResource(R.string.webcam_port))
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = port,
            onValueChange = onPortChanged,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (connecting) {
                CircularProgressIndicator(modifier = Modifier
                    .padding(end = 10.dp)
                    .size(25.dp))
                Text(stringResource(R.string.webcam_connecting))
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onConnect,
                enabled = !connecting && canConnect
            ) {
                Text(stringResource(id = R.string.webcam_connect))
            }
        }
    }
}

@Composable
fun CameraSettings(
    cameraInfos: List<CameraInfo>,
    cameraId: String,
    resolutionIdx: Int,
    frameRateRangeIdx: Int,
    onCameraIdChanged: (String) -> Unit,
    onResolutionIdxChanged: (Int) -> Unit,
    onFrameRateRangeIdxChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, bottom = 20.dp)
        ) {
            Text(
                text = stringResource(id = R.string.webcam_settings_camera_configuration),
                fontSize = 20.sp
            )
        }

        val selectedCamera = cameraInfos.find { it.id == cameraId }!!
        val cameraItems: List<DropdownItem> = cameraInfos.map { option ->
            DropdownItem(
                onClick = { onCameraIdChanged(option.id) },
                text = { Text(text = getCameraIdText(lensFacing = option.lensFacing)) }
            )
        }
        val resolutionItems: List<DropdownItem> = selectedCamera
            .resolutions
            .map { resolution ->
                val text = getResolutionText(resolution)
                DropdownItem(
                    onClick = {
                        onResolutionIdxChanged(selectedCamera.resolutions.indexOf(resolution))
                    },
                    text = { Text(text = text) }
                )
            }
        val frameRateRangeItems: List<DropdownItem> = selectedCamera
            .frameRateRanges
            .map { range ->
                val text = getFrameRateRangeText(range)
                DropdownItem(
                    onClick = {
                        onFrameRateRangeIdxChanged(selectedCamera.frameRateRanges.indexOf(range))
                    },
                    text = { Text(text = text) }
                )
            }


        SettingItem(text = stringResource(id = R.string.webcam_settings_camera)) {
            SettingsDropdownMenu(
                textToTheLeft = {
                    Text(text = getCameraIdText(lensFacing = selectedCamera.lensFacing))
                },
                items = cameraItems
            )
        }
        SettingItem(text = stringResource(id = R.string.webcam_settings_resolution)) {
            SettingsDropdownMenu(
                textToTheLeft = {
                    Text(text = getResolutionText(selectedCamera.resolutions[resolutionIdx]))
                },
                items = resolutionItems
            )
        }
        SettingItem(text = stringResource(id = R.string.webcam_settings_frame_rate)) {
            SettingsDropdownMenu(
                textToTheLeft = {
                    Text(text = getFrameRateRangeText(
                        selectedCamera.frameRateRanges[frameRateRangeIdx]))
                },
                items = frameRateRangeItems
            )
        }
    }
}

@Composable
private fun getCameraIdText(lensFacing: Int): String {
    return if (lensFacing == CameraMetadata.LENS_FACING_FRONT) {
        stringResource(id = R.string.webcam_settings_camera_front)
    } else {
        stringResource(id = R.string.webcam_settings_camera_back)
    }
}

private fun getResolutionText(resolution: Size): String =
    "${resolution.width}x${resolution.height}"

private fun getFrameRateRangeText(range: Range<Int>): String {
    if (range.lower == range.upper) {
        return "${range.lower} fps"
    }
    return "${range.lower} - ${range.upper} fps"
}