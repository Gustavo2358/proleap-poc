# ADR-0012 — Condições contextuais usam projeção pós-binding

Status: Accepted
Type: Contemporary
Recorded: 2026-09-01

## Context

Em abbreviated combined relation conditions, um bare nominal tail pode ser object de uma relation abreviada ou uma nova simple condition. A distinção exige declaration kind, qualification e scope: a parse tree e o lowering pré-binding não sabem se o nome é DATA/INDEX ou CONDITION.

Ao mesmo tempo, a fonte precisa permanecer lossless quanto a conectores, precedência, parênteses, `NOT`, subject/operator escritos ou omitidos e boundaries de inserção. A AST atual, as symbol tables, as occurrences e a resolução são produtos imutáveis e separados. IDs de AST seguem pre-order canônico por `ProgramUnit`, provenance não pode ser inventada, e ambiguity não pode ser eliminada por ordem.

O contrato IBM e os oracles `COND-*` estão em [expressões condicionais](../../domain/conditional-expressions.md) e [oracles normativos](../../evals/conditional-expression-oracles.md). O [Discovery do PR #14](../../history/evidence/semantic-condition-context-discovery-report.md) demonstra a perda atual, mas não define a decisão.

A validade de uma relation-condition também depende de compatibilidade de tipos regida pela regra IBM, e isso não é name binding. Em `N = IDX`, o binding pode concluir `N → DATA` e `IDX → INDEX` sem provar que a comparação é permitida para index-name: index-name participa de relation-condition somente nas combinações admitidas pela IBM. A admissibilidade type-sensitive pertence a etapa posterior ao binding e não pode ser alegada pelo resolver nem pela projeção estrutural.

## Decision

Adotar uma representação em duas camadas:

1. A AST permanece produto de superfície, anterior ao binding. Ela deve preservar de forma tipada e lossless a condition sequence escrita, inclusive todos os connectors/children, precedência, grupos, distribuição, `NOT`, relations completas/abreviadas e uma alternativa contextual explícita quando a declaration for necessária para distinguir condition-name de object abreviado.
2. A AST não recebe candidate IDs, selected binding nem predicate já especializado. Relações herdadas usam descritores/referências estruturais a componentes escritos, não clones nem compartilhamento de instâncias `Ast.Node` como filhos.
3. `ReferenceOccurrences` contém uma occurrence para cada uso nominal escrito. O contexto da occurrence contextual admite as classes que a regra COBOL mantém possíveis; subject/operator herdados não criam occurrences sintéticas e uma occurrence escrita não é duplicada para simular expansão textual.
4. `ReferenceResolution` continua responsável somente pelo binding nominal, incluindo qualification, scope, candidates e estados de incerteza. Ele não reconstrói subject, operator, `NOT`, parênteses ou precedência e não valida compatibilidade de tipos da relation-condition: `PIC`/`USAGE` checking e a admissibilidade type-sensitive ficam fora do binding nominal.
5. Um produto imutável separado, denominado conceitualmente `ConditionSemantics`, consome AST de superfície, occurrences e resolution e projeta predicates normalizados após o binding. Ele especializa DATA/INDEX como object abreviado e CONDITION como nova simple condition somente quando a resolução autoriza essa conclusão. A projeção materializa a relation normalizada sem afirmar que ela é type-valid: resolver `N → DATA` e `IDX → INDEX` não prova que `N = IDX` é admissível pela regra IBM para index-name.
6. `ConditionSemantics` possui identidade local própria, namespaced por `ProgramUnit`, determinística e independente de `Ast.Meta.id`. Seus componentes referenciam anchors AST/occurrence/resolution existentes e marcam explicitamente `WRITTEN` ou `INHERITED`; um componente herdado aponta para a origem escrita do estado corrente e não recebe source span físico inventado.
7. `AMBIGUOUS`, `UNRESOLVED`, `UNSUPPORTED`, opção de dialeto desconhecida e source inválido permanecem nós/estados explícitos do produto. A projeção não escolhe branch, candidate ou expansão completa quando os pré-requisitos não permitem decisão única.
8. CFG, predicate analysis e dataflow futuros consomem o predicate especializado de `ConditionSemantics` e, quando precisarem de admissibilidade type-sensitive, o resultado da validação posterior (item 9). Navegação e ferramentas de source fidelity continuam consumindo a AST de superfície. Nenhum consumidor reparseia `writtenText` ou infere categoria por `grammarRule`.
9. A compatibilidade semântica de operands pertence a uma etapa posterior de validação de condições, denominada conceitualmente `ConditionValidation`. Ela consumirá `ConditionSemantics`, a informação de declaração/tipo necessária e os contratos IBM aplicáveis, e produzirá a validade semântica da relation — distinguindo pelo menos: relation semanticamente válida; relation semanticamente inválida; validade ainda não verificável/incompleta — sem alterar AST, occurrences, resolution ou `ConditionSemantics`. Enum, classe, schema e diagnostic codes não são congelados neste ADR.
10. Pipeline conceitual: `Surface AST → ReferenceOccurrences → ReferenceResolution → ConditionSemantics → ConditionValidation → consumidores como CFG/predicate/dataflow`. `ConditionSemantics` e `ConditionValidation` ainda não existem em produção; nenhum produto novo foi implementado no PR deste ADR. A divisão de responsabilidades é arquitetural e um slice futuro decidirá API/schema concretos.

O algoritmo futuro deve ser estrutural e por unit: uma traversal da condition surface com estado de subject/operator e uma tabela indexada de resolution por `(unitId, referenceAstNodeId)` produz a projeção em `O(condition nodes + relevant resolution entries)`, descontado o lookup nominal já executado. Não há scan textual nem `O(references × all declarations)`.

## Challenge pass e alternativas

### Alternativa 1 — somente AST contextual

Preservar abbreviation/ambiguity na AST atende losslessness, IDs, provenance e occurrences escritas. Falha como contrato completo quando CFG, predicate analysis ou dataflow precisam de uma relation especializada: cada consumidor teria de combinar AST e resolution, reproduzir o state machine IBM e decidir como representar uncertainty. Essa duplicação viola Open/Closed e permite divergência entre consumidores.

Challenge: `A = B OR C`, alternando `C` entre DATA, INDEX, CONDITION, ambiguous e unresolved. Uma AST pré-binding não produz sozinha o mesmo predicate para essas cinco classes. Acrescentar uma projeção compartilhada para resolver o desafio transforma esta alternativa na decisão adotada.

### Alternativa 2 — normalização durante lowering

Materializar relations completas no `AstBuilder` simplifica consumidores quando todos os operands já têm significado conhecido. Ela falha no caso central porque lowering ocorre antes de symbols/resolution: promover todo bare tail a relation quebra condition-name real; promovê-lo a condition-name quebra DATA/INDEX.

Challenge: `A = B OR C` com DATA local versus CONDITION global visível em containing program. A decisão correta depende de scope e binding. Clonar `A`/operator para antecipar ambas as branches cria IDs e provenance sintéticos e occurrences duplicadas; compartilhar os nodes existentes viola o pre-order de árvore. Manter duas branches incertas no lowering deixa de ser normalização e volta a ser AST contextual.

### Alternativa 3 — AST de superfície + produto pós-binding

É a alternativa escolhida. Ela separa source fidelity de predicate especializado e posiciona cada decisão na primeira fase que possui a informação necessária.

Challenges restantes e respostas:

- **Novo produto e joins:** aumenta a superfície arquitetural. A identidade por unit e os joins por AST/occurrence IDs tornam ownership explícito e evitam dependência reversa.
- **Ambiguidade sem predicate final:** o produto conserva nó contextual/uncertain e bloqueia claim de normalização completa; não escolhe por conveniência.
- **Expansão sem occurrences sintéticas:** normalized operands referenciam bindings/anchors já existentes; multiplicidade semântica não é falsificada como multiplicidade de texto.
- **Provenance herdada:** `INHERITED` referencia a origem escrita do subject/operator e a condição de superfície, sem alegar que tokens omitidos existiam fisicamente.
- **Normalização versus validação:** normalizar a relation não autoriza alegar admissibilidade type-sensitive; a validação permanece em etapa posterior (`ConditionValidation`), sem reparse textual ou heurística.
- **Incisão:** uma implementação completa toca AST/lowering, occurrences e o novo projector, mas não exige que symbol table ou resolver passem a interpretar predicates. O trabalho permanece fatiado; este ADR não autoriza essas mudanças.

## Rationale

A distinção principal é binding-dependent, portanto nenhuma fase anterior pode fechá-la exatamente. A representação dual mantém a AST reutilizável e lossless, conserva o resolver nominal e fornece uma única interpretação compartilhada aos consumidores futuros. A fronteira normalização/validação evita que o projector afirme admissibilidade type-sensitive sem a informação de declaração/tipo e mantém o resolver exclusivamente nominal. Isso preserva ADR-0003, INV-AST-001/003, INV-RES-001, INV-PROV-002 e a ausência de reparse textual de INV-AST-002.

## Consequences

- O primeiro slice executável deve tornar a condition surface lossless sem ainda construir `ConditionSemantics` nem alterar o resolver.
- Occurrences contextuais e o projector pós-binding exigem slices próprios e oracles que cubram DATA, INDEX, CONDITION, qualification, scope, ambiguity e provenance.
- A validação type-sensitive da relation (`ConditionValidation`) pertence a etapa futura: o projector não declara `VALID` quando a informação de tipo necessária ainda não estiver materializada.
- Aceitar este ADR não autoriza implementação: `ConditionSemantics`, `ConditionValidation` e os slices executáveis continuam exigindo autorização explícita posterior.
- Snapshots futuros precisam separar IDs/árvores da AST e de `ConditionSemantics`; migração de cardinalidade AST só pode refletir nodes escritos legitimamente materializados.
- Effects, reads e predicates semânticos futuros podem reutilizar um binding em múltiplos pontos normalizados, mas source occurrences continuam inventário dos usos escritos.
- Grammar só muda se uma investigação demonstrar que os contexts/tokens atuais não preservam a estrutura necessária; reordenar alternativas não resolve declaration kind.

## Rejected alternatives

- AST contextual como único produto consumível, sem projeção compartilhada.
- Normalização/specialization completa durante lowering pré-binding.
- Patch no resolver que reconstrua condition semantics por parent, texto ou `grammarRule`.
- Clonagem/compartilhamento de `Ast.Node` para representar subject/operator herdados.
- Occurrences sintéticas para tokens omitidos.
- Declarar uma relation type-valid a partir apenas do binding nominal (por exemplo, `candidate.kind() == INDEX ⇒ relation válida`), sem validação type-sensitive posterior.
- Atribuir `PIC`/`USAGE` checking ao resolver ou type checking ao `AstBuilder`.

## Related invariants

INV-COND-001, INV-COND-002, INV-AST-001, INV-AST-002, INV-AST-003, INV-SYM-001, INV-PROV-002, INV-RES-001, INV-DET-001 e INV-PERF-001.
