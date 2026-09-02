# WORK-COND-002 — Decisão arquitetural para condições contextuais

## Problema

O contrato IBM fechado em WORK-COND-001 prova que um bare nominal tail de abbreviated combined relation condition pode ser object DATA/INDEX herdando subject/operator ou nova simple condition CONDITION. Declaration kind, qualification e scope são necessários para distinguir essas classes, mas a AST é construída antes de symbols/occurrences/resolution.

O frontend atual fecha cedo demais parte desses tails por `grammarRule`, perde estrutura em `abbreviation`/`relationCombinedComparison`, achata precedência e não define identidade/provenance para elementos herdados. Corrigir somente a occurrence ou o resolver deixaria a condition semantics incompleta. Antes de qualquer implementação, o projeto precisa decidir onde preservar superfície, onde especializar o meaning e qual produto será consumido por CFG, predicate analysis e dataflow.

## Objetivo

Comparar com challenge pass pelo menos AST contextual, normalização no lowering e AST de superfície com produto pós-binding; escolher a alternativa sustentada pelos oracles `COND-*`; e registrar ADR/invariants suficientemente precisos para fatiar implementação posterior sem alterar código neste work item.

A decisão proposta é ADR-0012: AST contextual/lossless de superfície mais `ConditionSemantics` separado e normalizado pós-binding. O status permanece `Proposed` até review/merge deste checkpoint.

## Domínio de entrada suportado

O domínio de decisão inclui as condições cobertas pelo contrato canônico: relation completa, subject/operator omitidos, abbreviated tails nominais e literais, relation completa que atualiza o estado, nova simple condition, condition-name, `AND`/`OR`/`NOT`, precedência, boundary e distribuição parentéticos, qualification, subscripts, scope/nested programs, DATA, INDEX, CONDITION e level-66 RENAMES como DATA.

O domínio arquitetural atravessa parse tree, AST, symbol tables, occurrences e resolution somente para definir responsabilidades. `SEARCH`, a correção estrutural de condition-name subscriptado e shapes de grammar sem validade IBM comprovada permanecem evidência/consumidores do contrato, não implementação deste slice.

## Classes semânticas

1. **Relation explícita:** subject e operator escritos; atualiza o estado herdável.
2. **Abbreviation estruturalmente decidível:** subject ou subject+operator omitidos, mas o object não pode ser confundido com nova simple condition.
3. **Tail contextual:** nominal escrito admite CONDITION ou DATA/INDEX e precisa permanecer não especializado até binding.
4. **Nova simple condition estrutural:** class/sign/complex condition encerra herança sem depender de declaration kind.
5. **Condition-name pós-binding:** candidate CONDITION transforma o tail contextual em atom de condição e encerra herança.
6. **Object abreviado pós-binding:** candidate DATA/INDEX transforma o tail contextual em relation com subject/operator herdados.
7. **Binding incerto:** ambiguous, unresolved, unsupported ou option desconhecida conserva alternativa contextual sem predicate completo inventado.
8. **Source inválido:** cross-set homonym no mesmo programa e formas proibidas pela IBM continuam diagnósticos/unsupported; não recebem branch conveniente.
9. **Componente escrito versus herdado:** ambos participam do predicate normalizado, mas somente o escrito possui token/occurrence próprio.

## Premissas

| Premissa | Classificação | Consequência arquitetural |
| --- | --- | --- |
| O contrato e os oracles `COND-*` de WORK-COND-001 são a regra normativa fechada | `LANGUAGE_GUARANTEED` | Alternativas são desafiadas contra os mesmos casos, não contra o lowering atual. |
| Declaration kind, qualification e scope podem ser necessários para interpretar bare tail | `LANGUAGE_GUARANTEED` | Lowering pré-binding não pode produzir predicate final exato. |
| AST, symbols, occurrences e resolution permanecem produtos separados | `ARCHITECTURE_GUARANTEED` | Binding e normalized predicate não entram na AST/symbol table. |
| Cada `Ast.Node` possui identidade única no pre-order canônico da unit | `ARCHITECTURE_GUARANTEED` | Subject/operator herdados não podem clonar ou compartilhar children AST. |
| Provenance aproximada/herdada deve ser distinguível de token escrito | `ARCHITECTURE_GUARANTEED` | Produto normalizado marca `WRITTEN`/`INHERITED` e não inventa physical span. |
| Occurrence representa uso nominal escrito, não expansão textual fictícia | `ARCHITECTURE_GUARANTEED` | Não criar duplicates/synthetic occurrences para cada relation expandida. |
| CFG/predicate/dataflow precisarão de predicate especializado | `SPECIFICATION_GUARANTEED` para a fronteira; consumidores ainda futuros | Um produto compartilhado evita que cada consumidor reimplemente o state machine. |
| Os nomes Java finais e o schema exato do novo produto já estão decididos | `UNCERTAIN` | `ConditionSemantics` é nome conceitual; API concreta pertence ao slice de implementação autorizado. |
| Frequência do corpus atual mede relevância geral | `OBSERVED_IN_CURRENT_CORPUS_ONLY` | Não altera a escolha arquitetural. |

## Comportamento esperado

- A AST de superfície materializa todos os components escritos e uma shape contextual explícita para tails binding-dependent, sem candidate IDs.
- Descritores de herança apontam para a origem estrutural escrita sem entrar como children compartilhados; `Ast.children` continua árvore e IDs continuam pre-order contíguo.
- Occurrences são coletadas uma vez por nominal escrito. Um tail contextual publica admissible kinds suficientes para binding, mas não contém selected candidate.
- Symbols continuam declarativos. RENAMES permanece DATA; condition-name e index-name mantêm seus kinds atuais.
- Resolution aplica unit, nesting, scope, qualification e kind e conserva candidates/status. Ela não interpreta connectors, `NOT`, parênteses ou state de subject/operator.
- `ConditionSemantics` pós-binding percorre a surface condition, junta resolution por IDs estáveis e produz predicate com precedência/distribuição corretas e componentes `WRITTEN`/`INHERITED`.
- Candidate CONDITION inicia simple condition e encerra herança; candidate DATA/INDEX especializa object abreviado; nova relation explícita atualiza o estado.
- Ambiguous/unresolved/unsupported preserva nó contextual e bloqueia claim de predicate totalmente normalizado.
- CFG, predicate analysis e dataflow dependem do produto normalizado, não de `writtenText`, grammar parent ou lógica duplicada no resolver.

## Comportamento diante de incerteza

Se binding não produzir um único candidate semanticamente válido, `ConditionSemantics` não escolhe meaning. Ele preserva anchors, candidates/status e o fragmento contextual necessário ao diagnóstico. Source IBM inválido não é promovido a ambiguidade executável; forma aceita pela grammar sem autoridade permanece unsupported.

O wording/código de diagnostics IBM negativos continua incerto porque `cob2` não está disponível. O nome final de records/classes, layout de snapshot e migração exata dos IDs do novo produto são decisões de implementação, não lacunas que reabrem a separação de fases escolhida.

## Fora de escopo

- alterar `AstBuilder`, `Ast`, collector, resolver, grammar, manifests ou qualquer código de produção;
- criar `ConditionSemantics` ou tests executáveis;
- implementar lowering lossless do Slice 3;
- corrigir condition-name subscriptado, `SEARCH WHEN` ou occurrences;
- alterar baselines/snapshots de corpus;
- criar CFG, predicate analysis, dataflow, effects ou inferência de valores;
- validar o source WAUX ausente ou executar compilador IBM indisponível.

## Regras de domínio relacionadas

- [Expressões condicionais combinadas e abreviadas](../../../domain/conditional-expressions.md)
- [AST semântica](../../../domain/semantic-ast.md)
- [Modelo de símbolos](../../../domain/symbol-model.md)
- [Resolução de referências](../../../domain/reference-resolution.md)
- [Provenance](../../../domain/provenance.md)
- [Oracles normativos `COND-*`](../../../evals/conditional-expression-oracles.md)

## ADRs/invariantes relacionados

Decisão proposta: ADR-0012. Decisões preservadas: ADR-0002, ADR-0003, ADR-0005, ADR-0008 e ADR-0009. Invariantes centrais: INV-COND-001, INV-COND-002, INV-AST-001 a INV-AST-003, INV-SYM-001, INV-PROV-002, INV-RES-001, INV-COV-001, INV-DET-001 e INV-PERF-001.
