# Estado

## Onde estamos

Os seis checkpoints da migração documental e os Checkpoints 1–7 foram
executados no PR #27, que permanece sob review. O CP1 mantém o target model e o
consumer exclusivamente em `src/test`; eles continuam como especificação
executável independente.

O core A2+B de produção agora publica `Unit`, `Policy`, coleções imutáveis de
DATA e statement facts, gaps localizados e coverage/readiness. O envelope não
possui mais os singletons `move`, `call` ou `ordering`. `CobolSemanticPort`
consulta somente esse state materializado e oferece as famílias MOVE/CALL/IF e
as relações de raiz/filhos como views derivadas do inventário plural.

O CP3 removeu `CobolMoveCallAdapter` e introduziu
`semanticproduct.projection.CobolSemanticProductProjector` como seam estável de
produção. A projection recebe uma publicação fechada dos produtos canônicos e
materializa, para a `ProgramUnit` selecionada, todas as DATA entries suportadas
e todos os MOVE/CALL observados.

O CP4 acrescentou todos os IF estruturais da mesma unit ao inventário. Condition
surface, referências READ com binding DATA disponível, termination, branches,
nesting e continuation segura são projetados por identities tipadas. Filhos
diretos de IF usam `THEN(parentIf)`/`ELSE(parentIf)`.

O CP5 percorre o inventário estrutural tipado completo da `ProgramUnit` e
publica exatamente um fact para cada `Ast.Statement`: MOVE/CALL/IF sustentados
permanecem facts tipados e as demais ocorrências atravessam como
`ObservedStatement`. Statements sob PERFORM ou outra família estrutural ainda
não projetada existem no produto, mas preservam containment `UNKNOWN` e gap
localizado.

O CP6 publica o produto no composition root imediatamente após
`ResolutionAnalysisReport.compose`, quando AST/units, symbols, occurrences,
resolution, report e policy/provenance já estão fechados, e antes de snapshots
de resolution e da apresentação final. `ExplorerMain.publishSemanticProduct`
apenas reúne esses produtos em `FrontendProducts` e chama
`CobolSemanticProductProjector.open`; não reconstrói produto canônico nem contém
regra de projeção.

No mesmo fluxo, somente o `CobolSemanticPort` materializado é entregue a
`CobolLoweringReadinessConsumer`. O consumer produz um audit imutável e tipado,
não retém o port e não depende de frontend, projection, snapshots, presentation
ou composition root.

O CP7 acrescentou `semanticproduct.transport.SemanticProductJsonWriter` como
output adapter separado e fez o composition root escrever
`semantic-product.json` diretamente do `CobolSemanticPort`. O envelope
`cobol-semantic-product` usa `contractVersion` `1.0.0`, inventário de statements
com discriminador tipado e DTOs de transporte; não depende do audit do CP6 nem
reabre produtos do frontend.

## Verde conhecido

- `CobolSemanticProduct.State` é fechado, imutável e namespaced por
  `UnitId`; DATA, statement, operand e candidate possuem identities próprias.
- O inventário tipado usa `StatementFact` com `MoveFact`, `CallFact`, `IfFact`
  estrutural e `ObservedStatement` genérico. Cada statement alcançável por
  `Ast.children` na unit recebe um único fact, anchor/provenance e
  `ProgramPoint` estrutural; a sequência completa é `0..N-1`. Uma nova família
  não altera o envelope do state e uma variante AST desconhecida falha fechada.
- Containment flat/híbrido preserva `ROOT`, `THEN`, `ELSE`, `UNKNOWN`, nesting e
  continuation por identity. `UNKNOWN` identifica uma ocorrência sabidamente
  aninhada cuja relação de parent/branch ainda não foi projetada; não pertence
  aos roots e exige coverage parcial e gap estrutural localizado. Branches
  vazias são consultáveis sem afirmar se havia ELSE sintaticamente vazio ou
  ELSE ausente e sem publicar CFG.
- `NominalBinding` representa `RESOLVED`, `AMBIGUOUS`, `UNRESOLVED` e
  `INPUT_MISSING`; somente o caso resolvido único possui `selected`.
  Operand/reference identity continua existindo nos demais casos.
- Candidates e facts não podem cruzar namespace, e todo candidate publicado
  precisa ter declaration correspondente na publicação fechada.
- Coverage individual distingue `MODELED`, `PARTIAL`, `UNSUPPORTED` e
  `INPUT_MISSING`. A summary satisfaz a igualdade entre statements observados e
  a soma de modeled, partial, unsupported e input-missing, corresponde aos facts
  individuais, distingue zero statements de inventário indisponível e não pode
  elevar lowering, CFG ou effects/dataflow acima do fact individual mais fraco
  ou do status do inventário.
- CALL variável continua com runtime target `UNKNOWN` e gap localizado;
  binding nominal não se torna target de runtime nem storage identity.
- `LiteralSource` preserva `LiteralKind` no core. Um teste direto atravessa o
  port com numeric `1` e alphanumeric `'1'`, ambos com value `"1"`, e
  prova que continuam semanticamente distinguíveis sem AST, raw text ou PIC.
- A projection percorre deterministicamente as coleções canônicas, sem
  `single(...)`, `findFirst()`, primeiro/último match, filtro que descarte
  statements ou pairing MOVE→CALL. Os joins continuam indexados; completar o
  inventário acrescenta somente passes lineares sobre nodes/facts.
- Cada MOVE/CALL publicado retém identity, program point estrutural, operands,
  roles, binding, candidates e provenance próprios. Ambiguidade nominal mantém
  todos os candidates sem selecionar um deles; candidate de namespace inválido
  faz a projection falhar fechada.
- Cada IF suportado recebe um `IfFact` no mesmo inventário, inclusive IF nested.
  A condition preserva shape tipado, provenance e todas as referências DATA
  READ disponíveis com status/reason/candidates/selected canônicos. Binding
  ambíguo não apaga IF, children nem continuation.
- Continuação de IF é um fact estrutural, não edge: o projector usa o próximo
  sibling tipado ou a continuação do IF enclosing. Como o CP5 publica todo
  sibling observado, um DISPLAY posterior deixa de causar
  `CONTINUATION_NOT_PROJECTED`; o gap permanece somente quando o IF termina sob
  uma família estrutural cuja continuação ainda não é projetável.
- Completude dos branches é independente da continuation. DISPLAY/PERFORM ou
  outro filho direto observado agora pertence explicitamente ao THEN/ELSE e não
  mantém `BRANCH_CONTENT_NOT_PROJECTED`; uma lista AST realmente vazia continua
  sendo ramo vazio. Isso não modela a semântica do filho nem elimina
  `CONDITION_SEMANTICS_NOT_AVAILABLE` do IF.
- Ramo falso vazio continua sendo uma coleção vazia com continuation
  conservadora. `explicitlyTerminated` preserva apenas termination do IF; não há
  `elsePresent`, `hasElse` ou tentativa de distinguir ELSE ausente de ELSE vazio.
- Reasons resolvidas atravessam da resolution sem perda: tanto
  `UNIQUE_VISIBLE_DECLARATION` quanto `QUALIFIED_HIERARCHY_MATCH` permanecem
  distintos no `NominalBinding` da boundary.
- DATA vem da symbol table canônica e seus atributos/provenance/coverage vêm da
  declaração AST e do report de coverage já publicados. Declarations necessárias
  à closure de candidates também atravessam por identity canônica, inclusive
  quando pertencem a uma unit ancestral visível.
- O `ResolutionAnalysisReport` é input obrigatório e autoridade para gaps de
  binding e CALL dinâmico, além dos claims agregados que ele efetivamente
  publica. A projection reconcilia report, occurrences e resolution e falha
  fechada diante de divergência; não recalcula binding nem runtime target.
- MOVE/CALL observados mas fora das shapes iniciais atravessam como
  `ObservedStatement` com coverage/gap localizado. CALL variável continua com
  runtime target `UNKNOWN`; argumentos, `RETURNING` e exception flow ainda não
  representáveis no fact são mantidos como incompletude explícita.
- EVALUATE, PERFORM, GO TO, SEARCH, embedded language e NEXT SENTENCE preservam
  kind/shape que a variante AST tipada sustenta. `ModeledStatement`,
  `PreservedStatement` e `UnsupportedStatement` permanecem deliberadamente
  genéricos: seus campos textuais não são usados como uma segunda parser engine.
  Todo `ObservedStatement` recebe coverage conservadora e gap de capability
  correspondente; containment desconhecido acrescenta gap estrutural.
- Como a AST canônica ainda não publica literal kind tipado, todo MOVE literal
  do CP3 usa `LiteralKind.UNKNOWN`, coverage/readiness conservadoras e o gap
  localizado `LITERAL_KIND_NOT_PUBLISHED`, sem inferência por `rawLexeme`, value
  ou PIC.
- O gate de arquitetura inspeciona o pacote de projection e proíbe dependência
  em builders/collectors/resolvers, `SourceMap`, ANTLR, snapshots e
  presentation. No CP6, ele também inspeciona source e bytecode do consumer:
  somente o port, os facts do core e os tipos próprios são permitidos;
  `writtenText` e `grammarRule` também permanecem proibidos. O oracle CP1
  continua independente da implementação.
- O consumer do CP6 percorre exclusivamente o port e reconstrói Unit, policy,
  DATA, todos os statements, program points, roots, containment, branches,
  nesting e continuation. MOVE retém literal/target/role/binding; CALL retém
  operand/binding/runtime `UNKNOWN`; IF retém condition surface/references e
  membership THEN/ELSE; `ObservedStatement` retém existência, kind/shape,
  coverage e gap. Provenance, coverage, gaps e readiness são copiados sem
  promoção ou nova interpretação COBOL.
- O JSON v1 transporta unit, policy, DATA, statement headers, MOVE/CALL/IF e
  `ObservedStatement`, roots/branches/containment/continuation, operands,
  bindings, condition surface, runtime unknowns, gaps, coverage, readiness e
  provenance. Campos opcionais indisponíveis permanecem explicitamente `null`.
- DATA, statements, candidates, gaps e include chains mantêm a ordem publicada.
  Branch relations seguem IFs em program point canônico, sempre THEN antes de
  ELSE, e children mantêm a ordem do port. A materialização e a escrita são
  lineares no tamanho do produto e não usam bag `Map<String,Object>`.
- Handles JSON são unit-scoped (`data:*`, `statement:*`, `operand:*`) e
  reproduzíveis para publicações equivalentes. Eles não são identidade
  persistente e nenhum contrato preserva IDs após edição estrutural, mudança de
  analyzer ou mudança de versão do transporte.
- O oracle do CP7 parseia o documento com uma biblioteca JSON genérica, prova o
  fixture de 3 DATA e 14 statements, integridade de todas as referências,
  igualdade byte a byte entre análises independentes e serializações repetidas,
  ausência de metadata volátil e consistência após edição estrutural.
- Falsificações temporárias de ordem, `generatedAt`, perda de IF continuation e
  import de `Ast` falharam nos gates esperados e foram removidas. O gate de
  arquitetura inspeciona source e bytecode do adapter e permite somente a
  boundary do Semantic Product, JDK e biblioteca JSON.
- No fechamento do CP7, os testes focais e os gates `docs`, `architecture`,
  `fast`, `semantic`, `performance` e `full` passaram em duas execuções. A
  segunda passagem ocorreu após revisão adversarial completa do diff.
- No fixture estrutural, o audit contém 3 DATA e 14 statements: 7 MOVE, 3 CALL,
  3 IF e 1 `ObservedStatement`. O summary contém 3 modeled, 11 partial, zero
  unsupported e zero input-missing; o observado preservado é partial com gap
  `OBSERVED_STATEMENT_PARTIAL` e bloqueia conservadoramente as três claims
  agregadas.

  | família | lowering | CFG | effects/dataflow | incompletude visível |
  | --- | --- | --- | --- | --- |
  | DATA | `SUFFICIENT` | `NOT_APPLICABLE` | `PARTIAL` | storage/layout/aliases ausentes |
  | MOVE | `PARTIAL` | `SUFFICIENT` | `PARTIAL` | literal kind e storage ausentes |
  | CALL | `SUFFICIENT` | `SUFFICIENT` | `PARTIAL` | runtime target `UNKNOWN` e storage ausente |
  | IF | `PARTIAL` | `SUFFICIENT` | `PARTIAL` | condition semantics não publicada |
  | observed preservado | `BLOCKED` | `BLOCKED` | `BLOCKED` | capability de lowering ainda não tipada |
  | agregado do fixture | `BLOCKED` | `BLOCKED` | `BLOCKED` | herda o fact observado mais fraco |
- `MaterializedCobolSemanticPort` constrói eager, uma única vez e em `O(N)`,
  índices derivados do state materializado. Lookup por statement e containment
  deixa de varrer o inventário global; roots e views MOVE/CALL/IF/observed são
  pré-computadas em ordem estrutural. A semântica de `CobolSemanticProduct.State`
  não mudou.
- `SemanticProductStatementInventoryTest` é um oracle permanente independente
  do projector: deriva o esperado apenas da AST tipada, reconcilia anchors,
  containment, famílias, counts, gaps e readiness, e cobre statements fora da
  capability antes/entre/depois de facts suportados, consecutivos, em branches,
  sob estrutura não projetada, em units sem MOVE/CALL/IF e em múltiplos IFs
  nested. O core prova separadamente a representabilidade de statement
  `INPUT_MISSING` com motivo localizado e de zero real versus inventário
  indisponível.
- O oracle permanente do CP6 prova a publicação fechada e a reconstrução
  boundary-only. As falsificações removendo continuation, promovendo `PARTIAL`
  e introduzindo dependência em `Ast` foram detectadas e revertidas. Os testes
  focais e os gates `docs`, `architecture`, `fast`, `semantic`, `performance` e
  `full` passam no fechamento do CP6. AST, grammar, symbols, occurrences,
  resolution, report, snapshots e fixtures de produção não foram alterados.

## Restante

- Obter review humano do Checkpoint 7 no PR #27.
- Executar o Checkpoint 8 somente com a autorização aplicável. Lowering e a
  integração downstream posterior permanecem futuros.
- Manter EVALUATE, PERFORM, GO TO, terminal semantics, ALTER, SEARCH,
  CobolLower, IR, CFG, effects/storage e dataflow fora deste checkpoint.

## Descobertas que afetam o plano

- A representação mínima suficiente para o CP2 é flat/híbrida: containment
  pertence ao header de cada statement, continuation pertence ao `IfFact`, e
  roots/branch children são queries derivadas. Isso evita duplicar membership e
  não converte program point em execution order.
- `ConditionSurface` carrega apenas shape, referências READ já conhecidas e
  provenance. Predicate normalization continua produto pós-binding futuro; o
  CP2 não afirma semântica de condição que o frontend ainda não publicou.
- `Ast.LiteralExpression` publica value e `rawLexeme`, mas não um kind tipado.
  Produzir `ALPHANUMERIC`/`NUMERIC` conhecido exige enrichment canônico anterior
  do frontend; até lá, a projection conserva `UNKNOWN` e incompletude localizada.
- O core exige parent `IfFact` publicado para containment exato `THEN`/`ELSE`.
  O CP5 usa essa autoridade para qualquer filho direto de IF, inclusive facts
  observados; `UNKNOWN` sob estruturas não cobertas permanece intacto.
- A AST tipada sustenta continuation quando o próximo sibling observado possui
  fact ou quando o IF termina em um scope de IF cuja continuation é segura. Ela
  não sustenta a continuação de um IF que termina sob PERFORM/EVALUATE ou outra
  família estrutural ainda não projetada.
- A cobertura de `Ast.ModeledStatement` não publica a família COBOL específica:
  distinguir DISPLAY de GOBACK nesse nó exigiria interpretar `grammarRule` ou
  `writtenText`. O CP5 preserva honestamente kind/shape genéricos nesses casos;
  variantes estruturais tipadas continuam mais específicas.
- O frontend atual não produz legitimamente um finding statement-level
  `INPUT_MISSING` nas fixtures disponíveis. O core prova que o estado e o gap
  localizado são representáveis; na projection, gaps globais de input tornam o
  `InventoryStatus` `INPUT_MISSING`, preservam facts independentes já observados
  e bloqueiam as três claims agregadas sem atribuir o gap global a um statement
  arbitrário.
- Algumas referências de condição resolvem para entidades fora do namespace DATA
  representável por `ConditionSurface`; o IF estrutural continua publicado e
  recebe `CONDITION_REFERENCE_KIND_NOT_PROJECTED`, sem fabricar `DataItemId` ou
  recalcular o binding.
- A resolution produz `QUALIFIED_HIERARCHY_MATCH` para bindings qualificados
  bem-sucedidos. O core e a projection agora preservam essa reason distintamente
  de `UNIQUE_VISIBLE_DECLARATION`; normalizá-las seria perda de autoridade.
- O report canônico publica claims nominais/dependency-ready por unit, não as
  três dimensões próprias de readiness do Semantic Product. O projector traduz
  somente esses claims disponíveis e limita-os pelo fact individual mais fraco
  e pela disponibilidade do inventário; uma equivalência mais rica depende de
  autoridade canônica adicional, não de classificação paralela local.
- A integração no composition root expôs occurrences cobertas, com input
  completo, por uma `ExternalClassification` canônica cujo gap nominal é
  deliberadamente suprimido pelo report. O projector indexa essa cobertura,
  mantém tais referências fora de `DataReference` e registra
  `CONDITION_REFERENCE_KIND_NOT_PROJECTED`; não altera resolver/report nem
  fabrica binding DATA para um construct externo.
- O transporte não precisa duplicar joins: roots e children vêm das views já
  indexadas do port; as demais coleções são copiadas na ordem canônica. A
  estrutura flat/híbrida permanece observável por containment mais relations de
  branch, sem introduzir CFG ou execution order.
- O JSON v1 é somente output. Não existe reader de produção, round-trip de
  domínio, migração longitudinal ou promessa de compatibilidade entre versões;
  essas ausências são deliberadas neste checkpoint.
