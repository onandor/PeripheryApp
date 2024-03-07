package com.onandor.peripheryapp.kbm.input

import android.content.Context
import com.onandor.peripheryapp.kbm.bluetooth.HidDataSender
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TouchpadController @Inject constructor(
    private val context: Context,
    private val hidDataSender: HidDataSender
) {

    private val DATA_RATE_US = 11250L
    private val buttonEventQueue: Queue<ButtonEvent> = LinkedList()

    private var dX = 0f
    private var dY = 0f
    private var dWheel = 0f
    private var leftButton = false
    private var rightButton = false

    private lateinit var executor: ScheduledThreadPoolExecutor
    private var scheduledFuture: ScheduledFuture<*>? = null

    fun init() {
        executor = ScheduledThreadPoolExecutor(1)
        scheduledFuture = executor.scheduleAtFixedRate(
            this::sendData,
            0,
            DATA_RATE_US,
            TimeUnit.MICROSECONDS
        )
    }

    fun buttonDown(button: MouseButton) {
        synchronized(buttonEventQueue) {
            buttonEventQueue.add(ButtonEvent(button.value, true))
        }
    }

    fun buttonUp(button: MouseButton) {
        synchronized(buttonEventQueue) {
            buttonEventQueue.add(ButtonEvent(button.value, false))
        }
    }

    fun move(x: Float, y: Float) {
        synchronized(buttonEventQueue) {
            dX += x
            dY += y
        }
    }

    fun scroll(wheel: Float) {
        synchronized(buttonEventQueue) {
            dWheel += wheel
        }
    }

    private fun sendData() {
        synchronized(buttonEventQueue) {
            val y = dY.toInt()
            val x = dX.toInt()
            val wheel = dWheel.toInt()
            dX = 0f
            dY = 0f
            dWheel = 0f

            if (buttonEventQueue.isNotEmpty()) {
                val event = buttonEventQueue.remove()
                if (event.button == MouseButton.LEFT.value) {
                    leftButton = event.state
                } else {
                    rightButton = event.state
                }
            }

            hidDataSender.sendMouse(leftButton, rightButton, false, x, y, wheel)
        }
    }

     fun release() {
         if (scheduledFuture == null) {
             return
         }
         executor.shutdownNow()
         scheduledFuture!!.cancel(true)
         scheduledFuture = null
     }
}