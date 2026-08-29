# WORK-GRAMMAR-001 — Especializar literais DFHRESP e DFHVALUE

## Resultado

A ambiguidade vinha da admissão dos tokens `DFHRESP` e `DFHVALUE` em
`cobolWord`, caminho comum de `qualifiedDataName -> tableCall -> identifier`.
A remoção somente desses dois tokens de `cobolWord` preservou as regras CICS
especializadas e os table calls COBOL reais.

## Evidência durável

`CicsExpressionGrammarInvariantTest` cobre 13 hosts derivados da gramática,
quatro formas CICS e o controle `MY-TABLE(IDX)`. O COACTUPC acrescenta evidência
E2E: sete `DFHRESP(NORMAL)` e três `DFHRESP(NOTFND)` permanecem especializados
na parse tree e literals na AST, sem ocorrências ou gaps nominais equivalentes.

Os antigos totais exatos de nós, referências e gaps foram retirados dos oracles.
Eles continuam visíveis na saída da aplicação como telemetria, enquanto o gate
inspeciona categorias semânticas e preserva table calls reais como controle
negativo.

## Verificação

O gate `semantic` passou com 227 testes. O E2E do `full` regenerou os artefatos
do COACTUPC e passou CICS-EXPRESSION-001. O agregado `full` terminou vermelho
somente no check de naming por um identificador legado preexistente em
`docs/work/history/WORK-TEST-001.md`, fora do escopo desta mudança.
