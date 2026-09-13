# F-21 Campo 0.1.14-dev

## R2.8 — DRAFT persistente

- `Occupation` aceita Project/Station nulos enquanto o operador ainda está nas primeiras etapas;
- o novo rastreio grava imediatamente um DRAFT no Room;
- a migration V19→V20 torna essas relações opcionais sem apagar dados existentes;
- reabertura de DRAFT incompleto foi coberta por teste instrumentado;
- exportação/importação JSON aceita registros incompletos;
- o restante da reidratação completa (process death em ACTIVE) continua em andamento.
