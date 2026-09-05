# Semantic Product de produção — DATA, MOVE, CALL e IF/ELSE

## Problema

O Discovery de `WORK-SEMANTIC-PRODUCT-001` provou a boundary A2+B com o
fixture mínimo:

```cobol
01 WS-PGM PIC X(8).
MOVE 'PGMA' TO WS-PGM.
CALL WS-PGM.
```

Esse fixture demonstrou estado COBOL-specific próprio, closure, imutabilidade,
identities namespaced, joins por binding nominal, provenance, program order e
target de runtime `UNKNOWN`. Ele era prova de boundary, não cardinalidade de
produção.

A primeira implementação transformou a prova em limite: o state/port têm
`move()`, `call()` e um `Ordering` específico; o projector exige exatamente um
`MOVE`, exatamente um `CALL`, um único target e uma única DATA comum aos dois.
O projector também produz localmente parte do gap/readiness do CALL em vez de
consumir `ResolutionAnalysisReport`. Esses são fatos do código atual e
permanecem úteis como baseline, mas conflitam com INV-SP-001/002/004 e com a
direção aceita na ADR-0013.

Um produto que entende uma classe de statement, mas publica somente sua primeira
ocorrência, cria falsa completude. Do mesmo modo, omitir um statement observado
porque sua semântica é parcial faz downstream confundir “não suportado” com
“não existe”. A remediation precisa corrigir cardinalidade, extensibilidade e
readiness antes de congelar o transporte JSON ou iniciar `CobolLower`.

## Objetivo

Materializar, para cada `ProgramUnit` selecionada, um Semantic Product
COBOL-specific A2+B, fechado e imutável, que:

- publique todas as ocorrências semanticamente cobertas de DATA, `MOVE`
  literal para DATA, `CALL` identifier/expression e `IF/ELSE`;
- preserve statement identity, program point estrutural, ordem, nesting,
  branches, operands, roles, binding nominal, provenance e policy;
- represente constructs observados mas partial/unsupported/unknown sem omissão;
- cresça por famílias/coleções tipadas de facts, sem singleton por construct;
- permita a um consumer que conhece somente o port reconstruir a parte suportada
  da unit e iniciar lowering sem AST, symbols, occurrences, resolver ou report;
- declare separadamente lowering readiness, CFG readiness e effects/dataflow
  readiness por construct;
- mantenha target dinâmico de CALL desconhecido e `DataItemId` distinto de
  identidade final de storage;
- só depois dessa prova produza um JSON determinístico por adapter separado.

O Semantic Product continua anterior a `CobolLower`, Analysis IR, CFG,
Statement Effects/Storage Semantics, Reaching Definitions, Possible Values e
Dependency Facts. Este work item prepara a entrada dessas fases; não as
implementa.

## Domínio de entrada suportado

O domínio é uma fonte COBOL normalizada e preprocessada cuja compilation unit
contenha uma ou mais `ProgramUnit`; cada publicação é namespaced por uma unit
selecionada. Dentro dela podem existir quaisquer quantidades, inclusive zero,
das capabilities cobertas:

- DATA entries da superfície de declaração suportada, inclusive múltiplas
  declarações necessárias aos facts publicados, com `PIC` quando disponível e
  incompletude localizada quando atributos relevantes faltarem;
- todos os `MOVE` cuja capability inicial seja source literal tipada e um único
  target DATA nominalmente resolvido;
- todos os `CALL` cuja sintaxe seja identifier/expression e cujo operando DATA
  tenha binding nominal representável, sem exigir que compartilhe identidade
  com algum `MOVE` específico;
- todos os `IF/ELSE` estruturais suportados, inclusive IF sem ELSE, nesting
  simples e statements antes, dentro e depois de branches;
- todos os statements observados no inventário da unit, mesmo quando a família
  ainda não possui fact completo: esses casos atravessam como coverage/gap
  localizado, não somem.

Outra forma de `MOVE`, `CALL` literal, condição cuja semântica pós-binding ainda
não exista, `EVALUATE`, `PERFORM`, `GO TO`, terminal, `ALTER`, `SEARCH` ou outro
statement não eleva automaticamente a ProgramUnit inteira a falha. O produto
publica o que é independentemente sustentado e registra a forma fora da
capability como partial/unsupported/unknown. Enrichment semântico dessas
famílias pertence aos slices indicados no backlog.

Multiple/nested program units não autorizam lookup global: cada publicação usa
`ProgramUnitId`, parentage e identities compostas conforme ADR-0005. Coverage
incremental pode limitar shapes semânticas aceitas; nunca limita artificialmente
quantas ocorrências da shape aceita aparecem na unit.

## Classes semânticas

- **Unit e contexto:** identity namespaced da unit, parentage/structural path
  necessário, policy normalizada/versionada e availability dos facts que dela
  dependem.
- **Declarations:** coleção imutável de DATA facts com handle boundary-owned,
  nome canônico, atributos suportados, provenance e coverage. Handle nominal
  não afirma storage independente.
- **Statements:** coleção/família tipada e extensível com identity, kind,
  program point, structure, operands/roles, binding, provenance, coverage e
  readiness. O contrato inicial inclui `MOVE`, `CALL` e `IF`; inventário
  parcial/unsupported continua representado.
- **Structure:** relações tipadas de containment/branch/nesting, reconstruíveis
  sem posição incidental. O shape concreto pode ser hierárquico, flat por
  identities ou híbrido; o oracle decide pela suficiência ao consumer.
- **Binding nominal:** status, reason, candidates e selected identity somente
  quando a resolução canônica é única. Ambiguity nunca escolhe candidate.
- **Readiness:** dimensões distintas para structure/lowering, CFG e
  effects/dataflow. A implementação pode escolher nomes equivalentes a
  `SUFFICIENT`, `PARTIAL`, `BLOCKED` e `NOT_APPLICABLE`, desde que significado
  e claim scope sejam explícitos.
- **Coverage e uncertainties:** modeled/partial/unsupported/input-missing e
  gaps localizados, mais summary reconciliado com o inventário de statements.
- **Provenance:** localização expandida/original, exactness e include chain por
  declaration, statement, operand/reference, branch/condition e gap localizado.
- **Transport:** projeção JSON posterior, versionada e determinística, derivada
  somente do state/port correto. JSON não é uma classe de domínio do produto.

Adicionar nova família de statement não pode exigir um novo campo singleton no
state fundamental nem um novo método singular que redefina o envelope. Também
não autoriza `Map<String,Object>`: extensão permanece tipada.

## Premissas

- ADR-0013 e INV-SP-001–006 governam a direção. A decisão H permanece A2
  boundary-owned + B read-only sobre uma publicação fechada.
- O código atual é a fonte de verdade do estado implementado: core e adapter
  cobrem uma única relação DATA/MOVE/CALL; `ExplorerMain` ainda não publica o
  produto; IF facts, coverage da unit e JSON não existem.
- A AST atual já materializa `Ast.IfStatement` com condition, `thenBranch`,
  `elseBranch`, explicit termination e nesting; o collector percorre condition
  e ambos os branches. Isso sustenta o slice estrutural de IF, não predicate
  completo, CFG ou reachability.
- AST, compilation units, symbol tables, occurrences, resolution, report,
  policy e provenance permanecem produtos separados e imutáveis. O projector
  faz joins por identities canônicas, sem mutá-los.
- `ResolutionAnalysisReport` é autoridade de seus gaps, readiness e claims; o
  projector não mantém classificação paralela. Cada produto canônico governa
  somente os fatos que realmente publica.
- `ProgramPoint` é ordem estrutural determinística da publicação, não ordem de
  execução. Branches e nesting exigem relações tipadas próprias.
- Binding nominal de `CALL WS-PGM` não é value analysis. Literal de algum
  `MOVE`, `VALUE` de declaration ou proximidade textual nunca vira target final.
- Execuções equivalentes reproduzem handles, facts e ordem transportáveis; isso
  não promete identidade persistente após edição ou mudança de versão.
- Um `DataItemId` identifica a declaration/binding nominal. Storage layout,
  regions, aliases e overlap de `REDEFINES`/`RENAMES` continuam posteriores.
- Fixture, corpus e implementação estreita são evidência; o contrato deste
  work item e as fontes canônicas definem a direção futura.

## Comportamento esperado

### Fronteira e autoridades

```text
AST / units / symbols / occurrences / resolution / report / policy / provenance
                              │
                              ▼
              projector COBOL-specific de facts canônicos
                              │
                              ▼
       A2 — state próprio, imutável, fechado e namespaced
                              │
                              ▼
               B — port tipado, read-only e fechado
                              │
                              ▼
               consumer independente de lowering-readiness
```

O projector projeta e reconcilia; não redescobre. A AST tipada fornece
surface/shape; units fornecem namespace; symbols fornecem declarations;
occurrences fornecem roles; resolution fornece binding, candidates, status e
reason; report fornece gaps/readiness/claims; provenance/policy vêm de seus
produtos canônicos. Ausência de informação vira estado explícito.

Boundary e port não importam nem retêm AST, parser/ANTLR, SymbolTable,
occurrences, resolver, report, `SourceMap` completo, snapshots, HTML ou
`ExplorerMain`. O package/área de projection é o único ponto que conhece as
entradas do frontend. Nome e layout exatos dessa área serão decididos pelo
oracle e pelo gate de arquitetura; `CobolMoveCallAdapter` não é seam futura.

### Cardinalidade, ordem e structure

Para uma unit com `N` DATA, `N` MOVE, `N` CALL e múltiplos IF/ELSE, o produto
publica cada ocorrência coberta exatamente uma vez e mantém identity/anchor
estável dentro da publicação. Zero ocorrências de uma família é distinguível de
capability indisponível ou statement observado porém unsupported.

Statements em branches continuam pertencendo ao inventário da unit e à sua
relação estrutural. Um consumer deve reconstruir sequência, IF, THEN, ELSE,
nesting e fallthrough estrutural potencial sem inspecionar AST. O produto não
publica edge, reachability, truth value ou branch probability.

### Disciplina de readiness por construct

| Construct | Surface | Identity | Structure | Nominal binding | CFG readiness | Effects/dataflow readiness | Unknowns | Provenance | Coverage |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| DATA suportada | declaration, nome e atributos cobertos | `DataItemId` namespaced | parentage/hierarquia necessária ao slice | declaration/entity reconciliada | `NOT_APPLICABLE` isoladamente | partial até existir Storage Semantics para layout/alias; identity nominal disponível | PIC/clause/layout ausente localizado | declaration e atributos publicados | cada entry observada classificada |
| `MOVE` literal → DATA | source literal e target tipados | statement, operand e DATA handles | program point e containment/branch | target com status/reason/candidates | suficiente para fallthrough estrutural, salvo gap explícito | suficiente para derivar source literal e `DEF` nominal do target; storage effect permanece partial | binding/shape/storage gaps localizados | statement, literal e target | cada MOVE observado modeled ou gap explícito |
| `CALL` identifier/expression | syntax e operando DATA tipados | statement, operand e DATA handles | program point, containment e exception structure coberta | operando com status/reason/candidates; runtime target separado | suficiente apenas para sucessor local/fallthrough coberto; efeitos interprocedurais separados | suficiente para `USE` nominal do operando de target; efeitos da chamada continuam partial | runtime target `UNKNOWN`, policy/linkage/gaps localizados | statement e operando | cada CALL observado modeled ou gap explícito |
| `IF/ELSE` estrutural | condition surface, then/else e termination | statement, condition, branch/child handles | ordem, branches, nesting e join reconstruíveis | references da condition usam binding canônico disponível | estruturalmente suficiente para dois successors conservadores e join/fallthrough; predicate pode ser partial | references/roles suficientes apenas onde condition surface/binding suportam `USE`; sem effects de branch | predicate/validation/branch knowledge parciais localizados | IF, condition e branches/children | cada IF observado e seus children classificados |
| statement fora da capability | kind/surface preservável e anchor quando disponível | identity namespaced se estruturalmente produzida | containment conhecido ou gap | binding existente continua transportado; nada é fabricado | `PARTIAL/BLOCKED`, nunca fallthrough implícito | `PARTIAL/BLOCKED`, nunca efeito vazio | motivo específico | provenance disponível | observed + partial/unsupported/input-missing |

Readiness é uma claim sobre a informação publicada, não sobre a fase futura já
existir. Marcar IF como CFG-ready significa que o futuro lowerer pode enumerar
successors conservadores; não significa que CFG foi construída. Marcar MOVE
como effects-ready em nível nominal não transforma `DataItemId` em storage
region nem publica `GEN/KILL`.

### Oracle downstream

O primeiro target model deve conter, no mínimo:

```cobol
01 WS-X PIC X(8).
01 FLAG PIC 9.

MOVE 'A' TO WS-X.
IF FLAG = 1
    MOVE 'B' TO WS-X
ELSE
    MOVE 'C' TO WS-X
END-IF.
CALL WS-X.
```

O fixture final pode acrescentar outro DATA, MOVE, CALL e IF nested para provar
cardinalidade e nesting. Um fake lowerer que só conhece o port deve conseguir
reconstruir declarations, statements, branches, operands/roles, bindings,
program points, coverage, gaps e provenance. Ele não calcula CFG nem reaching
definitions, mas demonstra informação suficiente para que, futuramente, o
join no CALL permita:

```text
RD(CALL, WS-X) = { MOVE 'B' TO WS-X, MOVE 'C' TO WS-X }
possible values at CALL = { "B", "C" }
```

O `MOVE 'A'` não alcança esse ponto pelos dois caminhos e o Semantic Product
não deve antecipar essa conclusão; ela é somente o oracle arquitetural das
boundaries posteriores.

### Critérios de aceitação

1. State/port aceitam N statements e não têm cardinalidade singleton por kind.
2. Todas as ocorrências cobertas da unit são publicadas exatamente uma vez.
3. Statement partial/unsupported observado permanece no inventário/coverage.
4. IF/ELSE e nesting são reconstruíveis sem AST e sem edges de CFG no produto.
5. MOVE/CALL preservam operands, roles, identities e binding sem runtime value.
6. Matriz de readiness é observável e coerente com coverage individual/global.
7. Consumer independente usa apenas o port e consegue preparar lowering.
8. Boundary não tem frontend leakage; projector não executa nova análise.
9. Handles/ordem são determinísticos em execuções equivalentes e não são
   anunciados como identidade persistente.
10. JSON só é criado depois dos critérios estruturais e do consumer estarem
    verdes; seu envelope é extensível e preserva incompletude.

## Comportamento diante de incerteza

| Situação | Publicação obrigatória | Proibição |
| --- | --- | --- |
| zero statements de uma família | coleção vazia acompanhada de capability/coverage disponível | confundir com capability não produzida |
| segundo, terceiro ou enésimo MOVE/CALL suportado | um fact por ocorrência, com identity/program point próprios | selecionar primeiro/último ou exigir par MOVE→CALL |
| MOVE fora da shape inicial | statement observado + surface possível + partial/unsupported e motivo | falhar a unit inteira ou omitir o MOVE |
| CALL variável resolvido nominalmente | operando DATA, binding e runtime target `UNKNOWN` | usar literal/`VALUE`/MOVE anterior como target final |
| ambiguity/unresolved/input missing | status, reason, todos os candidates aplicáveis, gaps e facts independentes | escolher candidate, fabricar handle ou empty success |
| IF com predicate semanticamente parcial | branches/nesting e references sustentadas + predicate/readiness partial | apagar IF, escolher branch ou alegar reachability |
| ELSE ausente | branch ausente explicitamente, com fallthrough estrutural reconstruível | tratar como unsupported ou inventar branch |
| statement unsupported entre statements suportados | inventário/coverage localizado preserva sua posição/containment | fechar a sequência como se o statement não existisse |
| report e facts individuais divergem | falha fechada identificando autoridade/unit/fact | reconciliar por heurística ou elevar claim global |
| provenance aproximada ou COPY ausente | exactness/input gap e facts independentes preservados | inventar source span ou apagar a unit inteira |
| storage/layout não modelado | identity nominal + storage/effects readiness partial | assumir storage independente por `DataItemId` |

## Fora de escopo

- implementar ou desenhar prematuramente `CobolLower`, Analysis IR, CFG,
  Statement Effects, Storage Semantics, Reaching Definitions, Possible Values,
  targets dinâmicos finais ou Dependency Facts;
- implementar `EVALUATE`, `PERFORM`, `GO TO`, terminal semantics, `ALTER` ou
  `SEARCH` facts neste slice; cada família tem handoff próprio;
- corrigir F-01, F-SP-007, `ConditionSemantics`, `ConditionValidation`, grammar,
  AST, symbol tables, occurrence collector, resolver ou report para acomodar o
  produto;
- resolver possible-values por busca textual, ordem linear, `VALUE`, primeiro
  `MOVE`, nearest write ou qualquer mini-dataflow no projector/consumer;
- transformar o Semantic Product em AST 1:1, IR, CFG, bag sem tipos, schema
  multi-language, plugin framework, serializer genérico ou snapshot;
- decidir antecipadamente representação hierárquica versus flat/híbrida de
  branches sem o oracle do consumer;
- criar `semantic-product.json` antes do Checkpoint 7 ou colocar JSON no
  state/port/core do lowerer;
- implementar futuro JSON input adapter, `LowererInputPort`, adapter in-memory
  ou repositório do `CobolLower`;
- criar identidade persistente/migração entre edições ou versões;
- modelar layout/aliases completos, tratar `REDEFINES`/`RENAMES` como regiões
  prontas ou assumir `DataItemId == StorageId`;
- alterar snapshots/UI, fixtures existentes, baselines, grammar ou refatorar
  componentes fora da menor incisão autorizada por cada checkpoint.

## Regras de domínio relacionadas

- `docs/domain/semantic-ast.md`: surface, statements, IF, conditions, coverage e
  pre-order estrutural atuais.
- `docs/domain/compilation-units.md`: `ProgramUnitId`, parentage e boundary de
  análise.
- `docs/domain/symbol-model.md`: declarations e relações nominais sem binding ou
  layout.
- `docs/domain/reference-resolution.md`: occurrences, binding, candidates,
  CALL semantics e separação de runtime values.
- `docs/domain/provenance.md`: origem física, exactness e include chain.
- `docs/domain/conditional-expressions.md`: condition surface atual e limites
  de `ConditionSemantics`/`ConditionValidation` ainda futuros.

## ADRs/invariantes relacionados

ADR-0013 é a decisão principal. Permanecem aplicáveis ADR-0002, ADR-0003,
ADR-0004, ADR-0005, ADR-0008, ADR-0009, ADR-0010 e ADR-0012.

Os invariantes centrais são INV-SP-001–006, complementados por INV-AST-001–003,
INV-SYM-001, INV-PROV-001/002, INV-RES-001/002, INV-COV-001/003 e
INV-DET-001. A implementação atual é uma exceção explícita nos invariantes
INV-SP; o plano corretivo abaixo é o único caminho autorizado para removê-la.
