package com.seuxico.sxgestao.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
internal interface GestaoDao {
    @Query("SELECT * FROM Barbeiro ORDER BY id") suspend fun exportarBarbeiros(): List<Barbeiro>
    @Query("SELECT * FROM Servico ORDER BY id") suspend fun exportarServicos(): List<Servico>
    @Query("SELECT * FROM Atendimento ORDER BY id") suspend fun exportarAtendimentos(): List<Atendimento>
    @Query("SELECT * FROM ItemAtendimento ORDER BY id") suspend fun exportarItens(): List<ItemAtendimento>
    @Query("SELECT * FROM Pagamento ORDER BY id") suspend fun exportarPagamentos(): List<Pagamento>
    @Query("SELECT * FROM PagamentoSemanal ORDER BY barbeiro_id, semana_inicio_local") suspend fun exportarConfirmacoes(): List<PagamentoSemanal>
    @Query("DELETE FROM PagamentoSemanal") suspend fun limparConfirmacoes()
    @Query("DELETE FROM Pagamento") suspend fun limparPagamentos()
    @Query("DELETE FROM ItemAtendimento") suspend fun limparItens()
    @Query("DELETE FROM Atendimento") suspend fun limparAtendimentos()
    @Query("DELETE FROM Barbeiro") suspend fun limparBarbeiros()
    @Query("DELETE FROM Servico") suspend fun limparServicos()
    @Query("SELECT * FROM Barbeiro ORDER BY nome") fun barbeiros(): Flow<List<Barbeiro>>
    @Query("SELECT * FROM Servico ORDER BY nome") fun servicos(): Flow<List<Servico>>
    @Query("SELECT * FROM Barbeiro WHERE id = :id") suspend fun barbeiro(id: String): Barbeiro?
    @Query("SELECT * FROM Servico WHERE id = :id") suspend fun servico(id: String): Servico?
    @Insert suspend fun inserir(barbeiro: Barbeiro)
    @Update suspend fun atualizar(barbeiro: Barbeiro)
    @Insert suspend fun inserir(servico: Servico)
    @Update suspend fun atualizar(servico: Servico)
    @Insert suspend fun inserir(atendimento: Atendimento)
    @Update suspend fun atualizar(atendimento: Atendimento)
    @Insert suspend fun inserirItens(itens: List<ItemAtendimento>)
    @Insert suspend fun inserirPagamentos(pagamentos: List<Pagamento>)
    @Query("DELETE FROM ItemAtendimento WHERE atendimento_id = :id") suspend fun removerItens(id: String)
    @Query("DELETE FROM Pagamento WHERE atendimento_id = :id") suspend fun removerPagamentos(id: String)
    @Transaction @Query("SELECT * FROM Atendimento WHERE id = :id") suspend fun atendimento(id: String): AtendimentoCompleto?
    @Transaction @Query("SELECT * FROM Atendimento WHERE dia_local >= :inicio AND dia_local < :fim ORDER BY efetivo_utc_ms")
    fun historico(inicio: Long, fim: Long): Flow<List<AtendimentoCompleto>>
    @Transaction @Query("SELECT * FROM Atendimento WHERE barbeiro_id = :barbeiro AND semana_inicio_local = :semana AND cancelado = 0")
    suspend fun semana(barbeiro: String, semana: Long): List<AtendimentoCompleto>
    @Query("SELECT * FROM PagamentoSemanal WHERE barbeiro_id = :barbeiro AND semana_inicio_local = :semana")
    suspend fun confirmacao(barbeiro: String, semana: Long): PagamentoSemanal?
    @Query("SELECT * FROM PagamentoSemanal ORDER BY semana_inicio_local") fun confirmacoes(): Flow<List<PagamentoSemanal>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun inserir(confirmacao: PagamentoSemanal): Long
    @Query("DELETE FROM PagamentoSemanal WHERE barbeiro_id = :barbeiro AND semana_inicio_local = :semana")
    suspend fun desfazer(barbeiro: String, semana: Long)
}

@Database(entities = [Barbeiro::class, Servico::class, Atendimento::class, ItemAtendimento::class, Pagamento::class, PagamentoSemanal::class], version = 1, exportSchema = true)
abstract class GestaoDatabase : RoomDatabase() {
    internal abstract fun dao(): GestaoDao
    companion object {
        @Volatile private var instance: GestaoDatabase? = null
        fun getInstance(context: Context): GestaoDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, GestaoDatabase::class.java, "sx-gestao.db")
                .build().also { instance = it }
        }
    }
}
