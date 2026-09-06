# Discovery bilateral — Semantic Product e Analysis IR 2.0.0

## Executive summary

**O port atual permite uma publicação AIR V2 válida e conservadora de
identidades, declarações nominais, usos disponíveis e inventário observado.
Não contém fatos suficientes para o primeiro CFG executável fechado, nem
para `assign`, predicate ou `invoke` precisos nas capabilities declaradas.**
A suficiência é relativa à observação exigida: conservar inventário com
fronteiras abertas é possível; reconstruir fluxo e valores só com o port
exige os enrichments mínimos abaixo. Nenhum lowerer/consumer AIR foi
implementado ou certificado por este audit.

Das cinco famílias materializadas, DATA preserva precisamente a identidade
nominal; MOVE, CALL, IF e ObservedStatement permitem abstrações conservadoras.
Os fatos de identidade, binding, membership e provenance não são uma operação
AIR completa. Não há percentual significativo de cobertura da linguagem:
`InventoryStatus.COMPLETE` conta statements observados, não todas as
capabilities normativas, entradas ou declarações físicas.

O primeiro alvo é `AIR-STRUCTURE@2`. `unknown_type(u)`, com `u` de código
`TYPE_UNKNOWN`, e bindings de storage abertos permitem DATA sem inventar
células. `opaque` com os três
envelopes máximos permite conservar statements sem supor fallthrough. Isso
produz no máximo CFG parcial com fronteira aberta, inclusive para o interior
da unidade; não apenas uma aresta para o fim. O primeiro CFG pequeno com
controle fechado exige entrada/assinatura e semântica terminal publicadas.

Blockers de precisão reais: entrada e conclusão da unidade, sequenciamento
semântico, terminal behavior, endereçamento dos operandos, pureza/limites de
avaliação da condição, interpretação do target e contrato de CALL; para fluxo
escalar, domínio/conversão de MOVE e associação declarativa de storage.
Literal kind e ConditionSemantics completa **podem esperar** no slice de
controle; não se pode preencher as lacunas com tipos ou operadores escolhidos.

`BACKLOG-IR-001` pode ser promovido, após autorização de implementação, para
adotar o contrato externo e seus validadores/oracles. `BACKLOG-LOWER-001` pode
começar por tradução nominal/inventário com fronteiras abertas. A recomendação
para valor observável é fechar antes o pequeno enrichment de entrada/terminal,
depois implementar lowerer e CFG em checkpoints separados. F-02 ainda precisa
ser mergeado e revalidado antes de apoiar a nova cadeia na garantia de
integridade. Este audit não concede autorização para nenhum desses trabalhos.

## Fontes, método e prerequisite

Audit em 2026-09-06, branch nova a partir de `main` e `origin/main`
`107ce08a6d73a6a328f6d81e1ed859e9d92e86e5` (merge do PR #27). O worktree
estava limpo antes da troca; `fetch`, `switch main` e `pull --ff-only origin
main` confirmaram sincronização. O ZIP já estava fora do Git, em
`/tmp/analysis-ir-check.RIhicL/repository.zip`; extração em diretório irmão
`extracted/analysis-ir-main`. Nenhum desses inputs integra o diff.

| Input | Identidade verificável |
| --- | --- |
| AIR | README e todos os documentos normativos declaram **2.0.0** |
| ZIP | 101117 bytes; SHA-256 `6d89854f6cd4abf5d0f2e20dcbd6b003cc700d617dd2483c0e28244fa3fbb7e6` |
| Metadata real | Comentário do ZIP: `0b2fbce7046010b22b32efa8cbc3e75ccba09442`; API GitHub confirmou esse [commit](https://github.com/Gustavo2358/analysis-ir/commit/0b2fbce7046010b22b32efa8cbc3e75ccba09442), de 2026-09-06T01:41:23Z. Não há diretório `.git` extraído. |
| F-02 | PR #28 **OPEN**, `mergedAt=null`; head `52ee4ebfb9f10f6d3d4467bf20377b6f2df9e4ef`, fora da main auditada |

O histórico de [WORK-SEMANTIC-PRODUCT-002] e o YAML/state de
WORK-AST-002 foram consultados conforme solicitado. O state da branch de
F-02 registra implementação e refinamentos de candidates/scopes; o state da
main ainda descreve seu Discovery. Nenhum deles prova merge. A inspeção da
implementação pendente limita-se ao validator e ao seu estado de trabalho;
não foi usada como produção da main. `git diff main...origin/feat/semantic-product-integrity-validator`
mostra que o port/projector não muda nesse PR: mesmo após F-02, os gaps de
suficiência listados aqui permanecem.

Foram lidos integralmente README, especificação 00–11,
[invariantes AIR] e [oracles AIR] do ZIP. Exemplos não substituem norma.
Referências `[Axx]` abaixo apontam
para os mesmos arquivos no commit fixado; `[Sx]` apontam para código da
baseline, com símbolos/linhas para reprodução. O contrato interno continua
sendo ADR-0013, INV-SP-* e [COBOL Semantic Product].

Critério aplicado: uma capability só está disponível se um consumidor
independente consegue observar seu comportamento necessário com os fatos
publicados, sem consultar o produtor novamente ([A00] §4, [A07] §8, [A11]).
A boundary auditada é `frontend → CobolSemanticPort → CobolLower → AIR →
consumers`. AST, symbols, occurrences, resolution, report, SourceMap, source,
JSON, snapshots e parser são proibidos como entradas alternativas do lowerer.
Usá-los **neste audit** para mostrar o que se perde não autoriza seu uso futuro
para recuperar os fatos perdidos.

## Evidência factual reproduzível

| Ref | Superfície e localização na baseline |
| --- | --- |
| [S1] | `CobolSemanticProduct`: IDs/Policy/provenance 79–164; DATA/binding 184–259; anchors/containment 261–293; operands/facts 308–418; coverage/State 420–451; validações 453–670 |
| [S2] | `CobolSemanticPort` 15–51: somente uma unit, DATA/statements/gaps/coverage e consultas estruturais; não tem Entry, signature, storage ou predicate |
| [S3] | Projector: gates MOVE/CALL 212–235; DATA 350–408; emissão 411–590; continuation 647–674; gaps CALL 714–731; readiness 897–965; inventário 983–1047; traversal 1400–1440 |
| [S4] | Probe independente CP8: `reconstruct`, `validateIfStructure`, `validateReadyBinding`; reconstrói DTOs próprios, não AIR, TypeRef, operações ou envelopes |
| [S5] | CP8: teste positivo e quatro falsificações; fixture tem 14 statements, 1 observado, literal UNKNOWN; asserts de CALL SUFFICIENT não verificam invoke AIR |
| [S6] | `Ast.DataReference` 334–349: preserva subscript groups e reference modification; esses campos não existem em `[S1].DataReference` |
| [S7] | Composition root `publishSemanticProduct` e chamada da unit primária: publicação por unit não é inventário AIR de todas as units/entradas |
| [S8] | `SourceMap.location` 260–267 e `UnicodeText.lineColumn` 78–83: linhas base 1, colunas base 0 em code points, extremidade final inclusiva |

Experimento descartável, após `mvn -q clean test-compile`, usando
`AstBoundaryTestSupport.analyze` e `ExplorerMain.publishSemanticProduct` da
baseline. A compilação limpa evita herdar classes de F-02 da branch anterior.
Driver e classes ficaram em /tmp. Não são testes novos nem um lowerer.

Entrada comum: `PROGRAM-ID. AUDIT-PROBE`, WORKING-STORAGE com
`01 TAB. 05 WS-X PIC X(8) OCCURS 2 TIMES. 01 IX PIC 9.`, PROCEDURE DIVISION,
um dos statements abaixo, END PROGRAM. O helper exige zero erros gramaticais;
isso demonstra comportamento da implementação, não validade COBOL de toda
variante aceita. O controle bare da tabela serve para comparar a projeção.

| Statement | Produto observado | Gaps e perda |
| --- | --- | --- |
| `CALL WS-X` | CallFact; lowering/CFG SUFFICIENT; selected WS-X | somente DYNAMIC_CALL_TARGET_VALUE_UNKNOWN |
| `CALL WS-X(IX)` | mesmos estados e binding nominal | frontend tem CALL_TARGET e SUBSCRIPT:IX; port só tem o primeiro e o mesmo gap de runtime |
| `MOVE 'A' TO WS-X(IX)` | MoveFact; PARTIAL/SUFFICIENT | frontend tem VALUE_WRITE e SUBSCRIPT:IX; port só publica target nominal e LITERAL_KIND_NOT_PUBLISHED |
| `GOBACK` | ObservedStatement; BLOCKED/BLOCKED | MODELED_STATEMENT / GENERIC_MODELED_STATEMENT; OBSERVED_STATEMENT_UNSUPPORTED |
| `CONTINUE` | mesma classificação genérica | não distingue ausência comprovada de efeito de término |
| `STOP RUN` | mesma classificação genérica | não distingue retorno da unit de término do run unit |

Reproduzir a perda exige comparar ocorrências tipadas de entrada e facts do
port, nunca extrair semântica do texto do diagnóstico. O contracaso de
reference modification é também demonstrável pela inspeção de `[S6]` e pelo
gate de capability `[S3]`, mas não foi executado pelo driver. Não se alega
que F-02 corrige essa projeção.

## AIR V2 delta

| Premissa do CP8 / expectativa anterior | Veredito contra V2 |
| --- | --- |
| Boundary fechada, plural e sem frontend downstream | **Mantida** como arquitetura; probe prova isolamento/reconstrução, não conformidade AIR. |
| DATA nominal pode atravessar sem storage físico | **Refinada**: Object requer TypeRef e associação explícita `unknown(scope,reason)`. Não criar Cell por DataItemId. |
| Literal UNKNOWN simplesmente atravessa como literal IR | **Descartada**: [A02] §2 proíbe literal unknown_type. Conservar occurrence/valor de origem e uma expressão abstrata em opaque; não é literal executável AIR. |
| Tipo desconhecido impede qualquer cópia | **Descartada** como regra geral: sameDomain permite cópia sem domínio concreto, se semântica de cópia, memória e prova forem estabelecidas. O port atual não fornece essa prova para MOVE. |
| CALL nominal resolved + runtime UNKNOWN é lowering-ready | **Refutada para invoke V2**: falta interpretação `known(text)`, address semantics e contrato de interação/outcomes. O fallback é opaque. |
| IF estrutural ready dispensa predicate preciso | **Mantida com condição**: branch aceita unknown bool puro, não unknown_type; ausência de garantia de pureza/erros exige opaque. Membership não prova sequência/saída. |
| ObservedStatement bloqueia toda representação IR | **Descartada**: opaque máximo é normativo; bloqueia precisão/fechamento, não validade conservadora. |
| Storage só importa depois do CFG | **Refinada**: consequências de storage são consumer; fatos declarativos de associação/alias/duração precedem AIR escalar precisa. |
| Perfil anterior e extensões podem ser reutilizados | **Descartada para perfil**: núcleo 2.0.0/perfis @2; extensões local/indirect/regions continuam @1. Validator precisa I-49–I-54 e O-69–O-85 aplicáveis. |

Não foi encontrada implementação AIR V1 no projeto. O que se corrige são
expectativas de handoff do CP8; não se reescreve seu resultado histórico como
se já tivesse testado outra especificação. [A09] §5.1 fixa a mudança major.

## Bilateral sufficiency matrix

`PRECISE` classifica o fato no escopo indicado; `CONSERVATIVE` admite abstração
normativa; `BLOCKED` indica a representação/observação específica que falta.
Uma mesma capability tem linhas distintas para fato nominal e comportamento.
`V/C` significa candidato a publicação VALID e consumo CONSERVATIVE, **ainda
sem implementação/validação AIR**. `PFP` significa PRECISE_FOR_PROFILE, que só
pode ser alegado depois dos oracles completos do papel/perfil [A10]. Owners:
**A** frontend/Semantic Product; **B** CobolLower/contrato AIR; **C** consumer.

Na coluna Perfil, STRUCTURE, SCALAR, DEPENDENCY e REGION abreviam os nomes
normativos AIR-STRUCTURE@2, AIR-SCALAR-FLOW@2,
AIR-DEPENDENCY-OBSERVATION@2 e AIR-REGION-FLOW@2, respectivamente.

| Semantic capability | Fato publicado hoje | Requisito AIR V2 | Representação AIR candidata | Perfil | Status lowering | Precisão / conformidade | Fato ausente | Owner correto | Próxima ação |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| DATA nominal | id, nome, PIC opcional, origem [S1] 184; [S3] 350 | identidade/origem/escopo [A01] §6; [A07] §2 | ObjectId correlacionado | STRUCTURE@2 | PRECISE nominal | identidade preservável; objeto completo usa próxima linha | nenhum para identidade local | B | mapear namespace e origem |
| DATA tipo/storage | nenhum domínio/associação/duração [S1] | TypeRef e binding explícito [A02] §1; [A03] §4 | unknown_type + unknown(all publication/environment); atributos indisponíveis com gaps | STRUCTURE@2 | CONSERVATIVE | V/C; storage/values OPEN | tipo, duração/visibilidade exatos | B fallback; A fatos precisos | não ler PIC como tipo; não fabricar Cell |
| Independência/alias/EntryState | DataItemId apenas; FILLER/hierarquia/relações não publicados [S3] | prova de separação, associação/duração/estado inicial [A03] | nenhuma Cell independente precisa | SCALAR/REGION@2 | BLOCKED preciso | PFP não alegável | associação declarativa e autoridade; seeds por entrada | A fatos; C consequências | enrichment antes de efeitos/RD precisos |
| MOVE literal occurrence | kind UNKNOWN, value, origem/id [S1] 308; [S3] 475 | literal exige known(T) [A02] §2 | unknown(unknown_type(u),…,reason) em opaque; value preservado em origem diagnóstica | STRUCTURE@2 | CONSERVATIVE | V/C; literal AIR preciso BLOCKED | domínio semântico e interpretação de valor | B fallback; A literal canônico | manter gap de tipo distinto do de valor |
| MOVE atualização | target WRITE + binding [S1] 349 | assign exige sameDomain, conversão/avaliação e escrita completa [A04] §2 | opaque com target nominal e envelope de escrita aberto | STRUCTURE@2 | CONSERVATIVE | V/C; não afirma cópia/strong update | conversão, local e extensão da escrita | A para precisão; B abstração | sem assign/havoc isolado que perca source |
| MOVE preciso | não publica compatibilidade [S3] | [A02] §1.3–1.4; [A04] §2 | assign somente após provas e normalização estabelecidas | SCALAR@2 | BLOCKED | PFP não alegável | literal/domain, conversão, alias/layout aplicável | A → B | literal kind sozinho não basta |
| Endereçamento MOVE/CALL | base nominal; perde subscript/modificação [S6], experimento | preservar ADDRESS_READ, local e avaliação [A02] §5–7; [A07] PROD-02/05 | usos nominais + acesso aberto em opaque | STRUCTURE@2 | CONSERVATIVE para abstração; BLOCKED para acesso preciso | V/C; claims SUFFICIENT atuais insuficientes | occurrence de índice/offset, selector, limites/pureza | A | corrigir capability/readiness e publicar acesso; não recuperar AST |
| CALL target nominal | operand CALL_TARGET resolved e runtime UNKNOWN [S1] 360 | objeto que contém nome ≠ recurso [A01] §7 | uso nominal em opaque; RESOURCE_TARGET_UNKNOWN | STRUCTURE@2 | PRECISE nominal / CONSERVATIVE interação | binding exato, dependencies OPEN | programa runtime | C | preservar before(site) e remainder |
| CALL invoke calculado | syntax única, policy de opções; sem interpretação de nome [S1] | ComputedResource exige namespace, política, expression known(text) [A04] §7 | opaque até contrato de interpretação; depois invoke com unknown text derivado | STRUCTURE/DEPENDENCY@2 | BLOCKED como invoke; CONSERVATIVE opaque | V/C via fallback, sem PFP da interação | interpretação do nome e condições de avaliação | A → B | não promover read(DATA) a text |
| CALL args/results/outcomes | não há listas; gaps quando presentes [S3] 714, 913 | posições/modos, resultados por normal, bounds/outcomes [A04] §7.1–7.3/11; [A05] §4 | opaque máximo; invoke parcial futuro sob contrato suficiente | STRUCTURE@2 | CONSERVATIVE fallback | V/C; sem transmissão precisa | aridade/roles/ordem/retorno/exception targets | A; B traduz ausência explícita | ausência de cláusula não prova pureza/retorno único |
| IF membership | THEN/ELSE por parent, continuation opcional [S1] 384; [S2] | preservar alternativas e destinos [A05] §2 | decomposição derivada com destinos conhecidos e remainder onde faltar controle | STRUCTURE@2 | PRECISE relações / CONSERVATIVE controle | V/C; membership não é PFP de CFG | entradas de branches/continuação executável universal | A fatos mínimos; B labels | não criar reconvergência de ramo terminal |
| IF predicate | shape + READs conhecidos; sem operator/object/pureza [S1] 332 | branch exige bool puro; desconhecido impuro exige opaque [A02] §4; [A07] §5 | opaque avaliador/controle com reads e limites; branch unknown bool só após garantia | STRUCTURE@2 | CONSERVATIVE opaque; BLOCKED branch puro geral | V/C; values OPEN | pureza/totalidade ou envelope de avaliação; não exige ainda operador preciso | A → B | mínimo contrato de avaliação antes de ConditionSemantics completa |
| ObservedStatement | identity, shape genérica, origem/gap [S1] 395 | envelopes interpretáveis [A04] §9; [A06] §3 | opaque; memória máxima, any_control, any_resource | STRUCTURE@2 | CONSERVATIVE | V/C; controle/efeitos/deps OPEN | nenhum para envelope máximo; semântica de família para fechar | B fallback; A precisão | nunca nop, omissão ou normal-only |
| Provenance | expanded/original, include chain, exact [S1] 118–146; [S8] | base/unidade, origem derivada/lacunas [A06] §5 | WRITTEN/DERIVED com convenções explicitadas | STRUCTURE@2 | PRECISE quanto à exatidão publicada | válida sem promover approximate | origem própria da unit/gap global pode faltar | B; A se precisar mais detalhe | usar UNAVAILABLE onde faltar, nunca span fabricado |
| IDs/namespace | unit, statement, operand, data; sem AIR IDs [S1] | IDs AIR próprios, owners fechados [A01] §4/9 | tabelas de correlação e DERIVED para decomposição | STRUCTURE@2 | PRECISE no escopo da unit | validade verificável; sem persistência entre edições | nenhum para novos IDs AIR | B | IDs auxiliares não apagam origin do port |
| Program points | ordinal de traversal [S1] 261 | before/after(outcome)/entry/exit [A01] §5 | before/after de operações geradas correlacionados ao anchor | STRUCTURE@2 | PRECISE correlação; BLOCKED execution order inferida | V/C com controle aberto | next executável/saída por semântica | A → B | não transformar ordinal em aresta |
| Entradas/unidades | port de uma unit; roots não são Entry [S2], [S7] | corpo disponível tem Entry e assinatura; múltiplas entradas preservadas [A01] §2 | body/inventário de entradas indisponível, ou entrada abstrata DERIVED com any_control | STRUCTURE@2 | CONSERVATIVE abstração; BLOCKED entrada precisa | V/C de escopo aberto; sem CFG fechado | entrada real, signature availability, início, fim, entry inventory | A | primeiro enrichment para CFG útil |
| Containment fora de IF/multi-unit | UNKNOWN para outros parents; candidates externos renamespaced [S3] 350, 631 | unidade/visibilidade/captura explícitas [A01] §2/6 | escopos de unit e captura abertos | STRUCTURE@2 | CONSERVATIVE | V/C, sem herança/alias entre ports | owners relacionáveis de declarações compartilhadas, procedure regions | A | não unir por nome/provenance; compor ports somente com contrato |
| Coverage/gaps | contagem de statements; Gap requer statement, sem GapId [S1] 409–451 | inventários por domínio, UncertaintyId/escopo [A06] §1/4 | ID AIR derivado por occurrence do gap; missing capability de port em escopo explícito | STRUCTURE@2 | CONSERVATIVE | V/C; COMPLETE apenas no inventário alegado | detalhes de gaps DATA/input/entrada sem statement | B fallback; A granularidade | não omitir unidade vazia com input missing |
| Binding ambíguo/unresolved | candidates + reason/selected [S1] 206 | choice não seleciona primeiro; remainder explícito [A02] §5 | candidatos conhecidos + resto quando não comprovada exaustividade | STRUCTURE@2 | PRECISE nominal / CONSERVATIVE local | V/C; sem disjunção física | certeza do local físico, não novo lookup | B; A facts de acesso | lacuna de tipo não muda binding resolved |
| Runtime unknowns | DYNAMIC_CALL_TARGET_VALUE_UNKNOWN [S1] 360 | target value antes de efeitos e resto [A06] §6; [A08] §5–6 | uncertainty de recurso/valor, não falha nominal | DEPENDENCY@2 | CONSERVATIVE | V/C; targets precisos não alegáveis | valores possíveis/contexto/recurso | C | RD → PV → interpretação/catálogo conforme consulta |
| CALL literal/recursos | ObservedStatement; literal/semântica PROGRAM não atravessam [S3] 228 | categoria/ação, nome/namespace/site/origem [A08] §6; [A10] §8 | opaque máximo hoje; ResourceRef/invoke/envelope conhecido após enrichment | DEPENDENCY@2 | BLOCKED observação nominal exata; CONSERVATIVE inventário | sem perfil de dependências completo | literal nominal, policy normalizada, target interno/externo | A → B; C observação | prioridade alta; dispensa RD/PV para OBSERVED |
| GO TO/terminais/PERFORM/EVALUATE/SEARCH/ALTER | somente observado, parte do nesting UNKNOWN [S3] 243 | transfer semantics, frames/labels/seleção [A05] | opaque máximo hoje | STRUCTURE e extensões @2 | CONSERVATIVE fallback; BLOCKED precisão da família | sem PFP das extensões | ver ordem e mapeamento abaixo | A → B → C | priorizar entradas/terminais e saltos antes de seleções complexas |

## Unknowns e operações: decisões de tradução

### unknown_type e identidade de gaps

`Gap` não tem ID próprio nem endpoint DATA/Unit: todo gap exige StatementId
existente. Isso **não bloqueia** UncertaintyId AIR. B pode atribuir IDs próprios
determinísticos no namespace da publicação, correlacionados à ocorrência do
gap na lista, StatementId, scope/code e origem; mensagens não são chaves.
Dois gaps iguais não provam que duas entidades têm o mesmo domínio. Não há
necessidade de criar uma API GapId do frontend apenas para fornecer AIR IDs.

LITERAL_KIND_NOT_PUBLISHED sustenta TYPE_UNKNOWN ancorado no OperandId do
literal. Sua expressão abstrata também precisa de razão de valor, por exemplo
SOURCE_SEMANTICS_UNAVAILABLE; TYPE_UNKNOWN sozinho não a substitui. O value
do port permanece metadado de origem do operando; consumer não o interpreta
como literal AIR. Gaps de DATA, assinatura, storage e avaliação que não existem
como records podem nascer da **ausência contratual da capability no port**:
uncertainty DERIVED com a declaração/unit/site e regra versionada de tradução,
sem alegar uma causa específica que o port não identifica. Tipo desconhecido
não vira REFERENCE_UNRESOLVED quando o binding está resolved.

Inventory INPUT_MISSING sem statements admite uncertainty de unit e origem
UNAVAILABLE. Isso conserva a falta de input mas não recupera qual COPY faltou;
granularidade/recurso de COPY exigirá enrichment A. Duração/visibilidade não
publicadas não recebem activation/private/external por default. Storage
inventory indisponível é distinto de zero conhecido ([A01], [A03], [A06]).

### sameDomain e MOVE

Não há premissa, alias exato ou associação objeto–célula no port atual.
DataItemId só prova declaração nominal; não prova domínio estável de uma vista,
ausência de conversão ou independência. MOVE A TO B ainda é observado fora do
slice literal: nem sua leitura tipada atravessa como MoveFact. A mera
existência de MOVE ou coincidência de gap/value/PIC não é prova de sameDomain.

Mesmo com literal kind conhecido, AIR `assign` exige que o valor já tenha sido
normalizado para a conversão COBOL, com compatibilidade de domínio e condições
de escrita. IBM documenta conversão, padding e truncamento em [MOVE IBM];
`PIC X(8)` e value `A` não autorizam atribuir AIR text(A) diretamente.
Sem prova, [A04] §2 exige opaque que preserve source/destino e escritas
conhecidas. Abrir o efeito para may-write é admissível onde não se conhece
extensão/obrigatoriedade; não chamar isso DEF precisa ou strong update.

O-77/O-82–O-85 protegem também o positivo futuro: uma cópia de fato comprovada
não deve perder sua relação de valor apenas por unknown_type. DomainProofScope
é estático: entry(e) não cobre corpo, invocation(k) não cobre k2, interseção
vazia não prova nada e unit(u) não cobre units contidas. Não há activation(...).

### IF

O bit explicitlyTerminated é delimitação de IF, não término da execução.
Membership e continuation estrutural são fatos úteis independentes do valor
do predicate. Porém ConditionSurface não publica garantia de avaliação pura,
total, efeitos excepcionais nem completude de reads. `shape=RELATION` não
prova essas propriedades para toda expressão que a gramática admite.

V2 admite `branch(unknown(known(bool),deps,remainingReads,reason),t,f)` **quando
domínio booleano/pureza estão estabelecidos independentemente**. Não exige
operator `=` nem literal 1. Hoje o fallback geral é opaque com reads nominais
preservados e avaliação/controle aberto. Pode conservar possibilidades de
THEN/ELSE sem alegar que são exaustivas; não inventar qual filho é first
executável a partir de traversal. O enrichment mínimo para diamond fechado
é contrato de avaliação + entradas/continuações executáveis, não toda a
ConditionSemantics/ConditionValidation futura ([A02] §4; [A07] §5).

### CALL

CallFact admite apenas target Ast.DataReference, embora enum/claim diga
identifier/expression. Expressões não DATA viram observado. A shape DATA inclui
subscripts e slices que o port não distingue. A leitura do nome deve preservar
address reads; nominal binding não estabelece que ler a declaração inteira
seja o mesmo acesso que o programa faz.

No fallback, a correlação nominal deve acompanhar um acesso aberto cujo scope
inclui a declaração, sem afirmar `read(object(base))` exato. O envelope máximo
cobre a avaliação desconhecida, mas não recupera a occurrence IX perdida nem
autoriza dizer que todos os operandos conhecidos pelo frontend foram publicados.

Para invoke calculado, V2 exige categoria/ação, namespace, interpretação e
expression `known(text)`. Policy com PGMNAME/DYNAM/DLL é evidência de opções,
não contrato normalizado de interpretação/call effects. Um futuro contrato A
pode estabelecer apenas que o **resultado da interpretação** é textual:
então B emite unknown text com dependencies de tipos desconhecidos. Não
converte por conveniência o tipo original de DATA.

Hoje CALL sem USING/RETURNING/exception clauses pode ser identificado no
projector pela ausência dos gaps específicos; mas não há lista/availability
tipada desses inventários no port e o core não valida essa equivalência.
Além disso `callReadiness` não rebaixa lowering por exceptionFlow (só CFG).
F-02 não resolve isso. Ausência de ON EXCEPTION não exclui saída excepcional,
halt/diverge ou controle não local; `normal(next)` só afirma retorno possível
e exige uma continuação sustentada. O fallback atual é opaque com target
nominal e outros argumentos/resultados/efeitos/controle abertos. Quando
invoke for permitido, assinaturas parciais são válidas, mas não transmissão
precisa sem sameDomain; escritas de resultados só no normal assegurado
([A04] §7, [A05] §4; [CALL IBM]).

### ObservedStatement, entrada abstrata e limites do primeiro CFG

[A05] §6 e [A06] §3 permitem o topo explícito:

```text
opaque observado:
  knownOperands = somente os facts disponíveis (zero conhecido ≠ nenhum uso)
  memory = leituras/escritas conhecidas + outras leituras/escritas
           em todo armazenamento da publicação e ambiente
  control = alternativas sustentadas + remainder any_control
            (inclui saídas, término, divergência e controle externo)
  dependencies = usos conhecidos + remainder any_resource
  reasons = gaps de origem + razões derivadas de capability indisponível
```

O argumento de conservadorismo é inclusão: sem um limite menor estabelecido,
esses conjuntos contêm todo comportamento abstraído. Não é análise nova da
linguagem e não depende de observedKind para deduzir opcode. Cada statement
continua contabilizado; um parent opaco e seus children não viram execução
sequencial duplicada. Se não for possível decompor com controle sustentado,
preservar coverage/origin dos filhos e semântica aberta do escopo, sem afirmar
que foram executados uma vez cada.

É possível construir **uma candidata de smoke test** apenas com o port:
objetos com associação aberta, uma sequence opaca por occurrence, entrada
abstrata DERIVED que admite todos os labels/saídas/controle externo e
assinatura/inventário de entradas explicitamente indisponíveis. Ela não afirma
que essa entrada é PROGRAM-ID/ENTRY real, nem que é a única entrada COBOL;
representa a união aberta dessas possibilidades. Toda lacuna tem escopo; todos
os labels referidos existem. [A01] admite assinatura desconhecida e origem
derivada; [A07] admite decomposição conservadora; [A05] define any_control.

Se o primeiro contrato concreto não materializar esse escopo abstrato sem
confundi-lo com entrada real, **rejeitar corpo executável** e publicar somente
inventário/body indisponível. Não escolher rootStatements[0] como entrada. A
candidata é prova de representabilidade conservadora, não decisão de schema
nem claim de implementação AIR-STRUCTURE completa. Seu consumer CFG precisa
de fronteira aberta ou resultado indisponível; não produz o diamond útil.

## Findings e owners mínimos

Prioridade P0 antecede promoção de claims; P1 habilita o primeiro fluxo útil;
P2 habilita precisão escalar/dependências; P3 é expansão posterior. A taxonomia
downstream abaixo segue [classificação de impacto]; não se rotula BLOCKS_CFG
um consumidor que ainda não foi implementado. BLOCKED na matriz qualifica a
representação precisa; REDUCES_PRECISION é sustentado pelo fallback máximo.

### F-AIR-01 — prerequisite remoto não satisfeito — P0

Evidência: main 107ce08 e PR #28 OPEN; primeira boundary pendente: validação
cross-product antes da publicação. Owner **A**, no trabalho de F-02 existente.
Menor ação: review/merge separado e revalidar a nova baseline; não cherry-pick
neste audit. Impacto: não alegar integridade integrada nem tests F-02 normais
na main. Risco de overclaim: usar o verde de outra branch como garantia atual.

```yaml
downstream_impact:
  class: NOT_APPLICABLE
  rationale: Este registro verifica o prerequisite de workflow e snapshot; não atribui um novo defeito semântico a uma execução válida nem reclassifica o finding F-02 original.
  evidence:
    - main 107ce08 e PR 28 OPEN com head 52ee4eb e mergedAt null em 2026-09-06.
```

### F-AIR-02 — readiness nominal não certifica invoke/CFG V2 — P0

Evidência: [S3] callReadiness, [S4]/[S5] e [A04] §7. Primeira boundary:
claim de suficiência no Semantic Product. Owner **A** para scopes/readiness e
contrato de avaliação/interação; **B** apenas para fallback. Menor mudança:
definir capability de CALL por interpretação/addressing/inventários/outcomes,
sem exigir runtime targets conhecidos. Impacto: CFG não pode assumir normal
único e dependency consumer não pode interpretar DATA como nome textual.
Risco: elevar bool/text ou transformar lista ausente em assinatura vazia.

```yaml
downstream_impact:
  class: BLOCKS_SEMANTIC_PRODUCT
  rationale: A claim SUFFICIENT excede os fatos do port exigidos por INV-SP-003 e invoke V2; binding nominal pode estar correto, mas a primeira garantia insuficiente já é publicada antes de IR ou CFG.
  evidence:
    - CobolSemanticProductProjector.callReadiness e CallFact sem contrato de interpretação/outcomes.
    - AIR 2.0.0 especificacao/04-operacoes.md seção 7 e O-79-STRUCT.
```

### F-AIR-03 — perda de endereçamento sob capability suportada — P0

Evidência: experimento CALL/MOVE WS-X(IX), [S1].DataReference, [S3] gates e
[S6]. Primeira boundary: frontend → port. Owner **A**. Menor mudança:
publicar shape do acesso, occurrences/roles de índices/offsets, seleção e
availability/limites; enquanto ausentes, rebaixar explicitamente a capability.
Não basta testar apenas bare references. Impacto: reads/effects e valor do
target podem estar errados se se usar o objeto inteiro. Risco: perda de IX
ocultada pelo gap de runtime/literal. F-02 verifica integridade, não essa perda.

```yaml
downstream_impact:
  class: BLOCKS_SEMANTIC_PRODUCT
  rationale: Occurrences e AST têm SUBSCRIPT IX, mas a boundary que alega suporte não o publica nem localiza essa perda; INV-SP-002/003 falha antes de qualquer consumer AIR. Um fallback aberto posterior evita subaproximação, mas não torna verdadeira a claim original.
  evidence:
    - Experimento descartável na baseline 107ce08 descrito neste relatório.
    - Ast.DataReference e gates moveCapability/callCapability versus DataReference do port.
```

### F-AIR-04 — entrada, sequência e saída executáveis não publicadas — P1

Evidência: [S2] sem Entry/signature e [S1] ProgramPoint estrutural;
[S3] achata procedure containers e continuation deriva siblings.
Primeira boundary de precisão: port → contrato de produtor [A01] §2–5.
Owner **A** para entry inventory, assinatura/availability, procedure ownership,
início/conclusão e relação de sequência semanticamente estabelecida; **B**
para IDs/labels/decomposição. Menor slice: uma forma de entrada de unit e seu
terminal, sem CFG no frontend. Impacto: nem o primeiro root nem a ausência
de continuation podem virar entry/return. Risco: confundir EOF, fim lexical
de IF e retorno normal; inventar que não há ENTRY alternativo.

```yaml
downstream_impact:
  class: REDUCES_PRECISION
  rationale: Anchors estruturais continuam corretos no escopo atual; entrada/controle indisponíveis podem ser sobreaproximados por any_control ou corpo indisponível. A perda é de CFG fechado, não prova de defeito em IR ou CFG inexistentes.
  evidence:
    - CobolSemanticPort e AIR 01 seções 2 a 5, AIR 05 seção 6, AIR 11 B-03.
```

### F-AIR-05 — terminal genérico impede eliminar fallthrough — P1

Evidência: GOBACK/STOP RUN/CONTINUE no experimento; [A04] §8–9; IBM terminal
rules abaixo. Primeira boundary de precisão: frontend → port. Owner **A**.
Menor fato: espécie/escopo de saída e condições de aplicação (root/called,
signature, INITIAL/ambiente quando pertinente), ou bound de saídas e ausência
de successor local comprovada. B escolhe return/halt/opaque compatível.
Impacto: CFG com terminal preciso é menor prerequisite que literal typing.
Risco: STOP literal não é STOP RUN; EXIT PROGRAM em main pode não sair.

```yaml
downstream_impact:
  class: REDUCES_PRECISION
  rationale: ObservedStatement declara bloqueio e preserva inventário; opaque com any_control inclui o comportamento perdido sem afirmar fallthrough. O primeiro fato específico falta no port, não no futuro CFG.
  evidence:
    - Probe de GOBACK, CONTINUE e STOP RUN; AIR 04 seção 9, AIR 05 seção 6 e O-18/O-19/O-34.
```

### F-AIR-06 — literal desconhecido não valida assign — P2

Evidência: [S3] literal UNKNOWN e [A02] §2/[A04] §2. Owner **A** para
literal/domain e regra de conversão/escrita, **B** para tradução. Primeira
boundary de precisão: port → AIR assign. Menor fato: valor em domínio
estabelecido e transformação COBOL aplicável ao destino; não apenas enum.
Impacto: candidatos A/B/C não são valores AIR correntes ainda. Risco:
sameDomain circular, PIC usado como parser de tipos e padding ignorado.

```yaml
downstream_impact:
  class: REDUCES_PRECISION
  rationale: O port já declara literal parcial; AIR exige opaque preservando source/destino quando não há prova. A abstração inclui as escritas possíveis e não exige uma atribuição inválida para manter validade.
  evidence:
    - LiteralSource e projectStatement; AIR 02 seções 1.3, 1.4 e 2; AIR 04 seção 2 e O-77.
```

### F-AIR-07 — IF estrutural não garante avaliação pura — P1 para diamond

Evidência: ConditionSurface publica somente shape/references/origem; [A07]
§5. Primeira boundary de precisão: fatos de avaliação frontend → port.
Owner **A** para resultado lógico/avaliação/limites, **B** para branch ou
opaque. Menor fato: domínio booleano e pureza/totalidade, ou envelope de
avaliação que permita manter as saídas conhecidas. ConditionSemantics precisa
pode esperar. Impacto: controlar alternativas sem decidir truth value.
Risco: afirmar remainingReads=none por lista de referências projetáveis vazia,
ou usar qualquer relation shape como prova de ausência de exceção.

```yaml
downstream_impact:
  class: REDUCES_PRECISION
  rationale: Membership e reads nominais continuam fatos corretos, e opaque aberto inclui avaliação não estabelecida. O gap bloqueia branch puro geral, mas não a conservação de alternativas por abstração.
  evidence:
    - ConditionSurface, ifReadiness, AIR 02 seção 4 e O-74-STRUCT.
```

### F-AIR-08 — storage declarativo precisa preceder precisão escalar — P2

Evidência: DATA sem associação, duração, seeds, FILLER ou alias; [A03].
Primeira boundary de precisão: fatos declarativos frontend → port. Owner
**A** para fatos COBOL declarativos; **B** para associação AIR; **C** para
resolver consequências em células/intervalos, effects e strong/weak update.
Menor slice: justificativa de associação/separação/duração de escalares
selecionados, com restante aberto nas outras classes. Impacto: desfaz a
dependência circular de um consumer que precisaria reabrir DataEntry para
descobrir layout. Risco: inferir disjunção por ID ou valor inicial zero.

```yaml
downstream_impact:
  class: REDUCES_PRECISION
  rationale: A ausência de storage é declarada e a associação unknown preserva alias aberto. Storage consumers ainda não existem; a insuficiência demonstrada é para precisão escalar, não uma falha já executada de dataflow.
  evidence:
    - DataDeclaration, INV-SP-005, AIR 03 seções 3, 4 e 8; O-12/O-14/O-78.
```

### F-AIR-09 — gaps unit/DATA e identidade compartilhada são incompletos — P2

Evidência: Gap requer statement; coverage conta statements; declarations
ancestrais recebem IDs da unit selecionada ([S3] 350–378). Owner **A** para
inventários/origins/owners mais ricos; **B** para uncertainty IDs e escopos
abertos atuais. Primeira boundary de precisão: projeção da unit no port.
Menor mudança: endpoints de gap por entidade/escopo e origem
de declaration compartilhada quando compor units. Impacto: COPY/dependência
estrutural e alias entre units não podem ser reconstruídos por nome/include
chain. Risco: confundir duas cópias nominais de declaration com duas células.

```yaml
downstream_impact:
  class: REDUCES_PRECISION
  rationale: Na publicação por unit, os joins nominais fecham. Unknown em escopo de unit/publication conserva input/storage ausentes sem inventar relações; precisão multi-unit e explicação de gaps globais exigem novos facts.
  evidence:
    - Gap, CoverageSummary, declarations e AIR 01 seções 2, 8 e 9; AIR 06 seções 1, 4 e 5.
```

### F-AIR-10 — CALL literal é ganho cedo de dependency observation — P2

Evidência: [S3] rejeita CALL_LITERAL_TARGET enquanto resolução já conhece
observação externa/literal; [A08] §6.1, O-24/O-45/O-68. Primeira boundary de
precisão: observação nominal frontend → port. Owner **A** publica
observação canônica/target/policy; **B** traduz site/recurso; **C** agrega
OBSERVED, separando catálogo e MAY_EXECUTE. Menor enrichment: literal, namespace,
policy, status interno/externo e origem por site, com resto de interação aberto.
Impacto: observações nominais úteis antes de RD/PV. Risco: afirmar artefato
externo resolvido ou todas as dependências porque há um literal.

```yaml
downstream_impact:
  class: REDUCES_PRECISION
  rationale: O literal vira observado explicitamente unsupported; opaque any_resource ainda é conservador, mas perde a observação nominal já possível no frontend. Não há consumer de dependências defeituoso a classificar.
  evidence:
    - callCapability, contrato de reference resolution, AIR 08 seção 6 e O-24/O-45.
```

## Controle, PERFORM e extensões

Regras COBOL abaixo usam Enterprise COBOL for z/OS 6.4, coerente com as fontes
do domínio atual; não são deduzidas dos fixtures. Policy inicialmente
UNSPECIFIED impede tomar opções de compilação como resolvidas.

| Construct | Correspondência AIR e fatos necessários | Decisão/prioridade |
| --- | --- | --- |
| GOBACK / EXIT PROGRAM / STOP RUN / STOP literal | IBM distingue retorno, término e suspensão; GOBACK depende do contexto, EXIT PROGRAM não sai de main, STOP literal pode retomar. AIR return/raise/halt têm escopos distintos. Publicar regra de saída aplicável e efeitos residuais. [Terminação IBM], [STOP IBM], [A04] §8 | P1: entrada + terminal mínimo antes do CFG fechado. Não juntar todas as formas num enum “terminal” com regra única. |
| GO TO direto | Destino PROCEDURE resolvido, entrada executável e natureza do salto permitem jump; não inferir label de nome nem descartar frames locais automaticamente. [GO TO IBM], [A05] §1/7.4 | Após terminal/ordem; antecede EVALUATE amplo. |
| GO TO DEPENDING ON | IBM seleciona por posição 1..n e fora da faixa continua. Precisa selector, lista posicional e next real. Pode usar dispatch/testes após domínio/conversão estabelecidos; não é diretamente label(S). [GO TO condicional IBM], [A04] §6 | Mesmo checkpoint de transfers em sub-slice próprio; default obrigatório. Sem selector semantics, opaque com destinos e continuation conservadores. |
| EVALUATE | Subjects/ALSO, testes ordenados, seleção prioritária, ranges, OTHER, alternativas e conclusão. dispatch aceita casos disjuntos por igualdade, não prioridade geral. [A04] §6, [A05] §3 | Depois de controle básico; BACKLOG-SP-001 deixa de ser automaticamente o próximo após IF. F-01 continua relevante nas shapes afetadas. |
| PERFORM inline | Body/control de TIMES/UNTIL/VARYING/AFTER, pre/post-test, capturas/atualizações/EXIT PERFORM. Pode decompor em controle do core; não precisa frame local apenas por keyword. [PERFORM IBM], [A05] §3 | F-SP-007 e facts de controle antes da precisão; não converter controls em children sem papéis. |
| PERFORM procedure/THRU | Forte correspondência com entrada, intervalo de conclusão e resume de control.local@1; ativação compartilhada e passagem ordinária pelo fim correspondem a local.boundary(port,default). Necessários targets/intervalos canônicos, completion ports, default, resume, disciplina de topo e abandonos. [PERFORM básico IBM], [A05] §7 | AIR-LOCAL-CONTROL@2 só após demonstrar equivalência; fallback de retornos possíveis é conservador, sem pareamento preciso. |
| SEARCH / SEARCH ALL | Entradas/saídas, VARYING, index updates, testes ordenados/limites e AT END precisam de fatos. ALL não autoriza algoritmo/correção de busca por mera shape. [A05] §3; domínio atual de referência | Depois de loops/seleção necessários; opaque aberto até lá. |
| ALTER / altered GO TO | Modifica destino de GO TO subsequente; requer owner do site alterável, estado inicial/restabelecimento, universo completo de labels e writes. label(S)/indirect.jump pode representar estado de controle se semântica estabelecida. [ALTER IBM], [A05] §8 | AIR-INDIRECT-CONTROL@2 posterior. Não transformar endereço/número/nome em label nem fechar S a partir do corpus. |

A correspondência PERFORM não é identidade universal: IBM proíbe PERFORM
recursivo no contrato básico consultado e exige regras de nesting de ranges,
admitindo saídas comuns e passagem normal quando não invocado. AIR admite
recursão local e verifica apenas o frame do topo. O producer precisa provar
que os programas no slice obedecem à disciplina aplicável; entrada alternativa,
saída comum e salto para fora merecem contracasos próprios. Não gerar
local.unwind a partir de intuição nem chamar qualquer EXIT de local.resume.
O-56–O-60 cobrem pareamento, portas, topo e unwind; se a regra IBM efetiva
divergir, manter opaque ou propor extensão própria, sem redefinir AIR.

No controle indireto, S é um universo semântico finito da unit, anterior a
RD/PV; valores possíveis podem refiná-lo depois. GO TO DEPENDING ON tem
selector numérico e alternativa fora da faixa: não se mapeia automaticamente
à extensão. ALTER possui estado mutável de destino e é candidato mais forte,
mas nenhum desses facts atravessa o port hoje. O-61–O-63 evitam circularidade
CFG ↔ PV. Os efeitos precisos/limites de INITIAL/THREAD/LP devem vir de A.

## Perfis e primeiro vertical slice recomendado

| Alvo | Viabilidade e escopo honesto |
| --- | --- |
| AIR-STRUCTURE@2 | Primeiro alvo. Agora: inventário/nominal e frontier CFG aberta; após entry/terminal: CFG pequeno com controle fechado. Precisa validar I-01–I-54 aplicáveis e demonstrar todos os sub-requisitos STRUCT do papel antes de claim global PFP. |
| AIR-SCALAR-FLOW@2 | Depois de acesso, tipos/conversões, associações de células, alias/separação e EntryState. Unknown_type continua permitido; não dispensa provas de cópia nem layouts/conversões pertinentes. |
| AIR-DEPENDENCY-OBSERVATION@2 | CALL literal e relações estruturais podem avançar sem dataflow; sites calculados permanecem com remainder. Herda estrutura para sites executáveis; observação não afirma execução nem catálogo completo. |
| AIR-LOCAL-CONTROL@2 | Após facts de PERFORM compatíveis e oracles O-56–O-60; não prerequisite do primeiro linear/diamond. |
| AIR-INDIRECT-CONTROL@2 | Após estado/universo de controle indireto e O-61–O-63; não prerequisite de GO TO direto. |
| AIR-REGION-FLOW@2 | Após facts de base/intervalos/unidades/codecs, alias e O-37–O-40/O-51–O-55/O-81-REGION; não exigir no primeiro CFG. |

**Primeiro slice útil recomendado:** uma unit cujo corpo seja `GOBACK`, com
inventário completo, entrada identificada e contrato de chamada/saída para o
subdomínio escolhido (sequencial, sem método/INITIAL/efeitos especiais
silenciosamente assumidos). Fixture conceitual a implementar em checkpoint A:

```cobol
       IDENTIFICATION DIVISION.
       PROGRAM-ID. AIR-FIRST.
       PROCEDURE DIVISION.
           GOBACK.
       END PROGRAM AIR-FIRST.
```

O frontend deve **publicar**, não apenas deixar conhecido ao autor do fixture:
entry/body inventory, entry start, disponibilidade/posições da assinatura e
conclusão normal da ativação sem successor local. O contrato de saída normal
para entrada raiz/chamada e efeitos residuais precisa ser estabelecido; se
`return([])` exigir mais do que o fact garante, usar opaque com saída normal
fechada e memória/dependências abertas, preservando o escopo de término
adequado. Não escolher `halt` por grafia GOBACK. São uma capability/um fixture
mínimos, nunca um limite de uma ocorrência no produto.

O lowerer cria Publication/Unit/Entry/Label/Operation próprios, origens DERIVED
para auxiliares, uncertainty IDs, scope/coverage e uma sequence terminada.
O consumer independente prova `entry → operação terminal → exit` e zero
successor local; ao adicionar outra operação observada depois do terminal, ela
continua no inventário e só é alcançável por entrada/transferência sustentada.
O oracle distingue return de halt e testa o contexto de invocação.

Precisos: identidade/origem disponível, entrada, ownership, regra de controle
e fechamento de referências. Podem ficar unknown: domínio/storage/inicialização
fora do necessário, efeitos/dependências de encerramento não delimitados e
origem da unit indisponível. Hoje o próprio GOBACK e a entrada são blockers
desse **CFG fechado**. Nenhum literal typing ou predicate é prerequisite.

Declaração inicial: **versão 2.0.0, papel e capacidades efetivamente
implementadas, alvo AIR-STRUCTURE@2, consumo conservador no escopo publicado**.
Um fixture terminal não basta para anunciar implementação completa de
AIR-STRUCTURE@2 ou PRECISE_FOR_PROFILE: [A10] §3 exige também os outros
sub-requisitos estruturais. O primeiro checkpoint deve registrar os ainda
pendentes; nunca inventar um perfil “STRUCTURE-lite@2” como se fosse normativo.

**Segundo incremento do mesmo percurso:** MOVE literal → GOBACK, depois IF
com dois ramos. MOVE pode permanecer opaque com source unknown_type, target e
escrita conservadores, mas sua continuidade normal deve ser estabelecida no
subdomínio de acesso. IF exige apenas avaliação/envelope e controle, sem
normalizar `FLAG = 1`. A seguir CALL literal permite uma observação útil de
dependência antes de qualquer fluxo escalar.

O fixture proposto na solicitação (`MOVE A; IF FLAG=1 MOVE B ELSE MOVE C;
CALL WS-X; GOBACK`) é **adequado como alvo posterior**, não como primeiro
oracle fechado atual. Além do GOBACK genérico, faltam entrada/sequência,
avaliação de IF, interpretação/outcomes de CALL e dados de conversão/storage.
Em STRUCTURE conservador, A/B/C são evidência de origem, não valores propagáveis;
em SCALAR preciso, o esperado `{B,C}` em before(call) requer escrita completa,
domínios/conversões/alias e ausência de outros caminhos influentes provados.
`PIC X(8)` também exige tratar padding antes de interpretar nomes. Não usar
`{B,C}` como oracle do primeiro audit/CFG.

## Ordem proposta de próximos checkpoints

1. **Prerequisite externo:** review/merge de F-02 e revalidação da main; não
   integra este PR. Não esperar F-02 para ler/fechar o contrato AIR.
2. **Contrato AIR/validator e oracles do slice** (`BACKLOG-IR-001`, B): fixar
   2.0.0, invariantes, TypeRef, scopes, origens, envelopes máximos e negociação.
   Definir o limite de conformidade do papel, sem criar nova semântica AIR.
3. **Enrichment mínimo de entrada e terminal** (A, BACKLOG-SP-005/SP-003):
   habilita AIR-FIRST; corrigir o alcance das claims e endereçamento perdido
   antes de alegar suporte ao respectivo MOVE/CALL (BACKLOG-SP-006).
4. **CobolLower do primeiro slice** (B, BACKLOG-LOWER-001), somente port;
   integridade AIR independente e fallback obrigatório para todo observado.
5. **CFG estrutural independente** (C, BACKLOG-CFG-001): terminal, fronteira
   aberta, ordem intrassequência, depois linear/IF. Publicações AIR construídas
   sem COBOL testam o consumer; não reparar lowerer dentro de CFG.
6. **Controle útil e observação nominal**: A/B publicam avaliação/continuações
   de IF e transferências diretas; CALL literal/contrato de recurso
   (BACKLOG-SP-007) alimenta dependency OBSERVED sem RD/PV. EVALUATE, PERFORM,
   SEARCH e ALTER entram em slices próprios na ordem da necessidade real.
7. **Precisão escalar**: A publica literal/domínio/conversão e storage
   declarativo (BACKLOG-SP-008); B normaliza; C implementa effects/storage
   (BACKLOG-DF-001). Alias/layout ausentes permanecem abertos. Não derivar esses
   fatos de DataEntry em consumer, pois ele só conhece AIR.
8. **RD → PV → dynamic CALL** (C, BACKLOG-DF-004/003/002): observar o target
   antes dos efeitos da chamada e conservar candidates/remainder. Dependency
   enumeration/catalog resolution são produtos finais separados; observação
   literal do passo 6 não precisa esperar este passo.

A sequência de **execução** do pipeline não muda. A ordem de implementação
introduz prerequisites A antes de precisão AIR e sobe terminais/GO TO antes
de EVALUATE amplo. Não requer redesenho universal: novas capabilities e seus
unknowns entram no envelope plural, com testes por occurrence e compatibilidade
de cada consumer. Não promover readiness agregada além do menor escopo provado.

## Oracles, gates e handoff

[Oracles futuros] vincula cada falsificação a checkpoint, owner e obrigação
AIR. EVAL-SP-001/002/003 continuam evidência da boundary e transporte atuais;
não são certificados AIR. Testes semânticos existentes devem permanecer
verdes mesmo com as limitações documentadas. O estado operacional de gates,
commit e PR pertence ao [work item], não a este contrato durável.

Decisão recomendada para o próximo PR: contrato AIR estrutural/validação e
oracles, seguido do enrichment mínimo entry/terminal e de lowerer/CFG em
checkpoints revisáveis. Se a prioridade humana for resultado imediato de
dependências nominais, CALL literal pode preceder diamond/controle complexo,
preservando frontier aberta. Isso altera prioridade, não elimina prerequisites
de cada claim. Nenhuma questão em aberto impede concluir este Discovery.

[A00]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/00-escopo-e-convencoes.md
[A01]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/01-modelo-e-identidades.md
[A02]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/02-tipos-valores-e-operandos.md
[A03]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/03-memoria-e-aliases.md
[A04]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/04-operacoes.md
[A05]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/05-controle-e-invocacoes.md
[A06]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/06-incompletude-e-proveniencia.md
[A07]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/07-contrato-de-produtores.md
[A08]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/08-contrato-de-consumidores.md
[A09]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/09-extensibilidade-e-compatibilidade.md
[A10]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/10-perfis-de-conformidade.md
[A11]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/11-rastreabilidade-bilateral.md
[invariantes AIR]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/conformidade/01-invariantes.md
[oracles AIR]: https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/conformidade/02-oraculos.md
[S1]: ../../src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/CobolSemanticProduct.java
[S2]: ../../src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/CobolSemanticPort.java
[S3]: ../../src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/projection/CobolSemanticProductProjector.java
[S4]: ../../src/test/java/io/github/gustavo2358/cobolexplorer/semanticproduct/loweringreadiness/SemanticPortLoweringProbe.java
[S5]: ../../src/test/java/io/github/gustavo2358/cobolexplorer/SemanticProductCheckpoint8LoweringReadinessTest.java
[S6]: ../../src/main/java/io/github/gustavo2358/cobolexplorer/Ast.java
[S7]: ../../src/main/java/io/github/gustavo2358/cobolexplorer/ExplorerMain.java
[S8]: ../../src/main/java/io/github/gustavo2358/cobolexplorer/SourceMap.java
[COBOL Semantic Product]: ../domain/cobol-semantic-product.md
[WORK-SEMANTIC-PRODUCT-002]: ../work/history/WORK-SEMANTIC-PRODUCT-002.md
[classificação de impacto]: ../engineering/downstream-impact-classification.md
[Oracles futuros]: ../evals/semantic-product-air-v2-oracles.md
[work item]: ../work/active/WORK-SEMANTIC-PRODUCT-003/state.md
[MOVE IBM]: https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=items-assigning-values-elementary-data-move
[CALL IBM]: https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-call-statement
[PERFORM IBM]: https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-perform-statement
[PERFORM básico IBM]: https://www.ibm.com/docs/en/cobol-zos/6.4?topic=statement-basic-perform
[GO TO IBM]: https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-go-statement
[GO TO condicional IBM]: https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statement-conditional-go
[ALTER IBM]: https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-alter-statement
[Terminação IBM]: https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=subprograms-ending-reentering-main-programs
[STOP IBM]: https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-stop-statement
