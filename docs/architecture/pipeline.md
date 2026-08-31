# Pipeline e fronteiras de análise

O pipeline atual preserva produtos separados e imutáveis:

```text
fonte COBOL físico
  → normalização / SourceMap
  → preprocessing / COPY
  → parse tree ANTLR
  → AST semântica
  → compilation units e symbol tables
  → ocorrências de referência
  → resolução nominal
  → classificação externa pós-resolução focalizada
  → futuras análises CFG e dataflow
```

Cada seta produz um artefato para a fase seguinte; uma fase não deve gravar conclusões de análise posterior no artefato anterior.

## Limites atuais

- O `SourceMap` nasce no texto físico e é composto pelas transformações.
- A parse tree representa a estrutura reconhecida pelas gramáticas.
- A AST preserva estrutura semântica, texto/provenance quando exigidos e construções opacas; não contém binding, CFG ou dataflow.
- Symbol tables modelam declarations, scopes, namespaces, entidades e relações declarativas, sem valores de runtime.
- Occurrences identificam usos tipados sem fazer lookup.
- `ReferenceResolution` é produto separado e imutável; preserva candidatos, status e diagnósticos para binding nominal.
- CFG, reaching definitions, propagação de valores e análise de linguagens embarcadas ainda não são produtos do pipeline. A ausência deles deve continuar observável como boundary/incompletude, não como resultado vazio.

`ExternalClassification` materializa o primeiro slice pós-resolução somente para a shape estrutural autorizada de `DFHRESP(...)`/`DFHVALUE(...)`: binding COBOL válido sempre vence; raiz `UNRESOLVED` pode produzir hipótese CICS `INFERRED` com provenance e occurrences do subtree cobertas. O produto não muta `ReferenceResolution`; relatório e snapshot mantêm o binding original e substituem apenas gaps artificiais explicitamente cobertos por um fato externo ainda bloqueante. Infraestrutura genérica e demais capabilities continuam no backlog.

COPY COBOL não resolvido é input externo ausente, não corrupção automática dos produtos posteriores. O preprocessor publica sua identidade por `Diagnostic.Code.UNRESOLVED_COPY`, sem reinterpretar mensagem humana. Quando o placeholder permite construir parse tree, AST, símbolos, occurrences e resolução de forma coerente, esses produtos e a classificação externa focalizada continuam sendo compostos. Cada COPY ausente permanece gap enumerável, a análise global fica incompleta e a classificação registra `CopyInputCompleteness.INCOMPLETE_UNRESOLVED_COPY`; nessa condição a projeção preserva também os gaps nominais cobertos, pois o universo de declarations COBOL não estava completo. `CopyInputCompleteness.COMPLETE` afirma somente disponibilidade dos COPYs solicitados; erros de preprocessor, lexer/parser recovery e incoerência interna continuam num eixo estrutural separado, fora desse fallback e com o fail-closed anterior.

Este documento descreve a fronteira consolidada. Consulte os [invariantes com IDs](invariants.md), os [ADRs](decisions/index.md) e o [mapa curto de componentes](../../ARCHITECTURE.md).
