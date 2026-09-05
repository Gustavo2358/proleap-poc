# Estado

## Onde estamos

`WORK-SEMANTIC-PRODUCT-002` foi criado como work item ativo para a primeira
implementação de produção do slice `MOVE` literal → `CALL` variável. Este PR é
somente o Checkpoint 1: contrato executável, plano, eval, estado e índice. Não
há implementação de produção, serializer, consumer de produção, CFG ou
dataflow.

A branch parte da `main` atualizada em `3972c66`, que já contém o Discovery dos
Checkpoints 2 e 3A e o Checkpoint 3B (`PR #26`). O Discovery mantém sua própria
memória e seus relatórios históricos; este item executa a decisão H já provada
sem reabrir A2 versus B.

## Verde conhecido

- Antes da alteração, `main` estava limpa, em sincronia com `origin/main`, e a
  branch focalizada foi criada a partir dela.
- O Checkpoint 2 aprovou A2 + B: estado COBOL-specific, materializado,
  boundary-owned, imutável, partial-aware e namespaced, acessado por facade
  fechada, tipada e read-only.
- O Checkpoint 3A provou closure, ausência de leakage e ausência de semantic
  reparsing para `CALL` literal; o Checkpoint 3B provou a mesma seam para
  declaração DATA + `MOVE` literal + `CALL` variável.
- O oracle 3B preserva `DataItemId` namespaced, `PIC`, literal, joins comuns,
  ordering explícito, provenance localizada, binding nominal `COMPLETE` e
  runtime target `UNKNOWN` com `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN`.
- O frontend atual não possui Semantic Product de produção; a composição vive
  em `ExplorerMain` e os tipos A2/B existentes são exclusivamente test-only.
- Nenhum arquivo em `src/main/**`, grammar, fixture, baseline ou teste foi
  alterado neste checkpoint.

## Restante

- Review humano deste contrato e deste PR.
- Após aprovação explícita, executar separadamente os Checkpoints 2, 3 e 4 do
  `plan.md`, cada um com seu próprio review e sem antecipar generalização.
- Manter o produto limitado ao domínio descrito: uma unit selecionada, uma
  DATA, um `MOVE` literal e um `CALL` variável para o mesmo handle nominal.
- Promover eval/oracle adicional ao catálogo somente se uma repetição útil for
  demonstrada em checkpoint posterior.

## Descobertas que afetam o plano

- `ExplorerMain` é o composition root atual e mantém os produtos do frontend
  separados até resolução/report/snapshots; a integração futura deve inserir a
  projeção no menor ponto possível, sem entregar esse lifecycle ao port.
- `Ast.Meta.id` é local ao pre-order da unit e não pode ser promovido a
  identidade pública. A implementação precisa projetar handles próprios que
  preservem `ProgramUnitId`.
- `ReferenceResolution` já separa `CallTargetSyntax` de `CallLinkage`, e o
  report já produz `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN`; a boundary deve
  transportar essa distinção, não rederivá-la por texto.
- A prova 3B demonstrou igualdade determinística de valores, mas não
  `analysisGeneration`, persistência ou intercâmbio. O primeiro produto usa uma
  publicação A2 única e não faz promessa cross-run/cross-version.
- `CobolLower`, IR, CFG, dataflow, possible-values e dependency extraction
  continuam explicitamente fora deste work item.
