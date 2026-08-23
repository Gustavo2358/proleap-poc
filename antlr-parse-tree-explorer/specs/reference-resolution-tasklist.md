# SDD — Resolução de referências COBOL para análise de produção

## Estado deste documento

Plano revisado depois da conclusão do `semantic-model-hardening-tasklist.md`.
Esta especificação substitui as premissas do plano inicial, mas **não autoriza
implementação**. A execução somente poderá começar após aprovação explícita.

## Contexto e motivação

O semantic model hardening transformou o explorer em uma fundação semântica
conservadora:

- a AST é imutável e preserva texto, spans, origem na parse tree, arquivo
  original e cadeia de COPY;
- `DataReference` representa base, qualificadores `OF`/`IN`, subscritos e
  reference modification;
- `ProcedureReference`, `FileReference`, `ProgramReference` e
  `IndexReference` são nós de primeira classe;
- declarações DATA, níveis 66/88, REDEFINES, RENAMES, OCCURS, VALUE, USAGE,
  LINKAGE e assinatura da Procedure Division são tipadas ou preservadas
  explicitamente;
- as 628 regras das gramáticas e as 50 alternativas de `statement` têm
  classificação verificável;
- construções ainda não interpretadas permanecem observáveis como
  `PRESERVED_UNINTERPRETED`, `UNSUPPORTED`, `INPUT_MISSING` ou
  `DEPENDENCY_UNKNOWN`.

O próximo passo pode, portanto, responder **“a qual declaração ou entidade
semântica este uso se refere?”**. Essa resposta precisa funcionar por unidade de
compilação, ser determinística, conservadora, configurável por dialeto e ter
custo previsível para futura execução sobre milhões de linhas.

Esta etapa não responderá **“qual valor esta variável possui neste ponto?”**.
No caso:

```text
MOVE 'CBSTM03B' TO WS-CALL-TARGET
CALL WS-CALL-TARGET
```

a resolução ligará cada uso estruturado de `WS-CALL-TARGET` à declaração DATA.
Somente CFG + reaching definitions + propagação de constantes poderão concluir
posteriormente que o CALL alcança `CBSTM03B`.

## Objetivo

Construir uma etapa isolada de **resolução de referências (name binding)** que:

- resolva referências DATA, PROCEDURE, FILE, INDEX e PROGRAM cobertas pela AST;
- aplique qualificação e visibilidade COBOL, sem busca textual heurística;
- preserve todos os candidatos quando o resultado for ambíguo;
- diferencie `RESOLVED`, `AMBIGUOUS`, `UNRESOLVED` e `UNSUPPORTED`;
- nunca converta ausência, ambiguidade ou falta de suporte em “não existe
  dependência”;
- componha seus resultados com as lacunas já conhecidas do frontend;
- seja executável por compilation unit, sem exigir carregar uma codebase inteira
  em memória;
- ofereça contratos para um futuro catálogo externo de programas, sem construir
  esse catálogo neste passo;
- produza snapshots e uma nova página HTML que expliquem e tornem auditável
  cada decisão.

## Premissas antigas que não são mais válidas

O plano anterior será atualizado nos seguintes pontos:

1. **Não é mais necessário criar `ProcedureReference`.** GO TO, PERFORM,
   THRU/THROUGH, ALTER, SORT e MERGE já preservam ocorrências com identidade,
   texto, qualificação, `Meta` e `SourceSpan`.
2. **Referências qualificadas não são mais strings achatadas.** A resolução
   deverá consumir `DataQualifier`, `SubscriptGroup` e
   `ReferenceModification`, nunca reconstruir estrutura com regex ou
   `getText()`.
3. **Não basta resolver DATA e PROCEDURE.** A AST agora expõe FILE, PROGRAM e
   INDEX; ignorá-los criaria falsos negativos para arquivos e subprogramas.
4. **`Ast.Program + SymbolTable` não são inputs suficientes para completude.**
   O resolver também precisa receber identidade da unidade, política de dialeto
   e lacunas de `AstBuildResult`/preprocessamento para não esconder COPYs
   ausentes ou nós opacos.
5. **IDs inteiros isolados não bastam em uma codebase.** Todo ID deverá ser
   namespaced por uma unidade de análise; estabilidade será garantida dentro de
   uma geração determinística, sem prometer estabilidade falsa após edição do
   fonte.
6. **O corpus não define as regras.** Fixtures do corpus são regressão; a matriz
   de resolução será derivada das formas da gramática versionada e de regras
   COBOL documentadas.
7. **O builder atual usa apenas o primeiro `programUnit`.** Isso bloquearia
   programas aninhados ou múltiplos top-level programs. A correção é
   pré-requisito da resolução de produção.

## Inventário concreto do estado atual

| Área | Estado após hardening | Consequência para este plano |
|---|---|---|
| `DataReference` | base, texto fiel, qualifiers, subscripts, reference modification e entendimento | consumir estrutura existente; não fazer parsing textual |
| `ProcedureReference` | primeira classe, inclusive em statements preservados | resolver cada ocorrência alcançável por `Ast.children` |
| FILE/PROGRAM/INDEX | nós próprios | criar políticas e namespaces explícitos |
| `NamedReference`/preservados | conteúdo reconhecido ou opaco com cobertura | produzir `UNSUPPORTED`/lacuna, nunca ausência silenciosa |
| Symbol Table | namespaces PROGRAM/DATA/PROCEDURE/FILE, escopos e índices locais/globais | fortalecer relações e visibilidade; não reescrever como resolver |
| SELECT + FD/SD | dois símbolos FILE independentes | modelar que podem representar uma única entidade de arquivo |
| DATA hierarchy | escopos por item e relações textuais sem binding | usar ancestry para qualificação e resolver relações em resultado separado |
| nested `programUnit` | gramática aceita; AST constrói somente o primeiro | criar modelo de compilation unit e identidade por program unit |
| GLOBAL/EXTERNAL/COMMON | texto parcialmente preservado, sem política de visibilidade | tipar atributos mínimos necessários antes do binding entre programas |
| cobertura | findings conservadores por regra/linha | compor cobertura do resolver com cobertura do frontend |
| pipeline | uma fonte por execução e outputs determinísticos | manter resolução por unidade e preparar orquestração externa futura |

## Base semântica e dialeto

A gramática ProLeap aceita COBOL amplo e extensões. Sintaxe não define sozinha
as regras de visibilidade. A implementação deverá possuir uma política
versionada, por exemplo `CobolResolutionPolicy`, com nome e versão observáveis.

A política inicial deverá documentar suas regras com fontes primárias de
linguagem, incluindo:

- IBM Enterprise COBOL — *Qualification*:
  <https://www.ibm.com/docs/en/cobol-zos/6.3?topic=reference-qualification>;
- IBM Enterprise COBOL — *References to PROCEDURE DIVISION names*:
  <https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-references-procedure-division-names>;
- IBM Enterprise COBOL — *Scope of names*:
  <https://www.ibm.com/docs/en/cobol-zos/6.4?topic=programs-scope-names>;
- IBM Enterprise COBOL — *Calling nested COBOL programs*:
  <https://www.ibm.com/docs/en/cobol-aix/5.1.0?topic=subprograms-calling-nested-cobol-programs>.

Quando uma opção do compilador alterar a semântica — por exemplo
`QUALIFY(EXTEND)` — ela deverá ser configuração explícita. Ausência de opção
não autoriza escolher a interpretação mais conveniente. Extensões aceitas pela
gramática, mas sem política implementada, deverão resultar em `UNSUPPORTED` ou
`UNRESOLVED` com motivo específico.

## Modelo conceitual antes/depois

### Antes

```text
Ast.Program (somente primeiro programUnit)
  ├─ referências estruturadas e imutáveis
  └─ cobertura do frontend

SymbolTable
  ├─ símbolos locais da única Ast.Program
  └─ IDs inteiros válidos apenas dentro da tabela
```

### Depois

```text
CompilationUnitModel
  └─ ProgramUnitModel[] (top-level e nested, com UnitId e atributos)
       ├─ Ast.Program imutável
       ├─ SymbolTable imutável
       ├─ AstScopeIndex imutável
       └─ frontend coverage

ResolutionEnvironment
  ├─ CobolResolutionPolicy versionada
  ├─ unidades da compilation unit
  └─ ExternalProgramCatalog plugável (vazio por padrão)

ReferenceResolver
  └─ ReferenceResolution imutável
       ├─ ReferenceOccurrence[]
       ├─ ResolutionEntry[]
       ├─ ResolutionDiagnostic[]
       ├─ ResolutionCoverage
       └─ índices por ocorrência, símbolo, entidade, status e unidade
```

A AST continuará sem `symbolId`, candidatos, status ou dados de fluxo.

## Decisões arquiteturais propostas

### 1. Unidade de análise e identidade

- Introduzir um modelo imutável de compilation unit capaz de representar todos
  os `programUnit` e sua relação de nesting.
- Cada programa terá `ProgramUnitId`; cada referência e símbolo será
  identificável por `(ProgramUnitId, localId)`.
- `ProgramUnitId` será determinístico dentro do artefato, derivado da posição
  estrutural e do nome canônico, com diagnóstico para nomes ausentes/duplicados.
- A origem continuará vindo de `Meta.provenance`; IDs não serão derivados
  apenas do caminho do arquivo, pois o mesmo copybook pode ser incluído várias
  vezes.
- O resolver operará uma compilation unit por vez. Um futuro batch runner
  poderá paralelizar unidades sem estado global mutável.

### 2. Fronteiras dos modelos

- `Ast`: sintaxe semântica compreendida/preservada, imutável, sem binding.
- `SymbolTable`: declarações, escopos, relações e atributos de visibilidade,
  sem usos resolvidos.
- `ReferenceResolution`: usos, candidatos, decisões e diagnósticos, separado e
  imutável.
- `ExternalProgramCatalog`: porta de consulta opcional; não é tabela de símbolos
  global nem fato de dependência.
- Snapshots/HTML dependem dos modelos, mas os modelos não dependem de JSON,
  JavaScript ou navegador.

### 3. Ocorrência e papel semântico

Cada referência alcançável deverá gerar uma `ReferenceOccurrence` com:

- ID determinístico e `ProgramUnitId`;
- `astNodeId`, `Meta`, span, proveniência e regra de origem;
- tipo: `DATA`, `CONDITION`, `INDEX`, `PROCEDURE`, `FILE`, `PROGRAM` ou
  `PRESERVED_NAMED`;
- papel: leitura, escrita, alvo de CALL, argumento de CALL, qualificador,
  subscript, offset/length, GO TO, PERFORM FROM/THROUGH, relação REDEFINES,
  RENAMES, OCCURS, file operation, assinatura etc.;
- nome escrito, base canônica e qualificadores estruturados;
- escopo de partida e política aplicada.

Um nome usado somente como qualificador terá papel `QUALIFIER_COMPONENT`; ele
não poderá ser confundido com uma leitura de valor em futuras análises def-use.
Referências dentro de subscritos e reference modification são ocorrências de
valor independentes.

### 4. Estados e motivos

Cada ocorrência terá exatamente um estado:

- `RESOLVED`: uma entidade semântica válida;
- `AMBIGUOUS`: mais de uma entidade válida após todas as regras aplicáveis;
- `UNRESOLVED`: nenhuma entidade disponível no ambiente consultado;
- `UNSUPPORTED`: a forma existe, mas a política ainda não sabe interpretá-la.

O estado sempre será acompanhado de um `ResolutionReason` estável, por exemplo:

- `UNIQUE_VISIBLE_DECLARATION`;
- `QUALIFIED_HIERARCHY_MATCH`;
- `MULTIPLE_VALID_CANDIDATES`;
- `DECLARATION_NOT_FOUND`;
- `EXTERNAL_CATALOG_NOT_PROVIDED`;
- `INPUT_INCOMPLETE`;
- `UNSUPPORTED_GRAMMAR_FORM`;
- `UNSUPPORTED_DIALECT_OPTION`;
- `INVALID_NAMESPACE_FOR_CONTEXT`.

`UNRESOLVED` não significa que a dependência inexiste. Um CALL literal cujo
programa não esteja no catálogo continua contendo um nome externo conhecido,
mas seu binding permanece não resolvido.

### 5. Entidades e aliases

- Candidatos apontarão para uma `SemanticEntityId`, não apenas para uma lista
  crua de declarações.
- SELECT e FD/SD homônimos poderão ser declarações da mesma entidade FILE; isso
  não deverá produzir ambiguidade artificial.
- REDEFINES e RENAMES continuarão símbolos distintos, ligados por relações
  resolvidas separadamente; não serão colapsados como aliases equivalentes.
- Condition names de nível 88 e index names manterão seus kinds, ancestry e
  regras de visibilidade.
- Relações terão diagnósticos próprios e nunca serão escritas de volta na AST.

### 6. Resolução DATA e INDEX

- A busca começa no programa que contém o uso e aplica escopo/visibilidade da
  política COBOL.
- Nomes simples são comparados case-insensitively pela forma canônica.
- Qualificadores precisam corresponder a ancestrais válidos na mesma hierarquia,
  na ordem estrutural de dentro para fora; níveis intermediários poderão ser
  omitidos somente quando permitido pela política.
- `IN` e `OF` são semanticamente equivalentes, mas a grafia permanece no nó.
- Qualificação por FILE precisa consultar a entidade FILE, não um DATA item de
  mesmo nome.
- Formas `paragraphName IN section`, `textName IN library` e
  `LINAGE-COUNTER IN file` da regra `qualifiedDataName` deverão ser classificadas
  explicitamente; nenhuma será tratada como DATA comum por acidente.
- `QUALIFY(EXTEND)` terá comportamento configurável e testes próprios.
- Nomes de nível 88, índices de OCCURS, REDEFINES, RENAMES, OCCURS DEPENDING/KEY
  e parâmetros LINKAGE/USING terão casos específicos.
- GLOBAL/EXTERNAL e nesting obedecerão a uma busca documentada. Se o suporte
  necessário não estiver tipado, a referência ficará `UNSUPPORTED`, não será
  procurada globalmente por conveniência.

### 7. Resolução PROCEDURE

- Section names e paragraph names só serão buscados dentro do mesmo programa.
- Paragraph qualificado deverá corresponder exatamente à section informada.
- Paragraph não qualificado dentro de sua própria section seguirá a regra
  contextual da política; duplicatas válidas fora dela não serão escolhidas por
  ordem de aparição.
- GO TO DEPENDING ON produzirá ocorrências independentes para cada target; sua
  expressão seletora DATA também será resolvida.
- PERFORM FROM e THROUGH terão resultados independentes.
- Procedure references reconhecidas em ALTER, SORT e MERGE serão resolvidas,
  mesmo que a semântica completa desses statements continue preservada; o
  container continuará bloqueando completude de dependências quando aplicável.
- Procedure names jamais serão resolvidos entre programas.

### 8. Resolução FILE

- `FileReference` será ligada à entidade FILE formada por SELECT e/ou FD/SD.
- Ausência de uma das declarações será observável, sem inventar a outra.
- Referências de operações OPEN/READ/WRITE/REWRITE/DELETE/START/RETURN,
  SORT/MERGE e parâmetros de CALL serão classificadas por papel.
- ASSIGN/DDNAME continuará sendo atributo preservado da entidade. Extrair o
  fato final “programa usa DDNAME X” pertence à etapa de fatos de dependência,
  não a este resolver.

### 9. Resolução PROGRAM

- CALL literal poderá resolver primeiro programas internos visíveis segundo
  nesting e COMMON.
- Depois poderá consultar `ExternalProgramCatalog`, se fornecido.
- O catálogo retornará identidades/candidatos, não conteúdo de programa nem
  fatos finais de dependência.
- Sem catálogo, nome externo ficará `UNRESOLVED/EXTERNAL_CATALOG_NOT_PROVIDED`;
  o nome literal continuará preservado para a etapa de dependências.
- CALL dinâmico resolverá apenas a `DataReference` que contém o target. Não
  tentará descobrir valores ou programas possíveis.
- Names de `ENTRY` e outras formas gramaticais PROGRAM serão inventariados e
  classificados; formas adiadas ficarão explícitas.

### 10. Cobertura e completude

O resultado deverá compor:

- cobertura da transformação Parse Tree → AST;
- erros léxicos/sintáticos e COPYs ausentes;
- referências coletadas por tipo/papel;
- estados e motivos da resolução;
- containers preservados/opacos que ainda podem esconder referências.

Serão expostos separadamente:

- `referenceBindingComplete`: todas as referências reconhecidas foram tratadas
  sem ambiguity/unresolved/unsupported relevante;
- `dependencyAnalysisReady`: não existem lacunas conhecidas que possam ocultar
  dependências para o objetivo selecionado.

Nenhum deles poderá ser `true` quando input incompleto ou uma construção
relevante estiver opaca. “Não encontrado” jamais será exibido como “nenhuma
dependência”.

### 11. Escala e determinismo

- Proibir algoritmo global `O(referências × símbolos)`.
- Construir índices imutáveis por unidade, namespace, nome canônico, entidade,
  ancestry e visibilidade.
- Lookup nominal deverá ser proporcional ao número de candidatos do mesmo nome,
  não ao total de símbolos da codebase.
- Resultados e diagnósticos terão ordem estável independente de `HashMap`.
- O resolver será reentrante/thread-safe ou instanciado por unidade sem estado
  estático mutável.
- Snapshots não duplicarão source text completo por ocorrência.
- Métricas registrarão tempo por etapa, cardinalidade dos índices, quantidade e
  máximo de candidatos, sem incorporar limites absolutos dependentes de uma
  máquina específica aos testes funcionais.
- Uma fixture sintética parametrizável com milhares de símbolos/referências
  verificará crescimento e ausência de varredura global por lookup.

## Matriz inicial de cobertura de referências

| Nó/forma | Namespace/entidade | Tratamento neste passo |
|---|---|---|
| `DataReference` base | DATA/CONDITION | resolver semanticamente |
| `DataQualifier` | DATA ou FILE conforme forma gramatical | resolver como componente não-value |
| referências em subscript/refmod | DATA/INDEX | resolver como ocorrências de leitura |
| `IndexReference` | INDEX_NAME no namespace DATA | resolver semanticamente |
| `ProcedureReference` | PARAGRAPH/PROCEDURE_SECTION | resolver semanticamente |
| `FileReference` | entidade FILE | resolver semanticamente |
| `ProgramReference` | programa interno ou catálogo externo | resolver quando disponível; senão explícito |
| `NamedReference` | determinado pela regra de origem | classificar; `UNSUPPORTED` se sem contrato seguro |
| referências reconhecidas em nós preservados | namespace do nó filho | resolver o filho; manter container incompleto |
| texto opaco sem nó reconhecido | desconhecido | finding de cobertura; não extrair por texto |
| special registers/intrinsics | builtin, sem declaração do usuário | classificar como não sujeito a binding |

A matriz será completada mecanicamente contra todas as regras
`EXPRESSION_REFERENCE`, `DATA_DECLARATION`, `ENVIRONMENT_DECLARATION`,
`PROCEDURE_STRUCTURE`, `STATEMENT` e `STATEMENT_COMPONENT` do manifesto. Presença
no corpus não altera a classificação.

## Estratégia TDD obrigatória

Cada fatia de cada fase seguirá este ciclo:

1. registrar comportamento/métricas atuais;
2. criar fixture COBOL mínima derivada da forma gramatical ou regra semântica;
3. escrever primeiro o teste do contrato público e do comportamento;
4. executar o teste e registrar RED pela funcionalidade ausente;
5. implementar a menor mudança que o torne GREEN;
6. executar testes focados e depois a suíte completa;
7. refatorar somente com tudo verde;
8. registrar evidência RED/GREEN/REFACTOR no relatório da fase;
9. criar commit isolado da fase somente após revisão de escopo.

Testes validarão contratos e fatos semânticos, não nomes de métodos privados,
ordem acidental de coleções ou layout interno de mapas.

Casos positivos, negativos e limites deverão incluir:

- nome DATA simples único, duplicado, ausente e kind incompatível;
- hierarquia de grupos com qualificação parcial, completa e fora de ordem;
- `OF`, `IN`, múltiplos qualifiers e qualifier por FILE;
- `QUALIFY(EXTEND)` ligado/desligado/desconhecido;
- condition name 88 e index name;
- referências em subscritos, múltiplos subscritos e reference modification;
- REDEFINES, RENAMES FROM/THROUGH, OCCURS DEPENDING/KEY/INDEXED;
- WORKING-STORAGE, LOCAL-STORAGE, LINKAGE e FILE SECTION;
- GLOBAL, EXTERNAL, shadowing e programas nested/siblings;
- GO TO simples, qualificado e DEPENDING ON com múltiplos targets;
- PERFORM simples e THRU, paragraph duplicado em sections distintas;
- procedure reference inválida entre programas;
- FILE com SELECT+FD, apenas SELECT, apenas FD e nomes duplicados;
- CALL literal interno, COMMON, externo com/sem catálogo e ambíguo;
- CALL dinâmico ligando somente a variável;
- referência reconhecida em statement preservado;
- `NamedReference` e forma gramatical ainda não suportada;
- COPY aninhado, repetido, REPLACING e ausente;
- origem, spans, UnitId, IDs e ordem determinísticos.

## Estratégia de regressão

Ao final de cada fase e novamente no encerramento:

- executar toda a suíte Maven;
- executar `node --check` em todos os JavaScripts existentes;
- validar o manifesto das 628 regras e as 50 alternativas de statement;
- analisar COACTUPC, CBSTM03A e CBSTM03D sem novos erros léxicos/sintáticos;
- confirmar que CBSTM03D mantém 14 CALLs dinâmicos, zero estáticos, 14 targets
  `WS-CALL-TARGET` e os dois MOVEs;
- confirmar que cada um dos 14 targets liga à mesma declaração DATA, sem inferir
  `CBSTM03B`/`CEE3ABD` como valores;
- confirmar que os fatos essenciais atuais da Symbol Table não desaparecem;
- confirmar que os 14 CALLs literais de CBSTM03A continuam nomes PROGRAM
  preservados e não viram “sem dependência” quando não há catálogo;
- confirmar que COACTUPC continua incompleto devido aos três COPYs ausentes e
  CICS opaco;
- executar todas as fixtures sintéticas, inclusive as formas ausentes do corpus;
- comparar métricas antes/depois por fatos, explicando novos nós, símbolos,
  entidades, IDs e resultados;
- executar duas gerações e comparar hash agregado dos outputs;
- validar navegação Parse Tree ↔ AST ↔ Symbol Table ↔ Resolução ↔ Cobertura;
- validar HTML sem dependências externas;
- executar a fixture de escala e registrar cardinalidades/tempo sem threshold
  frágil de hardware;
- verificar que corpus, gramáticas, baselines e fontes COBOL permanecem byte a
  byte inalterados;
- revisar o diff para excluir CFG, reaching definitions, propagação de
  constantes, análise SQL e fatos finais de dependência;
- produzir relatório final com aprovados, diferenças esperadas, lacunas e
  capacidade declarada por programa.

Não usar igualdade cega de contagem de nós como único oráculo: compilation unit,
program nesting, entidades e novos snapshots alterarão métricas legitimamente.

## Observabilidade para produção

Por compilation unit e program unit, o relatório deverá expor:

- policy/dialeto e opções efetivamente usadas;
- hashes/identidade do input e estado de preprocessamento;
- referências por tipo, papel, namespace, estado e motivo;
- candidatos por ocorrência, distribuição e máximo de cardinalidade;
- relações SELECT↔FD, REDEFINES, RENAMES e demais bindings declarativos;
- referências externas sem catálogo e resultados retornados por catálogo;
- lacunas herdadas do frontend por regra, arquivo, linha e include chain;
- tempo e tamanho dos índices por etapa;
- `referenceBindingComplete` e `dependencyAnalysisReady`, com todos os motivos
  bloqueantes;
- links determinísticos para AST, símbolo/entidade, parse tree e fonte.

O relatório não poderá usar “análise completa” quando qualquer lacuna relevante
existir.

## Escopo

- requisitos mínimos de compilation unit/nested programs necessários ao binding;
- atributos de declaração/visibilidade necessários às regras COBOL;
- relações semânticas entre declarações da mesma entidade;
- coleta tipada de ocorrências já estruturadas na AST;
- resolução DATA, CONDITION, INDEX, PROCEDURE, FILE e PROGRAM;
- política inicial versionada e porta de catálogo externo;
- diagnósticos, cobertura, snapshots e jornada HTML;
- testes gramaticais, semânticos, integração, regressão e escala.

## Fora do escopo

- varrer ou indexar uma codebase inteira;
- persistência/distribuição de índices, cache incremental ou banco de dados;
- CFG, blocos básicos ou arestas de controle;
- reaching definitions, def-use, propagação de constantes ou merge de branches;
- descobrir valores possíveis de CALL dinâmico;
- converter resoluções em fatos finais de dependência;
- interpretar ASSIGN como DDNAME final;
- parsear EXEC SQL/SQL dinâmico ou descobrir tabelas;
- interpretar CICS/SQLIMS ou texto opaco;
- extrair referências por regex/palavras do texto COBOL;
- alterar o corpus para fazer exemplos passarem;
- suportar dialetos fora da gramática/política configurada sem diagnóstico.

## Tasklist passo a passo

### Fase 0 — Baseline e guarda de escopo

- [x] Registrar commit, status e hashes de código, gramáticas, corpus, outputs e
      especificações.
- [x] Registrar métricas dos três programas e os 23 testes existentes.
- [x] Criar testes de caracterização para primeiro `programUnit`, scopes,
      SELECT/FD duplicados e referências atualmente alcançáveis.
- [x] Criar `reference-resolution-regression-report.md` com baseline e seção de
      evidências TDD por fase.
- [x] Confirmar por teste que nenhuma resolução já ocorre na AST/Symbol Table.
- [x] Executar suíte completa e criar commit isolado somente com baseline/testes.

### Fase 1 — Matriz semântica e contratos de cobertura

- [ ] Inventariar mecanicamente todas as formas de nome/referência nas 628 regras
      e relacioná-las aos nós AST, namespaces, roles e política.
- [ ] Documentar regras COBOL/dialeto e opções configuráveis com fontes primárias.
- [ ] Escrever teste RED para regra/formato relevante sem classificação.
- [ ] Implementar manifesto versionado de resolução, sem resolver nomes ainda.
- [ ] Definir contratos imutáveis de policy, UnitId, reference kind/role, status,
      reason e completude.
- [ ] Tornar os testes verdes, refatorar e executar regressão completa.
- [ ] Registrar evidência e criar commit isolado da fase.

### Fase 2 — Compilation unit, nesting e visibilidade declarativa

- [ ] Criar fixtures de múltiplos top-level programs e nested programs com
      COMMON, GLOBAL, EXTERNAL, shadowing e siblings.
- [ ] Escrever testes RED provando que nenhuma `programUnit` pode desaparecer.
- [ ] Introduzir o modelo imutável de compilation unit e todos os ProgramUnitId.
- [ ] Preservar atributos PROGRAM COMMON/INITIAL/RECURSIVE e atributos DATA/FILE
      GLOBAL/EXTERNAL necessários à visibilidade.
- [ ] Evoluir Symbol Table por program unit e ancestry sem inserir usos/binding.
- [ ] Emitir diagnóstico/cobertura quando uma forma de visibilidade aceita pela
      gramática ainda não puder ser interpretada.
- [ ] Tornar testes verdes, executar regressão, documentar mudanças de métricas
      e criar commit isolado.

### Fase 3 — Entidades, relações, scope index e coleta de ocorrências

- [ ] Criar fixtures SELECT+FD/SD, SELECT-only, FD-only, REDEFINES, RENAMES,
      OCCURS e qualifiers por FILE.
- [ ] Escrever testes RED para entidade FILE única e relações declarativas.
- [ ] Implementar entidades/aliases e relações imutáveis na Symbol Table, sem
      resultados de uso.
- [ ] Implementar `AstScopeIndex` determinístico para mapear todo nó ao escopo
      sem repetir buscas ancestrais globais.
- [ ] Escrever testes RED para coleta exata de cada referência e role.
- [ ] Implementar `ReferenceOccurrenceCollector` exclusivamente via tipos AST e
      manifesto, incluindo qualifiers/subscripts/refmod e nós preservados.
- [ ] Provar que qualifiers não são value reads e que nenhuma ocorrência é
      duplicada ou perdida silenciosamente.
- [ ] Tornar testes verdes, executar regressão e criar commit isolado.

### Fase 4 — Modelo de resultado e resolução DATA/INDEX

- [ ] Criar `ReferenceResolution`, entries, candidates, diagnostics, índices e
      consultas imutáveis.
- [ ] Executar ciclos RED/GREEN/REFACTOR separados para nome simples,
      duplicidade, ausência e kind incompatível.
- [ ] Executar ciclos separados para OF/IN, qualifiers múltiplos/parciais,
      ordem inválida, FILE qualifier e `QUALIFY(EXTEND)`.
- [ ] Executar ciclos separados para nível 88, index names, sections DATA,
      GLOBAL/EXTERNAL e shadowing nested.
- [ ] Resolver relações REDEFINES/RENAMES/OCCURS em produto separado, respeitando
      restrições contextuais.
- [ ] Garantir `UNSUPPORTED` para formas sem política e diagnósticos estáveis.
- [ ] Medir lookup por índice e proibir scan global por referência em teste.
- [ ] Tornar testes verdes, executar regressão e criar commit isolado.

### Fase 5 — Resolução PROCEDURE, FILE e PROGRAM

- [ ] Executar TDD para GO TO simples/qualificado/DEPENDING ON, duplicatas por
      section e referência inválida entre programas.
- [ ] Executar TDD para PERFORM FROM/THROUGH e refs de ALTER/SORT/MERGE.
- [ ] Executar TDD para FILE entity em operações, SORT/MERGE e CALL parameters.
- [ ] Executar TDD para CALL literal de nested program, COMMON, external catalog
      ausente/presente e múltiplos candidatos.
- [ ] Confirmar que CALL dinâmico resolve somente a variável DATA.
- [ ] Implementar `ExternalProgramCatalog` mínimo e plugável com implementação
      vazia e fake de teste; não criar indexador de codebase.
- [ ] Tornar testes verdes, executar regressão e criar commit isolado.

### Fase 6 — Cobertura, diagnósticos e escala

- [ ] Compor cobertura do frontend, preprocessing, collector e resolver.
- [ ] Escrever testes RED para os quatro estados, reasons e flags conservadoras
      de completude.
- [ ] Garantir que opacos, COPY ausente e catálogo ausente nunca virem “zero
      dependências”.
- [ ] Criar fixture sintética parametrizável com milhares de símbolos e refs.
- [ ] Validar determinismo, cardinalidade dos índices, ausência de scan global,
      isolamento entre unidades e execução paralela segura.
- [ ] Registrar métricas de custo sem threshold absoluto dependente de hardware.
- [ ] Tornar testes verdes, executar regressão e criar commit isolado.

### Fase 7 — Snapshots, pipeline e jornada HTML

- [ ] Criar `ResolutionSnapshot` e `resolution-data.js` determinísticos.
- [ ] Integrar a etapa após Symbol Table em `ExplorerMain`, mantendo produtos
      separados e política/catálogo explícitos.
- [ ] Criar `resolution.html`/`resolution-app.js` com filtros por unit, kind,
      role, status e reason; busca e inspetor de candidatos/decisão.
- [ ] Adicionar pontes AST ↔ entidade/símbolo ↔ parse tree ↔ fonte e visão de
      cobertura/completude.
- [ ] Explicar visualmente que binding da variável de CALL não resolve valores.
- [ ] Atualizar navegação e README, sem dependências web externas.
- [ ] Regenerar `dist`, `dist-cbstm03a` e `dist-cbstm03d` somente nesta fase.
- [ ] Validar no navegador casos resolved/ambiguous/unresolved/unsupported.
- [ ] Executar regressão e criar commit isolado.

### Fase 8 — Regressão final e encerramento

- [ ] Executar integralmente a estratégia de regressão desta especificação.
- [ ] Validar todos os critérios de aceite e a fixture de escala.
- [ ] Produzir relatório com diferenças esperadas e cobertura pendente.
- [ ] Revisar o diff para excluir CFG, dataflow, valores de CALL, SQL e fatos
      finais de dependência.
- [ ] Verificar hashes de fontes, corpus, gramáticas e baselines.
- [ ] Apresentar os resultados para aprovação explícita.
- [ ] Criar commit isolado de encerramento somente após essa aprovação.

## Critérios de aceite objetivos

1. Todas as formas relevantes das 628 regras têm política de resolução ou
   classificação explícita; nenhuma decisão depende do corpus.
2. Nenhum `programUnit` top-level ou nested aceito pela grammar desaparece da
   AST/modelo de compilation unit.
3. AST, Symbol Table, scope index e Reference Resolution são separados,
   imutáveis e namespaced por unidade.
4. A AST não contém symbol IDs, candidatos, status ou resultados de binding.
5. Cada referência reconhecida gera exatamente uma ocorrência tipada e um
   resultado; texto opaco gera lacuna explícita, não referência inventada.
6. Qualifiers, subscripts e reference modification preservam roles corretos;
   qualifiers não são classificados como value reads.
7. DATA qualification respeita hierarchy, ordem, FILE namespace e policy de
   dialeto, incluindo comportamento explícito de `QUALIFY(EXTEND)`.
8. Nível 88, INDEX, REDEFINES, RENAMES, OCCURS, LINKAGE, GLOBAL, EXTERNAL e
   shadowing têm testes positivos e negativos.
9. GO TO, GO TO DEPENDING ON, PERFORM e THRU resolvem cada target de forma
   independente e nunca atravessam program unit.
10. SELECT e FD/SD compatíveis formam uma entidade FILE sem falsa ambiguidade;
    inconsistências permanecem diagnosticadas.
11. CALL literal interno respeita nesting/COMMON; CALL externo usa catálogo
    opcional ou permanece explicitamente unresolved.
12. CALL dinâmico resolve apenas sua referência DATA e não produz valores ou
    targets possíveis.
13. Os estados `RESOLVED`, `AMBIGUOUS`, `UNRESOLVED` e `UNSUPPORTED` são
    exercitados e sempre possuem reason/candidatos coerentes.
14. Lacuna relevante do frontend, input ou resolver impede completude e nunca
    é convertida em “nenhuma dependência”.
15. Lookup não faz varredura de todos os símbolos por referência; índices e
    fixture de escala comprovam custo proporcional aos candidatos nominais.
16. Resultados, diagnósticos, IDs e snapshots são determinísticos em duas
    gerações.
17. CBSTM03D mantém 14 CALLs dinâmicos ligados à única declaração
    `WS-CALL-TARGET`, sem inferência de `CBSTM03B`/`CEE3ABD`.
18. COACTUPC continua declarado incompleto enquanto COPYs/CICS permanecerem
    opacos; CBSTM03A não perde seus nomes PROGRAM externos.
19. Jornada HTML permite auditar uso, escopo, candidates, decisão e origem sem
    dependências externas.
20. Todos os testes, fixtures, JS checks, hashes e regressões passam.
21. Nenhum CFG, reaching definitions, propagação de constantes, análise SQL,
    DDNAME final ou fato final de dependência é implementado.

## Riscos e decisões que exigem aprovação

1. **Compilation unit entra como pré-requisito.** É uma mudança estrutural maior
   que o plano antigo, necessária porque hoje somente o primeiro `programUnit`
   é analisado.
2. **Symbol Table será fortalecida, não substituída.** Ela ganhará identidade de
   unidade, visibility/ancestry e relações de entidade; continuará sem usos.
3. **A política inicial será explícita e versionada.** Opções/dialetos não
   configurados não serão adivinhados.
4. **PROGRAM externo pode permanecer unresolved.** Isso não apaga o nome literal
   nem impede a futura etapa de registrar dependência declarada; apenas informa
   que a declaração externa não estava no ambiente.
5. **Catálogo externo será somente uma porta.** Indexar uma codebase massiva é
   uma etapa futura e não será antecipada.
6. **Cobertura conservadora permanece.** Resolver referências reconhecidas não
   torna um programa completo quando statements/COPY/linguagens embutidas ainda
   podem esconder dependências.
7. **Outputs HTML só serão regenerados na Fase 7.** Fases anteriores validarão
   domínio e snapshots por testes.
8. **Um commit por fase.** A execução poderá ser agrupada em lotes aprovados,
   mas cada fase terá teste, revisão e commit próprios.

## Aprovação solicitada

Esta especificação revisada é apenas o plano. Nenhum teste, código, artefato
gerado ou commit de implementação deverá ser iniciado antes da aprovação
explícita destas decisões de escopo e arquitetura.
