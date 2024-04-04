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
import java.io.FileOutputStream
import java.nio.ByteBuffer

class Encoder(
    private val mediaCodec: MediaCodec,
    private val onDataEncoded: (ByteArray) -> Unit,
) {

    private var spsPpsInfo: ByteArray? = null
    private val outputStream = ByteArrayOutputStream()
    private var spsPps: ByteArray? = null

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
            outputStream.write(sps?.array())
            outputStream.write(pps?.array())
            onDataEncoded(outputStream.toByteArray())
            outputStream.reset()
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
        /*
        if (ret.size > 5 && ret[4] == 0x65.toByte()) {
            println("insert SPS PPS info")
            outputStream.reset()
            outputStream.write(spsPpsInfo)
            outputStream.write(ret)
            ret = outputStream.toByteArray()
        }
         */
        //println("bufferInfo.size: " + bufferInfo.size + ", bufferInfo.offset: " + bufferInfo.offset)
        //muxer.writeSampleData(trackIndex, ByteBuffer.wrap(outData), bufferInfo)
        //fos.write(outData)
        outputStream.reset()
        return outData
    }

    fun release() {
        mediaCodec.stop()
        mediaCodec.release()
    }
}