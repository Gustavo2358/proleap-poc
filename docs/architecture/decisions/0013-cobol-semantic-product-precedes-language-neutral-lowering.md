# ADR-0013 — Semantic Product COBOL precede lowering para Analysis IR

Status: Accepted
Type: Contemporary
Recorded: 2026-09-05

## Context

O frontend produz AST, compilation units, símbolos, occurrences, resolução,
coverage, provenance e produtos pós-binding separados. O Discovery de
`WORK-SEMANTIC-PRODUCT-001` demonstrou que expor esses internals diretamente
não forma uma boundary fechada para consumidores downstream. Também demonstrou
que a semântica necessária antes do lowering contém conceitos próprios de
COBOL, enquanto CFG e dataflow precisam de uma representação orientada a
análise, não de uma cópia da AST.

O primeiro fixture de produção provou a boundary com uma DATA, um `MOVE` e um
`CALL`. Essa cardinalidade era evidência mínima, não um limite semântico da
`ProgramUnit`. Congelá-la no contrato faria código suportado desaparecer ou
obrigaria cada nova família de statement a remodelar o envelope.

## Decision

Adotar a seguinte fronteira arquitetural:

```text
COBOL Frontend
  → COBOL Semantic Product
  → CobolLower
  → Analysis IR
  → CFG
  → Statement Effects / Storage Semantics
  → Reaching Definitions
  → Possible Values
  → Dependency Facts
```

1. O Semantic Product é COBOL-specific. Ele materializa um estado próprio,
   imutável, fechado e namespaced, exposto por port/facade read-only (A2+B).
   Tipos do parser, AST, symbol table, occurrences, resolver, report,
   presentation e composition root não atravessam como API da boundary.
2. Neutralidade entre linguagens começa no lowering e na Analysis IR, ou em
   uma fronteira imediatamente associada a eles. Outros frontends possuem seus
   próprios produtos semânticos e lowerers; não existe Semantic Product
   universal pré-lowering.
3. Um vertical slice limita as capabilities semânticas que o produto afirma
   suportar, nunca a quantidade de ocorrências dessas capabilities numa
   `ProgramUnit`. Todas as ocorrências cobertas são publicadas. Constructs
   observados mas parciais, unsupported ou unknown permanecem informação
   positiva e não são convertidos em ausência.
4. O produto preserva structure, nesting, operands, roles, identities, binding
   nominal, provenance, coverage e incompletude suficientes para o lowering.
   Ele não publica IR, edges de CFG, `GEN/KILL`, reaching definitions,
   possible-values ou targets dinâmicos finais.
5. Um projector de frontend somente traduz e reconcilia produtos canônicos já
   materializados. AST tipada governa surface/shape; compilation units,
   namespace; symbols, declarations; occurrences, roles; resolution,
   binding/candidates/status; reports, seus gaps/readiness/claims; e os
   produtos de provenance/policy, os fatos que lhes pertencem. O projector não
   reparseia texto, resolve nomes, recalcula análise ou infere runtime.
6. A suficiência downstream é requisito do contrato. Cada construct declara
   separadamente surface, identity, structure, nominal binding, CFG readiness,
   effects/dataflow readiness, unknowns, provenance e coverage. Readiness
   parcial não pode ser apresentada como completa.
7. Identidade nominal de DATA não implica identidade final de storage.
   `REDEFINES`, `RENAMES`, layout e overlap exigem Storage Semantics posterior.
8. Handles e ordem podem ser determinísticos para execuções equivalentes e
   transporte versionado. Isso não promete identidade persistente após edição
   estrutural, mudança de analyzer ou mudança de contract version.

A representação concreta de branches — hierárquica, flat por identities ou
híbrida — e nomes exatos de classes continuam decisões de implementação. Toda
forma escolhida deve permitir reconstruir nesting e relações estruturais sem
AST e sem depender de posição incidental.

## Rationale

COBOL-specific facts são necessários para não forçar a IR a conhecer detalhes
da linguagem nem obrigar o lowerer a reabrir produtos do frontend. Uma boundary
materializada fornece closure e coerência; o port mantém a dependência do
consumer estreita. Separar capability de cardinalidade permite evolução
incremental sem produzir uma visão artificialmente truncada da unit. Readiness
por dimensão impede que estrutura conhecida seja confundida com controle,
efeitos ou valores ainda desconhecidos.

## Consequences

- O container do Semantic Product cresce por coleções/famílias tipadas de
  facts e capabilities, não por um singleton novo para cada construct.
- `ProgramPoint`/anchor do produto representa ordem estrutural determinística;
  não prova execution order, reachability ou edge de CFG.
- `CobolLower` deve consumir somente o contrato do Semantic Product e traduzir
  facts COBOL para Analysis IR; não executa parsing ou binding nominal.
- CFG depende de IR e da CFG readiness dos constructs. Effects/reaching
  definitions dependem de CFG e Storage Semantics suficientes. Possible Values
  e targets dinâmicos dependem desses fatos, sem atalhos textuais.
- Transporte JSON só pode ser congelado depois que a estrutura do produto e um
  consumer de lowering-readiness estiverem corretos; JSON permanece adapter,
  não domínio.
- A implementação estreita atual continua evidência histórica e estado a
  remediar por `WORK-SEMANTIC-PRODUCT-002`; esta decisão não implementa nenhuma
  fase da cadeia.

## Rejected alternatives

- Um Semantic Product universal ou language-neutral antes do lowering.
- `CobolLower` consumindo diretamente AST, symbols, occurrences, resolution ou
  report e reproduzindo joins do frontend.
- Semantic Product contendo Analysis IR, CFG ou fatos de dataflow.
- Projector que reanalisa texto, resolve referências ou deriva runtime values.
- Fixture mínima transformada em cardinalidade máxima do contrato.
- State com campos singleton por construct ou bag dinâmico sem tipos.

## Evidence

- `WORK-SEMANTIC-PRODUCT-001` e seus relatórios dos Checkpoints 2, 3A e 3B;
- implementação inicial de `CobolSemanticProduct`, `CobolSemanticPort` e do
  projector focalizado;
- ADR-0003, ADR-0004, ADR-0005 e ADR-0008;
- INV-COV-001/003, INV-RES-002 e INV-DET-001.

## Related invariants

INV-SP-001 a INV-SP-006, INV-AST-001, INV-RES-002, INV-COV-001,
INV-COV-003 e INV-DET-001.
