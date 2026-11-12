# Checklist de implementação do MVP

Sequência derivada do SRS, aprovada explicitamente pelo proprietário em
11/11/2026 nesta execução. Substitui a lacuna de planejamento identificada em
`mvp-preflight.md`. Os critérios completos continuam em `srs.md`,
`business-rules.md`, `database-design.md` e `ux-ui.md`.

## Ordem e critérios

- [x] 1. Fundação: quatro abas, navegação sem duplicação, recriação e tema escuro;
  um módulo :app, minSdk 24. Validar compilação, NavigationTest e lint.
- [x] 2. Persistência: seis tabelas e esquema v1, regras financeiras, snapshots,
  temporalidade, transações, bloqueios e confirmação idempotente. Executar os
  seis testes unitários e os testes de persistência, conferir resultados reais
  e compatibilidade API 24 conforme `instrumented-testing.md`.
- [x] 3. Cadastros (RF-01 e RF-02): listas, criação, edição e inativação de
  barbeiros/serviços; proprietário 0%, demais inicialmente 50%; entrada decimal
  exata, estados de formulário e preservação histórica.
- [x] 4. Registro de atendimento (RF-03 e RF-04): formulário único acessível pelo
  Início e Atendimentos, data/hora efetivas, serviços repetidos, descontos,
  pagamento único/dividido/gratuito, validação, preservação e descarte de rascunho.
- [x] 5. Histórico, edição e cancelamento (RF-05): detalhe com snapshots,
  vínculos inativos mantidos, confirmação explícita de cancelamento, edição
  atômica, bloqueios de origem/destino e retorno ao rascunho.
- [x] 6. Painel e comissões (RF-06 a RF-08): períodos civis, distribuição de
  recebimentos, parcela da barbearia, detalhe semanal, confirmação idempotente,
  desfazimento explícito e integração dos bloqueios com os formulários.
- [x] 7. Backup e restauração (RF-09): SAF, formato versionado, validação isolada
  integral, confirmação explícita e substituição atômica recuperável.
- [ ] 8. Validação final (RNF-01 a RNF-06): fluxos offline integrados, API 24,
  TalkBack, fonte ampliada, tema escuro, integridade e regressão completa.

## Checkpoint por etapa

Compilar, executar testes unitários e instrumentados relevantes, executar lint,
inspecionar XMLs novos (zero testes não valida), registrar resultados e limitações
e revisar regressões antes de avançar. Atualizar este documento e os documentos
afetados. Criar commit somente se houver base limpa e isolamento seguro.

## Evidências desta execução

- Git: branch `master` sem commits; arquivos existentes sem rastreamento.
  Não criar commit de etapa misturando a base preexistente.
- Validação inicial: tentativa no sandbox bloqueada por `SocketException` no
  download. Reexecução autorizada fora do sandbox baixou Gradle 9.6.0.
- ADB fora do sandbox identificou `emulator-5554` no estado `device`.
- A primeira tentativa não validou testes: unitários UP-TO-DATE e emulador
  desconectado antes dos instrumentados (`No connected devices`).
- Reexecução: `validation-base-rerun.log`, 6 unitários aprovados, zero falhas e
  ignorados; lint com zero erros, zero NewApi e 18 avisos (inclui checks antigos
  de Lifecycle que não puderam ser carregados; não foram suprimidos).
- API 24, Small_Phone x86: `validation-base-api24.log`, assembleDebug e APK de
  teste recompilados com --rerun-tasks, 12 instrumentados aprovados (10 Room,
  navegação/recriação e contexto), zero falhas/erros/ignorados. XML instrumentado
  inspecionado antes da execução seguinte (que reutiliza a pasta de resultados).
  Etapas 1 e 2 verificadas para prosseguir. XMLs unitários em `validation/base-api24/`
  foram copiados durante a etapa seguinte e incluem também o teste de formatos.
- APIs 29/35/37 e avaliação manual de acessibilidade permanecem na validação final.

### Etapa 3

Cadastros integrados com ViewModel/StateFlow, coleta por ciclo de vida, listas
observáveis, edição, proprietário somente leitura em 0%, inativação confirmada,
validação e prevenção de reenvio. Entrada exata em centavos e normalização ao
perder foco sem truncar precisão. Sem alteração do esquema.

`validation-etapa3-final.log`: assembleDebug, 7 unitários e 13 instrumentados
na API 24, zero falhas/erros/ignorados; lint sem erros/NewApi. XMLs em
`validation/etapa3/`. Inclui criação de barbeiro, taxa inválida rejeitada,
taxa 33,33%, inativação com preservação e serviço por R$ 12,30, além da regressão
de persistência e navegação. Sem commit: base inicial sem rastreamento.

### Etapa 4

Formulário único acessível pelas duas entradas, com ViewModel, validação pura,
data/hora e seletores nativos, fuso capturado, offsets ambíguos explícitos,
itens repetidos, descontos independentes, pagamento único/dividido/gratuito,
resumo, erros, foco e descarte confirmado. Sem alteração do esquema.

`validation-etapa4-final.log`: compilação, 9 unitários aprovados e 15
instrumentados API 24 aprovados, zero falhas/erros/ignorados; lint sem erros.
XMLs em `validation/etapa4/`. Os dois testes Compose novos registram dois itens
com descontos e divisão e verificam recusa/aceite de descarte com entrada inválida.
Sem commit pelo mesmo estado inicial do Git.

### Etapa 5

Histórico observável, detalhes com valores e situação, edição integrada ao mesmo
formulário, manutenção de vínculos inativos e snapshots, preservação do tempo
histórico sem reinterpretar campos inalterados e cancelamento lógico confirmado.
As proteções semanais transacionais continuam aplicadas; o acesso visual ao
detalhe de comissão será conectado na etapa 6.

`validation-etapa5.log`: compilação, 10 unitários e 17 instrumentados API 24
aprovados, zero falhas/erros/ignorados; lint sem erros. XMLs em
`validation/etapa5/`. Novos testes verificam edição com cadastros inativos,
preços/taxa/IDs preservados, quitação recalculada e recusa/aceite de cancelamento.
Sem mudança de esquema ou commit.

### Etapa 6

Painel diário e semanal com classificação civil persistida, recebimentos sem
multiplicação por itens, parcela da barbearia e consulta por semana. Detalhe de
comissão com confirmação idempotente, instante e desfazimento confirmado.
Bloqueios identificam todos os pares de origem/destino e abrem o detalhe semanal.
O formulário mantém entradas e reavalia dados observáveis ao retornar; só grava
no envio explícito, com nova verificação transacional.

`validation-etapa6.log`: compilação, 11 unitários e 19 instrumentados API 24
aprovados, zero falhas/erros/ignorados; lint sem erros. XMLs em
`validation/etapa6/`. Novos casos: parcela 70/100, confirmação/desfazimento,
recebimentos preservados e navegação dos dois bloqueios sem gravação automática.
Sem mudança de esquema ou commit.

### Etapa 7

SAF, formato JSON v1 com checksum e validação estrita integral, cópia isolada
confirmada e substituição transacional no mesmo banco. Novas escritas ficam
bloqueadas durante restauração; a navegação muda de geração após o commit.
Backup automático desabilitado. Contrato em `backup-format.md`.

A primeira execução detectou ordem implícita das relações diferente após restore.
A leitura pública agora ordena filhos por UUID. A asserção integral foi mantida.
Falha original e correção estão arquivadas em `validation/etapa7/`.

`validation-etapa7-final.log`: compilação, 11 unitários aprovados, lint sem erros
e 23 instrumentados aprovados. `validation-etapa7-interrupcao.log`: 24 instrumentados
API 24 aprovados após adicionar interrupção antes do commit; zero
falhas/erros/ignorados. Testes verificam roundtrip/reabertura, recusa, corrupção,
versão, tipos, FKs, datas e apuração inválidos, falha após exclusões, rollback
após reabrir, interrupção e recusa de escrita concorrente. Sem mudança de esquema
ou commit. SAF foi integrado pelos contratos Android; a confirmação visual foi
testada com URI de arquivo privado, sem automação do seletor externo.

### Etapa 8 — encerramento e limites

Em 11/11/2026 o proprietário dispensou explicitamente a última conferência.
Etapas 1 a 7 implementadas e integradas; a etapa 8 não é declarada integralmente
validada. Não haverá nova execução de testes para esse checkpoint.

Resultados anteriores à dispensa, conferidos em XMLs novos:

| Validação | Resultado |
| --- | --- |
| Compilação debug e APK instrumentado | Aprovadas |
| Unitários, sem cache | 11 aprovados, zero falhas/erros/ignorados |
| Instrumentados API 24, sem cache | 26 aprovados, zero falhas/erros/ignorados |
| Instrumentados API 37, AVD read-only | 26 aprovados, zero falhas/erros/ignorados |
| Lint da execução API 24 | Zero erros/NewApi; 19 avisos |
| API 29 | Imagem não instalada; não executada |
| API 35 | Não executada antes do encerramento solicitado |

Evidências: `validation/final-api24/`, `validation/final-api37/`,
`../validation-final-api24-corrigida.log` e `../validation-final-api37.log`.
O runner API 37 executou efetivamente 26 testes, apesar de um aviso transitório
do ADB durante a preparação.

A regressão cobre navegação completa de cadastro, atendimento, painel, comissão,
correção e cancelamento, fonte 200%, foco no primeiro erro, proprietário e backup
com comissão zero. O teste de rolagem virtualizada foi corrigido sem remover
asserções. A anotação que provocou falha interna do lint foi revertida, sem
desabilitar detectores.

Rótulos acessíveis do checkbox e das seleções, assinatura de `CampoDecimal`
conforme a convenção de Modifier e sua asserção de acessibilidade foram ajustados
depois desses resultados. A conferência adicional foi dispensada; esses ajustes
não têm nova aprovação de compilação, testes ou lint. Os 19 avisos anteriores
incluem Modifier, checks antigos de Lifecycle, versões de dependências, recursos
não usados e label redundante.

Avaliação humana de TalkBack/contraste e percurso do SAF externo seguem pendentes.
Roteiro: `manual-validation.md`. Esquema v1, minSdk 24 e um único módulo :app
preservados. Sem commits: a base inicial estava sem commits e sem rastreamento,
impedindo isolamento seguro por etapa.






