# Estado

## Onde estamos

Checkpoint 1 — Discovery Round 4 concluído na branch `implementation/work-cond-006-search-when`, partindo do head revisado `57fa50c411a89b7f1c6ee18e8eeaba38c725618f` do Round 3. Os contratos e refutações dos Rounds 1–3 foram preservados; PR #19 e o commit `5f41bc1` foram confirmados como ancestrais. `WORK-COND-005` não existe em `active/` e seu resumo existe em `history/`.

## Verde conhecido

- working tree limpa antes da criação da branch;
- `./scripts/harness/check-fast.sh` verde no baseline pós-merge;
- `SearchWhenConditionDiscoveryTest` verde com 20 testes, incluindo R1–R5, re-resolution controlada e substituições de declaration/shape;
- regressões focais `ContextualConditionOccurrenceTest`, `ConditionSurfaceAstTest` e `SemanticConditionContextDiscoveryTest` verdes;
- Rounds 3–4: suíte focal, `check-fast.sh`, `check-semantic.sh` e `check-full.sh` verdes;
- nenhuma alteração em `src/main`, grammar, resolver, snapshots ou baselines;
- S1–S6, SEARCH ALL, controle negativo e challenges documentados.

## Restante

Review humano final do Discovery e autorização explícita para Implementation. A implementação continua proibida neste PR até essa autorização. Slice 7 e `BACKLOG-RES-004` permanecem separados.

## Descobertas que afetam o plano

`searchStatement` usa `SEARCH ALL? qualifiedDataName searchVarying? atEndPhrase? searchWhen+ END_SEARCH?`; `searchWhen` usa `WHEN condition (NEXT SENTENCE | statement*)`. O ponto exato de perda da condition é `AstBuilder.visitSearchStatement → preserved → buildStructuredStatement → buildStatementClause`, que não chama o lowering de condition e deixa `recognizedNodes` vazio. O collector atual exige routing tipado: `Ast.children` garante reachability estrutural, mas não converte uma child em position `CONDITION`; o futuro `SearchWhen.condition` deve chamar explicitamente `visitConditionSurface`. `NEXT SENTENCE` é uma alternativa de token fora de `statement()`. O varying possui policy shape-sensitive: bare → DATA/{DATA, INDEX}; qualified → DATA/{DATA}; `searchVarying` não oferece subscript no root. Não há necessidade demonstrada de alterar grammar ou resolver.

SEARCH ALL compartilha a shape local, mas IBM exige key/preceding-key/equal-to/AND-only e compatibilidade própria; o bit `all` deve ser preservado e sua validação não deve ser absorvida silenciosamente pelo materialization slice.

## Round 2 self-review

- **SR-01 PASS** — os documentos do work item dizem que `Ast.children` não substitui `visitConditionSurface`; a busca de consistência não encontrou afirmação contrária.
- **SR-02 PASS** — o path futuro está explícito: `SearchWhen.condition → typed CONDITION position → ReferenceOccurrenceCollector.visitConditionSurface(...)`.
- **SR-03 PASS** — NEXT SENTENCE tem representação futura lossless fechada como `Ast.NextSentenceStatement` na action estrutural da branch.
- **SR-04 PASS** — o oracle verifica `when.statement().size() == 0` e tokens diretos `NEXT`/`SENTENCE`.
- **SR-05 PASS** — o oracle INDEX isola `VARYING SEARCH-IDX`, registra a falha atual e a seleção futura INDEX exigida.
- **SR-06 PASS** — o oracle DATA isola `VARYING SEARCH-COUNTER PIC 9(4)` e registra selectedCandidate DATA.
- **SR-07 PASS** — `SEARCH_VARYING` está fechado como role `CONTEXT_DEPENDENT`, primary DATA; bare admite `{DATA, INDEX}` e qualified admite `{DATA}`.
- **SR-08 PASS** — searched table, varying, condition e qualification têm posições/policies distintas; subscript não é atribuído ao root varying porque a grammar não o oferece.
- **SR-09 PASS** — `SearchWhen` Node é justificado por branch identity, provenance, condition→statements ownership e posição determinística, com precedente `EvaluateBranch`.
- **SR-10 PASS** — SEARCH ALL permanece no mesmo shape estrutural com `all=true`, mas sua validação semântica fica futura/separada.
- **SR-11 PASS** — não foi criada nova condition policy de SEARCH; a condição reutiliza a surface e helpers do Slice 5.
- **SR-12 PASS** — resolver, contratos de resolução, símbolos e filtering permanecem fora do scope.
- **SR-13 PASS** — nenhum arquivo `src/main` foi alterado.
- **SR-14 PASS** — CH-01–CH-15 têm evidência executável ou argumento verificável documentado em `eval.md`.
- **SR-15 PASS** — F1–F3, representation de NEXT SENTENCE, policy VARYING, source scope e must-not-change estão fechados; não resta decisão semântica nova para a implementação autorizada.

## Round 3 self-refutation

- **RF-01 PASS** — R1 mostra `selectedCandidate INDEX` para VARYING bare com DATA/{DATA, INDEX}.
- **RF-02 PASS** — R2 mostra `selectedCandidate DATA` para VARYING bare com DATA/{DATA, INDEX}.
- **RF-03 PASS** — R3 mostra que a policy qualified `{DATA}` exclui INDEX; a policy errada `{DATA, INDEX}` permitiria sua seleção no what-if controlado.
- **RF-04 PASS** — R3 está marcado explicitamente como **IBM-invalid controlled model-level what-if**.
- **RF-05 PASS** — R4 confirma que `searchVarying → qualifiedDataName` permite qualification, mas não subscript no root.
- **RF-06 PASS** — nenhuma documentação atribui subscripted shape ao root de SEARCH VARYING; subscripts permanecem apenas nas surfaces que a grammar realmente oferece.
- **RF-07 PASS** — nenhum documento diz que VARYING é sempre `{DATA, INDEX}`; todos registram a distinção bare/qualified.
- **RF-08 PASS** — `spec.md`, `plan.md`, `eval.md`, `state.md` e `work-item.yaml` descrevem a mesma policy shape-sensitive.
- **RF-09 PASS** — o source scope do collector inclui condition routing e varying policy routing.
- **RF-10 PASS** — source scope continua sem resolver e sem alteração de grammar.
- **RF-11 PASS** — declaration substitution mantém DATA/{DATA, INDEX}; somente binding muda para DATA, INDEX ou unresolved.
- **RF-12 PASS** — shape substitution muda admissibility de `{DATA, INDEX}` para `{DATA}`.
- **RF-13 PASS** — os testes constroem a policy hipotética a partir da surface AST; declarations só afetam re-resolution.
- **RF-14 PASS** — table, VARYING, condition e subscripts permanecem posições semânticas separadas.
- **RF-15 PASS** — a implementação futura pode usar helper puro `searchVaryingKinds(ref)` sem nova decisão de namespace ou alteração do resolver.

## Round 4 self-refutation

- **RF4-01 PASS** — R5 demonstra um qualified VARYING DATA realmente resolvível: `SEARCH-COUNTER OF SOME-GROUP`.
- **RF4-02 PASS** — a occurrence projetada de R5 é DATA/{DATA}, não `{DATA, INDEX}`.
- **RF4-03 PASS** — R5 reexecuta o `CobolReferenceResolver` real e retorna `RESOLVED`.
- **RF4-04 PASS** — R5 retorna `selectedCandidate.kind = DATA`.
- **RF4-05 PASS** — `SOME-GROUP` permanece preservado no qualifier AST (`name = SOME-GROUP`, `writtenText = OF SOME-GROUP`) e observável nas occurrences atuais.
- **RF4-06 PASS** — a policy do qualifier permanece independente da policy do root; nenhuma occurrence sintética é criada.
- **RF4-07 PASS** — R3 continua demonstrando que qualified INDEX não é selecionado com `{DATA}`.
- **RF4-08 PASS** — `eval.md` contém os cinco casos da matriz: bare DATA, bare INDEX, bare missing, qualified DATA e qualified INDEX.
- **RF4-09 PASS** — R5 demonstra que qualified não significa automaticamente unresolved.
- **RF4-10 PASS** — a auditoria documental não encontrou matriz incompleta após a inclusão de R5.
- **RF4-11 PASS** — nenhum documento afirma que qualified admite INDEX no Enterprise COBOL 6.4.
- **RF4-12 PASS** — nenhum documento afirma que o root de SEARCH VARYING pode ser subscripted.
- **RF4-13 PASS** — o resolver permanece sem alteração; R5 usa re-resolution controlada.
- **RF4-14 PASS** — zero `src/main` foi alterado.
- **RF4-15 PASS** — após R5, não resta decisão semântica para implementar SearchStatement/SearchWhen/VARYING; permanece apenas autorização humana.

## Tentativa final de refutação do Round 4

Foi procurado o menor contraexemplo em que um DATA qualified válido não resolvesse com `{DATA}`, ou em que a semântica IBM Enterprise COBOL 6.4 exigisse INDEX para uma forma qualified. R5 elimina o primeiro contraexemplo com o resolver atual; R3/R4 e a documentação IBM eliminam o segundo. Nenhum contraexemplo foi encontrado.

## Tentativa manual de contraexemplo

O menor contraexemplo procurado foi um `VARYING` qualified cujo índice hierarquicamente compatível pudesse ser selecionado. A grammar local aceita esse what-if, e o resolver seleciona INDEX somente quando a policy errada admite INDEX; isso confirma, em vez de quebrar, a regra de exclusão qualified. A busca por uma forma bare que exigisse excluir INDEX não encontrou contraexemplo: IBM 6.4 permite index-name ou item índice/integer; o root não tem subscript na regra local.
