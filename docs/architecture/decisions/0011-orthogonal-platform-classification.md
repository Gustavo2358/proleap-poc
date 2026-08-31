# ADR-0011 — Classificação de plataforma permanece ortogonal à semântica COBOL

Status: Accepted
Type: Contemporary
Recorded: 2026-08-30

## Context

Algumas formas aceitas pelo frontend têm duas explicações compatíveis com o mesmo texto. `DFHRESP(X)` e `DFHVALUE(X)` podem ser table calls COBOL quando há uma declaração compatível ou intrínsecos CICS quando o fonte passa pelo tradutor dessa plataforma. O pipeline atual não recebe, de forma geral, o modo real de tradução/compilação.

Tornar a gramática globalmente CICS-aware removeria nomes COBOL válidos. Acrescentar branches CICS ao resolver misturaria binding nominal com conhecimento de plataforma. Converter `UNRESOLVED` em sucesso apagaria a incerteza causada pelo input ausente.

## Decision

Sem contexto confiável de compilação, o pipeline aplica esta precedência:

1. tenta a explicação COBOL canônica;
2. preserva qualquer binding COBOL semanticamente válido;
3. somente após o fracasso da explicação COBOL permite uma classificação externa inferida e explicitamente incerta;
4. mantém classificação externa em produto ortogonal ao `ReferenceResolution`, ligada ao construct inteiro, às occurrences cobertas e à provenance;
5. mantém qualquer incompletude incompatível com uma alegação de análise completa.

Conhecimento de nomes e shapes de plataforma fica fora da gramática, AST, symbol table, collector e resolver COBOL. O composition root pode combinar produtos para relatório e apresentação, sem mutar resultados anteriores.

Extensões posteriores devem consumir AST, binding, futuros CFG/dataflow e outros produtos canônicos. A introdução de infraestrutura genérica de composição, providers, extractors ou semântica de CFG continua trabalho de backlog; esta decisão não escolhe registry, discovery mechanism, framework de DI, packages ou interfaces definitivas.

## Rationale

O desenho conserva simultaneamente a explicação COBOL comprovada, a hipótese externa e o limite epistemológico da entrada. Um produto separado também permite agrupar os gaps internos produzidos pela interpretação COBOL intermediária sem fingir que houve binding nominal.

## Consequences

- `DFHRESP` e `DFHVALUE` continuam nomes COBOL válidos quando declarados e resolvidos.
- A classificação externa não altera status, candidates ou diagnostics do binding nominal.
- Relatórios podem substituir gaps COBOL artificiais do mesmo subtree por um fato externo inferido, ainda bloqueador de completude quando apropriado.
- Classifiers precisam usar estrutura AST e identidades compostas, não regex, prefixos ou catálogos de argumentos observados.
- Metadata futura de compilação poderá habilitar uma decisão mais forte, mas deve entrar como input/policy explícita.

## Rejected alternatives

- Remover `DFHRESP`/`DFHVALUE` de `cobolWord` ou tornar a derivação CICS obrigatória.
- Hardcode de nomes CICS no resolver COBOL.
- Reclassificar qualquer nome iniciado por `DFH`.
- Anotar ou rebaixar a AST com plataforma inferida antes da resolução.
- Trocar `UNRESOLVED` por `RESOLVED`, `EXTERNAL_OBSERVED` ou silêncio.
- Criar agora um framework genérico ou um plugin CICS monolítico.

## Evidence

- A gramática versionada aceita `DFHRESP`/`DFHVALUE` como `cobolWord` e também possui literals CICS dedicados.
- `AstBuilder` representa `tableCall` como `DataReference` estruturada com subscript groups.
- `ReferenceOccurrenceCollector` cria occurrences separadas para a referência-base e seus subscripts.
- `CobolReferenceResolver` produz binding nominal separado e `ResolutionAnalysisReport` materializa hoje um gap para cada entry não resolvida.
- O relatório de investigação está preservado em `docs/history/evidence/platform-extension-boundary-report.md`.

## Related invariants

INV-EXT-001, INV-EXT-002, INV-EXT-003, INV-EXT-004, INV-AST-001, INV-AST-002, INV-RES-001 e INV-COV-001.
