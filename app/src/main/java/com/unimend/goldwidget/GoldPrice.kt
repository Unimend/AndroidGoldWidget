package com.unimend.goldwidget

/**
 * 黄金行情数据模型
 */
data class GoldPrice(
    val international: Double,      // 国际金价：美元/盎司
    val internationalChange: Double,// 国际金价涨跌额
    val internationalPercent: Double,// 国际金价涨跌幅 %
    val domestic: Double,           // 国内金价 AU9999：人民币/克
    val domesticChange: Double,     // 国内金价涨跌额
    val domesticPercent: Double,    // 国内金价涨跌幅 %
    val updateTime: String,         // 数据更新时间
    val tick: Long = 0L,
    val receivedAt: Long = 0L,
    val refreshFailed: Boolean = false
) {
    val isInternationalRise: Boolean get() = internationalChange >= 0
    val isDomesticRise: Boolean get() = domesticChange >= 0

    /** 序列化为 JSON 字符串（用于持久化到 Preferences） */
    fun toJson(): String {
        return org.json.JSONObject().apply {
            put("international", international)
            put("internationalChange", internationalChange)
            put("internationalPercent", internationalPercent)
            put("domestic", domestic)
            put("domesticChange", domesticChange)
            put("domesticPercent", domesticPercent)
            put("updateTime", updateTime)
            put("tick", tick)
            put("receivedAt", receivedAt)
            put("refreshFailed", refreshFailed)
        }.toString()
    }

    companion object {
        /** Preferences 中存储金价 JSON 的 key */
        const val KEY_PRICE = "gold_price_json"

        /** 加载失败时的降级展示 */
        fun placeholder() = GoldPrice(
            international = 0.0,
            internationalChange = 0.0,
            internationalPercent = 0.0,
            domestic = 0.0,
            domesticChange = 0.0,
            domesticPercent = 0.0,
            updateTime = "--",
            tick = 0L
        )

        /** 从 JSON 字符串反序列化；失败返回 placeholder */
        fun fromJson(json: String?): GoldPrice {
            if (json.isNullOrBlank()) return placeholder()
            return try {
                val obj = org.json.JSONObject(json)
                GoldPrice(
                    international = obj.optDouble("international", 0.0),
                    internationalChange = obj.optDouble("internationalChange", 0.0),
                    internationalPercent = obj.optDouble("internationalPercent", 0.0),
                    domestic = obj.optDouble("domestic", 0.0),
                    domesticChange = obj.optDouble("domesticChange", 0.0),
                    domesticPercent = obj.optDouble("domesticPercent", 0.0),
                    updateTime = obj.optString("updateTime", "--"),
                    tick = obj.optLong("tick", 0L),
                    receivedAt = obj.optLong("receivedAt", 0L),
                    refreshFailed = obj.optBoolean("refreshFailed", false)
                )
            } catch (e: Exception) {
                placeholder()
            }
        }
    }
}
