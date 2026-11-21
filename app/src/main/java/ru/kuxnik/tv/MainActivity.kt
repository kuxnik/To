package ru.kuxnik.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {

    private lateinit var store: Store
    private lateinit var engine: Engine
    private var resumeOnStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        engine = Engine(this, store)
        // Сразу включаем последний канал из избранного.
        store.lastChannel?.let { last ->
            val c = store.favorites.firstOrNull { it.id == last }
            if (c != null) engine.play(c, store.favorites)
        }
        setContent { TvApp(store, engine, window) }
    }

    override fun onStart() {
        super.onStart()
        if (resumeOnStart) engine.toggle()
        resumeOnStart = false
    }

    override fun onStop() {
        // Видео в фоне не нужно: ставим на паузу, вернёмся — продолжим с прямого эфира.
        resumeOnStart = engine.active.playWhenReady
        engine.pause()
        super.onStop()
    }

    override fun onDestroy() {
        engine.release()
        super.onDestroy()
    }
}
