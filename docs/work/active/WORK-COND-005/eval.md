# Avaliação

## O que prova corretude

A implementação correta deriva a policy do caminho tipado da surface AST **e da shape nominal escrita** (`qualifiers()`/`subscriptGroups()`), produz exatamente uma root occurrence por nominal escrito, mantém qualifier/subscript independentes e permite ao resolver atual selecionar DATA, INDEX, CONDITION ou RENAMES-as-DATA sem reconstruir a condição. O conjunto de testes deve matar soluções por grammarRule, permissividade global, shape ignorance, connector spelling, duplicação de occurrences, context leakage, binding no collector, manifesto como policy e diagnostics que apresentam occurrence contextual como CONDITION exclusiva.

Oracles independentes: IBM Enterprise COBOL 6.4 nas seções de conditional expressions, abbreviated combined relation conditions, general relations, index-names, condition-name e RENAMES; ADR-0009; ADR-0012; INV-COND-001/002; INV-COV-002; `selectedCandidate.kind()` como resultado nominal final.

### Oracles de implementação

| ID | Caso | Expectativa |
| --- | --- | --- |
| CO-01 | `IF C` | root `CONDITION/{CONDITION}`; DATA homônimo não é aceito como simple condition |
| CO-02 | `A = C`, `A = C OF G`, `A = C(I)` | relation policy shape-sensitive: bare `INDEX/{DATA, INDEX}`; qualified/subscripted `DATA/{DATA}`; nunca CONDITION por origin |
| CO-03 | `A = B OR C`, C DATA | uma occurrence contextual bare; resolve DATA sem `INVALID_NAMESPACE_FOR_CONTEXT` |
| CO-04 | mesma surface, C level 88 | mesma AST/policy pré-binding; resolve CONDITION |
| CO-05 | mesma surface, C index-name | resolve INDEX; validade PIC/USAGE permanece fora do binding |
| CO-06 | mesma surface, C level-66 | resolve candidate kind DATA; nenhum `ReferenceKind.RENAMES` |
| CO-07 | C ausente | uma occurrence, zero candidates, `UNRESOLVED/DECLARATION_NOT_FOUND` |
| CO-08 | cardinalidade | um C escrito gera uma root occurrence; nenhum clone por admissible kind ou componente herdado |
| CO-09 | `A = B OR C OF G` | C contextual `CONDITION/{DATA, CONDITION}` (INDEX excluído do root qualified); G continua `QUALIFIER_COMPONENT` com policy própria |
| CO-10 | `A = B OR C(I)` | C contextual `CONDITION/{DATA, CONDITION}` (INDEX excluído do root subscripted); I continua `SUBSCRIPT` com `INDEX/{DATA, INDEX}` |
| CO-11 | `(A = B OR C)` versus `(A = B) OR C` | primeiro C contextual; segundo C standalone CONDITION |
| CO-12 | `A = (B OR C)`, `A = (B OF G OR C(I))` | B/C relation operands shape-sensitive (`{DATA, INDEX}` bare; `{DATA}` qualified/subscripted), não standalone/contextual |
| CO-13 | `NOT C`, `A NOT = C`, `A = B OR NOT C` | logical standalone, relational NOT e logical contextual permanecem distintos |
| CO-14 | SET/EVALUATE | policies tipadas existentes ficam byte/behavior-compatible |
| CO-15 | PERFORM | `N TIMES` fica VALUE e `UNTIL C` fica standalone CONDITION sem grammarRule |
| CO-16 | manifest | `conditionNameReference` é `CONTEXTUAL_REFERENCE_ORIGIN` com `referenceKind == null`; `qualifiedDataName` continua `REFERENCE_ORIGIN/DATA`; manifesto permanece exaustivo/versionado/closed; `VERSION` sobe |
| CO-17 | `A = B OR MISSING` | occurrence `kind = CONDITION`, `admissibleKinds = {DATA, INDEX, CONDITION}`; diagnóstico human-readable NÃO diz apenas `CONDITION reference`; indica contexto/incerteza |
| CO-18 | `A = B OR MISSING OF G` | occurrence `kind = CONDITION`, `admissibleKinds = {DATA, CONDITION}`; continua humanamente contextual, não CONDITION exclusiva |
| CO-19 | `IF MISSING` | occurrence `{CONDITION}`; diagnóstico `CONDITION reference` continua correto |
| CO-20 | `NOT A = B OR C` | C usa `contextualPolicy(shape(C))`, não `standaloneConditionPolicy`; C bare → `{DATA, INDEX, CONDITION}`; NOT da primeira relation não termina a abbreviation nem é herdado |

Revisões Round 2: CO-02 e CO-12 agora exigem relation operands shape-sensitive; CO-09 e CO-10 agora excluem INDEX do root qualified/subscripted e mantêm children independentes.

## Classes positivas

- WAUX-like abbreviated DATA tail bare, inclusive cadeia `A = B OR C OR D` e connectors AND/OR;
- contextual condition-name real (bare, qualified e subscripted conforme conditional variable);
- contextual index-name bare com binding nominal correto;
- contextual level-66 RENAMES tratado por DATA;
- qualification IN/OF e subscript preservados com children independentes;
- tails dentro de grupo que ainda mantém inheritance;
- distributed operands com policy shape-sensitive;
- `PERFORM UNTIL condition-name` após tipagem do control;
- SET condition target, EVALUATE boolean selector e EVALUATE value selector como controles.

## Classes negativas

- `IF C` com C somente DATA/INDEX/RENAMES: não ampliar standalone para `{DATA, INDEX, CONDITION}`;
- `A = C OF G` ou `A = C(I)` com C somente INDEX: INDEX não é namespace admissível do root (NEG-INDEX-QUAL-01 / NEG-INDEX-SUB-01);
- `A = B OR C OF G` ou `A = B OR C(I)` com C somente INDEX: não resolve como INDEX (NEG-CONTEXT-INDEX-QUAL-01 / NEG-CONTEXT-INDEX-SUB-01);
- `A = C` com C somente CONDITION: relation operand não admite condition-name;
- `(A = B) OR C` com C somente DATA: boundary impede abbreviation;
- `A = (B OR CONDITION-88)`: simple condition dentro do scope distribuído é IBM inválido;
- `A = (B OR C = D)`: novo relational operator dentro da distribuição é inválido;
- `A = (NOT B OR C)`: logical NOT imediatamente após o `(` distribuído é inválido;
- `C IS NUMERIC`: class subject não recebe CONDITION pela grammar ancestry;
- SEARCH WHEN não ganha occurrence incidental neste slice.

### Negative oracles Round 2 (fixture IBM-válida ou what-if nominal isolado claramente documentado)

| ID | Caso | Expectativa |
| --- | --- | --- |
| NEG-INDEX-QUAL-01 | `A = C OF G`, C somente INDEX | INDEX não é namespace admissível do root; policy qualified `{DATA}` não seleciona INDEX |
| NEG-INDEX-SUB-01 | `A = C(I)`, C somente INDEX | INDEX não é namespace admissível do root; policy subscripted `{DATA}` não seleciona INDEX |
| NEG-CONTEXT-INDEX-QUAL-01 | `A = B OR C OF G`, C somente INDEX | não resolve como INDEX sob `{DATA, CONDITION}` |
| NEG-CONTEXT-INDEX-SUB-01 | `A = B OR C(I)`, C somente INDEX | não resolve como INDEX sob `{DATA, CONDITION}`; subscript I mantém `{DATA, INDEX}` |

## Classes ambíguas

- Condition-names duplicados ainda admissíveis após qualification: `AMBIGUOUS`, todos candidates, nenhum selected candidate.
- Data-names duplicados com qualification insuficiente: mesma postura.
- Qualify mode desconhecido com resultados STANDARD/EXTEND divergentes: `UNSUPPORTED_DIALECT_OPTION` conforme resolver atual.
- DATA+CONDITION homônimos no mesmo programa são source IBM inválido e não definem precedência de candidate.
- Colisão qualificada local/GLOBAL já caracterizada permanece `BACKLOG-RES-004`; não alterar resolver nem o oracle do Slice 5 para mascará-la.

## Casos adversariais

1. Trocar o if de `grammarRule` por outro nome/regra parental: CO-01/02/03/11/15 exigem typed position.
2. Tornar todo `conditionNameReference` `{DATA, INDEX, CONDITION}`: CO-01 e standalone NOT falham.
3. Tornar todo `ContextualConditionTail` `{CONDITION}`: CO-03/05/06 reproduzem WAUX.
4. Tornar todo `ContextualConditionTail` `{DATA, INDEX, CONDITION}` independente da shape: NEG-CONTEXT-INDEX-QUAL-01/SUB-01 e CO-09/10 falham.
5. Tornar todo relation operand `{DATA, INDEX}` independente da shape: NEG-INDEX-QUAL-01/SUB-01 e CO-02/12 falham.
6. Ignorar `qualifiers()`/`subscriptGroups()` e usar apenas o container AST: R2-3 e os NEG shape falham.
7. Contaminar relation operand com CONDITION: CO-02/12 falham.
8. Criar occurrence por kind: CO-08 e bijection occurrence↔resolution falham.
9. Propagar root context a qualifier: CO-09 falha.
10. Propagar root context a subscript: CO-10 falha.
11. Tratar todo IF como um único contexto: CO-11 falha.
12. Usar OR como sinal de contextualidade: `A = B AND C` e mixed AND/OR falham.
13. Colocar lookup/symbol table no collector: architecture boundary e phase assertions falham.
14. Materializar `A = C` ou occurrences para subject/operator omitidos: cardinality/pre-order falham.
15. Escolher primary DATA/INDEX como binding final: declaration substitution e `selectedCandidate.kind()` falham; primary decidido é CONDITION hint (shape-sensitive para relation operands).
16. Inferir PERFORM pelo texto `UNTIL`: CO-15 e review anti-textual falham.
17. Manter `conditionNameReference → REFERENCE_ORIGIN/CONDITION` no manifesto: CO-16 falha mesmo que o collector esteja correto (R2-4).
18. Classificar `conditionNameReference` como `REFERENCE_CONTAINER`: CO-16 falha porque a rule é root occurrence origin contextual (R2-5).
19. Colocar `conditionNameReference → {DATA, INDEX, CONDITION}` no manifesto como policy global: rejeitado arquiteturalmente (R2-6); manifesto classifica coverage/origin, não occurrence policy.
20. Diagnosticar contextual unresolved como `UNRESOLVED CONDITION reference` sem indicar admissibilidade múltipla: CO-17/18 falham (R2-7).
21. Tratar C de `NOT A = B OR C` como standalone: CO-20 falha (R2-8).
22. Aplicar NOT também à relation herdada de C (`NOT(A = C)`): CO-20 rejeita (R2-9).

## Casos de regressão

- `ConditionSurfaceAstTest`: surface topology, AND precedence, group boundaries, distributed operands, NOT e IDs.
- `ConditionNameSurfaceAstTest`/Discovery: base, qualifiers, subscript groups, provenance e `UNSPECIFIED`.
- `DataAndIndexReferenceResolverTest`: relation/subscript primary hint, SET condition targets, EVALUATE TRUE/FALSE/value, DATA/INDEX/RENAMES, ambiguity e scope.
- `ReferenceResolutionManifestTest`: todas as grammar rules classificadas; `conditionNameReference` sem kind único; `qualifiedDataName` permanece origin DATA.
- `ResolutionAnalysisReportTest` e `ResolutionSnapshotTest`: syntactic versus resolved semantic kinds, cardinalidade e deltas estruturados; `syntacticKindCounts` continua syntactic (kind hint); `resolvedSemanticKindCounts` usa `selectedCandidate().kind()`.
- `CicsIntrinsicClassifierTest`: classifier continua ortogonal e só atua após failure COBOL.
- `AstPreorderInvariantTest`: `PerformControl` não-node não consome ID e expressions mantêm pre-order.
- `SemanticConditionContextDiscoveryTest`: IF/EVALUATE/PERFORM com tails contextuais e o falso gap original.
- ausência de qualquer diff em resolver (algorithm), grammar, symbols e CICS; wording-only permitido nos resolver/report consumers conforme contrato de diagnóstico.

## Propriedades/relações metamórficas

1. **Declaration substitution (dividida por shape, Round 2):** a condition surface e a occurrence pré-binding são idênticas quando só a declaração muda.
   - Bare `A = B OR C` com C = DATA / INDEX / CONDITION / RENAMES / ausente: AST idêntica; policy contextual `{DATA, INDEX, CONDITION}`; muda apenas o resolution result.
   - Qualified `A = B OR C OF G`: policy root `{DATA, CONDITION}`; INDEX não participa.
   - Subscripted `A = B OR C(I)`: policy root `{DATA, CONDITION}`; policy de I `{DATA, INDEX}`; mudar I entre DATA e INDEX muda `selectedCandidate.kind()` do subscript, não a policy do root.
2. **Cardinality:** cardinalidade de root occurrences depende dos nomes escritos, não da quantidade de admissible kinds ou candidates.
3. **Expansion:** `A = B OR C` com C DATA e `A = B OR A = C` selecionam as mesmas entidades nominais escritas correspondentes, sem inventar occurrence para o A omitido.
4. **Connector:** trocar OR por AND não muda a policy do tail contextual.
5. **Case:** variar caixa não muda admissibility nem selected entity.
6. **Qualification:** qualification suficiente e redundante não muda a entidade; qualifier occurrence permanece independente; adicionar qualifier a um index-name muda a shape e retira INDEX da admissibility (bare vs qualified).
7. **Subscript:** trocar I entre DATA e INDEX muda `selectedCandidate.kind()` do subscript, não a policy do root; adicionar subscript ao root retira INDEX da admissibility do root.
8. **Neutral grouping:** parêntese que não cruza boundary preserva policy; `(A = B) OR C` é contracaso deliberado.
9. **SET/EVALUATE stability:** adicionar as condições do slice ao mesmo programa não altera policies desses constructs.
10. **Resolver reuse:** projetar em teste as policies shape-sensitive sobre a mesma AST é suficiente para o resolver atual bindar os kinds suportados e excluir INDEX fora da shape.
11. **NOT isolation:** `NOT C` (standalone), `NOT A = B OR C` (C contextual) e `A NOT = C` (relation operand) permanecem estruturalmente distintos.

## Expectativas de escala

- Uma traversal de cada AST; dispatch por tipo, shape e contexto constante por node.
- Uma occurrence por reference AST node, com IDs contíguos e determinísticos.
- Nenhum scan textual, parent grammar inspection ou lookup de declarations no collector.
- Candidate lookup continua indexado no resolver atual; nenhuma mudança em `O(references + same-name candidates)`.
- Nenhum threshold dependente de hardware; o gate `performance` não é exigido no Discovery porque não há código algorítmico novo, mas a implementação deve preservar INV-PERF-001.
