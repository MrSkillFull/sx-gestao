---
name: compose-ui
description: Implementa e revisa telas Jetpack Compose do SX Gestão. Use em formulários, navegação, componentes Material 3, estados de tela e acessibilidade.
---

# Interface Compose

- Reutilize o tema e os componentes Material 3. Organize a navegação no ponto de entrada da funcionalidade e passe estado e callbacks aos componentes de conteúdo.
- Colete o estado do ViewModel de forma compatível com o ciclo de vida, usando as APIs disponíveis nas dependências do projeto. Use estado local para detalhes de apresentação.
- Preserve a edição em formulários e apresente erros no campo correspondente. Mantenha o texto digitado durante a edição e converta dinheiro para centavos com precisão na validação.
- Exiba os estados pertinentes de vazio, carregamento, erro e operação em andamento. Impeça reenvios enquanto uma gravação estiver em curso. Consulte [Regras de negócio](../../../docs/business-rules.md) para pagamentos e comissões.
- Respeite os insets do `Scaffold`, o teclado e o aumento de fonte. Use rótulos, contraste e semântica que permitam operar a tela com TalkBack.

## Verificação

- [ ] O fluxo aceita, corrige e envia dados de acordo com as validações.
- [ ] Os campos e ações permanecem acessíveis com teclado e fonte ampliada.
- [ ] A navegação e o conteúdo foram verificados na API 24 quando afetados.
