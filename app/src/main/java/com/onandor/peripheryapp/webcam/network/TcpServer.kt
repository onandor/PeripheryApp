package com.onandor.peripheryapp.webcam.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.BindException
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Singleton

@Singleton
class TcpServer {

    sealed interface Event {
        data object PortInUse: Event
        data object CannotStart: Event
        data object ClientCannotConnect: Event
        data class ClientConnected(val client: TcpClient): Event
        data class ClientDisconnected(val clientId: Int): Event
    }

    companion object {
        const val PORT = 7220
    }

    private var mServerSocket: ServerSocket? = null
    private val mRunning = AtomicBoolean(false)
    private val mClients: MutableList<TcpClient> = mutableListOf()
    private var mNextClientId: Int = 0

    private val mEventFlow = MutableSharedFlow<Event>(replay = 10)
    val eventFlow = mEventFlow.asSharedFlow()

    private val listener = Runnable {
        while (mRunning.get()) {
            if (mServerSocket == null) {
                return@Runnable
            }
            try {
                val socket = mServerSocket!!.accept()
                val client = TcpClient(mNextClientId++, socket, this::onClientDisconnected)
                synchronized(mClients) {
                    mClients.add(client)
                }
                emitEvent(Event.ClientConnected(client))
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
            mServerSocket = ServerSocket(PORT)
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
        reset()
        mServerSocket?.close()
        mServerSocket = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun reset() {
        synchronized(mClients) {
            mClients.forEach { it.close() }
            mClients.clear()
        }
        mEventFlow.resetReplayCache()
    }

    private fun onClientDisconnected(id: Int) {
        synchronized(mClients) {
            mClients.removeIf { it.id == id }
        }
        emitEvent(Event.ClientDisconnected(id))
    }

    private fun emitEvent(event: Event) {
        CoroutineScope(Dispatchers.IO).launch {
            mEventFlow.emit(event)
        }
    }
}