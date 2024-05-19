package com.onandor.peripheryapp.webcam.network

import android.os.Handler
import android.os.HandlerThread
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
import javax.inject.Singleton

@Singleton
class TcpServer {

    sealed interface Event {
        data object PortInUse: Event
        data object CannotStart: Event
        data object ClientCannotConnect: Event
        data object Paused: Event
        data class ClientConnected(val client: TcpClient): Event
        data class ClientDisconnected(val clientId: Int): Event
    }

    private enum class State {
        LISTENING,
        PAUSED,
        STOPPED
    }

    companion object {
        const val PORT = 7220
    }

    private var mServerSocket: ServerSocket? = null
    private val mClients: MutableList<TcpClient> = mutableListOf()
    private var mNextClientId: Int = 0
    private var mState = State.STOPPED

    private var mListenerThread: Thread? = null

    private val mEventFlow = MutableSharedFlow<Event>(replay = 10)
    val eventFlow = mEventFlow.asSharedFlow()

    private val mListenerRunnable = Runnable {
        while (mState == State.LISTENING) {
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
            } catch (e: IOException) {
                e.printStackTrace()
                emitEvent(Event.ClientCannotConnect)
            } catch (_: InterruptedException) {}
        }
    }

    fun start() {
        if (mState == State.LISTENING || mState == State.PAUSED) {
            return
        }
        try {
            mServerSocket = ServerSocket(PORT)
            mState = State.LISTENING
            mListenerThread = Thread(mListenerRunnable).apply { start() }
        } catch (e: BindException) {
            emitEvent(Event.PortInUse)
        } catch (e: IOException) {
            emitEvent(Event.CannotStart)
        }
    }

    fun stop() {
        mState = State.STOPPED
        reset()
        mServerSocket?.close()
        mServerSocket = null
    }

    fun resume() {
        mState = State.LISTENING
        mListenerThread = Thread(mListenerRunnable).apply { start() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun reset() {
        emitEvent(Event.Paused)
        mState = State.PAUSED
        mListenerThread!!.interrupt()
        mListenerThread = null
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
        if (mState != State.LISTENING) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            mEventFlow.emit(event)
        }
    }
}