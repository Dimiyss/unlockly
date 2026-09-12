package com.arhiplabs.unstuckly.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.arhiplabs.unstuckly.R
import com.arhiplabs.unstuckly.UnstucklyApplication
import com.arhiplabs.unstuckly.data.model.HealthStatus
import com.arhiplabs.unstuckly.data.model.InstalledApp
import com.arhiplabs.unstuckly.data.model.InstalledAppHelper
import com.arhiplabs.unstuckly.data.model.Rule
import com.arhiplabs.unstuckly.domain.RuleEngine
import com.arhiplabs.unstuckly.ui.components.AppIconImage
import com.arhiplabs.unstuckly.ui.components.AppLogo
import com.arhiplabs.unstuckly.ui.theme.*
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 35

@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = LocalAppPreferences.current
    val healthMonitor = remember { UnstucklyApplication.instance.healthMonitor }
    var healthStatus by remember { mutableStateOf(healthMonitor.checkHealth()) }

    // Auto-refresh health status on resume (e.g. returning from battery settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                healthStatus = healthMonitor.checkHealth()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    var installedApps by remember { mutableStateOf<List<InstalledApp>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }

    var selectedProductive by remember {
        mutableStateOf<Set<String>>(emptySet())
    }
    var selectedBlocked by remember {
        mutableStateOf<Set<String>>(emptySet())
    }
    var emergencyPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val apps = InstalledAppHelper.getInstalledLauncherApps(context)
        installedApps = apps
        isLoadingApps = false

        // Filter default selections to only those installed on the device (Max 2 for Free Plan)
        val installedPkgSet = apps.map { it.packageName }.toSet()
        val defaultProductive = setOf("com.quizlet.quizlet", "com.duolingo", "com.khanacademy.android", "com.anki")
        val defaultBlocked = setOf("com.instagram.android", "com.zhiliaoapp.musically", "com.google.android.youtube", "com.twitter.android")

        selectedProductive = defaultProductive.intersect(installedPkgSet).take(RuleEngine.MAX_FREE_TIER_APPS_PER_CATEGORY).toSet()
        selectedBlocked = (defaultBlocked.intersect(installedPkgSet) - selectedProductive).take(RuleEngine.MAX_FREE_TIER_APPS_PER_CATEGORY).toSet()
    }

    val isStepValid = when (currentStep) {
        2 -> selectedProductive.isNotEmpty() && selectedProductive.size <= RuleEngine.MAX_FREE_TIER_APPS_PER_CATEGORY
        3 -> selectedBlocked.isNotEmpty() && selectedBlocked.size <= RuleEngine.MAX_FREE_TIER_APPS_PER_CATEGORY
        4 -> RuleEngine.isEmergencyPassphraseValid(emergencyPassword)
        5 -> healthStatus.batteryOptimized
        else -> true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
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
                    0 -> LanguageSelectionStep(
                        preferences = preferences
                    )
                    1 -> WelcomeStep()
                    2 -> InstalledAppSelectionStep(
                        title = stringResource(R.string.onboarding_productive_title),
                        subtitle = stringResource(R.string.onboarding_productive_subtitle),
                        icon = Icons.Default.School,
                        accentColor = SuccessGreen,
                        installedApps = installedApps,
                        isLoading = isLoadingApps,
                        selectedPackages = selectedProductive,
                        excludedPackages = selectedBlocked,
                        excludedLabel = stringResource(R.string.onboarding_excluded_blocked),
                        onToggle = { pkg ->
                            selectedProductive = if (selectedProductive.contains(pkg)) {
                                selectedProductive - pkg
                            } else {
                                selectedProductive + pkg
                            }
                        }
                    )
                    3 -> InstalledAppSelectionStep(
                        title = stringResource(R.string.onboarding_blocked_title),
                        subtitle = stringResource(R.string.onboarding_blocked_subtitle),
                        icon = Icons.Default.Block,
                        accentColor = ErrorRose,
                        installedApps = installedApps,
                        isLoading = isLoadingApps,
                        selectedPackages = selectedBlocked,
                        excludedPackages = selectedProductive,
                        excludedLabel = stringResource(R.string.onboarding_excluded_productive),
                        onToggle = { pkg ->
                            selectedBlocked = if (selectedBlocked.contains(pkg)) {
                                selectedBlocked - pkg
                            } else {
                                selectedBlocked + pkg
                            }
                        }
                    )
                    4 -> EmergencyPasswordStep(
                        password = emergencyPassword,
                        onPasswordChange = { emergencyPassword = it }
                    )
                    5 -> PermissionSetupStep(
                        healthStatus = healthStatus,
                        onRefresh = { healthStatus = healthMonitor.checkHealth() }
                    )
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
                        Text(stringResource(R.string.onboarding_back))
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
                                UnstucklyApplication.instance.ruleRepository.saveRule(newRule)
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
                        text = if (currentStep == totalSteps - 1) stringResource(R.string.onboarding_start) else stringResource(R.string.onboarding_continue),
                        fontWeight = FontWeight.Bold,
                        color = if (isStepValid) Color.White else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageSelectionStep(
    preferences: AppPreferences
) {
    val currentLanguage by preferences.language.collectAsState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(PrimaryIndigo.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_language_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_language_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            AppLanguage.entries.forEach { lang ->
                val isSelected = currentLanguage == lang
                val backgroundColor by animateColorAsState(
                    if (isSelected) PrimaryIndigo.copy(alpha = 0.18f)
                    else SurfaceDark,
                    label = "langBg"
                )
                val borderColor = if (isSelected) PrimaryIndigo else SurfaceVariantDark.copy(alpha = 0.6f)

                Card(
                    onClick = { preferences.setLanguage(lang) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = lang.flag,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = lang.displayName,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextPrimary
                                )
                            )
                        }

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, TextMuted, CircleShape)
                            )
                        }
                    }
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
        AppLogo(
            modifier = Modifier.height(180.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.onboarding_welcome_desc),
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
    excludedPackages: Set<String> = emptySet(),
    excludedLabel: String = "",
    maxSelection: Int? = RuleEngine.MAX_FREE_TIER_APPS_PER_CATEGORY,
    onToggle: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showOnlySelected by remember { mutableStateOf(false) }

    val installedPkgSet = remember(installedApps) { installedApps.map { it.packageName }.toSet() }
    val validSelectedCount = remember(selectedPackages, installedPkgSet) {
        selectedPackages.intersect(installedPkgSet).size
    }

    val filteredApps = remember(searchQuery, installedApps, showOnlySelected, selectedPackages) {
        installedApps.filter { app ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                app.name.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
            val matchesFilter = if (showOnlySelected) {
                selectedPackages.contains(app.packageName)
            } else {
                true
            }
            matchesSearch && matchesFilter
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

        Spacer(modifier = Modifier.height(10.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.onboarding_search_apps_hint), color = TextMuted, fontSize = 13.sp) },
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

        // Filter chips & selected summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChip(
                    selected = !showOnlySelected,
                    onClick = { showOnlySelected = false },
                    label = { Text(stringResource(R.string.onboarding_filter_all, installedApps.size), fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.2f),
                        selectedLabelColor = accentColor,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary
                    )
                )

                FilterChip(
                    selected = showOnlySelected,
                    onClick = { showOnlySelected = true },
                    label = { Text(stringResource(R.string.onboarding_filter_chosen, validSelectedCount), fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.2f),
                        selectedLabelColor = accentColor,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary
                    )
                )
            }

            Text(
                text = if (maxSelection != null) {
                    stringResource(R.string.onboarding_selected_count_limit, validSelectedCount, maxSelection)
                } else {
                    stringResource(R.string.onboarding_selected_count, validSelectedCount)
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    color = if (maxSelection != null && validSelectedCount >= maxSelection) WarningAmber else accentColor,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }

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
                    text = if (showOnlySelected && validSelectedCount == 0) {
                        stringResource(R.string.onboarding_empty_chosen)
                    } else {
                        stringResource(R.string.onboarding_empty_search, searchQuery)
                    },
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
                    val isExcluded = excludedPackages.contains(app.packageName)
                    Card(
                        onClick = {
                            if (!isExcluded) {
                                if (isChecked) {
                                    onToggle(app.packageName)
                                } else {
                                    if (maxSelection != null && validSelectedCount >= maxSelection) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.onboarding_free_plan_limit_toast, maxSelection),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        onToggle(app.packageName)
                                    }
                                }
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isExcluded -> SurfaceDark.copy(alpha = 0.3f)
                                isChecked -> SurfaceDark
                                else -> SurfaceDark.copy(alpha = 0.5f)
                            }
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
                                        color = if (isExcluded) TextMuted else TextPrimary
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isExcluded) excludedLabel else app.packageName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (isExcluded) WarningAmber else TextMuted,
                                        fontSize = 11.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isExcluded) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Excluded",
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
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
}

@Composable
fun EmergencyPasswordStep(
    password: String,
    onPasswordChange: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val length = password.trim().length
    val validation = remember(password) { RuleEngine.validateEmergencyPassphrase(password) }
    val isValid = validation.isValid

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
                text = stringResource(R.string.onboarding_emergency_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_emergency_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(stringResource(R.string.onboarding_emergency_input_label)) },
            placeholder = { Text(stringResource(R.string.onboarding_emergency_input_placeholder), color = TextMuted) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(if (passwordVisible) R.string.onboarding_emergency_hide else R.string.onboarding_emergency_show),
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
                text = if (isValid) stringResource(R.string.onboarding_emergency_valid) else (validation.error ?: stringResource(R.string.onboarding_emergency_chars_needed, MIN_PASSWORD_LENGTH - length)),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
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
                        text = stringResource(R.string.onboarding_emergency_why_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.onboarding_emergency_why_desc),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
    }
}

@Composable
fun PermissionSetupStep(
    healthStatus: HealthStatus,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.onboarding_perm_title),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_perm_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Mandatory Battery Optimization Notice
        if (!healthStatus.batteryOptimized) {
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_perm_battery_mandatory_notice),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = WarningAmber,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }

        // 1. Mandatory: Disable Battery Optimization
        PermissionItem(
            title = stringResource(R.string.perm_battery_title),
            description = stringResource(R.string.perm_battery_desc),
            isGranted = healthStatus.batteryOptimized,
            isMandatory = true,
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
                            Toast.makeText(context, "Could not open battery settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Usage Access
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

        // 3. Accessibility Service
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

        // 4. Display Over Other Apps
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

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onRefresh,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = PrimaryIndigo)
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.onboarding_perm_refresh), color = PrimaryIndigo)
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    isGranted: Boolean,
    isMandatory: Boolean = false,
    onGrant: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isMandatory && !isGranted) WarningAmber.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isMandatory && !isGranted) WarningAmber.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isGranted) SuccessGreen else WarningAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(text = title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (isMandatory && !isGranted) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarningAmber.copy(alpha = 0.2f),
                            contentColor = WarningAmber
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_perm_battery_required_badge),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (!isGranted) {
                Button(
                    onClick = onGrant,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMandatory) WarningAmber else PrimaryIndigo
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.btn_grant),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMandatory) Color.Black else Color.White
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.btn_granted),
                    color = SuccessGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
