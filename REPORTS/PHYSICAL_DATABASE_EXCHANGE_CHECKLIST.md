# Validação física — troca do banco de dados

## Objetivo

Comprovar que o envelope JSON exportado pelo APK OPERATIONAL pode ser compartilhado, importado em outra instalação e reaberto sem perda dos registros estruturados.

## Procedimento

1. No APK de origem, criar ou confirmar um projeto, uma estação com localidade, referências RN/MT/PA, uma ocupação e pelo menos uma medição BEFORE e AFTER.
2. Registrar um evento operacional.
3. Associar um artefato RAW de teste e confirmar o SHA-256 exibido.
4. Usar **Exportar banco para compartilhar** e salvar o JSON.
5. Copiar o JSON para o aparelho ou instalação de destino.
6. No APK de destino, usar **Importar banco JSON**.
7. Confirmar a mensagem de contagem e conflitos atualizados.
8. Fechar o aplicativo, removê-lo dos recentes e abrir novamente.
9. Conferir projetos, estação, localidade, referências, ocupação, estado, alturas, evento e metadados do RAW/SHA-256.

## Evidência a registrar

- APK/versionCode de origem e destino:
- Data/hora do teste:
- Resultado da exportação:
- Resultado da importação:
- Quantidades mostradas pelo resumo:
- IDs preservados:
- `municipality = null` preservado quando aplicável:
- Alturas BEFORE/AFTER preservadas:
- Evento preservado:
- RAW e SHA-256 conferidos:
- Dados ainda presentes após fechar/reabrir:
- Observações/falhas:

## Limitação conhecida

O JSON de troca preserva os metadados do artefato (papel, caminho, tamanho e SHA-256). A cópia dos bytes do arquivo RAW precisa ser validada separadamente; um caminho externo não deve ser interpretado como arquivo fisicamente transferido.
