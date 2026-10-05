package com.thanhnb.hocmoingay.core.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ThemeStyle { @SerialName("two_tone") TWO_TONE, @SerialName("wallpaper") WALLPAPER, @SerialName("ide") IDE }

@Serializable
enum class ThemeMode { @SerialName("system") SYSTEM, @SerialName("light") LIGHT, @SerialName("dark") DARK }

enum class Track { CODE, ENGLISH }

fun isDark(mode: ThemeMode, systemDark: Boolean) = when (mode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

val LocalTrackPalette = staticCompositionLocalOf { TrackPalette(CodeLight, EnglishLight) }

/** Accent của mảng đang hiển thị (spec §7.2). Đặt bằng [ProvideTrack]. */
val LocalTrack = staticCompositionLocalOf { CodeLight }

@Composable
fun HocTheme(style: ThemeStyle, dark: Boolean, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    val (scheme, palette) = when {
        style == ThemeStyle.WALLPAPER && Build.VERSION.SDK_INT >= 31 -> {
            val s = if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            s to TrackPalette(
                TrackColors(s.primary, s.onPrimary, s.primaryContainer, s.onPrimaryContainer),
                TrackColors(s.tertiary, s.onTertiary, s.tertiaryContainer, s.onTertiaryContainer),
            )
        }
        style == ThemeStyle.IDE -> {
            val a = if (dark) AmberDark else AmberLight
            ideScheme(dark) to TrackPalette(a, a)
        }
        // Hai sắc, và Hình nền trên máy < Android 12
        else -> twoToneScheme(dark) to if (dark) TrackPalette(CodeDark, EnglishDark) else TrackPalette(CodeLight, EnglishLight)
    }
    CompositionLocalProvider(LocalTrackPalette provides palette, LocalTrack provides palette.code) {
        // material3 1.4.0 (BOM 2026.09.00) còn để MaterialExpressiveTheme internal; đổi khi bản ổn định mở ra
        MaterialTheme(
            colorScheme = scheme,
            typography = appTypography(ide = style == ThemeStyle.IDE),
            content = content,
        )
    }
}

@Composable
fun ProvideTrack(track: Track, content: @Composable () -> Unit) {
    val p = LocalTrackPalette.current
    CompositionLocalProvider(LocalTrack provides if (track == Track.CODE) p.code else p.english, content = content)
}
