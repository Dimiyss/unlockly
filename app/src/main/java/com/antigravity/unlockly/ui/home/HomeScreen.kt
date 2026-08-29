package com.antigravity.unlockly.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.unlockly.UnlocklyApplication
import com.antigravity.unlockly.data.model.Rule
import com.antigravity.unlockly.data.model.Wallet
import com.antigravity.unlockly.service.AppTrackingForegroundService
import com.antigravity.unlockly.ui.theme.*
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 35

@Composable
fun HomeScreen(
    onNavigateToRuleEditor: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToStore: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = UnlocklyApplication.instance
    val scope = rememberCoroutineScope()

    // Start background tracking service if not already started
    LaunchedEffect(Unit) {
        AppTrackingForegroundService.startService(context)
    }

    val walletState by app.walletManager.walletFlow.collectAsState(initial = Wallet())
    val activeRulesState by app.ruleRepository.activeRules.collectAsState(initial = emptyList())
    val isAccruing by app.earnSessionManager.isAccruing.collectAsState()
    val activeProductivePackage by app.earnSessionManager.activeProductivePackage.collectAsState()
    val healthStatus = remember { app.healthMonitor.checkHealth() }

    val wallet = walletState ?: Wallet()
    val primaryRule = activeRulesState.firstOrNull()

    var showEmergencySheet by remember { mutableStateOf(false) }
    var emergencyInput by remember { mutableStateOf("") }
    var inputVisible by remember { mutableStateOf(false) }
    var emergencyError by remember { mutableStateOf<String?>(null) }

    // Password Gate for Editing Settings
    var showPasswordGateDialog by remember { mutableStateOf(false) }
    var passwordGateInput by remember { mutableStateOf("") }
    var passwordGateVisible by remember { mutableStateOf(false) }
    var passwordGateError by remember { mutableStateOf<String?>(null) }

    val handleEditRulesRequest = {
        val stored = primaryRule?.emergencyPassword
        if (stored.isNullOrBlank()) {
            onNavigateToRuleEditor()
        } else {
            passwordGateInput = ""
            passwordGateError = null
            showPasswordGateDialog = true
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Unlockly",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Earn Social Time",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Store / PRO Button
                    IconButton(onClick = onNavigateToStore) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Store & Power-Ups",
                            tint = WarningAmber
                        )
                    }

                    IconButton(onClick = onNavigateToHealth) {
                        Icon(
                            imageVector = if (healthStatus.isFullyHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Health Status",
                            tint = if (healthStatus.isFullyHealthy) SuccessGreen else WarningAmber
                        )
                    }

                    IconButton(onClick = handleEditRulesRequest) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Rules",
                            tint = PrimaryIndigo
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Live Status Pill Banner
            LiveStatusBanner(
                isAccruing = isAccruing,
                activePackage = activeProductivePackage,
                wallet = wallet
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Social Wallet Balance Card
            WalletBalanceCard(wallet = wallet)

            Spacer(modifier = Modifier.height(20.dp))

            // Today's Metrics Row
            DailyMetricsRow(earnedSeconds = wallet.earnedTodaySeconds, spentSeconds = wallet.spentTodaySeconds)

            Spacer(modifier = Modifier.height(20.dp))

            // Power-Ups & PRO Store Banner Card
            StorePromoCard(
                wallet = wallet,
                onClick = onNavigateToStore
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Active Rule Summary Card
            ActiveRuleCard(
                rule = primaryRule,
                onEditClick = handleEditRulesRequest
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Emergency Unlock Card
            Card(
                onClick = {
                    if (wallet.emergencyUnlockUsedToday) {
                        Toast.makeText(context, "Emergency unlock has already been used today. It resets at midnight.", Toast.LENGTH_LONG).show()
                    } else {
                        showEmergencySheet = true
                        emergencyError = null
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = if (wallet.emergencyUnlockUsedToday) SurfaceDark.copy(alpha = 0.6f) else SurfaceDark
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (wallet.emergencyUnlockUsedToday) SurfaceVariantDark else WarningAmber.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (wallet.emergencyUnlockUsedToday) TextMuted else WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Emergency Unlock",
                                fontWeight = FontWeight.Bold,
                                color = if (wallet.emergencyUnlockUsedToday) TextMuted else TextPrimary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (wallet.emergencyUnlockUsedToday) "Used today (resets at midnight)" else "Override limits with 35+ symbol passphrase (1x / day)",
                                color = if (wallet.emergencyUnlockUsedToday) WarningAmber else TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Password Gate Dialog for entering settings
    if (showPasswordGateDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordGateDialog = false },
            containerColor = SurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Password Required", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your emergency password to access and modify app settings / rules:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = passwordGateInput,
                        onValueChange = {
                            passwordGateInput = it
                            passwordGateError = null
                        },
                        placeholder = { Text("Enter passphrase...", color = TextMuted) },
                        visualTransformation = if (passwordGateVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordGateVisible = !passwordGateVisible }) {
                                Icon(
                                    imageVector = if (passwordGateVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = SurfaceVariantDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark
                        )
                    )

                    if (passwordGateError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = passwordGateError ?: "",
                            color = ErrorRose,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val entered = passwordGateInput.trim()
                        val stored = primaryRule?.emergencyPassword?.trim() ?: ""
                        if (entered == stored) {
                            showPasswordGateDialog = false
                            passwordGateInput = ""
                            passwordGateError = null
                            onNavigateToRuleEditor()
                        } else {
                            passwordGateError = "Incorrect password. Access denied."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Unlock Settings", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordGateDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Emergency Unlock Dialog
    if (showEmergencySheet) {
        AlertDialog(
            onDismissRequest = { showEmergencySheet = false },
            containerColor = SurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Unlock", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your 35+ character emergency passphrase to receive 15 minutes of emergency access:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = emergencyInput,
                        onValueChange = {
                            emergencyInput = it
                            emergencyError = null
                        },
                        placeholder = { Text("Enter 35+ char passphrase...", color = TextMuted) },
                        visualTransformation = if (inputVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { inputVisible = !inputVisible }) {
                                Icon(
                                    imageVector = if (inputVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = SurfaceVariantDark,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = BackgroundDark,
                            unfocusedContainerColor = BackgroundDark
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Length: ${emergencyInput.trim().length} / $MIN_PASSWORD_LENGTH",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (emergencyInput.trim().length >= MIN_PASSWORD_LENGTH) SuccessGreen else TextMuted
                        )
                    )

                    if (emergencyError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = emergencyError ?: "",
                            color = ErrorRose,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = emergencyInput.trim()
                        if (trimmed.length < MIN_PASSWORD_LENGTH) {
                            emergencyError = "Passphrase must be at least $MIN_PASSWORD_LENGTH symbols."
                            return@Button
                        }
                        if (wallet.emergencyUnlockUsedToday) {
                            emergencyError = "Emergency unlock has already been used today."
                            return@Button
                        }
                        scope.launch {
                            val rule = primaryRule
                            val stored = rule?.emergencyPassword
                            val matches = if (!stored.isNullOrBlank()) stored.trim() == trimmed else true

                            if (matches) {
                                app.walletManager.recordEmergencyUnlockUsed(900)
                                Toast.makeText(context, "Emergency access granted (+15m)", Toast.LENGTH_LONG).show()
                                showEmergencySheet = false
                                emergencyInput = ""
                            } else {
                                emergencyError = "Incorrect emergency passphrase."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    enabled = emergencyInput.trim().length >= MIN_PASSWORD_LENGTH
                ) {
                    Text("Unlock (+15m)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencySheet = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun LiveStatusBanner(
    isAccruing: Boolean,
    activePackage: String?,
    wallet: Wallet
) {
    val backgroundColor = when {
        wallet.isUnfrozen -> Color(0xFF38BDF8).copy(alpha = 0.15f)
        wallet.isBoostActive -> WarningAmber.copy(alpha = 0.15f)
        isAccruing -> SuccessGreen.copy(alpha = 0.15f)
        else -> SurfaceDark
    }

    val dotColor = when {
        wallet.isUnfrozen -> Color(0xFF38BDF8)
        wallet.isBoostActive -> WarningAmber
        isAccruing -> SuccessGreen
        else -> TextMuted
    }

    val text = when {
        wallet.isUnfrozen -> "❄️ Unfreeze active (Bypassing blocking rules)"
        wallet.isBoostActive && isAccruing -> "🔥 Earning with ${wallet.boostMultiplier}x Boost in ${activePackage ?: "study app"}"
        wallet.isBoostActive -> "🔥 ${wallet.boostMultiplier}x Boost active (Study to double earnings)"
        isAccruing -> "Actively earning in ${activePackage ?: "study app"}"
        activePackage != null -> "In study app (paused - interact to accrue)"
        else -> "No productive app currently active"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(50),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(dotColor, CircleShape)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (wallet.isUnfrozen) Color(0xFF38BDF8) else if (wallet.isBoostActive) WarningAmber else if (isAccruing) SuccessGreen else TextSecondary
                )
            )
        }
    }
}

@Composable
fun WalletBalanceCard(wallet: Wallet) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PrimaryIndigo.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = PrimaryIndigo,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Social Wallet Balance",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val formattedTime = formatSeconds(wallet.availableSeconds)
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 36.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val subtitle = when {
                    wallet.isUnfrozen -> "❄️ Unfreeze pass active — apps unblocked"
                    wallet.availableSeconds > 0 -> "Social apps unlocked"
                    else -> "Wallet empty - study to unlock"
                }

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = if (wallet.isUnfrozen) Color(0xFF38BDF8) else if (wallet.availableSeconds > 0) SuccessGreen else ErrorRose,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun StorePromoCard(
    wallet: Wallet,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF312E81).copy(alpha = 0.4f),
                            Color(0xFF1E1B4B).copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Store & Power-Ups",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (wallet.isProActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(WarningAmber, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("PRO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }
                        }
                        Text(
                            text = "2.0x Mega Boosts, Unfreeze Passes & PRO",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("Buy", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DailyMetricsRow(earnedSeconds: Long, spentSeconds: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MetricCard(
            title = "Earned Today",
            value = formatSecondsShort(earnedSeconds),
            icon = Icons.Default.ArrowUpward,
            iconTint = SuccessGreen,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = "Spent Today",
            value = formatSecondsShort(spentSeconds),
            icon = Icons.Default.ArrowDownward,
            iconTint = ErrorRose,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = title, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
        }
    }
}

@Composable
fun ActiveRuleCard(rule: Rule?, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Rule & Limits",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )

                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryIndigo)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (rule != null) {
                RuleDetailItem(
                    label = "Exchange Rate",
                    value = "${rule.productiveMinutesTarget}m study ➔ ${rule.rewardMinutes}m social"
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleDetailItem(
                    label = "Daily Cap",
                    value = "${rule.dailyCapMinutes} minutes max / day"
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleDetailItem(
                    label = "Productive Apps",
                    value = "${rule.productivePackages.size} apps configured"
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleDetailItem(
                    label = "Blocked Apps",
                    value = "${rule.blockedPackages.size} apps shielded"
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleDetailItem(
                    label = "Settings Protection",
                    value = if (rule.emergencyPassword.isNotBlank()) "Password Protected" else "Unlocked"
                )
            } else {
                Text(
                    text = "No active rules configured. Tap edit to setup your ratio.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted)
                )
            }
        }
    }
}

@Composable
fun RuleDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = TextPrimary)
        )
    }
}

fun formatSeconds(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format("%02dh %02dm %02ds", hours, minutes, seconds)
}

fun formatSecondsShort(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    return "${minutes}m"
}
