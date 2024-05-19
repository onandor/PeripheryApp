package com.onandor.peripheryapp.webcam.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.BindException
import java.net.ServerSocket
import java.net.SocketException
import javax.inject.Singleton

@Singleton
class TcpServer {

    sealed interface Event {
        data object PortInUse: Event
        data object CannotStart: Event
        data object ClientCannotConnect: Event
        data object Stopped: Event
        data class ClientConnected(val client: TcpClient): Event
        data class ClientDisconnected(val clientId: Int): Event
    }

    companion object {
        const val PORT = 7220
    }

    private var mServerSocket: ServerSocket? = null
    private val mClients: MutableList<TcpClient> = mutableListOf()
    private var mNextClientId: Int = 0
    private var mRunning = false

    private var mListenerThread: Thread? = null

    private val mEventFlow = MutableSharedFlow<Event>(replay = 10)
    val eventFlow = mEventFlow.asSharedFlow()

    private val mListenerRunnable = Runnable {
        while (mRunning) {
            if (mServerSocket == null) {
                mListenerThread!!.interrupt()
            }
            try {
                val socket = mServerSocket!!.accept()
                val client = TcpClient(
                    id = mNextClientId++,
                    socket = socket,
                    onDisconnected = this@TcpServer::onClientDisconnected
                )
                synchronized(mClients) {
                    mClients.add(client)
                }
                emitEvent(Event.ClientConnected(client))
            } catch (e: SocketException) {
                if (e.message != "Socket closed") {
                    emitEvent(Event.ClientCannotConnect)
                }
            } catch (e: IOException) {
                emitEvent(Event.ClientCannotConnect)
            } catch (_: InterruptedException) {}
        }
    }

    fun start() {
        if (mRunning) {
            return
        }
        try {
            mServerSocket = ServerSocket(PORT)
            mRunning = true
            mListenerThread = Thread(mListenerRunnable).apply { start() }
        } catch (e: BindException) {
            emitEvent(Event.PortInUse)
        } catch (e: IOException) {
            emitEvent(Event.CannotStart)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun stop() {
        if (!mRunning) {
            return
        }
        emitEvent(Event.Stopped)
        mRunning = false
        mListenerThread?.interrupt()
        mListenerThread = null
        synchronized(mClients) {
            mClients.forEach { it.close() }
            mClients.clear()
        }
        mEventFlow.resetReplayCache()
        mServerSocket?.close()
        mServerSocket = null
    }

    private fun onClientDisconnected(id: Int) {
        synchronized(mClients) {
            mClients.removeIf { it.id == id }
        }
        emitEvent(Event.ClientDisconnected(id))
    }

    private fun emitEvent(event: Event) {
        if (!mRunning) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            mEventFlow.emit(event)
        }
    }
}