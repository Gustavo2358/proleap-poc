# Pipeline e fronteiras de análise

O pipeline atual preserva produtos separados e imutáveis:

```text
fonte COBOL físico
  → normalização / SourceMap
  → preprocessing / COPY
  → parse tree ANTLR
  → AST semântica
  → compilation units e symbol tables
  → ocorrências de referência
  → resolução nominal
  → classificação externa pós-resolução focalizada
  → futuras análises CFG e dataflow
```

O caminho downstream adotado pela ADR-0013 é incremental e ainda não está todo
implementado:

```text
COBOL Frontend
  → COBOL Semantic Product
  → CobolLower
  → Analysis IR
  → CFG
  → Statement Effects / Storage Semantics
  → Reaching Definitions
  → Possible Values
  → Dependency Facts
```

O Semantic Product é a boundary COBOL-specific materializada entre os produtos
do frontend e o lowering. Neutralidade entre COBOL e outras linguagens começa
no lowerer/Analysis IR, não nessa boundary. A implementação inicial do produto
e de seu projector cobre apenas a prova DATA/MOVE/CALL e ainda não está ligada
ao composition root; `WORK-SEMANTIC-PRODUCT-002` governa sua remediation. As
fases a partir de `CobolLower` continuam futuras e aparecem aqui como direção e
dependências, não como produtos existentes.

Cada seta produz um artefato para a fase seguinte; uma fase não deve gravar conclusões de análise posterior no artefato anterior.

## Limites atuais

- O `SourceMap` nasce no texto físico e é composto pelas transformações.
- A parse tree representa a estrutura reconhecida pelas gramáticas.
- A AST preserva estrutura semântica, texto/provenance quando exigidos e construções opacas; não contém binding, CFG ou dataflow.
- Symbol tables modelam declarations, scopes, namespaces, entidades e relações declarativas, sem valores de runtime.
- Occurrences identificam usos tipados sem fazer lookup.
- `ReferenceResolution` é produto separado e imutável; preserva candidatos, status e diagnósticos para binding nominal.
- O Semantic Product preserva somente facts COBOL canônicos, materializados,
  imutáveis e suficientes ao lowering declarado. Ele não é AST, IR, CFG,
  serializer nem snapshot; sua coverage incremental não limita a quantidade de
  ocorrências suportadas na `ProgramUnit`.
- Projectors reconciliam AST tipada, units, symbols, occurrences, resolution,
  report, policy e provenance segundo a autoridade de cada produto. Eles não
  executam parsing, binding, gap analysis ou value inference novamente.
- `CobolLower` traduz o contrato COBOL-specific para Analysis IR sem reabrir os
  internals do frontend. A IR representa operações e controle necessários às
  análises posteriores sem impor uma taxonomia universal ao frontend.
- CFG, effects/storage, reaching definitions, possible-values e análise de
  linguagens embarcadas ainda não são produtos do pipeline. A ausência deles
  deve continuar observável como boundary/incompletude, não como resultado
  vazio.

## Readiness downstream

Cada construct do Semantic Product é avaliado separadamente por `surface`,
`identity`, `structure`, binding nominal, CFG readiness, effects/dataflow
readiness, unknowns, provenance e coverage. Um construct pode ter structure
suficiente e predicate parcial; essa combinação continua útil para lowering
conservador, mas não pode receber claim de completude maior.

- **Lowering readiness:** um consumer que conhece somente o port consegue
  reconstruir os facts suportados sem AST, symbol table, occurrences, resolver
  ou report.
- **CFG readiness:** um construct marcado ready contém informação suficiente
  para enumerar successors conservadoramente; unknown não vira fallthrough.
- **Effects/dataflow readiness:** operands e roles permitem derivar os
  reads/writes declarados sem voltar ao frontend; o Semantic Product não
  publica `GEN/KILL`.

Program points/anchors do produto representam ordem estrutural determinística,
não execution order, reachability ou arestas de CFG. Binding nominal continua
separado de valores de runtime conforme ADR-0004. Do mesmo modo, identidade
nominal DATA não é storage físico: alias, overlap, `REDEFINES` e `RENAMES`
dependem de Storage Semantics posterior.

Execuções equivalentes podem reproduzir handles e ordem para transporte
determinístico; isso não estabelece identidade persistente após edição ou
mudança de analyzer/contract version.

`ConditionSemantics` também ainda não existe em produção. ADR-0012 (`Accepted`) define esse produto imutável entre resolução nominal e consumidores de predicates para especializar condições cujo significado depende do binding, sem reescrever AST/occurrences/resolution e sem afirmar validade type-sensitive. A admissibilidade type-sensitive pertence a etapa conceitual posterior, `ConditionValidation`, que consumirá `ConditionSemantics`, informação de declaração/tipo e contratos IBM; ela também ainda não existe. O pipeline conceitual é `Surface AST → ReferenceOccurrences → ReferenceResolution → ConditionSemantics → ConditionValidation → CFG/predicate/dataflow`, com API/schema a decidir em slice futuro. Enquanto os slices executáveis não forem autorizados, o pipeline corrente permanece o diagrama acima e a lacuna continua explícita.

`ExternalClassification` materializa o primeiro slice pós-resolução somente para a shape estrutural autorizada de `DFHRESP(...)`/`DFHVALUE(...)`: binding COBOL válido sempre vence; raiz `UNRESOLVED` pode produzir hipótese CICS `INFERRED` com provenance e occurrences do subtree cobertas. O produto não muta `ReferenceResolution`; relatório e snapshot mantêm o binding original e substituem apenas gaps artificiais explicitamente cobertos por um fato externo ainda bloqueante. Infraestrutura genérica e demais capabilities continuam no backlog.

COPY COBOL não resolvido é input externo ausente, não corrupção automática dos produtos posteriores. O preprocessor publica sua identidade por `Diagnostic.Code.UNRESOLVED_COPY`, sem reinterpretar mensagem humana. Quando o placeholder permite construir parse tree, AST, símbolos, occurrences e resolução de forma coerente, esses produtos e a classificação externa focalizada continuam sendo compostos. Cada COPY ausente permanece gap enumerável, a análise global fica incompleta e a classificação registra `CopyInputCompleteness.INCOMPLETE_UNRESOLVED_COPY`; nessa condição a projeção preserva também os gaps nominais cobertos, pois o universo de declarations COBOL não estava completo. `CopyInputCompleteness.COMPLETE` afirma somente disponibilidade dos COPYs solicitados; erros de preprocessor, lexer/parser recovery e incoerência interna continuam num eixo estrutural separado, fora desse fallback e com o fail-closed anterior.

Este documento descreve a fronteira consolidada. Consulte os [invariantes com IDs](invariants.md), os [ADRs](decisions/index.md) e o [mapa curto de componentes](../../ARCHITECTURE.md).
