# Relatório de regressão — Resolução de referências

## Estado

Em construção. Iniciado na Fase 0 de
`reference-resolution-tasklist.md` e destinado a ser encerrado na Fase 8.

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
| 6 — cobertura/escala | pendente | — | completude e métricas | — |
| 7 — HTML | pendente | — | nova jornada visual | — |
| 8 — regressão final | pendente | — | — | — |

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

## Checklist final de regressão

- [ ] suíte Maven completa;
- [ ] sintaxe de todos os JavaScripts;
- [ ] matriz das 628 regras e 50 statements;
- [ ] três programas sem novos erros;
- [ ] fatos CALL/MOVE de CBSTM03D;
- [ ] fatos essenciais da Symbol Table;
- [ ] fixtures gramaticais e semânticas;
- [ ] determinismo de duas gerações;
- [ ] navegação HTML;
- [ ] fixture de escala;
- [ ] hashes e fronteiras de escopo.
