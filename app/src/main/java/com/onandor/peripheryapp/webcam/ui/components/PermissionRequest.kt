package com.onandor.peripheryapp.webcam.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.core.content.ContextCompat
import com.onandor.peripheryapp.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@Composable
fun PermissionRequest(
    modifier: Modifier,
    onCameraPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val _isCameraPermissionGranted = MutableStateFlow(
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    )
    val isCameraPermissionGranted by _isCameraPermissionGranted.collectAsState()

    val _isMicrophonePermissionGranted = MutableStateFlow(
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    )
    val isMicrophonePermissionGranted by _isMicrophonePermissionGranted.collectAsState()

    LaunchedEffect(isCameraPermissionGranted) {
        if (isCameraPermissionGranted) {
            onCameraPermissionGranted()
        }
    }

    if (!isCameraPermissionGranted) {
        PermissionMissing(
            modifier = modifier,
            onCameraPermissionGranted = { _isCameraPermissionGranted.update { true } },
            onMicrophonePermissionGranted = { _isMicrophonePermissionGranted.update { true } }
        )
    }
}

@Composable
private fun PermissionMissing(
    modifier: Modifier,
    onCameraPermissionGranted: () -> Unit,
    onMicrophonePermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val packageName = context.packageName

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            onCameraPermissionGranted()
        }
        if (permissions[Manifest.permission.RECORD_AUDIO] == true) {
            onMicrophonePermissionGranted()
        }
    }

    val appSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        val cameraPermissionGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        val microphonePermissionGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
        if (cameraPermissionGranted) {
            onCameraPermissionGranted()
        }
        if (microphonePermissionGranted) {
            onMicrophonePermissionGranted()
        }
    }

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
                text = stringResource(R.string.webcam_permission_request_camera_permission_denied),
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier.padding(bottom = 30.dp, start = 30.dp, end = 30.dp),
                text = stringResource(R.string.webcam_permission_request_microphone),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Row {
                Button(
                    onClick = {
                        permissionsLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.RECORD_AUDIO
                            )
                        )
                    }
                ) {
                    Text(stringResource(id = R.string.permission_request_grant_permissions))
                }
                Spacer(modifier = Modifier.width(20.dp))
                Button(
                    onClick = {
                        appSettingsLauncher.launch(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.parse("package:${packageName}")
                        })
                    }
                ) {
                    Text(stringResource(id = R.string.permission_request_app_settings))
                }
            }
        }
    }
}