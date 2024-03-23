package com.onandor.peripheryapp.webcam.stream

import android.media.MediaCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class Encoder(
    private val mediaCodec: MediaCodec
) {

    private val TIMEOUT_US = 10000L
    private var running = false
    private var spsPpsInfo: ByteArray? = null
    private val outputStream = ByteArrayOutputStream()

    private fun doEncodeVideoFromBuffer() {
        while (running) {
            val bufferInfo = MediaCodec.BufferInfo()
            var outputBuffers = mediaCodec.outputBuffers
            var outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)

            while (outputBufferIndex >= 0) {
                val outputBuffer = outputBuffers[outputBufferIndex]
                val outData = ByteArray(bufferInfo.size)
                outputBuffer[outData]

                if (spsPpsInfo == null) {
                    val spsPpsBuffer = ByteBuffer.wrap(outData)
                    if (spsPpsBuffer.getInt() == 0x00000001) {
                        spsPpsInfo = ByteArray(outData.size)
                        spsPpsInfo = outData.copyOf()
                    } else {
                        return
                    }
                } else {
                    outputStream.write(outData)
                }

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false)
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_US)
            }

            val ret = outputStream.toByteArray()
            if (ret.size > 5 && ret[4] == 0x65.toByte()) {
                outputStream.reset()
                outputStream.write(spsPpsInfo)
                outputStream.write(ret)
            }
            //println(outputStream)
            outputStream.reset()
        }
    }

    fun start() {
        running = true
        CoroutineScope(Dispatchers.IO).launch {
            doEncodeVideoFromBuffer()
        }
    }

    fun stop() {
        running = false
    }
}