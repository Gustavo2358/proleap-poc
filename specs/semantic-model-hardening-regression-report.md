# Relatório de regressão — Semantic Model Hardening

## Estado

Concluído em 2026-08-23. A auditoria final e seus resultados foram aprovados
explicitamente pelo usuário; a Fase 8 encerra o semantic model hardening.

## Baseline registrado

- commit inicial: `ffa053f08884fa6c4913bf493cc8d4829272ca01`;
- Java de produção, agregado SHA-256: `222dd3f511b0cbe8fe4f591b94052775d1dee42f015cb2a00e173ca15f9442e0`;
- gramáticas, agregado SHA-256: `4be036fc47a73e32dd68de6a3ab0008ef81895f52372046f6a35e986e92b202e`;
- corpus interno, agregado SHA-256: `f89cf02782dc26f3a4a79e37fc98c35112f0003d36423e674785251152b4b10e`;
- outputs versionados, agregado SHA-256: `4a61cb0c2fe06f51f49c9cf4769131657ca9ba3fd7796e24f22cd1048c91bd80`;
- plano de resolução suspenso SHA-256:
  `eaf75ef6344812fb585de3ecffbf927df052563b1e8e5f5b8ae296e8034f2674`.

Fontes COBOL principais:

| Fonte | SHA-256 |
|---|---|
| `../cbl/CBSTM03A.CBL` | `23c8753b6b4e0c24d4560c83861fe8162626bab195faec0fe88cf80b8bf432b5` |
| `corpus/cbl/CBSTM03D.CBL` | `d75535258cb80c8777993b6662146ed7f2f8cc5888a34d648b5ea68c310a7fac` |
| `corpus/cbl/COACTUPC.cbl` | `b5bb7d6ccad022e0fc91b4dd1e971f49d184adf89b56abdce14eccff35b39396` |

## Métricas semânticas atuais

| Programa | AST | Profundidade | CALL estático | CALL dinâmico | Embedded | Unsupported | Preserved statements | Escopos | Símbolos | Diagnósticos da tabela |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| COACTUPC | 9.189 | 11 | 1 | 0 | 14 | 0 | 0 | 651 | 853 | 2 |
| CBSTM03A | 2.740 | 11 | 14 | 0 | 0 | 0 | 37 | 219 | 209 | 0 |
| CBSTM03D | 2.752 | 11 | 0 | 14 | 0 | 0 | 37 | 221 | 211 | 0 |

Frontend atual:

- os três programas têm zero erros sintáticos;
- `COACTUPC` tem três COPYs ausentes;
- `CBSTM03A` e `CBSTM03D` não têm COPYs ausentes;
- `CBSTM03D` tem 14 targets `DataReference("WS-CALL-TARGET")` e uma única
  declaração DATA correspondente;
- o baseline anterior achatava, entre outros exemplos,
  `ACCTSIDI OF CACTUPAI` em `ACCTSIDIOFCACTUPAI` e uma reference modification
  completa em `DFHCOMMAREA(1:LENGTHOFCARDDEMO-COMMAREA)`.

Esses fatos estão protegidos por `SemanticModelBaselineCharacterizationTest`.
Contagens poderão mudar ao introduzir novos nós semânticos; os fatos essenciais
deverão permanecer e toda diferença será explicada neste relatório.

## Resultados por fase

| Fase | Estado | Evidência | Diferenças esperadas | Pendências |
|---|---|---|---|---|
| 0 — baseline | aprovada | 5 testes Maven verdes; baseline e hashes registrados | nenhuma mudança de produção | nenhuma |
| 1 — cobertura | aprovada | 11 testes Maven verdes; guarda das 628 regras | `AstBuilder` retorna AST + cobertura + diagnósticos; nós AST inalterados | findings cobrem statements nesta fase |
| 2 — proveniência | aprovada | 13 testes Maven verdes; fixtures de COPY aninhado, REPLACING e COPY ausente | `Meta` e snapshot expõem arquivo/linha/include chain/exatidão; `writtenName` mantém separadores | contextos que cruzam fronteiras são marcados `exact=false`; COPY ausente continua diagnóstico explícito |
| 3 — referências/expressões | aprovada | 15 testes Maven verdes; fixtures gramaticais focadas | referências, qualificadores recursivos, subscritos, reference modification e expressões são nós alcançáveis | construções não interpretadas usam `PreservedExpression`; sem binding ou valores resolvidos |
| 4 — procedure references | aprovada | 17 testes Maven verdes; fixture GO TO/PERFORM/ALTER/SORT/MERGE | procedure/file/program/index references são nós sem binding e alcançáveis | statements adiados continuam `UnsupportedStatement`, agora com referências reconhecidas |
| 5 — declarações | aprovada | 19 testes Maven verdes; fixture de declarações e assinatura | hierarquia e cláusulas DATA são nós; LINKAGE/USING e relações textuais ficam explícitos | cláusulas adiadas usam `PreservedDataClause`; binding não executado |
| 6 — statements | aprovada | 22 testes Maven verdes; fixture com as 50 alternativas | operands e clauses são estruturados; 16 statements adiados usam `PreservedStatement` | sem interpretação semântica dos 16 adiados |
| 7 — observabilidade | aprovada | 23 testes Maven, JS e navegador local verdes | `coverage-data.js` e aba Cobertura nas três jornadas | todos os programas permanecem conservadoramente incompletos |
| 8 — regressão final | aprovada | 23 testes Maven, todos os JavaScripts, duas gerações determinísticas, hashes e escopo validados | nenhuma mudança adicional de produção; relatório e tasklist atualizados | nenhuma |

## Evidência TDD da Fase 1

1. RED: `GrammarCoverageManifestTest` e `SemanticCoverageTest` falharam na
   compilação porque manifesto, taxonomia e `AstBuildResult` ainda não existiam.
2. GREEN: os cinco testes desses contratos passaram após a implementação
   mínima e a inclusão das 628 linhas explícitas do manifesto.
3. RED: `AstBuildCoverageTest` falhou porque `AstBuilder.build` ainda retornava
   somente `Ast.Program`.
4. GREEN: o builder passou a retornar produtos separados e o teste confirmou
   findings determinísticos para `MOVE`, `SET` e `GOBACK`.
5. REFACTOR/GUARD: a suíte completa terminou com 11 testes, zero falhas e sem
   alteração das métricas da AST ou da Symbol Table.

## Evidência TDD da Fase 2

1. RED: `SourceProvenanceTest` falhou na compilação pela ausência de source map,
   proveniência em `Meta` e construtor do builder que a recebesse.
2. GREEN: o preprocessador passou a manter segmentos imutáveis com arquivo,
   offsets originais, cadeia de COPY e flag de exatidão; COPY aninhado,
   REPLACING e ausência passaram nos testes focados.
3. RED: a verificação do snapshot falhou na compilação porque a origem ainda
   não era observável na representação achatada.
4. GREEN: o snapshot passou a expor arquivo/linha, profundidade de include e
   exatidão sem regenerar os artefatos HTML da fase 7.
5. GUARD: a suíte completa terminou com 13 testes e zero falhas. As métricas
   semânticas e da tabela de símbolos permaneceram iguais; qualificadores e
   reference modification deixaram de perder espaços por `getText()`.

Lacunas de input continuam conservadoras: COPY ausente gera diagnóstico
`unresolved_copy`, texto sintético rastreável ao statement original e
`exact=false`. Um contexto gramatical que cruza segmentos/arquivos também não
é declarado como slice exato.

## Evidência TDD da Fase 3

1. RED: `StructuredExpressionAstTest` falhou na compilação porque
   `DataReference` ainda não expunha base, qualifiers, grupos de subscritos e
   reference modification, e porque os novos nós de expressão não existiam.
2. GREEN: referências simples, `OF`, `IN`, qualificadores múltiplos,
   qualificador subscrito, múltiplos subscritos, subscript relativo e reference
   modification aberta/com comprimento passaram em fixtures derivadas das
   alternativas da gramática.
3. RED/GREEN: um subscript `qualifiedDataName integerLiteral?` inicialmente
   perdia o inteiro opcional; o teste falhou e a construção passou a ser
   `OperationExpression(RELATIVE_SUBSCRIPT)` com ambos os operandos.
4. RED/GREEN: EVALUATE inicialmente não tinha seletores estruturados; o contrato
   falhou na compilação e passou a representar `ALSO` tanto nos subjects quanto
   nos WHENs.
5. GREEN/GUARD: aritmética, condições, functions, special registers,
   IF/EVALUATE/PERFORM e fallback preservado ficaram navegáveis por
   `Ast.children` e pelo snapshot. A suíte terminou com 15 testes verdes.

As contagens mudaram deterministicamente porque referências antes escondidas
em strings/`RawExpression` agora são nós: COACTUPC passou de 4.100 para 6.603
nós (profundidade 8→11), CBSTM03A de 1.250 para 1.368 e CBSTM03D de 1.260 para
1.378. CALLs, unsupported statements, escopos, símbolos e diagnósticos não
mudaram. CBSTM03D continua com 14 CALLs dinâmicos para `WS-CALL-TARGET`.

## Evidência TDD da Fase 4

1. RED: `NominalReferenceAstTest` falhou na compilação pela ausência de
   `ProcedureReference`, `FileReference`, `ProgramReference`, qualificadores e
   referências reconhecidas em statements preservados.
2. GREEN: GO TO simples/DEPENDING ON/qualificado e PERFORM simples/THRU passaram
   a usar ocorrências com identidade, texto e spans próprios.
3. ALTER, SORT e MERGE continuam semanticamente adiados, mas preservam em ordem
   seus procedure/file references; CALL literal usa `ProgramReference`.
4. As novas ocorrências explicam as contagens 6.789/1.421/1.431; fatos de CALL,
   Symbol Table e cobertura de statements permanecem inalterados.

## Evidência TDD da Fase 5

1. RED: `DeclarationModelAstTest` falhou na compilação pela ausência de tipos de
   data section, hierarquia de declarações, cláusulas DATA, níveis especiais e
   assinatura da Procedure Division.
2. GREEN: uma fixture gramatical focada passou a provar grupos 01/05, FILLER,
   níveis 66/77/88, REDEFINES, RENAMES, OCCURS com range/DEPENDING/INDEXED,
   PICTURE, VALUE, USAGE, LOCAL-STORAGE, LINKAGE e USING/RETURNING.
3. Cláusulas não interpretadas, como `BLANK WHEN ZERO`, permanecem como
   `PreservedDataClause` com regra, texto, origem e referências reconhecidas;
   portanto, preservar não é apresentado como interpretar.
4. A Symbol Table ganhou kinds `RENAMES` e `INDEX_NAME`. Relações de REDEFINES,
   RENAMES e OCCURS são atributos textuais com `relationBinding=NOT_PERFORMED`,
   sem `symbolId`, binding ou resultado de resolução na AST.
5. RED/GREEN: o snapshot detectou que um índice de OCCURS recebia identidade
   antes dos operandos que o precedem na árvore. A construção passou a respeitar
   a ordem estrutural e a fixture protege IDs determinísticos.
6. GREEN/GUARD: a suíte completa terminou com 19 testes verdes e todos os
   JavaScripts passaram em `node --check`. Os nós cresceram, como esperado, para
   7.722/1.663/1.675; profundidade, CALLs, statements não suportados, escopos,
   símbolos e diagnósticos permaneceram iguais. COACTUPC passou a expor 2.463
   DataReferences porque relações de cláusulas deixaram de ficar escondidas em
   texto.

Os hashes das três fontes principais e do plano suspenso permanecem idênticos
ao baseline. Os HTMLs não foram regenerados: essa alteração está reservada para
a Fase 7.

## Evidência TDD da Fase 6

1. RED: `StatementModelAstTest` exigiu um caminho explícito para cada uma das
   50 alternativas da regra `statement`, operandos alcançáveis e ausência de
   `UnsupportedStatement`; o contrato inicialmente não compilava.
2. GREEN: os 31 statements aprovados para esta fase agora usam nós dedicados já
   existentes ou `ModeledStatement`. Cada operando mantém como papel a regra
   gramatical que o introduziu, além de referência/literal, `Meta` e origem.
3. Os 16 statements adiados usam `PreservedStatement`, com operandos e clauses
   estruturados e política `PRESERVED_UNINTERPRETED/DEPENDENCY_UNKNOWN`. As três
   linguagens embutidas continuam opacas, com payload preservado.
4. CALL passou a distinguir REFERENCE/VALUE/CONTENT, OMITTED, ADDRESS OF,
   LENGTH OF e GIVING/RETURNING. MOVE continua distinguindo CORRESPONDING e
   preserva referências de group moves e reference modification.
5. Operações de arquivo mantêm `FileReference`, dados, keys e clauses de fluxo
   excepcional. Nenhum coletor procura palavras no texto COBOL; a extração
   percorre exclusivamente contexts e alternativas da gramática versionada.
6. GREEN/GUARD: 22 testes passaram. As 50 alternativas são comparadas
   mecanicamente com a gramática e exercitadas por uma fixture sintética. Os
   nós passaram para 9.189/2.740/2.752 e COACTUPC expõe 2.871 DataReferences;
   CALLs, escopos, símbolos e diagnósticos permaneceram iguais.

## Evidência TDD da Fase 7

1. RED/GREEN: `CoverageSnapshotTest` exigiu serialização determinística,
   completude conservadora, motivos bloqueantes e deep links AST/parse tree.
2. `coverage-data.js` expõe totais por `ConstructionCoverage` e
   `DependencyKnowledge`, referências por namespace e forma, statements
   modelados/preservados, cláusulas DATA tipadas/preservadas, expressões opacas
   e linguagens embutidas.
3. A aba **Cobertura** mostra banner de completude, motivos, métricas e lacunas
   navegáveis para AST, parse tree e fonte/proveniência. A inspeção no navegador
   confirmou a navegação de `execCicsStatement` para AST #1743, linha expandida
   2604 e parse tree #20211, sem erros no console.
4. `dist`, `dist-cbstm03a` e `dist-cbstm03d` foram regenerados. Todos informam
   `complete=false`: COACTUPC tem três COPYs ausentes e CICS opaco; os programas
   CBSTM têm statements preservados ainda não interpretados.
5. Cláusulas DATA e expressões preservadas também geram findings navegáveis e
   impedem completude; não ficam escondidas apenas em uma métrica agregada.
6. Duas gerações integrais produziram o mesmo SHA-256 agregado:
   `57b33d11f05831707ed5b0512d981b586b973e265fe44f3756237f00f2e8b4ba`.
   Isso protege IDs, ordem e snapshots determinísticos.

## Evidência da regressão final — Fase 8

1. A suíte Maven completa terminou com **23 testes, zero falhas, zero erros e
   zero testes ignorados**. Isso inclui as fixtures sintéticas de referências,
   expressões, nomes de procedimento, declarações, proveniência/COPY e as 50
   alternativas da regra `statement`.
2. Todos os JavaScripts de templates e dos três diretórios gerados passaram em
   `node --check`. A busca nos templates confirmou somente recursos locais; não
   há dependência externa de execução no HTML.
3. COACTUPC, CBSTM03A e CBSTM03D foram regenerados duas vezes. Ambas as rodadas
   terminaram com zero erros léxicos e sintáticos e produziram o mesmo SHA-256
   agregado dos artefatos: `57b33d11f05831707ed5b0512d981b586b973e265fe44f3756237f00f2e8b4ba`.
4. `DynamicCallVariantTest` confirmou 14 ocorrências de `CALL
   WS-CALL-TARGET`, zero CALLs literais, um MOVE de `CBSTM03B` e um MOVE de
   `CEE3ABD`. Todos os CALLs permanecem dinâmicos e a Symbol Table mantém uma
   única declaração DATA de nível 05 para o target.
5. Os fatos essenciais da Symbol Table não regrediram: 651/853/2 para
   COACTUPC, 219/209/0 para CBSTM03A e 221/211/0 para CBSTM03D
   (escopos/símbolos/diagnósticos).
6. A evolução de 4.100/1.250/1.260 para 9.189/2.740/2.752 nós e de profundidade
   8 para 11 é esperada: referências, expressões, cláusulas, declarações e
   operandos antes achatados agora têm identidade própria. Duas gerações
   idênticas provam que IDs, ordem e snapshots são determinísticos.
7. A navegação Parse Tree ↔ AST ↔ Symbol Table e a visão de cobertura foram
   validadas no navegador na Fase 7; a regressão estática da Fase 8 confirmou
   novamente links e datasets locais. Nenhum arquivo de frontend mudou depois
   dessa validação funcional.
8. A completude continua conservadora: os três relatórios dizem
   `complete=false`. COACTUPC expõe três COPYs ausentes e 14 CICS opacos;
   CBSTM03A/CBSTM03D expõem 37 statements e nove expressões preservadas. Essas
   lacunas permanecem `DEPENDENCY_UNKNOWN`, nunca “nenhuma dependência”.
9. Os tree IDs de `baseline`, `cbl`, `cpy`, gramáticas e corpus são idênticos
   entre `ffa053f` e o HEAD auditado. As três fontes principais e o plano
   suspenso também mantêm exatamente os SHA-256 registrados no baseline.
10. A revisão do diff encontrou mudanças apenas no explorer. A AST não contém
    `symbolId`, binding ou resultado resolvido; os IDs existentes pertencem
    exclusivamente à Symbol Table declarativa. SQL/CICS/SQLIMS continuam como
    payloads opacos de `EmbeddedLanguageStatement`. Não há resolver, CFG,
    reaching definitions, propagação de constantes, análise SQL ou fatos
    finais de dependência.

## Cobertura pendente conhecida

- 16 famílias de statements permanecem preservadas estruturalmente, mas ainda
  não interpretadas semanticamente; três famílias de linguagem embutida ficam
  opacas conforme o escopo aprovado.
- COACTUPC não pode ser considerado completamente coberto enquanto os três
  COPYs ausentes e os 14 statements CICS opacos permanecerem.
- CBSTM03A e CBSTM03D não podem ser considerados completamente cobertos
  enquanto statements/expressões preservados mantiverem
  `DEPENDENCY_UNKNOWN`.
- Essas pendências são deliberadamente observáveis por regra, arquivo, linha,
  AST e parse tree e não bloqueiam o próximo passo de resolução de referências
  sobre o subconjunto estruturado.

## Checklist final de regressão

- [x] suíte Maven completa;
- [x] sintaxe de todos os JavaScripts;
- [x] três programas regenerados sem novos erros léxicos/sintáticos;
- [x] fatos de CALL/MOVE de CBSTM03D preservados;
- [x] fatos essenciais da Symbol Table preservados;
- [x] mudanças de nós, IDs e profundidade explicadas e determinísticas;
- [x] fixtures sintéticas completas;
- [x] navegação HTML validada;
- [x] completude semântica conservadora validada;
- [x] fontes, baselines e plano suspenso byte a byte inalterados;
- [x] ausência de resolver, CFG, dataflow, SQL e fatos finais confirmada.
