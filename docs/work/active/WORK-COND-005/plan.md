# Plano

## Fatiamento

### Checkpoint 1 — Discovery

1. Confirmar norma IBM e surface AST atual para standalone, relation operand, contextual tail, boundaries, distribuição e NOT.
2. Mapear `kind`, `admissibleKinds`, resolver dispatch, candidate filtering, diagnostics, snapshots, report, manifesto e CICS.
3. Adicionar `ContextualConditionOccurrenceDiscoveryTest` como evidência FACT, incluindo declaração metamórfica, children independentes, resolver what-if e a lacuna tipada de PERFORM.
4. Fechar matriz, alternativas, source scope, oracles e challenge pass.
5. Rodar gates, auditar zero `src/main/`, publicar commits/PR e parar para review humano.

### Checkpoint 2 — Implementation — não autorizado

1. Tipar controles de PERFORM como VALUE ou CONDITION sem criar `Ast.Node`/ID adicional; construir a lista por contexts ANTLR tipados e preservar a ordem atual.
2. Acrescentar traversal de condition surface no collector:
   - standalone nominal → `CONDITION/{CONDITION}`;
   - relation/distributed operand → `INDEX/{DATA, INDEX}`;
   - contextual tail → `CONDITION/{DATA, INDEX, CONDITION}`;
   - containers logical/grouped/negated encaminham somente aos fragments tipados.
3. Remover de `addDataReference` toda decisão por `grammarRule`; o fallback neutro de DataReference permanece DATA quando nenhum contexto tipado o especializa.
4. Preservar qualifier/subscript/reference-modification em helpers próprios; nenhum override do root deve vazar.
5. Reclassificar `conditionNameReference` no `ReferenceResolutionManifest` sem kind nominal autoritativo e subir a versão do manifesto.
6. Criar `ContextualConditionOccurrenceTest` e migrar os asserts de requisito/characterization dos discoveries anteriores.
7. Rodar regressões focais e gates; não iniciar closure sem review.

### Checkpoint 3 — Closure — não autorizado

Promover somente o contrato durável necessário para domínio/evals/invariant, auditar must-not-change, rodar `full`, arquivar o work item e atualizar o mesmo PR.

## Dependências

- ADR-0012 e INV-COND-001/002 governam a projeção pós-binding e a cardinalidade.
- Slices 3 e 4 já fornecem containers de condition surface e `DataReference` lossless.
- `BACKLOG-RES-004` continua separado: não ampliar `QualifierTarget.UNSPECIFIED` para DATA/FILE.
- SEARCH WHEN continua Slice 6; o helper de condition surface pode ser reutilizado depois, mas este slice não materializa SEARCH.
- Review humano deste Discovery e autorização explícita são pré-condições do Checkpoint 2.

## Superfície arquitetural provável

### Produção esperada

| Arquivo | Mudança provável | Limite |
| --- | --- | --- |
| `Ast.java` | wrapper/tag não-node para controls PERFORM VALUE/CONDITION; compatibility view de expressions e `Ast.children` sem mudança de ordem/IDs | sem binding, sem annotation em todo DataReference |
| `AstBuilder.java` | construir controls a partir de `performTimes`, `performUntil` e `performVarying` tipados | sem source-text parsing, sem grammar change |
| `ReferenceOccurrenceCollector.java` | helper estrutural para condition surface e policy contextual; remover equality por `grammarRule` | sem lookup, sem materializar predicate |
| `ReferenceOccurrences.java` | alinhar Javadoc do primary hint contextual | sem schema/enum novo |
| `ReferenceResolutionManifest.java` | deixar de declarar `conditionNameReference → CONDITION`; versionar contract | sem alterar grammar coverage |

`Ast.java`/`AstBuilder.java` eram must-not-change na hipótese inicial. O challenge FACT `PERFORM N TIMES` versus `PERFORM UNTIL C` provou que a AST atual não oferece informação suficiente ao collector sem recorrer a `grammarRule` ou texto. A ampliação é limitada a metadata não-node; não altera condition surface, pre-order, provenance ou binding.

### Must-not-change audit

- `Cobol.g4`, `ResolutionContracts`, `DataAndIndexReferenceResolver`, `CobolReferenceResolver`, `SymbolTable`, `CompilationUnitSymbolTables`, `ReferenceResolution` e `CicsIntrinsicClassifier` byte-identical;
- zero ConditionSemantics/ConditionValidation/CFG/dataflow;
- zero occurrences sintéticas;
- zero mudança no mapping `UNSPECIFIED → {DATA}`;
- zero SEARCH WHEN;
- zero reparse textual, parent grammar rule, spelling ou connector heuristic.

## Migrações requeridas

- `SemanticConditionContextDiscoveryTest`: migrar o falso-gap opt-in para expectativas normativas verdes e manter os facts históricos que continuam úteis.
- `ConditionNameSurfaceDiscoveryTest`: remover/atualizar apenas o assert que caracteriza o collector por grammarRule; manter surface/qualification/subscript.
- `ReferenceResolutionManifestTest`: provar que a rule não carrega kind único e que a policy continua exaustiva.
- `ConditionSurfaceAstTest`: preservar relation/distributed policies e boundaries.
- `DataAndIndexReferenceResolverTest`: preservar SET/EVALUATE e selected candidate kind.
- `AstPreorderInvariantTest`: provar que a nova metadata PERFORM não consome ID nem muda `Ast.children`.
- snapshots: aceitar apenas deltas de `admissibleKinds`, status/reason/candidate dos tails afetados e metadata PERFORM deliberadamente exposta; nenhuma renumeração AST/occurrence.
- CICS: executar regressão sem alterar classifier.

O discovery test novo permanece FACT; os oracles de produção entram no arquivo reservado `planned:.../ContextualConditionOccurrenceTest.java` somente após autorização.

## Artefatos esperados

- production files acima no Checkpoint 2;
- `ContextualConditionOccurrenceTest.java` com CO-01..CO-14;
- migration dos discovery tests sem apagar evidência adversarial;
- consumer regressions de resolver, report/snapshot, CICS, SET/EVALUATE e pre-order;
- gates focais, `fast`, `semantic` e `full` reais;
- auditoria final do `must_not_change` e de zero grammarRule authority.
