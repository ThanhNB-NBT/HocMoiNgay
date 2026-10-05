package com.thanhnb.hocmoingay.feature.settings

import com.thanhnb.hocmoingay.core.net.SyncJson
import com.thanhnb.hocmoingay.core.theme.ThemeMode
import com.thanhnb.hocmoingay.core.theme.ThemeStyle
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

/** settings.data (spec §4.1). Thêm trường mới thì luôn có giá trị mặc định. */
@Serializable
data class AppSettings(
    val theme: ThemeStyle = ThemeStyle.TWO_TONE,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    @SerialName("daily_minutes") val dailyMinutes: Int = 20,
    /** "HH:mm", tăng dần, không trùng (giai đoạn e đặt lịch nhắc theo danh sách này). */
    val reminders: List<String> = listOf("20:00"),
    @SerialName("preferred_language") val preferredLanguage: String = "python",
    @SerialName("english_level") val englishLevel: String? = null,
    @SerialName("active_courses") val activeCourses: List<String> = emptyList(),
)

val LANGUAGES = linkedMapOf(
    "python" to "Python", "javascript" to "JavaScript", "typescript" to "TypeScript",
    "java" to "Java", "kotlin" to "Kotlin", "go" to "Go", "rust" to "Rust", "cpp" to "C++", "csharp" to "C#",
)

// coerceInputValues: giá trị enum lạ (từ bản app mới hơn) về mặc định thay vì làm hỏng cả cài đặt
private val SettingsJson = Json(SyncJson) { coerceInputValues = true }

fun decodeSettings(data: String): AppSettings =
    runCatching { SettingsJson.decodeFromString(AppSettings.serializer(), data) }.getOrDefault(AppSettings())

/** Ghi các khoá app này biết, giữ nguyên khoá lạ của bản app mới hơn. */
fun encodeSettings(s: AppSettings, old: String?): String {
    val oldObj = old?.let { runCatching { SettingsJson.parseToJsonElement(it).jsonObject }.getOrNull() } ?: JsonObject(emptyMap())
    return JsonObject(oldObj + SettingsJson.encodeToJsonElement(AppSettings.serializer(), s).jsonObject).toString()
}

fun addReminder(list: List<String>, hhmm: String) = (list + hhmm).distinct().sorted()

/** IDE "ưu tiên tối" (spec §7.2): chọn IDE khi đang theo hệ thống thì chuyển Tối; người học vẫn đổi lại được. */
fun AppSettings.withTheme(t: ThemeStyle) =
    copy(theme = t, mode = if (t == ThemeStyle.IDE && mode == ThemeMode.SYSTEM) ThemeMode.DARK else mode)
