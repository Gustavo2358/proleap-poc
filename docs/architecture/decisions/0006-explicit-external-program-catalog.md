# ADR-0006 — Programas externos dependem de catálogo explícito

Status: Superseded
Type: Retrospective
Recorded: 2026-08-28

Decision predates this ADR.

## Context

O fonte analisado não contém necessariamente todos os programas chamáveis. Ausência de declaração local e ausência de inventário externo são estados diferentes.

## Superseded by

ADR-0010 — Dependências literais externas são observadas por artefato.

## Former decision

Resolver programas internos pelas regras COBOL de visibilidade e programas externos somente pela porta opcional `ExternalProgramCatalog`. Catálogo não fornecido produz `EXTERNAL_CATALOG_NOT_PROVIDED`; catálogo fornecido e vazio pode produzir `DECLARATION_NOT_FOUND` no domínio consultado.

## Rationale

O catálogo explicita o input externo e impede que falta de dados seja tratada como inexistência semântica.

## Consequences

Execuções sem catálogo permanecem incompletas para dependências externas. O core não varre nem indexa codebases implicitamente.

## Rejected alternatives

Criar símbolos externos fictícios, ligar qualquer nome por busca global ou converter catálogo ausente em ausência de programa.

## Evidence in current implementation

`ExternalProgramCatalog`, `CobolReferenceResolver` e testes de resolução PROGRAM/CALL externo.

## Related invariants

INV-RES-003 e INV-COV-001.
