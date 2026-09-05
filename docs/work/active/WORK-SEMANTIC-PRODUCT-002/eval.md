# Eval do Semantic Product extensível e lowering-ready

## O que prova corretude

O work item é correto quando um consumer independente, conhecendo somente o
port A2+B, reconstrói todas as ocorrências cobertas de uma `ProgramUnit` com
multiple DATA, multiple MOVE, multiple CALL e IF/ELSE nested, sem parser, AST,
symbols, occurrences, resolver, report ou presentation. A reconstrução preserva
identity, structure, branches, program points, operands, roles, nominal binding,
provenance, coverage, unknowns e readiness por construct.

O oracle arquitetural primário é:

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

O fixture executável deve acrescentar ao menos outra ocorrência relevante ou IF
nested para falsificar limites acidentais de `1`. O consumer não calcula CFG,
reaching definitions nem values. Ele prova que um futuro lowerer poderá
representar store/branch/call e que a cadeia posterior poderá concluir, sem
reinterpretar COBOL:

```text
RD(CALL, WS-X) = { MOVE 'B' TO WS-X, MOVE 'C' TO WS-X }
possible values at CALL = { "B", "C" }
```

Correctness também exige que cada construct seja avaliado nas nove dimensões
da spec: surface, identity, structure, nominal binding, CFG readiness,
effects/dataflow readiness, unknowns, provenance e coverage. Um PASS estrutural
não autoriza elevar predicate, CFG ou effects a completos.

Os testes atuais de singleton permanecem regressão do estado implementado até
o checkpoint que os migra. Eles não provam cardinalidade/extensibilidade. O
novo oracle do target model é o próximo checkpoint autorizado e deve ser criado
sem mudar produção; checkpoints seguintes fazem a implementação satisfazê-lo em
slices revisáveis.

## Classes positivas

| ID local | Classe | Propriedade observável |
| --- | --- | --- |
| SP-P01 | N DATA suportadas | Todas as declarations cobertas têm `DataItemId` namespaced, atributos disponíveis, provenance e coverage; nenhuma é escolhida apenas por participar do primeiro MOVE/CALL. |
| SP-P02 | N MOVE literal → DATA | Cada ocorrência tem statement identity, program point, literal/target, role de write, binding e provenance próprios. |
| SP-P03 | N CALL identifier/expression | Cada ocorrência preserva operando/binding DATA e runtime target `UNKNOWN`; não precisa parear com MOVE específico. |
| SP-P04 | IF com ELSE | Condition surface, THEN, ELSE, children, nesting, termination e join/fallthrough estrutural são reconstruíveis. |
| SP-P05 | IF sem ELSE | A ausência de ELSE é explícita e o successor falso conservador pode ser reconstruído como fallthrough, sem branch inventada. |
| SP-P06 | Statements antes/dentro/depois de branches | Um inventário único preserva containment e ordem estrutural sem confundir com execution order. |
| SP-P07 | Supported + partial coexistem | Facts suportados permanecem disponíveis e o statement parcial aparece no inventário/coverage com motivo. |
| SP-P08 | Consumer boundary-only | Um fake lowerer produz uma representação equivalente do slice sem imports ou objetos do frontend. |
| SP-P09 | Readiness dimensional | DATA/MOVE/CALL/IF publicam claims coerentes de lowering/CFG/effects conforme a matriz da spec, sem alegar as fases futuras. |
| SP-P10 | Transporte posterior | Depois dos demais checkpoints, JSON reproduz state/port extensível, inclusive gaps/readiness, em ordem determinística. |

Para counts exatos, a fixture mínima controlada é autoridade: se ela escreve
quatro MOVE e dois CALL suportados, o oracle exige quatro e dois. Isso é
consistência da fixture, não limite global de cardinalidade.

## Classes negativas

- `State.move()`, `State.call()` ou campo singleton equivalente como envelope
  desejado de produção.
- Projector usando `single(...)`, `findFirst()`, primeiro/último match ou par
  MOVE→CALL como filtro de uma unit.
- Adicionar `if`, `evaluate`, `perform` ou outro campo singular a cada família.
- Omitir statement partial/unsupported e ainda publicar summary completa.
- Tratar coleção vazia como prova simultânea de capability disponível e nenhum
  statement, sem availability/coverage.
- Usar `writtenText`, `grammarRule`, linha ou nome como fonte de structure,
  role, binding ou join.
- Fazer o projector resolver nomes, selecionar candidate, recalcular gap do
  report, inferir runtime target ou construir mini-dataflow.
- Expor AST, ParseTree, SymbolTable, occurrences, resolution, report,
  `SourceMap`, snapshots ou `ExplorerMain` pela boundary/port.
- Tratar program point como execution order, reachability ou edge de CFG.
- Marcar IF como predicate completo ou escolher THEN/ELSE por valor presumido.
- Tratar `DataItemId` como storage region independente ou ignorar aliases.
- Congelar JSON singleton antes de state/consumer corretos, ou anunciar handles
  determinísticos como identidade persistente.

## Classes ambíguas

- **Flat, hierárquico ou híbrido:** todos podem passar se branches/nesting forem
  reconstruíveis por relações tipadas e identities, com lookup determinístico;
  posição incidental ou duplicação de statement falha.
- **Records aninhados ou tipos separados:** ambos são válidos se preservarem
  ownership, imutabilidade, extension typing e port estreito.
- **Taxonomia nominal de readiness:** nomes concretos podem variar; precisam
  distinguir pelo menos suficiente, partial, blocked e not-applicable, declarar
  claim scope e impedir promoção silenciosa.
- **Condition surface parcial:** IF pode ser estruturalmente CFG-ready enquanto
  predicate/effects permaneçam partial. Isso é combinação explícita, não FAIL
  automático nem claim completa.
- **DATA publicada:** declarations independentes podem ser mais amplas que as
  usadas por MOVE/CALL desde que a regra seja determinística e toda entry da
  capability seja classificada; selecionar apenas a primeira ou só a
  declaration comum falha.
- **CALL literal:** permanece fora da capability inicial de CALL variável, mas
  sua ocorrência observada precisa aparecer como partial/unsupported ou fact
  canônico explicitamente classificado; não pode sumir.
- **Zero ocorrências:** é sucesso somente quando a capability foi produzida e o
  inventário prova zero; capability unavailable/blocked é outro estado.

## Casos adversariais

1. Intercalar dois MOVE e dois CALL para DATA diferentes; remover a exigência de
   mesma identity entre um par específico sem perder os bindings individuais.
2. Colocar MOVE antes do IF, nos dois branches, depois do IF e em IF nested;
   exigir cada occurrence uma vez e containment correto.
3. Inserir statement unsupported entre `MOVE` e `CALL`; facts adjacentes
   continuam, mas coverage/readiness não alegam sequência completa.
4. Usar IF sem ELSE e IF com ELSE vazio; distinguir as duas shapes sem inventar
   reachability.
5. Tornar uma condition reference ambiguous/unresolved; preservar candidates,
   status e structure do IF sem escolher branch nem apagar children.
6. Usar MOVE fora da capability inicial antes de um MOVE suportado; o projector
   não pode selecionar somente o suportado e omitir o primeiro.
7. Usar CALL variável sem MOVE anterior, com múltiplos MOVE possíveis e com
   `VALUE` na declaration; runtime target continua `UNKNOWN` nos três casos.
8. Construir duas units com local IDs iguais; todos os joins permanecem
   namespaced e nenhum fact cruza a unit.
9. Tornar COPY/provenance parcial; facts independentes continuam, exactness e
   gap permanecem observáveis.
10. Criar divergência controlada entre fact individual e summary do report; a
    projeção falha fechada em vez de elevar claim ou recalcular o report.
11. Inspecionar source/bytecode de boundary e consumer contra dependências de
    frontend, ANTLR, presentation, `writtenText` e `grammarRule` semântico.
12. Repetir a projeção equivalente e variar a ordem das queries do port; facts,
    handles e ordem permanecem iguais.
13. Editar a estrutura entre execuções; nenhum teste exige preservar handles da
    execução anterior, evitando identidade persistente acidental.
14. Desafiar readiness: IF com condition partial ainda enumera dois successors
    estruturais; statement de fluxo unsupported não vira fallthrough.
15. Desafiar effects: MOVE expõe source/target roles, IF expõe reads cobertos e
    CALL expõe uso do operando; nenhum teste aceita `GEN/KILL` no produto ou
    `DataItemId == StorageId`.

## Casos de regressão

- O fixture 3B continua provando DATA + MOVE literal + CALL variável, binding
  nominal comum, provenance e runtime target `UNKNOWN`, agora como caso N=1.
- Tests de imutabilidade/closure e consulta fora de ordem continuam verdes após
  a migração para collections.
- ADR-0004/EVAL-RES-CALL-002 continuam rejeitando target dinâmico derivado de
  binding nominal, literal, `VALUE` ou ordem linear.
- EVAL-AST-003/005 continuam protegendo structure, coverage e pre-order; o
  Semantic Product não muda IDs da AST nem a usa como identidade persistente.
- EVAL-COV-002/003 e EVAL-RES-REPORT-001 preservam unknown/input missing,
  facts independentes e claim conservadora.
- `Ast.IfStatement`, condition surface e occurrence traversal continuam
  inalterados; F-01 e `ConditionSemantics` não são remediados neste item.
- Architecture gate continua protegendo produtos existentes e passa a proteger
  `boundary → frontend/projection` quando o package split for materializado.
- Snapshots, HTML, CLI, grammar, fixtures/baselines existentes, AST, symbols,
  occurrences e resolver permanecem semanticamente equivalentes.

## Propriedades/relações metamórficas

- Duplicar um MOVE/CALL suportado com novos program points acrescenta exatamente
  um fact, sem substituir o anterior nem exigir novo campo no state.
- Inserir declaration não relacionada acrescenta sua classificação/declaration
  fact conforme a capability, sem alterar bindings existentes que continuem
  únicos.
- Mover um statement de top-level estrutural para THEN/ELSE conserva seus facts
  nominais e provenance, alterando somente containment/program point legítimos.
- Expandir IF sem ELSE para ELSE explícito acrescenta a branch; não altera o
  target de runtime dos CALLs nem cria CFG no produto.
- Trocar o literal de um MOVE altera apenas source/value/provenance dependente;
  o CALL variável continua runtime `UNKNOWN`.
- Renomear consistentemente DATA e seus usos preserva relações e statuses; IDs
  podem mudar se o contrato determinístico assim definir, sem promessa
  persistente.
- Resolver input/COPY ausente refina gaps e facts dependentes sem apagar facts
  independentes já sustentados.
- Reordenar queries do port não altera facts; repetir execução equivalente
  preserva handles/ordem transportáveis.
- Acrescentar statement unsupported reduz ou mantém readiness conforme o
  escopo, nunca aumenta a claim nem desaparece do inventário.
- Acrescentar uma futura família tipada não muda a semântica das famílias
  existentes nem exige converter o envelope em bag genérico.

## Expectativas de escala

A projeção percorre a surface da `ProgramUnit` e os inventários relevantes em
ordem determinística, com joins por índices/identities. A meta é
`O(nodes + declarations + occurrences + resolution entries + gaps + facts)`,
descontados os lookups canônicos já executados. É proibido resolver cada uso
varrendo todas as declarations ou reconciliar statements por busca textual.

Cardinalidade alta não autoriza truncar facts, candidates, branches, provenance
ou unknowns. Qualquer index auxiliar é construído uma vez por publicação. O
consumer de lowering-readiness percorre o produto, não o frontend. JSON
posterior preserva ordem sem depender de map/hash incidental. Threshold de
hardware não é oracle; `check-performance.sh` é exigido quando a implementação
alterar o caminho de projeção/indexação.
