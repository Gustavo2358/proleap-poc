# Avaliação — WORK-COND-002

## O que prova corretude

Corretude neste Discovery significa que a escolha arquitetural respeita simultaneamente o contrato IBM `COND-*` e as fronteiras atuais de produtos, identidade, provenance, ambiguity e performance; que as alternativas rejeitadas possuem contraexemplo concreto; e que nenhuma capacidade não implementada é apresentada como verde.

### Matriz comparativa

| Critério | 1. AST contextual somente | 2. Normalização no lowering | 3. Surface AST + pós-binding |
| --- | --- | --- | --- |
| Separação parse/AST/symbols/occurrences/resolution | preserva até o consumidor reimplementar binding/normalização | mistura meaning binding-dependent no lowering | preserva; adiciona produto explícito depois de resolution |
| Binding CONDITION versus object abreviado | permanece aberto, mas sem owner único da especialização | impossível decidir exatamente pré-binding | especializado na primeira fase com candidate kind/scope |
| Losslessness/source fidelity | forte | fraca se substituir superfície por expansão | forte na AST; predicate separado |
| Subject/operator escritos versus herdados | descritores possíveis | clones/sharing/synthetic fields difíceis | superfície referencia origem; produto marca `WRITTEN`/`INHERITED` |
| AND/OR/NOT, precedência, parênteses/distribuição | preserváveis, porém cada consumer normaliza | normalizáveis só nas classes não binding-dependent | preservados antes; normalizados uma vez após binding |
| IDs/pre-order AST | preservável | clone/shared node ameaça INV-AST-003 | AST preservada; produto possui namespace próprio |
| Provenance | natural para escrito; herdado ainda sem contrato de consumer | risco de span inventado para tokens omitidos | origem escrita + marker herdado explícito |
| Occurrences sintéticas/duplicadas | evitáveis | expansão tende a duplicar A/operands | proibidas; produto reutiliza anchors/bindings |
| Qualification/scope/ambiguity | exige join ad hoc por consumer | indisponíveis no lowering | usa resolution e conserva estados não resolvidos |
| CFG/predicate/dataflow | lógica IBM duplicada em cada consumer | predicate fácil apenas nos casos decididos cedo | produto único pronto para consumidores futuros |
| Incisão/Open-Closed | pequena agora, custo recorrente depois | grande na AST e acoplada | novo produto custa mais agora, fecha extensão dos consumidores |
| Texto/heurística | dispensável se todos os consumers forem rigorosos | tentação de reparse para resolver o caso central | explicitamente proibido; joins estruturais por ID |
| Veredito | insuficiente como arquitetura completa | rejeitada | escolhida, aceita em ADR-0012 |

## Classes positivas

- DATA/RENAMES resolvido em bare tail produz object abreviado somente no produto pós-binding.
- INDEX resolvido (`COND-P06`) tem ownership por fase: binding identifica INDEX; `ConditionSemantics` materializa a relation abreviada; a admissibilidade type-sensitive é verificada depois, na `ConditionValidation` conceitual. Binding nominal não declara a relation válida.
- CONDITION resolvido produz simple condition e encerra o estado herdado.
- relation completa posterior atualiza subject/operator antes da próxima abbreviation.
- boundary/distribuição, precedência e `NOT` permanecem distinguíveis na superfície e na projeção.
- qualification, subscripts, COPY provenance e nested scope chegam ao join sem perder anchors.
- múltiplos consumidores conseguem usar o mesmo predicate sem modificar resolver nem reparsear source.

## Classes negativas

- `grammarRule == conditionNameReference` não fecha admissible kind.
- lowering pré-binding não seleciona CONDITION, DATA ou INDEX final.
- resolver não reconstrói state de subject/operator, parênteses, connectors ou `NOT`.
- subject/operator omitidos não criam `Ast.Node`, token span ou occurrence fictícios.
- AST node não aparece como child por mais de um caminho e não é clonado para cada expansion.
- ambiguity/unresolved não é normalizado como primeira alternativa ou candidate ordenado.
- `writtenText` não é reparsed e corpus frequency não controla a decisão.
- `candidate.kind() == INDEX` não autoriza declarar a relation type-valid; a admissibilidade IBM pertence à validação posterior (`ConditionValidation`).
- resolver não recebe `PIC`/`USAGE` checking e o `AstBuilder` não recebe type checking.
- DATA+CONDITION homônimo no mesmo programa não vira caso de precedência entre candidates.
- nenhum artefato deste PR altera produção ou antecipa Slice 3.

## Classes ambíguas

1. Candidate set nominal ainda ambíguo: `ConditionSemantics` conserva a alternativa contextual e todos os candidates; não expõe predicate final completo.
2. Opção `QUALIFY` desconhecida com resultados divergentes: mantém unsupported/uncertain conforme resolution policy.
3. Nome ausente: conserva unresolved e a estrutura escrita/herdável conhecida, sem declarar ausência de predicate.
4. Shape aceita pela grammar sem validade IBM provada: permanece frontend-supported/semantic-unsupported, não language-positive.

## Casos adversariais

### Challenge da alternativa 1 — AST contextual somente

Executar conceitualmente `A = B OR C` com `C` DATA, INDEX, CONDITION, ambiguous e unresolved. A superfície pode representar todas as classes, mas não fornece um predicate único a CFG/dataflow. Se cada consumer juntar resolution e implementar herança, qualification effects, parentheses e `NOT`, surge duplicação e divergência. Introduzir uma projeção compartilhada resolve o challenge e transforma a alternativa em 3.

### Challenge da alternativa 2 — normalização durante lowering

Usar DATA local `C` e CONDITION global `C` no containing program. A grammar branch/grafia não seleciona a declaration aplicável; somente binding com scope decide. Expandir antes disso quebra uma classe válida. Clonar subject/operator cria IDs/provenance/occurrences sintéticos; compartilhar AST node quebra pre-order; manter duas branches incertas deixa de ser normalização final.

### Challenge da alternativa 3 — produto pós-binding

- **Custo de fase/join:** exigir identity por `(unitId, astNodeId/occurrenceId)` e índice determinístico, sem scan global.
- **Binding não conclusivo:** emitir nó contextual/uncertain e bloquear completeness, não escolher branch.
- **Provenance omitida:** marcar inherited component e referenciar a origem escrita do estado corrente, sem source location fictícia.
- **Occurrence multiplicity:** permitir que múltiplos semantic operand refs apontem ao mesmo binding escrito sem duplicar source occurrence.
- **Consumer coupling:** CFG/dataflow dependem da interface do novo produto, não de records concretos da AST ou de grammar names.

### Challenge arquitetural — normalização versus validação type-sensitive

Considere `N = IDX` com binding nominal idêntico nos dois casos:

| Caso | Declarações | Binding nominal | Relation normalizada | Validação type-sensitive |
| --- | --- | --- | --- | --- |
| INDEX válido (`COND-P06`) | `N` data-name numérico; `IDX` index-name em combinação admitida pela IBM | `N → DATA`, `IDX → INDEX` | `N = IDX` com object INDEX | semanticamente válida |
| INDEX incompatível (`COND-N04`) | `N` data-name não numérico/incompatível com index-name | `N → DATA`, `IDX → INDEX` | `N = IDX` com object INDEX | semanticamente inválida |

Os dois casos produzem o mesmo binding nominal e a mesma relation normalizada em `ConditionSemantics`; somente a etapa posterior `ConditionValidation` (conceitual, futura) pode distingui-los, usando declaração/tipo e os contratos IBM. Uma implementação futura equivalente a `candidate.kind() == INDEX ⇒ relation válida` está proibida: ela confundiria normalização com validação e violaria `COND-N04`/`COND-A06`. O mesmo desafio vale para dados com `USAGE INDEX` (que continuam DATA) e para qualquer combinação em que o binding nominal seja correto mas a regra IBM restrinja os operands.

### Challenge transversal dos oracles

COND-A01 a COND-A13 devem ser aplicáveis à representação proposta: branch enganoso, condition-name real, boundary/distribuição, `NOT`, precedência, INDEX, RENAMES, qualification, shadowing, source cross-set inválido, distribuição restrita, atualização do estado e término por qualquer simple condition. Uma alternativa que falha qualquer uma dessas classes não está pronta para implementação. Para `COND-P06`/`COND-N04`/`COND-A06`, o ownership é fixo: binding identifica INDEX; `ConditionSemantics` materializa a relation; `ConditionValidation` verifica a admissibilidade type-sensitive — nenhuma das três camadas fica sem owner.

## Casos de regressão

- O relatório/fixture/teste do PR #14 continuam caracterização da produção atual.
- EVAL-AST-005 protege IDs/pre-order contra clone/share/metadata ID no futuro.
- `SET condition-name` e `EVALUATE TRUE/FALSE` permanecem controles de contexto tipado que não devem ser generalizados por parent textual.
- INV-RES-001 e EVAL-RES-DATA-002/003 protegem ambiguity, qualification e nesting.
- O PR arquitetural deve ter diff zero em `src/`, `scripts/` e `pom.xml`.

## Propriedades/relações metamórficas

1. Expandir abbreviation válida para relation completa preserva bindings e truth structure, mudando apenas `WRITTEN`/`INHERITED` e a surface tree esperada.
2. Adicionar consumer novo de predicate não exige alterar AST builder, collector ou resolver.
3. Variar caixa/qualification redundante preserva specialization quando resolution preserva o candidate.
4. Reordenar declarations não relacionadas não muda o branch pós-binding.
5. Reexecutar projeção com mesmos produtos/policy produz IDs e tree idênticos.
6. Trocar um binding conclusivo por ambiguous/unresolved só degrada a specialization afetada; não apaga estrutura/provenance independente.
7. Trocar o tipo de um operand por outro compatível/incompatível não altera binding nem relation normalizada; só o veredito de `ConditionValidation` muda.

## Expectativas de escala

O futuro projector deve percorrer cada condition surface uma vez e consultar resolution por índice estável: `O(condition nodes + relevant resolution entries)` em tempo e `O(projected predicate nodes)` em memória, por `ProgramUnit`. Nenhum threshold de hardware é criado neste Discovery. O slice implementador deverá automatizar a propriedade algorítmica antes de reivindicar o gate `performance`.
