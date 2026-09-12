# ADR 0003 — Integração Spectra orientada a transporte

**Status:** Aceito para a futura fase Spectra B1.

## Decisão

O F-21 não modelará um receptor como se fosse exclusivamente Bluetooth.
A integração continua separada em:

```text
Receiver
  ↓
ConnectionAdapter / ReceiverTransport
  ├── Wi-Fi / TCP
  ├── Bluetooth
  └── Serial / USB, quando aplicável
```

Para a investigação inicial da família Spectra, Wi-Fi/TCP é o transporte
prioritário. Bluetooth e Serial/USB continuam alternativas possíveis, a serem
ativadas somente para um perfil de equipamento homologado.

## Evidência disponível

A engenharia reversa estática do SPSM identificou parâmetros equivalentes a
Wi-Fi, TCP/IP, Bluetooth e Serial, além de componente equivalente a cliente TCP.
Ela também revelou candidatos da família ASH/PASH (`RID`, `RCS`, `OBI`, `FIL`).

Essa evidência mostra a arquitetura do aplicativo de referência; ela não prova o
comportamento do receptor físico que estiver em bancada.

## Pendências obrigatórias de bancada

Permanecem `TBD` até observação física por fabricante, modelo, firmware,
transporte e função:

- transporte efetivamente utilizado pelo receptor;
- modo Wi-Fi (AP/cliente), SSID, IP e porta TCP/UDP;
- pareamento, serviço/UUID e modalidade Bluetooth;
- framing, terminadores, checksum, timeout e reconexão;
- comandos, respostas e efeitos de `RID`, `RCS`, `OBI` e `FIL`.

Nenhum desses valores pode ser fixado no código antes da validação.

## Ordem de Spectra B1

```text
Estudos/SPSM
→ ReceiverSpectraAdapter
→ abstração de transporte
→ Wi-Fi/TCP passivo
→ confirmar IP, porta e framing
→ operações READ_ONLY comprovadas
→ identificação/estado
→ CONTROL somente com evidência suficiente
```

`START`, `STOP`, configuração e download remoto não entram nesta fase por
suposição.

## Evidência e homologação

Todo resultado de bancada deve guardar a classificação:

| Nível | Significado |
| --- | --- |
| C0 | Não testado |
| C1 | Hipótese |
| C2 | Observado uma vez |
| C3 | Repetido |
| C4 | Confirmado pelo comportamento |
| C5 | Homologado |

O perfil operacional só pode usar comportamento C5 para o respectivo
fabricante, modelo, firmware, transporte e função.
