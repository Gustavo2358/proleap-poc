# Estado

## Onde estamos

Os seis checkpoints da migração documental, o Checkpoint 1 corretivo e o
Checkpoint 2 foram executados no PR #27, que permanece sob review. O CP1 mantém
o target model e o consumer exclusivamente em `src/test`; eles continuam como
especificação executável independente.

O core A2+B de produção agora publica `Unit`, `Policy`, coleções imutáveis de
DATA e statement facts, gaps localizados e coverage/readiness. O envelope não
possui mais os singletons `move`, `call` ou `ordering`. `CobolSemanticPort`
consulta somente esse state materializado e oferece as famílias MOVE/CALL/IF e
as relações de raiz/filhos como views derivadas do inventário plural.

O frontend real não foi conectado ao modelo plural. `CobolMoveCallAdapter`
continua com a seleção estreita preexistente e somente constrói uma publicação
plural de cardinalidade N=1 para preservar compilação e regressão. Essa bridge
é dívida transitória a remover no CP3; ela não autoriza projection plural nem
IF projection neste checkpoint.

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
- O caso original DATA + MOVE literal + CALL variável continua coberto como
  regressão N=1, com provenance, policy e handles determinísticos no contexto
  equivalente. A bridge transporta explicitamente o kind `ALPHANUMERIC` desse
  literal e marca seu inventário agregado como `PARTIAL`.
- O gate de arquitetura inspeciona explicitamente os tipos do core/port e agora
  bloqueia também dependência no `CobolMoveCallAdapter` e em áreas futuras de
  projection/adapter, além de frontend e ANTLR.
- Testes diretos de produção cobrem N DATA/statements, multiple MOVE/CALL,
  IF aninhado, branches vazias, containment, bindings incompletos,
  observed/unmodeled, coverage conservadora, readiness dimensional,
  imutabilidade, namespace e ausência de API singleton.
- Oracle CP1, regressões `SemanticProduct*`, architecture, `fast`, `semantic`,
  `performance` e `full` passam. AST, grammar, symbols, occurrences, resolution,
  report, `ExplorerMain`, snapshots e fixtures de produção não foram alterados.

## Restante

- Obter review humano do Checkpoint 2 no PR #27.
- Mediante autorização posterior, executar o Checkpoint 3: separar a seam de
  projection conforme necessário, remover a bridge singleton e publicar todas
  as ocorrências MOVE/CALL/DATA cobertas usando cada produto canônico como
  autoridade.
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
- A bridge N=1 mantém todos os guards do adapter atual, inclusive exatamente um
  MOVE/CALL/target, DATA comum e MOVE anterior ao CALL. Como o frontend atual
  não publica um kind tipado no `Ast.LiteralExpression`, a bridge aceita somente
  sua shape histórica quote-delimited e a transporta como `ALPHANUMERIC`; uma
  source numeric falha fechada em vez de ser classificada por value ou PIC. O
  core já representa `NUMERIC`; o CP3 deve remover a bridge e projetar apenas os
  kinds sustentados por autoridade canônica, localizando os demais como gap.
- O adapter ainda cria localmente o gap/readiness do CALL dinâmico; essa dívida
  preexistente continua reservada ao CP3, que deverá consumir o report canônico.
