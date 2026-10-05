package com.thanhnb.hocmoingay.core.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.BuildConfig
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.LocalTrack
import com.thanhnb.hocmoingay.core.theme.ProvideTrack
import com.thanhnb.hocmoingay.core.theme.Track
import com.thanhnb.hocmoingay.core.ui.PushButton
import com.thanhnb.hocmoingay.feature.TrackLabel
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val auth: Auth) : ViewModel() {
    data class Ui(val busy: Boolean = false, val error: String? = null)

    private val _ui = MutableStateFlow(Ui())
    val ui = _ui.asStateFlow()

    fun signIn(email: String, password: String) {
        if (_ui.value.busy) return
        _ui.value = Ui(busy = true)
        viewModelScope.launch {
            _ui.value = try {
                auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                Ui() // sessionStatus chuyển Authenticated, MainActivity tự đổi màn
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Ui(error = loginError(e))
            }
        }
    }
}

@Composable
fun LoginScreen(vm: LoginViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") } // không lưu mật khẩu vào Bundle
    var showPassword by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val ready = email.isNotBlank() && password.isNotEmpty()
    val submit = {
        if (ready && !ui.busy) {
            focus.clearFocus()
            vm.signIn(email, password)
        }
    }
    val field = RoundedCornerShape(16.dp)

    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 440.dp).verticalScroll(rememberScrollState()).padding(24.dp)) {
            TrackCollage()
            Spacer(Modifier.height(36.dp))
            Text("Học Mỗi Ngày", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Lập trình và tiếng Anh cho công việc, mỗi ngày 10–30 phút.",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, shape = field,
                leadingIcon = { Icon(Icons.Filled.Email, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Username },
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it }, label = { Text("Mật khẩu") }, singleLine = true, shape = field,
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Ẩn" else "Hiện") }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                // contentType: trình quản lý mật khẩu tự điền được
                modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.Password },
            )
            ui.error?.let {
                Spacer(Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = field,
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Warning, null, Modifier.size(20.dp))
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            PushButton(if (ui.busy) "Đang vào…" else "Bắt đầu học", submit, Modifier.fillMaxWidth(), enabled = ready && !ui.busy)
            // Chỉ bản debug có DEV_EMAIL (từ local.properties); release luôn rỗng nên nút không tồn tại
            if (BuildConfig.DEV_EMAIL.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { email = BuildConfig.DEV_EMAIL; password = BuildConfig.DEV_PASSWORD },
                    enabled = !ui.busy, modifier = Modifier.fillMaxWidth(),
                ) { Text("Điền tài khoản test (dev)") }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Đăng nhập một lần, sau đó học được cả khi offline.\nTài khoản do quản trị tạo, app không có đăng ký.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Hai thẻ chồng nhau nói luôn app dạy gì: một đoạn code và một câu tiếng Anh công việc. Chỉ để trang trí. */
@Composable
private fun TrackCollage() {
    Box(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
        ProvideTrack(Track.CODE) {
            val t = LocalTrack.current
            Surface(
                color = t.container, contentColor = t.onContainer, shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(0.82f).graphicsLayer { rotationZ = -3f },
            ) {
                Column(Modifier.padding(18.dp)) {
                    TrackLabel("Lập trình", t)
                    Spacer(Modifier.height(10.dp))
                    Text("for ngay in range(365):\n    hoc_mot_chut()", fontFamily = JetBrainsMono, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        ProvideTrack(Track.ENGLISH) {
            val t = LocalTrack.current
            Surface(
                color = t.container, contentColor = t.onContainer, shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 102.dp).fillMaxWidth(0.82f).graphicsLayer { rotationZ = 2f },
            ) {
                Column(Modifier.padding(18.dp)) {
                    TrackLabel("Tiếng Anh", t)
                    Spacer(Modifier.height(10.dp))
                    Text("“Could you walk me through it?”", style = MaterialTheme.typography.titleMedium)
                    Text("Bạn giải thích từng bước giúp mình nhé?", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
