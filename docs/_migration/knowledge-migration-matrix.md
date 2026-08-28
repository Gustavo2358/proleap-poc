# Matriz de migração de conhecimento — Harness v1

Status: transitório. Esta é a matriz de controle da migração; seus destinos só
se tornam canônicos quando a fase correspondente os criar e validar.

## Convenções

- `UNMAPPED` significa que a unidade foi identificada, mas ainda não recebeu
  destino definitivo.
- `MIGRATED`, `MERGED`, `ARCHIVED`, `SUPERSEDED`,
  `OBSOLETE_WITH_REASON` e `TRANSIENT_NO_MIGRATION` serão usados somente com
  evidência na fase apropriada.
- `Destino proposto` não é uma afirmação de que o arquivo já existe.

| Fonte | Seção/unidade | ID temporário | Classificação | Validade atual | Destino proposto | ID canônico | Estado | Notas |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `README.md` | propósito, execução e navegação | KM-README-001 | OPERATIONAL_PROCEDURE | atual | `README.md` | — | MIGRATED | ponte humana curta, com exemplos e links para as fontes canônicas |
| `README.md` | source format e preprocessing | KM-README-002 | DOMAIN_RULE | validado contra código/testes | `docs/domain/source-format-and-normalization.md`, `docs/domain/preprocessing.md` | ADR-0001, ADR-0002 | MIGRATED | README ficará como ponte curta na Fase 11 |
| `README.md` | provenance | KM-README-003 | INVARIANT | validado contra código/testes | `docs/domain/provenance.md`, invariantes | INV-PROV-001, INV-PROV-002 | MIGRATED | provenance nasce no fonte físico |
| `README.md` | AST, símbolos e resolução | KM-README-004 | ARCHITECTURE_BOUNDARY | validado contra código/testes | `ARCHITECTURE.md`, pipeline, ADRs e invariantes | ADR-0003–0008, INV-* | MIGRATED | detalhes de domínio ainda serão consolidados na Fase 4 |
| `README.md` | logging | KM-README-005 | OBSERVABILITY_CONTRACT | validado contra código/testes | `docs/engineering/observability-policy.md` | — | MIGRATED | README ficará como ponte curta na Fase 11 |
| `README.md` | números do corpus | KM-README-006 | REGRESSION_BASELINE | histórico | reports arquivados + baselines executáveis | — | MERGED | não são regra COBOL |
| `specs/AGENTS.md — antlr-parse-tree-explorer.md` | §§1–6, 11–12, 21–22, 27–35, 38–39 | KM-IMP-SEM-001 | TESTING_POLICY | validado contra README, código e testes no corte atual | `docs/engineering/semantic-analysis-policy.md` | — | MIGRATED | política semântica e fail-closed |
| mesmo | §§13–20 | KM-IMP-TEST-001 | TESTING_POLICY | validado contra fixtures e testes existentes | `docs/engineering/semantic-testing.md` | — | MIGRATED | equivalência, adversarial, propriedades e oráculos |
| mesmo | §§23–24 | KM-IMP-PERF-001 | PERFORMANCE_CONTRACT | validado contra teste de escala e índices atuais | `docs/engineering/performance-policy.md` | INV-PERF-001 | MIGRATED | sem thresholds dependentes de hardware |
| mesmo | §§7–10, 25–26 | KM-IMP-ARCH-001 | ARCHITECTURE_BOUNDARY | validado contra README, política e produtos atuais | `docs/architecture/pipeline.md`, `docs/domain/provenance.md`, `docs/domain/reference-resolution.md` | a definir na Fase 3 | MIGRATED | AST, binding, CFG/dataflow, linguagens embarcadas e resolução nominal |
| mesmo | §§36–37 | KM-IMP-REVIEW-001 | OPERATIONAL_PROCEDURE | validado como política de revisão | `docs/engineering/semantic-analysis-policy.md` | — | MERGED | comentários e red flags |
| `specs/cobol-reference-resolution-policy.md` | identidade e fontes | KM-RES-POL-001 | DOMAIN_RULE | policy ID/version superseded; fontes continuam relevantes | `docs/domain/reference-resolution.md` | — | SUPERSEDED | runtime atual é `explicit-options/3.0.0` |
| mesmo | QUALIFY | KM-RES-POL-002 | ALGORITHM_CONTRACT | validado contra código/testes | domain resolução | INV-RES-001 | MIGRATED | STANDARD/EXTEND/UNSPECIFIED |
| mesmo | namespaces e CALL | KM-RES-POL-003 | SUPPORTED_SCOPE | validado contra código/testes | domain resolução, ADRs/invariantes | ADR-0004–0006 | MIGRATED | catálogo externo e fronteira de dataflow |
| mesmo | manifesto | KM-RES-POL-004 | EVAL_ORACLE | validado contra código/testes | domain resolução + catálogo de evals | INV-COV-002, EVAL-RES-COV-001 | MIGRATED | classificação conservadora das regras |
| `specs/adr/0001-comment-entry-normalization.md` | decisão integral | KM-ADR-0001 | DECISION | atual | `docs/architecture/decisions/0001-comment-entry-normalization.md` | ADR-0001 | MIGRATED | identidade e data preservadas; fonte movida |
| `specs/source-normalizer-hardening-plan.md` | fases 1–7 | KM-NORM-001 | DOMAIN_RULE | validado contra código/testes | source format, preprocessing e provenance | ADR-0001, ADR-0002, INV-PROV-* | MIGRATED | contrato atual extraído sem narrativa TDD |
| mesmo | princípios, TDD e gate | KM-NORM-002 | TESTING_POLICY | validado | semantic policy/testing + script de regressão existente | — | MIGRATED | workflow específico permanece no oracle executável |
| mesmo | commits e checkboxes concluídos | KM-NORM-003 | COMPLETED_TRANSIENT_WORK | concluído | nenhum | — | TRANSIENT_NO_MIGRATION | histórico Git é suficiente |
| `specs/semantic-model-hardening-tasklist.md` | manifesto, coverage e incompletude | KM-SEM-001 | INVARIANT | validado contra código/testes | semantic AST/domain + invariantes | INV-COV-001, INV-COV-002 | MIGRATED | coverage é first-class |
| mesmo | AST, expressões e statements | KM-SEM-002 | DOMAIN_RULE | validado contra código/testes | `docs/domain/semantic-ast.md` | INV-AST-001, INV-AST-002 | MIGRATED | supported e preserved-but-uninterpreted |
| mesmo | provenance e COPY | KM-SEM-003 | DOMAIN_RULE | validado contra código/testes | provenance/preprocessing | ADR-0002, INV-PROV-* | MIGRATED | ownership mantido |
| mesmo | TDD, regressão e observabilidade | KM-SEM-004 | EVAL_ORACLE | validado contra testes atuais | semantic testing + catálogo de evals | EVAL-AST-*, EVAL-COV-* | MIGRATED | asserts permanecem nos testes executáveis |
| `specs/reference-resolution-tasklist.md` | unidade, fronteiras e ocorrências | KM-RES-001 | ARCHITECTURE_BOUNDARY | validado contra código/testes | ADRs, invariantes e domain docs | ADR-0003, ADR-0005 | MIGRATED | identidade por unit e produto separado |
| mesmo | DATA/INDEX/PROCEDURE/FILE/PROGRAM | KM-RES-002 | DOMAIN_RULE | validado contra código/testes | reference-resolution, symbol-model, compilation-units | INV-RES-* | MIGRATED | policy runtime registrada |
| mesmo | cobertura, diagnósticos, escala e determinismo | KM-RES-003 | INVARIANT | validado contra código/testes | invariantes, performance e catálogo de evals | INV-RES-*, INV-DET-001, INV-PERF-001, EVAL-RES-REPORT-001 | MIGRATED | incerteza preservada |
| mesmo | TDD, regressão e observabilidade | KM-RES-004 | TESTING_POLICY | validado contra testes atuais | semantic testing, performance e catálogo de evals | EVAL-RES-* | MIGRATED | regra separada de evidência |
| `specs/reference-resolution-semantic-correctness-hardening-II-tasklist.txt` | hipóteses e correções | KM-HARD-II-001 | REGRESSION_BASELINE | regras vigentes validadas; execução histórica | domain resolução, invariantes e testes adversariais | EVAL-RES-DATA-*, EVAL-RES-REL-001 | MIGRATED | GLOBAL, shadowing, qualifiers e relações |
| mesmo | opções e readiness | KM-HARD-II-002 | SUPPORTED_SCOPE | validado | domain resolução + backlog de CFG/dialeto | BACKLOG-CFG-001, BACKLOG-DIALECT-001 | MIGRATED | limites de completude preservados |
| `specs/reference-resolution-semantic-correctness-hardening-III-tasklist.txt` | CALL externo e linkage | KM-HARD-III-001 | DOMAIN_RULE | validado contra código/testes | reference-resolution, ADRs/invariantes e backlog | ADR-0004, ADR-0006, BACKLOG-DIALECT-001 | MIGRATED | DYNAM, DLL/NODLL, CALLINTERFACE e incerteza |
| mesmo | método e encerramento | KM-HARD-III-002 | TESTING_POLICY | histórico reutilizável | semantic testing + testes adversariais | EVAL-RES-CALL-*, EVAL-RES-PROG-* | MERGED | execução/commits não foram promovidos |
| `specs/antlr-parse-tree-explorer-logging-tasklist.md` | níveis, MDC e lifecycle | KM-LOG-001 | OBSERVABILITY_CONTRACT | validado contra código/testes | observability policy | — | MIGRATED | logging não é diagnostics |
| mesmo | overhead e dívidas | KM-LOG-002 | PERFORMANCE_CONTRACT | validado | observability, performance e backlog | BACKLOG-OBS-002, BACKLOG-OBS-003 | MIGRATED | pendências válidas registradas |
| mesmo | checklist comum e commits condicionais sem hash | KM-LOG-003 | COMPLETED_TRANSIENT_WORK | gates confirmados no encerramento; commits condicionais não exigidos | nenhum | — | TRANSIENT_NO_MIGRATION | caixas não atualizadas não são backlog diante da evidência final |
| `specs/cbstm03d-dynamic-calls-tasklist.md` | variante e boundary de dataflow | KM-CBSTM-001 | EVAL_ORACLE | atual | resolution domain, `DynamicCallVariantTest` e backlog | BACKLOG-DF-002 | MIGRATED | fixture didática, não regra de valores |
| mesmo | passos concluídos | KM-CBSTM-002 | COMPLETED_TRANSIENT_WORK | concluído | nenhum | — | TRANSIENT_NO_MIGRATION | Git preserva execução |
| `specs/project-naming-cleanup-tasklist.md` | identidade e guarda de gramáticas | KM-NAME-001 | INVARIANT | contrato atual limitado ao rename/atribuição | semantic analysis policy + `verify-naming.sh` | — | MERGED | hashes absolutos eram guarda da execução histórica |
| mesmo | tarefas concluídas | KM-NAME-002 | COMPLETED_TRANSIENT_WORK | concluído | nenhum | — | TRANSIENT_NO_MIGRATION | não criar docs de rename |
| `specs/reference-resolution-regression-report.md` | baseline, regressão e lacunas | KM-REP-RES-001 | HISTORICAL_EVIDENCE | histórico | `docs/history/evidence/reference-resolution-regression-report.md` | — | ARCHIVED | oráculos duráveis permanecem nos testes; catálogo vem na Fase 6 |
| `specs/reference-resolution-semantic-correctness-report.md` | contraexemplos e readiness | KM-REP-HARD-001 | HISTORICAL_EVIDENCE | histórico | `docs/history/evidence/reference-resolution-semantic-correctness-report.md` | — | ARCHIVED | sustentou ADRs e domain doc |
| `specs/semantic-model-hardening-regression-report.md` | baseline, coverage e limites | KM-REP-SEM-001 | HISTORICAL_EVIDENCE | histórico | `docs/history/evidence/semantic-model-hardening-regression-report.md` | — | ARCHIVED | números não viraram semântica normativa |
| `specs/semantic-interpretation-backlog.md` | CFG e statements | KM-BACKLOG-001 | ACTIVE_BACKLOG | atual | `docs/work/backlog.md` | BACKLOG-CFG-*, BACKLOG-DF-* | MIGRATED | fonte anterior removida após consolidação |
| mesmo | linguagens embarcadas, entradas externas e operação | KM-BACKLOG-002 | ACTIVE_BACKLOG | atual | backlog + domain boundaries | BACKLOG-EMB-001, BACKLOG-RUNNER-001, BACKLOG-OBS-* | MIGRATED | sem antecipar implementação |
| `scripts/source-normalizer-regression.sh` | cenário E2E | KM-GATE-001 | EVAL_ORACLE | atual | catálogo de evals + `scripts/harness/check-full.sh` | EVAL-PROV-002 | MIGRATED | script original continua executável e foi encapsulado |
| `scripts/verify-naming.sh` | proteção de identidade | KM-GATE-002 | OPERATIONAL_PROCEDURE | atual | `scripts/harness/check-full.sh` | — | MIGRATED | gramáticas/notices e fontes históricas/transitórias ficam fora do escopo; arquivos ignorados não entram no gate |
| testes, fixtures e manifests | contratos executáveis | KM-EXEC-001 | EVAL_ORACLE | atual | `docs/evals/semantic-eval-catalog.md` | EVAL-* | MIGRATED | catálogo aponta para os asserts sem duplicá-los |
| fontes de produção | fronteiras e algoritmos | KM-CODE-001 | ARCHITECTURE_BOUNDARY | validado no corte atual | `ARCHITECTURE.md`, pipeline, ADRs, invariantes e check de bytecode | ADR-0002–0009, INV-*, EVAL-ARCH-001 | MIGRATED | código permanece evidência executável; fronteiras verificáveis não dependem só de texto |
| `specs/HARNESS_ENGINEERING_IMPLEMENTATION_PLAN.md` | plano integral | KM-HARNESS-PLAN-001 | HISTORICAL_EVIDENCE | ativo até o encerramento | `docs/work/active/WORK-HARNESS-001/` e depois `docs/history/harness-v1-migration/` | WORK-HARNESS-001 | UNMAPPED | decisão A da grill: work item ativo agora; arquivar o plano no final |
