# Etapa 2 — Persistência Room

> Retomada em 11/11/2026: fundação e persistência passaram nos testes API 24.
> O JSON v1 foi exportado e exercitado; não houve mudança de esquema ou migração.
> Consulte `implementation-checklist.md` e `validation/` para os resultados atuais.
> As tentativas bloqueadas abaixo são o histórico da implementação.

## Implementação

- `app/build.gradle.kts` e `gradle/libs.versions.toml`: Room 2.8.4, KSP 2.3.10,
  exportação em `app/schemas` e desugaring 2.1.5; `minSdk` permanece 24.
- `data/local/Entities.kt`: as seis tabelas do DER, colunas e índices aprovados,
  UUIDs TEXT, FKs RESTRICT, PK composta da confirmação semanal.
- `data/local/GestaoDatabase.kt`: DAOs internos, consultas observáveis e banco v1
  singleton, sem substituição destrutiva. Room habilita as FKs na abertura.
- `data/repository/GestaoRepository.kt`: fronteira de escrita transacional,
  cadastros/inativação, atendimento/edição/cancelamento, snapshots, faturamento,
  apuração, confirmação idempotente e desfazimento. Cancelar e desfazer exigem
  sinal explícito de confirmação do chamador; a apresentação fica para outra etapa.
- `model/RegrasFinanceiras.kt`: centavos Long, soma verificada, taxas em pontos-base,
  comissão HALF_UP por atendimento sem multiplicação integral sujeita a overflow.
- `RegrasFinanceirasTest.kt` e `PersistenciaTest.kt`: testes puros e instrumentados.

## Decisões temporais e de concorrência

`java.time` é usado com core library desugaring. Hora local inexistente é rejeitada.
Hora ambígua exige `ZoneOffset` explícito entre os offsets válidos. Na criação, o
fuso padrão é capturado ao chamar o repositório. A edição sem nova data preserva
exatamente os dados temporais gravados; com nova data usa o fuso original.
Consultas históricas usam chaves civis persistidas e intervalos [início, fim).

Todas as leituras de decisão e gravações ocorrem em `withTransaction`, inclusive
confirmação/desfazimento. Room serializa as transações de escrita do banco. O DAO
não fica exposto às funcionalidades. A PK semanal é a garantia adicional de
unicidade; conflito de confirmação usa IGNORE e releitura, sem substituir a linha.
Não há join entre itens e pagamentos na apuração, evitando multiplicação de totais.

## Validação e pendências concretas

Tentativa: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`.
O wrapper falhou antes de configurar o projeto: o cache padrão `C:\.gradle` não era
gravável. Nova tentativa com `GRADLE_USER_HOME` no projeto falhou ao baixar
`gradle-9.6.0-bin.zip`: `java.net.SocketException: Permission denied: getsockopt`.
O SDK configurado em `local.properties` também teve acesso negado pelo ambiente.
Assim, compilação, resolução de dependências, lint e testes **não foram executados**.

Antes de aprovar esta etapa, executar em ambiente com Gradle/SDK acessíveis:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

Executar os instrumentados também em API 24; gerar e revisar o JSON oficial v1 em
`app/schemas`. Room 2.8.4 declara mínimo API 23, compatível com o mínimo 24 do app;
a compatibilidade do conjunto resolvido ainda depende do build/lint acima.
Referência: https://developer.android.com/jetpack/androidx/releases/room

Cobertura preparada: descontos/quitação, arredondamento/overflow, taxas, semana e
transições de fuso; FKs/PK/NOT NULL, rollback de criação/edição após falha na gravação
dos filhos, snapshots e IDs, cadastros inativos, cancelamento, faturamento sem
duplicação, bloqueios origem/destino inclusive comissão zero, confirmações paralelas,
edição concorrente com confirmação, desfazimento/reconfirmação, mudança de fuso e
reabertura do banco.

UI, navegação e identidade visual permanecem intactas. Backup não foi implementado.


