# Regras de negócio do SX Gestão

## Escopo do MVP

O SX Gestão é um app offline para o proprietário operar a barbearia. O proprietário é o único operador e usa o app sem autenticação. O MVP inclui cadastros de barbeiros e serviços, registro e histórico de atendimentos, recebimentos, faturamento diário e semanal, apuração e pagamento de comissões e backup manual.

Ficam fora do MVP o cadastro de clientes, agendamento, despesas, taxas de cartão, fechamento diário formal, pagamento parcial de comissão, relatórios PDF ou CSV, operação multiusuário, uso em múltiplos dispositivos e nuvem.

## Cadastros e atendimentos

- A taxa de comissão do barbeiro proprietário é fixa em 0%. Para os demais barbeiros, a taxa inicial é 50% e pode ser configurada entre 0% e 100%, inclusive, com até duas casas decimais.
- A inativação de barbeiros e serviços os retira das opções para novos atendimentos e preserva os registros históricos.
- Cada atendimento tem um único barbeiro e um ou mais itens de serviço. Um mesmo serviço pode aparecer em vários itens.
- Cada item pode ter um desconto fixo em reais, de zero até o preço do item, inclusive. O valor líquido do atendimento é a soma dos preços dos itens após seus respectivos descontos.
- Guarde os valores efetivamente aplicados em cada item e a taxa de comissão aplicada ao atendimento em snapshots históricos. Alterações de preço ou taxa no cadastro valem para novos registros e preservam os valores históricos.
- Na correção de um atendimento válido, preserve o preço aplicado aos itens mantidos e a taxa aplicada quando o barbeiro for mantido. Use o preço vigente para itens incluídos ou substituídos e a taxa vigente ao trocar de barbeiro. Cadastros inativos já vinculados podem ser mantidos na correção. Para novas associações, use cadastros ativos. Valide novamente os descontos e a quitação pelo líquido corrigido.
- O cancelamento é lógico. O atendimento permanece no histórico como cancelado e sai dos totais de faturamento, da distribuição por forma de pagamento e da apuração de comissão.

## Recebimentos e faturamento

- Aceite Pix, dinheiro, cartão de débito e cartão de crédito. O padrão é um único pagamento, com opção de dividir o total em partes do pagamento por forma de pagamento.
- Em atendimento com valor líquido positivo, todas as partes devem ser positivas e a soma deve ser exatamente igual ao valor líquido. O atendimento é quitado integralmente, sem recebimento parcial ou parcelamento futuro.
- Atendimento gratuito é permitido e fica sem pagamento.
- Use a data e a hora efetivas do atendimento para o faturamento diário e semanal e para determinar a semana de comissão. Use o fuso local do aparelho registrado no lançamento para classificar o atendimento em todas as consultas históricas. Preserve esse fuso também ao corrigir a data ou a hora efetivas.
- Conte o valor líquido uma única vez por atendimento e use os valores das partes do pagamento para distribuir o faturamento por forma de pagamento. Recebimentos de atendimentos e pagamentos de comissão são registros distintos.

## Comissões e pagamento semanal

- Calcule a comissão sobre o valor líquido total do atendimento, usando a taxa guardada no snapshot. Arredonde uma vez por atendimento ao centavo mais próximo, com meio centavo para cima (`HALF_UP`).
- Apure a comissão de cada barbeiro somando as comissões dos seus atendimentos válidos na semana civil local, de segunda-feira a domingo.
- A confirmação registra o pagamento integral do valor apurado e o instante da confirmação para um barbeiro em uma semana. Repetir a confirmação preserva o registro original, incluindo valor e instante.
- Desfazer a confirmação devolve o pagamento ao estado pendente e preserva os atendimentos e seus recebimentos. Bloqueie lançamento retroativo em semana confirmada para o barbeiro e qualquer correção, edição ou cancelamento de atendimento associado a uma confirmação, inclusive quando a comissão é zero ou permanece igual. Na edição, verifique os pares de barbeiro e semana de origem e de destino. Desfaça todas as confirmações que bloqueiam a operação antes de realizá-la. Depois, o proprietário pode reconfirmar o valor integral apurado e registrar o novo instante.

Ver [RF-08 no SRS](srs.md#requisitos-funcionais-e-critérios-de-aceite) e [Apuração e fechamento semanal no modelo de dados](database-design.md#apuração-e-fechamento-semanal).

## Backup e restauração

O proprietário inicia o backup manualmente e escolhe o arquivo pelo seletor do Android. A restauração exige um arquivo íntegro e compatível e a confirmação explícita do proprietário antes de substituir a base inteira. Se a operação falhar, preserve a base anterior.
