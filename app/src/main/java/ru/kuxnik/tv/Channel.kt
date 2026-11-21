package ru.kuxnik.tv

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class Channel(
    val url: String,
    val name: String,
    val logo: String = "",
    val groups: String = "",      // «News;General» из плейлиста
    val quality: String = "",     // «720p»
    val referrer: String = "",
    val userAgent: String = "",
    val geoBlocked: Boolean = false,
    val notAlways: Boolean = false,
) {
    val id: String get() = url

    val mono: String
        get() {
            val words = name.split(' ', '-', '.', '_').filter { it.isNotBlank() && it[0].isLetterOrDigit() }
            val s = if (words.size == 1) words[0].take(3) else words.take(2).joinToString("") { it.take(1) }
            return s.uppercase().ifEmpty { "TV" }
        }

    val color: Long get() = PALETTE[(url.hashCode() and 0x7fffffff) % PALETTE.size]

    val categories: List<String>
        get() = groups.split(';').map { it.trim() }.filter { it.isNotEmpty() && it != "Undefined" }

    val categoryText: String
        get() = categories.take(2).joinToString(", ") { CATEGORY_RU[it] ?: it }.ifEmpty { "Без категории" }

    fun toJson(): JSONObject = JSONObject()
        .put("url", url).put("name", name).put("logo", logo).put("groups", groups)
        .put("quality", quality).put("referrer", referrer).put("ua", userAgent)
        .put("geo", geoBlocked).put("na", notAlways)

    fun toMediaItem(): MediaItem {
        Headers.register(this)
        val meta = MediaMetadata.Builder()
            .setTitle(name)
            .setArtist(categoryText)
            .setExtras(Bundle().apply { putString(EXTRA, toJson().toString()) })
        if (logo.isNotBlank()) meta.setArtworkUri(Uri.parse(logo))
        val b = MediaItem.Builder().setMediaId(url).setUri(url).setMediaMetadata(meta.build())
        val lower = url.lowercase()
        when {
            ".m3u8" in lower || "m3u8" in lower -> b.setMimeType(MimeTypes.APPLICATION_M3U8)
            ".mpd" in lower -> b.setMimeType(MimeTypes.APPLICATION_MPD)
        }
        return b.build()
    }

    companion object {
        const val EXTRA = "channel"

        private val PALETTE = longArrayOf(
            0xFF7FC8FF, 0xFFFFB547, 0xFFFF8A80, 0xFF9BE38A, 0xFFD7B6FF, 0xFFF5E27A, 0xFF7FE0D0,
        )

        val CATEGORY_RU = mapOf(
            "News" to "Новости", "Movies" to "Кино", "Series" to "Сериалы", "Kids" to "Детские",
            "Animation" to "Мультфильмы", "Music" to "Музыка", "Sports" to "Спорт",
            "Documentary" to "Документальные", "Entertainment" to "Развлечения", "General" to "Общие",
            "Education" to "Познавательные", "Science" to "Наука", "Culture" to "Культура",
            "Lifestyle" to "Стиль жизни", "Cooking" to "Кулинария", "Travel" to "Путешествия",
            "Business" to "Бизнес", "Religious" to "Религия", "Comedy" to "Юмор", "Classic" to "Классика",
            "Family" to "Семейные", "Weather" to "Погода", "Auto" to "Авто", "Outdoor" to "Природа",
            "Legislative" to "Парламент", "Shop" to "Магазин", "Relax" to "Релакс",
        )

        fun fromJson(o: JSONObject) = Channel(
            url = o.optString("url"),
            name = o.optString("name"),
            logo = o.optString("logo"),
            groups = o.optString("groups"),
            quality = o.optString("quality"),
            referrer = o.optString("referrer"),
            userAgent = o.optString("ua"),
            geoBlocked = o.optBoolean("geo"),
            notAlways = o.optBoolean("na"),
        )

        fun fromItem(item: MediaItem?): Channel? {
            val s = item?.mediaMetadata?.extras?.getString(EXTRA) ?: return null
            return runCatching { fromJson(JSONObject(s)) }.getOrNull()
        }
    }
}

/** Заголовки Referer/User-Agent, которых требуют некоторые трансляции (из #EXTVLCOPT плейлиста). */
object Headers {
    const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private val byHost = ConcurrentHashMap<String, Pair<String, String>>()

    fun register(c: Channel) {
        if (c.referrer.isBlank() && c.userAgent.isBlank()) return
        val host = runCatching { Uri.parse(c.url).host }.getOrNull() ?: return
        byHost[host] = c.referrer to c.userAgent
    }

    fun forHost(host: String?): Map<String, String> {
        val h = host?.let { byHost[it] }
        val m = HashMap<String, String>()
        m["User-Agent"] = h?.second?.takeIf { it.isNotBlank() } ?: DEFAULT_UA
        h?.first?.takeIf { it.isNotBlank() }?.let { m["Referer"] = it }
        return m
    }
}
