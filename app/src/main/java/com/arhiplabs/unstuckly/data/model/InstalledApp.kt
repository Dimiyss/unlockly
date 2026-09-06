package com.arhiplabs.unstuckly.data.model

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledApp(
    val name: String,
    val packageName: String,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false
)

object InstalledAppHelper {

    suspend fun getInstalledLauncherApps(context: Context): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val ownPackage = context.packageName

        resolveInfos
            .asSequence()
            .filter { it.activityInfo != null && it.activityInfo.packageName != ownPackage }
            .map { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val name = resolveInfo.loadLabel(pm)?.toString() ?: packageName
                val icon = try {
                    resolveInfo.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                InstalledApp(
                    name = name,
                    packageName = packageName,
                    icon = icon,
                    isSystemApp = isSystem
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.name.lowercase() }
            .toList()
    }
}
