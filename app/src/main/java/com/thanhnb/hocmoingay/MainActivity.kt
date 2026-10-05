package com.thanhnb.hocmoingay

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thanhnb.hocmoingay.core.auth.AuthState
import com.thanhnb.hocmoingay.core.auth.LoginScreen
import com.thanhnb.hocmoingay.core.auth.LoginViewModel
import com.thanhnb.hocmoingay.core.theme.HocTheme
import com.thanhnb.hocmoingay.core.theme.isDark
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val g = (application as HocApp).graph
        g.scheduler.periodic()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Mỗi lần vào foreground: sync ngay + nghe realtime tới khi app xuống nền
                g.auth.state.collectLatest { s ->
                    if (s is AuthState.SignedIn) {
                        g.scheduler.now()
                        g.realtime.run(s.userId)
                    }
                }
            }
        }
        setContent {
            // null = Room chưa đọc xong: chưa vẽ gì, tránh nháy theme mặc định trước theme thật
            val settings = g.settings.settings.collectAsStateWithLifecycle(initialValue = null).value ?: return@setContent
            val auth = g.auth.state.collectAsStateWithLifecycle().value
            val dark = isDark(settings.mode, isSystemInDarkTheme())
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                )
                onDispose {}
            }
            HocTheme(settings.theme, dark) {
                Surface(Modifier.fillMaxSize()) {
                    when (auth) {
                        AuthState.Loading -> Unit
                        AuthState.SignedOut -> LoginScreen(viewModel { LoginViewModel(g.supabase.auth) })
                        is AuthState.SignedIn -> AppNav(g)
                    }
                }
            }
        }
    }
}
