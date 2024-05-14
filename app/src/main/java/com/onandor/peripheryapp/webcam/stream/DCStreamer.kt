package com.onandor.peripheryapp.webcam.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton

@Singleton
class DCStreamer {

    class TcpClient(private val socket: Socket) {

        private val inputStream = DataInputStream(socket.getInputStream())
        private val outputStream = DataOutputStream(socket.getOutputStream())
        private var inputJob: Job? = null

        init {
            createInputJob()
            sendInitialization()
        }

        private fun createInputJob() {
            inputJob = CoroutineScope(Dispatchers.IO).launch {
                var received: Int
                val outputBuffer = ByteArrayOutputStream()
                while (this.isActive) {
                    yield()
                    try {
                        received = inputStream.read()
                        if (received == -1) {
                            stop()
                        }
                        outputBuffer.write(received)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        stop()
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                        stop()
                    }
                }
            }
        }

        private fun sendInitialization() {
            val initialBytes = listOf(0x02, 0x80, 0x01, 0xe0, 0x21, 0xf5, 0xe8, 0x7f, 0x30)
            initialBytes.forEach { outputStream.write(it) }
        }

        fun stop() {
            try {
                inputStream.close()
                outputStream.close()
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                inputJob?.cancel()
                inputJob = null
            }
        }

        fun sendData(data: ByteArray) {
            outputStream.write(data)
        }
    }

    enum class ConnectionEvent {
        CLIENT_CONNECTED,
        CLIENT_DISCONNECTED,
        CONNECTION_LOST
    }

    companion object {
        const val PORT = 7220
    }

    private val _connectionEventFlow = MutableSharedFlow<ConnectionEvent>()
    val connectionEventFlow = _connectionEventFlow.asSharedFlow()

    private var serverSocket: ServerSocket? = null
    private val working = AtomicBoolean(true)
    private var client: TcpClient? = null
    private var batterySocket: Socket? = null

    private var sendDataJob: Job? = null
    private var dataQueue: Queue<ByteArray> = LinkedList()

    private val runnable = Runnable {
        try {
            serverSocket = ServerSocket(PORT)
            while (working.get()) {
                if (serverSocket == null) {
                    return@Runnable
                }
                val socket = serverSocket!!.accept()
                if (client != null) {
                    //socket.close()
                    batterySocket = socket
                    sendBattery(socket)
                    continue
                } else {
                    client = TcpClient(socket)
                    println("client connected")
                    sendDataJob = CoroutineScope(Dispatchers.IO).launch {
                        sendData()
                    }
                    sendEvent(ConnectionEvent.CLIENT_CONNECTED)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            disconnect()
        } catch (e: BindException) {
            e.printStackTrace() // TODO: address in use
            disconnect()
        }
        disconnect()
    }

    private fun sendBattery(socket: Socket) {
        val dos = socket.getOutputStream()
        val charList = listOf('\r', '\n', '\r', '\n', '5', '0')
        charList.forEach { dos.write(it.code) }
        for (i in 0 until 122) {
            dos.write('Z'.code)
        }
    }

    private suspend fun sendData() {
        var dataAvailable: Boolean
        var data = ByteArray(0)
        while (true) {
            yield()
            synchronized(dataQueue) {
                if (dataQueue.isEmpty()) {
                    dataAvailable = false
                } else {
                    data = dataQueue.remove()
                    dataAvailable = true
                }

                if (dataAvailable) {
                    try {
                        val size = ByteBuffer
                            .allocate(4)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putInt(data.size)
                            .array()

                        client?.sendData(size)
                        client?.sendData(data)
                    } catch (e: IOException) {
                        CoroutineScope(Dispatchers.IO).launch {
                            // TODO
                            //_connectionEventFlow.emit(Streamer.ConnectionEvent.HOST_UNREACHABLE_FAILURE)
                        }
                        disconnect()
                    }
                }
            }
            if (!dataAvailable) {
                delay(10)
            }
        }
    }

    fun queueData(data: ByteArray) {
        if (client == null) {
            return
        }
        synchronized(dataQueue) {
            dataQueue.add(data)
        }
    }

    fun startServer() {
        working.set(true)
        Thread(runnable).start()
    }

    fun stopServer() {
        disconnect()
        working.set(false)
        dataQueue.clear()
        serverSocket?.close()
        serverSocket = null
    }

    fun disconnect() {
        sendDataJob?.cancel()
        sendDataJob = null
        client?.stop()
        client = null
    }

    private fun sendEvent(event: ConnectionEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            _connectionEventFlow.emit(event)
        }
    }
}