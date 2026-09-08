# COBOL Semantic Product

O COBOL Semantic Product é a boundary COBOL-specific, materializada e imutável
entre o frontend e o futuro repositório externo `cobol-lower`. Cada publicação
pertence a uma `ProgramUnit`, expõe somente o `CobolSemanticPort`, possui
transporte `cobol-semantic-product.json` e permanece fechada depois da projeção.
O runner mantém `semantic-product.json` como alias compatível com bytes idênticos;
ele publica a unit primária selecionada, sem prometer inventário JSON multi-unit.
Essa é a fronteira pública deste repositório; o contrato atual é regido pela
ADR-0013 e pelos invariantes `INV-SP-001` a `INV-SP-008`.

O [audit bilateral contra Analysis IR 2.0.0](../architecture/semantic-product-air-v2-audit.md)
qualifica as claims abaixo: estados publicados pelo código atual não são
certificação AIR. O port sustenta representação nominal/inventário conservadora;
avaliação, interação e storage precisos têm prerequisites
explícitos. Divergências de readiness permanecem documentadas até remediação
autorizada. O slice de entry primária/GOBACK descrito abaixo fecha somente o
prerequisite local de entrada e saída; não certifica um perfil AIR.

## Profile escalar 4A

O [contrato elementar textual](scalar-text-move.md) define E1–E4 em 1.2.0: DATA
ScalarText, literal TextValue, WholeItemAccess, FULL_IDENTITY e normalContinuation.
As limitações gerais/storage/CALL/IF deste documento continuam vigentes fora
desse profile positivo; as matrizes históricas do audit não ampliam suas claims.

## Superfície atual

O projector publica:

- todas as declarações DATA selecionadas da unit e o fechamento de declarations
  necessário aos candidates nominais;
- todas as ocorrências suportadas de `MOVE` literal para DATA;
- todas as ocorrências suportadas de `CALL` por identifier/expression;
- todos os `IF` como facts estruturais com condition surface, membership
  `THEN`/`ELSE` e continuation quando conhecida;
- a entry primária com disponibilidade de início executável e assinatura;
- todo GOBACK tipado como `GobackFact`, com saída da invocação corrente e
  `LocalContinuation.NONE`, inclusive quando há statements físicos posteriores;
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
MOVE ALPHANUMERIC("A") → DATA WS-X

IF RELATION; READ DATA FLAG; predicate NOT_PUBLISHED
├── THEN
│   └── MOVE ALPHANUMERIC("B") → DATA WS-X
└── ELSE
    └── MOVE ALPHANUMERIC("C") → DATA WS-X

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
| DATA | nome canônico e `PICTURE` opcional explícito | `DataItemId(unit, localId)` | `NOT_APPLICABLE` | hierarquia/storage gerais não publicados; scalarText é prova local opcional | `NOT_APPLICABLE` | `NOT_APPLICABLE` | `NOT_APPLICABLE` | identidade de declaration, sem lookup downstream | layout/alias permanecem desconhecidos | declaration completa, inclusive include chain/exatidão | por declaration; fixture atual `MODELED` |
| MOVE literal → DATA | valor normalizado e `LiteralKind`; kind `ALPHANUMERIC` somente para literal básico provado | statement, literal operand e target operand distintos | disponível, estrutural | disponível quando o parent é suportado; caso contrário `CONTAINMENT_NOT_PROJECTED` | `normalContinuation` explícita com disponibilidade; ordem estrutural não é successor | literal source + DATA target | target `WRITE` | status, reason, candidates e `selected` quando resolved | cópia/acesso provados somente no profile FULL_IDENTITY; restante aberto | statement, source e target | `PARTIAL` enquanto kind for desconhecido |
| CALL identifier/expression | syntax discriminada; arguments/returning não publicados; exception flow rebaixa CFG mas não lowering no código | statement e target operand distintos | disponível, estrutural | disponível ou gap localizado | sequência estrutural disponível no profile sem exception flow | DATA operand | `CALL_TARGET` | variável nominal resolved no profile ready | target de programa é sempre `UNKNOWN` com gap próprio | statement e operand | `MODELED` no profile ready; runtime unknown não é omissão |
| IF/ELSE estrutural | condition `shape` e referências DATA conhecidas; predicate não publicado | statement e condition operands distintos | disponível, estrutural | membership ordenado `THEN`/`ELSE`, inclusive nesting | `IfFact.continuation` quando existe; ausência pode marcar fim estrutural, não prova saída executável | referências DATA conhecidas da condition | `READ` | preserva status/reason/candidates/selected por referência | truth value, operator/object normalizados e branch tomada não publicados | statement, condition e referências | `PARTIAL` por ausência de predicate semantics |
| Entry primária | disponibilidade de start e assinatura escrita | `EntryId(unit, localId)` | referência explícita ao statement quando conhecida | unit proprietária | não publica sequência | count conhecido ou indisponível | `PRIMARY` | assinatura completa não projetada | contexto runtime não inferido | PROCEDURE DIVISION ou unit quando ausente | entry individual; inventário alternativo aberto |
| GOBACK | variante tipada e saída da invocação corrente | `StatementId(unit, localId)` | disponível, estrutural | disponível ou gap localizado | `NONE`, sem successor local | valores de retorno não publicados | terminal COBOL GOBACK | não aplicável à saída local | destino externo e lifecycle não modelados | statement completo | `MODELED` para saída local com containment conhecido |
| `ObservedStatement` | kind/shape/gap genéricos; inclui PERFORM, EVALUATE, GO TO, SEARCH, DISPLAY e demais terminais/shapes | statement identity positiva | disponível | posição conhecida quando o parent é suportado; senão gap | nenhuma continuation semântica própria é inferida | não publicados para a família | não publicados | não publicado como semântica da família | qualquer efeito/transferência permanece desconhecido | statement | `PARTIAL`, `UNSUPPORTED` ou `INPUT_MISSING`, nunca ausência |

## Estados de readiness publicados pelo código

As três dimensões não são intercambiáveis.

| Família | Lowering readiness | CFG readiness | Effects/dataflow readiness | Conclusão |
| --- | --- | --- | --- | --- |
| DATA | `READY` | `NOT_APPLICABLE` | `PARTIAL` | declaração nominal disponível; scalarText prova profile local quando presente; layout geral aberto |
| MOVE literal → DATA | `READY` somente para FULL_IDENTITY; senão `PARTIAL`/`BLOCKED` | `READY` somente para continuação canônica conhecida; senão `PARTIAL` | `PARTIAL` | profile escalar provado em 1.2.0; demais casos preservam gaps |
| CALL identifier/expression | `READY` no profile atual | `READY` sem exception flow não projetado | `PARTIAL` | esses estados não provam `invoke` AIR V2: faltam interpretação do target, contrato de avaliação/outcomes e endereçamento completo |
| IF/ELSE estrutural | `PARTIAL` | `READY` quando branches/continuation/containment são exatos | `PARTIAL` | membership disponível não certifica branch AIR; avaliação/predicate e controle executável têm lacunas |
| `ObservedStatement` | `BLOCKED` | `BLOCKED` | `BLOCKED` | inventário está disponível; lowering semântico da família não está |
| Entry primária/start | `READY` com start e assinatura sem cláusulas conhecidos; senão rebaixado | `READY` somente para start conhecido | `BLOCKED` | inventário de entradas alternativas permanece `PARTIAL` |
| GOBACK | `READY` para saída da invocação corrente | `READY` para ausência de successor local com containment conhecido | `BLOCKED` | nenhum outro terminal, runtime/lifecycle ou perfil AIR é promovido |
| Publicação completa do fixture | `BLOCKED` | `BLOCKED` | `BLOCKED` | o agregado é limitado pelo `ObservedStatement`; isso não rebaixa facts independentes |

O CP8 provou reconstrução desses facts sem frontend. Não validou operações,
TypeRef, entradas, envelopes ou perfis AIR. Contra V2, DATA nominal permanece
representável com tipo/storage abertos; CALL precisa de fallback opaco e suas
claims `READY` não certificam a interação. O audit também reproduziu perda de
SUBSCRIPT em CALL/MOVE suportados. A matriz bilateral e os findings F-AIR-02/03
registram a divergência de suficiência, sem promover ou mudar estados em código.

## Destino dos gaps e unknowns

| Gap / incerteza | Antes de `CobolLower`? | Pode atravessar lowering? | Dependência posterior |
| --- | --- | --- | --- |
| `LITERAL_KIND_NOT_PUBLISHED` | antes de literal/assign precisos; não bloqueia inventário/controle conservador | AIR V2 exige expressão abstrata em `opaque`, não literal com `unknown_type` | frontend publica domínio/conversão e condições de escrita; kind sozinho não basta |
| `CONDITION_SEMANTICS_NOT_AVAILABLE` | sim, antes de promover lowering semântico de IF | sim, somente como condition parcial/opaque; branches continuam materiais | `ConditionSemantics`; `ConditionValidation` quando validade type-sensitive for necessária |
| `CONDITION_REFERENCE_KIND_NOT_PROJECTED` | sim para a referência/predicate afetada | pode atravessar como condition incompleta, nunca como ausência de read | enrichment de capability e produtos de condição; não bloqueia a estrutura já provada |
| `CONTAINMENT_NOT_PROJECTED` | sim para lowering estrutural exato do fact afetado | apenas como containment desconhecido e claim rebaixada | enrichment do Semantic Product antes do slice correspondente de CFG |
| `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN` | não para fallback; não substitui contrato de target/outcomes | sim, em interação conservadora; não prova precondições de `invoke` | Reaching Definitions → Possible Values → dynamic CALL resolution |
| gaps de `ObservedStatement` | antes de precisão da família | sim, `opaque` com envelopes máximos de memória/controle/recursos é válido | `BACKLOG-SP-001` a `BACKLOG-SP-004` e slices adicionais por capability |
| storage/layout/alias uncertainty | não para lowering nominal/estrutural; sim para precisão física | sim como associação AIR aberta e incertezas explícitas | fatos declarativos no frontend; depois normalização AIR, Effects / Storage Semantics, RD e PV |
| inventory/input incompleto | sim para claim de inventário completo | publicação AIR pode ser válida com coverage parcial e fronteiras abertas; nunca zero comprovado | corrigir input/preprocessing na primeira camada quebrada |

### Literal kind

AIR 2.0.0 exige domínio conhecido para literal. Com `LiteralKind.UNKNOWN`, o
lowerer conserva a occurrence, value como evidência de origem, provenance e
gap; a representação executável usa `unknown(unknown_type(u),...)` em `opaque`,
com razões distintas de tipo e valor. Não escolhe tipo/conversão por
`rawLexeme`, `PICTURE` ou spelling. O consumer AIR não interpreta esse metadado
como literal tipado.

MOVE preciso exige valor/domínio, conversão aplicável, compatibilidade
`sameDomain` e condições de escrita. Binding nominal ou MOVE escrito não
provam domínio comum, cópia sem conversão ou storage. O enrichment é próprio
do frontend; unknown_type não bloqueia por si só uma cópia comprovada, mas o
profile 4A publica essa prova em tipos próprios. Fora dele, a semântica de
cópia e storage continua indisponível; a categoria conhecida sozinha não basta.

### Condition semantics

O primeiro lowerer pode materializar somente a estrutura condicional:
statement/origin, condition surface parcial, referências conhecidas, branches,
nesting, termination, continuation e incerteza explícita. Ele não pode criar
um predicate normalizado. `ConditionSemantics`, seguido de
`ConditionValidation` quando houver pergunta type-sensitive, é prerequisite
antes de declarar IF semanticamente lowering-ready.

As relações de branch/continuation continuam reconstruíveis. Para `branch`
AIR, porém, a abstração precisa de resultado `known(bool)` puro e total; o
port não publica essa garantia para toda ConditionSurface. Sem ela, usar
`opaque` com reads disponíveis e avaliação/controle abertos. O enrichment
mínimo de avaliação pode preceder ConditionSemantics completa. Ordem de
children e ausência de continuation não autorizam execution order ou retorno.

### Statements observados

`PERFORM`, `EVALUATE`, `GO TO`, `SEARCH`, os demais statements terminais,
`DISPLAY` e demais families fora da capability continuam
facts positivos. O contrato garante inventário, identity, anchor, provenance,
coverage e o motivo de incompletude que estiver disponível. Ele não garante
targets, controls, operands, terminal behavior, successors ou effects da
família. Nenhum consumer pode tratar esses facts como no-op ou fallthrough.

AIR V2 permite `opaque` com memória máxima, `any_control` e `any_resource`.
Isso é tradução conservadora válida, embora o código atual mantenha BLOCKED
para a semântica da família. STOP RUN, EXIT PROGRAM e CONTINUE continuam
observados; somente GOBACK possui a capacidade terminal precisa deste slice.

## Entradas, acesso e provenance

O port publica `EntryInventory` com scope de capacidade `PRIMARY_ONLY` no
JSON e status `PARTIAL` (ou `INPUT_MISSING`). `ALTERNATE_ENTRIES_NOT_PROJECTED`
mantém o inventário de entradas aberto, inclusive quando nenhum ENTRY foi
observado. ENTRY alternativo conserva seu `ObservedStatement`; não se deduz
que a entrada primária seja a única. Procedure regions e sequência semântica
universal permanecem indisponíveis. ROOT continua containment, não entrada.

### Entry primária e GOBACK

`EntryFact` possui `EntryId(unit, localId)`, role `PRIMARY`, disponibilidade,
`ExecutableStart`, `EntrySignature`, provenance, coverage, readiness e gaps.
O namespace é o da publicação, também para seu target. Quando conhecido,
`start.statement` deve existir no inventário de statements da mesma unit.
O core rejeita target ausente/cruzado e identidades duplicadas antes do port.

O frontend materializa `Ast.Division.procedureEntry`, metadata não-node com
referência ao statement do corpo não declarativo. AstBuilder seleciona essa
relação pelos contextos tipados de sentences/paragraphs/sections, sem consultar
IDs ou roots projetados; o projector traduz a referência canônica por identidade.
Primeiro ENTRY, corpo vazio, altered GO TO sem nó, metadata ausente ou input
incompleto não autorizam fabricar start em um statement posterior. Declaratives
ainda não materializados mantêm gap `DECLARATIVES_NOT_PROJECTED`, start
indisponível e inventário de statements `PARTIAL`; não há nova modelagem dessas
regiões neste slice.

Assinatura descreve somente a interface escrita da PROCEDURE DIVISION:
`availability=KNOWN`, `parameterCount=0`, `returningClause=ABSENT` são publicados
quando o builder estabeleceu ausência de cláusulas. Isso não afirma ausência
de RETURN-CODE, effects ou valores de runtime. Havendo cláusulas, o count e a
presença de RETURNING/GIVING conhecidos são conservados, mas a assinatura é
`PARTIAL` com `ENTRY_SIGNATURE_NOT_PROJECTED`: posições/modos/tipos/bindings
não foram projetados. `UNAVAILABLE`/`INPUT_MISSING` exigem count ausente
(`null` no JSON), returning `UNKNOWN` e gap. Nenhuma lista vazia substitui
desconhecimento. A entry rebaixa lowering readiness se assinatura/start forem
incompletos; o CFG da entry afirma somente o início quando conhecido.

`Ast.GobackStatement` é a autoridade tipada para `GobackFact(StatementHeader)`.
O fact fixa `exit=CURRENT_PROGRAM_INVOCATION` e `localContinuation=NONE`:
conclui a invocação da ProgramUnit proprietária, independentemente de qual
entry a ativou, e não retoma statement local. Não há campo que aceite um next
espúrio. Com CALL ativo, a conclusão devolve controle ao chamador; a conclusão
da invocação primária de runtime é entregue ao ambiente. Unit top-level não
prova main program em runtime; o caminho estrutural da unit conserva nesting
sem inventar contexto de chamada. O slice não modela cleanup/INITIAL, valores
de retorno, thread/enclave ou efeitos de término do run unit.

No profile de input e containment conhecidos, GOBACK publica coverage `MODELED`,
lowering `SUFFICIENT` para saída da invocação e CFG `SUFFICIENT` para ausência
de successor local. Effects/dataflow é `BLOCKED`; storage e certificação AIR
não são claims. Containment desconhecido preserva gap e rebaixa CFG. A summary
`coverage` continua agregando statements e o relatório; o inventário e os gaps
de entries são uma superfície separada e devem ser consultados para entrada.
`GOBACK; CONTINUE` mantém ambos os facts; a ordem não cria fallthrough, e nenhum
deles é removido por suposta reachability. EXIT PROGRAM, STOP RUN, STOP literal
e EXEC CICS RETURN conservam suas capacidades anteriores.

Esses fatos derivam de IBM Enterprise COBOL for z/OS 6.4,
[Language Reference, GOBACK, p. 346](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf),
[procedures e início não declarativo](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=structure-procedures),
[transferência de controle](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=structure-transfer-control)
e [interface da PROCEDURE DIVISION](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=program-processing-data).
O início é uma relação semântica da entrada primária, não sequenciamento universal.
O algoritmo termina por percursos finitos de contextos, com tempo/espaço O(N)
e tradução por joins indexados, sem enumeração de caminhos ou successors.

DataReference conserva binding nominal, mas não subscripts/reference
modification presentes na AST. A perda foi reproduzida em CALL/MOVE com IX;
precisão de acesso exige enrichment de shape/occurrences/roles/limites e
readiness adequada, sem fazer o lowerer reabrir AST. Declarações ancestrais
fecham sobre IDs locais da publicação; isso não prova alias entre ports.

Provenance usa linhas base 1, colunas base 0 em code points Unicode e fim
inclusivo, conforme SourceMap/UnicodeText. B deve declarar essas convenções
na origem AIR, conservar include chain/exatidão e usar DERIVED/UNAVAILABLE
onde pertinente. Gap não ter ID próprio não impede criar UncertaintyId AIR;
gaps de DATA/unit ausentes limitam a granularidade da explicação.

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

O JSON publica `schema=cobol-semantic-product`, `contractVersion=1.2.0`.
A evolução 1.2.0 acrescenta os fatos tipados do [profile escalar MOVE](scalar-text-move.md)
com compatibilidade/limites explícitos. A evolução anterior 1.1.0 acrescentou `entryInventory`
(entries/start/signature/gaps/provenance/readiness) e variante `GOBACK`, mantendo
o significado dos campos existentes. Consumers que fechavam o conjunto de
variantes em 1.0.0 precisam reconhecer 1.1.0 ou rejeitá-lo explicitamente;
não podem ignorar a nova variante. Não há codec AIR nem dependência air-java.
Handles `entry:n` e `statement:n` são locais ao objeto `unit` do documento;
a identidade global é o par (unit, handle), não a string isolada.

## Constraints para Analysis IR

O CP8 não define classes, opcodes, forma de CFG, SSA ou schema da IR. Ele deriva
somente estes requisitos para `BACKLOG-IR-001`:

- todo statement traduzido precisa conservar identity/origin, program point
  estrutural, provenance, coverage e unknowns relevantes;
- DATA precisa manter identidade nominal sem ser promovida a storage region;
- MOVE precisa conservar source e target nominais; kind `UNKNOWN` exige
  abstração executável compatível com V2, não literal AIR de tipo desconhecido;
- CALL precisa representar syntax/operand nominal e runtime target desconhecido
  sem convertê-lo em target vazio ou programa escolhido;
- IF precisa representar branch structure, nesting, condition surface parcial,
  continuation e predicate desconhecido sem fabricar expressão normalizada;
- statement bloqueado/observado precisa continuar positivo e conservador;
- o contrato precisa distinguir lowering readiness, CFG readiness e
  effects/dataflow readiness, sem derivar uma dimensão da outra.

## Dependências downstream

O contrato externo adotado como alvo é AIR 2.0.0; sua semântica não é redesenhada
para acomodar o port. Antes de um CFG fechado: contrato/validator AIR, facts
de entrada/terminal e sequenciamento do slice, lowerer externo somente pelo
transporte JSON dos facts do port e consumer estrutural. O primeiro fixture
recomendado é uma unit com GOBACK; MOVE/IF/CALL crescem depois, com seus
prerequisites e fallback conservador.

A ownership física é cross-repo: `air-java` já possui o modelo/validator AIR;
`cobol-lower` consumirá o JSON deste produto e publicará AIR; `analysis-cfg`
consumirá essa Publication e construirá CFG. Somente os facts COBOL e seu JSON
pertencem a este repositório.

Antes de fluxo escalar preciso, o frontend publica fatos declarativos de
tipo/conversão/associação de storage. O lowerer traduz esses fatos; consumers
derivam Storage Semantics/Effects → RD → PV → targets dinâmicos. Consumers
não voltam a DataEntry para obter layout ausente. CALL literal é enrichment
prioritário de observação nominal que pode preceder dataflow.

A [ordem detalhada e o contrato do primeiro slice](../architecture/semantic-product-air-v2-audit.md)
e o [backlog](../work/backlog.md) separam A/frontend, B/lowering e C/consumers.
Readiness de uma dimensão não certifica outra nem conformidade com todos os
oracles de um perfil AIR @2.

## Regras COBOL relevantes ao handoff V2

A autoridade consultada é Enterprise COBOL for z/OS 6.4, sob as opções
efetivamente configuradas; Policy UNSPECIFIED não seleciona opções implícitas.
Estas regras delimitam as capabilities descritas acima e os enrichments futuros:

- [MOVE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=items-assigning-values-elementary-data-move)
  inclui conversão, padding e truncamento conforme os operandos. O [profile 4A](scalar-text-move.md) publica a cópia textual de extensão igual; LiteralKind conhecido sozinho
  não prova cópia de valor nem valida assign AIR.
- [Conclusão de programas](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=subprograms-ending-reentering-main-programs)
  depende do contexto de entrada; [STOP](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-stop-statement)
  distingue término de suspensão. GOBACK/EXIT PROGRAM/STOP não podem ganhar
  uma regra de saída única. O produto publica a saída local de GOBACK no
  escopo delimitado acima; os demais terminais continuam observados.
- [PERFORM básico](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=statement-basic-perform)
  possui disciplina de ranges, admite saída comum e distingue conclusão de
  PERFORM de passagem ordinária. O futuro fact procedure/THRU deve preservar
  essas distinções antes de alegar equivalência a control.local@1; PERFORM
  inline não exige essa extensão apenas por compartilhar a keyword.

O [audit](../architecture/semantic-product-air-v2-audit.md) detalha as
correspondências e contracasos de GO TO/DEPENDING ON/ALTER. Targets, seleção,
estado de controle e opções pertinentes continuam facts de frontend antes
da tradução AIR; nenhum consumer infere essas regras de texto ou shape.

## Evals relacionados

- `EVAL-SP-001`: contrato materializado, cobertura plural e consumer CP6;
- `EVAL-SP-002`: probe independente, falsificações e gate arquitetural;
- `EVAL-SP-003`: transporte JSON determinístico e sem recomputação;
- `EVAL-SP-004`: entry primária/start/GOBACK, assinatura explícita e adversariais;
- `EVAL-ARCH-001`: direção de dependências;
- `EVAL-RES-CALL-002`: binding nominal não resolve valor de CALL dinâmico.
