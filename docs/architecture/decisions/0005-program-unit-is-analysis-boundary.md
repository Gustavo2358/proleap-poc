# ADR-0005 — Program unit é fronteira de identidade e análise

Status: Accepted
Type: Retrospective
Recorded: 2026-08-28

Decision predates this ADR.

## Context

Uma compilation unit pode conter programas top-level e nested com regras próprias de visibilidade. Uma tabela global conveniente perderia ancestry e permitiria bindings inexistentes.

## Decision

Identificar cada programa por `ProgramUnitId`, preservar parentage em `CompilationUnitModel` e construir symbol tables independentes por unit. Resolução cruza units somente segundo regras COBOL explícitas.

## Rationale

A identidade namespaced preserva nesting, determinismo e fronteiras futuras de paralelização.

## Consequences

Todos os produtos semânticos carregam unit ID; parents precedem nested units; não há lookup global irrestrito da codebase.

## Rejected alternatives

Usar somente o primeiro programa ou uma tabela única global para todos os programas.

## Evidence in current implementation

`CompilationUnitModel`, `CompilationUnitSymbolTables`, `CompilationUnitModelTest` e fixtures de visibilidade/nesting.

## Related invariants

INV-DET-001 e INV-PERF-001.
