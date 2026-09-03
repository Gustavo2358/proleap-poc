# WORK-COND-004 — Preservar estrutura nominal completa de condition-name references

Status: concluído em 2026-09-03 no PR #18. Risco: alto.

O Slice 4 de `BACKLOG-COND-001` foi concluído e aprovado no review humano final do PR #18 (head `671a5d7`, sobre o Discovery aprovado `69e715e`).

## Resultado

- `conditionNameReference` passa a produzir `DataReference` estruturalmente completo, construído dos children diretos do context (`conditionName()`, `inData()`, `conditionNameSubscriptReference()`) — sem `firstDescendant`/reparse textual;
- `baseName` vem exclusivamente do condition-name escrito (nunca do subscript);
- qualifiers `IN`/`OF` são preservados estruturalmente, em ordem escrita, com connectors e spans próprios;
- subscripts viram `SubscriptGroup` tipado com subscripts como expression children;
- subscript qualificado mantém os próprios qualifiers — nunca promovidos à referência raiz;
- o último qualifier usa `QualifierTarget.UNSPECIFIED` (a parse tree não classifica DATA/FILE/MNEMONIC atrás do branch `inData`); não-finais permanecem `DATA` por posição na rule;
- o resolver mantém o compatibility mapping `UNSPECIFIED → {DATA}` (candidate universe inalterado);
- `ContextualConditionTail` continua aberto para o Slice 5; occurrence contextuais e o falso gap do collector não foram tocados;
- nenhuma mudança de política foi feita no collector (`ReferenceOccurrenceCollector`, grammar, snapshots e demais arquivos `must_not_change` permaneceram byte-identical).

## Decisões importantes

- a decisão final (round 2 do Discovery) foi **manter `DataReference`** — sem criar `ConditionNameReference`/`NominalReference` ou abstração equivalente (decisão D), porque o node novo só codificaria a posição, já estrutural nos containers tipados;
- a AST permanece pre-binding: nenhuma decisão DATA/INDEX/CONDITION, nenhum lookup ou candidate selection no lowering;
- a qualification não pode inferir `DATA` apenas pelo branch `inData` — o alvo é posicional e termina em `UNSPECIFIED`;
- `BACKLOG-RES-004` foi separado como dependência: a ampliação `UNSPECIFIED → {DATA, FILE}` regride `RESOLVED → UNSUPPORTED_DIALECT_OPTION/AMBIGUOUS` no contracaso local-DATA × outer-GLOBAL-FILE, porque a precedência local/GLOBAL pós-qualification (IBM resolution-of-names step 3) é regra geral de resolution e não pertence ao slice estrutural.

## Evidência

- oracles CN-01..CN-12 + S4-BOUNDARY-01 em `ConditionNameSurfaceAstTest`;
- nested-subscript adversarial (`FLAG-88 OF CUSTOMER(SUB OF SUB-GROUP)`);
- contracaso local DATA × outer GLOBAL FILE (`IF C OF Q`) mantido como regressão;
- provenance Contract A, pre-order/identity e `assertActualProductsJoin`;
- consumer impact CICS verificado (delta bounded documentado);
- gates `fast`, `performance`, `semantic` e `full` verdes; semantic challenge pass A–J registrado em `state.md` (arquivado neste PR).

Nota de fechamento — Challenge H (reparse textual): o diff do builder contém o fallback textual pré-existente de `buildQualifiers` (`written.replaceFirst("(?i)^(IN|OF)\\s+", "")`), extraído mecanicamente para o helper compartilhado `buildQualifier`. Esse fallback NÃO participa do lowering de `conditionNameReference`: para `InDataContext`, o `dataName` estrutural está sempre presente e o valor vem diretamente da parse tree — nenhum reparse textual foi introduzido para interpretar a estrutura de condition-name reference.

## Próximos trabalhos

- `BACKLOG-RES-004` (IBM resolution-of-names step 3; destravará `{DATA, FILE}`);
- Slice 5 de `BACKLOG-COND-001` (occurrences contextuais / remoção do acoplamento do collector ao `grammarRule` `conditionNameReference`).
