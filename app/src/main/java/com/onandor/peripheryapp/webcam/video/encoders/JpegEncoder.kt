package com.onandor.peripheryapp.webcam.video.encoders

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

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
        val frame = ByteArray(image.planes[0].buffer.remaining())
        image.planes[0].buffer[frame]
        val compressedFrame = compress(frame)
        compressedFrame?.let(onDataReady)
        image.close()
    }

    private fun compress(frame: ByteArray): ByteArray? {
        val inputStream = ByteArrayInputStream(frame)
        val bitmapOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
        }
        val bitmap = BitmapFactory.decodeStream(inputStream, null, bitmapOptions)
        inputStream.close()
        if (bitmap == null) {
            return null
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        bitmap.recycle()

        val jpeg = outputStream.toByteArray()
        outputStream.close()
        return jpeg
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