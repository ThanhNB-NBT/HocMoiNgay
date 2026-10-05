package com.thanhnb.hocmoingay.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.thanhnb.hocmoingay.R

val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
)

@OptIn(ExperimentalTextApi::class)
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.jetbrains_mono, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.jetbrains_mono, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
)

/** Kiểu IDE: tiêu đề dùng mono cho "font mono nổi bật hơn"; thân chữ luôn Be Vietnam Pro để dấu tiếng Việt dễ đọc. */
fun appTypography(ide: Boolean): Typography {
    val b = Typography()
    val head = if (ide) JetBrainsMono else BeVietnamPro
    fun TextStyle.f(ff: FontFamily) = copy(fontFamily = ff)
    return Typography(
        displayLarge = b.displayLarge.f(head), displayMedium = b.displayMedium.f(head), displaySmall = b.displaySmall.f(head),
        headlineLarge = b.headlineLarge.f(head), headlineMedium = b.headlineMedium.f(head), headlineSmall = b.headlineSmall.f(head),
        titleLarge = b.titleLarge.f(head), titleMedium = b.titleMedium.f(BeVietnamPro), titleSmall = b.titleSmall.f(BeVietnamPro),
        bodyLarge = b.bodyLarge.f(BeVietnamPro), bodyMedium = b.bodyMedium.f(BeVietnamPro), bodySmall = b.bodySmall.f(BeVietnamPro),
        labelLarge = b.labelLarge.f(BeVietnamPro), labelMedium = b.labelMedium.f(BeVietnamPro), labelSmall = b.labelSmall.f(BeVietnamPro),
    )
}
