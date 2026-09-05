# COBOL Semantic Product

O COBOL Semantic Product é a boundary COBOL-specific, materializada e imutável
entre o frontend e o futuro `CobolLower`. Cada publicação pertence a uma
`ProgramUnit`, expõe somente o `CobolSemanticPort` e permanece fechada depois
da projeção. O contrato atual é regido pela ADR-0013 e pelos invariantes
`INV-SP-001` a `INV-SP-006`.

## Superfície atual

O projector publica:

- todas as declarações DATA selecionadas da unit e o fechamento de declarations
  necessário aos candidates nominais;
- todas as ocorrências suportadas de `MOVE` literal para DATA;
- todas as ocorrências suportadas de `CALL` por identifier/expression;
- todos os `IF` como facts estruturais com condition surface, membership
  `THEN`/`ELSE` e continuation quando conhecida;
- todo statement tipado restante como `ObservedStatement`, sem transformá-lo
  em ausência ou em semântica inventada.

O port também publica policy efetiva, identities namespaced, program points
estruturais, provenance, gaps localizados e coverage reconciliada. Ele não
publica AST, symbol table, occurrences, resolution, report, `SourceMap`, texto
fonte, presentation, IR, CFG, storage regions, effects ou resultados de
dataflow.

`ProgramPoint` e a ordem das coleções são estruturais e determinísticos. Eles
não afirmam execution order, reachability nem CFG edges. `DataItemId` é
identidade nominal de declaração, não identidade de storage.

## Hipótese de lowering e falsificação

A hipótese auditada no fechamento de `WORK-SEMANTIC-PRODUCT-002` foi:

> H: para cada capability declarada como lowering-ready, o port contém todos
> os fatos necessários, sem acesso ao frontend e sem nova interpretação COBOL.

H seria falsa se pelo menos uma destas condições fosse observada:

1. uma claim `SUFFICIENT` dependesse de AST, symbols, occurrences, resolution,
   report, source text, projector, JSON ou presentation;
2. identity, program point, containment, branch membership ou continuation
   necessária não atravessasse o port ou contradissesse seus índices;
3. um operand nominal lowering-ready perdesse role, candidates ou `selected`;
4. runtime unknown, capability gap ou coverage incompleta desaparecesse;
5. o consumer precisasse deduzir literal kind, predicate, storage ou runtime
   target por regra COBOL adicional.

`SemanticPortLoweringProbe` é o oracle test-only independente. Sua única entrada
é `CobolSemanticPort`; ele não usa o consumer de readiness, JSON, frontend,
projector, composition root, ANTLR ou metadata textual. O gate arquitetural
inspeciona source e bytecode do probe.

Quatro states controlados provam que o oracle rejeita, de forma determinística:

- a remoção de um membro `THEN` ainda presente no containment;
- um `CALL` lowering-ready cujo binding perde seleção e candidates;
- um `ObservedStatement` cujo gap de capability é escondido;
- a remoção do próprio `ObservedStatement` com os demais índices ajustados,
  mas com coverage ainda afirmando o inventário original.

Essas mutações existem somente como test doubles. Nenhum estado de produção
mutado é persistido.

## Oracle vertical

Somente pelo port, o probe localiza no fixture principal esta espinha
estrutural:

```text
MOVE UNKNOWN("A") → DATA WS-X

IF RELATION; READ DATA FLAG; predicate NOT_PUBLISHED
├── THEN
│   └── MOVE UNKNOWN("B") → DATA WS-X
└── ELSE
    └── MOVE UNKNOWN("C") → DATA WS-X

continuation
└── CALL DATA WS-X; runtime target UNKNOWN
```

O fixture contém ainda statements independentes e nesting adicional; todos
continuam no inventário e nas relações de containment. A projeção acima isola
a espinha do oracle, não filtra a publicação.

Essa representação é estruturalmente equivalente a `MOVE A → WS-X`, branches
`THEN`/`ELSE` e continuation `CALL WS-X`. Ela deliberadamente não reproduz
`FLAG = 1`: o port contém `shape=RELATION` e a referência `READ FLAG`, mas não
contém operator nem object normalizados. Recriar `= 1` seria reinterpretação do
frontend. Também não calcula edges, reaching definitions, possible values ou
runtime call target.

## Matriz factual

`READY` abaixo corresponde a `ReadinessStatus.SUFFICIENT`. `PARTIAL`, `BLOCKED`
e `NOT_APPLICABLE` conservam o significado do contrato. Os estados descrevem o
profile atualmente materializado, não toda forma COBOL com o mesmo keyword.

| Família | Surface | Identity | Program point | Containment / nesting | Continuation | Operands | Roles | Nominal binding | Runtime unknowns | Provenance | Coverage |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| DATA | nome canônico e `PICTURE` opcional explícito | `DataItemId(unit, localId)` | `NOT_APPLICABLE` | hierarquia/storage não publicados | `NOT_APPLICABLE` | `NOT_APPLICABLE` | `NOT_APPLICABLE` | identidade de declaration, sem lookup downstream | layout/alias permanecem desconhecidos | declaration completa, inclusive include chain/exatidão | por declaration; fixture atual `MODELED` |
| MOVE literal → DATA | valor normalizado e `LiteralKind`; kind atual é `UNKNOWN` | statement, literal operand e target operand distintos | disponível, estrutural | disponível quando o parent é suportado; caso contrário `CONTAINMENT_NOT_PROJECTED` | sequência estrutural deriva da coleção ordenada e do enclosing IF; não é edge | literal source + DATA target | target `WRITE` | status, reason, candidates e `selected` quando resolved | tipo/conversão do literal e storage não conhecidos | statement, source e target | `PARTIAL` enquanto kind for desconhecido |
| CALL identifier/expression | syntax discriminada; o profile ready não inclui arguments/returning/exception flow não projetados | statement e target operand distintos | disponível, estrutural | disponível ou gap localizado | sequência estrutural disponível no profile sem exception flow | DATA operand | `CALL_TARGET` | variável nominal resolved no profile ready | target de programa é sempre `UNKNOWN` com gap próprio | statement e operand | `MODELED` no profile ready; runtime unknown não é omissão |
| IF/ELSE estrutural | condition `shape` e referências DATA conhecidas; predicate não publicado | statement e condition operands distintos | disponível, estrutural | membership ordenado `THEN`/`ELSE`, inclusive nesting | `IfFact.continuation` quando existe e é conhecida; terminal legítimo usa ausência | referências DATA conhecidas da condition | `READ` | preserva status/reason/candidates/selected por referência | truth value, operator/object normalizados e branch tomada não publicados | statement, condition e referências | `PARTIAL` por ausência de predicate semantics |
| `ObservedStatement` | kind/shape/gap genéricos; inclui PERFORM, EVALUATE, GO TO, SEARCH, DISPLAY/GOBACK e outras shapes | statement identity positiva | disponível | posição conhecida quando o parent é suportado; senão gap | nenhuma continuation semântica própria é inferida | não publicados para a família | não publicados | não publicado como semântica da família | qualquer efeito/transferência permanece desconhecido | statement | `PARTIAL`, `UNSUPPORTED` ou `INPUT_MISSING`, nunca ausência |

## Matriz de readiness

As três dimensões não são intercambiáveis.

| Família | Lowering readiness | CFG readiness | Effects/dataflow readiness | Conclusão |
| --- | --- | --- | --- | --- |
| DATA | `READY` | `NOT_APPLICABLE` | `PARTIAL` | declaração nominal pode atravessar lowering; storage/layout não |
| MOVE literal → DATA | `PARTIAL` | `READY` no profile estrutural conhecido | `PARTIAL` | estrutura e DEF nominal existem, mas literal kind e storage impedem precisão semântica completa |
| CALL identifier/expression | `READY` no profile atual | `READY` sem exception flow não projetado | `PARTIAL` | lowerer pode representar chamada por variável e runtime target desconhecido; não pode resolver o programa chamado |
| IF/ELSE estrutural | `PARTIAL` | `READY` quando branches/continuation/containment são exatos | `PARTIAL` | estrutura de branch está pronta; predicate semantics não está |
| `ObservedStatement` | `BLOCKED` | `BLOCKED` | `BLOCKED` | inventário está disponível; lowering semântico da família não está |
| Publicação completa do fixture | `BLOCKED` | `BLOCKED` | `BLOCKED` | o agregado é limitado pelo `ObservedStatement`; isso não rebaixa facts independentes |

Portanto, a resposta à hipótese H é positiva somente para as claims atuais de
DATA e do profile de CALL. Não há dependência escondida do frontend para essas
claims. Isso não torna MOVE, IF, o fixture completo ou a linguagem inteira
lowering-ready.

## Destino dos gaps e unknowns

| Gap / incerteza | Antes de `CobolLower`? | Pode atravessar lowering? | Dependência posterior |
| --- | --- | --- | --- |
| `LITERAL_KIND_NOT_PUBLISHED` | sim, antes de promover MOVE a `READY`; não bloqueia iniciar lowering partial-aware | sim, como kind `UNKNOWN` mais coverage/gap, se a IR admitir incerteza explícita | enrichment canônico de frontend; depois conversões/effects |
| `CONDITION_SEMANTICS_NOT_AVAILABLE` | sim, antes de promover lowering semântico de IF | sim, somente como condition parcial/opaque; branches continuam materiais | `ConditionSemantics`; `ConditionValidation` quando validade type-sensitive for necessária |
| `CONDITION_REFERENCE_KIND_NOT_PROJECTED` | sim para a referência/predicate afetada | pode atravessar como condition incompleta, nunca como ausência de read | enrichment de capability e produtos de condição; não bloqueia a estrutura já provada |
| `CONTAINMENT_NOT_PROJECTED` | sim para lowering estrutural exato do fact afetado | apenas como containment desconhecido e claim rebaixada | enrichment do Semantic Product antes do slice correspondente de CFG |
| `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN` | não | sim; é parte correta do `CALL` lowering-ready | Reaching Definitions → Possible Values → dynamic CALL resolution |
| gaps de `ObservedStatement` | sim antes do lowering semântico daquela família | o inventário e o blocker atravessam; a semântica da família não | `BACKLOG-SP-001` a `BACKLOG-SP-004` e slices adicionais por capability |
| storage/layout/alias uncertainty | não para lowering nominal/estrutural | sim como ausência explícita de storage identity | Statement Effects / Storage Semantics; depois Reaching Definitions e Possible Values |
| inventory/input incompleto | sim para qualquer claim agregada suficiente | somente como publication bloqueada/partial, nunca como zero comprovado | corrigir input/preprocessing na primeira camada quebrada |

### Literal kind

O primeiro lowerer pode transportar o valor do literal, `LiteralKind.UNKNOWN`,
provenance, coverage e gap sem interpretar texto. Para isso, o contrato mínimo
da IR precisa preservar uma fonte literal de kind desconhecido e não escolher
conversão/tipo por `rawLexeme`, `PICTURE` ou spelling.

Tipagem canônica de literal é prerequisite de frontend antes de elevar MOVE a
`READY` ou implementar effects/conversões que dependam dela. Esse enrichment
deve ser promovido em work item próprio quando necessário; não é condição para
iniciar a infraestrutura de lowering partial-aware.

### Condition semantics

O primeiro lowerer pode materializar somente a estrutura condicional:
statement/origin, condition surface parcial, referências conhecidas, branches,
nesting, termination, continuation e incerteza explícita. Ele não pode criar
um predicate normalizado. `ConditionSemantics`, seguido de
`ConditionValidation` quando houver pergunta type-sensitive, é prerequisite
antes de declarar IF semanticamente lowering-ready.

Essa lacuna não invalida a `CFG readiness` estrutural do IF: duas alternatives
e a continuation são reconstruíveis sem decidir truth value nem interpretar o
predicate.

### Statements observados

`PERFORM`, `EVALUATE`, `GO TO`, `SEARCH`, statements terminais,
`DISPLAY`/`GOBACK` genéricos e demais families fora da capability continuam
facts positivos. O contrato garante inventário, identity, anchor, provenance,
coverage e o motivo de incompletude que estiver disponível. Ele não garante
targets, controls, operands, terminal behavior, successors ou effects da
família. Nenhum consumer pode tratar esses facts como no-op ou fallthrough.

## Evolução aditiva

Não foi encontrado blocker de extensibilidade do envelope fundamental:

| Ponto | Efeito ao adicionar `PerformFact`, `EvaluateFact`, `GoToFact` etc. |
| --- | --- |
| `State` | a lista `statements` e os demais campos permanecem; não surge singleton por construct |
| família sealed | acrescenta-se uma variante explícita ao conjunto permitido; a mudança fica localizada e força handling compilável |
| port | `statements()`, `statement()`, roots e `children()` continuam válidos; uma view tipada adicional é conveniência, não remodelagem obrigatória |
| índices materializados | a identidade/estrutura comuns continuam indexadas; índice especializado, se necessário, é aditivo e local |
| consumer CP6 | adiciona handling explícito da nova variante; não muda a origem dos fatos |
| JSON CP7 | adiciona discriminador/DTO versionado e handling explícito; o envelope determinístico permanece |

Consumers/adapters precisarem reconhecer uma nova variante é comportamento
intencional. Redesenhar `State`, trocar o port por bags dinâmicos ou quebrar o
envelope para cada construct não é necessário.

## Constraints para Analysis IR

O CP8 não define classes, opcodes, forma de CFG, SSA ou schema da IR. Ele deriva
somente estes requisitos para `BACKLOG-IR-001`:

- todo statement traduzido precisa conservar identity/origin, program point
  estrutural, provenance, coverage e unknowns relevantes;
- DATA precisa manter identidade nominal sem ser promovida a storage region;
- MOVE precisa representar literal source, inclusive kind `UNKNOWN`, target
  DATA nominal e role de escrita;
- CALL precisa representar syntax/operand nominal e runtime target desconhecido
  sem convertê-lo em target vazio ou programa escolhido;
- IF precisa representar branch structure, nesting, condition surface parcial,
  continuation e predicate desconhecido sem fabricar expressão normalizada;
- statement bloqueado/observado precisa continuar positivo e conservador;
- o contrato precisa distinguir lowering readiness, CFG readiness e
  effects/dataflow readiness, sem derivar uma dimensão da outra.

## Dependências downstream

A ordem canônica permanece: contrato mínimo de `BACKLOG-IR-001`, primeiro
`BACKLOG-LOWER-001` boundary-only, `BACKLOG-CFG-001`, `BACKLOG-DF-001`
(Statement Effects / Storage Semantics), `BACKLOG-DF-004` (Reaching
Definitions), `BACKLOG-DF-003` (Possible Values), `BACKLOG-DF-002` (dynamic
CALL resolution) e `BACKLOG-DEPS-001`. IR e lowerer podem compartilhar um work
item, desde que o contrato consumido seja fechado antes da implementação que o
produz. O [backlog](../work/backlog.md) mantém a ordem, os critérios de promoção
e os enrichments `BACKLOG-SP-001` a `BACKLOG-SP-004` para EVALUATE, PERFORM,
GO TO/terminal, ALTER e SEARCH.

Não existe blocker oculto para começar o primeiro vertical slice conservador.
Existem blockers explícitos para anunciar MOVE, predicate de IF,
`ObservedStatement`, storage/effects, dataflow, target dinâmico ou a linguagem
inteira como completos.

## Evals relacionados

- `EVAL-SP-001`: contrato materializado, cobertura plural e consumer CP6;
- `EVAL-SP-002`: probe independente, falsificações e gate arquitetural;
- `EVAL-SP-003`: transporte JSON determinístico e sem recomputação;
- `EVAL-ARCH-001`: direção de dependências;
- `EVAL-RES-CALL-002`: binding nominal não resolve valor de CALL dinâmico.
