package com.seuxico.sxgestao.data.local

import androidx.room.*
import java.util.UUID

fun novoId(): String = UUID.randomUUID().toString()

@Entity(tableName = "Barbeiro")
data class Barbeiro(
    @PrimaryKey val id: String = novoId(), val nome: String, val ativo: Boolean = true,
    val proprietario: Boolean = false,
    @ColumnInfo(name = "taxa_comissao_bp") val taxaComissaoBp: Int = if (proprietario) 0 else 5000,
)
@Entity(tableName = "Servico")
data class Servico(
    @PrimaryKey val id: String = novoId(), val nome: String, val ativo: Boolean = true,
    @ColumnInfo(name = "preco_centavos") val precoCentavos: Long,
)
@Entity(tableName = "Atendimento", foreignKeys = [ForeignKey(
    entity = Barbeiro::class, parentColumns = ["id"], childColumns = ["barbeiro_id"],
    onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT,
)], indices = [Index(value = ["barbeiro_id", "semana_inicio_local", "cancelado"]), Index("dia_local")])
data class Atendimento(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "barbeiro_id") val barbeiroId: String,
    @ColumnInfo(name = "efetivo_utc_ms") val efetivoUtcMs: Long,
    @ColumnInfo(name = "fuso_id") val fusoId: String,
    @ColumnInfo(name = "offset_efetivo_segundos") val offsetEfetivoSegundos: Int,
    @ColumnInfo(name = "dia_local") val diaLocal: Long,
    @ColumnInfo(name = "semana_inicio_local") val semanaInicioLocal: Long,
    @ColumnInfo(name = "taxa_aplicada_bp") val taxaAplicadaBp: Int,
    val cancelado: Boolean = false,
)
@Entity(tableName = "ItemAtendimento", foreignKeys = [
    ForeignKey(entity = Atendimento::class, parentColumns = ["id"], childColumns = ["atendimento_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT),
    ForeignKey(entity = Servico::class, parentColumns = ["id"], childColumns = ["servico_id"], onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT),
], indices = [Index("atendimento_id"), Index("servico_id")])
data class ItemAtendimento(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "atendimento_id") val atendimentoId: String,
    @ColumnInfo(name = "servico_id") val servicoId: String,
    @ColumnInfo(name = "preco_aplicado_centavos") val precoAplicadoCentavos: Long,
    @ColumnInfo(name = "desconto_centavos") val descontoCentavos: Long,
)
@Entity(tableName = "Pagamento", foreignKeys = [ForeignKey(
    entity = Atendimento::class, parentColumns = ["id"], childColumns = ["atendimento_id"],
    onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT,
)], indices = [Index("atendimento_id")])
data class Pagamento(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "atendimento_id") val atendimentoId: String,
    val forma: String,
    @ColumnInfo(name = "valor_centavos") val valorCentavos: Long,
)
@Entity(tableName = "PagamentoSemanal", primaryKeys = ["barbeiro_id", "semana_inicio_local"], foreignKeys = [ForeignKey(
    entity = Barbeiro::class, parentColumns = ["id"], childColumns = ["barbeiro_id"],
    onDelete = ForeignKey.RESTRICT, onUpdate = ForeignKey.RESTRICT,
)])
data class PagamentoSemanal(
    @ColumnInfo(name = "barbeiro_id") val barbeiroId: String,
    @ColumnInfo(name = "semana_inicio_local") val semanaInicioLocal: Long,
    @ColumnInfo(name = "valor_confirmado_centavos") val valorConfirmadoCentavos: Long,
    @ColumnInfo(name = "confirmado_em_utc_ms") val confirmadoEmUtcMs: Long,
)

data class AtendimentoCompleto(
    @Embedded val atendimento: Atendimento,
    @Relation(parentColumn = "id", entityColumn = "atendimento_id") val itens: List<ItemAtendimento>,
    @Relation(parentColumn = "id", entityColumn = "atendimento_id") val pagamentos: List<Pagamento>,
)
