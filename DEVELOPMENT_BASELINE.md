# Development baseline — F-21 Campo

Data da auditoria: 2026-09-13

## Identificação

- Branch: `main`
- HEAD: `7d2cca8d37bf9f52676167c9153a5cfab1702588` — `feat: add visual tracking step progress`
- VersionName: `0.1.5-dev` (`operationalDebug` acrescenta `-debug`; LAB acrescenta `-lab`)
- VersionCode: `6`
- Room schema version: `15`
- Migrations registradas: `V1_TO_V2` até `V14_TO_V15`
- Toolchain: Android Gradle Plugin/Kotlin configurados no catálogo do projeto, Gradle `9.6.0` no GitHub Actions, JDK `17`, compileSdk `37`, targetSdk `36`, minSdk `26`.
- Último CI conhecido: PASS para o HEAD, incluindo unit tests, `assembleOperationalDebug` e upload do APK.

## Módulos encontrados

`app`, `data`, `domain`, `files`, `f21`, `hnproject`, `processing`, `receiver-api`, `receiver-manual` e `rinex`.

## Estado funcional observado

### Domínio e persistência

- Existem entidades de domínio para Project, Station, ReferencePoint, Occupation, Receiver, Antenna, alturas, eventos, proveniência e SHA-256.
- Room persiste Project, Station, ReferencePoint, Occupation, OccupationArtifact, OccupationEvent, HeightMeasurement e ReceiverConnectionProfile.
- A máquina de estados implementa `DRAFT → READY → ACTIVE → STOPPED → COLLECTED → VALIDATED` e `ABORTED` para estados iniciais/intermediários.
- A regra de domínio de READY verifica referência e altura BEFORE; a tela também calcula pendências de projeto, estação, localidade e equipamento.
- O modelo persistido de alturas é `HeightMeasurement` por fase, compatível com 1..N; ainda existem campos legados `hasBeforeHeight`/`beforeHeightMeters` em Occupation.
- Eventos operacionais têm DAO e persistência. Auditoria do domínio encontrada em `InMemoryAuditService`; não foi encontrada entidade/DAO Room para AuditEvent.

### UI e navegação

- A UI Compose está concentrada em `app/.../MainActivity.kt`.
- Existem rotas distintas para HOME, NEW, PROJECTS, STATIONS, CONNECTION, SETTINGS, ABOUT e SUMMARY.
- O HEAD atual inclui barra superior reutilizável, home reorganizada e régua visual de etapas 1–7.
- O fluxo NEW está dividido em etapas de projeto, estação, referência, equipamento, BEFORE e preparo/READY, seguido por estados READY, ACTIVE e finalização.
- Ainda há telas/fluxos extensos na mesma Activity e há código de compatibilidade/fluxo legado após as etapas novas; a navegação e a reidratação precisam de validação física.

### Arquivos e intercâmbio

- Há armazenamento controlado de RAW com `.part`, cópia e SHA-256 no fluxo de importação.
- Existe envelope JSON versionado para exportação/importação do banco, com upsert transacional e preservação de IDs, timestamps, alturas, eventos, artifacts e perfis.
- A troca JSON preserva metadados/caminho/tamanho/hash dos artifacts, mas não transporta automaticamente os bytes do RAW.

### Receptores

- Existe `ReceiverAdapter`, `ManualReceiverAdapter` e transporte TCP genérico passivo.
- Bluetooth permite descoberta/listagem e teste RFCOMM somente com UUID informado/observado.
- Não há protocolo Spectra implementado; IP, porta, UUID, framing e comandos permanecem sem evidência/homologação.

## Room e migrations

- `F21Database` usa `exportSchema = true` e versão 15.
- A cadeia explícita V1→V15 está registrada em `Migrations.kt` e é adicionada no builder do app.
- Não há `fallbackToDestructiveMigration` encontrado.
- A pasta `data/schemas` contém somente `.gitkeep`; portanto os JSONs de schema ainda não estão efetivamente versionados no Git.
- Existem testes JVM de validação do envelope de troca, mas não foram encontrados testes Room in-memory de migration/reabertura nem testes automatizados da cadeia histórica completa.

## Testes e CI

- Teste JVM em `data/src/test/.../DatabaseExchangeTest.kt` cobre validação do envelope e registros completos.
- Teste instrumentado existente somente confirma o namespace do pacote.
- Workflow `.github/workflows/android.yml` executa `gradle test`, `gradle assembleOperationalDebug` e upload do APK.
- O CI verde não substitui teste físico; não há registro no repositório de um round-trip físico completo nem de process death comprovado.
- Não há `gradlew.bat` no repositório; a build local depende de uma instalação Gradle externa, enquanto o CI usa Gradle 9.6.0.

## Principais lacunas para o Gate R2

1. Testes Room in-memory, migrations e fechamento/reabertura.
2. Schemas Room exportados e versionados no Git.
3. Reidratação completa da UI após fechamento/process death.
4. Auditoria persistente append-only.
5. Catálogos Project/Station/ReferencePoint com edição/arquivamento completos e sem duplicação em todos os caminhos.
6. Finalização RAW com verificação posterior e distinção explícita entre metadados importados e bytes disponíveis.
7. Teste automatizado end-to-end e, depois, teste físico do fluxo completo.

## Divergências em relação à auditoria anterior

- A versão real atual é `0.1.5-dev`, versionCode `6`, não `0.1.1`.
- O HEAD já contém melhorias visuais recentes e navegação separada na Activity, mas isso ainda não constitui aceite físico.
- A troca estruturada de banco já foi implementada; a validação física e transporte dos bytes RAW continuam pendentes.

## Status do baseline

`BASELINE_COMPLETE` — nenhum arquivo de código foi alterado durante esta auditoria; este documento é o diagnóstico de referência para o Bloco 1.
