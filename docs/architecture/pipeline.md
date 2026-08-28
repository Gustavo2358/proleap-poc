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

Este documento descreve a fronteira consolidada. Os invariantes com IDs, ADRs e mapa detalhado de dependências serão produzidos pela arqueologia da Fase 3.
