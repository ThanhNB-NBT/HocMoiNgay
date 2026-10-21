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
        val track = if (name == "can") "english" else "code"
        val json = assets.open("showcase/$name.json").bufferedReader().readText()
        val repo = LessonRepo(
            lesson = { LessonEntity(it, "showcase", body = json) }, track = { track },
            getProgress = { null }, putProgress = {}, cardsByIds = { emptyList() }, putCards = {},
            tx = { it() }, userId = { null }, afterWrite = {},
        )
        val vm = PlayerViewModel(name, repo, CodeApi { _, _ -> "{}" }, MutableStateFlow(false), flowOf(null), MainScope())
        setContent {
            HocTheme(ThemeStyle.TWO_TONE, dark) {
                Surface(Modifier.fillMaxSize()) {
                    PlayerScreen(vm, name, onBack = ::finish, onOpenEditor = {})
                }
            }
        }
    }
}
