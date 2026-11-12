package com.seuxico.sxgestao

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.*
import com.seuxico.sxgestao.ui.atendimentos.*
import com.seuxico.sxgestao.ui.inicio.*
import com.seuxico.sxgestao.ui.theme.SXGestaoTheme
import java.time.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class ComissoesTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var db: GestaoDatabase
    private lateinit var repo: GestaoRepository
    private val b = Barbeiro(nome = "João", taxaComissaoBp = 3000)
    private val outro = Barbeiro(nome = "Pedro")
    private val servico = Servico(nome = "Corte", precoCentavos = 10000)
    private val segunda = LocalDate.parse("2024-01-01")
    private lateinit var id: String
    @Before fun abrir() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, GestaoDatabase::class.java).build()
        repo = GestaoRepository(db)
        repo.salvarBarbeiro(b); repo.salvarBarbeiro(outro); repo.salvarServico(servico)
        id = repo.registrar(b.id, LocalDateTime.parse("2024-01-03T12:00:00"), listOf(ItemEntrada(servico.id)), listOf(PagamentoEntrada(FormaPagamento.PIX, 10000)), ZoneOffset.UTC)
    }
    @After fun fechar() { db.close() }
    @Test fun confirmarDesfazerPreservaApuracaoERecebimentos() {
        val vm = PainelViewModel(repo)
        compose.setContent { SXGestaoTheme { ComissaoScreen(b.id, segunda.toEpochDay(), {}, vm) } }
        compose.waitUntil(5000) { !vm.state.value.carregando && vm.state.value.semana == segunda }
        assertEquals(7000L, vm.state.value.resumo.parcelaBarbearia)
        compose.onNodeWithText("Confirmar pagamento").performScrollTo().performClick()
        compose.waitUntil(5000) { vm.state.value.confirmacoes.isNotEmpty() }
        val original = runBlocking { repo.atendimento(id) }
        assertEquals(7000L, vm.state.value.resumo.parcelaBarbearia)
        compose.onNodeWithText("Desfazer confirmação").performScrollTo().performClick()
        compose.onAllNodesWithText("Voltar").onLast().performClick()
        assertEquals(1, runBlocking { repo.confirmacoes().first().size })
        compose.onNodeWithText("Desfazer confirmação").performScrollTo().performClick()
        compose.onNodeWithText("Desfazer", substring = false).performClick()
        compose.waitUntil(5000) { vm.state.value.confirmacoes.isEmpty() }
        compose.onNodeWithText("Pendente", substring = false).assertExists()
        assertEquals(original, runBlocking { repo.atendimento(id) })
        assertEquals(7000L, vm.state.value.resumo.parcelaBarbearia)
    }
    @Test fun bloqueiosOrigemDestinoERetornoPreservamRascunhoSemSalvar() {
        runBlocking { repo.confirmar(b.id, segunda); repo.confirmar(outro.id, segunda) }
        val vm = AtendimentoViewModel(repo, id)
        val comissoes = PainelViewModel(repo)
        var consulta by mutableStateOf<ParSemana?>(null)
        compose.setContent { SXGestaoTheme {
            val par = consulta
            if (par == null) AtendimentoFormScreen({}, vm, { barbeiro, semana -> consulta = ParSemana(barbeiro, semana) })
            else ComissaoScreen(par.barbeiroId, par.semana, { consulta = null }, comissoes)
        } }
        compose.waitUntil(5000) { !vm.state.value.carregando }
        compose.runOnIdle { vm.alterar(vm.state.value.form.copy(barbeiroId = outro.id)) }
        compose.onNodeWithText("Salvar alterações").performScrollTo().performClick()
        compose.waitUntil(5000) { !vm.state.value.gravando && vm.state.value.erro != null }
        assertEquals(2, vm.bloqueios().size)
        compose.onNodeWithText("Ver comissão · João · 01/01/2024").performScrollTo().performClick()
        compose.onNodeWithText("Desfazer confirmação").performScrollTo().performClick()
        compose.onNodeWithText("Desfazer", substring = false).performClick()
        compose.waitUntil(5000) { comissoes.state.value.confirmacoes.size == 1 }
        compose.onNodeWithText("Voltar", substring = false).performScrollTo().performClick()
        compose.waitUntil(5000) { vm.bloqueios().size == 1 }
        assertEquals(outro.id, vm.state.value.form.barbeiroId)
        assertFalse(vm.state.value.salvo)
        assertEquals(b.id, runBlocking { repo.atendimento(id)!!.atendimento.barbeiroId })
        compose.onNodeWithText("Ver comissão · Pedro · 01/01/2024").performScrollTo().performClick()
        compose.onNodeWithText("Desfazer confirmação").performScrollTo().performClick()
        compose.onNodeWithText("Desfazer", substring = false).performClick()
        compose.waitUntil(5000) { comissoes.state.value.confirmacoes.isEmpty() }
        compose.onNodeWithText("Voltar", substring = false).performScrollTo().performClick()
        compose.waitUntil(5000) { vm.bloqueios().isEmpty() }
        assertFalse(vm.state.value.salvo)
        compose.onNodeWithText("Salvar alterações").performScrollTo().performClick()
        compose.waitUntil(5000) { vm.state.value.salvo }
        assertEquals(outro.id, runBlocking { repo.atendimento(id)!!.atendimento.barbeiroId })
    }
}
