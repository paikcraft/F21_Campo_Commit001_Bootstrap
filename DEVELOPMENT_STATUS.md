# DEVELOPMENT STATUS — auditoria do HEAD

Data da auditoria: 2026-09-13

## Identificação

- HEAD audit base: `b528fefbe408a3f516c649f1d19b537990edf308` (snapshot block in progress)
- Branch: `main`
- VersionName target: `0.1.13-dev`
- VersionCode target: `14`
- Room version target: `19`
- CI mais recente do HEAD: run `34783033672`, `PASS`, SHA conferido
- Build local: bloqueado neste ambiente; não existe `gradlew.bat` e não há Gradle global disponível

## Módulos e toolchain

- Módulos: `app`, `domain`, `data`, `files`, `rinex`, `processing`, `f21`, `hnproject`, `receiver-api`, `receiver-manual`.
- CI: JDK 17, Gradle 9.6.0, AGP 9.4.0, Kotlin 2.3.21, compileSdk 37, targetSdk 36, minSdk 26.
- Flavors: `operational` e `lab`; o artefato entregue é `operationalDebug`.

## Rotas/telas verificadas

`HOME`, `NEW` em etapas 1–6, `PROJECTS`, `STATIONS`, `SUMMARY`, `SETTINGS`, `EQUIPMENT`, `CONNECTION`, `ABOUT`, `ACTIVE` e `FINALIZATION` existem em `MainActivity.kt`. A home possui destinos distintos. Há também um ramo legado de fallback no fim da composição que ainda mantém formulário longo para estados não cobertos pelas rotas principais.

## Entidades persistidas

Room persiste Project, Station, ReferencePoint, Occupation, OccupationArtifact, OccupationEvent, HeightMeasurement, AuditEvent, ReceiverConnectionProfile, ReceiverCatalog e AntennaCatalog. O equipamento de uma Occupation é persistido em campos achatados da ocupação e reconstruído como `EquipmentSnapshot`.

## Migrations e schemas

- Cadeia explícita V1→V19 em `data/src/main/kotlin/br/f21campo/data/Migrations.kt` (V18→V19 adiciona `occupation_snapshots`).
- `exportSchema = true`.
- Schemas versionados presentes: 15, 17 e 18. Não há `16.json` versionado; a lacuna está documentada nos relatórios históricos.
- Não há `fallbackToDestructiveMigration` encontrado.
- Testes Room in-memory, migration/reopen e preservação de dados passam no CI.

## Testes

- Unitários: domínio, alturas, estados, readiness, eventos/auditoria, timer, RAW, transporte TCP e adapter manual.
- Instrumentados: bootstrap, migrations/reopen, catálogos/snapshots de equipamento, RAW, auditoria/eventos e `GateR2EndToEndTest`.
- Último CI: unitários, instrumentados e `assembleOperationalDebug` PASS.
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

- Snapshots históricos: Receiver/Antenna estão achatados em `occupations`, mas não existem `StationSnapshot`, `ReferencePointSnapshot`, `ReceiverSnapshot` e `AntennaSnapshot` persistidos como estrutura própria. Ao reabrir, estação e referência são buscadas pelo cadastro mutável.
- Reidratação após process death e comportamento visual no aparelho ainda não têm confirmação física nesta versão.
- Auditoria registra principalmente ações da Occupation; edição de cadastros Station/Reference fora da ocupação não tem trilha completa.
- Exportação/importação JSON preserva o núcleo atual, mas ainda não inclui snapshots, catálogos e auditoria.
- O ramo legado de fallback da `MainActivity` continua no código.

## Funcionalidades ausentes ou fora do marco

- Snapshot histórico dedicado de Station/Reference/Receiver/Antenna (implementação do bloco atual; aguardando testes/CI).
- Spectra B1/B2/B3 operacional, protocolo, comandos, download remoto e identificação automática.
- PPA/ppaScore, processamento GNSS próprio, PPP, parser RINEX completo e renderer F-21 definitivo.

## Dívidas e riscos

- `Occupation` começa em memória com IDs provisórios de projeto/estação e só é persistida após dados reais; isso não deve ser confundido com relações persistidas válidas.
- Existe modelo legado rígido `HeightObservation` no domínio, embora o fluxo operacional use `HeightMeasurement` 1..N.
- O seletor RAW depende de arquivo acessível pelo Android; não é importação direta da antena.
- Build local não pode ser reproduzido sem Gradle wrapper/global; CI é a evidência de build.
- O arquivo de auditoria enviado pelo usuário `AUDITORIA_ESTADO_ATUAL_F21_CAMPO.md` permanece não rastreado e não foi alterado.

## Marco atual

**B — GATE R2 MANUAL EM CONSTRUÇÃO.** O bloco R2.6 está em implementação; ainda falta validar a migration/testes no CI e o reteste físico.

## Primeiro bloco incompleto

**R2.6 — SNAPSHOTS HISTÓRICOS.** Implementar e validar snapshots persistidos para Station, ReferencePoint, Receiver e Antenna, com migration explícita, preservação/backfill de dados existentes, reidratação pelo snapshot e teste de fechamento/reabertura. Somente depois reavaliar `READY_FOR_PHYSICAL_TEST` e solicitar o roteiro físico atualizado.
