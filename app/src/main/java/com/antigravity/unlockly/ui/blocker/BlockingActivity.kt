package com.antigravity.unlockly.ui.blocker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.antigravity.unlockly.ui.theme.UnlocklyTheme

class BlockingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: ""

        setContent {
            UnlocklyTheme {
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
                        Toast.makeText(this@BlockingActivity, "Emergency access granted (15 min)", Toast.LENGTH_LONG).show()
                        finish()
                    }
                )
            }
        }
    }

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
