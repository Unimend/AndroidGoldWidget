package com.unimend.goldwidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 金价历史数据缓存。
 * 用于保存最近 N 个价格点，供折线图使用。
 * 存储在 SharedPreferences（数据量小且需要高频读写，比 DataStore 更轻量）。
 */
class GoldHistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("gold_history", Context.MODE_PRIVATE)

    /**
     * 单个历史价格点
     */
    data class Point(
        val timestamp: Long,
        val international: Double,
        val domestic: Double
    )

    /**
     * 追加一个新点。若超过 maxPoints，自动裁剪最旧的。
     */
    fun append(point: Point, maxPoints: Int = MAX_POINTS) {
        val list = load().toMutableList()
        list.add(point)
        // 裁剪到最多 maxPoints 个
        val trimmed = if (list.size > maxPoints) list.subList(list.size - maxPoints, list.size) else list
        save(trimmed)
    }

    /** 读取全部历史点 */
    fun load(): List<Point> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Point(
                    timestamp = o.getLong("t"),
                    international = o.getDouble("i"),
                    domestic = o.getDouble("d")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 清空历史 */
    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private fun save(list: List<Point>) {
        val arr = JSONArray()
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("t", p.timestamp)
                put("i", p.international)
                put("d", p.domestic)
            })
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "gold_history_points"
        const val MAX_POINTS = 480
    }
}
