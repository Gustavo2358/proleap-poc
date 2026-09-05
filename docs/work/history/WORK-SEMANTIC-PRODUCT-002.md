# WORK-SEMANTIC-PRODUCT-002 — Semantic Product extensível e readiness final

## Resultado

Concluído no PR #27 após os oito checkpoints. O work item substituiu a prova
singleton por um COBOL Semantic Product de produção materializado por
`ProgramUnit`, integrado ao composition root, exposto por port fechado e
transportado por JSON determinístico. Nenhum `CobolLower`, Analysis IR, CFG,
effects, storage ou dataflow foi implementado.

## Contrato entregue

- O envelope publica coleções de DATA e statements com identities namespaced,
  program points estruturais, containment, provenance, coverage e gaps.
- MOVE literal, CALL por identifier/expression e IF estrutural possuem facts
  tipados para todas as ocorrências cobertas; toda outra família observada
  permanece `ObservedStatement` positivo.
- O projector apenas traduz as autoridades canônicas do frontend. Consumer CP6
  e JSON CP7 recebem somente `CobolSemanticPort`.
- A extensão por novas variants de statement é localizada em sealed family,
  índices e handling explícito dos consumers/adapters; o envelope não exige
  redesign por construct.

## Veredito do Checkpoint 8

A hipótese de suficiência foi tentada contra, não presumida. Um probe test-only
que recebe exclusivamente `CobolSemanticPort` reconstrói a espinha estrutural
MOVE → IF/ELSE → CALL e detecta quatro falsificações controladas: membership de
branch removido, binding de CALL degradado, gap de capability escondido e
`ObservedStatement` removido. `ArchitectureBoundaryTest` bloqueia dependências
do probe para frontend, projector, JSON, consumer CP6, presentation e ANTLR.

O resultado por capability é:

- DATA: lowering `READY`, CFG `NOT_APPLICABLE`, effects/dataflow `PARTIAL`;
- MOVE literal → DATA: lowering `PARTIAL`, CFG `READY`, effects/dataflow
  `PARTIAL`, porque literal kind continua `UNKNOWN`;
- CALL identifier/expression: lowering e CFG `READY`, effects/dataflow
  `PARTIAL`; runtime target permanece explicitamente `UNKNOWN`;
- IF/ELSE: lowering `PARTIAL`, CFG estrutural `READY`, effects/dataflow
  `PARTIAL`; predicate semantics não é publicada;
- `ObservedStatement`: inventário disponível e lowering/CFG/effects `BLOCKED`.

Não há dependência escondida do frontend para as claims atualmente
`SUFFICIENT`. Isso permite iniciar um lowering partial-aware, mas não permite
anunciar MOVE, predicate de IF, statements observados, storage/dataflow, CALL
dinâmico final ou a linguagem inteira como completos.

## Conhecimento promovido e handoff

O contrato, a matriz completa, o destino dos gaps, os constraints para a futura
IR e a ordem recomendada estão em [COBOL Semantic Product](../../domain/cobol-semantic-product.md).
Os evals duráveis são `EVAL-SP-001` a `EVAL-SP-003`.

O próximo trabalho deve promover `BACKLOG-IR-001`/`BACKLOG-LOWER-001` com ordem
interna contrato mínimo → lowering boundary-only. Literal kind canônico é
prerequisite de frontend antes de elevar MOVE a ready; `ConditionSemantics` é
prerequisite antes de elevar predicate lowering de IF. Depois seguem CFG,
Statement Effects / Storage Semantics, Reaching Definitions, Possible Values,
dynamic CALL resolution e Dependency Facts. EVALUATE, PERFORM, GO TO/terminal,
ALTER e SEARCH continuam enrichments independentes no backlog.

O diretório ativo foi removido conforme o protocolo. O PR #27 permanece para
review humano e não foi mergeado por este checkpoint.
