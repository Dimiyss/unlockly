package com.arhiplabs.unstuckly.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

class ForegroundTracker(private val context: Context) {

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    private var lastKnownPackage: String = ""

    fun getForegroundPackageName(): String {
        val usm = usageStatsManager ?: return lastKnownPackage
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 60_000, now)

        var latestEventTime = 0L
        var currentForeground = ""

        if (events != null) {
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    val pkg = event.packageName ?: ""
                    if (!InteractionTrackerService.isIgnoredPackage(pkg) && event.timeStamp >= latestEventTime) {
                        latestEventTime = event.timeStamp
                        currentForeground = pkg
                    }
                }
            }
        }

        // Additional fallback using queryUsageStats if no recent resume event was in the window
        if (currentForeground.isEmpty()) {
            try {
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 60_000, now)
                val mostRecent = stats
                    ?.filter { !InteractionTrackerService.isIgnoredPackage(it.packageName) }
                    ?.maxByOrNull { it.lastTimeUsed }
                if (mostRecent != null && mostRecent.lastTimeUsed > 0) {
                    currentForeground = mostRecent.packageName
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (currentForeground.isNotEmpty()) {
            lastKnownPackage = currentForeground
        }
        return lastKnownPackage
    }
}
