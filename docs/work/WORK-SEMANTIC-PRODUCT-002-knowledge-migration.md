# Mapa transitório de migração — WORK-SEMANTIC-PRODUCT-002

Status: checklist temporária dos Checkpoints 1–6 da migração documental. Este
arquivo não é arquitetura, regra de domínio nem backlog. Ele será removido no
Checkpoint 6 quando todos os destinos abaixo estiverem materializados e
auditados.

Fonte transitória inventariada:
`TEMP-SEMANTIC-PRODUCT-IR-CFG-DATAFLOW-DIRECTION.md`.

## Legenda

- `EXISTE`: o harness já contém a decisão com autoridade compatível.
- `PARCIAL`: existe fundamento canônico, mas falta a decisão específica.
- `CONFLITA`: o contrato ativo ainda afirma uma direção supersedida.
- `NOVO`: não existe destino canônico suficiente.
- `HISTÓRICO`: descreve evidência ou estado implementado, sem promover norma.
- `CPn`: checkpoint em que o destino deve ser materializado.

## Decisão → destino canônico

| ID | Conhecimento normativo ou restrição | Estado antes da migração | Divergência ou limite observado | Destino canônico planejado | Ação |
| --- | --- | --- | --- | --- | --- |
| KM-001 | A cadeia é `COBOL Frontend → COBOL Semantic Product → CobolLower → Analysis IR → CFG → Statement Effects / Storage Semantics → Reaching Definitions → Possible Values → Dependency Facts`. | `PARCIAL` | `pipeline.md`, política de impacto e backlog mencionam partes da cadeia, mas não distinguem Semantic Product, lowering e IR como fronteiras sucessivas. | `docs/architecture/pipeline.md`; dependências executáveis futuras em `docs/work/backlog.md`. | CP2 + CP4 |
| KM-002 | O Semantic Product é COBOL-specific; neutralidade entre linguagens começa no lowering/Analysis IR, não no produto de frontend. | `PARCIAL` | Evidência histórica do Discovery e spec ativa registram COBOL-specific, mas não há decisão arquitetural durável sobre o ponto de neutralidade. | Novo ADR aceito, indexado em `docs/architecture/decisions/index.md`; síntese no pipeline. | CP2 |
| KM-003 | Semantic Product é boundary materializada A2+B: tipos próprios, imutável, fechado, namespaced e facade read-only. | `PARCIAL` | Está no work item e na história; os invariantes duráveis ainda não protegem a boundary de produção. | Novo ADR da direção de frontend/lowering e novos invariantes em `docs/architecture/invariants.md`; contrato detalhado no work item. | CP2 + CP3 |
| KM-004 | Semantic Product não é cópia 1:1 da AST, CFG, IR, resolver, serializer, snapshot, bag genérico nem schema universal. | `PARCIAL` | Restrições existem no work item estreito e na história, não como regra durável da boundary. | ADR/invariantes de Semantic Product; `spec.md` em Fora de escopo. | CP2 + CP3 |
| KM-005 | Vertical slice limita capability semântica, não cardinalidade de ocorrências na `ProgramUnit`. | `CONFLITA` | `spec.md`, `state.md`, `work-item.yaml`, tipos e adapter atuais limitam a uma DATA, um MOVE e um CALL. O código é estado implementado/prova anterior, não direção futura. | Invariante arquitetural; regra essencial curta em `AGENTS.md`; `spec.md` e `eval.md` do work item. | CP2 + CP3 |
| KM-006 | Todas as ocorrências cobertas de DATA/MOVE/CALL/IF devem ser publicadas; cardinalidade só é limitada pela entrada e por escala, não pelo contrato. | `CONFLITA` | Projector atual usa `single(...)` e `State.move()`/`call()`; isso deve permanecer fato histórico até a remediation de código. | `spec.md`, `plan.md`, `eval.md` e `state.md`; próximo checkpoint de código no plano. | CP3 |
| KM-007 | O container cresce por famílias/coleções tipadas de facts, não por campos singleton `move`, `call`, `if`, `evaluate`, `perform` etc. | `NOVO` | State/port implementados são acoplados ao primeiro fixture. Representação flat, hierárquica ou híbrida continua aberta. | `spec.md` como requisito de extensibilidade; oracle e remodelagem no `plan.md`; invariant protege a propriedade, sem congelar classe/shape. | CP2 + CP3 |
| KM-008 | `ProgramPoint` representa ordem estável de traversal/source, não execution order, reachability ou CFG; cada statement precisa de anchor próprio e relações estruturais tipadas. | `PARCIAL` | INV-AST-003 já rejeita inferir ordem textual/execução de AST ID; spec atual usa um `Ordering(move, call)` especial. | Invariante da boundary e `spec.md`; oracle no `eval.md`. | CP2 + CP3 |
| KM-009 | Known, unknown, partial, unsupported, ambiguous e input missing são informação positiva; ausência de fact não pode significar ausência de código. | `PARCIAL` | ADR-0008/INV-COV-001/003 cobrem incompletude nas fases atuais, mas não exigem inventário completo da `ProgramUnit` no Semantic Product. | Reforço/novo invariant de projeção completa; `spec.md` e eval de no-silent-omission. | CP2 + CP3 |
| KM-010 | Coverage semântica e cardinalidade são dimensões distintas; claim global não pode exceder facts individuais. | `PARCIAL` | Coverage atual é explícita, mas o work item usa a fixture controlada como domínio máximo. | Invariante; disciplina de construct/readiness no work item e eval. | CP2 + CP3 |
| KM-011 | O projector projeta/reconcilia fatos canônicos; não reparseia, resolve por nome, recalcula gaps, infere runtime nem cria análise paralela. | `PARCIAL` | ADR-0003 e invariantes proíbem mutação/reparse em fases existentes. O adapter atual já evita reparse/resolução, mas não usa o report como autoridade de gaps/readiness. | Invariante de projection; routing em `AGENTS.md`; `spec.md` define autoridades e o Checkpoint corretivo do projector. | CP2 + CP3 |
| KM-012 | AST tipada é autoridade de surface/shape; units de namespace; symbols de declarations; occurrences de roles; resolution de binding/candidates/status; report de gaps/readiness/claims; provenance/policy de seus produtos canônicos. | `PARCIAL` | Fontes estão documentadas separadamente. O work item estreito chama report de entrada, mas a implementação atual não o consome. | Seção de autoridades em `spec.md`; invariant impede projector de substituir autoridades; `state.md` registra a divergência implementada. | CP2 + CP3 |
| KM-013 | Presentation/snapshots nunca são autoridade semântica; a boundary permanece isolada de parser, ANTLR, `SourceMap` completo, `ExplorerMain` e providers vivos. | `PARCIAL` | ADR-0003, INV-AST-001/002 e Discovery sustentam; falta proteção explícita da boundary Semantic Product. | ADR/invariantes; `spec.md` preserva A2+B e architecture gate futuro. | CP2 + CP3 |
| KM-014 | Binding nominal não é valor de runtime; `CALL WS-PGM` não ganha target final por `MOVE`, `VALUE` ou proximidade textual. | `EXISTE` | ADR-0004 e INV-RES-002 são canônicos; o novo fluxo precisa referenciá-los sem duplicar sua norma. | Referência no novo ADR/pipeline e no work item/evals; backlog mantém possible-values e CALL dinâmico downstream. | CP2 + CP3 + CP4 |
| KM-015 | `DataItemId` nominal não deve ser assumido como storage físico definitivo; `REDEFINES`/`RENAMES` podem criar alias/overlap. | `PARCIAL` | `semantic-ast.md` e `BACKLOG-AST-001` dizem que binding estrutural não é layout; `BACKLOG-DF-001` menciona regiões. Falta restrição arquitetural explícita para lower/IR/dataflow. | Invariante durável; dependência de storage semantics no backlog. | CP2 + CP4 |
| KM-016 | Downstream sufficiency dirige o contrato: surface, identity, structure, nominal binding, CFG readiness, effects/dataflow readiness, unknowns, provenance e coverage devem ser avaliados por construct. | `NOVO` | O Discovery possui matriz histórica, mas o work item de produção não exige a disciplina completa. | Pipeline/invariant de readiness; matriz normativa em `spec.md`; oracles em `eval.md`. | CP2 + CP3 |
| KM-017 | Lowering readiness: consumer que conhece apenas `CobolSemanticPort` reconstrói a parte suportada sem AST, SymbolTable, occurrences, resolver ou report. | `PARCIAL` | A boundary estreita prova apenas o fixture linear. | Critério do work item; Checkpoints de oracle, composition root/consumer e review em `plan.md`; eval independente. | CP3 |
| KM-018 | CFG readiness: para construct marcado ready, o lowerer enumera conservadoramente todos os successors sem voltar à AST; desconhecido não vira fallthrough. | `NOVO` | CFG continua futuro; IF já tem shape estrutural suficiente, demais constructs têm gaps diferentes. | Pipeline/invariant; matriz do work item; dependências em `BACKLOG-CFG-001` e handoffs por construct. | CP2 + CP3 + CP4 |
| KM-019 | Effects/dataflow readiness: operands e roles permitem derivar reads/writes sem frontend; o Semantic Product não publica `GEN/KILL`. | `NOVO` | EVALs existentes cobrem roles nominais, não suficiência do Semantic Product. | Pipeline/invariant; matriz/eval do work item; `BACKLOG-DF-001`. | CP2 + CP3 + CP4 |
| KM-020 | Reaching definitions é o primeiro driver downstream concreto; possible-values e targets dinâmicos vêm depois de CFG + effects/storage + reaching definitions. | `PARCIAL` | Backlog já encadeia CFG → DF → possible-values → CALL, mas não liga esse driver ao contrato do Semantic Product/lowering. | Oracle arquitetural em `eval.md`; dependências explícitas no backlog. | CP3 + CP4 |
| KM-021 | Oracle mínimo ampliado: `MOVE 'A'`, `IF/ELSE` com `MOVE 'B'`/`MOVE 'C'`, depois `CALL WS-X`; futuro RD no CALL é `{MOVE B, MOVE C}` e possible-values é `{B,C}`. | `NOVO` | O oracle atual é apenas `MOVE 'PGMA' → CALL WS-PGM`. | `eval.md` do work item e primeiro checkpoint corretivo do `plan.md`; backlog usa a cadeia sem alegar implementação. | CP3 + CP4 |
| KM-022 | IF/ELSE entra no slice ampliado: shape, condition surface, branches, nesting, references/bindings, termination, provenance e coverage; sem truth, reachability, CFG edge ou probability. | `PARCIAL` | `Ast.IfStatement` e collector já preservam estrutura/references; Semantic Product não publica IF. Discovery classificou shape como suficiente. | `spec.md`, `plan.md`, `eval.md` e `state.md`. | CP3 |
| KM-023 | Multiple DATA: publicar todas as declarations exigidas pelos facts suportados, com identity, nome, PIC quando disponível, provenance e partialidade. | `CONFLITA` | Estado atual aceita lista, mas projector publica exatamente uma declaração escolhida pelo join MOVE/CALL. | `spec.md`, oracle/evals e checkpoints de remodelagem/projector. | CP3 |
| KM-024 | Multiple MOVE: publicar todas as ocorrências cobertas; formas parciais/unsupported observadas permanecem inventariadas e não derrubam nem somem com a unit inteira. | `CONFLITA` | Projector atual exige exatamente um MOVE e falha fora do slice. | `spec.md`, coverage discipline e evals; remediation no `plan.md`. | CP3 |
| KM-025 | Multiple CALL variável: publicar todas as ocorrências suportadas com operand, syntax, binding, program point, provenance e runtime target unknown. | `CONFLITA` | Projector atual exige exatamente um CALL e o mesmo DATA do MOVE. | `spec.md`, evals e remediation no `plan.md`. | CP3 |
| KM-026 | EVALUATE é o enrichment de control-flow posterior a IF, devido a predicate/validation e F-01 ainda abertos. | `PARCIAL` | Backlog de conditions/F-01 e Discovery registram gaps, mas não há handoff após o Semantic Product ampliado. | Novo item/ordenação explícita no backlog, dependente de readiness/frontend aplicável. | CP4 |
| KM-027 | PERFORM é posterior; `TIMES`, test mode, `VARYING`, `AFTER` e papéis ordenados precisam de enriquecimento antes de lowering exato. | `PARCIAL` | F-SP-007 está comprovado na história e parte do AST já tem `PerformControl`; backlog CFG cita PERFORM sem handoff do gap do Semantic Product. | Backlog/handoff específico, ligado a F-SP-007 e CFG readiness. | CP4 |
| KM-028 | GO TO, GO TO DEPENDING ON, terminal semantics, ALTER e SEARCH exigem slices separados de surface/readiness antes ou junto de lowering/CFG, conforme seus gaps reais. | `PARCIAL` | `BACKLOG-CFG-001/002` lista constructs, mas não explicita a pré-condição de Semantic Product/CobolLower para cada família. | Backlog com cadeia de dependências e postura conservadora; sem congelar IR. | CP4 |
| KM-029 | `CobolLower` traduz fatos COBOL-specific para Analysis IR e não reabre AST nem resolve nomes. | `NOVO` | Só aparece conceitualmente em história/work item; não há backlog próprio. | Novo backlog de lowering com precondição de lowering-readiness. | CP4 |
| KM-030 | Analysis IR representa operações/controle adequados a CFG/dataflow; não deve ser desenhada prematuramente nesta migração. | `PARCIAL` | Taxonomia downstream nomeia IR, mas backlog começa direto em CFG. | Novo backlog de IR dependente do CobolLower; texto deliberadamente não congela nodes/schema. | CP4 |
| KM-031 | CFG é derivada da IR e depende de CFG readiness por construct; Semantic Product não publica edges. | `PARCIAL` | `BACKLOG-CFG-001` separa CFG da AST/binding, mas não cita IR/lowering nem readiness. | Atualizar `BACKLOG-CFG-001` e pipeline. | CP2 + CP4 |
| KM-032 | Statement Effects/Storage Semantics e Reaching Definitions são produtos posteriores separados; storage regions/aliases precedem claims corretas de dataflow. | `PARCIAL` | `BACKLOG-DF-001` combina effects e reaching definitions em texto curto; cadeia precisa ficar explícita sem fechar design. | Enriquecer backlog/dependências e pipeline; manter design aberto. | CP2 + CP4 |
| KM-033 | Possible Values conserva known values + unknown remainder e depende de facts de dataflow apropriados; target dinâmico é consumer, não análise do projector. | `EXISTE` | `BACKLOG-DF-003/002` já contém a maior parte; falta ligar formalmente a nova cadeia Semantic Product/lowering/IR. | Referências/dependências no backlog atualizado. | CP4 |
| KM-034 | Dependency Facts são a última transformação e não podem derivar somente de literal/candidate nominal; consumers externos não implementam mini-dataflow. | `EXISTE` | `BACKLOG-DEPS-001` e extractors já preservam essa fronteira; falta integrar CobolLower/IR na rota completa. | Backlog atualizado sem duplicar regra. | CP4 |
| KM-035 | Determinismo de transporte em execuções equivalentes não é identidade persistente entre edição/estrutura/analyzer/contract versions. | `PARCIAL` | Está detalhado no work item e sustentado por INV-DET-001, mas não como guardrail arquitetural geral da boundary. | Invariante durável; `spec.md`/eval; JSON somente após estrutura correta. | CP2 + CP3 |
| KM-036 | JSON é adapter de transporte posterior ao contrato estrutural; envelope extensível, versionado e determinístico, nunca domínio/IR/schema universal. | `PARCIAL` | Work item atual antecipa JSON como Checkpoint 5 antes de corrigir cardinalidade. | `spec.md`, novo `plan.md` com JSON depois de core/projector/IF/coverage/consumer; eval de transporte. | CP3 |
| KM-037 | Boundary e projector têm dependency direction explícita: projection → boundary permitido; boundary → projection/frontend proibido. | `PARCIAL` | Código atual mantém tipos boundary-owned, mas adapter está no mesmo package e architecture gate não protege a seam completa. | Invariante/ADR; `spec.md`; checkpoint corretivo decide package/nome exatos sem congelá-los agora. | CP2 + CP3 |
| KM-038 | Nome estável de produção deve refletir responsabilidade do projector; `CobolMoveCallAdapter` e tipos experimentais permanecem fatos transitórios, não seam arquitetural futura. | `CONFLITA` | Código e docs ativos usam o nome do fixture. | `spec.md`, `plan.md` e `state.md` descrevem rename futuro; CP5 remove implicações ativas fora da história. | CP3 + CP5 |
| KM-039 | Flat, hierárquico ou híbrido para branches continua decisão de implementação; qualquer forma deve reconstruir nesting/regions por identidade tipada, não posição incidental. | `NOVO` | Documento temporário lista candidatas, sem decisão. | `spec.md` preserva o critério e deixa a shape para o checkpoint de oracle/remodelagem; não vira ADR. | CP3 |
| KM-040 | Prazo e sequência recomendada não autorizam modelar todo COBOL: slice ampliado cobre multiple DATA/MOVE/CALL + IF/ELSE; demais constructs seguem vertical slices. | `CONFLITA` | Work item ativo restringe o próprio slice a uma occurrence e exclui control constructs. | `spec.md`, `plan.md`, `state.md`; backlog para slices posteriores. O prazo transitório não vira arquitetura. | CP3 + CP4 |
| KM-041 | Anti-goals: não implementar IR/CFG/dataflow/possible-values/storage completo, supermodelo multi-language, plugin framework, refactor amplo de AST nem todos os gaps nesta remediation. | `PARCIAL` | Restrições estão dispersas no work item atual. | `must_not_change`, Fora de escopo e plano corretivo; backlog não concede autorização. | CP3 + CP4 |
| KM-042 | Gates por checkpoint devem testar core extensível, projection canônica, no silent omission, CFG/effects readiness e transporte determinístico. | `NOVO` | Gates estáveis existem, mas os oracles específicos pertencem ao work item. | `eval.md` + critérios dos checkpoints em `plan.md`; catálogo canônico somente quando houver testes executáveis. | CP3 |
| KM-043 | “Pequeno em coverage, completo no que afirma, sem limite artificial de cardinalidade e preparado para downstream” é o critério de sucesso. | `NOVO` | Não há formulação canônica consolidada. | Invariante/arquitetura e objetivo/aceitação do work item. | CP2 + CP3 |
| KM-044 | A migração documental não altera produção; o primeiro trabalho autorizado depois dela é o oracle executável do target model. | `NOVO` | Estado ativo aponta review do Checkpoint 3 e antigo Checkpoint 4. | `plan.md`, `state.md`, `work-item.yaml` e índice ativo. | CP3 |
| KM-045 | O documento temporário e este mapa só podem ser removidos após auditoria item a item e busca de referências/claims antigos. | `NOVO` | Regra existe apenas no documento temporário e na task autorizada. | Evidência transitória neste mapa; execução/auditoria CP5; remoção CP6. | CP5 + CP6 |

## Destinos realizados no Checkpoint 2

| IDs | Destino efetivo |
| --- | --- |
| KM-001–KM-004, KM-014, KM-016, KM-029–KM-032, KM-043 | ADR-0013 e `docs/architecture/pipeline.md` fixam a fronteira COBOL-specific, o início da neutralidade no lowering/IR, a cadeia downstream e sufficiency como driver, sem implementar as fases futuras. |
| KM-005–KM-010, KM-017–KM-019 | INV-SP-001/002/003 fixam capability != cardinality, publicação de todas as ocorrências cobertas, no-silent-omission e readiness separada por construct. |
| KM-011–KM-013, KM-037 | INV-SP-003/004 e ADR-0013 fixam isolamento A2+B, autoridades canônicas e projector sem nova análise; nomes/packages concretos continuam abertos. |
| KM-015 | INV-SP-005 fixa que identidade nominal DATA não é storage identity. |
| KM-035 | INV-SP-006 fixa determinismo de transporte sem identidade persistente. |
| KM-008 | INV-SP-003 e o pipeline fixam program point/anchor estrutural sem claim de execução/CFG. |
| Routing | `AGENTS.md`, `docs/index.md`, `docs/architecture/index.md` e `ARCHITECTURE.md` encaminham Semantic Product/lowering/readiness às fontes novas sem copiar o contrato detalhado. |

Os itens cujo destino principal é o contrato do work item (CP3), backlog/handoff
(CP4) ou auditoria/remoção (CP5–CP6) permanecem abertos neste mapa.

## Destinos realizados no Checkpoint 3

| IDs | Destino efetivo |
| --- | --- |
| KM-005–KM-010, KM-016–KM-025, KM-035–KM-044 | `spec.md` fixa cardinalidade irrestrita dentro da capability, container extensível, matriz de readiness, IF/ELSE, oracle downstream, incompletude, authorities, A2+B e transporte posterior; `eval.md` torna essas propriedades falsificáveis. |
| KM-020–KM-021 | `spec.md` e `eval.md` registram o oracle `MOVE → IF/ELSE → CALL` e a suficiência necessária para que reaching definitions e possible-values sejam calculados somente downstream. |
| KM-026–KM-034 | `spec.md` e `plan.md` delimitam EVALUATE/PERFORM e a cadeia lower/IR/CFG/effects/dataflow como handoffs; seus destinos duráveis de backlog permanecem para o Checkpoint 4. |
| KM-037–KM-039 | `spec.md`, `plan.md` e `eval.md` exigem expansão por famílias tipadas e relações estruturais, vedam novos singletons e deixam flat/hierárquico/híbrido como decisão de implementação orientada pelo consumer. |
| KM-042 | `eval.md` define as classes positivas, negativas, ambíguas, adversariais, regressões, propriedades metamórficas e escala; `plan.md` associa gates e evidências a cada checkpoint. |
| KM-044 | `work-item.yaml`, `plan.md`, `state.md` e `docs/work/index.md` identificam o oracle executável do target model, sem mudança de produção, como próximo trabalho autorizado após esta migração documental. |
| KH-001–KH-006 | `state.md` separa o verde histórico/implementado das correções planejadas; `spec.md` preserva o fixture linear como prova N=1 e usa a surface atual de IF apenas no alcance demonstrado. |
| KG-001–KG-010 | `spec.md`, `plan.md`, `eval.md` e `work-item.yaml` materializam os guardrails do slice; os vínculos com backlog são concluídos no Checkpoint 4. |

## Destinos realizados no Checkpoint 4

| IDs | Destino efetivo |
| --- | --- |
| KM-026 | `BACKLOG-SP-001` registra EVALUATE como próximo enrichment após IF/ELSE, condicionado por F-01 e sem promover surface parcial a predicate semantics completa. |
| KM-027 | `BACKLOG-SP-002` registra PERFORM e os gaps de TIMES, test mode, VARYING, FROM/BY/UNTIL e AFTER como precondições de readiness. |
| KM-028, KM-031 | `BACKLOG-SP-003/004` e `BACKLOG-CFG-001/002` preservam GO TO, GO TO DEPENDING ON, terminal semantics, ALTER e SEARCH por slices dependentes da CFG readiness de cada construct. |
| KM-029–KM-030 | `BACKLOG-LOWER-001` e `BACKLOG-IR-001` preservam CobolLower e Analysis IR como fases distintas, incrementais e ainda sem schema prematuro. |
| KM-032–KM-034 | `BACKLOG-DF-001`, `BACKLOG-DF-004`, `BACKLOG-DF-003`, `BACKLOG-DF-002` e `BACKLOG-DEPS-001` fixam a ordem effects/storage → reaching definitions → possible-values → targets dinâmicos/dependency facts. |
| KM-015, KG-007 | `BACKLOG-DF-001` exige storage/layout/alias explícito para REDEFINES/RENAMES e rejeita `DataItemId` nominal como storage físico definitivo. |
| KG-004–KG-006 | Os handoffs de lower/CFG/dataflow proíbem reconsulta ao frontend, fallthrough presumido e target de runtime derivado de binding nominal. |
| F-01 | `BACKLOG-RES-003` foi reavaliado para `BLOCKS_SEMANTIC_PRODUCT` contra a boundary já definida, preservando que classificação não autoriza remediação. |

## Contexto histórico que não deve virar norma nova

| ID | Contexto | Tratamento |
| --- | --- | --- |
| KH-001 | O fixture `01 WS-PGM` / `MOVE 'PGMA'` / `CALL WS-PGM` provou closure, identities, binding, provenance, ordering e runtime unknown. | Preservar em `WORK-SEMANTIC-PRODUCT-001` e seus relatórios; a nova spec o chama de prova de boundary, não domínio máximo. |
| KH-002 | O core e o adapter de produção atuais implementam exatamente uma DATA/MOVE/CALL, com `State.move()`, `State.call()`, `Ordering` e `single(...)`. | Registrar factualmente em `state.md`; não alterar Java nesta migração. O plano autoriza a correção futura em checkpoints próprios. |
| KH-003 | O adapter atual não consulta `ResolutionAnalysisReport`; projeta o gap de CALL diretamente a partir de `CallSemantics`. | Registrar como divergência implementada a corrigir no checkpoint de projector, sem falsificar o verde conhecido. |
| KH-004 | `Ast.IfStatement` já contém condition, branches, termination e nesting; o collector percorre referências em condition/then/else. | Evidência para incluir IF/ELSE no slice, sem alegar predicate semantics, CFG ou reachability. |
| KH-005 | Discovery classificou IF como estruturalmente suficiente, EVALUATE como parcial, PERFORM controls como bloqueio F-SP-007 e terminal/ALTER/SEARCH com gaps próprios. | Preservar história; transformar somente as dependências futuras relevantes em backlog/spec. |
| KH-006 | A recomendação H/A2+B e os oracles 3A/3B são decisões/evidências anteriores aprovadas. | Preservar a história e promover apenas os princípios duráveis necessários; não reescrever os relatórios históricos. |
| KH-007 | O prazo de 15 de setembro motivou o tamanho do slice. | Contexto transitório, não arquitetura nem backlog permanente. |

## Anti-goals e sinais de regressão

Cada item deve aparecer no destino indicado; não basta classificá-lo como
“absorvido”.

| ID | Guardrail | Destino |
| --- | --- | --- |
| KG-001 | Novo construct não adiciona singleton ao State. | INV + spec/eval. |
| KG-002 | Projector não resolve/reparseia/recalcula análise. | INV + spec/eval. |
| KG-003 | Unsupported/partial observado não desaparece. | INV + spec/eval. |
| KG-004 | Lowerer não consulta AST/resolver/report para descobrir structure/binding. | Pipeline/ADR + lowering-readiness eval. |
| KG-005 | Unknown não vira empty success/fallthrough. | INV + spec/eval + backlog CFG. |
| KG-006 | Nominal binding não vira runtime target. | ADR-0004/INV-RES-002 + spec/eval. |
| KG-007 | Data identity não vira storage identity sem modelo de alias/layout. | INV + backlog storage/effects. |
| KG-008 | JSON não antecede state/container/consumer corretos. | plan + eval. |
| KG-009 | Um fixture não vira domínio máximo. | INV + AGENTS routing + spec. |
| KG-010 | A migração não implementa Semantic Product, projector, lowerer, IR, CFG ou dataflow. | work-item `must_not_change` + review dos diffs dos seis checkpoints. |

## Cobertura das seções da fonte transitória

| Seção da fonte | IDs deste mapa |
| --- | --- |
| 1 — motivo | KM-005, KM-006, KH-001, KH-002 |
| 2 — objetivo arquitetural e responsabilidades | KM-001 a KM-004, KM-014, KM-029 a KM-034 |
| 3 — coverage != cardinality | KM-005 a KM-010 |
| 4 — downstream readiness matrix | KM-016 a KM-019 |
| 5 — reaching-definitions oracle | KM-020, KM-021 |
| 6 — IF/ELSE | KM-022, KH-004 |
| 7 — DATA/MOVE/CALL/IF slice | KM-022 a KM-025 |
| 8 — State extensível | KM-007, KM-037 a KM-039 |
| 9 — program points/ordering | KM-008 |
| 10 — CFG readiness | KM-018, KM-028, KM-031 |
| 11 — dataflow readiness/effects | KM-019, KM-032 |
| 12 — nominal identity vs storage | KM-015 |
| 13 — unknown/partial/unsupported | KM-009, KM-010 |
| 14 — autoridades semânticas | KM-011 a KM-013 |
| 15 — boundary vs adapter | KM-003, KM-004, KM-037 |
| 16 — determinismo | KM-035 |
| 17 — JSON posterior | KM-036 |
| 18 — slice/deadline | KM-040, KH-007 |
| 19 — EVALUATE/PERFORM posteriores | KM-026, KM-027 |
| 20 — novo oracle | KM-021 |
| 21–23 — lowering/CFG/dataflow sufficiency | KM-017 a KM-019 |
| 24 — plano de migração | Todos os destinos acima |
| 25 — checkpoints corretivos | KM-044 e futura versão de `plan.md` |
| 26 — gates | KM-042 |
| 27 — anti-goals | KM-041 e KG-001 a KG-010 |
| 28 — sinais de caminho errado | KG-001 a KG-009 |
| 29 — definição de sucesso | KM-043 e KM-021 |
| 30 — critério de exclusão | KM-045 e checklist de saída abaixo |
| 31 — referências/audit de realidade | KH-001 a KH-006 |
| 32 — regra final | KM-001, KM-005, KM-016, KM-043 |

## Checklist de saída

- [ ] KM-001–KM-045 possuem destino materializado ou descarte explícito.
- [ ] KG-001–KG-010 aparecem nos contratos indicados.
- [ ] KH-001–KH-007 continuam distinguíveis de direção normativa.
- [ ] Arquitetura geral e routing do harness foram atualizados.
- [ ] `WORK-SEMANTIC-PRODUCT-002` foi corrigido integralmente.
- [ ] Backlog/handoffs preservam a cadeia de dependências sem antecipar a IR.
- [ ] Auditoria textual não encontra cardinalidade singleton como direção ativa.
- [ ] O documento temporário não é referenciado por fonte canônica.
- [ ] A autorização explícita desta task cobre a revisão da migração e remoção
  após a auditoria dos Checkpoints 5 e 6; não há espera entre checkpoints.
- [ ] Documento temporário e este mapa foram removidos no Checkpoint 6.
