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
| 6. Joins usam identidade composta, com provenance/exatidão preservadas | **PARTIAL** | IDs AST/symbol/occurrence reiniciam em parent e child; `ProgramUnitId`/`SemanticEntityId` evitam colisão e o pipeline normal reconcilia. Occurrence meta é idêntica ao AST node; CICS transformado mantém origem e `exact=false`. Porém a composição aceita entry ausente do occurrence product e uma symbol table controlada cujo `declarationAstNodeId` não existe na AST da mesma unit. |

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
- **Evidência:** a composição aceita resolution entries ausentes do occurrence product; uma symbol table controlada com `declarationAstNodeId=Integer.MAX_VALUE` também atravessa resolução e report sem rejeição. Os dois oráculos ativáveis falham porque nenhuma exception ocorre.
- **Contrato/invariant/eval:** ADR-0003/0005; INV-SYM-001, INV-PROV-002, INV-DET-001, INV-PERF-001; EVAL-SYM-001/002, EVAL-RES-REL-001, EVAL-RES-DET-001/PERF-001.
- **Impacto:** consumidor futuro pode juntar fatos de units/produtos incompatíveis e obter readiness aparentemente válida.
- **Menor superfície provável:** validator linear cross-product em ponto de composição, complementado por checks locais onde o produto possui contexto suficiente.
- **Riscos:** espalhar validação parcial, exigir scans quadráticos ou transformar `UNRESOLVED`/`AMBIGUOUS` válidos em erro interno.
- **Alternativas:** validator separado antes do report; ampliar `compose` com AST/tables; combinação de checks locais onde houver contexto suficiente + integrador. A escolha permanece aberta.

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

### Resultado posterior do Slice 1

- A implementação registra coverage no ponto comum de materialização de `Statement`, `DataEntry`, `DataClause` e `PreservedExpression`; `SemanticCoverage.Report` rejeita dois findings concretos para o mesmo `astNodeId`.
- `DataEntry` tipada é container `MODELED + NOT_DEPENDENCY_BEARING`; SQL opaco continua preservado/unknown. `PICTURE` e `USAGE` não adicionam dependência nominal e não afirmam layout. `VALUE`, `OCCURS`, `REDEFINES` e `RENAMES` são estruturados como `MODELED`, mas permanecem `DEPENDENCY_UNKNOWN`; clauses/expressions realmente fallback continuam `PRESERVED_UNINTERPRETED + DEPENDENCY_UNKNOWN`.
- O snapshot deixou de duplicar preserved clauses, preserved expressions e unsupported statements que agora já possuem finding concreto; `RawExpression` estruturalmente ausente mantém o fallback legado.
- A implementação não alterou AST, scopes, símbolos, occurrences, resolution entries/candidates nem `ResolutionAnalysisReport`. A reconciliação e o teste repetido preservam esses produtos e CALL/FILLER.
- Essa classificação foi decisão da implementação: o discovery demonstrou o blind spot e a incoerência estrutural, mas deixou a taxonomia exata de clauses aberta.

### Evidência atual

- Focal normal: 23 testes verdes nas fronteiras, snapshot, CALL e required oracles; os dois testes de F-02 ficaram skipped.
- Gate `fast`: verde.
- Gate `semantic`: verde após revisão da taxonomia contra os relatórios existentes.
- Gate `full`: verde, incluindo E2E estruturado do normalizador e naming.
- Opt-in: `mvn -Dast.boundary.required=true -Dtest=AstSemanticBoundaryRequiredOracleTest test` executa quatro oráculos; somente os dois de F-02 permanecem vermelhos. Os dois de F-01 agora integram o gate normal.

## Discovery arquitetural do Slice 2 — F-02

### 1. Reprodução dos dois oráculos

Base reproduzida: `main` sincronizada no SHA `9aba9a897cc7f45ba7da3a25079d66aee838ba55`, merge commit do PR #12. Branch exclusiva: `discovery/work-ast-002-slice-2-cross-product-integrity`.

O comando opt-in abaixo executou quatro testes; os dois F-01 passaram e somente os dois F-02 falharam porque nenhuma exception foi lançada:

```bash
mvn -Dast.boundary.required=true \
  -Dtest=AstSemanticBoundaryRequiredOracleTest test
```

Resultado observado: 4 testes, 2 failures, 0 errors, 0 skipped. As failures foram:

- `reportFailsClosedWhenResolutionContainsOccurrenceMissingFromCollectorProduct`: `ResolutionAnalysisReport.compose(...)` aceitou resolution entries cujas occurrences foram removidas do produto do collector;
- `crossProductValidationRejectsSymbolWhoseDeclarationAstNodeDoesNotExist`: resolver e report aceitaram uma symbol table cuja primeira declaration aponta para `Integer.MAX_VALUE`, ausente da AST da mesma unit.

O frontend de ambos os cenários é válido: o helper exige zero erros de preprocessing e zero syntax errors antes de construir os produtos. A execução sem opt-in de `AstSemanticBoundaryCharacterizationTest,AstSemanticBoundaryRequiredOracleTest` ficou verde com 14 testes, 0 failures e somente os 2 F-02 skipped. `currentPipelineProductsJoinExactlyWhenProducedNormally` executou `assertActualProductsJoin(...)` sobre os produtos normais. Em cada oracle F-02, apenas um produto foi corrompido depois dessa construção válida: o map de occurrences no primeiro e `declarationAstNodeId` de um symbol no segundo.

Esses estados não são `UNRESOLVED`, `AMBIGUOUS`, gap de coverage nem COBOL parcialmente conhecido. A categoria futura é:

```text
INTERNAL PRODUCT INTEGRITY FAILURE
```

### 2. Inventário e ownership atual

| Produto | Produtor/owner atual | Checks autocontidos relevantes | Join ausente |
| --- | --- | --- | --- |
| `CompilationUnitModel` / AST | `AstBuilder` | unit ID pertence à compilation unit, IDs de unit únicos, parent precede child; pre-order AST protegido por INV-AST-003, testes e fail-closed de `AstSnapshot` | facts posteriores ainda não são reconciliados contra seus nodes/units |
| `CompilationUnitBuildResult` / coverage | `AstBuilder`, `SemanticCoverage.Report` | keys de coverage/diagnostics iguais às units; finding IDs contíguos; um finding concreto por `astNodeId` | nenhuma evidência F-02 exige que coverage seja input do validator |
| `CompilationUnitSymbolTables` | `CompilationUnitSymbolTableBuilder` | `UnitSymbols` não nulo e unit IDs sem duplicata | conjunto/parentage não é comparado ao model |
| `SymbolTable` | `SymbolTableBuilder` | scope/symbol/entity/relation IDs contíguos; parent scope precedente; symbol scope válido; entity declaration IDs válidos; relation owner válido e binding `NOT_PERFORMED` | declaration/scope/relation AST IDs não são comparados à AST da mesma unit |
| `AstScopeIndex` | `AstScopeIndex.build` | percorre a AST, rejeita node ID duplicado e mapeia nodes alcançáveis | o resultado não é reconciliado como produto por unit; scopes ancorados em AST inexistente podem ser ignorados |
| `ReferenceOccurrences` | `ReferenceOccurrenceCollector` | occurrence IDs contíguos e um occurrence por reference AST node dentro do container; campos tipados não nulos | container/unit, AST, scope e `Meta` não são verificados em conjunto |
| `ReferenceResolution` / candidates | `CobolReferenceResolver` | entry IDs contíguos; cardinalidade de `RESOLVED`, `AMBIGUOUS` e `EXTERNAL_OBSERVED` | não há bijeção com o collector nem verificação referencial dos candidates |
| `DeclarationRelationResolution` | `DataAndIndexReferenceResolver` | entry IDs contíguos | não há bijeção nem comparação de unit/kind/reference node com `DeclarationRelation` |
| `ExternalClassification` | `CicsIntrinsicClassifier` | IDs/root/covered occurrences locais, únicos e ordenados | faz coerência parcial AST/occurrence/resolution, mas retorna vazio em inconsistência; não substitui fail-closed global |
| `ResolutionAnalysisReport` | `ResolutionAnalysisReport.compose` | compõe input/coverage/binding/external gaps e readiness | trata occurrence sem resolution como gap, não detecta resolution extra e não recebe tables/scopes |
| `ResolutionSnapshot` | apresentação | serializa model, resolution e report | assume products já íntegros; não deve virar autoridade de validação |

### 3. Separação dos invariants do reconciliador de teste

#### A. Invariants já garantidos localmente

- IDs e parentage internos de `CompilationUnitModel`;
- cardinalidade e keys locais de `CompilationUnitBuildResult`/`SemanticCoverage.Report`;
- IDs contíguos e referências internas por índice em `SymbolTable` para scope de symbol, declaration IDs de entity e owner de relation;
- traversal sem duplicate AST node ID em `AstScopeIndex`;
- IDs contíguos e reference node único dentro de um `ReferenceOccurrences`;
- IDs contíguos e regras locais de cardinalidade por status em `ReferenceResolution.Entry`;
- IDs contíguos em `DeclarationRelationResolution`;
- IDs/roots/coverage localmente únicos em `ExternalClassification`.

Esses checks devem permanecer com seus produtos. F-02 não justifica movê-los para um componente global nem duplicá-los sem necessidade.

#### B. Invariants que exigem dois ou mais produtos

- igualdade dos conjuntos de `ProgramUnitId` entre model, tables, scope indexes e occurrence containers, incluindo parentage de `UnitSymbols`;
- existência, na AST da mesma unit, de AST anchors de scopes, declarations, relations e occurrences;
- scope da occurrence existente e igual ao `AstScopeIndex` produzido para seu node;
- `Occurrence.meta` igual ao `Meta` do reference AST node;
- bijeção entre occurrences coletadas e resolution entries por `(ProgramUnitId, occurrenceId)`, comparando o payload completo da occurrence;
- existência e domínio correto de cada `SemanticEntityId` candidate e de cada `declarationSymbolId` na unit do candidate;
- bijeção entre declaration relations e relation resolutions por `(ProgramUnitId, relationId)`, com `kind` e `referenceAstNodeId` idênticos;
- interpretação de todo ID local somente junto da unit e, para entities, do domínio.

#### C. Asserts de caracterização que não viram regra do validator

- zero parser/preprocessor errors da fixture;
- cardinalidades 10/14/20/1 e a taxonomia de coverage da fixture do Slice 1;
- readiness falsa por CICS/unknown, CALL literal/dinâmico e detalhes de FILLER/RENAMES específicos do cenário;
- texto escrito, canonical name, attributes e ordem de corpus como chave de join;
- JUnit `assertAll`/mensagens do helper e a estratégia de acumular failures.

O validator reutiliza o conhecimento estrutural de `assertActualProductsJoin(...)`, mas não copia o helper mecanicamente nem depende de JUnit, fixture ou texto COBOL.

### 4. Matriz cross-product completa

| Origem → destino | Identidade/index | Regra de integridade | Owner |
| --- | --- | --- | --- |
| model → AST | `ProgramUnitId`, depois `astNodeId` local | cada unit possui uma árvore alcançável; traversal canônica forma um índice sem duplicata e sem colisão entre units | AST local; validator constrói o índice-anchor |
| tables → model | `ProgramUnitId` | exatamente um `UnitSymbols` para cada unit existente; nenhuma unit extra; `parentId` igual ao model | validator |
| scope indexes → model/AST/table | `ProgramUnitId`, `astNodeId`, `scopeId` | exatamente um index por unit; todo node alcançável possui mapping; scope retornado existe na table; todo scope não-root ancorado em AST aponta para node da mesma unit | validator |
| symbols → scope/AST | `(unit, symbolId)` | `scopeId` existe; `declarationAstNodeId` existe na AST da mesma unit | scope local no construtor; AST join no validator |
| declaration entities → symbols | `(unit, entityId)` | `declarationSymbolIds` existem na mesma table | local em `SymbolTable`; usado novamente somente para reconciliar candidate |
| declaration relations → symbol/AST | `(unit, relationId)` | `ownerSymbolId` existe; `referenceAstNodeId` existe na AST da mesma unit | owner local; AST join no validator |
| occurrence container → model | map key `ProgramUnitId` | exatamente um produto por unit existente; nenhuma unit extra | validator |
| occurrence → container/AST/scope | `(unit, occurrenceId)` e `(unit, referenceAstNodeId)` | payload unit igual ao container; node existe na mesma unit; scope existe e é o mapping do node; `Meta`/provenance é igual ao node | validator |
| resolution entry → occurrence | `(ProgramUnitId, occurrenceId)` | corresponde exatamente à occurrence coletada; composite ID não duplica; não há entry extra | validator |
| occurrence → resolution entry | mesma chave composta | toda occurrence coletada possui exatamente uma entry | validator |
| candidate `DATA_SYMBOL` | `SemanticEntityId(unit, DATA_SYMBOL, localId)` | unit existe; localId referencia symbol DATA/CONDITION compatível; declaration IDs referenciam a table alvo | validator, sem considerar status |
| candidate `INDEX_SYMBOL` | `SemanticEntityId(unit, INDEX_SYMBOL, localId)` | localId referencia `INDEX_NAME`; declaration IDs existem na unit alvo | validator |
| candidate `PROCEDURE_SYMBOL` | `SemanticEntityId(unit, PROCEDURE_SYMBOL, localId)` | localId referencia section/paragraph; declaration IDs existem na unit alvo | validator |
| candidate `FILE_ENTITY` | `SemanticEntityId(unit, FILE_ENTITY, localId)` | localId referencia entity FILE; declaration IDs existem na unit alvo | validator |
| candidate `PROGRAM_UNIT` | `SemanticEntityId(targetUnit, PROGRAM_UNIT, localId)` | target unit existe; localId corresponde ao ordinal determinístico dessa unit no model; declaration IDs permanecem vazios | validator |
| relation resolution → declaration relation | `(ProgramUnitId, relationId)` | relation existe; `kind` e `referenceAstNodeId` são idênticos; composite ID não duplica; candidates são válidos | validator |
| declaration relation → relation resolution | mesma chave composta | toda relation materializada possui exatamente uma resolution entry | validator |

Candidates podem legitimamente apontar para uma unit ancestral visível diferente da unit da occurrence. A validação usa a unit do `SemanticEntityId`, não exige igualdade com a use unit e não reexecuta regras COBOL de visibilidade. `UNRESOLVED`, `AMBIGUOUS` e `UNSUPPORTED` não alteram essas regras referenciais: candidate presente sob qualquer status válido precisa existir, e status sem candidate pode continuar semanticamente normal.

### 5. Coverage e frontend

`CompilationUnitBuildResult` já falha se coverage/diagnostics não tiverem exatamente as keys do model. `SemanticCoverage.Report` já protege IDs determinísticos e duplicata de finding concreto; os oráculos promovidos do Slice 1 protegem correspondência finding/node/meta. Não apareceu evidência de que F-02 precise consumir coverage ou `FrontendState`. Missing COPY, lexer/parser error e coverage unknown pertencem ao eixo de incompletude, não ao validator cross-product.

### 6. Pontos de composição auditados

| Ponto | Produtos disponíveis | Conclusão |
| --- | --- | --- |
| `ExplorerMain`, após symbol build | model/AST + tables | cedo demais: occurrences/resolution ainda não existem |
| loop de collection em `ExplorerMain` | unit AST + table + `AstScopeIndex` + occurrence da unit | útil para checks locais, mas ainda não cobre resolution/candidates/relations globais |
| imediatamente após `CobolReferenceResolver.resolve(...)` | model/AST, todas as tables, todos os scope indexes se retidos, occurrences, resolution, candidates e relation resolution | único ponto atual que reconcilia tudo antes de qualquer consumidor; ponto recomendado |
| `CicsIntrinsicClassifier.classify(...)` | model/AST + occurrences + resolution | primeiro consumidor pós-resolution; checks parciais retornam vazio e não cobrem tables/scopes/candidates |
| `ResolutionAnalysisReport.compose(...)` | build/model/coverage + occurrences + resolution + external classification | tarde demais para proteger classifier e sem tables/scopes; owner inadequado |
| `ResolutionSnapshot.from/write` | model + resolution + report | apresentação; deve assumir validação anterior |
| testes/helpers | combinações variadas | devem chamar o validator diretamente para unit tests ou reproduzir a ordem de orquestração em integration tests |

Não há outro call site de produção de `ResolutionAnalysisReport.compose(...)`; `ExplorerMain` é o único orquestrador atual. Testes constroem produtos diretamente e não redefinem ownership de produção.

### 7. Alternativas arquiteturais

| Critério | A. Validator dedicado na orquestração | B. Validação dentro de `ResolutionAnalysisReport.compose` | C. Checks locais + integrador dedicado |
| --- | --- | --- | --- |
| ADR-0003, produtos separados | preserva | report passa a conhecer tables/scopes e mistura apresentação com integridade | preserva e mantém checks autocontidos nos owners atuais |
| ADR-0005, identidade por unit | centraliza chaves compostas | possível, mas exige ampliar muito a assinatura | centraliza somente os joins compostos |
| SRP | forte | fraco; report já compõe gaps, métricas e readiness | mais forte: produto valida a si; integrador valida relações |
| Open/Closed / CFG-dataflow futuro | validator pode ganhar overload/input explícito | cada futuro produto inflaria o report ou duplicaria validação fora dele | novos produtos adicionam joins no integrador sem contaminar os anteriores |
| Incisão em `ExplorerMain` | reter scopes + uma chamada | reter scopes/tables + ampliar chamada do report, mas ainda depois do classifier | igual a A; sem mudanças nos consumidores |
| Testabilidade independente | alta | exige construir contexto de report/coverage | alta para local e cross-product separadamente |
| Fail-closed antes do consumo | sim, se chamado logo após resolver | não protege o classifier na ordem atual | sim |
| Determinismo/O(n) | natural com índices | possível, porém alheio à responsabilidade do report | natural e auditável |
| Risco de duplicação | classifier/report podem manter checks parciais defensivos | alto quando CFG/dataflow não consumirem report | menor se o integrador for a única autoridade cross-product |
| Risco de report excessivo | nenhum | alto | nenhum |

Recomendação: **C**, materializada por um validator dedicado como em A e pelos invariants locais já existentes. Não adicionar novos checks locais no Slice 2 sem um caso que possa ser decidido por um único produto; não espalhar joins por resolver, classifier, report ou snapshot.

### 8. API proposta

API package-private inicial, sem criar aggregate ou marker type antes de existir um segundo orquestrador:

```java
final class SemanticProductIntegrityValidator {
    static void validate(
            CompilationUnitModel model,
            CompilationUnitSymbolTables symbolTables,
            Map<ResolutionContracts.ProgramUnitId, AstScopeIndex> scopeIndexesByUnit,
            Map<ResolutionContracts.ProgramUnitId, ReferenceOccurrences> occurrencesByUnit,
            ReferenceResolution resolution) {
        // fail on the first deterministic integrity violation
    }
}
```

`ReferenceResolution` já transporta `candidates` e `DeclarationRelationResolution`; duplicá-los como parâmetros permitiria combinações impossíveis adicionais. `CompilationUnitBuildResult` não entra porque coverage/frontend já têm invariants próprios e não participam dos joins F-02. O retorno é `void`: validação não cria novo produto semântico nem muta os existentes.

### 9. Ponto exato de integração

Em `ExplorerMain`, manter o `AstScopeIndex` construído para cada unit no mesmo loop que coleta occurrences. Depois:

```text
resolve(model, tables, occurrences)
→ SemanticProductIntegrityValidator.validate(...)
→ CicsIntrinsicClassifier.classify(...)
→ ResolutionAnalysisReport.compose(...)
→ ResolutionSnapshot
```

O required oracle de occurrence deve ser refinado na Fase 2 para exercitar o validator/ponto de orquestração, não para transferir ownership a `ResolutionAnalysisReport`. O requisito permanece idêntico: resolution extra é corrupção e precisa lançar antes do report.

### 10. Modelo de erro fail-closed

Estados COBOL válidos porém incompletos continuam resultados normais:

| Estado | Representação normal |
| --- | --- |
| `UNRESOLVED`, `AMBIGUOUS`, `UNSUPPORTED` | status/reason/candidates/diagnostic na resolução |
| missing COPY | `FrontendState`/gap de input e análise parcial conservadora |
| coverage unknown / preserved opaque | finding/gap de coverage |
| CALL dinâmico sem valor | gap de call semantics |

Estados internos impossíveis lançam uma única exception dedicada, sem hierarchy adicional:

```java
final class SemanticProductIntegrityException extends IllegalStateException
```

Mensagem mínima determinística:

```text
INTERNAL PRODUCT INTEGRITY FAILURE product=<product>
unit=<ProgramUnitId> domain=<domain> <idField>=<id> detail=<stable-detail>
```

Campos obrigatórios quando aplicáveis: produto, `ProgramUnitId`, domínio (`AST_NODE`, `SCOPE`, `SYMBOL`, `SEMANTIC_ENTITY`, `OCCURRENCE` ou `DECLARATION_RELATION`) e o ID local específico (`astNodeId`, `scopeId`, `symbolId`, `localId`, `occurrenceId` ou `relationId`). Não usar `writtenName`, source text ou mensagem humana como chave. Falhar na primeira violação segundo ordem model/list preserva diagnóstico determinístico e evita transformar corrupção em uma coleção de gaps recuperáveis.

### 11. Análise de complexidade

Índices auxiliares construídos uma vez:

- `ProgramUnitId → ProgramUnit` e ordinal de program unit;
- `ProgramUnitId → Map<astNodeId, Ast.Node>` por traversal canônica;
- `ProgramUnitId → SymbolTable` e acesso direto a scopes/symbols/entities por IDs contíguos;
- `(ProgramUnitId, occurrenceId) → Occurrence`;
- `(ProgramUnitId, relationId) → DeclarationRelation`;
- sets de composite identities vistos em resolution e relation resolution.

Tempo:

```text
O(units + nodes + scopes + symbols + entities + relations
  + occurrences + resolutionEntries + candidates
  + candidateDeclarationSymbolIds)
```

Espaço auxiliar: `O(units + nodes + occurrences + relations)`, ou linear no tamanho agregado. `AstScopeIndex` já existe e é reutilizado. Cada candidate usa lookup direto pela unit/domain/localId; cada resolution entry usa lookup hash da occurrence. Não há scan de nodes por symbol, symbols por candidate, occurrences por entry nem join por texto.

### 12. Superfície provável da futura implementação

- novo `src/main/java/io/github/gustavo2358/cobolexplorer/SemanticProductIntegrityValidator.java`, incluindo a única exception package-private ou uma classe package-private adjacente;
- novo `src/test/java/io/github/gustavo2358/cobolexplorer/SemanticProductIntegrityValidatorTest.java`;
- `ExplorerMain.java`: reter `scopeIndexesByUnit` e executar uma chamada antes do classifier;
- `AstSemanticBoundaryRequiredOracleTest.java`: refinar o call target e promover os dois F-02 quando verdes;
- `AstBoundaryTestSupport.java`: reutilizar o validator como oracle positivo somente depois de manter independência suficiente dos asserts de caracterização;
- `docs/domain/reference-resolution.md`, `docs/architecture/pipeline.md`, invariant/eval catalog somente se a implementação aprovada tornar o contrato durável.

Não é prevista alteração de `ResolutionAnalysisReport`, `ExternalClassification`, `ResolutionSnapshot`, AST, symbol shapes, occurrence/resolution shapes, grammar, parser ou baselines.

### 13. Matriz mínima de regressão da Fase 2

Evitar um teste por campo quando uma classe parametrizada discrimina o mesmo ownership:

1. **Happy path composto:** fixture normal; inclui `UNRESOLVED` e `AMBIGUOUS` válidos e prova que status semântico não é corrupção.
2. **Namespace de unit:** table/scope/occurrence product de unit inexistente ou ausente e occurrence cuja unit difere do container.
3. **AST/scope/declaration:** symbol declaration AST órfã, occurrence reference AST órfã, occurrence scope inválido/mismatch e relation reference AST órfã. `relation owner` inválido deve continuar sendo rejeitado pelo construtor de `SymbolTable`, demonstrando ownership local.
4. **Bijeção occurrence/resolution:** occurrence órfã, resolution extra, duplicate composite identity e payload de occurrence incompatível sob a mesma chave.
5. **Referência de candidate:** entity/localId inexistente em cada família discriminante e `declarationSymbolId` inexistente; candidate válido em `UNSUPPORTED`/`AMBIGUOUS` não é rejeitado.
6. **Bijeção de declaration relation:** relation resolution órfã, relation desaparecida e mismatch de kind/reference AST.
7. **Identidade composta:** parent/nested units com IDs locais repetidos; nenhum join cruza units e candidate ancestral válido continua aceito.
8. **Integração de ordem:** primeiro consumidor pós-resolution só executa após validação; corrupção falha antes do classifier/report/snapshot.

Os dois oráculos atuais permanecem opt-in neste Discovery. Na Fase 2, seus cenários continuam obrigatórios, mas o primeiro deve chamar o owner arquitetural recomendado em vez de tornar o report owner por acidente.

### 14. Riscos e dúvidas restantes

- **Bypass do validator:** um futuro orquestrador pode chamar consumidores diretamente. Mitigação: documentar a ordem no pipeline e testar o único ponto de produção atual; um `ValidatedProducts` só deve ser considerado quando houver segundo orquestrador real.
- **Duplicação defensiva:** classifier e report já fazem checks parciais. Eles podem permanecer para validar seus próprios inputs/outputs, mas não devem ser ampliados nem tratados como autoridade global.
- **Candidates cross-unit:** DATA/FILE/PROGRAM visíveis podem pertencer a unit ancestral/alvo. O validator valida a target unit do entity ID e não exige candidate na use unit.
- **Programa candidate:** o contrato atual usa o ordinal determinístico no model como `PROGRAM_UNIT.localId`. A Fase 2 deve proteger esse contrato sem redesenhar identidade.
- **Scope anchors:** root usa `astNodeId=-1`; somente scopes com anchor concreto participam do join AST.
- **Diagnostics de resolution:** IDs e backlinks de diagnostics são invariants internos do próprio produto, não joins solicitados por F-02. Não ampliar o Slice 2 sem um caso de corrupção/consumo que demonstre necessidade.
- **SQL/FILLER, FILLER REDEFINES, CFG/dataflow:** permanecem decisões ou produtos separados e não são desbloqueados por este Discovery.

Não resta dúvida arquitetural bloqueante para iniciar a futura implementação. Permanecem decisões de detalhe revisáveis sobre nome/visibilidade da exception e se o primeiro teste de integração exercita `ExplorerMain` ou um helper de orquestração sem I/O.

### 15. Critérios de aceite da Fase 2

- os dois oráculos F-02, refinados para o owner correto, ficam verdes no gate normal;
- produtos normais, `UNRESOLVED`, `AMBIGUOUS`, `UNSUPPORTED`, missing COPY e coverage unknown continuam resultados/gaps normais;
- toda corrupção da matriz falha com `SemanticProductIntegrityException` antes da classificação externa;
- mensagem identifica produto, unit, domínio e ID local aplicável com prefixo `INTERNAL PRODUCT INTEGRITY FAILURE`;
- joins usam somente identities compostas/estruturais, nunca written text;
- implementação é determinística e linear nas cardinalidades agregadas;
- `ResolutionAnalysisReport`, `ExplorerMain` além da chamada mínima, classifier, snapshots e shapes dos produtos não absorvem responsabilidade nova;
- testes focais, `fast`, `semantic`, `performance` e `full` ficam verdes conforme a implementação no caminho normal;
- nenhum CFG, dataflow, storage, possible value ou target dinâmico é iniciado;
- PR de implementação permanece separado deste checkpoint e depende de autorização explícita após review.
