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

    private const val DEFAULT_PROMPT = "\"你是一个 Android 自动化 Agent。我会默认发送屏幕的 UI 结构文本给你。\n" +
            "\n" +
            "如果通过文本能明确操作，请直接输出指令（如 CLICK）。\n" +
            "\n" +
            "如果页面全是被遮挡的图片、复杂的图形验证码，或者 UI 树信息不足以让你判断，请输出 SNAPSHOT_REQUIRED。\n" +
            "\n" +
            "当我收到该指令后，会发送当前页面的 Base64 图片给你进行视觉分析。\""

    fun askAi(uiInfo: String, callback: (String) -> Unit) {
        val json = JSONObject().apply {
            put("model", "Qwen2.5-32B-AWQ") // 或 qwen2.5-vl
            val messages = JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("system", DEFAULT_PROMPT)
                put("content", "当前手机 UI 如下：\n$uiInfo\n请给出下一步操作的坐标，格式如：CLICK(500,1200)")
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

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream) // 压缩一下，否则 128GB 内存也经不起大图折腾
        val byteArray = outputStream.toByteArray()
        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
    }
}