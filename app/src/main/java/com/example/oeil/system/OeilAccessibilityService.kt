package com.example.oeil.system

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.example.oeil.R
import com.example.oeil.data.AndroidActionExecutor
import com.example.oeil.model.InstalledApp
import com.example.oeil.model.ScreenElement
import com.example.oeil.model.SystemAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OeilAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        OeilAccessibility.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            refreshTaskStatusOverlay()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        pendingApproval?.let { continuation ->
            pendingApproval = null
            if (continuation.isActive) continuation.resume(false)
        }
        removeApprovalOverlay()
        removeTaskStatusOverlay()
        OeilAccessibility.detach(this)
        super.onDestroy()
    }

    internal fun screenElements(): List<ScreenElement> {
        return withActiveNodes(allowMissingScreen = true) { nodes ->
            nodes.mapIndexedNotNull { index, node ->
                if (!node.isVisibleToUser || node.isPassword) return@mapIndexedNotNull null
                val label = node.label()
                if (label.isBlank()) return@mapIndexedNotNull null
                ScreenElement(
                    id = "e$index",
                    label = label.take(MAX_LABEL_LENGTH),
                    clickable = node.isClickable,
                    editable = node.isEditable
                )
            }.take(MAX_SCREEN_ELEMENTS)
        }
    }

    internal fun installedApps(): List<InstalledApp> {
        cachedApps?.let { return it }
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager
            .queryIntentActivities(launcherIntent, 0)
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                val packageName = activity.packageName
                if (label.isBlank() || packageName.isBlank()) null
                else InstalledApp(label = label.take(MAX_LABEL_LENGTH), packageName = packageName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .take(MAX_INSTALLED_APPS)
        cachedApps = apps
        return apps
    }

    internal suspend fun approveAndPerform(action: SystemAction): String? =
        withContext(Dispatchers.Main.immediate) {
            prepareTarget(action)
            if (!showApproval(action)) return@withContext null
            execute(action)
        }

    private fun prepareTarget(action: SystemAction) {
        if (action.type !in setOf("tap", "type")) return
        val id = action.targetId ?: error("The tap target is missing.")
        withActiveNodes { nodes ->
            val target = nodes.getOrNull(id.removePrefix("e").toIntOrNull() ?: -1)
                ?: error("That screen target is no longer available. Try again.")
            validateTarget(target, action)
            if (action.type == "type") {
                if (!target.isEditable || target.isPassword) {
                    error("The selected target is not a safe text field.")
                }
            } else {
                var clickable = target
                var ownsClickable = false
                while (!clickable.isClickable) {
                    val parent = clickable.parent ?: break
                    if (ownsClickable) clickable.recycle()
                    clickable = parent
                    ownsClickable = true
                }
                try {
                    if (!clickable.isClickable) error("That screen target is not clickable.")
                } finally {
                    if (ownsClickable) clickable.recycle()
                }
            }
        }
    }

    private fun execute(action: SystemAction): String {
        return when (action.type) {
            "tap" -> {
                withTarget(action) { target ->
                    var clickable = target
                    var ownsClickable = false
                    while (!clickable.isClickable) {
                        val parent = clickable.parent ?: break
                        if (ownsClickable) clickable.recycle()
                        clickable = parent
                        ownsClickable = true
                    }
                    val clicked = try {
                        clickable.isClickable &&
                            clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    } finally {
                        if (ownsClickable) clickable.recycle()
                    }
                    if (!clicked) error("Android could not tap the approved control.")
                    "Tapped “${action.targetLabel}”."
                }
            }
            "type" -> {
                withTarget(action) { target ->
                    val text = action.text?.takeIf { it.isNotBlank() }
                        ?: error("The text to enter is missing.")
                    if (!target.isEditable || target.isPassword) {
                        error("The approved target is not a safe text field.")
                    }
                    if (!target.isFocused) target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                    val arguments = Bundle().apply {
                        putCharSequence(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                            text
                        )
                    }
                    if (!target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)) {
                        error("Android could not enter text in the approved field.")
                    }
                    "Entered the approved text."
                }
            }
            "scroll" -> scroll(action)
            "back" -> {
                if (!performGlobalAction(GLOBAL_ACTION_BACK)) {
                    error("Android could not perform the Back action.")
                }
                "Went back."
            }
            "home" -> {
                if (!performGlobalAction(GLOBAL_ACTION_HOME)) {
                    error("Android could not go to the home screen.")
                }
                "Went to the home screen."
            }
            "recents" -> {
                if (!performGlobalAction(GLOBAL_ACTION_RECENTS)) {
                    error("Android could not open the recent apps switcher.")
                }
                "Opened recent apps."
            }
            "notifications" -> {
                if (!performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)) {
                    error("Android could not open the notification shade.")
                }
                "Opened notifications."
            }
            "open_app" -> openApp(action)
            "open_url" -> openUrl(action)
            else -> error("Unsupported system action.")
        }
    }

    private fun withTarget(action: SystemAction, block: (AccessibilityNodeInfo) -> String): String {
        val id = action.targetId ?: error("The screen target is missing.")
        return withActiveNodes { nodes ->
            val target = nodes.getOrNull(id.removePrefix("e").toIntOrNull() ?: -1)
                ?: error("That screen target is no longer available. Try again.")
            validateTarget(target, action)
            block(target)
        }
    }

    private fun validateTarget(target: AccessibilityNodeInfo, action: SystemAction) {
        val expectedLabel = action.targetLabel?.trim().orEmpty()
        if (!target.isVisibleToUser || expectedLabel.isBlank() ||
            !target.label().contains(expectedLabel, ignoreCase = true)
        ) {
            error("The screen changed while waiting for approval. No action was performed.")
        }
    }

    private fun scroll(action: SystemAction): String {
        val forward = action.direction == "forward"
        if (!forward && action.direction != "backward") {
            error("The scroll direction is invalid.")
        }
        return withActiveNodes { nodes ->
            val scrollable = nodes.asReversed().firstOrNull { it.isScrollable }
                ?: error("There is no scrollable area on this screen.")
            val scrollAction = if (forward) {
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            } else {
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            }
            if (!scrollable.performAction(scrollAction)) {
                error("Android could not scroll this screen.")
            }
            if (forward) "Scrolled down." else "Scrolled up."
        }
    }

    private fun openApp(action: SystemAction): String {
        val requestedPackage = action.packageName?.trim()?.takeIf { it.isNotBlank() }
        val requestedName = action.appName?.trim()?.takeIf { it.isNotBlank() }

        val packageName = when {
            requestedPackage != null &&
                packageManager.getLaunchIntentForPackage(requestedPackage) != null ->
                requestedPackage

            else -> installedApps()
                .firstOrNull { app ->
                    requestedName != null && app.label.equals(requestedName, ignoreCase = true)
                }
                ?.packageName
                ?: installedApps()
                    .firstOrNull { app ->
                        requestedName != null &&
                            app.label.contains(requestedName, ignoreCase = true)
                    }
                    ?.packageName
                ?: requestedPackage?.takeIf {
                    packageManager.getLaunchIntentForPackage(it) != null
                }
                ?: error("That app is not installed or cannot be opened.")
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: error("That app is not installed or cannot be opened.")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(launchIntent)
        return "Opened $packageName."
    }

    private fun openUrl(action: SystemAction): String {
        val url = action.url?.trim()?.takeIf { it.isNotBlank() }
            ?: error("The link to open is missing.")
        if (!url.startsWith("http://", ignoreCase = true) &&
            !url.startsWith("https://", ignoreCase = true)
        ) {
            error("Only http and https links can be opened.")
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
        return "Opened $url."
    }

    private fun flatten(root: AccessibilityNodeInfo, limit: Int): List<AccessibilityNodeInfo> {
        val nodes = ArrayList<AccessibilityNodeInfo>(minOf(limit, 64))
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        while (pending.isNotEmpty() && nodes.size < limit) {
            val node = pending.removeFirst()
            nodes.add(node)
            for (index in node.childCount - 1 downTo 0) {
                node.getChild(index)?.let(pending::addFirst)
            }
        }
        pending.forEach(AccessibilityNodeInfo::recycle)
        return nodes
    }

    private inline fun <T> withActiveNodes(
        allowMissingScreen: Boolean = false,
        block: (List<AccessibilityNodeInfo>) -> T
    ): T {
        val root = activeAppRoot()
        if (root == null && allowMissingScreen) return block(emptyList())
        root ?: error("No screen is available.")
        val nodes = flatten(root, MAX_TREE_NODES)
        return try {
            block(nodes)
        } finally {
            nodes.forEach(AccessibilityNodeInfo::recycle)
        }
    }

    private fun activeAppRoot(): AccessibilityNodeInfo? {
        val windows = windows
        return try {
            val appWindow = windows
                .filter {
                    it.type != AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY &&
                        it.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }
                .maxByOrNull { window ->
                    (if (window.isFocused) 2 else 0) + (if (window.isActive) 1 else 0)
                }
            appWindow?.root ?: rootInActiveWindow
        } finally {
            windows.forEach(AccessibilityWindowInfo::recycle)
        }
    }

    private suspend fun showApproval(action: SystemAction): Boolean =
        suspendCancellableCoroutine { continuation ->
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = approvalView(action) { approved ->
                removeApprovalOverlay()
                if (pendingApproval === continuation) pendingApproval = null
                if (continuation.isActive) continuation.resume(approved)
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            approvalOverlay = view
            pendingApproval = continuation
            try {
                windowManager.addView(view, params)
            } catch (error: RuntimeException) {
                approvalOverlay = null
                pendingApproval = null
                continuation.resumeWithException(error)
            }
            continuation.invokeOnCancellation {
                Handler(Looper.getMainLooper()).post {
                    if (pendingApproval === continuation) pendingApproval = null
                    removeApprovalOverlay()
                }
            }
        }

    private fun approvalView(action: SystemAction, decide: (Boolean) -> Unit): View {
        val overlay = FrameLayout(this).apply {
            setBackgroundColor(0x66000000)
            isClickable = true
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.rgb(17, 28, 22))
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Color.rgb(70, 105, 78))
            }
            elevation = dp(12).toFloat()
        }
        panel.addView(TextView(this).apply {
            text = getString(R.string.accessibility_approval_title)
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, 0, 0, dp(8))
        })
        panel.addView(TextView(this).apply {
            text = action.summary
            setTextColor(Color.WHITE)
            textSize = 16f
            setPadding(0, 0, 0, dp(8))
        })
        val detail = buildList {
            action.targetLabel?.let {
                add(getString(R.string.accessibility_approval_screen_label, it))
            }
            action.text?.let {
                add(getString(R.string.accessibility_approval_text_label, it))
            }
            action.packageName?.let {
                add(getString(R.string.accessibility_approval_app_label, it))
            }
        }.joinToString("\n")
        if (detail.isNotBlank()) {
            panel.addView(TextView(this).apply {
                text = detail
                setTextColor(Color.LTGRAY)
                textSize = 14f
                setPadding(0, 0, 0, dp(8))
            })
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        buttons.addView(Button(this).apply {
            text = getString(R.string.accessibility_approval_deny)
            setOnClickListener { decide(false) }
        })
        buttons.addView(Button(this).apply {
            text = getString(R.string.accessibility_approval_allow)
            setOnClickListener { decide(true) }
        })
        panel.addView(buttons)
        overlay.addView(
            panel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP
            ).apply {
                setMargins(dp(16), dp(36), dp(16), 0)
            }
        )
        return overlay
    }

    private fun removeApprovalOverlay() {
        val view = approvalOverlay ?: return
        approvalOverlay = null
        if (view.parent == null) return
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.removeView(view)
    }

    internal fun updateTaskStatus(detail: String?) {
        taskStatusHandler.post {
            taskStatus = detail?.takeIf { it.isNotBlank() }
            refreshTaskStatusOverlay()
        }
    }

    private fun refreshTaskStatusOverlay() {
        val detail = taskStatus
        if (detail == null || !isOeilInBackground()) {
            removeTaskStatusOverlay()
            return
        }
        showTaskStatusOverlay(detail)
    }

    private fun isOeilInBackground(): Boolean {
        // Accessibility overlays (approval and status) are not app windows. Use
        // the top application window so the banner stays up while a dialog is
        // awaiting an answer.
        val foreground = windows
            .filter { it.type == AccessibilityWindowInfo.TYPE_APPLICATION }
            .maxByOrNull { it.layer }
            ?.root?.packageName?.toString()
            ?: rootInActiveWindow?.packageName?.toString()
            ?: return false
        return foreground != packageName
    }

    private fun showTaskStatusOverlay(detail: String) {
        if (taskStatusOverlay != null) {
            taskStatusLabel?.text = statusText(detail)
            return
        }
        val label = TextView(this).apply {
            text = statusText(detail)
            setTextColor(Color.WHITE)
            textSize = 13f
            maxLines = 2
            maxWidth = dp(320)
        }
        val pill = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(8), dp(18), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.rgb(17, 28, 22))
                cornerRadius = dp(22).toFloat()
                setStroke(dp(1), Color.rgb(70, 105, 78))
            }
            elevation = dp(8).toFloat()
        }
        pill.addView(ProgressBar(this, null, android.R.attr.progressBarStyleSmall).apply {
            indeterminateTintList = ColorStateList.valueOf(Color.rgb(103, 200, 144))
            layoutParams = LinearLayout.LayoutParams(dp(18), dp(18)).apply {
                rightMargin = dp(10)
            }
        })
        pill.addView(label)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = dp(72)
        }
        try {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.addView(pill, params)
            taskStatusOverlay = pill
            taskStatusLabel = label
        } catch (_: RuntimeException) {
            taskStatusOverlay = null
            taskStatusLabel = null
        }
    }

    private fun statusText(detail: String): String =
        if (detail.isBlank()) {
            getString(R.string.accessibility_status_working)
        } else {
            getString(R.string.accessibility_status_detail, detail)
        }

    private fun removeTaskStatusOverlay() {
        taskStatusLabel = null
        val view = taskStatusOverlay ?: return
        taskStatusOverlay = null
        if (view.parent == null) return
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.removeView(view)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun AccessibilityNodeInfo.label(): String = buildString {
        text?.toString()?.trim()?.takeIf { it.isNotBlank() }?.let(::append)
        contentDescription?.toString()?.trim()?.takeIf {
            it.isNotBlank() && !contains(it, ignoreCase = true)
        }?.let {
            if (isNotEmpty()) append(" — ")
            append(it)
        }
        if (isEmpty() && isEditable && !isPassword) append("Text field")
    }

    private companion object {
        const val MAX_TREE_NODES = 250
        const val MAX_SCREEN_ELEMENTS = 100
        const val MAX_LABEL_LENGTH = 500
        const val MAX_INSTALLED_APPS = 300
    }

    private var approvalOverlay: View? = null
    private var pendingApproval: CancellableContinuation<Boolean>? = null
    private var taskStatus: String? = null
    private var taskStatusOverlay: View? = null
    private var taskStatusLabel: TextView? = null
    private val taskStatusHandler = Handler(Looper.getMainLooper())
    @Volatile private var cachedApps: List<InstalledApp>? = null

}

object OeilAccessibility : AndroidActionExecutor {
    private val _connected = MutableStateFlow(false)
    val connected = _connected.asStateFlow()
    @Volatile private var service = WeakReference<OeilAccessibilityService>(null)

    internal fun attach(value: OeilAccessibilityService) {
        service = WeakReference(value)
        _connected.value = true
    }

    internal fun detach(value: OeilAccessibilityService) {
        if (service.get() === value) {
            service.clear()
            _connected.value = false
        }
    }

    override suspend fun screenElements(): List<ScreenElement> = withContext(Dispatchers.Main.immediate) {
        service.get()?.screenElements().orEmpty()
    }

    override suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        service.get()?.installedApps().orEmpty()
    }

    override fun updateTaskStatus(detail: String?) {
        service.get()?.updateTaskStatus(detail)
    }

    override suspend fun approveAndPerform(action: SystemAction): String? = withContext(Dispatchers.Main.immediate) {
        val active = service.get() ?: error(
            "Enable Oeil system control in Android Accessibility settings to perform actions."
        )
        active.approveAndPerform(action)
    }

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
