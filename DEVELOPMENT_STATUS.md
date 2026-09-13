# DEVELOPMENT STATUS — auditoria do HEAD

Data da auditoria: 2026-09-13

## Identificação

- HEAD code: `76fae44126b97879eb3df877434278fe71335701` (R2.8 test extension in progress)
- Branch: `main`
- VersionName: `0.1.14-dev`
- VersionCode: `15`
- Room version: `20`
- CI mais recente do HEAD: run `34786163981`, `PASS`, SHA conferido
- Build local: bloqueado neste ambiente; não existe `gradlew.bat` e não há Gradle global disponível

## Módulos e toolchain

- Módulos: `app`, `domain`, `data`, `files`, `rinex`, `processing`, `f21`, `hnproject`, `receiver-api`, `receiver-manual`.
- CI: JDK 17, Gradle 9.6.0, AGP 9.4.0, Kotlin 2.3.21, compileSdk 37, targetSdk 36, minSdk 26.
- Flavors: `operational` e `lab`; o artefato entregue é `operationalDebug`.

## Rotas/telas verificadas

`HOME`, `NEW` em etapas 1–6, `PROJECTS`, `STATIONS`, `SUMMARY`, `SETTINGS`, `EQUIPMENT`, `CONNECTION`, `ABOUT`, `ACTIVE` e `FINALIZATION` existem em `MainActivity.kt`. A home possui destinos distintos. Há também um ramo legado de fallback no fim da composição que ainda mantém formulário longo para estados não cobertos pelas rotas principais.

## Entidades persistidas

Room persiste Project, Station, ReferencePoint, Occupation, OccupationSnapshot, OccupationArtifact, OccupationEvent, HeightMeasurement, AuditEvent, ReceiverConnectionProfile, ReceiverCatalog e AntennaCatalog. O equipamento também é preservado na fotografia histórica da ocupação.

## Migrations e schemas

- Cadeia explícita V1→V20 em `data/src/main/kotlin/br/f21campo/data/Migrations.kt` (V19→V20 permite DRAFT sem Project/Station).
- `exportSchema = true`.
- Schemas versionados presentes: 15, 17, 18, 19 e 20. Não há `16.json` versionado; a lacuna está documentada nos relatórios históricos.
- Não há `fallbackToDestructiveMigration` encontrado.
- Testes Room in-memory, migration/reopen e preservação de dados passam no CI.

## Testes

- Unitários: domínio, alturas, estados, readiness, eventos/auditoria, timer, RAW, transporte TCP e adapter manual.
- Instrumentados: bootstrap, migrations/reopen, catálogos/snapshots de equipamento, RAW, auditoria/eventos e `GateR2EndToEndTest`.
- Último CI: unitários, instrumentados e `assembleOperationalDebug` PASS; novo teste ACTIVE/reabertura aguarda Actions.
- Teste físico após a versão 0.1.12: ainda não registrado.

## Funcionalidades PASS no código/testes

- Home com navegação distinta.
- READY/INICIAR e máquina de estados persistente.
- Project, Station e ReferencePoint RN/MT/PA com IDs preservados e arquivamento lógico.
- Catálogo manual Receiver/Antenna e snapshot de equipamento achatado na Occupation.
- Alturas operacionais BEFORE/AFTER 1..N, unidade explícita, tipos e derivados.
- Autosave básico e reconstrução da UI por `Continuar rastreio` no fluxo principal.
- Eventos/timeline, auditoria append-only, RAW controlado, SHA-256, deduplicação e verificação.
- Importação RAW diretamente na tela de Finalização (0.1.12).
- Transporte TCP passivo e descoberta Bluetooth genérica sem protocolo Spectra.

## Funcionalidades parciais

- Snapshots históricos: PASS no bloco R2.6; `occupation_snapshots` preserva Station, ReferencePoint, Receiver e Antenna, com backfill V18→V19 e reidratação pelo snapshot.
- DRAFT inicial agora é persistido com Project/Station nulos, sem IDs fictícios; o teste de ACTIVE/reabertura foi adicionado, mas process death físico ainda não foi observado.
- Auditoria registra principalmente ações da Occupation; edição de cadastros Station/Reference fora da ocupação não tem trilha completa.
- Exportação/importação JSON preserva o núcleo atual, mas ainda não inclui snapshots, catálogos e auditoria.
- O ramo legado de fallback da `MainActivity` continua no código.

## Funcionalidades ausentes ou fora do marco

- Spectra B1/B2/B3 operacional, protocolo, comandos, download remoto e identificação automática.
- PPA/ppaScore, processamento GNSS próprio, PPP, parser RINEX completo e renderer F-21 definitivo.

## Dívidas e riscos

- `Occupation` começa em memória com IDs provisórios de projeto/estação e só é persistida após dados reais; isso não deve ser confundido com relações persistidas válidas.
- Existe modelo legado rígido `HeightObservation` no domínio, embora o fluxo operacional use `HeightMeasurement` 1..N.
- O seletor RAW depende de arquivo acessível pelo Android; não é importação direta da antena.
- Build local não pode ser reproduzido sem Gradle wrapper/global; CI é a evidência de build.
- O arquivo de auditoria enviado pelo usuário `AUDITORIA_ESTADO_ATUAL_F21_CAMPO.md` permanece não rastreado e não foi alterado.

## Marco atual

**B — GATE R2 MANUAL EM CONSTRUÇÃO.** R2.6 passou em testes, migration e CI; R2.8 agora possui teste automatizado de DRAFT e ACTIVE após reabertura. O reteste físico ainda é obrigatório.

## Primeiro bloco incompleto

**R2.8 — AUTOSAVE E REIDRATAÇÃO COMPLETA.** DRAFT inicial sem IDs fictícios e ACTIVE com timestamps, snapshots, alturas e eventos são reabertos por teste instrumentado. Falta apenas a confirmação física do comportamento da Activity após remoção dos recentes/process death.
