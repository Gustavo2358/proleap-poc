# state

## Onde estamos

W2A IMPLEMENTED / QUALIFIED / AWAITING_HUMAN_REVIEW, qualificação local concluída.
Branch feat/cp6-w2a-if-semantic-facts; SP 1.4.0; decisão storage A.

## Verde conhecido

RED A–D, GREEN nominal/negativos/nested, 13 mutações W2A, 13 MOVE e 8 CALL,
restauração exata e segundo GREEN. Full/performance PASS; CLI determinístico.
597 testes executados sem falhas, um skip histórico explícito.
[Handoff e recibos](../../evidence/WORK-AST-006/README.md).

## Restante

Publicação como Draft PR e CI no SHA publicado são registrados no handoff
pós-commit; revisão humana antes de merge. Sem auto-merge.
W2C/W2B/W2D NOT_STARTED / NOT_AUTHORIZED.

## Descobertas que afetam o plano

Coverage esparsa combina findings de statement/data com superfície tipada exata.
Unresolved DATA/INDEX conserva kind gap. W1A arquivado após confirmação do merge
#34. Origem de braço ancora um token; owner/filhos guardam spans completos.
