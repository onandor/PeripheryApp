package com.onandor.peripheryapp.webcam.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.BindException
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton

@Singleton
class DCStreamer {

    class TcpClient(private val socket: Socket) {

        companion object {
            const val CLIENT_DISCONNECTED = 0
        }

        private val inputStream = DataInputStream(socket.getInputStream())
        private val outputStream = DataOutputStream(socket.getOutputStream())
        private var inputJob: Job? = null

        private val _eventFlow = MutableSharedFlow<Int>()
        val eventFlow = _eventFlow.asSharedFlow()

        init {
            createInputJob()
            sendInitialization()
        }

        private fun createInputJob() {
            inputJob = CoroutineScope(Dispatchers.IO).launch {
                while (this.isActive) {
                    yield()
                    try {
                        val received = inputStream.read()
                        if (received == -1) {
                            emitEvent(CLIENT_DISCONNECTED)
                            stop()
                        }
                    } catch (e: SocketException) {
                        emitEvent(CLIENT_DISCONNECTED)
                        stop()
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

        private fun emitEvent(event: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                _eventFlow.emit(event)
            }
        }
    }

    enum class ConnectionEvent {
        CLIENT_CONNECTED,
        CLIENT_DISCONNECTED,
        CONNECTION_LOST,
        PORT_IN_USE
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
    private var clientEventJob: Job? = null

    private val runnable = Runnable {
        try {
            serverSocket = ServerSocket(PORT)
            while (working.get()) {
                if (serverSocket == null) {
                    return@Runnable
                }
                val socket = serverSocket!!.accept()
                if (client != null) {
                    batterySocket = socket
                    sendBattery(socket) // TODO: send battery later when asked
                    continue
                } else {
                    client = TcpClient(socket)
                    sendDataJob = CoroutineScope(Dispatchers.IO).launch {
                        sendData()
                    }
                    clientEventJob = collectClientEvents()
                    emitEvent(ConnectionEvent.CLIENT_CONNECTED)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            disconnect()
        } catch (e: BindException) {
            emitEvent(ConnectionEvent.PORT_IN_USE)
            disconnect()
        }
        disconnect()
    }

    private fun collectClientEvents() = CoroutineScope(Dispatchers.IO).launch {
        client!!.eventFlow.collect { event ->
            if (event == TcpClient.CLIENT_DISCONNECTED) {
                emitEvent(ConnectionEvent.CLIENT_DISCONNECTED)
                disconnect()
            }
        }
    }

    private fun sendBattery(socket: Socket) {
        val dos = socket.getOutputStream()
        val charList = listOf('\r', '\n', '\r', '\n', '5', '0') // TODO: send correct battery
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
                        emitEvent(ConnectionEvent.CONNECTION_LOST)
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
        clientEventJob?.cancel()
        clientEventJob = null
        client?.stop()
        client = null
    }

    private fun emitEvent(event: ConnectionEvent) {
        CoroutineScope(Dispatchers.IO).launch {
            _connectionEventFlow.emit(event)
        }
    }
}