# WORK-TEST-001 — Restaurar relatório PIT focalizado

## Diagnóstico

O PIT não estava falhando por incompatibilidade entre Temurin 25.0.4, Maven 3.9.16, PIT 1.21.0 ou o plugin JUnit 5 1.2.3. O comando focalizado leva cerca de dois minutos, mas a invocação anterior usava um canal não persistente que encerrava o processo Maven perto de 30 segundos, enquanto o PIT já havia calculado coverage e começado a matar mutantes. Isso podia deixar `target/` recompilável somente após uma nova compilação e um XML observado antes do `runEnd`.

O XML `mutations.xml` tem `partial="true"` mesmo após conclusão. A inspeção do `XMLReportListener` do PIT 1.21.0 confirma que esse atributo recebe `ReportOptions.shouldReportCoverage()`; representa cobertura parcial no formato XML, não relatório truncado. A validade do relatório foi confirmada pelo `BUILD SUCCESS`, fechamento do XML, 931 mutations e artefatos HTML/XML materializados.

## Ajuste

Executar o comando focalizado em sessão persistente (PTY) e fazer polling até o exit code, em vez de encerrar a chamada inicial:

```bash
mvn -Dmaven.repo.local=/tmp/proleap-poc-m2 -Pmutation-adversarial \
  test-compile org.pitest:pitest-maven:mutationCoverage
```

Não foi necessário alterar a configuração Maven: o perfil já limita as classes a `AstBuilder*` e `ReferenceOccurrenceCollector*`, usa os três testes semânticos e produz HTML/XML.

O trabalho também encontrou uma lacuna legítima: selector `ALSO` sem subject posicional era classificado como `VALUE_COMPARISON`. Ele agora fica `OTHER`, preservando falha fechada. Os testes passaram a verificar cardinalidade de occurrences, traversal de subject/selector/statement e múltiplas posições `ALSO`.

## Resultado PIT

| Medida | Resultado |
| --- | ---: |
| Duração | 2m03s |
| Mutantes | 931 |
| Mortos | 537 (58%) |
| Sem cobertura | 235 |
| Força de testes | 77% |
| Timeouts / erros de execução | 0 / 0 |

Todos os mutantes das decisões novas de `EVALUATE` foram mortos, incluindo classificação `TRUE`/`FALSE`, limite e incremento da posição `ALSO`, fallback fechado, visita de subjects, selectors genéricos e statements de branches. Os sobreviventes restantes não pertencem a esse slice decisório.

## Verificação

- `mvn test`: 158 testes verdes.
- `check-fast`: passou.
- `check-semantic`: passou.
- `scripts/source-normalizer-regression.sh full`: passou.
- `check-full`: passou.

O primeiro `check-full` revelou somente um baseline textual residual de COACTUPC (1.245 gaps). Foi atualizado para o resultado semanticamente explicado, 1.137, e o gate foi repetido verde.
