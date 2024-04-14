package com.onandor.peripheryapp.webcam.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.DataOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.net.UnknownHostException
import java.util.LinkedList
import java.util.Queue

class Streamer {

    private var address: InetAddress? = null
    private var port: Int = 0
    private var sendDataJob: Job? = null
    private var dataQueue: Queue<ByteArray> = LinkedList()

    private var socket: Socket? = null
    private var dos: DataOutputStream? = null

    fun startStream(ipAddress: String, port: Int) {
        sendDataJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                address = InetAddress.getByName(ipAddress)
                this@Streamer.port = port

                socket = Socket(ipAddress, port)
                dos = DataOutputStream(socket?.getOutputStream())
            } catch (e: SocketException) {
                // TODO
                e.printStackTrace()
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            } catch (e: ConnectException) {
                e.printStackTrace()
            }
            sendData()
        }
    }

    fun stopStream() {
        dos?.close()
        sendDataJob?.cancel()
        sendDataJob = null
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
                        e.printStackTrace()
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