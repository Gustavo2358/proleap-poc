# spec

## Problema

Discovery CP6 W2 aprovado pelo humano: W2-G01/G02/G03/G05 impedem interpretar IF simples apenas pelo SP.

## Objetivo

Somente W2A no frontend: fatos source-derived suficientes para uma tradução futura.

## Domínio de entrada suportado

IF simples com referência textual escalar inteira resolvida = literal básico; braços de MOVE escalar textual já admitido pelo W1 e continuação canônica. Nested preservado, sem admissão produtiva completa.

## Classes semânticas

Closed/open; ELSE ABSENT/PRESENT/UNKNOWN e conteúdo separado; predicate KNOWN ou indisponível; storage comprovado ou indisponível.

## Premissas

LANGUAGE_GUARANTEED: IBM Enterprise COBOL 6.4, IF, alphanumeric comparisons e WORKING-STORAGE. ARCHITECTURE_GUARANTEED: AST/occurrences/resolution/provenance canônicos. SPECIFICATION_GUARANTEED: autorização humana de W2A e profile conservador.

## Comportamento esperado

Completion vem das listas canônicas e continuação herdada nos braços, nunca de IDs/linhas. Predicate booleano puro e total apenas no profile admitido; predicate truth value is NOT evaluated. ELSE vazio permanece presente e parcial.

Decision gate STORAGE, antes de produção: **Opção A**. Fact tipado próprio IndependentStorageSet, separado de scalarText. O contrato scalarText explicitamente não promete disjunção universal; B não tem autoridade suficiente. Prova limitada a uma seção WORKING-STORAGE integralmente conhecida, cujos roots são itens 01/77 elementares locais elegíveis, sem outros layouts/cláusulas/overlays. Todas as declarações e a seção exigem provenance exata e cobertura suficiente. Binding nominal e IDs distintos não são a autoridade. storage independence is source-derived evidence, not inferred from distinct IDs. IBM WORKING-STORAGE descreve itens independentes; REDEFINES/RENAMES/EXTERNAL/estruturas e entradas incompletas são excluídos conservadoramente.

Decision gate VERSION: **1.3.0 → 1.4.0**, minor aditiva conforme evolução SP e INTERNAL-CONTRACT-DEV-001; um writer corrente, sem negociação/downgrade/reader produtivo neste repo. Bytes históricos permanecem congelados. Consumers 1.3-only devem rejeitar 1.4.0 até autorização própria.

## Comportamento diante de incerteza

Input/recovery, acesso parcial, binding ambíguo/unresolved, condição não admitida, origem ausente, conteúdo/estrutura não comprovados nunca fortalecem garantias; conteúdo conhecido permanece visível.

## Fora de escopo

AIR/CFG/dataflow, avaliação de FLAG, alias analysis geral, IF COBOL geral, W2C/B/D. Sem merge/auto-merge.

## Regras de domínio relacionadas

[SP](../../../domain/cobol-semantic-product.md), [MOVE](../../../domain/scalar-text-move.md), [CALL](../../../domain/call-semantic-product.md). Autoridade IBM: [IF](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-if-statement), [comparação alfanumérica](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=conditions-alphanumeric-comparisons), [WORKING-STORAGE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=overview-working-storage-section).

## ADRs/invariantes relacionados

ADR-0013; INV-SP-001/002/006/008/009; INV-RES-002. Nenhuma classe AIR importada.
