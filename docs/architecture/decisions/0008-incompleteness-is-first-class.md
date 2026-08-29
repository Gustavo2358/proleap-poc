# ADR-0008 — Incompletude é resultado de primeira classe

Status: Accepted
Type: Retrospective
Recorded: 2026-08-28

Decision predates this ADR.

## Context

COPY ausente, erro de frontend, construção preservada e binding não concluído podem deixar fatos conhecidos e lacunas simultaneamente.

## Decision

Representar coverage, dependency knowledge, status e motivos explicitamente. `UNSUPPORTED`, `INPUT_MISSING`, `DEPENDENCY_UNKNOWN`, `UNRESOLVED` e `AMBIGUOUS` permanecem observáveis e bloqueiam alegação incompatível de completude.

## Rationale

Uma análise conservadora precisa distinguir ausência provada de incapacidade ou falta de input.

## Consequences

Relatórios e snapshots transportam gaps e diagnostics; consumidores devem consultar readiness antes de usar resultados como completos.

## Rejected alternatives

Fallback para coleção vazia, primeiro candidato ou sucesso parcial sem indicador de incompletude.

## Evidence in current implementation

`SemanticCoverage`, `ResolutionAnalysisReport`, enums de resolução e testes de coverage/readiness.

## Related invariants

INV-COV-001, INV-RES-001 e INV-RES-003.
