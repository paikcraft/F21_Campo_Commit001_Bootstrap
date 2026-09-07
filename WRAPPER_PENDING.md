# Gradle Wrapper — pendência técnica do ambiente de geração

O Commit 001 fixa Gradle 9.6.0 em `BUILD.md` e no workflow de CI.

Este pacote ainda não contém `gradle-wrapper.jar`, pois o ambiente usado para gerar
o código não possui Gradle instalado nem permite obter o binário do wrapper diretamente.
Por isso, não foi criado um `gradlew` incompleto ou enganoso.

O CI usa `gradle/actions/setup-gradle@v4` com Gradle 9.6.0 e pode gerar o wrapper depois
de o primeiro build ser validado:

```bash
gradle wrapper --gradle-version 9.6.0
```

Depois, devem ser versionados:

- `gradlew`
- `gradlew.bat`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`

A ausência do wrapper neste pacote está explicitamente registrada e não deve ser
interpretada como build verificado.
