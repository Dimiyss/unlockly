package com.arhiplabs.unstuckly.ui.blocker

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.arhiplabs.unstuckly.R
import com.arhiplabs.unstuckly.ui.theme.AppPreferences
import com.arhiplabs.unstuckly.ui.theme.UnstucklyTheme

class BlockingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            window.attributes = window.attributes.apply {
                blurBehindRadius = 50
            }
        }
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""
        val preferences = AppPreferences.getInstance(this)

        setContent {
            val language by preferences.language.collectAsState()
            val localizedContext = remember(language) {
                preferences.getLocalizedContext(this@BlockingActivity)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext
            ) {
                UnstucklyTheme(preferences = preferences) {
                    OverlayContent(
                        blockedPackage = blockedPackage,
                        onOpenUnlockly = {
                            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                            if (launchIntent != null) {
                                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                startActivity(launchIntent)
                            }
                            finish()
                        },
                        onEmergencyUnlocked = {
                            val toastMsg: CharSequence = localizedContext.getString(R.string.blocker_emergency_granted_toast)
                            Toast.makeText(
                                localizedContext,
                                toastMsg,
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        }
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Redirect to home screen when back button is pressed on blocker
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "extra_blocked_package"
    }
}
