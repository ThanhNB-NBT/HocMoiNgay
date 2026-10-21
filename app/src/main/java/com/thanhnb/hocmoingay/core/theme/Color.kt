package com.thanhnb.hocmoingay.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Accent của một mảng. Cặp nào cũng đạt tương phản ≥ 4.5:1 (ThemeTest). */
data class TrackColors(val accent: Color, val onAccent: Color, val container: Color, val onContainer: Color)

data class TrackPalette(val code: TrackColors, val english: TrackColors)

/** Mực xanh đậm: chữ chính ở chế độ sáng. */
val Ink = Color(0xFF14213D)

/** Nền sáng: trắng pha chút xanh trời; thẻ nội dung là trắng tinh nổi trên nó. */
val Sky = Color(0xFFF3F7FF)

/** Xanh dương tươi: nút chính, khối mục tiêu ngày. */
val Azure = Color(0xFF2F5FF5)

// "Bầu trời": nền xanh rất nhạt, thẻ trắng, xanh dương tươi làm màu chính.
// Lập trình: xanh dương; Tiếng Anh: tím than.
val CodeLight = TrackColors(Color(0xFF2F5BEA), Color.White, Color(0xFFE3EBFF), Color(0xFF1F3FB3))
val CodeDark = TrackColors(Color(0xFF8FB0FF), Color(0xFF0B1530), Color(0xFF1F2D57), Color(0xFFDCE6FF))
val EnglishLight = TrackColors(Color(0xFF6D4AD8), Color.White, Color(0xFFECE6FF), Color(0xFF3B2390))
val EnglishDark = TrackColors(Color(0xFFB9A4FF), Color(0xFF1E1050), Color(0xFF2E2257), Color(0xFFECE6FF))
// IDE — một accent amber cho cả hai mảng
val AmberLight = TrackColors(Color(0xFFB45309), Color.White, Color(0xFFFEF3C7), Color(0xFF78350F))
val AmberDark = TrackColors(Color(0xFFFBBF24), Color(0xFF451A03), Color(0xFF78350F), Color(0xFFFEF3C7))

/** Màu phụ: đỏ cam cho câu sai và khoảnh khắc chuỗi ngày, xanh lá cho câu đúng. */
data class FunColors(
    val coral: Color, val onCoral: Color, val coralContainer: Color, val onCoralContainer: Color,
    val mint: Color, val mintContainer: Color,
)

val FunLight = FunColors(Color(0xFFC73A22), Color.White, Color(0xFFFDE7E2), Color(0xFF6B1C0E), Color(0xFF12A06A), Color(0xFFDFF7EC))
val FunDark = FunColors(Color(0xFFFF8A70), Color(0xFF2A0D05), Color(0xFF41201A), Color(0xFFFFDAD2), Color(0xFF4CD69A), Color(0xFF123A2A))

fun twoToneScheme(dark: Boolean): ColorScheme {
    val c = if (dark) CodeDark else CodeLight
    val e = if (dark) EnglishDark else EnglishLight
    val base = if (dark) {
        darkColorScheme(
            primary = Color(0xFF7FA2FF), onPrimary = Color(0xFF0B1530),
            primaryContainer = Color(0xFF171E2D), onPrimaryContainer = Color(0xFFE6ECF8),
            secondaryContainer = Color(0xFF22305A), onSecondaryContainer = Color(0xFFDCE6FF),
            background = Color(0xFF0F1420), onBackground = Color(0xFFE6ECF8),
            surface = Color(0xFF0F1420), onSurface = Color(0xFFE6ECF8),
            surfaceVariant = Color(0xFF222A3D), onSurfaceVariant = Color(0xFF9AA8C4),
            outline = Color(0xFF64708A), outlineVariant = Color(0xFF263049),
            surfaceContainerLowest = Color(0xFF171E2D), surfaceContainerLow = Color(0xFF131927), // lowest làm mặt thẻ nên sáng hơn nền
            surfaceContainer = Color(0xFF1A2133), surfaceContainerHigh = Color(0xFF222A3D),
            surfaceContainerHighest = Color(0xFF2B3449),
        )
    } else {
        lightColorScheme(
            primary = Azure, onPrimary = Color.White,
            // khối nội dung: thẻ trắng trên nền xanh nhạt, Pushable tự thêm viền mảnh
            primaryContainer = Color.White, onPrimaryContainer = Ink,
            secondaryContainer = Color(0xFFE3EBFF), onSecondaryContainer = Color(0xFF1F3FB3),
            background = Sky, onBackground = Ink,
            surface = Sky, onSurface = Ink,
            surfaceVariant = Color(0xFFE3EBFA), onSurfaceVariant = Color(0xFF4E5D7C),
            outline = Color(0xFF75839F), outlineVariant = Color(0xFFDCE4F2),
            surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFEAF0FC),
            surfaceContainer = Color(0xFFE3EBFA), surfaceContainerHigh = Color(0xFFD9E2F5),
            surfaceContainerHighest = Color(0xFFCDD8EE),
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
