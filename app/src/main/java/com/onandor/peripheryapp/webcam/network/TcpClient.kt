package com.onandor.peripheryapp.webcam.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.Socket
import java.util.Scanner

class TcpClient(
    val id: Int,
    private val socket: Socket,
    private val onDisconnected: (Int) -> Unit
) {

    private val mInputStream = DataInputStream(socket.getInputStream())
    private val mOutputStream = DataOutputStream(socket.getOutputStream())
    private val mScanner = Scanner(mInputStream)
    private var mInputJob: Job? = null

    @JvmName("readInputStrings")
    fun readInput(callback: (TcpClient, String) -> Unit) {
        if (mInputJob != null) {
            return
        }
        mInputJob = CoroutineScope(Dispatchers.IO).launch {
            while (mScanner.hasNextLine()) {
                yield()
                callback(this@TcpClient, mScanner.nextLine())
            }
            close()
            onDisconnected(id)
        }
    }

    @JvmName("readInputBytes")
    fun readInput(callback: (TcpClient, Byte) -> Unit) {
        if (mInputJob != null) {
            return
        }
        mInputJob = CoroutineScope(Dispatchers.IO).launch {
            while (mScanner.hasNext()) {
                yield()
                callback(this@TcpClient, mScanner.nextByte())
            }
            close()
            onDisconnected(id)
        }
    }

    fun send(data: ByteArray) {
        try {
            mOutputStream.write(data)
            mOutputStream.flush()
        } catch (e: IOException) {
            close()
            onDisconnected(id)
        }
    }

    fun send(data: Int) {
        try {
            mOutputStream.write(data)
        } catch (e: IOException) {
            close()
            onDisconnected(id)
        }
    }

    fun close() {
        mInputJob?.cancel()
        mInputStream.close()
        mOutputStream.close()
        mScanner.close()
        socket.close()
    }
}