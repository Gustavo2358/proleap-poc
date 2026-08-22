# ANTLR Parse Tree Atlas

Explorador visual e interativo da parse tree produzida por `Cobol.g4`. O projeto é autocontido: contém cópias da gramática escolhida, do programa selecionado, dos copybooks disponíveis e da infraestrutura de normalização/preprocessamento criada no benchmark.

## Por que `COACTUPC.cbl`?

No resultado do benchmark com a gramática ProLeap, ele é o maior programa do corpus e gera a maior árvore:

- 4.236 linhas originais;
- 57.227 nós;
- 37.736 tokens;
- profundidade máxima 39;
- 51 ocorrências de `GO TO`, 64 de `PERFORM` e 10 nós `evaluateStatement`.

Três COPYs de sistema/aplicação não estão presentes no corpus original. Como no benchmark, elas viram comentários diagnósticos; o parser principal ainda reconhece o programa sem erros léxicos ou sintáticos.

## Gerar e abrir

Requisitos: JDK 17+ e Maven 3.9+.

```bash
./run.sh
```

Depois abra `dist/index.html`. O explorador funciona diretamente via `file://`, sem servidor e sem dependências web externas.

Também é possível executar manualmente:

```bash
mvn compile exec:java
```

Argumentos opcionais do gerador:

```bash
mvn exec:java -Dexec.args="--source corpus/cbl/COACTUPC.cbl --copybooks corpus/cpy --output dist"
```

## Como ler a interface

- **Árvore**: navegação hierárquica virtualizada pelos 57 mil nós, com busca por regras e tokens.
- **Regras**: frequência das regras da gramática; revela as camadas intermediárias criadas pelo parser.
- **Fluxo**: índice dos nós `GO TO`, `PERFORM`, `EVALUATE` e `IF`, sempre com retorno ao contexto da árvore.
- **Código**: trecho sincronizado do fonte normalizado/pré-processado.
- **Inspetor**: tipo runtime, profundidade, intervalo de tokens, filhos e breadcrumb do nó selecionado.

O painel de fluxo é deliberadamente um índice da **sintaxe**, não um CFG. O ANTLR fornece o nó do `GO TO` e os tokens do destino, mas não resolve automaticamente uma aresta até o parágrafo-alvo. Essa ligação pertence à etapa posterior de AST/CFG.

## Estrutura

```text
antlr-parse-tree-explorer/
├── corpus/                    # cópia isolada do programa e copybooks
├── src/main/antlr4/           # Cobol.g4 + CobolPreprocessor.g4
├── src/main/java/             # preprocessamento copiado + exportador da árvore
├── src/main/resources/web/    # interface estática
├── dist/                      # resultado gerado, pronto para abrir
├── pom.xml
└── run.sh
```

`tree-data.js` usa uma representação plana da árvore. Cada nó guarda seu `id`, pai, tipo, nome, posição, intervalo de tokens, profundidade e quantidade de filhos. A interface reconstrói as relações em memória e renderiza somente as linhas visíveis, evitando criar dezenas de milhares de elementos DOM simultaneamente.
