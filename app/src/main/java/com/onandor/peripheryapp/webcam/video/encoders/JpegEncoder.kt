package com.onandor.peripheryapp.webcam.video.encoders

import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface

class JpegEncoder(
    val width: Int,
    val height: Int,
    val onDataReady: (ByteArray) -> Unit
) : Encoder {

    private val mHandlerThread = HandlerThread("JpegEncoderThread").apply { start() }
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

    override fun start() {
        mImageReader.setOnImageAvailableListener(mImageListener, mHandler)
    }

    override fun flush() {}

    override fun release() {
        inputSurface.release()
        mImageReader.close()
    }
}