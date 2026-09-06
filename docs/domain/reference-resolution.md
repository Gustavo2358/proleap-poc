# Resolução de referências

## Propósito, entradas e saídas

Este documento descreve o binding nominal atual. A execução usa `CobolResolutionPolicy.initial()` com ID `cobol-explorer/explicit-options`, versão `3.0.0` e opções `QUALIFY`, `PGMNAME`, `DYNAM` e `DLL` inicialmente `UNSPECIFIED`; compiler options reconhecidas pelo preprocessor substituem os modos correspondentes.

- **Entradas:** compilation units, symbol tables por unit, occurrences tipadas e policy.
- **Saída:** `ReferenceResolution` imutável com entries, candidates, diagnostics, métricas e resolução separada de declaration relations.

## Produto e fronteira

`ReferenceOccurrenceCollector` coleta ocorrências tipadas da AST sem lookup. `CobolReferenceResolver` consome occurrences, compilation units e symbol tables e produz `ReferenceResolution` separado, imutável, com candidates, status, motivos e diagnostics.

`ReferenceResolution` registra somente a explicação nominal COBOL. Uma classificação posterior de plataforma não altera `status`, `reason`, candidates ou diagnostics desse produto. Sem contexto confiável de compilação, uma construção COBOL não resolvida pode receber um fato externo inferido em produto ortogonal; a composição de relatório pode agrupar as occurrences artificiais do mesmo construct somente quando todos os COPYs solicitados estão disponíveis, mas deve manter a hipótese, sua incerteza e provenance observáveis. `CopyInputCompleteness` representa somente esse eixo de disponibilidade e não afirma integridade estrutural do frontend. Sob COPY não resolvido, a classificação registra `INCOMPLETE_UNRESOLVED_COPY` e permanece ao lado dos gaps nominais cobertos, pois uma declaration ausente ainda poderia refinar o binding. Uma referência COBOL resolvida sempre precede essa classificação inferida.

Resolução é nominal. Ela não faz CFG, reaching definitions, propagação de constantes ou inferência de valores de runtime. Assim, `CALL WS-CALL-TARGET` pode resolver a declaração de `WS-CALL-TARGET`, mas não os programas que a variável pode conter. Futuras análises devem preservar targets estáticos conhecidos e um remainder dinâmico/incerto quando ambos coexistirem.

## Integridade cross-product

Após `CobolReferenceResolver.resolve(...)`, a orquestração executa
`SemanticProductIntegrityValidator.validate(model, tables, scopeIndexesByUnit,
occurrencesByUnit, resolution)` antes da classificação externa, relatório e
Semantic Product. Invariants autocontidos continuam nos produtos; este validator
é o owner dos joins entre eles, sem lookup nominal, nova análise ou mutação.

- Model, tables, scope indexes e occurrence containers possuem exatamente as
  mesmas units; parentage das tables coincide com o model.
- AST IDs seguem o pre-order local, sem duplicatas; anchors de scopes,
  declarations e relations existem na AST da mesma unit. Scopes correspondem
  ao containment; declarations preservam kind/namespace/scope estruturais.
- Occurrences preservam o scope indexado e o `Meta` completo do node, incluindo
  provenance. Occurrences/resolution e relations/relation-resolution formam
  bijeções por `(ProgramUnitId, id local)`, com payload correspondente.
- `SemanticEntityId(unit, domain, localId)` seleciona o alvo do candidate.
  DATA_SYMBOL representa DATA_ITEM/RENAMES como DATA ou CONDITION_NAME como
  CONDITION; INDEX_SYMBOL representa INDEX_NAME; PROCEDURE_SYMBOL representa
  seção/parágrafo; FILE_ENTITY representa a entity FILE. PROGRAM_UNIT usa o
  ordinal determinístico da target unit no model.
- `declarationSymbolIds` é exatamente `[symbol.id()]`, a lista da entity FILE
  ou a lista vazia para PROGRAM_UNIT, respectivamente. Cada lista de candidates
  possui identidades distintas. Candidates ancestrais/entre units são permitidos;
  a validação não reexecuta visibilidade COBOL. Nomes e attributes são payload,
  nunca chaves desses joins.

Qualquer violação lança `SemanticProductIntegrityException`, com diagnóstico
determinístico iniciado por `INTERNAL PRODUCT INTEGRITY FAILURE`, produto, unit,
domínio e ID aplicável. Consumidores distinguem a exception por tipo, nunca por
parsing da mensagem. Não há recuperação automática. `UNRESOLVED`, `AMBIGUOUS`,
`UNSUPPORTED` e `EXTERNAL_OBSERVED`, assim como missing COPY/coverage unknown
com produtos estruturalmente coerentes, não são falhas de integridade.

O custo é linear nas cardinalidades agregadas de units, nodes, scopes, symbols,
relations, occurrences, resolution entries, candidates e seus declaration IDs,
com espaço auxiliar linear. Os índices de scopes existentes são reutilizados;
o validator constrói índices de anchors e faz joins diretos por identidade.
Coverage/frontend e backlinks de diagnostics mantêm seus owners próprios.

## Regras e limites atuais

- Nomes COBOL são comparados pela forma canônica case-insensitive, preservando a grafia escrita para diagnóstico e navegação.
- Operandos de uma operação AST classificada como relacional usam policy shape-sensitive: `INDEX/{DATA, INDEX}` somente para `indexAdmissibleNominalShape(ref)` (qualifiers e subscript groups vazios, `referenceModification == null`); relation/distributed roots qualified, subscripted ou reference-modified usam `DATA/{DATA}`. A categoria final continua vindo do candidate selecionado.
- O alvo de `SET condition-name TO TRUE` ou `FALSE` é uma occurrence CONDITION. A classificação vem de `setToStatement` e `booleanLiteral` tipados; `SET` de dados e índices permanece DATA/INDEX e não é promovido a CONDITION.
- Um selector nominal direto de `WHEN` é occurrence CONDITION somente quando seu contexto AST o associa, pela posição `ALSO`, a selection subject `TRUE` ou `FALSE` tipado. `WHEN NOT condition-name` pertence à mesma classe. Selector nominal sob subject de valor permanece DATA/INDEX; literals, intervalos `THRU` e formas sem a regra estrutural exata não são promovidos.
- Estrutura de scopes não é, por si só, a relação completa de visibilidade COBOL. Namespace, kind, unit, nesting, `GLOBAL`, `COMMON`, shadowing e qualificação participam conforme a categoria da referência.
- Qualificadores DATA seguem ancestry estrutural de dentro para fora; `IN` e `OF` são equivalentes semanticamente. `STANDARD`, `EXTEND` e `UNSPECIFIED` preservam diferenças de dialeto em vez de selecionar uma variante silenciosamente.
- PROCEDURE é local ao program unit; paragraph qualificado depende da section escrita.
- FILE representa a entidade formada pelas declarações compatíveis, sem ambiguidade artificial entre `SELECT` e `FD`/`SD`.
- PROGRAM considera programas internos visíveis segundo regras COBOL. Um `CALL` literal sem programa interno visível é uma dependência externa observada (`EXTERNAL_OBSERVED`), com nome preservado e sem candidate, símbolo sintético ou lookup fora do artefato.
- Formas aceitas pela gramática sem política segura resultam em `UNSUPPORTED`; input ausente e ambiguidade permanecem observáveis com motivo próprio.

Bare nominal tails em [condições combinadas/abreviadas](conditional-expressions.md) são coletados contextualmente: `CONDITION/{DATA, INDEX, CONDITION}` para a shape index-admissible e `CONDITION/{DATA, CONDITION}` quando há qualification, subscript ou reference modification. O primary `CONDITION` é somente hint de superfície; o resolver seleciona o candidate admissível e publica seu kind final. A origem `conditionNameReference` no `ReferenceResolutionManifest` é `CONTEXTUAL_REFERENCE_ORIGIN` com `referenceKind == null`, separada da occurrence policy. O resolver continua apenas nominal: aplica qualification/scope e produz candidate/status, mas não reconstrói subject/operator, `NOT`, precedência ou parênteses e não valida compatibilidade de tipos da relation-condition. `PIC`/`USAGE` checking e a admissibilidade type-sensitive pertencem a `ConditionValidation` futura, nunca ao binding: resolver `N → DATA` e `IDX → INDEX` não declara `N = IDX` semanticamente válida.

Em `QUALIFY(STANDARD)`, todos os candidates que correspondem à sequência ordenada permanecem válidos. Em `EXTEND`, a correspondência totalmente qualificada pode distinguir o único candidate conforme a policy IBM implementada. Em `UNSPECIFIED`, divergência entre as duas variantes resulta em `UNSUPPORTED_DIALECT_OPTION`.

Linkage de CALL é separado da forma sintática do target. `DYNAM`, `NODYNAM`, `DLL`, `NODLL`, `PGMNAME` e combinações inválidas/ausentes permanecem explícitas; literal no fonte não implica automaticamente linkage estático.

O algoritmo não pode escolher primeiro candidato, ordem de corpus ou candidato mais próximo por conveniência. Candidatos semanticamente válidos devem ser preservados quando a linguagem não seleciona um único.

## Cobertura

`ReferenceResolutionManifest` classifica a superfície do frontend de modo explícito e conservador na versão `1.1.0`: `conditionNameReference` é `CONTEXTUAL_REFERENCE_ORIGIN` sem `referenceKind`, enquanto `qualifiedDataName` permanece `REFERENCE_ORIGIN/DATA`. O manifesto não contém `admissibleKinds` e não é uma occurrence policy table. O catálogo de evals ligará capacidades, fixtures e oráculos a estas regras na Fase 6.

`RESOLVED` exige exatamente um candidate; `EXTERNAL_OBSERVED` não possui candidate; `AMBIGUOUS` conserva dois ou mais. `UNRESOLVED` e `UNSUPPORTED` carregam reason específico. O relatório combina binding, frontend coverage, COPYs, parser diagnostics e incertezas reais antes de afirmar readiness. Cada fato tipado `Diagnostic.Code.UNRESOLVED_COPY` vira gap com nome e linha; o contador é derivado desses fatos e permanece no snapshot. `FrontendState` distingue `copyInputCompleteness()` dos erros estruturais que impedem o classifier: zero COPY ausente pode coexistir com parser error sem afirmar frontend completo. A observação de target literal externo não é, por si, uma lacuna.

## Complexidade e determinismo

Resolvers usam índices por nome canônico, unit, scope e domínio semântico. Métricas expõem declarations indexadas, lookups nominais, candidates inspecionados e cardinalidade máxima. IDs e candidate order são determinísticos; não há scan global por referência como algoritmo deliberado.

## SEARCH VARYING e SEARCH WHEN

`SearchStatement` fornece ao collector três posições nominais independentes: searched reference, `VARYING` e referências na condition de cada `SearchWhen`. A condition usa o mesmo routing de condition surface das demais condições e não altera o algoritmo nominal do resolver.

Para `VARYING`, a occurrence mantém role `CONTEXT_DEPENDENT` e primary `DATA`. A admissibilidade depende somente da shape AST: root bare → `{DATA, INDEX}`; root qualified → `{DATA}`. Assim, uma declaração bare pode resolver DATA ou INDEX, enquanto uma forma qualified restringe a seleção a DATA. Não existe `ReferenceKind.SEARCH_VARYING`.

`SEARCH ALL` preserva seu bit estrutural `all=true`, mas o resolver não valida keys, ordem, igualdade, conectores ou compatibilidade type-sensitive. Essas validações continuam fora do binding nominal e não são inferidas de grammar coverage. O manifesto classifica as boundaries `searchStatement` e `searchWhen` como materializadas/reference-ready; `grammarRule` continua provenance/coverage, não namespace.

## Fontes semânticas

- IBM Enterprise COBOL — [Qualification](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=reference-qualification)
- IBM Enterprise COBOL — [References to PROCEDURE DIVISION names](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-references-procedure-division-names)
- IBM Enterprise COBOL — [Scope of names](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=programs-scope-names)
- IBM Enterprise COBOL — [Calling nested COBOL programs](https://www.ibm.com/docs/en/cobol-aix/5.1.0?topic=subprograms-calling-nested-cobol-programs)
- IBM Enterprise COBOL — [SET condition-names](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=statement-format-4-set-condition-names)

## Evidência executável e relações

Testes principais: `DataAndIndexReferenceResolverTest`, `ProcedureFileProgramReferenceResolverTest`, `CallSemanticsTest`, `ProgramNameCanonicalizerTest`, `ReferenceResolutionManifestTest`, `ResolutionAnalysisReportTest`, `ExternalClassificationProjectionTest` e `PartialAnalysisMissingCopyTest`. Evals: EVAL-RES-DATA-001 a EVAL-RES-DATA-003, EVAL-RES-REL-001, EVAL-RES-PROC-001, EVAL-RES-FILE-001, EVAL-RES-PROG-001, EVAL-RES-PROG-002, EVAL-RES-CALL-001, EVAL-RES-CALL-002, EVAL-RES-COV-001, EVAL-RES-REPORT-001, EVAL-RES-DET-001, EVAL-RES-PERF-001 e EVAL-COV-003.

Invariantes: INV-RES-001, INV-RES-002, INV-RES-003, INV-COND-001, INV-COND-002, INV-EXT-001 a INV-EXT-004, INV-COV-001, INV-COV-003, INV-DET-001 e INV-PERF-001. ADRs: ADR-0003, ADR-0004, ADR-0005, ADR-0008, ADR-0010, ADR-0011 e ADR-0012.
