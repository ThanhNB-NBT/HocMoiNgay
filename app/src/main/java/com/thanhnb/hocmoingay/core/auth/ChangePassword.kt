package com.thanhnb.hocmoingay.core.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Hộp đổi mật khẩu: mật khẩu cũ + mới 2 lần. Không lưu mật khẩu vào Bundle (remember, không rememberSaveable). */
@Composable
fun ChangePasswordDialog(change: suspend (old: String, new: String) -> Unit, onDone: (String?) -> Unit) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun submit() {
        error = newPasswordProblem(old, new, again)
        if (error != null) return
        busy = true
        scope.launch {
            runCatching { change(old, new) }
                .onSuccess { onDone("Đã đổi mật khẩu.") }
                .onFailure { error = passwordError(it); busy = false }
        }
    }
    val vt = if (show) VisualTransformation.None else PasswordVisualTransformation()
    val kb = KeyboardOptions(keyboardType = KeyboardType.Password)
    AlertDialog(
        onDismissRequest = { if (!busy) onDone(null) },
        title = { Text("Đổi mật khẩu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(old, { old = it }, Modifier.fillMaxWidth(), label = { Text("Mật khẩu hiện tại") }, singleLine = true, visualTransformation = vt, keyboardOptions = kb)
                OutlinedTextField(new, { new = it }, Modifier.fillMaxWidth(), label = { Text("Mật khẩu mới (≥ 8 ký tự)") }, singleLine = true, visualTransformation = vt, keyboardOptions = kb)
                OutlinedTextField(again, { again = it }, Modifier.fillMaxWidth(), label = { Text("Nhập lại mật khẩu mới") }, singleLine = true, visualTransformation = vt, keyboardOptions = kb)
                TextButton(onClick = { show = !show }) { Text(if (show) "Ẩn mật khẩu" else "Hiện mật khẩu") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            }
        },
        confirmButton = {
            TextButton(onClick = ::submit, enabled = !busy && old.isNotEmpty() && new.isNotEmpty()) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Đổi")
            }
        },
        dismissButton = { TextButton(onClick = { onDone(null) }, enabled = !busy) { Text("Huỷ") } },
    )
}
