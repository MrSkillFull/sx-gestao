# SX Gestão

- Use Kotlin, Jetpack Compose e Room. Preserve o funcionamento offline dos fluxos do MVP e mantenha um único módulo Gradle, `:app`.
- Mantenha `minSdk = 24` (Android 7.0). Valide APIs de plataforma e requisitos de bibliotecas contra esse mínimo.
- Siga a arquitetura híbrida: UI e ViewModels por funcionalidade, dados e persistência centralizados. Consulte [Arquitetura](docs/architecture.md) antes de mudar responsabilidades, pacotes ou dependências.
- Represente dinheiro em centavos de real com `Long` no armazenamento e nos cálculos. Formate em reais na interface. Consulte [Regras de negócio](docs/business-rules.md) para o escopo do MVP e as regras de atendimentos e operações financeiras.
- Limite mudanças ao MVP solicitado. Preserve registros históricos e mantenha as entregas pequenas e verificáveis.
- Use as skills em `.agents/skills/` conforme a tarefa: arquitetura Android, Compose, Room ou testes.
