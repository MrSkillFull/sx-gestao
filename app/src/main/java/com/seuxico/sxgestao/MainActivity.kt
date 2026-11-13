package com.seuxico.sxgestao

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuxico.sxgestao.model.TemaAplicativo
import com.seuxico.sxgestao.ui.configuracoes.TemaViewModel
import com.seuxico.sxgestao.ui.navigation.SXGestaoApp
import com.seuxico.sxgestao.ui.theme.SXGestaoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this)[TemaViewModel::class.java]
        setContent {
            val tema by vm.tema.collectAsStateWithLifecycle()
            SXGestaoTheme(tema = tema) {
                val surface = MaterialTheme.colorScheme.surface.toArgb()
                val background = MaterialTheme.colorScheme.background.toArgb()
                SideEffect {
                    val escuro = tema == TemaAplicativo.PRETO
                    enableEdgeToEdge(
                        statusBarStyle = if (escuro) SystemBarStyle.dark(Color.TRANSPARENT)
                            else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                        navigationBarStyle = if (escuro) SystemBarStyle.dark(surface)
                            else SystemBarStyle.light(surface, Color.rgb(23, 18, 15)),
                    )
                    window.setBackgroundDrawableResource(android.R.color.transparent)
                    window.decorView.setBackgroundColor(background)
                }
                SXGestaoApp(tema = tema, selecionarTema = vm::selecionar)
            }
        }
    }
}
