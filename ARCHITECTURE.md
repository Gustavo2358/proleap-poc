# Arquitetura — COBOL Structure Atlas

O projeto transforma fonte COBOL em produtos semânticos separados, imutáveis e navegáveis. A direção de dependência acompanha o pipeline; resultados de fases posteriores não são gravados nos artefatos anteriores.

```text
fonte físico
  → SourceNormalizer / SourceMap
  → PreprocessorEngine / COPY
  → ANTLR parse tree
  → AstBuilder / CompilationUnitModel
  → SymbolTable por program unit
  → ReferenceOccurrences
  → ReferenceResolution
  → snapshots e relatório de cobertura
```

## Componentes

| Componente | Entrada | Produto | Fronteira principal |
| --- | --- | --- | --- |
| normalização | registros físicos e política fixed-format | texto normalizado + `SourceMap` | provenance nasce antes da transformação |
| preprocessing | fonte mapeado e copybooks | texto expandido + mapa composto + diagnostics | input ausente permanece explícito |
| frontend ANTLR | texto preprocessado | parse tree | reconhece sintaxe, não binding |
| AST | parse tree + mapa | `CompilationUnitModel` e coverage | sem símbolos, binding, CFG ou dataflow |
| símbolos | `Ast.Program` por unit | escopos, declarações, entidades e relações | não infere valores nem resolve usos |
| ocorrências | AST + índice de escopo | usos nominais tipados | coleta sem lookup |
| resolução | units, símbolos, ocorrências e policy | candidatos, decisões e diagnostics | binding nominal sem value resolution |
| apresentação | produtos semânticos | snapshots/HTML | DTOs não viram modelo semântico |

O detalhamento do pipeline está em [docs/architecture/pipeline.md](docs/architecture/pipeline.md). As fronteiras normativas estão em [invariants.md](docs/architecture/invariants.md), e o racional em [ADRs](docs/architecture/decisions/index.md). Contratos de cada subsistema ficam em [docs/domain/](docs/domain/index.md).

CFG, reaching definitions, value propagation e parsers dedicados para linguagens embarcadas não existem no pipeline atual. Quando forem introduzidos, devem consumir produtos anteriores sem retroalimentar a AST ou o binding nominal.
