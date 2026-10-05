package com.thanhnb.hocmoingay.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Accent của một mảng. Cặp nào cũng đạt tương phản ≥ 4.5:1 (ThemeTest). */
data class TrackColors(val accent: Color, val onAccent: Color, val container: Color, val onContainer: Color)

data class TrackPalette(val code: TrackColors, val english: TrackColors)

/** Mực gần đen nghiêng lạnh: chữ trên khối vàng lê, coral, accent sáng của chế độ tối. */
val Ink = Color(0xFF12171B)

/** Vàng lê: mặt nút khối, thẻ mục tiêu, tab đang chọn. Không bao giờ làm màu chữ. */
val Pear = Color(0xFFF6CE00)

// Hai sắc (theme "Hum" của hallmark, đổi từ OKLCH): nền kem, vàng lê là khối chính, mỗi mảng một màu riêng.
// Lập trình: tím oải hương; Tiếng Anh: xanh cyan đậm.
val CodeLight = TrackColors(Color(0xFF773AC1), Color.White, Color(0xFFEAD9FF), Color(0xFF3A1463))
val CodeDark = TrackColors(Color(0xFFCBA1FA), Ink, Color(0xFF3E2660), Color(0xFFEBDEFB))
val EnglishLight = TrackColors(Color(0xFF006CA2), Color.White, Color(0xFFC2E7FE), Color(0xFF003354))
val EnglishDark = TrackColors(Color(0xFF64C4F0), Ink, Color(0xFF003A54), Color(0xFFCCEAFC))
// IDE — một accent amber cho cả hai mảng
val AmberLight = TrackColors(Color(0xFFB45309), Color.White, Color(0xFFFEF3C7), Color(0xFF78350F))
val AmberDark = TrackColors(Color(0xFFFBBF24), Color(0xFF451A03), Color(0xFF78350F), Color(0xFFFEF3C7))

/** Màu phụ: coral cho đúng một khoảnh khắc mạnh mỗi màn (chuỗi ngày, thành tích), mint cho trạng thái đúng. */
data class FunColors(
    val coral: Color, val onCoral: Color, val coralContainer: Color, val onCoralContainer: Color,
    val mint: Color, val mintContainer: Color,
)

val FunLight = FunColors(Color(0xFFF53B4B), Ink, Color(0xFFFFD5D2), Color(0xFF5C0E15), Color(0xFF00884B), Color(0xFFC6F1D3))
val FunDark = FunColors(Color(0xFFFF696D), Ink, Color(0xFF572222), Color(0xFFFFD9D6), Color(0xFF63D18F), Color(0xFF153C25))

fun twoToneScheme(dark: Boolean): ColorScheme {
    val c = if (dark) CodeDark else CodeLight
    val e = if (dark) EnglishDark else EnglishLight
    val base = if (dark) {
        darkColorScheme(
            // tối: vàng lê vừa là primary (đủ sáng làm chữ trên nền tối) vừa là container
            primary = Pear, onPrimary = Ink, primaryContainer = Pear, onPrimaryContainer = Ink,
            secondaryContainer = Color(0xFF44360D), onSecondaryContainer = Color(0xFFF5E5A8),
            background = Color(0xFF101419), onBackground = Color(0xFFF1EFE6),
            surface = Color(0xFF101419), onSurface = Color(0xFFF1EFE6),
            surfaceVariant = Color(0xFF262C33), onSurfaceVariant = Color(0xFFA7AFB7),
            outline = Color(0xFF6B727A), outlineVariant = Color(0xFF353B42),
            surfaceContainerLowest = Color(0xFF1A1F25), surfaceContainerLow = Color(0xFF161B21), // lowest dùng làm mặt thẻ nên sáng hơn nền
            surfaceContainer = Color(0xFF1D2228), surfaceContainerHigh = Color(0xFF262C33),
            surfaceContainerHighest = Color(0xFF2F363D),
        )
    } else {
        lightColorScheme(
            // sáng: vàng lê trên nền kem chỉ ~1.3:1 nên primary (chữ, công tắc, viền ô nhập) là vàng đậm, vàng lê lui về container
            primary = Color(0xFF7A5700), onPrimary = Color.White, primaryContainer = Pear, onPrimaryContainer = Ink,
            secondaryContainer = Color(0xFFF8E8AB), onSecondaryContainer = Color(0xFF432F00),
            background = Color(0xFFF7F5EC), onBackground = Ink,
            surface = Color(0xFFF7F5EC), onSurface = Ink,
            surfaceVariant = Color(0xFFEEEBDF), onSurfaceVariant = Color(0xFF4F565E),
            outline = Color(0xFF81878D), outlineVariant = Color(0xFFD8D4C6),
            surfaceContainerLowest = Color(0xFFFFFDF7), surfaceContainerLow = Color(0xFFF3F0E6),
            surfaceContainer = Color(0xFFEEEBDF), surfaceContainerHigh = Color(0xFFE5E1D3),
            surfaceContainerHighest = Color(0xFFDCD8C8),
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
