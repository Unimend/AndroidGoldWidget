package com.unimend.goldwidget

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
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
 * 黄金实时价格组合组件（复刻 UIMaker 苹果风设计稿）。
 *
 * 结构：
 *   ┌─ 大底（深绿黑 #111612，圆角22）─────────────┐
 *   │  标题「实时黄金价格」（金色 15 粗体）         │
 *   │  ┌国际金价格(纯黑)┐  ┌国内金价格(纯黑)┐       │
 *   │  │ 标签(灰10粗)    │  │ 标签(灰10粗)    │       │
 *   │  │ 价格(绿27粗)    │  │ 价格(白27粗)    │       │
 *   │  └──────────────┘  └──────────────┘       │
 *   │  ┌─ 价格曲线图（深色底，圆角12）─────────┐   │
 *   │  └───────────────────────────────────────┘   │
 *   └──────────────────────────────────────────────┘
 */
object GoldChartWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                CombinedWidgetContent()
            }
        }
    }
}

@Composable
private fun CombinedWidgetContent() {
    val context = androidx.glance.LocalContext.current
    val priceJson = currentState(stringPreferencesKey(GoldPrice.KEY_PRICE))
    val price = GoldPrice.fromJson(priceJson)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(GoldColors.CARD_BG))
            .cornerRadius(22.dp)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .clickable(actionRunCallback<RefreshAction>())
    ) {
        // 标题
        Text(
            text = "实时黄金价格",
            style = TextStyle(
                color = ColorProvider(GoldColors.TITLE_GOLD),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(GlanceModifier.height(12.dp))

        // 两个价格格，横向并排、等分宽度（defaultWeight 平分可用空间，避免超宽裁剪）
        Row(
            modifier = GlanceModifier.fillMaxWidth()
        ) {
            PriceCell(
                label = "国际金价",
                price = price.international,
                priceColor = GoldColors.PRICE_INTL,
                borderColor = GoldColors.INTL_BORDER
            )
            Spacer(GlanceModifier.width(12.dp))
            PriceCell(
                label = "国内金价",
                price = price.domestic,
                priceColor = GoldColors.PRICE_DOM,
                borderColor = GoldColors.DOM_BORDER
            )
        }

        Spacer(GlanceModifier.height(14.dp))

        // 曲线图（defaultWeight 撑满剩余高度，避免底部空底）
        val file = GoldRefreshService.chartFilePath(context)
        val bitmap = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null

        if (bitmap != null) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(ColorProvider(GoldColors.CHART_BG))
                    .cornerRadius(12.dp)
            ) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = "金价走势",
                    modifier = GlanceModifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            Text(
                text = "走势图加载中…",
                style = TextStyle(
                    color = ColorProvider(GoldColors.UNIT_GRAY),
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun androidx.glance.layout.RowScope.PriceCell(
    label: String,
    price: Double,
    priceColor: androidx.compose.ui.graphics.Color,
    borderColor: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .background(ColorProvider(GoldColors.SUB_BG))
            .cornerRadius(12.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(
                color = ColorProvider(GoldColors.PRICE_LABEL),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = formatPrice(price),
            style = TextStyle(
                color = ColorProvider(priceColor),
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )
        )
        // 底部彩色细线（模拟设计稿的彩色边框）
        Spacer(GlanceModifier.height(6.dp))
        Spacer(
            GlanceModifier
                .fillMaxWidth()
                .height(2.dp)
                .background(ColorProvider(borderColor))
        )
    }
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
