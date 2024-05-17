package com.onandor.peripheryapp.webcam.tcp

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.BindException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton

class TcpServer(val port: Int) {

    sealed interface Event {
        data object PortInUse: Event
        data object CannotStart: Event
        data object ClientCannotConnect: Event
        data class ClientConnected(val client: TcpClient): Event
        data class ClientDisconnected(val clientId: Int): Event
    }

    private var mServerSocket: ServerSocket? = null
    private val mRunning = AtomicBoolean(false)
    private val mClients: MutableList<TcpClient> = mutableListOf()
    private var mNextClientId: Int = 0

    private val mServerEventFlow = MutableSharedFlow<Event>()
    val serverEventFlow = mServerEventFlow.asSharedFlow()

    private val listener = Runnable {
        while (mRunning.get()) {
            if (mServerSocket == null) {
                return@Runnable
            }
            try {
                val socket = mServerSocket!!.accept()
                synchronized(mClients) {
                    val client = TcpClient(mNextClientId++, socket, this::onClientDisconnected)
                    mClients.add(client)
                    emitEvent(Event.ClientConnected(client))
                }
            } catch (e: IOException) {
                emitEvent(Event.ClientCannotConnect)
            }
        }
    }

    fun start() {
        if (mRunning.get()) {
            return
        }
        try {
            mServerSocket = ServerSocket(port)
            mRunning.set(true)
            Thread(listener).start()
        } catch (e: BindException) {
            emitEvent(Event.PortInUse)
        } catch (e: IOException) {
            emitEvent(Event.CannotStart)
        }
    }

    fun stop() {
        mRunning.set(false)
        mClients.forEach { it.close() }
        mClients.clear()
        mServerSocket?.close()
        mServerSocket = null
    }

    fun broadcast(data: ByteArray) {
        mClients.forEach { it.send(data) }
    }

    private fun onClientDisconnected(id: Int) {
        mClients.removeIf { it.id == id }
        emitEvent(Event.ClientDisconnected(id))
    }

    private fun emitEvent(event: Event) {
        CoroutineScope(Dispatchers.IO).launch {
            mServerEventFlow.emit(event)
        }
    }
}