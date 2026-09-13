# LIMITAÇÕES CONHECIDAS — 0.1.10-dev

- O Gate R2 está `READY_FOR_PHYSICAL_TEST`, não homologado.
- A integração Spectra não foi iniciada: não há IP, porta, UUID, framing, checksum ou comandos presumidos.
- A seleção de RAW usa o seletor de arquivos do Android; não há download remoto da antena.
- Não há parser RINEX completo, processamento GNSS próprio, PPP, PPA ou renderer F-21 definitivo.
- A validação de teclado, rolagem e recuperação após process death ainda depende do teste físico.
- O catálogo de Receiver/Antenna é manual; origem informada pelo operador permanece explícita.
- `COLLECTED` e `VALIDATED` são estados distintos e a aprovação de coordenadas não é automática.
- A execução Gradle local permanece bloqueada neste ambiente pela ausência de `gradlew.bat`/Gradle global; o build oficial foi executado no GitHub Actions.
