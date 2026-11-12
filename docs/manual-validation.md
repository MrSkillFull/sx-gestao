# Validação manual no Android Studio

Use um emulador ou instalação de teste sem dados de operação. Restaurar substitui
toda a base após confirmação explícita.

## Preparar

Abra a raiz no Android Studio, sincronize pelo wrapper, disponibilize SDK 37 e
um AVD API 24. Não altere o mínimo 24. Execute:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest --rerun-tasks
```

Confira XMLs novos em `app/build/test-results/testDebugUnitTest` e
`app/build/outputs/androidTest-results/connected/debug`. Exija os testes
esperados, sem falhas/ignorados: zero testes não valida. Execute `app` e desligue
a rede. Para backup, escolha um provedor de arquivos locais pelo SAF.

## Roteiro

1. Crie proprietário (0% fixo), outro barbeiro (50% inicial) e serviços. Aceite
   33,33%, rejeite 33,333% e 100,01%. Edite e inative preservando históricos.
2. Abra Novo atendimento pelo Início e por Atendimentos. Lance duas ocorrências
   de serviço a R$ 30,00 com descontos R$ 5,00 e R$ 30,00. Confira líquido
   R$ 25,00 e quite com Pix R$ 10,00 + débito R$ 15,00. Rejeite soma divergente,
   partes não positivas, desconto acima do preço e três casas decimais.
3. Aplique desconto integral: atendimento gratuito sem pagamento. Retorne a
   líquido positivo e confira nova quitação. Teste seletores de data/hora,
   31/02/2028 inválido e 29/02/2028 válido. Tente sair com entrada inválida pela
   barra e pelo sistema; teste Continuar editando e Descartar alterações.
4. Confira preços, descontos, taxa e recebimentos no detalhe. Mude cadastros e
   confira snapshots antigos. Edite mantendo vínculos inativos; novas associações
   exigem ativos e preços/taxas vigentes. Recuse e confirme cancelamento: confira
   somente leitura, histórico preservado e retirada dos totais.
5. Lance retroativamente no domingo e na segunda à meia-noite. Confira semanas
   e formas. Troque o fuso do aparelho: dia e semana históricos permanecem.
6. Para líquido semanal R$ 100,00 e comissão R$ 30,00, confira parcela R$ 70,00
   antes/depois de confirmar/desfazer. Semana vazia deve mostrar zero. Consulte
   outras semanas pelos botões e pelo seletor.
7. Confirme duas vezes e confira valor/instante originais. Tente retroativo,
   edição só de pagamento e cancelamento, inclusive comissão zero e origem/destino
   confirmados. Confira todos os bloqueios e Ver comissão. Volte sem desfazer e
   confira rascunho; desfaça, retorne, revalide e envie explicitamente. Nenhuma
   volta pode gravar automaticamente. Reconfirme o total vigente.
8. Gere backup por SAF, altere a base e selecione o arquivo. Recuse e confira
   preservação. Selecione de novo, aguarde validação e confirme. Confira toda a
   base e reabra o app. Arquivos corrompidos/incompletos devem ser rejeitados
   antes da substituição. Falha e interrupção transacionais têm testes automáticos.
9. Ative TalkBack e confira leitura, foco, nomes das ações, erros, confirmações
   e resultados. Amplie a fonte do Android a 200%, abra o teclado e confira
   rolagem e foco no primeiro erro. Alterne aparência clara/escura do sistema:
   o app permanece escuro em preto, laranja e neutros. Avalie contraste.

O teste automatizado de fonte ampliada não substitui avaliação humana de TalkBack
e contraste. O teste de confirmação de restore usa URI privada; percorra também
o seletor SAF externo. Repita os fluxos na API 24 e demais APIs disponíveis.
