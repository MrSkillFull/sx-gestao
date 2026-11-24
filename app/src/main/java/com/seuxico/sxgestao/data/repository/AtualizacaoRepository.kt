package com.seuxico.sxgestao.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class InfoAtualizacao(
    val versao: String,
    val notas: String,
    val urlDownload: String,
    val nomeArquivo: String
)

class AtualizacaoRepository(
    private val repoOwner: String = "MrSkillFull",
    private val repoName: String = "sx-gestao"
) {

    /**
     * Consulta a última release publicada no GitHub e compara com a versão atual instalada.
     * Retorna [InfoAtualizacao] se houver versão mais recente com APK disponível; senão null.
     */
    suspend fun verificarAtualizacao(versaoAtual: String): InfoAtualizacao? = withContext(Dispatchers.IO) {
        try {
            val endpoint = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "SXGestao-App")
            }

            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext null
            }

            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)

            val tagName = json.optString("tag_name", "").trim()
            val versaoRemota = tagName.removePrefix("v").removePrefix("V")
            if (versaoRemota.isBlank()) return@withContext null

            if (!ehVersaoMaisNova(versaoRemota, versaoAtual)) {
                return@withContext null
            }

            val assets = json.optJSONArray("assets") ?: return@withContext null
            var downloadUrl: String? = null
            var nomeArquivo = "SXGestao-v$versaoRemota.apk"

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.optString("name", "")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url", "")
                    nomeArquivo = name
                    break
                }
            }

            if (downloadUrl.isNullOrBlank()) return@withContext null

            val notas = json.optString("body", "").trim()

            InfoAtualizacao(
                versao = versaoRemota,
                notas = notas,
                urlDownload = downloadUrl,
                nomeArquivo = nomeArquivo
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Baixa o APK da release salvando no diretório especificado (ex: cacheDir).
     * Reporta o progresso via callback. Suporta cancelamento por coroutine.
     */
    suspend fun baixarApk(
        urlDownload: String,
        destino: File,
        onProgresso: (baixado: Long, total: Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            var urlAtual = URL(urlDownload)
            var redirecionamentos = 0
            val maxRedirecionamentos = 5

            // Lidar com redirecionamentos HTTP (comum em GitHub assets que redirecionam para AWS S3)
            while (redirecionamentos < maxRedirecionamentos) {
                conn = (urlAtual.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "SXGestao-App")
                }

                val status = conn.responseCode
                if (status == HttpURLConnection.HTTP_MOVED_PERM ||
                    status == HttpURLConnection.HTTP_MOVED_TEMP ||
                    status == 307 || status == 308
                ) {
                    val novaUrl = conn.getHeaderField("Location") ?: break
                    urlAtual = URL(novaUrl)
                    conn.disconnect()
                    redirecionamentos++
                } else {
                    break
                }
            }

            val connection = conn ?: throw IllegalStateException("Falha ao abrir conexão para download.")
            val totalBytes = connection.contentLength.toLong()
            inputStream = connection.inputStream

            if (destino.exists()) {
                destino.delete()
            }
            outputStream = FileOutputStream(destino)

            val buffer = ByteArray(8192)
            var bytesLidos: Int
            var totalBaixado = 0L

            while (inputStream.read(buffer).also { bytesLidos = it } != -1) {
                outputStream.write(buffer, 0, bytesLidos)
                totalBaixado += bytesLidos
                onProgresso(totalBaixado, totalBytes)
            }

            outputStream.flush()
            destino
        } finally {
            try { outputStream?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            try { conn?.disconnect() } catch (_: Exception) {}
        }
    }

    companion object {
        /**
         * Compara semântica de versões numericamente (ex: "1.0.3" vs "1.0.2").
         * Suporta sufixos ou números extras. Retorna true se [remota] > [atual].
         */
        fun ehVersaoMaisNova(remota: String, atual: String): Boolean {
            val partesRemota = parsePartes(remota)
            val partesAtual = parsePartes(atual)
            val maxLen = maxOf(partesRemota.size, partesAtual.size)

            for (i in 0 until maxLen) {
                val numRemoto = partesRemota.getOrElse(i) { 0 }
                val numAtual = partesAtual.getOrElse(i) { 0 }
                if (numRemoto > numAtual) return true
                if (numRemoto < numAtual) return false
            }
            return false
        }

        private fun parsePartes(versao: String): List<Int> {
            val limpo = versao.trim().removePrefix("v").removePrefix("V")
            return limpo.split(".", "-", "_").mapNotNull {
                // Extrai apenas dígitos caso haja algo como "1-beta" ou similar
                it.takeWhile { c -> c.isDigit() }.toIntOrNull()
            }
        }
    }
}
