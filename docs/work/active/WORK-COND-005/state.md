# Estado

## Onde estamos

Checkpoint 2 — Implementation do work item na branch `implementation/work-cond-005-contextual-occurrences`, iniciado sobre o head aprovado `b17f81f`. F1–F5 permanecem fechados e não foram reabertos. A implementação foi autorizada; nenhum arquivo de produção foi alterado até a confirmação do TDD RED.

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

## Restante

Implementar GREEN, adicionar adversarial tests, executar regressões/gates, atualizar o mesmo PR e parar para review humano. SEARCH WHEN permanece Slice 6; `BACKLOG-RES-004` permanece separado; closure/merge não são autorizados.

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

`READY_FOR_IMPLEMENTATION` (após fechamento de F5 no Discovery Round 3; aguarda novo review humano). Policy = `occurrencePolicy(position, nominalShape)`: standalone `{CONDITION}`; contextual `{DATA, INDEX, CONDITION}` somente para `indexAdmissibleNominalShape(ref)`, caso contrário `{DATA, CONDITION}`; relation/distribution `{DATA, INDEX}` somente para essa shape, caso contrário `{DATA}`; `indexAdmissibleNominalShape(ref)` exige qualifiers vazios, subscript groups vazios e `referenceModification == null`; qualifier/subscript independentes; resolver inalterado; manifesto com `CONTEXTUAL_REFERENCE_ORIGIN`/null e version bump; primary CONDITION é hint de superfície; diagnostics contextuais não mentem; `NOT A = B OR C` mantém C contextual. A implementação aguarda review humano.
