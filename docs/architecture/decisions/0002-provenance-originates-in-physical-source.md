# ADR-0002 — Provenance começa no fonte físico

Status: Accepted
Type: Retrospective
Recorded: 2026-08-28

Decision predates this ADR.

## Context

Normalização, COPY e preprocessing transformam o texto antes do parser. Criar posições somente sobre o texto resultante perderia arquivo, linha física, cadeia de inclusão e grau de exatidão.

## Decision

Criar o `SourceMap` a partir do arquivo físico e compô-lo em toda transformação. Segmentos transformados conservam origem e declaram `exact=false`; COPYs mantêm include chain.

## Rationale

Provenance é dado semântico consumido por diagnostics, AST e snapshots, não decoração reconstruível no final.

## Consequences

Toda transformação precisa transportar o mapa; APIs que recebem somente texto não podem representar o pipeline completo.

## Rejected alternatives

Recriar identity map após normalização ou preprocessing.

## Evidence in current implementation

`SourceNormalizer.Result`, `SourceMap`, `PreprocessorEngine.Outcome` e `SourceProvenanceTest`.

## Related invariants

INV-PROV-001 e INV-PROV-002.
