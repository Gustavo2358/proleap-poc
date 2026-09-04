# Semantic Product Boundary Discovery — Checkpoint 2

Data da inspeção: 2026-09-04
Work item: `WORK-SEMANTIC-PRODUCT-001`
Escopo autorizado: **Checkpoint 2 — Boundary Design and Sufficiency** somente.

Este relatório usa [`semantic-product-boundary-checkpoint-1.md`](semantic-product-boundary-checkpoint-1.md)
como baseline factual. Ele define e compara candidatas de boundary, testa sua
suficiência contra os constructs e slices observados e reavalia os findings. Não
implementa Semantic Product, `CobolLower`, IR, CFG, dataflow, serializer ou JSON,
e não executa o Checkpoint 3.

## 1. Executive Summary

### Recomendação

A menor boundary estável, depois do strongest-opponent test, é uma composição
**H — estado semântico materializado próprio (A2) exposto por uma facade/port
fechada e tipada (B)**. O estado A2 é COBOL-specific, imutável, partial-aware e
namespaced; a facade é a única superfície que o futuro `CobolLower` consome. O
lowerer recebe somente cinco capacidades coerentes:

1. superfície estrutural tipada, com ordem, nesting, operands, roles, anchors e
   estados explícitos de cobertura;
2. identidade de compilation/program unit e handles namespaced para declarations,
   occurrences e relações;
3. resultados de binding nominal, incluindo todos os candidates, status e reason,
   sem valores de runtime;
4. cobertura, incompletude, diagnostics e provenance localizada;
5. capabilities pós-binding opcionais e versionadas, como a
   `ExternalClassification` atual e futuros `ConditionSemantics`/
   `ConditionValidation`.

Essa não é uma proposta para expor as classes atuais como API, nem para criar um
schema de transporte. A2 é um modelo de domínio próprio materializado em
memória, não `Ast + SymbolTable + ReferenceResolution` dentro de um record. B é
uma superfície de leitura sobre um único estado já fechado: suas queries não
podem disparar nova análise no frontend. Adapters do frontend fazem a tradução
e o lowerer depende somente do contrato COBOL-specific, não de ANTLR, parse
tree, snapshots ou do composition root.

```text
frontend COBOL internals
  SourceMap / preprocessor / ANTLR / AST builder / indexes / tables / resolver
          │  adapter: projeção, joins, estados e uma geração coerente
          ▼
 A2 — materialized COBOL boundary state
      tipos próprios, imutáveis, partial-aware, identities namespaced
          │  facade B fechada: queries tipadas sobre estado já materializado
          ▼
CobolLower
          │  lowering futuro, sem definir sua representação neste checkpoint
          ▼
future IR
```

### Por que H é a menor boundary estável

O aggregate/record A1 transforma os produtos atuais em campos públicos; o
envelope C apenas move seus tipos e lifetimes para a fronteira. A2 é diferente:
seus tipos pertencem à boundary, são uma projeção mínima e não expõem
`Ast`, `SymbolTable` ou `ReferenceResolution`. Portanto A2 resolve closure,
coerência e intercâmbio futuro sem transformar internals em API, desde que não
seja implementado como cópia 1:1.

B puro continua atraente por manter o contrato estreito e facilitar ISP, mas uma
facade que executa lazy semantic computation ou depende do provider vivo não é
uma boundary independente. Os testes adversariais abaixo mostram que a melhor
forma plausível de B precisa ser fechada sobre um estado materializado; caso
contrário closure, lifecycle e futura prova de interchange permanecem
indeterminados. H preserva a facade como contrato externo e usa A2 como estado
fechado interno — não é um compromisso estético, mas a combinação mínima que
passa esses testes. C continua sendo apenas passagem de internals.

### Limite de suficiência

A boundary consegue transportar corretamente hoje DATA binding, targets
procedurais, `CALL` literal, binding nominal de `CALL` variável, nested units,
incompletude de COPY, provenance localizada e a classificação externa focalizada.
Ela consegue transportar os gaps de `EVALUATE`, `PERFORM`, `ALTER`, `SEARCH`,
terminais e condições como estados explícitos, mas não pode inventar os papéis
que o frontend ainda não materializa. Lowering completo desses casos permanece
bloqueado por enriquecimento do frontend, ou deve ser conservadoramente
incompleto.

**Evidence status da recomendação:** `PLAUSIBLE` como decisão de Discovery,
`STRONGLY_SUPPORTED` para os requisitos de joins, partial analysis e
provenance; a necessidade operacional de um estado fechado foi inferida pelos
testes adversariais, não implementada. Não é decisão aceita: o review humano
pode rejeitar a forma sem desfazer contrato normativo de produção.

## 2. Design Drivers

Os drivers abaixo são derivados da baseline do Checkpoint 1 e dos contratos atuais,
não de uma preferência por formato de API.

| Driver | Requirement para a boundary | Evidence status |
| --- | --- | --- |
| Semântica estrutural | O lowerer precisa da forma tipada, ordem, nesting e operands sem parse tree ou grammar contexts. | `PROVEN` — `Ast`, `semantic-ast.md`, R1/R4/R7 do CP1 |
| Joins sem texto | DATA/procedure/program references precisam ligar surface, occurrence, scope, declaration e resolution por identidade composta. | `PROVEN` — `ProgramUnitId`, `Ast.Meta.id`, `SemanticEntityId`, occurrences e resolução |
| Incerteza preservada | `AMBIGUOUS`, `UNRESOLVED`, `UNSUPPORTED`, `EXTERNAL_OBSERVED`, `DEPENDENCY_UNKNOWN`, COPY ausente e diagnostics não podem virar coleção vazia. | `PROVEN` — ADR-0008, INV-COV-001/003, EVAL-COV-003 |
| Runtime separado | Binding nominal de `CALL WS-PGM` não pode virar target final; `CALL 'XPTO'` preserva o literal observado mesmo quando linkage é desconhecido. | `PROVEN` — ADR-0004/0010, EVAL-RES-CALL-001/002 |
| Provenance útil | O lowerer e seus diagnostics precisam apontar para fonte física, span, exactness e include chain do fato, sem exigir todos os segmentos do mapa. | `STRONGLY_SUPPORTED` — ADR-0002, INV-PROV-001/002, CP1 §6 |
| Multi-unit | Nested programs exigem parentage, namespace e visibilidade por `ProgramUnitId`; uma tabela global não é suficiente. | `PROVEN` — ADR-0005, EVAL-UNIT-001, R5 |
| Produtos separados | AST, symbols, occurrences, resolution e produtos pós-binding têm ownership e lifetimes diferentes e não devem ser mutados pela boundary. | `PROVEN` — ADR-0003, INV-AST-001, INV-SYM-001 |
| Closure da análise | O lowerer deve consumir facts de um estado publicado; queries não podem manter parser, resolver ou caches mutáveis vivos nem iniciar nova análise. | `STRONGLY_SUPPORTED` — ownership/lifetime do CP1 e challenge de closure/lifecycle desta remediation |
| Contexto ausente | Compiler/build options podem não existir no corpus real; a ausência deve limitar somente facts dependentes da policy. | `STRONGLY_SUPPORTED` — CP1 §2 e `CallSemanticsTest` |
| Evolução | `ConditionSemantics`, `ConditionValidation`, classifications e extractors futuros não devem exigir mega-record nem bag sem tipos. | `PLAUSIBLE` — backlog e ADR-0012; contrato futuro ainda não implementado |
| Linguagem | A semântica antes do lowering é COBOL-specific; a neutralidade começa depois do lowerer. | `STRONGLY_SUPPORTED` — tipos, namespaces e regras IBM atuais |
| Apresentação separada | HTML, JavaScript e snapshots devem permanecer adapters, não fonte de semântica para o lowerer. | `PROVEN` — CP1 §8, INV-AST-001/002 |

## 3. Candidate Boundaries

### A — Aggregate/record materializado de produtos atuais (A1)

Um objeto imutável reúne `Ast`, units, symbols, occurrences, resolution,
coverage, diagnostics, provenance e classificações, como campos de um record.
É um modelo de composição explícito e pode fazer sentido como objeto privado de
orquestração, mas se for entregue diretamente ao lowerer transforma os produtos
atuais e seus tipos em API pública. Esta é a alternativa A originalmente
avaliada; ela não deve ser confundida com A2.

### A2 — Materialized Boundary Model

Um adapter constrói um **modelo de domínio próprio da boundary**, materializado
em memória e publicado como um estado imutável. Seus tipos carregam somente a
surface e os fatos semânticos que o lowerer precisa: identities namespaced,
joins coerentes, status de partial/incomplete analysis, provenance localizada e
capabilities pós-binding tipadas. Nenhum `Ast`, `SymbolTable`,
`ReferenceResolution`, índice ou objeto do parser atravessa a fronteira; A2
também não é um schema JSON.

A2 só permanece uma candidata forte se for uma projeção mínima, com um core
tipado pequeno e módulos/capabilities opcionais tipados. Uma cópia 1:1 da AST,
dos símbolos e da resolução seria outra forma de A1 e seria evidência contra
A2. A publicação deve ser atômica por geração de análise, para impedir que
surface de uma geração seja combinada com resolution de outra.

### B — Facade/query-oriented boundary

Um port estável oferece operações tipadas sobre capabilities sem devolver os
objetos internos. A melhor forma plausível de B é uma **facade fechada**: as
consultas selecionam e relacionam fatos já publicados no estado A2, escondem
índices, joins e ordem de construção, e não chamam AST builder, preprocessor,
resolver ou caches para completar uma resposta. Consultas são orientadas a
fatos semânticos — por exemplo, unit, node, occurrence, declaration e
resolution — e a observação é determinística e imutável.

“Query-oriented” aqui não significa uma linguagem de consulta, reflection ou
bag dinâmica. As consultas possíveis precisam ser contratos tipados, com handles
namespaced e status de ausência/incompletude explícitos.

### C — Envelope/composição dos produtos existentes

Um envelope passa `Ast`, `CompilationUnitModel`, symbol tables, occurrences,
resolution, coverage, diagnostics, provenance e produtos pós-binding como campos
separados. O lowerer continua fazendo parte dos joins ou conhece os tipos
concretos; o envelope é transparente e preserva bem o estado corrente, mas não
cria uma abstração estável sobre o frontend. C não deve ser confundido com A2:
composição de produtos internos não é modelo próprio da boundary.

## 4. Trade-off Comparison

| Critério | A — aggregate/record | B — facade/query-oriented | C — envelope/composição |
| --- | --- | --- | --- |
| Acoplamento ao frontend | Médio/alto: cada campo tende a ser um tipo atual ou uma cópia 1:1 dele. | Baixo: o port pode devolver views e handles próprios da boundary; internals ficam atrás do adapter. | Alto: o envelope normalmente publica exatamente os produtos e seus lifetimes atuais. |
| Capacidade de preservar joins | Alta se todos os produtos forem carregados, mas o consumer pode reimplementar joins em campos públicos. | Alta: joins obrigatórios podem ser encapsulados e validados na facade, sem esconder candidates ou status. | Média/alta: carrega os dados, mas deixa a coerência entre eles dependente do consumidor. |
| Incomplete analysis | Boa se o record tiver status para cada parte; ruim se campos ausentes parecerem vazios. | Forte: cada capability pode responder `available`, `partial`, `unsupported` ou `input-missing` por tipos explícitos, além da claim global. | Variável: a presença do campo não prova que o produto está completo e o envelope atual não resolve essa distinção. |
| Provenance | Boa, mas tende a carregar `SourceMap` ou duplicar metadata em muitos campos. | Forte com provenance localizada por handle; o `SourceMap` inteiro permanece interno. | Boa no estado atual, porém amarrada à forma dos `Ast.Meta` e dos snapshots. |
| Evolução/versionamento | Fraca: análise nova pressiona um mega-record e mudanças semânticas viram breaking change. | Forte: core pequeno e capabilities opcionais/versionadas; mudança semântica ganha versão própria. | Média: produtos individuais versionam, mas o envelope precisa conhecer cada nova composição. |
| Nested compilation units | Forte se o record transportar parentage e namespaces; fácil de errar usando IDs locais isolados. | Forte: unit handle é requisito central e consultas nunca aceitam ID sem namespace. | Forte enquanto todos os products estiverem presentes; fácil o consumer usar a unit primária por engano. |
| Produtos pós-binding | Campos opcionais funcionam até o record crescer; campo ausente pode ser confundido com não encontrado. | Natural: `ExternalClassification`, `ConditionSemantics` e `ConditionValidation` são ports opcionais e tipados. | Fácil anexar products atuais, mas cria dependência direta de cada classe e versão. |
| Analysis/compiler context | Pode incluir policy e evidence demais ou omitir a disponibilidade de contexto. | Expõe policy normalizada, disponibilidade e facts derivados; raw evidence é opcional e localizada. | Tende a repassar `PreprocessorEngine.Outcome` ou policy atual, expondo o composition root. |
| Facilidade de teste | Alta para snapshots do record; menor para testar cada join se o record aceita estados incoerentes. | Alta para consumer reconstruction: fakes tipados, cenários parciais e propriedades de join sem parser. | Média: testes precisam montar muitos produtos internos e reproduzir seus lifetimes. |
| Extensibilidade futura | Baixa/média; adições frequentes ao record violam OCP ou acumulam campos opcionais. | Alta: nova capacidade é um port separado; consumidores que não a conhecem continuam válidos. | Média: composição explícita pode crescer, mas cada consumer fica acoplado à lista de produtos. |
| Risco de tornar internals API | Alto se expuser classes atuais; médio se copiar todas sem política de evolução. | Baixo, desde que o port não devolva `Ast`, `ParseTree`, snapshots ou índices internos. | Alto por definição: os tipos internos são a superfície. |
| HLASM/PL/I com lowerers separados | Produz records paralelos e incentiva um falso supermodelo comum antes do lowering. | Permite ports específicos por linguagem com mesma disciplina; não exige semantic product universal. | Amarra cada lowerer à taxonomia concreta do frontend e não oferece reutilização sem vazamento. |

### Strongest-opponent comparison: A2 versus B

A tabela anterior mantém A1 e C como controles, mas não é suficiente para
decidir contra a melhor alternativa materializada. A comparação abaixo usa A2
em sua forma forte (tipos próprios, core pequeno, capabilities opcionais,
publicação atômica) e B em sua forma forte (facade fechada sobre estado já
materializado). Onde uma propriedade depende dessa condição, isso é indicado em
vez de ser atribuído automaticamente à alternativa.

| Propriedade | A2 materialized model | B facade/query port |
| --- | --- | --- |
| Acoplamento aos internals | Baixo se o adapter projeta tipos próprios; o custo fica concentrado na tradução. Alto somente se virar cópia 1:1, o que não é A2 forte. | Baixo na superfície; ainda há risco indireto se queries delegarem ao provider frontend ou retornarem views dos internals. |
| Fechamento do produto semântico | Forte: o estado imutável contém a geração publicada e não precisa de frontend vivo. | Condicional: forte sobre A2 fechado; fraco se a query executar análise lazy após o frontend “terminar”. |
| Imutabilidade | Natural no estado materializado e em suas capabilities. | Garantível no port, mas não decorre de “query” por si só; provider/caches também precisam ser snapshot-safe. |
| Preservação de joins | Forte: joins obrigatórios podem ser reconciliados uma vez na construção atômica. | Forte se encapsulados na facade; consumer não reimplementa joins, mas a coerência depende do estado/provider. |
| Nested compilation units | Forte com parentage, structural path e handles namespaced materializados. | Forte se cada query exige unit namespace; uma facade sobre IDs locais ou provider global falha. |
| Provenance | Localizada e coerente com cada anchor na geração; o mapa completo não é necessário. | Expõe a mesma provenance por views; não ganha nada ao devolver `SourceMap` inteiro e não deve depender dele em query. |
| Analysis/compiler context | Policy normalizada, availability e facts derivados congelados junto da geração. | Pode expor os mesmos facts; raw options e recomputação lazy seriam acoplamento e não requisito. |
| Partial/incomplete analysis | Forte com availability/claim/status tipados em cada capability; ausência não é coleção vazia. | Forte se cada query retornar resultado tipado com status; zero results sem esse contrato falha o teste de completeness. |
| Determinismo | Forte por publicação única, ordem definida e geração identificada. | Condicional: exige ordem definida e independência da ordem das queries; lazy caches podem violar isso. |
| Facilidade de consumer independente | Forte: value model pode ser consumido sem composição root ou lifecycle externo. | Forte para dependências e fakes, desde que o port seja fechado; uma facade sobre provider vivo não é independente. |
| Evolução/versionamento | Forte com core pequeno + capabilities próprias/versionadas; fraco se acumular tudo no record. | Forte com sub-ports/capabilities versionadas; uma interface monolítica de queries vira outro mega-modelo. |
| Capabilities pós-binding | Anexadas como produtos tipados e coerentes com a mesma geração; consumers optam por elas. | Expostas como sub-ports tipados; a facade não deve recalculá-las nem mutar nominal binding. |
| Facilidade futura de snapshot/interchange | Forte: A2 é a fonte natural de uma futura representação; não exige replay de queries. | B sozinha tem pressão arquitetural para inventar A2 atrás dela; sobre A2, snapshot/interchange pode usar o estado, não a ordem de consultas. |
| Risco de mega-model | Real se projetar toda AST/symbol/resolution 1:1; evitável pelo core mínimo e capabilities. | Real se cada fato ganhar um método no port ou se a facade virar “god interface”; ISP precisa ser mantido. |
| Risco de lifecycle/provider coupling | Baixo após a construção do estado. | Baixo somente na variante fechada; alto se guardar `AstScopeIndex`, resolver, parser, preprocessor ou caches mutáveis. |
| Facilidade de testar | Estado puro permite invariantes de consistência, geração e partial status; adapter exige testes de projeção. | Fakes de port facilitam consumer reconstruction; ainda exige teste separado de closure, ordem e provider. |
| Memória/materialização | Custo de materializar tipos/joins próprios; a evidência atual não mede esse custo nem prova que seja impeditivo. | Pode ser mais enxuta se for só view, mas sobre A2 tem custo equivalente; não há evidência para creditar uma vantagem de memória. |
| OCP/DIP | DIP pelo contrato próprio e adapters; OCP por capabilities aditivas, se o core não crescer. | DIP muito claro no consumer; OCP por sub-ports, desde que novas queries não exijam quebrar uma interface central. |
| HLASM/PL-I com lowerers separados | Permite estados próprios por linguagem; a disciplina comum fica na seam, não num modelo semântico universal. | Permite ports específicos, mas uma facade universal de queries ainda criaria falsa taxonomia comum. |

### Resultado do strongest-opponent test

A2 vence B puro em closure, lifecycle independente, determinismo publicável e
pressão de intercâmbio. B vence A2 puro em superfície mínima, ISP e facilidade
de substituir o consumer por um fake. A2 não perde por expor internals — A2
forte explicitamente não os expõe — e B não ganha closure/lifecycle de graça:
essas vantagens só existem na facade fechada.

Assim, a recomendação do relatório muda de **B sobre produtos internos** para
**H: A2 como estado semântico fechado, acessado por B como port externo**. H
não evita uma decisão: cada parte resolve um teste diferente e ambas são
necessárias para a menor boundary que seja simultaneamente independente e
estreita. O lowerer vê B; A2 não é API de internals e não é IR.

### Adversarial tests

| Teste | Resultado para B | Resultado para A2 | Consequência arquitetural |
| --- | --- | --- | --- |
| Closure após o frontend terminar | B sobrevive somente se as queries leem fatos pré-materializados; uma query que chama nova lógica do frontend falha. Não há implementação atual que prove a variante fechada. | Passa por construção se A2 for um snapshot completo da geração e não guardar provider lazy. | H exige A2 como estado de backing e proíbe semantic computation escondida na facade. |
| Lifecycle do consumer | A facade independente passa; uma implementação que retém `AstScopeIndex`, resolver, preprocessor, parser, composition root ou caches mutáveis falha. | Passa após publicação; o adapter pode morrer sem afetar o lowerer. | O contrato deve transportar estado, não lifecycle de provider. |
| Determinismo independente da ordem das queries | Possível, mas precisa de ordenação e resultados definidos no contrato; query-order-dependent lazy cache é rejeitado. | Naturalmente verificável por uma geração imutável e ordem publicada. | A2 fornece a base; B apenas seleciona sem alterar estado. |
| Completeness de zero results | Falha se `[]` for a única resposta; passa apenas com `available/partial/input-missing/unsupported` tipados. | Passa se availability e claim fizerem parte do estado/capability, sem usar vazio como sucesso. | As duas formas precisam do mesmo invariant de partial; H não permite ambiguidade semântica. |
| Pressão de snapshot/round-trip | B isolada provavelmente exigiria um segundo modelo materializado; isso é custo real e torna “facade sem estado” incompleta para interchange futuro. | É a fonte natural do snapshot, embora serializer/round-trip não pertençam a este checkpoint. | H evita inventar um segundo semantic model no CP3; a prova futura parte de A2. |
| Evolution pressure | Sub-ports opcionais passam; uma facade com todos os métodos num único contrato vira mega-interface. | Core pequeno + capabilities tipadas passa; cópia 1:1 ou mega-record falha. | A forma vencedora exige ambos os limites, não um bag dinâmico. |
| Duplication | B evita duplicar tipos na superfície, mas não elimina a necessidade de um estado fechado por trás. | A2 passa somente como projeção mínima; duplicação 1:1 é evidência contra a candidata. | O adapter deve mapear só requisitos da matriz de suficiência. |
| Consistency entre gerações | B não pode resolver essa questão se consulta providers de gerações diferentes. | Passa com publicação atômica e `analysisGeneration` compartilhada por core/capabilities. | H deve rejeitar mistura de surface e resolution de gerações distintas. |
| Cost | Não há evidência atual de que a facade ou seu backing seja mais barato. | O custo extra de materialização é uma hipótese, não finding; deve ser medido somente em trabalho autorizado. | Nenhuma decisão de performance é inventada neste Discovery. |

## 5. Recommended Boundary

### Forma mínima

A boundary recomendada é H: um estado A2 materializado e imutável, exposto por
uma facade B fechada com sub-ports tipados. A facade é o contrato consumido por
`CobolLower`; o estado A2 é a unidade de closure, consistência e futura
interchange. Conceitualmente:

```text
A2 — CobolSemanticState (boundary-owned, immutable, one analysis generation)
  ├─ core semantic state
  │    ├─ UnitSurfaceState
  │    ├─ NominalSemanticsState
  │    ├─ AnalysisState
  │    └─ LocalProvenanceState
  └─ typed optional capability states
       ├─ ExternalClassification (current)
       ├─ ConditionSemantics (future)
       ├─ ConditionValidation (future)
       └─ future classifications/extractors
          │
          ▼
B — CobolSemanticPort (closed, read-only)
  ├─ UnitSurfacePort
  │    ├─ program units, parentage, order
  │    ├─ typed surface nodes, children, operands, roles
  │    └─ node/statement anchors
  ├─ NominalSemanticsPort
  │    ├─ declaration/entity handles and scope facts
  │    ├─ written reference occurrences
  │    ├─ resolution status, reason, candidates and relations
  │    └─ call syntax/linkage facts without runtime values
  ├─ AnalysisStatePort
  │    ├─ coverage/dependency knowledge and COPY completeness
  │    ├─ diagnostics, gaps and readiness claim scope
  │    └─ normalized analysis/compiler context
  ├─ LocalProvenancePort
  │    └─ physical location, expanded location, exactness and include chain
  └─ Optional typed post-binding ports
       ├─ ExternalClassification (current)
       ├─ ConditionSemantics (future)
       ├─ ConditionValidation (future)
       └─ future classifications/extractors
```

Os nomes acima são papéis arquiteturais, não nomes de classes a implementar
neste checkpoint. O estado A2 precisa usar tipos próprios e o port B precisa
devolver apenas boundary views/handles próprios, não os tipos `Ast.*`,
`SymbolTable`, `ReferenceResolution`, `SourceMap` ou snapshots. O adapter pode
descartar o frontend depois da publicação; nenhuma query do port pode completar
um fato chamando o resolver ou outra lógica de análise.

### Requisitos do port

- **Read-only e determinístico:** a consulta não muta o estado A2 nem nenhum
  produto anterior; mesma entrada, policy e versão produz a mesma ordem e as
  mesmas identidades locais, conforme `INV-DET-001`.
- **Namespaced:** nenhum handle de unit, AST, occurrence ou entity é aceito sem
  `ProgramUnitId`/artifact context. IDs locais não são globais.
- **Join-safe:** uma resolução é encontrada por occurrence/anchor namespaced; um
  candidate é exposto completo, com `SemanticEntityId` e payload reconciliável,
  sem depender do nome escrito como chave.
- **Partial-aware:** ausência de uma capability, COPY ausente, parser recovery,
  unsupported e ambiguity aparecem como estados; uma lista vazia não significa
  “nenhum fato”.
- **Semântica, não apresentação:** a surface é tipada; texto e regra são apenas
  evidência/fidelity. HTML, JavaScript e snapshots não são retornados pelo port.
- **Sem runtime leakage:** o port publica binding nominal e valores/targets
  dinâmicos como desconhecidos quando não há análise autorizada de CFG/dataflow.
- **Compatível com produtos separados:** o port compõe views; não anota AST com
  binding nem transforma `ReferenceResolution` em um resultado de execução.
- **Fechado após publicação:** B só seleciona fatos do A2 já publicado; não
  retém `AstScopeIndex`, resolver, preprocessor, parser, composition root ou
  caches mutáveis como dependência do consumer.
- **Geração coerente:** core e capabilities carregam a mesma identidade de
  análise; uma facade não pode misturar surface, resolution e classification de
  gerações diferentes.

### Menor conjunto necessário para `CobolLower`

Para baixar um construct, o consumer precisa de `surface shape + identity +
semantic facts + uncertainty + provenance local`. Não precisa de toda a tabela de
símbolos, todos os scopes operacionais, todos os segmentos do `SourceMap` ou os
diagnostics brutos de cada fase. Quando o lowerer precisa de uma declaração, ele
consulta o declaration handle e seus atributos sem receber o índice que o
frontend usou para encontrá-la.

O port não promete que cada construct é suficiente para lowering completo. Ele
promete que, onde o frontend sabe, o fato atravessa sem perda, e onde não sabe,
a lacuna atravessa de maneira explícita e tipada. A materialização A2 não cria
semântica ausente: ela congela facts conhecidos e estados de incompletude da
mesma geração. Isso é menor e mais estável que fabricar uma shape completa para
cada lacuna.

## 6. Crosses / Does Not Cross

“Crosses” significa que o conceito atravessa a boundary em uma view/port tipada;
não significa que a classe atual de produção será exposta diretamente.

| Information | Crosses boundary? | Why / requirement do lowerer | Como a informação permanece disponível quando não cruza |
| --- | --- | --- | --- |
| AST semantic structure | YES | Reconstruir construct, ordem, nesting, operands e roles sem ANTLR. | — |
| CompilationUnit identity | YES | Namespaces, nested visibility e isolamento exigem unit/parent/structural path. | — |
| Program/unit parentage and visibility facts | YES | Lowerer não pode resolver um target com tabela global nem perder nested scope. | — |
| Symbol/declaration identity | YES | DATA/procedure/program bindings precisam de entity/declaration identity e atributos semânticos. | — |
| Full SymbolTable indexes | NO | Índices são mecanismo de lookup do frontend, não requisito do lowerer. | Declaration handles, scope facts e candidates carregam o resultado observável. |
| Reference occurrences | YES | Ligam uso escrito, role, namespace admissível, scope e anchor ao resultado de resolução. | — |
| ReferenceResolution | YES | Lowerer precisa de binding nominal e distinção entre `RESOLVED`, `AMBIGUOUS`, `UNRESOLVED`, `UNSUPPORTED` e `EXTERNAL_OBSERVED`. | — |
| Candidates/status/reasons | YES | Ambiguity, candidate sets e motivo de não decisão não podem ser descartados nem substituídos por primeiro candidato. | — |
| Declaration relations | CONDITIONAL | Cruzam quando o construct a ser baixado depende da relação declarativa; `REDEFINES`/`RENAMES` não viram layout/alias automaticamente. | Relations/unknown ficam em capability nominal com status; layout futuro é outro produto. |
| ExternalClassification | CONDITIONAL | Cruza para consumers que usam a hipótese CICS focalizada; não é requisito de todo lowerer COBOL. | O binding COBOL permanece no port nominal; ausência da capability é explícita. |
| Coverage/incompleteness | YES | Lowerer precisa distinguir ausência de fato, unsupported, input missing e dependency unknown de “nenhuma ocorrência”. | — |
| Diagnostics | YES, tipados/normalizados | Explicam por que um construct não pode ser baixado completamente e preservam estado fail-closed. | Diagnósticos puramente operacionais podem continuar em observabilidade, fora do core do port. |
| Readiness/claim scope | YES | O consumer deve saber o que `COMPLETE`/`INCOMPLETE` cobre; readiness não prova CFG/dataflow. | — |
| Localized provenance | YES | Diagnóstico, trace e fato baixado precisam apontar à origem física/expandida, exactness e include chain. | — |
| Full SourceMap | NO, por padrão | Lowering não precisa enumerar segmentos nem reconstruir cada transformação para consumir facts locais. | A localização física, exactness e include chain seguem em cada anchor; um futuro serviço de provenance pode ser capability separada se houver requisito provado. |
| CobolResolutionPolicy normalized | YES | Interpreta facts policy-dependent e audita modos `QUALIFY`, `PGMNAME`, `DYNAM`, `DLL` e suas versões. | — |
| Raw compiler options / `CompilerOption.writtenText` | NO como requisito | Raw source/config evidence não é necessária para baixar um fato já normalizado e não pode ser pré-condição quando está ausente. | Policy normalizada, availability/uncertainty e provenance localizada da diretiva, quando existe, mantêm a evidência relevante. |
| Derived facts + uncertainty | YES | É a forma mínima de o lowerer consumir linkage, external classification e condições sem acessar o algoritmo que os derivou. | — |
| `AstScopeIndex` | NO | É um índice operacional usado para produzir occurrences; expô-lo cria acoplamento ao algoritmo de coleta. | Scope/visibility facts e occurrence/resolution joins são publicados por handles. |
| `Ast.Meta.id` as public global identity | NO | O ID é local, pre-order e não é estável contra edição; não deve virar chave pública global. | Um opaque node anchor namespaced pode carregar a identidade da geração para joins. |
| Boundary node/occurrence handles | YES | O lowerer precisa de joins, mas com lifetime e namespace explícitos. | — |
| `ConditionSemantics` future | CONDITIONAL | Consumer de predicates exatos precisará da projeção pós-binding quando ela existir; não existe hoje. | Surface, occurrences, resolution e estado `NOT_PRODUCED/INCOMPLETE` ficam separados até o produto futuro. |
| `ConditionValidation` future | CONDITIONAL | Só cruza para consumer que exige admissibilidade type-sensitive; binding nominal não pode alegá-la. | `ConditionSemantics` e declaration/type facts, com validação ausente explícita, preservam o limite atual. |
| Embedded language payload | YES, opaco | Preserva construct, language tag, payload e provenance para futuras portas SQL/CICS/SQLIMS. | A semântica interna não cruza como fato COBOL; analyzer dedicado futuro produzirá capability própria. |
| Full embedded parser internals | NO | O lowerer COBOL não deve conhecer parser SQL/CICS/SQLIMS. | Payload opaco, coverage e estado `UNKNOWN/INCOMPLETE` continuam observáveis. |
| ANTLR ParseTree / contexts / token stream | NO | Não há requisito do lowerer que justifique depender da gramática ou de object identity do parser. | Surface tipada, anchors, occurrences, resolution, coverage e provenance carregam o necessário. |
| HTML snapshots / JavaScript | NO | São projections de apresentação, com cobertura e lifecycle diferentes. | O port consulta produtos de domínio diretamente; snapshots continuam adapters humanos. |
| `grammarRule` dependency | NO | Grammar rule é coverage/provenance, não papel semântico final. | O construct atravessa com kind/roles/status tipados e coverage associada. |
| `writtenText` reparsing dependency | NO | Lowerer não pode recuperar papel pelo texto nem depender de ordem implícita de descendentes. | Texto pode cruzar apenas como fidelity/evidence, nunca como fonte de semântica. |

### Consequência importante

“Não cruza” não significa “é descartado”. Significa que a informação necessária
foi transformada em um fato semanticamente suficiente ou em uma lacuna explícita.
Se não for possível fazer essa transformação, o construct não recebe status de
suficiente: o port publica a incompletude e o lowerer decide conservadoramente.

## 7. Analysis Context Decision

### Decisão proposta

O consumer precisa da combinação mínima:

1. **policy normalizada e versionada**, com `policyId`, `policyVersion`, modos
   conhecidos (`QUALIFY`, `PGMNAME`, `DYNAM`, `DLL`) e um estado explícito para
   ausência/indisponibilidade;
2. **facts derivados com uncertainty**, como `CallSemantics.linkage`, status de
   resolução, canonicalização aplicada e dependências que realmente dependem
   da policy;
3. **evidence bruta somente como suporte opcional de auditoria**, localizada e
   sem formato de `Map<String,Object>`. Ela não é pré-requisito do lowerer nem
   deve ser fabricada quando não existe.

Raw compiler evidence isolada é insuficiente: o lowerer teria de repetir
normalização e interpretar precedence. Facts derivados isolados são consumíveis,
mas a policy normalizada explica seus limites e permite versionar a regra que os
produziu. A combinação evita tanto reparse/configuration leakage quanto facts
sem contexto.

### Options ausentes não invalidam fatos independentes

Quando o corpus não contém nem fornece compiler/build options, o port publica
`policy` com os modos correspondentes como `UNSPECIFIED`/unavailable e não eleva
isso a erro global. A regra é localizar a incerteza no fato dependente:

| Caso | O que cruza | O que permanece desconhecido |
| --- | --- | --- |
| `CALL 'XPTO'`, options ausentes | target nominal observado `XPTO`; `EXTERNAL_OBSERVED`; reason `LITERAL_EXTERNAL_PROGRAM`; occurrence, unit e provenance; policy normalizada com `DYNAM/DLL=UNSPECIFIED`; `linkage=UNKNOWN`. | linkage efetivo dependente de options; nenhum candidate externo é inventado. |
| `CALL WS-PGM`, options ausentes | occurrence `PROGRAM/CALL_TARGET`; binding nominal DATA de `WS-PGM`; syntax dynamic; provenance e coverage. | valor de runtime e programa(s) final(is); policy não cria target. |
| source/COPY/provenance | unidades, surface, facts nominais independentes, gaps de COPY e provenance disponível. | declarations/decisions que dependiam do COPY ausente; claim global continua incompleta. |

Esse comportamento é exigido por `INV-COV-003`, `INV-RES-002`, `INV-RES-003` e
`ADR-0010`. O lowerer pode emitir um efeito de chamada literal observado com
linkage desconhecido; não deve bloquear a existência do target nominal por falta
de configuração nem convertê-lo em linkage estático.

### O que não será feito

O port não repassa `PreprocessorEngine.Outcome` inteiro, não exige JCL/build
metadata que o analyzer não possui e não manda o lowerer recalcular
canonicalização, linkage ou regras de visibility. Uma futura fonte confiável de
contexto pode produzir uma nova policy/capability versionada sem reescrever
retroativamente os produtos anteriores.

## 8. Semantic Sufficiency Matrix

Esta é a matriz principal de teste da candidata H. “Disponível hoje?” descreve a
baseline factual; “Boundary candidata carrega?” descreve o que o par A2+B pode
transportar, incluindo ausência e incerteza — não uma promessa de que o frontend
já conheça o fato.

| Slice | Informação necessária | Disponível hoje? | Boundary candidata carrega? | Gap | Consequência |
| --- | --- | --- | --- | --- | --- |
| DATA binding | Surface/roles de `MOVE` etc.; occurrence escrita; unit/scope; declaration/entity identity; status/candidates; provenance. | Sim, nos produtos separados. | Sim, por surface + occurrence/resolution + declaration handle. | Nenhum para binding nominal nos casos cobertos. | Lowerer pode baixar referência nominal sem lookup textual; efeitos de memória continuam fora. |
| Procedure targets | Forma `PERFORM`/`GO TO`; targets, ordem, `THRU` anchors; binding PROCEDURE por unit/scope. | Sim para targets/ranges escritos. | Sim, com roles de target e resolution status. | Não há edges, fallthrough ou expansão de execução. | Suficiente para operands/range escritos; CFG futuro continua separado. |
| CALL literal | Literal observado, syntax, occurrence, external status/reason, linkage policy-dependent, provenance. | Sim. | Sim, incluindo `EXTERNAL_OBSERVED` sem candidate. | Linkage pode ser `UNKNOWN`; não há catálogo externo/call graph. | Lowerer preserva target nominal e incerteza sem inferir target de runtime. |
| CALL variável | `DataReference`/expression, role `CALL_TARGET`, binding DATA e dynamic-target unknown. | Sim. | Sim, como binding nominal + `dynamicTarget=UNKNOWN`/coverage. | Possible-values e targets finais não existem. | Consumer pode baixar chamada dinâmica conservadora; `WS-PGM` não vira programa concreto. |
| Nested program visibility | `ProgramUnitId`, parentage, structural path, per-unit symbols/scopes e visibility result. | Sim nos casos cobertos. | Sim; nenhum query aceita ID local sem unit. | Limites de dialeto/visibilidade ainda são os do resolver atual. | Joins cross-unit são possíveis sem tabela global; gaps do resolver continuam explícitos. |
| Incomplete COPY | Missing input identity, diagnostics, copy completeness, known facts e global incomplete claim. | Sim para COPY ausente. | Sim, como coverage/input capability e facts independentes. | Declarations do COPY não disponível permanecem desconhecidas. | Lowerer usa facts seguros e não trata collection vazia como universo completo. |
| Provenance | Expanded/original location, physical file, span, exactness, include chain, anchor. | Sim localizada, inclusive COPY nested. | Sim por node/occurrence/diagnostic handle. | Necessidade do `SourceMap` completo não foi provada. | Rastreamento e diagnostics preservados; reconstrução de transformações fica fora. |
| Conditions / F-01 | Lossless condition surface; contextual occurrences; binding; future normalized predicate; status of ambiguity/error. | Surface/contexto sim; resolução combinada contém bug F-01; `ConditionSemantics` não existe. | Carrega surface e estado correto do produto disponível, mas não corrige nem oculta o bug. | Semântica combinada correta e eventual validation não estão disponíveis. | Lowerer não pode afirmar predicate exato; precisa parar/conservadoramente preservar até frontend/produto futuro ser corrigido. |
| PERFORM controls / F-SP-007 | Count, test mode, variable, FROM, BY, UNTIL, nested `AFTER` roles and boundaries. | Parcial: lista plana VALUE/CONDITION. | Carrega os itens observados e a incompletude; não cria roles ausentes. | Papéis não reconstruíveis sem grammar/text order; current frontend must be enriched for exact lowering. | Target/body/range podem baixar; control lowering completo é bloqueado ou conservador. |
| ALTER | Typed redirection/effect or explicit unsupported/preserved status and provenance. | Não; `PreservedStatement` genérico. | Sim como `PRESERVED_ONLY`/unknown effect, não como redirect semantics. | Efeito de redirecionamento não modelado. | Consumer não deve assumir fallthrough/target; exact lowering waits for frontend/CFG contract. |
| Embedded languages | Language tag, opaque payload, source/provenance, coverage/unknown; dedicated product when available. | Sim no nível opaco; sem parser de payload. | Sim opaco e partial-aware; analyzer dedicado é optional. | Host variables, embedded dependencies e effects não estão disponíveis. | COBOL lowerer conserva/encapsula o opaque construct; extractor/lowerer dedicado futuro trabalha em outra capability. |
| Post-binding classification | Classification kind/technology, certainty, covered construct/occurrences, reason, provenance and COPY completeness. | `ExternalClassification` focalizada existe; demais products não. | Conditional: port expõe a capability quando produzida e ausência tipada quando não. | Não há framework geral nem interpretações de SQL/CICS/IMS. | CICS intrinsic fact pode ser consumido sem mutar nominal binding; future products evoluem separadamente. |
| Analysis/compiler context | Normalized policy, availability, fact dependency and optional localized evidence. | Policy e facts atuais sim; external options may be absent. | Sim, sem exigir raw evidence. | Policy-dependent facts ficam menos precisos quando unavailable. | `CALL 'XPTO'` continua observável com `linkage=UNKNOWN`; análise não é invalidada. |

### Verdict da matriz

A candidata H é **suficiente como transporte semântico** para os fatos que o
frontend já estabelece e para a expressão explícita dos gaps. A2 fecha o estado
e B o torna consumível sem provider; nenhuma das duas é suficiente para
**lowering exato de todos os constructs pedidos**. Essa distinção é essencial:
uma boundary correta não converte uma lacuna de frontend em uma semântica falsa.

## 9. Control Construct Sufficiency

### Regra de consumer reconstruction

Um construct só é “suficiente” para lowering quando os papéis que o consumer
precisa estão em campos/visões tipados. O consumer não pode usar ANTLR,
`ParserRuleContext`, `grammarRule`, ordem implícita dos descendentes ou reparsear
`writtenText`. `STRUCTURALLY_SUFFICIENT` abaixo é sempre limitado aos papéis
indicados; não significa CFG, fallthrough, effects, possible-values ou runtime.

As disposições usam:

- **(a)** a boundary transporta corretamente hoje;
- **(b)** o frontend precisa ser enriquecido antes do lowering exato;
- **(c)** o consumer pode lidar conservadoramente hoje, preservando unknown/
  incomplete sem inventar fallthrough ou target;
- **(d)** ainda `UNASSESSED`: não há evidência suficiente para decidir o owner ou
  requisito da primeira boundary.

| Construct | O que a boundary carrega hoje | Suficiência limitada | Gap e disposição |
| --- | --- | --- | --- |
| `IF` | condition surface, then/else, ordem, nesting, occurrences e nominal status. | `STRUCTURALLY_SUFFICIENT` para shape/branches. | Predicate semantics geral e CFG não existem: (a) para shape, (c) para efeitos/edges; condição contextual complexa aguarda produto futuro. |
| `EVALUATE` | subjects, branches, selectors, `OTHER`, ordem e índices `ALSO`. | `PARTIALLY_STRUCTURED`. | Predicate/validation geral não existe: (a) para surface, (c) para branch não decidível; não usar posição como semântica extra. |
| `EVALUATE TRUE` | subject booleano e contexto posicional de selector quando reconhecido. | `PARTIALLY_STRUCTURED`. | F-01 torna um caso de binding incorreto: (b) para lowering exato; (c) somente se o consumer preservar o estado sem escolher branch. |
| `PERFORM paragraph` | `performKind=PROCEDURE`, `fromReference` e binding PROCEDURE. | `STRUCTURALLY_SUFFICIENT` para target escrito. | Sem CFG/efeito de chamada: (a) target; (c) execução. |
| `PERFORM THRU` | Anchors `from`/`through` e seus bindings. | `STRUCTURALLY_SUFFICIENT` para range escrito. | Expansão/fallthrough não são produto: (a) anchors; (c) execução. |
| Inline `PERFORM` | `performKind=INLINE`, body, ordem e nesting. | Suficiente para body sem control. | Controls introduzem lacunas: (a) body; (b)/(c) conforme control. |
| `PERFORM TIMES` | Control `VALUE` e expression preservada. | `PARTIALLY_STRUCTURED`. | Papel `count` não é tipado: (b) para lowering exato; (c) para preservar control unknown. |
| `PERFORM UNTIL` | Control `CONDITION` e predicate surface. | `PARTIALLY_STRUCTURED`. | Modo default/teste não está em campo: (b) para exato; (c) para predicate/status conservador. |
| `PERFORM WITH TEST BEFORE` | Predicate condition e controls. | `PARTIALLY_STRUCTURED`. | `BEFORE` depende de metadata textual/grammar: (b) para exato; (c) para não assumir modo. |
| `PERFORM WITH TEST AFTER` | Predicate condition e controls. | `PARTIALLY_STRUCTURED`. | `AFTER` não é papel tipado: (b) para exato; (c) para não assumir pre/post-test. |
| `PERFORM VARYING` | Lista plana VALUE/VALUE/VALUE/CONDITION e expressions. | `PARTIALLY_STRUCTURED`. | Variable, FROM, BY, UNTIL não têm roles: (b) para exato; (c) para transportar cadeia desconhecida. |
| `PERFORM ... AFTER ...` | A forma aparece como `performVaryingClause`; controles da cadeia são achatados. | `PARTIALLY_STRUCTURED`. | Níveis/fronteiras de cada `AFTER` não são recuperáveis: (b) para exato; (c) para não fabricar nesting. |
| `GO TO` | Kind, lista ordenada de procedure targets e bindings. | `STRUCTURALLY_SUFFICIENT` para operands. | Edges e execução são CFG: (a) operands; (c) CFG posterior. |
| `GO TO DEPENDING ON` | Kind, selector, targets e ordem. | `STRUCTURALLY_SUFFICIENT` para operands. | Possible-values/edges não existem: (a) surface; (c) selector/targets dinâmicos. |
| `ALTER` | `PreservedStatement`, texto/provenance/coverage. | `PRESERVED_ONLY`. | Redirection effect não é tipado: (b) para exato; (c) somente com effect unknown explícito. |
| `SEARCH` | `SearchStatement`, searched/varying, `SearchWhen`, ordem, conditions e branches. | `PARTIALLY_STRUCTURED`. | Validation, effects e CFG não existem: (a) shape; (c) execution/validation desconhecida. |
| `SEARCH ALL` | Shape `all=true`, keys/conditions preservadas quando reconhecidas. | `PARTIALLY_STRUCTURED`. | `ALL` não prova keys/order/equality/type compatibility: (b) para lowering/validation exatos; (c) para preservar branch unknown. |
| `NEXT SENTENCE` | `NextSentenceStatement` e fronteiras de sentence. | `PARTIALLY_STRUCTURED`. | Target/efeito do salto não é produto dedicado: (a) statement/boundaries; (b) para exato; (c) para não assumir fallthrough. |
| `EXIT PROGRAM` | `ModeledStatement` genérico; distinção depende da regra `EXIT PROGRAM?`. | `PRESERVED_ONLY`. | Terminalidade não é kind dedicado: (b) para exato; (c) como efeito terminal unknown somente se o consumer aceitar a incerteza. |
| `EXIT PARAGRAPH` | Não há node; grammar rejeita a forma atual. | `UNKNOWN`. | (b) frontend/grammar precisa produzir boundary observável; não há base para lowering atual. |
| `EXIT SECTION` | Não há node; grammar rejeita a forma atual. | `UNKNOWN`. | (b) frontend/grammar precisa produzir boundary observável; não converter erro de parse em efeito vazio. |
| `EXIT PERFORM` | Não há node nem enclosing target. | `UNKNOWN`. | (b) frontend precisa materializar kind/anchor; (c) somente como input parse-incomplete, sem semântica. |
| `GOBACK` | `ModeledStatement` genérico. | `PRESERVED_ONLY`. | Terminalidade não é tipada: (b) para exato; (c) para efeito desconhecido explícito. |
| `STOP RUN` | `ModeledStatement` genérico. | `PRESERVED_ONLY`. | Terminalidade não é tipada: (b) para exato; (c) para efeito desconhecido explícito. |
| `CALL literal` | `ProgramReference`, target literal, occurrence, `EXTERNAL_OBSERVED`, reason, policy/linkage e provenance. | `STRUCTURALLY_SUFFICIENT` para target nominal e distinção de linkage. | Linkage unknown quando options faltam e runtime externo não é catalogado: (a) target; (c) uncertainty. |
| `CALL variável` | `DataReference`, occurrence `CALL_TARGET`, binding DATA e syntax dynamic. | `STRUCTURALLY_SUFFICIENT` para binding nominal. | Target/value runtime não existe: (a) nominal; (c) dynamic remainder; possible-values é futuro. |
| Nested programs | Units, parentage, scopes, symbols, visibility e resolution namespaced. | `STRUCTURALLY_SUFFICIENT` para os casos cobertos. | Regras não cobertas permanecem statuses/gaps: (a) known visibility; (c) incomplete. |

Não há gap classificado como (d) nesta tabela: o Checkpoint 1 já produziu
evidência suficiente para separar facts transportáveis, enriquecimento necessário
e consumer conservador. (d) continua disponível para um caso novo cujo contrato
de lowerer ainda não exista; não deve ser usado para esconder uma lacuna já
demonstrada.

## 10. Partial Analysis / Provenance Model

### Partial analysis

A boundary deve transportar duas dimensões independentes:

```text
construction coverage  ≠  semantic/dependency completeness
```

`MODELED` significa que uma forma estrutural foi construída. Não significa que
seus efeitos, valores, aliases ou regras de plataforma foram interpretados.
`PRESERVED_UNINTERPRETED`, `UNSUPPORTED`, `INPUT_MISSING`,
`DEPENDENCY_UNKNOWN`, `UNRESOLVED`, `AMBIGUOUS` e `EXTERNAL_OBSERVED` continuam
observáveis no capability apropriado. Um `Status` de ausência de capability deve
ser distinto de uma lista de fatos vazia.

Para COPY ausente, o estado A2 pode conter surface, units, símbolos, occurrences
e resolução dos trechos construídos, além de `UNRESOLVED_COPY`,
`INCOMPLETE_UNRESOLVED_COPY`, gaps nominais e claim global `INCOMPLETE`. A
boundary não deve bloquear tudo porque um input externo faltou, nem alegar
completude porque uma fase conseguiu produzir alguma estrutura.

A2 deve capturar esse estado uma única vez; B não pode descobrir, em uma query
posterior, que o COPY faltante ficou disponível e então alterar o resultado.
“Zero results” só é observável junto com a availability/claim da capability.

### Provenance

O requisito mínimo que cruza é provenance localizada:

- artifact/source identity;
- localização no texto expandido quando útil;
- localização física/original;
- span e exactness;
- include chain completa, inclusive COPY nested;
- anchor do node, occurrence, diagnostic ou post-binding fact.

O `SourceMap` continua no frontend porque ele é o mecanismo de composição e
indexação de segmentos, não o fato que o lowerer consome. Se um futuro consumer
precisar reconstruir uma transformação inteira, isso será uma capability explícita
de provenance com contrato próprio; não é motivo para expor o mapa hoje. Se
necessário, essa capability também será materializada na geração A2, nunca
resolvida lazily por B.

### Invariantes de partial/provenance para implementação futura

Estes são critérios de contrato propostos, ainda não aceitos como novos
invariantes:

- todo fato que cruza tem anchor namespaced ou declara explicitamente que não é
  localizado;
- provenance aproximada nunca é apresentada como exata;
- input ausente reduz a claim e os facts dependentes, mas não apaga facts
  independentes;
- products ausentes ou não produzidos têm reason/availability, não `null`/lista
  vazia sem significado;
- nenhum consumer repara um gap por texto, grammar rule ou posição acidental.

## 11. Post-Binding Evolution

### Modelo de capabilities, não mega-record

O núcleo do estado A2 deve ser pequeno e relativamente fechado: surface, unit
identity, nominal semantics, analysis state e local provenance. Produtos que
aparecem depois do binding são capabilities separadas, com tipo, version e
identity próprias. A facade B expõe somente as capabilities que o consumer
declara; o lowerer ou outro consumer não mantém um catálogo de campos nem
consulta um provider para descobrir uma análise nova.

Isso evita duas falhas opostas:

- **mega-record:** toda nova análise modifica um contrato monolítico e obriga
  consumers antigos a conhecer campos sem relação com eles;
- **plugin bag:** um `Map<String,Object>` ou registry sem tipos desloca o
  problema para strings, casts e conflitos de runtime.

A composição futura deve usar sub-ports/interfaces de capability explicitamente
tipados, materializados na mesma geração A2 e publicados de forma atômica. Um
produto novo pode ser aditivo para consumers antigos e obrigatório somente para
um novo perfil/version do lowerer. A ausência retorna estado tipado, não `null`
nem “nenhum fato”. Assim, A2 evita um mega-record sem converter a facade em um
`Map<String,Object>`.

### Produtos atuais e futuros

| Product | Posição na evolução | Regra de composição |
| --- | --- | --- |
| `ExternalClassification` | Atual, pós-binding, focalizado em CICS intrinsic. | Capability opcional; não altera `ReferenceResolution`; carrega certainty, reason, covered construct, occurrences e provenance. |
| `ConditionSemantics` | Futuro documentado por ADR-0012. | Capability pós-binding separada, com identidade própria e anchors AST/occurrence/resolution; ausência não permite reparse nem predicate inventado. |
| `ConditionValidation` | Futuro, posterior a `ConditionSemantics`. | Capability separada e type-sensitive; pode declarar válido, inválido ou não verificável/incompleto sem mutar produtos anteriores. |
| Future classifications | Novas interpretações de plataforma/technology. | Produto typed e ortogonal ao binding COBOL, com certainty, coverage, provenance e version. |
| Future extractors | CICS/DB2/IMS/organizational facts conforme backlog autorizado. | Extractor consome a capability apropriada e produz seu próprio product; não escreve no AST/resolver nem cria dependency facts por regex. |
| CFG/dataflow products | Fases posteriores, fora deste checkpoint. | Não entram no port core; quando autorizados, receberão capabilities/ports próprios e manterão runtime-value boundary. |

`ConditionSemantics` não deve ser simulada por uma query especial que reconstrói
grammar state no `CobolLower` ou em B. Quando existir, será materializada como
capability versionada na mesma geração A2 e projetada pelo port; até lá a
boundary torna a ausência explícita. A mesma regra vale para
`ConditionValidation`, classifications e extractors futuros.

## 12. Identity / Versioning

### Identity dentro de uma análise

O lowerer precisa de handles compostos, no mínimo:

```text
artifact/analysis context
  + ProgramUnitId
  + product-local domain
  + local id / anchor
```

Isso preserva os joins atuais: `(unitId, astNodeId)`,
`(unitId, occurrenceId)` e `SemanticEntityId(unitId, domain, localId)`. A facade
deve impedir que um ID local de uma unit seja consultado em outra.

O `Ast.Meta.id` pode ser usado internamente como parte de um anchor da geração,
mas não é uma identidade global nem uma promessa de estabilidade contra edição.
`ReferenceResolution.Entry.id` também não deve ser tratado como identidade
cross-version fora da geração que o produziu.

### Versioning

Propõe-se separar três versões e uma identidade de publicação:

1. **contract version:** shape e semântica do port core;
2. **capability/product version:** interpretação e campos de um product pós-binding;
3. **analysis context/policy version:** regra que produziu facts policy-dependent.

4. **analysis generation:** identidade da publicação imutável que liga core,
   capabilities, provenance e policy. Não é uma promessa de identidade
   cross-run; serve para rejeitar mistura dentro do mesmo consumo.

Mudança aditiva em capability opcional não deve invalidar um lowerer que não a
consome. Mudança que altera o significado de um campo, candidate/status ou
provenance exige nova versão compatível/incompatível explicitamente declarada;
não há migração silenciosa. A versão do port não torna IDs resistentes a edição.
Uma facade B deve capturar uma única geração A2; se uma capability não pertence
à geração, ela é `NOT_PRODUCED`/incompatível, não uma consulta a outro provider.

### Persistência e intercâmbio

Cross-run persistence, round-trip e transporte são decisões do Checkpoint 3 ou
de um work item posterior. A boundary de Discovery é conceitualmente um modelo
A2 e port B em memória, não um schema serializado. Se a persistência futura
exigir equivalência entre versões, A2 é a fonte natural da representação e
deverá existir uma identidade semântica/migration map explícita; não se deve
inventar um segundo modelo por replay de queries nem usar `Ast.Meta.id`, posição
textual ou snapshot HTML como substituto.

## 13. Clean Architecture Seam

### Dependências propostas

```text
COBOL frontend internals
        │ adapter/builder
        ▼
A2 — CobolSemanticState (boundary-owned, immutable, one generation)
        │ read-only typed projection
        ▼
B — Cobol semantic port/contract  ←  CobolLower
        │
        └→ optional future product capabilities
CobolLower  →  future IR
```

Em termos de DIP, os tipos A2 e o port B pertencem conceitualmente ao lado da
política/use case que o `CobolLower` consome; o adapter/builder do frontend é
detalhe externo. O lowerer não importa `AstBuilder`, `ExplorerMain`, ANTLR,
`AstScopeIndex`, resolver ou snapshot writers. O composition root conhece a
montagem e descarta os providers após publicar A2, mas não vira parte do
contrato do lowerer.

### SOLID/Clean Architecture check

- **DIP:** `CobolLower` depende da abstração semântica; o frontend depende do
  contrato para fornecer a view. Nenhum use case depende do parser concreto.
- **OCP:** novos produtos pós-binding entram como capabilities tipadas; o core
  não abre branches por CICS/SQL/IMS para cada análise.
- **SRP:** AST constrói surface; symbols inventariam declarations; occurrences
  coletam usos; resolver faz binding; adapter constrói A2; facade projeta views;
  lowerer baixa; nenhum componente absorve responsabilidades futuras.
- **ISP:** sub-ports pequenos permitem que um consumer de target nominal não
  dependa de `ConditionValidation` ou de um parser embedded.
- **Clean boundary:** snapshots/HTML, `SourceMap` e os providers são detalhes de
  adapter; product facts atravessam por tipos A2 e views/handles B tipadas.

### Não criar um Semantic Product universal

A forma recomendada para outras linguagens é análoga, não idêntica:

```text
COBOL frontend → Cobol semantic port → CobolLower ┐
HLASM frontend → Hlasm semantic port → HlasmLower ├→ common IR (future)
PL/I frontend  → Pli semantic port   → PliLower  ┘
```

COBOL-specific constructs, qualification, conditions, nested program visibility
e CALL semantics permanecem no port COBOL. HLASM e PL/I podem ter outras
surfaces, identities e incompletudes. A linguagem-neutralidade começa depois do
lowering porque não há evidência para uma ontologia semântica pré-lowering única.

## 14. HLASM/PL-I Pressure Test

### Teste contra reutilização prematura

Se o record A1 fosse universalizado, ele precisaria acomodar AST/declarações/
occurrences/resolution específicas de COBOL, labels e macros de HLASM e
declarações/preprocessamento de PL/I. `PERFORM`, condition-name 88, `CALL` e
COPY não têm equivalentes obrigatórios nessas linguagens. Campos opcionais
virariam um mega-record e os lowerers ainda teriam de interpretar estados
específicos.

Com H, cada frontend fornece um estado materializado e uma facade específicos.
A disciplina comum é apenas: core pequeno, views tipadas, identities
namespaced, partial analysis, localized provenance, versioning e products
separados. Não há dependência de que HLASM/PL-I publiquem `ProgramUnitId` ou
`ReferenceResolution` com a mesma taxonomia de COBOL.

### Resultado

H passa o pressure test porque compartilha a **seam**, não o **modelo semântico
pré-lowering**: A2 e B são específicos por linguagem. A1 falharia por
acumulação de campos universais; C falharia por expor taxonomias concretas de
cada frontend ao consumer errado.

## 15. Reassessment of CP1 Findings

### Regra aplicada

Cada finding abaixo usa uma única classe primária, conforme
[`downstream-impact-classification.md`](../../engineering/downstream-impact-classification.md).
As classes novas são comparadas com a boundary recomendada neste relatório,
explicitamente ainda `Proposed` e sujeita a review. Onde a evidência não prova a
primeira fronteira, `UNASSESSED` é mantido.

### Revalidação após o strongest-opponent test

A forma da boundary mudou de B sobre produtos internos para H (A2 materializado
e próprio, exposto por B fechada), mas isso não transforma nenhum gap do CP1 em
outro tipo de impacto. As oito classes abaixo foram rechecadas contra closure,
geração coerente, partial analysis e os limites da matriz; nenhuma classificação
foi alterada nesta remediation.

| Finding | Classe mantida | Efeito da nova comparação |
| --- | --- | --- |
| F-SP-001 | `NOT_APPLICABLE` | A2 é um novo modelo de boundary, não evidência de que a ausência de um aggregate de internals seja defeito downstream. |
| F-SP-002 | `UNASSESSED` | H exige geração coerente, mas o requisito cross-run/cross-version ainda não foi provado. |
| F-SP-003 | `NOT_APPLICABLE` | A2/B continuam separados de snapshots e projections de presentation. |
| F-SP-004 | `UNASSESSED` | H transporta gaps explícitos, mas não decide o owner entre Semantic Product e CFG. |
| F-SP-007 | `BLOCKS_SEMANTIC_PRODUCT` | Materializar A2 não recupera roles de `PERFORM` ausentes; a primeira fronteira quebrada permanece a mesma. |
| F-SP-006 | `REDUCES_PRECISION` | H preserva policy/availability e facts independentes; options ausentes continuam reduzindo apenas precisão dependente. |
| F-SP-005 | `UNASSESSED` | Capabilities tipadas reduzem risco de evolução, mas ainda não existe consumer oracle futuro. |
| F-01 | `BLOCKS_SEMANTIC_PRODUCT` | A2/B não corrigem binding incorreto; carregar o erro como fato continua bloqueando o produto. |

### F-SP-001 — produtos separados e ausência de aggregate final

**Reassessment:** a ausência de aggregate de produtos internos não quebra a
boundary recomendada. H usa A2 como modelo próprio da boundary, mas isso não
reclassifica a ausência de um agregado no frontend como falha semântica
downstream. O finding continua útil como driver de composição e não descreve
uma falha semântica downstream.

```yaml
downstream_impact:
  class: NOT_APPLICABLE
  rationale: >
    A boundary recomendada é H: A2 materializa tipos próprios e B expõe o port;
    nenhum dos dois exige um aggregate público dos produtos internos. Os
    produtos atuais preservam os fatos e joins necessários para a projeção, e
    não há evidência de perda semântica causada pela ausência de um record único
    no frontend. BLOCKS_SEMANTIC_PRODUCT é rejeitado porque A2 é uma construção
    futura da boundary, não uma lacuna demonstrada no produto atual;
    REDUCES_PRECISION é rejeitado porque nenhuma informação foi demonstrada como
    perdida.
  evidence:
    - docs/history/evidence/semantic-product-boundary-checkpoint-1.md §3 e §13 (produtos separados e composição atual)
    - Este relatório §4–§5 (A2 próprio e B fechada, sem envelope de internals)
```

### F-SP-002 — joins, namespaces e lifetimes

**Reassessment:** a boundary fecha a necessidade operacional de namespaced handles,
mas a persistência cross-run/cross-version ainda não é um requisito demonstrado
do lowerer. O finding permanece `UNASSESSED` para essa dimensão durável.

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    Os joins da geração corrente são demonstrados e H exige ProgramUnitId,
    domínios locais e uma analysis generation coerente, mas ainda não há
    contrato aceito que prove se persistência cross-run/cross-version é requisito
    da primeira fronteira ou responsabilidade de um adapter posterior.
    BLOCKS_SEMANTIC_PRODUCT não é provado porque os joins correntes podem ser
    publicados em A2/B; REDUCES_PRECISION não é provado porque não há resultado
    sound demonstradamente menos preciso.
  evidence:
    - docs/history/evidence/semantic-product-boundary-checkpoint-1.md §5 e §14
    - Este relatório §6 e §12 (handles namespaced, A2 e identidade por geração)
  reassess_when:
    - semantic-product-identity-contract-accepted
    - cross-run-consumer-oracle-added
```

### F-SP-003 — projections de presentation com cobertura de unit diferente

**Reassessment:** permanece presentation-only.

```yaml
downstream_impact:
  class: NOT_APPLICABLE
  rationale: >
    A assimetria foi demonstrada em snapshots/HTML, enquanto os produtos de
    domínio e a facade proposta fazem os joins multi-unit necessários. Nenhum
    cálculo semântico do lowerer depende de uma projection primary-unit. Não há
    impacto semântico downstream dentro do escopo; uma futura UX multi-unit é
    trabalho de apresentação separado.
  evidence:
    - docs/history/evidence/semantic-product-boundary-checkpoint-1.md §8 e §13
    - docs/architecture/pipeline.md (snapshots como projections)
```

### F-SP-004 — control constructs sem efeito downstream tipado completo

**Reassessment:** permanece `UNASSESSED`. A família inclui gaps de surface,
terminalidade, salto e efeitos que podem ter a primeira responsabilidade no
Semantic Product ou no futuro CFG; o contrato de cada consumer ainda não separa
esses owners de modo demonstrável.

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    H exige roles tipados ou estado explícito de preservação,
    mas F-SP-004 agrega ALTER, terminalidade, NEXT SENTENCE, EXIT, SEARCH e
    efeitos de controle. A representação atual é genérica ou não observável;
    ainda assim, a primeira responsabilidade pode ser uma capability semântica
    do produto ou um contrato do futuro CFG, dependendo do construct. BLOCKS_IR,
    BLOCKS_CFG e BLOCKS_DATAFLOW não podem ser escolhidos sem esses contratos;
    REDUCES_PRECISION também não é provado porque alguns gaps podem mudar o
    significado. A2/B carregam o estado preservado/unknown, mas isso não
    decide o owner do enriquecimento para lowering exato.
  evidence:
    - docs/history/evidence/semantic-product-boundary-checkpoint-1.md §11–§12 (matriz de control e embedded)
    - Este relatório §8–§9 (matriz de suficiência e papéis semânticos)
    - docs/architecture/invariants.md INV-COV-001 e INV-EMB-001
  reassess_when:
    - control-construct-semantic-product-contract-defined
    - cfg-consumer-contract-defined
```

### F-SP-007 — papéis de controle de PERFORM achatados

**Reassessment:** é a especialização de F-SP-004 para a perda de roles de
`TIMES`, test mode, `VARYING` e `AFTER`.

```yaml
downstream_impact:
  class: BLOCKS_SEMANTIC_PRODUCT
  rationale: >
    O lowerer precisa distinguir count, predicate/test mode, variable/FROM/BY/
    UNTIL e níveis de AFTER. A lista atual de PerformControl com VALUE/CONDITION
    e a ordem dos descendentes não permitem reconstruir esses papéis sem
    grammarRule ou writtenText; A2/B podem transportar a perda, mas não
    podem afirmar a semântica ausente. BLOCKS_IR/CFG/DATAFLOW são rejeitados por
    não haver contratos ou falhas nessas camadas; REDUCES_PRECISION é rejeitado
    porque trocar test mode ou roles pode ser incorreto, não apenas menos preciso.
    Target, range e body continuam consumíveis separadamente.
  evidence:
    - docs/history/evidence/semantic-product-boundary-checkpoint-1.md §11 e F-SP-007
    - docs/history/evidence/semantic-product-boundary-checkpoint-1.md §13 F-SP-007
    - Este relatório §9 (PERFORM controls e consumer reconstruction)
```

### F-SP-006 — contexto de análise parcialmente retido/indisponível

**Reassessment:** a ausência de options é uma perda conservadora de precisão
somente nos facts que dependem da policy, não um bloqueio global.

```yaml
downstream_impact:
  class: REDUCES_PRECISION
  rationale: >
    Quando DYNAM/DLL/PGMNAME não estão disponíveis, o frontend preserva target
    literal, status EXTERNAL_OBSERVED, provenance e facts nominais independentes,
    mas publica linkage ou canonicalizações policy-dependent como UNKNOWN/
    UNSPECIFIED. Os oracles de CALL mostram que nenhum fato falso é afirmado e
    que a análise continua sound e conservadora; a perda é somente de precisão
    no subconjunto dependente da policy. BLOCKS_SEMANTIC_PRODUCT é rejeitado
    porque H transporta policy/availability e facts derivados; classes
    posteriores são rejeitadas porque não há produto IR/CFG/dataflow incorreto.
  evidence:
    - docs/history/evidence/semantic-product-boundary-checkpoint-1.md §2 (C1–C3 e CALL literal)
    - src/test/java/io/github/gustavo2358/cobolexplorer/CallSemanticsTest.java
    - Este relatório §7 (decision de analysis context)
```

### F-SP-005 — composição de futuros produtos pós-binding

**Reassessment:** o risco de evolução está delimitado, mas products futuros ainda
não têm contrato executável nem consumer oracle; não se força uma classe.

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    H define uma seam para ExternalClassification atual e capabilities futuras,
    mas ConditionSemantics, ConditionValidation e
    extractors adicionais ainda não existem em produção. Não há evidência para
    dizer se uma ausência futura quebrará Semantic Product, lowering ou somente
    um consumer opcional; nem há contrato de IR/CFG/dataflow que permita classes
    posteriores. BLOCKS_SEMANTIC_PRODUCT e REDUCES_PRECISION são rejeitados por
    falta de produto/consumer oracle concreto, não por alegação de que o risco
    seja inexistente.
  evidence:
    - docs/history/evidence/semantic-product-boundary-checkpoint-1.md §9 e F-SP-005
    - docs/architecture/decisions/0011-orthogonal-platform-classification.md
    - docs/architecture/decisions/0012-contextual-conditions-use-post-binding-projection.md
  reassess_when:
    - post-binding-capability-contract-accepted
    - post-binding-consumer-oracle-added
```

### F-01 — condição combinada em `EVALUATE TRUE`

**Reassessment:** a boundary torna a primeira camada demonstrável: o produto
semântico não pode publicar binding correto para o caso observado enquanto a
cadeia occurrence/resolution continua errada.

```yaml
downstream_impact:
  class: BLOCKS_SEMANTIC_PRODUCT
  rationale: >
    O caso FLAG-ON AND OTHER-ON possui surface preservada, mas a occurrence/
    resolution atual pode classificar a condition-name como DATA/{DATA} e
    terminar UNRESOLVED/INVALID_NAMESPACE_FOR_CONTEXT. H
    exige que facts nominais e futuros predicates sejam semanticamente corretos
    ou explicitamente incertos; carregar a resposta incorreta como fato não
    satisfaz esse contrato. BLOCKS_IR, BLOCKS_CFG, BLOCKS_DATAFLOW,
    BLOCKS_DEPENDENCY_FACTS e REDUCES_PRECISION são rejeitados porque a primeira
    falha demonstrada está antes dessas camadas e o bug pode mudar a interpretação,
    não apenas reduzir precisão. A surface lossless continua disponível e a
    correção permanece fora deste checkpoint.
  evidence:
    - src/test/java/io/github/gustavo2358/cobolexplorer/ConditionNameSurfaceDiscoveryTest.java
    - src/test/java/io/github/gustavo2358/cobolexplorer/ContextualConditionOccurrenceDiscoveryTest.java
    - docs/work/backlog.md BACKLOG-RES-003 e docs/architecture/decisions/0012-contextual-conditions-use-post-binding-projection.md
    - Este relatório §8–§9 (conditions/F-01 e suficiência)
```

### Summary of changed classes

| Finding | CP1 | CP2 | Motivo da mudança |
| --- | --- | --- | --- |
| F-SP-001 | `UNASSESSED` | `NOT_APPLICABLE` | H usa A2 próprio, mas não transforma a ausência de aggregate de internals em defeito downstream. |
| F-SP-002 | `UNASSESSED` | `UNASSESSED` | Joins correntes são provados; persistência durável ainda não. |
| F-SP-003 | `NOT_APPLICABLE` | `NOT_APPLICABLE` | Continua presentation-only. |
| F-SP-004 | `UNASSESSED` | `UNASSESSED` | Família heterogênea; a primeira responsabilidade Semantic Product/CFG ainda não é provada. |
| F-SP-007 | `UNASSESSED` | `BLOCKS_SEMANTIC_PRODUCT` | Roles de PERFORM demonstradamente ausentes. |
| F-SP-006 | `UNASSESSED` | `REDUCES_PRECISION` | Options ausentes preservam soundness e reduzem somente facts dependentes. |
| F-SP-005 | `UNASSESSED` | `UNASSESSED` | Products futuros sem contrato/oracle executável. |
| F-01 | `UNASSESSED` | `BLOCKS_SEMANTIC_PRODUCT` | A primeira falha demonstrada está no binding necessário ao produto. |

As classificações acima são da boundary **proposta** deste Discovery; a
remediation não alterou nenhuma classe em relação ao CP2 anterior e não são
autorização para remediar F-01, F-SP-004 ou F-SP-007.

## 16. Proposed ADRs / Invariants

Nenhum ADR ou invariant canônico foi criado ou marcado `Accepted` neste
checkpoint. As propostas abaixo são material para review e devem ser rejeitadas,
alteradas ou promovidas somente por work item/autorização posterior.

### ADR proposals — status `Proposed`, sem promoção

**P1 — Boundary semântica COBOL é estado próprio + port language-specific.** O
futuro lowerer consome uma facade de views/handles tipados sobre um A2
materializado, imutável e boundary-owned; A2 não expõe AST, symbols,
occurrences, resolution ou índices do frontend. Os produtos internos continuam
separados; snapshots e ANTLR ficam fora.

**P2 — Analysis context atravessa como policy normalizada + facts derivados.** A
ausência de compiler/build options é um estado normal de disponibilidade; só
facts policy-dependent ficam `UNKNOWN`/`UNSPECIFIED`. Raw evidence é opcional,
localizada e nunca requisito do consumer.

**P3 — Produtos pós-binding são capabilities tipadas e versionadas.**
`ExternalClassification` acompanha somente consumers que a pedem; futuros
`ConditionSemantics`, `ConditionValidation` e extractors são módulos opcionais
do estado A2 projetados por sub-ports B. Eles não entram em mega-record nem em
bag sem tipos, e não mutam produtos anteriores.

**P4 — Identity é namespaced e versionada por produto.** Joins usam unit +
domain-local identity dentro de uma geração; cross-run/cross-version não é
prometido por AST ID, posição ou snapshot, e exige contrato próprio se necessário.

**P5 — A boundary não expõe parser/presentation internals.** `ParseTree`,
grammar contexts, `AstScopeIndex`, `SourceMap` completo, HTML/JS e
`writtenText` como fonte de semântica permanecem detalhes internos/adapters.

**P6 — Publicação é atômica por analysis generation.** Core A2,
capabilities, provenance e policy observável pertencem à mesma geração; B não
consulta providers de gerações diferentes. Uma incompatibilidade é um estado
explícito, não uma mistura silenciosa.

### Invariant proposals — não promovidos

- **I1 — Surface/semantic separation:** binding, classification e futuras
  predicate products nunca são gravados na surface AST.
- **I2 — Namespaced join:** todo handle de node, occurrence, declaration,
  candidate ou post-binding fact carrega a unit necessária; ID local isolado é
  inválido na boundary.
- **I3 — Partial is observable:** capability ausente, input missing, unsupported,
  ambiguity e dependency unknown nunca são representados por empty success.
- **I4 — Local provenance:** todo fato localizado preserva source físico,
  exactness e include chain; transformações não recriam identity map.
- **I5 — No runtime inference:** nominal binding e call syntax não afirmam
  possible-values, dynamic target final, CFG ou dataflow.
- **I6 — Typed evolution:** capabilities opcionais têm contrato/version próprio;
  consumidor antigo pode ignorar capability desconhecida sem interpretar
  campos arbitrários.
- **I7 — Coherent generation:** todo fato observado pelo port pertence a uma
  analysis generation única; surface, resolution e pós-binding de gerações
  diferentes não formam um produto válido.

Essas propostas respeitam ADR-0002, ADR-0003, ADR-0004, ADR-0005, ADR-0008,
ADR-0009, ADR-0010, ADR-0011 e ADR-0012, além de INV-AST-001/002/003,
INV-SYM-001, INV-COND-001/002, INV-PROV-001/002, INV-RES-001/002/003,
INV-EXT-001/002/003/004, INV-COV-001/002/003, INV-EMB-001 e INV-DET-001.

## 17. Risks / Unknowns

| Risk/unknown | Por que permanece aberto | Guardrail para o próximo trabalho |
| --- | --- | --- |
| A2 duplicar o frontend | Os tipos próprios ainda são uma decisão de Discovery; sem implementação não há prova do tamanho da projeção. | Começar por um core mínimo ancorado na matriz; rejeitar cópia 1:1 de AST/symbol/resolution e métodos B que apenas repassem internals. |
| Facade grande demais ou lazy | As consultas exatas e perfis de lowerer ainda não foram implementados; uma facade pode virar god interface ou esconder semantic computation. | Sub-ports pequenos sobre A2 publicado; rejeitar qualquer query que retenha provider ou execute análise após a publicação. |
| Custo de materialização | A evidência atual não mede memória/tempo nem mostra que o custo seja impeditivo. | Medir apenas em trabalho autorizado; não trocar closure por uma otimização presumida. |
| Estado incoerente entre produtos | A2 ainda não tem builder/validator implementado; a geração coerente é requisito proposto. | Publicação atômica com `analysisGeneration`; rejeitar surface/resolution/capability de gerações diferentes. |
| Roles de `PERFORM` | CP1 provou flattening, mas não decidiu o desenho de enriquecimento. | Tratar F-SP-007 como bloqueio do produto para lowering exato; não recuperar por texto. |
| F-01 | A surface é lossless, porém binding combinado continua incorreto. | Corrigir somente com autorização de BACKLOG-RES-003/work item próprio; não esconder como uncertainty genérica. |
| `SourceMap` completo | Não há consumer atual que enumere todos os segmentos. | Exigir caso de uso/oracle antes de cruzar o mapa inteiro. |
| Identity cross-version | IDs atuais são determinísticos por geração, não resistentes a edição. | Checkpoint 3/work item de persistência deve definir migration/identity map separado. |
| Optional capability discovery | Named ports são preferíveis, mas a forma de composição concreta ainda não foi testada. | Proibir `Map<String,Object>`; exigir tipos, version, availability, generation e conflito determinístico. |
| Analysis options externas | A restrição de ausência veio do review e não de um metadata source real. | Não invalidar facts independentes; futura metadata deve trazer autoridade/provenance/precedence. |
| Conditions future | `ConditionSemantics`/`Validation` são documentados, não produtos atuais. | Consumer não reparseia surface para compensar capability ausente. |
| Embedded languages | Payload opaco é suficiente para preservação, não para facts internos. | Analyzer dedicado por linguagem e capability separada; regex permanece proibida. |
| Full lowering contract | IR/CFG/dataflow ainda não têm contrato e não foram desenhados aqui. | Manter consequências como gaps; não reclassificar além da primeira boundary provada. |
| HLASM/PL-I alignment | A analogia da seam ainda não foi exercitada em frontends reais. | Não criar produto universal pré-lowering; validar cada port por linguagem. |

## 18. Inputs for Checkpoint 3

O Checkpoint 3 **não foi executado nem autorizado**. Se houver autorização
posterior, estes são os inputs que devem ser levados adiante para falsificar a
candidata, sem assumir que ela foi aceita:

1. um consumer independente de `CobolSemanticPort` que não importe classes do
   frontend, ANTLR, snapshots ou `writtenText`, consumindo somente a facade B
   sobre o estado A2 publicado;
2. prova de consumer reconstruction para DATA, procedure, CALL literal/variável,
   nested units, COPY incompleto, provenance e `ExternalClassification`;
3. cenários de ambiguity, unresolved, unsupported, `EXTERNAL_OBSERVED`, options
   ausentes e facts conhecidos + unknown coexistindo;
4. challenge específico para `PERFORM TIMES`, `UNTIL`, test before/after,
   `VARYING`, nested `AFTER`, `ALTER`, `SEARCH ALL`, `NEXT SENTENCE` e EXIT;
5. verificação de que nenhum caminho do consumer usa ParseTree, grammar context,
   `grammarRule`, HTML ou reparsing de strings;
6. identidade/ordem determinística dentro de uma geração e comportamento claro
   para capability ausente/partial, incluindo rejeição de mistura de gerações;
7. eventual decisão, somente com evidência, sobre intercâmbio ou persistência,
   partindo do A2 materializado e sem criar um segundo modelo por replay de
   queries;
8. closure/lifecycle: prova de que B não retém `AstScopeIndex`, resolver,
   preprocessor, parser, composition root ou caches mutáveis.

Esses são critérios de falsificação, não autorização para criar serializer, JSON,
IR, CFG, dataflow ou Semantic Product.

## 19. Self-review obrigatório

| Pergunta | Resultado | Evidência no relatório |
| --- | --- | --- |
| Escolhi a boundary com trade-off explícito? | SIM | §§3–5, strongest-opponent table e recomendação H. |
| Provei que o lowerer não precisa de ANTLR/grammar/text reparsing? | SIM, como requisito/defesa de contrato | §6 e §13; a matriz de constructs usa reconstruction test. |
| Expliquei cada informação que cruza e que não cruza? | SIM | §6, tabela completa e consequência de “não cruza”. |
| Testei incomplete analysis e compiler-options absent? | SIM | §§7–8; COPY ausente e `CALL 'XPTO'`/`CALL WS-PGM`. |
| Testei F-SP-007/PERFORM e F-01? | SIM | §§8–9 e findings reavaliados. |
| Reavaliei todos os 8 findings? | SIM | §15, oito blocos `downstream_impact` e summary. |
| A2 foi avaliada como boundary própria, não envelope de internals? | SIM | §3, strongest-opponent table e adversarial tests. |
| B foi submetida a closure/lifecycle/determinism/completeness challenges? | SIM | §4, adversarial tests e §§5/10. |
| A2 foi submetida a evolution/duplication/consistency/cost challenges? | SIM | §4 e §17. |
| A alternativa rejeitada foi apresentada em sua forma mais forte plausível? | SIM | A2 própria e B fechada foram comparadas na tabela de strongest opponent. |
| A recomendação decorre dos trade-offs, não da anterior? | SIM | B puro perdeu closure/interchange; H foi escolhido pelo resultado adversarial. |
| Findings afetados foram revalidados? | SIM | §15, oito blocos `downstream_impact` e tabela de revalidação. |
| Evitei implementação/JSON/IR/Checkpoint 3? | SIM | escopo desta remediation, §§16 e 18. |
| Todo downstream_impact está válido? | SIM, sujeito à validação mecânica do gate | Cada bloco tem `class`, rationale e evidence; `UNASSESSED` tem `reassess_when`. |

## 20. Evidence and validation inputs

Baseline factual principal:

- `docs/history/evidence/semantic-product-boundary-checkpoint-1.md` §§1–17;
- `docs/architecture/pipeline.md`;
- `docs/architecture/invariants.md`;
- ADRs 0002, 0003, 0004, 0005, 0008, 0009, 0010, 0011 e 0012;
- `docs/domain/semantic-ast.md`, `compilation-units.md`, `symbol-model.md`,
  `reference-resolution.md` e `provenance.md`;
- `docs/engineering/downstream-impact-classification.md`,
  `semantic-analysis-policy.md`, `semantic-testing.md`, gates e catálogo de
  evals;
- `work-item.yaml`, `spec.md`, `plan.md`, `eval.md` e `state.md` de
  `WORK-SEMANTIC-PRODUCT-001`.

Nenhum probe, fixture, generated file ou arquivo em `/tmp` é parte deste
checkpoint. Nenhuma alteração em `src/main/**`, `src/test/**` ou grammar é
autorizada ou necessária.

### Validation executed

| Gate/check | Resultado |
| --- | --- |
| `./scripts/harness/check-docs.sh` (via fast/full) | `PASSED` |
| `./scripts/harness/check-architecture.sh` (via fast/full) | `PASSED` |
| `./scripts/harness/check-fast.sh` | `PASSED` |
| `./scripts/harness/check-semantic.sh` (via full) | `PASSED` |
| `./scripts/harness/check-performance.sh` | `PASSED` |
| `./scripts/harness/check-full.sh` | `PASSED` |
| `git diff --check` | `PASSED` |

### Validation da remediation

Após o challenge A2/B e as correções de lifecycle, os gates declarados no
`work-item.yaml` foram executados novamente nesta branch:

| Gate/check | Resultado |
| --- | --- |
| `./scripts/harness/check-docs.sh` | `PASSED` |
| `./scripts/harness/check-architecture.sh` | `PASSED` |
| `./scripts/harness/check-fast.sh` | `PASSED` |
| `./scripts/harness/check-semantic.sh` | `PASSED` |
| `./scripts/harness/check-full.sh` | `PASSED` |
| validator mecânico dos oito `downstream_impact` | `PASSED` — 8 records; todos com `class`, `rationale` e `evidence`; `UNASSESSED` com `reassess_when` |
| `git diff --check` | `PASSED` |

Nenhum arquivo em `src/main/**`, `src/test/**` ou grammar foi alterado. Nenhum
serializer, JSON, Semantic Product, `CobolLower`, IR, CFG ou dataflow foi
executado ou implementado.

**Checkpoint 2 status:** concluído e aguardando review humano.
**Checkpoint 3 status:** não autorizado e não iniciado.
