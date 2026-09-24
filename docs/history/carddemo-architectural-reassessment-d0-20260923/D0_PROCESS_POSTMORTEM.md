# D0 — postmortem crítico do processo

A metodologia foi insuficiente para qualificar viabilidade de produto. Muitos gates fortes e úteis foram aplicados a um espaço de exemplos escolhido pela própria decomposição da implementação. Fechar responsabilidade de uma wave passou a funcionar como substituto prático de medir se programas reais ainda atravessavam a pipeline. Os documentos antigos frequentemente limitam corretamente suas alegações; o erro foi permitir progressão arquitetural sem um gate adicional de viabilidade real.

## Evidência de evolução, não memória do processo

Amostra estratificada de dez fontes reais, mesmos bytes e resolução do runner, seis runtimes congelados. W8 reutiliza a medição canônica. Todos os pins/classes/commands e outputs estão em probes/history; a JVM/heap/timeouts dos probes constam nas medições. Nenhum build histórico foi refeito.

| Programa | Pré-positive | W5 | W6 | W6-R1 | W7 | W7-R1 | W8 atual |
| --- | --- | --- | --- | --- | --- | --- | --- |
| CBACT01C | dependency PARTIAL | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | F2 |
| COACTUPC | frontend owner failure | frontend owner failure | frontend owner failure | frontend owner failure | frontend owner failure | frontend owner failure | F2 |
| CBEXPORT | dependency PARTIAL | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | F2 |
| COPAUS1C | dependency PARTIAL | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | F2 |
| COACCT01 | dependency PARTIAL | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | F2 |
| CBPAUP0C | dependency PARTIAL | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | F3 |
| COADM01C | dependency PARTIAL | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | F5 |
| COPAUS2C | dependency PARTIAL | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | F6 |
| CBACT04C | dependency PARTIAL | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | lower BLOCKED | F4 |
| COBSWAIT | dependency PARTIAL | dependency PARTIAL | dependency PARTIAL | dependency PARTIAL | dependency PARTIAL | dependency PARTIAL | dependency PARTIAL |

**9/10** chegavam a dependency PARTIAL antes da campanha; **1/10** desde W5 até W7-R1 e no atual. Oito regressões de viabilidade na amostra já estariam visíveis no máximo em W5; o nono blocker já existia antes. A causa exata de introdução entre o baseline pré-positive e W5 não foi isolada por bisect, portanto não atribuir tudo ao commit W5.

COACTUPC histórico falha em `SOURCE_DEPENDENCY_OWNER_UNPROVED` durante semantic product, embora o wrapper classifique o motivo como PREPROCESSING_FAILED. O stderr preservado demonstra a fase real. W8 corrige/ultrapassa essa barreira e expõe F2. Isso é melhora de uma fronteira, não regressão F2 exclusiva de W8.

CBACT01C pré-positive tinha 103 OBSERVED e **zero PERFORM_PROCEDURE**; W5 tem 69 OBSERVED e 34 PERFORM_PROCEDURE. O antigo produto chegava ao fim sem o controle positivo novo. A publicação mais expressiva expôs consumer invariants contraditórios e storage gates; voltar a OBSERVED para restaurar 9/10 esconderia os fatos. F5 já existia como gap em COADM01C pré-positive, mas não era blocker de pipeline. [Comparação de qualidade](probes/history-quality.json).

Não há evidência para afirmar que os 69 atuais bloqueariam exatamente nas mesmas famílias em W5. A amostra prova a falha sistêmica que um gate real teria sinalizado; código comum e witnesses explicam sua extensão atual. Não extrapolar contagens históricas não executadas.

## Por que as waves foram aprovadas

| Viés | Evidência / mecanismo | Intervenção concreta |
| --- | --- | --- |
| Corpus | W6 usou 39 históricos + 3 derivados, 45 sites; gates por responsabilidade | full CardDemo 73 em toda wave arquitetural, depois do corpus qualificado |
| Witness | frontiers sintéticas centradas em MOVE/CALL receberam caminhos especiais; EXIT/SET/DISPLAY real combinados com fallthrough escaparam | fonte real primeiro; derivar mínimo só depois; contraste por outcome equivalente em verbos diferentes |
| First-loss | W5 documentou dependências de W6/W7 e não exigia dependency PASS; a progressão não mediu CardDemo amplo | publicar distribuição por estágio e reexecutar full após remover mecanismo; acompanhar próximos blockers |
| Gate | FAST/full/CI verificam invariantes e oracles locais, não denominador do produto | dois gates separados: viabilidade e qualidade; green CI nunca substitui a matriz real |
| Contract | produtor preservou frontiers; consumer manteve regra incompatible para OBSERVED | testes de fronteira produzidos pelo frontend real, incluindo final statement + próximo parágrafo |
| Realism | COACTUPC combina 61 ranges, copies, IF/EVAL, calls, CICS; FILE cria edges por canal auxiliar | corpus combinado com nested/range/repetition/handlers e entradas ordinárias |
| Approval | escopo local CLOSED foi suficiente para avançar apesar da viabilidade não medida | fechamento requer impacto real medido e regressões explicitamente aceitas, além do contrato local |
| Adversarial | havia negativos sérios de cross-return/dead code e metamórficos, mas gerados em domínio estreito | auditoria independente language→edges; trocar construct terminal, misturar FILE handlers, shared DAG vivo |

Não há registro que permita inferir motivos pessoais para CardDemo não ser gate desde W5. Há evidência objetiva de que os critérios escolhidos não o exigiam. W5_EVIDENCE declara que dependency PASS não era requisito de fechamento. W6_E2E declara sucesso restrito ao corpus congelado; W7 ampliou casos de controle e performance, mas continuou sem cobertura real ampla de referência auxiliar. A decisão de governança que faltou foi uma qualificação transversal obrigatória, não simplesmente mais testes unitários.

## Performance também precisa de realismo

W6-R1 corrigiu OOM de cold DAG e melhorou cadeia profunda, mas o live DAG12 ainda gera 8.191 contextos/20.507 blocos. W7 distinguiu latência ANTLR (exemplo ~62 s frontend) de lower (~0,915 s) e preservou timeout anterior: distinção correta, insuficiente para provar escala do CardDemo. F7 agora mostra 23.873 sequences num programa real antes de validação. Medir apenas tamanho de corpo ou cold/dead code premia a otimização que não cobre todos os caminhos vivos.

## Novo protocolo de fechamento

1. Discovery real corpus primeiro, com pins, configuração e first-loss.
2. Witness real representativo e CFG/outcomes derivados da linguagem.
3. Mínimo derivado que preserve o mecanismo, mantendo o real como regressão permanente.
4. Implementação com autoridade/prova explícita.
5. Metamórficos de contexto, verbos terminais, gaps e reference closure.
6. Corpus sintético qualificado e negativos históricos inalterados.
7. CardDemo 73 FULL, sem whitelist, com budgets estáveis.
8. Redistribuição de first-loss e Gate B por sites, sem celebrar só arquivos existentes.
9. Somente então fechar wave; toda limitação/queda real deve estar visível na revisão.

CardDemo é corpus de viabilidade, não oracle absoluto de semântica. Mais candidates ou comportamento antigo não equivalem a correção. O pós-D0 precisa também de futuro corpus corporativo autorizado para testar generalização; isso não foi executado aqui.
