package com.onandor.peripheryapp.kbm.input

import android.content.Context
import com.onandor.peripheryapp.kbm.bluetooth.BluetoothController
import com.onandor.peripheryapp.utils.BtSettingKeys
import com.onandor.peripheryapp.utils.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TouchpadController @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val settings: Settings
) {

    object PollingRates {
        val LOW = 20000L
        val HIGH = 11250L
    }

    private var pollingRateJob: Job? = null
    private val buttonEventQueue: Queue<ButtonEvent> = LinkedList()

    private var dX = 0f
    private var dY = 0f
    private var dWheel = 0f
    private var leftButton = false
    private var rightButton = false

    private lateinit var executor: ScheduledThreadPoolExecutor
    private var sendDataTask: ScheduledFuture<*>? = null

    fun init() {
        executor = ScheduledThreadPoolExecutor(1)
        pollingRateJob = settings
            .observe(BtSettingKeys.MOUSE_POLLING_RATE, PollingRates.HIGH)
            .onEach { resetSendDataTask(it) }
            .launchIn(CoroutineScope(Dispatchers.Main))
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

            bluetoothController.sendMouse(leftButton, rightButton, false, x, y, wheel)
        }
    }

    private fun resetSendDataTask(pollingRate: Long) {
        println("resetSendDataTask")
        if (sendDataTask != null) {
            sendDataTask?.cancel(true)
            sendDataTask = null
        }
        sendDataTask = executor.scheduleAtFixedRate(
            this::sendData,
            0,
            pollingRate,
            TimeUnit.MICROSECONDS
        )
    }

     fun release() {
         pollingRateJob?.cancel()
         pollingRateJob = null

         executor.shutdownNow()
         sendDataTask?.cancel(true)
         sendDataTask = null
     }
}