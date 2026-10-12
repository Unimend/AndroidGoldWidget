package com.unimend.goldwidget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 极客风终端数据仪表配色（GitHub 暗色系） */
object GoldColors {
    val BG_DARK = Color(0xFF1C1C1E)       // （保留，兼容旧引用）
    val LABEL_GRAY = Color(0xFF8E8E93)
    val UNIT_GRAY = Color(0xFF636366)
    val PRICE_WHITE = Color(0xFFF2F2F7)
    val RISE_RED = Color(0xFFFF453A)
    val FALL_GREEN = Color(0xFF30D158)

    // 极客风终端配色
    val CARD_BG = Color(0xFF000000)       // 系列统一纯黑底
    val SUB_BG = Color(0xFF161B22)        // 子区域：略浅
    val CHART_BG = Color(0x00000000)      // 折线图底：透明（融入卡片）
    val GRID = Color(0xFF1C2128)          // 网格线：暗灰
    val TITLE_GOLD = Color(0xFFF2F2F7)
    val PRICE_INTL = Color(0xFFE6EDF3)    // 主价格：高亮白
    val PRICE_DOM = Color(0xFFE6EDF3)     // 国内价：高亮白
    val PRICE_LABEL = Color(0xFF8B949E)   // 次要文字：灰
    val INTL_BORDER = Color(0xFF3FB950)   // 涨：绿
    val DOM_BORDER = Color(0xFFF85149)    // 跌：红
    val ACCENT_GREEN = Color(0xFF3FB950)  // 荧光绿
    val ACCENT_RED = Color(0xFFF85149)    // 红
    val DIM = Color(0xFF6E7681)           // 最暗灰
}

/** 格式化价格 */
fun formatPrice(v: Double): String {
    if (v <= 0) return "--"
    return String.format("%.2f", v)
}

/** 格式化涨跌（带符号和百分比） */
fun formatChange(change: Double, percent: Double): String {
    if (change == 0.0 && percent == 0.0) return "--"
    val sign = if (change >= 0) "+" else ""
    return String.format("%s%.2f (%.2f%%)", sign, change, percent)
}

/**
 * 点击刷新动作（两个组件共用）
 */
class RefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // 回调生命周期内完成刷新，周期任务与手动请求共享互斥锁。
        GoldRefreshService.refreshNow(context)
    }
}

/** 只切换本组件的曲线，不请求网络，不改变其他组件。 */
class ToggleChartAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            prefs[GoldChartWidget.domesticKey] = !(prefs[GoldChartWidget.domesticKey] ?: false)
        }
        GoldChartWidget.update(context, glanceId)
    }
}
