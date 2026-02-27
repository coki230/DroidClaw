package com.example.axondroid

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.axondroid.ui.theme.AxonDroidTheme
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val btnStartAi = findViewById<Button>(R.id.btnStartAi)

        btnStartAi.setOnClickListener {
            val service = MyAccessibilityService.instance

            // 权限检查逻辑
            if (service == null) {
                Toast.makeText(this, "请开启 AxonDroid 无障碍服务", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                return@setOnClickListener // 必须 return，否则会执行下面的逻辑导致崩溃
            }

            val useInput = findViewById<EditText>(R.id.useInput)
            val useInputText = useInput.text.toString()

            service.captureScreen { bitmap ->
                if (bitmap == null) {
                    runOnUiThread { Toast.makeText(this, "截屏失败", Toast.LENGTH_SHORT).show() }
                    return@captureScreen
                }

                // 执行压缩与转换
                val base64 = bitmapToBase64(bitmap)

                runOnUiThread {
                    Toast.makeText(this, "AxonDroid 正在思考...", Toast.LENGTH_SHORT).show()
                }

                // 发送给 AI
                ApiClient.askAi(base64, useInputText) { finalRes ->
                    Log.d("AxonDroid", "AI Response: $finalRes")

                    runOnUiThread {
                        // 执行解析与操作
                        ActionParser.parseAndExecute(finalRes, service)
                    }
                }
            }
        }
    } // <--- 这里之前漏掉了 onCreate 的闭合大括号

    /**
     * 将 Bitmap 压缩并转为 Base64 字符串
     */
    fun bitmapToBase64(
        bitmap: Bitmap,
        maxShortSide: Int = 768,
        jpegQuality: Int = 75
    ): String {
        // 1. 等比例缩小
        val scaledBitmap = if (bitmap.width > bitmap.height) {
            val newHeight = maxShortSide
            val newWidth = (bitmap.width * newHeight.toFloat() / bitmap.height).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            val newWidth = maxShortSide
            val newHeight = (bitmap.height * newWidth.toFloat() / bitmap.width).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }

        // 2. 压缩为 JPEG
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)

        // 3. 转 Base64
        val byteArray = outputStream.toByteArray()
        val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        // 4. 清理资源 (注意：如果原始 bitmap 还在别处使用，不要 recycle 它)
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        // bitmap.recycle() // 建议根据生命周期管理，如果 captureScreen 传给你的 bitmap 还要用，就别在这 recycle

        return base64String
    }
}

// 这里的 Composable 函数应该放在 Activity 类外面
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}