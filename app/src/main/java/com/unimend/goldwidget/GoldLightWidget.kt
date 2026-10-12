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
 * 浅色半透明主题组件（毛玻璃白效果）。
 * 与极客风组件共存，桌面上可同时添加两个。
 */
object GoldLightWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                LightWidgetContent()
            }
        }
    }
}

// 浅色半透明配色
private val LIGHT_CARD_BG = Color(0x80FFFFFF)   // 50% 白
private val LIGHT_SUB_BG = Color(0x66FFFFFF)    // 40% 白
private val LIGHT_CHART_BG = Color(0x4DFFFFFF)  // 30% 白
private val LIGHT_TITLE = Color(0xFFB8860B)     // 深琥珀
private val LIGHT_INTL = Color(0xFF007AFF)      // iOS蓝
private val LIGHT_DOM = Color(0xFF1D1D1F)       // 深黑
private val LIGHT_LABEL = Color(0xFF8E8E93)     // 灰

@Composable
private fun LightWidgetContent() {
    val context = androidx.glance.LocalContext.current
    val priceJson = currentState(stringPreferencesKey(GoldPrice.KEY_PRICE))
    val price = GoldPrice.fromJson(priceJson)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(LIGHT_CARD_BG))
            .cornerRadius(14.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(actionRunCallback<RefreshAction>())
    ) {
        // 标题
        Text(
            text = if (price.refreshFailed) "黄金价格 · 缓存行情" else "黄金价格 · ${price.updateTime}",
            style = TextStyle(
                color = ColorProvider(LIGHT_TITLE),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(GlanceModifier.height(12.dp))

        // 两个价格格
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            LightPriceCell("国际金价", price.international, LIGHT_INTL, LIGHT_INTL)
            Spacer(GlanceModifier.width(12.dp))
            LightPriceCell("国内金价", price.domestic, LIGHT_DOM, LIGHT_TITLE)
        }

        Spacer(GlanceModifier.height(14.dp))

        // 曲线图
        val file = GoldRefreshService.lightChartFilePath(context)
        val bitmap = if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null

        if (bitmap != null) {
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(ColorProvider(LIGHT_CHART_BG))
                    .cornerRadius(12.dp)
            ) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = "金价走势",
                    modifier = GlanceModifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        } else {
            Text(
                text = "走势图加载中…",
                style = TextStyle(color = ColorProvider(LIGHT_LABEL), fontSize = 10.sp)
            )
        }
    }
}

@Composable
private fun androidx.glance.layout.RowScope.LightPriceCell(
    label: String,
    price: Double,
    priceColor: Color,
    borderColor: Color
) {
    Column(
        modifier = GlanceModifier
            .defaultWeight()
            .background(ColorProvider(LIGHT_SUB_BG))
            .cornerRadius(12.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Text(
            text = label,
            style = TextStyle(color = ColorProvider(LIGHT_LABEL), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = formatPrice(price),
            style = TextStyle(color = ColorProvider(priceColor), fontSize = 27.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(6.dp))
        Spacer(
            GlanceModifier.fillMaxWidth().height(2.dp).background(ColorProvider(borderColor))
        )
    }
}

/**
 * 浅色版接收器
 */
class GoldLightWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoldLightWidget

    override fun onUpdate(
        context: Context,
        appWidgetManager: android.appwidget.AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        GoldRefreshService.ensureRunning(context)
    }
}
