package com.arhiplabs.unstuckly.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.arhiplabs.unstuckly.R
import com.arhiplabs.unstuckly.UnstucklyApplication
import kotlinx.coroutines.*

class AppTrackingForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val foregroundTracker by lazy { ForegroundTracker(this) }
    private val powerManager by lazy { getSystemService(Context.POWER_SERVICE) as PowerManager }

    private var trackingJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        startTrackingLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startTrackingLoop() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            val app = UnstucklyApplication.instance
            while (isActive) {
                try {
                    val isInteractive = powerManager.isInteractive
                    var currentPkg = InteractionTrackerService.currentPackage.value
                    if (currentPkg.isEmpty()) {
                        currentPkg = foregroundTracker.getForegroundPackageName()
                    }

                    if (currentPkg.isNotEmpty()) {
                        app.earnSessionManager.tick(currentPkg, isInteractive)
                        app.blockingCoordinator.evaluateForegroundPackage(currentPkg)
                    }

                    // Check for blocked apps playing in Picture-in-Picture (PiP)
                    val pipPkg = InteractionTrackerService.instance?.detectPipPackage()
                    if (!pipPkg.isNullOrEmpty()) {
                        app.blockingCoordinator.evaluatePipPackage(pipPkg)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingJob?.cancel()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Unlockly App Usage Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors productive time and enforces social limits."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Unlockly Active")
            .setContentText("Tracking productive study sessions & social limits.")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "unlockly_tracking_channel"

        fun startService(context: Context) {
            val intent = Intent(context, AppTrackingForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
