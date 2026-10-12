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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 前台服务：亮屏时每次刷新完成后等待 5 秒，再拉取行情。
 */
class GoldRefreshService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var refreshJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(android.app.NotificationManager::class.java).createNotificationChannel(
            android.app.NotificationChannel(CHANNEL_ID, "黄金行情监控", android.app.NotificationManager.IMPORTANCE_LOW))
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
                val manager = android.appwidget.AppWidgetManager.getInstance(this@GoldRefreshService)
                val hasWidgets = listOf(GoldChartWidgetReceiver::class.java, GoldLightWidgetReceiver::class.java)
                    .any { manager.getAppWidgetIds(android.content.ComponentName(this@GoldRefreshService, it)).isNotEmpty() }
                if (!hasWidgets) { stopSelf(); break }
                if (!getSystemService(android.os.PowerManager::class.java).isInteractive) {
                    delay(REFRESH_INTERVAL_MS)
                    continue
                }
                try {
                    val success = refreshNow(this@GoldRefreshService)
                    if (!success) {
                        Log.w(TAG, "本次刷新失败，稍后重试")
                    }
                } catch (e: CancellationException) {
                    throw e
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
        const val REFRESH_INTERVAL_MS = 5_000L
        private val refreshMutex = Mutex()
        private val repository = GoldPriceRepository()
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
        suspend fun refreshNow(context: Context): Boolean = refreshMutex.withLock {
            val price = repository.fetch()
            if (price == null) {
                val manager = GlanceAppWidgetManager(context)
                listOf(GoldChartWidget::class.java, GoldLightWidget::class.java).forEach { widgetClass ->
                    manager.getGlanceIds(widgetClass).forEach { id ->
                        updateAppWidgetState(context, id) { prefs ->
                            prefs[priceKey] = GoldPrice.fromJson(prefs[priceKey]).copy(refreshFailed = true).toJson()
                        }
                    }
                }
                GoldChartWidget.updateAll(context)
                GoldLightWidget.updateAll(context)
                return@withLock false
            }
            withContext(Dispatchers.IO) {

            // 追加历史点（折线图数据源）
            val history = GoldHistoryStore(context)
            history.append(
                GoldHistoryStore.Point(
                    timestamp = System.currentTimeMillis(),
                    international = price.international,
                    domestic = price.domestic
                )
            )

            // 生成折线图 bitmap（深色极客版 + 浅色版各一套）
            val chartBitmap = GoldChartRenderer.render(history.load(), CHART_WIDTH, CHART_HEIGHT)
            saveChartBitmap(context, chartBitmap)
            val lightBitmap = GoldChartRenderer.renderLight(history.load(), CHART_WIDTH, CHART_HEIGHT)
            saveLightChartBitmap(context, lightBitmap)
            chartBitmap.recycle()
            lightBitmap.recycle()
            }

            // 更新两个组件的价格 state（tick +1 用于闪烁相位）
            val manager = GlanceAppWidgetManager(context)
            val allWidgets = listOf(GoldChartWidget::class.java, GoldLightWidget::class.java)
            allWidgets.forEach { widgetClass ->
                manager.getGlanceIds(widgetClass).forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        val prevTick = GoldPrice.fromJson(prefs[priceKey]).tick
                        prefs[priceKey] = price.copy(tick = prevTick + 1).toJson()
                    }
                }
            }

            // 触发两个组件的 UI 重建
            GoldChartWidget.updateAll(context)
            GoldLightWidget.updateAll(context)
            Log.d(TAG, "行情刷新完成：${price.updateTime}")
            true
        }

        // 折线图尺寸（px，宽高比约 4:1，匹配扁形显示区）
        private const val CHART_WIDTH = 1000
        private const val CHART_HEIGHT = 360

        /** 折线图 bitmap 的缓存文件路径 */
        fun chartFilePath(context: Context): java.io.File =
            java.io.File(context.filesDir, "gold_chart.png")

        /** 浅色版折线图 bitmap 的缓存文件路径 */
        fun lightChartFilePath(context: Context): java.io.File =
            java.io.File(context.filesDir, "gold_chart_light.png")

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

        private fun saveLightChartBitmap(context: Context, bitmap: android.graphics.Bitmap) {
            try {
                val file = lightChartFilePath(context)
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
            } catch (e: Exception) {
                Log.e(TAG, "保存浅色折线图失败", e)
            }
        }
    }
}
