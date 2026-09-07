# COMMIT 006 — Occupation State Machine

COMMIT: 006 — Occupation State Machine
STATUS: PASS
GIT HASH: 6f21b93
FILES CHANGED:
- `domain/src/main/kotlin/br/f21campo/domain/OccupationStateMachine.kt`
- `domain/src/test/kotlin/br/f21campo/domain/OccupationStateMachineTest.kt`
DB VERSION: 1
MIGRATIONS: baseline v1
TESTS EXECUTED: `gradle test`, `assembleOperationalDebug`, `assembleLabDebug`, GitHub Actions run do Commit 006
TEST RESULTS: PASS
APKS GENERATED: operationalDebug, labDebug
SHA256: artefato publicado pelo workflow
KNOWN LIMITATIONS:
- interface visual da ocupação será implementada nos próximos commits;
- sem controle automático de receptor;
- sem integração Spectra, RINEX, processamento, F-21, `.hnproject` e PPA.
DECISIONS:
- timestamps confirmados não são sobrescritos;
- `STOPPED` é distinto de `COLLECTED`;
- bruto é obrigatório antes de `COLLECTED`;
- fechamento do processo não foi usado como encerramento da ocupação.
BLOCKERS: nenhum
NEXT COMMIT: 007 — Receiver / Antenna Manual

## Evidência do CI

- GitHub Actions exibiu marca verde para `feat: implement occupation state machine`.
- Testes, builds operacional/LAB e publicação dos APKs concluídos com sucesso.
