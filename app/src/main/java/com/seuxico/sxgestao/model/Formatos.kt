package com.seuxico.sxgestao.model

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/** Conversão exata: rejeita expoentes, milhares, sinais e precisão excedida. */
fun centavos(texto: String): Long {
    require(Regex("[0-9]+([,.][0-9]{1,2})?").matches(texto)) { "Informe um valor com até duas casas decimais." }
    return BigDecimal(texto.replace(',', '.')).movePointRight(2).longValueExact()
}
fun decimal(valor: Long): String = BigDecimal.valueOf(valor, 2).toPlainString().replace('.', ',')
fun reais(valor: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))
    .format(BigDecimal.valueOf(valor, 2))
