## O que prova corretude

Parse trees concretas distinguem os contexts CICS dedicados de `tableCall` pelo mesmo intervalo, e o pipeline não inventaria referências nominais para os argumentos CICS.

## Classes positivas

`DFHRESP` e `DFHVALUE` com mais de um argumento válido, em todos os hosts compartilhados derivados da gramática.

## Classes negativas

`MY-TABLE(IDX)` nos mesmos hosts deve permanecer `TableCallContext` e produzir referências nominais usuais.

## Classes ambíguas

Inputs cujo prefixo tokenizado permite tanto literal CICS quanto `cobolWord`/`tableCall` antes da correção.

## Casos adversariais

Argumentos diferentes de `NORMAL`, variação entre `DFHRESP` e `DFHVALUE` e table call com a mesma forma parentizada.

## Casos de regressão

Suíte Maven, corpus, grammar coverage, AST, resolução e regressão upstream/NIST quando já disponível.

## Propriedades/relações metamórficas

Substituir `MY-TABLE(IDX)` por `DFHRESP(NORMAL)` preserva o host estrutural e muda somente a categoria da expressão inserida.

## Expectativas de escala

Matriz finita derivada das regras host; nenhuma dependência do tamanho ou conteúdo do corpus.
