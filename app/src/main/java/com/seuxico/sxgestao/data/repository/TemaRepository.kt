package com.seuxico.sxgestao.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.seuxico.sxgestao.model.TemaAplicativo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Preferência local do aparelho, independente dos registros e do backup da barbearia. */
class TemaRepository(private val preferencias: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences("aparencia", Context.MODE_PRIVATE)
    )

    private val mutableTema = MutableStateFlow(
        TemaAplicativo.doValorSalvo(preferencias.getString("tema", null))
    )
    val tema = mutableTema.asStateFlow()

    fun selecionar(tema: TemaAplicativo) {
        preferencias.edit().putString("tema", tema.name).apply()
        mutableTema.value = tema
    }
}
