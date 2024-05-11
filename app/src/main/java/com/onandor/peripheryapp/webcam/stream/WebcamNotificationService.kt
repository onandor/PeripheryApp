package com.onandor.peripheryapp.webcam.stream

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.onandor.peripheryapp.R

class WebcamNotificationService() : Service() {

    companion object {
        private const val NOTIFICATION_ID = 0x64
        private const val NOTIFICATION_CHANNEL_NAME = "All notifications"
        private const val NOTIFICATION_CHANNEL_ID = "PeripheryAppNotification"
        private const val ACTION_START = "com.onandor.peripheryapp.WebcamNotificationService.START"

        fun buildIntent(): Intent {
            return Intent(ACTION_START)
        }
    }

    private var notificationManager: NotificationManager? = null

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
            startForeground()
        }
        return START_STICKY
    }

    private fun startForeground() {
        try {
            val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setLocalOnly(true)
                .setOngoing(true)
                .setSmallIcon(applicationInfo.icon)
                .setContentTitle("PeripheryApp")
                .setContentText("Webcam is running.")
                .build()
            ServiceCompat.startForeground(
                /* service = */ this,
                /* id = */ NOTIFICATION_ID,
                /* notification = */ notification,
                /* foregroundServiceType = */
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
                } else {
                    0
                }
            )
        } catch (_: Exception) {}
    }
}