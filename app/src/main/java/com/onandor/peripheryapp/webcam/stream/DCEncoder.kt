package com.onandor.peripheryapp.webcam.stream

import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface

class DCEncoder(
    val width: Int,
    val height: Int,
    val onDataReady: (ByteArray) -> Unit
) : Encoder {

    private val mHandlerThread = HandlerThread("DCEncoderThread").apply { start() }
    private val mHandler = Handler(mHandlerThread.looper)

    private val mImageReader: ImageReader = ImageReader.newInstance(
        width, height, ImageFormat.JPEG, 1)

    override val inputSurface: Surface = mImageReader.surface

    private val mImageListener = ImageReader.OnImageAvailableListener { reader ->
        val image: Image = reader.acquireNextImage()
        val outFrame = ByteArray(image.planes[0].buffer.remaining())
        image.planes[0].buffer[outFrame]
        onDataReady(outFrame)
        image.close()
    }

    private fun sendInitialization() {
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
        onDataReady(initialBytes)
    }

    private fun Int.to2ByteArray() : ByteArray = byteArrayOf(shr(8).toByte(), toByte())

    override fun start() {
        sendInitialization()
        mImageReader.setOnImageAvailableListener(mImageListener, mHandler)
    }

    override fun flush() {}

    override fun release() {
        inputSurface.release()
        mImageReader.close()
    }
}