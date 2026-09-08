# spec

## Problema

O contrato 1.1.0 não prova domínio, acesso escalar, cópia nem continuação de MOVE.

## Objetivo

Fechar E1–E4 exclusivamente no profile elementar textual autorizado pelo usuário.

## Domínio de entrada suportado

Item elementar local em WORKING-STORAGE; literal alfanumérico básico; um destino direto resolvido; extensões lógicas iguais. Continuação entre statements diretos de uma mesma região de sentences canônica.

## Classes semânticas

Positiva: identidade textual completa. Negativas: mismatch, categoria não coberta, OCCURS, overlays, modificadores. Incerta: binding ou continuação ausente.

## Premissas

LANGUAGE_GUARANTEED: regras IBM 6.4 de literal básico, PIC X e elementary MOVE. ARCHITECTURE_GUARANTEED: AST tipada e resolução são autoridades. SPECIFICATION_GUARANTEED: limite do profile definido no pedido 4A. Não assumir byte codec nem contexto runtime.

## Comportamento esperado

Fatos tipados independentes de readiness e spellings; JSON minor 1.2.0. Preservar CP3, provenance individual e inventários parciais.

## Comportamento diante de incerteza

Não emitir prova positiva. Conservar ocorrência, binding/candidates, gaps e disponibilidade explícita. Fim físico não prova NONE.

## Fora de escopo

AIR, lower, CFG, dataflow, CALL, storage geral, padding/truncation/conversão executáveis, repos irmãos.

## Regras de domínio relacionadas

docs/domain/cobol-semantic-product.md; handoff técnico ../checkpoint-4-discovery.md read-only.

## ADRs/invariantes relacionados

ADR-0013; INV-SP-001 a INV-SP-007.
