# F-21 Campo 0.1.13-dev

## R2.6 — snapshots históricos

- adicionada a entidade Room `occupation_snapshots`;
- adicionada a migration explícita V18→V19 com backfill dos dados existentes;
- Station, ReferencePoint, Receiver e Antenna usados na ocupação passam a ser preservados como fotografia histórica;
- o repositório captura os componentes progressivamente e não sobrescreve valores já capturados;
- `Continuar rastreio` reidrata a tela a partir do snapshot quando o cadastro atual tiver sido editado;
- testes de migration, fechamento/reabertura e fluxo Gate R2 foram ampliados;
- versão OPERATIONAL: `0.1.13-dev` (versionCode 14).

Ainda não fazem parte desta versão: protocolo Spectra, comandos remotos, PPA, parser RINEX completo ou teste físico concluído.
