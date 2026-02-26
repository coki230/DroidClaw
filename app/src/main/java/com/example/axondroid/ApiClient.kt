package com.example.axondroid

import android.graphics.Bitmap
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object ApiClient {
    private val client = OkHttpClient()
    private const val VLLM_URL = "http://192.168.1.24:1234/v1/chat/completions" // 替换为你机器的 IP

    private const val DEFAULT_PROMPT = "你是一个Android 自动化智能体。\n" +
            "你的任务是根据 UI 操作手机。\n" +
            "规则：\n" +
            "禁止输出任何解释性文字、代码块或道歉。\n" +
            "必须只输出动作指令，格式为：ACTION: CLICK(x, y) 或 ACTION: INPUT(text) 或 ACTION: WAIT。\n" +
            "如果找不到目标，输出 ACTION: SNAPSHOT_REQUIRED。"

    fun askAi(uiInfo: String, useInput: String, callback: (String) -> Unit) {
        val json = JSONObject().apply {
            put("model", "Qwen2.5-VL-72B-Instruct-AWQ")
            val messages = JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("system", DEFAULT_PROMPT)
                val contentArray = JSONArray()
                val textPart = JSONObject().apply {
                put("type", "text")
                put("text", useInput)
            }
                contentArray.put(textPart)
                val imagePart = JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", "data:image/jpeg;base64,$uiInfo")
                        put("detail", "low")          // 推荐加上，能显著减少 token 消耗
                        // put("detail", "auto")      // 或者用 auto，看模型表现
                    })
                }
                contentArray.put(imagePart)
                put("content", contentArray)
            })
            put("messages", messages)
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