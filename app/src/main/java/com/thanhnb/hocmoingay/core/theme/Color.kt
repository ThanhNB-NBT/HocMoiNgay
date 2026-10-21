package com.thanhnb.hocmoingay.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Accent của một mảng. Cặp nào cũng đạt tương phản ≥ 4.5:1 (ThemeTest). */
data class TrackColors(val accent: Color, val onAccent: Color, val container: Color, val onContainer: Color)

data class TrackPalette(val code: TrackColors, val english: TrackColors)

/** Mực: chữ chính và nút hành động chính ở chế độ sáng. */
val Ink = Color(0xFF16181D)

/** Giấy: nền chế độ sáng, chữ và nút chính ở chế độ tối. */
val Paper = Color(0xFFF7F6F2)

// "Giấy mực": nền giấy, chữ mực, mỗi mảng một màu trầm. Không màu nào chỉ để trang trí.
// Lập trình: xanh rừng; Tiếng Anh: xanh mực.
val CodeLight = TrackColors(Color(0xFF2D6A4F), Color.White, Color(0xFFDCEBE2), Color(0xFF12372A))
val CodeDark = TrackColors(Color(0xFF7CC4A0), Color(0xFF0E2219), Color(0xFF1E3A2F), Color(0xFFCFEBDC))
val EnglishLight = TrackColors(Color(0xFF1F4F8F), Color.White, Color(0xFFDCE6F4), Color(0xFF0E2A52))
val EnglishDark = TrackColors(Color(0xFF8DB4EA), Color(0xFF0B1A2E), Color(0xFF1C2F4A), Color(0xFFD6E4F7))
// IDE — một accent amber cho cả hai mảng
val AmberLight = TrackColors(Color(0xFFB45309), Color.White, Color(0xFFFEF3C7), Color(0xFF78350F))
val AmberDark = TrackColors(Color(0xFFFBBF24), Color(0xFF451A03), Color(0xFF78350F), Color(0xFFFEF3C7))

/** Màu phụ: đất nung cho câu sai và khoảnh khắc chuỗi ngày, xanh rừng cho câu đúng. */
data class FunColors(
    val coral: Color, val onCoral: Color, val coralContainer: Color, val onCoralContainer: Color,
    val mint: Color, val mintContainer: Color,
)

val FunLight = FunColors(Color(0xFFB9472F), Color.White, Color(0xFFF6E2DB), Color(0xFF5A1A0E), Color(0xFF2D6A4F), Color(0xFFDCEBE2))
val FunDark = FunColors(Color(0xFFF08A6E), Color(0xFF2A0D05), Color(0xFF3D1E16), Color(0xFFF8D9CF), Color(0xFF7CC4A0), Color(0xFF1E3A2F))

fun twoToneScheme(dark: Boolean): ColorScheme {
    val c = if (dark) CodeDark else CodeLight
    val e = if (dark) EnglishDark else EnglishLight
    val base = if (dark) {
        darkColorScheme(
            primary = Color(0xFFECEAE4), onPrimary = Color(0xFF121316),
            primaryContainer = Color(0xFF26282C), onPrimaryContainer = Color(0xFFECEAE4),
            secondaryContainer = Color(0xFF26282C), onSecondaryContainer = Color(0xFFECEAE4),
            background = Color(0xFF121316), onBackground = Color(0xFFECEAE4),
            surface = Color(0xFF121316), onSurface = Color(0xFFECEAE4),
            surfaceVariant = Color(0xFF26282C), onSurfaceVariant = Color(0xFFA3A6AB),
            outline = Color(0xFF6E7176), outlineVariant = Color(0xFF2E3034),
            surfaceContainerLowest = Color(0xFF1B1C20), surfaceContainerLow = Color(0xFF17181B), // lowest làm mặt thẻ nên sáng hơn nền
            surfaceContainer = Color(0xFF1E1F23), surfaceContainerHigh = Color(0xFF26282C),
            surfaceContainerHighest = Color(0xFF2F3135),
        )
    } else {
        lightColorScheme(
            primary = Ink, onPrimary = Color.White,
            primaryContainer = Color(0xFFEAE8E1), onPrimaryContainer = Ink,
            secondaryContainer = Color(0xFFEAE8E1), onSecondaryContainer = Ink,
            background = Paper, onBackground = Ink,
            surface = Paper, onSurface = Ink,
            surfaceVariant = Color(0xFFEDEBE5), onSurfaceVariant = Color(0xFF5A5F66),
            outline = Color(0xFF8A8E94), outlineVariant = Color(0xFFE0DED7),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F0EB),
            surfaceContainer = Color(0xFFECEAE4), surfaceContainerHigh = Color(0xFFE4E2DB),
            surfaceContainerHighest = Color(0xFFDBD9D1),
        )
    }
    return base.copy(
        secondary = c.accent, onSecondary = c.onAccent,
        tertiary = e.accent, onTertiary = e.onAccent, tertiaryContainer = e.container, onTertiaryContainer = e.onContainer,
    )
}

fun ideScheme(dark: Boolean): ColorScheme {
    val a = if (dark) AmberDark else AmberLight
    return if (dark) {
        darkColorScheme(
            primary = a.accent, onPrimary = a.onAccent, primaryContainer = a.container, onPrimaryContainer = a.onContainer,
            secondary = Color(0xFFD4D4D8), onSecondary = Color(0xFF18181B),
            secondaryContainer = Color(0xFF3F3F46), onSecondaryContainer = Color(0xFFF4F4F5),
            tertiary = a.accent, onTertiary = a.onAccent, tertiaryContainer = a.container, onTertiaryContainer = a.onContainer,
            background = Color(0xFF18181B), onBackground = Color(0xFFF4F4F5),
            surface = Color(0xFF18181B), onSurface = Color(0xFFF4F4F5),
            surfaceVariant = Color(0xFF27272A), onSurfaceVariant = Color(0xFFA1A1AA),
            outline = Color(0xFF71717A), outlineVariant = Color(0xFF3F3F46),
            surfaceContainerLowest = Color(0xFF0F0F11), surfaceContainerLow = Color(0xFF1F1F23),
            surfaceContainer = Color(0xFF222226), surfaceContainerHigh = Color(0xFF27272A),
            surfaceContainerHighest = Color(0xFF2F2F34),
        )
    } else {
        lightColorScheme(
            primary = a.accent, onPrimary = a.onAccent, primaryContainer = a.container, onPrimaryContainer = a.onContainer,
            secondary = Color(0xFF52525B), onSecondary = Color.White,
            secondaryContainer = Color(0xFFE4E4E7), onSecondaryContainer = Color(0xFF18181B),
            tertiary = a.accent, onTertiary = a.onAccent, tertiaryContainer = a.container, onTertiaryContainer = a.onContainer,
            background = Color(0xFFFAFAFA), onBackground = Color(0xFF18181B),
            surface = Color(0xFFFAFAFA), onSurface = Color(0xFF18181B),
            surfaceVariant = Color(0xFFF4F4F5), onSurfaceVariant = Color(0xFF52525B),
            outline = Color(0xFF71717A), outlineVariant = Color(0xFFD4D4D8),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F7F8),
            surfaceContainer = Color(0xFFF4F4F5), surfaceContainerHigh = Color(0xFFEDEDEF),
            surfaceContainerHighest = Color(0xFFE4E4E7),
        )
    }
}
