package com.seuxico.sxgestao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.seuxico.sxgestao.ui.theme.Surface
import com.seuxico.sxgestao.ui.navigation.SXGestaoApp
import com.seuxico.sxgestao.ui.theme.SXGestaoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Surface.toArgb()),
        )
        setContent { SXGestaoTheme { SXGestaoApp() } }
    }
}

