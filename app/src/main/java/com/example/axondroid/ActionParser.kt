package com.example.axondroid

object ActionParser {
    // 1. CLICK(x, y)
    private val clickRegex = Regex("""ACTION:\s*CLICK\(\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 2. LONG_PRESS(x, y)
    private val longPressRegex = Regex("""ACTION:\s*LONG_PRESS\(\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 3. SWIPE(x1, y1, x2, y2)
    private val swipeRegex = Regex("""ACTION:\s*SWIPE\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 4. DRAG(x1, y1, x2, y2)
    private val dragRegex = Regex("""ACTION:\s*DRAG\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 5 & 6. INPUT(target, text) - 兼容 hintText 或 resourceId
    private val inputRegex = Regex("""ACTION:\s*INPUT\(\s*([^,)]+)\s*,\s*([^)]+)\s*\)""", RegexOption.IGNORE_CASE)

    fun parseAndExecute(response: String, service: MyAccessibilityService) {
        val cleanResponse = response.trim()

        when {
            // 处理点击
            clickRegex.containsMatchIn(cleanResponse) -> {
                clickRegex.find(cleanResponse)?.let {
                    val x = it.groupValues[1].toFloat()
                    val y = it.groupValues[2].toFloat()
                    service.simulateClick(x, y)
                }
            }

            // 处理长按
            longPressRegex.containsMatchIn(cleanResponse) -> {
                longPressRegex.find(cleanResponse)?.let {
                    val x = it.groupValues[1].toFloat()
                    val y = it.groupValues[2].toFloat()
                    service.simulateLongPress(x, y)
                }
            }

            // 处理滑动
            swipeRegex.containsMatchIn(cleanResponse) -> {
                swipeRegex.find(cleanResponse)?.let {
                    val x1 = it.groupValues[1].toFloat()
                    val y1 = it.groupValues[2].toFloat()
                    val x2 = it.groupValues[3].toFloat()
                    val y2 = it.groupValues[4].toFloat()
                    service.simulateSwipe(x1, y1, x2, y2, durationMs = 500)
                }
            }

            // 处理拖拽 (时长通常比滑动更长，以触发长按拾起效果)
            dragRegex.containsMatchIn(cleanResponse) -> {
                dragRegex.find(cleanResponse)?.let {
                    val x1 = it.groupValues[1].toFloat()
                    val y1 = it.groupValues[2].toFloat()
                    val x2 = it.groupValues[3].toFloat()
                    val y2 = it.groupValues[4].toFloat()
                    service.simulateDrag(x1, y1, x2, y2)
                }
            }

            // 处理输入
            inputRegex.containsMatchIn(cleanResponse) -> {
                inputRegex.find(cleanResponse)?.let {
                    val target = it.groupValues[1].trim()
                    val text = it.groupValues[2].trim()
                    service.inputTextByText(target, text)
                }
            }

            // 处理截图请求
            cleanResponse.contains("SNAPSHOT_REQUIRED", ignoreCase = true) -> {
                // 回调给 Activity 执行截屏逻辑
            }
        }
    }
}