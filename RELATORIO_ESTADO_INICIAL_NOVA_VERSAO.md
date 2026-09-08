# Estado inicial — Nova versão F-21 Campo

Data: 2026-09-07

## Base

- Repositório: `F21_Campo_Commit001_Bootstrap`
- HEAD inicial: `283ebea`
- Versão encontrada: `0.1.0-dev`, `versionCode = 1`
- Módulos: app, data, domain, files, f21, hnproject, processing, receiver-api, receiver-manual, rinex

## Room

- Room: `2.8.4`
- Banco: versão 1
- Entidades Room atuais: Project e Station
- Schema exportado durante o CI
- Migrations explícitas: ainda não há migration de upgrade; existe apenas declaração de baseline
- `fallbackToDestructiveMigration`: não encontrado

## Estado funcional

- Domain primitives: implementadas
- Entidades de domínio: implementadas
- State machine: implementada
- Receiver/Antenna manual: implementados
- Alturas: modelo inicial implementado; precisa aceitar 1..N
- Eventos/auditoria: base implementada
- RawFileStore: implementado com SHA-256 e deduplicação
- UI: protótipo funcional concentrado em uma tela longa; precisa ser reorganizado em etapas
- Autosave/Continuar rastreio: ainda não concluídos
- ReferencePoint: ainda não existe como entidade própria

## Validação existente

- GitHub Actions já validou builds e testes de commits anteriores.
- Testes físicos anteriores validaram o bootstrap, cadastro de estação e flavors.
- O Gate R2 completo ainda não foi executado no fluxo final.

## Decisão de versão

- A base não é `0.2.0`; é `0.1.0-dev`.
- Para esta evolução será usada `0.1.1-dev`, com `versionCode = 2`, evitando forçar uma versão inconsistente.

## Pendências que não serão inventadas

- protocolo Spectra;
- arquivo específico produzido pelo receptor;
- formato obrigatório RN/MT/PA;
- quantidade fixa de alturas;
- PPA ou indicador equivalente;
- regra normativa de aceitação.
