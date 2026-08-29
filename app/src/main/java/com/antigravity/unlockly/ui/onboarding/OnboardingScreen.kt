package com.antigravity.unlockly.ui.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.unlockly.UnlocklyApplication
import com.antigravity.unlockly.data.model.InstalledApp
import com.antigravity.unlockly.data.model.InstalledAppHelper
import com.antigravity.unlockly.data.model.Rule
import com.antigravity.unlockly.ui.components.AppIconImage
import com.antigravity.unlockly.ui.theme.*
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 35

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 5

    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        installedApps = InstalledAppHelper.getInstalledLauncherApps(context)
        isLoadingApps = false
    }

    var selectedProductive by remember {
        mutableStateOf(
            setOf("com.quizlet.quizlet", "com.duolingo", "com.khanacademy.android", "com.anki")
        )
    }
    var selectedBlocked by remember {
        mutableStateOf(
            setOf("com.instagram.android", "com.zhiliaoapp.musically", "com.google.android.youtube", "com.twitter.android")
        )
    }
    var emergencyPassword by remember { mutableStateOf("") }

    val isStepValid = when (currentStep) {
        3 -> emergencyPassword.trim().length >= MIN_PASSWORD_LENGTH
        else -> true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Progress Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalSteps) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(height = 6.dp, width = if (index == currentStep) 24.dp else 8.dp)
                            .background(
                                color = if (index == currentStep) PrimaryIndigo else SurfaceVariantDark,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Main Content per Step
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (currentStep) {
                    0 -> WelcomeStep()
                    1 -> InstalledAppSelectionStep(
                        title = "Target / Productive Apps",
                        subtitle = "Select all study apps where active usage will earn you social time.",
                        icon = Icons.Default.School,
                        accentColor = SuccessGreen,
                        installedApps = installedApps,
                        isLoading = isLoadingApps,
                        selectedPackages = selectedProductive,
                        onToggle = { pkg ->
                            selectedProductive = if (selectedProductive.contains(pkg)) {
                                selectedProductive - pkg
                            } else {
                                selectedProductive + pkg
                            }
                        }
                    )
                    2 -> InstalledAppSelectionStep(
                        title = "Blocked / Distractor Apps",
                        subtitle = "Select social apps that will consume your wallet and get blocked when empty.",
                        icon = Icons.Default.Block,
                        accentColor = ErrorRose,
                        installedApps = installedApps,
                        isLoading = isLoadingApps,
                        selectedPackages = selectedBlocked,
                        onToggle = { pkg ->
                            selectedBlocked = if (selectedBlocked.contains(pkg)) {
                                selectedBlocked - pkg
                            } else {
                                selectedBlocked + pkg
                            }
                        }
                    )
                    3 -> EmergencyPasswordStep(
                        password = emergencyPassword,
                        onPasswordChange = { emergencyPassword = it }
                    )
                    4 -> PermissionSetupStep()
                }
            }

            // Action Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStep < totalSteps - 1) {
                            currentStep++
                        } else {
                            // Save rule to Room and proceed
                            scope.launch {
                                val newRule = Rule(
                                    productivePackages = selectedProductive.toList(),
                                    blockedPackages = selectedBlocked.toList(),
                                    productiveMinutesTarget = 30,
                                    rewardMinutes = 20,
                                    dailyCapMinutes = 120,
                                    emergencyPassword = emergencyPassword.trim()
                                )
                                UnlocklyApplication.instance.ruleRepository.saveRule(newRule)
                                onFinishOnboarding()
                            }
                        }
                    },
                    enabled = isStepValid,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryIndigo,
                        disabledContainerColor = PrimaryIndigo.copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (currentStep == totalSteps - 1) "Start Unlockly" else "Continue",
                        fontWeight = FontWeight.Bold,
                        color = if (isStepValid) Color.White else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(PrimaryIndigo.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Earn Your Screen Time",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Spend active minutes studying in your chosen learning apps to earn screen time for distractor apps. Turn habit into reward!",
            style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun InstalledAppSelectionStep(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    installedApps: List<InstalledApp>,
    isLoading: Boolean,
    selectedPackages: Set<String>,
    onToggle: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(searchQuery, installedApps) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.fillMaxSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search installed apps...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = SurfaceVariantDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Selected summary
        Text(
            text = "Selected ${selectedPackages.size} apps",
            style = MaterialTheme.typography.labelMedium.copy(
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = accentColor)
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No apps found matching \"$searchQuery\"",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    val isChecked = selectedPackages.contains(app.packageName)
                    Card(
                        onClick = { onToggle(app.packageName) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isChecked) SurfaceDark else SurfaceDark.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppIconImage(app = app, modifier = Modifier.size(36.dp))

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { onToggle(app.packageName) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    checkmarkColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyPasswordStep(
    password: String,
    onPasswordChange: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val length = password.trim().length
    val isValid = length >= MIN_PASSWORD_LENGTH

    val statusColor = when {
        isValid -> SuccessGreen
        length > 0 -> WarningAmber
        else -> TextMuted
    }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Emergency Passphrase",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Create a long emergency passphrase (at least 35 symbols). If you truly need urgent access when an app is blocked, typing this entire phrase will unblock it.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Emergency Passphrase (min 35 symbols)") },
            placeholder = { Text("e.g., I promise to stay focused on my learning goals today 2026!", color = TextMuted) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide passphrase" else "Show passphrase",
                        tint = TextSecondary
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = statusColor,
                unfocusedBorderColor = if (length > 0) statusColor else SurfaceVariantDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Live Character Counter & Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isValid) "✓ Passphrase length met ($length chars)" else "Requires ${MIN_PASSWORD_LENGTH - length} more chars",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                text = "$length / $MIN_PASSWORD_LENGTH",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { (length.toFloat() / MIN_PASSWORD_LENGTH.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = statusColor,
            trackColor = SurfaceVariantDark
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Why 35+ characters?",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "A long sentence introduces intentional friction to break dopamine loops, while still guaranteeing you can never be permanently locked out.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }
}

@Composable
fun PermissionSetupStep() {
    val context = LocalContext.current
    val healthMonitor = remember { UnlocklyApplication.instance.healthMonitor }
    var healthStatus by remember { mutableStateOf(healthMonitor.checkHealth()) }

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Required Permissions",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Unlockly needs system access to detect active study sessions and enforce application limits.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        PermissionItem(
            title = "Usage Access",
            description = "Detects when productive or social apps are in the foreground.",
            isGranted = healthStatus.usageAccessGranted,
            onGrant = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItem(
            title = "Accessibility Service",
            description = "Measures real user interaction (scrolls, touches) to ensure active study time.",
            isGranted = healthStatus.accessibilityGranted,
            onGrant = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionItem(
            title = "Display Over Other Apps",
            description = "Shows the blocker overlay when your social wallet expires.",
            isGranted = healthStatus.overlayGranted,
            onGrant = {
                context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { healthStatus = healthMonitor.checkHealth() },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = PrimaryIndigo)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Refresh Permission Status", color = PrimaryIndigo)
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onGrant: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isGranted) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = title, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = description, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (!isGranted) {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Grant", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Granted", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
