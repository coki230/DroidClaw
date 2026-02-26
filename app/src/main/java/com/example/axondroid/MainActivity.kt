package com.example.axondroid

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
                // 第一步：感知 (Capture)
                val uiContext = service.captureUIStructure()

                // 提示用户正在思考（UI 反馈）
                Toast.makeText(this, "AxonDroid 正在思考...", Toast.LENGTH_SHORT).show()

                // 第二步：思考 (Reasoning)
                ApiClient.askAi(uiContext) { response ->

                    // 第三步：解析回复中的坐标
                    val regex = Regex("""CLICK\((\d+),\s*(\d+)\)""")
                    val match = regex.find(response)

                    if (match != null) {
                        val x = match.groupValues[1].toFloat()
                        val y = match.groupValues[2].toFloat()

                        // 第四步：执行 (Execution)
                        // 注意：ApiClient 是在子线程回调的，操作 UI/Service 必须回到主线程
                        runOnUiThread {
                            service.performClickAction(x, y)
                            Toast.makeText(this, "执行点击: $x, $y", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "AI 未返回有效坐标", Toast.LENGTH_LONG).show()
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