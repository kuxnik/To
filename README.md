# ТВ

Онлайн-трансляции ТВ для Android-магнитолы (1024×600) и смартфонов.

- Каталог общедоступных каналов [iptv-org](https://github.com/iptv-org/iptv): Россия или весь мир, категории, поиск.
- Избранное, полный экран, кнопки на руле (MediaSession).
- Запас буфера до 60 с, пребуферизация следующего канала, режим «только звук».

**Скачать:** [TV.apk](https://github.com/kuxnik/To/releases/latest/download/TV.apk)

Каждый пуш в `main` собирает APK через GitHub Actions и кладёт его в Releases.
Стек: Kotlin, Jetpack Compose, Media3 (ExoPlayer, HLS/DASH, PlayerView), Coil.
