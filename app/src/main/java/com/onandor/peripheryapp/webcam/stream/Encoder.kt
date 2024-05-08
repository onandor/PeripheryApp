package com.onandor.peripheryapp.webcam.stream

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Size

class Encoder(
    private val mediaCodec: MediaCodec,
    private val onDataEncoded: (ByteArray) -> Unit,
) {

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
        mediaCodec.setCallback(mediaCodecCallback)
        mediaCodec.start()
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

    fun release() {
        try {
            mediaCodec.stop()
        } catch (_: Exception) {}
        mediaCodec.release()
    }
}