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
    private val frameRate: Int,
    val onDataEncoded: (ByteArray) -> Unit
) {

    private val mHandlerThread = HandlerThread("DCEncoderThread").apply { start() }
    private val mHandler = Handler(mHandlerThread.looper)

    private val mImageReader: ImageReader = ImageReader.newInstance(
        width, height, ImageFormat.JPEG, 1)

    val inputSurface: Surface = mImageReader.surface

    private val mImageListener = ImageReader.OnImageAvailableListener { reader ->
        val image: Image = reader.acquireNextImage()
        val outFrame = ByteArray(image.planes[0].buffer.remaining())
        image.planes[0].buffer[outFrame]
        onDataEncoded(outFrame)
        image.close()
    }
    
    init {
        mImageReader.setOnImageAvailableListener(mImageListener, mHandler)
    }
}