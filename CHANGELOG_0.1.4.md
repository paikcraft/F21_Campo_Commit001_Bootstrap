# F-21 Campo 0.1.4-dev

## Fluxo de campo

- READY bloqueia e direciona para o requisito pendente.
- Projetos, estações e referências RN/MT/PA salvos podem ser reutilizados.
- Rastreios salvos podem ser consultados pelo Banco de Estações.
- Eventos, alturas, RAW e resumo de SHA-256 aparecem na consulta posterior.
- Android Back retorna à etapa anterior ou ao início sem descartar o rastreio autosalvo.

## Integridade

- A cópia RAW em `.part` recebe verificação de tamanho e SHA-256 antes da finalização.
- Arquivo deduplicado já existente é conferido antes de ser reutilizado.
- Perfis de conexão de bancada são persistidos em Room 13, por migration explícita.

## Wi-Fi de bancada

- O aplicativo pode testar se uma porta TCP informada aceita conexão.
- O teste não envia comandos ou bytes Spectra.

## Interface

- Hierarquia visual aproximada do C32 de Campo: cabeçalhos azuis, cartões operacionais, ação primária destacada e ações secundárias contornadas.
- Rastreio ativo e Finalização receberam cabeçalhos operacionais.
