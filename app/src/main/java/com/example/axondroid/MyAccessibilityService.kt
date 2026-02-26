package com.example.axondroid

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.Rect
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class MyAccessibilityService : AccessibilityService() {

    companion object {
        // 单例引用，方便 ApiClient 找到当前的 Service 实例
        var instance: MyAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this // 服务启动时保存实例
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null // 服务关闭时清空
    }

    // 执行模拟点击的核心逻辑
    fun performClickAction(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                // 点击成功后，可以在这里通知 AI 执行下一步
            }
        }, null)
    }

//    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
//        // 这里的逻辑通常由 MainActivity 触发 API 后回调执行
//    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                val className = event.className?.toString()
                Log.i("AxonDroid", "检测到页面切换: $packageName / $className")
                // 你可以在这里自动触发一次 AI 思考
            }
        }
    }

    // 【感知】抓取当前屏幕所有可点击元素的文本和坐标
    fun captureUIStructure(): String {
        val rootNode = rootInActiveWindow ?: return "No Window"
        val sb = StringBuilder()
        traverseNode(rootNode, sb)
        return sb.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, sb: StringBuilder) {
        if (node.isClickable) {
            val rect = Rect()
            node.getBoundsInScreen(rect)
            val text = node.text ?: node.contentDescription ?: "Unnamed"
            sb.append("元素: $text, 坐标: [${rect.centerX()}, ${rect.centerY()}]\n")
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { traverseNode(it, sb) }
        }
    }

    // 【执行】根据坐标模拟点击
    fun performAction(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 100)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun captureScreen(onComplete: (Bitmap?) -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // 使用 Android 11+ 的无障碍截屏 API
            takeScreenshot(
                Display.DEFAULT_DISPLAY, java.util.concurrent.Executors.newSingleThreadExecutor(),
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        val bitmap = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                        onComplete(bitmap)
                    }
                    override fun onFailure(errorCode: Int) {
                        onComplete(null)
                    }
                })
        } else {
            // 旧版本通常需要 MediaProjection API，比较复杂
            onComplete(null)
        }
    }
}