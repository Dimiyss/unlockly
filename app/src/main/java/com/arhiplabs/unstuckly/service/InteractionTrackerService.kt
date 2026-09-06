package com.arhiplabs.unstuckly.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.app.ActivityManager
import android.content.Context
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.arhiplabs.unstuckly.UnstucklyApplication
import com.arhiplabs.unstuckly.domain.PipHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PipWindowInfo(
    val packageName: String,
    val bounds: Rect
)

class InteractionTrackerService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var cachedBlockedPackages = setOf<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.flags = info.flags or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            serviceInfo = info
        } catch (e: Exception) {
            e.printStackTrace()
        }

        instance = this
        _isServiceActive.value = true

        // Cache active rule's blocked packages for fast lookup
        serviceScope.launch {
            try {
                UnstucklyApplication.instance.ruleRepository.activeRules.collect { rules ->
                    cachedBlockedPackages = rules.flatMap { it.blockedPackages }.toSet()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        _lastInteractionTimestamp.value = now

        val pkgName = event.packageName?.toString()
        if (!pkgName.isNullOrEmpty() && cachedBlockedPackages.contains(pkgName)) {
            _lastKnownBlockedPackage.value = pkgName
        }

        // Monitor PiP changes immediately whenever window states or windows change
        if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            checkPipState()
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SCROLLED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
                if (!pkgName.isNullOrEmpty() && pkgName != currentPackage.value) {
                    _currentPackage.value = pkgName
                    serviceScope.launch {
                        try {
                            UnstucklyApplication.instance.blockingCoordinator.evaluateForegroundPackage(pkgName)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun detectPipPackage(): String? = detectPipWindowInfo()?.packageName

    fun detectPipWindowInfo(): PipWindowInfo? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val windowList = windows
                if (!windowList.isNullOrEmpty()) {
                    for (window in windowList) {
                        if (window.isInPictureInPictureMode) {
                            val bounds = Rect()
                            window.getBoundsInScreen(bounds)

                            // 1. Try window root packageName
                            var pkg = window.root?.packageName?.toString()

                            // 2. Try window title
                            if (pkg.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val title = window.title?.toString()?.lowercase() ?: ""
                                if (title.contains("youtube")) {
                                    pkg = "com.google.android.youtube"
                                }
                            }

                            // 3. Try visible running processes matching blocked packages
                            if (pkg.isNullOrEmpty()) {
                                pkg = resolvePipPackageFromProcesses()
                            }

                            // 4. Fallback to last known blocked package or YouTube
                            if (pkg.isNullOrEmpty()) {
                                pkg = _lastKnownBlockedPackage.value
                            }
                            if (pkg.isNullOrEmpty()) {
                                pkg = "com.google.android.youtube"
                            }

                            return PipWindowInfo(pkg, bounds)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private fun resolvePipPackageFromProcesses(): String? {
        try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return null
            val processes = am.runningAppProcesses ?: return null
            val candidates = if (cachedBlockedPackages.isNotEmpty()) {
                cachedBlockedPackages
            } else {
                setOf("com.google.android.youtube")
            }

            for (proc in processes) {
                if (proc.importance <= ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    for (pkg in proc.pkgList ?: emptyArray()) {
                        if (candidates.contains(pkg)) {
                            return pkg
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun dismissPipByGesture(bounds: Rect): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        if (bounds.isEmpty) return false

        val displayMetrics = resources.displayMetrics
        val startX = bounds.centerX().toFloat()
        val startY = bounds.centerY().toFloat()
        // Universal Android PiP dismiss target: bottom center of screen
        val endX = (displayMetrics.widthPixels / 2).toFloat()
        val endY = (displayMetrics.heightPixels - 50).toFloat()

        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 250))
            .build()

        return dispatchGesture(gesture, null, null)
    }

    fun dismissPipWindow(targetPackage: String? = null): Boolean {
        val pipInfo = detectPipWindowInfo()
        if (pipInfo != null) {
            if (targetPackage == null || pipInfo.packageName == targetPackage) {
                // 1. Dispatch universal drag-to-dismiss gesture
                dismissPipByGesture(pipInfo.bounds)

                // 2. Kill background process to terminate media player
                PipHelper.killAppProcess(this, pipInfo.packageName)
                PipHelper.pauseMediaPlayback(this)

                // 3. Try clicking dismiss node in hierarchy if accessible
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try {
                        windows?.forEach { w ->
                            if (w.isInPictureInPictureMode) {
                                val root = w.root
                                root?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS.id)
                                tryDismissNode(root)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return true
            }
        }
        return false
    }

    private fun tryDismissNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        try {
            for (action in node.actionList) {
                if (action.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS.id) {
                    if (node.performAction(action.id)) return true
                }
            }
            val count = node.childCount
            for (i in 0 until count) {
                val child = node.getChild(i) ?: continue
                val desc = child.contentDescription?.toString()?.lowercase() ?: ""
                val text = child.text?.toString()?.lowercase() ?: ""
                val resName = child.viewIdResourceName?.lowercase() ?: ""
                if (desc.contains("close") || desc.contains("dismiss") ||
                    text.contains("close") || text.contains("dismiss") ||
                    resName.contains("close") || resName.contains("dismiss")
                ) {
                    if (child.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                        return true
                    }
                }
                if (tryDismissNode(child)) {
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun checkPipState() {
        val pipInfo = detectPipWindowInfo()
        val pipPkg = pipInfo?.packageName
        _currentPipPackage.value = pipPkg
        if (!pipPkg.isNullOrEmpty()) {
            serviceScope.launch {
                try {
                    UnstucklyApplication.instance.blockingCoordinator.evaluatePipPackage(pipPkg)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onInterrupt() {
        _isServiceActive.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) {
            instance = null
        }
        _isServiceActive.value = false
    }

    companion object {
        var instance: InteractionTrackerService? = null
            private set

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        private val _lastInteractionTimestamp = MutableStateFlow(System.currentTimeMillis())
        val lastInteractionTimestamp: StateFlow<Long> = _lastInteractionTimestamp.asStateFlow()

        fun updateLastInteractionTimestamp(timestamp: Long = System.currentTimeMillis()) {
            _lastInteractionTimestamp.value = timestamp
        }

        private val _currentPackage = MutableStateFlow("")
        val currentPackage: StateFlow<String> = _currentPackage.asStateFlow()

        private val _lastKnownBlockedPackage = MutableStateFlow<String?>(null)
        val lastKnownBlockedPackage: StateFlow<String?> = _lastKnownBlockedPackage.asStateFlow()

        private val _currentPipPackage = MutableStateFlow<String?>(null)
        val currentPipPackage: StateFlow<String?> = _currentPipPackage.asStateFlow()
    }
}
