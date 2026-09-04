# Plano — WORK-COND-007

## Fatiamento

1. Confirmar que `main` contém o merge do PR #20 e abrir a branch do work item.
2. Catalogar o corpus já versionado e executar `fast`/`semantic` antes da expansão.
3. Clonar o CardDemo fora do repositório, fixar SHA, inspecionar licença e construir triagem de todos os candidatos COBOL.
4. Selecionar aproximadamente dez programas por diversidade semântica; tentar refutar a seleção com CS-01..CS-10; então copiar somente programas e closure de COPY necessários.
5. Executar a pipeline atual sem modificar produção, registrar produtos completos/parciais, diagnósticos e timings.
6. Extrair findings por cadeia AST → occurrence → resolution, incluindo cardinalidade semântica, provenance e ausência/duplicação de nominais.
7. Para cada hipótese de bug, produzir BR-01..BR-15, minimal reproducer, contraexemplo e classificação. Derivar apenas regressions de characterization/oracle, sem antecipar correção.
8. Registrar CardDemo inventory, self-refutation, performance observacional, recomendações e gates; revisar diff e abrir PR como `NOT READY FOR MERGE`.

## Dependências

- Merge do PR #20 em `main` — confirmado em `8c6f449`.
- Acesso de leitura ao repositório público `aws-samples/aws-mainframe-modernization-carddemo`.
- Maven/dependências e entrypoints de harness existentes.
- Convenção atual de corpus em `corpus/cbl`, `corpus/cpy` e `corpus/cpy-bms`.

## Superfície arquitetural provável

O trabalho deve permanecer em `docs/work/active/WORK-COND-007`, `docs/work/index.md`, `corpus/carddemo/**`, testes de caracterização e, se necessário, harness documental. `src/main/**`, a gramática, occurrences, resolver e semantic manifest são superfícies explicitamente proibidas neste checkpoint.

## Migrações requeridas

Nenhuma. Não serão alterados manifests, baselines, formatos de produto ou contratos existentes. A única adaptação permitida é configuração de caminho no harness, documentada e separada do source upstream.

## Artefatos esperados

- `corpus/carddemo/cbl`, `corpus/carddemo/cpy`, `corpus/carddemo/cpy-bms` com somente a seleção e closure necessárias.
- `corpus/carddemo/licenses/APACHE-2.0.txt` e `AMAZON-NOTICE.txt`.
- `corpus/carddemo/provenance.md` com repositório, SHA, data, licença, seleção, hashes e transformações.
- `docs/work/active/WORK-COND-007/eval.md` com tabelas de seleção, baseline, inventory, findings, BR-01..BR-15, CS-01..CS-10, FR-01..FR-10 e recomendações.
- Teste/harness de caracterização somente se ele proteger significado durável sem codificar uma correção de produção.
- Commits append-only e PR de Discovery explicitamente não pronto para merge.
