package com.onandor.peripheryapp.webcam.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.webcam.ui.views.CameraPreviewView
import com.onandor.peripheryapp.webcam.viewmodels.CameraViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    Scaffold { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            CameraPreviewView(
                modifier = Modifier.fillMaxSize(),
                controller = viewModel.getController(context)
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