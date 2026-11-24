package com.seuxico.sxgestao

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuxico.sxgestao.model.TemaAplicativo
import com.seuxico.sxgestao.ui.components.AtualizacaoViewModel
import com.seuxico.sxgestao.ui.components.DialogoAtualizacao
import com.seuxico.sxgestao.ui.configuracoes.TemaViewModel
import com.seuxico.sxgestao.ui.navigation.SXGestaoApp
import com.seuxico.sxgestao.ui.theme.SXGestaoTheme
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm = ViewModelProvider(this)[TemaViewModel::class.java]
        val atualizacaoVm = ViewModelProvider(this)[AtualizacaoViewModel::class.java]
        if (savedInstanceState == null) {
            @Suppress("DEPRECATION")
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "0.0.0"
            atualizacaoVm.verificar(versionName)
        }
        setContent {
            val tema by vm.tema.collectAsStateWithLifecycle()
            val estadoAtualizacao by atualizacaoVm.estado.collectAsStateWithLifecycle()
            SXGestaoTheme(tema = tema) {
                val snackbarHostState = remember { SnackbarHostState() }
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
                LaunchedEffect(estadoAtualizacao) {
                    if (estadoAtualizacao == AtualizacaoViewModel.EstadoAtualizacao.Nenhuma) {
                        snackbarHostState.showSnackbar("Não foi possível encontrar atualizações disponíveis.", withDismissAction = false)
                    }
                }
                SnackbarHost(hostState = snackbarHostState)
                val info = when (val estado = estadoAtualizacao) {
                    is AtualizacaoViewModel.EstadoAtualizacao.Disponivel -> estado.info
                    is AtualizacaoViewModel.EstadoAtualizacao.Baixando -> atualizacaoVm.info
                    is AtualizacaoViewModel.EstadoAtualizacao.Erro -> estado.info
                    else -> null
                }
                if (estadoAtualizacao is AtualizacaoViewModel.EstadoAtualizacao.Instalando) {
                    val arquivo = (estadoAtualizacao as AtualizacaoViewModel.EstadoAtualizacao.Instalando).arquivo
                    androidx.compose.runtime.LaunchedEffect(arquivo) {
                        instalarApk(arquivo)
                        atualizacaoVm.dispensar()
                    }
                } else if (info != null) {
                    DialogoAtualizacao(
                        info = info,
                        estado = estadoAtualizacao,
                        onConfirmar = { atualizacaoVm.confirmarDownload(cacheDir) },
                        onDispensar = atualizacaoVm::dispensar,
                        onTentarNovamente = { atualizacaoVm.tentarNovamente(cacheDir) },
                    )
                }
            }
        }
    }

    private fun instalarApk(file: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, "Permita a instalação e abra a atualização novamente.", Toast.LENGTH_LONG).show()
            return
        }
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }
}
