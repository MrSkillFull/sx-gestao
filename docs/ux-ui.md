# UX/UI do SX Gestão

## Direção e referências

O SX Gestão atende ao proprietário da barbearia em um aparelho Android. Os fluxos do MVP funcionam offline. O MVP usa exclusivamente o tema escuro, com laranja em destaque, independentemente da aparência configurada no Android. O tema claro fica para depois do MVP. O registro de atendimento ocorre em uma única tela rolável, organizada por seções. O Início prioriza os números do dia, a ação **Novo atendimento**, as comissões semanais e o resumo da semana.

O [SRS](srs.md) define o escopo e os critérios do produto. As [regras de negócio](business-rules.md) definem cálculos e validações, o [modelo de dados](database-design.md) define os valores históricos e a [arquitetura](architecture.md) orienta a implementação.

## Princípios e navegação

- **Agilidade no balcão.** Mantenha a ação principal visível, apresente os campos na ordem do lançamento e recalcule o resumo conforme a edição.
- **Valores legíveis.** Mostre o período junto de cada total. Diferencie o faturamento, a distribuição dos recebimentos e a comissão com rótulos próprios. Formate dinheiro em reais, com duas casas decimais e vírgula decimal.
- **Histórico confiável.** Exiba a data e hora efetivas e os valores aplicados. Identifique atendimentos cancelados, vínculos inativos e comissões confirmadas por texto.
- **Correção no contexto.** Informe o que precisa mudar perto do campo ou da ação, preserve os dados digitados se houver erro e deixe explícito o efeito das confirmações.

A barra inferior usa **Início**, **Atendimentos**, **Cadastros** e **Configurações**, nessa ordem, com ícone e rótulo. Preserve o estado de cada destino principal ao alternar abas. Use a barra superior com título e voltar nas telas internas. **Novo atendimento** no Início e em Atendimentos abre o mesmo formulário. O histórico fica em Atendimentos, a apuração de comissões é acessada pelo Início e o backup fica em Configurações.

## Início

Mostre a data, o faturamento líquido e a quantidade de atendimentos válidos do dia no primeiro bloco, com os recebimentos por forma logo abaixo. Posicione **Novo atendimento** junto aos indicadores, com o maior destaque entre as ações. Em seguida, mostre as comissões da semana por barbeiro, com valor e situação, e o resumo da semana com intervalo de segunda a domingo, faturamento líquido, parcela da barbearia e recebimentos por forma. **Parcela da barbearia** é o faturamento líquido da semana menos a soma das comissões apuradas na mesma semana, incluindo pendentes e confirmadas. O texto de apoio **Líquido da semana menos comissões apuradas** acompanha o valor. O indicador mede a receita após as comissões apuradas, antes de despesas. O status de pagamento fica na seção de comissões. Use a data efetiva para classificar os lançamentos conforme o SRS. Mostre valores zerados e uma mensagem contextual em períodos vazios.

```text
Início
Hoje · [data]
Faturamento líquido  [R$]   Atendimentos  [n]
[ Novo atendimento ]
Recebimentos por forma  [valores]

Comissões semanais · [segunda a domingo]
[barbeiro]  [R$]  [Pendente / Confirmada]  ›

Resumo da semana · [segunda a domingo]
Faturamento líquido  [R$]
Parcela da barbearia  [R$]
Líquido da semana menos comissões apuradas
Recebimentos por forma  [valores]

Início | Atendimentos | Cadastros | Configurações
```

## Registro e edição de atendimento

O formulário segue a ordem **Atendimento → Serviços → Pagamento → Resumo**. Todas as seções ficam na mesma tela. Mantenha o total líquido e a ação principal disponíveis junto ao rodapé quando houver espaço. Com teclado aberto ou texto ampliado, permita alcançar a ação pela rolagem e mantenha o campo em foco visível.

| Seção | Comportamento |
| --- | --- |
| Atendimento | Seleção de um barbeiro e campos editáveis de data e hora efetivas. Sugira a data e hora atuais no lançamento. Na edição, apresente os valores do registro e mantenha o fuso de lançamento ao corrigi-los. |
| Serviços | Mostre uma linha por ocorrência, com nome, preço aplicado, campo **Desconto (R$)**, valor líquido e ação de remoção identificável. **Adicionar serviço** abre a seleção de serviços ativos. O mesmo serviço pode ser adicionado novamente com desconto independente. |
| Pagamento | Para líquido positivo, apresente **Pagamento único** como padrão, com seleção de Pix, dinheiro, cartão de débito ou cartão de crédito e o valor integral preenchido pelo total. Em **Dividir pagamento**, cada parte tem forma, valor e ação de remoção. Mostre o total informado e o valor que falta ou excede. Com líquido zero, mostre **Atendimento gratuito** e conclua com zero partes. |
| Resumo | Mostre subtotal, desconto total e líquido. Use **Concluir atendimento** na inclusão e **Salvar alterações** na edição. Na edição, identifique o preço e a taxa aplicados quando apresentados, conforme as regras de snapshots. |

Ao alterar serviços ou descontos, atualize o total. No modo dividido, preserve os valores digitados enquanto o líquido for positivo e sinalize a diferença a corrigir. Se a mudança tornar o atendimento gratuito, informe que os pagamentos foram removidos do formulário. Aponte barbeiro ausente, falta de serviços, desconto fora do limite e pagamento inválido junto à seção correspondente. Ao tentar gravar com erros, leve o foco ao primeiro ponto a corrigir. As validações definitivas seguem o SRS e a persistência, inclusive quando o cadastro ou uma confirmação mudou durante a edição. Ao retornar de **Ver comissão**, siga o fluxo de revalidação descrito em [Comissões semanais](#comissões-semanais).

Ao voltar pela barra superior ou pelo sistema, ou acionar outra navegação que encerre a inclusão ou edição, compare o formulário ao estado inicial da inclusão ou ao registro carregado na edição. Se houver alterações ainda não salvas, inclusive entradas inválidas, mostre **Descartar alterações?**. **Continuar editando** fecha o diálogo e mantém as entradas. **Descartar alterações** encerra o formulário e abandona o rascunho. Sem mudanças ou após gravação bem-sucedida, saia normalmente. **Ver comissão** é uma consulta dentro do fluxo e mantém o rascunho.

```text
‹ Novo atendimento
Atendimento
[ Barbeiro ▾ ]   [ Data efetiva · dd/mm/aaaa ]   [ Hora efetiva · hh:mm ]

Serviços
[Corte · R$ 30,00]   [ Desconto R$ 5,00 ]   R$ 25,00
[Corte · R$ 30,00]   [ Desconto R$ 30,00 ]  R$  0,00
[ + Adicionar serviço ]

Pagamento
[ Único ]  [ Dividido ]
[ Forma ▾ ]   R$ 25,00

Resumo
Subtotal R$ 60,00 · Descontos R$ 35,00
Total R$ 25,00   [ Concluir atendimento ]

Ao sair com alterações (inclusão ou edição)
Descartar alterações?
As alterações deste atendimento serão perdidas.
[ Continuar editando ]   [ Descartar alterações ]
```

## Histórico e cancelamento

Em **Atendimentos**, liste registros com data e hora efetivas, barbeiro, líquido e situação. Ao abrir um registro, mostre os serviços e seus preços e descontos aplicados, a taxa de comissão registrada e as partes do pagamento ou **Gratuito**. Identifique vínculos inativos no detalhe. A edição permite manter um vínculo inativo existente e oferece cadastros ativos para novas associações. Siga as regras de snapshots ao manter ou trocar itens e barbeiro.

Para atendimento válido, ofereça **Editar** e **Cancelar atendimento**. Antes de cancelar, confirme identificando barbeiro, data e valor. Informe que o registro ficará no histórico como cancelado e sairá dos totais. Use **Voltar** e **Cancelar atendimento** no diálogo. Após gravar, mostre o resultado no detalhe e atualize as listas e totais afetados. O detalhe de um cancelado é somente leitura.

```text
Atendimentos  [ Novo atendimento ]
[data · hora]  [barbeiro]  [R$]  [Registrado / Cancelado]  ›
    Detalhe · data efetiva, itens, taxa aplicada e pagamentos
    [ Editar ]  [ Cancelar atendimento ]
```

## Comissões semanais

A partir do Início, permita consultar a apuração por semana, com a semana atual em destaque. Ao abrir uma apuração, mostre o detalhe por barbeiro e intervalo de segunda a domingo. Exiba o valor apurado e **Pendente** ou o valor confirmado, **Confirmada** e a data e hora da confirmação. A ação **Confirmar pagamento** registra a quitação integral. **Desfazer confirmação** abre diálogo com barbeiro, intervalo e valor, informando que a comissão voltará a ficar pendente e que os atendimentos e recebimentos serão preservados.

Quando uma confirmação impedir um lançamento retroativo, uma edição ou um cancelamento, mostre o motivo e identifique todos os pares de barbeiro e semana afetados. Na edição, considere origem e destino e indique ambos quando forem distintos e estiverem confirmados. O mesmo feedback vale para comissão zero ou inalterada. Ofereça **Ver comissão** para acessar o detalhe correspondente.

Ao voltar ao formulário de inclusão ou edição, com ou sem desfazimento da confirmação, mantenha todas as entradas e revalide o atendimento completo com os dados atuais do banco. Reavalie vínculos, preços e taxas aplicáveis conforme as regras de snapshots, data e hora, descontos, totais, pagamentos e confirmações semanais aplicáveis. Atualize o resumo e os avisos sem sobrescrever entradas. Se houver pendência, indique a correção no campo ou na seção correspondente e mantenha o rascunho. Aguarde o proprietário tocar em **Concluir atendimento** ou **Salvar alterações**. No envio, a [validação transacional](database-design.md#conclusão-edição-e-cancelamento) verifica novamente o estado vigente antes de gravar, inclusive se houver nova confirmação depois do retorno. A reconfirmação ocorre no detalhe semanal, com o valor atualizado.

```text
‹ Comissão semanal
[barbeiro] · [segunda a domingo]
[R$]   [Pendente / Confirmada]
[Confirmada em data e hora, quando aplicável]
[ Confirmar pagamento / Desfazer confirmação ]

No atendimento bloqueado
Comissão confirmada para [barbeiro], de [data] a [data].
Desfaça a confirmação para continuar.   [ Ver comissão ]

Ao voltar ao formulário
Entradas preservadas · [resumo e avisos revalidados]
[ Concluir atendimento / Salvar alterações ]
```

## Cadastros

Em **Cadastros**, agrupe **Barbeiros** e **Serviços**. Mostre nome, situação e taxa ou preço na lista. Ofereça as ações de cadastrar, editar e inativar no contexto de cada grupo. Identifique o proprietário e exiba a taxa fixa como somente leitura. Para os demais barbeiros, sugira 50% na criação e mostre a faixa e a precisão aceitas junto ao campo de taxa quando necessário. Na inativação, informe que o cadastro sairá das novas seleções e continuará associado aos atendimentos anteriores.

```text
Cadastros
[ Barbeiros ] [ Serviços ]
[ + Novo barbeiro / Novo serviço ]
[nome]  [taxa ou preço]  [Ativo / Inativo]  ›
    Detalhe  [ Editar ]  [ Inativar ]
```

## Configurações e backup

- **Gerar backup.** Abra o seletor de arquivos do Android para escolher o destino. Após a gravação, confirme a conclusão. Se houver falha, informe o problema e ofereça nova tentativa.
- **Restaurar backup.** Abra o seletor do Android, identifique o arquivo escolhido e mostre **Validando arquivo…**. Se a validação falhar, mostre a causa disponível e permita escolher outro arquivo. Com um arquivo validado, apresente a confirmação abaixo.
- Durante a restauração, mostre **Restaurando backup…** e impeça operações concorrentes sobre os dados. Após sucesso, atualize as telas a partir da base restaurada. Em caso de falha, informe que os dados anteriores foram preservados. Fechar o seletor ou escolher **Voltar** no diálogo preserva os dados locais.

```text
Configurações
Backup  [ Gerar backup ]  [ Restaurar backup ]
    Seletor do Android → Validação → Confirmação

Restaurar backup?
O arquivo [nome] substituirá todos os dados atuais do SX Gestão.
[ Voltar ]   [ Substituir dados ]
```

## Entrada e formatação de campos

Use os mesmos padrões de entrada em atendimentos e cadastros. Mantenha rótulos visíveis e informe o formato esperado no texto de apoio quando necessário.

| Campo | Teclado e formato pt-BR | Validação |
| --- | --- | --- |
| Dinheiro | Teclado numérico decimal e prefixo fixo `R$`. Aceite vírgula decimal e até duas casas durante a edição. Exiba valores válidos com duas casas e separador de milhar, como `R$ 1.234,56`. | Indique formato inválido, precisão excedida ou valor fora do intervalo permitido para o campo. Aplique os limites de preço, desconto e pagamento definidos nas regras de negócio. |
| Percentual | Teclado numérico decimal e sufixo fixo `%`. Aceite até duas casas com vírgula decimal, como `33,33%`. | Aplique a faixa e a precisão do RF-01. Mantenha a taxa do proprietário somente leitura. |
| Data e hora | Teclado numérico na edição direta, com indicação `dd/mm/aaaa` para data e `hh:mm` em 24 horas para hora. Ofereça também seletores de data e hora. | Verifique preenchimento, data existente no calendário e hora válida. Aplique as regras de data e hora efetivas e preserve o fuso de lançamento na edição. |

Ao receber foco, permita editar o valor sem separadores de milhar ou símbolos monetários e percentuais no texto digitável. Mantenha entradas parciais enquanto a pessoa digita. Ao sair do campo, normalize valores válidos e mostre erro junto ao campo se a entrada estiver inválida ou incompleta, preservando o que foi digitado. Rejeite precisão excedida sem arredondar ou truncar. Use **Próximo** para avançar na ordem visual e **Concluir** no último campo para fechar o teclado. No envio explícito do formulário, leve o foco ao primeiro campo ou controle que exige correção.

Preserve a representação do [modelo de dados](database-design.md#dicionário-de-dados). Dinheiro usa `Long` em centavos e taxas usam inteiros em pontos-base. Por exemplo, `R$ 12,30` corresponde a `1230` centavos e `33,33%` corresponde a `3333` pontos-base.

## Componentes e tokens

Reutilize o esquema escuro e a navegação da fundação Compose descrita na arquitetura. Use os componentes abaixo como referência de implementação, sujeitos à compatibilidade com a API 24.

| Uso | Compose e Material 3 |
| --- | --- |
| Estrutura e navegação | `Scaffold`, `TopAppBar`, `NavigationBar` e `NavigationBarItem`. |
| Painel e listas | `Card` para grupos de indicadores, `ListItem` para registros e `LazyColumn` para listas extensas. |
| Formulários | `OutlinedTextField` com rótulo, texto de apoio, estado de erro e prefixo ou sufixo conforme o campo, menu de seleção, seletores de data e hora, opção segmentada para pagamento e controles de inclusão e remoção por item. |
| Ações e estados | `Button` para ação principal, `OutlinedButton` ou `TextButton` para ações secundárias, `AlertDialog` para confirmações, `SnackbarHost` para retornos breves e indicador de progresso para operações em curso. |

Valores HEX **provisórios**, para protótipo. Use papéis semânticos do `ColorScheme` e valide contraste nos estados interativos do tema escuro.

| Papel | Escuro do MVP |
| --- | --- |
| `primary` / `onPrimary` | `#FF9A52` / `#231100` |
| `background` / `onBackground` | `#121212` / `#F4F1EF` |
| `surface` / `onSurface` | `#1D1D1D` / `#F4F1EF` |
| `onSurfaceVariant` / `outline` | `#CFC4BC` / `#998E86` |
| `error` / `onError` | `#FFB4AB` / `#690005` |

Use a família padrão do tema Material 3 como ponto de partida. Aplique `headlineMedium` (28 sp) ao faturamento principal, `titleLarge` (22 sp) aos títulos de tela, `titleMedium` (16 sp) aos títulos de seção, `bodyLarge` (16 sp) a campos e valores e `bodyMedium` (14 sp) a apoio e erros. Use `labelLarge` (14 sp) nas ações. Permita que texto e valores quebrem linha ou passem para uma coluna com fonte ampliada.

## Acessibilidade, estados e feedback

- Garanta áreas de toque de pelo menos 48 dp. Mantenha ordem de foco alinhada à ordem de leitura, títulos de seção reconhecíveis e rótulos persistentes nos campos. Dê nomes específicos a ações por linha, como **Remover segundo item Corte**.
- Use texto para distinguir estados e erros, com ícones e cor como reforço. Verifique contraste de texto, foco e limites dos controles no tema escuro. Com TalkBack, anuncie erros de envio, bloqueios e resultados importantes. Evite anunciar cada alteração enquanto o proprietário digita.
- Respeite áreas do sistema e teclado. Verifique rolagem, reflow, foco em campos com erro e acesso a ações com o tamanho de fonte do Android ampliado.
- **Carregamento e gravação.** Mostre progresso durante leitura inicial e operações demoradas. Enquanto houver uma gravação em curso, desabilite reenvios. Mostre sucesso após o retorno da operação e atualize o conteúdo com os dados persistidos.
- **Vazio.** No histórico, use **Nenhum atendimento registrado** com acesso a **Novo atendimento**. Em cadastros, ofereça a criação do primeiro registro. Na seleção sem opções ativas, indique o cadastro necessário. Em apurações vazias, informe o período sem movimento.
- **Erro e recuperação.** Use mensagem junto ao campo para validação, mensagem persistente na seção para bloqueios e opção de tentar novamente para falhas de leitura ou gravação recuperáveis. Preserve a edição em caso de falha. Use mensagens breves de sucesso para operações concluídas e mantenha a situação atual visível na tela.

## Aceite UX e rastreabilidade

Os casos abaixo complementam os [critérios funcionais](srs.md#requisitos-funcionais-e-critérios-de-aceite) e [não funcionais](srs.md#requisitos-não-funcionais-e-critérios-de-aceite) do SRS.

| Caso de aceite | Referência |
| --- | --- |
| No Início, identificar o dia, o líquido, a ação **Novo atendimento**, as comissões e o resumo semanal. Após um registro retroativo com pagamento dividido, localizar o total e as formas no período efetivo, inclusive após troca do fuso do aparelho. | RF-03, RF-04, RF-06, RF-07 |
| No resumo semanal, conferir **Parcela da barbearia** de R$ 70,00 quando o líquido é R$ 100,00 e as comissões apuradas somam R$ 30,00. Confirmar e desfazer um pagamento de comissão e conferir que a parcela permanece R$ 70,00. Em semana sem movimento, conferir R$ 0,00. | RF-06, RF-07, RF-08 |
| Em uma tela, registrar dois serviços iguais com descontos independentes. Quitar R$ 25,00 com uma forma ou R$ 10,00 + R$ 15,00. Corrigir soma divergente com ajuda da mensagem e concluir um atendimento gratuito sem partes. | RF-03, RF-04, RNF-03, RNF-05 |
| Na inclusão e na edição, sair sem mudanças e conferir a saída direta. Alterar um campo e tentar sair pela barra superior e pelo sistema. Escolher **Continuar editando** e conferir as entradas. Repetir e escolher **Descartar alterações**, conferindo que o rascunho foi abandonado e a base permanece intacta. | RF-03, RF-05 |
| Conferir teclado, formatos pt-BR e normalização ao perder o foco. Aceitar R$ 12,30, 33,33% e 29/02/2028. Rejeitar três casas decimais, 100,01% e 31/02/2028. Conferir preservação da entrada inválida, mensagem no campo, foco no primeiro erro ao enviar e unidades persistidas para os valores válidos. | RF-01, RF-03, RF-04, RNF-03, RNF-05 |
| No histórico, conferir preços, descontos e taxa aplicados após mudanças de cadastro. Editar um registro válido, manter vínculos inativos existentes e usar cadastros ativos em novas associações. Recusar o cancelamento e verificar o registro preservado. Confirmá-lo e verificar o rótulo **Cancelado** e a atualização dos totais. | RF-01, RF-02, RF-05, RF-06, RNF-06 |
| Confirmar a comissão e conferir valor e instante. Tentar lançar retroativamente, editar ou cancelar em semana confirmada, inclusive com comissão zero, edição só do pagamento e origem e destino bloqueados. Identificar cada confirmação, desfazer após consentimento, retornar à operação e reconfirmar a apuração atualizada. | RF-05, RF-07, RF-08, RNF-04, RNF-06 |
| Abrir **Ver comissão** a partir da inclusão ou edição bloqueada. Voltar sem desfazer e conferir rascunho e bloqueio. Desfazer e voltar após uma mudança de cadastro que afete uma nova associação. Conferir entradas preservadas, validação integral e ausência de gravação automática. Se houver nova confirmação entre o retorno e o envio, conferir que a gravação fica bloqueada e o rascunho permanece. | RF-02 a RF-05, RF-08, RNF-04 |
| Criar e editar cadastros, identificar a taxa fixa do proprietário e corrigir uma taxa inválida. Após inativar um cadastro, conferir a ausência nas novas seleções e a identificação no histórico. | RF-01, RF-02 |
| Fazer backup offline pelo seletor do Android. Rejeitar arquivo inválido ou incompatível, recusar a substituição e simular falha após confirmar, preservando a base anterior. Restaurar um arquivo válido e conferir que as telas passam a mostrar os dados restaurados após a conclusão. | RF-09, RNF-01, RNF-04, RNF-06 |
| Percorrer as quatro abas e os fluxos principais em Android API 24, com TalkBack, fonte ampliada e tema escuro. Alternar a aparência do Android e conferir que o app permanece escuro. Conferir foco, mensagens, acesso às ações com teclado aberto e feedback durante gravação, inclusive em toque repetido. | RNF-02, RNF-04, RNF-05 |

## Pontos visuais em aberto

- Validar a paleta final, incluindo estados de foco, seleção e desabilitado, no tema escuro.
- Validar a densidade dos indicadores do Início e das linhas de serviço e pagamento em telas estreitas e com fonte ampliada.
- Refinar a iconografia e a apresentação do resumo do formulário com o teclado aberto após avaliação em protótipo.
