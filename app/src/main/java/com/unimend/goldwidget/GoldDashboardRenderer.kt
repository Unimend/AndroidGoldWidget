package com.unimend.goldwidget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import java.util.Locale

/** One canvas sized to the actual host; all typography scales uniformly. */
object GoldDashboardRenderer {
    fun render(context: Context, price: GoldPrice, width: Int, height: Int, domestic: Boolean = false): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.density = Bitmap.DENSITY_NONE
        val canvas = Canvas(bitmap)
        val scale = width / 900f
        canvas.scale(scale, scale)
        val logicalHeight = height / scale
        val font = Typeface.createFromAsset(context.assets, "doto.ttf")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val margin = 38f
        fun text(value: String, x: Float, centerY: Float, size: Float, color: Int,
                 centered: Boolean = false) {
            paint.typeface = if (size >= 60f || value == "GOLD") font else Typeface.create("sans-serif-medium", Typeface.NORMAL)
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
        val chartHeight = (logicalHeight - chartTop - 175f).toInt()
        if (chartHeight >= 80) {
            val points = GoldHistoryStore(context).load()
            if (points.size >= 2) {
                val chartWidth = 900f - margin * 2
                val chart = GoldChartRenderer.render(points, (chartWidth * scale).toInt(),
                    (chartHeight * scale).toInt(), domestic, scale)
                canvas.drawBitmap(chart, null, RectF(margin, chartTop, margin + chartWidth,
                    chartTop + chartHeight), Paint(Paint.FILTER_BITMAP_FLAG))
                chart.recycle()
            } else {
                text("WAITING FOR HISTORY", 450f, chartTop + chartHeight / 2f, 25f, grey, true)
            }
        }
        text(if (domestic) "AU9999 · CNY/G · LOCAL HISTORY" else "XAU · USD/OZ · LOCAL HISTORY",
            450f, logicalHeight - 145f, 19f, grey, true)
        return bitmap
    }
}

