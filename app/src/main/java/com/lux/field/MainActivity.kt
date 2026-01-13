package com.lux.field

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.lux.field.ui.navigation.LuxNavGraph
import com.lux.field.ui.theme.LuxFieldTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LuxFieldTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LuxNavGraph()
                }
            }
        }
    }
}
