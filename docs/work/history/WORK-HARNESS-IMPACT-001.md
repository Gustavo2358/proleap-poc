# WORK-HARNESS-IMPACT-001 — Downstream semantic impact classification

## Resultado

O work item institucionalizou no harness uma taxonomia downstream baseada em
evidência, com exatamente oito classes canônicas, classe primária única e a
regra `earliest broken layer wins`. Impacto permanece separado de priority,
severity e autorização de remediação.

## Taxonomia e registro

As classes canônicas são:

```text
BLOCKS_SEMANTIC_PRODUCT
BLOCKS_IR
BLOCKS_CFG
BLOCKS_DATAFLOW
BLOCKS_DEPENDENCY_FACTS
REDUCES_PRECISION
UNASSESSED
NOT_APPLICABLE
```

Todo registro exige `class`, `rationale` e `evidence`. `reassess_when` é
obrigatório somente para `UNASSESSED`, opcional para as demais classes e, se
presente, não pode ser vazio. `confidence` foi conscientemente adiada.

## Validator e lifecycle

`HarnessDocsTest` protege vocabulário fechado, conteúdo mínimo, classes
desconhecidas, `UNASSESSED` sem reassessment, campos vazios e consistência
entre `active/index/history`, incluindo ausência de sobreposição.

O protocolo passou a separar lifecycle hygiene local/estrutural de verificação
remota/contextual. O validator cobre apenas a parte local; merge/closure remoto
não é inferido por heurística e, sem contexto confiável, não deve ser assumido.

## F-01 e escopo

F-01 continua `CONFIRMED_KNOWN_BUG` com downstream impact `UNASSESSED`; nenhuma
correção foi feita. A mudança não criou produtos downstream nem alterou
produção: zero `src/main/**`, Semantic Product, Cobol Lower, IR, CFG, dataflow
ou dependency extractor.

O work item foi concluído e aprovado para closure no PR #22. Este resumo é
promovido no próprio PR de implementação; sua entrada em `main` dependerá da
integração desse PR.
