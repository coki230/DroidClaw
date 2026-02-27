package com.example.axondroid

import android.graphics.Bitmap
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object ApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)      // 连接超时：建立 TCP 连接的时间
        .readTimeout(300, TimeUnit.SECONDS)         // 读取超时：等待服务器返回数据的时间（大模型建议设长一点）
        .writeTimeout(30, TimeUnit.SECONDS)        // 写入超时：发送数据给服务器的时间
        .build()
    private const val VLLM_URL = "http://192.168.1.24:1234/v1/chat/completions" // 替换为你机器的 IP
    private val SYSTEM_PROMPT = """
    # ROLE
    你是一个 Android 自动化智能体。手机已经处于无障碍服务模式，你只能通过指令与手机交互。你具有操作手机的完全权限

    # INTERFACE FORMAT (STRICT)
    你必须**只**输出以下格式之一，严禁任何自然语言描述：
    - ACTION: CLICK(x, y)
    - ACTION: LONG_PRESS(x, y)
    - ACTION: SWIPE(x1, y1, x2, y2)
    - ACTION: DRAG(x1, y1, x2, y2)
    - ACTION: INPUT("target", "text")
    - ACTION: SNAPSHOT_REQUIRED

    # CONSTRAINTS
    - 严禁输出解释、代码块、Markdown 格式或道歉。
    - 如果需要打开应用但没看到图标，输出 ACTION: SNAPSHOT_REQUIRED。
    - 哪怕无法完成任务，也只能输出上述指令，不能说话。

    # EXAMPLES
    User: 打开微信
    Assistant: ACTION: CLICK(150, 300)
    
    User: 搜索天气
    Assistant: ACTION: INPUT("搜索栏", "北京天气")
""".trimIndent()

    fun askAi(uiInfo: String, useInput: String, callback: (String) -> Unit) {
        val json = JSONObject().apply {
            put("model", "Qwen2.5-VL-72B-Instruct-AWQ")

            val messages = JSONArray().apply {
                // 第一条：system 消息（纯文本 content）
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT)
                })

                // 第二条：user 消息（多模态 content，是一个 JSONArray）
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        // 文本部分
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", useInput)
                        })

                        // 图片部分
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$uiInfo")
                                put("detail", "low")
                            })
                        })
                    })
                })
            }
            put("messages", messages)
            put("temperature", 0.1)
            put("top_p", 0.1)
            put("max_tokens", 80)
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(VLLM_URL).post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback("Error: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    callback("Error: Unexpected code $response")
                    return
                }

                val resBody = response.body?.string()
                callback(resBody ?: "")
            }
        })
    }
}