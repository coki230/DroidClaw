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
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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


    // 在 MyAccessibilityService 类中
    suspend fun captureScreenSuspend(): Bitmap? = suspendCancellableCoroutine { continuation ->
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                takeScreenshot(
                    Display.DEFAULT_DISPLAY,
                    java.util.concurrent.Executors.newSingleThreadExecutor(),
                    object : TakeScreenshotCallback {
                        override fun onSuccess(screenshot: ScreenshotResult) {
                            // 1. 从硬件缓冲获取 Bitmap
                            val hwBitmap = Bitmap.wrapHardwareBuffer(
                                screenshot.hardwareBuffer,
                                screenshot.colorSpace ?: android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB)
                            )

                            // 2. 关键：Hardware Bitmap 不能直接压缩，需要转为软件 Bitmap
                            val softwareBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)

                            // 3. 恢复协程
                            continuation.resume(softwareBitmap)
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.e("Agent", "截屏失败，错误码: $errorCode")
                            continuation.resume(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("Agent", "调用截屏 API 异常: ${e.message}")
                continuation.resume(null)
            }
        } else {
            Log.e("Agent", "当前 Android 版本过低，不支持 takeScreenshot")
            continuation.resume(null)
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
        showVisualFeedback(x, y) // 先画个红点提醒我
        val (realX, realY) = CoordinateMapper.mapToOriginal(x, y)
        val path = Path().apply { moveTo(realX, realY) }
        val stroke = StrokeDescription(path, 0L, durationMs, false)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // 模拟长按
    fun simulateLongPress(x: Float, y: Float, durationMs: Long = 800L) {  // >500ms 为长按
        val (realX, realY) = CoordinateMapper.mapToOriginal(x, y)
        simulateClick(realX, realY, durationMs)
    }

    // 模拟滑动（从 (x1,y1) 到 (x2,y2)）
    fun simulateSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 600L) {
        val (realX1, realY1) = CoordinateMapper.mapToOriginal(x1, y1)
        val (realX2, realY2) = CoordinateMapper.mapToOriginal(x2, y2)

        val path = Path().apply {
            moveTo(realX1, realY1)
            lineTo(realX2, realY2)  // 直线滑动，可改成 quadTo / cubicTo 做曲线
        }
        val stroke = StrokeDescription(path, 0L, durationMs, false)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    // 模拟拖拽（长按后移动）
    fun simulateDrag(x1: Float, y1: Float, x2: Float, y2: Float) {
        val (realX1, realY1) = CoordinateMapper.mapToOriginal(x1, y1)
        val (realX2, realY2) = CoordinateMapper.mapToOriginal(x2, y2)
        val path = Path().apply {
            moveTo(realX1, realY1)
            lineTo(realX2, realY2)
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

    fun inputTextAtCurrentFocus(content: String) {
        // 1. 获取当前屏幕上获得焦点的节点（通常是闪烁光标所在的输入框）
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focusedNode != null) {
            // 2. 执行输入动作
            val arguments = android.os.Bundle()
            arguments.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                content
            )
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

            if (success) {
                Log.i("AxonDroid", "成功在当前焦点输入: $content")
            } else {
                Log.e("AxonDroid", "输入动作执行失败（可能该节点不支持 SET_TEXT）")
            }

            // 记得回收节点防止内存泄漏
            focusedNode.recycle()
        } else {
            Log.e("AxonDroid", "当前屏幕没有找到任何聚焦的输入框")
        }
    }

    fun performEditorActionSearch() {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focusedNode != null) {
            // 执行动作：类似于点击键盘右下角的“搜索”或“回车”
            val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE.hashCode())
            // 注意：标准做法是使用 performAction(AccessibilityNodeInfo.ACTION_IME_ENTER)
            // 但兼容性最好的是下面这个：
            if(!success) {
                val bundle = Bundle()
                bundle.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, EditorInfo.IME_ACTION_SEARCH)
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle) // 某些系统支持
            }

            // 通用兜底：发送回车键事件
//            focusedNode.recycle()
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

    fun showVisualFeedback(x: Float, y: Float) {
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.RED)
                setStroke(2, Color.WHITE)
            }
            alpha = 0.8f
        }

        val params = WindowManager.LayoutParams(
            60, 60, // 圆圈大小
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = (x.toInt() - 30)
            this.y = (y.toInt() - 30)
        }

        windowManager.addView(view, params)

        // 500ms 后自动移除红点
        view.postDelayed({
            try { windowManager.removeView(view) } catch (e: Exception) {}
        }, 500)
    }
}