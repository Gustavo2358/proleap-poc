# Estado

## Onde estamos

Os seis checkpoints da migração documental foram executados no PR #27, que
permanece sob review. O target de produção deixou de ser o fixture singleton e
passou a ser uma `ProgramUnit` com todas as ocorrências semanticamente cobertas
de DATA, MOVE literal, CALL variável e IF/ELSE, mais inventário explícito de
constructs observados que sejam partial, unsupported ou bloqueados por input
ausente.

ADR-0013 e INV-SP-001–006 fixam a direção durável. `spec.md`, `plan.md` e
`eval.md` descrevem a correção em oito checkpoints independentes. O mapa de
migração e o documento de direção transitório foram retirados da árvore
publicada no commit `82030b0`; a auditoria item a item que precedeu a remoção
permanece rastreável no commit `41e47d0`.

Esta migração e os ajustes de review são somente harness/documentação. A
implementação continua no estado anterior ao contrato corrigido: nenhum Java,
fixture ou teste do Semantic Product foi alterado. O próximo trabalho autorizado
é o Checkpoint 1 do `plan.md`: um oracle executável, test-only, do target model,
sem alteração de produção.

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
- `Ast.IfStatement` já preserva condition, `thenBranch`, `elseBranch`,
  termination e nesting; `Ast.children` e o collector percorrem os statements e
  referências disponíveis. Uma `elseBranch` vazia não distingue ELSE ausente de
  ELSE sintaticamente vazio. A surface sustenta o ramo falso e sua continuação,
  não essa distinção, predicate semantics, CFG, reachability ou dataflow.
- AST, compilation units, symbol tables, occurrences, resolution, report,
  policy, provenance e presentation permanecem produtos separados e
  autoridades de suas próprias semânticas.
- Não há composition-root publication nem JSON do Semantic Product em
  produção. Lowering, Analysis IR, CFG, effects/storage e dataflow também não
  estão implementados por este work item.
- O fechamento da migração e estes ajustes de review foram verificados por
  `git diff --check` e `./scripts/harness/check-full.sh`; a lista integral dos
  destinos auditados permanece no commit `41e47d0`.

## Restante

- Obter aprovação do review documental do PR #27; os quatro ajustes solicitados
  foram incorporados, os transitórios já foram removidos e a auditoria
  permanece no histórico Git.
- Executar, como próximo trabalho autorizado, o Checkpoint 1 corretivo:
  fixture e oracle/consumer test-only do target model com multiple
  DATA/MOVE/CALL, IF/ELSE, nesting, incompletude e readiness.
- Executar os Checkpoints 2–8 do `plan.md` somente na ordem registrada, com a
  evidência do anterior e a autorização aplicável. Eles remodelam A2+B,
  corrigem projection, acrescentam IF/coverage, integram o composition root,
  provam lowering-readiness, publicam JSON por último e fecham o handoff.
- Manter EVALUATE, PERFORM, GO TO, terminal semantics, ALTER, SEARCH,
  CobolLower, IR, CFG e dataflow em backlog até seus pré-requisitos e work
  items próprios.
- Usar 15 de setembro de 2026 como meta operacional de priorização: buscar uma
  boundary utilizável pelo downstream por slices de capability, sem reduzir
  garantias, truncar ocorrências, omitir gaps ou anunciar completude falsa.

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
- IF/ELSE tem surface suficiente para o slice estrutural inicial, mas a
  presença sintática de ELSE vazio não é publicada pela AST tipada. Se essa
  distinção se tornar necessária, ela é gap com precondição de frontend, não
  convite a reparse ou inferência. EVALUATE é partial por F-01; PERFORM
  permanece bloqueado pelos controles incompletos de F-SP-007, inclusive TIMES,
  test mode, VARYING e AFTER.
- O futuro consumer de lowering-readiness precisa depender somente do port.
  Determinismo de handles/JSON dentro do mesmo contrato de transporte não cria
  identidade persistente entre edições, versões ou execuções distintas.
