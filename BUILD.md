# BUILD — F-21 Campo / Commit 001

Toolchain escolhida para o bootstrap:

- JDK: 17
- Gradle Wrapper: 9.6.0
- Android Gradle Plugin: 9.4.0
- Kotlin: 2.3.21
- compileSdk: 37
- targetSdk: 36
- minSdk: 26
- Compose BOM: 2026.08.00

## Builds

```bash
./gradlew test
./gradlew assembleOperationalDebug
./gradlew assembleLabDebug
```

## Observação deste pacote

O ambiente de geração desta resposta não possui Android SDK/Gradle local configurado, portanto os builds Android ainda precisam ser executados em Android Studio/CI com SDK 37 instalado.

## CI bootstrap

Enquanto o wrapper ainda não estiver materializado, `.github/workflows/android.yml`
instala explicitamente Gradle 9.6.0 e executa os mesmos gates de build.

Após o primeiro CI verde, gerar e versionar o Gradle Wrapper 9.6.0.
