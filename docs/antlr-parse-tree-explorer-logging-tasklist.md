# Logging do `antlr-parse-tree-explorer` — tasklist de implementação

Documento vivo da implementação iniciada sobre o commit
`a087ba04cfc787acc56165493905b1a9eed821b8`.

## Regras de acompanhamento

- Uma caixa só é marcada após verificação efetiva do respectivo critério.
- Alterações funcionais seguem RED → GREEN → testes relacionados → suíte completa → gate real.
- Antes de cada commit: revisão integral do diff da fase, `git diff --check`, conferência de escopo e gate completo.
- Artefatos de regressão são gerados somente em `/tmp`.
- Escopo funcional: somente `antlr-parse-tree-explorer/**`; este documento é a única alteração permitida em `docs/**`.
- Nenhum `push` será realizado.

## Registro inicial

- Commit inicial: `a087ba04cfc787acc56165493905b1a9eed821b8`.
- Worktree inicial: limpo.
- Logging encontrado no módulo: um `System.out.printf` user-facing em `ExplorerMain`; nenhum `System.err`, `printStackTrace` ou framework de logging.
- Commits concluídos: _nenhum ainda_.

## Gate de regressão comum a todas as fases

- [ ] `mvn test` passa.
- [ ] execução real canônica do `ExplorerMain` passa.
- [ ] `index.html`, `ast.html`, `symbols.html`, `resolution.html` existem e não estão vazios.
- [ ] `tree-data.js`, `ast-data.js`, `symbol-data.js`, `resolution-data.js` existem e não estão vazios.
- [ ] lexer/parser permanecem sem regressão inesperada.
- [ ] AST, symbol tables, occurrences e resolução continuam produzidas.
- [ ] `ResolutionAnalysisReport` permanece coerente.
- [ ] métricas semânticas canônicas não se degradam sem explicação.

O comando/harness, diretório temporário e resultados de cada execução serão registrados nas fases abaixo.

## Fase 0 — Baseline, tasklist e caracterização

- [x] Criar esta tasklist antes de modificar código de produção.
- [x] Registrar commit inicial e estado inicial do worktree.
- [x] Mapear `System.out`, `System.err`, `printStackTrace` e frameworks existentes.
- [x] Mapear boundaries principais do pipeline.
- [x] Caracterizar comportamento atual do `ExplorerMain`.
- [x] Registrar baseline funcional de `COACTUPC.cbl`.
- [x] Registrar métricas semânticas relevantes.
- [x] Registrar tempo aproximado do caso canônico.
- [x] Garantir mecanicamente que o gate detecta artefatos ausentes e degradação semântica.
- [x] Escrever primeiro os testes de caracterização necessários e confirmar RED quando aplicável.
- [x] Executar testes focados e relacionados.
- [x] Executar `mvn test`.
- [x] Executar regressão real.
- [x] Revisar diff, logging, segurança, semântica e escopo.
- [x] Executar `git diff --check` e conferir arquivos da fase.
- [x] Commit da fase e hash: `b13a996` — `test: establish logging regression baseline`.

### Evidências e decisões da fase 0

- O harness existente `scripts/source-normalizer-regression.sh` já executa a suíte e três jornadas reais, mas será avaliado e ampliado em vez de duplicado.
- O `System.out.printf` atual é o resumo deliberadamente user-facing da CLI; seu papel definitivo será decidido na fase 2.
- Boundaries: entrada/saída e falha operacional em `ExplorerMain`; normalização em `SourceNormalizer`; expansão/fallback de COPY e opções em `PreprocessorEngine`; captura de problemas ANTLR em `AntlrDiagnosticListener`; construção semântica em `AstBuilder` e `CompilationUnitSymbolTableBuilder`; coleta e resolução em `ReferenceOccurrenceCollector` e `CobolReferenceResolver`; completude em `ResolutionAnalysisReport`.
- Pipeline atual: lê fonte → normaliza → preprocessa → lexer/tokens → parser/tree → exporta árvore → AST/coverage → tabelas → ocorrências → resolução/report → exporta artefatos → imprime resumo CLI.
- RED observado na checagem focada: expectativas inferidas incorretas (`preprocessedLines=5406`, `dataSymbols=761`, `fileSymbols=5`) foram rejeitadas; o baseline foi corrigido exclusivamente para os valores produzidos e conferidos nos artefatos (`7408`, `751`, `0`). Não houve mudança de produção para tornar o teste verde.
- Baseline canônico: parse tree 57.227 nós/37.682 tokens/profundidade 39; AST 9.189 nós/profundidade 11/1 CALL literal/14 embedded; símbolos 853/651 scopes/2 diagnósticos; 3.058 referências, 1.605 resolved, 2 ambiguous, 1.451 unresolved, 0 unsupported; 1.471 gaps; `dependencyAnalysisReady=false`; `candidateInspections=2009`, máximo 3.
- Tempo aproximado antes de logging: `/usr/bin/time -p mvn -q compile exec:java ...` = `real 13.36s` (inclui Maven/compile incremental), `user 30.63s`, `sys 0.72s`.
- `mvn test`: 109 testes, 0 falhas/erros/skips, 46.395s.
- Gate ampliado: `./scripts/source-normalizer-regression.sh phase0`, passou; saída em `/tmp/proleap-source-normalizer-phase0.6nNOVI`; validou arquivos não vazios, lexer/parser, baseline CLI, métricas de árvore/AST/símbolos, status/contagens/métricas de resolução, provenance e casos auxiliares.
- Revisão: somente testes/harness/tasklist; nenhum logging, source COBOL ou mudança semântica; sem arquivos periféricos.

## Fase 1 — Infraestrutura de logging

- [x] Escrever testes dos contratos de configuração e confirmar RED.
- [x] Adicionar SLF4J compatível com Java 17 e o Maven atual.
- [x] Adicionar Logback como implementação padrão.
- [x] Criar configuração default conservadora e formato estruturado.
- [x] Permitir override de níveis sem recompilação.
- [x] Introduzir `runId` e MDC de baixa cardinalidade.
- [x] Garantir limpeza do MDC em sucesso e falha.
- [x] Definir logger por classe, sem dependência Logback no domínio.
- [x] Executar testes focados/relacionados, suíte e regressão real.
- [x] Medir comportamento com configuração padrão.
- [x] Revisar diff completo e executar `git diff --check`.
- [x] Commit da fase e hash: `0aec643` — `feat: establish analyzer logging infrastructure`.

## Fase 2 — Observabilidade do pipeline no `ExplorerMain`

- [x] Escrever testes de início, fim, falha e campos essenciais; confirmar RED.
- [x] Instrumentar `analysis_started` e `analysis_completed` em `INFO`.
- [x] Instrumentar resumos/timings de normalização, preprocessing, parsing, AST, símbolos, ocorrências e resolução em `DEBUG`.
- [x] Correlacionar `runId`, `source` e `programUnit` quando aplicável.
- [x] Registrar `analysis_failed` em `ERROR` com stacktrace único no boundary.
- [x] Preservar semântica e decidir/documentar saída user-facing da CLI.
- [x] Executar testes focados/relacionados, suíte e regressão real.
- [x] Revisar diff completo e executar `git diff --check`.
- [x] Commit da fase e hash: `fbafb84` — `feat: instrument analyzer pipeline lifecycle`.

## Fase 3 — Normalização, preprocessing e ANTLR

- [x] Escrever testes antes da implementação e confirmar RED.
- [x] Adicionar resumo `DEBUG` e decisões `TRACE` relevantes ao `SourceNormalizer`.
- [x] Adicionar políticas/decisões `TRACE` ao `PreprocessorEngine`.
- [x] Adicionar `WARN` útil para COPY unresolved/cycle/I/O, com fallback e impacto e sem alta cardinalidade.
- [x] Manter `AntlrDiagnosticListener` como produtor de `Diagnostic`, sem `WARN` individual.
- [x] Emitir resumo `parse_degraded` somente quando houver degradação.
- [x] Testar alta cardinalidade sem `WARN` por erro individual.
- [x] Verificar ausência de parse-tree dump e de custo caro com nível desligado.
- [x] Executar testes focados/relacionados, suíte e regressão real.
- [x] Revisar diff completo e executar `git diff --check`.
- [ ] Commit da fase e hash: `feat: add frontend diagnostic logging`.

## Fase 4 — AST e symbol tables

- [ ] Escrever testes de resumo por program unit e invariância; confirmar RED.
- [ ] Adicionar resumo `ast_built` por program unit.
- [ ] Adicionar decisões AST `TRACE` semanticamente úteis e controladas.
- [ ] Adicionar resumo `symbol_table_built` por program unit.
- [ ] Adicionar decisões de símbolos/escopos/relações em `TRACE` com custo protegido.
- [ ] Confirmar que logging não altera AST ou symbol tables.
- [ ] Executar testes focados/relacionados, suíte e regressão real.
- [ ] Revisar diff completo e executar `git diff --check`.
- [ ] Commit da fase e hash: `feat: trace semantic model construction`.

## Fase 5 — Coleta e resolução de referências

- [ ] Escrever testes de resolved/unresolved/ambiguous, agregação e fonte semântica; confirmar RED.
- [ ] Adicionar resumo `references_collected` por program unit e `TRACE` individual controlado.
- [ ] Adicionar resumo `resolution_completed` e decisões `TRACE` sem despejar objetos.
- [ ] Proteger construção de listas de candidatos com `isTraceEnabled()`.
- [ ] Usar `ResolutionAnalysisReport` como fonte da completude.
- [ ] Emitir um `WARN analysis_degraded` agregado quando `dependencyAnalysisReady=false`.
- [ ] Confirmar ausência de `INFO/WARN` por ocorrência.
- [ ] Executar testes focados/relacionados, suíte e regressão real.
- [ ] Revisar diff completo e executar `git diff --check`.
- [ ] Commit da fase e hash: `feat: trace reference resolution decisions`.

## Fase 6 — Códigos, severidade e consistência de diagnósticos

- [ ] Avaliar tamanho/risco de `code`, `severity`, frontend, phase, file, posição, token e exception class.
- [ ] Evitar parsing frágil de `message` no logging quando houver alternativa localizada.
- [ ] Se seguro/localizado: escrever testes RED, introduzir códigos/severidade, migrar consumidores e verificar compatibilidade.
- [ ] Se grande demais: documentar dívida e não forçar breaking change.
- [ ] Executar testes, suíte/regressão e revisão se houver código alterado.
- [ ] Commit/hash somente se houver código: `refactor: stabilize frontend diagnostic codes`.

## Fase 7 — Performance, volume e hardening

- [ ] Auditar concatenação e estruturas criadas apenas para `DEBUG/TRACE`.
- [ ] Auditar `toString`/serializações grandes de parse tree, AST e `SourceMap`.
- [ ] Auditar logs em loops de linha/token/node/símbolo/referência/candidato.
- [ ] Auditar `WARN` de alta cardinalidade e stacktrace duplicado.
- [ ] Auditar dados sensíveis, MDC não limpo e contexto ausente.
- [ ] Executar comparação reproduzível OFF/WARN versus INFO.
- [ ] Registrar comando, corpus, repetições, tempos e volume aproximado.
- [ ] Investigar regressão material e adicionar teste se necessário.
- [ ] Executar suíte/regressão e revisão após qualquer ajuste.
- [ ] Commit/hash somente se houver código/configuração: `perf: harden analyzer logging overhead`.

## Fase 8 — Revisão global e encerramento

- [ ] Revisar semântica global dos níveis ERROR/WARN/INFO/DEBUG/TRACE.
- [ ] Confirmar separação entre `Diagnostic` e logging.
- [ ] Confirmar `ResolutionAnalysisReport` como fonte da completude.
- [ ] Revisar cada `WARN`/`ERROR` quanto a local, fallback e impacto.
- [ ] Confirmar ausência de custo pesado com TRACE desligado e volume proporcional em nível padrão.
- [ ] Confirmar ausência de source/literal/dump sensível indevido.
- [ ] Confirmar nomes/campos consistentes e testes robustos.
- [ ] Confirmar ausência de lógica de negócio em logging e de Logback no domínio.
- [ ] Confirmar escopo estrito e registrar qualquer exceção.
- [ ] Executar testes focados, `mvn test` e gate completo.
- [ ] Repetir medição de overhead.
- [ ] Executar `git diff --check`, revisar `git status` e esta tasklist inteira.
- [ ] Registrar todos os hashes de commit em ordem.
- [ ] Commit/hash final somente se revisão exigir mudanças: `refactor: finalize analyzer logging hardening`.

## Testes criados

- Fase 0: baseline de artefatos `coactupc-semantic-baseline.txt` consumido pelo harness existente; cobertura ampliada para `coverage-data.js` e métricas canônicas de frontend/AST/símbolos/resolução.
- Fase 1: `LoggingInfrastructureTest` (4 contratos): níveis default, override global, override seletivo por classe e lifecycle/restauração de MDC.
- Fase 2: `ExplorerMainLoggingTest` (2 contratos): lifecycle/timings/correlação e produtos preservados; falha escapando com um único stacktrace e MDC limpo.
- Fase 3: `FrontendLoggingTest` (6 contratos): decisões de normalização, agregação de 100 COPYs ausentes, ciclos/I/O, decisões trace de preprocessamento, 250 diagnósticos ANTLR sem WARN e resumo único de degradação após recuperação.

## Comandos de regressão e resultados

- Fase 0: `mvn test` — 109 testes verdes.
- Fase 0: `./scripts/source-normalizer-regression.sh phase0` — passou, artefatos em `/tmp/proleap-source-normalizer-phase0.6nNOVI`.
- Fase 1: `mvn -Dtest=LoggingInfrastructureTest test` — 4 testes verdes após RED por dependências/contexto ausentes e RED seletivo (`TRACE` esperado, herança `WARN` observada).
- Fase 1: `mvn test` — 113 testes verdes, 0 falhas/erros/skips, 46.124s.
- Fase 1: `./scripts/source-normalizer-regression.sh phase1` — passou, artefatos em `/tmp/proleap-source-normalizer-phase1.qZVn2a`; métricas idênticas à baseline.
- Fase 2: `mvn -Dtest=ExplorerMainLoggingTest test` — RED com zero eventos; GREEN com 2 testes.
- Fase 2: relacionados `ExplorerMainLoggingTest,ResolutionSnapshotTest,LoggingInfrastructureTest` — 9 testes verdes.
- Fase 2: `mvn test` — 115 testes verdes, 0 falhas/erros/skips, 44.795s.
- Fase 2: `./scripts/source-normalizer-regression.sh phase2` — passou, artefatos em `/tmp/proleap-source-normalizer-phase2.4csHzW`; métricas canônicas idênticas.
- Fase 3: `mvn -Dtest=FrontendLoggingTest test` — RED por eventos ausentes; GREEN com 6 testes.
- Fase 3: relacionados `FrontendLoggingTest,SourceNormalizerTest,PreprocessorEnginePolicyTest,SourceNormalizationPreprocessingIntegrationTest,SourceProvenanceTest,ExplorerMainLoggingTest` — 31 testes verdes.
- Fase 3: `mvn test` — 121 testes verdes, 0 falhas/erros/skips, 45.885s (execução final após a revisão).
- Fase 3: `./scripts/source-normalizer-regression.sh phase3-final` — passou, artefatos em `/tmp/proleap-source-normalizer-phase3-final.pGd7Vi`; todos os nove artefatos de cada uma das três jornadas estavam presentes e não vazios; métricas canônicas preservadas.

## Medição de overhead

_A preencher na fase 7 e repetir na fase 8._

## Decisões técnicas e dívidas deliberadas

- MDC futuro em processamento assíncrono/multithread deverá ser propagado explicitamente; a implementação atual é síncrona.
- Versões verificadas nas fontes oficiais em 2026-08-26: SLF4J 2.0.18 e Logback 1.6.3. Ambos suportam Java 17 (Logback 1.6.x requer Java 11+ e SLF4J 2.0.1+). `slf4j-api` é dependência de compilação; `logback-classic` fica em runtime/default provider.
- Default: `root=WARN`, `ExplorerMain=INFO`; categorias de decisões permanecem `WARN`. Overrides por propriedades/ambiente (`ANALYZER_LOG_LEVEL`, `PREPROCESSOR_LOG_LEVEL`, `RESOLVER_LOG_LEVEL` etc.) ou `logback.configurationFile`.
- Formato estruturado em stderr inclui timestamp, nível, classe, thread, `runId`, `source`, `programUnit` e mensagem `event=...`; stdout permanece reservado à CLI.
- O escopo MDC restaura o mapa anterior no fechamento, inclusive diante de exceção via try-with-resources. Nenhuma classe de domínio importa Logback; somente o teste de configuração o faz.
- Com configuração padrão na Fase 1, as execuções reais não geraram avisos internos do provider nem volume adicional; eventos de lifecycle começam na Fase 2.
- `System.out.printf` foi mantido como contrato user-facing da CLI e continua alimentando o baseline; eventos operacionais são enviados separadamente a stderr.
- O boundary mantém a fase operacional corrente e registra exceção escapando uma vez em `analysis_failed`; camadas internas não registram stacktrace.
- Default da Fase 2 produz exatamente dois eventos `INFO` por execução normal. Contagens que exigem varrer texto/mapas para `DEBUG` estão protegidas por `isDebugEnabled()`.
- `Diagnostic` continua sendo o único detalhe individual de ANTLR; `AntlrDiagnosticListener` só fornece campos técnicos mínimos em `TRACE`, protegido por `isTraceEnabled()` e sem token/mensagem arbitrária.
- COPY ausente, ciclo e I/O são sumarizados uma vez por processamento, com `count`, `reason`, `fallback` e `impact`; decisões individuais ficam em `TRACE`. O `WARN parse_degraded` é emitido uma vez após lexer/parser quando há erros e a árvore foi produzida.
- Demais decisões serão registradas quando tomadas.

## Desvios e exceções de escopo

- Nenhum até o momento.

## Commits em ordem

1. `b13a996` — `test: establish logging regression baseline`
2. `0aec643` — `feat: establish analyzer logging infrastructure`
3. `fbafb84` — `feat: instrument analyzer pipeline lifecycle`
