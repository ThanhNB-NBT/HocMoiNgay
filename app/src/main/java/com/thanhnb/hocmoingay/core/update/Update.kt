package com.thanhnb.hocmoingay.core.update

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.content.edit
import com.thanhnb.hocmoingay.BuildConfig
import com.thanhnb.hocmoingay.core.ui.PushButton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cập nhật trong app (như Gác Truyện): đọc release mới nhất của repo public trên GitHub, so version,
 * tải APK vào cacheDir rồi gọi trình cài hệ thống cài đè. App không lên chợ ứng dụng nên đây là đường cập nhật duy nhất.
 */
data class Release(val version: String, val apkUrl: String, val notes: String)

private const val REPO = "ThanhNB-NBT/HocMoiNgay"

/** Release mới hơn bản đang chạy, null nếu đã mới nhất (hoặc release chưa có APK). Lỗi mạng thì ném. */
suspend fun latestRelease(current: String = BuildConfig.VERSION_NAME): Release? = withContext(Dispatchers.IO) {
    val c = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
    c.connectTimeout = 10_000
    c.readTimeout = 10_000
    c.setRequestProperty("Accept", "application/vnd.github+json")
    if (c.responseCode == 404) return@withContext null // chưa có release nào
    if (c.responseCode != 200) throw IOException("GitHub trả ${c.responseCode}")
    val r = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
    val version = r.optString("tag_name").removePrefix("v")
    if (!isNewer(version, current)) return@withContext null
    val assets = r.optJSONArray("assets") ?: return@withContext null
    val apk = (0 until assets.length()).map(assets::getJSONObject)
        .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) } ?: return@withContext null
    Release(version, apk.getString("browser_download_url"), r.optString("body"))
}

/** So version x.y.z theo từng số (1.10.0 > 1.9.9). */
internal fun isNewer(a: String, b: String): Boolean {
    fun nums(s: String) = Regex("""\d+""").findAll(s).map { it.value.toInt() }.toList()
    val x = nums(a)
    val y = nums(b)
    for (i in 0 until 3) {
        val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
        if (d != 0) return d > 0
    }
    return false
}

private suspend fun downloadApk(ctx: Context, url: String, onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
    val f = File(ctx.cacheDir, "updates/HocMoiNgay.apk").apply { parentFile!!.mkdirs() } // ghi đè bản tải dở lần trước
    val c = URL(url).openConnection() as HttpURLConnection // theo 302 sang CDN của GitHub
    c.connectTimeout = 15_000
    c.readTimeout = 30_000
    if (c.responseCode != 200) throw IOException("HTTP ${c.responseCode}")
    val total = c.contentLengthLong
    c.inputStream.use { input ->
        f.outputStream().use { out ->
            val buf = ByteArray(64 * 1024)
            var got = 0L
            while (true) {
                ensureActive() // bấm Huỷ thì dừng giữa chừng
                val n = input.read(buf)
                if (n < 0) break
                out.write(buf, 0, n)
                got += n
                if (total > 0) onProgress(got.toFloat() / total)
            }
        }
    }
    f
}

/** Trình cài hệ thống cài đè; lần đầu Android hỏi cho phép "cài ứng dụng không rõ nguồn" từ app này. */
private fun install(ctx: Context, apk: File) {
    val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", apk)
    ctx.startActivity(
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

/** Hỏi tải bản mới; đang tải thì hiện tiến trình, đóng hộp thoại là huỷ tải. */
@Composable
fun UpdateDialog(release: Release, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var progress by remember { mutableStateOf<Float?>(null) } // null = chưa tải
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = { if (progress == null) onDismiss() },
        title = { Text(if (progress == null) "Có bản mới ${release.version}" else "Đang tải bản mới…") },
        text = {
            val p = progress
            when {
                p != null -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator({ p }, Modifier.fillMaxWidth())
                    Text("${(p * 100).toInt()}%")
                }
                error != null -> Text("Tải không được: $error")
                else -> Text(release.notes.ifBlank { "Tải bản mới rồi cài đè bản đang dùng." }, maxLines = 12, overflow = TextOverflow.Ellipsis)
            }
        },
        confirmButton = {
            if (progress == null) TextButton(onClick = {
                progress = 0f
                error = null
                scope.launch {
                    try {
                        install(ctx, downloadApk(ctx, release.apkUrl) { progress = it })
                        onDismiss()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w("Update", "tải APK lỗi", e)
                        error = e.message ?: "lỗi mạng"
                        progress = null
                    }
                }
            }) { Text(if (error == null) "Tải & cài" else "Thử lại") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (progress == null) "Để sau" else "Huỷ") } },
    )
}

/**
 * Mở app thì kiểm im lặng (chỉ bản release — bản dev ký khác, cài đè không được).
 * Mỗi version chỉ hỏi một lần; cờ "đã hỏi" ghi SAU khi hộp thoại đóng, để app tắt ngang thì lần sau vẫn hỏi lại.
 */
@Composable
fun AutoUpdatePrompt() {
    if (BuildConfig.BUILD_TYPE != "release") return
    val prefs = LocalContext.current.getSharedPreferences("update", Context.MODE_PRIVATE)
    var release by remember { mutableStateOf<Release?>(null) }
    LaunchedEffect(Unit) {
        release = runCatching { latestRelease() }.getOrNull()?.takeIf { it.version != prefs.getString("dismissed", null) }
    }
    release?.let { r ->
        UpdateDialog(r) {
            prefs.edit { putString("dismissed", r.version) }
            release = null
        }
    }
}

/** Mục "Phiên bản" trong Cài đặt: kiểm chủ động, kể cả version đã bấm "Để sau". */
@Composable
fun UpdateCheck() {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }
    var release by remember { mutableStateOf<Release?>(null) }
    Text(
        status ?: "Đang dùng bản ${BuildConfig.VERSION_NAME}.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PushButton(if (checking) "Đang kiểm tra…" else "Kiểm tra cập nhật", {
        checking = true
        scope.launch {
            try {
                release = latestRelease()
                status = if (release == null) "Bản ${BuildConfig.VERSION_NAME} là mới nhất." else null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status = "Không kiểm tra được, thử lại khi có mạng."
            } finally {
                checking = false
            }
        }
    }, Modifier.fillMaxWidth(), enabled = !checking)
    release?.let { UpdateDialog(it) { release = null } }
}
