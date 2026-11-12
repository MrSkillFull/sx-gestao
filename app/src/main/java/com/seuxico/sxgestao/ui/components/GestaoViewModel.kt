package com.seuxico.sxgestao.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seuxico.sxgestao.data.local.GestaoDatabase
import com.seuxico.sxgestao.data.repository.GestaoRepository

val LocalGestaoRepository = staticCompositionLocalOf<GestaoRepository?> { null }

@Composable
inline fun <reified T : ViewModel> gestaoViewModel(crossinline criar: (GestaoRepository) -> T): T {
    val context = LocalContext.current.applicationContext
    val repository = LocalGestaoRepository.current ?: GestaoRepository(GestaoDatabase.getInstance(context))
    return viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V =
            criar(repository) as V
    })
}
