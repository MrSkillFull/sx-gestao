package com.seuxico.sxgestao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.*
import java.time.*
import java.security.MessageDigest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.*
import org.junit.*
import org.junit.Assert.*

class BackupTest {
    private lateinit var db: GestaoDatabase
    private lateinit var repo: GestaoRepository
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val nome = "backup-teste-${novoId()}.db"
    private val semana = LocalDate.parse("2024-01-01")
    private val b = Barbeiro(nome = "João", taxaComissaoBp = 3333)
    private val s = Servico(nome = "Corte", precoCentavos = 3000)
    private lateinit var id: String
    @Before fun abrir() = runBlocking {
        db = Room.databaseBuilder(context, GestaoDatabase::class.java, nome).build()
        repo = GestaoRepository(db, Clock.fixed(Instant.parse("2024-01-08T12:00:00Z"), ZoneOffset.UTC))
        repo.salvarBarbeiro(b); repo.salvarServico(s)
        id = repo.registrar(b.id, LocalDateTime.parse("2024-01-03T23:59:00"), listOf(ItemEntrada(s.id), ItemEntrada(s.id, 500)),
            listOf(PagamentoEntrada(FormaPagamento.PIX, 1000), PagamentoEntrada(FormaPagamento.DEBITO, 4500)), ZoneId.of("America/Sao_Paulo"))
        val cancelado = repo.registrar(b.id, LocalDateTime.parse("2024-01-04T00:00:00"), listOf(ItemEntrada(s.id, 3000)), emptyList(), ZoneOffset.UTC)
        repo.cancelar(cancelado, true)
        val dono = Barbeiro(nome = "Proprietário", proprietario = true)
        repo.salvarBarbeiro(dono)
        repo.registrar(dono.id, LocalDateTime.parse("2024-01-05T00:00:00"), listOf(ItemEntrada(s.id, 3000)), emptyList(), ZoneOffset.UTC)
        repo.confirmar(dono.id, semana)
        repo.confirmar(b.id, semana)
        repo.salvarBarbeiro(b.copy(ativo = false, taxaComissaoBp = 10000)); repo.salvarServico(s.copy(ativo = false, precoCentavos = 9000))
    }
    @After fun fechar() { db.close(); context.deleteDatabase(nome) }
    @Test fun roundtripIntegralRecusaEReabertura() = runBlocking {
        val backup = repo.exportarBackup()
        val anterior = repo.atendimento(id)
        val confirmacao = repo.confirmacoes().first().single { it.barbeiroId == b.id }
        val candidato = repo.validarBackup(backup)
        repo.salvarServico(Servico(nome = "Extra", precoCentavos = 1))
        try { repo.restaurarBackup(candidato, false); fail("Exige confirmação") } catch (_: IllegalArgumentException) { }
        assertEquals(2, repo.servicos().first().size)
        repo.restaurarBackup(candidato, true)
        assertEquals(1, repo.servicos().first().size)
        assertEquals(anterior, repo.atendimento(id))
        assertEquals(confirmacao, repo.confirmacoes().first().single { it.barbeiroId == b.id })
        assertEquals(0L, repo.confirmacoes().first().single { it.barbeiroId != b.id }.valorConfirmadoCentavos)
        db.close()
        db = Room.databaseBuilder(context, GestaoDatabase::class.java, nome).build()
        repo = GestaoRepository(db)
        assertEquals(backup, repo.exportarBackup())
        assertFalse(repo.barbeiros().first().single { it.id == b.id }.ativo)
        assertEquals(3, repo.historico(semana, semana.plusDays(7)).first().size)
    }
    @Test fun rejeitaCorrupcaoVersaoTiposVinculosETotais() = runBlocking {
        val backup = repo.exportarBackup()
        fun rejeitar(texto: String) { assertTrue(runCatching { repo.validarBackup(texto) }.isFailure) }
        rejeitar("arquivo inválido")
        rejeitar(backup.dropLast(10))
        rejeitar(JSONObject(backup).put("versaoFormato", 2).toString())
        rejeitar(JSONObject(backup).put("payload", "{}").toString())
        fun alterar(mudar: (JSONObject) -> Unit): String {
            val envelope = JSONObject(backup)
            val payload = JSONObject(envelope.getString("payload"))
            mudar(payload)
            val texto = payload.toString()
            val hash = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }
            return envelope.put("payload", texto).put("sha256", hash).toString()
        }
        rejeitar(alterar { it.remove("itens") })
        rejeitar(alterar { it.getJSONArray("pagamentos").getJSONArray(0).put(3, "1000") })
        rejeitar(alterar { it.getJSONArray("barbeiros").getJSONArray(0).put(2, 1) })
        rejeitar(alterar { it.getJSONArray("itens").getJSONArray(0).put(2, novoId()) })
        rejeitar(alterar { it.getJSONArray("confirmacoes").getJSONArray(0).put(2, 1) })
        rejeitar(alterar { it.getJSONArray("atendimentos").getJSONArray(0).put(5, 0) })
        assertEquals(backup, repo.exportarBackup())
    }
    @Test fun falhaDuranteInsercaoReverteTodaSubstituicaoMesmoAposReabrir() = runBlocking {
        val candidato = repo.validarBackup(repo.exportarBackup())
        val extra = Servico(nome = "Preservar", precoCentavos = 1230)
        repo.salvarServico(extra)
        val antes = repo.exportarBackup()
        db.openHelper.writableDatabase.execSQL("CREATE TRIGGER falha_restore BEFORE INSERT ON Servico BEGIN SELECT RAISE(ABORT, 'falha simulada'); END")
        try { repo.restaurarBackup(candidato, true); fail("Deveria falhar após as exclusões") } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        assertFalse(EstadoBase.restaurando.value)
        db.close()
        db = Room.databaseBuilder(context, GestaoDatabase::class.java, nome).build()
        repo = GestaoRepository(db)
        assertEquals(antes, repo.exportarBackup())
        assertTrue(repo.servicos().first().any { it.id == extra.id })
    }
    @Test fun interrupcaoAntesDoCommitPreservaBaseERecusaEscritasConcorrentes() = runBlocking {
        val candidato = repo.validarBackup(repo.exportarBackup())
        repo.salvarServico(Servico(nome = "Preservar na interrupção", precoCentavos = 1))
        val antes = repo.exportarBackup()
        db.close()
        val armado = AtomicBoolean(false)
        val chegou = CountDownLatch(1)
        val continuar = CountDownLatch(1)
        db = Room.databaseBuilder(context, GestaoDatabase::class.java, nome).setQueryCallback({ sql, _ ->
            if (armado.get() && sql.startsWith("DELETE FROM Servico")) {
                chegou.countDown()
                check(continuar.await(10, TimeUnit.SECONDS))
            }
        }, Executor { it.run() }).build()
        repo = GestaoRepository(db)
        armado.set(true)
        val trabalho = launch(Dispatchers.Default) { repo.restaurarBackup(candidato, true) }
        try {
            assertTrue(chegou.await(10, TimeUnit.SECONDS))
            assertTrue(EstadoBase.restaurando.value)
            try { repo.salvarServico(Servico(nome = "Concorrente", precoCentavos = 1)); fail("Escrita concorrente deve ser recusada") } catch (_: IllegalStateException) { }
            trabalho.cancel()
        } finally { continuar.countDown() }
        trabalho.join()
        assertFalse(EstadoBase.restaurando.value)
        db.close()
        db = Room.databaseBuilder(context, GestaoDatabase::class.java, nome).build()
        repo = GestaoRepository(db)
        assertEquals(antes, repo.exportarBackup())
    }
}
