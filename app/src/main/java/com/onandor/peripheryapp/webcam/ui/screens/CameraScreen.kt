package com.onandor.peripheryapp.webcam.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.webcam.stream.StreamVideoOutput
import com.onandor.peripheryapp.webcam.viewmodels.CameraViewModel

@SuppressLint("SourceLockedOrientationActivity")
@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Scaffold { innerPadding ->
        Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
            Row(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = viewModel::navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.ArrowBack,
                        contentDescription = null
                    )
                }
                CameraPreviewView(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(4 / 3f),
                    cameraProvider = viewModel.getCameraProvider(context),
                    videoCapture = viewModel.videoCapture,
                    onCameraGot = viewModel::onCameraGot
                )
                Column {
                    IconButton(onClick = viewModel::onToggleCamera) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_cameraswitch),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
    BackHandler {
        viewModel.navigateBack()
    }
}

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    cameraProvider: ProcessCameraProvider,
    videoCapture: VideoCapture<StreamVideoOutput>,
    onCameraGot: (Camera) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        modifier = modifier,
        factory = {
            PreviewView(it).apply {
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(this.surfaceProvider)
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    /* lifecycleOwner = */ lifecycleOwner,
                    /* cameraSelector = */ CameraSelector.DEFAULT_FRONT_CAMERA,
                    /* ...useCases = */ preview, videoCapture
                )
                onCameraGot(camera)
            }
        }
    )
}

fun Context.findActivity(): Activity? = when(this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}