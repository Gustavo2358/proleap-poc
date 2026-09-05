# Estado

## Onde estamos

`WORK-SEMANTIC-PRODUCT-002` permanece ativo para a primeira implementação de
produção do slice `MOVE` literal → `CALL` variável. Este PR executa somente o
Checkpoint 3: `CobolMoveCallAdapter` projeta os produtos canônicos já fechados
do frontend para `CobolSemanticProduct.State`, e `CobolSemanticPort` continua a
ser a única superfície downstream.

O adapter não altera `ExplorerMain`, não executa resolução, não usa
`ResolutionAnalysisReport`, snapshots ou HTML como fonte semântica e não
reparseia texto. O Checkpoint 4 continua reservado para publicação no
composition root e consumer independente; JSON, lowering, IR, CFG e dataflow
continuam fora deste checkpoint.

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
- `CobolSemanticProduct.State` publica somente fatos boundary-owned do slice:
  unit/data identity, declaração/PIC, literal, MOVE, CALL variável, program
  points, ordering, policy, nominal binding, runtime `UNKNOWN` e incertezas
  localizadas. Binding parcial preserva status, reason e candidates sem
  fabricar handle selecionado.
- `CobolSemanticPort` é read-only e consulta apenas o estado publicado; não há
  query lazy, parser, resolver, cache mutável ou tipo do frontend na boundary.
- `SemanticProductMoveCallContractTest` prova construção sem frontend,
  imutabilidade/closure, join de identidade, separação nominal/runtime,
  ordering, estados partial/unknown, ambiguity e independência da ordem das
  consultas.
- O contrato corrigido distingue o Semantic Product in-memory do
  `semantic-product.json`: o JSON será um artefato interno, determinístico,
  versionável e documentado de transporte para inspeção e desenvolvimento
  isolado, não um modelo de domínio ou formato público.
- O fluxo isolado será `CobolSemanticPort` → JSON output adapter → artefato →
  futuro JSON input adapter → `LowererInputPort`; a integração final poderá
  trocar o trecho JSON por um adapter in-memory. Os cores do frontend e do
  lowerer permanecem independentes de JSON e podem viver em repositórios
  separados.
- O adapter de produção consome `CompilationUnitBuildResult`,
  `CompilationUnitSymbolTables`, occurrences e `ReferenceResolution`; os tipos
  A2/B não carregam nenhuma dessas referências.
- O adapter usa `CallSemantics.targetSyntax` para preservar
  `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN`, sem depender do report composto; policy,
  binding nominal, partialidade e provenance são projetados dos produtos
  canônicos correspondentes.
- A composição continua em `ExplorerMain`; o adapter foi implementado como
  ponto de tradução reutilizável, sem wiring ou consumer de produção.
- Nenhum arquivo de grammar, AST, symbols, occurrences, resolver, fixture,
  baseline ou `ExplorerMain` foi alterado neste checkpoint.
- O teste focalizado do adapter, o Checkpoint 3B, o contrato A2/B,
  `check-docs.sh`, `check-architecture.sh`, `check-fast.sh`,
  `check-semantic.sh`, `check-performance.sh`, `check-full.sh` e
  `git diff --check` passaram após a implementação.

## Restante

- Review humano do Checkpoint 3 e deste PR.
- Após aprovação explícita, executar separadamente o Checkpoint 4 e os demais
  checkpoints do `plan.md`, sem antecipar generalização.
- O Checkpoint 5 produzirá `semantic-product.json` somente por JSON output
  adapter do frontend, consumindo `CobolSemanticState`/`CobolSemanticPort` e
  exigindo determinismo, suficiência, versionamento/documentação e preservação
  explícita de UNKNOWN/partial/incompleteness para a mesma entrada
  normalizada/preprocessada, configuração efetiva/policy, versão do analisador e
  versão do contrato.
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
  separados; a integração futura deve inserir a projeção no menor ponto
  possível, sem entregar esse lifecycle ao port.
- `Ast.Meta.id` permanece local ao pre-order da unit. O adapter usa esse ID
  somente no join namespaced com occurrence/resolution e projeta
  `DataItemId` a partir do `SemanticEntityId(DATA_SYMBOL, localId)` do
  candidate resolvido.
- `ReferenceResolution` separa `CallTargetSyntax` de `CallLinkage`. Para este
  slice, `IDENTIFIER_OR_EXPRESSION` é suficiente para publicar runtime
  `UNKNOWN`, a incerteza exigida e partialidade sem consultar report ou inferir
  valor do `MOVE`.
- O adapter publica somente o candidate DATA selecionado, sua declaração
  tipada/PIC, o literal AST, os dois fatos e a relação de ordem; shapes fora do
  slice ou joins inconsistentes falham de forma fechada.
- A prova 3B demonstrou igualdade determinística de valores, mas não o contrato
  completo de reprodução dos IDs transportados, `analysisGeneration`, persistência
  ou identidade persistente. O primeiro produto usa uma publicação A2 única. O
  futuro JSON output adapter deverá reproduzir handles e projeção para a mesma
  combinação de entrada normalizada/preprocessada, configuração efetiva/policy,
  versão do analisador e versão do contrato, sem converter essa propriedade de
  transporte em identidade
  persistente após edições ou mudanças de versão, nem promover JSON a domínio,
  schema público ou protocolo universal.
- O futuro `CobolLower` terá seu próprio `LowererInputPort` e core independente.
  Em desenvolvimento isolado, seu JSON input adapter poderá traduzir o artefato;
  na integração final, um adapter in-memory ligará `CobolSemanticPort` ao mesmo
  port. Nenhuma dessas implementações está autorizada neste work item.
- `CobolLower`, IR, CFG, dataflow, possible-values e dependency extraction
  continuam explicitamente fora deste work item.
