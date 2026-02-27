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
import android.accessibilityservice.GestureDescription.StrokeDescription

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


    // 查找控件（按 text 或 id）
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.firstOrNull()
    }

    fun findNodeById(id: String): AccessibilityNodeInfo? {
        return rootInActiveWindow?.findAccessibilityNodeInfosByViewId(id)?.firstOrNull()
    }

    // 模拟点击（用手势注入，像人手指点）
    fun simulateClick(x: Float, y: Float, durationMs: Long = 50L) {  // 短按 50ms
        val path = Path().apply { moveTo(x, y) }
        val stroke = StrokeDescription(path, 0L, durationMs, false)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // 模拟长按
    fun simulateLongPress(x: Float, y: Float, durationMs: Long = 800L) {  // >500ms 为长按
        simulateClick(x, y, durationMs)
    }

    // 模拟滑动（从 (x1,y1) 到 (x2,y2)）
    fun simulateSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 600L) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)  // 直线滑动，可改成 quadTo / cubicTo 做曲线
        }
        val stroke = StrokeDescription(path, 0L, durationMs, false)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // 模拟拖拽（长按后移动）
    fun simulateDrag(x1: Float, y1: Float, x2: Float, y2: Float) {
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo(x2, y2)
        }
        val stroke = StrokeDescription(path, 0L, 1200L, false)  // 总时长 1.2s
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // 输入文字（直接设值，像人打字但更快）
    fun inputText(node: AccessibilityNodeInfo?, text: String) {
        node?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, android.os.Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        })
    }

    // 根据 文本内容 查找并输入文字
    fun inputTextByText(targetText: String, content: String) {
        // 1. 先根据 LLM 传来的字符串找到对应的节点
        val node = findNodeByText(targetText)

        if (node != null) {
            // 2. 如果找到了，执行输入
            val arguments = android.os.Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                content
            )
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.i("AxonDroid", "已在 [$targetText] 中输入: $content")
        } else {
            Log.e("AxonDroid", "找不到包含文本 [$targetText] 的输入框")
        }
    }

    // 示例：点击某个按钮（先找控件，取中心点坐标）
    fun clickButtonByText(buttonText: String) {
        val node = findNodeByText(buttonText)
        if (node != null) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val centerX = (bounds.left + bounds.right) / 2f
            val centerY = (bounds.top + bounds.bottom) / 2f
            simulateClick(centerX, centerY)
        }
    }

    // 模拟系统操作（如返回键）
    fun performBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performHome() {
        performGlobalAction(GLOBAL_ACTION_HOME)
    }
}