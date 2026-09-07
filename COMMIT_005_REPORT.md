# COMMIT 005 — Project / Station

COMMIT: 005 — Project / Station
STATUS: PASS
GIT HASH: 41153da
FILES CHANGED:
- `app/src/main/kotlin/br/f21campo/app/MainActivity.kt`
- `app/build.gradle.kts`
DB VERSION: 1
MIGRATIONS: baseline v1
TESTS EXECUTED: `gradle test`, `assembleOperationalDebug`, `assembleLabDebug`, GitHub Actions run `34166444619`
TEST RESULTS: PASS
APKS GENERATED: operationalDebug, labDebug
SHA256: artifact `f21-campo-debug-apks`, `sha256:290d67f2ed516d2a633db0c7c5e5dc3ed94a82998dd47928693627fe42530915`
KNOWN LIMITATIONS:
- edição/arquivamento avançados e busca dedicada serão ampliados nos próximos commits;
- sem integração automática Spectra, RINEX, processamento, F-21, `.hnproject` e PPA.
DECISIONS:
- Station ID é criado uma vez e preservado ao salvar;
- localidade é opcional;
- municipality permanece nulo quando não informado;
- persistência usa repository, não acesso direto da UI ao DAO.
BLOCKERS: nenhum
NEXT COMMIT: 006 — Occupation State Machine

## Evidência física

- `operationalDebug` instalado e executado em aparelho físico: tela exibiu Modo `OPERATIONAL` e `Estação salva — ID preservado`.
- `labDebug` instalado e executado em aparelho físico: tela exibiu Modo `LAB` e `Estação salva — ID preservado`.
- Evidências visuais fornecidas pelo operador em 2026-09-07.
