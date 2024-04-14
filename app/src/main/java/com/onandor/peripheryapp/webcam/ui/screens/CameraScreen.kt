package com.onandor.peripheryapp.webcam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.webcam.stream.StreamVideoOutput
import com.onandor.peripheryapp.webcam.viewmodels.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val cameraProvider = remember { ProcessCameraProvider.getInstance(context).get() }

    Scaffold { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            CameraPreviewView(
                modifier = Modifier.fillMaxSize(),
                cameraProvider = cameraProvider,
                videoCapture = viewModel.videoCapture,
                onCameraGot = viewModel::onCameraGot
            )
            IconButton(
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = viewModel::onToggleCamera
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_cameraswitch),
                    contentDescription = "",
                    tint = MaterialTheme.colorScheme.surface
                )
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