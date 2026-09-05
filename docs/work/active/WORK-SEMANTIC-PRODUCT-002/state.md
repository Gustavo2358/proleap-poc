# Estado

## Onde estamos

Os seis checkpoints da migração documental e os Checkpoints 1–3 foram
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
e todos os MOVE/CALL de raiz observados. MOVE/CALL dentro de branches não são
falsamente publicados como roots: dependem da IF frontend projection reservada
ao CP4. `ExplorerMain` e o composition root continuam inalterados.

## Verde conhecido

- `CobolSemanticProduct.State` é fechado, imutável e namespaced por
  `UnitId`; DATA, statement, operand e candidate possuem identities próprias.
- O inventário tipado usa `StatementFact` com `MoveFact`, `CallFact`, `IfFact`
  estrutural e `ObservedStatement` genérico. Uma nova família não altera o
  envelope do state.
- Containment flat/híbrido preserva `ROOT`, `THEN`, `ELSE`, nesting e
  continuation por identity. Branches vazias são consultáveis sem afirmar se
  havia ELSE sintaticamente vazio ou ELSE ausente e sem publicar CFG.
- `NominalBinding` representa `RESOLVED`, `AMBIGUOUS`, `UNRESOLVED` e
  `INPUT_MISSING`; somente o caso resolvido único possui `selected`.
  Operand/reference identity continua existindo nos demais casos.
- Candidates e facts não podem cruzar namespace, e todo candidate publicado
  precisa ter declaration correspondente na publicação fechada.
- Coverage individual distingue `MODELED`, `PARTIAL`, `UNSUPPORTED` e
  `INPUT_MISSING`. A summary classifica exatamente o inventário, distingue zero
  statements de inventário indisponível e não pode elevar lowering, CFG ou
  effects/dataflow acima do fact individual mais fraco.
- CALL variável continua com runtime target `UNKNOWN` e gap localizado;
  binding nominal não se torna target de runtime nem storage identity.
- `LiteralSource` preserva `LiteralKind` no core. Um teste direto atravessa o
  port com numeric `1` e alphanumeric `'1'`, ambos com value `"1"`, e
  prova que continuam semanticamente distinguíveis sem AST, raw text ou PIC.
- A projection percorre deterministicamente as coleções canônicas, sem
  `single(...)`, `findFirst()`, primeiro/último match ou pairing MOVE→CALL. O
  teste adversarial publica quatro DATA, três MOVE e três CALL intercalados para
  DATA distintas, incluindo declaration não usada, e preserva o caso N=1 como
  regressão.
- Cada MOVE/CALL publicado retém identity, program point estrutural, operands,
  roles, binding, candidates e provenance próprios. Ambiguidade nominal mantém
  todos os candidates sem selecionar um deles; candidate de namespace inválido
  faz a projection falhar fechada.
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
- Como a AST canônica ainda não publica literal kind tipado, todo MOVE literal
  do CP3 usa `LiteralKind.UNKNOWN`, coverage/readiness conservadoras e o gap
  localizado `LITERAL_KIND_NOT_PUBLISHED`, sem inferência por `rawLexeme`, value
  ou PIC.
- O gate de arquitetura inspeciona o novo pacote de projection e proíbe
  dependência em builders/collectors/resolvers, `SourceMap`, ANTLR, snapshots e
  presentation. O oracle CP1 permanece independente da implementação.
- `MaterializedCobolSemanticPort` constrói eager, uma única vez e em `O(N)`,
  índices derivados do state materializado. Lookup por statement e containment
  deixa de varrer o inventário global; roots e views MOVE/CALL/IF/observed são
  pré-computadas em ordem estrutural. A semântica de `CobolSemanticProduct.State`
  não mudou.
- Testes diretos de produção cobrem N DATA/statements, multiple MOVE/CALL,
  IF aninhado, branches vazias, containment, bindings incompletos,
  observed/unmodeled, coverage conservadora, readiness dimensional,
  imutabilidade, namespace e ausência de API singleton.
- Os 37 testes focais de projection/core/oracle/architecture e os gates `docs`,
  `architecture`, `fast`, `semantic`, `performance` e `full` passam. AST,
  grammar, symbols, occurrences, resolution, report, `ExplorerMain`, snapshots
  e fixtures de produção não foram alterados.

## Restante

- Obter review humano do Checkpoint 3 no PR #27.
- Executar os Checkpoints 4–8 somente na ordem registrada e com a autorização
  aplicável. IF frontend projection, coverage completa da `ProgramUnit`,
  composition root, consumer de lowering-readiness e JSON permanecem futuros.
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
- O core do CP2 exige parent `IfFact` publicado para containment `THEN`/`ELSE`.
  Como IF frontend projection pertence ao CP4, MOVE/CALL aninhados são
  deliberadamente adiados em vez de receber containment `ROOT` falso. O
  inventário agregado do CP3 permanece `PARTIAL` por esse recorte.
- O report canônico publica claims nominais/dependency-ready por unit, não as
  três dimensões próprias de readiness do Semantic Product. O projector traduz
  somente esses claims disponíveis, limita-os pelo fact individual mais fraco e
  mantém a summary parcial; uma equivalência mais rica depende de autoridade
  canônica adicional, não de classificação paralela local.
