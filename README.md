# COBOL Structure Atlas

Explorador visual e interativo da jornada `parse tree → AST → symbol table → resolução de referências` produzida a partir de `Cobol.g4`. O projeto é autocontido: contém cópias da gramática escolhida, do programa selecionado, dos copybooks disponíveis e da infraestrutura de normalização/preprocessamento criada no baseline.

## Por que `COACTUPC.cbl`?

No resultado do baseline com a gramática COBOL, ele é o maior programa do corpus e gera a maior árvore:

- 4.236 linhas originais;
- 57.227 nós;
- 37.682 tokens;
- profundidade máxima 39;
- 51 ocorrências de `GO TO`, 64 de `PERFORM` e 10 nós `evaluateStatement`.

Três COPYs de sistema/aplicação não estão presentes no corpus original. Como no baseline, elas viram comentários diagnósticos; o parser principal ainda reconhece o programa sem erros léxicos ou sintáticos.

## Gerar e abrir

Requisitos: JDK 17+ e Maven 3.9+.

```bash
./run.sh
```

Depois abra:

- `dist/index.html` para a parse tree;
- `dist/ast.html` para a AST semântica;
- `dist/symbols.html` para a tabela de símbolos;
- `dist/resolution.html` para os bindings nominais e a cobertura conservadora.

As páginas funcionam diretamente via `file://`, sem servidor e sem dependências web externas. Os inspetores permitem navegar entre um nó da AST e sua origem na parse tree.

Também é possível executar manualmente:

```bash
mvn compile exec:java
```

## Contrato de source format e provenance

O frontend aceita fonte COBOL em formato fixo por uma fronteira explícita. O
arquivo bruto origina o `SourceMap` antes de qualquer transformação; normalização,
COPY e preprocessing compõem esse mapa, sem recriar um mapa `identity` no meio do
pipeline.

- colunas 1–6 são sequence area, coluna 7 é indicator area, colunas 8–72 são
  program text e o restante é identification area;
- LF, CRLF e CR são preservados exatamente; conteúdo Unicode é preservado
  verbatim e offsets/colunas são contados por code point. Separadores Unicode
  de linha e tabs ambíguos no program text falham localmente;
- blank, `*`, `/`, `-` e `D`/`d` possuem políticas fechadas. Debug lines são
  incluídas ou excluídas por opção explícita; indicador desconhecido não possui
  fallback;
- continuação usa estado lexical para literais com aspas simples/duplas e
  palavras COBOL. Continuação órfã, após comentário ou em categoria não
  implementada produz diagnóstico localizado;
- comment entries são derivados dos parágrafos que a gramática declara,
  delimitados por Area A e por `END-REMARKS` quando aplicável. Pontos no texto
  não encerram a entrada, e nenhuma regex global decide seu estado;
- texto transformado preserva seus registros físicos e recebe `exact=false`.
  Conteúdo intocado e line endings mantêm segmentos exatos, inclusive dentro
  de COPYs aninhados.

O preprocessor também é fail-closed: cada alternativa top-level de
`CobolPreprocessor.startRule` possui política testada contra o parser gerado.
`EJECT`, `SKIP1/2/3` e `TITLE` são branqueados preservando quebras de linha;
EXECs são preservados de forma opaca; compiler options são extraídas; COPY é
expandido. `REPLACE` top-level permanece deliberadamente unsupported e falha na
fronteira de preprocessing, em vez de chegar silenciosamente ao parser COBOL.

O gate de regressão executa a suíte Maven e a aplicação real para o caso
canônico, a regressão de comment entry e COPYs com normalização aninhada:

```bash
MAVEN_BIN="${MAVEN_BIN:-mvn}" ./scripts/source-normalizer-regression.sh manual
```

O comando verifica erros de lexer/parser, baseline semântico, artefatos web,
provenance e ausência de comment entries na AST semântica.

Para verificar que a identidade do projeto não regrediu, execute:

```bash
./scripts/verify-naming.sh
```

Argumentos opcionais do gerador:

```bash
mvn exec:java -Dexec.args="--source corpus/cbl/COACTUPC.cbl --copybooks corpus/cpy --output dist"
```

## Logging do analisador

O logging usa SLF4J com Logback. A configuração padrão mantém `root=WARN` e
`ExplorerMain=INFO`: uma execução normal mostra apenas o lifecycle de baixa
cardinalidade e degradações relevantes. O formato inclui `runId`, `source` e
`programUnit` via MDC, além de timestamp, nível, logger e thread.

Os níveis podem ser alterados sem recompilar, por propriedade de sistema ou
variável de ambiente. Para observar métricas de todas as fases instrumentadas no
boundary principal:

```bash
mvn exec:java -DANALYZER_LOG_LEVEL=DEBUG
```

Para tracing seletivo de decisões, use a propriedade da classe de interesse:

```bash
mvn exec:java -DPREPROCESSOR_LOG_LEVEL=TRACE
mvn exec:java -DRESOLVER_LOG_LEVEL=TRACE
```

Também estão disponíveis `NORMALIZER_LOG_LEVEL`,
`ANTLR_DIAGNOSTIC_LOG_LEVEL`, `AST_LOG_LEVEL`, `SYMBOL_LOG_LEVEL` e
`REFERENCE_COLLECTOR_LOG_LEVEL`. Uma configuração Logback externa completa pode
ser escolhida com `-Dlogback.configurationFile=/caminho/logback.xml`; nunca é
necessário habilitar `root=TRACE`.

O pipeline atual é síncrono. Se no futuro houver execução assíncrona ou
multithread, o mapa MDC precisará ser propagado explicitamente para cada tarefa.

Os três caminhos são efetivamente usados pelo pipeline; `COACTUPC.cbl` e `dist` são apenas defaults. Cada execução analisa um programa-fonte e pode escrever em uma pasta independente, permitindo manter várias jornadas lado a lado. Por exemplo, o candidato com mais `CALLs` do corpus foi gerado com:

```bash
mvn compile exec:java \
  -Dexec.args="--source corpus/cbl/CBSTM03A.CBL --copybooks corpus/cpy --output dist-cbstm03a"
```

Abra `dist-cbstm03a/ast.html` e selecione a aba **CALLs**. Esse programa contém 14 call sites estáticos: 13 para `CBSTM03B` e um para `CEE3ABD`. O corpus atual não contém `CALL` cujo alvo seja uma variável; portanto, `dynamicCalls` continua zero nesse segundo exemplo.

### Variante didática com CALLs dinâmicos

`corpus/cbl/CBSTM03D.CBL` é uma variante isolada de `CBSTM03A.CBL` criada para preparar uma futura demonstração de reaching definitions. Os 14 call sites usam a mesma variável `WS-CALL-TARGET`, inicialmente preenchida com `SPACES`. Dois `MOVE`s mínimos reproduzem a sequência original de alvos: 13 chamadas para `CBSTM03B`, seguidas por uma chamada para `CEE3ABD`.

Gere sua jornada visual sem alterar as saídas anteriores:

```bash
mvn compile exec:java \
  -Dexec.args="--source corpus/cbl/CBSTM03D.CBL --copybooks corpus/cpy --output dist-cbstm03d"
```

Abra `dist-cbstm03d/index.html`, `ast.html`, `symbols.html` e `resolution.html`. A AST mostra 14 CALLs dinâmicos e nenhum estático; a tabela de símbolos mostra a declaração única de `WS-CALL-TARGET`; a resolução liga os 14 usos à declaração dessa variável. A descoberta dos valores possíveis da variável permanece deliberadamente fora deste passo: ela dependerá do futuro CFG e da análise de fluxo.

Na página `ast.html`, a aba **Cobertura** explica se o programa está suficientemente
coberto para análise de dependências. Construções preservadas, linguagens
embutidas e COPYs ausentes aparecem como lacunas navegáveis; nunca são
convertidas silenciosamente em “nenhuma dependência”.

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

Na etapa **Symbol Table**:

- **Escopos**: raiz, programa, divisions, sections, descrições de arquivo, grupos de dados e paragraphs;
- **Namespaces**: `PROGRAM`, `DATA`, `PROCEDURE` e `FILE`, separados para preparar as regras de lookup COBOL;
- **Símbolos**: programa, `SELECT`, `FD`, data items, condition names de nível 88, procedure sections e paragraphs;
- **Identidade**: nome escrito, forma canônica case-insensitive, tipo, escopo declarador e nó de origem na AST;
- **Ambiguidades preservadas**: declarações repetidas no mesmo namespace e escopo geram diagnósticos, sem uma escolha prematura.

Na etapa **Resolução de referências**:

- **Ocorrências tipadas**: cada uso conserva unidade, kind, role, regra da gramática, texto escrito, span e proveniência;
- **Decisões explicáveis**: `RESOLVED`, `AMBIGUOUS`, `UNRESOLVED` e `UNSUPPORTED` sempre aparecem com motivo e todos os candidatos válidos;
- **Política explícita**: dialeto, modo de `QUALIFY` e presença ou ausência do catálogo externo fazem parte do snapshot;
- **Pontes**: cada binding volta ao nó da AST, à parse tree, ao fonte e, quando aplicável, aos símbolos candidatos;
- **Cobertura conservadora**: COPY ausente, input inválido, construção opaca ou referência sem binding bloqueiam a alegação de análise completa;
- **Fronteira de dataflow**: resolver `CALL WS-CALL-TARGET` identifica a variável, não os valores de programa que ela poderá conter em runtime.

O painel de fluxo é deliberadamente um índice da **sintaxe**, não um CFG. O ANTLR fornece o nó do `GO TO` e os tokens do destino; a etapa de resolução pode ligá-lo à identidade do parágrafo, mas ainda não constrói arestas de fluxo entre blocos básicos.

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

## Contrato da tabela de símbolos e da resolução

`SymbolTableBuilder` depende apenas de `Ast.Program`: não conhece ANTLR, tokens ou classes do parser. Ele coleta declarações, reconstrói escopos e normaliza nomes COBOL em maiúsculas com locale neutro. O resultado em `SymbolTable` é imutável e oferece lookup local/lexical que mantém múltiplos candidatos visíveis.

`ReferenceOccurrenceCollector` percorre referências tipadas da AST sem fazer lookup. `CobolReferenceResolver` consome as ocorrências e as tabelas namespaced por program unit e produz um `ReferenceResolution` separado e imutável. Portanto:

- o literal `CSUTLDTC` de um `CALL` não vira símbolo local;
- targets de `GO TO` e `PERFORM` podem apontar para candidatos de procedure no produto de resolução;
- `DataReference` pode apontar para candidatos DATA/CONDITION/INDEX no produto de resolução;
- não há CFG, reaching definitions, constant resolution ou análise SQL.

A AST e a tabela continuam livres de `symbolId`, candidatos e status. O binding de uma variável usada em `CALL` não descobre os seus valores possíveis nem produz fatos finais de dependência.

Resultados atuais para `COACTUPC.cbl`:

- parse tree: 57.227 nós, profundidade 39;
- AST: 9.189 nós, profundidade 11;
- tabela: 853 declarações em 651 escopos;
- 492 data items, 259 condition names e 101 paragraphs;
- dois diagnósticos de nomes de dados repetidos no mesmo escopo, preservados como candidatos;
- um `CALL` literal para `CSUTLDTC`;
- zero `CALLs` dinâmicos;
- 14 statements de linguagem embutida preservados de forma opaca.

## Estrutura

```text
antlr-parse-tree-explorer/
├── corpus/                    # cópia isolada do programa e copybooks
├── src/main/antlr4/           # Cobol.g4 + CobolPreprocessor.g4
├── src/main/java/             # frontend + AST + Symbol Table + Reference Resolution + exportadores
├── src/main/resources/web/    # jornada visual Parse Tree → AST → Symbol Table → Resolução
├── dist/                      # resultado gerado, pronto para abrir
├── pom.xml
└── run.sh
```

`tree-data.js`, `ast-data.js`, `symbol-data.js` e `resolution-data.js` usam representações planas para a interface. A interface reconstrói as relações em memória; os modelos Java permanecem tipados e imutáveis. As formas planas são apenas DTOs de visualização.
