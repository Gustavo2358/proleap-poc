# ADR-0010 — Dependências literais externas são observadas por artefato

Status: Accepted
Type: Contemporary
Recorded: 2026-08-29

## Context

O produto analisa uma compilation unit individual e não pretende manter catálogo ou índice da codebase. Um `CALL` literal sem program unit interno visível ainda revela uma dependência nominal conhecida.

## Decision

Remover `ExternalProgramCatalog`. Após aplicar exclusivamente as regras de visibilidade interna, registrar um target literal restante como `EXTERNAL_OBSERVED/LITERAL_EXTERNAL_PROGRAM`, sem candidate, símbolo sintético ou lookup entre artefatos.

## Consequences

A observação externa não é gap nem reduz readiness. Ela preserva o nome e a localização da dependência. Incertezas independentes — opções de linkage ausentes ou inválidas e target por identifier/expression — permanecem explícitas e bloqueantes.

## Rejected alternatives

Tratar o literal como `RESOLVED` sem identidade, manter `UNRESOLVED` e apenas ocultar o gap, ou varrer fontes fora do artefato.

## Related invariants

INV-RES-002, INV-RES-003 e INV-COV-001.
