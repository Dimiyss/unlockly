package com.arhiplabs.unstuckly.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arhiplabs.unstuckly.UnstucklyApplication
import com.arhiplabs.unstuckly.data.model.Rule
import com.arhiplabs.unstuckly.data.model.Wallet
import com.arhiplabs.unstuckly.service.AppTrackingForegroundService
import com.arhiplabs.unstuckly.ui.theme.*
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 35

@Composable
fun HomeScreen(
    onNavigateToRuleEditor: () -> Unit,
    onNavigateToHealth: () -> Unit,
    onNavigateToStore: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = UnstucklyApplication.instance
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
    var showPreferencesDialog by remember { mutableStateOf(false) }

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

    if (showPreferencesDialog) {
        com.arhiplabs.unstuckly.ui.components.PreferencesDialog(
            onDismissRequest = { showPreferencesDialog = false }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.tagline),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Preferences (Theme & Language)
                    IconButton(onClick = { showPreferencesDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.preferences_title),
                            tint = PrimaryIndigo
                        )
                    }

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

            Spacer(modifier = Modifier.height(16.dp))

            // Initial Study Target Milestone Card
            InitialStudyTargetCard(
                wallet = wallet,
                rule = primaryRule
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Today's Metrics Row
            DailyMetricsRow(
                studiedSeconds = wallet.productiveStudySecondsToday,
                earnedSeconds = wallet.earnedTodaySeconds,
                spentSeconds = wallet.spentTodaySeconds
            )

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
                    containerColor = if (wallet.emergencyUnlockUsedToday) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
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
                                    if (wallet.emergencyUnlockUsedToday) MaterialTheme.colorScheme.surfaceVariant else WarningAmber.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (wallet.emergencyUnlockUsedToday) MaterialTheme.colorScheme.onSurfaceVariant else WarningAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.emergency_unlock_title),
                                fontWeight = FontWeight.Bold,
                                color = if (wallet.emergencyUnlockUsedToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (wallet.emergencyUnlockUsedToday) androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.emergency_unlock_used) else androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.emergency_unlock_subtitle),
                                color = if (wallet.emergencyUnlockUsedToday) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Password Required", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Enter your emergency password to access and modify app settings / rules:",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = passwordGateInput,
                        onValueChange = {
                            passwordGateInput = it
                            passwordGateError = null
                        },
                        placeholder = { Text("Enter passphrase...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        visualTransformation = if (passwordGateVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordGateVisible = !passwordGateVisible }) {
                                Icon(
                                    imageVector = if (passwordGateVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Emergency Unlock Dialog
    if (showEmergencySheet) {
        AlertDialog(
            onDismissRequest = { showEmergencySheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.emergency_unlock_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.emergency_desc),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = emergencyInput,
                        onValueChange = {
                            emergencyInput = it
                            emergencyError = null
                        },
                        placeholder = { Text(androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.enter_passphrase), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        visualTransformation = if (inputVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { inputVisible = !inputVisible }) {
                                Icon(
                                    imageVector = if (inputVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Length: ${emergencyInput.trim().length} / $MIN_PASSWORD_LENGTH",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (emergencyInput.trim().length >= MIN_PASSWORD_LENGTH) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
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
                        val validation = com.arhiplabs.unstuckly.domain.RuleEngine.validateEmergencyPassphrase(trimmed)
                        if (!validation.isValid) {
                            emergencyError = validation.error ?: "Invalid emergency passphrase."
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
                    enabled = com.arhiplabs.unstuckly.domain.RuleEngine.isEmergencyPassphraseValid(emergencyInput)
                ) {
                    Text("Unlock (+15m)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencySheet = false }) {
                    Text(androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.close), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        else -> MaterialTheme.colorScheme.surface
    }

    val dotColor = when {
        wallet.isUnfrozen -> Color(0xFF38BDF8)
        wallet.isBoostActive -> WarningAmber
        isAccruing -> SuccessGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val text = when {
        wallet.isUnfrozen -> androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.live_status_unfrozen)
        wallet.isBoostActive && isAccruing -> androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.live_status_boost_earning, wallet.boostMultiplier.toString(), activePackage ?: "app")
        wallet.isBoostActive -> androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.live_status_boost_idle, wallet.boostMultiplier.toString())
        isAccruing -> androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.live_status_active, activePackage ?: "app")
        activePackage != null -> androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.live_status_paused)
        else -> androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.live_status_idle)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
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
                    color = if (wallet.isUnfrozen) Color(0xFF38BDF8) else if (wallet.isBoostActive) WarningAmber else if (isAccruing) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PrimaryIndigo.copy(alpha = 0.12f), Color.Transparent)
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
                    text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.wallet_balance_title),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val formattedTime = formatSeconds(wallet.availableSeconds)
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 36.sp
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                val subtitle = when {
                    wallet.isUnfrozen -> androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.wallet_status_unfrozen)
                    wallet.availableSeconds > 0 -> androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.wallet_status_unlocked)
                    else -> androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.wallet_status_empty)
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            PrimaryIndigo.copy(alpha = 0.12f),
                            Color.Transparent
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
                                text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.store_promo_title),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
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
                            text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.store_promo_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
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
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.btn_buy),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun InitialStudyTargetCard(
    wallet: Wallet,
    rule: Rule?
) {
    val targetMinutes = rule?.productiveMinutesTarget ?: 30
    val targetSeconds = targetMinutes * 60L
    val studiedSeconds = wallet.productiveStudySecondsToday
    val isCompleted = studiedSeconds >= targetSeconds
    val progress = if (targetSeconds > 0) (studiedSeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f) else 1f
    val rewardMinutes = rule?.rewardMinutes ?: 20

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isCompleted) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f),
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.School,
                        contentDescription = null,
                        tint = if (isCompleted) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.initial_study_target_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = if (isCompleted) androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.initial_study_target_met, formatSecondsShort(studiedSeconds)) else androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.initial_study_target_desc),
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isCompleted) SuccessGreen else PrimaryIndigo,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.target_required, formatSecondsShort(studiedSeconds), targetMinutes.toString()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Text(
                    text = if (isCompleted) androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.target_reward_credited, rewardMinutes.toString()) else androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.target_reward_allowance, rewardMinutes.toString()),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = PrimaryIndigo,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Horizontal Status Badge at the bottom of the card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isCompleted) SuccessGreen.copy(alpha = 0.15f) else ErrorRose.copy(alpha = 0.15f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isCompleted) androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.target_unlocked_badge) else androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.target_locked_badge),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isCompleted) SuccessGreen else ErrorRose,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun DailyMetricsRow(studiedSeconds: Long, earnedSeconds: Long, spentSeconds: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            title = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.metric_studied),
            value = formatSecondsShort(studiedSeconds),
            icon = Icons.Default.School,
            iconTint = PrimaryIndigo,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.metric_earned),
            value = formatSecondsShort(earnedSeconds),
            icon = Icons.Default.ArrowUpward,
            iconTint = SuccessGreen,
            modifier = Modifier.weight(1f)
        )

        MetricCard(
            title = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.metric_spent),
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    ),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            )
        }
    }
}

@Composable
fun ActiveRuleCard(rule: Rule?, onEditClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.active_rules_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = PrimaryIndigo)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (rule != null) {
                RuleDetailItem(
                    label = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.rule_exchange_rate),
                    value = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.rule_rate_format, rule.productiveMinutesTarget, rule.rewardMinutes)
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleDetailItem(
                    label = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.rule_daily_cap),
                    value = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.rule_cap_format, rule.dailyCapMinutes)
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleDetailItem(
                    label = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.rule_productive_apps),
                    value = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.rule_apps_configured, rule.productivePackages.size)
                )
                Spacer(modifier = Modifier.height(8.dp))
                RuleDetailItem(
                    label = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.rule_blocked_apps),
                    value = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.rule_apps_shielded, rule.blockedPackages.size)
                )
            } else {
                Text(
                    text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.no_active_rules),
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(text = label, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
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
