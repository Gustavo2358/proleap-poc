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

O modelo representa containment e identidade, não um call graph nem catálogo da codebase. Programas externos não são procurados fora do artefato: `CALL` literal sem alvo interno é registrado separadamente como dependência externa observada. Input que não produz nenhuma unit é erro do frontend/orquestração, não compilation unit vazia válida para análise completa.

## Complexidade e determinismo

O índice por ID é construído uma vez; parentage é validado na construção. Lookups por ID não exigem varrer todas as units.

## Evidência executável

`CompilationUnitModelTest`, `ReferenceResolutionBaselineCharacterizationTest` e fixtures de baseline, visibility, nested `GLOBAL` e `COMMON` em `src/test/resources/cobol/resolution/`.

## Relações

Evals: EVAL-UNIT-001, EVAL-RES-DATA-003, EVAL-RES-PROG-001 e EVAL-RES-DET-001. Invariantes: INV-DET-001, INV-PERF-001, INV-RES-002 e INV-RES-003. ADRs: ADR-0005 e ADR-0006.

## Ownership de input na fronteira física (R4)

Conforme [IBM Enterprise COBOL 6.4 — program structure](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=structure-cobol-program),
END PROGRAM é opcional somente na última unidade externa que não contém programas
nested. O AstBuilder admite EOF para ownership de input apenas nessa posição da
árvore, com anteriores explicitamente delimitadas, start/stop reais, EOF consumido
no fim do texto expandido e fronteira física conhecida no SourceMap. O início deve
pertencer ao source principal. Sem evidência de integridade do preprocessamento/lexer,
ou com erro do parser, essa nova prova não é emitida. A prova explícita existente
não recebe nova interpretação.

SourceMap conserva identidade e offset do fim físico através da composição. Slices
transformados e mapas marcados como inclusão não são fontes completas. Essa fronteira
não é um token END PROGRAM nem uma origem textual fabricada; nenhuma provenance
sintética é marcada exact. Os gaps conservam as próprias regiões e include chains.

UnitInputProof e EntryInputProof reutilizam ownership por ocorrência, sem consulta
a spelling de COPY, nome do programa ou caminho. EOF não fornece conteúdo faltante
nem fechamento de storage. FactLocalitySemantics aplica as mesmas relações R2 de
REGION_CONTEXT/REGION_CLOSURE/DECLARATION_CONTEXT e SP 2.40.0 permanece inalterado.
O construtor interno sem evidência de integridade não licencia a nova prova EOF.

A qualificação percorre apenas as unidades top-level para a única candidata final,
e usa a consulta de provenance indexada. Não há enumeração de caminhos ou varredura
de nós por gap. O custo de associação existente das regiões SourceMap permanece.
Evidência: EofUnitBoundaryTest, FactDependencyLocalityTest e controles de provenance.
