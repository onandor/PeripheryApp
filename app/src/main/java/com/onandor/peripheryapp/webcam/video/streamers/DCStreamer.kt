package com.onandor.peripheryapp.webcam.video.streamers

import com.onandor.peripheryapp.webcam.network.TcpClient
import com.onandor.peripheryapp.webcam.network.TcpServer
import com.onandor.peripheryapp.webcam.video.Utils.Companion.to2ByteArray
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

class DCStreamer(private val tcpServer: TcpServer, width: Int, height: Int): IStreamer {

    private val mServerEventJob: Job
    private var mVideoClient: TcpClient? = null
    private var mBatteryClient: TcpClient? = null
    private var mStopped: Boolean = false
    private val initializationBytes: ByteArray

    private var mSendFramesJob: Job? = null
    private val mFrameQueue: Queue<ByteArray> = LinkedList()

    private val mEventFlow = MutableSharedFlow<StreamerEvent>()
    override val eventFlow = mEventFlow.asSharedFlow()

    init {
        val widthBytes = width.to2ByteArray()
        val heightBytes = height.to2ByteArray()
        initializationBytes = listOf(
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

        mServerEventJob = collectServerEvents()
    }

    private fun collectServerEvents() = CoroutineScope(Dispatchers.IO).launch {
        tcpServer.eventFlow.collect { event ->
            when (event) {
                is TcpServer.Event.ClientConnected -> {
                    if (mVideoClient == null) {
                        // Initialization bytes are queued first, sending them before sending
                        // frames begins
                        event.client.send(initializationBytes)
                        mVideoClient = event.client
                        mVideoClient!!.readInput(this@DCStreamer::onInput)
                    } else if (mBatteryClient == null) {
                        mBatteryClient = event.client
                        sendBattery()
                    } else {
                        event.client.close()
                    }
                }
                is TcpServer.Event.ClientDisconnected -> {
                    if (event.clientId == mVideoClient?.id) {
                        emitEvent(StreamerEvent.CLIENT_DISCONNECTED)
                    } else if (event.clientId == mBatteryClient?.id) {
                        mBatteryClient = null
                    }
                }
                TcpServer.Event.ClientCannotConnect -> {}
                else -> { emitEvent(StreamerEvent.CANNOT_START) }
            }
        }
    }

    private fun onInput(client: TcpClient, input: String) { println(input) }

    override fun queueFrame(frame: ByteArray) {
        if (mStopped) {
            return
        }
        synchronized(mFrameQueue) {
            mFrameQueue.add(frame)
        }
    }

    private suspend fun sendFrames() {
        var frameAvailable: Boolean
        var frame: ByteArray
        while (true) {
            yield()
            synchronized(mFrameQueue) {
                if (mFrameQueue.isEmpty() || mVideoClient == null) {
                    frameAvailable = false
                } else {
                    frameAvailable = true
                    frame = mFrameQueue.remove()
                    val size = ByteBuffer
                        .allocate(4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(frame.size)
                        .array()
                    mVideoClient!!.send(size)
                    mVideoClient!!.send(frame)
                }
            }
            if (!frameAvailable) {
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

    private fun emitEvent(event: StreamerEvent) = CoroutineScope(Dispatchers.IO).launch {
        mEventFlow.emit(event)
    }

    override fun start() {
        mSendFramesJob = CoroutineScope(Dispatchers.IO).launch { sendFrames() }
    }

    override fun stop() {
        mStopped = true
        mServerEventJob.cancel()
        mSendFramesJob?.cancel()
        mFrameQueue.clear()
        tcpServer.stop()
    }
}