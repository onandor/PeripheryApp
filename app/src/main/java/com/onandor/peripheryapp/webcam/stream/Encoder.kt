package com.onandor.peripheryapp.webcam.stream

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

class Encoder(
    private val mediaCodec: MediaCodec,
    private val onDataEncoded: (ByteArray) -> Unit,
) {

    private var spsPpsInfo: ByteArray? = null
    private val outputStream = ByteArrayOutputStream()
    private lateinit var muxer: MediaMuxer
    private var trackIndex = -1

    private val mediaCodecCallback = object : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            println("onInputBufferAvaiable")
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo
        ) {
            val data = encode(codec, index, info)
            data?.let(onDataEncoded)
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            println("onError")
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            //trackIndex = muxer.addTrack(format)
            //muxer.start()
        }
    }

    init {
        mediaCodec.setCallback(mediaCodecCallback)
        mediaCodec.start()

        //val outputPath = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "test.mp4").toString()
        //muxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    }

    fun encode(mediaCodec: MediaCodec, index: Int, bufferInfo: MediaCodec.BufferInfo): ByteArray? {
        val outputBuffer = mediaCodec.getOutputBuffer(index)
        val outData = ByteArray(bufferInfo.size)
        if (outputBuffer == null) {
            throw RuntimeException("Encoder: Couldn't fetch buffer at index $index")
        } else {
            outputBuffer[outData]
        }

        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            mediaCodec.releaseOutputBuffer(index, false)
            return null
        }

        if (spsPpsInfo == null) {
            val spsPpsBuffer = ByteBuffer.wrap(outData)
            if (spsPpsBuffer.getInt() == 0x00000001) {
                spsPpsInfo = ByteArray(outData.size)
                spsPpsInfo = outData.copyOf()
            } else {
                return null
            }
        } else {
            outputStream.write(outData)
        }

        mediaCodec.releaseOutputBuffer(index, false)

        var ret = outputStream.toByteArray()
        if (ret.size > 5 && ret[4] == 0x65.toByte()) {
            outputStream.reset()
            outputStream.write(spsPpsInfo)
            outputStream.write(ret)
            ret = outputStream.toByteArray()
        }
        //muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo)
        outputStream.reset()
        return ret
    }

    fun release() {
        mediaCodec.stop()
        mediaCodec.release()
        //muxer.stop()
        //muxer.release()
    }
}