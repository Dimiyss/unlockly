package com.antigravity.unlockly.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.antigravity.unlockly.UnlocklyApplication
import com.antigravity.unlockly.ui.navigation.NavRoutes
import com.antigravity.unlockly.ui.navigation.UnlocklyNavHost
import com.antigravity.unlockly.ui.theme.UnlocklyTheme
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UnlocklyTheme {
                val navController = rememberNavController()
                val ruleRepository = remember { UnlocklyApplication.instance.ruleRepository }
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    val rules = ruleRepository.allRules.firstOrNull()
                    startDestination = if (rules.isNullOrEmpty()) {
                        NavRoutes.ONBOARDING
                    } else {
                        NavRoutes.HOME
                    }
                }

                startDestination?.let { destination ->
                    UnlocklyNavHost(
                        navController = navController,
                        startDestination = destination
                    )
                }
            }
        }
    }
}
