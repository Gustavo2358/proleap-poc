# Gates do harness

Os entrypoints em `scripts/harness/` são a interface operacional estável do Harness v1. Todos aceitam `MAVEN_BIN` para selecionar o executável Maven e informam nome e estado do gate na saída.

| Gate | Uso | Conteúdo atual |
| --- | --- | --- |
| `check-fast.sh` | feedback estrutural rápido | agrega `check-docs.sh` e `check-architecture.sh` |
| `check-docs.sh` | integridade do sistema de conhecimento | executa somente `HarnessDocsTest`: links internos, índice/IDs de ADRs e definições/referências de invariants, evals e backlog |
| `check-architecture.sh` | fronteiras arquiteturais | executa somente `ArchitectureBoundaryTest` sobre referências diretas de bytecode |
| `check-semantic.sh` | contratos semânticos | executa a suíte Maven completa, incluindo manifestos, fixtures, determinismo e provenance |
| `check-performance.sh` | propriedade algorítmica | executa o cenário focalizado EVAL-RES-PERF-001; não impõe threshold de hardware |
| `check-full.sh` | saúde geral antes de encerrar trabalho | agrega fast, semantic, regressão E2E do normalizador e verificação de naming |

Exemplos:

```bash
./scripts/harness/check-fast.sh
./scripts/harness/check-semantic.sh
./scripts/harness/check-full.sh
./scripts/harness/check-performance.sh
```

`scripts/source-normalizer-regression.sh` e `scripts/verify-naming.sh` permanecem entrypoints válidos e são encapsulados pelo full gate, sem reimplementação. O script E2E ainda executa a suíte Maven por conta própria; essa repetição é preservada nesta primeira unificação para não mudar silenciosamente o contrato do script legado.

Em falha, começar pelo nome do gate emitido e usar o [catálogo de evals](../evals/semantic-eval-catalog.md) para localizar capability, fixture, regra e invariant relacionados.
