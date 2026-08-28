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
| `README.md` | propósito, execução e navegação | KM-README-001 | OPERATIONAL_PROCEDURE | atual | `README.md` | — | UNMAPPED | reduzir duplicação sem remover instruções de uso |
| `README.md` | source format e preprocessing | KM-README-002 | DOMAIN_RULE | atual | `docs/domain/source-format-and-normalization.md`, `docs/domain/preprocessing.md` | a definir | UNMAPPED | README ficará como ponte curta |
| `README.md` | provenance | KM-README-003 | INVARIANT | atual | `docs/domain/provenance.md`, invariantes | INV-PROV-* | UNMAPPED | provenance nasce no fonte físico |
| `README.md` | AST, símbolos e resolução | KM-README-004 | ARCHITECTURE_BOUNDARY | validado contra código/testes | `ARCHITECTURE.md`, pipeline, ADRs e invariantes | ADR-0003–0008, INV-* | MIGRATED | detalhes de domínio ainda serão consolidados na Fase 4 |
| `README.md` | logging | KM-README-005 | OBSERVABILITY_CONTRACT | atual | `docs/engineering/observability-policy.md` | a definir | UNMAPPED | conteúdo atual é suficiente para doc próprio |
| `README.md` | números do corpus | KM-README-006 | REGRESSION_BASELINE | histórico | histórico/evals quando útil | — | UNMAPPED | não são regra COBOL |
| `specs/AGENTS.md — antlr-parse-tree-explorer.md` | §§1–6, 11–12, 21–22, 27–35, 38–39 | KM-IMP-SEM-001 | TESTING_POLICY | validado contra README, código e testes no corte atual | `docs/engineering/semantic-analysis-policy.md` | — | MIGRATED | política semântica e fail-closed |
| mesmo | §§13–20 | KM-IMP-TEST-001 | TESTING_POLICY | validado contra fixtures e testes existentes | `docs/engineering/semantic-testing.md` | — | MIGRATED | equivalência, adversarial, propriedades e oráculos |
| mesmo | §§23–24 | KM-IMP-PERF-001 | PERFORMANCE_CONTRACT | validado contra teste de escala e índices atuais | `docs/engineering/performance-policy.md` | INV-PERF-* futuro | MIGRATED | sem thresholds dependentes de hardware |
| mesmo | §§7–10, 25–26 | KM-IMP-ARCH-001 | ARCHITECTURE_BOUNDARY | validado contra README, política e produtos atuais | `docs/architecture/pipeline.md`, `docs/domain/provenance.md`, `docs/domain/reference-resolution.md` | a definir na Fase 3 | MIGRATED | AST, binding, CFG/dataflow, linguagens embarcadas e resolução nominal |
| mesmo | §§36–37 | KM-IMP-REVIEW-001 | OPERATIONAL_PROCEDURE | validado como política de revisão | `docs/engineering/semantic-analysis-policy.md` | — | MERGED | comentários e red flags |
| `specs/cobol-reference-resolution-policy.md` | identidade e fontes | KM-RES-POL-001 | DOMAIN_RULE | atual | `docs/domain/reference-resolution.md` | a definir | UNMAPPED | links IBM e dialeto |
| mesmo | QUALIFY | KM-RES-POL-002 | ALGORITHM_CONTRACT | atual | domain resolução | RULE-RES-* opcional | UNMAPPED | STANDARD/EXTEND/UNSPECIFIED |
| mesmo | namespaces e CALL | KM-RES-POL-003 | SUPPORTED_SCOPE | atual | domain resolução, ADRs/invariantes | a definir | UNMAPPED | catálogo externo e fronteira de dataflow |
| mesmo | manifesto | KM-RES-POL-004 | EVAL_ORACLE | atual | domain resolução + eval catalog | EVAL-RES-* | UNMAPPED | classificação conservadora das regras |
| `specs/adr/0001-comment-entry-normalization.md` | decisão integral | KM-ADR-0001 | DECISION | atual | `docs/architecture/decisions/0001-comment-entry-normalization.md` | ADR-0001 | MIGRATED | identidade e data preservadas; fonte movida |
| `specs/source-normalizer-hardening-plan.md` | fases 1–7 | KM-NORM-001 | DOMAIN_RULE | validar contra código/testes | source format, preprocessing e provenance | a definir | UNMAPPED | extrair contrato atual, não narrativa TDD |
| mesmo | princípios, TDD e gate | KM-NORM-002 | TESTING_POLICY | atual quando geral | semantic policy/testing + gates | a definir | UNMAPPED | não duplicar workflow específico |
| mesmo | commits e checkboxes concluídos | KM-NORM-003 | COMPLETED_TRANSIENT_WORK | histórico | nenhum | — | UNMAPPED | preservar Git; não copiar |
| `specs/semantic-model-hardening-tasklist.md` | manifesto, coverage e incompletude | KM-SEM-001 | INVARIANT | validar | semantic AST/domain + invariantes | INV-COV-* | UNMAPPED | coverage é first-class |
| mesmo | AST, expressões e statements | KM-SEM-002 | DOMAIN_RULE | validar | `docs/domain/semantic-ast.md` | a definir | UNMAPPED | supported e preserved-but-uninterpreted |
| mesmo | provenance e COPY | KM-SEM-003 | DOMAIN_RULE | validar | provenance/preprocessing | a definir | UNMAPPED | manter ownership correto |
| mesmo | TDD, regressão e observabilidade | KM-SEM-004 | EVAL_ORACLE | validar | eval catalog/engineering | EVAL-* | UNMAPPED | catalogar, não duplicar asserts |
| `specs/reference-resolution-tasklist.md` | unidade, fronteiras e ocorrências | KM-RES-001 | ARCHITECTURE_BOUNDARY | validar | ADRs, invariantes e domain docs | a definir | UNMAPPED | identidade por unit e produto separado |
| mesmo | DATA/INDEX/PROCEDURE/FILE/PROGRAM | KM-RES-002 | DOMAIN_RULE | validar | reference-resolution, symbol-model, compilation-units | a definir | UNMAPPED | policy vigente também é fonte |
| mesmo | cobertura, diagnósticos, escala e determinismo | KM-RES-003 | INVARIANT | validar | invariantes, performance e eval catalog | INV-RES-*, INV-DET-* | UNMAPPED | preservar incerteza |
| mesmo | TDD, regressão e observabilidade | KM-RES-004 | TESTING_POLICY | validar | engineering/evals/gates | EVAL-RES-* | UNMAPPED | distinguir regra de evidência |
| `specs/reference-resolution-semantic-correctness-hardening-II-tasklist.txt` | hipóteses e correções | KM-HARD-II-001 | REGRESSION_BASELINE | histórico com regras vigentes a extrair | domain resolução + eval catalog | EVAL-RES-* | UNMAPPED | GLOBAL, shadowing, qualifiers e relações |
| mesmo | opções e readiness | KM-HARD-II-002 | SUPPORTED_SCOPE | validar | domain resolução/backlog | a definir | UNMAPPED | limites de completude |
| `specs/reference-resolution-semantic-correctness-hardening-III-tasklist.txt` | CALL externo e linkage | KM-HARD-III-001 | DOMAIN_RULE | validar | reference-resolution + ADR/invariantes | a definir | UNMAPPED | DYNAM, DLL/NODLL e incerteza |
| mesmo | método e encerramento | KM-HARD-III-002 | TESTING_POLICY | histórico | semantic testing/evals | EVAL-RES-* | UNMAPPED | somente lições reutilizáveis |
| `specs/antlr-parse-tree-explorer-logging-tasklist.md` | níveis, MDC e lifecycle | KM-LOG-001 | OBSERVABILITY_CONTRACT | validar | observability policy | a definir | UNMAPPED | logging não é diagnostics |
| mesmo | overhead e dívidas | KM-LOG-002 | PERFORMANCE_CONTRACT | validar | observability/performance/backlog | a definir | UNMAPPED | registrar pendências ainda válidas |
| mesmo | checkboxes sem commit de código | KM-LOG-003 | COMPLETED_TRANSIENT_WORK | histórico | nenhum | — | UNMAPPED | não são backlog sem evidência |
| `specs/cbstm03d-dynamic-calls-tasklist.md` | variante e boundary de dataflow | KM-CBSTM-001 | EVAL_ORACLE | atual | eval catalog + resolution domain | EVAL-RES-* | UNMAPPED | fixture didática, não regra de valores |
| mesmo | passos concluídos | KM-CBSTM-002 | COMPLETED_TRANSIENT_WORK | histórico | nenhum | — | UNMAPPED | Git preserva execução |
| `specs/project-naming-cleanup-tasklist.md` | identidade e guarda de gramáticas | KM-NAME-001 | INVARIANT | atual | architecture/invariants + engineering/gates | INV-PROJ-* | UNMAPPED | gramáticas e atribuições protegidas |
| mesmo | tarefas concluídas | KM-NAME-002 | COMPLETED_TRANSIENT_WORK | histórico | nenhum | — | UNMAPPED | não criar docs de rename |
| `specs/reference-resolution-regression-report.md` | baseline, regressão e lacunas | KM-REP-RES-001 | HISTORICAL_EVIDENCE | histórico | `docs/history/evidence/` + eval catalog | EVAL-RES-* | UNMAPPED | extrair apenas oráculos/regras duráveis |
| `specs/reference-resolution-semantic-correctness-report.md` | contraexemplos e readiness | KM-REP-HARD-001 | HISTORICAL_EVIDENCE | histórico | history/evals/domain | EVAL-RES-* | UNMAPPED | evidência para ADRs retrospectivos |
| `specs/semantic-model-hardening-regression-report.md` | baseline, coverage e limites | KM-REP-SEM-001 | HISTORICAL_EVIDENCE | histórico | history/evals/domain | EVAL-AST-* | UNMAPPED | não usar números como semântica |
| `specs/semantic-interpretation-backlog.md` | CFG e statements | KM-BACKLOG-001 | ACTIVE_BACKLOG | atual | `docs/work/backlog.md` | BACKLOG-* opcional | UNMAPPED | manter fora do escopo do Harness v1 |
| mesmo | linguagens embarcadas, entradas externas e operação | KM-BACKLOG-002 | ACTIVE_BACKLOG | atual | backlog + domain boundaries | a definir | UNMAPPED | sem antecipar implementação |
| `scripts/source-normalizer-regression.sh` | cenário E2E | KM-GATE-001 | EVAL_ORACLE | atual | check-full + eval catalog | EVAL-PROV-* | UNMAPPED | encapsular, não reimplementar |
| `scripts/verify-naming.sh` | proteção de identidade | KM-GATE-002 | OPERATIONAL_PROCEDURE | atual | check-full/check-fast conforme custo | INV-PROJ-* | UNMAPPED | preservar exceção das gramáticas |
| testes, fixtures e manifests | contratos executáveis | KM-EXEC-001 | EVAL_ORACLE | atual | docs/evals catalog | EVAL-* | UNMAPPED | classificação detalhada na Fase 6 |
| fontes de produção | fronteiras e algoritmos | KM-CODE-001 | ARCHITECTURE_BOUNDARY | validado no corte atual | `ARCHITECTURE.md`, pipeline, ADRs e invariantes | ADR-0002–0009, INV-* | MIGRATED | código permanece evidência executável, não texto normativo |
| `specs/HARNESS_ENGINEERING_IMPLEMENTATION_PLAN.md` | plano integral | KM-HARNESS-PLAN-001 | HISTORICAL_EVIDENCE | ativo até o encerramento | `docs/history/harness-v1-migration/` | — | UNMAPPED | decisão A da grill: arquivar no final |
