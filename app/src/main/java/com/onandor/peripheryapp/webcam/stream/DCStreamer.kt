package com.onandor.peripheryapp.webcam.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
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

        fun start() {
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

        fun sendData(bytes: ByteArray) {
            outputStream.write(bytes)
        }
    }

    companion object {
        fun Int.toNByteArray(n: Int): ByteArray {
            val bytes = mutableListOf<Byte>()
            for (i in n - 1 downTo 0) {
                bytes.add(shr(n * 8).toByte())
            }
            return bytes.toByteArray()
        }
    }

    private val PORT = 4747

    private var serverSocket: ServerSocket? = null
    private val working = AtomicBoolean(true)
    private var client: TcpClient? = null

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
                    socket.close()
                    continue
                }
                client = TcpClient(socket)
                client!!.start()
                sendDataJob = CoroutineScope(Dispatchers.IO).launch {
                    sendData()
                }
            }
        } catch (e1: IOException) {
            e1.printStackTrace()
            try {
                sendDataJob?.cancel()
                client?.stop()
                client = null
            } catch (e2: IOException) {
                e2.printStackTrace()
            }
        }
        sendDataJob?.cancel()
        client?.stop()
        client = null
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
                        client?.sendData(data.size.toNByteArray(4))
                        client?.sendData(data)
                    } catch (e: IOException) {
                        CoroutineScope(Dispatchers.IO).launch {
                            //_connectionEventFlow.emit(Streamer.ConnectionEvent.HOST_UNREACHABLE_FAILURE)
                        }
                        client?.stop()
                        client = null
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
        Thread(runnable).start()
    }

    fun stopServer() {
        working.set(false)
        dataQueue.clear()
    }

    private fun onDataReceived(data: ByteArray) {

    }
}