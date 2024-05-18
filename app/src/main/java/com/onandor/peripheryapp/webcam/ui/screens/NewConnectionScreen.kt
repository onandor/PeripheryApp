package com.onandor.peripheryapp.webcam.ui.screens

import android.hardware.camera2.CameraMetadata
import android.util.Range
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.ui.components.SettingItem
import com.onandor.peripheryapp.ui.components.SettingsDropdownMenu
import com.onandor.peripheryapp.utils.DropdownItem
import com.onandor.peripheryapp.webcam.video.CameraInfo
import com.onandor.peripheryapp.webcam.video.streamers.StreamerType
import com.onandor.peripheryapp.webcam.network.TcpServer
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
    
    if (uiState.tcpServerEvent != null) {
        val toastText = when (uiState.tcpServerEvent) {
            TcpServer.Event.PortInUse -> stringResource(id = R.string.webcam_port_in_use)
            TcpServer.Event.CannotStart -> stringResource(id = R.string.webcam_tcp_server_cannot_start)
            else -> ""
        }
        LaunchedEffect(uiState.tcpServerEvent) {
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
            viewModel.onToastShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.webcam_new_connection)) },
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
        if (uiState.noCameras) {
            NoCameras(Modifier.padding(innerPadding))
        } else if (!uiState.isCameraPermissionGranted) {
            PermissionRequest(
                modifier = Modifier.padding(innerPadding),
                onCameraPermissionGranted = viewModel::onCameraPermissionGranted
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StreamerTypeSelector(
                    streamerType = uiState.streamerType,
                    onStreamerTypeChanged = viewModel::onStreamerTypeChanged
                )
                HorizontalDivider(modifier = Modifier.padding(start = 75.dp, end = 75.dp, top = 15.dp, bottom = 20.dp))
                DCConnectionSettings(
                    address = uiState.address,
                    port = uiState.port
                )
                HorizontalDivider(modifier = Modifier.padding(start = 75.dp, end = 75.dp, top = 20.dp, bottom = 20.dp))
                CameraSettings(
                    cameraInfos = uiState.cameraInfos,
                    cameraId = uiState.cameraId,
                    resolutionIdx = uiState.resolutionIdx,
                    frameRateRangeIdx = uiState.frameRateRangeIdx,
                    onCameraIdChanged = viewModel::onCameraIdChanged,
                    onResolutionIdxChanged = viewModel::onResolutionIdxChanged,
                    onFrameRateRangeIdxChanged = viewModel::onFrameRateRangeIdxChanged,
                    bitRate = uiState.bitRate,
                    bitRates = uiState.bitRates,
                    onBitRateChanged = viewModel::onBitRateChanged,
                    streamerType = uiState.streamerType
                )
            }
        }
    }
}

@Composable
fun NoCameras(modifier: Modifier) {
    Surface {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                modifier = Modifier.size(120.dp),
                painter = painterResource(id = R.drawable.ic_videocam_off),
                contentDescription = ""
            )
            Text(
                modifier = Modifier.padding(30.dp),
                text = stringResource(id = R.string.webcam_no_cameras),
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StreamerTypeSelector(
    streamerType: Int,
    onStreamerTypeChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val currentStreamerName = when (streamerType) {
            StreamerType.CLIENT -> stringResource(id = R.string.webcam_streamer_client)
            StreamerType.DC -> stringResource(id = R.string.webcam_streamer_dc)
            else -> ""
        }
        val items = listOf(
            DropdownItem(
                onClick = { onStreamerTypeChanged(StreamerType.CLIENT) },
                text = { Text(text = stringResource(id = R.string.webcam_streamer_client)) }
            ),
            DropdownItem(
                onClick = { onStreamerTypeChanged(StreamerType.DC) },
                text = { Text(text = stringResource(id = R.string.webcam_streamer_dc)) }
            )
        )
        Text(
            text = stringResource(id = R.string.webcam_streamer),
            fontSize = 20.sp
        )
        SettingsDropdownMenu(
            items = items,
            textToTheLeft = { Text(text = currentStreamerName) }
        )
    }
}

@Composable
fun DCConnectionSettings(
    address: String,
    port: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            text = stringResource(id = R.string.webcam_connection),
            fontSize = 20.sp
        )
        Card(modifier = Modifier.padding(start = 15.dp, end = 15.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.webcam_ip_address) + ": " + address,
                        fontSize = 20.sp
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.webcam_port) + ": " + port,
                        fontSize = 20.sp
                    )
                }
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
    onFrameRateRangeIdxChanged: (Int) -> Unit,
    bitRate: Int,
    bitRates: List<Int>,
    onBitRateChanged: (Int) -> Unit,
    streamerType: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, bottom = 20.dp)
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
        val bitRateItems: List<DropdownItem> = bitRates.map {
            DropdownItem(
                onClick = { onBitRateChanged(it) },
                text = { Text(text = "${it / 1000} Kbps") }
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
        if (streamerType == StreamerType.CLIENT) {
            SettingItem(text = stringResource(id = R.string.webcam_settings_frame_rate)) {
                SettingsDropdownMenu(
                    textToTheLeft = {
                        Text(text = getFrameRateRangeText(
                            selectedCamera.frameRateRanges[frameRateRangeIdx]))
                    },
                    items = frameRateRangeItems
                )
            }
            SettingItem(text = stringResource(id = R.string.webcam_settings_bit_rate) ) {
                SettingsDropdownMenu(
                    textToTheLeft = { Text(text = "${bitRate / 1000} Kbps") },
                    items = bitRateItems
                )
            }
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