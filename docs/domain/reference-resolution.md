# Resolução de referências

Este documento descreve o binding nominal atual. A fonte externa de regras COBOL é o dialeto IBM Enterprise compatível configurado em `cobol-explorer/ibm-enterprise-compatible`; a [política legada](../../specs/cobol-reference-resolution-policy.md) permanece como fonte de migração até a Fase 4 consolidar integralmente este contrato.

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

O algoritmo não pode escolher primeiro candidato, ordem de corpus ou candidato mais próximo por conveniência. Candidatos semanticamente válidos devem ser preservados quando a linguagem não seleciona um único.

## Cobertura

`ReferenceResolutionManifest` classifica a superfície do frontend de modo explícito e conservador. O catálogo de evals ligará capacidades, fixtures e oráculos a estas regras na Fase 6.
