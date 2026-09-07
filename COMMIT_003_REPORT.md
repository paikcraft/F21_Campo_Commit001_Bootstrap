# COMMIT 003 — Entidades persistentes do domínio

COMMIT: 003 — Entidades persistentes do domínio
STATUS: PASS
GIT HASH: 8ebd278
FILES CHANGED:
- `domain/src/main/kotlin/br/f21campo/domain/Entities.kt`
- `domain/src/test/kotlin/br/f21campo/domain/EntitiesTest.kt`
DB VERSION: N/A — Room entra no Commit 004
MIGRATIONS: N/A
TESTS EXECUTED: `gradle test`, GitHub Actions run `34164779708`
TEST RESULTS: PASS
APKS GENERATED: operationalDebug, labDebug
SHA256: APKs publicados pelo workflow
KNOWN LIMITATIONS:
- entidades ainda não persistidas em Room;
- sem integração automática Spectra;
- sem validação RINEX;
- sem processamento;
- sem F-21;
- sem `.hnproject`;
- PPA não implementado.
DECISIONS:
- `Station.id` permanece estável em edições;
- receptor e antena são entidades distintas;
- ocupação guarda snapshot do equipamento;
- seis leituras de altura são preservadas;
- coordenadas e resultados mantêm proveniência e estado de validação;
- nenhum campo normativo foi inventado.
BLOCKERS: nenhum
NEXT COMMIT: 004 — Room / Repositories / Migration v1
