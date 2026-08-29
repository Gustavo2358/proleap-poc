# WORK-RES-002 — Veredito sobre W3D-AUX e categorias de resolução

## Veredito executivo

O report anexo está **parcialmente confirmado na revisão atual**:

1. A explicação arquitetural do defeito histórico de `W3D-AUX` é correta: usar o primeiro `dataName` descendente confunde declaration site com reference site. Porém, esse defeito **já estava corrigido** no commit `3cd4c96` antes desta investigação. O código atual usa os accessors tipados de `DataDescriptionEntryFormat1/2/3`; a suíte adversarial não reproduziu símbolo falso nem ambiguidade.
2. A categoria de index-names em relações estava **confirmadamente defeituosa**. O `ReferenceOccurrenceCollector` inferia relação pela string do operador. Cinco de quinze grafias válidas falharam no teste vermelho: `=`, `GREATER THAN`, `IS NOT GREATER THAN`, `LESS THAN` e `IS NOT LESS THAN`.
3. As contagens privadas atribuídas ao snapshot X0DB2 — 376 ambiguidades e 148 namespaces inválidos, bem como as projeções pós-correção — **não foram verificadas**, porque fonte e artefatos do programa não estavam disponíveis. Permanecem evidência reportada, não resultado reproduzido.

## Autoridade semântica

A documentação IBM separa o nome/FILLER que identifica o item redefinidor do `data-name-2` que é objeto de REDEFINES; omitir o nome trata a entrada como FILLER. Ela também permite index-name como operando de condição relacional:

- [IBM Enterprise COBOL — REDEFINES clause](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-redefines-clause)
- [IBM Enterprise COBOL — Format 1 data description entry](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=entry-format-1)
- [IBM Enterprise COBOL — General relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=expressions-general-relation-conditions)
- [IBM Enterprise COBOL — Comparison of index-names and index data items](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=conditions-comparison-index-names-index-data-items)

## Evidência adversarial

### DATA anônima e W3D-AUX

`AstBuilderTypedTraversalTest` cobre FILLER explícito e implícito com nomes descendentes em `REDEFINES`, `OCCURS DEPENDING ON`, `OCCURS KEY` e `USING`. Cinco entradas anônimas permaneceram `FILLER`; cada nome declarativo real apareceu uma única vez.

`filler-redefines-owner.cbl` executa o pipeline AST → símbolos → ocorrências → resolução para o formato mínimo de W3D. O oracle exige exatamente um símbolo `W3D-AUX` e um único candidato `RESOLVED` para `MOVE SPACES TO W3D-AUX`.

### Grafias relacionais

`index-name-relational-operators.cbl` cobre quinze formas aceitas pela produção `relationalOperator`: símbolos, palavras, `IS`, `NOT` e formas compostas. Antes da correção, 5/15 terminavam `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`; depois da correção, 15/15 têm `admissibleKinds={DATA, INDEX}` e selecionam o único `INDEX_NAME`.

A relação metamórfica protegida é: trocar somente a grafia do operador por outra alternativa da mesma produção não altera a categoria da expressão nem os namespaces admissíveis dos operandos.

## Solução implementada

`Ast.OperationExpression` passou a carregar `OperationCategory`. `AstBuilder.conditionExpression` marca toda `RelationArithmeticComparisonContext` como `RELATIONAL` e preserva separadamente a grafia em `operator`/`writtenText`. `ReferenceOccurrenceCollector` consulta apenas a categoria tipada; o helper textual `isRelationalOperator` foi removido. O snapshot expõe `category` para auditoria.

Essa solução mantém as fronteiras do pipeline: a AST classifica a sintaxe reconhecida; o coletor define os tipos admissíveis; o resolver continua responsável por candidates, visibilidade e decisão nominal.

## Mutation testing

O perfil Maven `mutation-adversarial` usa PIT 1.21.0 com o plugin JUnit 5 1.2.3, focalizado em `AstBuilder` e `ReferenceOccurrenceCollector` contra três classes de oracle.

Resultado final:

- 900 mutantes gerados; 502 mortos; 238 sem cobertura; força dos testes cobertos de 76%;
- região `buildDataEntry`: 16 mutantes no domínio suportado mortos, zero sobreviventes; 2 sem cobertura pertencem à alternativa EXEC SQL opaca, fora deste slice;
- lowering da relação: 3/3 mutantes mortos;
- coleta de operandos relacionais: 3/3 mutantes mortos, incluindo inversão da categoria e remoção da visita;
- nenhum threshold global foi introduzido: sobreviventes fora do slice continuam visíveis como backlog, sem serem apresentados como cobertura deste incidente.

Comando reproduzível:

```bash
mvn -Pmutation-adversarial test-compile org.pitest:pitest-maven:mutationCoverage
```

O HTML/XML local é gerado em `target/pit-reports/` e não é versionado.

## Verificação e limites

- `./scripts/harness/check-fast.sh`: passou.
- `./scripts/harness/check-semantic.sh`: passou.
- `./scripts/harness/check-full.sh`: passou.
- PIT focalizado: passou, sem mutante sobrevivente nas decisões do incidente.

Não foi alterada a gramática, a política de lookup, a visibilidade COBOL ou qualquer baseline privado. O efeito numérico no X0DB2 exige regeneração autorizada no ambiente que contém esse programa.

