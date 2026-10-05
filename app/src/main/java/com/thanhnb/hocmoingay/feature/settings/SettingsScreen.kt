package com.thanhnb.hocmoingay.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.thanhnb.hocmoingay.core.theme.ThemeMode
import com.thanhnb.hocmoingay.core.theme.ThemeStyle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepo) : ViewModel() {
    val settings = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private fun edit(f: (AppSettings) -> AppSettings) {
        viewModelScope.launch { repo.update(f) }
    }

    fun setTheme(t: ThemeStyle) = edit { it.withTheme(t) }
    fun setMode(m: ThemeMode) = edit { it.copy(mode = m) }
    fun setMinutes(m: Int) = edit { it.copy(dailyMinutes = m) }
    fun addTime(t: String) = edit { it.copy(reminders = addReminder(it.reminders, t)) }
    fun removeTime(t: String) = edit { it.copy(reminders = it.reminders - t) }
    fun setLanguage(l: String) = edit { it.copy(preferredLanguage = l) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    var addingTime by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } },
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Section("Kiểu giao diện")
            Segmented(
                listOf(ThemeStyle.TWO_TONE to "Hai sắc", ThemeStyle.WALLPAPER to "Hình nền", ThemeStyle.IDE to "IDE"),
                selected = s.theme, onSelect = vm::setTheme,
                enabled = { it != ThemeStyle.WALLPAPER || Build.VERSION.SDK_INT >= 31 },
            )
            if (Build.VERSION.SDK_INT < 31) Hint("Màu theo hình nền cần Android 12 trở lên.")

            Section("Sáng hay tối")
            Segmented(
                listOf(ThemeMode.SYSTEM to "Hệ thống", ThemeMode.LIGHT to "Sáng", ThemeMode.DARK to "Tối"),
                selected = s.mode, onSelect = vm::setMode,
            )

            Section("Thời lượng mỗi ngày")
            Segmented(listOf(10 to "10 phút", 20 to "20 phút", 30 to "30 phút"), selected = s.dailyMinutes, onSelect = vm::setMinutes)

            Section("Giờ nhắc học")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                s.reminders.forEach { t ->
                    InputChip(
                        selected = false, onClick = { vm.removeTime(t) }, label = { Text(t) },
                        trailingIcon = { Icon(Icons.Filled.Close, "Xoá giờ nhắc $t", Modifier.size(18.dp)) },
                    )
                }
                AssistChip(
                    onClick = { addingTime = true }, label = { Text("Thêm giờ") },
                    leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(18.dp)) },
                )
            }
            if (s.reminders.isEmpty()) Hint("Chưa có giờ nhắc nào, app sẽ không nhắc học.")

            Section("Ngôn ngữ lập trình ưa thích")
            Hint("Bài có nhiều ngôn ngữ sẽ mở sẵn ngôn ngữ này.")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LANGUAGES.forEach { (k, label) ->
                    FilterChip(selected = s.preferredLanguage == k, onClick = { vm.setLanguage(k) }, label = { Text(label) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (addingTime) TimeDialog(onDismiss = { addingTime = false }, onPick = { vm.addTime(it); addingTime = false })
}

@Composable
private fun Section(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun Hint(text: String) =
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, enabled: (T) -> Boolean = { true }) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (v, label) ->
            SegmentedButton(
                selected = v == selected, onClick = { onSelect(v) }, enabled = enabled(v),
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val st = rememberTimePickerState(initialHour = 20, initialMinute = 0, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick("%02d:%02d".format(st.hour, st.minute)) }) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Huỷ") } },
        text = { TimePicker(st) },
    )
}
