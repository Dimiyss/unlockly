package com.arhiplabs.unstuckly.ui.blocker

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.arhiplabs.unstuckly.R
import com.arhiplabs.unstuckly.UnstucklyApplication
import com.arhiplabs.unstuckly.data.model.Rule
import com.arhiplabs.unstuckly.ui.theme.*
import kotlinx.coroutines.launch

private const val MIN_PASSWORD_LENGTH = 35

private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    init {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

class BlockingOverlayManager(private val context: Context) {

    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun showOverlay(blockedPackage: String): Boolean {
        if (!Settings.canDrawOverlays(context)) return false
        if (overlayView != null) return true

        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { showOverlay(blockedPackage) }
            return true
        }

        val newLifecycleOwner = OverlayLifecycleOwner()
        val preferences = AppPreferences.getInstance(context)
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(newLifecycleOwner)
            setViewTreeViewModelStoreOwner(newLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(newLifecycleOwner)

            setContent {
                val language by preferences.language.collectAsState()
                val localizedContext = remember(language) {
                    preferences.getLocalizedContext(context)
                }

                CompositionLocalProvider(
                    LocalContext provides localizedContext
                ) {
                    UnstucklyTheme(preferences = preferences) {
                        OverlayContent(
                            blockedPackage = blockedPackage,
                            onOpenUnlockly = {
                                hideOverlay()
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                if (launchIntent != null) {
                                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    context.startActivity(launchIntent)
                                }
                            },
                            onEmergencyUnlocked = {
                                hideOverlay()
                                val toastMsg: CharSequence = localizedContext.getString(R.string.blocker_emergency_granted_toast)
                                Toast.makeText(
                                    localizedContext,
                                    toastMsg,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    }
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        return try {
            windowManager.addView(composeView, params)
            overlayView = composeView
            lifecycleOwner = newLifecycleOwner
            true
        } catch (e: Exception) {
            e.printStackTrace()
            newLifecycleOwner.destroy()
            overlayView = null
            lifecycleOwner = null
            false
        }
    }

    fun hideOverlay() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { hideOverlay() }
            return
        }
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        overlayView = null
    }
}

@Composable
fun OverlayContent(
    blockedPackage: String,
    onOpenUnlockly: () -> Unit,
    onEmergencyUnlocked: () -> Unit = {}
) {
    val context = LocalContext.current
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var emergencyInput by remember { mutableStateOf("") }
    var inputVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val app = remember { UnstucklyApplication.instance }
    val walletState by app.walletManager.walletFlow.collectAsState(initial = null)
    var primaryRule by remember { mutableStateOf<Rule?>(null) }

    LaunchedEffect(Unit) {
        primaryRule = app.ruleRepository.getPrimaryActiveRule()
    }

    val targetMinutes = primaryRule?.productiveMinutesTarget ?: 30
    val targetSeconds = targetMinutes * 60L
    val studiedSeconds = walletState?.productiveStudySecondsToday ?: 0L
    val isInitialTargetMet = studiedSeconds >= targetSeconds

    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xF00F172A) else Color(0x99000000))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.92f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = stringResource(R.string.target_locked_badge),
                    tint = ErrorRose,
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (!isInitialTargetMet) {
                        stringResource(R.string.blocker_study_target_required)
                    } else {
                        stringResource(R.string.blocker_time_limit_reached)
                    },
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                val descText = if (!isInitialTargetMet) {
                    val progressPercent = (studiedSeconds * 100 / maxOf(1, targetSeconds)).toInt()
                    stringResource(
                        R.string.blocker_initial_target_desc,
                        targetMinutes.toString()
                    ) + "\n\n" + stringResource(
                        R.string.blocker_progress_format,
                        (studiedSeconds / 60).toInt(),
                        targetMinutes,
                        progressPercent
                    )
                } else {
                    stringResource(R.string.blocker_wallet_empty_desc)
                }

                Text(
                    text = descText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary action: Earn time in Unstuckly
                Button(
                    onClick = onOpenUnlockly,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.School, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.blocker_earn_time_button),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Emergency unlock toggle button
                OutlinedButton(
                    onClick = {
                        showEmergencyDialog = !showEmergencyDialog
                        errorMessage = null
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (showEmergencyDialog) {
                            stringResource(R.string.blocker_cancel_emergency_btn)
                        } else {
                            stringResource(R.string.blocker_emergency_unlock_btn)
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Emergency Passphrase Entry Section
                AnimatedVisibility(visible = showEmergencyDialog) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Divider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Text(
                            text = stringResource(R.string.blocker_emergency_prompt),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = emergencyInput,
                            onValueChange = {
                                emergencyInput = it
                                errorMessage = null
                            },
                            placeholder = {
                                Text(
                                    stringResource(R.string.blocker_emergency_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    fontSize = 12.sp
                                )
                            },
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
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryIndigo,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.blocker_length_format,
                                    emergencyInput.trim().length,
                                    MIN_PASSWORD_LENGTH
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (emergencyInput.trim().length >= MIN_PASSWORD_LENGTH) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = ErrorRose,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val trimmed = emergencyInput.trim()
                                val validation = com.arhiplabs.unstuckly.domain.RuleEngine.validateEmergencyPassphrase(trimmed)
                                if (!validation.isValid) {
                                    errorMessage = validation.error ?: context.getString(R.string.blocker_invalid_passphrase)
                                    return@Button
                                }
                                scope.launch {
                                    val rule = app.ruleRepository.getPrimaryActiveRule()
                                    val storedPass = rule?.emergencyPassword
                                    val isMatch = if (!storedPass.isNullOrBlank()) {
                                        storedPass.trim() == trimmed
                                    } else {
                                        true
                                    }

                                    if (isMatch) {
                                        app.walletManager.addRewardSeconds(900)
                                        onEmergencyUnlocked()
                                    } else {
                                        errorMessage = context.getString(R.string.blocker_incorrect_passphrase)
                                    }
                                }
                            },
                            enabled = com.arhiplabs.unstuckly.domain.RuleEngine.isEmergencyPassphraseValid(emergencyInput),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.blocker_confirm_emergency_btn),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
