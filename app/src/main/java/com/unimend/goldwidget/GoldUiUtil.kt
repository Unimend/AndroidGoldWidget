package com.unimend.goldwidget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 太空舱科幻配色（金色+琥珀） */
object GoldColors {
    val BG_DARK = Color(0xFF1C1C1E)       // （保留，兼容旧引用）
    val LABEL_GRAY = Color(0xFF8E8E93)
    val UNIT_GRAY = Color(0xFF636366)
    val PRICE_WHITE = Color(0xFFF2F2F7)
    val RISE_RED = Color(0xFFFF453A)
    val FALL_GREEN = Color(0xFF30D158)

    // 浅色半透明主题（毛玻璃白效果，透明度加大）
    val CARD_BG = Color(0x80FFFFFF)       // 大底：50% 白，明显透出壁纸
    val SUB_BG = Color(0x66FFFFFF)        // 价格子格：40% 白
    val CHART_BG = Color(0x4DFFFFFF)      // 曲线底图：30% 白
    val GRID = Color(0x33E0C07A)          // 网格线：淡金
    val TITLE_GOLD = Color(0xFFB8860B)    // 标题金：深琥珀（浅底上用深色）
    val PRICE_INTL = Color(0xFF007AFF)    // 国际价：iOS蓝（浅底清晰）
    val PRICE_DOM = Color(0xFF1D1D1F)     // 国内价：深黑
    val PRICE_LABEL = Color(0xFF8E8E93)   // 价格标签：灰
    val INTL_BORDER = Color(0xFF007AFF)   // 国际格边框：蓝
    val DOM_BORDER = Color(0xFFB8860B)    // 国内格边框：金
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
        // 用独立协程后台刷新，立即返回，不阻塞点击反馈
        CoroutineScope(Dispatchers.IO).launch {
            GoldRefreshService.refreshNow(context)
        }
    }
}
