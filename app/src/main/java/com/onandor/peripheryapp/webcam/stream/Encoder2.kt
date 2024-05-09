package com.onandor.peripheryapp.webcam.stream

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.view.Surface
import java.lang.Exception

class Encoder2(private val onDataEncoded: (ByteArray) -> Unit,) {

    private val width = 640
    private val height = 480
    private val bitRate = 2500
    val frameRate = 30

    private val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    private val I_FRAME_INTERVAL = 1

    private val mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE)
    var inputSurface: Surface? = null

    private var spsPpsNalu: ByteArray? = null

    private val mediaCodecCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            val data = encode(codec, index, info)
            data?.let(onDataEncoded)
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            // TODO
            println("onError")
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            val sps = format.getByteBuffer("csd-0")
            val pps = format.getByteBuffer("csd-1")
            if (sps == null || pps == null) {
                return
            }

            spsPpsNalu = sps.array() + pps.array()
        }
    }

    init {
        val format = MediaFormat
            .createVideoFormat(MIME_TYPE, width, height)
            .apply {
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL)
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        inputSurface = mediaCodec.createInputSurface()
    }

    fun encode(mediaCodec: MediaCodec, index: Int, bufferInfo: MediaCodec.BufferInfo): ByteArray? {
        val outputBuffer = mediaCodec.getOutputBuffer(index)
        val outData = ByteArray(bufferInfo.size)
        if (outputBuffer == null) {
            throw RuntimeException("Encoder: Couldn't fetch buffer at index $index")
        } else {
            outputBuffer[outData]
        }

        mediaCodec.releaseOutputBuffer(index, false)

        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 ||
            spsPpsNalu == null) {
            // Configuration change is handled in the callback
            return null
        }

        // Insert SPS and PPS NALU before every key frame to be sure
        return if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
            spsPpsNalu!! + outData
        } else {
            outData
        }
    }

    fun start() {
        mediaCodec.setCallback(mediaCodecCallback)
        mediaCodec.start()
    }

    fun release() {
        try {
            mediaCodec.stop()
        } catch (_: Exception) {}
        mediaCodec.release()
        inputSurface?.release()
    }
}