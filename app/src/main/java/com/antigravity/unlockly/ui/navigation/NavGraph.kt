package com.antigravity.unlockly.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.antigravity.unlockly.ui.home.HomeScreen
import com.antigravity.unlockly.ui.onboarding.OnboardingScreen
import com.antigravity.unlockly.ui.rules.RuleEditorScreen
import com.antigravity.unlockly.ui.settings.HealthScreen
import com.antigravity.unlockly.ui.store.StoreScreen

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val RULE_EDITOR = "rule_editor"
    const val HEALTH = "health"
    const val STORE = "store"
}

@Composable
fun UnlocklyNavHost(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onNavigateToRuleEditor = { navController.navigate(NavRoutes.RULE_EDITOR) },
                onNavigateToHealth = { navController.navigate(NavRoutes.HEALTH) },
                onNavigateToStore = { navController.navigate(NavRoutes.STORE) }
            )
        }

        composable(NavRoutes.RULE_EDITOR) {
            RuleEditorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.HEALTH) {
            HealthScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.STORE) {
            StoreScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
