package com.seuxico.sxgestao.ui.configuracoes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.seuxico.sxgestao.data.repository.TemaRepository
import com.seuxico.sxgestao.model.TemaAplicativo

class TemaViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TemaRepository(application)
    val tema = repository.tema

    fun selecionar(tema: TemaAplicativo) = repository.selecionar(tema)
}
