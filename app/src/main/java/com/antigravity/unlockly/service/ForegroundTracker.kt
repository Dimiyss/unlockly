package com.antigravity.unlockly.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build

class ForegroundTracker(private val context: Context) {

    private val usageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
    }

    fun getForegroundPackageName(): String {
        val usm = usageStatsManager ?: return ""
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 10_000, now) ?: return ""

        val event = UsageEvents.Event()
        var currentForeground = ""

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentForeground = event.packageName ?: ""
            }
        }
        return currentForeground
    }
}
