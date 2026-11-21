package ru.kuxnik.tv

import android.view.ViewGroup
import android.view.Window
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// ---------- Цвета (как в макете) ----------
private val Bg = Color(0xFF0E1116)
private val Panel = Color(0xFF151920)
private val Raised = Color(0xFF1E232C)
private val Sel = Color(0xFF2A303B)
private val TextC = Color(0xFFF2F4F7)
private val Sub = Color(0xFFDCE1E8)
private val Muted = Color(0xFF9AA4B2)
private val Amber = Color(0xFFFFB547)
private val AmberBg = Color(0xFF3A2A10)
private val Red = Color(0xFFFF8A80)
private val RedBg = Color(0xFF3A1414)
private val Black = Color(0xFF05070A)

// ---------- Масштаб: макет подгоняется под любой экран ----------
private val LocalScale = staticCompositionLocalOf { 1f }

private val Int.u: Dp
    @Composable @ReadOnlyComposable get() = (this * LocalScale.current).dp

private val Int.t: TextUnit
    @Composable @ReadOnlyComposable get() = (this * LocalScale.current / LocalDensity.current.fontScale).sp

// ---------- Иконки ----------
private fun icon(d: String, fill: Boolean = true, w: Float = 2.2f): ImageVector =
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            if (fill) addPath(addPathNodes(d), fill = SolidColor(Color.White))
            else addPath(
                addPathNodes(d), fill = null, stroke = SolidColor(Color.White), strokeLineWidth = w,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
            )
        }.build()

private const val STAR_D = "M12,3.5l2.6,5.3 5.9,0.9 -4.3,4.1 1,5.8L12,16.8l-5.2,2.8 1,-5.8 -4.3,-4.1 5.9,-0.9z"
private val IcPlay = icon("M8,5v14l11,-7z")
private val IcPause = icon("M6,5h4v14h-4zM14,5h4v14h-4z")
private val IcPrev = icon("M6,5h2.5v14H6zM20,5v14L9.5,12z")
private val IcNext = icon("M15.5,5H18v14h-2.5zM4,5v14l10.5,-7z")
private val IcStar = icon(STAR_D)
private val IcStarO = icon(STAR_D, fill = false, w = 1.8f)
private val IcSearch = icon("M18,11a7,7 0 1,1 -14,0a7,7 0 1,1 14,0M20,20l-4,-4", fill = false)
private val IcBack = icon("M15,5l-7,7 7,7", fill = false, w = 2.4f)
private val IcFull = icon("M4,9V4h5M20,9V4h-5M4,15v5h5M20,15v5h-5", fill = false)
private val IcExit = icon("M9,4v5H4M15,4v5h5M9,20v-5H4M15,20v-5h5", fill = false)
private val IcGear = icon(
    "M4,7h10M18,7h2M4,17h4M12,17h8M18,7a2,2 0 1,1 -4,0a2,2 0 1,1 4,0M12,17a2,2 0 1,1 -4,0a2,2 0 1,1 4,0",
    fill = false,
)
private val IcTv = icon("M5,5h14a2,2 0 0,1 2,2v8a2,2 0 0,1 -2,2h-14a2,2 0 0,1 -2,-2v-8a2,2 0 0,1 2,-2zM8,21h8M12,17v4", fill = false, w = 1.6f)
private val IcBars = icon("M5,10v4M9,7v10M13,4v16M17,8v8M21,11v2", fill = false, w = 2.4f)

@Composable
private fun Ic(v: ImageVector, size: Int, color: Color) {
    Image(rememberVectorPainter(v), null, Modifier.size(size.u), colorFilter = ColorFilter.tint(color))
}

@Composable
private fun Txt(
    text: String, size: Int, color: Color = TextC, weight: FontWeight = FontWeight.SemiBold,
    modifier: Modifier = Modifier, maxLines: Int = 1, mono: Boolean = false, spacing: Float = 0f,
) {
    BasicText(
        text, modifier,
        style = TextStyle(
            color = color, fontSize = size.t, fontWeight = weight,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            letterSpacing = spacing.sp,
        ),
        maxLines = maxLines, overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun Btn(w: Int, h: Int = w, radius: Int, bg: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.width(w.u).height(h.u).clip(RoundedCornerShape(radius.u)).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(44.u).clip(RoundedCornerShape(22.u)).background(if (selected) Amber else Raised)
            .clickable(onClick = onClick).padding(horizontal = 18.u),
        contentAlignment = Alignment.Center,
    ) { Txt(label, 16, if (selected) Bg else Sub, if (selected) FontWeight.ExtraBold else FontWeight.Bold) }
}

private fun channelsWord(n: Int): String {
    val m10 = n % 10
    val m100 = n % 100
    val w = when {
        m10 == 1 && m100 != 11 -> "канал"
        m10 in 2..4 && m100 !in 12..14 -> "канала"
        else -> "каналов"
    }
    return "$n $w"
}

// ---------- Состояние ----------
class PlayerUi {
    var playing by mutableStateOf(false)
    var buffering by mutableStateOf(false)
    var error by mutableStateOf(false)
    var channel by mutableStateOf<Channel?>(null)
    var video by mutableStateOf("")
    var bufferMs by mutableLongStateOf(0L)

    fun sync(p: Player) {
        playing = p.playWhenReady
        buffering = p.playbackState == Player.STATE_BUFFERING
        error = p.playerError != null
        channel = Channel.fromItem(p.currentMediaItem)
        val v = p.videoSize
        video = if (v.width > 0) "${v.width}×${v.height}" else ""
    }
}

class CatalogState {
    var query by mutableStateOf("")
    var russia by mutableStateOf(true)
    var category by mutableStateOf<String?>(null)
    var all by mutableStateOf<List<Channel>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var reload by mutableIntStateOf(0)

    fun filtered(): List<Channel> {
        val q = query.trim().lowercase()
        return all.filter { c ->
            (q.isEmpty() || q in c.name.lowercase()) &&
                (category == null || category in c.categories)
        }
    }
}

private val CATEGORIES = listOf(
    "News", "Movies", "Series", "Kids", "Music", "Sports", "Documentary", "Entertainment", "General",
)
private val BUFFER_STEPS = listOf(0, 5, 10, 20, 30, 45, 60)

// ---------- Приложение ----------
@Composable
fun TvApp(store: Store, engine: Engine, window: Window) {
    val ui = remember { PlayerUi() }
    val catalog = remember { CatalogState() }
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(if (store.favorites.isEmpty()) 1 else 0) }
    var full by rememberSaveable { mutableStateOf(false) }
    val player = engine.active

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = ui.sync(player)
        }
        player.addListener(l)
        ui.sync(player)
        onDispose { player.removeListener(l) }
    }
    LaunchedEffect(player) {
        while (true) {
            ui.bufferMs = player.totalBufferedDuration
            delay(500)
        }
    }
    LaunchedEffect(catalog.russia, catalog.reload) {
        catalog.loading = true
        try {
            catalog.all = Iptv.load(context, if (catalog.russia) "RU" else null)
            catalog.error = null
        } catch (e: Exception) {
            catalog.error = "Каталог недоступен. Проверьте интернет."
        }
        catalog.loading = false
    }

    // Полный экран — прячем системные панели.
    val view = LocalView.current
    DisposableEffect(full) {
        val c = WindowCompat.getInsetsController(window, view)
        if (full) {
            c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            c.hide(WindowInsetsCompat.Type.systemBars())
        } else c.show(WindowInsetsCompat.Type.systemBars())
        onDispose { }
    }
    BackHandler(enabled = full || tab != 0) { if (full) full = false else tab = 0 }

    val play = { c: Channel, list: List<Channel> ->
        engine.play(c, list)
        tab = 0
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Bg)) {
        val land = maxWidth > maxHeight
        val phoneLand = land && maxHeight < 480.dp
        val scale = when {
            !land -> maxWidth.value / 400f
            else -> minOf(maxWidth.value / 1024f, maxHeight.value / 600f)
        }
        CompositionLocalProvider(LocalScale provides scale.coerceIn(0.55f, 2.2f)) {
            when {
                // Телефон повернули горизонтально на экране просмотра — видео во весь экран.
                full || (phoneLand && tab == 0) -> FullScreen(store, ui, engine, player, onExit = { full = false })
                tab == 1 -> CatalogScreen(store, ui, catalog, land, play, onBack = { tab = 0 })
                tab == 2 -> SettingsScreen(store, engine, land, onBack = { tab = 0 })
                land -> WatchLand(store, ui, engine, player, onTab = { tab = it }, onFull = { full = true })
                else -> WatchPortrait(store, ui, engine, player, onTab = { tab = it }, onFull = { full = true })
            }
        }
    }
}

// ---------- Видео ----------
@OptIn(UnstableApi::class)
@Composable
private fun Video(player: Player, ui: PlayerUi, store: Store, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.background(Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    setKeepContentOnPlayerReset(true)
                    keepScreenOn = true
                }
            },
            update = { it.player = player },
            onRelease = { it.player = null },
            modifier = Modifier.fillMaxSize(),
        )
        // Подписи поверх видео
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val msg = when {
                ui.channel == null -> "Выберите канал"
                ui.playing && ui.error -> "Нет связи с трансляцией · переподключаюсь…"
                store.audioOnly && ui.playing -> "Режим «только звук»"
                ui.playing && ui.buffering && store.bufferSec > 0 ->
                    "Набираю запас ${ui.bufferMs / 1000} / ${store.bufferSec} с"
                ui.playing && ui.buffering -> "Подключение…"
                else -> null
            }
            if (msg != null) {
                if (ui.channel == null || store.audioOnly) Ic(IcTv, 56, Color(0xFF3A4250))
                Txt(msg, 16, Color(0xFF8E98A6), FontWeight.Bold)
            }
        }
        if (!ui.playing && ui.channel != null) {
            Box(Modifier.fillMaxSize().background(Color(0x99050708)), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(96.u).clip(RoundedCornerShape(48.u)).background(Amber),
                    contentAlignment = Alignment.Center,
                ) { Ic(IcPlay, 44, Bg) }
            }
        }
        if (ui.bufferMs > 0 && ui.playing) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(12.u).clip(RoundedCornerShape(8.u))
                    .background(Color(0xBF0E1116)).padding(horizontal = 10.u, vertical = 4.u),
            ) { Txt("Буфер ${ui.bufferMs / 1000} с", 13, Sub, FontWeight.Bold) }
        }
        // Прозрачный слой для нажатий (PlayerView сам их не отдаёт).
        Box(Modifier.fillMaxSize().clickable(onClick = onClick))
    }
}

@Composable
private fun Logo(c: Channel?, w: Int, h: Int, radius: Int, textSize: Int) {
    val mod = Modifier.width(w.u).height(h.u).clip(RoundedCornerShape(radius.u))
    when {
        c == null -> Box(mod.background(Raised), contentAlignment = Alignment.Center) { Ic(IcTv, h / 2, Muted) }
        c.logo.isNotBlank() -> {
            var loaded by remember(c.logo) { mutableStateOf(false) }
            Box(mod.background(if (loaded) Sub else Color(c.color)), contentAlignment = Alignment.Center) {
                if (!loaded) Txt(c.mono, textSize, Bg, FontWeight.ExtraBold)
                AsyncImage(
                    model = c.logo, contentDescription = null, contentScale = ContentScale.Fit,
                    onSuccess = { loaded = true },
                    modifier = Modifier.fillMaxSize().padding(4.u),
                )
            }
        }
        else -> Box(mod.background(Color(c.color)), contentAlignment = Alignment.Center) {
            Txt(c.mono, textSize, Bg, FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun LiveInfo(ui: PlayerUi, big: Boolean) {
    val c = ui.channel
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.u)) {
        Txt(c?.name ?: "Канал не выбран", if (big) 22 else 24, TextC, FontWeight.ExtraBold, Modifier.weight(1f, fill = false))
        if (c != null) {
            Box(
                Modifier.height(26.u).clip(RoundedCornerShape(13.u)).background(RedBg).padding(horizontal = 10.u),
                contentAlignment = Alignment.Center,
            ) { Txt("● ЭФИР", 12, Red, FontWeight.ExtraBold, spacing = 0.5f) }
            val tech = listOf(ui.video.ifEmpty { c.quality }, c.categoryText).filter { it.isNotEmpty() }.joinToString(" · ")
            Txt(tech, 13, Muted, FontWeight.Medium, mono = true)
        }
    }
}

@Composable
private fun Controls(store: Store, ui: PlayerUi, engine: Engine, onFull: () -> Unit, compact: Boolean) {
    val c = ui.channel
    val fav = store.isFavorite(c?.id)
    val side = if (compact) 52 else 64
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Btn(side, radius = 18, bg = Panel, onClick = { c?.let { store.toggleFavorite(it) } }) {
            Ic(if (fav) IcStar else IcStarO, 26, if (fav) Amber else Muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 12.u else 18.u)) {
            Btn(if (compact) 60 else 72, 64.coerceAtMost(side + 4), radius = 18, bg = Raised, onClick = { engine.prev() }) { Ic(IcPrev, 30, TextC) }
            Btn(if (compact) 76 else 88, 64.coerceAtMost(side + 4), radius = 32, bg = Amber, onClick = { engine.toggle() }) {
                Ic(if (ui.playing) IcPause else IcPlay, 34, Bg)
            }
            Btn(if (compact) 60 else 72, 64.coerceAtMost(side + 4), radius = 18, bg = Raised, onClick = { engine.next() }) { Ic(IcNext, 30, TextC) }
        }
        Btn(side, radius = 18, bg = Panel, onClick = onFull) { Ic(IcFull, 26, TextC) }
    }
}

@Composable
private fun ChannelRow(c: Channel, selected: Boolean, onAir: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(62.u).clip(RoundedCornerShape(12.u))
            .background(if (selected) Sel else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.u),
    ) {
        Logo(c, 56, 40, 8, 14)
        Column(Modifier.weight(1f)) {
            Txt(c.name, 17, TextC, FontWeight.Bold)
            Txt(c.categoryText, 13, Muted)
        }
        if (onAir) Ic(IcBars, 20, Amber)
    }
}

@Composable
private fun FavList(store: Store, ui: PlayerUi, engine: Engine, onTab: (Int) -> Unit, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.u)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.u)) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(14.u)).background(Bg).padding(5.u),
                horizontalArrangement = Arrangement.spacedBy(6.u),
            ) {
                Box(
                    Modifier.weight(1f).height(42.u).clip(RoundedCornerShape(10.u)).background(Sel),
                    contentAlignment = Alignment.Center,
                ) { Txt("Избранное", 16, TextC, FontWeight.Bold) }
                Box(
                    Modifier.weight(1f).height(42.u).clip(RoundedCornerShape(10.u)).clickable { onTab(1) },
                    contentAlignment = Alignment.Center,
                ) { Txt("Каталог", 16, Muted, FontWeight.Bold) }
            }
        }
        if (store.favorites.isEmpty()) {
            Column(Modifier.padding(8.u), verticalArrangement = Arrangement.spacedBy(14.u)) {
                Txt("Пока пусто. Найдите канал в каталоге и нажмите ★", 16, Muted, maxLines = 3)
                Box(
                    Modifier.height(52.u).clip(RoundedCornerShape(14.u)).background(Amber)
                        .clickable { onTab(1) }.padding(horizontal = 20.u),
                    contentAlignment = Alignment.Center,
                ) { Txt("Открыть каталог", 16, Bg, FontWeight.ExtraBold) }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.u)) {
                items(store.favorites, key = { it.id }) { c ->
                    val cur = ui.channel?.id == c.id
                    ChannelRow(c, cur, cur && ui.playing) { engine.play(c, store.favorites) }
                }
            }
        }
    }
}

// ---------- Магнитола: просмотр ----------
@Composable
private fun WatchLand(store: Store, ui: PlayerUi, engine: Engine, player: Player, onTab: (Int) -> Unit, onFull: () -> Unit) {
    Row(Modifier.fillMaxSize()) {
        FavList(
            store, ui, engine, onTab,
            Modifier.width(300.u).fillMaxHeight().background(Panel)
                .padding(start = 12.u, end = 12.u, top = 18.u, bottom = 12.u),
        )
        Column(
            Modifier.weight(1f).fillMaxHeight().padding(start = 20.u, end = 20.u, top = 18.u, bottom = 16.u),
            verticalArrangement = Arrangement.spacedBy(12.u),
        ) {
            Row(Modifier.height(48.u), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { LiveInfo(ui, big = true) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.u)) {
                    Btn(48, radius = 14, bg = Panel, onClick = { onTab(1) }) { Ic(IcSearch, 22, TextC) }
                    Btn(48, radius = 14, bg = Panel, onClick = { onTab(2) }) { Ic(IcGear, 22, TextC) }
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Video(player, ui, store, Modifier.fillMaxHeight().aspectRatio(16f / 9f), onClick = onFull)
            }
            Controls(store, ui, engine, onFull, compact = false)
        }
    }
}

// ---------- Смартфон: просмотр ----------
@Composable
private fun WatchPortrait(store: Store, ui: PlayerUi, engine: Engine, player: Player, onTab: (Int) -> Unit, onFull: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Video(player, ui, store, Modifier.fillMaxWidth().aspectRatio(16f / 9f), onClick = onFull)
        Column(Modifier.padding(16.u), verticalArrangement = Arrangement.spacedBy(14.u)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { LiveInfo(ui, big = false) }
                Btn(44, radius = 12, bg = Panel, onClick = { onTab(2) }) { Ic(IcGear, 20, TextC) }
            }
            Controls(store, ui, engine, onFull, compact = true)
        }
        FavList(
            store, ui, engine, onTab,
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(topStart = 24.u, topEnd = 24.u))
                .background(Panel).padding(start = 12.u, end = 12.u, top = 14.u, bottom = 12.u),
        )
    }
}

// ---------- Во весь экран ----------
@Composable
private fun FullScreen(store: Store, ui: PlayerUi, engine: Engine, player: Player, onExit: () -> Unit) {
    var shown by remember { mutableIntStateOf(1) }
    LaunchedEffect(shown) {
        if (shown > 0) {
            delay(4_000)
            shown = 0
        }
    }
    Box(Modifier.fillMaxSize().background(Black)) {
        Video(player, ui, store, Modifier.fillMaxSize(), onClick = { shown++ })
        if (shown > 0) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.u)
                    .background(Color(0xD90E1116)).padding(horizontal = 24.u),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.u),
            ) {
                Logo(ui.channel, 56, 40, 8, 14)
                Txt(ui.channel?.name ?: "", 20, TextC, FontWeight.ExtraBold, Modifier.weight(1f))
                Btn(64, 56, radius = 16, bg = Sel, onClick = { engine.prev(); shown++ }) { Ic(IcPrev, 26, TextC) }
                Btn(64, 56, radius = 16, bg = Amber, onClick = { engine.toggle(); shown++ }) {
                    Ic(if (ui.playing) IcPause else IcPlay, 28, Bg)
                }
                Btn(64, 56, radius = 16, bg = Sel, onClick = { engine.next(); shown++ }) { Ic(IcNext, 26, TextC) }
                Btn(64, 56, radius = 16, bg = Sel, onClick = onExit) { Ic(IcExit, 24, TextC) }
            }
        }
    }
}

// ---------- Каталог ----------
@Composable
private fun CatalogScreen(
    store: Store, ui: PlayerUi, c: CatalogState, land: Boolean,
    play: (Channel, List<Channel>) -> Unit, onBack: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val list = remember(c.all, c.query, c.category) { c.filtered() }
    Column(
        Modifier.fillMaxSize().padding(horizontal = if (land) 28.u else 16.u, vertical = 20.u),
        verticalArrangement = Arrangement.spacedBy(16.u),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.u)) {
            Btn(56, radius = 16, bg = Panel, onClick = onBack) { Ic(IcBack, 26, TextC) }
            BasicTextField(
                value = c.query,
                onValueChange = { c.query = it },
                singleLine = true,
                textStyle = TextStyle(color = TextC, fontSize = 19.t, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(Amber),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Row(
                        Modifier.fillMaxWidth().height(56.u).clip(RoundedCornerShape(16.u)).background(Panel)
                            .padding(horizontal = 18.u),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.u),
                    ) {
                        Ic(IcSearch, 24, Muted)
                        Box(Modifier.weight(1f)) {
                            if (c.query.isEmpty()) Txt("Название канала", 19, Muted)
                            inner()
                        }
                        if (c.query.isNotEmpty()) {
                            Box(Modifier.clickable { c.query = "" }.padding(6.u)) { Txt("✕", 18, Muted) }
                        }
                    }
                },
            )
            if (land) {
                Box(
                    Modifier.height(56.u).clip(RoundedCornerShape(16.u)).background(Panel).padding(horizontal = 16.u),
                    contentAlignment = Alignment.Center,
                ) { Txt(channelsWord(list.size), 14, Muted, FontWeight.Bold) }
            }
        }

        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.u),
        ) {
            Chip("Россия", c.russia) { c.russia = !c.russia }
            CATEGORIES.forEach { cat ->
                Chip(Channel.CATEGORY_RU[cat] ?: cat, c.category == cat) {
                    c.category = if (c.category == cat) null else cat
                }
            }
        }

        when {
            c.loading && c.all.isEmpty() -> Txt("Загружаю каталог…", 18, Muted)
            c.error != null && c.all.isEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(12.u)) {
                Txt(c.error!!, 18, Muted)
                Box(
                    Modifier.height(48.u).clip(RoundedCornerShape(14.u)).background(Raised)
                        .clickable { c.reload++ }.padding(horizontal = 18.u),
                    contentAlignment = Alignment.Center,
                ) { Txt("Повторить", 16, TextC, FontWeight.Bold) }
            }
            list.isEmpty() -> Txt("Ничего не нашлось", 18, Muted)
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(if (land) 3 else 1),
                horizontalArrangement = Arrangement.spacedBy(12.u),
                verticalArrangement = Arrangement.spacedBy(12.u),
                modifier = Modifier.weight(1f),
            ) {
                items(list, key = { it.id }) { ch -> ChannelCard(store, ui, ch) { play(ch, list) } }
            }
        }
        Txt("Каталог общедоступных трансляций iptv-org. Часть каналов может не работать.", 13, Muted, maxLines = 2)
    }
}

@Composable
private fun ChannelCard(store: Store, ui: PlayerUi, c: Channel, onPlay: () -> Unit) {
    val fav = store.isFavorite(c.id)
    val cur = ui.channel?.id == c.id
    Row(
        Modifier.fillMaxWidth().height(100.u).clip(RoundedCornerShape(18.u))
            .background(if (cur) Sel else Panel).clickable(onClick = onPlay)
            .padding(start = 12.u, end = 10.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.u),
    ) {
        Logo(c, 80, 56, 10, 18)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.u)) {
            Txt(c.name, 17, TextC, FontWeight.Bold)
            Txt(c.categoryText, 13, Muted)
            val tags = listOfNotNull(
                c.quality.takeIf { it.isNotEmpty() },
                "не 24/7".takeIf { c.notAlways },
                "гео-блок".takeIf { c.geoBlocked },
            )
            if (tags.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.u)) {
                tags.forEach { tag ->
                    Box(
                        Modifier.clip(RoundedCornerShape(5.u)).background(Sel).padding(horizontal = 6.u, vertical = 2.u),
                    ) { Txt(tag, 11, Sub, FontWeight.Medium, mono = true) }
                }
            }
        }
        Btn(44, radius = 12, bg = Color.Transparent, onClick = { store.toggleFavorite(c) }) {
            Ic(if (fav) IcStar else IcStarO, 22, if (fav) Amber else Muted)
        }
        Btn(44, radius = 22, bg = if (cur && ui.playing) Amber else Sel, onClick = onPlay) {
            if (cur && ui.playing) Ic(IcBars, 18, Bg) else Ic(IcPlay, 18, TextC)
        }
    }
}

// ---------- Настройки ----------
@Composable
private fun Section(title: String, text: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.u)).background(Panel).padding(24.u),
        verticalArrangement = Arrangement.spacedBy(14.u),
    ) {
        Txt(title, 22, TextC, FontWeight.ExtraBold)
        Txt(text, 16, Muted, maxLines = 4)
        content()
    }
}

@Composable
private fun SettingsScreen(store: Store, engine: Engine, land: Boolean, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = if (land) 28.u else 16.u, vertical = 20.u),
        verticalArrangement = Arrangement.spacedBy(20.u),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.u)) {
            Btn(56, radius = 16, bg = Panel, onClick = onBack) { Ic(IcBack, 26, TextC) }
            Txt("Настройки", 28, TextC, FontWeight.ExtraBold)
        }
        Section(
            "Запас буфера",
            "Канал начинает показывать, когда накопится запас. Пока он есть, провалы связи не заметны. " +
                "Чем больше запас, тем дольше включается канал и сильнее отставание от эфира.",
        ) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.u)) {
                BUFFER_STEPS.forEach { s ->
                    Chip(if (s == 0) "Авто" else "$s с", store.bufferSec == s) { engine.setBuffer(s) }
                }
            }
        }
        Section(
            "Готовить следующий канал",
            "Следующий канал списка заранее подключается без звука, и «Вперёд» срабатывает мгновенно. " +
                "Трафик примерно вдвое больше, а слабую магнитолу два видеопотока могут нагружать.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.u)) {
                Chip("Включено", store.prebuffer) { engine.setPrebuffer(true) }
                Chip("Выключено", !store.prebuffer) { engine.setPrebuffer(false) }
            }
        }
        Section(
            "Только звук",
            "Картинка не показывается — удобно в дороге или при слабом интернете: меньше нагрузка на магнитолу.",
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.u)) {
                Chip("Включено", store.audioOnly) { engine.setAudioOnly(true) }
                Chip("Выключено", !store.audioOnly) { engine.setAudioOnly(false) }
            }
        }
        Spacer(Modifier.height(4.u))
        Txt("ТВ · сборка ${BuildConfig.VERSION_CODE}", 14, Muted, FontWeight.Medium)
    }
}
