# ADR 0002 — Separação LAB x OPERATIONAL

**Status:** Aceito no bootstrap.

O produto terá flavors `operational` e `lab`.
O flavor LAB poderá receber ferramentas de diagnóstico e engenharia reversa em commits futuros.
O flavor OPERATIONAL não deve expor comandos experimentais ou não homologados.
