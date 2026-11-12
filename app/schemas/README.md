# Esquemas Room

O KSP exporta aqui o esquema oficial da versão 1 ao compilar `:app:assembleDebug`.
Versione o JSON gerado antes de distribuir o primeiro banco. Não há banco Room de
versão anterior nesta etapa, portanto não há migração inicial nem fallback destrutivo.
O JSON v1 está exportado. Na retomada de 11/11/2026 o build KSP e os dez testes
instrumentados de persistência passaram na API 24, incluindo reabertura,
restrições e rollback. Nenhuma mudança de esquema foi necessária nessa retomada.
