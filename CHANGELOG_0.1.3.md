# F-21 Campo 0.1.3-dev

## Alteracoes principais

- Fluxo READY/INICIAR recebeu tela propria para evitar retorno ao formulario antigo.
- Conexao de bancada foi adicionada como perfil neutro, sem comandos Spectra automaticos.
- Alturas BEFORE 1..N agora sao persistidas ao avancar para READY.
- Finalizacao exige altura AFTER persistida e RAW_RECEIVER associado antes de coletar.
- Continuar rastreio recarrega projeto/LH, estacao, referencia RN/MT/PA, alturas, unidade e evidencias.
- Snapshot manual de receptor/antena teve o mapeamento de fabricante e numero de serie corrigido.
- Testes de dominio foram alinhados ao modelo de alturas 1..N, removendo a suposicao 3+3.

## Limites mantidos

- Sem protocolo Spectra automatico.
- Sem START/STOP remoto.
- Sem PPA ou indicador equivalente.
- Sem parser RINEX completo.
- Sem renderer F-21 definitivo.
