package com.onandor.peripheryapp.webcam.video.streamers

import com.onandor.peripheryapp.utils.AssetLoader
import com.onandor.peripheryapp.webcam.network.TcpClient
import com.onandor.peripheryapp.webcam.network.TcpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.Queue
import kotlin.text.StringBuilder

class WebStreamer(
    private val tcpServer: TcpServer,
    private val assetLoader: AssetLoader
) : IStreamer {

    companion object {
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
                        // mClient should be the client that sends the /video request
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
                mVideoClient = client
                sendAsset(mVideoClient!!, Assets.INDEX_PAGE, ContentTypes.HTML)
            }
            Requests.VIDEO -> {
                sendAsset(client, Assets.TEST_IMAGE, ContentTypes.JPEG)
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
        client.send(createHtmlHeader(asset.size, contentType))
        client.send(asset)
    }

    private fun createHtmlHeader(size: Int, contentType: String): ByteArray {
        val headerBuilder = StringBuilder()
        headerBuilder.appendLine("HTTP/1.1 200 OK")
        headerBuilder.appendLine("Content-Type: $contentType")
        headerBuilder.append("Content-Length: $size")
        headerBuilder.append("\r\n\r\n")
        return headerBuilder.toString().toByteArray(Charsets.UTF_8)
    }

    override fun queueFrame(frame: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    private fun emitEvent(event: StreamerEvent) = CoroutineScope(Dispatchers.IO).launch {
        mEventFlow.emit(event)
    }
}