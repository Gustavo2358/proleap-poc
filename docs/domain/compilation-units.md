# Compilation units e program units

## Propósito

`CompilationUnitModel` mantém o inventário completo de programas reconhecidos em um fonte, incluindo top-level e nested programs. Essa é a fronteira de identidade usada pelos produtos semânticos posteriores.

## Entradas e saídas

- **Entrada:** todos os program contexts produzidos pelo `AstBuilder`.
- **Saída:** `CompilationUnitModel` imutável, ordenado, indexado por `ProgramUnitId`.

`ProgramUnitId` combina compilation unit, caminho estrutural e nome canônico. Cada unit preserva `parentId` quando nested e seu próprio `Ast.Program`.

## Contrato atual

- Nenhum program unit reconhecido pode desaparecer porque não é o primeiro do arquivo.
- Parent programs precedem nested programs na ordem determinística.
- Symbol tables são construídas separadamente por unit, preservando ancestry.
- Visibilidade não é inferida pela árvore estrutural isoladamente; `GLOBAL`, `COMMON`, nesting e shadowing são aplicados pelo algoritmo de resolução pertinente.
- Outros top-level programs não viram candidatos globais por conveniência.

## Fronteiras e incerteza

O modelo representa containment e identidade, não um call graph nem catálogo da codebase. Programas externos pertencem a `ExternalProgramCatalog`. Input que não produz nenhuma unit é erro do frontend/orquestração, não compilation unit vazia válida para análise completa.

## Complexidade e determinismo

O índice por ID é construído uma vez; parentage é validado na construção. Lookups por ID não exigem varrer todas as units.

## Evidência executável

`CompilationUnitModelTest`, `ReferenceResolutionBaselineCharacterizationTest` e fixtures de baseline, visibility, nested `GLOBAL` e `COMMON` em `src/test/resources/cobol/resolution/`.

## Relações

Evals: EVAL-UNIT-001, EVAL-RES-DATA-003, EVAL-RES-PROG-001 e EVAL-RES-DET-001. Invariantes: INV-DET-001, INV-PERF-001, INV-RES-002 e INV-RES-003. ADRs: ADR-0005 e ADR-0006.
