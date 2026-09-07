# COMMIT 004 — Room / Repositories / Migration v1

COMMIT: 004 — Room / Repositories / Migration v1
STATUS: BLOCKED
GIT HASH: 98f6dd7
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
TESTS EXECUTED: `gradle test`, `assembleOperationalDebug`, `assembleLabDebug`, GitHub Actions run `34165464641`
TEST RESULTS: PASS para a baseline Room sem processamento; geração de schema pendente
APKS GENERATED: operationalDebug, labDebug
SHA256: APKs publicados pelo workflow
KNOWN LIMITATIONS:
- `room-compiler`/KSP não está ativo porque a combinação atual AGP 9.4/Kotlin 2.3.21 falhou no CI;
- schema exportado automaticamente ainda pendente;
- teste in-memory/reopen/migration ainda pendente;
- sem integração automática Spectra, RINEX, processamento, F-21, `.hnproject` e PPA.
DECISIONS:
- domínio permanece sem annotations Room;
- mappers traduzem entre entidades Room e domínio;
- não foi usado `fallbackToDestructiveMigration`.
BLOCKERS: compatibilidade do processador Room/KSP com o toolchain atual.
NEXT COMMIT: resolver KSP/Room e concluir schema/migration antes do Commit 005
