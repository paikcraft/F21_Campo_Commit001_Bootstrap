# Development progress — F-21 Campo

## 2026-09-13 — baseline

- BLOCO: Baseline de desenvolvimento
- STATUS: PASS (diagnóstico)
- COMMIT: `7d2cca8` auditado; documentação deste baseline aguardando commit
- TESTES LOCAIS: não executados; nenhuma alteração de código
- GITHUB ACTIONS: último workflow do HEAD PASS
- ROOM VERSION: 15
- MIGRATION: V1→V15 registrada; schemas efetivos ainda ausentes
- ALTERAÇÕES: criado `DEVELOPMENT_BASELINE.md` com estado real, módulos, rotas, Room, CI, testes e lacunas
- LIMITAÇÕES: teste físico, Room migration/reopen, auditoria persistente e reidratação completa ainda pendentes
- PRÓXIMO BLOCO: Bloco 1 — READY, INICIAR e navegação básica, conforme aceite do prompt mestre

## 2026-09-13 — Bloco 1

- BLOCO: READY, INICIAR e navegação básica
- STATUS: PASS (código e Actions; teste físico ainda não executado)
- COMMIT: `c8a6a38`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`
- GITHUB ACTIONS: run `34758960061` PASS, SHA conferido
- ROOM VERSION: 15
- MIGRATION: não alterada
- ALTERAÇÕES: checklist de prontidão extraído para `OccupationReadiness`, com teste de pendências completas/incompletas; fluxo existente mantém READY, INICIAR e rotas HOME distintas
- LIMITAÇÕES: aceite físico do botão READY/INICIAR e recuperação ainda precisam ser confirmados no aparelho
- PRÓXIMO BLOCO: Bloco 2 — Room, schemas e migrations
