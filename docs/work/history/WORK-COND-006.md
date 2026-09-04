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

`SearchWhenConditionDiscoveryTest` foi convertido em oracle de implementação. `SearchWhenMaterializationAdversarialTest` cobre A1–A10: ausência de duplicação, três posições semânticas, substituição de declaration e shape, tail contextual, ownership de múltiplos WHEN, SEARCH ALL, qualification, `NOT` e routing tipado standalone. IR-01..IR-20 passaram. A regressão final separada foi considerada redundante porque A1, A6 e A9, combinados com a suíte funcional, já cobrem a fixture composta de SEARCH, VARYING, múltiplos WHEN, relation abreviada, condition-name, NEXT SENTENCE e a bijeção occurrence/resolution.

## Closure

CR-01..CR-15 passaram antes da promoção. O único contrato promovido é o comprovado por código, testes e manifest: boundary AST tipada, ownership de branches, reuse da condition surface, policy de occurrences do Slice 5, policy bare/qualified de VARYING, preservação de NEXT SENTENCE e `all=true` estrutural. Claims de validação normativa, semântica de tipos, CFG, dataflow, runtime e targets dinâmicos não foram promovidos.

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

## Gates e encerramento

Passaram `check-fast.sh`, `check-semantic.sh` e `check-full.sh`, incluindo regressão E2E e naming, além de `git diff --check`. A Closure teve zero alterações em `src/main`, grammar, resolver, snapshots ou baselines. `BACKLOG-COND-001` mantém Slice 7 pendente e `BACKLOG-RES-004` permanece separado.

Status: arquivado após Closure; merge do PR #20 continua dependente de review humano.
