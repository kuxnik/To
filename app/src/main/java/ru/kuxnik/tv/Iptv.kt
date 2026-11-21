package ru.kuxnik.tv

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Каталог общедоступных трансляций iptv-org (https://github.com/iptv-org/iptv).
 * Берём готовые плейлисты M3U: по стране или общий.
 */
object Iptv {
    private val ATTR = Regex("""([\w-]+)="([^"]*)"""")
    private val QUALITY = Regex("""\((\d{3,4}[pi])\)""")
    private val TAGS = Regex("""\[[^\]]*]""")

    private fun playlistUrl(country: String?) =
        if (country == null) "iptv-org.github.io/iptv/index.m3u"
        else "iptv-org.github.io/iptv/countries/${country.lowercase()}.m3u"

    /** Загрузить плейлист; при ошибке сети — последний сохранённый. */
    suspend fun load(context: Context, country: String?): List<Channel> = withContext(Dispatchers.IO) {
        val cache = File(context.cacheDir, "iptv_${country ?: "all"}.m3u")
        val text = try {
            download(playlistUrl(country)).also { cache.writeText(it) }
        } catch (e: Exception) {
            if (cache.exists()) cache.readText() else throw e
        }
        parse(text)
    }

    private fun download(path: String): String {
        var last: Exception? = null
        // На магнитолах бывают сбиты часы/сертификаты — после https пробуем http.
        for (scheme in listOf("https", "http")) {
            try {
                val c = URL("$scheme://$path").openConnection() as HttpURLConnection
                c.connectTimeout = 10_000
                c.readTimeout = 30_000
                c.setRequestProperty("User-Agent", "TvMagnitola/${BuildConfig.VERSION_NAME}")
                try {
                    if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
                    return c.inputStream.bufferedReader().use { it.readText() }
                } finally {
                    c.disconnect()
                }
            } catch (e: Exception) {
                last = e
            }
        }
        throw last ?: IOException("Каталог недоступен")
    }

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        val seen = HashSet<String>()
        var attrs: Map<String, String> = emptyMap()
        var title = ""
        var referrer = ""
        var ua = ""
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.isEmpty() -> {}
                line.startsWith("#EXTINF") -> {
                    attrs = ATTR.findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
                    val lastQuote = line.lastIndexOf('"')
                    val comma = line.indexOf(',', if (lastQuote >= 0) lastQuote else 0)
                    title = if (comma >= 0) line.substring(comma + 1).trim() else ""
                    referrer = ""
                    ua = ""
                }
                line.startsWith("#EXTVLCOPT:http-referrer=") -> referrer = line.substringAfter('=')
                line.startsWith("#EXTVLCOPT:http-user-agent=") -> ua = line.substringAfter('=')
                line.startsWith("#") -> {}
                else -> {
                    if (title.isNotEmpty() && seen.add(line)) {
                        val quality = QUALITY.find(title)?.groupValues?.get(1).orEmpty()
                        val name = title.replace(QUALITY, "").replace(TAGS, "").trim()
                        out += Channel(
                            url = line,
                            name = name.ifEmpty { title },
                            logo = attrs["tvg-logo"].orEmpty(),
                            groups = attrs["group-title"].orEmpty(),
                            quality = quality,
                            referrer = referrer.ifEmpty { attrs["http-referrer"].orEmpty() },
                            userAgent = ua.ifEmpty { attrs["http-user-agent"].orEmpty() },
                            geoBlocked = "[Geo-blocked]" in title,
                            notAlways = "[Not 24/7]" in title,
                        )
                    }
                    title = ""
                }
            }
        }
        return out.sortedBy { it.name.lowercase() }
    }
}
