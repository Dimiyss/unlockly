package com.antigravity.unlockly.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.unlockly.UnlocklyApplication
import com.antigravity.unlockly.ui.onboarding.PermissionItem
import com.antigravity.unlockly.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val healthMonitor = remember { UnlocklyApplication.instance.healthMonitor }
    var healthStatus by remember { mutableStateOf(healthMonitor.checkHealth()) }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Permission Health", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "System Diagnostics",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryIndigo)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ensure all required background services and permissions are granted for reliable enforcement.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )

            Spacer(modifier = Modifier.height(20.dp))

            PermissionItem(
                title = "Usage Access",
                description = "Used to detect when productive or distracting apps are opened.",
                isGranted = healthStatus.usageAccessGranted,
                onGrant = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                title = "Accessibility Service",
                description = "Measures active screen touches/scrolls and window changes.",
                isGranted = healthStatus.accessibilityGranted,
                onGrant = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                title = "Draw Over Other Apps",
                description = "Displays full-screen blocker when social wallet is exhausted.",
                isGranted = healthStatus.overlayGranted,
                onGrant = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                title = "Disable Battery Optimization",
                description = "Prevents Android OS from killing tracking service in background.",
                isGranted = healthStatus.batteryOptimized,
                onGrant = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = { healthStatus = healthMonitor.checkHealth() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryIndigo)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Re-check System Health", fontWeight = FontWeight.Bold)
            }
        }
    }
}
