# CARDEMO_ARCHITECTURAL_REASSESSMENT_D0_READY_FOR_REVIEW

**Recomendação: C — topologia de controle produzida pelo frontend, com outcomes/regions tipados no SP e tradução genérica no lower.** Preservar positive memory; localizar dependências de prova; instituir CardDemo completo como gate de viabilidade separado da qualidade semântica. Sem implementação.

Baseline canônico CURRENT_W8_WORKTREE_STATE: **4/73 chegaram a dependency, todos PARTIAL; 69/73 não chegaram**. SP: 71 PARTIAL + 2 BLOCKED. Pins W7-R1 aprovados e estado W8 estão separados em D0_BASELINE. Corpus `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`, 44 fontes checkout + 29 ZIP incluindo .cl2. Nenhuma exclusão.

## Achados decisivos

- **F2, 46 programas:** o AST conhece local continuation e ordinary fallthrough, mas o projector põe ordinary em normalContinuation de OBSERVED e o lower o interpreta como controle explícito incompatível com completion. Frontier às vezes é omitida por prova global de input/EVALUATE ou por composição incompleta de handler FILE/IF implícito. Quatro clusters sobrepostos: EXIT raiz 28; outros sequenciais raiz 8; frontier composta 17; frontier não publicada 32. Análise dos 46, 1.512 sites, 1.964 pares site/parágrafo; 14 witnesses reais/24 fronteiras aprofundadas.
- **F3: SAME_MECHANISM.** Em CBPAUP0C, targets externos são EXITs de parágrafos seguintes, não sibling arms. O pai e seus filhos usam meanings diferentes de normalContinuation.
- **F4: MIXED.** Profile explícito leva cinco casos adicionais a dependency PARTIAL e revela F7 em dois. JCL indica IBM V6.3; 6.4/1047 para todo checkout+UniKix não foi autenticado. A ausência de profile não justifica descartar conhecimento lógico independente.
- **F5:** inputComplete(unit) amplia o impacto de DFHAID/DFHBMSCA ausentes sobre storage/controle local. Não inventar conteúdo dos COPYs; preservar fatos cuja independência for provada.
- **F6:** SQL INCLUDE vira DataEntry level SQL; nível −1 reprova roots da seção e causa SECTION_NOT_PROVEN global. SQLCA/AUTHFRDS são relevantes pela expansão/tipagem ausente, não pelo status nominal em si.
- **F1:** duas manifestações da falta de policy de colunas HT: rejeição em conteúdo e contagem errada no prefixo de COPY. Normalização por arquivo antes de COPY, com stops declarados, é o mecanismo a definir.
- **F7:** 30 I-02 por variante CBACT04C, todos handlers INVALID KEY de três READs omitidos da closure de ativação. Não é pruning por demanda; FILE usa scheduling eager. 60 referências mapeadas individualmente.

A amostra histórica de dez programas mostra 9/10 chegando a dependency PARTIAL antes da campanha e 1/10 desde W5; oito regressões de viabilidade que o gate real teria detectado. Os produtos antigos tinham PERFORMs OBSERVED: não são oracle de correção. Não extrapolar esses números aos 73 históricos não executados.

## Decisão e limites

A corrigida é expressável, mas conserva risco de duplicação e não demonstrou escala: live DAG12 gera 8.191 contextos/20.507 blocos para 30 statements; CBACT04C real já monta 23.873 sequences inválidas. B é promissora no contrato control.local, mas **UNSUPPORTED no CFG atual**; custo/precisão do solver são UNKNOWN. C elimina duplicação demonstrada e pode migrar sem nova AIR inicialmente, mas herda o custo do backend escolhido e precisa gates de escala.

**Não modelar tudo está errado? PARTIALLY.** Values/effects podem continuar parciais; controle precisa cobertura muito alta e todos os edges publicados precisam ser íntegros. Storage pode ser parcial por região, com alias/identity e provas explícitas. Runtime exato não é necessário. Construir topology fonte antes de precisão de valores: **YES**, sem emulador COBOL.

Próxima wave proposta: CONTROL_TOPOLOGY_AUTHORITY, para remover o primeiro mecanismo F2/F3 e reference-closure F7 quando exposto. Não prometer `4 + 47 = 51 PASS`; novos blockers podem aparecer. Seguem fact-dependency locality e configuração/normalização explícitas, com full73 e quality gates a cada wave.

## Estado de entrega

Product repos modified: **NO**. PRs modified: **NO**. Corpus, fixtures e oracles preservados. W8 **PAUSED**; W9 **NOT STARTED**. **STOPPED BEFORE CODE: YES**.

Começar por [recomendação](D0_RECOMMENDATION.md), [F2 deep dive](D0_F2_DEEP_DIVE.md), [scorecard](D0_ARCHITECTURE_SCORECARD.md), [postmortem](D0_PROCESS_POSTMORTEM.md) e [índice de evidência](D0_EVIDENCE_INDEX.md). Limitações e decisões hipotéticas permanecem explícitas; READY_FOR_REVIEW não significa aprovação da futura implementação.
