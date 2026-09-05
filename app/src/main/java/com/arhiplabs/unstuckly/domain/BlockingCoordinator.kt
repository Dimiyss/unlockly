package com.arhiplabs.unstuckly.domain

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.arhiplabs.unstuckly.data.repository.RuleRepository
import com.arhiplabs.unstuckly.ui.blocker.BlockingActivity
import com.arhiplabs.unstuckly.ui.blocker.BlockingOverlayManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class BlockingCoordinator(
    private val context: Context,
    private val walletManager: WalletManager,
    private val ruleRepository: RuleRepository
) {

    private val overlayManager by lazy { BlockingOverlayManager(context) }

    private val _isBlockedAppActive = MutableStateFlow(false)
    val isBlockedAppActive: StateFlow<Boolean> = _isBlockedAppActive.asStateFlow()

    suspend fun evaluateForegroundPackage(packageName: String) = withContext(Dispatchers.Main) {
        // Ignore evaluation when foreground package is Unlockly itself (prevents overlay self-dismissal)
        if (packageName == context.packageName) {
            return@withContext
        }

        val rule = ruleRepository.getPrimaryActiveRule() ?: run {
            _isBlockedAppActive.value = false
            overlayManager.hideOverlay()
            return@withContext
        }

        val isBlockedApp = rule.blockedPackages.contains(packageName)
        if (!isBlockedApp) {
            _isBlockedAppActive.value = false
            overlayManager.hideOverlay()
            return@withContext
        }

        _isBlockedAppActive.value = true
        val wallet = walletManager.getWallet()

        if (wallet.isUnfrozen) {
            // Unfreeze active: bypass all blocking rules
            overlayManager.hideOverlay()
            return@withContext
        }

        if (wallet.availableSeconds > 0) {
            overlayManager.hideOverlay()
            walletManager.deductSpentSeconds(1)
        } else {
            // Wallet is empty! Trigger blocking
            val overlaySuccess = if (Settings.canDrawOverlays(context)) {
                overlayManager.showOverlay(packageName)
            } else {
                false
            }

            if (!overlaySuccess) {
                launchBlockingActivity(packageName)
            }
        }
    }

    private fun launchBlockingActivity(packageName: String) {
        val intent = Intent(context, BlockingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(BlockingActivity.EXTRA_BLOCKED_PACKAGE, packageName)
        }
        context.startActivity(intent)
    }
}
