# COBOL Grammar Bake-off

## 1. Executive Summary

grammars-v4 Cobol85 preprocessed 14/31 and parsed 30/31; ProLeap Cobol preprocessed 14/31 and parsed 31/31. A recomendação considera cobertura antes de desempenho e vale **segundo este corpus e este experimento**.

**RECOMMENDATION: Cobol.g4**

## 2. Question Being Evaluated

Qual par grammar + preprocessor é a melhor baseline sintática para `preprocessing → ANTLR parse tree → AstBuilder próprio`, sem ASG, metamodelo ou analisadores ProLeap.

## 3. Candidates

### grammars-v4 Cobol85

`Cobol85Preprocessor.g4` → `Cobol85.g4`. Ambos são combined grammars, entry rule `startRule`.

### ProLeap Cobol

`CobolPreprocessor.g4` → `Cobol.g4`. Também combined grammars, entry rule `startRule`; nenhuma dependência `proleap-cobol*` foi usada.

Nenhum dos quatro arquivos declara `tokenVocab`, modes, semantic predicates, actions, opções especiais ou Java embutido. Listeners/visitors gerados são opcionais; o runner usa a API genérica de parse tree. Não há dependência externa de grammar.

## 4. Environment

- Benchmark date: 2026-08-21

- java.version: 25.0.4

- java.vendor: Eclipse Adoptium

- os.name: Linux

- os.version: 7.0.0-27-generic

- os.arch: amd64

- ANTLR tool/runtime: 4.13.2 for both candidates

- Maven: 3.9.16

- Java compilation target: 17

## 5. Corpus

- Programs: 31

- Copybooks supplied: 30

- Program LOC / bytes: 20650 / 1155526

- Copybook LOC / bytes: 2786 / 133714

- Min / median / max program LOC: 41 / 494 / 4236

Source format: **FIXED**. A inspeção da coluna 7 encontrou indicadores de comentário (`*`) e 11 continuações (`-`); não foi detectado arquivo FREE. O normalizador remove sequence area (1–6), interpreta a coluna 7 e limita a área de programa a 8–72.

Caracterização textual (número de programas contendo ao menos uma ocorrência; não usada semanticamente):

| Construct | Files |
|---|---:|
| COPY | 28 |
| CALL | 17 |
| PERFORM | 30 |
| GO TO | 8 |
| IF | 29 |
| EVALUATE | 22 |
| EXEC SQL | 0 |
| EXEC CICS | 17 |
| EXEC SQLIMS | 0 |
| REDEFINES | 16 |
| OCCURS | 20 |

## 6. Methodology

Cada frontend implementa a mesma interface `CobolFrontend`. Foram executados 2 warmups e 5 runs medidos por frontend, sequencialmente. Tempos usam `System.nanoTime`; os CSVs por arquivo guardam a mediana. Estatísticas do corpus inteiro usam soma por run e depois mediana, p95 e média. Success exige zero lexer/parser syntax errors; uma árvore recuperada com erros é FAILED. Linhas de diagnostics do parser referem-se ao texto normalizado/expandido; diagnostics de COPY usam a posição no arquivo que contém a diretiva.

O preprocessing é parse-tree-driven: a grammar correspondente reconhece `COPY`, `REPLACING` e `EXEC`; uma transformação comum resolve nomes case-insensitively em `cpy/`, tenta `.cpy`/`.CPY`, expande recursivamente e aplica pseudo-text `REPLACING`. COPY ausente vira `unresolved_copy`, nunca é criado ou ignorado silenciosamente. As quatro grammars oficiais são copiadas sem modificação para `target/antlr4` durante o build.

## 7. Preprocessor Results

| Frontend | Preprocessed | ANTLR preprocessor errors | Unresolved COPY |
|---|---:|---:|---:|
| grammars-v4 Cobol85 | 14/31 | 0 | 51 |
| ProLeap Cobol | 14/31 | 0 | 51 |

A métrica `preprocessed` exige parse do preprocessor sem erro e zero COPY não resolvido. O parser principal ainda é executado após substituir COPY ausente por comentário, permitindo separar capacidade da grammar de disponibilidade da biblioteca.

COPYs ausentes (nomes únicos): `COACTUP`, `COACTVW`, `COADM01`, `COBIL00`, `COCRDLI`, `COCRDSL`, `COCRDUP`, `COMEN01`, `CORPT00`, `COSGN00`, `COTRN00`, `COTRN01`, `COTRN02`, `COUSR00`, `COUSR01`, `COUSR02`, `COUSR03`, `DFHAID`, `DFHBMSCA`. São layouts BMS/IBM ou copybooks de aplicação não fornecidos; nenhum arquivo sintético foi adicionado.

`EXEC CICS`, `EXEC SQL` e `EXEC SQLIMS` são preservados como texto opaco em tokens marcados (`*>EXECCICS`, `*>EXECSQL`, `*>EXECSQLIMS`) e aparecem sob regras `exec*Statement`; não há análise da DSL. Os dois pares usam esse mesmo contrato léxico.

## 8. Parser Correctness Results

| Frontend | Parse success | Lexer errors | Parser errors | Errors/KLOC |
|---|---:|---:|---:|---:|
| grammars-v4 Cobol85 | 30/31 | 0 | 6 | 0.291 |
| ProLeap Cobol | 31/31 | 0 | 0 | 0.000 |

Arquivos problemáticos:

**grammars-v4 Cobol85**

- `COACTUPC.cbl`: 0 lexer + 6 parser errors; first: extraneous input ',' expecting {ABORT, ADDRESS, ALL, AS, ASCII, ASSOCIATED_DATA, ASSOCIATED_DATA_LENGTH, ATTRIBUTE, AUTO, AUTO_SKIP, BACKGROUND_COLOR, BACKGROUND_COLOUR, BEEP, BELL, BINARY, BIT, BLINK, BOUNDS, CAPABLE... near `,`.

**ProLeap Cobol**

- Nenhuma falha de parser.

## 9. Performance Results

**grammars-v4 Cobol85**: preprocessing median/p95/mean 627.259 / 628.503 / 626.697 ms; parsing 12564.469 / 12641.853 / 12559.891 ms; total 13191.408 / 13270.356 / 13186.588 ms; 1565.413 LOC/s.

**ProLeap Cobol**: preprocessing median/p95/mean 628.763 / 631.829 / 628.335 ms; parsing 12901.714 / 12988.897 / 12927.598 ms; total 13533.543 / 13613.506 / 13555.932 ms; 1525.838 LOC/s.

As medidas incluem lexing no tempo de parsing. Performance é secundária à correção; diferenças pequenas não mudam o veredito.

## 10. Memory Results

| Frontend | Max observed heap bytes |
|---|---:|
| grammars-v4 Cobol85 | 432603248 |
| ProLeap Cobol | 445794800 |

O valor é **pico de heap observado**, amostrado aproximadamente a cada 1 ms com `totalMemory-freeMemory`; não é RSS nem garantia do pico real. Não foi usado `System.gc()`. Como os frontends rodam no mesmo JVM, retenção/JIT pode influenciar a comparação.

## 11. Parse Tree Characteristics

| Frontend | Tokens | Tree nodes | Max depth |
|---|---:|---:|---:|
| grammars-v4 Cobol85 | 160695 | 247639 | 42 |
| ProLeap Cobol | 160695 | 247666 | 42 |

Árvore menor ou menos profunda não implica automaticamente grammar melhor. Ambas expõem regras nomeadas para statements e clauses, adequadas a visitor próprio.

## 12. Qualitative Syntax Analysis

MOVE, CALL, IF, EVALUATE, PERFORM, COMPUTE, SET, STRING, UNSTRING e INSPECT aparecem no corpus e são nós explícitos em ambas. COPY desaparece após expansão, corretamente pertencendo à fase de preprocessing. REDEFINES e OCCURS aparecem como clauses explícitas. A estrutura das duas grammars é muito próxima; `Cobol85.g4` é mais extensa, enquanto `Cobol.g4` contém permissividades úteis (por exemplo, ponto opcional em `paragraph` e zero ou mais `programUnit`).

Amostras compactas reais (primeiro nó coletado, truncado):

**grammars-v4 Cobol85**

```text
dataDescriptionEntryFormat1: (dataDescriptionEntryFormat1 01 (dataName (cobolWord FD-ACCTFILE-REC)) .\n )
performStatement: (performStatement PERFORM (performProcedureStatement (procedureName (paragraphName (cobolWord 0000-ACCTFILE-OPEN)))))
ifStatement: (ifStatement IF (condition (combinableCondition (simpleCondition (relationCondition (relationArithmeticComparison (arithmeticExpression (multDivs (powers (basis (identifier (qualifiedDataName (qualifiedDataNameFormat1 (dataName (cobolWord END-OF-FILE))))))))) (relationalOperator =) (arithmeticExpression (multDivs (powers (basis (literal 'N')))))))))) (ifThen (statement (performStatement PERFORM (performProcedureStatement (procedureName (paragraphName (cobolWord 1000-ACCTFILE-GET-NEXT)))))) (stat
moveStatement: (moveStatement MOVE (moveToStatement (moveToSendingArea (literal (numericLiteral (integerLiteral 0)))) TO (identifier (qualifiedDataName (qualifiedDataNameFormat1 (dataName (cobolWord APPL-RESULT)))))))
callStatement: (callStatement CALL (literal 'COBDATFT') (callUsingPhrase USING (callUsingParameter (callByReferencePhrase (callByReference (identifier (qualifiedDataName (qualifiedDataNameFormat1 (dataName (cobolWord CODATECN-
```

**ProLeap Cobol**

```text
dataDescriptionEntryFormat1: (dataDescriptionEntryFormat1 01 (dataName (cobolWord FD-ACCTFILE-REC)) .\n )
performStatement: (performStatement PERFORM (performProcedureStatement (procedureName (paragraphName (cobolWord 0000-ACCTFILE-OPEN)))))
ifStatement: (ifStatement IF (condition (combinableCondition (simpleCondition (relationCondition (relationArithmeticComparison (arithmeticExpression (multDivs (powers (basis (identifier (qualifiedDataName (qualifiedDataNameFormat1 (dataName (cobolWord END-OF-FILE))))))))) (relationalOperator =) (arithmeticExpression (multDivs (powers (basis (literal 'N')))))))))) (ifThen (statement (performStatement PERFORM (performProcedureStatement (procedureName (paragraphName (cobolWord 1000-ACCTFILE-GET-NEXT)))))) (stat
moveStatement: (moveStatement MOVE (moveToStatement (moveToSendingArea (literal (numericLiteral (integerLiteral 0)))) TO (identifier (qualifiedDataName (qualifiedDataNameFormat1 (dataName (cobolWord APPL-RESULT)))))))
callStatement: (callStatement CALL (literal 'COBDATFT') (callUsingPhrase USING (callUsingParameter (callByReferencePhrase (callByReference (identifier (qualifiedDataName (qualifiedDataNameFormat1 (dataName (cobolWord CODATECN-
```

## 13. Procedure Division / CFG Suitability

Ambas modelam `procedureDivisionBody → paragraphs + procedureSection*`, com `paragraph`, `sentence` e `statement` explícitos. IF separa `ifThen`/`ifElse`; EVALUATE separa selects, WHEN e OTHER; PERFORM separa inline de procedure e representa THRU; GO TO separa simple de DEPENDING ON. Isso permite identificar branches, alvos, fallthrough e limites de basic blocks sem parse textual. Entre elas, a árvore é essencialmente isomórfica; a escolha para CFG é dominada pela cobertura medida e pela permissividade de parágrafos, não por diferença estrutural grande.

## 14. Data Division / Symbol Resolution Suitability

Ambas usam `dataDescriptionEntryFormat1/2/3`: level e name são filhos diretos, seguidos por clauses nomeadas para PIC, VALUE, REDEFINES, OCCURS, USAGE e demais atributos; level 66 representa RENAMES e 88 condition names. Parentage de 01/05/10 precisa ser reconstruída pela pilha de níveis no AstBuilder (não é nesting estrutural), mas level/name/picture/value/redefines/occurs/usage são extraíveis sem heurística textual. Não surgiu vantagem estrutural decisiva entre as duas.

## 15. CALL Representation

Classificação textual das ocorrências: CALL literal=31, CALL variável=0, CALL USING=28, explicit BY REFERENCE=0, BY CONTENT=0, BY VALUE=0.

Nas duas grammars, `callStatement` contém alvo `(identifier | literal)`, `callUsingPhrase` e parâmetros separados em BY REFERENCE, BY VALUE e BY CONTENT. Portanto target, arguments e passing mode são diretamente visitáveis; as regras são praticamente idênticas.

## 16. Failure Analysis

COPYs ausentes são falha de preprocessing, não da grammar. O único parser failure residual ocorre em `COACTUPC.cbl`: seis vírgulas entre operands de dois `STRING`. `Cobol.g4` aceita `stringSending (COMMACHAR? stringSending)*`; `Cobol85.g4` exige `stringSending+` sem vírgula. É uma incompatibilidade localizada, não seis features distintas, e constitui `potential_local_patch`. Maior contagem em um arquivo: grammars-v4 Cobol85=6, ProLeap Cobol=0. Cobol85 apresentou uma cascata localizada pior; os seis diagnostics provêm do mesmo padrão sintático. As grammars oficiais permaneceram intactas.

## 17. Maintainability and Customization

`Cobol.g4` tem 3.269 linhas versus 5.654 de `Cobol85.g4`; o preprocessor correspondente tem 670 versus 1.902. Menor tamanho favorece leitura, mas `Cobol85` contém cobertura lexical/sintática adicional. Nenhuma tem actions ou dependências runtime, portanto ambas são forkáveis. `potential_local_patch`: adicionar `COMMACHAR?` entre operands de `stringSendingPhrase` em `Cobol85.g4`, espelhando a regra já presente em `Cobol.g4`; isso provavelmente resolve o único arquivo falho, mas não foi aplicado ao benchmark. Construir preprocessing próprio é tecnicamente razoável: resolução de bibliotecas, formato fixo, conditional compilation e políticas de COPY são preocupações do ambiente e já exigem código host, enquanto as grammars de preprocessor apenas reconhecem sintaxe.

## 18. Limitations of This Experiment

O corpus é pequeno, concentrado em uma aplicação CardDemo, com COPYs IBM/BMS ausentes; não representa todo COBOL empresarial. A normalização FIXED e transformação EXEC são código do spike, embora comuns aos candidatos. Medição no mesmo JVM não isola completamente JIT/GC. Constructs ausentes ou raros não autorizam conclusão ampla. O experimento não valida semântica, layout, SQL/CICS, AST ou CFG.

## 19. Verdict

RECOMMENDATION: Cobol.g4

### Why

1. Cobertura: 31/31 contra 30/31.
2. Syntax errors: 0 contra 6.
3. As árvores têm estrutura semelhante para AST/CFG/Data Division/CALL; portanto cobertura decide antes de microdiferenças de tempo.

### Main trade-offs

O candidato recomendado deve ser lido como a melhor baseline neste corpus. O outro pode ter regras pontualmente mais estritas ou cobertura de dialect features que mereçam cherry-pick posterior; o preprocessor pode ser substituído independentemente.

### What this does NOT prove

Não prova cobertura universal de COBOL, nem qualidade semântica, nem que patches locais devam ser incorporados antes de um corpus maior.

### Recommended architecture

```text
COBOL
  ↓
preprocessor próprio, usando as regras atuais como referência e mantendo COPY/EXEC diagnosticáveis
  ↓
Cobol.g4
  ↓
ANTLR Parse Tree
  ↓
our AstBuilder
  ↓
our AST
  ↓
our CFG
  ↓
our dataflow analyses
```

## 20. Recommended Next Step

Adicionar um corpus estratificado do banco (dialetos, DB2/CICS/IMS, copybooks de sistema e formatos mistos), congelá-lo, repetir sem patches e só depois criar uma branch experimental com cada `potential_local_patch`. Construir primeiro um AstBuilder fino para Data Division, CALL e control flow no candidato recomendado.

### Explicit answers to the evaluation questions

1. Preprocessing: 14/31 e 14/31; empate dos preprocessors.
2. Parsing sem syntax errors: 30/31 e 31/31.
3. Falha: `COACTUPC.cbl` apenas em Cobol85; Cobol não falhou.
4–5. Construct causador: `STRING` com vírgulas; é diferença da grammar. COPYs ausentes são preprocessing separado.
6. Cobol85 gerou seis diagnostics do mesmo padrão; Cobol, zero.
7. AstBuilder: estruturas quase isomórficas; Cobol.g4 vence pela cobertura.
8. CFG: ambas estruturam IF/EVALUATE/PERFORM/GO TO; Cobol.g4 é a baseline.
9. Data Division: empate estrutural; clauses nomeadas em ambas.
10. CALL: empate estrutural; target e passing modes explícitos.
11. EXEC DSLs: tratamento opaco equivalente, preservado em nós exec.
12. Mais rápida no total mediano: grammars-v4 Cobol85.
13. Menor pico de heap observado: grammars-v4 Cobol85 (com viés de ordem/JVM compartilhada).
14. A diferença de performance é pequena frente à diferença de correção e não altera a decisão.
15. `potential_local_patch`: vírgula opcional em `Cobol85.stringSendingPhrase`; não aplicado.
16. `Cobol.g4` é menor e, neste corpus, mais simples de manter como fork.
17. Faz sentido construir preprocessor próprio e manter as regras atuais como referência.
18. Baseline recomendada: **Cobol.g4**.
## Provenance

| File | SHA-256 | Local/upstream provenance |
|---|---|---|
| `Cobol85.g4` | `c338bff84b5a7d89113dacdff69764593688fd0915f24fba2f07a5fec2063e35` | Local candidate; header attributes Ulrich Wolffgang/proleap.io and names the associated upstream project |
| `Cobol85Preprocessor.g4` | `8d88a679ae574a2645c827c21f467031669e2713d149c8fec46bc0dab86b4841` | Local candidate; header attributes Ulrich Wolffgang/proleap.io and names the associated upstream project |
| `Cobol.g4` | `77460471863292add5a113698bcaa3c7ba0e239b446063a9cccd09f9a2fb908d` | Local candidate; header attributes Ulrich Wolffgang/proleap.io and names the associated upstream project |
| `CobolPreprocessor.g4` | `e78114b62294aba39f30fa1294082c6de4d099c1b521c5a8503f2b48853ed651` | Local candidate; header attributes Ulrich Wolffgang/proleap.io and names the associated upstream project |
