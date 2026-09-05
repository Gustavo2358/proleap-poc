# Estado

## Onde estamos

`WORK-SEMANTIC-PRODUCT-002` foi criado como work item ativo para a primeira
implementação de produção do slice `MOVE` literal → `CALL` variável. Este PR é
somente o Checkpoint 1 e sua remediation contratual: contrato executável, plano,
eval, estado e índice. Não há implementação de produção, JSON output adapter,
`semantic-product.json`, serializer genérico, consumer de produção, CFG ou
dataflow.

A branch parte da `main` atualizada, que já contém o Discovery dos Checkpoints 2
e 3A e o Checkpoint 3B, mergeado no `PR #26`. `WORK-SEMANTIC-PRODUCT-001` foi
arquivado conforme o lifecycle hygiene; seu resumo e os relatórios históricos
dos Checkpoints 2, 3A e 3B preservam a memória necessária. Este item executa a
decisão H já provada sem reabrir A2 versus B.

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
- O contrato corrigido distingue o Semantic Product in-memory do
  `semantic-product.json`: o JSON será um artefato interno, determinístico,
  versionável e documentado de transporte para inspeção e desenvolvimento
  isolado, não um modelo de domínio ou formato público.
- O fluxo isolado será `CobolSemanticPort` → JSON output adapter → artefato →
  futuro JSON input adapter → `LowererInputPort`; a integração final poderá
  trocar o trecho JSON por um adapter in-memory. Os cores do frontend e do
  lowerer permanecem independentes de JSON e podem viver em repositórios
  separados.
- O frontend atual não possui Semantic Product de produção; a composição vive
  em `ExplorerMain` e os tipos A2/B existentes são exclusivamente test-only.
- Nenhum arquivo em `src/main/**`, grammar, fixture, baseline ou teste foi
  alterado neste checkpoint.
- `check-docs.sh`, `check-architecture.sh`, `check-fast.sh`,
  `check-semantic.sh`, `check-performance.sh` e `check-full.sh` passaram após
  esta remediation; `git diff --check` também passou.

## Restante

- Review humano deste contrato e deste PR.
- Após aprovação explícita, executar separadamente os Checkpoints 2, 3, 4 e 5
  do `plan.md`, cada um com seu próprio review e sem antecipar generalização.
- O Checkpoint 5 produzirá `semantic-product.json` somente por JSON output
  adapter do frontend, consumindo `CobolSemanticState`/`CobolSemanticPort` e
  exigindo determinismo, suficiência, versionamento/documentação e preservação
  explícita de UNKNOWN/partial/incompleteness para a mesma entrada/análise.
- O Checkpoint 5 é responsável apenas pela saída do frontend. Não implementa o
  futuro repositório `CobolLower`, JSON input adapter, `LowererInputPort` ou
  adapter in-memory; apenas documenta a fronteira necessária para a futura
  troca de adapters sem alteração dos cores.
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
  `analysisGeneration`, persistência ou identidade cross-run/cross-version. O
  primeiro produto usa uma publicação A2 única. O futuro JSON output adapter
  deverá produzir um transporte determinístico para a mesma entrada/análise,
  sem promover JSON a domínio, schema público ou protocolo universal.
- O futuro `CobolLower` terá seu próprio `LowererInputPort` e core independente.
  Em desenvolvimento isolado, seu JSON input adapter poderá traduzir o artefato;
  na integração final, um adapter in-memory ligará `CobolSemanticPort` ao mesmo
  port. Nenhuma dessas implementações está autorizada neste work item.
- `CobolLower`, IR, CFG, dataflow, possible-values e dependency extraction
  continuam explicitamente fora deste work item.
