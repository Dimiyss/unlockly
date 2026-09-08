package com.arhiplabs.unstuckly.domain

import com.arhiplabs.unstuckly.data.repository.RuleRepository
import com.arhiplabs.unstuckly.service.InteractionTrackerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EarnSessionManager(
    private val walletManager: WalletManager,
    private val ruleRepository: RuleRepository
) {

    private val _isAccruing = MutableStateFlow(false)
    val isAccruing: StateFlow<Boolean> = _isAccruing.asStateFlow()

    private val _activeProductivePackage = MutableStateFlow<String?>(null)
    val activeProductivePackage: StateFlow<String?> = _activeProductivePackage.asStateFlow()

    private var pendingSeconds: Double = 0.0

    suspend fun tick(currentPackage: String, isScreenInteractive: Boolean) {
        val rule = ruleRepository.getPrimaryActiveRule() ?: run {
            _isAccruing.value = false
            _activeProductivePackage.value = null
            return
        }

        val isProductiveApp = rule.productivePackages.contains(currentPackage)
        val now = System.currentTimeMillis()
        val lastInteraction = InteractionTrackerService.lastInteractionTimestamp.value
        val isUserActive = (now - lastInteraction) <= IDLE_THRESHOLD_MS

        if (isProductiveApp && isScreenInteractive && isUserActive) {
            val wallet = walletManager.getWallet()
            if (RuleEngine.isDailyCapReached(wallet.earnedTodaySeconds, rule.dailyCapMinutes)) {
                _isAccruing.value = false
                _activeProductivePackage.value = currentPackage
                return
            }

            _isAccruing.value = true
            _activeProductivePackage.value = currentPackage

            // 1. Record productive study time
            walletManager.addProductiveStudySeconds(1L)

            // 2. Accrue rewards incrementally from second 1 based on configured ratio & boost multiplier
            val rewardRatePerSecond = if (rule.productiveMinutesTarget > 0) {
                (rule.rewardMinutes.toDouble() / rule.productiveMinutesTarget.toDouble()) * wallet.effectiveBoostMultiplier
            } else {
                1.0 * wallet.effectiveBoostMultiplier
            }
            pendingSeconds += rewardRatePerSecond

            if (pendingSeconds >= 0.999999) {
                val addSec = Math.round(pendingSeconds).toLong()
                pendingSeconds = (pendingSeconds - addSec.toDouble()).coerceAtLeast(0.0)
                if (addSec > 0) {
                    walletManager.addRewardSeconds(addSec)
                }
            }
        } else {
            _isAccruing.value = false
            _activeProductivePackage.value = if (isProductiveApp) currentPackage else null
        }
    }

    companion object {
        const val IDLE_THRESHOLD_MS = 120_000L // 120 seconds idle timeout to accommodate audio/speaking intervals
    }
}
