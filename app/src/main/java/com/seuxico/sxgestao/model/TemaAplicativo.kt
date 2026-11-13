package com.seuxico.sxgestao.model

enum class TemaAplicativo {
    LARANJA, PRETO;

    companion object {
        fun doValorSalvo(valor: String?): TemaAplicativo =
            entries.firstOrNull { it.name == valor } ?: LARANJA
    }
}
