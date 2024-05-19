package com.onandor.peripheryapp.webcam.video.streamers

import com.onandor.peripheryapp.utils.AssetLoader
import com.onandor.peripheryapp.webcam.network.TcpClient
import com.onandor.peripheryapp.webcam.network.TcpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.util.LinkedList
import java.util.Queue
import kotlin.text.StringBuilder

class IpStreamer(
    private val tcpServer: TcpServer,
    private val assetLoader: AssetLoader
) : IStreamer {

    companion object {

        private const val FRAME_BOUNDARY = "mjpegframe"

        private object Assets {
            const val CANNOT_CONNECT_PAGE = "webcam_cannot_connect.html"
            const val INDEX_PAGE = "webcam_index.html"
            const val TEST_IMAGE = "test.jpg"
        }

        private object Requests {
            const val ROOT = "/"
            const val VIDEO = "/video"
        }

        private object ContentTypes {
            const val HTML = "text/html"
            const val JPEG = "image/jpeg"
            const val MULTIPART_REPLACE = "multipart/x-mixed-replace; boundary=$FRAME_BOUNDARY"
        }
    }

    private val mServerEventJob: Job
    private var mSendFramesJob: Job? = null
    private val mFrameQueue: Queue<ByteArray> = LinkedList()
    private var mVideoClient: TcpClient? = null

    private val mEventFlow = MutableSharedFlow<StreamerEvent>()
    override val eventFlow: SharedFlow<StreamerEvent> = mEventFlow.asSharedFlow()

    init {
        mServerEventJob = collectServerEvents()
    }

    private fun collectServerEvents() = CoroutineScope(Dispatchers.IO).launch {
        tcpServer.eventFlow.collect { event ->
            when (event) {
                is TcpServer.Event.ClientConnected -> {
                    if (mVideoClient == null) {
                        event.client.readInput { client, input: String -> respond(client, input) }
                    } else {
                        sendAsset(event.client, Assets.CANNOT_CONNECT_PAGE, ContentTypes.HTML)
                    }
                }
                is TcpServer.Event.ClientDisconnected -> {
                    if (mVideoClient != null && mVideoClient?.id == event.clientId) {
                        emitEvent(StreamerEvent.CLIENT_DISCONNECTED)
                    }
                }
                else -> { emitEvent(StreamerEvent.CANNOT_START) }
            }
        }
    }

    private fun respond(client: TcpClient, input: String) {
        when (getRequest(input)) {
            Requests.ROOT -> {
                if (mVideoClient == null) {
                    sendAsset(client, Assets.INDEX_PAGE, ContentTypes.HTML)
                } else {
                    sendAsset(client, Assets.CANNOT_CONNECT_PAGE, ContentTypes.HTML)
                }
            }
            Requests.VIDEO -> {
                mVideoClient = client
                val header = createHttpHeader(0, ContentTypes.MULTIPART_REPLACE)
                mVideoClient!!.send(header)
            }
        }
    }

    private fun getRequest(input: String): String {
        val pattern = Regex("GET.*")
        if (pattern.matches(input)) {
            val inputSplit = input.split(" ")
            if (inputSplit.size < 2) {
                return ""
            }
            return inputSplit[1]
        }
        return ""
    }

    private fun sendAsset(
        client: TcpClient,
        fileName: String,
        contentType: String
    ) {
        val asset = assetLoader.loadAssetAsByteArray(fileName)
        client.send(createHttpHeader(asset.size, contentType))
        client.send(asset)
    }

    private fun createHttpHeader(
        size: Int,
        contentType: String,
        frameHeader: Boolean = false
    ): ByteArray {
        val builder = StringBuilder()
        if (frameHeader) {
            builder.appendLine("--$FRAME_BOUNDARY")
        } else {
            builder.appendLine("HTTP/1.1 200 OK")
        }
        builder.appendLine("Content-Type: $contentType")
        if (contentType != ContentTypes.MULTIPART_REPLACE) {
            builder.append("Content-Length: $size")
        }
        builder.append("\r\n\r\n")
        return builder.toString().toByteArray(Charsets.UTF_8)
    }

    override fun queueFrame(frame: ByteArray) {
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
                    val frameHeader = createHttpHeader(frame.size, ContentTypes.JPEG, true)
                    println(frame.size)
                    mVideoClient!!.send(frameHeader)
                    mVideoClient!!.send(frame)
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
        mServerEventJob.cancel()
        mSendFramesJob?.cancel()
        mFrameQueue.clear()
        tcpServer.reset()
    }

    private fun emitEvent(event: StreamerEvent) = CoroutineScope(Dispatchers.IO).launch {
        mEventFlow.emit(event)
    }
}