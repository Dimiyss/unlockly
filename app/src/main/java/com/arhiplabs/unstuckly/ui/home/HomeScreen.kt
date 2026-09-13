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
import androidx.compose.ui.graphics.luminance
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
import com.arhiplabs.unstuckly.ui.components.AppLogo
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
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppLogo(
                        modifier = Modifier.height(42.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.tagline),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Preferences (Theme & Language)
                    IconButton(
                        onClick = { showPreferencesDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.preferences_title),
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Store / PRO Button
                    IconButton(
                        onClick = onNavigateToStore,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Store & Power-Ups",
                            tint = WarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToHealth,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (healthStatus.isFullyHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = "Health Status",
                            tint = if (healthStatus.isFullyHealthy) SuccessGreen else WarningAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = handleEditRulesRequest,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Rules",
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(24.dp)
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
    val isAvailable = wallet.availableSeconds > 0
    val availableMinutes = wallet.availableSeconds / 60
    val availableSecsRemainder = wallet.availableSeconds % 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            if (isAvailable) PrimaryIndigo.copy(alpha = 0.14f) else WarningAmber.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Header Row: "Доступно зараз" + Info Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.time_available_now),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (wallet.isUnfrozen) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF38BDF8).copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "❄️ Unfrozen",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF38BDF8),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Big numbers display
            if (isAvailable || wallet.isUnfrozen) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = if (availableMinutes > 0) "$availableMinutes" else "$availableSecsRemainder",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 44.sp,
                            lineHeight = 46.sp
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (availableMinutes > 0) androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.minutes_unit) else "s",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.social_time_available),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            } else {
                // Call To Action when wallet is empty: "Час навчання"
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 42.sp
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.minutes_unit),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Prominent CTA Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PrimaryIndigo.copy(alpha = 0.18f), SecondaryPurple.copy(alpha = 0.12f))
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(PrimaryIndigo, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.time_to_study_title),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.time_to_study_desc),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Focus benefit hint pill banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(SuccessGreen.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.focus_benefit_hint),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
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
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    val cardBorder = if (isDark) {
        BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f))
    } else {
        BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.20f))
    }

    val gradientColors = if (isDark) {
        listOf(
            Color(0xFFF59E0B).copy(alpha = 0.15f),
            Color(0xFF141923),
            Color.Transparent
        )
    } else {
        listOf(
            Color(0xFFFEF3C7).copy(alpha = 0.50f),
            Color(0xFFFFFBEB).copy(alpha = 0.25f),
            Color.Transparent
        )
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors = gradientColors))
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
                                if (isDark) {
                                    Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                                } else {
                                    Brush.linearGradient(listOf(Color(0xFFF59E0B).copy(alpha = 0.14f), Color(0xFFF59E0B).copy(alpha = 0.08f)))
                                },
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF0F172A) else Color(0xFFD97706),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.pro_badge_title),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (wallet.isProActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isDark) Color(0xFFF59E0B) else Color(0xFFF59E0B).copy(alpha = 0.18f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.pro_badge_desc),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color(0xFFF59E0B) else Color(0xFFF59E0B).copy(alpha = 0.14f),
                        contentColor = if (isDark) Color(0xFF0F172A) else Color(0xFFB45309)
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.btn_get_pro),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF0F172A) else Color(0xFFB45309)
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
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SecondaryPurple.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Header with target icon and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            PrimaryIndigo.copy(alpha = 0.16f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.next_focus_session_title),
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

            // Big duration display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$targetMinutes ${androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.minutes_unit)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Text(
                    text = "${formatSecondsShort(studiedSeconds)} / ${targetMinutes}m",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = if (isCompleted) SuccessGreen else PrimaryIndigo,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Reward tag & completion status
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isCompleted) SuccessGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = if (isCompleted) SuccessGreen else SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.after_session_available)} +${rewardMinutes}m",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen
                        )
                    )
                }

                Text(
                    text = if (isCompleted) androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.target_unlocked_badge) else androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.target_locked_badge),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun DailyMetricsRow(studiedSeconds: Long, earnedSeconds: Long, spentSeconds: Long) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.today_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            Text(
                text = "${androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.details_link)} ›",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                title = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.metric_studied),
                value = formatSecondsShort(studiedSeconds),
                icon = Icons.Default.School,
                iconTint = PrimaryIndigo,
                containerTint = PrimaryIndigo.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.metric_earned),
                value = formatSecondsShort(earnedSeconds),
                icon = Icons.Default.WbSunny,
                iconTint = SuccessGreen,
                containerTint = SuccessGreen.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = androidx.compose.ui.res.stringResource(com.arhiplabs.unstuckly.R.string.used_today),
                value = formatSecondsShort(spentSeconds),
                icon = Icons.Default.Schedule,
                iconTint = WarningAmber,
                containerTint = WarningAmber.copy(alpha = 0.12f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    containerTint: Color = MaterialTheme.colorScheme.surfaceVariant,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(containerTint, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                ),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
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
    val seconds = totalSeconds % 60
    return when {
        totalSeconds <= 0L -> "0m"
        totalSeconds < 60L -> "${seconds}s"
        seconds == 0L -> "${minutes}m"
        else -> "${minutes}m ${seconds}s"
    }
}
