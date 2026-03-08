package com.kanjilens.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class ScreenCaptureService : Service() {

    companion object {
        private const val TAG = "KanjiLens"
        const val CHANNEL_ID = "kanjilens_capture"
        const val NOTIFICATION_ID = 1
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"

        // Shared reference so the Activity can access the projection
        var captureManager: ScreenCaptureManager? = null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        // Start foreground FIRST — required before getMediaProjection on Android 14+
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "Service startForeground called")

        // Now safe to initialize MediaProjection
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0

        @Suppress("DEPRECATION")
        val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        Log.d(TAG, "Service resultCode=$resultCode, resultData=${resultData != null}, captureManager=${captureManager != null}")

        if (resultData != null) {
            try {
                val projectionManager =
                    getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val projection = projectionManager.getMediaProjection(resultCode, resultData)
                Log.d(TAG, "Service got MediaProjection: ${projection != null}")
                if (projection != null) {
                    captureManager?.setProjection(projection)
                        ?: Log.e(TAG, "captureManager is null!")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get MediaProjection", e)
            }
        } else {
            Log.e(TAG, "Missing resultCode or resultData")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        captureManager = null
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "KanjiLens screen capture active"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KanjiLens")
            .setContentText("Ready to capture")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
