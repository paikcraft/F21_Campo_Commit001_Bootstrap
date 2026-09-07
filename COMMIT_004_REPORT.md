# COMMIT 004 — Room / Repositories / Migration v1

COMMIT: 004 — Room / Repositories / Migration v1
STATUS: PASS
GIT HASH: 71ae190
FILES CHANGED:
- `data/src/main/kotlin/br/f21campo/data/ProjectStationEntities.kt`
- `data/src/main/kotlin/br/f21campo/data/F21Database.kt`
- `data/src/main/kotlin/br/f21campo/data/Daos.kt`
- `data/src/main/kotlin/br/f21campo/data/Converters.kt`
- `data/src/main/kotlin/br/f21campo/data/Mappers.kt`
- `data/src/main/kotlin/br/f21campo/data/ProjectStationRepository.kt`
- `data/src/main/kotlin/br/f21campo/data/DatabaseMigrations.kt`
- `gradle/libs.versions.toml`
DB VERSION: 1 (baseline declarada)
MIGRATIONS: baseline v1 registrada; nenhuma migration destrutiva
TESTS EXECUTED: `gradle test`, `assembleOperationalDebug`, `assembleLabDebug`, GitHub Actions run `34166180764`
TEST RESULTS: PASS
APKS GENERATED: operationalDebug, labDebug
SHA256: APKs publicados pelo workflow
KNOWN LIMITATIONS:
- schema exportado automaticamente pelo KSP durante o CI; a versionação do arquivo gerado será consolidada no próximo ajuste de artefatos;
- teste in-memory/reopen/migration completo fica para a próxima etapa de validação de infraestrutura;
- sem integração automática Spectra, RINEX, processamento, F-21, `.hnproject` e PPA.
DECISIONS:
- domínio permanece sem annotations Room;
- mappers traduzem entre entidades Room e domínio;
- não foi usado `fallbackToDestructiveMigration`.
BLOCKERS: nenhum para o baseline Room v1.
NEXT COMMIT: 005 — Project / Station
