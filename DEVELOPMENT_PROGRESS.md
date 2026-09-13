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

## 2026-09-13 — Bloco 4

- BLOCO: autosave e reidratação completa
- STATUS: PASS (persistência/reidratação automatizada e Actions; process death físico ainda não executado)
- COMMIT: `36bf227`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`
- GITHUB ACTIONS: run `34764002452` PASS, SHA `36bf227` conferido; unitários, instrumentados e build operationalDebug concluídos
- ROOM VERSION: 15
- MIGRATION: nenhuma alteração; dados existentes preservados
- ALTERAÇÕES: DRAFT só é autosalvo após Projeto/Estação reais; mudanças da ocupação são persistidas pelo estado; `Continuar rastreio` reidrata projeto, estação, município, referência, equipamento manual, duração, estado, timestamps, alturas/unidades, eventos, RAW e SHA-256; teste de fechamento/reabertura verifica ocupação e evidências
- LIMITAÇÕES: snapshots históricos ainda usam os campos de equipamento persistidos na ocupação e IDs reconstruídos no mapper; snapshots dedicados pertencem ao Bloco 6; confirmação física de fechar/remover dos recentes permanece pendente
- PRÓXIMO BLOCO: Bloco 5 — UI operacional com cara de campo

## 2026-09-13 — Bloco 5

- BLOCO: UI operacional com cara de campo
- STATUS: PASS (fluxo/rotas e Actions; avaliação visual física ainda pendente)
- COMMIT: `88ef022`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`
- GITHUB ACTIONS: run `34764684349` PASS, SHA `88ef022` conferido; unitários, instrumentados e build operationalDebug concluídos
- ROOM VERSION: 15
- MIGRATION: não alterada
- ALTERAÇÕES: `ACTIVE` e `FINALIZATION` passaram a ser rotas operacionais próprias; READY/INICIAR conduz ao rastreio ativo; PARAR conduz à finalização; reabertura restaura a rota conforme o estado; home recebeu cabeçalho operacional consistente com a inspiração C32; estado e dados permanecem persistidos ao navegar para o início
- LIMITAÇÕES: identidade visual ainda é uma evolução funcional, não uma cópia definitiva do C32; teste físico de navegação e teclado continua pendente
- PRÓXIMO BLOCO: Bloco 6 — Receiver/Antenna manuais e snapshots

## 2026-09-13 — Bloco 6

- BLOCO: Receiver/Antenna manuais, catálogo persistente e snapshots
- STATUS: PASS (código, migration, testes instrumentados e Actions; teste físico ainda não executado)
- COMMITS: `c457d60` → `b87c0c5`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`/Gradle global
- GITHUB ACTIONS: run `34765738305` PASS para `c457d60`; run `34766100652` PASS para `b87c0c5`; SHA dos dois workflows conferido; unitários, instrumentados Room e build operationalDebug concluídos
- ROOM VERSION: 17
- MIGRATION: V15→V16 cria catálogos `receiver_catalog`/`antenna_catalog`; V16→V17 preserva firmware informado no snapshot da ocupação; schema `17.json` versionado
- ALTERAÇÕES: catálogo manual separado para Receiver e Antenna; criar/listar/editar/arquivar; seleção no fluxo de novo rastreio; origem continua `OPERATOR`; ocupação copia fabricante/modelo/serial/firmware para seu snapshot persistido; teste confirma que editar/arquivar catálogo não reescreve ocupação histórica; versão do APK incrementada para `0.1.6-dev`/versionCode 7
- LIMITAÇÕES: compatibilidade Spectra continua fora deste marco; nenhum IP/porta/UUID/protocolo é presumido; teste físico do catálogo e do fluxo completo ainda depende do aparelho; exportação/importação de catálogos será tratada no bloco de intercâmbio estruturado
- PRÓXIMO BLOCO: Bloco 7 — alturas 1..N definitivas e refinamento visual operacional do fluxo BEFORE/AFTER

## 2026-09-13 — Bloco 7

- BLOCO: alturas 1..N definitivas e refinamento visual BEFORE/AFTER
- STATUS: PASS (código, testes instrumentados e Actions; teste físico ainda não executado)
- COMMIT: `81194a6`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`/Gradle global
- GITHUB ACTIONS: run `34766912892` PASS para `81194a6`; SHA conferido; unitários, instrumentados e build operationalDebug concluídos
- ROOM VERSION: 17
- MIGRATION: nenhuma alteração de schema; IDs das medições já existentes foram preservados
- ALTERAÇÕES: cada `HeightMeasurement` mantém ID; registro BEFORE/AFTER substitui somente a fase editada e elimina duplicação; UI permite 1..N leituras, unidade explícita e tipo VERTICAL/SLANT/OTHER; médias/amplitudes/delta continuam derivados; cartões de altura receberam hierarquia visual operacional inspirada no C32
- LIMITAÇÕES: teste físico de teclado, rolagem e visual ainda pendente; as regras de tolerância continuam deliberadamente ausentes; a tela legada de compatibilidade ainda mantém edição longa fora do wizard
- PRÓXIMO BLOCO: Bloco 8 — eventos/timeline/auditoria persistente com refinamento visual da tela ACTIVE

## 2026-09-13 — Bloco 8

- BLOCO: eventos, timeline e auditoria persistente com refinamento visual ACTIVE
- STATUS: PASS (código, migration, testes instrumentados e Actions; teste físico ainda não executado)
- COMMITS: `2aca444` → `983b4e0`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`/Gradle global
- GITHUB ACTIONS: run `34767743200` PASS para `2aca444`; run `34768034292` PASS para `983b4e0`; SHA dos dois workflows conferido; unitários, instrumentados e build operationalDebug concluídos
- ROOM VERSION: 18
- MIGRATION: V17→V18 cria `audit_events`; schema `18.json` versionado; auditoria usa insert append-only sem update/delete
- ALTERAÇÕES: eventos de campo agora selecionam categoria e severidade; tela ACTIVE mostra linha do tempo de eventos e auditoria; transições READY/ACTIVE/STOPPED/COLLECTED/VALIDATED, equipamento, alturas e RAW geram registros de auditoria; fechamento/reabertura recupera auditoria por ocupação
- LIMITAÇÕES: o teste físico da tela ACTIVE e do comportamento de rolagem ainda depende do aparelho; auditoria de edição de cadastros fora da ocupação pode ser ampliada; Spectra continua sem protocolo automático
- PRÓXIMO BLOCO: Bloco 9 — RAW/SHA-256 e finalização com inventário visual e resumo operacional

## 2026-09-13 — Bloco 9

- BLOCO: RAW, SHA-256 e finalização com inventário visual
- STATUS: PASS (código, testes unitários/instrumentados e Actions; teste físico ainda não executado)
- COMMIT: `d1051cf`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`/Gradle global
- GITHUB ACTIONS: run `34768687672` PASS para `d1051cf`; SHA conferido; testes de deduplicação/integridade e build operationalDebug concluídos
- ROOM VERSION: 18
- MIGRATION: nenhuma alteração de schema neste bloco; artifacts existentes preservados
- ALTERAÇÕES: associação RAW deduplica por SHA-256 dentro da ocupação; inventário mostra nome, tamanho e hash completo; verificação pós-cópia recalcula tamanho/hash; teste de arquivo corrompido; auditoria registra verificação válida ou inválida
- LIMITAÇÕES: a importação continua sendo seleção de arquivo já presente/acessível ao Android, não download remoto da antena; teste físico do seletor e persistência após reinstalação ainda pendente
- PRÓXIMO BLOCO: Bloco 10 — teste automatizado end-to-end do Gate R2 e pacote operacional para teste físico

## 2026-09-13 — Bloco 10

- BLOCO: teste automatizado end-to-end do Gate R2 e pacote operacional para teste físico
- STATUS: READY_FOR_PHYSICAL_TEST (automação, build e Actions PASS; confirmação física ainda não executada)
- COMMIT: `e1e2891`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`/Gradle global
- GITHUB ACTIONS: run `34769326389` PASS, SHA `e1e2891af2e747a26ec25d205e470830dc33a4e0` conferido; testes unitários, instrumentados, build `operationalDebug`, upload do APK e schemas concluídos
- ROOM VERSION: 18
- MIGRATION: nenhuma alteração neste bloco; cadeia V1→V18 permanece explícita e sem destructive migration
- ALTERAÇÕES: criado `GateR2EndToEndTest` cobrindo Project → Station → ReferencePoint → Occupation → equipamento/snapshots → BEFORE → READY → ACTIVE → evento → STOPPED → AFTER → RAW/SHA-256 → COLLECTED → VALIDATED → fechamento/reabertura; versão do APK incrementada para `0.1.10-dev`/versionCode 11
- APK OPERATIONAL: artefato do run `34769326389`; SHA-256 `985DE14D5A07D8AF62D7F3598C729E49EDABC051833EC3E73DD900C85DD9B830`
- LIMITAÇÕES: teste físico do fluxo completo, teclado/rolagem, fechamento pelo sistema e confirmação visual ainda dependem do aparelho; RAW continua sendo arquivo selecionado no dispositivo, sem download da antena; Spectra, PPA, RINEX completo e renderer F-21 permanecem fora do escopo
- PRÓXIMO BLOCO: pausa humana para teste físico do Gate R2; somente após confirmação PASS avaliar Spectra B1 READ_ONLY

## 2026-09-13 — Correção pós-teste físico

- BLOCO: busca Bluetooth, tela Sobre e comportamento do teclado
- STATUS: READY_FOR_PHYSICAL_RETEST (código, build e Actions PASS; correção ainda não repetida no aparelho)
- COMMIT: `ed699dc`
- TESTES LOCAIS: `git diff --check` PASS; execução Gradle local BLOCKED por ausência de `gradlew.bat`/Gradle global
- GITHUB ACTIONS: run `34781927359` PASS, SHA `ed699dcb3f970060d8afd8f068b703dc137480a6` conferido; unitários, instrumentados e build `operationalDebug` concluídos
- ROOM VERSION: 18; nenhuma migration alterada
- ALTERAÇÕES: busca Bluetooth passou a solicitar permissões conforme a API (Dispositivos próximos no Android 12+ e Localização até Android 11), informar Localização desativada, capturar bloqueios de permissão e aceitar broadcasts do serviço Bluetooth; campos de texto usam `BringIntoViewRequester` para não ficarem atrás do teclado; tela Sobre recebeu cartões de descrição, versão, modo, contato e privacidade inspirados no padrão do Nivelamento de Bolso; APK incrementado para `0.1.11-dev`/versionCode 12
- APK OPERATIONAL: artefato do run `34781927359`; SHA-256 `6EE7C0FF714A0AAA8F1B5D008FB237BEA6DE61C5F25A7232619735D9D8B05893`
- LIMITAÇÕES: ainda não há comprovação física de que a busca encontre o hardware; descoberta genérica não presume UUID/protocolo Spectra; o teste da rolagem com teclado e a avaliação visual da tela Sobre precisam ser repetidos
- PRÓXIMO BLOCO: reteste físico da busca Bluetooth e do teclado; depois retomar o Gate R2 ou registrar BLOCKED conforme o resultado
