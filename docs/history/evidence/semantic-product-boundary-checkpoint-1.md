# Semantic Product Boundary Discovery — Checkpoint 1

Data da inspeção: 2026-09-04
Work item: `WORK-SEMANTIC-PRODUCT-001`
Escopo autorizado: **Current semantic state and boundary requirements** somente. Não houve execução dos Checkpoints 2 ou 3.

Este relatório responde à pergunta: **qual conhecimento semântico o frontend COBOL realmente estabeleceu ao final da pipeline, como esse conhecimento pode ser exposto de forma coerente, rastreável e estável, e qual é a menor boundary suficiente para permitir lowering sem conhecer internals do frontend?** Ele não escolhe a forma da boundary, não define um contrato de transporte e não desenha uma IR.

## 1. Executive Summary

### Fatos estabelecidos

- A pipeline executável é mais rica que `AST → Symbols → Resolution`: ela passa por fonte física, normalização/provenance, preprocessing/COPY, ANTLR, AST, compilation units, symbol tables, occurrences, resolução nominal, classificação externa CICS focalizada, report de análise e projections de presentation.
- Não existe hoje um objeto único que contenha todo o estado semântico final. `CompilationUnitBuildResult` agrupa AST/modelo, coverage e diagnostics por unidade; `ResolutionAnalysisReport` compõe estado de frontend, occurrences, resolução, classificação e gaps; nenhum dos dois contém o conjunto completo.
- AST, símbolos, occurrences e resolução são produtos distintos, imutáveis e ligados por identidades compostas. A AST não é enriquecida por binding.
- `ExternalClassification` é um produto atual, pós-binding e ortogonal à resolução. Ele acrescenta a possibilidade classificada de intrínsecos CICS `DFHRESP`/`DFHVALUE` quando o binding COBOL não resolveu, preservando occurrences cobertas, provenance e incompletude.
- `ConditionSemantics` e `ConditionValidation` não existem em produção. São decisões/produtos futuros documentados; a AST, occurrences e resolução atuais preservam superfície e binding nominal suficientes para investigação posterior, não uma semântica de predicado já materializada.
- Incompletude é primeira classe: fatos conhecidos coexistem com `UNRESOLVED_COPY`, coverage parcial, statuses `UNRESOLVED`/`AMBIGUOUS`/`UNSUPPORTED`, dependência desconhecida, diagnostics e claim `INCOMPLETE`.
- Provenance localizada já está materializada em `Ast.Meta` e nos produtos posteriores. A necessidade de atravessar o objeto `SourceMap` inteiro ainda é `UNKNOWN`; não foi encontrada dependência downstream atual que leia esse objeto diretamente.
- A representação atual é suficiente para um futuro lowerer conhecer ordem estrutural, operands, referências nominais, candidatos/statuses, unidades, scopes, targets procedurais e provenance localizada em vários slices. Ela não prova suficiência para efeitos de controle, terminalidade, `ALTER`, validações de `SEARCH`, valores de runtime ou qualquer contrato de CFG/dataflow.

### Hipóteses avaliadas

| Hipótese | Resultado do Checkpoint 1 | Evidence status |
| --- | --- | --- |
| H1 — não existe produto final único | Confirmada: existem produtos separados e uma composição operacional no `ExplorerMain`/report, mas não um produto único. | `PROVEN` |
| H2 — produtos semânticos devem permanecer separados | Confirmada como decisão vigente para AST, símbolos, occurrences e resolução; futuros produtos devem respeitar a mesma separação até decisão posterior. | `PROVEN` |
| H3 — pode existir aggregate/envelope | Não decidido. Há uma necessidade observada de visão coerente, mas a forma — aggregate, facade, envelope ou outra — é questão do Checkpoint 2. | `PLAUSIBLE` |
| H4 — wire format não é domain contract | A implementação atual usa snapshots JavaScript como adapters de browser; isso sustenta a separação, mas não decide transporte futuro. | `STRONGLY_SUPPORTED` |
| H5 — produto continua COBOL-specific | O estado e os tipos atuais são COBOL-specific; não há evidência que justifique um produto universal neste checkpoint. | `STRONGLY_SUPPORTED` |

### Principal consequência para o review

O review do Checkpoint 2 precisa decidir quais produtos formam uma visão coerente e quais joins fazem parte de uma boundary estável. Antes disso não é possível provar se a menor boundary deve carregar produtos diretamente, oferecer consultas ou encapsular uma composição. Também não há razão comprovada para iniciar o design da IR antes de resolver essa suficiência frontend/lowerer.

## 2. Real Current Pipeline

O fluxo abaixo foi reconstruído de `ExplorerMain`, `AstBuilder`, `ReferenceOccurrenceCollector`, `CobolReferenceResolver`, `CicsIntrinsicClassifier`, `ResolutionAnalysisReport` e dos writers de snapshot.

```text
arquivo físico / String de entrada
  → SourceNormalizer.Result + SourceMap + diagnostics
  → PreprocessorEngine.Outcome + texto pré-processado + SourceMap composta + COPY state
  → lexer/parser ANTLR + parse tree + diagnostics
  → AstBuilder
  → CompilationUnitBuildResult
       ├─ CompilationUnitModel / Ast.Program(s)
       ├─ SemanticCoverage.Report por ProgramUnitId
       └─ diagnostics por ProgramUnitId
  → CompilationUnitSymbolTableBuilder
  → CompilationUnitSymbolTables / SymbolTable por unidade
  → AstScopeIndex + ReferenceOccurrenceCollector por unidade
  → ReferenceOccurrences por unidade
  → CobolReferenceResolver
  → ReferenceResolution + DeclarationRelationResolution
  → CicsIntrinsicClassifier (somente quando o frontend não tem erros estruturais)
  → ExternalClassification
  → ResolutionAnalysisReport
  → AstSnapshot / SymbolTableSnapshot / CoverageSnapshot / ResolutionSnapshot
  → arquivos JavaScript/HTML de apresentação
```

| Stage | Producer | Inputs | Output type | Ownership/lifetime | Mutability | Current consumers |
| --- | --- | --- | --- | --- | --- | --- |
| Fonte física | `ExplorerMain` | arquivo ou `String` de entrada | `String` original | variável local durante a execução | `String` imutável | normalização e provenance inicial |
| Normalização | `SourceNormalizer` | fonte física e source format | resultado de normalização, `SourceMap`, diagnostics | mantido até preprocessing/AST | value objects imutáveis | `PreprocessorEngine`, `AstBuilder`, logging |
| Preprocessing/COPY | `PreprocessorEngine` | fonte normalizada, caminhos de COPY | `PreprocessorEngine.Outcome`, texto, `SourceMap` composta, `CopyInputCompleteness`, diagnostics | mantido até AST e `FrontendState` | outcome imutável; engine é executor | parser, AST, report, classifier |
| Parsing | lexer/parser ANTLR em `ExplorerMain` | texto pré-processado | tokens, parse tree, diagnostics | árvore mantida até export e `AstBuilder` | internals ANTLR; não é produto de domínio | export do parse tree e `AstBuilder` |
| Identidade transitória do parse tree | `ExplorerMain` | parse tree/token stream | `IdentityHashMap<ParseTree,Integer>` e métricas | somente durante uma execução | mapa local, dependente de object identity | browser export, `ParseTreeOrigin` da AST |
| AST semantic surface | `AstBuilder` | parse tree, texto, `SourceMap` | `Ast.Program` em `CompilationUnitBuildResult` | mantida por todas as fases semânticas | records/listas imutáveis | compilation units, scopes, occurrences, snapshots |
| Coverage/diagnostics da AST | `AstBuilder` | contexts e AST | `SemanticCoverage.Report`, `Diagnostic` por unidade | junto de `CompilationUnitBuildResult`; report usa depois | imutável | report, coverage snapshot, testes |
| Compilation units | `AstBuilder`/`CompilationUnitModel` | AST de programas top-level/nested | `CompilationUnitModel` com `ProgramUnitId` | mantido até resolução e snapshot | imutável | symbol builder, resolver, snapshots |
| Scopes/index | `AstScopeIndex` | `Ast.Program` | índice de AST para scope | local ao loop de composição em `ExplorerMain`; retido nos testes; não entra nos resultados principais | índice derivado imutável após construção | collector, resolver e oráculos de integridade |
| Symbol tables | `CompilationUnitSymbolTableBuilder` | `CompilationUnitModel` e AST | `CompilationUnitSymbolTables`/`SymbolTable` | mantido até report/snapshot | imutável; `bindingStatus=NOT_PERFORMED` | occurrences, resolver, symbol snapshot |
| Reference occurrences | `ReferenceOccurrenceCollector` | AST e `AstScopeIndex` | `Map<ProgramUnitId, ReferenceOccurrences>` | mantido até classifier/report/snapshot | produtos imutáveis | resolver, classifier, report, snapshot |
| Nominal resolution | `CobolReferenceResolver` | model, symbol tables, occurrences, policy | `ReferenceResolution` e relações resolvidas | mantido até classifier/report/snapshot | imutável | classifier, report, resolution snapshot |
| Post-binding focalizado | `CicsIntrinsicClassifier` | model, occurrences, resolution, COPY completeness | `ExternalClassification` | mantido até report/snapshot | imutável; não muta resolução | `ResolutionAnalysisReport`, `ResolutionSnapshot` |
| Readiness/coverage composition | `ResolutionAnalysisReport.compose` | frontend state, occurrences, resolution, classification | `ResolutionAnalysisReport` com gaps, claim, completeness e métricas | criado no composition root e usado para saída | imutável | logging, snapshot, testes |
| Presentation projections | `AstSnapshot`, `SymbolTableSnapshot`, `CoverageSnapshot`, `ResolutionSnapshot` | produtos acima | records/strings de snapshot e arquivos `*.js` | vivem como arquivos de saída; não retornam ao domínio | projeções imutáveis antes de escrita | HTML/browser |

Há composição posterior explícita, mas ela ocorre no composition root e nos adapters: `ExplorerMain` produz produtos separados, chama as análises, depois passa subconjuntos diferentes aos snapshots. Nenhuma etapa encontrada calcula uma regra semântica essencial exclusivamente no HTML. O parse tree é internamente necessário para construir a AST e para a página de parse, mas não deve atravessar a futura boundary como dependência de consumer.

## 3. Final Semantic Products Inventory

| Product | Unique semantic knowledge | Required downstream? | Why | Stable enough for boundary? | Concern |
| --- | --- | --- | --- | --- | --- |
| `Ast.Program` / `Ast` | surface COBOL tipada, ordem, nesting, operands, expressões, provenance localizada e formas preservadas | Sim, para estrutura e operandos | lowering precisa saber o construct escrito e sua ordem | Parcialmente: contrato de domínio vigente e imutável | IDs são locais à unidade; certas construções são preserved/generic; `ParseTreeOrigin` é metadata de uma geração |
| `CompilationUnitModel` | inventário top-level/nested, parent, structural path e `ProgramUnitId` | Sim | sem ele não há namespace, visibilidade ou unidade de análise | Forte dentro de uma execução determinística | canonicalização/nome de arquivo e estabilidade entre versões ainda exigem decisão |
| `CompilationUnitBuildResult` | composição de model + coverage/diagnostics por unidade | Indiretamente | útil para transportar o resultado inicial completo da construção | Não como contrato final provado | não contém símbolos, occurrences ou resolução |
| `SemanticCoverage.Report` | estado de construção e conhecimento de dependência por finding, razões e completude de coverage | Sim quando o consumer precisa distinguir conhecido de incompleto | evita interpretar preservado/ausente como completo | Forte como produto de coverage separado | não é equivalente ao significado semântico completo |
| `Diagnostic` / `FrontendState` | erro de I/O, preprocessing, lexer/parser e condição de execução fail-closed | Sim | consumer precisa saber por que a visão é parcial | Forte como observabilidade; responsabilidade de domínio ainda deve ser decidida | diagnóstico top-level e gaps do report têm camadas diferentes |
| `CompilationUnitSymbolTables` / `SymbolTable` | declarations, scopes, entities, indexes e relações declarativas | Sim para binding e lowering de referências declaradas | resolução e lowerer precisam da entidade/declaration, não apenas do texto | Forte como produto separado | IDs locais; não representa valor de runtime |
| `AstScopeIndex` | associação derivada AST node → scope e lookup estrutural usado na coleta | Talvez, não necessariamente | é indispensável hoje para produzir occurrences; não foi provado necessário ao lowerer | Não como produto de boundary atual | é um índice operacional, não uma tabela semântica independente publicada |
| `ReferenceOccurrences` | superfície de cada referência, role, namespace admissível, scope, AST anchor e preservation | Sim | resolução e consumer precisam diferenciar read/write, target, selector e qualifier | Forte como produto separado | IDs locais por unidade; sem ele a resolução não pode ser interpretada com segurança |
| `ReferenceResolution` | status, reason, candidates, declaration identities, call semantics, diagnostics e relation resolution | Sim | é o binding nominal e a incerteza sobre ele | Forte no domínio atual | não dá valores de runtime, CFG, possible-values ou target final de CALL variável |
| `ExternalClassification` | classificação pós-binding de intrínseco CICS, covered occurrences, reason, provenance e COPY completeness | Condicionalmente sim | acrescenta conhecimento externo observado/inferido que não pertence ao binding COBOL | Forte no escopo focalizado atual | não é parser embedded nem catálogo geral de dependências |
| `ResolutionAnalysisReport` | joins de frontend state, coverage, occurrences, resolution, external classification, gaps, claim e readiness | Sim para diagnóstico/readiness; não como substituto automático | dá uma visão de análise com falhas explícitas | Não provado como Semantic Product | é composto e orientado a report; não contém todos os produtos fonte |
| `SourceMap` | mapa completo de transformações, segmentos e include chain | Necessidade do lowerer: `UNKNOWN` | pode responder rastreabilidade além da metadata localizada | Interno hoje | classe package-private, sem accessor público, e não é retida em `CompilationUnitBuildResult` |
| `AstSnapshot` / `SymbolTableSnapshot` / `CoverageSnapshot` | views achatadas de uma unidade, métricas e links para browser | Não como domínio | servem navegação e inspeção humana | Não | `AstSnapshot`, symbol e coverage são primary-unit only |
| `ResolutionSnapshot` | projeção full-unit de resolution, candidates, classifications, gaps, relations e provenance | Não como domínio | serve à página de resolução | Não | schema JavaScript e `window.RESOLUTION_DATA` são adapter de presentation |

Os produtos não são reconstruíveis todos a partir de um único outro produto. Em particular, `ReferenceResolution` carrega occurrences e candidates, mas não reconstitui toda a AST nem os symbol tables; `ResolutionAnalysisReport` carrega status e gaps, mas não é fonte para reconstruir declarations ou a surface; snapshots descartam ou achatam detalhes. Portanto, “required downstream” não implica ainda “campo direto” de uma futura boundary.

| Product | ANTLR/parser internals | Object identity | Determinismo observado | Reconstruibilidade a partir de outros produtos |
| --- | --- | --- | --- | --- |
| `Ast.Program` | não carrega contexts; `ParseTreeOrigin` é metadata de origem | não para joins semânticos | sim, com pre-order e mesma entrada | não a partir de symbols/resolution; é fonte estrutural |
| `CompilationUnitModel` | não | não | sim, por structural path/nome canônico na geração | parcialmente derivável do AST, mas parent/IDs são conhecimento próprio |
| `CompilationUnitBuildResult` | não, além da origem registrada no AST | não | sim como composição de resultados determinísticos | parcialmente de model + coverage + diagnostics, não de resolution |
| `SemanticCoverage.Report` | não | não | sim, findings ordenados e contíguos | pode ser recalculado do AST/build, mas não da resolution |
| `CompilationUnitSymbolTables` / `SymbolTable` | não | não | sim dentro da unidade e policy | recalculável de AST/model, não recuperável com a mesma garantia de outros produtos |
| `AstScopeIndex` | não | não | sim como índice derivado | recomputável da AST; não é produto semântico final atual |
| `ReferenceOccurrences` | não | não | sim para mesma AST/ordem | parcialmente carregado por entries de resolution, mas não substituível por elas |
| `ReferenceResolution` | não | não | sim para mesma policy/ordem | não sem rerodar resolver sobre occurrences e tables |
| `DeclarationRelationResolution` | não | não | sim dentro da resolução | não sem relations, occurrences e resolução |
| `ExternalClassification` | não | não | sim para mesma composição/policy | não a partir de resolution isolada; depende de model, occurrences e COPY state |
| `ResolutionAnalysisReport` | não | não | sim, conforme produtos de entrada | recomputável dos produtos, mas não reconstrói os produtos fonte |
| `SourceMap` | não é parser product; guarda transformação/preprocessing | não como identidade semântica | determinismo do pipeline, sem contrato de intercâmbio | não é recuperável integralmente da AST posterior |
| snapshots | podem expor `parseNodeId` para browser, sem contexto ANTLR | não para domínio; parse id transitório | sim para mesma projeção | não; são views e podem perder informação |

Essa segunda matriz distingue “recomputável” de “reconstruível”: um builder pode recalcular um produto usando fontes e policy, mas isso não significa que outro produto contenha informação suficiente para reproduzi-lo sem rerun.

## 4. Ownership and Lifetime

O ownership atual é procedural, não um aggregate explícito:

1. `ExplorerMain` cria e mantém os resultados de normalização, preprocessing, parser, AST e produtos derivados durante uma execução.
2. `AstBuilder` é o produtor da AST, coverage e diagnostics por unidade; a `CompilationUnitModel` mantém as referências imutáveis aos programas.
3. O symbol builder cria uma tabela por `ProgramUnitId`. O `AstScopeIndex` é reconstruído/retido localmente para viabilizar coleta e resolução, mas não é incluído em um resultado público de composição.
4. O collector cria occurrences por unidade e não faz lookup. O resolver consome occurrences e symbol tables e cria uma resolução imutável, incluindo relações declarativas.
5. O classifier recebe os produtos anteriores e cria uma classificação externa sem mutar AST ou resolução.
6. `ResolutionAnalysisReport.compose` faz o join operacional e materializa gaps/readiness. Esse report é depois consumido pelos snapshots e logging.
7. Os snapshots têm lifetime de adapter: tornam-se arquivos JavaScript para as páginas e não são reintroduzidos na pipeline semântica.

Não há um owner durável que retenha, em um único objeto, `Ast`, model, tables, scopes, occurrences, resolution, classification, coverage, diagnostics, provenance e report. A infraestrutura de testes confirma isso: `AstBoundaryTestSupport.Analysis` mantém todos os produtos separadamente e `composePostAstProducts` recompõe report/classifier a partir deles.

**Finding:** a execução já tem uma visão coerente implícita no composition root, mas sua ownership não é um contrato de domínio.
**Evidence status:** `PROVEN`.
**Evidence:** `ExplorerMain.java:34-236`; `AstBoundaryTestSupport.java:32-206`; `ResolutionAnalysisReport.compose`.
**Implication:** o Checkpoint 2 precisa decidir a unidade de composição sem inferir que o report ou o snapshot já sejam a boundary.
**Open question:** a menor visão coerente para lowering exige materializar todos esses produtos ou somente uma composição consultável de alguns deles?

## 5. Identity and Join Model

### Identidades encontradas

| Identity | Criação/namespace | Estabilidade observada | Dependência de traversal/object identity | Persistibilidade atual | Joins |
| --- | --- | --- | --- | --- | --- |
| `ProgramUnitId` | `AstBuilder`, por compilation unit, structural path e nome canônico | determinístico para a mesma entrada/policy/estrutura | não depende de object identity; depende da estrutura | valor serializável, mas estabilidade cross-run/versionamento não foi contratada | model, tables, occurrences, resolution, classification e snapshots |
| `Ast.Meta.id` / AST node id | `AstBuilder`, contador pre-order reiniciado por program unit | determinístico e único dentro da unidade | traversal pre-order é requisito; não usa object identity no valor | persistível somente com `ProgramUnitId` e mesma convenção; não é global | coverage, occurrences, provenance, snapshots |
| `Scope.id` | `SymbolTable`/scope builder, local à tabela | contíguo e determinístico na unidade | ordem estrutural; sem object identity no contrato | somente com `ProgramUnitId` | symbols, occurrences e AST scope joins |
| `Symbol.id` | `SymbolTable`, local à tabela | contíguo/determinístico dentro da unidade | ordem de declaração/produção | somente com `ProgramUnitId` | declaration relations e candidate payloads |
| `Entity.id` / `SemanticEntityId` | entities da tabela; ID composto por unit/domain/localId | local determinístico; identidade semântica usa composição | não depende de object identity | `SemanticEntityId` é serializável com namespace completo | resolution candidates, declarations, relations |
| `ReferenceOccurrences.Occurrence.id` | collector, local por unidade | contíguo na ordem de coleta | ordem de AST; não usa object identity | somente com `ProgramUnitId` | resolution e external classification |
| `ReferenceResolution.Entry.id` | resolver, contador global do resultado | estável apenas na geração/ordem daquela resolução | ordem de units/occurrences; não é identity persistente provada | serializável no snapshot, mas não durável por si só | snapshot e report |
| `Diagnostic.id` | produtos que diagnosticam | global/local conforme o produto; usado como referência no resultado | não há contrato de identity cross-run | serializável no contexto do resultado | resolution/report/snapshot |
| `ExternalClassification.Entry.id` | classifier, global no resultado | estável na execução/ordem | depende de traversal determinístico | serializável no snapshot, sem contrato cross-run | report/snapshot; ancora unit + AST + occurrence |
| parse node id | mapa local de `ParseTree` em `ExplorerMain` | válido somente na árvore daquela execução | explicitamente `IdentityHashMap`/object identity | não é persistível como identidade de domínio | browser e `Ast.Meta.origin`, não como join semântico |

### Diagrama real de joins

```text
CompilationUnitBuildResult
  ├─ CompilationUnitModel
  │    └─ ProgramUnitId
  │         └─ Ast.Program
  │              └─ Ast.Meta.id (local à unidade)
  │                   ├─ SemanticCoverage.Finding.astNodeId
  │                   ├─ ReferenceOccurrence.referenceAstNodeId
  │                   └─ provenance / snapshots
  ├─ coverageByProgramUnit[ProgramUnitId]
  └─ diagnosticsByProgramUnit[ProgramUnitId]

CompilationUnitModel + Ast.Program(s)
  └─ CompilationUnitSymbolTables[ProgramUnitId]
       ├─ Scope.id (local)
       ├─ Symbol.id (local)
       ├─ Entity.id (local)
       └─ DeclarationRelation.id (local)

Ast.Program + AstScopeIndex
  └─ ReferenceOccurrences[ProgramUnitId]
       └─ Occurrence.id (local)
            └─ ReferenceResolution.Entry.id (global no resultado)
                 ├─ status / reason / diagnostics
                 ├─ candidates → SemanticEntityId(ProgramUnitId, domain, localId)
                 └─ DeclarationRelationResolution(ProgramUnitId, relationId)

ReferenceResolution + occurrences + model + COPY completeness
  └─ ExternalClassification.Entry
       └─ root (ProgramUnitId, rootAstNodeId, rootOccurrenceId)
          + covered occurrence ids locais à unidade

frontend state + coverage + occurrences + resolution + classification
  └─ ResolutionAnalysisReport

primary unit → AstSnapshot / SymbolTableSnapshot / CoverageSnapshot
full model + resolution + report → ResolutionSnapshot
```

O join crítico é sempre `(ProgramUnitId, localId)`, não um ID local isolado. A resolução usa `SemanticEntityId` composto; o report usa chaves compostas de unidade e occurrence; o snapshot cria uma chave textual de unidade para o browser. O uso de `IdentityHashMap` fica restrito à correlação transitória do parse tree e não deve contaminar uma boundary semântica.

**Finding:** os IDs são determinísticos e úteis para joins dentro de uma geração, mas persistência durável e estabilidade entre versões ainda não foram provadas.
**Evidence status:** `PROVEN` para namespaces locais; `UNKNOWN` para identidade pública cross-run/cross-version.
**Evidence:** `ProgramUnitId`, `SemanticEntityId`, `SymbolTable`, `ReferenceOccurrences`, `ReferenceResolution` e `AstPreorderInvariantTest`.
**Implication:** qualquer futura boundary precisa transportar o namespace da unidade e declarar o lifetime das identidades; não pode assumir que `Entry.id` ou parse node id sejam IDs públicos.
**Open question:** qual identidade, se alguma, deve ser estabilizada/versionada para intercâmbio sem congelar detalhes de traversal?

## 6. Provenance Model

A provenance começa na fonte física e é composta através de normalização, preprocessing e expansão de COPY. `SourceMap` mantém segmentos e include frames; `SourceProvenance` materializada no `Ast.Meta` inclui localização expandida/original, exactness e include chain. Occurrences preservam a metadata do AST. Os snapshots expõem essa informação para inspeção.

| Caso | Evidência executada | Resultado atual |
| --- | --- | --- |
| source principal | `SourceProvenanceTest` e `provenance/main.cbl` | `Ast.Meta` e occurrences apontam arquivo físico, linha/span e exactness correspondente |
| COPY | `provenance/main.cbl` + `cpy/FIRST.cpy` | expansion location, original source e frame de include são preservados através da substituição |
| COPY aninhado | `cpy/FIRST.cpy` inclui `cpy/SECOND.cpy`; `SourceProvenanceTest` | include chain contém os frames aninhados; provenance continua acessível em AST e occurrences |
| COPY ausente | `PartialAnalysisMissingCopyTest` | gap/diagnostic de input é preservado e fatos independentes continuam publicados |

`AstSnapshot` já expõe `sourceFile`, `sourceLine`, include depth e exactness; `ResolutionSnapshot` inclui provenance em entries e classifications. Portanto, para rastrear um fato semântico localizado ao source, a metadata localizada é suficiente no estado atual. Não há accessor público do `SourceMap` inteiro e ele não é campo do `CompilationUnitBuildResult`.

**Conclusão:** a necessidade do `SourceMap` inteiro atravessar a boundary é `UNKNOWN`, não “necessária” nem “descartada”. Ela depende de o futuro lowerer precisar reconstruir transformações, enumerar todos os segmentos ou apenas apontar fatos para a fonte. Essa decisão pertence ao Checkpoint 2.

## 7. Incompleteness Model

O pipeline atual consegue manter fatos conhecidos e incerteza de forma simultânea, sem usar coleção vazia como prova de completude:

| Situação | Contrato real | Como aparece |
| --- | --- | --- |
| fato estruturado conhecido | `ConstructionCoverage.MODELED`, `DependencyKnowledge.REFERENCE_READY`, AST/occurrence/resolution | node tipado, finding, occurrence e binding/candidate quando aplicável |
| COPY ausente | `Diagnostic.Code.UNRESOLVED_COPY`, `CopyInputCompleteness.INCOMPLETE_UNRESOLVED_COPY`, gap de input | placeholder preservado, diagnostics, claim/readiness incompleta; facts independentes continuam |
| referência unresolved | `ReferenceResolution.Status.UNRESOLVED` e reason, frequentemente `DECLARATION_NOT_FOUND` ou `INPUT_INCOMPLETE` | entry explícito sem candidate válido e gap correspondente |
| resolução ambígua | `Status.AMBIGUOUS`, todos os candidates válidos | entry com múltiplos candidates; não escolhe um silenciosamente |
| forma unsupported | `ConstructionCoverage.UNSUPPORTED` ou `Status.UNSUPPORTED`, reason | finding/entry/gap; não é promovida a readiness |
| forma preservada mas não interpretada | `PRESERVED_UNINTERPRETED`, `PreservedExpression`, `RawExpression`, `PRESERVED_REFERENCE_CONTAINER` | texto/operandos e provenance permanecem, mas sem semântica especializada completa |
| dependência desconhecida | `DependencyKnowledge.DEPENDENCY_UNKNOWN`, gaps de call/coverage | explícita no report; não equivale a “zero dependências” |
| classificação externa inferida | `ExternalClassification` com `INFERRED`, reason, covered occurrences e COPY completeness | fato adicional separado, sem alterar resolução nominal |
| parser/preprocessor boundary conhecida | diagnostics de lexer/parser/preprocessor e `FrontendState` | classifier pode ser omitido fail-closed; report publica gaps e claim `INCOMPLETE` |

Os termos conceituais pedidos se alinham aos contratos reais assim: “unknown” é uma descrição do conhecimento ausente que aparece em `DEPENDENCY_UNKNOWN`, reasons, diagnostics ou gaps; não existe enum geral `UNKNOWN` para ser introduzido neste Discovery. “Incomplete” é claim/completude de input/coverage. “Unresolved”, “ambiguous” e “unsupported” são statuses/reasons distintos já materializados.

## 8. Presentation/Snapshot Audit

Os adapters atuais fazem joins diferentes:

- `AstSnapshot` achata a AST da **primary unit**, preserva índice pre-order, parse origin, spans/provenance e métricas.
- `SymbolTableSnapshot` publica a symbol table da primary unit, scopes, symbols, counts e diagnostics.
- `CoverageSnapshot` combina a primary AST com coverage e contagens de parser/unresolved.
- `ResolutionSnapshot` recebe o model completo, occurrences, resolution, report e external classifications; publica units, entries, candidates, relation resolutions, gaps, diagnostics e provenance.
- As páginas HTML carregam scripts separados e fazem navegação por IDs entre parse node, AST node, symbol e resolution entry. Não há resolução semântica implementada no JavaScript.

Isso revela duas necessidades reais: IDs e provenance precisam sobreviver a joins, e uma visão multi-unit não pode ser confundida com as três views primary-only. Também revela uma assimetria de apresentação já conhecida (`BACKLOG-UI-001`): resolution é full-unit, enquanto AST/symbol/coverage são primárias. Ela não prova, sozinha, perda de domínio, mas é uma questão para a futura boundary e para a UX.

Não foram encontrados cálculos semânticos novos escondidos no HTML. String formatting materializa a forma de exibição, não um contrato de domínio. `ResolutionSnapshot` é um adapter de browser; não é a boundary por ser o snapshot mais completo.

## 9. Post-Binding Products

### `ExternalClassification` — existe hoje

`ExplorerMain` chama `CicsIntrinsicClassifier` depois de `CobolReferenceResolver.resolve`. O classifier indexa occurrences por unidade/AST node, consulta a resolução e, para a forma focalizada de `DFHRESP`/`DFHVALUE` sem binding COBOL, cria uma entrada CICS `POSSIBLE_INTRINSIC` com `INFERRED`, occurrences cobertas, provenance e completeness de COPY. Binding COBOL válido tem precedência; a resolução não é mutada. `ResolutionAnalysisReport` valida a coerência dessa projeção e o `ResolutionSnapshot` a publica.

Esse produto acrescenta conhecimento pós-binding, mas é um produto separado e de escopo focalizado. Não é um parser genérico de embedded language, não representa `EXEC CICS` e não é uma extração geral de dependency facts.

### `ConditionSemantics` — futuro documentado

ADR-0012 e a documentação descrevem um produto posterior à resolução que poderia normalizar condições dependentes de binding, ancorado por AST/occurrences e com identidade própria. Não existe classe ou execução correspondente em `src/main`. A atual AST e o collector preservam a surface e os anchors nominais; isso é pré-condição útil, não prova de que a futura semântica esteja pronta.

### `ConditionValidation` — futuro documentado

É descrita como uma análise posterior, type-sensitive, dependente de `ConditionSemantics` e declarations/types. Não existe em produção. Sua ausência deixa em aberto se a boundary precisará carregar apenas facts nominais/surface ou também resultados de validação.

### Evolução

Não há hoje contrato de conjunto fechado de campos, mecanismo de extensão ou versionamento do agregado inexistente. O Checkpoint 1 somente estabelece que produtos pós-binding precisam ser compostos sem mutar produtos anteriores e que a futura responsabilidade de evolução é uma decisão do Checkpoint 2; nenhum plugin framework foi proposto ou implementado.

## 10. Lowering Requirement Slices

Os requisitos abaixo são requisitos de informação observados em casos concretos; não são desenho de IR nem de API.

### R1 — DATA binding

Em `MOVE 'X' TO WS-A` (e na fixture `resolution/data-binding.cbl`), `Ast.MoveStatement` preserva source/targets e roles. O collector produz occurrence de leitura para a origem literal quando aplicável e de escrita para `WS-A`; o resolver liga o target a um símbolo DATA via candidate/`SemanticEntityId`, com scope, unidade, declaration identity e provenance acessíveis nos produtos. Um lowerer precisará, no mínimo, da ordem/identidade da statement, roles, referência estruturada, resultado nominal e representação explícita de unresolved/ambiguous; AST isolada não contém o binding.

### R2 — CALL literal

`CALL 'XPTO'` vira `Ast.CallStatement` com `ProgramReference` de syntax `LITERAL_PROGRAM_NAME`. A resolution entry é `EXTERNAL_OBSERVED`, reason `LITERAL_EXTERNAL_PROGRAM`, sem candidate sintético; `CallSemantics` registra syntax e linkage conforme policy. O fato observado de dependência é distinto de certeza de linkagem e de target de runtime. O lowerer precisará receber o target literal observado e essa distinção, sem exigir catálogo interno para observar o nome.

### R3 — CALL variável

`CALL WS-PGM` preserva `DataReference`; a occurrence é `PROGRAM/CALL_TARGET` com admissibilidade apropriada e o binding liga `WS-PGM` a um símbolo DATA. `CallSemantics` identifica `IDENTIFIER_OR_EXPRESSION`/dynamic; `ResolutionAnalysisReport` registra que o valor do target dinâmico é desconhecido. O binding de `WS-PGM` não é o valor de runtime nem o programa chamado. Possible-values e resolução de target final permanecem futuros.

### R4 — PROCEDURE binding

`PERFORM PARA-A` e `GO TO PARA-B` preservam referências tipadas a procedures; o resolver produz candidates de `PROCEDURE_SYMBOL` com unidade/scope. `PERFORM ... THRU ...` preserva os anchors `from` e `through`; `GO TO DEPENDING ON` preserva targets e selector. Um lowerer precisará dos targets escritos, ordem, unidade e resultado nominal, mas não recebe ainda edges de execução.

### R5 — nested program

`CompilationUnitModel` inventaria top-level e nested programs, com `ProgramUnitId`, `parentId` e structural path. Há uma symbol table e scopes por unidade; resolução aplica regras de nesting/visibility/GLOBAL/COMMON/shadowing nos casos cobertos. Um lowerer precisa dessa identidade/containment e dos joins qualificados por unidade; uma AST plana ou local IDs isolados não bastam.

### R6 — condition semantics

Para `EVALUATE TRUE WHEN FLAG-ON`, a AST distingue subject booleano, branch, selector e posição; occurrences/resolution podem ligar o condition-name conforme contexto. Para `WHEN FLAG-ON AND OTHER-ON`, F-01 continua presente: a surface é preservada, mas o routing/binding atual não prova a semântica combinada corretamente. Isso é evidência de requisito — precedence/parentheses/conjunção, subject/ALSO index, ordem de branches, anchors e status por nome — e não autorização para corrigir F-01. `ConditionSemantics`/`ConditionValidation` permanecem futuros.

### R7 — control-flow structure

O frontend preserva várias formas estruturais, mas não produz CFG. A matriz completa está na seção seguinte. Para lowering, os requisitos conhecidos são ordem/nesting, forma do construct, targets e operands preservados, terminação explícita quando existir e incerteza para efeitos não modelados. O comportamento de edges, fallthrough, reaching definitions e valores não foi produzido.

## 11. Control-Flow Readiness Matrix

| Construct | Parse | Typed AST | References | Binding | Structural info for lowering | Gap |
| --- | --- | --- | --- | --- | --- | --- |
| `IF` | Sim | `Ast.IfStatement`, condition, then/else, terminator | Sim para condition e statements internos reconhecidos | Sim conforme contexto; ambiguidade/unresolved preservados | ordem, nesting e ramos escritos | sem CFG/fallthrough/efeitos |
| `EVALUATE` | Sim | `EvaluateStatement`, subjects, branches, selectors, `OTHER`, terminator | Sim para subjects/selectors reconhecidos | Sim para formas cobertas; F-01 é exceção | ordem de branches e `subjectIndex`/contexto | sem predicate semantics geral; F-01 |
| `PERFORM` | Sim | inline/procedure, `from`, `through`, controls | Sim para targets e operands reconhecidos | Sim para procedures/data refs cobertos | forma, range escrito, controles e ordem | sem expansão de range/CFG |
| `PERFORM THRU` | Sim | dois targets estruturados | Sim para `from`/`through` | Sim conforme declarations | range nominal preservado | sem resolução de sequência/CFG |
| `GO TO` | Sim | `GoToStatement` `SIMPLE` | Sim para targets | Sim para procedures cobertos | lista e ordem dos targets | sem edges |
| `GO TO DEPENDING ON` | Sim | kind `DEPENDING_ON`, targets e selector | Sim para targets/selector | Sim conforme declarations | selector e lista preservados | sem possible-values/edges |
| `ALTER` | Sim | `PreservedStatement` com texto/operands genéricos | não há shape dedicada comprovada; reconhecimento genérico depende do caminho | não há binding especializado comprovado | texto e preservation, não efeito de redirecionamento | sem modelo do efeito; `BACKLOG-CFG-002` |
| `SEARCH` | Sim | `SearchStatement`/`SearchWhen`, `ALL`, searched, varying, at-end | Sim para referências/conditions reconhecidas | Sim conforme contexto | forma, ordem de WHENs, `ALL`, clauses e body | validação de SEARCH/CFG futura |
| `NEXT SENTENCE` | Sim | `NextSentenceStatement`; sentence preserva terminator | Não há target nominal | N/A | statement e fronteiras de sentence preservados | sem regra de salto/CFG formal |
| `GOBACK` | Sim | `ModeledStatement` genérico | Não aplicável | N/A | grammar rule/text e posição | não há terminalidade tipada/dedicated |
| `EXIT PROGRAM` | Sim | `ModeledStatement` genérico | Não aplicável | N/A | grammar rule/text e posição | não há terminalidade tipada/dedicated |
| `STOP RUN` | Sim | `ModeledStatement` genérico | Não aplicável | N/A | grammar rule/text e posição | não há terminalidade tipada/dedicated |

“Parse: Sim” não equivale a modelagem suficiente. Em particular, `ALTER`, terminais genéricos e `NEXT SENTENCE` preservam superfície, mas ainda não oferecem contrato tipado de efeitos/terminalidade para um consumidor de controle. Esses gaps não são classificados como bloqueio de CFG neste checkpoint, porque o contrato do Semantic Product e do consumer CFG ainda não existe.

## 12. Embedded/Platform Surface

O parser reconhece `EXEC CICS`, `EXEC SQL` e `EXEC SQLIMS/DLI`. `AstBuilder` materializa `Ast.EmbeddedLanguageStatement` com language (`CICS`, `SQL`, `SQLIMS` ou `UNKNOWN`) e payload textual/raw; não há parser de payload nem extraction de host variables ou dependency facts. A fronteira atual é, portanto, estruturada no nível de “linguagem embedded + payload opaco”, não no significado interno do comando.

`ExternalClassification` não classifica o payload `EXEC`; classifica somente as formas CICS intrínsecas focalizadas após resolução nominal. Quando essa classificação ocorre, seus links para AST/occurrences, provenance e COPY completeness são estruturados. Para embedded payload, provenance da AST continua disponível, mas links internos e uncertainty específica ainda dependem de futuros parsers/extractors. Não foi criado parser, regex de reanálise ou plugin.

## 13. Findings

### F-SP-001 — produtos semânticos são separados e não há aggregate final

**Finding:** o estado útil final é distribuído por produtos imutáveis; a composição coerente existe operacionalmente, mas não existe como objeto de domínio único.
**Evidence status:** `PROVEN`.
**Evidence:** `ExplorerMain`, `CompilationUnitBuildResult`, `CompilationUnitSymbolTables`, `ReferenceOccurrences`, `ReferenceResolution`, `ExternalClassification`, `ResolutionAnalysisReport` e `AstBoundaryTestSupport.Analysis`.
**Downstream impact:**

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    A primeira boundary downstream ainda não foi definida; não é possível provar se a ausência de um aggregate bloqueia um Semantic Product, um lowerer ou apenas requer uma composição diferente.
  evidence:
    - src/main/java/io/github/gustavo2358/cobolexplorer/ExplorerMain.java:132-207
    - src/main/java/io/github/gustavo2358/cobolexplorer/ResolutionAnalysisReport.java
    - src/test/java/io/github/gustavo2358/cobolexplorer/AstBoundaryTestSupport.java:32-206
  reassess_when:
    - semantic-product-contract-defined
```

**Rationale:** o achado é arquitetural, não autorização para introduzir um aggregate.
**Reassess when:** contrato da boundary e matriz de suficiência do Checkpoint 2 definidos.

### F-SP-002 — joins dependem de namespaces compostos e lifetimes diferentes

**Finding:** muitos IDs são locais a `ProgramUnitId`/produto; entries de resolution/classification são IDs de uma geração. Joins corretos já usam chaves compostas, mas estabilidade pública cross-run/cross-version não está contratada.
**Evidence status:** `PROVEN` para a localidade e os joins; `UNKNOWN` para persistência durável.
**Evidence:** `ProgramUnitId`, `SemanticEntityId`, `SymbolTable`, `ReferenceOccurrences`, `ReferenceResolution`, `ResolutionSnapshot` e `AstPreorderInvariantTest`.
**Downstream impact:**

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    Sem o contrato de identidade do produto semântico e do lowerer, não se pode dizer se a estabilidade cross-run é exigida na primeira camada downstream ou se será responsabilidade de um adapter posterior.
  evidence:
    - src/main/java/io/github/gustavo2358/cobolexplorer/ResolutionContracts.java
    - src/main/java/io/github/gustavo2358/cobolexplorer/ReferenceOccurrences.java
    - src/main/java/io/github/gustavo2358/cobolexplorer/ReferenceResolution.java
  reassess_when:
    - semantic-product-identity-contract-defined
```

**Rationale:** object identity é transitória apenas na correlação do parse tree; o domínio usa IDs compostos, porém o lifetime desses IDs ainda não é uma promessa de intercâmbio.
**Reassess when:** identidade e evolução forem decisões do Checkpoint 2.

### F-SP-003 — projections de presentation não têm mesma cobertura de unidade

**Finding:** AST/symbol/coverage snapshots são primary-unit, enquanto resolution snapshot compõe todas as unidades. Isso é uma assimetria de apresentação observável.
**Evidence status:** `PROVEN`.
**Evidence:** `AstSnapshot.from`, `SymbolTableSnapshot`, `CoverageSnapshot`, `ResolutionSnapshot.from`, páginas HTML e `BACKLOG-UI-001`.
**Downstream impact:**

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    A assimetria é de apresentação hoje; sem saber se o lowerer requer uma visão multi-unit completa na mesma operação, não é possível classificá-la como falha de uma boundary semântica futura.
  evidence:
    - src/main/java/io/github/gustavo2358/cobolexplorer/AstSnapshot.java
    - src/main/java/io/github/gustavo2358/cobolexplorer/SymbolTableSnapshot.java
    - src/main/java/io/github/gustavo2358/cobolexplorer/ResolutionSnapshot.java
  reassess_when:
    - lowering-unit-scope-defined
```

**Rationale:** snapshot/HTML continuam adapters e não foram promovidos a API de domínio.
**Reassess when:** o escopo de compilation units do lowerer for decidido.

### F-SP-004 — control constructs preservados não têm efeito downstream tipado completo

**Finding:** `ALTER` é preservado genericamente; `NEXT SENTENCE` não tem target nominal; `GOBACK`, `EXIT PROGRAM` e `STOP RUN` são statements modelados genericamente, sem terminalidade dedicated; `SEARCH` ainda não tem validação/CFG.
**Evidence status:** `PROVEN`.
**Evidence:** `AstBuilder`/`Ast` e fixtures/testes de procedure, search e typed traversal; `BACKLOG-CFG-001`/`BACKLOG-CFG-002`.
**Downstream impact:**

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    O contrato do Semantic Product e do futuro consumer de controle ainda não existe; a evidência mostra lacuna de modelagem especializada, mas não permite escolher entre requisito do produto, requisito de lowering ou responsabilidade exclusiva de CFG.
  evidence:
    - src/main/java/io/github/gustavo2358/cobolexplorer/AstBuilder.java:553-602
    - src/main/java/io/github/gustavo2358/cobolexplorer/AstBuilder.java:714-745
    - src/main/java/io/github/gustavo2358/cobolexplorer/Ast.java:163-230
  reassess_when:
    - semantic-product-sufficiency-matrix-defined
    - cfg-consumer-contract-defined
```

**Rationale:** nenhum construct foi corrigido; a matriz registra o que existe e o que não foi provado.
**Reassess when:** Checkpoint 2 definir a suficiência e os limites do consumer.

### F-SP-005 — composição de futuros produtos pós-binding não está definida

**Finding:** `ExternalClassification` já é pós-binding; `ConditionSemantics` e `ConditionValidation` são futuros e não há regra de evolução/extension/versionamento para produtos pós-binding.
**Evidence status:** `PROVEN` quanto ao estado atual/futuro documentado.
**Evidence:** `CicsIntrinsicClassifier`, `ExternalClassification`, ADR-0011, ADR-0012, `semantic-ast.md`, `reference-resolution.md`; busca de produção sem classes `ConditionSemantics`/`ConditionValidation`.
**Downstream impact:**

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    A relação futura entre produtos pós-binding e a boundary ainda não foi decidida; classificar a ausência de versionamento como bloqueio anteciparia o contrato que este checkpoint deve apenas investigar.
  evidence:
    - src/main/java/io/github/gustavo2358/cobolexplorer/CicsIntrinsicClassifier.java
    - docs/architecture/decisions/0012-contextual-conditions-use-post-binding-projection.md
    - docs/domain/reference-resolution.md
  reassess_when:
    - post-binding-product-policy-defined
```

**Rationale:** o resultado atual exige composição separada, mas não determina se um futuro produto será campo, consulta ou extensão.
**Reassess when:** Checkpoint 2 decidir responsabilidades de evolução.

### F-01 — condição combinada em `EVALUATE TRUE`

**Finding:** a condição `FLAG-ON AND OTHER-ON` continua com o problema documentado de routing/binding; o Discovery não o corrige nem o transforma em contrato.
**Evidence status:** `PROVEN` como comportamento/lacuna existente; consequência downstream ainda não determinada.
**Evidence:** `ConditionNameSurfaceDiscoveryTest`, `ContextualConditionOccurrenceDiscoveryTest`, `evaluate-condition-names.cbl`, ADR-0012 e `BACKLOG-RES-003`.
**Downstream impact:**

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    O contrato do Semantic Product ainda não existe; a primeira camada downstream afetada não pode ser provada neste checkpoint e o finding deve permanecer no estado canônico UNASSESSED.
  evidence:
    - src/test/java/io/github/gustavo2358/cobolexplorer/ConditionNameSurfaceDiscoveryTest.java
    - src/test/java/io/github/gustavo2358/cobolexplorer/ContextualConditionOccurrenceDiscoveryTest.java
    - docs/engineering/downstream-impact-classification.md
  reassess_when:
    - semantic-product-contract-defined
    - ir-requirements-defined
```

**Rationale:** preservar a surface e explicitar a lacuna é o resultado autorizado; não há correção oportunista neste work item.
**Reassess when:** contrato de produto e requisitos downstream forem revisados.

## 14. Unknowns That Must Be Resolved in Checkpoint 2

1. Qual é a menor visão coerente: AST + symbols + occurrences + resolution, ou uma seleção adicional de coverage, diagnostics, provenance e pós-binding?
2. A boundary deve expor produtos diretamente, consultas sobre eles ou uma composição encapsulada? Record, facade e envelope são alternativas a comparar, não decisões atuais.
3. Quais invariantes tornam um aggregate/visão semanticamente coerente sem mutar os produtos separados?
4. O conjunto de campos é fechado? Como evolui quando surgirem `ConditionSemantics`, `ConditionValidation` e outras análises pós-binding?
5. Qual é a responsabilidade de versionamento: produto em memória, identidade, adapter de snapshot ou todos?
6. `ProgramUnitId`/IDs locais são suficientes com namespace, ou é necessária identidade estável além da execução?
7. `Ast.Meta.id`, parse origin e `ReferenceResolution.Entry.id` devem cruzar a boundary, ou apenas anchors semânticos com unit namespace?
8. O lowerer precisa de `AstScopeIndex` ou somente do resultado semântico produzido por ele?
9. Provenance localizada basta para todos os consumidores? Em que cenário o `SourceMap` inteiro seria necessário?
10. Como representar, na boundary escolhida, preservado, unsupported, unresolved, ambiguous, external inferred e input incomplete sem colapsá-los?
11. Qual o escopo de compilation units e visibilidade esperado por lowering, especialmente nested programs e cross-unit CALL literal?
12. `ExternalClassification` deve acompanhar a visão sempre, ser um produto opcional ou ser consultado separadamente?
13. Que informações de condição são responsabilidade do produto semântico e quais pertencem à futura análise de condições?
14. Qual é a suficiência mínima para `ALTER`, `SEARCH`, `NEXT SENTENCE` e statements terminais antes de qualquer CFG?
15. Quais fatos de control-flow precisam cruzar o lowerer sem virar CFG antecipado?
16. Quais informações embedded podem cruzar como payload opaco, e quais exigem um futuro produto separado?
17. Como evitar que snapshot/HTML ou um wire format sejam confundidos com o contrato de domínio?
18. Existe algum consumer real que justifique iniciar a IR antes de a boundary COBOL e os requisitos de lowering serem definidos? A evidência deste checkpoint não encontrou um.

## 15. Checkpoint 2 Decision Inputs

O review humano deverá decidir, com base neste relatório:

- uma boundary candidata e seus limites explícitos;
- a comparação record/facade/envelope ou alternativa equivalente, sem assumir que o transporte define o domínio;
- o que cruza e o que permanece em produtos internos, parser internals, presentation ou adapters;
- responsabilidades de versionamento/evolução e tratamento de futuros produtos pós-binding;
- a matriz de suficiência semântica para R1–R7, incluindo nested units, provenance, incomplete analysis e control constructs;
- a reavaliação dos findings e de F-01 usando a taxonomia downstream somente depois que o contrato permitir provar a primeira camada quebrada;
- a seam de Clean Architecture e os ADRs/invariantes necessários;
- o critério de saída para autorizar o Checkpoint 3.

O Checkpoint 2 deverá terminar em novo review humano. Nenhuma dessas decisões foi tomada neste relatório.

## 16. Respostas explícitas às perguntas centrais

1. **Último estado semântico útil atual:** `ResolutionAnalysisReport` após `ReferenceResolution` e `ExternalClassification`, junto de AST/model, symbol tables, occurrences, coverage, diagnostics e provenance; o report sozinho não contém tudo.
2. **Objeto único atual:** não. `CompilationUnitBuildResult` e `ResolutionAnalysisReport` são os agrupamentos mais próximos, com responsabilidades diferentes.
3. **Produtos para visão coerente:** no mínimo AST/compilation units, symbol tables/scopes, occurrences e resolution; coverage, diagnostics, provenance e classification entram conforme o caso consumidor. A composição exata é unknown.
4. **Domínio versus presentation:** AST, model, coverage, diagnostics, symbols, occurrences, resolution e `ExternalClassification` são produtos semânticos/analíticos; `AstSnapshot`, `SymbolTableSnapshot`, `CoverageSnapshot`, `ResolutionSnapshot`, JavaScript e HTML são presentation adapters.
5. **AST isolada:** não é suficiente para binding, candidate/status, scopes, external classification ou incompletude; é necessária para a surface estrutural.
6. **Joins indispensáveis:** `ProgramUnitId` + AST node id para coverage/occurrences; unit + local IDs para scopes/symbols/entities/relations; occurrence para resolution; candidate `SemanticEntityId`; unit/AST/occurrence para classification; provenance via `Ast.Meta`.
7. **IDs persistíveis/determinísticos:** são determinísticos dentro da mesma execução/entrada e persistíveis como valores compostos; estabilidade cross-run/cross-version como contrato público é `UNKNOWN`.
8. **Provenance materializada:** source físico, locations expandida/original, span/exactness e include chain, inclusive nested COPY, em `Ast.Meta` e projections posteriores.
9. **SourceMap inteiro:** `UNKNOWN`; não parece necessário para rastreamento localizado já provado, mas não se conhece ainda o requisito de transformação completo do lowerer.
10. **Incompletude:** diagnostics, coverage/dependency knowledge, resolution statuses/reasons, COPY completeness, report gaps, claim e readiness explícitos; facts conhecidos não são apagados.
11. **Acréscimo de `ExternalClassification`:** classificação CICS pós-binding, inferida e rastreável para `DFHRESP`/`DFHVALUE`, sem mutar binding e sem representar runtime target.
12. **Pressão futura de `ConditionSemantics`/`ConditionValidation`:** exigem composição pós-binding separada, anchors/identidade e evolução; hoje só surface/contexto/nominal binding existem.
13. **Control-flow preservado:** IF, EVALUATE, PERFORM, PERFORM THRU, GO TO e GO TO DEPENDING ON têm formas e referências estruturadas; SEARCH é typed; demais têm preservação parcial/genericidade.
14. **Gaps:** ALTER, efeitos/terminalidade, NEXT SENTENCE jump, SEARCH validation, condition semantics combinada, CFG edges, possible-values e dynamic target values.
15. **Gaps ainda UNASSESSED:** F-01 e findings de identidade/composição/control-flow, pois a boundary e o consumer downstream não foram definidos.
16. **Informação conhecida necessária ao lowerer:** forma/order/nesting, operands e roles, unit/scope/declaration identity, nominal binding/status/candidates, procedure/program targets, explicit incompleteness, post-binding facts quando relevantes e provenance localizada.
17. **Perguntas antes de escolher forma:** unidade de composição, invariantes, ownership/lifetime, identity/versioning, cross-unit scope, future post-binding, SourceMap, incomplete model, presentation/transport separation e suficiência por slice.
18. **Razão comprovada para iniciar IR antes do Checkpoint 2:** nenhuma. O estado atual só permite levantar requisitos de lowering; não prova boundary COBOL suficiente nem contrato de interchange.

## 17. Checkpoint 1 self-validation

- **Evidence completeness:** conclusões de ausência, suficiência e estabilidade foram marcadas como `UNKNOWN`, `PLAUSIBLE` ou `UNASSESSED` quando não havia contrato/evidência suficiente; fontes são código, testes, fixtures e documentação canônica.
- **Boundary contamination:** JSON, record, facade, envelope, IR e CFG aparecem apenas como alternativas, trabalhos futuros ou perguntas a decidir; nenhum foi implementado ou adotado como contrato.
- **Current versus future:** `ExternalClassification` está separado dos futuros `ConditionSemantics`/`ConditionValidation`; CFG, dataflow e dependency facts permanecem futuros.
- **Impact classification:** todos os findings novos/reavaliados usam classes canônicas; a ausência de contrato mantém a primeira camada downstream como `UNASSESSED`, inclusive F-01.
- **Presentation leak:** snapshots/HTML foram tratados como adapters e evidência de joins, não como domain API.
- **Runtime-value leak:** `CALL WS-PGM` foi tratado como binding nominal DATA + target dinâmico desconhecido; nenhum valor de runtime foi inferido.
- **Scope:** nenhuma alteração em `src/main/**`, grammar, AST, symbols, occurrences, resolver, lowerer, IR, CFG ou dataflow.
- **Lifecycle:** o work item ativo tem os cinco arquivos obrigatórios, está indexado em `docs/work/index.md`, e o histórico/evidence report usa a família canônica `docs/history/evidence/`.
