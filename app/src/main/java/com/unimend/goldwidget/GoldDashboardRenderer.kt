package com.unimend.goldwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.util.Locale

/** One canvas sized to the actual host; all typography scales uniformly. */
object GoldDashboardRenderer {
    fun render(context: Context, price: GoldPrice, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = width / 900f
        canvas.scale(scale, scale)
        val logicalHeight = height / scale
        val font = Typeface.createFromAsset(context.assets, "doto.ttf")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 38f
        fun text(value: String, x: Float, centerY: Float, size: Float, color: Int,
                 centered: Boolean = false) {
            paint.typeface = font
            paint.textSize = size
            paint.color = color
            val bounds = Rect()
            paint.getTextBounds(value, 0, value.length, bounds)
            val origin = if (centered) x - (bounds.left + bounds.right) / 2f else x - bounds.left
            canvas.drawText(value, origin, centerY - (bounds.top + bounds.bottom) / 2f, paint)
        }
        val white = Color.WHITE
        val grey = Color.rgb(145, 145, 145)
        val red = Color.rgb(255, 45, 45)
        text("GOLD", margin, 34f, 30f, white)
        val status = when {
            price.receivedAt == 0L -> "WAITING"
            price.refreshFailed || System.currentTimeMillis() - price.receivedAt > 120_000L -> "STALE"
            else -> price.updateTime
        }
        text(status, 740f, 34f, 22f, if (status == "STALE") red else grey)
        val centers = floatArrayOf(235f, 665f)
        val prices = doubleArrayOf(price.international, price.domestic)
        val changes = doubleArrayOf(price.internationalPercent, price.domesticPercent)
        val labels = arrayOf("XAU / USD-OZ", "AU9999 / CNY-G")
        for (index in 0..1) {
            text(labels[index], centers[index], 92f, 24f, grey, true)
            text(formatPrice(prices[index]), centers[index], 148f, 68f, white, true)
            val change = if (prices[index] <= 0) "--" else String.format(Locale.US, "%+.2f%%", changes[index])
            text(change, centers[index], 205f, 28f,
                if (changes[index] >= 0) red else Color.rgb(95, 175, 125), true)
        }
        val chartTop = 245f
        val chartHeight = (logicalHeight - chartTop - 52f).toInt()
        if (chartHeight >= 80) {
            val points = GoldHistoryStore(context).load()
            if (points.size >= 2) {
                val chart = GoldChartRenderer.render(points, (900f - margin * 2).toInt(), chartHeight)
                canvas.drawBitmap(chart, margin, chartTop, null)
                chart.recycle()
            } else {
                text("WAITING FOR HISTORY", 450f, chartTop + chartHeight / 2f, 25f, grey, true)
            }
        }
        text("SINA · LOCAL HISTORY", 450f, logicalHeight - 24f, 19f, grey, true)
        return bitmap
    }
}

