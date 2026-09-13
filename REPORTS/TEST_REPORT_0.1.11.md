# TEST REPORT — 0.1.11-dev

## Resultado

`READY_FOR_PHYSICAL_RETEST`

O CI passou nos testes unitários, instrumentados e no build `operationalDebug`. A correção foi motivada por um teste físico que mostrou falha ao iniciar a busca Bluetooth e teclado sobrepondo o campo editado.

## Correções cobertas pelo código

- Permissões Bluetooth diferenciadas por API Android.
- Diagnóstico para permissão negada, Bluetooth desligado e Localização desativada em APIs antigas.
- Recepção de eventos `ACTION_FOUND`/`DISCOVERY_FINISHED` do serviço Bluetooth.
- Rolagem automática para o campo focado quando o teclado aparece.
- Tela Sobre em cartões com descrição, modo, versão, contato e privacidade.

## Ainda pendente

- Repetir no aparelho a busca de dispositivos próximos.
- Confirmar que o campo de altura/evento permanece visível com o teclado.
- Confirmar visualmente a tela Sobre.
- Nenhum teste de protocolo Spectra foi feito ou presumido.
