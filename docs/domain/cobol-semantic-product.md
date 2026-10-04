# COBOL Semantic Product

Produção corrente: [SP 2.44.0 — terminal SEND tipado](terminal-send-r7-r7b.md), herdando [SP 2.43.0 — comandos CICS delimitados](cics-command-facts.md), herdando [SP 2.42.0 — eventos explícitos CICS ABEND](cics-abend-events.md), preservando SP 2.41.0/CICS_HANDLER e SP 2.40.0/FactDependencies. As versões por feature e seções históricas abaixo não autorizam downgrade dessa publicação.

Current compositional control: [W7 contract](control-composition.md). SP2.38 independent MOVE receivers are specified below. Historical profile qualification below does not gate independent branch entries or predicate coverage.

Writer corrente: [controle condicional FILE / SP 2.25.0](file-dependencies.md), storage 1.8.0
([source evidence](evidence-preserving-entry.md)),
com prova tipada por condição de entrada, preservando [CICS Program Control](cics-program-control.md).

Contrato corrente: [Storage Semantics e relações físicas / SP 2.8.0](storage-semantics.md), preservando os fatos regionais SP 2.7 e [GO TO DEPENDING ON / SP 2.6.0](goto-depending.md), preservando [PERFORM family / SP 2.5.0](perform-family.md), preservando [GO TO first slice / SP 2.1.0](goto-semantic-product.md), preservando [EVALUATE / SP 2.0.0](evaluate-semantic-product.md), preservando [entry e input localizado / SP 1.9.0](../architecture/entry-localized-input.md), preservando [composição e parcialidade / SP 1.8.0](../architecture/compositional-partial-lowering.md), preservando [IF W2A](if-semantic-product.md), preservando [CALL e fitting W1A](call-semantic-product.md).

O COBOL Semantic Product é a boundary COBOL-specific, materializada e imutável
entre o frontend e o futuro repositório externo `cobol-lower`. Cada publicação
pertence a uma `ProgramUnit`, expõe somente o `CobolSemanticPort`, possui
transporte `cobol-semantic-product.json` e permanece fechada depois da projeção.
O runner mantém `semantic-product.json` como alias compatível com bytes idênticos;
ele publica a unit primária selecionada, sem prometer inventário JSON multi-unit.
Essa é a fronteira pública deste repositório; o contrato atual é regido pela
ADR-0013 e pelos invariantes `INV-SP-001` a `INV-SP-010`.

O caminho de produção `SemanticProductJsonWriter.write(port, path)` serializa
diretamente para um OutputStream fechado pelo adapter. Ele mantém os DTOs de
transporte, mas não materializa o JSON integral em byte[] ou String. `serialize`
permanece disponível para consumers que precisam de bytes, testes e goldens
pequenos; ambos os caminhos têm o mesmo output determinístico. Os probes de
escala exercitam a escrita em arquivo. Isso não constitui um writer incremental
de facts nem remove a árvore DTO ou a verbosidade preexistente do contrato.

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

## Profile IF W2A

O [contrato IF 1.4.0](if-semantic-product.md) acrescenta predicate BOOLEAN/PURE/TOTAL,
truth UNKNOWN, reads completos com acesso inteiro no profile admitido, presença e
conteúdo dos braços, completion executável de IF/MOVE interno e prova separada de
independência de storage. As limitações gerais e as observações do audit abaixo
continuam válidas fora desse profile. Não se publica um predicate normalizado.

## Superfície atual

O projector publica:

- todas as declarações DATA selecionadas da unit e o fechamento de declarations
  necessário aos candidates nominais;
- todas as ocorrências suportadas de `MOVE` literal ou DATA para DATA;
- todas as ocorrências suportadas de `CALL` literal ou por identifier/expression;
- todos os `IF` com condition surface, membership `THEN`/`ELSE`,
  presença/conteúdo/entry dos arms e continuation; o profile W2A acrescenta
  garantias explícitas de avaliação/completion sem calcular truth;
- a entry primária com disponibilidade de início executável e assinatura;
- todo GOBACK tipado como `GobackFact`, com saída da invocação corrente e
  `LocalContinuation.NONE`, inclusive quando há statements físicos posteriores;
- todo statement tipado restante como `ObservedStatement`, sem transformá-lo
  em ausência ou em semântica inventada.

O port também publica policy efetiva, identities namespaced, program points
estruturais, provenance, gaps localizados e coverage reconciliada. Ele não
publica AST, symbol table, occurrences, resolution, report, `SourceMap`, texto
fonte, presentation, IR, CFG ou resultados de dataflow. SP 2.7 acrescenta layout físico e efeitos
fonte de MOVE canônicos sob perfil explícito, descritos no contrato de storage;
isso não publica regiões AIR nem efeitos de runtime inferidos pelo projector.

`ProgramPoint` e a ordem das coleções são estruturais e determinísticos. Eles
não afirmam execution order, reachability nem CFG edges. `DataItemId` é
identidade nominal de declaração, não identidade de storage.

## NEXT SENTENCE observado

Quando recebe AST e coverage íntegros, o projector preserva NextSentence como
`ObservedStatement` com `observedKind=NEXT_SENTENCE` e
`observedShape=TYPED_NEXT_SENTENCE`, identidade/provenance, gap e readiness de
lowering/CFG/effects-dataflow bloqueada. Não publica CONTINUE, no-op ou
fallthrough para essa variante. A regra COBOL e a representação de origem
estão na [AST semântica](semantic-ast.md#next-sentence).

O contrato 1.4.0 não publica sentence boundaries ou o target after-period.
Procedure containers são achatados no inventário; em SEARCH, o child possui
parent SEARCH com branch UNKNOWN, sem identidade pública de SearchWhen.
Logo, identidade tipada é necessária, mas insuficiente para lowering preciso
de NEXT SENTENCE. Uma futura capability precisa de fatos canônicos adicionais
no frontend/SP antes de ser traduzida por cobol-lower; consumers não podem
reabrir fonte/AST nem inferir o destino por ProgramPoint, provenance ou
IfFact.continuation. Coverage ausente continua sendo corrupção rejeitada pelo
projector, não um substituto para a incompletude explícita desse controle.

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
| MOVE literal → DATA | valor normalizado e `LiteralKind`; kind `ALPHANUMERIC` somente para literal básico provado | statement, literal operand e target operand distintos | disponível, estrutural | disponível quando o parent é suportado; caso contrário `CONTAINMENT_NOT_PROJECTED` | `normalContinuation` explícita com disponibilidade; ordem estrutural não é successor | literal source + DATA target | target `WRITE` | status, reason, candidates e `selected` quando resolved | cópia/acesso provados no profile FULL_IDENTITY/FITTED_TEXT; restante aberto | statement, source e target | `PARTIAL` enquanto kind for desconhecido |
| CALL literal/data | soma tipada, superfície explícita de cláusulas | statement/target distintos | estrutural | disponível ou gap | normalContinuation condicional explícita | literal lógico ou DATA | CALL_TARGET para DATA | status/reason/candidates/selected conservados | runtime UNKNOWN, effects UNKNOWN, outcomes OPEN | statement/target separados | MODELED somente com prova da primeira slice |
| IF/ELSE | condition surface e referências DATA; predicate guarantee/arms explícitos no profile W2A | statement e condition operands distintos | disponível, estrutural | membership ordenado `THEN`/`ELSE`, inclusive nesting | `continuation` estrutural e `normalContinuation` executável com disponibilidade separada; ausência não prova saída | referências DATA conhecidas da condition | `READ` | preserva status/reason/candidates/selected por referência | truth UNKNOWN; operator/object normalizados e branch tomada não publicados | statement, condition e referências | `MODELED` no profile W2A simples; senão `PARTIAL` com gaps |
| Entry primária | disponibilidade de start e assinatura escrita | `EntryId(unit, localId)` | referência explícita ao statement quando conhecida | unit proprietária | não publica sequência | count conhecido ou indisponível | `PRIMARY` | assinatura completa não projetada | contexto runtime não inferido | PROCEDURE DIVISION ou unit quando ausente | entry individual; inventário alternativo aberto |
| GOBACK | variante tipada e saída da invocação corrente | `StatementId(unit, localId)` | disponível, estrutural | disponível ou gap localizado | `NONE`, sem successor local | valores de retorno não publicados | terminal COBOL GOBACK | não aplicável à saída local | destino externo e lifecycle não modelados | statement completo | `MODELED` para saída local com containment conhecido |
| `ObservedStatement` | kind/shape/gap genéricos; inclui PERFORM, EVALUATE, GO TO, SEARCH, DISPLAY e demais terminais/shapes | statement identity positiva | disponível | posição conhecida quando o parent é suportado; senão gap | nenhuma continuation semântica própria é inferida | não publicados para a família | não publicados | não publicado como semântica da família | qualquer efeito/transferência permanece desconhecido | statement | `PARTIAL`, `UNSUPPORTED` ou `INPUT_MISSING`, nunca ausência |

## Estados de readiness publicados pelo código

As três dimensões não são intercambiáveis.

| Família | Lowering readiness | CFG readiness | Effects/dataflow readiness | Conclusão |
| --- | --- | --- | --- | --- |
| DATA | `READY` | `NOT_APPLICABLE` | `PARTIAL` | declaração nominal disponível; scalarText prova profile local quando presente; layout geral aberto |
| MOVE literal → DATA | `READY` para FULL_IDENTITY ou FITTED_TEXT; senão `PARTIAL`/`BLOCKED` | `READY` somente para continuação canônica conhecida; senão `PARTIAL` | `PARTIAL` | profile escalar 4A e fitting W1A; demais casos preservam gaps |
| CALL literal/data | `READY` sob as provas W1A de target, origem, acesso e cláusulas ausentes | `READY` para successor canônico conhecido | `PARTIAL` | não certifica AIR; interpretação de nome, efeitos/outcomes AIR e lowering pertencem a checkpoints posteriores |
| IF/ELSE estrutural | `READY` no profile W2A; senão `PARTIAL` | `READY` quando branches/continuation/containment são exatos | `PARTIAL` | membership isolado não certifica branch AIR; as provas W2A são explícitas, com gaps fora da slice |
| `ObservedStatement` | `BLOCKED` | `BLOCKED` | `BLOCKED` | inventário está disponível; lowering semântico da família não está |
| Entry primária/start | `READY` com start e assinatura sem cláusulas conhecidos; senão rebaixado | `READY` somente para start conhecido | `BLOCKED` | inventário de entradas alternativas permanece `PARTIAL` |
| GOBACK | `READY` para saída da invocação corrente | `READY` para ausência de successor local com containment conhecido | `BLOCKED` | nenhum outro terminal, runtime/lifecycle ou perfil AIR é promovido |
| Publicação completa do fixture | `BLOCKED` | `BLOCKED` | `BLOCKED` | o agregado é limitado pelo `ObservedStatement`; isso não rebaixa facts independentes |

O CP8 provou reconstrução desses facts sem frontend. Não validou operações,
TypeRef, entradas, envelopes ou perfis AIR. Contra V2, DATA nominal permanece
representável com tipo/storage abertos; CALL precisa de fallback opaco e suas
claims `READY` não certificam a interação. O audit também reproduziu perda de
SUBSCRIPT em CALL/MOVE suportados. A matriz bilateral e os findings F-AIR-02/03
registram a divergência de suficiência no baseline auditado. W1A publica acesso
inteiro somente no subconjunto provado e rebaixa os demais CALLs; não certifica AIR.

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
ENTRYs iniciais são declarações: a relação primária aponta para o primeiro
statement executável seguinte. Corpo vazio, altered GO TO sem nó e metadata
ausente não autorizam fabricar start. Input incompleto exige a prova localizada
canônica descrita em SP 1.9; o inventário continua incompleto. A forma escrita
da assinatura é preservada quando a mesma prova local demonstra que os gaps
pertencem a DATA ou a outra unidade com fronteira comprovada. Declaratives
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
W1A distingue acesso inteiro provado de acesso indisponível, mas não publica
a expressão de acesso parcial. Essa precisão exige enrichment de shape/occurrences/roles/limites e
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

O JSON publica `schema=cobol-semantic-product`, `contractVersion=1.4.0`.
A evolução W2A está no [contrato IF](if-semantic-product.md).
A evolução W1A está no [contrato CALL e fitting](call-semantic-product.md): target
literal/data, superfície/continuação explícitas e resultado ajustado, com consumer
interno atualizado posteriormente em W1C conforme INTERNAL-CONTRACT-DEV-001.
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
- CALL precisa representar target literal/DATA e runtime target desconhecido
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
não voltam a DataEntry para obter layout ausente. CALL literal já é publicado como target tipado por W1A, sem dataflow.

A [ordem detalhada e o contrato do primeiro slice](../architecture/semantic-product-air-v2-audit.md)
e o [backlog](../work/backlog.md) separam A/frontend, B/lowering e C/consumers.
Readiness de uma dimensão não certifica outra nem conformidade com todos os
oracles de um perfil AIR @2.

## Regras COBOL relevantes ao handoff V2

A autoridade consultada é Enterprise COBOL for z/OS 6.4, sob as opções
efetivamente configuradas; Policy UNSPECIFIED não seleciona opções implícitas.
Estas regras delimitam as capabilities descritas acima e os enrichments futuros:

- [MOVE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=items-assigning-values-elementary-data-move)
  inclui conversão, padding e truncamento conforme os operandos. O [profile 4A](scalar-text-move.md) publica a cópia textual de extensão igual; W1A acrescenta right-padding provado. LiteralKind conhecido sozinho
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
- `EVAL-SP-008`: IF W2A, predicate, arms/completion e independência limitada;
- `EVAL-ARCH-001`: direção de dependências;
- `EVAL-RES-CALL-002`: binding nominal não resolve valor de CALL dinâmico.

## CP6 PERFORM BASIC

SP 1.6.0 adds the [isolated paragraph PERFORM profile](perform-basic.md); MOVE data sources introduced in SP 1.5.0 remain unchanged.

## SP1.8 — intrinsic bodies and partial statements

The current wire removes activation-global resume from a paragraph's last MOVE.
BASIC_PROCEDURE_PERFORM carries targetEntry, ordered targetStatements, targetExit
and its own normalContinuation. primaryStatements remains only in legacy SP1.6/1.7
wire contracts; the SP1.8 primary graph is shared through explicit continuations.
ObservedStatement adds normalContinuation and knownReferences. These nominal facts
retain identity/role/binding/provenance, but do not invent exact access/effect proof.
A known observed continuation is the only normal in-unit successor; abnormal exits
and nontermination remain unproved. No observed construction disappears.

### SP2.9 / storage1.2 — fixed RENAMES

Extensão 2.9.0; `storage.version=1.2.0` adiciona `renames` obrigatório
(vazio quando ausente), com `{id,owner,from,through,status,provenance,gapCodes}`.
Endpoints nullable preservam resolução disponível mesmo quando a faixa não é
provada. PROVEN requer from e vista existente coerente com toda a faixa; sem
THROUGH a categoria é herdada. UNPROVEN requer gaps e vista sem precisão.
Nenhum alias aloca uma nova base. Declarações de aliases não usados também são
publicadas. O reader 2.8 é preservado no lower; não existe writer duplo.

### ST-W6.2 / SP2.10

`regionalAccess` exige `view` e `slice` (nullable). Slice provado transporta
`offset` absoluto e `extent` em bytes como decimais canônicos não negativos,
com extensão positiva e range contido no item. Views de declaração permanecem
inteiras; acesso dinâmico ou sem prova não substitui slice por acesso inteiro.

### ST-W6.3 / SP2.11

`MOVE.additionalTransfers` é lista obrigatória, possivelmente vazia, de
`{source,target,effect}` com operandos próprios. A primeira transferência continua
em source/target/regionalMove. Ordem do array é semântica; lower não usa nomes
para ordenar ou descobrir receivers. FIT_TEXT significa ajuste à direita com
SPACE, ao extent explícito do target. FITTED_LITERAL_BYTES conserva o literal
original e publica o vetor após esse ajuste; LITERAL_BYTES continua byte-exato.
Múltiplos receivers com fonte DATA admitem efeitos regionais exatos somente quando
captura do emissor e interferência são provadas; sobreposição de origem permanece
conservadora. SP2.38 acrescenta uma prova lógica separada para literal textual,
que não depende de layout físico e pode coexistir com gaps regionais.

ST-W6.4 consumes the same SP2.11 transfer sequence: canonical fixed textual
CORRESPONDING publishes each implicit pair with declaration identity/origin and
its own regional effect. Both primary and additional sources can differ. The
sequence contains only selected pairs; unmatched bytes are not written. Matching
is owned by StorageCorrespondence, never by a projector or downstream consumer.

### W8 / SP2.38: MOVE por receiver

Quando um MOVE multi-receiver publica ao menos uma transferência lógica provada ou
uma sequência regional parcialmente indisponível, o writer seleciona `2.38.0`.
`source`, `target` e `regionalMove` continuam a primeira transferência;
`additionalTransfers[]` mantém a ordem e cada efeito regional próprio, inclusive
`UNAVAILABLE` com gap localizado. O campo opcional `logicalTransfers[]` contém
`{target,value}` para cada receiver textual inteiro cujo literal de origem e
extent escalar estão provados. `value` é o texto ajustado por padding/truncamento
para aquele receiver; o target referencia um operand WRITE publicado no mesmo MOVE.
Uma transferência lógica não certifica bytes, codec ou acesso regional.

A ausência de `logicalTransfers` mantém os bytes históricos. SP2.37 não aceita a
semântica de sequência regional parcialmente indisponível nem o novo campo.
Receivers com alias explícito mantêm a mesma identidade de storage no downstream;
o source literal é estável durante a sequência. Fonte DATA com overlap requer
prova de captura e não herda esta regra de literal.

### ST-W7.1 / SP2.12, storage1.3

`storage.entryState` é obrigatório: `{mode,conditions}`; mode é INITIAL,
PRESERVED ou UNKNOWN. Conditions contém `{node,kind,bytes,gapCodes,provenance}`.
Node referencia uma declaração física existente, única por condição.
LITERAL_BYTES exige modo INITIAL, origem exata e bytes no extent integral da
vista com codec provado; PRESERVE exige modo PRESERVED; UNKNOWN exige gaps e
nenhum byte. Ausência de condições nunca implica zero. O CLI seleciona o perfil
de invocação com `--entry-storage-state initial|preserved|unknown` (default unknown).
A publicação de VALUE é canônica no frontend; projector não interpreta cláusulas.
Não há MOVE sintético nem condição reaplicada em retorno/backedge.

## SP 2.14: CICS Program Control

The current writer publishes 2.14.0, superseding the Draft 2.13.0 shape. `CICS_PROGRAM_CONTROL` is an additive statement family with LINK/XCTL, PROGRAM target, preserved options, bound host references and independently partial control/effects/signature. Paragraph-local and ordinary continuations are separate required fields. Typed condition profiles must agree with options and gaps. See [CICS](cics-program-control.md). The storage contract remains 1.3.0. Readers must explicitly support this version.

### EXEC DLI observado

No writer 2.15.0, DLI usa a variante existente `OBSERVED`, com
`observedKind=EMBEDDED_LANGUAGE`, `observedShape=OPAQUE_DLI`, `coverage=PARTIAL`
e gap `OBSERVED_STATEMENT_PARTIAL` vinculado à identidade/provenance do statement.
`knownReferences=[]` significa dependências não determinadas por esta capability.
`normalContinuation=UNAVAILABLE` não afirma fallthrough nem retorno de comando IMS.
Readiness de lowering, CFG e efeitos permanece `BLOCKED`; inventário estrutural
completo não promove semântica. O consumer local conserva shape/gap/readiness.
Não há novo schema, enum wire ou facts IMS. Oráculos: `ExecDliOpaqueTest` e
`ExecDliProvenanceTest`; [handoff do fix](../work/exec-dli-fix.md).

## RF-W1 / SP 2.16.0, storage 1.5.0

A enum InitialStorageKind acrescenta POSSIBLE_LITERAL_BYTES; proof acrescenta
DECLARATIVE_POSSIBILITY. A forma possível exige bytes materializados no extent
exato da view, allocation INDEPENDENT_LOCAL_WORKING_STORAGE, provenance exata,
mode UNKNOWN e gap ENTRY_STATE_NOT_PROVEN obrigatório. Outros blockers de
lifecycle continuam publicados. Não é LITERAL_BYTES forte. Nenhuma lista de
bytes é inferida se a própria declaração/codec/layout não tiver prova.

O lower transporta a distinção por entry.possibilities@1; não há MOVE sintético,
reseed em backedge ou reinterpretação do perfil PRESERVED. Contrato completo e
provas em [DVI](declarative-value-inference.md).

## Dependency preservation — SP 2.32.0

[Dependency preservation](dependency-preservation.md) defines occurrence-local
`POSSIBLE_TEXT`, its validation and canonical whole CICS host references independent
of physical layout. The writer emits 2.32.0 only when this new evidence is present.

## Positive topology W2 — SP 2.33.0

An EVALUATE arm whose condition is not interpreted preserves its ordinal, body,
known read references and condition provenance. It omits the literal `selection`
and publishes `conditionReads` and `conditionOrigin`; see
[EVALUATE](evaluate-semantic-product.md). The writer emits 2.33.0 only when that
arm shape is present. Other SP versions and supported literal arms retain their
existing wire shape. Missing interpretation stays in coverage and does not imply
global memory or control effects.

## W3-R1 — SP 2.34.0 / storage 1.10.0

`storage.logicalExactViews` transports local, complete TEXT view identity as
`node`, `representative`, `length` in logical characters. It does not publish
physical bytes, codec, offset, extent or allocation. The producer emits a group
only when recognized WORKING-STORAGE declarations belong to one proved storage
component and every member is elementary, locally modeled, has the same logical
length and interpretation, and the relation chain is proved. A missing COPY
remains `INPUT_MISSING`; it does not suppress this local declaration proof.

The lower validates member closure, shared source component, positive relations,
and compatibility with any published text transfer. It may bind distinct AIR
objects to the same logical Cell. Silence in this field is not alias proof.
SP 2.34.0 is emitted only when this fact is present; storage version is 1.10.0.
The earlier `logicalTextViews` inventory and physical layout contracts retain
their own admission rules. The parser's recognized MOVE continuation is published
even if another source artifact is missing; it is not inferred from array order.

## W3-R1 FILE record grounding — SP 2.35.0 / storage 1.11.0

The same `logicalExactViews(node, representative, length)` wire fact now also
carries a complete local TEXT group-to-descendant chain. Each group in the
chain has exactly one complete child component; the terminal elementary view
has a supported positive logical character length. Partial children, partial
REDEFINES, FILLER, distinct components and nonlocal declarations do not acquire
this identity. An independently complete elementary root may publish a
singleton exact view to establish its logical TEXT domain. A singleton asserts
no alias with another declaration. The proof depends on declarations and
positive local relations, not on FILE statements, literals, missing input or
physical byte layout. The earlier SP 2.34.0/storage 1.10.0 sibling-overlay
shape remains decodable by compatible lowers.

## W5 — SP 2.36 partial structural facts

[Contract and availability](partial-structural-facts.md): `PERFORM_PROCEDURE` can
publish `STRUCTURAL_FACTS` and an independent `targetEntry`. Existing wire shapes
and legacy specialization behavior remain versioned. Structural facts do not
assert whole-body/effects precision and do not select an executable AIR strategy.

## Control topology authority — 2.39.0

The new contract and legacy boundary are specified in [control-topology.md](control-topology.md).

## SP 2.40 fact dependency locality

[Fact dependency locality](fact-dependency-locality.md) defines the new causal proof
graph. 2.40 requires it alongside R1 topology; <=2.39 retains historical meaning.

## SP 2.45 — exceptional source-event authority

[Exceptional CICS events](cics-exceptional-handlers.md) add `ControlTopology.exceptionalEvents` descriptors with closed runtime premises. No handler destination is selected in SP. Ordinary topology and FactDependencies retain their meaning. Empty event inventories are omitted from historical wire.

### SP 2.46 — facts for dependency recall and precision

This draft extends the canonical source facts described in
[locality](recall-locality.md), [CICS host effects](cics-host-effects.md),
[CICS condition registration](cics-condition-registration.md),
[DLI host effects](dli-host-effects.md), [logical entry invariants](logical-entry-invariants.md),
[text predicates](text-predicates.md), and [SQL normal completion](sql-normal-completion.md).
The writer selects 2.46 when a new predicate, invariant, command effect or registration proof is present. Older shapes keep their previous version. Available SQL INCLUDE members expand through the existing source map; unavailable members remain explicit.

A whole-item logical proof can now apply to a closed elementary member of a record. Equal whole overlays use their existing shared cell; they never become independent allocations. Unrelated EXTERNAL/GLOBAL declarations no longer revoke a separately proved local member. Missing declaration context, unresolved overlays, repetition on the target path and unavailable allocation remain barriers.

Historical tests that expected all nested members/overlays to lack scalar facts now check these positive proofs and shared binding identity. Tests for incomplete slices, unknown input, ambiguous names and unsupported commands remain negative. No real corpus fixture or program name belongs to this implementation.


## SP 2.47 — nominalValues

`nominalValues` publica fatos de texto do fonte para candidatos condicionais.
A autoridade `NOMINAL_TEXT_SOURCE_V1` contém `symbols(node, extent)`,
`assignments(statement, target, source)`, `conditions(statement, predicate)` e
`queries(statement, node)`. Termos são READ, LITERAL, SPACES, LOW_VALUES,
HIGH_VALUES ou UNKNOWN; predicados são EQ, NOT, AND e OR. Todas as referências
pertencem aos inventários canônicos da mesma unidade. Literais e nomes são
obtidos dos fatos tipados; nomes de exibição não são mecanismo de correlação.

O fato descreve o operando inteiro nominal e a operação escrita. Não atesta
alocação, ausência de aliases, execução ou inicialização. Uma localização de
fonte aproximada não apaga um operando estruturado e resolvido. O consumidor
preserva essa limitação e a distingue das garantias do modelo executável.
Campos sem essas garantias podem contribuir para descoberta de dependências.

O produtor emite o bloco quando há uma consulta computada textual. Contratos
anteriores continuam sem o bloco; um consumidor antigo deve recusar 2.47.
Veja [regra e validação](../architecture/conditional-dependency-candidates.md).

## SP 2.48 — scoped PERFORM completion

[PERFORM control completion](perform-control-completion.md) extends the existing
control topology with `SECTION` regions and `ESCAPE` targets. An escape is an
explicit occurrence outcome referencing an enclosing `PARAGRAPH` or `INLINE_BODY`.
It completes that paragraph or leaves that inline invocation, respectively.
`COMPLETE` retains normal iteration completion, including `EXIT PERFORM CYCLE`.
Section boundaries preserve ordinary continuation separately from invocation return.

For multi-level VARYING, `Phase.level` selects a positive level and its three typed
control operands. Level 1 uses `loop`; levels 2..N use `varying.afterLoops` in order.
Each condition has its own predicate, reference IDs and provenance. Initialization,
updates, resets and tests use the published phase edges for BEFORE/AFTER. Historical
single-level phases omit `level` (decoded as 0) and retain their existing payload.

Only publications using these new topology facts select 2.48. Earlier wire formats
omit the new fields and keep their meaning; consumers must reject new facts under
older versions. Numeric value evaluation remains outside this capability.


## SP 2.49 — explicit synthetic model assumptions

`NOMINAL_TEXT_SOURCE_V2` extends every `symbols` entry with mandatory boolean
`modelAssumed`. True means the declaration's shape comes from an analysis model.
The producer selects SP 2.49 only when such a symbol is present. Ordinary products
retain V1 (`node`, `extent`) and their existing version/bytes. A consumer must
reject missing, nonboolean or silently downgraded model markers.

The AST's typed input authority determines this marker; adapters do not infer it
from a path, declaration name or DFH prefix. Binding, PIC and group qualification
remain available. Model declarations cannot supply declarative initial values,
exact storage/local-cell proof or executable predicate proof. The existing located
input gap and COPY provenance remain explicit. Nominal predicate syntax is allowed
with its symbol authority; it is not an executable proof.

The source-value consumer propagates model influence through copies and joins.
An influenced write retains predecessor candidates and unknown remainder. Width
interpretations may add a candidate but cannot remove observed source text. An
influenced condition cannot exclude either branch. Synthetic initial seeds are
ignored. A later ordinary write with independent proof can still kill candidates.
This does not introduce AIR cells, CFG successors or executable dependency edges.

## SP 2.50 — structural model locality

`MODEL_STORAGE` is an unavailable input with producer-owned scope. It does not
stand for unknown source text or grant physical layout, exact cells, initialization
or a kill. It affects declarations originating in the model, their shared storage
components (including aliases), and still-open surrounding records. Independently
closed real records after a structural model keep their own declaration context.
Actual missing COPY and unlocated input retain the existing prefix rules.

The producer carries the distinction from SourceMap through UnitInputProof; no
consumer infers it from a member name or a provenance URI. SP 2.50 transports this
input kind; older versions cannot publish it. Source uncertainty preserves the
kind and remains unavailable.

### SP 2.51 — FILE composite topology

Conditional contract for composite FILE statements. R2 control topology publishes
owned FILE_POINT identities and explicit per-use/phase successors; no new source
statement is invented. Inventory FILE remains 1.6, with the same resources, uses,
events and effects. Details: [control topology](control-topology.md#file-composite-control--sp-251).

### SP 2.52 — source continuation possibilities

An unsupported completion retains its executable UNKNOWN_LOCAL outcome. The
frontend may also publish `controlTopology.sourceContinuations`: statement,
canonical grammar-owned target and proof references with CONTROL_POSSIBILITY.
This is source-only hypothetical completion, not normal execution or success.
The target uses the existing symbolic continuation/boundary, preserving PERFORM
composition without downstream source reconstruction. Known terminal/transfer
constructs are excluded. For an unmodeled RETURN/XCTL, locally recognizable
RESP/NOHANDLE source evidence permits a local condition-return hypothesis, even
when its operand form cannot be qualified for execution: terminal success does
not prove terminal failure. RESP2 alone does not enable it. Gaps and executable
UNKNOWN_LOCAL remain explicit. Recognizing an option does not authorize NORMAL
or a qualified condition event. See [CICS source preservation](../work/cics-source-preservation.md).
A hypothesis, including a proof alias that depends on
it, cannot authorize any executable outcome, region, boundary, binding, phase,
FILE point or exceptional event. Typed and wire consumers enforce this boundary.

Only products containing these facts select 2.52. Older products omit the field.
The lower source-evidence projection may consume the possibility while keeping
its frontier and proof; executable lowering ignores it. See the
[W1 contract and oracle](../work/carddemo-control-w1.md). No ALTER or recursive
PERFORM semantics are introduced by the source possibility representation.

## SP 2.53 — área BMS implícita e controle independente de memória

CICS_COMMAND admite `implicitArea` como DataReference opcional resolvida no produtor: RECEIVE_MAP WRITE, SEND_MAP READ, área inteira, owner do statement e provenance derivada (`exact=false`) do MAP literal. Exclui INTO/FROM/SET explícitos. Binding ausente/ambíguo não é publicado como selecionado. As opções escritas não são modificadas. O wire exige 2.53 para esse campo e conserva os perfis anteriores. SourceContinuations pode estar vazio quando a nova versão decorre apenas dessa capacidade.

Destinos da ControlTopology independem de footprint físico. Lower conserva o efeito MAY aberto para referências não admitidas, nunca MUST; comandos sem destino provado continuam sem sucessor inventado. INITIAL concede existência de alocação, não seed forte. DECLARE TABLE reconhecido não aloca storage; INCLUDE desconhecido permanece input indisponível. [Regra e qualificação W3](../work/carddemo-control-w3.md).

## SP 2.54 — catálogo CICS fechado

CICS_COMMAND acrescenta ASKTIME, FORMATTIME, ASSIGN, INQUIRE_PROGRAM, SEND_TEXT e WRITEQ_TD. Cada família conserva opções, direção host e LENGTH estrutural; nenhum efeito implícito dessas famílias autoriza hostEffects fechado. Conhecimento de controle permanece na topologia. NOHANDLE duplicado sem operando conserva opções e warning CICS_COMMAND_DUPLICATE_FLAG_IGNORED; a paridade warning/duplicação é validada e a capacidade exige SP2.54. [Regra, fontes e testes W4](../work/carddemo-control-w4.md).

## SP 2.55 — SQL e DL/I com efeitos externos abertos

`SQL_HOST_OPERANDS` e `DLI_EXTERNAL_OPERANDS` conservam referências canônicas de leitura/escrita, bounds READ/WRITE/EXPOSURE `ALL`, ambiente e valores `UNKNOWN`, e nenhum MUST. SQL SELECT INTO/UPDATE/INSERT/DELETE/OPEN/FETCH/CLOSE e IMS CHKP/REPL/ISRT/DLET recebem conclusão normal possível e outcomes externos abertos. Não há prova de sucesso de I/O, status, posição do cursor/PCB ou valores retornados. DECLARE CURSOR é declarativo e não aloca armazenamento COBOL. O parser SQL consome a forma fechada inteira, com limite de 256 níveis de expressão; formas não admitidas permanecem opacas. WHENEVER não tem dispatch modelado. Detalhes e fontes em [W5](../work/carddemo-control-w5.md).

## SP2.56 — controle COBOL

Acrescenta regiões SENTENCE/SEARCH/SEARCH_ARM, escape de SENTENCE, outcome/target PROGRAM_HALT e prova SEARCH_INDEX_MAY. A prova exige READ/WRITE ALL, exposição NONE, ambiente NONE e nenhum MUST; índice/resultado não são inferidos. Contratos de [topologia](control-topology.md) e [W6](../work/carddemo-control-w6.md) definem premissas contextuais e limites.

### SP 2.57 — PERFORM binding precondition

Publications with a specified `controlTopology.bindings[].reentryPolicy` require
2.57.0. Every binding carries the field in that version. SOURCE_UNDEFINED records
an undefined active reentry; UNSPECIFIED grants no additional authority.
The policy is orthogonal to completion endpoints and phases. See
[control topology](control-topology.md#sp-257--active-binding-reentry).

### SP 2.58–2.59 — MOVE footprint and source text expressions

2.58 introduces the MOVE_TARGETS effect proof. It bounds receiving MAY writes
and structured source reads independently of executable value admission. It
never authorizes MUST, storage exposure or a known value. Unresolved receivers
keep an open write bound; special registers remain implicit runtime state.

2.59 permits NOMINAL_TEXT_SOURCE_V3. A term may recursively carry exactly one
argument for UPPER_ASCII, TRIM_SPACES, TRIM_LEADING_SPACES or
TRIM_TRAILING_SPACES. These generic operators are selected by canonical typed
COBOL syntax. They preserve source supports and modelAssumed confidence.
Unsupported transforms and runtime functions remain UNKNOWN. V1/V2 serialized
leaf terms remain unchanged. See the active [campaign](../work/carddemo-values-control.md).

## Source table text — SP 2.60 / qualified source 1.4

NOMINAL_TEXT_SOURCE_V4 adds `tableFields`. Each field has a real storage node
identity and initializer values linked to real declaration origins. The frontend
uses typed DISPLAY character extents, fixed OCCURS bounds and proved REDEFINES
components. Numeric DISPLAY columns participate in offsets; their contents are
not program names. No physical memory capability is asserted.

This is an index-insensitive summary of each textual field and its overlapping
textual aliases. Valid constant indices and unknown indices admit the field's
possible occurrences, with an open remainder. An invalid constant index does not
publish a query. Every update to a summary is weak; conditions over the summary
cannot eliminate elements. Copying a table value preserves the predecessor
snapshot, initializer/assignment supports and index uncertainty. Model VALUEs
cannot initialize this analysis or grant a kill proof.

Only typed literal group writes are sliced in the producer. Unsupported partial
writes, ODO, non-DISPLAY geometry and unproved overlays remain unknown. A bounded
summary can retain obsolete names; it cannot certify an occurrence-specific kill.
No lower or CFG code reads declaration syntax, member names or statement text.

The wire uses a closed V4 variant; old nominal authorities reject table fields
and CHOICE terms. Source evidence 1.4 is required, preserving older envelopes and
requiring full declaration provenance. These facts do not change AIR or CFG.

IBM authorities: [OCCURS](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=entry-occurs-clause),
[subscripting](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=table-subscripting),
[REDEFINES](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=entry-redefines-clause).

## Declared alternate starts — SP 2.61

`SOURCE_DECLARED` entry inventories retain the primary and observed alternate
ENTRY declarations. Each alternate has a source declaration identity, an optional
external name and its own signature availability. `controlTopology.entryPoints`
is the authority for starts; entry/declaration/start identities must agree.
The `alternate-entry-start` grammar proof licenses only the next executable
statement after the declaration. Sequential ENTRY remains neutral. Nested
programs, conflicting names and PROCEDURE DIVISION RETURNING do not acquire
external roots. Invalid or unavailable starts retain explicit entry gaps.

Each admitted root is a separate external activation. Its PERFORM completion
binds within that activation; roots do not create edges to each other. USING
parameters remain unknown. Absent RETURNING is independent of parameter
uncertainty. A nominal LINKAGE name without a grounded location cannot license
an executable read or a fabricated local Cell. Its computed target remains open.

Qualified source 1.5 identifies ALTERNATE_ENTRY roots and retains the grammar
proof; older envelopes cannot carry that authority. Runtime entry/signature
coverage remains PARTIAL. A trailing ENTRY without an admitted executable start
is inventoried but does not produce an AIR entry in this capability.

Authority: [IBM Enterprise COBOL ENTRY](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-entry-statement).
Oracles: AlternateEntryTest, AlternateEntrySuite, QualifiedSourceContractTest.

## Runtime condition registrations — SP 2.62 / qualified source 1.6

The canonical control product separates `conditionRegistrations` from
`conditionEvents`. A registration identifies its source statement, condition,
LABEL/DEFAULT/IGNORE disposition, optional resolved procedure target and grammar
proof. An event identifies a possible PGMIDERR on LINK/XCTL, command-local bypass,
error continuation and the separate default-abend event. All targets retain
published identities and provenance. LINK successful return has its own ordinary
proof, independent of the error disposition.

Only event-relevant PGMIDERR/ERROR state is tabulated. Each reaching registration
keeps its source identity and proofs. Successful replacement kills that condition's
previous disposition; omitted labels request the system default and suppress
ERROR fallback. IGNORE and RESP/NOHANDLE admit error continuation. A selected label
branches in the current COBOL activation, stays registered, and cannot manufacture
a PERFORM return. LINK callee state is outside the current activation. External
COBOL calls open the current definitions without discarding their possible targets.

PUSH/POP stacks and other event families remain unmodeled. The producer publishes
potential restoration relations, including error continuation, with a POP
prerequisite. Consumers must propagate that prerequisite along the same contextual
execution path to the event. Reaching the POP somewhere in the unit is insufficient.
A proved later specific registration (LABEL, DEFAULT or IGNORE) kills the restored
alternatives for that condition. Replacing ERROR kills restored ERROR alternatives,
while independently possible specific dispositions survive. Separate entries and
matched PERFORM calls/returns must not share a restoration merely by source ID.
These CONTROL_POSSIBILITY facts cannot be executable AIR authority. Dead POP,
causally later POP without a return path, and bypassed events do not activate them.
This bounded approximation does not claim exact stack restoration or completeness.

The lowerer validates registration effects, event command/options, version, proofs
and canonical provenance at both wire and in-memory admission. Qualified source
1.6 carries the condition state and LINK default-event provenance. The CFG still
projects AIR transitions; its dependency consumer validates and transports the
qualified source evidence without recognizing CICS syntax or catalog names.

Authorities and independent adversaries: [campaign](../work/carddemo-values-control.md).

### Table geometry and level 88

Table shape and tableFields describe storage-bearing declarations. A CONDITION_88
child annotates its conditional variable; it does not turn an elementary PIC item
into a group or add width to any ancestor. Geometry placement ignores those
components as well as field selection. An 88 VALUE is a predicate value, never
an initializer for the annotated field or a sibling. Declaration origins, weak
updates and the index-insensitive open remainder are unchanged.

Authority: IBM [special levels](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=relationships-special-level-numbers)
and [condition-name VALUE](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=vc-format-2).
Permanent contrasts: NominalTableTest and lower SourceTableSuite.

### CICS nominal incompleteness

Program-control and FILE targets/options transport canonical reference-report
gaps using the same mapping as typed commands and handlers. A published incomplete
DATA binding retains NOMINAL_BINDING at its owning statement and the operand's
source provenance; a missing COPY remains an input gap independently. Capability
gaps do not substitute for nominal gaps. Literal targets, ambiguous candidates,
read/write roles and existing physical/control proof remain unchanged.
`CicsNominalGapTest` covers unresolved/ambiguous names, missing input and isolation
between statements.


## Gaps atuais — SP 2.63

SP 2.63 introduziu a regra para publicações com `controlTopology`. A obrigação de um gap localizado
pode ser satisfeita pela prova positiva atual correspondente: membership,
invocação de PERFORM ou NO_OP com controle local fechado. O predicado textual
validado dispensa o gap de predicado indisponível. Perfis especializados continuam
limitados; suas restrições de isolamento não são gaps da análise composicional.

Esta regra substitui as exigências históricas acima de gap incondicional para
OBSERVED ou containment UNKNOWN. Gaps de input, literal desconhecido, binding e
runtime mantêm suas obrigações próprias. [Contrato atual e limites](active-gaps.md).


## Condições 88 e SET — SP 2.64

A versão 2.64.0 introduziu esta capacidade. `conditionNames` publica definições e usos
nominais de 88, valores/intervalos, árvores de predicados e atribuições SET
ordenadas. A semântica fica nos fatos canônicos do frontend; projector e consumidor
somente traduzem esses fatos. Os 88 não alocam armazenamento nem tornam seu pai
um grupo. Gaps redundantes de capability são satisfeitos por essas provas;
obrigações independentes de input, controle, binding e runtime permanecem.

[Contrato, regras, limites e complexidade](condition-names.md).

## Valores numéricos e MOVE inteiro — SP 2.65

A versão corrente do produtor é **2.65.0**. Literais de ponto fixo publicam
`kind=NUMERIC` e `value` decimal canônico, independentemente da capacidade do
receptor. ZERO/ZEROS/ZEROES têm valor numérico zero. A classificação não autoriza
conversão para texto, ponto flutuante ou codificação física.

`MoveFact.integerTransfers` contém `{target, value}` por receptor provado.
`value` é o inteiro literal ou null para leitura do DATA original. Cada destino
tem binding único e `wholeItemAccess` com `scalarInteger`; o DAG causal publica
`LOGICAL_INTEGER` e `LOCAL_CELL` disponíveis. DATA de origem deve ter capacidade
menor ou igual à dos receptores de um prefixo provado; literal não negativo deve caber.
A lista mantém a ordem escrita e admite múltiplos destinos. Somente a lista
completa remove os gaps de identidade/whole item do MOVE.

O domínio compartilhado também serve aos controles numéricos existentes.
Não há segunda interpretação de gaps, memória numérica paralela nem layout
físico deduzido do PIC. Regras, limites e oracles: [MOVE numérico](numeric-move.md).
