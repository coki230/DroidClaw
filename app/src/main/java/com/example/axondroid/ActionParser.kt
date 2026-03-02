package com.example.axondroid

object ActionParser {
    // 1. CLICK(x, y)
    private val clickRegex = Regex("""ACTIONS:\s*CLICK\(\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 2. LONG_PRESS(x, y)
    private val longPressRegex = Regex("""ACTION:\s*LONG_PRESS\(\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 3. SWIPE(x1, y1, x2, y2)
    private val swipeRegex = Regex("""ACTION:\s*SWIPE\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 4. DRAG(x1, y1, x2, y2)
    private val dragRegex = Regex("""ACTION:\s*DRAG\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)

    // 5 INPUT_TEXT_AT_CURRENT_FOCUS(text)
    private val inputRegex = Regex("""ACTION:\s*INPUT_TEXT_AT_CURRENT_FOCUS\(\s*([^)]+)\s*\)""", RegexOption.IGNORE_CASE)

    fun parseActionsAndExecute(response: String, service: MyAccessibilityService) {
        // 伪代码：解析 AI 返回的逻辑链
        val actions = parseActionList(response)
        for (action in actions) {
            execute(action, service)
            Thread.sleep(200) // 快速连续操作
        }
    }

    fun parseActionList(aiResponse: String): List<String> {
        // 1. 使用正则匹配 ACTIONS: [...] 括号内部的内容
        // 匹配 [ 到 ] 之间的所有字符
        val regex = Regex("""ACTIONS:\s*\[(.*?)\]""", RegexOption.IGNORE_CASE)
        val matchResult = regex.find(aiResponse)

        val actionsString = matchResult?.groupValues?.get(1) ?: ""

        if (actionsString.isEmpty()) return emptyList()

        // 2. 根据逗号分割成单个指令，并去除前后的空格
        // 注意：如果是 INPUT("text, with comma") 这种情况，简单的 split(",") 会出问题
        // 这里我们假设指令内部参数不含逗号，或者使用更复杂的正则拆分
        return actionsString.split(Regex(""",(?=[A-Z])""")).map { it.trim() }
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