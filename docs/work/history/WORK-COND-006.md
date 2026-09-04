# WORK-COND-006 — Materializar conditions de `SEARCH WHEN`

## Resultado

Concluído pelo PR #20 após Discovery, Implementation e Closure. O slice materializa a fronteira estrutural e nominal de `SEARCH WHEN`; não implementa semântica de runtime, CFG, dataflow, `ConditionSemantics`, `ConditionValidation` ou validação normativa completa de `SEARCH ALL`.

## Problema original

`SEARCH` atravessava `AstBuilder` pelo caminho `PreservedStatement`. O lowering preservava alguns operands e clauses, mas não materializava `searchWhen.condition`; por isso condition-names standalone, tails contextuais, `NOT` e qualificações podiam desaparecer antes de occurrences e resolution.

## Discovery

Rounds 1–4 fecharam a localização da perda e o menor contrato:

- F1: `SearchWhen.condition` cruza boundary tipada e chama `visitConditionSurface`.
- F2: a alternativa `NEXT SENTENCE` é preservada como `Ast.NextSentenceStatement`.
- F3: `VARYING` tem policy independente e shape-sensitive.
- R1–R5: VARYING bare usa `DATA/{DATA,INDEX}` e resolve DATA ou INDEX; bare missing permanece unresolved; qualified DATA usa `DATA/{DATA}` e resolve DATA; qualified INDEX é excluído.
- Self-refutations SR-01..SR-15, RF-01..RF-15 e RF4-01..RF4-15 passaram.

## Implementation

`Ast.SearchStatement` e `Ast.SearchWhen` substituem a representação preserved de SEARCH. `Ast.children` preserva a ordem searched reference, varying, at-end, whens; dentro da branch, condition e statements. O builder reutiliza o lowering de conditions existente. O collector roteia searched reference, VARYING e condition de modo independente, sem nova policy de condition e sem alteração do resolver. O manifest classifica `searchStatement` e `searchWhen` como `MODELED`/`REFERENCE_READY`.

`SEARCH ALL` preserva `all=true`, mas não afirma validade de keys, ordem, igualdade, conectores ou compatibilidade. `grammarRule` permanece provenance/coverage.

## Evidência e adversarial tests

`SearchWhenConditionDiscoveryTest` foi convertido em oracle de implementação. `SearchWhenMaterializationAdversarialTest` cobre A1–A10: ausência de duplicação, três posições semânticas, substituição de declaration e shape, tail contextual, ownership de múltiplos WHEN, SEARCH ALL, qualification, `NOT` e routing tipado standalone. IR-01..IR-20 passaram. Não foi criada nova fixture de regressão na Closure: A1, A6 e A9, combinados com a suíte funcional, foram considerados cobertura suficiente para SEARCH, VARYING, múltiplos WHEN, relation abreviada, condition-name, NEXT SENTENCE e a bijeção occurrence/resolution. O import `Map` corrigido em `8df6e61` apenas tornou compilável esse teste adversarial existente; não adicionou oracle semântico novo.

## Closure

O contrato promovido é o comprovado por código, testes e manifest: boundary AST tipada, ownership de branches, reuse da condition surface, policy de occurrences do Slice 5, policy bare/qualified de VARYING, preservação de NEXT SENTENCE e `all=true` estrutural. Claims de validação normativa, semântica de tipos, CFG, dataflow, runtime e targets dinâmicos não foram promovidos.

Após a promoção, o challenge CC-01..CC-10 passou: domínio explica AST, condition routing, VARYING, NEXT SENTENCE e limites de SEARCH ALL; backlog mostra Slice 6 concluído e Slice 7 pendente; work index aponta para este histórico; nenhum contrato depende somente do PR body.

### Closure challenge final — CC-01..CC-10

- **CC-01 PASS** — `semantic-ast.md` explica `SearchStatement`/`SearchWhen` e a ordem de `Ast.children`.
- **CC-02 PASS** — `conditional-expressions.md` e `reference-resolution.md` explicam condition routing e reuse.
- **CC-03 PASS** — o domínio documenta VARYING bare/qualified e suas admissibilities.
- **CC-04 PASS** — o domínio documenta `NEXT SENTENCE` como `Ast.NextSentenceStatement`.
- **CC-05 PASS** — SEARCH ALL validation está explicitamente futura.
- **CC-06 PASS** — `BACKLOG-COND-001` mostra Slice 6 concluído e Slice 7 pendente.
- **CC-07 PASS** — `docs/work/index.md` aponta WORK-COND-006 para este histórico.
- **CC-08 PASS** — este histórico contém decisões, evidências, limitações e gates suficientes.
- **CC-09 PASS** — não há links duráveis apontando para `docs/work/active/WORK-COND-006`.
- **CC-10 PASS** — o contrato promovido não depende somente do body do PR.

## Closure evidence repair

O processo pretendido era `state cleanup → CR self-refutation → promotion → archive`. A auditoria append-only demonstrou, porém, que o `state.md` ainda estava stale no commit imediatamente anterior ao archive (`Checkpoint 2 — Implementation em andamento` e gates incompletos), e que CR-01..CR-15 não estavam persistidos no `state.md` antes da remoção de `docs/work/active/WORK-COND-006/`. Portanto, a claim de que o stale state foi corrigido antes da promoção não é sustentada pelo histórico Git; os commits anteriores não foram reescritos. A evidência reparada abaixo é uma nova verificação no head atual.

### F2 confirmado

No tree `96f10af803a530d2bf4bd704b2c6bd8f88636bb8`, `SearchWhenMaterializationAdversarialTest.java` já continha os usos `Map<Integer, Long>` nas linhas 34 e 36, importava `EnumSet`, `List`, `Set` e `Collectors`, não continha import wildcard `java.util.*` e estava presente no tree. O gate semantic executa `mvn -q test`, e o full chama o semantic gate; portanto a classe pertence ao conjunto compilado pelos gates. Sem `java.util.Map`, aquele tree não compila.

O commit `8df6e61` contém somente a adição de `import java.util.Map;`. Ele corrigiu a compilabilidade do teste adversarial existente; não criou uma nova regressão semântica e não indica defeito na implementação de produção. Assim, os gates finais não são atribuídos retroativamente a `96f10af`; a distinção é entre implementação semântica e provenance do tree de teste/gate.

## Closure self-refutation — repaired evidence

- **CR-01 PASS** — `AstBuilder.visitSearchStatement` chama `buildSearch`, `Ast.SearchStatement` é o tipo produzido e A1/A2 o localizam; [semantic-ast.md](../../domain/semantic-ast.md) documenta a boundary.
- **CR-02 PASS** — `grammar-rule-manifest.tsv` classifica `searchStatement` como `MODELED`/`REFERENCE_READY`; A7 confirma a distinção estrutural de `all`.
- **CR-03 PASS** — o manifest classifica `searchWhen` como `MODELED`/`REFERENCE_READY`; `buildSearchWhen` materializa condition e statements, coberto por S4/A6.
- **CR-04 PASS** — `visitConditionSurface` usa CONDITION para `DataReference`; S1/A10 verificam `CONDITION/{CONDITION}` e resolution.
- **CR-05 PASS** — `buildConditionSurface` produz relation/tail; S3 e A5/A9 verificam `ContextualConditionTail` e `{DATA, INDEX, CONDITION}` para C bare.
- **CR-06 PASS** — `searchVaryingKinds` retorna `{DATA, INDEX}` para root bare; R1/R2 e A3 verificam selected INDEX/DATA.
- **CR-07 PASS** — a mesma helper retorna `{DATA}` para root qualified; R3/R5 e A4 verificam DATA e exclusão de INDEX.
- **CR-08 PASS** — `statementsInside` preserva a alternativa como `Ast.NextSentenceStatement`; F2 e A6 verificam a ação da branch.
- **CR-09 PASS** — A7 e `searchAllSharesTheGrammarBoundaryButHasDistinctNormativeRestrictions` verificam `all=true`; [conditional-expressions.md](../../domain/conditional-expressions.md) mantém validação normativa futura.
- **CR-10 PASS** — o único routing de condition é `visitConditionSurface`; não há `searchConditionKinds`, e o contrato está documentado em [reference-resolution.md](../../domain/reference-resolution.md).
- **CR-11 PASS** — diff desde `e1e3b6a` contém apenas documentação até este repair; `src/main/antlr4/Cobol.g4`, resolver e contratos de resolução permanecem inalterados.
- **CR-12 PASS** — A1 agrupa occurrences e resolution entries por `referenceAstNodeId` e exige cardinalidade um.
- **CR-13 PASS** — A6 exige dois `SearchWhen`, conditions distintas e statements sem mistura de ownership.
- **CR-14 PASS** — A9 exige `LogicalCondition(OR)`, `NegatedCondition(RelationCondition)` e `ContextualConditionTail`; S3 cobre abbreviated relation.
- **CR-15 PASS** — nenhum claim adicional é promovido: SEARCH ALL validation, ConditionSemantics, ConditionValidation, CFG, dataflow, runtime e dynamic targets continuam futuros no domínio e neste histórico.

## Evidence-repair checks

- **ER-01 PASS** — após esta correção, nenhum documento afirma que o stale state foi corrigido antes da promoção; a claim foi substituída pela limitação histórica explícita acima.
- **ER-02 PASS** — após esta correção, nenhum documento atribui os gates finais ao head `96f10af`; a provenance correta é o head validado após `8df6e61` e o repair final, conforme os SHAs registrados na sequência.

## Post-repair closure challenge

- **CC-01 PASS** — [semantic-ast.md](../../domain/semantic-ast.md) explica `SearchStatement`/`SearchWhen` e `Ast.children`.
- **CC-02 PASS** — [conditional-expressions.md](../../domain/conditional-expressions.md) e [reference-resolution.md](../../domain/reference-resolution.md) explicam routing/reuse.
- **CC-03 PASS** — os mesmos documentos explicam VARYING bare/qualified.
- **CC-04 PASS** — o domínio documenta `Ast.NextSentenceStatement`.
- **CC-05 PASS** — SEARCH ALL validation está explicitamente futura.
- **CC-06 PASS** — [backlog.md](../backlog.md) mantém Slice 6 concluído e Slice 7 pendente.
- **CC-07 PASS** — [index.md](../index.md) aponta para este histórico.
- **CC-08 PASS** — este histórico agora explica a trilha real, a correção F1/F2, evidências e limitações.
- **CC-09 PASS** — não há links duráveis para `docs/work/active/WORK-COND-006`.
- **CC-10 PASS** — as claims de gate desta correção não são vinculadas a `96f10af`; o SHA validado é registrado no handoff final do repair.

## Gates e encerramento

Passaram `check-fast.sh`, `check-semantic.sh` e `check-full.sh`, incluindo regressão E2E e naming, além de `git diff --check`. A Closure teve zero alterações em `src/main`, grammar, resolver, snapshots ou baselines. `BACKLOG-COND-001` mantém Slice 7 pendente e `BACKLOG-RES-004` permanece separado.

## Gate provenance

Validated tree before evidence-repair commit: `e1e3b6a9eb86cf116812b0dac5f152c1f7c8ff23`.

Os testes focais e os três gates acima foram reexecutados com o conteúdo documental do repair no working tree. O commit seguinte é documental e append-only; a validação pós-commit e seu SHA exato são registrados no handoff final/PR body, sem atribuição retroativa ao head `96f10af`.

Status: arquivado após Closure; merge do PR #20 continua dependente de review humano.
