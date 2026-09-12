# plan

## Fatiamento

RED A–D preservados antes de produção; facts canônicos; port/JSON 1.4; GREEN; negativos/nested; 13 challenges; restauração exata; segundo GREEN; gates; commit/push/Draft PR/CI; review humano.

## Dependências

W1A commit 53d774026a1e4bcd969c7783a1d277aaa87b5f2f, tree a43f0fc4ef8a227d47f012b9fcb4e410842ffcc0. Checkout herdado bd6dd1c49bd724899a599beeff7dd3cb66d0f879 limpo e tree idêntica. Branch nova rebaseada sobre origin/main após confirmação do merge #34; branch W1 preservada. Discovery humano em artefatos-e2e/cp6-w2-discovery, somente leitura.

## Superfície arquitetural provável

AstBuilder preserva presença e completion por relações; snapshot pós-binding prova predicate/storage; projector traduz; core valida closure; writer lê port. O(S+R+D), sem pares completos ou scans globais por IF.

## Migrações requeridas

SP 1.4.0 aditivo, sem reescrever evidência histórica. Lifecycle W1A arquivado após confirmação GitHub do merge #34 em 2026-09-11. Siblings intocados.

## Artefatos esperados

Fixtures COBOL reais; contrato; oracles de port/JSON; recibos brutos com comandos/exits/hashes; handoff e Draft PR somente frontend.
