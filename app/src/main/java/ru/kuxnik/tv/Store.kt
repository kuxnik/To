package ru.kuxnik.tv

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import org.json.JSONArray

/** Избранное и настройки — хранятся на устройстве. */
class Store(context: Context) {
    private val prefs = context.getSharedPreferences("tv", Context.MODE_PRIVATE)

    val favorites = mutableStateListOf<Channel>().apply { addAll(load("favorites")) }

    fun isFavorite(id: String?) = id != null && favorites.any { it.id == id }

    fun toggleFavorite(c: Channel) {
        val i = favorites.indexOfFirst { it.id == c.id }
        if (i >= 0) favorites.removeAt(i) else favorites.add(c)
        save("favorites", favorites)
    }

    private val bufferState = mutableIntStateOf(prefs.getInt("buffer", 0))
    var bufferSec: Int
        get() = bufferState.intValue
        set(v) {
            bufferState.intValue = v
            prefs.edit().putInt("buffer", v).apply()
        }

    private val prebufferState = mutableStateOf(prefs.getBoolean("prebuffer", false))
    var prebuffer: Boolean
        get() = prebufferState.value
        set(v) {
            prebufferState.value = v
            prefs.edit().putBoolean("prebuffer", v).apply()
        }

    private val audioOnlyState = mutableStateOf(prefs.getBoolean("audioOnly", false))
    var audioOnly: Boolean
        get() = audioOnlyState.value
        set(v) {
            audioOnlyState.value = v
            prefs.edit().putBoolean("audioOnly", v).apply()
        }

    var lastChannel: String?
        get() = prefs.getString("last", null)
        set(v) = prefs.edit().putString("last", v).apply()

    private fun load(key: String): List<Channel> = runCatching {
        val arr = JSONArray(prefs.getString(key, "[]"))
        (0 until arr.length()).map { Channel.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    private fun save(key: String, list: List<Channel>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
