package com.onandor.peripheryapp.webcam.viewmodels

import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onandor.peripheryapp.navigation.INavigationManager
import com.onandor.peripheryapp.navigation.navargs.CameraNavArgs
import com.onandor.peripheryapp.utils.AssetLoader
import com.onandor.peripheryapp.webcam.video.CameraController
import com.onandor.peripheryapp.webcam.video.CameraInfo
import com.onandor.peripheryapp.webcam.video.encoders.JpegEncoder
import com.onandor.peripheryapp.webcam.video.streamers.DCStreamer
import com.onandor.peripheryapp.webcam.video.encoders.H264Encoder
import com.onandor.peripheryapp.webcam.video.streamers.ClientStreamer
import com.onandor.peripheryapp.webcam.video.encoders.Encoder
import com.onandor.peripheryapp.webcam.video.streamers.IStreamer
import com.onandor.peripheryapp.webcam.video.streamers.StreamerEvent
import com.onandor.peripheryapp.webcam.video.streamers.StreamerType
import com.onandor.peripheryapp.webcam.video.Utils.Companion.to2ByteArray
import com.onandor.peripheryapp.webcam.network.TcpServer
import com.onandor.peripheryapp.webcam.video.streamers.IpStreamer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.inject.Inject

data class CameraUiState(
    val previewAspectRatio: Float = 1f,
    val showControls: Boolean = false,
    val zoom: Float = CameraController.DEFAULT_ZOOM,
    val zoomRange: ClosedFloatingPointRange<Float> = zoom..zoom,
    val aeCompensation: Float = CameraController.DEFAULT_AE_COMPENSATION.toFloat(),
    val aeRange: ClosedFloatingPointRange<Float> = aeCompensation..aeCompensation,
    val aeCompensationEV: Float = CameraController.DEFAULT_AE_COMPENSATION.toFloat(),
    val currentCamera: CameraViewModel.CameraOption = CameraViewModel.CameraOption(),
    val cameras: List<CameraViewModel.CameraOption> = emptyList(),
    val streamerCannotStart: Boolean = false
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val navManager: INavigationManager,
    private val cameraController: CameraController,
    tcpServer: TcpServer,
    assetLoader: AssetLoader
): ViewModel() {

    data class CameraOption(
        val id: String = "",
        val lensFacing: Int = -1
    )

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState = _uiState.asStateFlow()

    private var camera: CameraInfo
    private val resolution: Size
    private val frameRateRange: Range<Int>
    private val bitRate: Int
    private var previewSurface: Surface? = null
    private val cameraInfos: List<CameraInfo> = cameraController.getCameraInfos()

    private lateinit var encoder: Encoder
    private lateinit var streamer: IStreamer

    init {
        val navArgs = navManager.getCurrentNavAction()!!.navArgs as CameraNavArgs

        camera = cameraInfos.first { it.id == navArgs.cameraId }
        resolution = camera.resolutions[navArgs.resolutionIdx]
        frameRateRange = camera.frameRateRanges[navArgs.frameRateRangeIdx]
        bitRate = navArgs.bitRate

        val compatibleCameras = cameraInfos
            .filter {
                it.frameRateRanges.contains(frameRateRange) && it.resolutions.contains(resolution)
            }.map {
                CameraOption(
                    id = it.id,
                    lensFacing = it.lensFacing
                )
            }

        _uiState.update {
            it.copy(
                previewAspectRatio = resolution.width.toFloat() / resolution.height.toFloat(),
                zoomRange = camera.zoomRange.lower..camera.zoomRange.upper,
                aeRange = camera.aeRange.lower.toFloat()..camera.aeRange.upper.toFloat(),
                currentCamera = compatibleCameras.first { option -> option.id == camera.id },
                cameras = compatibleCameras
            )
        }

        when (navArgs.streamerType) {
            StreamerType.CLIENT -> {
                streamer = ClientStreamer(tcpServer)
                encoder = H264Encoder(resolution.width, resolution.height, bitRate, frameRateRange.upper) {
                    streamer.queueFrame(it)
                }
            }
            StreamerType.DC -> {
                streamer = DCStreamer(tcpServer)
                sendDCStreamerInit(resolution.width, resolution.height)
                encoder = JpegEncoder(resolution.width, resolution.height) {
                    streamer.queueFrame(it)
                }
            }
            StreamerType.IP -> {
                streamer = IpStreamer(tcpServer, assetLoader)
                encoder = JpegEncoder(resolution.width, resolution.height) {
                    streamer.queueFrame(it)
                }
            }
        }

        viewModelScope.launch { streamer.eventFlow.collect { onStreamerEvent(it) } }
        streamer.start()
    }

    private fun sendDCStreamerInit(width: Int, height: Int) {
        val widthBytes = width.to2ByteArray()
        val heightBytes = height.to2ByteArray()
        val initialBytes = listOf(
            widthBytes[0],
            widthBytes[1],
            heightBytes[0],
            heightBytes[1],
            0x21.toByte(),
            0xf5.toByte(),
            0xe8.toByte(),
            0x7f.toByte(),
            0x30.toByte()
        ).toByteArray()
        streamer.queueFrame(initialBytes)
    }

    private fun onStreamerEvent(event: StreamerEvent) {
        when (event) {
            StreamerEvent.CLIENT_DISCONNECTED -> navigateBack()
            StreamerEvent.CANNOT_START -> { /* TODO */ }
        }
    }

    fun onPreviewSurfaceCreated(previewSurface: Surface) {
        if (this.previewSurface != null) {
            this.previewSurface = previewSurface
            cameraController.updateCaptureTargets(listOf(previewSurface, encoder.inputSurface))
            return
        }
        this.previewSurface = previewSurface
        cameraController.start(camera, frameRateRange, listOf(previewSurface, encoder.inputSurface))
        encoder.start()
    }

    fun onPause() {
        cameraController.updateCaptureTargets(listOf(encoder.inputSurface))
        this.previewSurface!!.release()
    }

    fun onShowControls() {
        _uiState.update { it.copy(showControls = true) }
    }

    fun onHideControls() {
        _uiState.update { it.copy(showControls = false) }
    }

    fun onZoomChanged(value: Float) {
        val newZoom = roundTo1Decimal(value)
        _uiState.update { it.copy(zoom = newZoom) }
        cameraController.zoom(newZoom)
    }

    fun onAeCompensationChanged(value: Float) {
        val newAeCompensation = roundTo1Decimal(value)
        _uiState.update {
            it.copy(
                aeCompensation = newAeCompensation,
                aeCompensationEV = roundTo2Decimals(newAeCompensation * 0.16666667f)
            )
        }
        cameraController.adjustExposure(newAeCompensation.toInt())
    }

    fun onCameraChanged(option: CameraOption) {
        camera = cameraInfos.first { it.id == option.id }
        _uiState.update {
            it.copy(
                zoom = CameraController.DEFAULT_ZOOM,
                zoomRange = camera.zoomRange.lower..camera.zoomRange.upper,
                aeCompensation = CameraController.DEFAULT_AE_COMPENSATION.toFloat(),
                aeCompensationEV = CameraController.DEFAULT_AE_COMPENSATION.toFloat(),
                aeRange = camera.aeRange.lower.toFloat()..camera.aeRange.upper.toFloat(),
                currentCamera = option
            )
        }
        encoder.flush()
        cameraController.reset()
        cameraController.start(
            camera, frameRateRange, listOf(previewSurface!!, encoder.inputSurface))
    }

    private fun roundTo1Decimal(value: Float): Float {
        val df = DecimalFormat("#.#")
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(value).toFloat()
    }

    private fun roundTo2Decimals(value: Float): Float {
        val df = DecimalFormat("#.##")
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(value).toFloat()
    }

    fun navigateBack() {
        streamer.stop()
        encoder.release()
        cameraController.stop()
        navManager.navigateBack()
    }
}