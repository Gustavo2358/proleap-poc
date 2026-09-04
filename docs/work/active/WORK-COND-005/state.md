# Estado

## Onde estamos

Checkpoint 3 — Closure em execução na branch `implementation/work-cond-005-contextual-occurrences`, sobre o head aprovado `1f5d320`. Checkpoint 2 foi humanamente aprovado; F1–F5 permanecem fechados e não foram reabertos. Este checkpoint não autoriza mudanças em `src/main/`.

## Verde conhecido

- Surface AST distingue standalone, contextual tail, relation operand, distribuição, NOT e boundaries; `DataReference` expõe `qualifiers()`/`subscriptGroups()`/`referenceModification()` tipados para a shape nominal escrita.
- F1 fechado: policy contextual é composição `relationOperandPolicy(shape)` com `standaloneConditionPolicy`; index-admissible `{DATA, INDEX, CONDITION}`, qualified/subscripted/reference-modified `{DATA, CONDITION}`; relation operands index-admissible `{DATA, INDEX}`, demais `{DATA}`. Matriz Position × Shape registrada na spec; INDEX fora de roots qualified/subscripted confirmado por IBM 6.4 + symbol/resolver contracts.
- F2 fechado: `conditionNameReference → CONTEXTUAL_REFERENCE_ORIGIN` com `referenceKind == null` (não é container; é origem estrutural contextual); invariantes do entry; `qualifiedDataName` continua origin DATA; version bump do manifesto registrado no plan/eval; ADR-0009 em must_read/related_decisions; INV-COV-002 em related_invariants.
- F3 fechado: `kind` permanece routing/primary surface hint (`CONDITION` para contextual tail, qualquer shape); categoria final é `selectedCandidate().kind()`; presentation label `CONTEXTUAL_CONDITION` para occurrence com CONDITION em admissibleKinds e size maior que 1; sem novo `ReferenceKind`; resolver/report files entram no scope futuro com restrição presentation-only (wording), registrada no work-item.yaml/spec/plan.
- F4 fechado: `NOT A = B OR C` produz `LogicalCondition(NegatedCondition(RelationCondition(A,B)), ContextualConditionTail(C))`; C permanece contextual (shape index-admissible `{DATA, INDEX, CONDITION}`); NOT da primeira relation não transforma C em standalone nem é herdado.
- Gap preexistente de relation operands (qualified/subscripted com `{DATA, INDEX}`) caracterizado como `PREEXISTING_RELATION_OCCURRENCE_OVERADMISSIBILITY`; lifecycle decidido: corrigir no próprio Slice 5 (mesmo helper, sem resolver change).
- FACTs R2-01..R2-04, oracles de NOT e children independentes adicionados a `ContextualConditionOccurrenceDiscoveryTest`; suíte focal passou.
- Round 3: FACTs R3-01..R3-03 adicionados. `A = C(1:2)` prova `referenceModification != null` com qualifiers/subscriptGroups vazios; occurrence atual registra `PREEXISTING_RELATION_REFERENCE_MODIFICATION_OVERADMISSIBILITY`; projeção futura `DATA/{DATA}` resolve C DATA sem mudar o resolver. `A = (C(1:2) OR D)` foi materializado como `DistributedOperandGroup` e recebe a mesma policy futura.
- Gate `fast`: passou. Gate `semantic`: passou. Gate `full`: passou, incluindo regressão E2E e naming.
- TDD RED confirmado antes das mudanças de produção; nenhum commit vermelho foi criado.
- GREEN confirmado em `ContextualConditionOccurrenceTest` (13 casos), `ReferenceResolutionManifestTest` e na suíte de regressões diretamente afetada.
- `indexAdmissibleNominalShape(ref)` é uma única policy O(1): qualifiers vazios, subscript groups vazios e `referenceModification == null`.
- Relation e `DistributedOperandGroup` usam a mesma policy de admissibilidade; contextual tails fazem união com `CONDITION`, mantendo `CONDITION` como primary kind.
- Uma referência escrita continua gerando uma ocorrência nominal e uma entrada de resolução; admissibility não é materializada em ocorrências sintéticas.
- `PerformControl` tipado distingue `VALUE`/`CONDITION` sem ser `Ast.Node`; `controlExpressions`, pre-order e IDs permanecem compatíveis.
- `conditionNameReference` agora mapeia para `CONTEXTUAL_REFERENCE_ORIGIN` com `referenceKind == null`; `qualifiedDataName` permanece `REFERENCE_ORIGIN/DATA`; manifesto em `1.1.0`; construção contextual com kind falha rápido.
- Diagnósticos de admissibilidade múltipla usam `CONTEXTUAL_CONDITION`; `IF MISSING` mantém `CONDITION reference`.
- Camada adversarial cobre grammarRule coupling, conjunto contextual universal, falso bare, contaminação de children, cardinalidade, boundaries, connector, NOT, manifesto, diagnostics, posição de PERFORM e substituição metamórfica de declaração.
- Regressões `ConditionSurfaceAstTest`, `ConditionNameSurfaceAstTest`, `ConditionNameSurfaceDiscoveryTest`, `SemanticConditionContextDiscoveryTest`, `DataAndIndexReferenceResolverTest`, `ReferenceResolutionManifestTest`, `AstPreorderInvariantTest`, `ResolutionAnalysisReportTest`, `ResolutionSnapshotTest` e `CicsIntrinsicClassifierTest` passaram; SET/EVALUATE permaneceram sem alteração de policy.
- Gates finais: `check-fast.sh` passou; `check-semantic.sh` passou; `check-performance.sh` passou; `check-full.sh` passou com E2E, artefatos semânticos e naming.

## Restante

Criar o resumo histórico, promover contratos duráveis, atualizar backlog/índices e arquivar este diretório. SEARCH WHEN permanece Slice 6; `BACKLOG-RES-004` permanece separado; merge não é autorizado.

## TDD RED — confirmado

`ContextualConditionOccurrenceTest` foi criado antes de qualquer mudança em `src/main/`. A suíte focal confirmou RED pelos motivos esperados: tails `A = B OR C` ainda eram `CONDITION/{CONDITION}`, relation roots qualified/subscripted/reference-modified ainda recebiam `{DATA, INDEX}`, `conditionNameReference` ainda era `REFERENCE_ORIGIN/CONDITION`, e `PerformStatement` ainda não expunha controls tipados. O teste de standalone permaneceu correto como controle negativo. Nenhum commit vermelho foi criado.

## Descobertas que afetam o plano

- `ReferenceOccurrenceCollector` e `ReferenceResolutionManifest` contêm os couplings por `conditionNameReference`; o collector também aplica `{DATA, INDEX}` a todo relation operand sem considerar a shape.
- IF e nodes condition tipados bastam para helpers estruturais; PERFORM mistura VALUE e UNTIL CONDITION em lista sem tag. Por isso `Ast.java`/`AstBuilder.java` entram no futuro scope apenas para metadata não-node de controls, sem mudar pre-order.
- Resolver candidate algorithm, symbol model, CICS e `ResolutionContracts` não precisam mudar; wording de diagnóstico contextual poderá tocar `DataAndIndexReferenceResolver`, `CobolReferenceResolver` e `ResolutionAnalysisReport` (presentation only).
- Reference modification não é aplicável ao root de `ContextualConditionTail` na grammar atual (`conditionNameReference` não possui `referenceModifier`); documentado na spec, sem ampliar escopo. A dimensão permanece obrigatória na shape geral e no helper de relation/distributed operands.
- RENAMES continua DATA; nenhum `ReferenceKind.RENAMES`/novo kind contextual.
- O manifesto classifica coverage/origin; a policy da occurrence vem da typed AST position + shape. `conditionNameReference → CONTEXTUAL_REFERENCE_ORIGIN` não carrega admissibility.

## Semantic challenge pass — Discovery Round 2

### Challenge R2-1 — universal contextual set
Imaginar todo `ContextualConditionTail → {DATA, INDEX, CONDITION}`: NEG-CONTEXT-INDEX-QUAL-01/SUB-01 e CO-09/10 matam (qualified/subscripted excluem INDEX).

### Challenge R2-2 — relation overadmissibility
Imaginar todo `RelationCondition.object → {DATA, INDEX}`: NEG-INDEX-QUAL-01/SUB-01 e CO-02/12 matam (qualified/subscripted ficam `{DATA}`).

### Challenge R2-3 — INDEX shape ignorance
Usar apenas o container AST e ignorar `qualifiers()`/`subscriptGroups()`/`referenceModification()`: os FACTs shape-sensitive, CO-21 e os NEG oracles falham; a shape é condição necessária da policy.

### Challenge R2-4 — manifest backslide
Imaginar `conditionNameReference → REFERENCE_ORIGIN/CONDITION`: CO-16 falha (contract exige `CONTEXTUAL_REFERENCE_ORIGIN`/null).

### Challenge R2-5 — container misclassification
Imaginar `conditionNameReference → REFERENCE_CONTAINER`: CO-16 falha porque a rule é root occurrence origin contextual, não apenas container com referências nos filhos.

### Challenge R2-6 — manifest becomes policy
Imaginar `conditionNameReference → {DATA, INDEX, CONDITION}` como policy global no manifesto: rejeitado arquiteturalmente; o manifesto não determina occurrence admissibility.

### Challenge R2-7 — diagnostic lies
Imaginar contextual unresolved apresentado como `UNRESOLVED CONDITION reference` sem indicar admissibility múltipla: CO-17/18 falham.

### Challenge R2-8 — NOT closes inheritance
Imaginar `NOT A = B OR C` tratando C como standalone: CO-20 falha.

### Challenge R2-9 — NOT propagation
Imaginar aplicar NOT também à relation herdada de C: CO-20 rejeita.

### Challenge R2-10 — resolver policy leakage
Nenhuma decisão nova exige candidate selection change, scope rule, qualification rule ou ambiguity precedence; wording-only nos resolver files. Se exigir: `ARCHITECTURAL_DECISION_REQUIRED` e STOP.

## Semantic challenge pass — Discovery Round 3

### Challenge R3-1 — false bare detection
`qualifiers.empty && subscriptGroups.empty → INDEX` falha em `C(1:2)`, pois `referenceModification != null` torna a shape não index-admissible.

### Challenge R3-2 — parenthesis textual heuristic
`C(I)` e `C(1:2)` são diferenciados por `subscriptGroups()` e `referenceModification()`, não por `writtenText`.

### Challenge R3-3 — duplicated shape logic
Relation e distributed operands devem consumir o mesmo helper `indexAdmissibleNominalShape(ref)`; a projeção distribuída confirmou a mesma policy.

## Decisão final

Decisão final do Discovery: `READY_FOR_IMPLEMENTATION`, após o fechamento de F5 no Round 3. Ela foi implementada e aprovada humanamente no Checkpoint 2. Policy = `occurrencePolicy(position, nominalShape)`: standalone `{CONDITION}`; contextual `{DATA, INDEX, CONDITION}` somente para `indexAdmissibleNominalShape(ref)`, caso contrário `{DATA, CONDITION}`; relation/distribution `{DATA, INDEX}` somente para essa shape, caso contrário `{DATA}`; `indexAdmissibleNominalShape(ref)` exige qualifiers vazios, subscript groups vazios e `referenceModification == null`; qualifier/subscript independentes; resolver inalterado; manifesto com `CONTEXTUAL_REFERENCE_ORIGIN`/null; primary CONDITION é hint de superfície; diagnostics contextuais não mentem; `NOT A = B OR C` mantém C contextual.

## Semantic challenge pass — Implementation

1. Policy depende de `grammarRule`? PASS — traversal tipado e shape helpers decidem a policy.
2. Policy depende de connector spelling? PASS — OR e AND compartilham a policy.
3. Helper esquece `referenceModification`? PASS — a condição estrutural inclui explicitamente `== null`.
4. Relation e distributed usam a mesma regra? PASS.
5. Contexto vaza para children? PASS — qualifier e subscript mantêm policies próprias.
6. Cardinalidade aumentou? PASS — uma ocorrência nominal por referência escrita e bijection com resolution entry.
7. PERFORM depende de posição/list index? PASS — usa `PerformControlContext` construído de contextos ANTLR tipados.
8. Manifesto virou policy table? PASS — não contém `admissibleKinds` e contextual origin exige kind nulo.
9. Diagnostics mudaram resolução? PASS — somente label human-readable foi alterado.
10. Resolver algorithm foi tocado? PASS — candidate selection, scope, qualification, visibility, ambiguity e dispatch não mudaram.
11. AST ID/pre-order mudou? PASS — wrapper não é node e `Ast.children` usa a compatibility view existente.
12. Declaração concreta influencia surface AST? PASS — substituição DATA/INDEX/CONDITION/RENAMES/MISSING altera somente resolução.

## Checkpoint 2 — aprovado humanamente

Implementação concluída sobre `b17f81f` nos commits `d94043e` e `1f5d320`. PR #19 permanece aberto e sem merge.

## Checkpoint 3 — Closure em execução

O regression WAUX-like foi adicionado e passou sem alteração de produção. A promoção documental, o arquivamento e os gates finais ainda são os passos deste checkpoint.
