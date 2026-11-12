# Arquitetura do SX Gestão

## Estado da implementação na retomada

Fundação e persistência verificadas na API 24 em 11/11/2026. Cadastros possui
tela e ViewModel próprios, com StateFlow e coleta por ciclo de vida. O factory
em `ui/components/GestaoViewModel.kt` injeta o repositório sobre o singleton
Room. Conversão e formatação decimal ficam em `model/Formatos.kt`. Não foi
alterado o esquema v1. Consulte `implementation-checklist.md` para evidências.
Atendimentos possui formulário, histórico e detalhe, com ViewModels próprios.
Início possui painel e detalhe semanal; Configurações contém SAF e backup.
`LocalGestaoRepository` injeta uma base isolada nos testes de navegação completa;
a produção continua usando o singleton Room. Core library desugaring 2.1.5
está configurado e foi exercitado nos testes API 24.

Backup JSON v1 captura um retrato transacional e restaura o candidato validado
em uma única transação, sem troca de arquivos SQLite/WAL. `EstadoBase` coordena
bloqueio de escritas e geração das telas após commit. Ver `backup-format.md`.
O esquema permanece v1, sem migração necessária e sem fallback destrutivo.
As descrições da fundação abaixo registram o estágio inicial.

## Projeto e plataforma

O projeto tem um único módulo Gradle, `:app`, com Kotlin, Compose e Material 3. O Gradle declara `minSdk = 24`, `compileSdk = 37` e `targetSdk = 37`. O namespace e o application ID são `com.seuxico.sxgestao`.

A fundação contém uma `MainActivity` enxuta, tema Material 3 claro e escuro em preto, laranja e neutros, e quatro telas provisórias nos pacotes de funcionalidade. `ui/navigation` concentra o Navigation Compose e a barra inferior, com preservação de estado e sem duplicar destinos ao selecionar a mesma aba. Room, KSP e as dependências específicas de ViewModel e coleta de estado no Compose serão tratados nas respectivas tarefas de implementação. As telas estáticas desta etapa não precisam de ViewModels ou repositórios.

## Organização do MVP

Use arquitetura híbrida com MVVM simplificado em uma Activity. Organize UI e ViewModels por funcionalidade e centralize os dados e a persistência. As telas emitem ações e renderizam estado imutável exposto pelo ViewModel por `StateFlow`. O ViewModel coordena a validação de formulários e as operações do repositório. Room sobre SQLite é a fonte de verdade dos dados locais. Os fluxos funcionam offline e sem servidor.

| Pacote | Responsabilidade |
| --- | --- |
| `ui/inicio`, `ui/atendimentos`, `ui/cadastros`, `ui/configuracoes` | Telas e ViewModels por funcionalidade. Crie os pacotes conforme as entregas. |
| `ui/navigation`, `ui/theme`, `ui/components` | Rotas, tema e componentes compartilhados. |
| `data/local` | Banco Room, entidades, DAOs e migrações. |
| `data/repository` | Consultas e operações de gravação usadas pelas funcionalidades. |
| `model` | Tipos compartilhados e regras financeiras em Kotlin puro. |

A navegação planejada usa Navigation Compose e tem quatro telas principais: Início, Atendimentos, Cadastros e Configurações. O lançamento de atendimento pode ser aberto pelo Início e por Atendimentos. A direção visual inicial usa preto e laranja, com atenção ao contraste. Mantenha a lógica financeira independente de Compose e Room, sem uma camada de domínio separada no MVP.

## Persistência e integridade

- Use `Long` em centavos de real para armazenar e calcular valores monetários. Formate os valores em reais na interface. Aplique as regras de cálculo e de snapshots históricos descritas em [Regras de negócio](business-rules.md).
- Use transações atômicas para gravações relacionadas, como concluir ou editar um atendimento com seus itens, partes do pagamento e snapshots, cancelar um atendimento ou confirmar e desfazer um pagamento semanal. Verifique o [bloqueio semanal definido no modelo de dados](database-design.md#apuração-e-fechamento-semanal) na mesma operação que altera o atendimento, de forma consistente com confirmações concorrentes. Garanta na persistência a unicidade da confirmação por barbeiro e semana, inclusive em tentativas simultâneas.
- A representação persistida deve registrar a data e a hora efetivas e o fuso usado no lançamento, com informação suficiente para reproduzir o dia e a semana históricos quando o aparelho mudar de fuso. Em consultas por intervalo, use início inclusivo e fim exclusivo. Para uma semana local, os limites são o início da segunda-feira e o início da segunda-feira seguinte.
- Use o Storage Access Framework (SAF) para escolher os arquivos do backup manual e da restauração. Restrinja o acesso aos arquivos selecionados, sem permissões amplas de armazenamento. Faça as verificações de integridade e compatibilidade antes da substituição. Disponibilize a base restaurada somente após a conclusão integral e preserve a base anterior em caso de falha.

## Decisões técnicas de implementação

- Validar a representação temporal do [modelo de dados](database-design.md#dicionário-de-dados) com as consultas reais e definir o tratamento das transições de fuso.
- Definir o formato e o versionamento do arquivo de backup, as verificações de integridade e compatibilidade e o mecanismo de substituição atômica da base. Revisar a configuração de backup do template no manifesto para alinhá-la ao escopo offline.
- Escolher versões das dependências de persistência, navegação e estado compatíveis com a API 24.

## Compatibilidade com API 24

Verifique o `minSdk` das versões resolvidas de novas dependências e revise APIs de plataforma com o lint `NewApi`. APIs introduzidas acima de 24 exigem alternativa compatível, proteção por versão ou desugaring quando suportado e configurado. Em particular, confirme o suporte a `java.time` antes de usá-lo para a semana de apuração. O projeto declara core library desugaring 2.1.5.

Ao implementar mudanças que afetem compatibilidade, rode o lint e valide os fluxos envolvidos em emulador ou aparelho com API 24.

