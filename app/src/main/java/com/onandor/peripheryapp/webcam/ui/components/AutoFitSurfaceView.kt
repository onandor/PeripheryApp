package com.onandor.peripheryapp.webcam.ui.components

import android.content.Context
import android.view.SurfaceView
import kotlin.math.roundToInt

class AutoFitSurfaceView(private val context: Context) : SurfaceView(context, null) {

    private var mAspectRatio = 0f

    fun setAspectRatio(width: Int, height: Int) {
        require(width > 0 && height > 0) { "Size must be greater than zero" }
        mAspectRatio = width.toFloat() / height.toFloat()
        holder.setFixedSize(width, height)
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        if (mAspectRatio == 0f) {
            setMeasuredDimension(width, height)
        } else {
            val newWidth: Int
            val newHeight: Int
            val actualRatio = if (width > height) mAspectRatio else 1f / mAspectRatio
            if (width < height * actualRatio) {
                newWidth = (height * actualRatio).roundToInt()
                newHeight = height
            } else {
                newWidth = width
                newHeight = (width / actualRatio).roundToInt()
            }
            setMeasuredDimension(newWidth, newHeight)
        }
    }
}