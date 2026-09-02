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

O mapa curto está em [ARCHITECTURE.md](ARCHITECTURE.md). Fronteiras detalhadas ficam no [pipeline arquitetural](docs/architecture/pipeline.md).

## Regras universais

- Corpus, fixtures e testes são evidência; não são a especificação isolada da linguagem.
- Para regra COBOL, use a fonte oficial do dialeto configurado e registre como o projeto a representa no documento de domínio.
- Prefira algoritmos derivados da semântica a regex, busca textual ou heurística de corpus.
- Falhe de forma fechada diante de input ausente, construção não suportada ou dependência desconhecida.
- Preserve AST, símbolos, ocorrências, resolução, futuros CFG/dataflow e apresentação como produtos distintos.
- Binding nominal não autoriza inferir valores de runtime ou targets dinâmicos finais.
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
| mudança semântica transversal | [análise semântica](docs/engineering/semantic-analysis-policy.md) e [testes semânticos](docs/engineering/semantic-testing.md) |
| desempenho | [política de desempenho](docs/engineering/performance-policy.md) e domínio afetado |
| logging | [política de observabilidade](docs/engineering/observability-policy.md) |
| gate, docs ou workflow | [gates](docs/engineering/gates.md) e [protocolo de work items](docs/engineering/work-item-protocol.md) |
| CFG, dataflow ou domínio ainda inexistente | [backlog](docs/work/backlog.md); crie contrato somente quando o trabalho for autorizado |

## Trabalho ativo

O índice de trabalho está em [docs/work/index.md](docs/work/index.md). `WORK-AST-002` permanece ativo para executar, em slices independentes, o hardening revisado de `BACKLOG-AST-001`: o Slice 1 foi mergeado no PR #10 e o Slice 2 está no checkpoint de Discovery do PR #13. A implementação de F-02 exige merge/review desse Discovery e autorização explícita posterior. `WORK-COND-004` executa o Slice 4 de `BACKLOG-COND-001`: preserva a estrutura nominal completa de condition-name references na surface AST — nome, qualification IN/OF e subscripts — sem binding e sem decisão DATA/INDEX/CONDITION; está no checkpoint de Discovery e a implementação depende de review humano e autorização explícita. O Slice 1 foi concluído pelo PR #15 e arquivado como `WORK-COND-001`, a decisão arquitetural do Slice 2 foi promovida pelo PR #16 e arquivada como `WORK-COND-002`, e o Slice 3 foi concluído pelo PR #17 e arquivado como `WORK-COND-003`. `WORK-AST-003` foi concluído: o PR #11 fechou o Discovery de IDs/traversal e o PR #12 implementou a correção, removendo aquele bloqueio de `WORK-AST-002`. As conclusões de WORK-EXT-001 e WORK-COV-001 são baselines válidas, mas não autorizam iniciar taint localizado, itens `BACKLOG-EXT`, CFG, dataflow ou outras tecnologias.

Ao trabalhar em um item:

1. leia primeiro `work-item.yaml` e `state.md`;
2. carregue somente os caminhos em `must_read` relevantes ao slice atual;
3. consulte `spec.md`, `plan.md` e `eval.md` conforme a decisão em curso;
4. mantenha `state.md` curto e factual quando o estado material mudar;
5. não transforme detalhes transitórios em documentação canônica.

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
