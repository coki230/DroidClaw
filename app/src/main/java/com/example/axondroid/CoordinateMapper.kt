package com.example.axondroid

object CoordinateMapper {
    var scaleX: Float = 1f
    var scaleY: Float = 1f

    // 在 bitmapToBase64 之前或期间计算比例
    fun calculateScale(originalW: Int, originalH: Int, scaledW: Int, scaledH: Int) {
        scaleX = originalW.toFloat() / scaledW
        scaleY = originalH.toFloat() / scaledH
    }

    // 将 AI 返回的坐标还原为原始屏幕坐标
    fun mapToOriginal(aiX: Float, aiY: Float): Pair<Float, Float> {
        return Pair(aiX * scaleX, aiY * scaleY)
    }
}