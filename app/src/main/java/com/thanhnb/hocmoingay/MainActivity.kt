package com.thanhnb.hocmoingay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thanhnb.hocmoingay.core.theme.HocTheme
import com.thanhnb.hocmoingay.core.theme.JetBrainsMono
import com.thanhnb.hocmoingay.core.theme.ThemeStyle

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HocTheme(ThemeStyle.TWO_TONE, isSystemInDarkTheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Học Mỗi Ngày", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Dấu: ắ ằ ẳ ẵ ặ ể ễ ộ ợ ữ Ỹ", style = MaterialTheme.typography.bodyLarge)
                        Text("fun main() = println(\"Xin chào\")", fontFamily = JetBrainsMono)
                    }
                }
            }
        }
    }
}
