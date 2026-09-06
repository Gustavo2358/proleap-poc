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
  → validação de integridade cross-product
  → classificação externa pós-resolução focalizada
  → relatório canônico de análise
  → COBOL Semantic Product / CobolSemanticPort
  → adapters JSON, snapshots e apresentação
```

O caminho downstream adotado pela ADR-0013 é incremental e atravessa fronteiras
de repositório:

```text
este repositório: COBOL Frontend
  → COBOL Semantic Product
  → cobol-semantic-product.json
  ── fronteira de repositório ──
cobol-lower: JSON → AIR 2.0.0 Publication (depende de air-java)
  ── fronteira de repositório ──
analysis-cfg: AIR Publication → CFG
  → Statement Effects / Storage Semantics
  → Reaching Definitions
  → Possible Values
  → Dependency Facts
```

`air-java` já existe como repositório separado e possui o modelo/validator Java
da AIR 2.0.0. `cobol-lower` será uma aplicação/repositório separado.
`analysis-cfg` também já existe separadamente. Portanto este repositório não
implementa AIR, não contém `CobolLower`, não produz AIR diretamente e não
constrói CFG; sua boundary pública termina no Semantic Product JSON.

O Semantic Product é a boundary COBOL-specific materializada entre os produtos
do frontend e o lowering. Neutralidade entre COBOL e outras linguagens começa
no lowerer/Analysis IR, não nessa boundary. O produto está ligado ao
composition root e publica DATA, MOVE literal, CALL por identifier/expression,
IF estrutural, entry primária/início canônico, saída local GOBACK e um inventário
positivo dos demais statements. Entry inventory alternativo e assinatura não
projetada preservam disponibilidade/gaps. DATA e o profile
atual de CALL recebem readiness suficiente no código; isso não certifica AIR
2.0.0. O [audit bilateral](semantic-product-air-v2-audit.md) encontrou gaps de
entrada/controle, avaliação/interação e perda de endereçamento. MOVE/IF são
parciais; `ObservedStatement` bloqueado no port admite `opaque` conservador
AIR com fronteiras abertas. A [matriz canônica](../domain/cobol-semantic-product.md)
distingue estados do código e suficiência externa. As fases a partir de
`cobol-lower` aparecem aqui como direção e dependências cross-repo, não como
produtos a implementar neste repositório.

Cada seta produz um artefato para a fase seguinte; uma fase não deve gravar conclusões de análise posterior no artefato anterior.

## Limites atuais

- O `SourceMap` nasce no texto físico e é composto pelas transformações.
- A parse tree representa a estrutura reconhecida pelas gramáticas.
- A AST preserva estrutura semântica, texto/provenance quando exigidos e construções opacas; não contém binding, CFG ou dataflow.
- Symbol tables modelam declarations, scopes, namespaces, entidades e relações declarativas, sem valores de runtime.
- Occurrences identificam usos tipados sem fazer lookup.
- `ReferenceResolution` é produto separado e imutável; preserva candidatos, status e diagnósticos para binding nominal.
- `SemanticProductIntegrityValidator` reconcilia AST, tables/scopes, occurrences,
  resolution, declaration relations e candidates imediatamente após a resolução,
  reutilizando os scope indexes. Corrupção interna lança
  `SemanticProductIntegrityException` antes do primeiro consumidor pós-resolution;
  incompletude semântica válida permanece resultado normal. O validator não cria
  produto, não resolve nomes e não repara fatos; o [contrato de integridade](../domain/reference-resolution.md#integridade-cross-product)
  define seus joins. Novos orquestradores devem preservar essa ordem.
- O Semantic Product preserva somente facts COBOL canônicos, materializados,
  imutáveis e suficientes ao lowering declarado. Ele não é AST, IR, CFG,
  serializer nem snapshot; sua coverage incremental não limita a quantidade de
  ocorrências suportadas na `ProgramUnit`.
- Projectors reconciliam AST tipada, units, symbols, occurrences, resolution,
  report, policy e provenance segundo a autoridade de cada produto. Eles não
  executam parsing, binding, gap analysis ou value inference novamente.
- O `cobol-lower` externo consumirá `cobol-semantic-product.json`, dependerá de
  `air-java` e traduzirá o contrato COBOL-specific para AIR sem reabrir os
  internals do frontend.
- O `analysis-cfg` externo consome AIR Publication e é responsável pela construção de CFG.
  Effects/storage, reaching definitions e possible-values permanecem produtos
  downstream separados; ausência de readiness deve continuar observável como
  boundary/incompletude, não como resultado vazio.

## Readiness downstream

Cada construct do Semantic Product é avaliado separadamente por `surface`,
`identity`, `structure`, binding nominal, CFG readiness, effects/dataflow
readiness, unknowns, provenance e coverage. Um construct pode ter structure
suficiente e predicate parcial; essa combinação continua útil para lowering
conservador, mas não pode receber claim de completude maior.

- **Lowering readiness:** o probe local que conhece somente o port consegue
  reconstruir os facts suportados sem AST, symbol table, occurrences, resolver
  ou report; a fronteira cross-repo transporta esses facts pelo JSON.
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

Na AIR V2, fatos declarativos de associação, duração, codec e alias precisam
estar disponíveis antes do lowering preciso que os usa; Storage Semantics
deriva suas consequências depois. Esse consumer não recupera fatos COBOL
ausentes pela AST. `unknown_type` e associação aberta permitem iniciar
representação conservadora sem inventar células. O primeiro CFG fechado exige
entrada e saída executáveis além dos anchors estruturais atuais.

Execuções equivalentes podem reproduzir handles e ordem para transporte
determinístico; isso não estabelece identidade persistente após edição ou
mudança de analyzer/contract version.

`ConditionSemantics` também ainda não existe em produção. ADR-0012 (`Accepted`) define esse produto imutável entre resolução nominal e consumidores de predicates para especializar condições cujo significado depende do binding, sem reescrever AST/occurrences/resolution e sem afirmar validade type-sensitive. A admissibilidade type-sensitive pertence a etapa conceitual posterior, `ConditionValidation`, que consumirá `ConditionSemantics`, informação de declaração/tipo e contratos IBM; ela também ainda não existe. O pipeline conceitual é `Surface AST → ReferenceOccurrences → ReferenceResolution → ConditionSemantics → ConditionValidation → CFG/predicate/dataflow`, com API/schema a decidir em slice futuro. Enquanto os slices executáveis não forem autorizados, o pipeline corrente permanece o diagrama acima e a lacuna continua explícita.

`ExternalClassification` materializa o primeiro slice pós-resolução somente para a shape estrutural autorizada de `DFHRESP(...)`/`DFHVALUE(...)`: binding COBOL válido sempre vence; raiz `UNRESOLVED` pode produzir hipótese CICS `INFERRED` com provenance e occurrences do subtree cobertas. O produto não muta `ReferenceResolution`; relatório e snapshot mantêm o binding original e substituem apenas gaps artificiais explicitamente cobertos por um fato externo ainda bloqueante. Infraestrutura genérica e demais capabilities continuam no backlog.

COPY COBOL não resolvido é input externo ausente, não corrupção automática dos produtos posteriores. O preprocessor publica sua identidade por `Diagnostic.Code.UNRESOLVED_COPY`, sem reinterpretar mensagem humana. Quando o placeholder permite construir parse tree, AST, símbolos, occurrences e resolução de forma coerente, esses produtos e a classificação externa focalizada continuam sendo compostos. Cada COPY ausente permanece gap enumerável, a análise global fica incompleta e a classificação registra `CopyInputCompleteness.INCOMPLETE_UNRESOLVED_COPY`; nessa condição a projeção preserva também os gaps nominais cobertos, pois o universo de declarations COBOL não estava completo. `CopyInputCompleteness.COMPLETE` afirma somente disponibilidade dos COPYs solicitados; erros de preprocessor, lexer/parser recovery e incoerência interna continuam num eixo estrutural separado, fora desse fallback e com o fail-closed anterior.

Este documento descreve a fronteira consolidada. Consulte os [invariantes com IDs](invariants.md), os [ADRs](decisions/index.md) e o [mapa curto de componentes](../../ARCHITECTURE.md).
