package com.antigravity.unlockly.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.antigravity.unlockly.UnlocklyApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InteractionTrackerService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceActive.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        _lastInteractionTimestamp.value = now

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                val pkgName = event.packageName?.toString()
                if (!pkgName.isNullOrEmpty() && pkgName != currentPackage.value) {
                    _currentPackage.value = pkgName
                    serviceScope.launch {
                        try {
                            UnlocklyApplication.instance.blockingCoordinator.evaluateForegroundPackage(pkgName)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceActive.value = false
    }

    companion object {
        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _lastInteractionTimestamp = MutableStateFlow(System.currentTimeMillis())
        val lastInteractionTimestamp: StateFlow<Long> = _lastInteractionTimestamp.asStateFlow()

        private val _currentPackage = MutableStateFlow("")
        val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()
    }
}
