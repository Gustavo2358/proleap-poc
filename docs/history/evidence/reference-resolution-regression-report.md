# Relatório de regressão — Resolução de referências

> Evidência histórica arquivada. Para o contrato atual, consulte [resolução de referências](../../domain/reference-resolution.md).

## Estado

Concluído. A auditoria da Fase 8 e a criação do commit isolado de encerramento
foram aprovadas explicitamente após a apresentação dos resultados.

## Baseline pós-hardening

- commit inicial: `c08e3f269ea2571afd17c44be0d95f26a82feb97`;
- testes Maven antes desta etapa: 23, todos verdes;
- Java de produção, SHA-256 agregado: `af7b168665151b1d8fc5d8ad19b431a33ccbe4871fce7227e949f3af4784a833`;
- gramáticas, SHA-256 agregado: `4be036fc47a73e32dd68de6a3ab0008ef81895f52372046f6a35e986e92b202e`;
- corpus interno, SHA-256 agregado: `f89cf02782dc26f3a4a79e37fc98c35112f0003d36423e674785251152b4b10e`;
- outputs versionados, SHA-256 agregado: `04fb0d56bb4ba9a7c4c6b430253bba8b6b1526b36d85a00b76a28e2774dd742b`;
- tasklist aprovada, SHA-256:
  `cd2c3c978c54c648b898ec2330e5ecdf68aa85c7c4128dda0914d1bcbbd0ce9c`.

Os agregados foram calculados ordenando os caminhos completos relativos à raiz
do repositório, aplicando SHA-256 a cada arquivo e depois ao conjunto ordenado.

Fontes principais:

| Fonte | SHA-256 |
|---|---|
| `../cbl/CBSTM03A.CBL` | `23c8753b6b4e0c24d4560c83861fe8162626bab195faec0fe88cf80b8bf432b5` |
| `corpus/cbl/CBSTM03D.CBL` | `d75535258cb80c8777993b6662146ed7f2f8cc5888a34d648b5ea68c310a7fac` |
| `corpus/cbl/COACTUPC.cbl` | `b5bb7d6ccad022e0fc91b4dd1e971f49d184adf89b56abdce14eccff35b39396` |

## Métricas semânticas iniciais

| Programa | Parse tree | AST | Profundidade AST | CALL estático | CALL dinâmico | Escopos | Símbolos | Diagnósticos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| COACTUPC | 57.227 | 9.189 | 11 | 1 | 0 | 651 | 853 | 2 |
| CBSTM03A | 11.795 | 2.740 | 11 | 14 | 0 | 219 | 209 | 0 |
| CBSTM03D | 11.916 | 2.752 | 11 | 0 | 14 | 221 | 211 | 0 |

Todos têm zero erros léxicos/sintáticos. COACTUPC mantém três COPYs ausentes e
14 CICS opacos. CBSTM03D mantém 14 targets `WS-CALL-TARGET` e dois MOVEs de
literal para essa variável.

## Caracterização anterior à resolução

- a gramática aceita múltiplos e nested `programUnit`, mas `AstBuilder` retorna
  somente o primeiro encontrado;
- SELECT e FD/SD homônimos são hoje dois símbolos FILE independentes;
- DATA, PROCEDURE, FILE, PROGRAM e INDEX já possuem referências estruturadas;
- AST e Symbol Table não contêm resultado de binding;
- nenhuma referência possui `symbolId`, candidatos ou status de resolução.

Esses fatos são protegidos por
`ReferenceResolutionBaselineCharacterizationTest`; mudanças futuras deverão ser
intencionais, TDD-first e explicadas aqui.

## Resultados por fase

| Fase | Estado | Evidência | Diferenças esperadas | Pendências |
|---|---|---|---|---|
| 0 — baseline | aprovada | 26 testes verdes; três caracterizações novas | nenhuma mudança de produção | nenhuma |
| 1 — matriz/contratos | aprovada | 28 testes e todos os JS verdes; guarda das 628 regras | manifesto/policy/contratos imutáveis, sem binding | regras conservadoras serão refinadas por TDD nas fases de resolução |
| 2 — compilation unit | aprovada | 31 testes e todos os JS verdes; fixture com cinco unidades | modelo completo, IDs namespaced, ancestry e visibilidade tipada | binding entre unidades permanece deliberadamente ausente |
| 3 — entidades/coleta | aprovada | 35 testes e todos os JS verdes; oráculo exato de 16 ocorrências | entidades FILE, relações nominais, scope index O(1) e occurrences tipadas | lookup/candidatos/binding permanecem deliberadamente ausentes |
| 4 — DATA/INDEX | aprovada | 42 testes e todos os JS verdes; sete testes novos de binding | resultado imutável, DATA/CONDITION/INDEX, relações e métricas de índice | namespaces PROCEDURE/FILE/PROGRAM continuam UNSUPPORTED até a Fase 5 |
| 5 — PROCEDURE/FILE/PROGRAM | aprovada | 49 testes e todos os JS verdes; sete testes novos | resolver composto, catálogo externo plugável e admissibleKinds | cobertura/completude agregada permanece para a Fase 6 |
| 6 — cobertura/escala | aprovada | 53 testes e todos os JS verdes; escala com 1.200 declarações | relatório conservador, gaps e métricas de custo | snapshot/HTML permanecem para a Fase 7 |
| 7 — HTML | aprovada | 56 testes, JS verdes, navegador e hashes determinísticos | snapshot/pipeline e quarta página da jornada | regressão final permanece para a Fase 8 |
| 8 — regressão final | aprovada | 56 testes, 31 JS, duas gerações e 21 critérios auditados | nenhuma mudança funcional | nenhuma |

## Evidência TDD

### Fase 0

Testes de caracterização registram comportamento existente; por definição não
há RED funcional nesta fase. Eles serão os oráculos que ficarão RED quando as
mudanças intencionais das Fases 2 e 3 começarem.

A suíte completa terminou com 26 testes, zero falhas, zero erros e zero testes
ignorados. Nenhum arquivo de produção, grammar, corpus ou output foi alterado.

### Fase 1

1. RED: `ReferenceResolutionManifestTest` falhou na compilação porque
   `ResolutionContracts` e `ReferenceResolutionManifest` ainda não existiam.
2. GREEN: os contratos passaram a expor UnitId namespaced, kinds, roles, policy
   versionada, `QUALIFY` explícito, completude conservadora, os quatro status e
   reasons estáveis, sem implementar resolver ou candidatos.
3. GREEN: o manifesto de resolução passou a expandir deterministicamente as
   mesmas 628 chaves do manifesto das grammars. Origens DATA/CONDITION/INDEX,
   PROCEDURE/FILE/PROGRAM, qualifiers, relações e built-ins usam overrides
   exatos; as demais regras herdam somente classificação conservadora.
4. REFACTOR: lookup do manifesto passou a usar índice imutável O(1), e uma
   guarda falha se um override deixar de existir após mudança da grammar.
5. GUARD: a suíte completa terminou com 28 testes verdes e todos os JavaScripts
   passaram em `node --check`. AST, Symbol Table, corpus, grammars e outputs não
   foram modificados; nenhuma referência foi ligada a símbolo nesta fase.

### Fase 2

1. RED: `CompilationUnitModelTest` falhou na compilação pela ausência do modelo
   de compilation unit, do builder multi-programa, da visibilidade tipada e das
   tabelas namespaced.
2. GREEN: a fixture cobre dois top-level programs, três nested programs em dois
   níveis, siblings e shadowing. Todos os cinco `programUnit` reconhecidos pela
   grammar são materializados em ordem estrutural, com ancestry explícita.
3. GREEN: cada programa recebe `ProgramUnitId` determinístico e namespace local
   de IDs AST. COMMON, INITIAL, RECURSIVE, LIBRARY e DEFINITION são atributos
   imutáveis de PROGRAM; GLOBAL/EXTERNAL são visibilidade tipada em DATA e FILE,
   sem remover as cláusulas originalmente preservadas.
4. GREEN: `CompilationUnitSymbolTables` mantém uma tabela de declarações por
   unidade e sua ancestry. Os atributos declarativos chegam aos símbolos, mas
   nenhuma busca entre unidades ou ligação de uso foi executada.
5. GUARD: GLOBAL+EXTERNAL conflitantes geram diagnóstico explícito em vez de
   escolher uma visibilidade silenciosamente. O método legado `build` continua
   retornando o primeiro programa, enquanto o novo contrato completo é usado
   por `buildCompilationUnit`.
6. REGRESSÃO: a suíte completa terminou com 31 testes verdes; os três
   JavaScripts passaram em `node --check`; `git diff --check` não encontrou
   erros. Grammar, corpus, fontes de demonstração, HTML e outputs não foram
   alterados nesta fase.

### Fase 3

1. RED: `EntityScopeAndOccurrenceTest` falhou na compilação pela ausência de
   entidades, relações, `AstScopeIndex`, modelo de ocorrência e collector.
2. GREEN: SELECT+FD homônimos formam uma entidade FILE com duas declarações;
   SELECT-only, FD-only e SD-only continuam entidades explícitas com uma
   declaração. Nenhuma entidade contém resultado de binding.
3. GREEN: REDEFINES, RENAMES FROM/THROUGH e OCCURS DEPENDING/KEY/INDEX são
   relações nominais imutáveis. Elas preservam owner, nó AST da referência e
   texto escrito, sempre com `bindingStatus=NOT_PERFORMED`.
4. GREEN: `AstScopeIndex` mapeia cada nó AST exatamente uma vez ao escopo dono
   mais interno e oferece consulta O(1). A fixture prova cobertura de todos os
   nós e determinismo entre construções.
5. GREEN: `ReferenceOccurrenceCollector` percorre somente tipos AST e o
   manifesto. O oráculo exato cobre 16 ocorrências de relações, referência
   qualificada, qualificador FILE inequívoco na grammar, subscript, reference
   modification e statement preservado; uma segunda fixture cobre FILE,
   PROGRAM, PROCEDURE e DATA em operações e CALL/GO TO.
6. CONSERVADORISMO: `IN nome` sintaticamente ambíguo não é promovido a FILE por
   aparência do nome ou pelo corpus. Somente a forma que a grammar identifica
   como `inFile` recebe kind FILE. Operandos de statements ainda sem contrato
   direcional recebem `CONTEXT_DEPENDENT`, nunca read/write inventado.
7. GUARD: cada nó de referência pode originar uma única ocorrência; tentativa
   de visita duplicada falha explicitamente. Qualificadores têm role
   `QUALIFIER_COMPONENT` e não são contabilizados como value reads.
8. REGRESSÃO: a suíte completa terminou com 35 testes verdes; os três
   JavaScripts passaram em `node --check`; `git diff --check` não encontrou
   erros. Grammar, corpus, programas de demonstração, HTML e outputs não foram
   alterados nesta fase; contagens existentes de escopos/símbolos permanecem.

### Fase 4

1. RED: `DataAndIndexReferenceResolverTest` falhou na compilação pela ausência
   de `ReferenceResolution`, candidates, relações resolvidas e resolver.
2. GREEN: cada ocorrência recebe entry imutável com status, reason, candidatos
   namespaced por `SemanticEntityId`, diagnósticos e consultas indexadas. AST e
   Symbol Table não recebem IDs de binding.
3. GREEN: nomes simples únicos, duplicados, ausentes e incompatíveis são
   diferenciados. DATA, nível 88/CONDITION e INDEX preservam seus kinds.
4. GREEN: OF e IN, qualificação parcial/múltipla, ordem inválida e ancestry de
   FILE SECTION são resolvidos estruturalmente. Nenhum qualifier é reconstruído
   por regex ou source text.
5. POLICY: STANDARD mantém todos os matches; EXTEND prefere o único match
   totalmente qualificado conforme IBM; UNSPECIFIED retorna
   `UNSUPPORTED_DIALECT_OPTION` quando as opções divergiriam.
6. VISIBILIDADE: declaração local sombreia ancestral; somente declaração
   ancestral tipada GLOBAL é importada. EXTERNAL local permanece explícita.
7. RELAÇÕES: REDEFINES, RENAMES e OCCURS têm produto de binding separado,
   reutilizando a identidade da ocorrência nominal sem escrever na Symbol
   Table.
8. ESCALA: índices por unit/name são construídos uma vez; métricas contam
   lookups, candidatos inspecionados, cardinalidade máxima e declarações
   indexadas. O teste proíbe custo proporcional ao total de símbolos por uso.
9. REGRESSÃO: 42 testes Maven verdes, três JavaScripts válidos e diff sem erros.
   Grammar, corpus, HTML, outputs e programas de demonstração não mudaram.

### Fase 5

1. RED: `ProcedureFileProgramReferenceResolverTest` falhou na compilação pela
   ausência do resolver composto e de `ExternalProgramCatalog`.
2. PROCEDURE: GO TO simples, qualificado e DEPENDING ON, PERFORM FROM/THROUGH e
   referências preservadas em ALTER/SORT/MERGE são ligadas apenas dentro do
   program unit. Duplicatas por section ficam ambíguas; nome existente somente
   em nested program permanece não resolvido.
3. FILE: SELECT+FD produz um candidato de entidade com ambas as declarações;
   SELECT-only e FD-only continuam resolvíveis; ausência permanece explícita.
   O texto `ASSIGN TO` é propagado como atributo da entidade, sem ainda extrair
   o fato final DDNAME.
4. ALTERNATIVAS: CALL BY REFERENCE é um contexto em que a grammar admite DATA
   ou FILE. `admissibleKinds` preserva esse conjunto e o resolver combina ambos
   os namespaces; não há fallback por aparência do nome.
5. PROGRAM: filhos diretos e COMMON respeitam nesting; sibling não COMMON não é
   interno visível; duplicatas preservam todos os candidatos. Catálogo ausente,
   catálogo vazio, match único e match múltiplo têm resultados distintos.
6. CATÁLOGO: `ExternalProgramCatalog` é somente uma porta de lookup e identidade,
   com implementação vazia e fake testada. Nenhum indexador de codebase ou
   carregamento de fonte externo foi criado.
7. REGRESSÃO CALL: CBSTM03D mantém 14 CALL targets DATA `WS-CALL-TARGET`, todos
   ligados à mesma declaração e sem inferência de valor. CBSTM03A mantém 14
   nomes PROGRAM literais, todos `UNRESOLVED/EXTERNAL_CATALOG_NOT_PROVIDED` sem
   catálogo — nunca “nenhuma dependência”.
8. REGRESSÃO: 49 testes Maven verdes, três JavaScripts válidos e diff sem erros.
   Grammar, corpus, baselines, HTML e outputs permaneceram inalterados.

### Fase 6

1. RED: `ResolutionAnalysisReportTest` falhou na compilação pela ausência do
   produto que compõe frontend, collector e resolver.
2. GREEN: `ResolutionAnalysisReport` agrega policy, status/reasons/kinds/roles,
   summaries por program unit, gaps tipados e flags conservadoras de
   `referenceBindingComplete` e `dependencyAnalysisReady`.
3. CONSERVADORISMO: COPY ausente, erros frontend, cobertura
   `DEPENDENCY_UNKNOWN`, diagnósticos semânticos, referência em container
   preservado e qualquer binding não RESOLVED bloqueiam a alegação de análise
   completa. O resultado expõe unknowns; nunca os converte em zero dependências.
4. INTEGRIDADE: cada ocorrência coletada precisa possuir entry de resolução;
   produto ausente ou ocorrência perdida gera gap explícito por program unit.
5. ESCALA: a fixture parametrizável gera 1.200 declarações e 2.400 referências.
   O teste verifica cardinalidade dos índices e limita inspeções ao conjunto de
   candidatos do mesmo nome, sem threshold de tempo dependente da máquina.
6. DETERMINISMO: duas resoluções produzem a mesma assinatura estável. Quatro
   instâncias executadas em paralelo sobre inputs imutáveis produzem resultados
   idênticos e mantêm isolamento entre program units.
7. MÉTRICAS: o relatório registra declarações indexadas, lookups nominais,
   candidatos inspecionados, cardinalidade máxima, referências coletadas e
   quantidade de unidades, sem incluir duração não determinística no snapshot.
8. REGRESSÃO: 53 testes Maven verdes, três JavaScripts válidos e diff sem erros.
   Grammar, corpus, baselines, HTML e outputs permaneceram inalterados.

### Fase 7

1. RED: `ResolutionSnapshotTest` falhou primeiro pela ausência de
   `ResolutionSnapshot`; após a menor projeção determinística, permaneceu RED
   pela ausência de `resolution.html`; por fim, o teste de integração falhou
   enquanto `ExplorerMain` ainda produzia somente três etapas.
2. GREEN: `ResolutionSnapshot` projeta, sem recalcular binding, policy, catálogo,
   completude, métricas, units, occurrences, candidatos, diagnósticos, relações
   e gaps. O fonte normalizado aparece uma única vez; cada ocorrência conserva
   spans, proveniência e pontes para AST/parse tree/símbolos.
3. PIPELINE: `ExplorerMain` usa o modelo completo da compilation unit, constrói
   tabelas e occurrences namespaced e executa o resolver depois da Symbol Table.
   AST, Symbol Table, occurrences, resolution e report permanecem produtos
   separados. A página legada de AST/tabela continua representando a primeira
   unidade para compatibilidade.
4. UI: `resolution.html` oferece busca e filtros por unit/kind/role/status/reason,
   inspector de todos os candidatos, fonte, policy/catálogo, custo e lacunas de
   cobertura. Não usa rede, `fetch` ou dependências web externas.
5. LIMITE EXPLÍCITO: a seleção de `CALL_TARGET` DATA explica que o binding liga
   `WS-CALL-TARGET` à sua declaração, mas CFG/reaching definitions continuam
   necessários para descobrir valores possíveis e subprogramas chamados.
6. NAVEGADOR: a fixture focada exibiu 4 RESOLVED, 1 AMBIGUOUS, 1 UNRESOLVED e
   1 UNSUPPORTED. O filtro AMBIGUOUS retornou uma ocorrência e preservou dois
   candidatos; pontes apontaram para AST #24 e parse tree #130; zero erros de
   console foram observados.
7. CBSTM03D: o filtro `CALL_TARGET` retornou exatamente 14 bindings RESOLVED de
   `WS-CALL-TARGET`, todos com a mensagem que separa binding de dataflow. A
   página manteve 331 gaps e `dependencyAnalysisReady=false`, evitando alegação
   indevida de cobertura completa.
8. OUTPUTS: somente `dist`, `dist-cbstm03a` e `dist-cbstm03d` foram regenerados
   no repositório. Respectivamente, os snapshots contêm 3.058/1.468,
   580/345 e 582/331 referências/gaps.
9. DETERMINISMO: duas gerações consecutivas de `resolution-data.js` produziram
   SHA-256 idênticos: `697cbcc55d383c8ac47e00c5cecf1baa80adb8b1570697ca3b1ce3c9eb62110b`,
   `da9c1eb1b800f902251d06801d746f683cb5c7600cbe7a6d6fbc3070d355afd3` e
   `c13f1ce9a0c1bfdef5af018dc03b5d644128578ea8e34655992e47a02828198c`.
10. REGRESSÃO: a suíte completa terminou com 56 testes, zero falhas/erros; todos
    os quatro JavaScripts-fonte e os três snapshots gerados passaram em
    `node --check`. Gramáticas, corpus, fontes COBOL e baselines não foram
    modificados.

## Evidência da regressão final — Fase 8

### Suíte, fixtures e escala

1. A suíte Maven completa terminou com **56 testes, zero falhas, zero erros e
   zero ignorados** em 52,90 s. Os testes executam todas as fixtures sintéticas
   versionadas de grammar, provenance/COPY, declarations, expressions,
   statements, compilation units, visibilidade, entidades, occurrences,
   DATA/INDEX/PROCEDURE/FILE/PROGRAM, catálogo, cobertura e snapshots.
2. `GrammarCoverageManifestTest` e `ReferenceResolutionManifestTest` validaram
   mecanicamente as 598 regras COBOL + 30 regras do preprocessor e as 50
   alternativas diretas de `statement`. Cada uma das 628 chaves possui
   classificação, rationale e policy section sem consultar o corpus.
3. A fixture de escala criou 1.200 declarações e 2.400 referências. O teste
   focado terminou em 6,94 s incluindo startup/compilação Maven e verificou que
   `candidateInspections <= references × 2`; não existe threshold funcional
   dependente do hardware. O mesmo teste comparou duas assinaturas integrais de
   resolução.
4. Quatro instâncias do resolver executadas em paralelo sobre inputs imutáveis
   produziram resultados iguais e preservaram isolamento entre program units.

### Regressão dos três programas

| Programa | Lexer/parser | AST | CALLs | Symbol Table | Resolution | Gaps | Dependency ready |
|---|---|---:|---|---|---:|---:|---|
| COACTUPC | 0/0; 3 COPYs ausentes | 9.189 | 1 estático | 651 scopes / 853 symbols / 2 diagnostics | 3.058 refs | 1.468 | não |
| CBSTM03A | 0/0 | 2.740 | 14 estáticos | 219 / 209 / 0 | 580 refs | 345 | não |
| CBSTM03D | 0/0 | 2.752 | 14 dinâmicos | 221 / 211 / 0 | 582 refs | 331 | não |

5. COACTUPC preserva seu CALL PROGRAM literal `'CSUTLDTC'` como
   `UNRESOLVED/EXTERNAL_CATALOG_NOT_PROVIDED`. O relatório mantém um gap
   `UNRESOLVED_COPY` cuja mensagem registra três COPYs e 14 gaps originados por
   `execCicsStatement`; por isso não declara análise completa.
6. Os 14 CALLs de CBSTM03A continuam referências PROGRAM com os textos
   `'CBSTM03B'` e `'CEE3ABD'`. Sem catálogo, todos permanecem
   `UNRESOLVED/EXTERNAL_CATALOG_NOT_PROVIDED`, nunca “sem dependência”.
7. CBSTM03D mantém 14 CALLs dinâmicos, zero estáticos, uma declaração DATA nível
   05 de `WS-CALL-TARGET`, um MOVE de `CBSTM03B` e um MOVE de `CEE3ABD`. Os 14
   usos `CALL_TARGET` estão `RESOLVED/UNIQUE_VISIBLE_DECLARATION` para a mesma
   entidade namespaced `DATA_SYMBOL #8`.
8. Nenhum resultado de CALL dinâmico contém `CBSTM03B` ou `CEE3ABD` como
   candidato PROGRAM. Os literais continuam somente no fonte/MOVEs; não houve
   inferência de valor, CFG ou dataflow.

### Determinismo, integridade e navegador

9. COACTUPC, CBSTM03A e CBSTM03D foram regenerados duas vezes. O agregado de
   todos os arquivos de `dist`, `dist-cbstm03a` e `dist-cbstm03d` foi idêntico
   antes e depois das duas rodadas:
   `35a0ec597373753b48dd14b4dad64c5a7da38891cb06226087b1432017d636cb`.
10. Os 31 JavaScripts dos templates e das três jornadas passaram em
    `node --check`. Templates HTML/CSS/JS não contêm HTTP(S), `@import`,
    `fetch` ou dependência externa de runtime.
11. A navegação real no navegador percorreu, sem erro de console:
    `resolution.html → symbols.html#symbol=0 → ast.html#node=0 →
    index.html#node=0 → resolution.html`.
12. Gramáticas e corpus mantêm exatamente os agregados do baseline:
    `4be036fc47a73e32dd68de6a3ab0008ef81895f52372046f6a35e986e92b202e`
    e `f89cf02782dc26f3a4a79e37fc98c35112f0003d36423e674785251152b4b10e`.
13. As fontes principais mantêm os SHA-256 originais:
    `23c875…32b5` (CBSTM03A), `d75535…7fac` (CBSTM03D) e
    `b5bb7d…9396` (COACTUPC). O diff desde o baseline não altera `baseline`,
    `cbl`, `cpy`, grammars, corpus ou o relatório final do hardening.
14. O Java de produção possui agora o agregado esperado
    `48a57a66efd499c59a8536d43141ed43a854ff592ba9425aa485071dd63fea11`;
    a mudança frente ao baseline corresponde aos contratos e implementações de
    compilation unit, occurrences, resolver, coverage e snapshot documentados
    por fase.

### Validação dos critérios de aceite

| # | Estado | Evidência principal |
|---:|---|---|
| 1 | atendido | manifests cobrem 628 regras/50 statements sem corpus |
| 2 | atendido | fixture materializa cinco top-level/nested program units |
| 3 | atendido | produtos imutáveis, separados e IDs namespaced |
| 4 | atendido | reflexão e revisão confirmam AST sem campos de binding |
| 5 | atendido | collector exige ocorrência única e report detecta perda |
| 6 | atendido | testes de qualifier/subscript/refmod e roles independentes |
| 7 | atendido | hierarchy/ordem/FILE e STANDARD/EXTEND/UNSPECIFIED testados |
| 8 | atendido | 88, INDEX, relações, LINKAGE, visibility e shadowing testados |
| 9 | atendido | GO TO/DEPENDING/PERFORM/THRU isolados por program unit |
| 10 | atendido | entidade FILE une SELECT+FD/SD sem ambiguidade artificial |
| 11 | atendido | nesting/COMMON e catálogo ausente/vazio/único/múltiplo testados |
| 12 | atendido | CALL dinâmico liga somente DATA e não produz targets possíveis |
| 13 | atendido | fixture exercita os quatro status com reason/candidates coerentes |
| 14 | atendido | frontend/containers/binding bloqueiam completude conservadoramente |
| 15 | atendido | índices e fixture de escala proíbem scan global por referência |
| 16 | atendido | assinaturas, IDs, snapshots e outputs repetidos são idênticos |
| 17 | atendido | 14 CALL_TARGET de CBSTM03D convergem para DATA_SYMBOL #8 |
| 18 | atendido | COACTUPC incompleto e nomes PROGRAM de CBSTM03A preservados |
| 19 | atendido | filtros, candidatos, decisões, pontes e fonte validados no browser |
| 20 | atendido | 56 testes, 31 JS, hashes, fixtures e regressões verdes |
| 21 | atendido | revisão de classes/diff exclui CFG, dataflow, SQL e fatos finais |

## Diferenças esperadas consolidadas

- a compilation unit passa a representar todos os programas top-level/nested;
- AST PROGRAM/DATA/FILE preserva atributos declarativos mínimos de visibilidade;
- a Symbol Table acrescenta entidades FILE, relações, visibility e atributos
  PROGRAM, mas continua sem usos ou resultados de binding;
- occurrences, scope index, policy, catálogo, resolution e analysis report são
  produtos novos e separados;
- a quarta página e `resolution-data.js` tornam decisões e gaps auditáveis;
- contagens antigas de nós/símbolos essenciais permanecem protegidas por fatos,
  enquanto novos atributos e DTOs alteram legitimamente os hashes dos outputs.

## Cobertura pendente e limites para produção

O name binding aprovado está concluído, mas o projeto **não deve ser chamado de
analisador de dependências production-ready**. Permanecem explícitos:

- 1.468/345/331 gaps em COACTUPC/CBSTM03A/CBSTM03D; nenhum dos três programas
  está `dependencyAnalysisReady`;
- programas externos sem catálogo continuam unresolved; não existe ainda um
  indexador/orquestrador de codebase massiva;
- CALL dinâmico continua sem valores possíveis, por ausência intencional de CFG
  e reaching definitions;
- ASSIGN/DDNAME e EXEC SQL/CICS continuam preservados, sem fatos finais;
- o snapshot informa identidade textual e estado de preprocessing por gaps,
  mas ainda não incorpora o SHA-256 do input como campo operacional;
- métricas estruturais de índices/lookups/candidatos existem, mas duração por
  etapa ainda é medida externamente e não faz parte do report determinístico;
- gaps globais carregam regra/linha/unidade, mas não repetem toda a cadeia de
  provenance/COPY; occurrences estruturadas preservam essa cadeia completa;
- as páginas AST/Symbol Table legadas mostram a unidade primária. A resolução
  preserva IDs namespaced das nested units e desabilita links enganosos, mas uma
  visualização multi-unit completa permanece futura.

Esses itens não invalidam os 21 critérios do passo de resolução nominal, mas
devem entrar no planejamento do runner/observabilidade antes de uso massivo em
produção.

## Checklist final de regressão

- [x] suíte Maven completa;
- [x] sintaxe de todos os JavaScripts;
- [x] matriz das 628 regras e 50 statements;
- [x] três programas sem novos erros;
- [x] fatos CALL/MOVE de CBSTM03D;
- [x] fatos essenciais da Symbol Table;
- [x] fixtures gramaticais e semânticas;
- [x] determinismo de duas gerações;
- [x] navegação HTML;
- [x] fixture de escala;
- [x] hashes e fronteiras de escopo.
