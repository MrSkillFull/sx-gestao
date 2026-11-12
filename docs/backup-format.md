# Backup manual v1

Compatibilidade: formato `SXGESTAO`, `versaoFormato = 1`, `versaoBanco = 1`.
O app v1 aceita somente essa combinação. Não há migração nem fallback destrutivo.
Arquivos são escolhidos exclusivamente por SAF, sem permissões amplas.
Backup automático e transferência automática de dados estão desabilitados.

## Arquivo

Envelope JSON UTF-8 com exatamente `formato`, `versaoFormato`, `versaoBanco`,
`payload` e `sha256`. Payload é uma string contendo um objeto JSON com seis
tabelas. O checksum SHA-256 hexadecimal minúsculo é calculado sobre os bytes
UTF-8 exatos dessa string. É uma verificação de corrupção, não uma assinatura.

Cada tabela é um array de linhas; cada linha é um array com comprimento fixo.
Strings, inteiros e booleanos têm tipos estritos, sem conversões de texto para
número. Inteiros monetários permanecem em centavos. A ordem das colunas é:

| Tabela | Colunas |
| --- | --- |
| barbeiros | id, nome, ativo, proprietario, taxaComissaoBp |
| servicos | id, nome, ativo, precoCentavos |
| atendimentos | id, barbeiroId, efetivoUtcMs, fusoId, offsetEfetivoSegundos, diaLocal, semanaInicioLocal, taxaAplicadaBp, cancelado |
| itens | id, atendimentoId, servicoId, precoAplicadoCentavos, descontoCentavos |
| pagamentos | id, atendimentoId, forma, valorCentavos |
| confirmacoes | barbeiroId, semanaInicioLocal, valorConfirmadoCentavos, confirmadoEmUtcMs |

## Integridade e substituição

Exportação captura as seis tabelas na mesma transação Room. Validação lê uma
cópia isolada em memória e confere estrutura completa, versão, checksum, tipos,
UUIDs canônicos, unicidade, vínculos, domínios e regras dos agregados. Classificação
temporal é conferida pelo instante e offset históricos, sem reaplicar regras
atuais do fuso. Snapshots não são comparados aos preços/taxas vigentes. Cada
confirmação deve igualar sua apuração, com arredondamento por atendimento.

A confirmação substitui exatamente o candidato validado, sem reabrir o arquivo.
Exclusões e inserções ocorrem em uma única transação no banco existente, na ordem
das FKs. O mecanismo de commit/rollback do SQLite protege também contra
interrupção antes do commit; não se copiam arquivos SQLite ou WAL abertos.
Falha não confirma a transação. Não há alteração do esquema v1.

`EstadoBase` bloqueia novas escritas durante restauração, mostra um diálogo modal
e muda a geração da navegação somente após o commit, descartando telas e
rascunhos vinculados à base substituída. As novas telas começam carregando os
dados restaurados. Itens e pagamentos são expostos pelo repositório em ordem
canônica de UUID, independentemente da ordem física das linhas SQLite.
