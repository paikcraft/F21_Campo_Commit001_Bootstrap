# TEST REPORT — 0.1.12-dev

## Resultado

`READY_FOR_PHYSICAL_RETEST`

O CI passou nos testes unitários, instrumentados e no build `operationalDebug` do fluxo com importação RAW reposicionada na Finalização.

## Alteração verificada pelo código

- A tela Finalização apresenta `IMPORTAR RAW DO CELULAR` antes das alturas AFTER.
- O seletor usa a mesma cópia controlada, SHA-256 em streaming, pós-verificação e associação à ocupação já testados no bloco RAW.
- A mensagem diferencia arquivo selecionado no aparelho de uma futura transferência direta da antena.

## Ainda pendente

- Confirmar no aparelho que o botão fica visível e é fácil de usar no fluxo de campo.
- Selecionar um arquivo de teste, conferir inventário/hash e concluir `COLLECTED`.
- Não há conexão ou download automático de antena nesta versão.
