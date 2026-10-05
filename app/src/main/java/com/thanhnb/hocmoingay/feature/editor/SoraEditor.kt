package com.thanhnb.hocmoingay.feature.editor

import android.content.Context
import android.widget.HorizontalScrollView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import com.thanhnb.hocmoingay.R
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.SymbolInputView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IThemeSource

object Sora {
    @Volatile private var ready = false

    /** Nạp grammar một lần (đọc khoảng 1,5 MB JSON). Gọi trên Dispatchers.IO trước khi tạo editor đầu tiên. */
    fun init(ctx: Context) = synchronized(this) {
        if (ready) return
        FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(ctx.applicationContext.assets))
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
        ready = true
    }

    fun useTheme(json: String, dark: Boolean) = synchronized(this) {
        val name = "hum-" + json.hashCode()
        val reg = ThemeRegistry.getInstance()
        reg.loadTheme(ThemeModel(IThemeSource.fromInputStream(json.byteInputStream(), "$name.json", null), name).apply { isDark = dark })
        reg.setTheme(name)
    }
}

@Composable
fun SoraEditor(initial: String, lang: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { Sora.init(ctx) }; ready = true }
    if (!ready) return Box(modifier)
    val colors = editorColors()
    val theme = remember(colors) { tmTheme(colors) }
    val change by rememberUpdatedState(onChange)
    Column(modifier) {
        // đổi ngôn ngữ hoặc theme thì dựng lại editor; nội dung lấy từ `initial` (bản nháp mới nhất)
        key(lang, theme) {
            val editor = remember {
                Sora.useTheme(theme, colors.dark)
                CodeEditor(ctx).apply {
                    val mono = ResourcesCompat.getFont(ctx, R.font.jetbrains_mono)
                    typefaceText = mono
                    typefaceLineNumber = mono
                    setTextSize(15f)
                    tabWidth = 4
                    isWordwrap = false
                    colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
                    setEditorLanguage(TextMateLanguage.create(scopeOf(lang), true))
                    setText(initial)
                    subscribeAlways(ContentChangeEvent::class.java) { change(text.toString()) }
                }
            }
            DisposableEffect(editor) { onDispose { editor.release() } }
            AndroidView({ editor }, Modifier.weight(1f).fillMaxWidth())
            AndroidView(
                { c ->
                    HorizontalScrollView(c).apply {
                        setBackgroundColor(colors.bar.toArgb())
                        addView(SymbolInputView(c).apply {
                            bindEditor(editor)
                            textColor = colors.fg.toArgb()
                            val s = symbolsFor(lang)
                            addSymbols(s.map { it.first }.toTypedArray(), s.map { it.second }.toTypedArray())
                        })
                    }
                },
                Modifier.fillMaxWidth().height(44.dp),
            )
        }
    }
}
