package com.arhiplabs.unstuckly.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.arhiplabs.unstuckly.UnstucklyApplication
import com.arhiplabs.unstuckly.ui.navigation.NavRoutes
import com.arhiplabs.unstuckly.ui.navigation.UnstucklyNavHost
import com.arhiplabs.unstuckly.ui.theme.AppPreferences
import com.arhiplabs.unstuckly.ui.theme.UnstucklyTheme
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val preferences = AppPreferences.getInstance(newBase)
        val localizedContext = preferences.getLocalizedContext(newBase)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = AppPreferences.getInstance(this)

        setContent {
            val language by preferences.language.collectAsState()
            val localizedContext = remember(language) {
                preferences.getLocalizedContext(this@MainActivity)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration
            ) {
                key(language) {
                    UnstucklyTheme(preferences = preferences) {
                        val navController = rememberNavController()
                        val ruleRepository = remember { UnstucklyApplication.instance.ruleRepository }
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
                            UnstucklyNavHost(
                                navController = navController,
                                startDestination = destination
                            )
                        }
                    }
                }
            }
        }
    }
}
