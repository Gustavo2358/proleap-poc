# eval

## O que prova corretude

Oracles de produto materializado/JSON derivados dos fontes, sem inferir esperado do projector. Fonte IBM Language Reference 6.4, MOVE/Alignment e CALL/EXIT PROGRAM.
[Recibos e evidência](../../evidence/WORK-AST-005/README.md).

## Classes positivas

Literal CALL PROGA; dynamic WS-PGM X8; identidade X5; continuations entre sentences.

## Classes negativas

USING, RETURNING/GIVING, ON/NOT ON EXCEPTION/OVERFLOW, refmod/subscript, storage não admitido, fronteira de paragraph, input incompleto.

## Classes ambíguas

Binding ambiguous/unresolved conserva candidatos/status/reason; nenhuma seleção fabricada.

## Casos adversariais

Apagar literal/acesso, trocar continuação, falsificar ausência USING/RETURNING, unknown→none, padding→raw, fabricar acesso. Produção restaurada byte-exact e segundo GREEN.

## Casos de regressão

Parser, resolução, AST, MOVE, CALL, IF, NEXT SENTENCE, nested provenance, CP5 produto. Consumer 1.2-only deve rejeitar SP novo por contrato.

## Propriedades/relações metamórficas

Duas execuções frontend com bytes iguais; renomeação de variável, variação literal e extensão não dependem de corpus; runtime não deriva de MOVE anterior.

## Expectativas de escala

Sem limites artificiais; índices existentes e passes lineares; custo adicional proporcional ao resultado de padding publicado.
