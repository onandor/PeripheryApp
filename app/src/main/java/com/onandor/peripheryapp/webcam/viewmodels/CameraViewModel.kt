package com.onandor.peripheryapp.webcam.viewmodels

import android.content.Context
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.lifecycle.ViewModel
import com.onandor.peripheryapp.navigation.INavigationManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val navManager: INavigationManager
) : ViewModel() {

    private var controller: LifecycleCameraController? = null

    private val imageAnalyzer = object : ImageAnalysis.Analyzer {

        private var frameSkipCounter = 0

        override fun analyze(image: ImageProxy) {
            if (frameSkipCounter % 60 != 0) {
                frameSkipCounter++
                image.close()
                return
            }
            //println("height: ${image.height}, width: ${image.width}")
            //println("rotationDegrees: ${image.imageInfo.rotationDegrees}")
            //println(image.format)
            frameSkipCounter = 0
            image.close()
        }
    }

    private val resolutionSelector = ResolutionSelector.Builder()
        .setResolutionStrategy(
            ResolutionStrategy(
                Size(1280, 720),
                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
            )
        )
        .build()

    fun getController(context: Context): LifecycleCameraController {
        if (controller != null) {
            return controller!!
        }
        controller = LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            setImageAnalysisAnalyzer(Dispatchers.Main.asExecutor(), imageAnalyzer)
            imageCaptureResolutionSelector = resolutionSelector
            imageAnalysisResolutionSelector = resolutionSelector
        }
        return controller!!
    }

    fun onToggleCamera() {
        controller?.cameraSelector =
            if (controller?.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }
    }

    fun navigateBack() {
        navManager.navigateBack()
    }
}