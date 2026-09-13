# F-21 Campo 0.1.7-dev

## Alterações

- Leituras de altura BEFORE e AFTER agora são persistidas como conjunto variável 1..N.
- Regravar uma fase substitui somente as medições daquela fase e evita duplicação.
- Cada medição preserva ID, unidade na proveniência, tipo e instante.
- UI de alturas ganhou cartões de campo e seleção de tipo `VERTICAL`, `SLANT` ou `OTHER`.

## Limitações

- Nenhuma tolerância normativa foi inferida.
- O teste físico da nova UI ainda está pendente.
