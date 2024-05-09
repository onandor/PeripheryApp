package com.onandor.peripheryapp.webcam.ui.screens

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.camera2.CameraManager
import android.view.LayoutInflater
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.onandor.peripheryapp.R
import com.onandor.peripheryapp.webcam.viewmodels.CameraViewModel2

@Composable
fun CameraScreen2(
    viewModel: CameraViewModel2 = hiltViewModel()
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val activity = context.findActivity() ?: return@DisposableEffect onDispose {}
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
            CameraSurfaceView(modifier = Modifier.fillMaxHeight()) { surface ->
                viewModel.onPreviewSurfaceCreated(
                    previewSurface = surface,
                    cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                )
            }
        }
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
                        val orientation = it.findActivity()?.resources?.configuration?.orientation
                        if (orientation == ActivityInfo.SCREEN_ORIENTATION_USER) {
                            onPreviewSurfaceCreated(holder.surface)
                        }
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