package com.unimend.goldwidget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 黄金行情数据源：新浪财经公开行情接口
 * 无需注册、免费、稳定。
 *
 * hf_XAU    国际现货黄金（伦敦金），美元/盎司
 * gds_AU9999 沪金99（国内 AU9999），人民币/克
 */
class GoldPriceRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 抓取实时金价。网络异常或解析失败时返回 null。
     */
    suspend fun fetch(): GoldPrice? = withContext(Dispatchers.IO) {
        try {
            val url = "https://hq.sinajs.cn/list=hf_XAU,gds_AU9999"
            val request = Request.Builder()
                .url(url)
                .header("Referer", "https://finance.sina.com.cn")
                .header("User-Agent", "Mozilla/5.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    android.util.Log.e("GoldRepo", "HTTP ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string() ?: return@withContext null
                val parsed = parse(body)
                if (parsed == null) android.util.Log.e("GoldRepo", "解析失败: ${body.take(80)}")
                parsed
            }
        } catch (e: Exception) {
            android.util.Log.e("GoldRepo", "fetch 异常: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }
    }

    /**
     * 解析新浪返回的 JS 字符串。
     * 示例：var hq_str_hf_XAU="4160.34,4181.87,...";
     */
    private fun parse(body: String): GoldPrice? {
        try {
            val international = extract(body, "hq_str_hf_XAU")?.let { fields ->
                val parts = fields.split(",")
                if (parts.size < 8) return@let null
                val current = parts[0].toDouble()
                val prevClose = parts[1].toDouble().takeIf { it > 0 }
                    ?: parts[2].toDouble().takeIf { it > 0 } ?: current
                Triple(current, current - prevClose, (current - prevClose) / prevClose * 100)
            } ?: return null

            val domestic = extract(body, "hq_str_gds_AU9999")?.let { fields ->
                val parts = fields.split(",")
                if (parts.size < 8) return@let null
                val current = parts[0].toDouble()
                val prevClose = parts[1].toDouble().takeIf { it > 0 }
                    ?: parts[2].toDouble().takeIf { it > 0 } ?: current
                Triple(current, current - prevClose, (current - prevClose) / prevClose * 100)
            } ?: return null

            return GoldPrice(
                international = international.first,
                internationalChange = international.second,
                internationalPercent = international.third,
                domestic = domestic.first,
                domesticChange = domestic.second,
                domesticPercent = domestic.third,
                updateTime = ""
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun extract(body: String, varName: String): String? {
        val marker = "${varName}=\""
        val start = body.indexOf(marker)
        if (start < 0) return null
        val valueStart = start + marker.length
        val end = body.indexOf("\"", valueStart)
        if (end < 0) return null
        return body.substring(valueStart, end)
    }
}
