# COMMIT 001 — Relatório

STATUS: SOURCE BOOTSTRAP CRIADO / BUILD ANDROID AINDA NÃO EXECUTADO NESTE AMBIENTE

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
- teste JVM de bootstrap incluído.
- teste Android de package incluído.
- Gradle wrapper será incluído separadamente.
- build Android não executado neste ambiente por ausência de SDK/Gradle instalado.

NEXT COMMIT: 002 — Domain Primitives
