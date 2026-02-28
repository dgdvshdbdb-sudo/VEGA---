package com.vega.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class VegaAccessibilityService : AccessibilityService() {

    companion object {
        var instance: VegaAccessibilityService? = null
        const val TAG = "VegaAccService"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Toast.makeText(this, "✅ VEGA Accessibility Active!", Toast.LENGTH_LONG).show()
        Log.d(TAG, "Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                Log.d(TAG, "App: ${event.packageName} — ${event.className}")
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                Log.d(TAG, "Clicked: ${event.text}")
            }
        }
    }

    override fun onInterrupt() {
        instance = null
        Toast.makeText(this, "VEGA Accessibility Off", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // ─── GESTURE CONTROLS ─────────────────────────────────

    /** Kisi bhi coordinate pe click karo */
    fun clickAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Click at ($x, $y)")
    }

    /** Swipe gesture — scroll ke liye */
    fun swipe(fromX: Float, fromY: Float, toX: Float, toY: Float, duration: Long = 300) {
        val path = Path().apply {
            moveTo(fromX, fromY)
            lineTo(toX, toY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()
        dispatchGesture(gesture, null, null)
        Log.d(TAG, "Swipe ($fromX,$fromY) -> ($toX,$toY)")
    }

    /** Screen pe upar scroll karo */
    fun scrollUp() {
        val display = resources.displayMetrics
        swipe(display.widthPixels / 2f, display.heightPixels * 0.7f,
              display.widthPixels / 2f, display.heightPixels * 0.3f)
    }

    /** Screen pe neeche scroll karo */
    fun scrollDown() {
        val display = resources.displayMetrics
        swipe(display.widthPixels / 2f, display.heightPixels * 0.3f,
              display.widthPixels / 2f, display.heightPixels * 0.7f)
    }

    /** Back button press */
    fun pressBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /** Home button press */
    fun pressHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /** Recent apps */
    fun pressRecents() {
        performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /** Notifications panel kholna */
    fun openNotifications() {
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /** Quick settings kholna */
    fun openQuickSettings() {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    // ─── NODE FINDER ──────────────────────────────────────

    /** Text se UI element dhundo aur click karo */
    fun clickNodeWithText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val node = findNodeByText(root, text)
        return if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "Clicked node: $text")
            true
        } else {
            Log.d(TAG, "Node not found: $text")
            false
        }
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (nodeText.contains(text.lowercase()) || nodeDesc.contains(text.lowercase())) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) return found
        }
        return null
    }

    /** Current screen ka text extract karo */
    fun getCurrentScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        return extractText(root)
    }

    private fun extractText(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) sb.append(text).append(" ")
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            sb.append(extractText(child))
        }
        return sb.toString()
    }
}
