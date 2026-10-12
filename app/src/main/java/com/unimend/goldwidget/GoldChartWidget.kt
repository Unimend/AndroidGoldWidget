package com.unimend.goldwidget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.compose.ui.unit.sp
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color

object GoldChartWidget : GlanceAppWidget() {
    val domesticKey = booleanPreferencesKey("chart_domestic")
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val price = GoldPrice.fromJson(currentState(stringPreferencesKey(GoldPrice.KEY_PRICE)))
            val size = LocalSize.current
            val domestic = currentState(domesticKey) ?: false
            val width = (size.width.value * context.resources.displayMetrics.density).toInt().coerceIn(400, 2200)
            val height = (width * size.height.value / size.width.value.coerceAtLeast(1f)).toInt().coerceIn(300, 2200)
            val bitmap = GoldDashboardRenderer.render(context, price, width, height, domestic)
            Box(GlanceModifier.fillMaxSize().background(ColorProvider(Color.Black))
                .cornerRadius(14.dp), contentAlignment = Alignment.BottomCenter) {
                Image(ImageProvider(bitmap), "国际与国内黄金价格及本地采样走势",
                    modifier = GlanceModifier.fillMaxSize().clickable(actionRunCallback<RefreshAction>()), contentScale = ContentScale.Fit)
                Row(modifier = GlanceModifier.fillMaxWidth().height(48.dp)
                    .clickable(actionRunCallback<ToggleChartAction>()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(if (domestic) "国内 AU9999  ⇄  切换国际" else "国际 XAU  ⇄  切换国内",
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = 12.sp,
                        textAlign = androidx.glance.text.TextAlign.Center))
                }
            }
        }
    }
}

class GoldChartWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GoldChartWidget
    override fun onUpdate(context: Context, manager: android.appwidget.AppWidgetManager, ids: IntArray) {
        super.onUpdate(context, manager, ids)
        GoldRefreshService.ensureRunning(context)
    }
}
