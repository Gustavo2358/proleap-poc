## Problema

`DFHRESP(...)` e `DFHVALUE(...)` possuem regras CICS especializadas, mas podem competir com `identifier -> tableCall` em contextos de expressão e valor.

## Objetivo

Mapear transitivamente a competição, caracterizá-la na parse tree e corrigir produção somente se existir uma alteração gramatical única, pequena e geral.

## Domínio de entrada suportado

Hosts aceitos pela própria `Cobol.g4` nos quais as duas construções CICS e um table call COBOL normal sejam sintaticamente admissíveis.

## Classes semânticas

- literal especializado `DFHRESP(argument)`;
- literal especializado `DFHVALUE(argument)`;
- controle negativo `MY-TABLE(IDX)`.

## Premissas

- `LANGUAGE_GUARANTEED`: `DFHRESP` e `DFHVALUE` são tokens e construções CICS dedicadas na gramática suportada.
- `ARCHITECTURE_GUARANTEED`: AST e referências derivam dos contexts concretos da parse tree.
- `UNCERTAIN`: a competição pode ou não convergir em uma abstração gramatical comum; isso deve ser demonstrado antes da correção.

## Comportamento esperado

INVARIANTE CICS-EXPRESSION-001: em cada host compartilhado suportado, a construção CICS possui o context especializado cobrindo seu intervalo e nenhum `TableCallContext` equivalente; `MY-TABLE(IDX)` continua table call no mesmo host.

## Comportamento diante de incerteza

Se a correção exigir alterações ad hoc em vários hosts independentes, interromper sem editar produção e registrar o veredito negativo.

## Fora de escopo

Compensações no AST, collector ou resolver; reconhecimento textual; exceções por argumento ou corpus; mudanças em corpus, baseline ou upstream.

## Regras de domínio relacionadas

`docs/domain/semantic-ast.md` e `docs/domain/reference-resolution.md`.

## ADRs/invariantes relacionados

ADR-0003, ADR-0009, INV-AST-002 e INV-COV-002.
