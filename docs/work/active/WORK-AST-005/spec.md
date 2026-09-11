# spec

## Problema

SP perde literal CALL e não publica acesso/continuação/cláusulas; MOVE X8 não possui fitting.

## Objetivo

CP6 W1A somente neste frontend; autorização implementation.

## Domínio de entrada suportado

CALL literal básico ou referência DATA; primeira slice scalar local inteira, sem USING/RETURNING/handlers, com sucessor canônico conhecido. MOVE literal básico para receiver escalar de extensão igual ou maior.

## Classes semânticas

Literal/data, resolvido/ambíguo/unresolved, inteiro/refmod/subscript, cláusulas presentes/ausentes/desconhecidas, identidade/padding/indisponível.

## Premissas

LANGUAGE_GUARANTEED: IBM COBOL 6.4 MOVE/alinhamento e retorno CALL. ARCHITECTURE_GUARANTEED: AST, resolução e provenance canônicas. SPECIFICATION_GUARANTEED: escopo W1A e INTERNAL-CONTRACT-DEV-001 aprovado nesta sessão.

## Comportamento esperado

SP corrente 1.3.0; CALL tipado com target literal/data, normalContinuation condicional, surface explícita, effects UNKNOWN e outcomes OPEN. MOVE X5 mantém FULL_IDENTITY; X8 publica padding e resultado PROGA com três espaços.

## Comportamento diante de incerteza

Sem prova não promover acesso, valor ajustado ou readiness. Runtime target dinâmico permanece UNKNOWN. Lower 18016f16 rejeitará 1.3.0: EXPECTED UNSUPPORTED_CONTRACT; atualização pertence a W1C. Sem E2E verde entre pins incompatíveis.

## Fora de escopo

W1A DOES NOT resolve dynamic target values. W1A DOES NOT interpret runtime program names. W1A DOES NOT emit AIR. Sem USING completo, branches excepcionais, truncation, grammar/vendor ou siblings.

## Regras de domínio relacionadas

[SP](../../../domain/cobol-semantic-product.md), [MOVE](../../../domain/scalar-text-move.md).

## ADRs/invariantes relacionados

ADR-0013; INV-SP-001/002/006/008/009; INV-RES-002.
