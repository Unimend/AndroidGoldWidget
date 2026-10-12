package com.unimend.goldwidget

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.*
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color

object GoldChartWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val price = GoldPrice.fromJson(currentState(stringPreferencesKey(GoldPrice.KEY_PRICE)))
            val size = LocalSize.current
            val width = (size.width.value * 2f).toInt().coerceIn(400, 1600)
            val height = (width * size.height.value / size.width.value.coerceAtLeast(1f)).toInt().coerceIn(300, 1400)
            val bitmap = GoldDashboardRenderer.render(context, price, width, height)
            Box(GlanceModifier.fillMaxSize().background(ColorProvider(Color.Black))
                .cornerRadius(14.dp).clickable(actionRunCallback<RefreshAction>())) {
                Image(ImageProvider(bitmap), "国际与国内黄金价格及本地采样走势",
                    modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.Fit)
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
