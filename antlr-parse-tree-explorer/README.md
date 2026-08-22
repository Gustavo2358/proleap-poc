# COBOL Structure Atlas

Explorador visual e interativo da jornada `parse tree → AST` produzida a partir de `Cobol.g4`. O projeto é autocontido: contém cópias da gramática escolhida, do programa selecionado, dos copybooks disponíveis e da infraestrutura de normalização/preprocessamento criada no benchmark.

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

Depois abra:

- `dist/index.html` para a parse tree;
- `dist/ast.html` para a AST semântica.

As páginas funcionam diretamente via `file://`, sem servidor e sem dependências web externas. Os inspetores permitem navegar entre um nó da AST e sua origem na parse tree.

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

Na etapa **AST**:

- **Estrutura**: programa, divisions, sections, paragraphs, sentences, statements e expressions;
- **Tipos**: frequência dos conceitos definidos pelo nosso domínio;
- **CALLs**: separação entre targets literais e expressions dinâmicas;
- **Origem**: regra e nó da parse tree que deram origem a cada nó semântico;
- **Compressão didática**: tamanho da região sintática condensada por cada conceito.

O painel de fluxo é deliberadamente um índice da **sintaxe**, não um CFG. O ANTLR fornece o nó do `GO TO` e os tokens do destino, mas não resolve automaticamente uma aresta até o parágrafo-alvo. Essa ligação pertence à etapa posterior de AST/CFG.

## Contrato da AST neste MVP

A AST é única, imutável e não contém tabela de símbolos ou resultados de análise. Ela modela:

- programa e quatro divisions;
- file bindings e data entries necessárias para evolução futura;
- sections, paragraphs e sentences;
- ponto final como `SentenceTerminator.PERIOD`, não como token;
- `CALL`, `IF`, `EVALUATE`, `PERFORM`, `GO TO`, `MOVE` e `NEXT SENTENCE`;
- literals, data references e expressions ainda não especializadas;
- statements não modelados como `UnsupportedStatement`, preservando texto e statements aninhados;
- `EXEC SQL`, `EXEC CICS` e `EXEC SQLIMS` como `EmbeddedLanguageStatement` opaco.

O último item é o ponto de extensão para um MVP futuro. O payload original e sua origem já ficam preservados; um plugin poderá parsear SQL sem alterar o núcleo da AST COBOL.

Resultados atuais para `COACTUPC.cbl`:

- parse tree: 57.227 nós, profundidade 39;
- AST: 4.100 nós, profundidade 8;
- um `CALL` literal para `CSUTLDTC`;
- zero `CALLs` dinâmicos;
- 14 statements de linguagem embutida preservados de forma opaca.

## Estrutura

```text
antlr-parse-tree-explorer/
├── corpus/                    # cópia isolada do programa e copybooks
├── src/main/antlr4/           # Cobol.g4 + CobolPreprocessor.g4
├── src/main/java/             # preprocessamento + AstBuilder + exportadores
├── src/main/resources/web/    # jornada visual Parse Tree → AST
├── dist/                      # resultado gerado, pronto para abrir
├── pom.xml
└── run.sh
```

`tree-data.js` e `ast-data.js` usam representações planas para a interface. Cada nó guarda identidade, pai, tipo, posição, profundidade e origem. A interface reconstrói as relações em memória e renderiza somente as linhas visíveis, evitando criar milhares de elementos DOM simultaneamente. O modelo Java permanece tipado em `Ast.java`; a forma plana é apenas um DTO de visualização.
