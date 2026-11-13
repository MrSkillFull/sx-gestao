package com.seuxico.sxgestao

import android.content.Context
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.seuxico.sxgestao.data.repository.TemaRepository
import com.seuxico.sxgestao.model.TemaAplicativo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TemaTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun trocaOsDoisTemasEMantemEscolhaAoRecriar() {
        val repository = TemaRepository(compose.activity)
        val original = repository.tema.value
        try {
            compose.onNode(isSelectable() and hasText("Configurações")).performClick()
            listOf(
                "Preto e laranja" to TemaAplicativo.PRETO,
                "Laranja e preto" to TemaAplicativo.LARANJA,
            ).forEach { (titulo, esperado) ->
                compose.onNodeWithText(titulo).performClick().assertIsSelected()
                compose.waitForIdle()
                assertEquals(esperado, TemaRepository(compose.activity).tema.value)
                compose.activityRule.scenario.recreate()
                compose.waitForIdle()
                compose.onNodeWithText(titulo).assertIsSelected()
            }
        } finally {
            TemaRepository(compose.activity).selecionar(original)
        }
    }

    @Test
    fun preferenciaAusenteOuDesconhecidaUsaLaranja() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("tema_teste", Context.MODE_PRIVATE)
        try {
            prefs.edit().clear().commit()
            assertEquals(TemaAplicativo.LARANJA, TemaRepository(prefs).tema.value)
            prefs.edit().putString("tema", "desconhecido").commit()
            assertEquals(TemaAplicativo.LARANJA, TemaRepository(prefs).tema.value)
        } finally {
            prefs.edit().clear().commit()
        }
    }
}
