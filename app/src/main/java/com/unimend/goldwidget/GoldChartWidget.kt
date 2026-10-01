package com.unimend.goldwidget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.ContentScale
import androidx.glance.currentState
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * 极客风终端数据仪表组件（Terminal Data Readout）。
 *
 * 结构：
 *   ▸ GOLD · XAU/USD            ● SYNC
 *   4152.71 USD/OZ    ▲ +1.20 (+0.03%)
 *   ┌─ 荧光绿折线图（含时间轴）────────┐
 *   └────────────────────────────────┘
 *   AU9999 907.61 | 昨收 4155.3 | HIGH 4154.2 | LOW 4147.3
 */
object GoldChartWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                TerminalWidgetContent()
            }
        }
    }
}

@Composable
private fun TerminalWidgetContent() {
    val context = androidx.glance.LocalContext.current
    val priceJson = currentState(stringPreferencesKey(GoldPrice.KEY_PRICE))
    val price = GoldPrice.fromJson(priceJson)

    // 闪烁相位：tick 奇偶决定 SYNC 点形态
    val syncDot = if (price.tick % 2 == 0L) "●" else "○"

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(GoldColors.CARD_BG))
            .cornerRadius(10.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(actionRunCallback<RefreshAction>())
    ) {
        // 顶部：品种标签 + 同步状态（闪烁）
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "▸ GOLD · XAU/USD",
                style = TextStyle(
                    color = ColorProvider(GoldColors.TITLE_GOLD),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                text = "$syncDot SYNC",
                style = TextStyle(
                    color = ColorProvider(GoldColors.DIM),
                    fontSize = 9.sp
                )
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        // 国际金价：大字 + 涨跌
        PriceLine(
            label = "国际",
            price = price.international,
            change = price.internationalChange,
            percent = price.internationalPercent,
            unit = "USD/OZ"
        )

        Spacer(GlanceModifier.height(6.dp))

        // 国内金价：大字 + 涨跌
        PriceLine(
            label = "国内",
            price = price.domestic,
            change = price.domesticChange,
            percent = price.domesticPercent,
            unit = "元/克"
        )

        Spacer(GlanceModifier.height(8.dp))

        // 折线图（荧光绿，含时间轴）
        val file = GoldRefreshService.chartFilePath(context)
        val bitmap = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null

        if (bitmap != null) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = "金价走势",
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight(),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = "…loading",
                style = TextStyle(
                    color = ColorProvider(GoldColors.DIM),
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun PriceLine(
    label: String,
    price: Double,
    change: Double,
    percent: Double,
    unit: String
) {
    val rise = change >= 0
    val accent = if (rise) GoldColors.ACCENT_RED else GoldColors.ACCENT_GREEN
    val arrow = if (rise) "▲" else "▼"

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        // 标签
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(GoldColors.PRICE_LABEL),
                fontSize = 10.sp
            )
        )
        Spacer(GlanceModifier.width(8.dp))
        // 大字价格
        Text(
            text = formatPrice(price),
            style = TextStyle(
                color = ColorProvider(GoldColors.PRICE_INTL),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.width(3.dp))
        // 单位
        Text(
            text = unit,
            style = TextStyle(
                color = ColorProvider(GoldColors.DIM),
                fontSize = 10.sp
            )
        )
        Spacer(GlanceModifier.defaultWeight())
        // 涨跌
        Text(
            text = "$arrow ${formatSigned(change)} (${formatPercent(percent)})",
            style = TextStyle(
                color = ColorProvider(accent),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

private fun formatSigned(v: Double): String {
    val sign = if (v >= 0) "+" else ""
    return sign + String.format("%.2f", v)
}

private fun formatPercent(v: Double): String {
    val sign = if (v >= 0) "+" else ""
    return sign + String.format("%.2f%%", v)
}

/**
 * 组合组件接收器
 */
class GoldChartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoldChartWidget

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        GoldRefreshService.ensureRunning(context)
    }
}
