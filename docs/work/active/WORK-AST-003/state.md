# Estado

## Onde estamos

Fase 2 implementada em `fix/work-ast-003-preorder-invariant` sobre `47484979b666a61758539630dcf4425249c68340`, merge do PR #11. A correção, regressões e promoção canônica do invariant estão prontas para o PR de implementação; merge permanece proibido até review.

## Verde conhecido

- Teste focal normal: 5 testes verdes, incluindo os três triggers, os controles negativos e o oracle estrutural/determinístico sobre a superfície representativa.
- `PERFORM` procedure com `UNTIL` e com `THRU + UNTIL` atravessam `AstSnapshot` com referências e controles preservados.
- Os call sites `FileDescription` e `DataEntry` mantêm `CONFLICTING_DECLARATION_VISIBILITY` ancorado à declaração sem gap estrutural.
- Gates `fast`, `semantic` e `full` verdes; nenhum snapshot ou baseline foi atualizado.
- Parsing, AST shape, provenance, coverage, símbolos/scopes, occurrences, resolução, classificação externa e relatórios existentes permaneceram verdes.

## Restante

- Publicar commit e PR de implementação separados contra `main`, sem merge automático, e aguardar review.
- WORK-AST-002 Slice 2 permanece sem autorização e não foi iniciado.

## Descobertas que afetam o plano

- Nenhuma nova violação da classe foi encontrada; os dois call sites auditados usam agora a mesma anchor estrutural.
- O oracle é `O(nodes)` e a produção não ganhou passe ou índice novo; `performance` não é gate aplicável a esta correção.
- Churn de IDs ficou restrito aos triggers antes rejeitados pelo snapshot; entradas previamente válidas e seus produtos não exigiram atualização de baseline.
