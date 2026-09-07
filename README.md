# F-21 Campo

Bootstrap do novo aplicativo Android para rastreabilidade de ocupações GNSS e futura geração de F-21.

## Estado

**Commit 001 / Bootstrap.** Ainda não há funcionalidade operacional de campo.

## Arquitetura

- Kotlin
- Jetpack Compose
- núcleo multi-receptor
- flavors `operational` e `lab`
- domínio independente de fabricante

## Limitações atuais

- Spectra ainda não integrada automaticamente.
- Nenhum protocolo proprietário implementado.
- PPA não implementado e não será inventado.
- Project/Station/Occupation entram nos commits seguintes.

Veja `BUILD.md` e `docs/adr/`.
