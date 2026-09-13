# Formato inicial de exportação e importação — F-21 Campo

## Referências encontradas

Foram localizados no acervo do projeto os exemplos:

- `F-21_RN13-CPRM.pdf`
- `Ficha_de_Campo_RN13-CPRM.pdf`

Esses arquivos são referências visuais para os futuros renderizadores. Eles não são tratados como banco de dados nem como fonte para inventar campos operacionais.

## Troca de dados do banco

A primeira versão da troca de dados será um JSON UTF-8 versionado, compartilhável pelo Android. O arquivo conterá apenas dados estruturados, nunca o banco SQLite bruto:

```json
{
  "format": "f21-database-exchange",
  "formatVersion": 1,
  "exportedAt": "",
  "projects": [],
  "stations": [],
  "referencePoints": [],
  "occupations": [],
  "heightMeasurements": [],
  "events": [],
  "artifacts": [],
  "receiverConnectionProfiles": []
}
```

Regras obrigatórias:

- preservar IDs, timestamps, estados, origem e snapshots quando presentes;
- não importar artefato sobrescrevendo outro com o mesmo ID sem registrar conflito;
- validar `format` e `formatVersion` antes de gravar;
- manter `municipality` nulo quando nulo na origem;
- preservar SHA-256 e caminho lógico dos artefatos, sem assumir que o arquivo físico acompanha o JSON;
- realizar importação em transação e rejeitar o conjunto inteiro se houver registro inválido;
- registrar a origem como `IMPORTED` e gerar auditoria da operação.

## Próximo bloco de implementação

1. Criar DTOs de troca no módulo `data`.
2. Implementar exportação por `ACTION_CREATE_DOCUMENT`.
3. Implementar importação por `ACTION_OPEN_DOCUMENT`.
4. Validar e persistir em transação Room, com relatório de conflitos.
5. Criar renderizadores independentes para F-21 e Ficha de Campo usando os PDFs de referência apenas para conferência visual.

Nenhum formato proprietário, regra normativa ou cálculo PPA é inferido por este documento.
