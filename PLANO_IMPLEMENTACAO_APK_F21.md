# Plano de implementação do APK — F-21 Campo

## Objetivo

Construir um aplicativo Android offline-first para registrar rastreios GNSS de campo, preservar proveniência, manter histórico e preparar os dados para futura geração de produtos F-21.

O APK deverá possuir dois flavors:

- `operational`: uso normal em campo;
- `lab`: bancada, diagnóstico e testes.

## Regras gerais

- não inventar protocolos de receptores;
- não declarar integração Spectra sem evidência;
- não reproduzir ou aproximar PPA;
- não transformar dado informado pelo operador em dado detectado;
- não substituir arquivos brutos originais;
- preservar histórico e identidade das estações;
- manter o aplicativo funcional offline;
- separar domínio, persistência, UI e adapters de equipamento.

## Fase 1 — Bootstrap e toolchain

### Implementado

- projeto Android Kotlin + Compose;
- flavors `operational` e `lab`;
- módulos independentes de fabricante;
- AGP 9 com Kotlin integrado;
- Gradle 9.6;
- workflow GitHub Actions;
- geração e publicação dos APKs.

### Critérios de aceite

- testes unitários verdes;
- `operationalDebug` verde;
- `labDebug` verde;
- instalação física dos dois APKs;
- tela exibe versão e flavor correto.

## Fase 2 — Primitivas do domínio

### Implementado

- `EntityId` baseado em UUID;
- relógio injetável;
- SHA-256 em hexadecimal minúsculo;
- proveniência;
- estados de validação;
- erros estruturados;
- resultados de domínio.

### Critérios de aceite

- domínio sem dependência Android;
- testes determinísticos;
- enums persistíveis independentes de tradução.

## Fase 3 — Entidades do domínio

### Implementado

- `Project`;
- `Station`;
- `Receiver`;
- `Antenna`;
- `Occupation`;
- snapshots de equipamento;
- medições de altura;
- eventos;
- arquivos;
- metadados RINEX;
- resultados de processamento;
- coordenadas;
- versões F-21;
- auditoria.

### Critérios de aceite

- edição de estação preserva o mesmo ID;
- `municipality` ausente continua ausente;
- receptor e antena continuam entidades distintas;
- equipamento antigo não muda quando o catálogo é editado;
- seis leituras só devem ser mantidas se confirmadas pelo procedimento real.

## Fase 4 — Room e persistência

### Implementado parcialmente

- entidades Room iniciais;
- DAOs;
- converters;
- mappers domínio ↔ Room;
- repository;
- banco versão 1;
- Room 2.8.4;
- KSP compatível com o toolchain;
- schema gerado no CI.

### Pontos a concluir

- incluir todas as entidades no banco;
- versionar os schemas gerados;
- migration test com banco antigo;
- teste in-memory;
- teste de fechamento/reabertura;
- impedir perda de dados;
- não usar `fallbackToDestructiveMigration`.

## Fase 5 — Estações e projetos

### Implementado parcialmente

- cadastro de estação;
- edição básica;
- preservação de ID;
- salvamento pelo repository;
- UI inicial.

### Pontos a implementar

- tela inicial semelhante ao C32;
- `Novo rastreio`;
- `Continuar`;
- `Carregar projeto`;
- `Configurações e sobre`;
- busca de estações;
- arquivamento lógico;
- histórico de alterações;
- dados de projeto separados dos dados da estação.

## Fase 6 — Máquina de estados da ocupação

### Implementado

- `DRAFT`;
- `READY`;
- `ACTIVE`;
- `STOPPED`;
- `COLLECTED`;
- `VALIDATED`;
- `ABORTED`;
- transições inválidas rejeitadas;
- timestamps confirmados preservados;
- bruto obrigatório antes de `COLLECTED`.

### Pontos a implementar na UI

- indicador visual de estado;
- botão contextual por estado;
- `Nova ocupação` após `STOPPED`;
- recuperação automática após process death;
- confirmação antes de abortar;
- timeline da ocupação.

## Fase 7 — Receptor e antena manual

### Implementado

- adapter vendor-neutral;
- adapter manual;
- capabilities automáticas desativadas;
- cadastro de receptor e antena;
- snapshot por ocupação.

### Pontos a implementar

- catálogo de receptores;
- catálogo de antenas;
- número de série;
- fabricante e modelo;
- edição sem alterar snapshots antigos;
- seleção do equipamento na ocupação;
- indicação clara de que o dado foi informado manualmente.

## Fase 8 — Alturas da antena

### Implementado

- leituras `BEFORE` e `AFTER`;
- tipo `VERTICAL`, `SLANT`, `OTHER`;
- média;
- amplitude;
- delta;
- rejeição de `NaN` e `Infinity`.

### Decisão pendente

Confirmar se o procedimento real usa:

- uma medida;
- três medidas;
- três antes e três depois;
- outro conjunto de observações.

O APK não deve obrigar seis medidas sem confirmação operacional.

## Fase 9 — Eventos e auditoria

### Implementado

- categorias de evento;
- severidades;
- timeline ordenada;
- `AuditEvent` append-only;
- separação entre evento operacional e auditoria do sistema.

### Pontos a implementar

- persistência da timeline em Room;
- tela de eventos;
- filtros por severidade;
- exportação do histórico;
- registro automático das transições da ocupação.

## Fase 10 — Arquivo bruto e Gate R2

### Implementado

- seletor de arquivos Android;
- cópia para armazenamento controlado;
- arquivo temporário `.part`;
- SHA-256 em streaming;
- validação do tamanho;
- deduplicação por conteúdo;
- imutabilidade lógica.

### Pontos a implementar

- associação persistente do bruto à ocupação;
- metadados do arquivo;
- listagem dos arquivos associados;
- validação pós-reabertura;
- impedir substituição do original;
- tela de confirmação do hash.

## Fase 11 — Layout inspirado no C32

### Estrutura proposta

1. Tela inicial
   - Novo rastreio
   - Continuar rastreio
   - Carregar projeto
   - Configurações e sobre

2. Dados gerais
   - projeto;
   - comissão/ocasião;
   - equipe;
   - responsável;
   - data;
   - observações.

3. Estação e referência
   - estação;
   - RN, MT ou PA;
   - código da referência;
   - município e localidade;
   - dados comuns e dados exclusivos.

4. Equipamento
   - receptor;
   - antena;
   - números de série;
   - origem manual ou detectada.

5. Ocupação
   - estado;
   - iniciar;
   - parar;
   - abortar;
   - nova ocupação.

6. Medições
   - altura conforme procedimento confirmado;
   - tipo de medida;
   - observações;
   - validações.

7. Arquivos e eventos
   - importar bruto;
   - SHA-256;
   - eventos de campo;
   - timeline.

8. Resumo
   - dados do rastreio;
   - equipamento;
   - medições;
   - arquivos;
   - auditoria;
   - finalização.

## Fase 12 — Teste físico Gate R2

Executar no aparelho:

1. criar projeto;
2. criar estação;
3. criar ocupação;
4. selecionar receptor e antena;
5. registrar alturas;
6. colocar em `READY`;
7. iniciar;
8. registrar evento;
9. parar;
10. importar arquivo bruto real;
11. validar SHA-256;
12. fechar/matar o aplicativo;
13. reabrir;
14. confirmar todos os dados.

## Fase 13 — Integrações futuras

Somente após o Gate R2 estável:

- validação RINEX;
- processamento;
- coordenadas;
- renderer F-21;
- `.hnproject`;
- adapter Spectra real;
- homologação de receptores;
- banco histórico e gráficos temporais.

## Critério de conclusão do APK inicial

O APK inicial será considerado pronto quando:

- o fluxo completo funcionar offline;
- os dados sobreviverem ao fechamento do app;
- o bruto permanecer íntegro;
- o SHA-256 for reproduzível;
- a estação mantiver identidade;
- o equipamento antigo permanecer como snapshot;
- os estados inválidos forem bloqueados;
- os APKs operational e lab forem testados fisicamente;
- as limitações forem documentadas.
