# Resolução de referências

## Propósito, entradas e saídas

Este documento descreve o binding nominal atual. A execução usa `CobolResolutionPolicy.initial()` com ID `cobol-explorer/explicit-options`, versão `3.0.0` e opções `QUALIFY`, `PGMNAME`, `DYNAM` e `DLL` inicialmente `UNSPECIFIED`; compiler options reconhecidas pelo preprocessor substituem os modos correspondentes.

- **Entradas:** compilation units, symbol tables por unit, occurrences tipadas e policy.
- **Saída:** `ReferenceResolution` imutável com entries, candidates, diagnostics, métricas e resolução separada de declaration relations.

## Produto e fronteira

`ReferenceOccurrenceCollector` coleta ocorrências tipadas da AST sem lookup. `CobolReferenceResolver` consome occurrences, compilation units e symbol tables e produz `ReferenceResolution` separado, imutável, com candidates, status, motivos e diagnostics.

`ReferenceResolution` registra somente a explicação nominal COBOL. Uma classificação posterior de plataforma não altera `status`, `reason`, candidates ou diagnostics desse produto. Sem contexto confiável de compilação, uma construção COBOL não resolvida pode receber um fato externo inferido em produto ortogonal; a composição de relatório pode agrupar as occurrences artificiais do mesmo construct somente quando todos os COPYs solicitados estão disponíveis, mas deve manter a hipótese, sua incerteza e provenance observáveis. `CopyInputCompleteness` representa somente esse eixo de disponibilidade e não afirma integridade estrutural do frontend. Sob COPY não resolvido, a classificação registra `INCOMPLETE_UNRESOLVED_COPY` e permanece ao lado dos gaps nominais cobertos, pois uma declaration ausente ainda poderia refinar o binding. Uma referência COBOL resolvida sempre precede essa classificação inferida.

Resolução é nominal. Ela não faz CFG, reaching definitions, propagação de constantes ou inferência de valores de runtime. Assim, `CALL WS-CALL-TARGET` pode resolver a declaração de `WS-CALL-TARGET`, mas não os programas que a variável pode conter. Futuras análises devem preservar targets estáticos conhecidos e um remainder dinâmico/incerto quando ambos coexistirem.

## Regras e limites atuais

- Nomes COBOL são comparados pela forma canônica case-insensitive, preservando a grafia escrita para diagnóstico e navegação.
- Operandos de uma operação AST classificada como relacional admitem DATA ou INDEX; a categoria vem do lowering gramatical e independe de a grafia ser `=`, `GREATER`, `LESS` ou uma forma composta.
- O alvo de `SET condition-name TO TRUE` ou `FALSE` é uma occurrence CONDITION. A classificação vem de `setToStatement` e `booleanLiteral` tipados; `SET` de dados e índices permanece DATA/INDEX e não é promovido a CONDITION.
- Um selector nominal direto de `WHEN` é occurrence CONDITION somente quando seu contexto AST o associa, pela posição `ALSO`, a selection subject `TRUE` ou `FALSE` tipado. `WHEN NOT condition-name` pertence à mesma classe. Selector nominal sob subject de valor permanece DATA/INDEX; literals, intervalos `THRU` e formas sem a regra estrutural exata não são promovidos.
- Estrutura de scopes não é, por si só, a relação completa de visibilidade COBOL. Namespace, kind, unit, nesting, `GLOBAL`, `COMMON`, shadowing e qualificação participam conforme a categoria da referência.
- Qualificadores DATA seguem ancestry estrutural de dentro para fora; `IN` e `OF` são equivalentes semanticamente. `STANDARD`, `EXTEND` e `UNSPECIFIED` preservam diferenças de dialeto em vez de selecionar uma variante silenciosamente.
- PROCEDURE é local ao program unit; paragraph qualificado depende da section escrita.
- FILE representa a entidade formada pelas declarações compatíveis, sem ambiguidade artificial entre `SELECT` e `FD`/`SD`.
- PROGRAM considera programas internos visíveis segundo regras COBOL. Um `CALL` literal sem programa interno visível é uma dependência externa observada (`EXTERNAL_OBSERVED`), com nome preservado e sem candidate, símbolo sintético ou lookup fora do artefato.
- Formas aceitas pela gramática sem política segura resultam em `UNSUPPORTED`; input ausente e ambiguidade permanecem observáveis com motivo próprio.

Bare nominal tails em [condições combinadas/abreviadas](conditional-expressions.md) são uma limitação conhecida: a occurrence atual ainda pode ser fechada por `grammarRule` antes do binding. ADR-0012 (`Accepted`) define preservar na occurrence todas as classes semanticamente admissíveis e especializar o predicate em produto pós-binding separado. Mesmo nesse desenho, o resolver continua apenas nominal: aplica qualification/scope e produz candidate/status, mas não reconstrói subject/operator, `NOT`, precedência ou parênteses e não valida compatibilidade de tipos da relation-condition. `PIC`/`USAGE` checking e a admissibilidade type-sensitive pertencem a `ConditionValidation` futura, nunca ao binding: resolver `N → DATA` e `IDX → INDEX` não declara `N = IDX` semanticamente válida.

Em `QUALIFY(STANDARD)`, todos os candidates que correspondem à sequência ordenada permanecem válidos. Em `EXTEND`, a correspondência totalmente qualificada pode distinguir o único candidate conforme a policy IBM implementada. Em `UNSPECIFIED`, divergência entre as duas variantes resulta em `UNSUPPORTED_DIALECT_OPTION`.

Linkage de CALL é separado da forma sintática do target. `DYNAM`, `NODYNAM`, `DLL`, `NODLL`, `PGMNAME` e combinações inválidas/ausentes permanecem explícitas; literal no fonte não implica automaticamente linkage estático.

O algoritmo não pode escolher primeiro candidato, ordem de corpus ou candidato mais próximo por conveniência. Candidatos semanticamente válidos devem ser preservados quando a linguagem não seleciona um único.

## Cobertura

`ReferenceResolutionManifest` classifica a superfície do frontend de modo explícito e conservador. O catálogo de evals ligará capacidades, fixtures e oráculos a estas regras na Fase 6.

`RESOLVED` exige exatamente um candidate; `EXTERNAL_OBSERVED` não possui candidate; `AMBIGUOUS` conserva dois ou mais. `UNRESOLVED` e `UNSUPPORTED` carregam reason específico. O relatório combina binding, frontend coverage, COPYs, parser diagnostics e incertezas reais antes de afirmar readiness. Cada fato tipado `Diagnostic.Code.UNRESOLVED_COPY` vira gap com nome e linha; o contador é derivado desses fatos e permanece no snapshot. `FrontendState` distingue `copyInputCompleteness()` dos erros estruturais que impedem o classifier: zero COPY ausente pode coexistir com parser error sem afirmar frontend completo. A observação de target literal externo não é, por si, uma lacuna.

## Complexidade e determinismo

Resolvers usam índices por nome canônico, unit, scope e domínio semântico. Métricas expõem declarations indexadas, lookups nominais, candidates inspecionados e cardinalidade máxima. IDs e candidate order são determinísticos; não há scan global por referência como algoritmo deliberado.

## Fontes semânticas

- IBM Enterprise COBOL — [Qualification](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=reference-qualification)
- IBM Enterprise COBOL — [References to PROCEDURE DIVISION names](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-references-procedure-division-names)
- IBM Enterprise COBOL — [Scope of names](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=programs-scope-names)
- IBM Enterprise COBOL — [Calling nested COBOL programs](https://www.ibm.com/docs/en/cobol-aix/5.1.0?topic=subprograms-calling-nested-cobol-programs)
- IBM Enterprise COBOL — [SET condition-names](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=statement-format-4-set-condition-names)

## Evidência executável e relações

Testes principais: `DataAndIndexReferenceResolverTest`, `ProcedureFileProgramReferenceResolverTest`, `CallSemanticsTest`, `ProgramNameCanonicalizerTest`, `ReferenceResolutionManifestTest`, `ResolutionAnalysisReportTest`, `ExternalClassificationProjectionTest` e `PartialAnalysisMissingCopyTest`. Evals: EVAL-RES-DATA-001 a EVAL-RES-DATA-003, EVAL-RES-REL-001, EVAL-RES-PROC-001, EVAL-RES-FILE-001, EVAL-RES-PROG-001, EVAL-RES-PROG-002, EVAL-RES-CALL-001, EVAL-RES-CALL-002, EVAL-RES-COV-001, EVAL-RES-REPORT-001, EVAL-RES-DET-001, EVAL-RES-PERF-001 e EVAL-COV-003.

Invariantes: INV-RES-001, INV-RES-002, INV-RES-003, INV-COND-001, INV-COND-002, INV-EXT-001 a INV-EXT-004, INV-COV-001, INV-COV-003, INV-DET-001 e INV-PERF-001. ADRs: ADR-0003, ADR-0004, ADR-0005, ADR-0008, ADR-0010, ADR-0011 e ADR-0012.
