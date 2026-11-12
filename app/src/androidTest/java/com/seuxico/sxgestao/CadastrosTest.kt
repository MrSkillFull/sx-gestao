package com.seuxico.sxgestao

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.seuxico.sxgestao.data.local.GestaoDatabase
import com.seuxico.sxgestao.data.repository.GestaoRepository
import com.seuxico.sxgestao.ui.cadastros.*
import com.seuxico.sxgestao.ui.theme.SXGestaoTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

class CadastrosTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: GestaoDatabase
    private lateinit var repo: GestaoRepository
    @Before fun abrir() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, GestaoDatabase::class.java).build()
        repo = GestaoRepository(db)
    }
    @After fun fechar() { db.close() }

    @Test fun criarValidarEditarEInativar() {
        // Injeta base isolada: nenhuma gravação no banco do operador.
        val vm = CadastrosViewModel(repo)
        compose.setContent { SXGestaoTheme { CadastrosScreen(vm) } }
        compose.onNodeWithText("Novo barbeiro").performClick()
        compose.onNodeWithText("Nome").performTextInput("João")
        compose.onNodeWithText("Taxa de comissão").performTextReplacement("33,333")
        compose.onNodeWithText("Salvar cadastro").performScrollTo().performClick()
        compose.onNodeWithText("Informe de 0 a 100%, com até duas casas.").assertIsDisplayed()
        assertTrue(runBlocking { repo.barbeiros().first() }.isEmpty())
        compose.onNodeWithText("Taxa de comissão").performTextReplacement("33,33")
        compose.onNodeWithText("Salvar cadastro").performScrollTo().performClick()
        compose.waitUntil(5000) { vm.state.value.form == null }
        compose.onNodeWithText("João").performScrollTo().performClick()
        compose.onNodeWithText("Nome").performTextReplacement("João Silva")
        compose.onNodeWithText("Salvar cadastro").performScrollTo().performClick()
        compose.waitUntil(5000) { vm.state.value.form == null }
        compose.onNodeWithText("João Silva").performScrollTo().performClick()
        compose.onNodeWithText("Inativar cadastro").performScrollTo().performClick()
        compose.onNodeWithText("Inativar", useUnmergedTree = true).performClick()
        compose.waitUntil(5000) { vm.state.value.form == null }
        val b = runBlocking { repo.barbeiros().first().single() }
        assertFalse(b.ativo)
        assertEquals("João Silva", b.nome)
        assertEquals(3333, b.taxaComissaoBp)
        compose.onNodeWithText("Serviços").performScrollTo().performClick()
        compose.onNodeWithText("Novo serviço").performClick()
        compose.onNodeWithText("Nome").performTextInput("Corte")
        compose.onNodeWithText("Preço").performTextInput("12,30")
        compose.onNodeWithText("Salvar cadastro").performScrollTo().performClick()
        compose.waitUntil(5000) { vm.state.value.form == null }
        assertEquals(1230L, runBlocking { repo.servicos().first().single().precoCentavos })
    }
    @Test fun focoNoPrimeiroErroComFonteAmpliadaETaxaDoProprietario() {
        val vm = CadastrosViewModel(repo)
        compose.setContent {
            val densidade = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(densidade, 2f)) { SXGestaoTheme { CadastrosScreen(vm) } }
        }
        compose.waitUntil(5000) { !vm.state.value.carregando }
        compose.onNodeWithText("Novo barbeiro").performScrollTo().performClick()
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Salvar cadastro"))
        compose.onNodeWithText("Salvar cadastro").performClick()
        compose.onNodeWithText("Nome").assertIsFocused()
        compose.onNodeWithText("Nome").performTextInput("Dono")
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(isToggleable())
        compose.onNode(isToggleable()).assertContentDescriptionEquals("Barbeiro proprietário, taxa fixa em zero por cento")
        compose.onNode(isToggleable()).performClick()
        assertEquals("0,00", vm.state.value.form!!.valor)
        compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Salvar cadastro"))
        compose.onNodeWithText("Salvar cadastro").performClick()
        compose.waitUntil(5000) { vm.state.value.form == null }
        val dono = runBlocking { repo.barbeiros().first().single() }
        assertTrue(dono.proprietario)
        assertEquals(0, dono.taxaComissaoBp)
    }
}
