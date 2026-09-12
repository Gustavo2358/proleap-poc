# Gates do harness

Os entrypoints em `scripts/harness/` são a interface operacional estável do Harness v1. Todos aceitam `MAVEN_BIN` para selecionar o executável Maven e informam nome e estado do gate na saída.

| Gate | Uso | Conteúdo atual |
| --- | --- | --- |
| `check-fast.sh` | feedback estrutural rápido | agrega `check-docs.sh` e `check-architecture.sh` |
| `check-docs.sh` | integridade do sistema de conhecimento | executa somente `HarnessDocsTest`: links internos, índice/IDs de ADRs e definições/referências de invariants, evals e backlog |
| `check-architecture.sh` | fronteiras arquiteturais | executa somente `ArchitectureBoundaryTest` sobre referências diretas de bytecode |
| `check-semantic.sh` | contratos semânticos | executa a suíte Maven completa, incluindo manifestos, fixtures, determinismo e provenance |
| `check-performance.sh` | propriedade algorítmica | executa os cenários EVAL-RES-PERF-001 e EVAL-SP-006 (escala física/fatos 4A); não impõe threshold de hardware |
| `check-full.sh` | saúde geral antes de encerrar trabalho | agrega fast, semantic, regressão E2E do normalizador e verificação de naming |

Exemplos:

```bash
./scripts/harness/check-fast.sh
./scripts/harness/check-semantic.sh
./scripts/harness/check-full.sh
./scripts/harness/check-performance.sh
```

`scripts/source-normalizer-regression.sh` e `scripts/verify-naming.sh` permanecem entrypoints válidos e são encapsulados pelo full gate, sem reimplementação. O script E2E ainda executa a suíte Maven por conta própria; essa repetição é preservada nesta primeira unificação para não mudar silenciosamente o contrato do script legado.

O E2E executa o runner estruturado dos artefatos gerados com o módulo nativo
`node:test`; portanto `check-full.sh` requer Node.js 18 ou superior, além de JDK
17+ e Maven 3.9+.

Em falha, começar pelo nome do gate emitido e usar o [catálogo de evals](../evals/semantic-eval-catalog.md) para localizar capability, fixture, regra e invariant relacionados.

O challenge focalizado 4A usa `python3 scripts/harness/challenge-scalar-move.py`: GREEN, mutações semânticas RED, restauração byte-identical e segundo GREEN. Logs/hashes ficam em target/checkpoint-4a/challenges; erro de compilação não conta como mutante morto.

O workflow Semantic Product gates executa full, performance e os challenges no PR,
com JDK 17 e Node 22. Foi adicionado no 4A porque o baseline não tinha workflows
GitHub Actions. Actions são fixadas por SHA; permissões apenas contents:read.

O challenge CP6 W1A usa `python3 scripts/harness/challenge-call-w1a.py`: oito mutações
focais de CALL/fitting, restauração byte-exact e segundo GREEN. Logs/hashes ficam
em target/cp6-w1a/challenges; o script recusa sobrescrever evidência existente.
O [handoff W1A](../work/evidence/WORK-AST-005/README.md) preserva os recibos locais.

O challenge CP6 W2A usa `python3 scripts/harness/challenge-if-w2a.py`: treze
mutações de completion, arms, predicate/reads/access, independência e origem,
restauração byte-exact de toda produção e segundo GREEN. O runner recusa
sobrescrever o diretório; `W2A_CHALLENGE_OUT` seleciona uma execução nova.
`IfSemanticsScaleTest` integra o performance gate com contadores N/2N,
incluindo visitas reais do builder às regiões de completion.
O workflow existente continua sem refatoração; executa full/performance e os
challenges W1. W2A tem campanha focal de qualificação local com recibo próprio;
ela não é reivindicada como execução remota do workflow.
