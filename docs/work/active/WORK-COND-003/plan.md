# Plano — WORK-COND-003

## Fatiamento

1. **S3.1 — Modelo de superfície:** novos nodes tipados em `Ast.java` — `LogicalCondition` (com `LogicalConnector`), `GroupedCondition`, `RelationCondition`, `NegatedCondition`, `ContextualConditionTail`, `DistributedOperandGroup`, `ClassCondition` — todos `Ast.Expression`, com `Ast.children` cobrindo cada um exatamente uma vez.
2. **S3.2 — Lowering estrutural:** reescrever o caminho de condições do `AstBuilder` como descida estrutural com estado mínimo de abbreviation (somente para escolher a shape de bare nominals; nunca para materializar herança), folding de precedência AND>OR e materialização de todas as `abbreviation`s do tail.
3. **S3.3 — Suporte de consumidores:** `AstSnapshot` ganha label/attributes dos novos nodes; `ReferenceOccurrenceCollector` ganha somente o caso estrutural de `RelationCondition` replicando `visitRelationalOperand` (sem mudar regras de classificação de DATA/INDEX/CONDITION); nenhum outro consumidor muda.
4. **S3.4 — Oracles executáveis:** `ConditionSurfaceAstTest` com S3-01 a S3-10, identidade/pre-order (reusando INV-AST-003), provenance de escrito/omitido, metamórficas M1–M3 e oracle negativo anti-`grammarRule`.
5. **S3.5 — Migração de baselines:** atualizar somente asserts/snapshots diretamente afetados pela nova estrutura (discovery test, boundary matrix, coverage snapshot, typed traversal, structured expressions), com justificativa por mudança de cardinalidade.
6. **S3.6 — Gates e challenge pass:** `fast`, `semantic`, `full`; revisão adversarial contra as doze estratégias erradas listadas no slice.

## Dependências

- ADR-0012 `Accepted` (PR #16 mergeado), INV-COND-001/002 presentes — verificado em `main`.
- Grammar atual já reconhece `condition`/`andOrCondition`/`abbreviation+`/`relationCombinedComparison`; zero diff de grammar previsto (verificado com dump de parse tree dos casos S3).
- `AstBoundaryTestSupport.analyze` como entrada independente dos testes novos.
- `Inv-Cond` collectors/resolvers permanecem intactos; o falso gap CONDITION pode continuar neste slice.

## Superfície arquitetural provável

- `Ast.java`: os sete records novos + `LogicalConnector` + branches em `Ast.children` + permissões no sealed `Expression`.
- `AstBuilder.java`: novo bloco `buildConditionSurface`/`buildLogicalTree` substituindo o trecho atual de `conditionExpression` para `Condition`/`CombinableCondition`/`SimpleCondition`/`Relation*`/`ClassCondition`/`Abbreviation`; helpers `metaForRange` (span real entre dois contexts escritos para folds de precedência) e `spanOf` (span de token para parênteses).
- `AstSnapshot.java`: `label`/`attributes` para os novos types (snapshot permanece fail-closed posicional).
- `ReferenceOccurrenceCollector.java`: um único branch novo para `RelationCondition` que delega a `visitRelationalOperand` no subject/object presentes — suporte estrutural ao node novo, sem alterar a política de classificação. Documentado antes da edição conforme o protocolo.

Não criar: `ConditionSemantics.java`, `ConditionValidation.java`, projector, predicate, CFG/dataflow. Não alterar: grammar, `ReferenceOccurrences`, `ReferenceResolution`, `DataAndIndexReferenceResolver`, `CobolReferenceResolver`, symbol tables.

## Migrações requeridas

- `SemanticConditionContextDiscoveryTest`: substituir asserts da shape antiga (MIXED_LOGICAL, `abbreviation(0)`, PreservedExpression distribuído, `{DATA}` rígido) pela caracterização da nova surface, mantendo o diagnóstico do falso gap CONDITION intacto.
- `AstSemanticBoundaryCharacterizationTest`: a row `preserved-expression/abbreviation` vira boundary modelada (`abbreviation` agora produz `RelationCondition`); cardinalidades e findings recontados.
- `CoverageSnapshotTest`: `abbreviation` deixa o snapshot de coverage preservado; ajustar a contagem da boundary preservada.
- `AstBuilderTypedTraversalTest`/`StructuredExpressionAstTest`: asserts de tipo dos nodes de relação/condição migram para os novos types.
- Qualquer snapshot/ID afetado: diff explicado; nenhum expected de resolution é alterado para esverdear teste.

## Artefatos esperados

- Work item completo em `docs/work/active/WORK-COND-003/`; resumo `docs/work/history/WORK-COND-002.md` e ausência do diretório ativo anterior;
- AST de superfície lossless com os nodes novos, IDs pre-order contíguos e provenance correta;
- `ConditionSurfaceAstTest` cobrindo S3-01 a S3-10 + identidade/provenance + M1–M3 + oracle negativo;
- migrações de baselines com justificativa registrada em `state.md`;
- gates `fast`, `semantic` e `full` verdes; grammar sem diff.
