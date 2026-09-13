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

## 2026-09-13 — Bloco 2

- BLOCO: Room, schemas e migrations
- STATUS: PASS (testes unitários, instrumentados e Actions; teste físico ainda não executado)
- COMMITS: `b1abb49` → `48a31c7` → `acf97d7` → `1f60b43` → `2bcb6eb` → `c9d2141` → `0188e82` → `b1e98df`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`
- GITHUB ACTIONS: run `34762184701` PASS, SHA `b1e98df` conferido; unitários, instrumentados Room/reabertura e build operationalDebug concluídos
- ROOM VERSION: 15
- MIGRATION: `Migrations.ALL` explícita, cadeia V1→V15 registrada e testada; nenhum destructive migration
- ALTERAÇÕES: schemas Room exportados/versionados em `data/schemas`, teste de cadeia de migrations, teste instrumentado de upgrade V1→V15 preservando dados, teste instrumentado de fechamento/reabertura e artefato de schemas no CI; runner ajustado para perfil válido e KVM
- LIMITAÇÕES: a cadeia testada começa no schema V1 sintético disponível; teste físico e reidratação completa do fluxo ainda pendentes
- PRÓXIMO BLOCO: Bloco 3 — Project, Station e ReferencePoint reais

## 2026-09-13 — Bloco 3

- BLOCO: Project, Station e ReferencePoint reais
- STATUS: PASS (código, testes instrumentados e Actions; teste físico ainda não executado)
- COMMITS: `d687aa0` → `ea3621f`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`
- GITHUB ACTIONS: run `34763362862` PASS, SHA `ea3621f` conferido; unitários, teste instrumentado de identidade/arquivamento, migration/reopen e build operationalDebug concluídos
- ROOM VERSION: 15
- MIGRATION: nenhuma alteração de schema; arquivamento usa `archivedAtEpochMillis` existente
- ALTERAÇÕES: DAOs e repository passaram a consultar ativos, pesquisar por identidade, preservar IDs em edição e arquivar sem apagar; tela de Projetos ganhou criação/listagem/pesquisa/edição/arquivamento; Banco de Estações ganhou criação/listagem/pesquisa/edição/município explícito/arquivamento/histórico; referências RN/MT/PA são reutilizadas por estação+tipo+código; DRAFT não é gravado antes de Projeto e Estação reais existirem
- LIMITAÇÕES: fluxo de recuperação completa e autosave ainda pertencem ao Bloco 4; teste físico continua pendente
- PRÓXIMO BLOCO: Bloco 4 — autosave e reidratação completa
