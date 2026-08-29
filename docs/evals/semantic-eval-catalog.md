# Catálogo de evals semânticos

Este catálogo dá IDs estáveis às capacidades críticas do harness. Ele não duplica asserts: o oracle executável continua sendo o teste, fixture, manifesto ou script indicado. Um eval só prova o que sua coluna **Contrato observado** declara.

## Convenções

- **Tipo:** `contract`, `semantic`, `adversarial`, `regression`, `property`, `determinism`, `scale` ou `e2e`.
- **Tier:** `fast` para estrutura/documentação; `semantic` para a suíte Maven; `full` para corpus/scripts; `performance` para propriedades algorítmicas sem limite de tempo dependente de hardware.
- **Fixtures:** `synthetic` identifica fontes construídas no teste; caminhos identificam regressões versionadas.
- **Rejeita implementação ingênua:** explicita o contraexemplo protegido, sem reproduzir a expectativa detalhada do teste.
- Os nomes de classes abaixo referem-se a `src/test/java/io/github/gustavo2358/cobolexplorer/`, salvo indicação contrária.

## Source format, preprocessing e provenance

| ID | Tipo / tier | Contrato observado | Oracle executável e fixtures | Regras relacionadas | Rejeita implementação ingênua |
| --- | --- | --- | --- | --- | --- |
| EVAL-SRC-001 | contract / semantic | Registros físicos, margens, caracteres e terminações seguem a policy explícita. | `SourceNormalizerTest`; synthetic e `src/test/resources/cobol/source-format/` | source format; INV-AST-002, INV-PROV-002 | Truncar ou aceitar entrada inválida silenciosamente. |
| EVAL-SRC-002 | adversarial / semantic | Indicators, comentários e continuations preservam a semântica fixed-format. | `SourceNormalizerTest`; synthetic e fixtures de source format | source format; ADR-0001; INV-COV-002 | Tratar linhas isoladamente ou inserir linhas artificiais. |
| EVAL-PRE-001 | contract / semantic | Diretivas e alternativas do preprocessor têm classificação exaustiva e falham fechadas. | `PreprocessorEnginePolicyTest`; `page-directives.cbl` | preprocessing; INV-COV-001, INV-COV-002 | Ignorar regra nova ou `REPLACE` não suportado. |
| EVAL-PRE-002 | adversarial / semantic | COPY aninhado compõe normalização e reporta ausência, ciclo ou fonte ilegível. | `SourceNormalizationPreprocessingIntegrationTest`; `source-format-integration/` | preprocessing e source format; INV-PROV-001 | Expandir texto sem preservar as fronteiras do include. |
| EVAL-PROV-001 | semantic / semantic | Source map atravessa COPY/REPLACING e chega à AST com origem, cadeia e exatidão observáveis. | `SourceProvenanceTest`; `provenance/` | provenance; INV-PROV-001, INV-PROV-002 | Recriar mapa identidade depois de transformar o texto. |
| EVAL-PROV-002 | e2e / full | O corpus COACTUPC mantém baseline normalizado e semântico versionado. | `scripts/source-normalizer-regression.sh`; `coactupc-*-baseline.txt` e corpus configurado pelo script | source format, preprocessing e provenance | Validar apenas exemplos sintéticos e perder regressões do corpus. |

## AST e cobertura

| ID | Tipo / tier | Contrato observado | Oracle executável e fixtures | Regras relacionadas | Rejeita implementação ingênua |
| --- | --- | --- | --- | --- | --- |
| EVAL-AST-001 | semantic / semantic | AST nasce da parse tree, possui metadados rastreáveis e não incorpora produtos posteriores; declaradores e categorias de expressão usam contextos tipados. | `AstBuilderTest`, `AstBuilderTypedTraversalTest`, `AstBuildCoverageTest`, `SourceProvenanceTest` | semantic AST; INV-AST-001, INV-AST-002, INV-PROV-002 | Reparsear texto achatado, selecionar referência descendente como declarador, classificar relação pela grafia ou anexar binding à AST. |
| EVAL-AST-002 | adversarial / semantic | Expressões e referências qualificadas/subscritas permanecem estruturadas. | `StructuredExpressionAstTest`; `semantic/expressions.cbl`, `semantic/references.cbl` | semantic AST; INV-AST-002 | Guardar somente o texto ou nome terminal. |
| EVAL-AST-003 | semantic / semantic | Statements suportados e preservados mantêm tipo, estrutura e coverage explícitos. | `StatementModelAstTest`, `SemanticCoverageTest`; `semantic/statements.cbl` | semantic AST; INV-COV-001, INV-EMB-001 | Interpretar fallback como statement sem efeito. |
| EVAL-AST-004 | contract / semantic | Referências nominais e declarações são inventariadas sem binding nem valores de runtime. | `NominalReferenceAstTest`, `DeclarationModelAstTest`; fixtures `semantic/` | semantic AST e symbol model; INV-AST-001, INV-SYM-001, INV-RES-002 | Resolver nomes durante a construção da AST. |
| EVAL-COV-001 | contract / semantic | As 628 regras do parser e 50 alternativas de statement possuem classificação explícita. | `GrammarCoverageManifestTest` | semantic AST; INV-COV-001, INV-COV-002 | Cobertura por lista parcial que não falha quando a gramática cresce. |
| EVAL-COV-002 | regression / semantic | Coverage conservadora e snapshot tornam incompletude uma saída estável. | `SemanticCoverageTest`, `CoverageSnapshotTest`, `AstBuildCoverageTest` | semantic AST; INV-COV-001 | Contabilizar construção opaca como plenamente compreendida. |

## Compilation units, símbolos e ocorrências

| ID | Tipo / tier | Contrato observado | Oracle executável e fixtures | Regras relacionadas | Rejeita implementação ingênua |
| --- | --- | --- | --- | --- | --- |
| EVAL-UNIT-001 | contract / semantic | Top-level e nested programs têm containment, IDs e ordem determinísticos. | `CompilationUnitModelTest`; `baseline-compilation-unit.cbl` | compilation units; INV-DET-001, INV-PERF-001 | Tratar um arquivo como uma única unidade implícita. |
| EVAL-SYM-001 | semantic / semantic | Declarações são indexadas por unit, scope e namespace sem executar binding. | `SymbolTableBuilderTest`; `semantic/declarations.cbl` | symbol model; INV-SYM-001, INV-DET-001 | Usar uma tabela global por nome ou escolher duplicata. |
| EVAL-SYM-002 | adversarial / semantic | Entidades e occurrences preservam papel semântico, origem e estado não resolvido. | `EntityScopeAndOccurrenceTest`; `entities-and-occurrences.cbl` | symbol model; INV-SYM-001, INV-RES-001 | Confundir occurrence com declaração ou target escolhido. |

## Resolução nominal e CALL

| ID | Tipo / tier | Contrato observado | Oracle executável e fixtures | Regras relacionadas | Rejeita implementação ingênua |
| --- | --- | --- | --- | --- | --- |
| EVAL-RES-DATA-001 | adversarial / semantic | DATA/INDEX distinguem resolved, ambiguous, unresolved e namespace incorreto; FILLER não cria candidato pelo nome de uma cláusula, index-name permanece admissível em relações reconhecidas e `SET condition-name TO TRUE/FALSE` usa somente CONDITION. | `DataAndIndexReferenceResolverTest`; `data-binding.cbl`, `filler-redefines-owner.cbl`, `subscript-semantic-kind.cbl`, `index-name-relational-operators.cbl`, `set-condition-name.cbl` | reference resolution; INV-RES-001 | Criar símbolo de referência descendente, escolher o primeiro candidato, inferir namespace relacional por substring ou tratar todo SET como DATA/INDEX/CONDITION. |
| EVAL-RES-DATA-002 | adversarial / semantic | Qualification STANDARD/EXTEND/UNSPECIFIED aplica hierarquia e policy explícitas. | `DataAndIndexReferenceResolverTest`; fixtures de qualifier/collision | reference resolution; INV-RES-001 | Comparar apenas nome terminal ou aceitar qualifier parcial sempre. |
| EVAL-RES-DATA-003 | adversarial / semantic | GLOBAL, shadowing e visibilidade nested respeitam unit e caminho estrutural. | `DataAndIndexReferenceResolverTest`; fixtures `global-*`, `nested-*` | reference resolution e compilation units; INV-DET-001 | Tornar toda declaração ancestral visível. |
| EVAL-RES-REL-001 | adversarial / semantic | REDEFINES e RENAMES resolvem por relação estrutural e nível válido. | `DataAndIndexReferenceResolverTest`; fixtures `redefines-*`, `renames-*` | reference resolution e symbol model; INV-RES-001 | Fazer lookup nominal global da relação. |
| EVAL-RES-PROC-001 | semantic / semantic | PROCEDURE resolve parágrafos/seções com qualification e ambiguidade explícitas. | `ProcedureFileProgramReferenceResolverTest`; `procedure-binding.cbl` | reference resolution; INV-RES-001 | Colapsar namespaces ou selecionar seção por proximidade. |
| EVAL-RES-FILE-001 | adversarial / semantic | FILE respeita namespace, FD, GLOBAL e shadowing nested. | `ProcedureFileProgramReferenceResolverTest`; fixtures `file-*`, `nested-global-file.cbl` | reference resolution e symbol model; INV-RES-001 | Tratar FD como DATA comum. |
| EVAL-RES-PROG-001 | adversarial / semantic | PROGRAM distingue nested, COMMON, sibling e dependência literal externa observada dentro do limite do artefato. | `ProcedureFileProgramReferenceResolverTest`; fixtures `program-binding.cbl`, `common-program-visibility.cbl` | reference resolution; INV-RES-003 | Procurar qualquer PROGRAM-ID do arquivo, fabricar candidato externo ou buscar a codebase. |
| EVAL-RES-PROG-002 | contract / semantic | Canonicalização respeita PGMNAME e preserva incerteza quando a opção não basta. | `ProgramNameCanonicalizerTest`, `ProcedureFileProgramReferenceResolverTest`; fixtures `*canonicalization.cbl`, `program-name-policy.cbl` | reference resolution; INV-COV-001 | Aplicar uppercase/truncation universais. |
| EVAL-RES-CALL-001 | adversarial / semantic | Sintaxe do target e linkage DYNAM/NODYNAM/DLL são dimensões separadas. | `CallSemanticsTest`; fixtures `call-linkage-*.cbl`, `invalid-dynam-dll-call.cbl` | reference resolution; INV-RES-002 | Inferir target dinâmico apenas da opção de linkage. |
| EVAL-RES-CALL-002 | regression / semantic | CALL por identifier resolve a variável, não seu valor final; CBSTM03D permanece fronteira de dataflow. | `DynamicCallVariantTest`, `CallSemanticsTest`; `call-identifier-runtime-target.cbl` e corpus CBSTM03D | reference resolution; INV-RES-002 | Converter binding nominal da variável em programa chamado. |
| EVAL-RES-COV-001 | contract / semantic | Toda regra relevante à resolução possui classificação conservadora no manifesto. | `ReferenceResolutionManifestTest` | reference resolution; INV-COV-001, INV-COV-002 | Declarar completude por ausência de occurrence. |
| EVAL-RES-REPORT-001 | semantic / semantic | Relatório conserva estados de binding e observação externa, gaps de input, readiness e métricas por espécie semântica. | `ResolutionAnalysisReportTest`, `ResolutionSnapshotTest`; `coverage-states.cbl` | reference resolution; INV-COV-001, INV-RES-003 | Somar dependência literal observada a unresolved ou omitir premissa ausente. |
| EVAL-RES-DET-001 | determinism / semantic | Execuções repetidas, paralelas e por unit preservam IDs, ordem e isolamento. | `ResolutionAnalysisReportTest`, `ResolutionSnapshotTest` | compilation units e reference resolution; INV-DET-001 | Compartilhar estado mutável entre análises ou depender de hash order. |
| EVAL-RES-PERF-001 | scale / performance | Crescimento aumenta índices/lookups/candidatos pelas cardinalidades observáveis, sem scan global por referência. | `ResolutionAnalysisReportTest`, `DataAndIndexReferenceResolverTest` | symbol model e reference resolution; INV-PERF-001 | Resolver cada uso varrendo todas as declarações. |

## Observabilidade e apresentação

| ID | Tipo / tier | Contrato observado | Oracle executável e fixtures | Regras relacionadas | Rejeita implementação ingênua |
| --- | --- | --- | --- | --- | --- |
| EVAL-OBS-001 | contract / semantic | Configuração, níveis e MDC seguem o contrato operacional. | `LoggingInfrastructureTest`, `ExplorerMainLoggingTest` | observability policy | Logging global sem identidade de análise. |
| EVAL-OBS-002 | adversarial / semantic | Falha e lifecycle registram fronteira útil sem alterar o resultado. | `ExplorerMainLoggingTest`, `FrontendLoggingTest` | observability policy | Capturar erro sem contexto ou imprimir payload irrestrito. |
| EVAL-OBS-003 | property / semantic | Instrumentação das fases semânticas preserva AST, símbolos e resolução. | `SemanticModelLoggingTest`, `ResolutionLoggingTest` | observability e pipeline; INV-DET-001 | Fazer o logger participar da decisão semântica. |
| EVAL-UI-001 | regression / semantic | Snapshots materializam produtos separados e estados de coverage/resolution de forma determinística. | `CoverageSnapshotTest`, `ResolutionSnapshotTest` | pipeline; INV-AST-001, INV-COV-001, INV-DET-001 | Apresentação recomputar ou completar fatos ausentes. |

## Fronteiras arquiteturais

| ID | Tipo / tier | Contrato observado | Oracle executável e fixtures | Regras relacionadas | Rejeita implementação ingênua |
| --- | --- | --- | --- | --- | --- |
| EVAL-ARCH-001 | contract / fast | Produtos iniciais não dependem diretamente de símbolos, resolução ou apresentação; símbolo não depende do parser/resolver. | `ArchitectureBoundaryTest`; bytecode compilado | ADR-0003; INV-AST-001, INV-SYM-001 | Acrescentar uma dependência reversa no package único e escondê-la atrás de import ou helper. |

## Força dos oráculos

| ID | Tipo / tier | Contrato observado | Oracle executável e fixtures | Regras relacionadas | Rejeita implementação ingênua |
| --- | --- | --- | --- | --- | --- |
| EVAL-MUT-001 | adversarial / semantic | Os oráculos de DATA anônima e categoria relacional detectam mutações nas decisões estruturais que protegem. | perfil Maven `mutation-adversarial`; `AstBuilderTypedTraversalTest`, `StructuredExpressionAstTest`, `DataAndIndexReferenceResolverTest` | semantic testing; INV-AST-002, INV-RES-001 | Inverter detecção de FILLER/relação ou remover a visita relacional sem falha observável. |

## Lacunas deliberadamente fora do catálogo executável atual

Os itens `BACKLOG-EVAL-001` a `BACKLOG-EVAL-003` e `BACKLOG-PERF-001` registram famílias ainda incompletas: propriedades metamórficas amplas, modelo diferencial, extensão do mutation testing focalizado para outros contratos e escala das fases anteriores à resolução. A cobertura de `EVAL-MUT-001` não é generalizada para todo o pipeline.
