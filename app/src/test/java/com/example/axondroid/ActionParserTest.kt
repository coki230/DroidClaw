package com.example.axondroid
import org.junit.Test
import org.junit.Assert.*

class ActionParserTest {
    @Test
    fun testParse() {
        val input = "ACTIONS: [CLICK(180, 97), INPUT_TEXT_AT_CURRENT_FOCUS(\"gold, price\"), PRESS_ENTER]; EXPLAIN: ..."
        val result = ActionParser.parseActionList(input)

        println("解析结果: $result")
        assertEquals(3, result.size)
        assertEquals("CLICK(180, 97)", result[0])
    }
}