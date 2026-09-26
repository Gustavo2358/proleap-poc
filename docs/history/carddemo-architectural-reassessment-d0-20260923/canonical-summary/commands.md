# Comandos e parâmetros

Diretório de trabalho: `/home/gustavo/workspace/teste-e2e`; data 2026-09-23 (America/Sao_Paulo).

```sh
python3 -B artefatos-e2e/carddemo-validation-20260923/run_carddemo.py --work artefatos-e2e/carddemo-validation-20260923/preflight --select app/cbl/CBACT01C.cbl --stage-timeout-seconds 120
python3 -B artefatos-e2e/carddemo-validation-20260923/run_carddemo.py --work artefatos-e2e/carddemo-validation-20260923/full --stage-timeout-seconds 120 > artefatos-e2e/carddemo-validation-20260923/full-run.log 2>&1
python3 -B artefatos-e2e/carddemo-validation-20260923/summarize.py
```

A rodada canônica usou o `runtime-closure.json` W8, Java absoluto `/home/gustavo/.sdkman/candidates/java/21.0.12+1.1-tem/bin/java`, `-Xmx2g`, uma JVM por etapa e timeout 120 s. `run_carddemo.py` lê e confere os SHAs do source e dos cinco produtores, além da existência de cada entrada do classpath.

Para os contraprobes, o mesmo runner recebeu `--select <caminho lógico>` e `--frontend-arg=--storage-profile --frontend-arg=ibm-enterprise-6.4-fixed-display-1047@1`, gravando em diretório novo para cada fonte. Foram rodados `COPAUS2C.cbl`, `COADM01C.cbl` e os sete caminhos listados em `profile-pure-7/selection.json`. Os comandos individuais e exits constam de `profile-pure-7/case-*.log`/`measurement.json`, `profile-probe/measurements.json` e `profile-input-probe/measurements.json`.

`AirValidationProbe.java` compõe `SpFileInput` + `CobolLowerer` com os classpaths exatos do runtime W8. A segunda invocação usou retenção diagnóstica de 30.000 (em vez de 10.000 do CLI) e imprimiu somente `INVALID_IR`, para revelar a regra I-02. Não mudou a publicação AIR ou o status do validador. Classes temporárias foram compiladas em `/tmp/carddemo-air-validation-probe-20260923` e não foram versionadas.
