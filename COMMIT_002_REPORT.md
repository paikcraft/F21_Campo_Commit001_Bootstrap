# COMMIT 002 — Domain Primitives

COMMIT: 002 — Domain Primitives
STATUS: PASS
GIT HASH: 7b676ad
FILES CHANGED:
- `domain/src/main/kotlin/br/f21campo/domain/EntityId.kt`
- `domain/src/main/kotlin/br/f21campo/domain/AppClock.kt`
- `domain/src/main/kotlin/br/f21campo/domain/Sha256.kt`
- `domain/src/main/kotlin/br/f21campo/domain/Provenance.kt`
- `domain/src/main/kotlin/br/f21campo/domain/DomainResult.kt`
- `domain/src/test/kotlin/br/f21campo/domain/DomainPrimitivesTest.kt`
DB VERSION: N/A
MIGRATIONS: N/A
TESTS EXECUTED: `gradle test`, GitHub Actions run `34164395551`
TEST RESULTS: PASS
APKS GENERATED: operationalDebug, labDebug
SHA256: artefatos APK publicados pelo workflow; hashes serão registrados no gate R2
KNOWN LIMITATIONS:
- sem integração automática Spectra;
- sem validação RINEX;
- sem processamento;
- sem F-21;
- sem `.hnproject`;
- PPA não implementado.
DECISIONS:
- UUID canônico em texto;
- SHA-256 em hexadecimal minúsculo;
- relógio injetável para testes;
- proveniência e validação modeladas como enums estáveis;
- nenhum Android/Room foi introduzido no domínio.
BLOCKERS: nenhum
NEXT COMMIT: 003 — Entidades persistentes do domínio
