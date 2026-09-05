# Plano

## Fatiamento

O fixture original continua como regressão da boundary, não como limite. Os
checkpoints antigos ainda não executados são substituídos pela sequência abaixo.
Cada checkpoint deve revisar seu diff completo, executar os gates proporcionais
ao risco e preservar facts conhecidos mesmo quando encontre gaps.

1. **Oracle executável do target model — próximo checkpoint autorizado.** Sem
   alterar produção, criar fixture e consumer/oracle test-only executável com
   multiple DATA, multiple MOVE literal, multiple CALL variável, `IF/ELSE`,
   statement dentro/fora de branches, merge posterior e nesting simples. O
   oracle define todas as ocorrências esperadas, relations/anchors, binding,
   provenance, coverage e readiness. Deve provar que unsupported observado não
   pode desaparecer e que um consumer boundary-only consegue reconstruir a
   entrada necessária ao lowering. Não implementa CFG nem calcula o conjunto de
   reaching definitions.
2. **Remodelagem do core A2+B para cardinalidade e extensão.** Sem frontend,
   substituir singleton `move/call/ordering` por container imutável de
   declarations/statements e relações estruturais tipadas. Provar N facts,
   zero-facts versus unavailable, ordering/program points, identities
   namespaced, closure, imutabilidade, no frontend leakage e crescimento por
   família tipada. Decidir hierárquico/flat/híbrido somente pela capacidade de
   reconstruir nesting e lookup, sem fixar IR.
3. **Projector de todas as ocorrências MOVE/CALL e correções arquiteturais.**
   Dar à seam nome/pacote estável por responsabilidade, projetar todas as
   ocorrências cobertas de MOVE/CALL e as DATA necessárias, remover `single(...)`
   e a obrigação de um par comum. Usar cada produto canônico como autoridade,
   inclusive `ResolutionAnalysisReport` para gaps/readiness/claims. Provar joins
   por identity, ausência de reparse/resolução/inferência e publicação explícita
   das shapes fora da capability.
4. **IF/ELSE facts.** Acrescentar a segunda família estrutural ao mesmo
   container, preservando condition surface, references/bindings disponíveis,
   statements de THEN/ELSE disponíveis, ramo falso, nesting, termination,
   program points, provenance, coverage e readiness. Provar dois successors
   conservadores e join reconstruível como informação de CFG-readiness, sem
   publicar edges, truth, reachability ou predicate semantics inexistente. A
   AST atual não distingue ELSE ausente de ELSE sintaticamente vazio; o
   checkpoint não inventa essa distinção nem altera a AST. Se um consumer a
   exigir, registra gap e precondição de frontend próprios.
5. **Coverage e incompleteness da ProgramUnit.** Reconciliar inventário de
   statements com modeled/partial/unsupported/input-missing, localizar unknowns
   e impedir claim global acima dos facts individuais. Provar que statement
   desconhecido entre facts suportados não some, não vira efeito vazio e não
   elimina facts independentes.
6. **Composition root e consumer independente de lowering-readiness.** Inserir
   a menor publicação atômica após os produtos do frontend estarem coerentes.
   O consumer recebe somente o port, opera após liberar o frontend e reconstrói
   DATA/MOVE/CALL/IF/ELSE, structure, operands, roles, binding, gaps e
   provenance. Auditar explicitamente lowering, CFG e effects/dataflow
   sufficiency sem implementar lowerer, IR, CFG ou effects.
7. **JSON determinístico v1.** Somente com state/container e consumer verdes,
   criar JSON output adapter separado, consumindo apenas state/port. Documentar
   versão e envelope extensível; preservar statements, structure, coverage,
   uncertainties, provenance e readiness; provar bytes/ordem/handles
   determinísticos para execuções equivalentes. JSON permanece transporte
   interno de inspeção/desenvolvimento, não domínio nem identidade persistente.
8. **Review de lowering/IR readiness e handoff.** Auditar a matriz por construct
   e o oracle `MOVE → IF/ELSE → CALL`; demonstrar que um futuro `CobolLower`
   pode iniciar sem frontend internals. Registrar gaps e dependências de
   EVALUATE/PERFORM/outros constructs no backlog, sem desenhar a IR nem iniciar
   CFG/dataflow. Encerrar o work item somente se invariantes, evals e lifecycle
   hygiene estiverem coerentes.

Esta task documental autoriza, depois de concluir sua migração, somente o
Checkpoint 1 corretivo acima. Checkpoints 2–8 exigem que a evidência do anterior
esteja disponível e a autorização aplicável; nenhum checkpoint autoriza
implicitamente work de backlog.

## Meta operacional

15 de setembro de 2026 é a meta operacional que orienta priorização e recorte
das capabilities em direção a um Semantic Product utilizável por lowering, CFG
e dataflow. Ela favorece slices verticais que entreguem evidência downstream e
evita prolongar preparação ou ampliar escopo sem necessidade demonstrada.

A data não é promessa de entrega, decisão arquitetural nem autorização para
pular checkpoints. Ela não permite limitar cardinalidade artificialmente,
reduzir garantias semânticas, omitir lacunas, converter unknown/partial em
sucesso ou anunciar completude falsa. Quando prazo e correção entrarem em
tensão, o slice deve ser reduzido por capability e a incompletude deve continuar
explícita.

## Dependências

- ADR-0013 e INV-SP-001–006 definem capability versus cardinalidade, boundary
  COBOL-specific, projection sem análise, readiness, storage identity e
  determinismo de transporte.
- `WORK-SEMANTIC-PRODUCT-001` e os relatórios 3A/3B preservam a prova A2+B e o
  fixture linear como baseline histórica.
- A implementação atual de `CobolSemanticProduct`, `CobolSemanticPort` e
  `CobolMoveCallAdapter` é o ponto de partida factual: singleton MOVE/CALL,
  `single(...)`, mesmo DATA e gap local de CALL ainda existem.
- `Ast.IfStatement`, `Ast.children` e `ReferenceOccurrenceCollector` já
  preservam condition, branches, nesting e nominal references suficientes para
  o slice estrutural; `ConditionSemantics`/`Validation` continuam futuros.
- AST, units, symbol tables, occurrences, resolution, report, policy e
  provenance são autoridades separadas; o projector depende deles, nunca o
  inverso.
- F-01 bloqueia as shapes combinadas de `EVALUATE TRUE`; F-SP-007 bloqueia
  lowering exato dos controls de PERFORM. Nenhum dos dois é corrigido aqui.
- `CobolLower`, Analysis IR, CFG, effects/storage e dataflow dependem dos
  contratos de readiness e do backlog; não são precondição implementada para os
  checkpoints deste work item.

## Superfície arquitetural provável

Os nomes abaixo indicam responsabilidades, não classes congeladas:

```text
frontend products canônicos
  AST / units / symbols / occurrences / resolution / report / policy / provenance
                              │
                              ▼
                  projection/translation seam
                              │
                              ▼
Cobol Semantic Product A2
  ├─ Unit + Policy
  ├─ DataDeclarations[]
  ├─ StatementFacts[]
  │    ├─ MoveFact
  │    ├─ CallFact
  │    ├─ IfFact
  │    └─ observed partial/unsupported facts
  ├─ typed structural relations
  ├─ coverage/readiness summary
  └─ localized uncertainties/provenance
                              │
                              ▼
Cobol Semantic Port B
                              │
                ┌─────────────┴─────────────┐
                ▼                           ▼
lowering-readiness consumer       JSON output adapter (CP7)
```

Dependency direction obrigatória:

```text
projection → boundary    permitido
boundary → projection    proibido
boundary → frontend      proibido
```

O futuro `CobolLower` dependerá de seu próprio input port e receberá uma
tradução do `CobolSemanticPort`, por adapter in-memory ou JSON input adapter
fora deste work item. Neutralidade começa no lower/Analysis IR. O state não
adquire campos `evaluate`, `perform`, `goto` ou equivalentes a cada novo slice;
novos statement facts entram na mesma disciplina de extensão tipada.

## Migrações requeridas

- Preservar o fixture e testes atuais como regressão da capability inicial, mas
  remover de contratos futuros qualquer significado de “exatamente um”.
- Remodelar state/port e testes de singleton para collections/structure somente
  no Checkpoint 2 corretivo.
- Substituir a seam nomeada pelo fixture por papel de projection estável e, se
  necessário, separar package de boundary e package de projection no
  Checkpoint 3; o nome concreto não é decidido nesta documentação.
- Trocar selection `single(...)` por traversal/inventário determinístico de
  todas as ocorrências cobertas; joins continuam indexed/namespaced.
- Incorporar report/coverage como autoridade de gaps/readiness sem duplicar
  classifications no projector.
- Acrescentar IF/ELSE e relações estruturais sem migrar AST, grammar,
  occurrences ou resolver.
- Integrar a publicação no composition root somente após core/projector/coverage
  estarem corretos.
- Migrar JSON antigo planejado para depois do consumer de lowering-readiness;
  nenhum schema é congelado antes do Checkpoint 7.

Nenhuma migração de código, fixture, baseline ou schema ocorre na task
documental que reescreve este plano.

## Artefatos esperados

| Checkpoint | Artefatos focalizados |
| --- | --- |
| 1 | fixture de target model; oracle/consumer test-only executável; contrato explícito de counts, structure e readiness; nenhum `src/main` alterado |
| 2 | core A2+B extensível e testes diretos sem frontend |
| 3 | projection seam corrigida, adapter tests de multiple MOVE/CALL/DATA e architecture gate ampliado |
| 4 | IF/ELSE facts e testes de branches/nesting/partial predicate |
| 5 | inventário/coverage/incompleteness por unit e oracles de no-silent-omission |
| 6 | menor wiring no composition root e consumer independente de lowering-readiness |
| 7 | JSON output adapter, artefato/versionamento documentado e testes de determinismo/suficiência |
| 8 | review/handoff factual para lowering/IR e atualização final de evals/state/backlog/lifecycle |

Todos os checkpoints preservam grammar, AST, symbols, occurrences, resolver,
snapshots e baselines, salvo autorização nova baseada em finding independente.
Nenhum artefato de `CobolLower`, IR, CFG, effects/storage, dataflow ou Dependency
Facts é criado por este work item.
