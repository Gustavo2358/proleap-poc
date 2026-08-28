# ADR-0003 — Produtos de análise semântica permanecem separados

Status: Accepted
Type: Retrospective
Recorded: 2026-08-28

Decision predates this ADR.

## Context

AST, declarations, occurrences e bindings possuem ciclos de vida e responsabilidades diferentes. Mesclá-los faria a AST depender de policy de resolução e permitiria mutação entre fases.

## Decision

Manter `Ast`, `SymbolTable`, `ReferenceOccurrences` e `ReferenceResolution` como produtos separados e imutáveis. O builder de símbolos consome somente `Ast.Program`; o collector não faz lookup; o resolver não grava decisões nos produtos anteriores.

## Rationale

A separação torna uncertainty e candidate sets explícitos, mantém a AST reutilizável e evita dependências reversas.

## Consequences

Consumidores combinam produtos por IDs estáveis. Conveniência de frontend ou snapshot não redefine o modelo semântico.

## Rejected alternatives

Anotar nós AST com `symbolId`, candidato escolhido ou estado de resolução; fazer symbol table coletar usos e bindings.

## Evidence in current implementation

Comentários e APIs de `Ast`, `SymbolTableBuilder`, `ReferenceOccurrences`, `ReferenceResolution` e testes com `bindingStatus=NOT_PERFORMED`.

## Related invariants

INV-AST-001, INV-SYM-001, INV-RES-001 e INV-PERF-001.
