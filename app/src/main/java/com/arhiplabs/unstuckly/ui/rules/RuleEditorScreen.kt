package com.arhiplabs.unstuckly.ui.rules

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arhiplabs.unstuckly.UnstucklyApplication
import com.arhiplabs.unstuckly.data.model.InstalledApp
import com.arhiplabs.unstuckly.data.model.InstalledAppHelper
import com.arhiplabs.unstuckly.data.model.Rule
import com.arhiplabs.unstuckly.data.model.Wallet
import com.arhiplabs.unstuckly.domain.RuleEngine
import com.arhiplabs.unstuckly.ui.components.AppPickerDialog
import com.arhiplabs.unstuckly.ui.theme.*
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 35

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = remember { UnstucklyApplication.instance }
    val ruleRepository = remember { app.ruleRepository }
    val activeRules by ruleRepository.activeRules.collectAsState(initial = emptyList())
    val currentRule = activeRules.firstOrNull()

    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    LaunchedEffect(Unit) {
        installedApps = InstalledAppHelper.getInstalledLauncherApps(context)
    }

    var targetMinutes by remember(currentRule) { mutableStateOf((currentRule?.productiveMinutesTarget ?: 30).toString()) }
    var rewardMinutes by remember(currentRule) { mutableStateOf((currentRule?.rewardMinutes ?: 20).toString()) }
    var dailyCapMinutes by remember(currentRule) { mutableStateOf((currentRule?.dailyCapMinutes ?: 120).toString()) }

    var selectedProductive by remember(currentRule) {
        mutableStateOf(currentRule?.productivePackages?.toSet() ?: setOf("com.quizlet.quizlet", "com.duolingo"))
    }
    var selectedBlocked by remember(currentRule) {
        mutableStateOf(currentRule?.blockedPackages?.toSet() ?: setOf("com.instagram.android", "com.zhiliaoapp.musically"))
    }
    var emergencyPassword by remember(currentRule) {
        mutableStateOf(currentRule?.emergencyPassword ?: "")
    }

    var showProductivePicker by remember { mutableStateOf(false) }
    var showBlockedPicker by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    val walletState by app.walletManager.walletFlow.collectAsState(initial = null)
    val isPro = walletState?.isProActive == true

    val passwordLength = emergencyPassword.trim().length
    val passphraseValidation = remember(emergencyPassword) { RuleEngine.validateEmergencyPassphrase(emergencyPassword) }
    val isPasswordValid = passwordLength == 0 || passphraseValidation.isValid
    val isSaveEnabled = isPasswordValid && (passphraseValidation.isValid || currentRule?.emergencyPassword?.isNotEmpty() == true)

    val productiveCount = selectedProductive.count { pkg -> installedApps.any { it.packageName == pkg } }
    val blockedCount = selectedBlocked.count { pkg -> installedApps.any { it.packageName == pkg } }

    if (showProductivePicker) {
        AppPickerDialog(
            title = "Target / Productive Apps",
            subtitle = if (isPro) "Select study apps that accrue wallet balance (PRO: Unlimited)" else "Select up to 2 study apps (Free Limit: 2 apps)",
            accentColor = SuccessGreen,
            installedApps = installedApps,
            selectedPackages = selectedProductive,
            excludedPackages = selectedBlocked,
            excludedLabel = "Already chosen as Blocked App",
            maxSelection = if (isPro) null else RuleEngine.MAX_FREE_TIER_APPS_PER_CATEGORY,
            onSaveSelection = { selectedProductive = it },
            onDismiss = { showProductivePicker = false }
        )
    }

    if (showBlockedPicker) {
        AppPickerDialog(
            title = "Blocked / Distractor Apps",
            subtitle = if (isPro) "Select social apps that consume wallet balance (PRO: Unlimited)" else "Select up to 2 distractor apps (Free Limit: 2 apps)",
            accentColor = ErrorRose,
            installedApps = installedApps,
            selectedPackages = selectedBlocked,
            excludedPackages = selectedProductive,
            excludedLabel = "Already chosen as Target App",
            maxSelection = if (isPro) null else RuleEngine.MAX_FREE_TIER_APPS_PER_CATEGORY,
            onSaveSelection = { selectedBlocked = it },
            onDismiss = { showBlockedPicker = false }
        )
    }

    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            TopAppBar(
                title = { Text("Edit Rule & Apps", fontWeight = FontWeight.Bold, color = TextPrimary) },
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
            // Section 1: App Selections
            Text(
                text = if (isPro) "App Selections (PRO: Unlimited Apps)" else "App Selections (Free: Max 2 Apps per Category)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryIndigo)
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Productive Apps Card
            AppSelectionCard(
                title = "Productive / Target Apps",
                subtitle = "$productiveCount apps selected" + if (isPro) " (PRO: Unlimited)" else " ($productiveCount/2 Free Limit)",
                accentColor = SuccessGreen,
                icon = Icons.Default.School,
                onClick = { showProductivePicker = true }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Blocked Apps Card
            AppSelectionCard(
                title = "Blocked / Distractor Apps",
                subtitle = "$blockedCount apps selected" + if (isPro) " (PRO: Unlimited)" else " ($blockedCount/2 Free Limit)",
                accentColor = ErrorRose,
                icon = Icons.Default.Block,
                onClick = { showBlockedPicker = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 2: Exchange Rate

            Text(
                text = "Exchange Rate Configuration",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryIndigo)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isPro) "Target Study Time (PRO: 15m, 30m, 45m, 1h available):" else "Target Study Time (Free tier: 30m only; 15m is PRO):",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Quick Preset Selection Chips (15m [PRO], 30m [Free], 45m [PRO], 60m [PRO])
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(15, "15m", true),  // 15m requires PRO
                    Triple(30, "30m", false), // 30m is the single Free tier interval
                    Triple(45, "45m", true),  // 45m requires PRO
                    Triple(60, "1h", true)    // 1h requires PRO
                ).forEach { (minutes, label, requiresPro) ->
                    val isSelected = targetMinutes == minutes.toString()
                    val isLocked = requiresPro && !isPro

                    Surface(
                        onClick = {
                            if (isLocked) {
                                Toast.makeText(
                                    context,
                                    "Free package supports only the 30m interval. Upgrade to PRO for ${label} and other flexible intervals.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                targetMinutes = minutes.toString()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isSelected -> PrimaryIndigo
                            isLocked -> SurfaceDark.copy(alpha = 0.5f)
                            else -> SurfaceDark
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) PrimaryIndigo else SurfaceVariantDark
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else if (isLocked) TextMuted else TextPrimary,
                                fontSize = 13.sp
                            )
                            if (requiresPro) {
                                Text(
                                    text = if (isPro) "PRO" else "🔒 PRO",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else WarningAmber
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = targetMinutes,
                onValueChange = { targetMinutes = it },
                label = { Text(if (isPro) "Productive Target Minutes (Min 15m)" else "Productive Target Minutes (Free: 30m only)") },
                supportingText = {
                    Text(
                        text = if (isPro) "PRO: 15m, 30m, 45m, 1h and custom targets allowed."
                        else "Free package supports only 30m target. Upgrade to PRO for 15m and other intervals.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = SurfaceVariantDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = rewardMinutes,
                onValueChange = { rewardMinutes = it },
                label = { Text("Reward Earned (Minutes)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = SurfaceVariantDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = dailyCapMinutes,
                onValueChange = { dailyCapMinutes = it },
                label = { Text("Daily Cap Limit (Minutes)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = SurfaceVariantDark,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Emergency Passphrase
            Text(
                text = "Emergency Passphrase (Min 35 Symbols)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = PrimaryIndigo)
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = emergencyPassword,
                onValueChange = { emergencyPassword = it },
                label = { Text("Emergency Passphrase") },
                placeholder = { Text("Enter at least 35 characters...", color = TextMuted) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isPasswordValid) PrimaryIndigo else ErrorRose,
                    unfocusedBorderColor = if (isPasswordValid) SurfaceVariantDark else ErrorRose,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (passphraseValidation.isValid) "✓ Valid emergency passphrase"
                    else if (passwordLength > 0) (passphraseValidation.error ?: "Requires ${MIN_PASSWORD_LENGTH - passwordLength} more characters")
                    else "Passphrase required (min 35 symbols)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = if (passphraseValidation.isValid) SuccessGreen else if (passwordLength > 0) ErrorRose else TextMuted
                    ),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$passwordLength / $MIN_PASSWORD_LENGTH",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (passphraseValidation.isValid) SuccessGreen else if (passwordLength > 0) ErrorRose else TextMuted
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val parsedTarget = targetMinutes.toIntOrNull() ?: 30
                    if (!isPro && parsedTarget != 30) {
                        Toast.makeText(
                            context,
                            "Free package supports only the 30m minimum interval at the beginning of the day. Upgrade to PRO for 15m, 45m and flexible options.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }
                    if (isPro && parsedTarget < 15) {
                        Toast.makeText(
                            context,
                            "PRO tier requires minimum 15m target study time.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }
                    if (!RuleEngine.isAppCountValid(selectedProductive.size, selectedBlocked.size, isPro)) {
                        Toast.makeText(
                            context,
                            "Free plan allows up to ${RuleEngine.MAX_FREE_TIER_APPS_PER_CATEGORY} apps per category. Upgrade to PRO for unlimited apps.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }

                    scope.launch {
                        val updated = Rule(
                            id = currentRule?.id ?: 0L,
                            productivePackages = selectedProductive.toList(),
                            blockedPackages = selectedBlocked.toList(),
                            productiveMinutesTarget = parsedTarget,
                            rewardMinutes = rewardMinutes.toIntOrNull() ?: 20,
                            dailyCapMinutes = dailyCapMinutes.toIntOrNull() ?: 120,
                            emergencyPassword = emergencyPassword.trim()
                        )
                        ruleRepository.saveRule(updated)
                        onNavigateBack()
                    }
                },
                enabled = passphraseValidation.isValid,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryIndigo,
                    disabledContainerColor = PrimaryIndigo.copy(alpha = 0.4f)
                )
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Rule Changes", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AppSelectionCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Edit",
                tint = TextSecondary
            )
        }
    }
}
