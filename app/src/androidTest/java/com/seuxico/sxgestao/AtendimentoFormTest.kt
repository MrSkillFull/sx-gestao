package com.seuxico.sxgestao

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.*
import com.seuxico.sxgestao.ui.atendimentos.*
import com.seuxico.sxgestao.ui.theme.SXGestaoTheme
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class AtendimentoFormTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: GestaoDatabase
    private lateinit var repo: GestaoRepository
    private lateinit var vm: AtendimentoViewModel
    @Before fun abrir() {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, GestaoDatabase::class.java).build()
        repo = GestaoRepository(db)
        runBlocking { repo.salvarBarbeiro(Barbeiro(nome = "João")); repo.salvarServico(Servico(nome = "Corte", precoCentavos = 3000)) }
        vm = AtendimentoViewModel(repo)
    }
    @After fun fechar() { db.close() }
    @Test fun registrarRepeticaoDescontoEPagamento() {
        compose.setContent { SXGestaoTheme { AtendimentoFormScreen({}, vm) } }
        compose.onNodeWithText("Selecionar Barbeiro").performScrollTo().performClick()
        compose.onNodeWithText("João").performClick()
        repeat(2) {
            compose.onNode(hasText("Adicionar serviço") and hasClickAction()).performScrollTo().performClick()
            compose.onNode(hasText("Corte ·", substring = true) and hasClickAction()).performClick()
        }
        compose.onNodeWithText("Desconto do item 1").performScrollTo().performTextReplacement("5,00")
        compose.onNodeWithText("Desconto do item 2").performScrollTo().performTextReplacement("30,00")
        compose.onNodeWithText("Dividir pagamento").performScrollTo().performClick()
        compose.onNodeWithText("Valor da parte 1").performScrollTo().performTextReplacement("10,00")
        compose.onNodeWithText("Adicionar parte").performScrollTo().performClick()
        compose.onNodeWithText("Valor da parte 2").performScrollTo().performTextReplacement("15,00")
        compose.onNodeWithText("Concluir atendimento").performScrollTo().performClick()
        compose.waitUntil(5000) { vm.state.value.salvo }
        val a = runBlocking { repo.historico(LocalDate.now().minusDays(1), LocalDate.now().plusDays(1)).first().single() }
        assertEquals(2, a.itens.size)
        assertEquals(2500L, a.pagamentos.sumOf { it.valorCentavos })
    }
    @Test fun rascunhoPreservadoAoRecusarDescarte() {
        var saiu = false
        compose.setContent { SXGestaoTheme { AtendimentoFormScreen({ saiu = true }, vm) } }
        compose.onNodeWithText("Data efetiva").performScrollTo().performTextReplacement("31/02/2028")
        compose.onNodeWithText("Voltar").performScrollTo().performClick()
        compose.onNodeWithText("Continuar editando").performClick()
        compose.onNodeWithText("Data efetiva").assertTextContains("31/02/2028")
        assertFalse(saiu)
        compose.onNodeWithText("Voltar").performScrollTo().performClick()
        compose.onNodeWithText("Descartar alterações", substring = false).performClick()
        assertTrue(saiu)
    }
    @Test fun editarMantemSnapshotsEVinculosInativos() {
        val dados = runBlocking {
            val b = repo.barbeiros().first().single()
            val s = repo.servicos().first().single()
            val id = repo.registrar(b.id, java.time.LocalDateTime.parse("2024-01-03T12:00:00"),
                listOf(ItemEntrada(s.id, 500)), listOf(PagamentoEntrada(FormaPagamento.PIX, 2500)), java.time.ZoneOffset.UTC)
            repo.salvarBarbeiro(b.copy(ativo = false, taxaComissaoBp = 10000))
            repo.salvarServico(s.copy(ativo = false, precoCentavos = 9000))
            id to repo.atendimento(id)!!
        }
        val editar = AtendimentoViewModel(repo, dados.first)
        compose.setContent { SXGestaoTheme { AtendimentoFormScreen({}, editar) } }
        compose.waitUntil(5000) { !editar.state.value.carregando }
        compose.onNodeWithText("João (Inativo)").assertExists()
        compose.onNodeWithText("Desconto do item 1").performScrollTo().performTextReplacement("10,00")
        compose.onNodeWithText("Salvar alterações").performScrollTo().performClick()
        compose.waitUntil(5000) { editar.state.value.salvo }
        val depois = runBlocking { repo.atendimento(dados.first)!! }
        assertEquals(dados.second.atendimento, depois.atendimento)
        assertEquals(dados.second.itens.single().id, depois.itens.single().id)
        assertEquals(3000L, depois.itens.single().precoAplicadoCentavos)
        assertEquals(1000L, depois.itens.single().descontoCentavos)
        assertEquals(2000L, depois.pagamentos.single().valorCentavos)
        assertEquals(dados.second.pagamentos.single().id, depois.pagamentos.single().id)
    }
    @Test fun cancelamentoExigeConfirmacaoEPreservaHistorico() {
        val id = runBlocking {
            val b = repo.barbeiros().first().single()
            val s = repo.servicos().first().single()
            repo.registrar(b.id, java.time.LocalDateTime.parse("2024-01-03T12:00:00"),
                listOf(ItemEntrada(s.id)), listOf(PagamentoEntrada(FormaPagamento.PIX, 3000)), java.time.ZoneOffset.UTC)
        }
        val historico = HistoricoViewModel(repo)
        compose.setContent { SXGestaoTheme { AtendimentoDetalheScreen(id, {}, {}, historico) } }
        compose.waitUntil(5000) { !historico.state.value.carregando }
        compose.onNodeWithText("Cancelar atendimento", substring = false).performScrollTo().performClick()
        compose.onAllNodesWithText("Voltar").onLast().performClick()
        assertFalse(runBlocking { repo.atendimento(id)!!.atendimento.cancelado })
        compose.onNodeWithText("Cancelar atendimento", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Confirmar cancelamento").performClick()
        compose.waitUntil(5000) { historico.state.value.registros.single().atendimento.cancelado }
        compose.onNodeWithText("Cancelado · somente leitura").performScrollTo().assertIsDisplayed()
        assertEquals(1, runBlocking { repo.atendimento(id)!!.pagamentos.size })
    }
}
