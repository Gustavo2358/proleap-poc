# ADR-0004 — Binding nominal não resolve valores de runtime

Status: Accepted
Type: Retrospective
Recorded: 2026-08-28

Decision predates this ADR.

## Context

Um alvo de `CALL` pode ser literal ou expressão. Associar uma variável à sua declaração não determina quais valores chegarão a ela por diferentes caminhos de controle.

## Decision

Reference resolution termina no binding nominal. Valores possíveis, targets dinâmicos, reaching definitions e propagação de constantes pertencem a CFG/dataflow futuros e serão produtos separados.

## Rationale

Inferência de valor sem controle de fluxo fabricaria certeza e misturaria fases.

## Consequences

`CALL WS-TARGET` resolve DATA e permanece sem programa final. Targets conhecidos e remainder dinâmico deverão coexistir quando a análise futura for parcial.

## Rejected alternatives

Usar `VALUE`, `MOVE` anterior no texto ou ordem linear como target final dentro do resolver nominal.

## Evidence in current implementation

`Ast.CallTargetSyntax`, `ReferenceResolution.CallSemantics`, fixtures de CALL dinâmico e `CallSemanticsTest`.

## Related invariants

INV-RES-002.
