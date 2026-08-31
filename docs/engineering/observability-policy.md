# Política de observabilidade

Logging operacional, diagnostics semânticos e snapshots possuem funções diferentes. Logging explica o lifecycle da execução; diagnostics pertencem aos resultados do analisador; snapshots são artefatos determinísticos para navegação e regressão.

## Níveis e volume

- `ERROR`: falha que escapa do boundary da análise, com stacktrace emitido uma única vez.
- `WARN`: degradação agregada com motivo, fallback e impacto; não um evento por token, nó, COPY ou referência.
- `INFO`: início e conclusão do lifecycle, com cardinalidade constante.
- `DEBUG`: resumos e timings por fase/program unit.
- `TRACE`: decisões individuais, protegido por `isTraceEnabled()` quando construir detalhes custa memória ou CPU.

O default é `root=WARN` e `ExplorerMain=INFO`. Overrides por classe/categoria, variável ou propriedade não exigem recompilação. `stdout` permanece reservado ao contrato user-facing da CLI; logging vai para `stderr`.

## Correlação e contexto

O formato inclui timestamp, nível, logger, thread, `runId`, `source`, `programUnit` e `event`. `AnalysisLogContext` restaura o MDC anterior em sucesso ou falha. O pipeline atual é síncrono; execução futura assíncrona ou paralela deve propagar o contexto explicitamente.

## Boundaries

- Classes de domínio dependem somente da API SLF4J, não de Logback.
- `Diagnostic` continua sendo o resultado individual e navegável; logging não faz parsing frágil de sua mensagem para reconstruir semântica.
- `ResolutionAnalysisReport.completeness()` é a fonte do `analysis_degraded` agregado.
- O composition root registra uma decisão agregada por execução do classifier, incluindo `executed`, motivo, quantidade de COPYs unresolved, `copyInputCompleteness`, fallback e impacto; o campo descreve somente disponibilidade de COPY e não emite evento por occurrence.
- Camadas internas não repetem stacktrace já registrado pelo boundary.
- Fonte COBOL, literals, parse trees, ASTs e `SourceMap` completos não são despejados em logs por padrão.

## Desempenho e segurança

Listas de candidates, serializações e métricas que exigem traversal só são construídas quando o nível correspondente está habilitado. Eventos default não crescem com tokens/nós/references. Antes de elevar nível em produção, considerar cardinalidade e possível conteúdo sensível.

## Evidência executável

`LoggingInfrastructureTest`, `ExplorerMainLoggingTest`, `FrontendLoggingTest`, `SemanticModelLoggingTest` e `ResolutionLoggingTest` verificam configuração, lifecycle, agregação, custo protegido e invariância dos produtos.

## Dívidas relacionadas

Propagação de MDC em concorrência e estabilização transversal de code/severity de diagnostics permanecem no [backlog](../work/backlog.md). Timings são métricas operacionais, não parte de snapshots semânticos determinísticos.
