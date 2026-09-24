# D0 — baseline e preservação

Estado: W5 CLOSED; W6/R1 e W7/R1 CLOSED **sob corpus qualificado**; W8 PAUSED; W9 NOT STARTED. D0 é investigação, sem implementação, commit, mudança de branch ou operação de PR.

## APPROVED_W7_R1_BASELINE versus CURRENT_W8_WORKTREE_STATE

| Repositório | W7-R1 aprovado | HEAD W8 observado |
| --- | --- | --- |
| proleap-poc | f62a4140f34f074db62143f24031ec4988456063 | 5d8cad6b8d1e4be12720e852ce1d56eb47e969bf |
| cobol-lower | 8fafe2ef3b9665d6e5d834be34ff48f2f0a84aae | cc5226e744da0048d0c3064b402bae1123a0be45 |
| air-java | 980d4989a18f876996390cc61af41419a595eb7b | 980d4989a18f876996390cc61af41419a595eb7b |
| analysis-ir | b628e4c1a62de61a157cac85ae71ba4cd111052e | b628e4c1a62de61a157cac85ae71ba4cd111052e |
| analysis-cfg | 7a4a2104d3c284e8efc1700413839f33551b474f | bae087c89a3a851d91a70a053a37fb3bb25af097 |

Os cinco worktrees estavam limpos, na branch `feat/positive-memory-topology`. A captura anterior aos probes, status e diffs integrais estão em [baseline/initial.json](probes/baseline/initial.json) e arquivos adjacentes. A verificação posterior está em `probes/baseline/final-preservation.json`. A raiz agregadora não foi usada como repositório Git.

O resultado canônico **4/73** usa CURRENT_W8_WORKTREE_STATE, não o pin aprovado. W8 contém mudanças parciais de dependências nominais no frontend, transfers/effects no lower e extração no CFG. O diff capturado permite revisar exatamente esse delta. Nenhuma dessas mudanças foi apagada.

## Corpus e evidência reutilizada

CardDemo `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`: 44 fontes checkout + 29 membros ZIP, incluindo `.cl2`, total físico **73**. Sem whitelist, deduplicação ou exclusões. Os hashes dos 73 fontes foram conferidos individualmente contra as medições. Antes da análise, os **4.787** arquivos do manifesto canônico foram verificados, sem divergência: [verificação](probes/baseline/evidence-hash-validation.json).

Foram lidos o report, classification CSV, summary, commands, as medições integrais e os produtos SP/AST dos 71 fontes que os publicam. Também foram lidos os três conjuntos de probes de profile e seus resultados. [Projeção verificável](probes/all-programs-analysis.json), [probes de profile](probes/profile-probe-analysis.json).

O full canônico durou 240,895 s, sem timeout/falha de runner. Comandos usam o executável absoluto Java 21; a string de Java 25 do ambiente default não identifica a JVM realmente invocada. Não houve ajuste de heap/timeout como solução semântica em D0.

## Experimentos novos

1. Inventário externo dos 46 F2: 1.512 sites PERFORM, 1.964 pares site/parágrafo. Replica dois predicados de rejeição para localizar diagnósticos, **não constitui oracle semântico**.
2. Probe AST canônico sobre 14 programas F2, CBPAUP0C e COPAUS2C: preserva fontes e mede separadamente relações local/ordinária.
3. Probe F7 usa decoder, admission, montagem e validator reais, sem relaxar regras. Aumenta apenas a retenção de diagnósticos para 30.000, como o probe canônico, permitindo ver os 30 I-02. Fragmento inválido não é publicado como AIR aceita.
4. Amostra histórica estratificada de dez checkout sources × seis runtimes, 60 execuções. As medições canônicas W8 completam a comparação. [Builds e pins](probes/history/builds.json), [medições](probes/history/measurements.json). Sem checkout, rebuild ou alteração dos produtos.

A amostra histórica não certifica todos os 73 em cada wave. Os runtimes são snapshots congelados; hashes atuais e manifests históricos são registrados. Quaisquer discrepâncias de evidência histórica são explícitas em [verificação](probes/historical-evidence-hashes.json). A execução grava apenas o diretório D0. Não foram repetidos FAST/full de produto: não há alteração de produto a qualificar; os experimentos respondem a perguntas causais distintas.
