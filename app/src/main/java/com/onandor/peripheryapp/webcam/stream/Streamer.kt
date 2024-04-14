package com.onandor.peripheryapp.webcam.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.CompletableFuture
import javax.inject.Singleton

@Singleton
class Streamer {

    private var address: InetAddress? = null
    private var port: Int = 0
    private var sendDataJob: Job? = null
    private var dataQueue: Queue<ByteArray> = LinkedList()

    private var socket: Socket? = null
    private var dos: DataOutputStream? = null

    private val _connectionEventFlow = MutableSharedFlow<ConnectionEvent>()
    val connectionEventFlow = _connectionEventFlow.asSharedFlow()

    enum class ConnectionEvent {
        CONNECTION_SUCCESS,
        UNKNOWN_HOST_FAILURE,
        TIMEOUT_FAILURE,
        HOST_UNREACHABLE_FAILURE
    }

    fun connect(ipAddress: String, port: Int): CompletableFuture<ConnectionEvent> {
        return CompletableFuture.supplyAsync {
            try {
                address = InetAddress.getByName(ipAddress)
                this@Streamer.port = port

                socket = Socket(ipAddress, port)
                dos = DataOutputStream(socket?.getOutputStream())
                ConnectionEvent.CONNECTION_SUCCESS
            } catch (e: ConnectException) {
                ConnectionEvent.TIMEOUT_FAILURE
            } catch (e: UnknownHostException) {
                ConnectionEvent.UNKNOWN_HOST_FAILURE
            }
        }
    }

    fun disconnect() {
        stopStream()
        dos?.close()
    }

    fun startStream() {
        sendDataJob = CoroutineScope(Dispatchers.IO).launch {
            sendData()
        }
    }

    private fun stopStream() {
        sendDataJob?.cancel()
        sendDataJob = null
        dataQueue.clear()
    }

    private suspend fun sendData() {
        var dataAvailable: Boolean
        var data = ByteArray(0)
        while (true) {
            synchronized(dataQueue) {
                if (dataQueue.isEmpty()) {
                    dataAvailable = false
                } else {
                    data = dataQueue.remove()
                    dataAvailable = true
                }

                if (dataAvailable) {
                    try {
                        dos?.write(data.size.to2ByteArray())
                        dos?.write(data)
                    } catch (e: IOException) {
                        CoroutineScope(Dispatchers.IO).launch {
                            _connectionEventFlow.emit(ConnectionEvent.HOST_UNREACHABLE_FAILURE)
                        }
                        stopStream()
                    }
                }
            }
            if (!dataAvailable) {
                delay(10)
            }
        }
    }

    private fun printBytes(bytes: ByteArray) {
        for (byte in bytes) {
            for (i in 0 .. 7) {
                print((byte.toInt() shr i) and 1)
            }
            print(" ")
        }
        println()
    }

    private fun Int.to2ByteArray() : ByteArray = byteArrayOf(shr(8).toByte(), toByte())

    fun queueData(data: ByteArray) {
        synchronized(dataQueue) {
            dataQueue.add(data)
        }
    }
}