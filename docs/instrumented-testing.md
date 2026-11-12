# Infraestrutura de testes instrumentados

Auditoria em 11/11/2026. Escopo limitado aos testes; minSdk 24, targetSdk 37,
produção, Room e regras financeiras permanecem inalterados.

## Retomada do MVP em 11/11/2026

XMLs novos confirmaram 26 testes aprovados na API 24 e 26 na API 37, sem falhas,
erros ou ignorados. Os 11 unitários passaram sem cache, com compilação e lint sem
erros. Evidências: `validation/final-api24/` e `validation/final-api37/`.

API 29 não está instalada; API 35 não foi executada antes do encerramento.
O proprietário dispensou a última conferência dos ajustes de acessibilidade e
assinatura Compose posteriores a esses resultados. Consulte o checkpoint 8 em
`implementation-checklist.md` e o roteiro `manual-validation.md`.
As contagens de 12 testes e resultados bloqueados abaixo são históricos.

## Dependências e diagnóstico

O modelo de dependências do lint da execução anterior
(`app/build/intermediates/android_test_lint_model/debug/generateDebugAndroidTestLintModel/debug-artifact-libraries.xml`)
registra as versões efetivamente resolvidas abaixo. Não confundir as versões
declaradas no catálogo com as transitivas escolhidas pelo Gradle.

| Artefato | Resolvido anteriormente | Configuração após a correção |
| --- | --- | --- |
| Espresso core | 3.5.1 | 3.7.0 explícito |
| Espresso idling-resource | 3.5.1 | transitivo; conferir resolução com core 3.7.0 |
| AndroidX Test ext.junit (AndroidJUnit4) | 1.1.5 | 1.3.0 explícito |
| AndroidX Test runner (AndroidJUnitRunner) | 1.5.2 | 1.7.0 explícito |
| AndroidX Test core (ActivityScenario) | 1.5.0 | transitivo; conferir resolução após atualização |
| AndroidX Test monitor | 1.6.1 | transitivo; conferir resolução após atualização |
| Test services storage | 1.4.2 | transitivo; conferir resolução após atualização |
| Test annotation | 1.0.1 | transitivo, sem alteração direta |
| Compose ui-test / ui-test-junit4 / ui-test-manifest | 1.10.4 | BOM 2026.02.01 preservado |
| JUnit | 4.13.2 | 4.13.2 preservado |

Não há Orchestrator configurado. O runner passa a ser dependência direta porque
é nomeado no defaultConfig, evitando depender de sua presença transitiva.
ext.junit é atualizado junto com Espresso para usar a família estável atual de
AndroidX Test e ActivityScenario. Não foram atualizadas bibliotecas de produção.

A causa da exceção relatada na API 37 é o acesso reflexivo do Espresso antigo
ao método interno `InputManager.getInstance`, não uma asserção da navegação.
As [notas oficiais de AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test#espresso-3.7.0)
registram em Espresso 3.7.0 a troca por `getSystemService`. Essa é a versão
estável mais recente publicada na data da auditoria; não é necessário adotar
3.8.0 alpha. Espresso 3.7.0, ext.junit 1.3.0 e runner 1.7.0 têm mínimo API 21,
compatível com 24. Isso fundamenta a escolha, mas não substitui execução em
24 e 37 nem inspeção do manifesto final do APK de teste.

Compose permanece alinhado ao BOM de produção. Suas ações e asserções já
sincronizam com Compose. O NavigationTest acrescenta seleção e conteúdo visível
antes de recriar, além de `waitForIdle()` antes e depois de `scenario.recreate()`.
Recriação ocorre fora da thread principal; nenhuma referência à Activity antiga
é guardada. Não se usam sleeps, retries, filtros por SDK ou testes ignorados.
Todas as asserções anteriores foram preservadas.

## API 35: resultado vazio e ambiente

O XML anterior `TEST-Pixel_9(AVD) - 15.xml`, de 11/11/2026 às 17:02:40,
contém `tests=0`, `failures=0`, `errors=0`, `skipped=0`. O arquivo
`test-result-exit-code.txt` contém 0. Portanto, nem o exit code nem
`BUILD SUCCESSFUL` comprovam execução: essa tentativa não validou o app.
Os registros disponíveis não contêm a mensagem original de instalação/ADB
necessária para determinar se houve dispositivo offline, falha de transporte,
armazenamento, assinatura ou falha ao iniciar instrumentation. Não há evidência
suficiente para atribuir o problema ao projeto ou corrigir uma causa específica.

Na sessão desta correção, ADB e emulator não foram encontrados nos caminhos do
SDK declarado em local.properties, nem adb no PATH. O wrapper inicialmente tentou
usar `C:\.gradle`, sem acesso. Com GRADLE_USER_HOME dentro do workspace, tentou
baixar Gradle 9.6.0, mas a rede retornou `SocketException: Permission denied:
getsockopt`. São bloqueios do ambiente de execução, anteriores à compilação e à
instalação, distintos da exceção do Espresso. Não se alterou local.properties,
SDKs ou versões de produção para contorná-los.

## Validação realizada e pendente

Tentativas nesta sessão: `:app:dependencies --configuration
debugAndroidTestRuntimeClasspath` e `:app:testDebugUnitTest :app:lintDebug
:app:assembleDebugAndroidTest :app:connectedDebugAndroidTest`. Ambas bloqueadas
no download do wrapper; nenhuma dessas tarefas chegou a executar. Os logs estão
em `validation-dependencies-before.log` e `validation-test-infrastructure.log`.
Não foi possível confirmar a resolução final das transitivas, os manifests dos
novos AARs, lint, compilação ou execução da correção.

| API | Executados nesta sessão | Aprovados | Falhas de teste | Ignorados | Situação |
| --- | ---: | ---: | ---: | ---: | --- |
| 24 | 0 | 0 | 0 | 0 | Não testada; ambiente indisponível |
| 29 | 0 | 0 | 0 | 0 | Não testada; ambiente indisponível |
| 35 | 0 | 0 | 0 | 0 | Não testada; XML histórico também vazio |
| 37 | 0 | 0 | 0 | 0 | Não testada; falha anterior relatada pelo solicitante |

Há 12 métodos instrumentados no código: 10 PersistenciaTest, 1 NavigationTest e
1 ExampleInstrumentedTest. XMLs unitários anteriores registram 6 aprovações,
sem falhas ou ignorados; são históricos, não reexecução desta correção.

## Matriz recomendada e procedimento de aceitação

Executar a suíte inteira nas quatro APIs, em emuladores independentes:

| API | Verificação principal | Resultado exigido por API |
| --- | --- | --- |
| 24 | Mínimo suportado, instalação, Compose e persistência/desugaring | 12 aprovados, 0 falhas, 0 ignorados |
| 29 | Regressão em plataforma intermediária | 12 aprovados, 0 falhas, 0 ignorados |
| 35 | Instalação dos dois APKs e início real do runner | 12 aprovados, 0 falhas, 0 ignorados |
| 37 | Regressão InputManager, navegação e recriação | 12 aprovados, 0 falhas, 0 ignorados |

1. Disponibilizar SDK, platform-tools e Gradle do wrapper. Não mudar min/targetSdk.
   Executar `gradlew.bat :app:dependencies --configuration debugAndroidTestRuntimeClasspath`
   e arquivar o relatório final; confirmar ausência de Espresso 3.5.1 e runner
   1.5.2 no runtime. Inspecionar manifests mesclados e dos AARs para mínimo <=24.
2. Executar `gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest`.
   Conferir especialmente NewApi e conflitos no manifesto instrumentado.
3. Para cada API, manter somente o dispositivo de teste conectado, registrar
   `adb devices -l`, `adb -s SERIAL shell getprop ro.build.version.sdk` e
   `adb -s SERIAL shell getprop sys.boot_completed` (deve retornar 1). Registrar
   versão do emulator, imagem e ABI. Não usar um emulador offline/unauthorized.
4. Arquivar relatórios anteriores fora da pasta de resultados e executar
   `gradlew.bat :app:connectedDebugAndroidTest --rerun-tasks --info`, salvando
   saída completa e exit code. Exigir XML novo com 12 testes, nomes esperados,
   0 failures/errors/skipped. Ausência de XML, XML antigo ou zero testes é falha
   da validação mesmo quando o Gradle retorna sucesso. Arquivar por API antes
   de passar à seguinte, pois connected reutiliza o diretório de saída.
5. Se houver falha de instalação, capturar o código `INSTALL_FAILED_*` e logcat.
   Conferir espaço em `/data`, estado do package manager, caminhos dos dois APKs
   e `adb -s SERIAL shell pm list instrumentation`. O componente esperado é
   `com.seuxico.sxgestao.test/androidx.test.runner.AndroidJUnitRunner`.
   Diagnosticar transporte/boot separadamente de erro de manifest/assinatura.
   Em emulador descartável, uma execução direta de `adb -s SERIAL shell am
   instrument -w -r com.seuxico.sxgestao.test/androidx.test.runner.AndroidJUnitRunner`
   após instalar ambos os APKs pode isolar o runner do Gradle; exigir também
   `OK (12 tests)` e preservar a saída, sem tomar exit code isolado como prova.

A correção está implementada, mas a compatibilidade em dispositivo permanece
pendente dessa matriz. Não houve avanço para a Etapa 3.
