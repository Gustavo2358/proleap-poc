# Estado

## Onde estamos

O contrato deste work item foi corrigido documentalmente no PR #27. O target
de produção deixa de ser o fixture singleton e passa a ser uma `ProgramUnit`
com todas as ocorrências semanticamente cobertas de DATA, MOVE literal, CALL
variável e IF/ELSE, mais inventário explícito de constructs observados que sejam
partial, unsupported ou bloqueados por input ausente.

ADR-0013 e INV-SP-001–006 fixam a direção durável. `spec.md`, `plan.md` e
`eval.md` agora descrevem a correção em oito checkpoints independentes. Esta
migração é somente harness/documentação: nenhum arquivo de produção ou teste do
Semantic Product foi alterado por ela.

A implementação continua no estado anterior ao contrato corrigido. Depois que
os seis checkpoints documentais desta migração terminarem, o próximo trabalho
autorizado é o Checkpoint 1 do novo `plan.md`: um oracle executável, test-only,
do target model, sem alteração de produção.

## Verde conhecido

- `WORK-SEMANTIC-PRODUCT-001` e os relatórios 3A/3B provaram a alternativa
  A2+B: state COBOL-specific, materializado, imutável, partial-aware e
  namespaced, exposto por port fechado, tipado e read-only.
- O fixture mínimo continua provando, como caso N=1, uma DATA, um MOVE literal,
  um CALL variável, binding nominal comum, provenance, ordering e runtime
  target `UNKNOWN` com `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN`.
- `CobolSemanticPort` consulta somente o state publicado; a boundary atual não
  expõe parser, AST, symbols, occurrences, resolution ou presentation.
- `SemanticProductMoveCallContractTest` protege closure, imutabilidade,
  identities namespaced, separação nominal/runtime, partialidade e consulta
  fora de ordem para o slice implementado.
- `Ast.IfStatement` já preserva condition, THEN, ELSE, termination e nesting;
  `Ast.children` e o collector percorrem sua estrutura e referências. Isso
  sustenta o slice estrutural de IF/ELSE, não predicate semantics, CFG,
  reachability ou dataflow.
- AST, compilation units, symbol tables, occurrences, resolution, report,
  policy, provenance e presentation permanecem produtos separados e
  autoridades de suas próprias semânticas.
- Não há composition-root publication nem JSON do Semantic Product em
  produção. Lowering, Analysis IR, CFG, effects/storage e dataflow também não
  estão implementados por este work item.

## Restante

- Concluir os Checkpoints 4–6 desta migração documental: backlog/handoffs,
  auditoria integral e remoção dos dois documentos transitórios.
- Executar depois, como próximo trabalho autorizado, o Checkpoint 1 corretivo:
  fixture e oracle/consumer test-only do target model com multiple
  DATA/MOVE/CALL, IF/ELSE, nesting, incompletude e readiness.
- Executar os Checkpoints 2–8 do `plan.md` somente na ordem registrada, com a
  evidência do anterior e a autorização aplicável. Eles remodelam A2+B,
  corrigem projection, acrescentam IF/coverage, integram o composition root,
  provam lowering-readiness, publicam JSON por último e fecham o handoff.
- Manter EVALUATE, PERFORM, GO TO, terminal semantics, ALTER, SEARCH,
  CobolLower, IR, CFG e dataflow em backlog até seus pré-requisitos e work
  items próprios.

## Descobertas que afetam o plano

- `CobolSemanticProduct.State` ainda contém `MoveFact move`, `CallFact call` e
  `Ordering ordering`; `CobolSemanticPort` ainda publica `move()`, `call()` e
  `ordering()`. Isso é estado implementado, não arquitetura desejada.
- `CobolMoveCallAdapter` ainda usa `single(...)`, exige exatamente um MOVE, um
  CALL e um target por statement, além de exigir o mesmo `DataItemId` no par.
  O Checkpoint 3 corretivo precisa substituir seleção por publicação de todas as
  ocorrências cobertas.
- O adapter atual não consome `ResolutionAnalysisReport`; cria localmente o gap
  do CALL dinâmico a partir de `CallSemantics`. A correção deve projetar cada
  autoridade canônica sem refazer análise ou reconciliar por texto.
- O projector atual publica somente a DATA participante do par MOVE/CALL.
  Declarations independentes, statements adicionais e incompletude da unit
  ainda não formam um produto fechado.
- `DataItemId` representa identidade nominal determinística no escopo da unit;
  não representa sozinho storage físico, região de alias ou layout final.
- Program points/ordering atuais são anchors estruturais. Não constituem
  execution order, reachability ou edges de CFG.
- IF/ELSE tem surface suficiente para o slice estrutural inicial. EVALUATE é
  partial por F-01; PERFORM permanece bloqueado pelos controles incompletos de
  F-SP-007, inclusive TIMES, test mode, VARYING e AFTER.
- O futuro consumer de lowering-readiness precisa depender somente do port.
  Determinismo de handles/JSON dentro do mesmo contrato de transporte não cria
  identidade persistente entre edições, versões ou execuções distintas.
