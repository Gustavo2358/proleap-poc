# WORK-COV-001 — Preservar análise parcial diante de COPY ausente

Status: concluído em 2026-08-31. Risco: alto.

O trabalho separou input externo ausente de corrupção interna. Um `COPY` não encontrado continua produzindo placeholder, diagnostic e claim incompleta, mas deixa de desabilitar globalmente a classificação externa quando parse, AST, occurrences e resolução permanecem estruturalmente coerentes. Erros de preprocessor, lexer/parser recovery e produtos incoerentes continuam fail-closed.

`ExternalClassification` agora registra `COMPLETE` ou `INCOMPLETE_UNRESOLVED_COPY`. Sob input completo, a projeção de WORK-EXT-001 permanece intacta e consolida somente os gaps artificiais cobertos. Sob COPY ausente, a hipótese `CICS/POSSIBLE_INTRINSIC/INFERRED` permanece observável ao lado dos gaps nominais originais, pois o membro faltante ainda poderia introduzir uma declaration COBOL. Cada COPY ausente vira gap determinístico com membro, arquivo e linha; contador, completude e contexto da classificação chegam ao snapshot e à UI sem recomputação semântica.

Evidências executáveis principais:

- [EVAL-COV-003](../../evals/semantic-eval-catalog.md#ast-e-cobertura) e [INV-COV-003](../../architecture/invariants.md#inv-cov-003--incompletude-preserva-fatos-independentes);
- `PartialAnalysisMissingCopyTest` cobre input completo/parcial, múltiplos COPYs, path explícito e MR1–MR4;
- `ExternalClassificationProjectionTest` cobre coexistência de hipótese, input gap e binding gaps, além de fail-closed por contexto incoerente;
- `ExplorerMainLoggingTest` cobre execução parcial e skip estrutural agregados;
- gates `fast`, `architecture`, `semantic`, `performance` e `full` passaram; o E2E de COACTUPC preservou três COPYs ausentes e análise global incompleta.

Não foram introduzidos taint localizado, símbolos especulativos, busca fora dos paths configurados, CFG, dataflow, possible-values, dynamic CALL, GRBE, providers, extractors ou framework genérico de uncertainty. Esses assuntos continuam sem autorização de início.
