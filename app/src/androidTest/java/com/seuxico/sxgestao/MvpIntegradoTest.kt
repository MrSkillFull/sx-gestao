package com.seuxico.sxgestao

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.*
import com.seuxico.sxgestao.model.reais
import com.seuxico.sxgestao.ui.navigation.SXGestaoApp
import com.seuxico.sxgestao.ui.theme.SXGestaoTheme
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import org.junit.*
import org.junit.Assert.*

class MvpIntegradoTest {
    @get:Rule val compose = createComposeRule()
    @Test fun cadastroAtendimentoPainelComissaoCorrecaoECancelamentoPelaNavegacao() {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, GestaoDatabase::class.java).build()
        val repo = GestaoRepository(db)
        try {
            compose.setContent { SXGestaoTheme { SXGestaoApp(repo) } }
            fun aba(nome: String) { compose.onNode(isSelectable() and hasText(nome)).performClick() }
            fun clicar(texto: String) { compose.onNodeWithText(texto, substring = false).performScrollTo().performClick() }
            aba("Cadastros")
            compose.waitUntil(5000) { compose.onAllNodes(hasText("Novo barbeiro") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
            clicar("Novo barbeiro")
            compose.onNodeWithText("Nome").performTextInput("João")
            compose.onNodeWithText("Taxa de comissão").performTextReplacement("30,00")
            clicar("Salvar cadastro")
            compose.waitUntil(5000) { runBlocking { repo.barbeiros().first().isNotEmpty() } }
            clicar("Serviços"); clicar("Novo serviço")
            compose.onNodeWithText("Nome").performTextInput("Corte")
            compose.onNodeWithText("Preço").performTextInput("100,00")
            clicar("Salvar cadastro")
            compose.waitUntil(5000) { runBlocking { repo.servicos().first().isNotEmpty() } }
            aba("Início"); clicar("Novo atendimento")
            compose.waitUntil(5000) { compose.onAllNodesWithText("Cadastre um barbeiro ativo na aba Cadastros.").fetchSemanticsNodes().isEmpty() }
            clicar("Selecionar Barbeiro"); compose.onNodeWithText("João").performClick()
            compose.onNode(hasText("Adicionar serviço") and hasClickAction()).performScrollTo().performClick()
            compose.onNode(hasText("Corte ·", substring = true) and hasClickAction()).performClick()
            clicar("Concluir atendimento")
            compose.waitUntil(5000) { compose.onAllNodes(hasText("Parcela da barbearia: ${reais(7000)}")).fetchSemanticsNodes().isNotEmpty() }
            clicar("João")
            clicar("Confirmar pagamento")
            compose.waitUntil(5000) { runBlocking { repo.confirmacoes().first().isNotEmpty() } }
            clicar("Desfazer confirmação"); compose.onNodeWithText("Desfazer", substring = false).performClick()
            compose.waitUntil(5000) { runBlocking { repo.confirmacoes().first().isEmpty() } }
            clicar("Voltar"); aba("Atendimentos"); clicar("João"); clicar("Editar")
            compose.onNodeWithText("Desconto do item 1").performScrollTo().performTextReplacement("10,00")
            clicar("Salvar alterações")
            compose.waitUntil(5000) { runBlocking { repo.historico(LocalDate.MIN, LocalDate.MAX).first().single().itens.single().descontoCentavos == 1000L } }
            clicar("Cancelar atendimento"); compose.onNodeWithText("Confirmar cancelamento").performClick()
            compose.waitUntil(5000) { runBlocking { repo.historico(LocalDate.MIN, LocalDate.MAX).first().single().atendimento.cancelado } }
            clicar("Voltar"); aba("Início")
            compose.onNodeWithText("Parcela da barbearia: ${reais(0)}").performScrollTo().assertIsDisplayed()
            assertEquals(1, runBlocking { repo.historico(LocalDate.MIN, LocalDate.MAX).first().single().pagamentos.size })
        } finally { db.close() }
    }
}
