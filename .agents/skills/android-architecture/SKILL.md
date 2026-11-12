---
name: android-architecture
description: Organiza funcionalidades Android do SX Gestão. Use ao criar ou refatorar pacotes, ViewModels, fluxo de estado, repositórios ou limites entre UI e dados.
---

# Arquitetura Android

Leia [Arquitetura](../../../docs/architecture.md) e compare a estrutura prevista com os pacotes existentes.

- Agrupe tela e ViewModel por funcionalidade em `ui/`. Centralize Room e repositórios em `data/`.
- Exponha estado imutável por `StateFlow`. Receba ações explícitas da tela no ViewModel e delegue acesso a dados ao repositório.
- Isole cálculos e validações financeiras reutilizáveis em funções Kotlin puras. Consulte [Regras de negócio](../../../docs/business-rules.md) antes de alterar esses contratos.
- Crie pacotes e abstrações à medida que surgirem responsabilidades concretas. Preserve os pontos de entrada e o comportamento dos fluxos existentes.

## Verificação

- [ ] UI, ViewModel e dados têm responsabilidades e direção de dependência claras.
- [ ] Regras financeiras podem ser executadas sem Compose ou Room.
- [ ] O fluxo opera offline e as APIs utilizadas respeitam a API 24.
