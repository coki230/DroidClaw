package com.example.axondroid

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.axondroid.ui.theme.AxonDroidTheme
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1. 找到你的按钮（如果你用了 ViewBinding，写法略有不同）
        val btnStartAi = findViewById<Button>(R.id.btnStartAi)

        // 2. 设置点击监听器
        btnStartAi.setOnClickListener {

            val service = MyAccessibilityService.instance
            if (service == null) {
                // 权限没开，跳转到系统设置界面
                Toast.makeText(this, "请开启 AxonDroid 无障碍服务", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }

            if (service != null) {
                val useInput = findViewById<EditText>(R.id.useInput)
                val useInputText = useInput.text.toString()
//                // 第一步：感知 (Capture)
//                val uiContext = service.captureUIStructure()
//
//                // 提示用户正在思考（UI 反馈）
//                Toast.makeText(this, "AxonDroid 正在思考...", Toast.LENGTH_SHORT).show()
//
//                // 第二步：思考 (Reasoning)
//                ApiClient.askAi(uiContext, useInputText) { response ->
//
//                    // 第三步：解析回复中的坐标
//                    val regex = Regex("""CLICK\((\d+),\s*(\d+)\)""")
//                    val match = regex.find(response)
//
//                    if (match != null) {
//                        val x = match.groupValues[1].toFloat()
//                        val y = match.groupValues[2].toFloat()
//
//                        // 第四步：执行 (Execution)
//                        // 注意：ApiClient 是在子线程回调的，操作 UI/Service 必须回到主线程
//                        runOnUiThread {
//                            service.performClickAction(x, y)
//                            Toast.makeText(this, "执行点击: $x, $y", Toast.LENGTH_SHORT).show()
//                        }
//                    } else {
//                        runOnUiThread {
//                            Toast.makeText(this, "AI 未返回有效坐标", Toast.LENGTH_LONG).show()
//                        }
//                    }
//                }


                service.captureScreen { bitmap ->
                    bitmap?.let {
                        val base64 = bitmapToBase64(it)

                        println("================================")
                        println(base64)
                        println("================================")

                        // 发送带图片的高级请求
                        ApiClient.askAi(base64, useInputText) { finalRes ->
//                        executeClick(finalRes)
                            println(finalRes)
                        }

                    }
                }
            } else {
                // 如果服务没开启，引导用户去设置
                Toast.makeText(this, "请先在系统设置中开启 AxonDroid 无障碍权限", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AxonDroidTheme {
        Greeting("Android")
    }
}

/**
 * 将 Bitmap 压缩并转为 Base64 字符串
 * 目标：让 Base64 长度控制在 2万~4万字符左右（适合 8192 token 模型）
 */
fun bitmapToBase64(
    bitmap: Bitmap,
    maxShortSide: Int = 768,           // 短边建议 512~896，768 是性价比高的点
    jpegQuality: Int = 75              // 70~85 通常够用，低于70可能失真明显
): String {
    // 1. 等比例缩小到短边不超过 maxShortSide
    val scaledBitmap = if (bitmap.width > bitmap.height) {
        val newWidth = maxShortSide
        val newHeight = (bitmap.height * newWidth.toFloat() / bitmap.width).toInt()
        Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    } else {
        val newHeight = maxShortSide
        val newWidth = (bitmap.width * newHeight.toFloat() / bitmap.height).toInt()
        Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    // 2. 压缩为 JPEG
    val outputStream = java.io.ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outputStream)

    // 3. 转 Base64
    val byteArray = outputStream.toByteArray()
    val base64String = Base64.encodeToString(byteArray, Base64.NO_WRAP)

    // 可选：打印长度方便调试
//    println("Compressed Base64 length: ${base64String.length} chars")

    // 清理
    scaledBitmap.recycle()
    bitmap.recycle()

    return base64String
}

//fun printAxonBanner() {
//    val banner = """
//        --------------------------------------------------
//        AxonDroid Agent Initializing...
//        Architecture: Local Multi-Modal Reasoning
//        Status: Checking Accessibility Permissions...
//        --------------------------------------------------
//    """.trimIndent()
//    Log.i("AxonDroid", banner)
//
//    if (isAccessibilityServiceEnabled()) {
//        Log.i("AxonDroid", "✅ Link Established: Accessibility Service is ON")
//    } else {
//        Log.e("AxonDroid", "❌ Link Broken: Please enable Accessibility Service")
//    }
//}