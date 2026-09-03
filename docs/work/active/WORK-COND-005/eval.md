# Avaliação

## O que prova corretude

A implementação correta deriva a policy do caminho tipado da surface AST, produz exatamente uma root occurrence por nominal escrito, mantém qualifier/subscript independentes e permite ao resolver atual selecionar DATA, INDEX, CONDITION ou RENAMES-as-DATA sem reconstruir a condição. O conjunto de testes deve matar soluções por grammarRule, permissividade global, connector spelling, duplicação de occurrences, context leakage e binding no collector.

Oracles independentes: IBM Enterprise COBOL 6.4 nas seções de conditional expressions, abbreviated combined relation conditions, general relations, index-names, condition-name e RENAMES; ADR-0012; INV-COND-001/002; `selectedCandidate.kind()` como resultado nominal final.

### Oracles de implementação

| ID | Caso | Expectativa |
| --- | --- | --- |
| CO-01 | `IF C` | root `CONDITION/{CONDITION}`; DATA homônimo não é aceito como simple condition |
| CO-02 | `A = C` | C usa relation policy `INDEX/{DATA, INDEX}`, nunca CONDITION por origin |
| CO-03 | `A = B OR C`, C DATA | uma occurrence contextual; resolve DATA sem `INVALID_NAMESPACE_FOR_CONTEXT` |
| CO-04 | mesma surface, C level 88 | mesma AST/policy pré-binding; resolve CONDITION |
| CO-05 | mesma surface, C index-name | resolve INDEX; validade PIC/USAGE permanece fora do binding |
| CO-06 | mesma surface, C level-66 | resolve candidate kind DATA; nenhum `ReferenceKind.RENAMES` |
| CO-07 | C ausente | uma occurrence, zero candidates, `UNRESOLVED/DECLARATION_NOT_FOUND` |
| CO-08 | cardinalidade | um C escrito gera uma root occurrence; nenhum clone por admissible kind ou componente herdado |
| CO-09 | `A = B OR C OF G` | C contextual; G continua `QUALIFIER_COMPONENT` com policy própria |
| CO-10 | `A = B OR C(I)` | C contextual; I continua `SUBSCRIPT` com `{DATA, INDEX}` |
| CO-11 | `(A = B OR C)` versus `(A = B) OR C` | primeiro C contextual; segundo C standalone CONDITION |
| CO-12 | `A = (B OR C)` | B/C relation operands `{DATA, INDEX}`, não standalone/contextual |
| CO-13 | `NOT C`, `A NOT = C`, `A = B OR NOT C` | logical standalone, relational NOT e logical contextual permanecem distintos |
| CO-14 | SET/EVALUATE | policies tipadas existentes ficam byte/behavior-compatible |
| CO-15 | PERFORM | `N TIMES` fica VALUE e `UNTIL C` fica standalone CONDITION sem grammarRule |
| CO-16 | manifest | `conditionNameReference` não publica kind nominal único; manifesto permanece exaustivo/versionado |

## Classes positivas

- WAUX-like abbreviated DATA tail, inclusive cadeia `A = B OR C OR D` e connectors AND/OR;
- contextual condition-name real;
- contextual index-name com binding nominal correto;
- contextual level-66 RENAMES tratado por DATA;
- qualification IN/OF e subscript preservados;
- tails dentro de grupo que ainda mantém inheritance;
- distributed operands;
- `PERFORM UNTIL condition-name` após tipagem do control;
- SET condition target, EVALUATE boolean selector e EVALUATE value selector como controles.

## Classes negativas

- `IF C` com C somente DATA/INDEX/RENAMES: não ampliar standalone para `{DATA, INDEX, CONDITION}`;
- `A = C` com C somente CONDITION: relation operand não admite condition-name;
- `(A = B) OR C` com C somente DATA: boundary impede abbreviation;
- `A = (B OR CONDITION-88)`: simple condition dentro do scope distribuído é IBM inválido;
- `A = (B OR C = D)`: novo relational operator dentro da distribuição é inválido;
- `A = (NOT B OR C)`: logical NOT imediatamente após o `(` distribuído é inválido;
- `C IS NUMERIC`: class subject não recebe CONDITION pela grammar ancestry;
- SEARCH WHEN não ganha occurrence incidental neste slice.

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
4. Contaminar relation operand com CONDITION: CO-02/12 falham.
5. Criar occurrence por kind: CO-08 e bijection occurrence↔resolution falham.
6. Propagar root context a qualifier: CO-09 falha.
7. Propagar root context a subscript: CO-10 falha.
8. Tratar todo IF como um único contexto: CO-11 falha.
9. Usar OR como sinal de contextualidade: `A = B AND C` e mixed AND/OR falham.
10. Colocar lookup/symbol table no collector: architecture boundary e phase assertions falham.
11. Materializar `A = C` ou occurrences para subject/operator omitidos: cardinality/pre-order falham.
12. Escolher primary DATA/INDEX como binding final: declaration substitution e `selectedCandidate.kind()` falham; primary decidido é CONDITION hint.
13. Inferir PERFORM pelo texto `UNTIL`: CO-15 e review anti-textual falham.
14. Manter `conditionNameReference → CONDITION` no manifesto: CO-16 falha mesmo que o collector esteja correto.

## Casos de regressão

- `ConditionSurfaceAstTest`: surface topology, AND precedence, group boundaries, distributed operands, NOT e IDs.
- `ConditionNameSurfaceAstTest`/Discovery: base, qualifiers, subscript groups, provenance e `UNSPECIFIED`.
- `DataAndIndexReferenceResolverTest`: relation/subscript primary hint, SET condition targets, EVALUATE TRUE/FALSE/value, DATA/INDEX/RENAMES, ambiguity e scope.
- `ReferenceResolutionManifestTest`: todas as grammar rules classificadas.
- `ResolutionAnalysisReportTest` e `ResolutionSnapshotTest`: syntactic versus resolved semantic kinds, cardinalidade e deltas estruturados.
- `CicsIntrinsicClassifierTest`: classifier continua ortogonal e só atua após failure COBOL.
- `AstPreorderInvariantTest`: `PerformControl` não-node não consome ID e expressions mantêm pre-order.
- `SemanticConditionContextDiscoveryTest`: IF/EVALUATE/PERFORM com tails contextuais e o falso gap original.
- ausência de qualquer diff em resolver, grammar, symbols e CICS.

## Propriedades/relações metamórficas

1. **Declaration substitution:** a condition surface e a occurrence pré-binding de `A = B OR C` são idênticas quando só a declaração de C muda entre DATA, INDEX, CONDITION, RENAMES e ausente; muda apenas o resolution result.
2. **Cardinality:** cardinalidade de root occurrences depende dos nomes escritos, não da quantidade de admissible kinds ou candidates.
3. **Expansion:** `A = B OR C` com C DATA e `A = B OR A = C` selecionam as mesmas entidades nominais escritas correspondentes, sem inventar occurrence para o A omitido.
4. **Connector:** trocar OR por AND não muda a policy do tail contextual.
5. **Case:** variar caixa não muda admissibility nem selected entity.
6. **Qualification:** qualification suficiente e redundante não muda a entidade; qualifier occurrence permanece independente.
7. **Subscript:** trocar I entre DATA e INDEX muda `selectedCandidate.kind()` do subscript, não a policy do root.
8. **Neutral grouping:** parêntese que não cruza boundary preserva policy; `(A = B) OR C` é contracaso deliberado.
9. **SET/EVALUATE stability:** adicionar as condições do slice ao mesmo programa não altera policies desses constructs.
10. **Resolver reuse:** projetar em teste `{DATA, INDEX, CONDITION}` com primary CONDITION sobre a mesma AST é suficiente para o resolver atual bindar os quatro declaration kinds suportados.

## Expectativas de escala

- Uma traversal de cada AST; dispatch por tipo e contexto constante por node.
- Uma occurrence por reference AST node, com IDs contíguos e determinísticos.
- Nenhum scan textual, parent grammar inspection ou lookup de declarations no collector.
- Candidate lookup continua indexado no resolver atual; nenhuma mudança em `O(references + same-name candidates)`.
- Nenhum threshold dependente de hardware; o gate `performance` não é exigido no Discovery porque não há código algorítmico novo, mas a implementação deve preservar INV-PERF-001.
