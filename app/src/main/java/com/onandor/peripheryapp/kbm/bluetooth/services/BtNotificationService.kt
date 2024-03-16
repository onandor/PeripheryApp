package com.onandor.peripheryapp.kbm.bluetooth.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothProfile
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.onandor.peripheryapp.R
import java.lang.Exception

class BtNotificationService() : Service() {

    private var notificationManager: NotificationManager? = null

    companion object {

        private const val NOTIFICATION_ID = 0x64
        private const val NOTIFICATION_CHANNEL_NAME = "All notifications"
        private const val NOTIFICATION_CHANNEL_ID = "PeripheryAppNotification"
        private const val ACTION_START = "com.onandor.peripheryapp.BtNotificationService.START"
        private const val EXTRA_DEVICE_NAME = "EXTRA_DEVICE_NAME"
        private const val EXTRA_CONNECTION_STATE = "EXTRA_CONNECTION_STATE"

        fun buildIntent(deviceName: String?, connectionState: Int): Intent {
            return Intent(ACTION_START)
                .putExtra(EXTRA_DEVICE_NAME, deviceName)
                .putExtra(EXTRA_CONNECTION_STATE, connectionState)
        }
    }

    private val notificationChannel = NotificationChannel(
        /* id = */ NOTIFICATION_CHANNEL_ID,
        /* name = */ NOTIFICATION_CHANNEL_NAME,
        /* importance = */ NotificationManager.IMPORTANCE_LOW
    )

    override fun onBind(intent: Intent?): IBinder? { return null }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(notificationChannel)
    }

    override fun onDestroy() {
        stopSelf()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && intent.action == ACTION_START) {
            val deviceName = intent.getStringExtra(EXTRA_DEVICE_NAME)
                ?: resources.getString(R.string.bt_device_name_unavailable)
            val connectionState =
                intent.getIntExtra(EXTRA_CONNECTION_STATE, BluetoothProfile.STATE_DISCONNECTED)
            startForeground(deviceName, connectionState)
        }
        return START_STICKY
    }

    private fun checkBluetoothPermission(): Boolean {
        val bluetoothPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat
                .checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        return bluetoothPermissionGranted
    }

    private fun getConnectionStateString(state: Int): String {
        val stateMap = mapOf(
            BluetoothProfile.STATE_CONNECTED to resources.getString(R.string.bt_state_connected_to),
            BluetoothProfile.STATE_CONNECTING to resources.getString(R.string.bt_state_connecting_to),
            BluetoothProfile.STATE_DISCONNECTING to resources.getString(R.string.bt_state_disconnecting_from),
            BluetoothProfile.STATE_DISCONNECTED to resources.getString(R.string.bt_state_ready_to_connect)
        )
        return stateMap[state] ?: resources.getString(R.string.bt_state_ready_to_connect)
    }

    private fun startForeground(deviceName: String, connectionState: Int) {
        if (!checkBluetoothPermission() || notificationManager == null) {
            stopSelf()
            return
        }
        val connectionStateString = getConnectionStateString(connectionState)
        val text = if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
            connectionStateString
        } else {
            "$connectionStateString $deviceName"
        }

        try {
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setLocalOnly(true)
                .setOngoing(true)
                .setSmallIcon(applicationInfo.icon)
                .setContentTitle("PeripheryApp")
                .setContentText(text)
                .build()
            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ NOTIFICATION_ID,
                /* notification = */ notification,
                /* foregroundServiceType = */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                }
            )
        } catch (_: Exception) {}
    }
}