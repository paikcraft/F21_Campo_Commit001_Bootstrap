# Dúvidas pendentes — F-21 Campo

Este documento reúne as questões que precisam ser confirmadas antes de consolidar o fluxo de campo e o layout definitivo.

## 1. Altura da antena

O prompt de arquitetura recebido especifica seis observações:

- três leituras `BEFORE`;
- três leituras `AFTER`;
- cálculo de média, amplitude e delta.

Ainda falta confirmar se esse procedimento corresponde ao procedimento real do F-21.

### Pergunta

A altura da antena deve ser:

- uma única medida por rastreio;
- três medidas antes e três depois;
- outra quantidade ou outro procedimento?

Não devemos manter seis leituras apenas por compatibilidade com o prompt se isso não corresponder à operação real.

## 2. Arquivo bruto

O termo “arquivo bruto” foi usado para a evidência original produzida durante o rastreio.

Ainda não está confirmado:

- qual arquivo o receptor Spectra produz;
- se o arquivo é proprietário ou RINEX;
- se o arquivo é copiado diretamente do receptor ou gerado posteriormente;
- em que momento o arquivo entra no fluxo;
- se um PPP importado deve ser tratado como produto processado, e não como bruto.

### Pergunta

Qual arquivo deve ser importado no F-21 Campo ao terminar um rastreio?

## 3. Integração Spectra

Ainda não há evidência suficiente para declarar:

- conexão automática;
- protocolo;
- porta ou endpoint;
- comando de início/parada;
- download automático;
- leitura automática de receptor e antena.

Até haver evidência, o Spectra deve aparecer apenas como equipamento informado manualmente.

## 4. Layout inspirado no C32 de campo

A tela atual concentra estação, ocupação, equipamento, alturas e eventos em uma única tela longa.

Foi identificada a necessidade de reorganizar o aplicativo em fluxo semelhante ao C32:

- tela inicial;
- novo rastreio;
- continuar rastreio não finalizado;
- carregar projeto;
- configurações e sobre;
- dados gerais do rastreio;
- estação/referência;
- receptor/antena;
- medições;
- eventos e arquivos;
- resumo e finalização.

### Perguntas

- O aplicativo deve trabalhar com “projeto” como unidade principal, como no C32?
- Deve existir uma tela inicial com `Novo rastreio`, `Continuar`, `Carregar projeto` e `Configurações`?
- O rastreio deve ser salvo automaticamente como rascunho?
- Quais campos devem aparecer na primeira tela?
- O usuário deve poder clonar os dados da referência anterior?

## 5. Estação, referência e rastreio

Ainda precisa ser definida a separação visual entre:

- dados imutáveis da estação;
- dados da comissão/ocasião;
- dados da referência rastreada;
- dados exclusivos daquele rastreio.

### Perguntas

- Quais campos pertencem à estação?
- Quais pertencem à comissão/ocasião?
- Quais pertencem à referência RN, MT ou PA?
- O usuário pode editar dados da estação durante um rastreio?
- A edição deve criar versão histórica ou apenas atualizar o cadastro?

## 6. RN, MT e PA

O modelo deve manter a seleção mutuamente exclusiva entre:

- RN;
- MT;
- PA.

### Perguntas

- O código da referência é sempre digitado pelo usuário?
- Há campos diferentes para RN, MT e PA?
- O tipo da referência pode ser alterado depois de iniciado o rastreio?
- Deve existir validação formal dos formatos desses códigos?

## 7. Produto PPP e fichas

Ainda precisa ser definido o limite entre:

- dados de campo;
- arquivo bruto;
- processamento PPP;
- produto F-21;
- ficha de campo.

### Perguntas

- O PPP será importado no aplicativo de campo ou somente no gabinete?
- O aplicativo de campo deve armazenar o PPP ou apenas o bruto?
- O produto gerado deve ser F-21, ficha, ou ambos?
- Quais dados do PPP são confiáveis para preencher automaticamente?

## 8. Validação física necessária

Antes de fechar o fluxo definitivo, ainda será necessário testar no aparelho:

- criar novo rastreio;
- salvar e continuar depois de fechar o aplicativo;
- criar nova ocupação após `STOPPED`;
- registrar a quantidade correta de alturas;
- importar o arquivo bruto real;
- confirmar o SHA-256;
- recuperar os dados após reabrir.

## Recomendação atual

As decisões mais urgentes são:

1. confirmar a quantidade real de medidas de altura;
2. identificar o arquivo bruto real do receptor;
3. aprovar a navegação em telas inspirada no C32;
4. separar estação, comissão/ocasião, referência e rastreio;
5. somente depois consolidar o Gate R2 físico.
