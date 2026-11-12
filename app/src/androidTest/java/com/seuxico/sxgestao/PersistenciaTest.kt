package com.seuxico.sxgestao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.time.*

class PersistenciaTest {
    private lateinit var db: GestaoDatabase
    private lateinit var repo: GestaoRepository
    private val segunda = LocalDate.parse("2024-01-01")
    private val local = LocalDateTime.parse("2024-01-03T12:00:00")
    private lateinit var barbeiro: Barbeiro
    private lateinit var servico: Servico
    @Before fun abrir() = runBlocking {
        db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, GestaoDatabase::class.java).build()
        repo = GestaoRepository(db, Clock.fixed(Instant.parse("2024-01-08T12:00:00Z"), ZoneOffset.UTC))
        barbeiro = Barbeiro(nome = "Barbeiro")
        servico = Servico(nome = "Corte", precoCentavos = 3000)
        repo.salvarBarbeiro(barbeiro)
        repo.salvarServico(servico)
    }
    @After fun fechar() { db.close() }
    private suspend fun registrar(data: LocalDateTime = local, b: Barbeiro = barbeiro) = repo.registrar(b.id, data,
        listOf(ItemEntrada(servico.id, 500), ItemEntrada(servico.id, 3000)),
        listOf(PagamentoEntrada(FormaPagamento.PIX, 1000), PagamentoEntrada(FormaPagamento.DEBITO, 1500)), ZoneId.of("America/Sao_Paulo"))
    private suspend fun falha(acao: suspend () -> Unit) {
        try { acao(); fail("Operação deveria ser rejeitada") } catch (e: IllegalArgumentException) { } catch (e: IllegalStateException) { }
    }
    @Test fun snapshotsInativosIdsQuitacaoECancelamento() = runBlocking {
        val id = registrar()
        val original = repo.atendimento(id)!!
        repo.salvarServico(servico.copy(precoCentavos = 9000, ativo = false))
        repo.salvarBarbeiro(barbeiro.copy(taxaComissaoBp = 10000, ativo = false))
        val itens = original.itens.map { ItemEntrada(it.servicoId, it.descontoCentavos, it.id) }
        val pagamentos = original.pagamentos.map { PagamentoEntrada(FormaPagamento.CREDITO, it.valorCentavos, it.id) }
        repo.editar(id, barbeiro.id, itens, pagamentos)
        val editado = repo.atendimento(id)!!
        assertEquals(original.itens.toSet(), editado.itens.toSet())
        assertEquals(original.atendimento, editado.atendimento)
        assertEquals(original.pagamentos.map { it.id }.toSet(), editado.pagamentos.map { it.id }.toSet())
        falha { repo.editar(id, barbeiro.id, itens, listOf(PagamentoEntrada(FormaPagamento.PIX, 1))) }
        assertEquals(editado, repo.atendimento(id))
        assertEquals(1250L, repo.apurar(barbeiro.id, segunda))
        val resumo = repo.faturamento(segunda, segunda.plusDays(7)).first()
        assertEquals(2500L, resumo.liquidoCentavos)
        assertEquals(2500L, resumo.recebimentos[FormaPagamento.CREDITO])
        falha { repo.cancelar(id, false) }
        repo.cancelar(id, true)
        assertTrue(repo.atendimento(id)!!.atendimento.cancelado)
        assertEquals(2, repo.atendimento(id)!!.itens.size)
        assertEquals(0L, repo.apurar(barbeiro.id, segunda))
        assertEquals(0L, repo.faturamento(segunda, segunda.plusDays(7)).first().liquidoCentavos)
    }
    @Test fun confirmacaoConcorrenteBloqueiosEDesfazimento() = runBlocking {
        val id = registrar()
        val original = repo.atendimento(id)!!
        val confirmacoes = coroutineScope { (1..8).map { async(Dispatchers.Default) { repo.confirmar(barbeiro.id, segunda) } }.awaitAll() }
        assertEquals(1, confirmacoes.toSet().size)
        assertEquals(1250L, confirmacoes.first().valorConfirmadoCentavos)
        assertEquals(1, repo.confirmacoes().first().size)
        falha { registrar() }
        falha { repo.cancelar(id, true) }
        falha { repo.editar(id, barbeiro.id, original.itens.map { ItemEntrada(it.servicoId, it.descontoCentavos, it.id) }, original.pagamentos.map { PagamentoEntrada(FormaPagamento.DINHEIRO, it.valorCentavos, it.id) }) }
        assertEquals(original, repo.atendimento(id))
        falha { repo.desfazerConfirmacao(barbeiro.id, segunda, false) }
        repo.desfazerConfirmacao(barbeiro.id, segunda, true)
        assertEquals(original, repo.atendimento(id))
        repo.cancelar(id, true)
        val posterior = GestaoRepository(db, Clock.fixed(Instant.parse("2024-01-09T12:00:00Z"), ZoneOffset.UTC)).confirmar(barbeiro.id, segunda)
        assertEquals(0L, posterior.valorConfirmadoCentavos)
        assertTrue(posterior.confirmadoEmUtcMs > confirmacoes.first().confirmadoEmUtcMs)
    }
    @Test fun bloqueioOrigemDestinoMesmoComComissaoZero() = runBlocking {
        val dono = Barbeiro(nome = "Dono", proprietario = true)
        repo.salvarBarbeiro(dono)
        val id = registrar(b = dono)
        val a = repo.atendimento(id)!!
        val itens = a.itens.map { ItemEntrada(it.servicoId, it.descontoCentavos, it.id) }
        val partes = a.pagamentos.map { PagamentoEntrada(FormaPagamento.PIX, it.valorCentavos, it.id) }
        repo.confirmar(barbeiro.id, segunda.plusWeeks(1))
        falha { repo.editar(id, barbeiro.id, itens, partes, local.plusWeeks(1)) }
        assertEquals(a, repo.atendimento(id))
        repo.confirmar(dono.id, segunda)
        falha { repo.editar(id, barbeiro.id, itens, partes, local.plusWeeks(2)) }
        falha { repo.cancelar(id, true) }
        falha { registrar(b = dono) }
        assertEquals(a, repo.atendimento(id))
    }
    @Test fun falhaNoMeioDaEdicaoReverteTudo() = runBlocking {
        val primeiro = registrar()
        val segundo = registrar()
        val antes = repo.atendimento(primeiro)!!
        val colisao = repo.atendimento(segundo)!!.pagamentos.first().id
        try {
            repo.editar(primeiro, barbeiro.id, antes.itens.map { ItemEntrada(it.servicoId, it.descontoCentavos, it.id) }, listOf(PagamentoEntrada(FormaPagamento.PIX, 2500, colisao)))
            fail("PK duplicada deveria abortar")
        } catch (e: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(antes, repo.atendimento(primeiro))
    }
    @Test fun integridadeReferencialNulabilidadeEChaves() = runBlocking {
        val sqlite = db.openHelper.writableDatabase
        sqlite.query("PRAGMA foreign_keys").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        val id = registrar()
        listOf(
            "DELETE FROM Barbeiro WHERE id = '${barbeiro.id}'",
            "UPDATE Servico SET id = '${novoId()}' WHERE id = '${servico.id}'",
            "INSERT INTO Servico(id,nome,ativo,preco_centavos) VALUES('${novoId()}',NULL,1,0)",
            "INSERT INTO Pagamento(id,atendimento_id,forma,valor_centavos) VALUES('${novoId()}','${novoId()}','PIX',1)",
            "INSERT INTO Barbeiro SELECT * FROM Barbeiro",
        ).forEach { sql ->
            try { sqlite.execSQL(sql); fail("Restrição esperada") } catch (e: android.database.sqlite.SQLiteConstraintException) { }
        }
        assertNotNull(repo.atendimento(id))
        sqlite.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
    }
    @Test fun criacaoParcialNaoSobreviveAFalha() = runBlocking {
        val id = registrar()
        val colisao = repo.atendimento(id)!!.pagamentos.first().id
        val antes = repo.historico(segunda, segunda.plusDays(7)).first()
        try {
            repo.registrar(barbeiro.id, local, listOf(ItemEntrada(servico.id)),
                listOf(PagamentoEntrada(FormaPagamento.PIX, 3000, colisao)), ZoneOffset.UTC)
            fail("PK duplicada deveria abortar toda a inclusão")
        } catch (e: android.database.sqlite.SQLiteConstraintException) { }
        assertEquals(antes, repo.historico(segunda, segunda.plusDays(7)).first())
    }
    @Test fun novaAssociacaoUsaPrecoETaxaVigentes() = runBlocking {
        val id = registrar()
        val original = repo.atendimento(id)!!
        repo.salvarServico(servico.copy(precoCentavos = 4000))
        val outro = Barbeiro(nome = "Outro", taxaComissaoBp = 3333)
        repo.salvarBarbeiro(outro)
        val mantido = original.itens.first { it.descontoCentavos == 500L }
        repo.editar(id, outro.id,
            listOf(ItemEntrada(servico.id, 0, mantido.id), ItemEntrada(servico.id)),
            listOf(PagamentoEntrada(FormaPagamento.PIX, 7000)))
        val editado = repo.atendimento(id)!!
        assertEquals(3333, editado.atendimento.taxaAplicadaBp)
        assertEquals(3000L, editado.itens.first { it.id == mantido.id }.precoAplicadoCentavos)
        assertEquals(4000L, editado.itens.first { it.id != mantido.id }.precoAplicadoCentavos)
        assertEquals(2333L, repo.apurar(outro.id, segunda))
        assertEquals(0L, repo.apurar(barbeiro.id, segunda))
    }
    @Test fun dataHistoricaPreservadaAoTrocarFusoDoAparelho() = runBlocking {
        val id = registrar(LocalDateTime.parse("2023-12-31T23:59:00"))
        val anterior = repo.atendimento(id)!!
        val padrao = java.util.TimeZone.getDefault()
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Tokyo"))
            val a = anterior.atendimento
            assertEquals(LocalDate.parse("2023-12-25").toEpochDay(), a.semanaInicioLocal)
            assertEquals(2500L, repo.faturamento(LocalDate.parse("2023-12-31"), segunda).first().liquidoCentavos)
            val itens = anterior.itens.map { ItemEntrada(it.servicoId, it.descontoCentavos, it.id) }
            val partes = anterior.pagamentos.map { PagamentoEntrada(FormaPagamento.PIX, it.valorCentavos, it.id) }
            repo.editar(id, barbeiro.id, itens, partes)
            assertEquals(a, repo.atendimento(id)!!.atendimento)
            repo.editar(id, barbeiro.id, itens, partes, LocalDateTime.parse("2024-01-01T00:00:00"))
            val depois = repo.atendimento(id)!!.atendimento
            assertEquals(a.fusoId, depois.fusoId)
            assertEquals(segunda.toEpochDay(), depois.semanaInicioLocal)
            assertEquals(0L, repo.apurar(barbeiro.id, LocalDate.parse("2023-12-25")))
            assertEquals(1250L, repo.apurar(barbeiro.id, segunda))
        } finally { java.util.TimeZone.setDefault(padrao) }
    }
    @Test fun confirmacaoEEdicaoConcorrentesNaoExibemEstadoParcial() = runBlocking {
        val id = registrar()
        val a = repo.atendimento(id)!!
        val itens = a.itens.map { ItemEntrada(it.servicoId, it.descontoCentavos, it.id) }
        val partes = a.pagamentos.map { PagamentoEntrada(FormaPagamento.CREDITO, it.valorCentavos, it.id) }
        val inicio = CompletableDeferred<Unit>()
        val resultados = coroutineScope {
            val editar = async(Dispatchers.Default) {
                inicio.await()
                try { repo.editar(id, barbeiro.id, itens, partes); true } catch (e: IllegalStateException) { false }
            }
            val confirmar = async(Dispatchers.Default) { inicio.await(); repo.confirmar(barbeiro.id, segunda) }
            inicio.complete(Unit)
            editar.await() to confirmar.await()
        }
        assertEquals(1250L, resultados.second.valorConfirmadoCentavos)
        val atual = repo.atendimento(id)!!
        if (resultados.first) assertTrue(atual.pagamentos.all { it.forma == "CREDITO" })
        else assertEquals(a, atual)
        // Confirmação primeiro: qualquer edição posterior, inclusive sem alterar o valor, é bloqueada.
        falha { repo.editar(id, barbeiro.id, itens, partes) }
        repo.desfazerConfirmacao(barbeiro.id, segunda, true)
        // Edição primeiro: a confirmação seguinte usa o novo total.
        repo.editar(id, barbeiro.id, a.itens.map { ItemEntrada(it.servicoId, 3000, it.id) }, emptyList())
        assertEquals(0L, repo.confirmar(barbeiro.id, segunda).valorConfirmadoCentavos)
    }
    @Test fun dadosPermanecemDepoisDeReabrirBanco() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val nome = "persistencia-teste-${novoId()}.db"
        var arquivo: GestaoDatabase? = null
        try {
            val primeira = Room.databaseBuilder(contexto, GestaoDatabase::class.java, nome).build()
            arquivo = primeira
            val r = GestaoRepository(primeira)
            r.salvarBarbeiro(barbeiro)
            r.salvarServico(servico)
            val id = r.registrar(barbeiro.id, local, listOf(ItemEntrada(servico.id)),
                listOf(PagamentoEntrada(FormaPagamento.DINHEIRO, 3000)), ZoneOffset.UTC)
            val original = r.atendimento(id)
            val confirmacao = r.confirmar(barbeiro.id, segunda)
            primeira.close()
            val segundaAbertura = Room.databaseBuilder(contexto, GestaoDatabase::class.java, nome).build()
            arquivo = segundaAbertura
            val reaberto = GestaoRepository(segundaAbertura)
            assertEquals(original, reaberto.atendimento(id))
            assertEquals(confirmacao, reaberto.confirmar(barbeiro.id, segunda))
        } finally {
            arquivo?.close()
            contexto.deleteDatabase(nome)
        }
    }
}




