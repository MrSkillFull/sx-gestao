---
name: android-testing
description: Valida o comportamento do SX Gestão com testes Android. Use em cálculos financeiros, regras de semana, persistência, formulários Compose ou regressões de compatibilidade.
---

# Testes Android

- Leia [Regras de negócio](../../../docs/business-rules.md) para definir resultados esperados. Use JUnit em `app/src/test` para cálculos e validações puras, com datas e valores explícitos.
- Cubra descontos por item, parcelas que fecham ou divergem do total e os limites entre domingo e segunda, incluindo virada de mês ou ano. Use os critérios aprovados de comissão e arredondamento.
- Use `app/src/androidTest` para DAOs, transações, migrações e comportamento Compose que dependa do Android. Verifique a confirmação repetida e o desfazimento do pagamento semanal quando esses fluxos forem alterados.
- Nos testes de tela, acione controles pela semântica e confira o resultado observável, como erro de validação ou retorno ao estado pendente.

## Verificação

- [ ] O teste observa a regra ou regressão e usa dados independentes da hora atual.
- [ ] Rode as tarefas aplicáveis com o wrapper, como `gradlew.bat :app:testDebugUnitTest`, `gradlew.bat :app:lintDebug` e, com dispositivo disponível, `gradlew.bat :app:connectedDebugAndroidTest`.
- [ ] Verifique mudanças sensíveis à versão em API 24 e registre o que foi executado e o que ficou pendente.
