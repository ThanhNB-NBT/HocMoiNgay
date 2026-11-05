package com.thanhnb.hocmoingay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.thanhnb.hocmoingay.core.net.CodeApi
import com.thanhnb.hocmoingay.core.db.LessonEntity
import com.thanhnb.hocmoingay.core.lesson.LessonRepo
import com.thanhnb.hocmoingay.core.theme.HocTheme
import com.thanhnb.hocmoingay.core.theme.ThemeStyle
import com.thanhnb.hocmoingay.feature.player.PlayerScreen
import com.thanhnb.hocmoingay.feature.player.PlayerViewModel
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.thanhnb.hocmoingay.core.code.LocalAssets
import com.thanhnb.hocmoingay.core.net.Assets
import com.thanhnb.hocmoingay.core.speech.LocalStt
import com.thanhnb.hocmoingay.core.speech.LocalTts
import com.thanhnb.hocmoingay.core.speech.Stt
import com.thanhnb.hocmoingay.core.speech.Tts
import java.io.File

/**
 * Chỉ có ở bản debug: mở màn học với một bài nằm sẵn trong assets/showcase, không cần đăng nhập hay server.
 * adb shell am start -n com.thanhnb.hocmoingay/com.thanhnb.hocmoingay.ShowcaseActivity --es lesson bien --ez dark false
 */
class ShowcaseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val name = intent.getStringExtra("lesson") ?: "bien"
        val dark = intent.getBooleanExtra("dark", false)
        val track = if (name == "can" || name.startsWith("en-")) "english" else "code"
        val json = assets.open("showcase/$name.json").bufferedReader().readText()
        val repo = LessonRepo(
            lesson = { LessonEntity(it, "showcase", body = json) }, track = { track },
            getProgress = { null }, putProgress = {}, cardsByIds = { emptyList() }, putCards = {},
            tx = { it() }, userId = { null }, afterWrite = {},
        )
        // bài en-*: "feedback" trả nhận xét mẫu có điểm từng tiêu chí để chụp màn rubric mà không cần server
        val api = CodeApi { fn, _ -> if (fn == "feedback") FAKE_FB else "{}" }
        val vm = PlayerViewModel(name, repo, api, MutableStateFlow(name.startsWith("en-")), flowOf(null), MainScope())
        // ảnh card đọc từ assets/showcase/<khoá bỏ dấu _ đầu>/<file> của bản debug thay cho storage
        val pics = Assets(File(cacheDir, "showcase-assets")) { key -> assets.open("showcase/" + key.removePrefix("_")).readBytes() } // aapt bỏ thư mục bắt đầu bằng _
        setContent {
            HocTheme(ThemeStyle.TWO_TONE, dark) {
                CompositionLocalProvider(LocalAssets provides pics, LocalTts provides remember { Tts(this) }, LocalStt provides remember { Stt(this) }) {
                    Surface(Modifier.fillMaxSize()) {
                        PlayerScreen(vm, name, onBack = ::finish, onOpenEditor = {})
                    }
                }
            }
        }
    }
}

private const val FAKE_FB = """{"task_done":true,"score":7,"fixes":[{"wrong":"The visitors was increase","right":"The number of visitors increased","why":"Chủ ngữ số nhiều và cần thì quá khứ."}],
"tone":"Trang trọng, hợp bài thi.","better_version":"","used_chunks":["rose considerably"],"notes":"Thiếu câu tổng quan ở đầu bài.",
"criteria":[{"name":"Task Achievement","score":6.0,"note":"Nêu thêm xu hướng chung và so sánh tháng cao nhất với thấp nhất."},
{"name":"Coherence and Cohesion","score":6.5,"note":"Chia đoạn rõ; dùng từ nối đa dạng hơn."},
{"name":"Lexical Resource","score":7.0,"note":"Từ vựng tả xu hướng tốt."},
{"name":"Grammatical Range and Accuracy","score":6.0,"note":"Còn lỗi hoà hợp chủ ngữ – động từ."}],"overall":6.5}"""
