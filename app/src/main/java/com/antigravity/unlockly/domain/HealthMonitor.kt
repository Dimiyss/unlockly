package com.antigravity.unlockly.domain

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.provider.Settings
import com.antigravity.unlockly.data.model.HealthStatus
import com.antigravity.unlockly.service.InteractionTrackerService

class HealthMonitor(private val context: Context) {

    fun checkHealth(): HealthStatus {
        val usageAccess = isUsageAccessGranted()
        val accessibility = isAccessibilityGranted()
        val overlay = isOverlayGranted()
        val batteryOptimized = isBatteryOptimizationIgnored()

        return HealthStatus(
            usageAccessGranted = usageAccess,
            accessibilityGranted = accessibility,
            overlayGranted = overlay,
            batteryOptimized = batteryOptimized
        )
    }

    private fun isUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun isAccessibilityGranted(): Boolean {
        return InteractionTrackerService.isServiceActive.value
    }

    private fun isOverlayGranted(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
