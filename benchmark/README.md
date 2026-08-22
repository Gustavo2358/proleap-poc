# COBOL grammar bake-off

Aplicação Java/Maven que compara exclusivamente os pares ANTLR fornecidos:

- `Cobol85Preprocessor.g4` → `Cobol85.g4`
- `CobolPreprocessor.g4` → `Cobol.g4`

Não usa ASG, metamodelo, runner nem dependência ProLeap. Os quatro arquivos oficiais na raiz não são alterados; o build os copia para `target/antlr4` e gera todos com ANTLR 4.13.2.

## Requisitos

- JDK 17+ (compilação com `--release 17`)
- Maven 3.9+

## Execução reproduzível

```bash
cd ~/workspace/proleap-poc/benchmark
./run-benchmark.sh
```

Comandos equivalentes:

```bash
mvn clean verify
mvn exec:java -Dexec.args="--warmups 2 --runs 5"
```

Para uma validação curta:

```bash
mvn exec:java -Dexec.args="--warmups 0 --runs 1"
```

O runner descobre `.cbl`/`.CBL` e `.cpy`/`.CPY` case-insensitively, interpreta o corpus como FIXED, expande COPYs a partir de `../cpy`, preserva EXEC como tokens opacos e captura todos os diagnostics sem console listeners padrão.

## Saídas

- `REPORT.md`: relatório autocontido e veredito.
- `results/files.csv`: resultado agregado por frontend/arquivo (mediana dos tempos).
- `results/summary.csv`: correção, erros normalizados, estatísticas de corpus, throughput, heap e árvore.
- `results/diagnostics.csv`: diagnostics individuais da primeira execução medida (resultados de correção são determinísticos).

Tempos do corpus são calculados por run completo e resumidos por mediana, p95 e média. Heap é uma estimativa observada por amostragem e não RSS.
