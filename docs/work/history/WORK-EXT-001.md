# WORK-EXT-001 — Classificar DFHRESP e DFHVALUE unresolved como possíveis intrínsecos CICS

Status: concluído em 2026-08-31. Risco: médio.

O trabalho materializou `ExternalClassification` como produto imutável posterior à resolução nominal e um classifier mínimo para a shape estrutural autorizada de `DFHRESP(...)`/`DFHVALUE(...)`. Binding COBOL válido, `AMBIGUOUS`, `UNSUPPORTED` e input de frontend incompleto não são promovidos; a hipótese aceita permanece `CICS/POSSIBLE_INTRINSIC/INFERRED`, ligada à raiz AST, provenance e occurrences cobertas.

`ReferenceResolution` permanece intacto. A composição no `ExplorerMain` passa o produto separado ao relatório e snapshot; a projeção troca somente gaps `REFERENCE_BINDING` artificiais cobertos por um fato externo bloqueante e conserva entries, diagnostics e gaps não relacionados. A apresentação consome os campos publicados sem recomputar a classificação.

Evidências executáveis principais:

- [EVAL-EXT-001](../../evals/semantic-eval-catalog.md#classificação-externa-pós-resolução);
- `ExternalCicsCharacterizationTest`, `CicsIntrinsicClassifierTest`, `ExternalClassificationProductTest` e `ExternalClassificationProjectionTest`;
- `ArchitectureBoundaryTest` para a ausência de dependências reversas;
- gates `fast`, `architecture`, `semantic` e `full` do Harness.

Infraestrutura genérica de extensibilidade, external symbols, extractors, GRBE, CFG, dataflow, possible-values e control-flow providers permanecem no backlog e não foram autorizados por esta conclusão.
