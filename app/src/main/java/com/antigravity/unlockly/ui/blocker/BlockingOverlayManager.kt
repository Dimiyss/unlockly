package com.antigravity.unlockly.ui.blocker

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import com.antigravity.unlockly.UnlocklyApplication
import com.antigravity.unlockly.data.model.Rule
import com.antigravity.unlockly.ui.theme.UnlocklyTheme
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

        val newLifecycleOwner = OverlayLifecycleOwner()
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(newLifecycleOwner)
            setViewTreeViewModelStoreOwner(newLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(newLifecycleOwner)

            setContent {
                UnlocklyTheme {
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
                            Toast.makeText(context, "Emergency access granted (15 min)", Toast.LENGTH_LONG).show()
                        }
                    )
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
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var emergencyInput by remember { mutableStateOf("") }
    var inputVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val app = remember { UnlocklyApplication.instance }
    val walletState by app.walletManager.walletFlow.collectAsState(initial = null)
    var primaryRule by remember { mutableStateOf<Rule?>(null) }

    LaunchedEffect(Unit) {
        primaryRule = app.ruleRepository.getPrimaryActiveRule()
    }

    val targetMinutes = primaryRule?.productiveMinutesTarget ?: 30
    val targetSeconds = targetMinutes * 60L
    val studiedSeconds = walletState?.productiveStudySecondsToday ?: 0L
    val isInitialTargetMet = studiedSeconds >= targetSeconds

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF00F172A))
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFFF43F5E),
                    modifier = Modifier.size(52.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (!isInitialTargetMet) "Study Target Required" else "Time Limit Reached",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (!isInitialTargetMet) {
                        "You must complete your initial ${targetMinutes}m study session today before accessing blocked apps.\n\nProgress: ${studiedSeconds / 60} / ${targetMinutes} min (${(studiedSeconds * 100 / maxOf(1, targetSeconds)).toInt()}%)"
                    } else {
                        "Your social wallet is empty. Spend productive time in study apps to earn more access time, or use your emergency passphrase if urgently needed."
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8)),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary action: Earn time in Unlockly
                Button(
                    onClick = onOpenUnlockly,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.School, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Earn Time in Unlockly", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Emergency unlock toggle button
                OutlinedButton(
                    onClick = {
                        showEmergencyDialog = !showEmergencyDialog
                        errorMessage = null
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE2E8F0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showEmergencyDialog) "Cancel Emergency Unlock" else "Emergency Unlock (Passphrase)")
                }

                // Emergency Passphrase Entry Section
                AnimatedVisibility(visible = showEmergencyDialog) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Divider(color = Color(0xFF334155), modifier = Modifier.padding(bottom = 12.dp))

                        Text(
                            text = "Enter your 35+ character emergency passphrase:",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFFCBD5E1)),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = emergencyInput,
                            onValueChange = {
                                emergencyInput = it
                                errorMessage = null
                            },
                            placeholder = { Text("Type emergency passphrase...", color = Color(0xFF64748B), fontSize = 12.sp) },
                            visualTransformation = if (inputVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { inputVisible = !inputVisible }) {
                                    Icon(
                                        imageVector = if (inputVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color(0xFF475569),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0F172A),
                                unfocusedContainerColor = Color(0xFF0F172A)
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Length: ${emergencyInput.trim().length} / $MIN_PASSWORD_LENGTH",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (emergencyInput.trim().length >= MIN_PASSWORD_LENGTH) Color(0xFF10B981) else Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            )
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFF43F5E),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                val trimmed = emergencyInput.trim()
                                val validation = com.antigravity.unlockly.domain.RuleEngine.validateEmergencyPassphrase(trimmed)
                                if (!validation.isValid) {
                                    errorMessage = validation.error ?: "Invalid emergency passphrase."
                                    return@Button
                                }
                                scope.launch {
                                    val rule = app.ruleRepository.getPrimaryActiveRule()
                                    val storedPass = rule?.emergencyPassword
                                    val isMatch = if (!storedPass.isNullOrBlank()) {
                                        storedPass.trim() == trimmed
                                    } else {
                                        // Fallback if rule had no pass configured: accept valid 35+ chars passphrase
                                        true
                                    }

                                    if (isMatch) {
                                        // Add 15 minutes of emergency reward time (900 seconds)
                                        app.walletManager.addRewardSeconds(900)
                                        onEmergencyUnlocked()
                                    } else {
                                        errorMessage = "Incorrect emergency passphrase. Try again."
                                    }
                                }
                            },
                            enabled = com.antigravity.unlockly.domain.RuleEngine.isEmergencyPassphraseValid(emergencyInput),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Confirm Emergency Unlock", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
