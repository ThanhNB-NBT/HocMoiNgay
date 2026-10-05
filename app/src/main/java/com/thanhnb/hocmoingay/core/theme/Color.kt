package com.thanhnb.hocmoingay.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Accent của một mảng. Cặp nào cũng đạt tương phản ≥ 4.5:1 (ThemeTest). */
data class TrackColors(val accent: Color, val onAccent: Color, val container: Color, val onContainer: Color)

data class TrackPalette(val code: TrackColors, val english: TrackColors)

// Hai sắc — Lập trình: indigo (palette "Educational App", ui-craft); Tiếng Anh: teal đậm
val CodeLight = TrackColors(Color(0xFF4F46E5), Color.White, Color(0xFFE0E7FF), Color(0xFF312E81))
val CodeDark = TrackColors(Color(0xFFA5B4FC), Color(0xFF1E1B4B), Color(0xFF3730A3), Color(0xFFE0E7FF))
val EnglishLight = TrackColors(Color(0xFF0F766E), Color.White, Color(0xFFCCFBF1), Color(0xFF134E4A))
val EnglishDark = TrackColors(Color(0xFF5EEAD4), Color(0xFF042F2E), Color(0xFF115E59), Color(0xFFCCFBF1))
// IDE — một accent amber cho cả hai mảng
val AmberLight = TrackColors(Color(0xFFB45309), Color.White, Color(0xFFFEF3C7), Color(0xFF78350F))
val AmberDark = TrackColors(Color(0xFFFBBF24), Color(0xFF451A03), Color(0xFF78350F), Color(0xFFFEF3C7))

fun twoToneScheme(dark: Boolean): ColorScheme {
    val c = if (dark) CodeDark else CodeLight
    val e = if (dark) EnglishDark else EnglishLight
    val base = if (dark) {
        darkColorScheme(
            secondary = Color(0xFFC4B5FD), onSecondary = Color(0xFF2E1065),
            secondaryContainer = Color(0xFF5B21B6), onSecondaryContainer = Color(0xFFEDE9FE),
            background = Color(0xFF131318), onBackground = Color(0xFFE4E1E9),
            surface = Color(0xFF131318), onSurface = Color(0xFFE4E1E9),
        )
    } else {
        lightColorScheme(
            secondary = Color(0xFF6D28D9), onSecondary = Color.White,
            secondaryContainer = Color(0xFFEDE9FE), onSecondaryContainer = Color(0xFF4C1D95),
            background = Color(0xFFFBFBFE), onBackground = Color(0xFF1B1B21),
            surface = Color(0xFFFBFBFE), onSurface = Color(0xFF1B1B21),
        )
    }
    return base.copy(
        primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.container, onPrimaryContainer = c.onContainer,
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
