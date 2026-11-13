package com.seuxico.sxgestao.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import com.seuxico.sxgestao.ui.components.LocalGestaoRepository
import com.seuxico.sxgestao.data.repository.GestaoRepository
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seuxico.sxgestao.data.repository.EstadoBase
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.seuxico.sxgestao.R
import com.seuxico.sxgestao.model.TemaAplicativo
import com.seuxico.sxgestao.ui.atendimentos.AtendimentosScreen
import com.seuxico.sxgestao.ui.atendimentos.AtendimentoFormScreen
import com.seuxico.sxgestao.ui.atendimentos.AtendimentoViewModel
import com.seuxico.sxgestao.ui.atendimentos.AtendimentoDetalheScreen
import com.seuxico.sxgestao.ui.components.gestaoViewModel
import com.seuxico.sxgestao.ui.cadastros.CadastrosScreen
import com.seuxico.sxgestao.ui.configuracoes.ConfiguracoesScreen
import com.seuxico.sxgestao.ui.inicio.InicioScreen
import com.seuxico.sxgestao.ui.inicio.ComissaoScreen

private enum class Destination(
    val route: String,
    @StringRes val label: Int,
    @DrawableRes val icon: Int,
) {
    Inicio("inicio", R.string.inicio, R.drawable.ic_home),
    Atendimentos("atendimentos", R.string.atendimentos, R.drawable.ic_receipt),
    Cadastros("cadastros", R.string.cadastros, R.drawable.ic_people),
    Configuracoes("configuracoes", R.string.configuracoes, R.drawable.ic_settings),
}

@Composable
fun SXGestaoApp(
    repository: GestaoRepository? = null,
    tema: TemaAplicativo = TemaAplicativo.LARANJA,
    selecionarTema: (TemaAplicativo) -> Unit = {},
) {
    CompositionLocalProvider(LocalGestaoRepository provides repository) { AppContent(tema, selecionarTema) }
}

@Composable
private fun AppContent(tema: TemaAplicativo, selecionarTema: (TemaAplicativo) -> Unit) {
    val geracao by EstadoBase.geracao.collectAsStateWithLifecycle()
    val restaurando by EstadoBase.restaurando.collectAsStateWithLifecycle()
    var ultimaGeracao by rememberSaveable { mutableLongStateOf(geracao) }
    val context = LocalContext.current
    LaunchedEffect(geracao) {
        if (geracao != ultimaGeracao) {
            ultimaGeracao = geracao
            android.widget.Toast.makeText(context, "Backup restaurado com sucesso.", android.widget.Toast.LENGTH_LONG).show()
        }
    }
    key(geracao) { Navegacao(tema, selecionarTema) }
    if (restaurando) Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Surface { Column(Modifier.padding(24.dp)) { CircularProgressIndicator(); Text("Restaurando backup…") } }
    }
}

@Composable
private fun Navegacao(tema: TemaAplicativo, selecionarTema: (TemaAplicativo) -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val abrirComissao: (String, Long) -> Unit = { barbeiro, semana -> navController.navigate("comissao/$barbeiro/$semana") }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (Destination.entries.any { it.route == backStackEntry?.destination?.route }) {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                        selected = backStackEntry?.destination?.route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(painterResource(destination.icon), contentDescription = null)
                        },
                        label = { Text(stringResource(destination.label)) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Inicio.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Inicio.route) { InicioScreen(novo = { navController.navigate("atendimento/novo") }, comissao = abrirComissao) }
            composable(Destination.Atendimentos.route) { AtendimentosScreen(novo = { navController.navigate("atendimento/novo") }, detalhe = { navController.navigate("atendimento/$it") }) }
            composable("atendimento/novo") { AtendimentoFormScreen(voltar = { navController.popBackStack() }, comissao = abrirComissao) }
            composable("atendimento/{id}") { entry ->
                val id = requireNotNull(entry.arguments?.getString("id"))
                AtendimentoDetalheScreen(id, voltar = { navController.popBackStack() }, editar = { navController.navigate("atendimento/$id/editar") }, comissao = abrirComissao)
            }
            composable("atendimento/{id}/editar") { entry ->
                val id = requireNotNull(entry.arguments?.getString("id"))
                val vm: AtendimentoViewModel = gestaoViewModel { AtendimentoViewModel(it, id) }
                AtendimentoFormScreen(voltar = { navController.popBackStack() }, vm = vm, comissao = abrirComissao)
            }
            composable("comissao/{barbeiro}/{semana}") { entry ->
                ComissaoScreen(requireNotNull(entry.arguments?.getString("barbeiro")), requireNotNull(entry.arguments?.getString("semana")).toLong(), voltar = { navController.popBackStack() })
            }
            composable(Destination.Cadastros.route) { CadastrosScreen() }
            composable(Destination.Configuracoes.route) { ConfiguracoesScreen(tema = tema, selecionarTema = selecionarTema) }
        }
    }
}

