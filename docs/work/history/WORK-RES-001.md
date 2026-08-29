# WORK-RES-001 — Observar CALLs literais externos por artefato

Concluído em 2026-08-29. O catálogo externo e seus candidatos sintéticos foram removidos. `CALL` literal sem program unit interno visível agora produz `EXTERNAL_OBSERVED/LITERAL_EXTERNAL_PROGRAM`, sem candidate, diagnostic ou gap.

Programas internos continuam sujeitos às regras de visibilidade existentes. Linkage ausente e target dinâmico por identifier/expression continuam lacunas explícitas. ADR-0010, o domínio de resolução, INV-RES-003 e os evals registram o novo limite por artefato.

Verificado por `check-fast.sh`, `check-semantic.sh`, `check-performance.sh`, regressão E2E do normalizador e `verify-naming.sh`.
