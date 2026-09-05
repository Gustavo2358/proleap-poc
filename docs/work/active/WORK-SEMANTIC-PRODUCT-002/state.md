# Estado

## Onde estamos

Os seis checkpoints da migração documental e o Checkpoint 1 corretivo foram
executados no PR #27, que permanece sob review. O oracle test-only agora
falsifica uma `ProgramUnit` com todas as ocorrências semanticamente cobertas de
DATA, MOVE literal, CALL variável e IF/ELSE, mais inventário explícito de um
construct observado fora da capability.

ADR-0013 e INV-SP-001–006 fixam a direção durável. `spec.md`, `plan.md` e
`eval.md` descrevem a correção em oito checkpoints independentes. O mapa de
migração e o documento de direção transitório foram retirados da árvore
publicada no commit `82030b0`; a auditoria item a item que precedeu a remoção
permanece rastreável no commit `41e47d0`.

`SemanticProductTargetModelOracleTest`, seu fixture e os tipos próprios de
target/consumer existem somente em `src/test`. A implementação de produção
continua no estado anterior ao contrato corrigido: nenhum `src/main` foi
alterado e o oracle permanece deliberadamente desconectado do state/port
singleton. Nenhum checkpoint de produção está autorizado por esta execução;
após review humano, o próximo checkpoint planejado é o Checkpoint 2 do
`plan.md`.

## Verde conhecido

- `WORK-SEMANTIC-PRODUCT-001` e os relatórios 3A/3B provaram a alternativa
  A2+B: state COBOL-specific, materializado, imutável, partial-aware e
  namespaced, exposto por port fechado, tipado e read-only.
- O fixture mínimo continua provando, como caso N=1, uma DATA, um MOVE literal,
  um CALL variável, binding nominal comum, provenance, ordering e runtime
  target `UNKNOWN` com `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN`.
- O novo fixture de target prova 3 DATA, 7 MOVE literal, 3 CALL variáveis, 2 IF
  aninhados com statements nos dois ramos, 1 IF com ramo falso vazio e 1
  DISPLAY observado fora da capability. Seu inventário contém 14 statements,
  sem seleção first/last nem paridade artificial entre MOVE e CALL.
- O consumer test-only conhece somente o port target plural. Ele reconstrói
  identities namespaced, program points, containment, branches, continuations,
  identities próprias dos operands, condição relacional tipada, roles, binding,
  provenance, coverage, gaps e as três dimensões de readiness, sem publicar IR,
  CFG, reachability, reaching definitions ou possible-values.
- O oracle preserva `MOVE 'B'` e `MOVE 'C'` nos ramos do IF, ambos ligados a
  `WS-X`, e `CALL WS-X` como continuação. Essa é a informação necessária para o
  futuro oracle de reaching definitions; nenhum conjunto RD é calculado neste
  checkpoint.
- O adversarial nominal mantém um IF completo quando sua condition reference é
  `AMBIGUOUS`: status, reason e todos os candidates chegam ao consumer, enquanto
  selected permanece vazio. `UNRESOLVED` também é representável sem fabricar
  `DataItemId`.
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
- O oracle focalizado, todas as regressões `SemanticProduct*Test`, os gates de
  arquitetura, performance e `full`, além de `git diff --check`, passam. A
  lista integral dos destinos da migração documental permanece no commit
  `41e47d0`.

## Restante

- Obter review humano do Checkpoint 1 no PR #27. O oracle está executável, mas
  não é contrato de produção nem autorização implícita para remodelá-lo.
- Mediante autorização posterior, executar o Checkpoint 2 do `plan.md`:
  remodelar o core A2+B para cardinalidade e extensão até satisfazer a parte
  boundary-only do oracle, sem iniciar projection ou IF facts de produção.
- Executar os Checkpoints 3–8 somente na ordem registrada, com a evidência do
  anterior e a autorização aplicável. Eles corrigem projection, acrescentam
  IF/coverage, integram o composition root, provam lowering-readiness, publicam
  JSON por último e fecham o handoff.
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
- O frontend atual aceita o fixture controlado e publica a evidência tipada para
  3 DATA, 7 MOVE, 3 CALL variáveis, 1 DISPLAY e 3 IF, inclusive `elseBranch`
  vazia. Não surgiu gap novo de AST/binding para este oracle; o adapter de
  produção o rejeita no guard existente `the selected unit must contain exactly
  one MOVE`.
- O DISPLAY é mantido no target inventory genérico como `UNSUPPORTED`, com
  observed kind/shape e gap localizado, sem fingir que pertence a MOVE, CALL ou
  IF. Os 7 MOVE, 3 CALL variáveis e os facts estruturais continuam disponíveis,
  e a summary global fica `BLOCKED` em vez de aparentar completude.
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
