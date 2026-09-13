# TEST REPORT — Gate R2

## Resultado geral

`READY_FOR_PHYSICAL_TEST`

O fluxo manual possui teste automatizado end-to-end e o CI ficou verde. Ainda não há evidência física registrada neste repositório para esta versão.

## Testes executados no GitHub Actions

- Unit tests: `PASS` no run `34769326389`.
- Instrumented tests: `PASS`, incluindo `GateR2EndToEndTest`, testes Room/reabertura e testes de integridade de arquivos.
- Build `assembleOperationalDebug`: `PASS`.
- Upload do APK e schemas: `PASS`.

O teste end-to-end cobre a sequência Project, Station, ReferencePoint RN, Occupation, Receiver/Antenna manual, alturas BEFORE e AFTER, READY, ACTIVE, evento, STOPPED, RAW com SHA-256, COLLECTED, VALIDATED e reabertura do banco.

## O que ainda não foi comprovado

- Teste físico em aparelho real nesta versão.
- Remoção do aplicativo dos recentes/process death e reidratação visual completa no dispositivo.
- Avaliação física de teclado, rolagem, tamanhos e contraste sob uso de campo.
- Conexão com antena/receptor; o RAW é selecionado localmente e não é baixado do hardware.

Portanto, não marcar `GATE_R2 = PASS` antes da confirmação física do roteiro.
