# D0 — proposta de gate permanente CardDemo

Esta proposta não altera gates/roadmaps atuais. Corpus físico congelado de 73 (44 checkout + 29 ZIP), mesmo pin, inclusive .cl2. Cada linha possui source SHA, container/member SHA quando aplicável, configuração explícita de dialect/normalização/profile/copy precedence, pins dos cinco repos e hashes de runtime/output.

## Gate A — Pipeline Viability

Executar SOURCE → frontend/SP → lower/AIR → AIR validation → CFG → dataflow/dependency em todos os 73. Por estágio: status, código/diagnóstico de primeira perda, tempo, pico de memória quando medido, schema/capabilities, output hash e trilha de provenance. “Arquivo existe” não é PASS.

Exigir decodificação completa e consistência de identidade/referências; zero INVALID_IR, traversal estrutural completo e nenhum cutoff oculto. Obrigações semânticas que o contrato permite deixar explícitas podem manter PARTIAL; validação interrompida por budget/capability não pode ser rebatizada como sucesso estrutural. CFG e dependency precisam ser decodificáveis, válidos no contrato e associados à mesma publicação; remainders não desaparecem por sucesso do processo.

Métrica principal: `reach_dependency_valid / 73`, com breakdown PARTIAL/complete/blocked/timeout e first-loss por família/mecanismo. Baseline **4/73 PARTIAL**. Nova wave deve preservar os quatro e medir quantos primeiros blockers removeu, quantos seguintes revelou e quais perdas surgiram. Meta final: todos os casos estruturalmente admissíveis atravessam, com PARTIAL honesto.

Antes de tornar 73/73 um gate normativo de sucesso, registrar para cada caso possível invalid COBOL/dialeto fora/missing input obrigatório/corrupted archive. Exclusão de sucesso só com evidência e aprovação humana, sem retirar a linha dos 73 físicos. D0 não pediu nem concedeu exclusões. Missing COPY por si só não prova impossibilidade de SP/AIR/CFG parcial.

## Gate B — Semantic Quality

| Dimensão | Exigência por site/oracle |
| --- | --- |
| required candidates | cada target justificado preservado com suporte e provenance |
| forbidden candidates | nenhum target introduzido por cross-return, fallthrough incorreto ou merge de contexto |
| dead code | ausência de candidate só quando controle e bounds justificam; unknown não vira unreachable |
| support | distinguir observação fonte, modelo, interpretação e evidência de valor/controle |
| provenance | statement/copy/edge/transfer/context chain resolvível ao input autenticado |
| remainders | source/model/effective/control mantidos; melhoria de uma dimensão não limpa todas |
| storage/effects | alias e kill/overwrite corretos; valores unknown não conservam literais indevidos |
| integrity | todos os edges/tags auxiliares no inventário e materializados no contexto correto |

Oracle de linguagem/manual para witnesses reais; não transformar output de hoje em golden verdadeiro por conveniência. Um caso pode passar A e falhar B. Publicar ambos. Não premiar aumento bruto de candidates.

## Ordem, determinismo e custo

Cada wave arquitetural: witnesses mínimos + reais → metamórficos → sintéticos qualificados → **CardDemo 73 FULL** → redistribuição de first-loss → revisão. Gates de repo/campanha obrigatórios continuam vigentes. Reusar hashes/evidência somente quando as fronteiras não foram invalidadas; o full CardDemo é obrigatório após implementação arquitetural porque a proposta o estabelece expressamente.

Repetições de determinismo por amostra adversarial: inventário físico permutado, callers iguais/diferentes, THRU versus single, ordinary entry, terminal CALL/SET/EXIT/FILE, dead handler e live DAG. Preservar todos os logs/raws, inclusive timeout/OOM. Budgets por estágio são parte da configuração; mudança de budget exige justificativa de desempenho e não resolve falha semântica. Casos grandes COACTUPC/CBACT04C e DAG vivo devem ter métricas nodes/edges/contextos antes/depois, não só tempo final.

Falha nova de integridade/negative oracle impede fechamento. Redução de blockers sem melhoria de B é viabilidade parcial, não prova de qualidade. Um plateau deve disparar discovery, não alteração de oracles para voltar ao verde.
