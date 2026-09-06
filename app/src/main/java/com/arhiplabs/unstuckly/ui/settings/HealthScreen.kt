package com.arhiplabs.unstuckly.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arhiplabs.unstuckly.R
import com.arhiplabs.unstuckly.UnstucklyApplication
import com.arhiplabs.unstuckly.ui.onboarding.PermissionItem
import com.arhiplabs.unstuckly.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = remember { UnstucklyApplication.instance }
    val healthMonitor = remember { app.healthMonitor }
    var healthStatus by remember { mutableStateOf(healthMonitor.checkHealth()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.permission_health_title),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                text = stringResource(R.string.system_diagnostics_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryIndigo)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.system_diagnostics_desc),
                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )

            Spacer(modifier = Modifier.height(20.dp))

            PermissionItem(
                title = stringResource(R.string.perm_usage_access_title),
                description = stringResource(R.string.perm_usage_access_desc),
                isGranted = healthStatus.usageAccessGranted,
                onGrant = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                title = stringResource(R.string.perm_accessibility_title),
                description = stringResource(R.string.perm_accessibility_desc),
                isGranted = healthStatus.accessibilityGranted,
                onGrant = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                title = stringResource(R.string.perm_overlay_title),
                description = stringResource(R.string.perm_overlay_desc),
                isGranted = healthStatus.overlayGranted,
                onGrant = {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            PermissionItem(
                title = stringResource(R.string.perm_battery_title),
                description = stringResource(R.string.perm_battery_desc),
                isGranted = healthStatus.batteryOptimized,
                onGrant = {
                    val packageName = context.packageName
                    val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(requestIntent)
                    } catch (e1: Exception) {
                        try {
                            val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:$packageName")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(detailsIntent)
                        } catch (e2: Exception) {
                            try {
                                val ignoreSettingsIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(ignoreSettingsIntent)
                            } catch (e3: Exception) {
                                e3.printStackTrace()
                            }
                        }
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
                Text(stringResource(R.string.btn_recheck_health), fontWeight = FontWeight.Bold)
            }
        }
    }
}
