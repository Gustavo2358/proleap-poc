# Inventário de fontes — migração Harness v1

Status: transitório. Este arquivo pertence somente à arqueologia da migração e
não é uma fonte normativa do produto.

## Método e corte

Inventário produzido na branch `codex/harness-engineering-v1`, criada a partir
de `main` em 2026-08-28. O repositório estava limpo no início da fase. A base
foi o conjunto versionado (`git ls-files`), complementado por inspeção de
scripts, fontes, testes, fixtures, manifestos e histórico recente.

Há 239 arquivos versionados neste corte. A classificação de estado abaixo é
inferida; ela não promove documentos históricos a contrato atual.

## Fontes documentais e de planejamento

| Caminho | Tipo | Estado inferido | Responsabilidade aproximada | Conhecimento durável | Trabalho aberto |
| --- | --- | --- | --- | --- | --- |
| `README.md` | visão do produto e contratos mistos | ativo | execução, pipeline, source format, AST, símbolos, resolução e logging | sim | não |
| `specs/HARNESS_ENGINEERING_IMPLEMENTATION_PLAN.md` | plano-base | ativo durante a migração | arquitetura do Harness v1 e fases | sim | sim |
| `specs/AGENTS.md — antlr-parse-tree-explorer.md` | conhecimento importado | temporário | políticas de engenharia semântica, testes, desempenho e boundaries | sim | não |
| `specs/cobol-reference-resolution-policy.md` | política de domínio | ativo | dialeto e regras atuais de resolução | sim | não |
| `specs/adr/0001-comment-entry-normalization.md` | ADR | ativo | normalização fechada de comment entry | sim | não |
| `specs/source-normalizer-hardening-plan.md` | plano concluído | histórico | source format, normalização e provenance | sim | não |
| `specs/semantic-model-hardening-tasklist.md` | tasklist concluída | histórico | AST, coverage, provenance e modelo semântico | sim | não |
| `specs/reference-resolution-tasklist.md` | tasklist concluída | histórico | unidades, símbolos, ocorrências, resolução e cobertura | sim | não |
| `specs/reference-resolution-semantic-correctness-hardening-II-tasklist.txt` | tasklist concluída | histórico | hardening semântico de resolução | sim | não |
| `specs/reference-resolution-semantic-correctness-hardening-III-tasklist.txt` | tasklist concluída | histórico | CALL externo e hardening de resolução | sim | não |
| `specs/antlr-parse-tree-explorer-logging-tasklist.md` | tasklist concluída | histórico | logging e observabilidade | sim | não |
| `specs/cbstm03d-dynamic-calls-tasklist.md` | tasklist concluída | histórico | fixture de CALL dinâmico | sim | não |
| `specs/project-naming-cleanup-tasklist.md` | tasklist concluída | histórico | identidade, nomenclatura e guardas das gramáticas | sim | não |
| `specs/reference-resolution-regression-report.md` | relatório | histórico | baseline e regressão da resolução | sim | não |
| `specs/reference-resolution-semantic-correctness-report.md` | relatório | histórico | contraexemplos, correções e limites de resolução | sim | não |
| `specs/semantic-model-hardening-regression-report.md` | relatório | histórico | regressão do modelo semântico | sim | não |
| `specs/semantic-interpretation-backlog.md` | backlog | ativo | trabalho futuro de CFG, dataflow, linguagens embarcadas e operação | sim | sim |

## Fontes executáveis e evidência

| Família | Caminhos inventariados | Papel | Estado inferido |
| --- | --- | --- | --- |
| Gramáticas | `src/main/antlr4/Cobol.g4`, `src/main/antlr4/CobolPreprocessor.g4` | fronteira sintática do frontend; atribuições preservadas | ativo/protegido |
| Manifesto de coverage | `src/main/resources/semantic-coverage/grammar-rule-manifest.tsv`; `GrammarCoverageManifest.java`; `ReferenceResolutionManifest.java` | superfície gramatical classificada e guardas de completude | ativo |
| Pipeline e proveniência | `SourceNormalizer.java`, `SourceMap.java`, `PreprocessorEngine.java`, `CopybookLibrary.java`, `AntlrDiagnosticListener.java`, `ExplorerMain.java` | normalização, preprocessing, diagnóstico e orquestração | ativo |
| AST e snapshots | `Ast.java`, `AstBuilder.java`, `AstBuildResult.java`, `AstSnapshot.java`, `SemanticCoverage.java`, `CoverageSnapshot.java` | estrutura semântica, coverage e exportação | ativo |
| Unidades e símbolos | `CompilationUnit*.java`, `SymbolTable*.java`, `AstScopeIndex.java`, `DeclarationRelationResolution.java` | unidades de análise, escopos, entidades e relações | ativo |
| Ocorrências e resolução | `ReferenceOccurrenceCollector.java`, `ReferenceOccurrences.java`, `ReferenceResolution*.java`, `CobolReferenceResolver.java`, `DataAndIndexReferenceResolver.java`, `ExternalProgramCatalog.java`, `ProgramNameCanonicalizer.java`, `ResolutionContracts.java`, `ResolutionAnalysisReport.java` | binding nominal, cobertura e relatórios | ativo |
| Observabilidade | `AnalysisLogContext.java`, `src/main/resources/logback.xml` e pontos de logging no pipeline | logging estrutural e degradação | ativo |
| Frontend | `src/main/resources/web/*`; snapshots `*Snapshot.java` | visualização de artefatos de análise | ativo |
| Testes Java | 31 arquivos em `src/test/java/io/github/gustavo2358/cobolexplorer/` | oráculos de contrato, semântica, regressão, determinismo e escala | ativo |
| Fixtures sintéticas | 50+ arquivos em `src/test/resources/cobol/{source-format,source-format-integration,provenance,preprocessor,semantic,resolution}/` | classes semânticas e contraexemplos | ativo |
| Corpus e copybooks | `corpus/cbl/{COACTUPC.cbl,CBSTM03A.CBL,CBSTM03D.CBL}` e `corpus/cpy/*` | cenários reais e variante didática | ativo; evidência, não especificação |
| Artefatos gerados | `dist/`, `dist-cbstm03a/`, `dist-cbstm03d/` | saídas versionadas para inspeção visual | histórico/regressão |
| Scripts | `scripts/source-normalizer-regression.sh`, `scripts/verify-naming.sh`, `run.sh` | gates e execução operacional existentes | ativo |
| Build e licença | `pom.xml`, `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md` | ambiente, dependências e atribuições | ativo |

## Índice dos oráculos já localizáveis

| Capacidade | Código/testes/fixtures principais | Observação |
| --- | --- | --- |
| Source format, line endings e comment entries | `SourceNormalizerTest`, `SourceProvenanceTest`, `SourceNormalizationPreprocessingIntegrationTest`, `scripts/source-normalizer-regression.sh` | inclui COPY e provenance |
| Política do preprocessor | `PreprocessorEnginePolicyTest` | cobertura de alternativas de topo |
| Modelo AST e cobertura | `AstBuilderTest`, `AstBuildCoverageTest`, `StructuredExpressionAstTest`, `StatementModelAstTest`, `NominalReferenceAstTest` | usa manifesto gramatical |
| Símbolos, entidades e escopos | `SymbolTableBuilderTest`, `EntityScopeAndOccurrenceTest`, `CompilationUnitModelTest`, `DeclarationModelAstTest` | inclui unidades aninhadas |
| Resolução nominal | `DataAndIndexReferenceResolverTest`, `ProcedureFileProgramReferenceResolverTest`, `CallSemanticsTest`, `ProgramNameCanonicalizerTest` | inclui fixtures adversariais |
| Cobertura e relatório | `SemanticCoverageTest`, `ResolutionAnalysisReportTest`, `CoverageSnapshotTest`, `ResolutionSnapshotTest` | inclui teste de escala algorítmica |
| Regressões E2E e apresentação | `ReferenceResolutionBaselineCharacterizationTest`, `SemanticModelBaselineCharacterizationTest`, `DynamicCallVariantTest` | corpus e snapshots |
| Logging | `LoggingInfrastructureTest`, `ExplorerMainLoggingTest`, `FrontendLoggingTest`, `SemanticModelLoggingTest`, `ResolutionLoggingTest` | separado de diagnostics |
| Nomenclatura | `scripts/verify-naming.sh` | protege gramáticas por exceção explícita |

## Fontes a analisar por evidência histórica quando necessário

O histórico Git é evidência suplementar, especialmente os commits que
implementaram fases listadas nas tasklists. Não deve ser carregado no contexto
normal. Os commits recentes confirmam que logging, source normalization e
rename foram concluídos antes desta migração.

## Exclusões deliberadas deste inventário

Não há `AGENTS.md` operacional no corte inicial. O arquivo com esse nome dentro
de `specs/` é a fonte importada descrita acima e não deve instruir agentes
automaticamente. Também não há `docs/` canônico nem `harness/` no corte inicial.
