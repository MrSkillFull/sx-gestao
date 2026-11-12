package com.seuxico.sxgestao

import android.net.Uri
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.seuxico.sxgestao.data.local.*
import com.seuxico.sxgestao.data.repository.*
import com.seuxico.sxgestao.ui.configuracoes.*
import com.seuxico.sxgestao.ui.theme.SXGestaoTheme
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*

class BackupInterfaceTest {
    @get:Rule val compose = createComposeRule()
    @Test fun validacaoPrecedeConfirmacaoERecusaPreservaBase() {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(contexto, GestaoDatabase::class.java).build()
        val repo = GestaoRepository(db)
        val arquivo = File(contexto.cacheDir, "backup-interface-${novoId()}.json")
        try {
            runBlocking { repo.salvarServico(Servico(nome = "Corte", precoCentavos = 3000)); arquivo.writeText(repo.exportarBackup()); repo.salvarServico(Servico(nome = "Barba", precoCentavos = 2000)) }
            val vm = ConfiguracoesViewModel(repo, contexto)
            compose.setContent { SXGestaoTheme { ConfiguracoesScreen(vm) } }
            compose.runOnIdle { vm.validar(Uri.fromFile(arquivo)) }
            compose.waitUntil(5000) { !vm.state.value.ocupado && vm.state.value.arquivo != null }
            compose.onNodeWithText("Restaurar backup?", substring = false).assertIsDisplayed()
            assertEquals(2, runBlocking { repo.servicos().first().size })
            compose.onNodeWithText("Voltar", substring = false).performClick()
            assertEquals(2, runBlocking { repo.servicos().first().size })
            compose.runOnIdle { vm.validar(Uri.fromFile(arquivo)) }
            compose.waitUntil(5000) { !vm.state.value.ocupado && vm.state.value.arquivo != null }
            compose.onNodeWithText("Substituir dados").performClick()
            compose.waitUntil(5000) { vm.state.value.mensagem != null }
            assertEquals(1, runBlocking { repo.servicos().first().size })
        } finally { arquivo.delete(); db.close() }
    }
}
