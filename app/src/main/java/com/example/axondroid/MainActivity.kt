package com.example.axondroid

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import androidx.lifecycle.lifecycleScope
import com.example.axondroid.ui.theme.AxonDroidTheme
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.delay

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

            startAiThinkingLoop(useInputText)
        }
    }

    // 1. 定义一个简单的对话记忆缓存
    private val conversationHistory = mutableListOf<String>()

    private fun startAiThinkingLoop(userTask: String) {
        val service = MyAccessibilityService.instance ?: return

        // 获取当前屏幕文本
//        val uiStructure = service.captureUIStructure()

        // 构造带记忆的 Prompt
        val memoryPrompt = conversationHistory.takeLast(5).joinToString("\n")
        val finalPrompt = "任务: $userTask\n历史记录: $memoryPrompt"

        // 发送给 Python 后端
        startAutoTask(finalPrompt)

    }
    private fun startAutoTask(userTask: String) {
        if (userTask.isBlank()) {
            Toast.makeText(this, "请输入任务需求", Toast.LENGTH_SHORT).show()
            return
        }

        // 启动生命周期感知的协程，当 Activity 销毁时会自动停止
        lifecycleScope.launch {
            Log.d("Agent", "开始执行任务: $userTask")
            runTaskStep(userTask, 1)
        }
    }

    /**
     * 递归执行任务步骤
     */
    private suspend fun runTaskStep(goal: String, step: Int) {
        // 获取服务实例
        val service = MyAccessibilityService.instance ?: run {
            Log.e("Agent", "无障碍服务未启动")
            return
        }

        // 1. 安全阀：防止无限循环
        if (step > 15) {
            Log.w("Agent", "任务超过15步，自动终止")
            Toast.makeText(this, "步数超限，任务停止", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("Agent", "正在执行第 $step 步...")

        // 2. 感知：获取当前最新的屏幕截图
        val bitmap = service.captureScreenSuspend()
        if (bitmap == null) {
            Log.e("Agent", "第 $step 步截屏失败，2秒后重试")
            delay(2000)
            runTaskStep(goal, step) // 失败重试，步数不增加
            return
        }

        // 转换 Base64 (确保 bitmapToBase64 能处理非 Hardware Bitmap)
        val base64 = bitmapToBase64(bitmap)

        // 3. 思考：发送给 AI 并等待结果
        // 假设 ApiClient.askAiSuspend 已经按之前的建议改写为挂起函数
        val aiResponse = ApiClient.askAiSuspend(base64, goal)
        Log.d("Agent", "AI 回复: $aiResponse")

        // 4. 执行：解析指令并在手机上模拟操作
        val isActionExecuted = ActionParser.parseAndExecute(aiResponse, service)

        // 5. 循环决策
        when {
            aiResponse.contains("TASK_COMPLETE") -> {
                Log.i("Agent", "任务圆满完成")
                Toast.makeText(this, "✅ 任务完成！", Toast.LENGTH_LONG).show()
            }

            isActionExecuted -> {
                // 动作成功执行，延迟 3 秒让页面跳转或动画完成
                Log.d("Agent", "动作执行成功，等待页面刷新...")
                delay(3000)
                runTaskStep(goal, step + 1)
            }

            else -> {
                // 如果 AI 返回了 SNAPSHOT_REQUIRED 或者没识别出有效指令
                Log.w("Agent", "未能执行有效动作，尝试重新观察...")
                delay(2000)
                runTaskStep(goal, step) // 重新观察，不增加步数
            }
        }
    }

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