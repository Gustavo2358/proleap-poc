# Resolução de referências

## Propósito, entradas e saídas

Este documento descreve o binding nominal atual. A execução usa `CobolResolutionPolicy.initial()` com ID `cobol-explorer/explicit-options`, versão `3.0.0` e opções `QUALIFY`, `PGMNAME`, `DYNAM` e `DLL` inicialmente `UNSPECIFIED`; compiler options reconhecidas pelo preprocessor substituem os modos correspondentes.

- **Entradas:** compilation units, symbol tables por unit, occurrences tipadas, policy e catálogo externo opcional.
- **Saída:** `ReferenceResolution` imutável com entries, candidates, diagnostics, métricas e resolução separada de declaration relations.

## Produto e fronteira

`ReferenceOccurrenceCollector` coleta ocorrências tipadas da AST sem lookup. `CobolReferenceResolver` consome occurrences, compilation units e symbol tables e produz `ReferenceResolution` separado, imutável, com candidates, status, motivos e diagnostics.

Resolução é nominal. Ela não faz CFG, reaching definitions, propagação de constantes ou inferência de valores de runtime. Assim, `CALL WS-CALL-TARGET` pode resolver a declaração de `WS-CALL-TARGET`, mas não os programas que a variável pode conter. Futuras análises devem preservar targets estáticos conhecidos e um remainder dinâmico/incerto quando ambos coexistirem.

## Regras e limites atuais

- Nomes COBOL são comparados pela forma canônica case-insensitive, preservando a grafia escrita para diagnóstico e navegação.
- Estrutura de scopes não é, por si só, a relação completa de visibilidade COBOL. Namespace, kind, unit, nesting, `GLOBAL`, `COMMON`, shadowing e qualificação participam conforme a categoria da referência.
- Qualificadores DATA seguem ancestry estrutural de dentro para fora; `IN` e `OF` são equivalentes semanticamente. `STANDARD`, `EXTEND` e `UNSPECIFIED` preservam diferenças de dialeto em vez de selecionar uma variante silenciosamente.
- PROCEDURE é local ao program unit; paragraph qualificado depende da section escrita.
- FILE representa a entidade formada pelas declarações compatíveis, sem ambiguidade artificial entre `SELECT` e `FD`/`SD`.
- PROGRAM considera programas internos visíveis segundo regras COBOL. Programa externo só é candidato com `ExternalProgramCatalog` explícito; catálogo ausente não significa programa inexistente.
- Formas aceitas pela gramática sem política segura resultam em `UNSUPPORTED`; input ausente e ambiguidade permanecem observáveis com motivo próprio.

Em `QUALIFY(STANDARD)`, todos os candidates que correspondem à sequência ordenada permanecem válidos. Em `EXTEND`, a correspondência totalmente qualificada pode distinguir o único candidate conforme a policy IBM implementada. Em `UNSPECIFIED`, divergência entre as duas variantes resulta em `UNSUPPORTED_DIALECT_OPTION`.

Linkage de CALL é separado da forma sintática do target. `DYNAM`, `NODYNAM`, `DLL`, `NODLL`, `PGMNAME` e combinações inválidas/ausentes permanecem explícitas; literal no fonte não implica automaticamente linkage estático.

O algoritmo não pode escolher primeiro candidato, ordem de corpus ou candidato mais próximo por conveniência. Candidatos semanticamente válidos devem ser preservados quando a linguagem não seleciona um único.

## Cobertura

`ReferenceResolutionManifest` classifica a superfície do frontend de modo explícito e conservador. O catálogo de evals ligará capacidades, fixtures e oráculos a estas regras na Fase 6.

`RESOLVED` exige exatamente um candidate; `AMBIGUOUS` conserva dois ou mais. `UNRESOLVED` e `UNSUPPORTED` carregam reason específico. O relatório combina binding com frontend coverage, COPYs, parser diagnostics e dependências desconhecidas antes de afirmar readiness.

## Complexidade e determinismo

Resolvers usam índices por nome canônico, unit, scope e domínio semântico. Métricas expõem declarations indexadas, lookups nominais, candidates inspecionados e cardinalidade máxima. IDs e candidate order são determinísticos; não há scan global por referência como algoritmo deliberado.

## Fontes semânticas

- IBM Enterprise COBOL — [Qualification](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=reference-qualification)
- IBM Enterprise COBOL — [References to PROCEDURE DIVISION names](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-references-procedure-division-names)
- IBM Enterprise COBOL — [Scope of names](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=programs-scope-names)
- IBM Enterprise COBOL — [Calling nested COBOL programs](https://www.ibm.com/docs/en/cobol-aix/5.1.0?topic=subprograms-calling-nested-cobol-programs)

## Evidência executável e relações

Testes principais: `DataAndIndexReferenceResolverTest`, `ProcedureFileProgramReferenceResolverTest`, `CallSemanticsTest`, `ProgramNameCanonicalizerTest`, `ReferenceResolutionManifestTest` e `ResolutionAnalysisReportTest`.

Invariantes: INV-RES-001, INV-RES-002, INV-RES-003, INV-COV-001, INV-DET-001 e INV-PERF-001. ADRs: ADR-0003, ADR-0004, ADR-0005, ADR-0006 e ADR-0008.
