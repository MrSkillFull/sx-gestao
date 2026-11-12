---
name: room-persistence
description: Implementa e revisa persistência local com Room no SX Gestão. Use em entidades, DAOs, relações, consultas, transações, esquema e migrações.
---

# Persistência Room

- Confira o Gradle e o catálogo antes de usar APIs do Room ou do KSP. Consulte [Arquitetura](../../../docs/architecture.md) para o estado da configuração.
- Modele relações com chaves estrangeiras, índices conforme as consultas e restrições de unicidade onde a regra exigir. Consulte [Regras de negócio](../../../docs/business-rules.md) antes de definir colunas, totais e limites semanais.
- Use `Flow` para consultas observáveis e operações suspensas para gravações. Exponha operações ao restante do app pelo repositório.
- Grave atendimento, itens e parcelas em uma única transação. Proteja a confirmação e o desfazimento semanal contra estado parcial e confirmações duplicadas.
- Ao mudar um esquema em uso, preserve dados com migração versionada, exporte o esquema e verifique a migração com registros representativos da versão anterior.

## Verificação

- [ ] Uma falha de gravação não deixa itens, parcelas ou estado semanal parciais.
- [ ] Consultas e restrições preservam o histórico, os centavos e os limites da semana.
- [ ] Esquema e migração foram verificados com dados existentes quando aplicável.
