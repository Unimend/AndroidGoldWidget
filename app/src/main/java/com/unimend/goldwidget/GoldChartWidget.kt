package com.unimend.goldwidget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.semantics.semantics
import androidx.glance.semantics.contentDescription
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
                .cornerRadius(14.dp), contentAlignment = Alignment.TopCenter) {
                Image(ImageProvider(bitmap), "国际与国内黄金价格及本地采样走势",
                    modifier = GlanceModifier.fillMaxSize().clickable(actionRunCallback<RefreshAction>()), contentScale = ContentScale.Fit)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(GlanceModifier.height((148f * size.width.value / 900f - 24f).coerceAtLeast(0f).dp))
                    Row(modifier = GlanceModifier.width(48.dp).height(48.dp)
                    .semantics {
                        contentDescription = if (domestic) "当前国内曲线，切换国际" else "当前国际曲线，切换国内"
                    }
                    .clickable(actionRunCallback<ToggleChartAction>()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically) {
                        Spacer(GlanceModifier.fillMaxSize())
                    }
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
