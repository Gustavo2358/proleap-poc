# ADR-0007 — Linguagens embarcadas usam fronteiras dedicadas

Status: Accepted
Type: Retrospective
Recorded: 2026-08-28

Decision predates this ADR.

## Context

`EXEC SQL`, `EXEC CICS` e `EXEC SQLIMS` possuem sintaxe e semântica próprias. O parser COBOL pode preservar seus blocos, mas não provar dependências internas.

## Decision

Representar o bloco como `EmbeddedLanguageStatement` opaco, com payload e provenance. Interpretação futura usará analisador dedicado; o core COBOL não extrai dependências por regex.

## Rationale

Preservar o payload mantém auditabilidade sem atribuir completude falsa.

## Consequences

Coverage permanece `PRESERVED_UNINTERPRETED/DEPENDENCY_UNKNOWN` até existir integração dedicada.

## Rejected alternatives

Ignorar o bloco ou reconhecer tabelas, programas e host variables por expressões regulares oportunistas.

## Evidence in current implementation

`Ast.EmbeddedLanguageStatement`, `AstBuilder.buildEmbedded`, snapshots e testes de AST/coverage.

## Related invariants

INV-EMB-001 e INV-COV-001.
