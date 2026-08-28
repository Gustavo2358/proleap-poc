# Política de desempenho

Desempenho é importante para programas COBOL grandes, mas uma otimização deve preservar o problema semântico resolvido. Limitar candidatos, ignorar construções incomuns, parar busca por conveniência, descartar ambiguidade ou substituir AST por regex não são otimizações; são mudanças semânticas que exigem decisão explícita.

## Estratégia

Preferir índices por identificador canônico, mapas imutáveis pré-computados, caching, memoization, worklists, menos travessias de AST, alocação consciente e fatos calculados uma vez por unidade de programa quando a semântica permitir.

Revisar explicitamente algoritmos com forma suspeita, como:

```text
O(nodes × symbols)
O(references × all declarations)
O(blocks²)
O(paths)
```

Para CFG e dataflow futuros, preferir algoritmos de grafo e worklists conhecidos a enumeração exponencial de caminhos.

## Verificação

O gate de desempenho valida propriedades algorítmicas e determinismo, não um tempo máximo dependente da máquina. A base atual inclui teste de escala que observa índices e cardinalidades da resolução. Medidas de tempo, memória ou volume de logging podem complementar investigação e relatórios, mas não devem virar teste funcional frágil sem contrato ambiental explícito.
