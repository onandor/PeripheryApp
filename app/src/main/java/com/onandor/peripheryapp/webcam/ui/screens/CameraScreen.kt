package com.onandor.peripheryapp.webcam.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.hardware.camera2.CameraMetadata
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.ui.components.SettingsDropdownMenu
import com.onandor.peripheryapp.utils.DropdownItem
import com.onandor.peripheryapp.webcam.viewmodels.CameraViewModel
import kotlinx.coroutines.launch

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
    
    if (uiState.streamerCannotStart) {
        val toastText = stringResource(id = R.string.webcam_streamer_cannot_start)
        LaunchedEffect(Unit) {
            Toast.makeText(context, toastText, Toast.LENGTH_LONG).show()
        }
    }

    BackHandler {
        viewModel.navigateBack()
    }

    Scaffold { innerPadding ->
        Surface(
            modifier = Modifier.padding(innerPadding),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val configuration = LocalConfiguration.current
                IconButton(onClick = viewModel::navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = null
                    )
                }
                if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    CameraSurfaceView(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(uiState.previewAspectRatio),
                        onPreviewSurfaceCreated = viewModel::onPreviewSurfaceCreated
                    )
                }
                IconButton(onClick = viewModel::onShowControls) {
                    Icon(
                        painterResource(id = R.drawable.ic_tune),
                        contentDescription = null
                    )
                }
            }
        }
        ControlsSheet(
            show = uiState.showControls,
            onDismissRequest = viewModel::onHideControls,
            zoom = uiState.zoom,
            zoomRange = uiState.zoomRange,
            onZoomChanged = viewModel::onZoomChanged,
            aeCompensation = uiState.aeCompensation,
            aeCompensationEV = uiState.aeCompensationEV,
            aeRange = uiState.aeRange,
            onAeCompensationChanged = viewModel::onAeCompensationChanged,
            currentCamera = uiState.currentCamera,
            cameras = uiState.cameras,
            onCameraChanged = viewModel::onCameraChanged
        )
    }
}

@Composable
fun CameraSurfaceView(
    modifier: Modifier = Modifier,
    onPreviewSurfaceCreated: (Surface) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = {
            SurfaceView(it).apply {
                this.holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        onPreviewSurfaceCreated(holder.surface)
                    }

                    override fun surfaceChanged(
                        holder: SurfaceHolder,
                        format: Int,
                        width: Int,
                        height: Int
                    ) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {}
                })
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsSheet(
    show: Boolean,
    onDismissRequest: () -> Unit,
    zoom: Float,
    zoomRange: ClosedFloatingPointRange<Float>,
    onZoomChanged: (Float) -> Unit,
    aeCompensation: Float,
    aeCompensationEV: Float,
    aeRange: ClosedFloatingPointRange<Float>,
    onAeCompensationChanged: (Float) -> Unit,
    currentCamera: CameraViewModel.CameraOption,
    cameras: List<CameraViewModel.CameraOption>,
    onCameraChanged: (CameraViewModel.CameraOption) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            sheetMaxWidth = 400.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                if (cameras.size > 1) {
                    val cameraItems = cameras.map {
                        val text = getCameraName(it)
                        DropdownItem(
                            onClick = {
                                coroutineScope.launch { sheetState.hide(); onDismissRequest() }
                                onCameraChanged(it)
                            },
                            text = { Text(text = text) }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(id = R.string.webcam_camera_change_camera))
                        SettingsDropdownMenu(
                            textToTheLeft = { Text(text = getCameraName(currentCamera)) },
                            items = cameraItems
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(id = R.string.webcam_camera_zoom))
                    Text(text = "${zoom}x")
                }
                Slider(
                    value = zoom,
                    steps = (zoomRange.endInclusive.toInt() - zoomRange.start.toInt()) * 10 - 1,
                    valueRange = zoomRange,
                    onValueChange = onZoomChanged,
                    enabled = zoomRange.endInclusive.toInt() - zoomRange.start.toInt() != 0
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(id = R.string.webcam_camera_exposure_compensation))
                    Text(text = "$aeCompensationEV EV")
                }
                Slider(
                    value = aeCompensation,
                    steps = (aeRange.endInclusive.toInt() - aeRange.start.toInt()) - 1,
                    valueRange = aeRange,
                    onValueChange = onAeCompensationChanged,
                    enabled = aeRange.start.toInt() != 0 && aeRange.endInclusive.toInt() != 0
                )
            }
        }
    }
}

@Composable
private fun getCameraName(cameraOption: CameraViewModel.CameraOption): String {
    return if (cameraOption.lensFacing == CameraMetadata.LENS_FACING_FRONT) {
        stringResource(id = R.string.webcam_settings_camera_front)
    } else {
        stringResource(id = R.string.webcam_settings_camera_back)
    }
}

fun Context.findActivity(): Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}