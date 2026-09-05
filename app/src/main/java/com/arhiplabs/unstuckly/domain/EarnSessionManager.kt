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
            val prevStudySeconds = wallet.productiveStudySecondsToday
            val newStudySeconds = prevStudySeconds + 1L
            walletManager.addProductiveStudySeconds(1L)

            val targetSeconds = rule.productiveMinutesTarget * 60L
            val rewardPerTargetSeconds = (rule.rewardMinutes * 60L * wallet.effectiveBoostMultiplier).toLong()

            // 2. Initial minimum interval milestone
            if (prevStudySeconds < targetSeconds) {
                // User is working toward their first minimum study requirement of the day
                if (newStudySeconds >= targetSeconds) {
                    // Milestone achieved! Deposit first full interval reward
                    walletManager.addRewardSeconds(rewardPerTargetSeconds)
                }
            } else {
                // Initial daily target was already fulfilled. Accrue subsequent rewards incrementally
                val rewardRatePerSecond = (rule.rewardMinutes.toDouble() / rule.productiveMinutesTarget.toDouble()) * wallet.effectiveBoostMultiplier
                pendingSeconds += rewardRatePerSecond

                if (pendingSeconds >= 1.0) {
                    val addSec = pendingSeconds.toLong()
                    pendingSeconds -= addSec
                    walletManager.addRewardSeconds(addSec)
                }
            }
        } else {
            _isAccruing.value = false
            _activeProductivePackage.value = if (isProductiveApp) currentPackage else null
        }
    }

    companion object {
        const val IDLE_THRESHOLD_MS = 60_000L // 60 seconds idle timeout as per plan specification
    }
}
