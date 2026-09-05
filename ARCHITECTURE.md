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
  → ResolutionAnalysisReport / classificação externa
  → CobolSemanticProduct / CobolSemanticPort
  → adapters JSON, snapshots e HTML
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
| Semantic Product | produtos canônicos do frontend | facts COBOL materializados por program unit + port fechado | DATA, MOVE literal, CALL variável, IF estrutural e inventário observado; readiness é dimensional |
| apresentação | produtos semânticos | snapshots/HTML | DTOs não viram modelo semântico |

## Direção downstream

```text
COBOL Semantic Product
  → CobolLower
  → Analysis IR
  → CFG
  → Statement Effects / Storage Semantics
  → Reaching Definitions
  → Possible Values
  → Dependency Facts
```

O Semantic Product continua COBOL-specific e não contém IR, CFG ou dataflow. O
lowering é a fronteira em que começa a representação mais neutra para análise.
Cada construct precisa declarar se sua structure, seus successors e seus
operands/roles são suficientes às etapas downstream; lacuna não pode ser
representada como ausência. Projectors apenas traduzem os produtos canônicos do
frontend e não executam análise semântica nova.

O detalhamento do pipeline está em [docs/architecture/pipeline.md](docs/architecture/pipeline.md). As fronteiras normativas estão em [invariants.md](docs/architecture/invariants.md), e o racional em [ADRs](docs/architecture/decisions/index.md). O contrato e a matriz atuais do produto estão em [COBOL Semantic Product](docs/domain/cobol-semantic-product.md); os demais contratos de subsistema ficam em [docs/domain/](docs/domain/index.md).

O Semantic Product está integrado ao composition root, possui consumer
boundary-only e transporte JSON determinístico. DATA e o profile atual de CALL
estão lowering-ready; MOVE e IF continuam parciais e statements apenas
observados permanecem bloqueados. `CobolLower`, Analysis IR, CFG,
effects/storage, reaching definitions, value propagation e parsers dedicados
para linguagens embarcadas não existem no pipeline integrado atual. Quando
forem introduzidos, devem consumir produtos anteriores sem retroalimentar a AST
ou o binding nominal.
