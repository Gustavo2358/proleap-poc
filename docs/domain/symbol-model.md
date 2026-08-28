# Modelo de símbolos

## Propósito

Symbol tables materializam declarations, structural scopes, namespaces, semantic entities e declaration relations para cada program unit. Elas não coletam usos nem realizam binding.

## Entradas e saídas

- **Entrada:** um `Ast.Program` por unit.
- **Saída:** `SymbolTable` imutável e `CompilationUnitSymbolTables` namespaced por `ProgramUnitId`.

## Modelo atual

Namespaces são `PROGRAM`, `DATA`, `PROCEDURE` e `FILE`. Symbols distinguem programas, `SELECT`, `FD`/`SD`, data items, condition names 88, RENAMES 66, index names, procedure sections e paragraphs. Scopes representam root, program, divisions, sections, file descriptions, data items e paragraphs.

Uma entidade FILE agrupa declarações compatíveis como `SELECT` e `FD`/`SD`, preservando aliases e attributes sem criar ambiguidade artificial. Declaration relations registram `REDEFINES`, `RENAMES`, `OCCURS DEPENDING ON`, keys e indexes com owner e referência ainda `NOT_PERFORMED`.

## Lookup e visibilidade

Nomes mantêm grafia e forma canônica case-insensitive. Índices locais/globais preservam listas de candidatos, inclusive duplicatas relevantes. Lookup estrutural auxilia a resolução, mas não é a relação completa de visibilidade COBOL; o resolver aplica unit ancestry, namespace, kind, qualification e regras do dialeto.

## Fronteiras e diagnostics

Symbol table não infere runtime values, não escolhe candidato para ocorrência e não depende de classes ANTLR. Duplicatas no mesmo scope/namespace geram diagnostics determinísticos em vez de seleção prematura.

## Complexidade

Índices por scope/namespace/nome e por visibilidade global evitam `O(references × all declarations)`. A construção percorre a AST da unit e materializa listas imutáveis.

## Evidência executável

`SymbolTableBuilderTest`, `EntityScopeAndOccurrenceTest`, `CompilationUnitModelTest`, `DeclarationModelAstTest` e fixtures de declarations/entities.

## Relações

Evals: EVAL-SYM-001, EVAL-SYM-002, EVAL-RES-REL-001, EVAL-RES-PERF-001 e EVAL-ARCH-001. Invariantes: INV-SYM-001, INV-RES-001, INV-DET-001 e INV-PERF-001. ADRs: ADR-0003 e ADR-0005.
