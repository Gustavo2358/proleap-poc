# Plano

## Fatiamento

### Checkpoint 1 — Discovery

#### Round 1 (revisado humanamente; head `433b634`)

1. Confirmar norma IBM e surface AST atual para standalone, relation operand, contextual tail, boundaries, distribuição e NOT.
2. Mapear `kind`, `admissibleKinds`, resolver dispatch, candidate filtering, diagnostics, snapshots, report, manifesto e CICS.
3. Adicionar `ContextualConditionOccurrenceDiscoveryTest` como evidência FACT, incluindo declaração metamórfica, children independentes, resolver what-if e a lacuna tipada de PERFORM.
4. Fechar matriz, alternativas, source scope, oracles e challenge pass.

#### Round 2 — correções do review humano (concluído; findings F1–F4)

1. Fechar F1 (admissibility shape-sensitive: bare vs qualified/subscripted) com a composição `contextualPolicy(shape) = relationOperandPolicy(shape) UNIÃO standaloneConditionPolicy(shape)`, a nova matriz Position × Shape, os NEG oracles e FACTs R2-01..R2-04.
2. Fechar F2 (contrato do `ReferenceResolutionManifest`): `CONTEXTUAL_REFERENCE_ORIGIN`, `referenceKind == null`, invariantes do entry, version bump planejado, ADR-0009 em must_read/related_decisions e INV-COV-002 em related_invariants.
3. Fechar F3 (primary `CONDITION` como routing/hint; label de diagnóstico `CONTEXTUAL_CONDITION` derivado; resolver files com restrição presentation-only).
4. Fechar F4 (oracle `NOT A = B OR C`: C permanece contextual; NOT da primeira relation não termina a abbreviation nem é herdado).
5. Revisar CO-02/09/10/12 e adicionar CO-17..CO-20; decidir o lifecycle do gap preexistente de relation operands (corrigir no Slice 5).
6. Rodar gates, auditar zero `src/main/`, publicar commits/PR e parar para novo review humano.

#### Round 3 — reference modification shape closure (este round; F5)

1. Fechar F5 substituindo `bare` pela propriedade explícita `indexAdmissibleNominalShape(ref)`, que exige `qualifiers().isEmpty()`, `subscriptGroups().isEmpty()` e `referenceModification() == null`.
2. Atualizar a matriz Position × Nominal Shape e a relation operand policy: `C(1:2)` recebe `DATA/{DATA}`; o contextual tail atual continua sem reference modification por construção da grammar.
3. Adicionar FACT-R3-01..R3-03, `CO-21` e `NEG-INDEX-REFMOD-01`, incluindo o what-if nominal controlado para candidate INDEX e a validação de `DistributedOperandGroup`.
4. Registrar a confirmação normativa IBM 6.4, distinguir `C(I)` de `C(1:2)` por typed AST e documentar o helper único, sem alterar grammar, resolver, manifesto, diagnostics, PERFORM ou `must_not_change`.
5. Rodar teste focal, gates `fast`, `semantic` e `full`, atualizar `state.md`/PR body e criar somente commits append-only; permanecer em `READY_FOR_IMPLEMENTATION` aguardando novo review humano.

### Checkpoint 2 — Implementation — não autorizado

Ordem planejada após autorização:

1. Adicionar oracles de produção shape-sensitive (matriz Position × Shape) em `planned:.../ContextualConditionOccurrenceTest.java`.
2. Tipar controles de PERFORM como VALUE ou CONDITION sem criar `Ast.Node`/ID adicional; construir a lista por contexts ANTLR tipados e preservar a ordem atual.
3. Introduzir primeiro o helper puro `indexAdmissibleNominalShape(ref)`, dependente de `DataReference.qualifiers()`, `subscriptGroups()` e `referenceModification()`. Então `relationOperandKinds(ref)` retorna `{DATA, INDEX}` somente para essa shape e `{DATA}` nos demais casos; `contextualKinds(ref) = relationOperandKinds(ref) UNIÃO {CONDITION}`.
4. Acrescentar traversal de condition surface tipada no collector:
   - standalone nominal → `CONDITION/{CONDITION}`;
   - relation/distributed operand → shape-sensitive (`relationOperandKinds(ref)`);
   - contextual tail → `CONDITION/contextualKinds(ref)`;
   - containers logical/grouped/negated encaminham somente aos fragments tipados.
5. Remover de `addDataReference` toda decisão por `grammarRule`; o fallback neutro de DataReference permanece DATA quando nenhum contexto tipado o especializa. Nunca usar `writtenText`, regex, token spelling ou parent grammar.
6. Adaptar o `ReferenceResolutionManifest`: nova `RuleClass.CONTEXTUAL_REFERENCE_ORIGIN` (nome pode variar), `conditionNameReference → CONTEXTUAL_REFERENCE_ORIGIN` com `referenceKind == null` (fail fast se `!= null`), manter `qualifiedDataName → REFERENCE_ORIGIN/DATA`, e subir `ReferenceResolutionManifest.VERSION`.
7. Adaptar diagnostics human-readable para occurrences contextuais (`CONTEXTUAL_CONDITION` / "contextual condition reference"), sem mudar resolver policy; somente os consumidores de wording de `DataAndIndexReferenceResolver`, `CobolReferenceResolver` e `ResolutionAnalysisReport` quando necessário.
8. Rodar regressões focais, snapshots e reports (aceitar somente deltas de `admissibleKinds`, status/reason/candidate dos tails afetados e metadata PERFORM deliberadamente exposta).
9. Gates + semantic challenge pass (R2-1..R2-10, R3-1..R3-3 e CO-01..CO-21).

### Checkpoint 3 — Closure — não autorizado

Promover somente o contrato durável necessário para domínio/evals/invariant, auditar must-not-change, rodar `full`, arquivar o work item e atualizar o mesmo PR.

## Dependências

- ADR-0012 e INV-COND-001/002 governam a projeção pós-binding e a cardinalidade.
- ADR-0009 (coverage explícita) e INV-COV-002 (cobertura gramatical fechada) governam a reclassificação versionada de `conditionNameReference` no manifesto de resolução; ADR-0009 entrou em must_read e related_decisions; INV-COV-002 entrou em related_invariants.
- Slices 3 e 4 já fornecem containers de condition surface e `DataReference` lossless com qualifiers/subscriptGroups/referenceModification tipados.
- `BACKLOG-RES-004` continua separado: não ampliar `QualifierTarget.UNSPECIFIED` para DATA/FILE.
- SEARCH WHEN continua Slice 6; o helper de condition surface pode ser reutilizado depois, mas este slice não materializa SEARCH.
- Review humano deste Discovery Round 3 e autorização explícita são pré-condições do Checkpoint 2.

## Superfície arquitetural provável

### Produção esperada

| Arquivo | Mudança provável | Limite |
| --- | --- | --- |
| `Ast.java` | wrapper/tag não-node para controls PERFORM VALUE/CONDITION; compatibility view de expressions e `Ast.children` sem mudança de ordem/IDs | sem binding, sem annotation em todo DataReference |
| `AstBuilder.java` | construir controls a partir de `performTimes`, `performUntil` e `performVarying` tipados | sem source-text parsing, sem grammar change |
| `ReferenceOccurrenceCollector.java` | helper estrutural shape-sensitive (`indexAdmissibleNominalShape(ref)` → `relationOperandKinds(ref)`/`contextualKinds(ref)`) para condition surface; remover equality por `grammarRule` | sem lookup, sem materializar predicate, sem contaminação de children |
| `ReferenceOccurrences.java` | alinhar Javadoc do primary hint contextual | sem schema/enum novo |
| `ReferenceResolutionManifest.java` | reclassificar `conditionNameReference` como `CONTEXTUAL_REFERENCE_ORIGIN` (referenceKind null, fail fast se presente) e subir `VERSION` | sem alterar grammar coverage; `qualifiedDataName` permanece origin DATA |
| `DataAndIndexReferenceResolver.java` | wording de diagnóstico contextual apenas, se necessário | sem mudança de resolução (selection/scope/qualification/ambiguity/dispatch) |
| `CobolReferenceResolver.java` | wording de diagnóstico contextual apenas, se necessário | sem mudança de resolução |
| `ResolutionAnalysisReport.java` | wording de contextual gap; `syntacticKindCounts` segue contando `kind`; `resolvedSemanticKindCounts` segue `selectedCandidate().kind()` | sem mudança de métricas/nomes |

`Ast.java`/`AstBuilder.java` eram must-not-change na hipótese inicial. O challenge FACT `PERFORM N TIMES` versus `PERFORM UNTIL C` provou que a AST atual não oferece informação suficiente ao collector sem recorrer a `grammarRule` ou texto. A ampliação é limitada a metadata não-node; não altera condition surface, pre-order, provenance ou binding. Resolver files entram no source_scope do work item com a restrição explícita `PRESENTATION/DIAGNOSTIC ADAPTATION ONLY — NO RESOLUTION POLICY CHANGE`, registrada no work-item.yaml e na spec.

### Must-not-change audit

- `Cobol.g4`, `ResolutionContracts`, `SymbolTable`, `CompilationUnitSymbolTables`, `ReferenceResolution` e `CicsIntrinsicClassifier` byte-identical;
- resolvers: nenhuma mudança de algoritmo (candidate selection, scope, qualification, ambiguity, dispatch, compatibleCandidates, local/global visibility, selectedCandidate); wording permitido conforme contrato de diagnóstico;
- zero ConditionSemantics/ConditionValidation/CFG/dataflow;
- zero occurrences sintéticas;
- zero mudança no mapping `UNSPECIFIED → {DATA}`;
- zero SEARCH WHEN;
- zero `ReferenceKind.CONTEXTUAL` / `ReferenceKind.RENAMES`;
- zero reparse textual, parent grammar rule, spelling ou connector heuristic;
- zero manifesto como tabela de occurrence policy.

## Migrações requeridas

- `SemanticConditionContextDiscoveryTest`: migrar o falso-gap opt-in para expectativas normativas verdes e manter os facts históricos que continuam úteis.
- `ConditionNameSurfaceDiscoveryTest`: remover/atualizar apenas o assert que caracteriza o collector por grammarRule; manter surface/qualification/subscript.
- `ReferenceResolutionManifestTest`: provar que `conditionNameReference` é `CONTEXTUAL_REFERENCE_ORIGIN` com `referenceKind == null`, que `qualifiedDataName` continua origin DATA e que a policy continua exaustiva/versionada/closed; subir `VERSION` (CO-16).
- `ConditionSurfaceAstTest`: preservar relation/distributed policies e boundaries.
- `DataAndIndexReferenceResolverTest`: preservar SET/EVALUATE e selected candidate kind; provar que policies shape-sensitive não alteram candidate selection.
- `AstPreorderInvariantTest`: provar que a nova metadata PERFORM não consome ID nem muda `Ast.children`.
- `ContextualConditionOccurrenceDiscoveryTest`: manter FACTs R2-01..R2-04, adicionar FACTs R3-01..R3-03, CO-21, NEG-INDEX-REFMOD-01 e oracle distribuído; preservar NOT, children independentes e os NEG oracles shape-sensitive.
- snapshots: aceitar apenas deltas de `admissibleKinds`, status/reason/candidate dos tails afetados e metadata PERFORM deliberadamente exposta; nenhuma renumeração AST/occurrence.
- CICS: executar regressão sem alterar classifier.
- Versionamento: registrar no eval/plan que a implementação sobe `ReferenceResolutionManifest.VERSION` (significado público da classificação muda) e que `syntacticKindCounts` permanece syntactic com `selectedCandidate().kind()` em `resolvedSemanticKindCounts`.

O discovery test novo permanece FACT; os oracles de produção entram no arquivo reservado `planned:.../ContextualConditionOccurrenceTest.java` somente após autorização.

## Artefatos esperados

- production files acima no Checkpoint 2;
- `ContextualConditionOccurrenceTest.java` com CO-01..CO-21 e NEG-INDEX-QUAL-01, NEG-INDEX-SUB-01, NEG-CONTEXT-INDEX-QUAL-01, NEG-CONTEXT-INDEX-SUB-01 e NEG-INDEX-REFMOD-01;
- migration dos discovery tests sem apagar evidência adversarial;
- consumer regressions de resolver, report/snapshot, CICS, SET/EVALUATE e pre-order;
- gates focais, `fast`, `semantic` e `full` reais;
- auditoria final do `must_not_change`, de zero grammarRule authority e de zero `src/main/` no Discovery.
