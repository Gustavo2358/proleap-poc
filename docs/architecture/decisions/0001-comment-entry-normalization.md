# ADR-0001 — Normalização fechada de comment entries COBOL

Status: Accepted
Type: Contemporary
Recorded: 2026-08-25

## Context

A gramática recebe comment entries pelo token sintético `COMMENTENTRYLINE` (`*>CE`), enquanto fonte fixed-format as expressa depois dos parágrafos que admitem `commentEntry`. A conversão precisa ocorrer antes do lexer COBOL sem deslocar o `SourceMap`.

## Decision

Usar scanner determinístico de registros fixed-format, após classificação exaustiva da indicator area e antes do parser. O catálogo fechado de owners é `programIdParagraph`, `authorParagraph`, `installationParagraph`, `dateWrittenParagraph`, `dateCompiledParagraph`, `securityParagraph` e `remarksParagraph`.

Headers começam em Area A e usam separador gramatical explícito. Pontos no conteúdo são dados; a próxima construção de Area A encerra a entrada, com `END-REMARKS` também reconhecido para `remarksParagraph`. Nenhuma linha é inserida ou removida; texto marcado recebe `exact=false`.

## Rationale

O scanner acompanha as fronteiras aceitas pela gramática, preserva registros físicos e torna mudança na superfície reconhecida detectável por teste de contrato.

## Consequences

- line endings e contagem de linhas permanecem estáveis;
- novos contexts com `commentEntry` exigem política e teste;
- formas fora do catálogo não recebem fallback heurístico.

## Rejected alternatives

- regex global de headers;
- booleano encerrado por ponto no texto;
- inserção de linha sintética que desloca provenance.

## Evidence in current implementation

`SourceNormalizer.CommentEntryOwner`, scanner de registros e testes de comment entry/source provenance.

## Related invariants

INV-AST-002, INV-PROV-001, INV-PROV-002 e INV-COV-002.
