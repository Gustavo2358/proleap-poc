# state

## Onde estamos

W1A IMPLEMENTED / AWAITING_HUMAN_REVIEW na branch feat/cp6-w1a-call-semantic-product. Status documental blocked significa aguardando review/merge humano; não bloqueio por incompatibilidade interna. W1B/W1C/W1D/W2 NOT_STARTED / NOT_AUTHORIZED.

## Verde conhecido

[Handoff e recibos](../../evidence/WORK-AST-005/README.md): 11 testes W1A; mvn clean verify com 579 testes, zero failures/errors e um skip preexistente. Full (docs, architecture, fast, semantic, normalizador E2E, naming), performance, oito challenges CALL e 13 MOVE com restauração byte-exact e segundo GREEN. CLI determinístico; fatos antigos CP5 equivalentes sob delta declarado de schema.

## Restante

Publicar commit/PR Draft e conferir CI remoto no HEAD/tree final; recibo pós-commit no handoff da sessão. Review humano obrigatório antes de merge. Não iniciar checkpoints posteriores.

## Descobertas que afetam o plano

SP corrente único 1.3.0, conforme INTERNAL-CONTRACT-DEV-001. Lower pinado 18016f16 rejeitou CP5/X8/literal com EXPECTED UNSUPPORTED_CONTRACT; W1C deverá atualizar consumer/pin. Sem E2E integrado verde entre pins incompatíveis. Argumentos de CALL aninhado em handler agora pertencem somente ao USING direto. WORK-AST-004 arquivado após merge remoto confirmado do PR #33.
