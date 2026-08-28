# ADR-0009 — Superfície gramatical possui cobertura explícita

Status: Accepted
Type: Retrospective
Recorded: 2026-08-28

Decision predates this ADR.

## Context

As gramáticas versionadas aceitam centenas de regras. Fallback implícito faria uma alternativa nova parecer suportada ou semanticamente vazia.

## Decision

Manter manifesto versionado que classifica cada regra do frontend, além de catálogos fechados para políticas específicas do preprocessor e da normalização. Mudança de grammar sem classificação correspondente deve falhar em teste.

## Rationale

A cobertura explícita torna a fronteira supported/preserved/unsupported auditável e independente do corpus.

## Consequences

Toda evolução de grammar exige decisão de coverage; manifestos são carregados por índice determinístico.

## Rejected alternatives

Inferir suporte pela presença no corpus, por nome da regra ou por fallback genérico silencioso.

## Evidence in current implementation

`grammar-rule-manifest.tsv`, `GrammarCoverageManifest`, `ReferenceResolutionManifest`, policy do preprocessor e seus testes de exaustividade.

## Related invariants

INV-AST-002 e INV-COV-002.
