package com.onandor.peripheryapp.webcam.video.streamers

import com.onandor.peripheryapp.webcam.network.TcpClient
import com.onandor.peripheryapp.webcam.network.TcpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue

class ClientStreamer(private val tcpServer: TcpServer): IStreamer {

    private val mServerEventJob: Job
    private var mClient: TcpClient? = null
    private var mStopped: Boolean = false

    private var mSendFramesJob: Job? = null
    private val mFrameQueue: Queue<ByteArray> = LinkedList()

    private val mEventFlow = MutableSharedFlow<StreamerEvent>()
    override val eventFlow = mEventFlow.asSharedFlow()

    init {
        mServerEventJob = collectServerEvents()
    }

    private fun collectServerEvents() = CoroutineScope(Dispatchers.IO).launch {
        tcpServer.eventFlow.collect { event ->
            when (event) {
                is TcpServer.Event.ClientConnected -> {
                    if (mClient == null) {
                        mClient = event.client
                        mClient!!.readInput { _,_: String -> }
                    } else {
                        event.client.close()
                    }
                }
                is TcpServer.Event.ClientDisconnected -> {
                    if (mClient?.id == event.clientId) {
                        emitEvent(StreamerEvent.CLIENT_DISCONNECTED)
                    }
                }
                else -> { emitEvent(StreamerEvent.CANNOT_START) }
            }
        }
    }

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
            synchronized(mFrameQueue) {
                if (mFrameQueue.isEmpty()) {
                    frameAvailable = false
                } else {
                    frameAvailable = true
                    frame = mFrameQueue.remove()
                    val size = ByteBuffer
                        .allocate(4)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .putInt(frame.size)
                        .array()
                    mClient?.send(size)
                    mClient?.send(frame)
                }
            }
            if (!frameAvailable) {
                delay(10)
            }
        }
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

    private fun emitEvent(event: StreamerEvent) = CoroutineScope(Dispatchers.IO).launch {
        mEventFlow.emit(event)
    }
}