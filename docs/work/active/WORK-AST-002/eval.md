# Avaliação

## O que prova corretude

A mesma fixture controlada é contada independentemente na parse tree e na AST; cada fronteira possui cardinalidade exata, node ID único/alcançável, coverage esperado, dependency knowledge esperado e regra de occurrence explícita. Um reconciliador linear de teste valida joins por `ProgramUnitId` e IDs locais sem lookup textual. Oráculos de requisito isolados demonstram qualquer diferença entre o contrato promovido e a produção atual.

## Classes positivas

- Statements simples e aninhados, data entries/grupos, levels 66/77/88, FILLER, clauses tipadas/preservadas e expressão preservada.
- Referências DATA resolvidas com AST node, scope, occurrence, resolution entry e declaration candidate coerentes.
- CALL literal externo observado e CALL por identifier com binding DATA, mantendo target final desconhecido.
- REDEFINES/RENAMES válidos com endpoints nominais e semantic scope estrutural.
- Repetição da análise e múltiplas program units com IDs locais repetidos sob identidades compostas distintas.

## Classes negativas

- Finding ausente, duplicado ou apontando para node inexistente.
- AST node duplicado/não alcançável, scope ausente, símbolo/relation órfão, occurrence sem resolution entry e candidate com domínio/unit/local ID inexistente.
- FILLER transformado em símbolo nominal, CALL identifier transformado em candidate PROGRAM ou unknown tratado como coleção vazia.

## Classes ambíguas

Bindings `AMBIGUOUS` válidos não são falha de integridade. Taxonomia final de `DependencyKnowledge` para clauses estruturalmente modeladas mas ainda sem storage/effects será mantida aberta quando mais de uma política arquitetural for razoável.

## Casos adversariais

FILLER antes/entre/depois de itens nomeados, FILLER REDEFINES, RENAMES com THRU, VALUE sem referência, clause preservada sem referência, qualifier/subscript/reference modification, IF/EVALUATE aninhados, linguagem embarcada e nested program com colisões intencionais de IDs locais.

## Casos de regressão

EVAL-AST-001 a 004, EVAL-COV-001/002, EVAL-SYM-001/002, EVAL-RES-REL-001, EVAL-RES-COV-001, EVAL-RES-REPORT-001, EVAL-RES-DET-001 e EVAL-RES-PERF-001. Cardinalidades existentes de AST/symbols/occurrences/resolution não serão alteradas nesta fase.

## Propriedades/relações metamórficas

- Repetir a análise preserva AST, findings, IDs e ordem.
- Inserir outro program unit reinicia IDs locais, mas não colide a identidade composta.
- Alterações semanticamente irrelevantes de caixa não mudam binding canônico.
- Remover toda referência nominal de uma construção desconhecida não pode remover seu gap de coverage.
- Resolver um endpoint nominal não autoriza inferir layout, alias, value ou target dinâmico.

## Expectativas de escala

O reconciliador de joins do teste percorre nodes, scopes, symbols, relations, occurrences, entries e candidates em `O(n)` no tamanho agregado dos produtos, usando índices auxiliares lineares. Nenhum oracle varre todas as declarações para cada referência. `check-performance.sh` não é gate desta fase porque produção não será alterada.

## Resultado do discovery

### 1. Executive summary

`BACKLOG-AST-001` exige mudanças de produção, mas a dimensão aparente é **média e localizada**, não uma reconstrução da AST. A AST atual já preserva statements, entries, clauses, grupos, FILLER, VALUE, REDEFINES/RENAMES, CALL e provenance com cardinalidade e reachability corretas na matriz. Os joins gerados pelo pipeline normal também reconciliam integralmente.

Duas lacunas de produção foram comprovadas: (1) coverage concreto existe apenas para statements, permitindo falsa readiness quando a incerteza não contém referência nominal; (2) não há validação cross-product fail-closed suficiente para rejeitar produtos combinados com IDs/occurrences órfãos. O manifesto também está desalinhado da shape AST tipada e precisa ser revisto junto do primeiro slice. Nenhuma evidência exige mudar gramática, parser, shapes de statements/CALL, binding nominal ou inferir storage/dataflow.

### 2. Guarantee matrix

| Garantia de BACKLOG-AST-001 | Estado | Evidência executável e observação |
| --- | --- | --- |
| 1. Cada statement e declaração DATA relevante possui AST única, determinística e alcançável | **SATISFIED** na superfície caracterizada | `matrixCountsParseContextsAstNodesAndCurrentCoverageExactly`: 10/10 statements, 14/14 data entries, 20/20 clauses e 1/1 preserved expression; `allFiftyStatementAlternativesRemainOneToOneFromContextToReachableAst` cobre as 50 alternativas. `Ast.children` e `AstScopeIndex` reconciliam todos os nodes. |
| 2. Unknown dependency-bearing produz incompletude mesmo sem referência nominal | **MISSING** | `characterizesCoverageBlindSpotForUnknownClauseWithoutNominalReference` observa `VALUE` + `BLANK WHEN ZERO` com apenas finding de `GOBACK`, zero gaps e readiness `true`. O oracle ativável exige o oposto e falha. Preserved expression sem referência também possui zero finding. |
| 3. Cada referência DATA coletada junta AST, scope, resolução e declaração candidate sem texto | **SATISFIED** para produtos normais | `everyReachableDataOrIndexReferenceHasOneOccurrenceInTheSameUnit` prova bijeção por unit; `assertActualProductsJoin` valida meta/provenance, scope, entry e candidate/declaration por índices compostos. Nenhum lookup usa written text no oracle. |
| 4. Grupos, FILLERs e endpoints REDEFINES/RENAMES permanecem disponíveis | **SATISFIED**, com uma decisão de contrato aberta | Grupo conserva nove filhos diretos; FILLERs antes/entre/depois têm `DataEntry`+scope e nenhum símbolo nominal; target de `FILLER REDEFINES` possui occurrence/resolution `RESOLVED`; RENAMES conserva from/through e relation resolution estrutural; OCCURS conserva bounds, DEPENDING ON e index-name. A ausência de `DeclarationRelation` com owner FILLER não perde o endpoint, mas o contrato do consumidor futuro deve escolher AST+occurrence ou relação sem owner-symbol. |
| 5. CALL literal e identifier/expression permanecem separados | **SATISFIED** | Dois CALLs exatos: literal → `ProgramReference` + `EXTERNAL_OBSERVED` sem candidate; identifier → `DataReference` + candidate `DATA_SYMBOL` e gap `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN`. |
| 6. Joins usam identidade composta, com provenance/exatidão preservadas | **PARTIAL** | IDs AST/symbol/occurrence reiniciam em parent e child; `ProgramUnitId`/`SemanticEntityId` evitam colisão e o pipeline normal reconcilia. Occurrence meta é idêntica ao AST node; CICS transformado mantém origem e `exact=false`. Porém `ResolutionAnalysisReport` aceita entry ausente do occurrence product e `SymbolTable` aceita `declarationAstNodeId` fora da AST porque não recebe esse produto. |

### Matriz de fronteiras v1

| Context ANTLR / fronteira | AST esperado | Cardinalidade fixture | Coverage esperado | Dependency knowledge esperado | Occurrence | Join esperado | Observado |
| --- | --- | ---: | --- | --- | --- | --- | --- |
| `statement` → concrete statement | `Ast.Statement` | 10 → 10 | policy da alternativa; 1 finding/node | policy da alternativa | somente filhos nominais | `(unit, astNodeId)` | 10 findings; **satisfeito** |
| `dataDescriptionEntry` | `Ast.DataEntry` | 14 → 14 | estrutura modelada/preservada explicitamente; 1 finding/node | rule/container-specific | entry não gera occurrence; clauses podem gerar | `(unit, astNodeId)` | 0 findings; **coverage ausente** |
| data clause direta | `Ast.DataClause` tipada ou preservada | 20 → 20 | typed=`MODELED`; fallback=`PRESERVED_UNINTERPRETED` | clause-specific; VALUE/fallback unknown | somente endpoints/refs nominais | `(unit, astNodeId)` | 0 findings; **coverage ausente** |
| `abbreviation` fallback | `Ast.PreservedExpression` | 1 → 1 | `PRESERVED_UNINTERPRETED` | `DEPENDENCY_UNKNOWN` | operands reconhecidos, se nominais | `(unit, astNodeId)` | 0 findings; **coverage ausente** |
| `dataRedefinesClause` em FILLER | `RedefinesClause(DataReference)` sob `DataEntry` | 1 → 1 | modeled structure | storage/alias unknown | 1 occurrence nominal do endpoint | `(unit, targetNodeId)` | target resolvido; owner sem símbolo/relation específica |
| `dataRenamesClause` range | `RenamesClause(from, through)` | 1 → 1 | modeled structure | storage range unknown | 2 occurrences + relations | composite relation/entity IDs | estruturalmente resolvido |
| CALL literal | `CallStatement` + `ProgramReference` | 1 → 1 | modeled/reference-ready | linkage conforme policy | PROGRAM/CALL_TARGET | composite occurrence/entity | external observed, sem candidate |
| CALL identifier | `CallStatement` + `DataReference` | 1 → 1 | modeled/reference-ready | target value unknown | DATA/CALL_TARGET | composite occurrence/entity | DATA resolved + dynamic-target gap |
| `execCicsStatement` preprocessado | `EmbeddedLanguageStatement(CICS)` | 1 → 1 | preserved | dependency unknown | nenhuma no core opaco | `(unit, astNodeId)` + provenance | finding/gap presente; `exact=false` preservado |
| `dataDescriptionEntryExecSql` | `DataEntry(level=SQL, OPAQUE)` | 1 → 1 | preserved | dependency unknown | nenhuma | `(unit, astNodeId)` | distinguível, mas `filler=true`; contrato ambíguo |

### 3. Findings

#### F-01 — Coverage concreto omite DATA e fallbacks sem referências

- **Classificação:** 3. lacuna de coverage/observabilidade.
- **Evidência:** matrix 14/14 entries, 20/20 clauses e 1/1 preserved expression versus 0 findings; value-only fica ready. `AstBuilder.buildStatement()` é o único caminho que adiciona `CoverageDraft`.
- **Contrato/invariant/eval:** ADR-0008/0009; INV-AST-002, INV-COV-001/002; EVAL-AST-003/004, EVAL-COV-001/002, EVAL-RES-COV-001 e EVAL-RES-REPORT-001.
- **Impacto:** CFG/dataflow ou outro consumidor pode interpretar ausência de gap como ausência de semântica relevante; VALUE/fallback desaparece da claim.
- **Menor superfície provável:** registro de findings em `AstBuilder` nas quatro fronteiras e classificação coerente no manifesto. `ResolutionAnalysisReport.addFrontendGaps` já bloqueia unknown quando o finding existe.
- **Riscos:** findings duplicados por wrapper, IDs não determinísticos, overblocking e confundir estrutura tipada com efeito/storage conhecido.
- **Alternativas:** emitir no momento do lowering (mantém context/meta exatos) ou fazer um passe AST pós-build com mapeamento explícito de boundary; o segundo não pode recuperar grammar policy por texto.

#### F-02 — Integridade cross-product não falha fechada

- **Classificação:** 2. lacuna comprovada de implementação.
- **Evidência:** relatório aceita resolution entries ausentes do occurrence product; `SymbolTable` aceita `declarationAstNodeId=Integer.MAX_VALUE`. Os dois oráculos ativáveis falham porque nenhuma exception ocorre.
- **Contrato/invariant/eval:** ADR-0003/0005; INV-SYM-001, INV-PROV-002, INV-DET-001, INV-PERF-001; EVAL-SYM-001/002, EVAL-RES-REL-001, EVAL-RES-DET-001/PERF-001.
- **Impacto:** consumidor futuro pode juntar fatos de units/produtos incompatíveis e obter readiness aparentemente válida.
- **Menor superfície provável:** validator linear cross-product em ponto de composição, complementado por checks locais onde o produto possui contexto suficiente.
- **Riscos:** espalhar validação parcial, exigir scans quadráticos ou transformar `UNRESOLVED`/`AMBIGUOUS` válidos em erro interno.
- **Alternativas:** validator separado antes do report; ampliar `compose` com AST/tables; combinação de constructors locais + integrador. A escolha permanece aberta.

#### F-03 — Manifesto e AST discordam sobre estrutura já tipada

- **Classificação:** 4. inconsistência entre produtos.
- **Evidência:** `ValueClause`, `RedefinesClause`, `RenamesClause`, `OccursClause`, `DataReference`, `tableCall` e reference modification são estruturados, enquanto várias entradas do manifesto ainda dizem `PRESERVED_UNINTERPRETED`/“flattened”.
- **Contrato/invariant/eval:** ADR-0009; INV-AST-002, INV-COV-002; EVAL-AST-002/004 e EVAL-COV-001.
- **Impacto:** ao generalizar findings, o manifesto atual produziria coverage estrutural falsa e rationale obsoleto.
- **Menor superfície provável:** revisão rule-by-rule do manifesto e asserts da matriz; não exige alterar a AST já tipada.
- **Riscos:** converter structure-known em dependency-known; apagar incerteza de storage/value/effects.
- **Alternativas:** manter `MODELED + DEPENDENCY_UNKNOWN` para regras tipadas cujo significado posterior não existe; usar `PRESERVED_UNINTERPRETED` somente em fallbacks/opaque reais.

#### F-04 — Entrada SQL opaca conflita `OPAQUE` com `filler=true`

- **Classificação:** 5. contrato/documentação insuficiente ou ambíguo.
- **Evidência:** `dataDescriptionEntryExecSql` vira `DataEntry(level="SQL", levelKind=OPAQUE, name="FILLER", filler=true)` e mantém raw declaration/provenance.
- **Impacto:** um storage consumer ingênuo poderia confundir container SQL opaco com FILLER COBOL, embora `levelKind=OPAQUE` permita distingui-los.
- **Menor superfície provável:** nenhuma até a revisão decidir se `filler` significa ausência de declarator ou FILLER semântico.
- **Riscos/alternativas:** mudar a shape pode afetar consumers; documentar que OPAQUE domina `filler`, separar kind dedicado ou modelar container DATA próprio são alternativas razoáveis ainda não escolhidas.

#### F-05 — `RawExpression` em fonte válida

- **Classificação:** 7. hipótese ainda não comprovada.
- **Evidência:** o builder cria `RawExpression` apenas quando recebe context nulo; a fixture válida e as 50 alternativas não produziram nenhum.
- **Próximo passo:** não criar requisito. Se um caso válido o produzir, decidir se é invariant interno ou preserved boundary com finding.

#### F-06 — VALUE runtime, aliases, effects e targets dinâmicos

- **Classificação:** 6. pertence a CFG/dataflow/storage e está fora de escopo.
- **Evidência:** IBM define VALUE como conteúdo inicial; a AST preserva lexemas, não possible values. REDEFINES/RENAMES possuem binding nominal estrutural, não regiões. CALL identifier conserva target final desconhecido.

### 4. False alarms

- Não falta lowering estrutural geral de statements: as 50 alternativas e statements aninhados são 1:1 e alcançáveis.
- Não falta uma AST de DATA básica: entries, special levels, groups, clauses tipadas, preserved clauses e lexemas VALUE já existem.
- FILLER não desaparece: possui `DataEntry`, meta, scope e posição; corretamente não possui símbolo nominal.
- `FILLER REDEFINES` não perde o endpoint: a referência DATA é coletada e resolvida; o que permanece aberto é apenas a forma ideal da relação com owner não simbólico.
- RENAMES conserva `from` e `through`, e a resolução declara explicitamente semantic scope apenas nominal/estrutural.
- CALL literal e variável não estão colapsados; binding DATA nunca vira candidate PROGRAM.
- `AstScopeIndex` cobre cada node alcançável exatamente uma vez e rejeita node ID duplicado dentro da unit.
- IDs locais repetidos entre nested units não colidem nos produtos normais; occurrences/candidates/relations transportam unit.
- Provenance não é recriada: nodes normais permanecem exatos e CICS transformado permanece aproximado com arquivo original.
- `ResolutionAnalysisReport` já sabe bloquear findings unknown; a entrada que falta é o finding concreto, não nova lógica de gap para esse caso.

### 5. Open questions

1. Qual taxonomia exata de dependency knowledge deve valer para `DataEntry`, PICTURE, USAGE, OCCURS e wrappers, evitando gaps duplicados sem alegar storage conhecido?
2. O validator cross-product deve viver como produto separado na orquestração, dentro de `ResolutionAnalysisReport.compose` com assinatura ampliada ou em estratégia híbrida?
3. `DataEntry.filler` significa FILLER COBOL ou simplesmente declarator ausente? A entrada SQL opaca exige decisão humana.
4. O futuro storage consumer pode usar AST containment + occurrence/resolution para `FILLER REDEFINES`, ou necessita uma declaration relation cujo owner seja AST node em vez de symbol?
5. A apresentação `AnalysisClaim.COMPLETE` precisa ser renomeada ou acompanhada de capability/version explícita para impedir leitura como CFG/dataflow-ready?
6. Nenhuma evidência prova `RawExpression` em input válido; isso permanece hipótese, não requisito.

### 6. Recommended implementation slices

Os três slices detalhados em `plan.md` são a menor sequência recomendada: (1) coverage concreto + manifesto coerente; (2) validator cross-product linear; (3) regressão/documentação/claim. Os dois primeiros são independentes e podem ser revisados separadamente, mas ambos devem estar verdes antes de `BACKLOG-CFG-001`. Nenhum slice autoriza CFG, storage, effects ou possible values.

### 7. Readiness assessment

**READY FOR IMPLEMENTATION**, condicionado à revisão independente deste PR e instrução explícita posterior. A evidência diferencia capacidade já existente de duas lacunas comprovadas, há quatro oráculos vermelhos reproduzíveis e a provável superfície é limitada. As alternativas arquiteturais abertas estão enumeradas e podem ser decididas na revisão sem exigir mais exploração de corpus ou mudança de produção nesta sessão.

## Execução dos oráculos

- Verde focal: `mvn -Dtest=AstSemanticBoundaryCharacterizationTest test` — 8 testes, 0 falhas.
- Vermelho intencional: `mvn -Dast.boundary.required=true -Dtest=AstSemanticBoundaryRequiredOracleTest test` — 4 testes, 4 falhas esperadas, cada uma ligada a F-01 ou F-02.
- Gate normal: os required oracles usam `@EnabledIfSystemProperty`; compilam sempre e ficam skipped salvo ativação explícita. Isso mantém o harness normal como sinal de regressão, sem esconder o comando que reproduz as lacunas.
- Gate `fast`: verde após a promoção e a documentação do discovery.
- Gate `semantic`: verde; 210 testes, 0 falhas, 0 erros e 4 skips intencionais dos required oracles opt-in.
