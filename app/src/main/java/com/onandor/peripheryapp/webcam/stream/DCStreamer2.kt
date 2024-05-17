package com.onandor.peripheryapp.webcam.stream

import com.onandor.peripheryapp.webcam.tcp.TcpClient
import com.onandor.peripheryapp.webcam.tcp.TcpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import javax.inject.Singleton

@Singleton
class DCStreamer2 {

    enum class Event {
        CLIENT_CONNECTED,
        CLIENT_DISCONNECTED,
        PORT_IN_USE,
        CANNOT_START
    }

    private val mTcpServer: TcpServer = TcpServer(7220)
    private val mServerEventJob: Job
    private var mVideoClient: TcpClient? = null
    private var mBatteryClient: TcpClient? = null
    private var mStopped: Boolean = false

    private var sendFramesJob: Job? = null
    private var frameQueue: Queue<ByteArray> = LinkedList()

    private val mEventFlow = MutableSharedFlow<Event>()
    val eventFlow = mEventFlow.asSharedFlow()

    init {
        mServerEventJob = collectServerEvents()
        mTcpServer.start()
        sendFramesJob = CoroutineScope(Dispatchers.IO).launch { sendFrames() }
    }

    private fun collectServerEvents() = CoroutineScope(Dispatchers.IO).launch {
        mTcpServer.serverEventFlow.collect { event ->
            when (event) {
                is TcpServer.Event.ClientConnected -> {
                    if (mVideoClient == null) {
                        mVideoClient = event.client
                        emitEvent(Event.CLIENT_CONNECTED)
                    } else if (mBatteryClient == null) {
                        mBatteryClient = event.client
                        sendBattery()
                    } else {
                        event.client.close()
                    }
                }
                is TcpServer.Event.ClientDisconnected -> {
                    if (event.clientId == mVideoClient!!.id) {
                        stop()
                        emitEvent(Event.CLIENT_DISCONNECTED)
                    } else if (event.clientId == mBatteryClient!!.id) {
                        mBatteryClient = null
                    }
                }
                TcpServer.Event.PortInUse -> {
                    stop()
                    emitEvent(Event.PORT_IN_USE)
                }
                else -> {
                    stop()
                    emitEvent(Event.CANNOT_START)
                }
            }
        }
    }

    fun send(data: ByteArray) {
        mVideoClient?.send(data)
    }

    fun queueFrame(frame: ByteArray) {
        if (mStopped) {
            return
        }
        synchronized(frameQueue) {
            frameQueue.add(frame)
        }
    }

    private suspend fun sendFrames() {
        var framesAvailable: Boolean
        var frame: ByteArray
        while (true) {
            yield()
            synchronized(frameQueue) {
                if (frameQueue.isEmpty()) {
                    framesAvailable = false
                } else {
                    framesAvailable = true
                    frame = frameQueue.remove()
                    val size = ByteBuffer
                        .allocate(4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(frame.size)
                        .array()
                    mVideoClient?.send(size)
                    mVideoClient?.send(frame)
                }
            }
            if (!framesAvailable) {
                delay(10)
            }
        }
    }

    private fun sendBattery() {
        val charList = listOf('\r', '\n', '\r', '\n', '5', '0') // TODO: send correct battery
        charList.forEach { mBatteryClient?.send(it.code) }
        for (i in 0 until 122) {
            mBatteryClient?.send('Z'.code)
        }
    }

    private fun emitEvent(event: Event) = CoroutineScope(Dispatchers.IO).launch {
        mEventFlow.emit(event)
    }

    fun stop() {
        mStopped = true
        sendFramesJob?.cancel()
        mTcpServer.stop()
    }
}