# COMMIT 001 — Relatório

STATUS: PASS

## Toolchain
- JDK 17
- Gradle 9.6.0
- AGP 9.4.0
- Kotlin 2.3.21
- compileSdk 37
- targetSdk 36
- minSdk 26
- Compose BOM 2026.08.00

## Módulos
app, domain, data, files, rinex, processing, f21, hnproject, receiver-api, receiver-manual

## Variants
operationalDebug
labDebug
operationalRelease
labRelease

## Decisões
- `br.f21campo` é applicationId provisório de bootstrap.
- nenhum módulo vendor foi criado.
- domínio é JVM puro.
- LAB e OPERATIONAL estão separados.
- sem permissões antecipadas.
- sem PPA.

## Validação realizada
- estrutura de diretórios e arquivos criada.
- testes JVM executados no GitHub Actions: PASS.
- `assembleOperationalDebug` executado no GitHub Actions: PASS.
- `assembleLabDebug` executado no GitHub Actions: PASS.
- APK `operationalDebug` instalado e executado em aparelho físico: PASS.
- evidência visual: tela exibiu `F-21 Campo`, versão `0.1.0-dev-debug`, modo `OPERATIONAL` e `Bootstrap OK`.
- workflow publica os APKs de debug como artefatos.

GIT COMMIT: 2585f49

## Evidência externa
- Execução física confirmada pelo operador em 2026-09-07.
- A captura de tela foi fornecida no encerramento do Commit 001.

NEXT COMMIT: 002 — Domain Primitives (iniciado)
