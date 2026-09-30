package com.unimend.goldwidget

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 前台服务：每 30 秒拉取一次实时金价并更新小组件。
 */
class GoldRefreshService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification("黄金行情监控中"))
        startRefreshing()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 确保循环在（防止被外部重启后不刷新）
        if (refreshJob?.isActive != true) startRefreshing()
        return START_STICKY
    }

    private fun startRefreshing() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            while (true) {
                try {
                    val success = refreshNow(this@GoldRefreshService)
                    if (!success) {
                        Log.w(TAG, "本次刷新失败，稍后重试")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "刷新异常", e)
                }
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun onDestroy() {
        refreshJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("黄金小组件")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setOngoing(true)
            .build()

    companion object {
        private const val TAG = "GoldRefreshService"
        const val CHANNEL_ID = "gold_price_channel"
        const val NOTIFICATION_ID = 1001
        const val REFRESH_INTERVAL_MS = 1_000L  // 1 秒刷新一次（最接近实时跳动）
        private val priceKey = stringPreferencesKey(GoldPrice.KEY_PRICE)

        /** 判断服务是否正在运行 */
        fun isRunning(context: Context): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            return manager.getRunningServices(Integer.MAX_VALUE)
                .any { it.service.className == GoldRefreshService::class.java.name }
        }

        /** 确保服务运行（幂等，供组件接收器/Activity 调用） */
        fun ensureRunning(context: Context) {
            if (isRunning(context)) return
            val intent = Intent(context, GoldRefreshService::class.java)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "启动服务失败（可能是后台限制，需用户前台打开App）", e)
            }
        }

        /**
         * 刷新一次金价并更新所有小组件。
         * @return 是否成功抓到并写入了新数据
         */
        suspend fun refreshNow(context: Context): Boolean {
            val price = withContext(Dispatchers.IO) {
                GoldPriceRepository().fetch()
            } ?: return false

            // 追加历史点（折线图数据源）
            val history = GoldHistoryStore(context)
            history.append(
                GoldHistoryStore.Point(
                    timestamp = System.currentTimeMillis(),
                    international = price.international,
                    domestic = price.domestic
                )
            )

            // 生成折线图 bitmap 并缓存到文件（只画国际金价单线，更清晰）
            val chartBitmap = GoldChartRenderer.render(history.load(), CHART_WIDTH, CHART_HEIGHT)
            saveChartBitmap(context, chartBitmap)

            // 更新组合组件的价格 state
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(GoldChartWidget::class.java).forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[priceKey] = price.toJson()
                }
            }

            // 触发组件 UI 重建
            GoldChartWidget.updateAll(context)
            return true
        }

        // 折线图尺寸（px，宽高比约 4:1，匹配扁形显示区）
        private const val CHART_WIDTH = 1600
        private const val CHART_HEIGHT = 400

        /** 折线图 bitmap 的缓存文件路径 */
        fun chartFilePath(context: Context): java.io.File =
            java.io.File(context.filesDir, "gold_chart.png")

        private fun saveChartBitmap(context: Context, bitmap: android.graphics.Bitmap) {
            try {
                val file = chartFilePath(context)
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存折线图失败", e)
            }
        }
    }
}
