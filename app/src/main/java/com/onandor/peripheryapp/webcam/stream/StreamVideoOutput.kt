package com.onandor.peripheryapp.webcam.stream

import android.annotation.SuppressLint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Range
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.ConstantObservable
import androidx.camera.core.impl.Observable
import androidx.camera.video.MediaSpec
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.VideoOutput
import androidx.camera.video.VideoSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class StreamVideoOutput(
    resolutionIdx: Int,
    val frameRate: Int
) : VideoOutput {

    object Resolutions {
        const val LOW = 0
        const val HIGH = 1
    }

    object FrameRates {
        const val LOW = 15
        const val HIGH = 30
    }

    private val I_FRAME_INTERVAL = 2
    private val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC

    private var resolution: Size = Size(640, 480)
    private var qualitySelection: Quality = Quality.SD
    private val minBitrate = 1500
    private val maxBitrate = 2500

    private var surface: Surface? = null
    var mediaCodec: MediaCodec? = null
        private set

    init {
        setResolution(resolutionIdx)
    }

    override fun onSurfaceRequested(request: SurfaceRequest) {
        if (surface != null) {
            request.provideSurface(
                surface!!,
                Dispatchers.Main.asExecutor()
            ) {}
            return
        }

        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            resolution.width,
            resolution.height
        ).apply {
            setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR)
            //setInteger(MediaFormat.KEY_BIT_RATE, maxBitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        }

        mediaCodec = findSuitableCodec()
        mediaCodec?.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        surface = mediaCodec?.createInputSurface()
        request.provideSurface(
            surface!!,
            Dispatchers.Main.asExecutor()
        ) {}
    }

    @SuppressLint("RestrictedApi")
    override fun getMediaSpec(): Observable<MediaSpec> {
        val videoSpec = VideoSpec.builder()
            .setFrameRate(Range(frameRate, frameRate))
            .setBitrate(Range(minBitrate, maxBitrate))
            .setQualitySelector(QualitySelector.from(qualitySelection))
            .build()
        val mediaSpec = MediaSpec.builder().setVideoSpec(videoSpec).build()
        return ConstantObservable.withValue(mediaSpec)
    }

    private fun findSuitableCodec(): MediaCodec {
        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        var results = mediaCodecList.codecInfos
            .filter { it.isEncoder }
            .filter { it.supportedTypes.contains(MIME_TYPE) }
            .filter { mediaCodecInfo ->
                mediaCodecInfo.supportedTypes.any { type ->
                    mediaCodecInfo.getCapabilitiesForType(type)
                        .encoderCapabilities
                        .isBitrateModeSupported(
                            MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
                        )
                }
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            results = results.filter { it.isHardwareAccelerated }
        }
        return MediaCodec.createByCodecName(results.first().name)
    }

    fun release() {
        surface?.release()
        surface = null
    }

    private fun setResolution(idx: Int) {
        when(idx) {
            Resolutions.LOW -> {
                resolution = Size(640, 480)
                qualitySelection = Quality.SD
            }
            Resolutions.HIGH -> {
                resolution = Size(1280, 720)
                qualitySelection = Quality.HD
            }
        }
    }
}