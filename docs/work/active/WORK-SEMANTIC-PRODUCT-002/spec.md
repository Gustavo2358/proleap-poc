# Semantic Product de produção — `MOVE` literal → `CALL` variável

## Problema

O frontend COBOL atual termina a análise com AST, compilation units, símbolos,
occurrences, resolução, cobertura, diagnostics, provenance e classificações
separados. O Checkpoint 3B provou, somente em código test-only, que o slice

```cobol
01 WS-PGM PIC X(8).
MOVE 'PGMA' TO WS-PGM.
CALL WS-PGM.
```

pode atravessar uma boundary A2+B sem carregar ANTLR, parse tree, internals do
frontend ou snapshots. Ainda não existe a mesma boundary em produção. Sem um
work item próprio, a implementação tende a promover os produtos atuais como
API, a consultar providers vivos ou a transformar o literal do `MOVE` em um
target de runtime — três violações do contrato aprovado.

Também falta uma projeção determinística para inspeção e para o desenvolvimento
isolado entre os dois bounded contexts. Ela deve existir como um adapter de
saída do frontend, na forma `CobolSemanticState` / `CobolSemanticPort` → JSON
output adapter → `semantic-product.json`. Esse artefato é um contrato interno
de transporte de desenvolvimento e inspeção: pode ser produzido pelo frontend,
copiado para um repositório independente e consumido, no futuro, por um JSON
input adapter que traduza seus fatos para o `LowererInputPort` do `CobolLower`.
Ele não é o Semantic Product, um modelo de domínio ou um formato universal
público. O core do frontend e o core do lowerer permanecem independentes de
JSON.

## Objetivo

Implementar o primeiro Semantic Product COBOL-specific de produção para esse
slice, como estado materializado em memória, imutável e fechado, exposto por uma
facade/port read-only com tipos próprios da boundary; o produto deve publicar o
binding nominal de `WS-PGM`, o literal escrito, a ordenação observada,
provenance localizada, policy/readiness e a incerteza explícita de que o target
de runtime do `CALL` continua desconhecido. Em checkpoint posterior, produzir
`semantic-product.json` por um adapter de saída separado, determinístico e
documentado, suficiente para o slice suportado e consumível sem executar o
frontend. Esse artefato servirá tanto à inspeção/debug quanto ao desenvolvimento
isolado do futuro `CobolLower`; nesse modo, um adapter JSON do repositório do
lowerer o traduzirá para seu `LowererInputPort`. Na integração final, um adapter
in-memory poderá ligar `CobolSemanticPort` ao mesmo `LowererInputPort`, sem
alterar o core de nenhum dos lados. O core do `CobolLower` não dependerá de JSON.

## Domínio de entrada suportado

O slice desta implementação é deliberadamente estreito:

- uma fonte COBOL já normalizada e preprocessada, com uma compilation unit e um
  `ProgramUnit` selecionado;
- uma declaração DATA elementar, nominalmente resolvida, com `PIC` disponível;
- um `MOVE` com uma única fonte literal e um único target DATA, nominalmente
  resolvido;
- um `CALL` cuja sintaxe seja identifier/expression e cujo operando seja a
  mesma declaração DATA nominalmente resolvida do target do `MOVE`;
- uma sequência linear observada em que o statement `MOVE` precede o `CALL`.

O caso de referência usa `WS-PGM`, `PIC X(8)`, o literal `PGMA` e a policy
realmente produzida pelo frontend. Nomes e valores podem variar dentro da mesma
classe semântica; isso não autoriza suportar outras formas de statement,
multiple targets, expressions de `MOVE`, CALL literal, nested units ou outros
constructs. O produto pode carregar a identidade namespaced da unit exigida
pelos joins, mas a aceitação deste slice não reivindica cobertura de nested
programs ou de múltiplas units.

## Classes semânticas

O produto publica somente classes de domínio necessárias para o slice:

- **surface tipada:** declaração DATA, `PIC`, literal, `MOVE`, `CALL` variável e
  seus papéis, sem usar texto como fonte de semântica;
- **identidade namespaced:** `UnitId` e `DataItemId` boundary-owned, sempre
  incluindo compilation unit/program unit e um identificador local do domínio;
- **binding nominal:** status, reason e candidate facts necessários para
  mostrar que os dois usos apontam à mesma DATA; binding não é valor;
- **relação de ordering:** program points e a relação estrita `MOVE` antes de
  `CALL`, sem afirmar reachability ou ordem de execução;
- **análise e incerteza:** binding nominal completo/incompleto, readiness do
  slice, `RuntimeTargetKnowledge.UNKNOWN` e uncertainty localizada;
- **provenance:** localização expandida e original, exactness e include chain
  para declaração, literal, `MOVE` e `CALL`;
- **analysis context mínimo:** policy COBOL normalizada, versionada e com modos
  ausentes representados como `UNSPECIFIED`, além dos facts derivados que
  explicam o limite da análise.
- **projeção de transporte separada:** `semantic-product.json` é um artefato
  posterior, produzido pelo adapter de saída do frontend a partir do estado/port
  tipado. Ele atende inspeção/debug e desenvolvimento isolado entre os bounded
  contexts; não é uma classe do Semantic Product, modelo de domínio ou formato
  universal/público.

Essas classes pertencem ao produto e não são aliases públicos de `Ast.*`,
`SymbolTable`, `ReferenceOccurrences`, `ReferenceResolution`, `SourceMap` ou
`ResolutionAnalysisReport`. O report e os produtos do frontend são entradas do
adapter; não são a boundary.

## Premissas

- A decisão do Discovery é H: A2 é o estado semântico COBOL-specific,
  materializado, próprio, imutável, partial-aware e namespaced; B é a facade/port
  fechada, tipada, read-only e orientada a fatos sobre esse estado.
- A publicação é uma unidade coerente: o port só consulta um estado A2 já
  fechado. Nenhuma query chama parser, preprocessor, resolver, índice ou cache
  mutável depois da publicação.
- AST, símbolos, occurrences e resolução continuam produtos separados e
  imutáveis. O adapter faz joins entre eles, mas não grava binding na AST nem
  altera qualquer produto anterior.
- `ProgramUnitId` é a autoridade de namespace na entrada. A boundary cria seus
  próprios handles; `Ast.Meta.id` e IDs locais do frontend não são identidades
  persistentes. `UnitId`, `DataItemId` e os demais handles publicados devem ser
  reproduzíveis em execuções equivalentes, conforme a definição de determinismo
  de transporte abaixo, mas podem mudar após edição do código, mudança estrutural,
  nova versão do analisador ou nova versão do contrato.
- O literal do `MOVE` e o binding nominal do `CALL WS-PGM` são fatos
  independentes. Mesmo quando há uma ordem linear observada, ela não é análise
  de valores.
- Provenance já chega do fonte físico através de normalização/preprocessing e
  COPY. A boundary transporta a proveniência localizada necessária, não recria
  um `SourceMap` sobre texto transformado.
- Os tipos experimentais do Checkpoint 3B são evidência executável e oracle de
  shape; não são API de produção nem devem ser importados pelo produto.
- Não há contrato de persistência nem de identidade persistente entre edições,
  mudanças estruturais ou versões para este slice. Há, porém, um contrato de
  reprodutibilidade determinística para transporte equivalente: o mesmo input,
  configuração, versão do analisador e versão do contrato devem reproduzir os
  handles transportados e a projeção observável. O contrato interno de
  transporte `semantic-product.json` é necessário apenas para o desenvolvimento
  isolado e a inspeção previstos no checkpoint posterior.

## Comportamento esperado

### Arquitetura A2 + B aprovada

O fluxo de produção deve ser:

```text
frontend COBOL já concluído
  AST / units / symbols / occurrences / resolution / report / provenance
             │
             ▼
adapter COBOL-specific de projeção e joins
             │
             ▼
A2 — estado materializado, boundary-owned e imutável
             │
             ▼
B — facade/port tipada, fechada e read-only
             │
             ▼
consumer de teste independente do slice
```

A projeção JSON é uma saída posterior e separada do produto:

```text
Frontend Core
    │ CobolSemanticPort
    ▼
JSON output adapter (checkpoint posterior)
    ▼
semantic-product.json
    ▼
JSON input adapter (futuro repositório CobolLower)
    ▼
Lowerer Input Port
    ▼
CobolLower Core
```

O JSON output adapter não pode ser uma dependência do estado ou do port. Para os
fins deste contrato, execuções equivalentes usam a mesma entrada
normalizada/preprocessada, a mesma configuração efetiva/policy, a mesma versão do
analisador e a mesma versão do contrato. Nessas condições, ele deve reproduzir
`UnitId`, `DataItemId` e os demais identificadores transportados, além da mesma
projeção observável e da mesma ordem de campos e coleções; não pode inserir
timestamp, identidade de objeto, ordem incidental de mapa ou metadata de
ambiente. O artefato deve carregar os fatos semanticamente suficientes para o
slice e preservar UNKNOWN, partial, incompleteness, provenance e ordering
observáveis.

Essa é uma propriedade de reprodutibilidade determinística do transporte, não de
identidade persistente. Ela não garante a preservação dos mesmos `UnitId`,
`DataItemId` ou outros handles depois de uma edição do código, mudança estrutural,
nova versão do analisador ou nova versão do contrato. Persistência ou migração de
identidade exigiria contrato próprio e não é definida aqui.

O futuro JSON input adapter pertence ao bounded context do `CobolLower`: ele
validará a versão/shape suportada e traduzirá o artefato para o
`LowererInputPort`, sem fazer o core conhecer JSON ou internals do frontend. Na
integração in-memory, um adapter diferente fará a mesma tradução diretamente a
partir do `CobolSemanticPort`; a troca de adapters não exige mudança no core do
frontend nem no core do lowerer. O JSON adapter de saída pode permanecer
disponível em produção para desenvolvimento local, debug, reprodução e testes,
mesmo não sendo o caminho principal de execução.

### Contrato mínimo de `semantic-product.json`

O checkpoint posterior deve documentar e testar, para o slice suportado, um
artefato que seja:

- determinístico para a mesma entrada normalizada/preprocessada, configuração
  efetiva/policy, versão do analisador e versão do contrato, inclusive na
  reprodução de `UnitId`, `DataItemId` e demais handles, na ordem de campos,
  coleções e facts observáveis;
- semanticamente suficiente para reconstruir o slice sem executar o frontend,
  sem expor seus internals e sem depender de texto reprocessado;
- versionável e documentado. Se a evolução exigir um marcador mínimo de schema
  ou versão do contrato, ele deve ser incluído e explicado sem criar um schema
  universal ou framework genérico;
- explícito sobre UNKNOWN, partial, incompleteness, ambiguity, unsupported,
  input missing, provenance e ordering, sem converter esses estados em ausência
  silenciosa ou coleção vazia;
- adequado para fixtures, testes, inspeção/debug, reprodução e desenvolvimento
  independente em repositório separado, sem exigir a execução do frontend;
- uma projeção de transporte interna entre bounded contexts, e não o modelo de
  domínio A2/B, formato público de interchange, protocolo multi-language ou
  infraestrutura de persistência.

O Checkpoint 5 é responsável somente por produzir a saída do frontend e por
provar esse contrato. Ele não implementa o repositório `CobolLower`, seu JSON
input adapter ou seu `LowererInputPort`.

O consumer de teste representa o primeiro uso downstream; `CobolLower` não é
implementado neste work item. O package da boundary não pode depender de
ANTLR, parse contexts, parser, `AstScopeIndex`, `SourceMap` completo, snapshots,
HTML, JavaScript ou composition root. Somente o adapter conhece os produtos do
frontend. A facade não é uma linguagem de consulta nem um bag dinâmico: suas
operações e resultados são tipados.

### Estado A2 mínimo

O estado materializado deve conter tipos boundary-owned equivalentes aos
seguintes papéis, sem exigir que os nomes de classes sejam iguais aos da prova
test-only:

| Papel | Conteúdo obrigatório | Regra de publicação |
| --- | --- | --- |
| `UnitId` | compilation unit identity, structural path e nome canônico da unit | nenhum handle local pode existir sem essa namespace |
| `DataItemId` | `UnitId` + identidade local própria do item DATA | deriva do candidate `DATA_SYMBOL`, não do nome escrito nem do `Ast.Meta.id` |
| declaração DATA | `DataItemId`, nome canônico, `PIC` e provenance | atributos ausentes permanecem desconhecidos/incompletos |
| literal do `MOVE` | valor semântico e provenance do literal | obtido da expression tipada; não de substring ou reparse |
| fato `MOVE` | program point, literal, target `DataItemId`, binding e provenance do statement | target precisa ser `VALUE_WRITE` e nominalmente resolvido |
| fato `CALL` | program point, operando `DataItemId`, sintaxe variável, binding, runtime target e provenance | sintaxe identifier/expression não pode virar nome de programa |
| ordering | relação estrita entre os dois program points | representa ordering observado, não CFG nem execução |
| analysis status | binding nominal, readiness/claim de escopo, `UNKNOWN` de runtime e uncertainties | unknown nunca é coleção vazia ou sucesso implícito |
| policy | `policyId`, versão e modos relevantes (`PGMNAME`, `DYNAM`, `DLL`, `QUALIFY`) | option ausente é `UNSPECIFIED`, sem invalidar fatos independentes |

O produto deve publicar o mesmo `DataItemId` no target do `MOVE` e no operando
do `CALL`. No caso provado, os dois bindings nominais são `COMPLETE`, o literal
é `PGMA`, o `MOVE` vem antes do `CALL`, e o runtime target é
`UNKNOWN` com a incerteza `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN` localizada no
`CALL`. A policy ausente pode manter `DYNAM`/`DLL`/`PGMNAME` como
`UNSPECIFIED`; ela não cria um target final.

### Regras de construção e joins

O adapter deve:

1. localizar `Ast.MoveStatement`, `Ast.CallStatement`,
   `Ast.LiteralExpression`, `Ast.DataReference` e `Ast.PictureClause` por
   traversal/shape tipada da unit;
2. obter a ocorrência de cada referência pelo par namespaced
   `(ProgramUnitId, referenceAstNodeId)` e confirmar os papéis canônicos
   `VALUE_WRITE` e `CALL_TARGET`;
3. exigir uma resolução `RESOLVED` com um único candidate DATA compatível para
   cada uso, reconciliar o `SemanticEntityId` e projetar um único
   `DataItemId` boundary-owned;
4. obter `name`, `PIC` e declaration anchor do símbolo/nó de declaração, e o
   valor do literal da AST tipada;
5. atribuir program points pela ordem semântica determinística da traversal
   publicada, sem usar `writtenText`, linha física ou `Ast.Meta.id` como prova de
   execução;
6. transportar do report os gaps e a razão de runtime desconhecido, mantendo
   binding nominal e readiness em dimensões distintas;
7. congelar todos os valores e coleções em uma publicação única antes de
   entregar o port.

Candidate, status, reason e diagnostics relevantes não podem ser descartados
quando a conclusão não for única. No caminho de aceitação feliz, o candidate
selecionado DATA é projetado como handle; nos caminhos parciais, o produto
publica o status/uncertainty e não fabrica um handle escolhido.

### Contrato da facade B

As consultas precisam permitir a um consumer independente obter, sem frontend
vivo: a unit namespaced, a declaração DATA, o fato `MOVE`, o fato `CALL`, a
relação de ordering, o analysis status, a policy e a provenance de cada anchor.
As consultas são somente leitura e não podem alterar resultados, construir
facts lazy ou depender da ordem em que forem chamadas. A facade não devolve
produtos do frontend nem expõe serializer, snapshot ou JSON. O JSON output
adapter posterior recebe somente tipos boundary-owned do state/port e fica fora
do contrato do domínio; um eventual JSON input adapter existe no bounded context
do lowerer, não no core do Semantic Product.

### Invariantes da boundary

Cada checkpoint de implementação deve proteger estes invariantes:

- **Boundary ownership:** nenhum tipo exposto pelo port pertence ao parser,
  AST, symbols, occurrences, resolver, report ou presentation.
- **Imutabilidade e closure:** state, records, listas, mapas e port são
  read-only; depois de o adapter publicar o estado, o frontend pode ser
  liberado sem invalidar uma consulta já aberta.
- **Join namespaced:** `UnitId` acompanha todos os handles; local ID isolado,
  nome escrito ou `Ast.Meta.id` isolado não resolve join na boundary.
- **Surface/semantic separation:** o adapter compõe surface e binding sem
  anotar AST, symbols, occurrences ou resolution.
- **No semantic reparsing:** `writtenText`, `grammarRule`, HTML/JS e posição
  lexical não são reprocessados para recuperar papel, binding ou ordering.
- **Nominal binding != runtime value:** `MOVE 'PGMA' TO WS-PGM` pode publicar
  literal, target nominal e ordering, mas nunca `PGMA` como target final do
  `CALL WS-PGM`.
- **Unknown/partial explícitos:** `UNKNOWN`, `INCOMPLETE`, ambiguity,
  unresolved, unsupported, input missing e dependency unknown não são
  representados por lista vazia ou sucesso completo.
- **Provenance observável:** cada declaração, literal, statement e uncertainty
  localizada mantém origem física/expandida, exactness e include chain; não há
  falsa identity map.
- **Ordering observável:** a relação publicada é determinística e verificável,
  mas não é uma aresta de CFG nem uma garantia de reachability.
- **Determinismo de transporte != identidade persistente:** em execuções
  equivalentes — mesma entrada normalizada/preprocessada, configuração
  efetiva/policy, versão do analisador e versão do contrato — os identificadores
  transportados e a ordem observável são reproduzíveis o suficiente para gerar
  JSON determinístico. Isso não torna `UnitId`, `DataItemId` ou outros handles
  identidades persistentes nem garante seus mesmos valores após edição do código,
  mudança estrutural, nova versão do analisador ou nova versão do contrato.
- **Publicação coerente:** core e capabilities da mesma consulta pertencem ao
  mesmo estado A2; persistência e migração de identidade entre publicações
  continuam fora deste slice.
- **Semantic Product != JSON:** `CobolSemanticState`/`CobolSemanticPort` são o
  produto tipado materializado em memória. `semantic-product.json` é produzido
  por um adapter de saída e é um transporte interno, determinístico, versionável
  e documentado para inspeção e desenvolvimento isolado; não é API/modelo de
  domínio, schema público ou dependência do core do frontend/lowerer. Um futuro
  JSON input adapter pode traduzi-lo para o `LowererInputPort`; a integração
  final pode substituí-lo por um adapter in-memory.

## Critérios observáveis de aceitação

O work item só pode ser considerado concluído quando todos os critérios abaixo
forem demonstrados por testes, inspeção de bytecode/fonte ou gates. Um teste
feliz isolado não substitui os critérios de boundary e de escopo.

| ID | Critério | Evidência mínima |
| --- | --- | --- |
| CA-01 | A publicação do caso de referência contém unit namespaced, declaração `WS-PGM`, `PIC X(8)`, literal `PGMA`, fatos `MOVE`/`CALL` e provenance localizada. | `SemanticProductMoveCallAdapterTest` ou equivalente, com asserts estruturais por campo. |
| CA-02 | Target do `MOVE` e operando do `CALL` têm o mesmo `DataItemId` boundary-owned, derivado de binding DATA único. | Teste de join com namespace e candidate `DATA_SYMBOL`; nenhum lookup por nome como chave. |
| CA-03 | `MOVE` antes de `CALL` é uma relação/program order explícita e determinística, sem claim de reachability, execução ou CFG. | Teste de ordering e inspeção do contrato do tipo. |
| CA-04 | `CALL` variável publica binding nominal conhecido, `runtimeTarget=UNKNOWN` e `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN` localizado no call site; `PGMA` não aparece como target de runtime. | Consumer independente e mutação adversarial que tenta promover o literal a target. |
| CA-05 | Policy ausente, ambiguity, unresolved, unsupported e input missing permanecem estados/reasons/candidates/uncertainties observáveis; nenhum vira empty success ou `COMPLETE` indevido. | Testes de contrato/adversariais sobre state e adapter, usando os enums do domínio atual. |
| CA-06 | State e port são imutáveis, a publicação é fechada e o consumer opera depois da liberação do frontend. | Teste de mutação de coleções, construção sem frontend e verificação de dependências no bytecode. |
| CA-07 | Boundary e consumer não dependem de ANTLR, parse tree, internals do frontend, snapshots, serializer, `writtenText` ou `grammarRule` para semântica. | `ArchitectureBoundaryTest`/teste focalizado de leakage e inspeção de source/bytecode. |
| CA-08 | A implementação muda somente o escopo autorizado, mantém os produtos de entrada separados e deixa todos os gates declarados verdes. | Revisão do diff, `git diff --check`, `check-docs`, `check-fast`, `check-architecture`, `check-semantic`, `check-performance` e `check-full`. |
| CA-09 | Em checkpoint posterior, o frontend produz `semantic-product.json` por adapter de saída separado, consumindo somente `CobolSemanticState`/`CobolSemanticPort`; para a mesma entrada normalizada/preprocessada, configuração efetiva/policy, versão do analisador e versão do contrato, o artefato reproduz IDs transportados e projeção determinística, sendo versionável, documentado, semanticamente suficiente para o slice e consumível sem executar o frontend. Isso não promete identidade persistente após edição, mudança estrutural ou mudança de versão. | Teste do adapter comparando bytes/valores, IDs e ordem observável; inspeção inclui UNKNOWN/partial/provenance e confirma que o JSON não entra no state/port nem no core do `CobolLower`. |
| CA-10 | O contrato deixa explícitos os dois modos: desenvolvimento isolado por JSON output adapter → artefato → futuro JSON input adapter → `LowererInputPort`, e integração final por adapter in-memory `CobolSemanticPort` → `LowererInputPort`; a troca não exige alterar os cores nem exige que frontend/lowerer estejam no mesmo repositório. | Revisão documental dos diagramas e fronteiras; nenhum repositório `CobolLower`, JSON input adapter ou `LowererInputPort` é implementado neste work item. |

O Checkpoint 1 deste work item satisfez somente a parte documental desses
critérios: contrato, oracles, plano e índice. O Checkpoint 2 implementa o core
A2 e o port B para os fatos materializados do slice e demonstra seus invariantes
por construção direta em testes, sem antecipar adapter frontend, integração,
JSON output adapter ou `semantic-product.json`. Os critérios que dependem da
projeção do frontend e dos checkpoints posteriores permanecem abertos.

## Comportamento diante de incerteza

O produto deve falhar de forma fechada e localizar a incerteza:

| Situação | Publicação obrigatória | Proibição |
| --- | --- | --- |
| ambos os usos resolvem a mesma DATA | bindings `COMPLETE`, `DataItemId` comum e readiness do slice observável | não usar nome textual como chave de reconciliação |
| `CALL` identifier/expression | operando nominal e `runtimeTarget=UNKNOWN`, com `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN` no call site | não publicar `PGMA`, catálogo de programas ou target final |
| options ausentes | policy com modos `UNSPECIFIED`; fatos independentes continuam publicados | não bloquear binding nominal nem inventar linkage |
| binding ambíguo, unresolved ou unsupported | status, reason, candidates quando houver, gap/uncertainty e claim incompleta | não selecionar primeiro candidate nem fabricar `DataItemId` |
| COPY/input ou pré-requisito ausente | fatos estruturalmente sustentados + incompletude identificável | não trocar a análise por coleções vazias ou alegar `COMPLETE` |
| forma fora do slice | `UNSUPPORTED`/`INCOMPLETE` observável e provenance disponível quando possível | não ampliar o produto silenciosamente |

O fato de o `MOVE` preceder o `CALL` é somente uma relação publicada para o
consumer deste slice. Não autoriza reaching definitions, propagação de
constante, conjunto de possible-values ou qualquer target de runtime. Esses
resultados pertencem a CFG/dataflow futuros.

## Fora de escopo

- implementar `CobolLower`, Canonical Analysis IR, CFG, dataflow, reaching
  definitions, possible-values, call graph ou dependency extraction;
- implementar o JSON output adapter ou gerar `semantic-product.json` antes do
  Checkpoint 5, que é o checkpoint posterior próprio para essa projeção;
- implementar o futuro repositório `CobolLower`, seu JSON input adapter,
  `LowererInputPort` ou o adapter in-memory de integração final;
- criar serializer/framework genérico, schema público de interchange, round-trip,
  persistência ou identidade persistente/migração entre edições, mudanças
  estruturais ou versões;
- colocar JSON dentro do Semantic Product, do `CobolSemanticState` ou do
  `CobolSemanticPort`, ou fazer o core do futuro `CobolLower` conhecer JSON. O
  contrato permite apenas o futuro JSON input adapter, fora deste work item,
  para traduzir o artefato ao `LowererInputPort`;
- generalizar a boundary para outras construções COBOL, outras linguagens,
  nested/multiple program units, `CALL` literal, `MOVE CORRESPONDING`, múltiplos
  targets, expressions de source ou control constructs;
- calcular valor de `WS-PGM`, targets possíveis ou target final de runtime por
  meio do `MOVE`, ordem linear, `VALUE`, texto ou qualquer heurística;
- alterar grammar, parser, AST, symbol tables, occurrence collector, resolver,
  `ResolutionAnalysisReport` ou seus contratos para fazer o slice passar;
- implementar `ConditionSemantics`, `ConditionValidation`, `ExternalClassification`
  adicional, embedded-language analyzer ou capabilities pós-binding novas;
- promover os tipos experimentais dos Checkpoints 3A/3B a contrato de produção;
- usar snapshots, HTML, JavaScript, `writtenText`, `grammarRule` ou
  `SourceMap` completo como fonte da boundary;
- refatorar o composition root além da menor publicação necessária no ponto
  existente da análise, ou alterar o contrato de saída dos snapshots;
- remediar F-01, F-SP-007, WORK-AST-002 ou qualquer finding/lacuna não exigido
  pelo slice.

## Regras de domínio relacionadas

- A AST é surface sem binding, derivada de contextos tipados e com IDs de
  pre-order locais: `docs/domain/semantic-ast.md`.
- Compilation units e program units fornecem namespace, parentage e fronteira
  de análise: `docs/domain/compilation-units.md`.
- Símbolos publicam declarações e entidades sem executar binding:
  `docs/domain/symbol-model.md`.
- Occurrences e resolution fazem o join nominal e preservam status/candidates:
  `docs/domain/reference-resolution.md`.
- Provenance começa no fonte físico e mantém exactness/include chain:
  `docs/domain/provenance.md`.

## ADRs/invariantes relacionados

Este trabalho executa a boundary aprovada no Discovery e preserva ADR-0002,
ADR-0003, ADR-0004, ADR-0005, ADR-0008, ADR-0009 e ADR-0010. Os invariantes
diretamente aplicáveis são INV-AST-001/002/003, INV-SYM-001,
INV-PROV-001/002, INV-RES-001/002, INV-COV-001/003 e INV-DET-001. As decisões
dos Checkpoints 2, 3A e 3B estão em:

- `docs/history/evidence/semantic-product-boundary-checkpoint-2.md`;
- `docs/history/evidence/semantic-product-boundary-checkpoint-3a.md`;
- `docs/history/evidence/semantic-product-boundary-checkpoint-3b.md`.

Este work item não reabre a escolha A2+B. Uma mudança desse contrato exigiria
novo Discovery/autorização, não ajuste incidental durante a implementação do
slice. A projeção JSON de transporte prevista no Checkpoint 5 não reabre a
escolha A2+B nem autoriza implementar o bounded context do lowerer.
