# AGENTS.md

## Escopo

Estas instruções valem para todo o repositório.

## Propósito

O COBOL Structure Atlas transforma fonte COBOL em produtos estruturais e semânticos separados, rastreáveis e navegáveis, preservando explicitamente incerteza e cobertura incompleta.

## Pipeline

```text
fonte físico → normalização/provenance → preprocessing/COPY → parse tree ANTLR
→ AST semântica → compilation units/símbolos → ocorrências → resolução nominal
→ snapshots e apresentação
```

A fronteira física deste repositório termina no COBOL Semantic Product,
materializado também como `cobol-semantic-product.json`. A direção downstream é
cross-repo:

```text
frontend COBOL deste repositório → cobol-semantic-product.json
cobol-lower (repo separado; depende de air-java) → AIR 2.0.0 Publication
analysis-cfg (repo separado) → CFG
```

`air-java` já possui o modelo/validator Java da AIR 2.0.0. Este repositório não
implementa AIR, `CobolLower` ou CFG.

O mapa curto está em [ARCHITECTURE.md](ARCHITECTURE.md). Fronteiras detalhadas ficam no [pipeline arquitetural](docs/architecture/pipeline.md).

## Regras universais

- Corpus, fixtures e testes são evidência; não são a especificação isolada da linguagem.
- Para regra COBOL, use a fonte oficial do dialeto configurado e registre como o projeto a representa no documento de domínio.
- Prefira algoritmos derivados da semântica a regex, busca textual ou heurística de corpus.
- Falhe de forma fechada diante de input ausente, construção não suportada ou dependência desconhecida.
- Preserve AST, símbolos, ocorrências, resolução, apresentação e os produtos downstream de CFG/dataflow como fronteiras distintas.
- Binding nominal não autoriza inferir valores de runtime ou targets dinâmicos finais.
- No Semantic Product, vertical slice limita capability semântica, não a quantidade de ocorrências suportadas na `ProgramUnit`; partial/unsupported observado não pode desaparecer.
- Projectors/adapters somente traduzem fatos canônicos e seus estados de readiness; não executam nova análise, reparsing ou resolução.
- Não enfraqueça fixture, baseline, manifesto ou gramática apenas para fazer um teste passar.
- Em mudança semântica não trivial, identifique antes a regra, o invariant/ADR e o eval aplicáveis.
- Quando houver work item ativo, respeite `source_scope`, `must_not_change` e os gates declarados nele.
- Backlog descreve trabalho futuro; não é autorização para iniciá-lo.

## Roteamento inicial

Comece no [índice de conhecimento](docs/index.md). Carregue somente o contexto exigido pela tarefa e pelo work item; amplie a leitura quando uma dependência concreta aparecer.

| Tarefa | Contexto inicial |
| --- | --- |
| mapa do pipeline ou dependências | [arquitetura](docs/architecture/index.md) e invariantes relacionados |
| source format ou normalização | [source format](docs/domain/source-format-and-normalization.md) e [provenance](docs/domain/provenance.md) |
| preprocessing ou COPY | [preprocessing](docs/domain/preprocessing.md) e provenance |
| AST ou coverage | [AST semântica](docs/domain/semantic-ast.md), invariantes e evals citados |
| compilation units | [compilation units](docs/domain/compilation-units.md) |
| símbolos ou ocorrências | [modelo de símbolos](docs/domain/symbol-model.md) |
| resolução ou CALL | [resolução de referências](docs/domain/reference-resolution.md) e política semântica |
| Semantic Product ou readiness local do frontend | [pipeline](docs/architecture/pipeline.md), [contrato do Semantic Product](docs/domain/cobol-semantic-product.md), [ADR-0013](docs/architecture/decisions/0013-cobol-semantic-product-precedes-language-neutral-lowering.md) e invariantes `INV-SP-*` |
| mudança semântica transversal | [análise semântica](docs/engineering/semantic-analysis-policy.md) e [testes semânticos](docs/engineering/semantic-testing.md) |
| impacto downstream de finding semântico | [classificação de impacto](docs/engineering/downstream-impact-classification.md) |
| desempenho | [política de desempenho](docs/engineering/performance-policy.md) e domínio afetado |
| logging | [política de observabilidade](docs/engineering/observability-policy.md) |
| gate, docs ou workflow | [gates](docs/engineering/gates.md) e [protocolo de work items](docs/engineering/work-item-protocol.md) |
| AIR, lowering, CFG ou dataflow downstream | [pipeline cross-repo](docs/architecture/pipeline.md) e [backlog/handoffs](docs/work/backlog.md); não implemente esses sistemas neste repositório |

## Trabalho ativo

O índice de trabalho está em [docs/work/index.md](docs/work/index.md). `WORK-AST-002` permanece ativo: Slice 1 mergeado no PR #10, Discovery do Slice 2 no PR #13 e implementação F-02 integrada pelo PR #28. `WORK-SEMANTIC-PRODUCT-003` concluiu o audit bilateral contra AIR 2.0.0 no PR #29 e está arquivado; a [matriz e os findings](docs/architecture/semantic-product-air-v2-audit.md) e os [oracles futuros](docs/evals/semantic-product-air-v2-oracles.md) permanecem canônicos, sem autorizar produção. `WORK-SEMANTIC-PRODUCT-004` foi concluído pelo PR #31 e arquivado. `WORK-SEMANTIC-PRODUCT-005` está ativo exclusivamente para Checkpoint 4A (E1–E4 do MOVE textual escalar); parar para review humano sem merge. `BACKLOG-IR-001`, `BACKLOG-LOWER-001` e `BACKLOG-CFG-001` permanecem somente como registros/handoffs cross-repo para `air-java`, `cobol-lower` e `analysis-cfg`. `WORK-COND-004` concluiu o Slice 4 pelo PR #18 e `WORK-COND-005` concluiu o Slice 5 pelo PR #19; ambos estão arquivados com resumos históricos. Os Slices 1–3 foram concluídos pelos PRs #15–#17 e arquivados como `WORK-COND-001` a `WORK-COND-003`. `WORK-AST-003` foi concluído: o PR #11 fechou o Discovery de IDs/traversal e o PR #12 implementou a correção, removendo aquele bloqueio de `WORK-AST-002`. As conclusões de WORK-EXT-001 e WORK-COV-001 são baselines válidas, mas não autorizam iniciar taint localizado, itens `BACKLOG-EXT`, CFG, dataflow ou outras tecnologias.

Ao trabalhar em um item:

1. leia primeiro `work-item.yaml` e `state.md`;
2. carregue somente os caminhos em `must_read` relevantes ao slice atual;
3. consulte `spec.md`, `plan.md` e `eval.md` conforme a decisão em curso;
4. mantenha `state.md` curto e factual quando o estado material mudar;
5. antes do handoff, execute a self-validation do harness: diretórios ativos,
   índice, histórico, contratos documentais e escopo do diff devem permanecer
   coerentes;
6. não transforme detalhes transitórios em documentação canônica.

Novo trabalho ativo segue o [protocolo](docs/engineering/work-item-protocol.md). Itens concluídos deixam `active/`; conhecimento durável vai para arquitetura, domínio, engenharia ou evals.

## Contexto histórico

Não carregue `docs/history/`, tasklists, reports ou commits antigos por padrão. Consulte história somente para investigar uma decisão, verificar provenance documental ou executar uma migração que a referencie explicitamente.

Fonte histórica não prevalece sobre ADR aceito, invariant ou contrato de domínio atual. Divergência deve permanecer explícita até ser resolvida pela autoridade adequada.

## Verificação

Use os entrypoints estáveis descritos em [gates do harness](docs/engineering/gates.md):

```bash
./scripts/harness/check-fast.sh
./scripts/harness/check-semantic.sh
./scripts/harness/check-performance.sh
./scripts/harness/check-full.sh
```

- `fast`: documentação e fronteiras arquiteturais baratas.
- `semantic`: suíte Maven e contratos semânticos.
- `performance`: propriedade algorítmica, sem threshold dependente de hardware.
- `full`: fast + semantic + regressão E2E + naming.

Para mudanças documentais/estruturais, comece por `fast`. Para semântica, execute `semantic`; para encerramento ou alteração transversal, execute `full` conforme o work item.
