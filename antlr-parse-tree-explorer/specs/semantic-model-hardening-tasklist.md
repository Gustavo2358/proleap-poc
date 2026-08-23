# SDD — Hardening do modelo semântico COBOL

## Estado e relação com o plano anterior

A implementação de resolução de referências está suspensa. O arquivo
`specs/reference-resolution-tasklist.md` não faz parte desta execução e deve
permanecer byte a byte inalterado até retomada explícita.

Este documento planeja somente o fortalecimento da AST, dos contratos
semânticos, dos snapshots e da observabilidade de cobertura. Nenhuma tarefa
deste plano deve ser executada antes de aprovação explícita.

## Contexto e motivação

O modelo atual é adequado para explicar a transformação inicial
`Parse Tree → AST`, mas ainda perde estrutura necessária para analisar uma
codebase COBOL bancária massiva. Em particular, nomes compostos são achatados,
expressões completas viram texto, declarações conservam cláusulas relevantes
somente dentro de uma string e 40 das 50 alternativas de `statement` da
gramática não possuem tratamento explícito no `AstBuilder`.

O corpus local é pequeno e não pode determinar o desenho da solução. Ele será
usado exclusivamente como regressão e evidência de exemplos reais. O contrato
de cobertura será dirigido pela totalidade das gramáticas ProLeap versionadas no
projeto:

- 598 regras de parser em `Cobol.g4`;
- 30 regras de parser em `CobolPreprocessor.g4`;
- 50 alternativas diretas de `statement`;
- todas as formas de `identifier`, declaração, seção e referência alcançáveis
  pelas regras relevantes para fluxo e dependências.

“COBOL como um todo”, neste plano, significa todo o dialeto aceito pela
`Cobol.g4` e `CobolPreprocessor.g4` ProLeap presentes no repositório. Uma forma
externa a essas gramáticas não poderá ser declarada suportada; deverá aparecer
como limitação do frontend.

## Princípio não heurístico

As futuras alterações deverão obedecer às seguintes regras:

1. Estrutura semântica será extraída de `ParserRuleContext`, alternativas e
   tokens definidos pela gramática, nunca de regex aplicada ao texto COBOL.
2. `ParseTree.getText()` não será usado para decompor nomes, qualificadores,
   subscritos, reference modification, cláusulas ou operandos.
3. O texto exato poderá ser obtido pelo intervalo do token/fonte apenas para
   preservar fidelidade escrita; ele não substituirá os campos estruturados.
4. Um manifesto de cobertura classificará explicitamente toda a superfície
   relevante da gramática. Uma nova alternativa adicionada à gramática deverá
   fazer um teste de cobertura falhar até ser classificada.
5. Ausência no corpus nunca será interpretada como ausência na linguagem.
6. Fixtures sintéticas pequenas cobrirão formas previstas pela gramática que
   não aparecem nos programas atuais.
7. Uma construção desconhecida ou parcialmente preservada nunca equivalerá a
   “nenhuma dependência”.

## Inventário comprovado na implementação atual

### Referências e relações achatadas

| Modelo atual | Informação preservada somente como string | Consequência |
|---|---|---|
| `DataReference` | `writtenName`, produzido com `context.getText()` | qualificadores, subscritos e reference modification não são navegáveis |
| `GoToStatement` | `List<String> targets` | cada uso não possui identidade, span ou origem próprios |
| `PerformStatement` | `fromProcedure`, `throughProcedure` e `control` | targets e expressões de controle não são nós semânticos |
| `EvaluateBranch` | `selector` | referências/literais do `WHEN` ficam opacos |
| `FileBinding` | cláusula `assignment` inteira | forma de ASSIGN e possível DDNAME não são estruturadas |
| `DataEntry` | `level`, `name` e `declaration` | todas as cláusulas de dados permanecem dentro de texto bruto |
| `FileDescription` | somente nome e entries | cláusulas de FD/SD são descartadas do modelo semântico |
| `RawExpression` | `role` e `rawText` | referências usadas em condições e cálculos não aparecem como filhos |
| `UnsupportedStatement` | regra, texto e statements aninhados | operandos e efeitos de definição/transporte não são estruturados |

`ParseTree.getText()` concatena tokens sem os espaços e comentários do canal
oculto. Assim, preserva parte dos lexemas, mas perde a forma escrita exata e
transforma estruturas como `ITEM OF GROUP`, subscritos e offsets em uma cadeia
que precisaria ser reinterpretada. Reinterpretar essa cadeia seria heurístico e
fica proibido por este plano.

### Declarações e escopos

- `DataEntry` é plano na AST; a hierarquia de grupos é reconstruída depois pelo
  `SymbolTableBuilder` a partir de números de nível.
- `REDEFINES`, `RENAMES`, `OCCURS`, `VALUE`, `USAGE`, `PICTURE` e demais
  cláusulas não possuem modelos próprios.
- nível 88 é reconhecido pela tabela, mas seus valores continuam textuais.
- nível 66 é tratado como `DATA_ITEM`; a relação de alias de `RENAMES` não é
  representada.
- índices declarados por `OCCURS ... INDEXED BY` não viram símbolos.
- a `LINKAGE SECTION` aparece como uma section genérica; não existe contrato
  para a assinatura de `PROCEDURE DIVISION USING/GIVING`.
- `FILLER` é convertido em nome textual na AST, embora corretamente não gere
  símbolo.
- `FILE_CONTROL` e `FILE_DESCRIPTION` são coletados, mas relações como ASSIGN,
  record key, status e aliases continuam textuais ou ausentes.
- o preprocessor expande COPYs em uma string única. `SourceSpan` aponta para o
  fonte expandido, sem arquivo original, cadeia de inclusão ou mapeamento de
  linha do copybook.

### Statements

O `switch` de `AstBuilder.buildStatement` trata explicitamente somente estas
10 alternativas da gramática: `CALL`, `IF`, `EVALUATE`, `PERFORM`, `GO TO`,
`MOVE`, `NEXT SENTENCE`, `EXEC CICS`, `EXEC SQL` e `EXEC SQLIMS`.

As outras 40 alternativas caem no fallback `UnsupportedStatement`. Isso inclui
construções importantes para def-use, fluxo e dependências, entre elas `SET`,
`STRING`, `UNSTRING`, `INITIALIZE`, `ACCEPT`, operações aritméticas e operações
de arquivo.

Mesmo nos statements tipados existem perdas:

- condições de `IF` são `RawExpression`;
- controles `UNTIL`, `TIMES` e `VARYING` de `PERFORM` são uma string;
- seletores de `EVALUATE WHEN` são strings;
- `MOVE CORRESPONDING` é apenas um boolean;
- argumentos especiais de CALL (`ADDRESS OF`, `LENGTH OF`, `OMITTED`) não têm
  representação própria;
- `CALL ... GIVING/RETURNING` não é preservado;
- special registers são confundidos com `DataReference` quando passam pela
  regra `identifier`.

### Snapshots e HTML

- `AstSnapshot` informa somente a quantidade total de
  `UnsupportedStatement`; não lista regras, linhas ou relevância da lacuna.
- a página da AST afirma que a AST foi construída, mesmo quando centenas de
  statements relevantes permanecem sem modelo.
- cada `UnsupportedStatement` pode ser inspecionado individualmente, mas não há
  visão agregada de cobertura nem critério de completude para dependências.
- erros de parser e COPYs ausentes aparecem na Parse Tree, porém não impedem que
  as páginas seguintes pareçam completas.
- `SymbolTableSnapshot` não consegue expor relações declarativas que a AST não
  estruturou.

## Evidência do corpus e dos snapshots atuais

Estes números são regressão, não fonte de requisitos:

| Evidência da parse tree/snapshot | `COACTUPC` | `CBSTM03D` |
|---|---:|---:|
| referências qualificadas (`qualifiedInData`) | 491 | 0 |
| subscritos | 10 | 15 |
| reference modification | 69 | 0 |
| special registers | 5 | 9 |
| function calls | 157 | 0 |
| `REDEFINES` após expansão de COPY | 57 | 1 |
| `OCCURS` | 1 | 3 |
| `VALUE` | 343 | 89 |
| `USAGE` | 8 | 9 |
| `UnsupportedStatement` na AST | 411 | 268 |
| `RawExpression` (condições) | 274 | 15 |

Em `COACTUPC`, os 411 statements não modelados incluem 226 `SET`, 53
`STRING`, 10 `INITIALIZE`, 9 `COMPUTE` e outros. Em `CBSTM03D`, os 268 incluem
95 `WRITE`, 83 `SET`, 33 `DISPLAY`, 12 `STRING`, 4 `COMPUTE`, 4 `ALTER` e
outros.

Também estão comprovados:

- `CBSTM03D`: subscritos simples e múltiplos, `OCCURS`, `REDEFINES`, nível 88,
  LINKAGE, group moves, `SET ADDRESS OF`, `INITIALIZE`, `STRING`, GO TO,
  PERFORM e `PERFORM ... THRU`;
- `COACTUPC`: referências qualificadas, reference modification com expressões
  em offset/comprimento, function calls, muitos REDEFINES vindos de copybooks,
  LINKAGE, SET, STRING, INITIALIZE, COMPUTE e linguagem embutida;
- `CBSTM03D`: 14 CALLs dinâmicos para `WS-CALL-TARGET` e dois MOVEs que mudam
  seu conteúdo;
- `COACTUPC`: três COPYs ausentes, portanto ele já não pode ser declarado
  completamente coberto para dependências.

Não foram observados nos artefatos atuais `RENAMES`, `UNSTRING`,
`MOVE CORRESPONDING`, `GO TO ... DEPENDING ON` nem
`PROCEDURE DIVISION USING`. Todos são suportados pela gramática e exigirão
fixtures sintéticas; a ausência no corpus não reduz sua prioridade contratual.

## Riscos para uso em produção

1. **Falso negativo de dependência:** uma referência escondida em expressão ou
   statement opaco pode ser convertida indevidamente em “nenhum uso”.
2. **Binding impossível ou incorreto:** nomes achatados não permitem aplicar
   qualificação COBOL sem reparsing heurístico.
3. **Def-use incompleto:** destinos de SET/STRING/READ/INITIALIZE e group moves
   não são visíveis como definições potenciais.
4. **Aliases invisíveis:** REDEFINES/RENAMES podem fazer uma escrita afetar
   outra visão dos mesmos bytes.
5. **Origem incorreta:** nós expandidos de COPY parecem pertencer ao arquivo
   principal e dificultam diagnóstico em codebases reais.
6. **Assinatura perdida:** LINKAGE e PROCEDURE DIVISION USING não se relacionam,
   ocultando entradas externas do programa.
7. **Cobertura enganosa:** zero erros sintáticos não significa análise
   semântica completa.
8. **IDs instáveis sem explicação:** novos nós alteram métricas e deep links;
   determinismo precisa ser testado, não presumido por contagem antiga.

## Decisões arquiteturais propostas

### 1. Resultado de construção e cobertura

`AstBuilder` passará a produzir um `AstBuildResult` imutável contendo:

- `Ast.Program`;
- `SemanticCoverageReport`;
- diagnósticos semânticos da transformação.

A AST continuará sem symbol IDs, bindings, CFG ou fatos de fluxo.

A cobertura terá duas dimensões independentes:

- `ConstructionCoverage`: `MODELED`, `PRESERVED_UNINTERPRETED`,
  `UNSUPPORTED`, `INPUT_MISSING`;
- `DependencyKnowledge`: `REFERENCE_READY`, `DEPENDENCY_UNKNOWN`,
  `NOT_DEPENDENCY_BEARING`.

Cada finding guardará regra da gramática, `Meta`, texto preservado, motivo,
relevância e nó AST relacionado quando existir. Um resumo por programa terá
estado `COMPLETE` somente se não houver `UNSUPPORTED`, `INPUT_MISSING` ou
`DEPENDENCY_UNKNOWN` relevante ao objetivo declarado.

Os futuros estados de resolução serão `RESOLVED`, `AMBIGUOUS`, `UNRESOLVED` e
`UNSUPPORTED`. Este passo não os calculará. Ele apenas garantirá que uma forma
estruturada possa chegar aos três primeiros e que uma forma não compreendida
possa chegar explicitamente a `UNSUPPORTED`.

### 2. Manifesto integral da gramática

Será criado um manifesto versionado e verificável que classifique todas as 628
regras de parser das duas gramáticas. Regras sem efeito semântico próprio ainda
terão classificação e justificativa, em vez de serem omitidas. Em particular, o
manifesto detalhará:

- todas as 50 alternativas de `statement`;
- todas as alternativas de `identifier`, `qualifiedDataName`, `tableCall`,
  `functionCall`, `referenceModifier`, `subscript` e `specialRegister`;
- todas as alternativas de `dataDivisionSection`;
- os três formatos declarativos de dados e todas as suas cláusulas;
- declarações de arquivo, FILE-CONTROL, assinatura da PROCEDURE DIVISION,
  sections, paragraphs e entry points;
- toda ocorrência gramatical de `procedureName`, `fileName` e referências de
  dados dentro de statements e cláusulas;
- COPY, REPLACE/REPLACING, compiler options, diretório/família de copybook e as
  três formas de linguagem embutida reconhecidas pelo preprocessor.

O teste de guarda deverá comparar o manifesto com o modelo da gramática ANTLR
ou com um inventário gerado de forma determinística. Regex sobre programas
COBOL não será aceita como mecanismo de cobertura.

### 3. Texto e proveniência

- `writtenText` será o recorte exato do fonte/token stream.
- nomes canônicos não substituirão a grafia original na AST.
- `Meta` será ampliado com proveniência: arquivo lógico, span no fonte
  expandido, span no arquivo original e cadeia de inclusões de COPY quando
  disponível.
- o preprocessor produzirá um source map por segmentos, inclusive para COPY
  REPLACING, COPY ausente e expansão recursiva.
- ausência de mapeamento será `INPUT_MISSING`, não uma linha silenciosamente
  atribuída ao programa principal.

### 4. Referências de dados estruturadas

`DataReference` passará a conter:

- `baseName`;
- `writtenText` exato;
- lista ordenada de `DataQualifier`, preservando `OF` ou `IN` e distinguindo
  qualificador DATA, FILE ou TABLE;
- lista de `SubscriptGroup`, preservando cada par de parênteses e a ordem de
  múltiplos subscritos;
- `ReferenceModification` opcional;
- `Meta` e origem da parse tree;
- status estrutural quando alguma parte for apenas preservada.

Cada `Subscript` será um nó com expressão própria, incluindo `ALL`, índice com
deslocamento e expressão aritmética. `ReferenceModification` terá offset e
comprimento opcional como expressões, cada qual com span próprio. Uma referência
qualificada e subscrita continuará sendo um único uso, com componentes filhos.

### 5. Expressões

Para não esconder usos necessários a def-use, a AST ganhará formas imutáveis
para:

- operadores aritméticos unários/binários;
- condições lógicas e relacionais;
- condition-name references;
- function calls e seus argumentos;
- special registers;
- literais;
- referências de dados estruturadas;
- expressão preservada mas ainda não interpretada, contendo regra, texto e
  filhos estruturados já reconhecidos.

`IF`, `EVALUATE` e controles de `PERFORM` usarão essas expressões. Um fallback
de expressão não poderá descartar referências filhas reconhecíveis pela
gramática.

### 6. Referências de procedimento e outros nomes

`ProcedureReference` será nó de primeira classe com identidade, `Meta`, span,
`writtenText`, nome base e qualificação opcional por section. Será usado em toda
ocorrência de `procedureName` da gramática, incluindo GO TO, GO TO DEPENDING ON,
PERFORM/THRU, ALTER e procedures de SORT/MERGE.

Também serão introduzidos nós sem binding para `FileReference`, referências a
programas/entry points quando semanticamente aplicável, index names e special
registers. Nenhum desses nós conterá `symbolId`.

### 7. Declarações de dados

As sections de dados terão tipo explícito (`FILE`, `WORKING_STORAGE`,
`LOCAL_STORAGE`, `LINKAGE` e demais alternativas da gramática), em vez de nome
de apresentação.

A hierarquia de grupos será primeira classe na AST. O modelo distinguirá:

- itens 01–49, 77, 66 e 88;
- nome ausente/FILLER sem inventar identidade;
- filhos de grupo;
- `PICTURE`, `USAGE`, `VALUE`, `OCCURS`, `REDEFINES` e `RENAMES` em cláusulas
  tipadas;
- valores/ranges de nível 88;
- índices de `OCCURS INDEXED BY`;
- referências `DEPENDING ON`, keys e limites;
- cláusulas restantes como `PreservedDataClause`, com regra, texto, referências
  filhas e cobertura explícita.

REDEFINES e RENAMES preservarão referências estruturadas, mas não serão ligados
a declarações neste passo. A Symbol Table poderá criar kinds próprios para
condition names, RENAMES e index names e preservar relações ainda não
resolvidas sem fabricar aliases.

### 8. LINKAGE, parâmetros e COPY

- `ProcedureSignature` preservará USING/CHAINING, modo REFERENCE/VALUE,
  OPTIONAL/ANY, GIVING/RETURNING e ordem dos parâmetros.
- parâmetros serão referências a declarações, não novas declarações inventadas.
- `LINKAGE SECTION` permanecerá distinguível de WORKING-STORAGE.
- cada declaração expandida de copybook conservará proveniência e include chain.
- COPY ausente tornará a cobertura incompleta.

### 9. Statements e fronteiras incrementais

Todas as 50 alternativas terão uma classificação explícita. A proposta inicial
é:

**Modelagem semântica neste hardening**

`ACCEPT`, `ADD`, `CALL`, `CLOSE`, `COMPUTE`, `CONTINUE`, `DELETE`, `DIVIDE`,
`EVALUATE`, `EXIT`, `GOBACK`, `GO TO`, `IF`, `INITIALIZE`, `INSPECT`, `MOVE`,
`MULTIPLY`, `NEXT SENTENCE`, `OPEN`, `PERFORM`, `READ`, `RELEASE`, `RETURN`,
`REWRITE`, `SET`, `START`, `STOP`, `STRING`, `SUBTRACT`, `UNSTRING` e `WRITE`.

O objetivo não é executar análises, mas preservar operandos, targets,
expressões, clauses de exceção e relações de entrada/saída que a própria
gramática torna explícitas. MOVE CORRESPONDING, group moves, alterações parciais
e CALL USING/GIVING receberão contratos próprios.

**Preservação estrutural para implementação semântica posterior**

`ALTER`, `CANCEL`, `DISABLE`, `DISPLAY`, `ENABLE`, `ENTRY`, `EXHIBIT`,
`GENERATE`, `INITIATE`, `MERGE`, `PURGE`, `RECEIVE`, `SEARCH`, `SEND`, `SORT` e
`TERMINATE`.

Cada um terá um `PreservedStatement` com kind, regra, texto, clauses e
referências/literais/procedures/files extraídos por visitors específicos da
gramática. Seu `DependencyKnowledge` será `DEPENDENCY_UNKNOWN` até receber
semântica suficiente. Um coletor genérico baseado em texto não será aceito.

**Preservação opaca com diagnóstico explícito**

`EXEC CICS`, `EXEC SQL` e `EXEC SQLIMS`. Payload e proveniência serão mantidos,
mas o relatório marcará dependência desconhecida até plugins posteriores.

Nenhuma alternativa de statement será descartada como irrelevante. Statements
puramente de fluxo poderão ser `NOT_DEPENDENCY_BEARING`, mas continuarão
modelados porque o CFG futuro precisará deles.

## Modelo conceitual antes/depois

```text
ANTES
DataReference("ITEM(IX)OFGROUP")
PerformStatement(from="A", through="B", control="VARYING ...")
DataEntry(level="05", name="ITEM", declaration="... OCCURS ...")
UnsupportedStatement(rule="setStatement", rawText="SET ...")

DEPOIS
DataReference
├─ baseName: ITEM
├─ writtenText: ITEM(IX) OF GROUP
├─ qualifiers: [DataQualifier(OF, GROUP)]
├─ subscriptGroups
│  └─ [Subscript(DataReference IX)]
└─ referenceModification: none

PerformStatement
├─ from: ProcedureReference A
├─ through: ProcedureReference B
└─ control: PerformVarying(expressões estruturadas)

DataDeclaration 05 ITEM
├─ OccursClause(...)
├─ ValueClause(...)
└─ children: [...]

SemanticCoverageReport
└─ findings por regra, linha, origem, status e impacto em dependências
```

## Escopo

- AST e contratos semânticos imutáveis.
- source map/proveniência necessários para Meta e COPY.
- manifesto integral de cobertura baseado na gramática.
- referências, expressões, declarações e statements definidos acima.
- ajustes correspondentes na Symbol Table, sem binding.
- snapshots da AST/Symbol Table e relatório de cobertura.
- observabilidade didática nos HTMLs existentes.
- fixtures, testes TDD e regressão dos três programas demonstrativos.

## Fora do escopo

- resolução de referências ou alteração da tasklist suspensa;
- qualquer `symbolId` dentro da AST;
- CFG, basic blocks ou arestas;
- def-use, reaching definitions ou kill/gen sets;
- propagação ou resolução de constantes;
- inferência de valores de CALL dinâmico;
- fatos finais de subprogramas, arquivos ou tabelas;
- parsing interno de SQL/CICS/SQLIMS;
- semântica de dialetos não aceitos pelas duas gramáticas atuais;
- alteração de fontes COBOL do corpus.

## Estratégia TDD obrigatória

Cada fatia seguirá rigorosamente:

1. registrar comportamento, métricas, hashes e snapshots atuais;
2. criar testes de caracterização do contrato existente;
3. criar fixture COBOL mínima e focada, derivada da regra da gramática;
4. escrever primeiro o teste do novo contrato semântico;
5. executar o teste e registrar que falha pela funcionalidade ausente;
6. implementar a menor alteração que o faça passar;
7. executar toda a fatia e refatorar somente com testes verdes;
8. registrar a cobertura obtida e iniciar a próxima fatia.

Testes validarão comportamento: estrutura, ordem, texto, spans, origem,
imutabilidade, cobertura e fatos semânticos. Não deverão fixar nomes de métodos
privados, ordem de `HashMap` ou outros detalhes acidentais.

### Fixtures obrigatórias de referências

- referência simples;
- `OF` e `IN` separadamente;
- múltiplos qualificadores e ordem correta;
- qualificação por arquivo e por tabela prevista na gramática;
- um subscript;
- múltiplos subscritos no mesmo grupo;
- múltiplos grupos de subscritos permitidos por `tableCall`;
- subscript `ALL`, índice com deslocamento e expressão aritmética;
- reference modification com offset apenas e com offset/comprimento;
- referência qualificada, subscrita e modificada ao mesmo tempo;
- function call e special register com referências internas;
- preservação exata de caixa, espaços e texto escrito;
- `Meta`, spans e origem de cada componente.

### Fixtures obrigatórias de procedimento

- GO TO simples;
- GO TO DEPENDING ON com vários targets e selector;
- procedure name qualificado por section;
- PERFORM simples;
- PERFORM THRU e THROUGH;
- PERFORM TIMES, UNTIL e VARYING/AFTER;
- ALTER com dois procedure names;
- procedures de entrada/saída de SORT e MERGE.

### Fixtures obrigatórias de declarações

- grupos aninhados e FILLER;
- níveis 01–49, 66, 77 e 88;
- REDEFINES;
- RENAMES simples e THRU;
- OCCURS fixo, range, DEPENDING ON, keys e INDEXED BY;
- VALUE simples, múltiplo e range;
- USAGE e PICTURE;
- LINKAGE + PROCEDURE DIVISION USING por REFERENCE/VALUE, OPTIONAL, ANY e
  GIVING/RETURNING;
- FILE-CONTROL/ASSIGN, FD/SD e clauses de key/status;
- declaração originada de COPY, COPY REPLACING, COPY recursivo e COPY ausente.

### Fixtures obrigatórias de statements e cobertura

- uma fixture mínima válida para cada uma das 50 alternativas de `statement`;
- casos positivos de todos os statements semanticamente modelados;
- preservação estrutural e `DEPENDENCY_UNKNOWN` para cada statement adiado;
- payload opaco e diagnóstico para as três linguagens embutidas;
- MOVE simples, group move, CORRESPONDING e target com reference modification;
- SET, STRING, UNSTRING, INITIALIZE, ACCEPT e READ INTO;
- CALL literal/dinâmico, USING nos três modos, OMITTED, ADDRESS/LENGTH e
  GIVING/RETURNING;
- ausência de perda silenciosa de referências aninhadas;
- guarda que falha quando uma alternativa da gramática não está no manifesto.

## Estratégia de regressão

Após as fatias, uma etapa separada produzirá
`specs/semantic-model-hardening-regression-report.md` contendo baseline,
resultado, diferenças esperadas e cobertura pendente.

A regressão deverá:

- executar toda a suíte Maven;
- executar `node --check` em todos os JavaScripts;
- analisar novamente `COACTUPC`, `CBSTM03A` e `CBSTM03D`;
- validar zero novos erros léxicos/sintáticos;
- manter `CBSTM03D` com 14 CALLs dinâmicos, zero estáticos, 14 targets base
  `WS-CALL-TARGET` e os dois MOVEs de target;
- confirmar que símbolos essenciais existentes não desapareceram;
- comparar fatos semânticos antes/depois, não apenas contagem de nós;
- explicar novos nós, alterações de IDs, profundidade e snapshots;
- executar duas gerações e provar IDs/ordem determinísticos;
- validar navegação Parse Tree ↔ AST ↔ Symbol Table e a visão de cobertura;
- validar HTML sem dependências externas;
- executar todas as fixtures, inclusive formas ausentes do corpus;
- recalcular hashes e provar que fontes COBOL, baselines e a tasklist suspensa
  permaneceram byte a byte inalterados;
- registrar que `COACTUPC` não é completamente coberto enquanto houver COPYs
  ausentes ou lacunas relevantes.

## Observabilidade para produção

Por programa, `SemanticCoverageReport` e os snapshots deverão informar:

- total por `ConstructionCoverage` e `DependencyKnowledge`;
- referências DATA/PROCEDURE/FILE/PROGRAM estruturadas por forma;
- statements por alternativa da gramática e classificação;
- declarações e cláusulas tipadas versus preservadas;
- expressões opacas e referências filhas preservadas;
- regras de origem, arquivo, include chain e linhas de cada lacuna;
- erros de parser/preprocessor e COPYs ausentes;
- contagem de CALLs literal, por referência estruturada e com target não
  suportado;
- `dependencyCoverageComplete: true|false`, acompanhado dos motivos;
- lista determinística das lacunas de maior impacto.

A página AST ganhará uma visão **Cobertura semântica**, sem criar ou executar a
etapa de resolução. Banners e métricas não poderão usar “análise completa” se
existir finding relevante. `UnsupportedStatement` e expressão opaca serão
navegáveis da métrica agregada até fonte/parse tree.

## Tasklist passo a passo

### Fase 0 — Baseline e guarda de escopo

- [x] Registrar commit, status do Git e hashes de código, gramática, corpus,
      outputs e `reference-resolution-tasklist.md`.
- [x] Capturar métricas e fatos semânticos atuais dos três programas.
- [x] Criar testes de caracterização sem alterar o comportamento.
- [x] Criar o esqueleto do relatório de regressão.

### Fase 1 — Manifesto e taxonomia de cobertura

- [x] Inventariar de forma mecanicamente verificável as 628 regras das duas
      gramáticas, detalhando as 50 alternativas de statement e as famílias
      declarativas/de referência.
- [x] Escrever testes RED para alternativa não classificada.
- [x] Implementar `ConstructionCoverage`, `DependencyKnowledge`, finding,
      resumo e `AstBuildResult` imutáveis.
- [x] Implementar o manifesto completo e tornar verde o teste de guarda.
- [x] Refatorar somente após todos os testes da fase passarem.

### Fase 2 — Proveniência e texto fiel

- [x] Criar fixtures de arquivo principal, COPY, REPLACING, nesting e ausência.
- [x] Escrever testes RED de source map/include chain/spans.
- [x] Implementar source map segmentado no preprocessor e ampliar `Meta`.
- [x] Substituir usos semânticos de `getText()` por contexts/tokens; manter
      source slice apenas como `writtenText`.
- [x] Tornar testes verdes e registrar lacunas de input explicitamente.

### Fase 3 — DataReference e expressões

- [x] Executar ciclos RED/GREEN/REFACTOR separados para nome simples,
      qualificadores, subscritos e reference modification.
- [x] Executar ciclos separados para aritmética, condições, functions, special
      registers e fallback estruturado.
- [x] Migrar IF/EVALUATE/PERFORM para expressões estruturadas.
- [x] Provar que toda referência reconhecida permanece alcançável por
      `Ast.children` e pelo snapshot.

### Fase 4 — ProcedureReference e demais referências nominais

- [ ] Executar TDD para GO TO, DEPENDING ON, PERFORM, THRU/THROUGH e
      qualificação por section.
- [ ] Cobrir procedure names de ALTER/SORT/MERGE conforme o manifesto.
- [ ] Introduzir FileReference e demais referências nominais sem binding.
- [ ] Validar identidade, spans, texto e ordem de cada ocorrência.

### Fase 5 — Declarações e assinatura

- [ ] Executar TDD para hierarquia, FILLER e níveis especiais.
- [ ] Executar fatias independentes para REDEFINES, RENAMES, OCCURS, VALUE,
      USAGE e PICTURE.
- [ ] Preservar todas as cláusulas restantes com cobertura explícita.
- [ ] Modelar tipos de data section, LINKAGE e ProcedureSignature.
- [ ] Atualizar SymbolTable/Snapshot para novos kinds e relações não resolvidas,
      sem introduzir binding.

### Fase 6 — Statements que definem ou transportam valores

- [ ] Fortalecer MOVE/CALL com testes RED, incluindo CORRESPONDING, group move,
      modos de passagem e GIVING.
- [ ] Implementar por TDD, uma fatia por vez, ACCEPT, arithmetic statements,
      INITIALIZE, INSPECT, SET, STRING e UNSTRING.
- [ ] Implementar por TDD as operações de arquivo classificadas para modelagem
      semântica, preservando FILE/DATA references e clauses de fluxo excepcional.
- [ ] Para cada statement adiado, implementar visitor gramatical específico e
      `PreservedStatement` com `DEPENDENCY_UNKNOWN`.
- [ ] Validar as 50 fixtures contra o manifesto; nenhuma alternativa poderá cair
      em fallback silencioso.

### Fase 7 — Snapshots e observabilidade HTML

- [ ] Criar snapshot determinístico do relatório de cobertura.
- [ ] Atualizar AST/Symbol snapshots para os novos nós e relações.
- [ ] Adicionar a visão Cobertura semântica e estados de completude aos HTMLs.
- [ ] Permitir navegação de cada lacuna até AST, parse tree e fonte/proveniência.
- [ ] Regenerar `dist`, `dist-cbstm03a` e `dist-cbstm03d` somente nesta fase.

### Fase 8 — Regressão e encerramento

- [ ] Executar integralmente a estratégia de regressão descrita acima.
- [ ] Produzir o relatório com diferenças esperadas e cobertura pendente.
- [ ] Revisar o diff para excluir resolver, bindings, CFG, dataflow, SQL e fatos
      de dependência.
- [ ] Verificar hashes do corpus, baselines e plano suspenso.
- [ ] Validar todos os critérios de aceite.
- [ ] Criar commit isolado somente após aprovação dos resultados.

## Critérios de aceite objetivos

1. A cobertura é derivada das 628 regras das duas gramáticas inteiras; todas as
   regras e as 50 alternativas de statement estão classificadas e protegidas
   por teste de guarda.
2. Nenhuma decisão de suporte usa presença/ausência no corpus ou parsing textual
   heurístico.
3. DataReference preserva estruturalmente base, texto, qualifiers, grupos de
   subscritos e reference modification, com expressões e spans próprios.
4. ProcedureReference é primeira classe em toda ocorrência de procedureName
   relevante e não contém binding.
5. Expressões não escondem silenciosamente referências reconhecidas.
6. Hierarquia, REDEFINES, RENAMES, 66, 88, OCCURS, VALUE, USAGE, LINKAGE,
   assinatura e origem de COPY são tipados ou explicitamente preservados com
   cobertura não enganosa.
7. Statements capazes de definir/transportar valores seguem a classificação
   aprovada; os adiados preservam operandos e produzem `DEPENDENCY_UNKNOWN`.
8. Construções modeladas, preservadas, não suportadas e ausentes por input são
   distinguíveis por programa, regra, arquivo e linha.
9. O sistema nunca converte CALL/referência desconhecida em “nenhuma
   dependência” e nunca declara completude com lacunas relevantes.
10. Ast, SymbolTable e cobertura são imutáveis e não contêm symbol IDs
    resolvidos, CFG ou dados de fluxo.
11. Todas as fixtures positivas/limites e a suíte de regressão passam.
12. CBSTM03D mantém os fatos essenciais de seus 14 CALLs e dois MOVEs.
13. Mudanças de IDs/métricas são determinísticas, semanticamente justificadas e
    documentadas no relatório.
14. Fontes COBOL, baselines e `reference-resolution-tasklist.md` permanecem byte
    a byte inalterados.
15. Nenhuma resolução, CFG, reaching definitions, propagação de constantes,
    análise SQL ou fato final de dependência é implementado.

## Riscos e decisões que exigem aprovação

1. **Contrato da linguagem:** a cobertura será integral para `Cobol.g4` e
   `CobolPreprocessor.g4` versionadas; dialetos fora delas serão frontend
   unsupported.
2. **Amplitude:** 31 statements terão modelo semântico neste hardening; 16 terão
   preservação estrutural explícita e 3 linguagens embutidas ficarão opacas.
3. **Breaking change controlado:** DataEntry, expressions, Procedure/GoTo e Meta
   mudarão estruturalmente; IDs e contagens serão regenerados e documentados.
4. **Source map de COPY:** entra no escopo porque sem ele a origem de produção é
   enganosa.
5. **Cobertura conservadora:** `COACTUPC` permanecerá incompleto enquanto seus
   três COPYs estiverem ausentes, mesmo com parsing sintático sem erros.
6. **Sem resolver:** referências serão estruturadas, mas nenhuma será ligada a
   símbolo neste passo.

## Aprovação

Esta tasklist é somente uma proposta de execução. Nenhuma fase, teste, alteração
de código, regeneração ou commit poderá começar sem aprovação explícita do
usuário para as decisões acima.
