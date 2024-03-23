package com.onandor.peripheryapp.webcam.stream

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.UnknownHostException
import java.util.LinkedList
import java.util.Queue

class Streamer {

    private var udpSocket = DatagramSocket()
    private var address: InetAddress? = null
    private var port: Int = 0
    private var sendDataJob: Job? = null
    private var dataQueue: Queue<ByteArray> = LinkedList()

    fun startStream(ipAddress: String, port: Int) {
        sendDataJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                address = InetAddress.getByName(ipAddress)
                this@Streamer.port = port
            } catch (e: SocketException) {
                e.printStackTrace()
            } catch (e: UnknownHostException) {
                e.printStackTrace()
            }
            sendData()
        }
    }

    fun stopStream() {
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
                        udpSocket.send(DatagramPacket(data, data.size, address, port))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            if (!dataAvailable) {
                delay(10)
            }
        }
    }

    fun queueData(data: ByteArray) {
        synchronized(dataQueue) {
            dataQueue.add(data)
        }
    }
}