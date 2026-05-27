package com.example.axondroid

object ActionParser {
    // 1. CLICK(x, y)
    private val clickRegex = Regex("""\s*CLICK\(\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 2. LONG_PRESS(x, y)
    private val longPressRegex = Regex("""\s*LONG_PRESS\(\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 3. SWIPE(x1, y1, x2, y2)
    private val swipeRegex = Regex("""\s*SWIPE\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 4. DRAG(x1, y1, x2, y2)
    private val dragRegex = Regex("""\s*DRAG\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 5 INPUT_TEXT_AT_CURRENT_FOCUS(text)
    private val inputRegex = Regex("""\s*INPUT_TEXT_AT_CURRENT_FOCUS\(\s*([^)]+)\s*\)""", RegexOption.IGNORE_CASE)

    fun parseActionsAndExecute(response: String, service: MyAccessibilityService): Boolean  {
        // 伪代码：解析 AI 返回的逻辑链
        val actions = parseActionList(response)
        for (action in actions) {
            val complete = execute(action, service)
            if (!complete) return false
            Thread.sleep(200) // 快速连续操作
        }
        return true
    }
    fun parseActionList(aiResponse: String): List<String> {
        // 匹配模式：大写字母指令 + 括号 + 括号内非右括号的内容
        // 例如：CLICK(12, 34) 或 INPUT("hi, logic")
        val pattern = Regex("""[A-Z_]+\s*\(.*?\)|[A-Z_]+(?=\s*[,\]])""")

        return pattern.findAll(aiResponse).map { it.value.trim() }.toList()
    }

    fun execute(action: String, service: MyAccessibilityService): Boolean {

        when {
            // 处理点击
            clickRegex.containsMatchIn(action) -> {
                clickRegex.find(action)?.let {
                    val x = it.groupValues[1].toFloat()
                    val y = it.groupValues[2].toFloat()
                    service.simulateClick(x, y)
                }
            }

            // 处理长按
            longPressRegex.containsMatchIn(action) -> {
                longPressRegex.find(action)?.let {
                    val x = it.groupValues[1].toFloat()
                    val y = it.groupValues[2].toFloat()
                    service.simulateLongPress(x, y)
                }
            }

            // 处理滑动
            swipeRegex.containsMatchIn(action) -> {
                swipeRegex.find(action)?.let {
                    val x1 = it.groupValues[1].toFloat()
                    val y1 = it.groupValues[2].toFloat()
                    val x2 = it.groupValues[3].toFloat()
                    val y2 = it.groupValues[4].toFloat()
                    service.simulateSwipe(x1, y1, x2, y2, durationMs = 500)
                }
            }

            // 处理拖拽 (时长通常比滑动更长，以触发长按拾起效果)
            dragRegex.containsMatchIn(action) -> {
                dragRegex.find(action)?.let {
                    val x1 = it.groupValues[1].toFloat()
                    val y1 = it.groupValues[2].toFloat()
                    val x2 = it.groupValues[3].toFloat()
                    val y2 = it.groupValues[4].toFloat()
                    service.simulateDrag(x1, y1, x2, y2)
                }
            }

            // 处理输入
            inputRegex.containsMatchIn(action) -> {
                inputRegex.find(action)?.let {
                    val text = it.groupValues[1].trim()
                    service.inputTextAtCurrentFocus( text)
                }
            }

            // 处理截图请求
            action.contains("SNAPSHOT_REQUIRED", ignoreCase = true) -> {
                // 回调给 Activity 执行截屏逻辑
            }

            // 处理enter
            action.contains("PRESS_ENTER", ignoreCase = true) -> {
                // 回调给 Activity 执行截屏逻辑
                service.performEditorActionSearch()
            }

            // 处理截图请求
            action.contains("TASK_COMPLETE", ignoreCase = true) -> {
                // 回调给 Activity 执行截屏逻辑
                return false
            }
        }
        return true
    }
}