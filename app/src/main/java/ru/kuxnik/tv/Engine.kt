package ru.kuxnik.tv

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.MediaSession

/**
 * Два плеера: [active] показывает канал, [standby] (если включена пребуферизация)
 * заранее подключает СЛЕДУЮЩИЙ канал списка без звука. По «Вперёд» они меняются
 * местами — переключение мгновенное. MediaSession даёт управление кнопками на руле.
 */
@OptIn(UnstableApi::class)
class Engine(private val context: Context, private val store: Store) {

    private val handler = Handler(Looper.getMainLooper())
    private val retries = HashMap<ExoPlayer, Int>()
    private val audioAttrs = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    /** Плеер, который сейчас на экране (Compose следит за сменой). */
    var active by mutableStateOf(build(store.bufferSec, main = true))
        private set
    private var standby: ExoPlayer = build(store.bufferSec, main = false)
    private var bufferSec = store.bufferSec

    private val session: MediaSession = MediaSession.Builder(context, wrap(active)).setId("tv").build()

    init {
        applyAudioOnly()
    }

    private fun listenerFor(p: ExoPlayer) = object : Player.Listener {
        override fun onPlayerError(error: PlaybackException) {
            val n = (retries[p] ?: 0) + 1
            retries[p] = n
            if (n > (if (p === active) 30 else 3)) return
            handler.postDelayed({
                if (p.playerError != null && (p === active && p.playWhenReady || p === standby)) {
                    p.seekToDefaultPosition()
                    p.prepare()
                    p.play()
                }
            }, minOf(2_000L * n, 15_000L))
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) retries[p] = 0
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (p === active && events.containsAny(
                    Player.EVENT_TIMELINE_CHANGED,
                    Player.EVENT_MEDIA_ITEM_TRANSITION,
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                )
            ) handler.post { syncStandby() }
        }
    }

    private fun build(sec: Int, main: Boolean): ExoPlayer {
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(20_000)
        val withHeaders = ResolvingDataSource.Factory(http) { spec ->
            spec.withAdditionalHeaders(Headers.forHost(spec.uri.host))
        }
        val sources = DefaultMediaSourceFactory(DefaultDataSource.Factory(context, withHeaders))
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(10))

        val loadControl = if (sec <= 0) {
            DefaultLoadControl()
        } else {
            val ms = sec * 1000
            val keep = maxOf(50_000, ms + 30_000)
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(keep, keep, ms, ms)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
        }
        val p = ExoPlayer.Builder(context)
            .setMediaSourceFactory(sources)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttrs, main)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        p.repeatMode = Player.REPEAT_MODE_ALL
        if (!main) p.volume = 0f
        p.addListener(listenerFor(p))
        return p
    }

    private fun wrap(p: ExoPlayer): Player = object : ForwardingPlayer(p) {
        override fun seekToNext() {
            if (!swapForward()) super.seekToNext()
        }

        override fun seekToNextMediaItem() {
            if (!swapForward()) super.seekToNextMediaItem()
        }
    }

    // ---------- Управление ----------

    fun play(c: Channel, list: List<Channel>) {
        val l = if (list.any { it.id == c.id }) list else listOf(c)
        val index = l.indexOfFirst { it.id == c.id }
        store.lastChannel = c.id
        // Выбрали канал, который уже готов в запасном плеере, — переключаемся мгновенно.
        if (store.prebuffer && standby.playbackState == Player.STATE_READY &&
            standby.currentMediaItemIndex == index && ids(standby) == l.map { it.id }
        ) {
            swapToStandby()
            return
        }
        active.setMediaItems(l.map { it.toMediaItem() }, index, C.TIME_UNSET)
        active.prepare()
        active.play()
    }

    fun toggle() {
        val p = active
        if (p.playWhenReady) {
            p.pause()
        } else if (p.mediaItemCount > 0) {
            p.seekToDefaultPosition() // прямой эфир, а не старый буфер
            p.prepare()
            p.play()
        }
    }

    fun next() {
        if (!swapForward()) active.run { seekToNextMediaItem(); play() }
        current()?.let { store.lastChannel = it.id }
    }

    fun prev() {
        active.run { seekToPreviousMediaItem(); play() }
        current()?.let { store.lastChannel = it.id }
    }

    fun current(): Channel? = Channel.fromItem(active.currentMediaItem)

    fun pause() = active.pause()

    // ---------- Пребуферизация ----------

    private fun ids(p: Player) = (0 until p.mediaItemCount).map { p.getMediaItemAt(it).mediaId }

    private fun swapForward(): Boolean {
        val n = active.mediaItemCount
        if (!store.prebuffer || n < 2) return false
        val next = (active.currentMediaItemIndex + 1) % n
        if (standby.currentMediaItemIndex != next || standby.playbackState != Player.STATE_READY) return false
        if (ids(standby) != ids(active)) return false
        swapToStandby()
        return true
    }

    private fun swapToStandby() {
        val old = active
        val fresh = standby
        old.setAudioAttributes(audioAttrs, false)
        old.volume = 0f
        fresh.setAudioAttributes(audioAttrs, true)
        fresh.volume = 1f
        fresh.playWhenReady = true
        standby = old
        active = fresh
        session.player = wrap(fresh)
        current()?.let { store.lastChannel = it.id }
        syncStandby()
    }

    private fun syncStandby() {
        val n = active.mediaItemCount
        if (!store.prebuffer || n < 2 || !active.playWhenReady) {
            if (standby.mediaItemCount > 0) {
                standby.stop()
                standby.clearMediaItems()
            }
            return
        }
        val next = (active.currentMediaItemIndex + 1) % n
        if (ids(standby) != ids(active)) {
            standby.setMediaItems((0 until n).map { active.getMediaItemAt(it) }, next, C.TIME_UNSET)
            standby.prepare()
        } else if (standby.currentMediaItemIndex != next) {
            standby.seekTo(next, C.TIME_UNSET)
        }
        if (standby.playbackState == Player.STATE_IDLE) standby.prepare()
        standby.volume = 0f
        standby.playWhenReady = true
    }

    // ---------- Настройки ----------

    fun setPrebuffer(on: Boolean) {
        store.prebuffer = on
        syncStandby()
    }

    fun setAudioOnly(on: Boolean) {
        store.audioOnly = on
        applyAudioOnly()
    }

    private fun applyAudioOnly() {
        for (p in listOf(active, standby)) {
            p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, store.audioOnly)
                .build()
        }
    }

    fun setBuffer(sec: Int) {
        store.bufferSec = sec
        if (sec == bufferSec) return
        bufferSec = sec
        val old = active
        val items = (0 until old.mediaItemCount).map { old.getMediaItemAt(it) }
        val index = old.currentMediaItemIndex
        val wasPlaying = old.playWhenReady

        handler.removeCallbacksAndMessages(null)
        val fresh = build(sec, main = true)
        val freshStandby = build(sec, main = false)
        session.player = wrap(fresh)
        old.release()
        standby.release()
        retries.clear()
        standby = freshStandby
        active = fresh
        applyAudioOnly()
        if (items.isNotEmpty()) {
            fresh.setMediaItems(items, index.coerceIn(0, items.lastIndex), C.TIME_UNSET)
            fresh.prepare()
            fresh.playWhenReady = wasPlaying
        }
    }

    fun release() {
        handler.removeCallbacksAndMessages(null)
        session.release()
        active.release()
        standby.release()
    }

    @Suppress("unused")
    private fun itemsOf(p: Player): List<MediaItem> = (0 until p.mediaItemCount).map { p.getMediaItemAt(it) }
}
