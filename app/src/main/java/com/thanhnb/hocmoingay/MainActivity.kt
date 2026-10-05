package com.thanhnb.hocmoingay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.thanhnb.hocmoingay.core.auth.AuthState
import com.thanhnb.hocmoingay.core.auth.LoginScreen
import com.thanhnb.hocmoingay.core.auth.LoginViewModel
import com.thanhnb.hocmoingay.core.theme.HocTheme
import com.thanhnb.hocmoingay.core.theme.ThemeStyle
import io.github.jan.supabase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val g = (application as HocApp).graph
        setContent {
            HocTheme(ThemeStyle.TWO_TONE, isSystemInDarkTheme()) {
                Surface(Modifier.fillMaxSize()) {
                    when (g.auth.state.collectAsStateWithLifecycle().value) {
                        AuthState.Loading -> Unit
                        AuthState.SignedOut -> LoginScreen(viewModel { LoginViewModel(g.supabase.auth) })
                        is AuthState.SignedIn -> AppNav()
                    }
                }
            }
        }
    }
}
