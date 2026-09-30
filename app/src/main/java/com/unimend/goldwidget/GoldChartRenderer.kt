package com.unimend.goldwidget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 金价折线图绘制器（太空舱科幻风，仿支付宝分时图）。
 * 单条国际金价折线，左侧 Y 轴价格刻度，底部 X 轴时间刻度。
 * - 右侧预留"未来时间"空白
 * - 曲线末端标注当前价格
 * - 水平虚线标注历史最高价
 * - 当前价为历史新高时高亮（霓虹青）
 */
object GoldChartRenderer {

    private val LINE_COLOR = Color.parseColor("#E0A000")       // 折线：深金（浅底清晰）
    private val GRID_COLOR = Color.parseColor("#33D0B070")     // 网格线：淡金
    private val AXIS_TEXT = Color.parseColor("#B8860B")        // 刻度文字：深琥珀
    private val X_AXIS_TEXT = Color.parseColor("#8E8E93")      // X 轴时间：灰
    private val HIGH_LINE_NORMAL = Color.parseColor("#8E8E93") // 最高价线（常态）：灰
    private val NEW_HIGH = Color.parseColor("#FF3B30")         // 新高态：红

    private const val AXIS_WIDTH = 70f     // 左侧 Y 轴区（容纳 24f 数字）
    private const val X_AXIS_HEIGHT = 32f  // 底部 X 轴区（容纳 24f 时间）
    private const val FUTURE_RATIO = 0.16f // 右侧留白（未来时间）比例
    private const val NEW_HIGH_EPS = 0.01  // 浮点容差（判断新高）

    fun render(points: List<GoldHistoryStore.Point>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)

        if (points.size < 2) return bitmap

        val padTop = 24f          // 顶部留白（容纳最高价标签）
        val plotLeft = AXIS_WIDTH
        val plotRight = width - 6f
        val plotBottom = height - X_AXIS_HEIGHT
        val plotW = plotRight - plotLeft
        val plotH = plotBottom - padTop

        // 折线实际占用的宽度（右侧留 future 空白）
        val drawW = plotW * (1f - FUTURE_RATIO)
        val drawRight = plotLeft + drawW

        // 网格线（仅横向参考线，去掉竖向包围感）
        val gridPaint = Paint().apply {
            color = GRID_COLOR
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        for (i in 0..4) {
            val y = padTop + plotH * i / 4f
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint)
        }

        // 提取国际金价序列
        val values = points.map { it.international }
        val min = values.minOrNull() ?: return bitmap
        val max = values.maxOrNull() ?: return bitmap
        val range = (max - min).takeIf { it > 0.0001 } ?: (max * 0.0001f).toDouble().takeIf { it > 0 }

        val currentPrice = values.last()
        val highPrice = max
        val isNewHigh = currentPrice >= highPrice - NEW_HIGH_EPS

        // 强调色：新高用霓虹青，否则用暗琥珀
        val highlightColor = if (isNewHigh) NEW_HIGH else HIGH_LINE_NORMAL

        // Y 轴刻度（文字左对齐，尽量贴左边缘）
        val axisPaint = Paint().apply {
            color = AXIS_TEXT
            textSize = 24f
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
        for (i in 0..3) {
            val y = padTop + plotH * i / 3f
            val value = max - (range ?: 1.0) * i / 3f
            canvas.drawText(String.format("%.0f", value), 2f, y + 8f, axisPaint)
        }

        // 历史最高价虚线 + 标签
        val highY = padTop + plotH * (1f - ((highPrice - min) / (range ?: 1.0)).toFloat())
        val highLinePaint = Paint().apply {
            color = highlightColor
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
            isAntiAlias = true
        }
        canvas.drawLine(plotLeft, highY, plotRight, highY, highLinePaint)

        // 最高价数值标签（右侧对齐到 plotRight 内侧，避免向右超界被裁）
        val highLabel = if (isNewHigh) {
            String.format(Locale.getDefault(), "%.1f 新高", highPrice)
        } else {
            String.format(Locale.getDefault(), "%.1f", highPrice)
        }
        val highLabelPaint = Paint().apply {
            color = highlightColor
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT  // 右对齐，文字向左延伸，不会超右界
        }
        canvas.drawText(highLabel, plotRight - 4f, highY - 4f, highLabelPaint)

        // X 轴时间刻度（字体加大，往下挪贴底）
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val first = points.first().timestamp
        val last = points.last().timestamp
        val mid = points[points.size / 2].timestamp
        val timeY = height - 3f  // 基线贴近底部

        val timePaintStart = Paint().apply {
            color = X_AXIS_TEXT; textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.LEFT
        }
        canvas.drawText(fmt.format(Date(first)), plotLeft, timeY, timePaintStart)

        val timePaintMid = Paint().apply {
            color = X_AXIS_TEXT; textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.CENTER
        }
        canvas.drawText(fmt.format(Date(mid)), (plotLeft + drawRight) / 2f, timeY, timePaintMid)

        val timePaintEnd = Paint().apply {
            color = X_AXIS_TEXT; textSize = 24f; isAntiAlias = true; textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(fmt.format(Date(last)), drawRight, timeY, timePaintEnd)

        // 画折线 + 下方填充
        val lastX = plotLeft + drawW  // 曲线末端 x 坐标（供当前价标签使用）
        drawLineWithFill(canvas, values, plotLeft, padTop, drawW, plotH, range, plotBottom)

        // 曲线末端当前价标签（圆点 + 数值，新高时高亮）
        val lastY = padTop + plotH * (1f - ((currentPrice - min) / (range ?: 1.0)).toFloat())
        val dotPaint = Paint().apply {
            color = highlightColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(lastX, lastY, 5f, dotPaint)

        val currentLabelPaint = Paint().apply {
            color = highlightColor
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
            style = Paint.Style.FILL
        }
        val currentLabel = String.format(Locale.getDefault(), "%.1f", currentPrice)
        canvas.drawText(currentLabel, lastX + 8f, lastY + 7f, currentLabelPaint)

        return bitmap
    }

    private fun drawLineWithFill(
        canvas: Canvas,
        values: List<Double>,
        left: Float,
        top: Float,
        plotW: Float,
        plotH: Float,
        range: Double?,
        bottom: Float
    ) {
        val min = values.minOrNull() ?: return
        val max = values.maxOrNull() ?: return
        val r = range ?: return

        val linePaint = Paint().apply {
            color = LINE_COLOR
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val xs = FloatArray(values.size)
        val ys = FloatArray(values.size)
        values.forEachIndexed { i, v ->
            val normalized = 1f - ((v - min) / r).toFloat()
            xs[i] = left + plotW * i / (values.size - 1)
            ys[i] = top + plotH * normalized
        }

        val path = Path()
        values.forEachIndexed { i, _ ->
            if (i == 0) path.moveTo(xs[i], ys[i]) else path.lineTo(xs[i], ys[i])
        }
        canvas.drawPath(path, linePaint)
    }
}
