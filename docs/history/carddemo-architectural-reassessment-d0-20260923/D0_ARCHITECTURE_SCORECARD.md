# D0 — scorecard por critério concreto

SUPPORTED significa evidência positiva **no escopo escrito na célula**, que pode ser expressividade do contrato, não implementação end-to-end. PARTIALLY_SUPPORTED indica suporte condicionado/incompleto. UNSUPPORTED significa capacidade ausente demonstrada; UNKNOWN significa evidência insuficiente. Não há soma ou nota numérica. C é escolha de autoridade; A/B são também estratégias de execução, portanto não são alternativas perfeitamente ortogonais.

**Capacidade operacional B hoje: UNSUPPORTED no CFG** (`docs/domain/local-control.md`). A tabela distingue isso da expressividade normativa.

| Critério | A corrigida | B control.local | C frontend topology |
| --- | --- | --- | --- |
| F2 coverage | PARTIALLY_SUPPORTED — regra comum desenhada; sem fix executado | PARTIALLY_SUPPORTED — ports expressam retorno; ainda precisa corrigir frontend | PARTIALLY_SUPPORTED — quatro witnesses e 46 inventariados; futura execução |
| F3 coverage | PARTIALLY_SUPPORTED — conclusão de região corrige contrato em desenho | PARTIALLY_SUPPORTED — stack não recupera arm topology sozinho | PARTIALLY_SUPPORTED — Complete(arm) compõe com region boundary |
| Paragraph ordinary execution | SUPPORTED — mapa AST e ordinary relation já existem, publicação desigual | SUPPORTED — boundary default com stack vazia, contrato normativo | SUPPORTED — default fonte explícito, demonstrado no AST |
| PERFORM contextual return | PARTIALLY_SUPPORTED — especializado funciona nos gates qualificados, falha CardDemo | SUPPORTED — invoke/boundary expressam matching no contrato; consumer falta | PARTIALLY_SUPPORTED — binding explícito, backend precisa qualificação |
| Multiple callers | SUPPORTED — W6 context separation em corpus qualificado | SUPPORTED — body compartilhado, frame contém resume distinto | PARTIALLY_SUPPORTED — topologia compartilhada; matching depende do backend |
| Nested PERFORM | PARTIALLY_SUPPORTED — nested gates passam; real/fanout escala aberta | PARTIALLY_SUPPORTED — top-only serve nesting bem formado; restrições COBOL permanecem | PARTIALLY_SUPPORTED — região tipada; caso CBEXPORT desenhado |
| THRU | PARTIALLY_SUPPORTED — ranges qualificados; F2 mostra frontier inconsistente | SUPPORTED — porta final diferente de intermediárias no contrato | PARTIALLY_SUPPORTED — ordem/portas source explícitas; não implementado |
| Repetition | PARTIALLY_SUPPORTED — wrappers W6; resume inline real pode faltar | PARTIALLY_SUPPORTED — wrapper por iteração expressável; solver sem medição | PARTIALLY_SUPPORTED — loop topology separada de count/value proof |
| GO TO escape | PARTIALLY_SUPPORTED — closure existe; context semantics precisa prova | PARTIALLY_SUPPORTED — jump conserva stack; unwind requer prova adicional | PARTIALLY_SUPPORTED — transfer tipado não infere completion; dialeto/active range |
| IF/EVALUATE composition | PARTIALLY_SUPPORTED — whitelists/admission contradizem F3 | PARTIALLY_SUPPORTED — ainda exige regions/outcomes frontend | PARTIALLY_SUPPORTED — regra comum elimina reconciliação por successor pai |
| Localized gaps | PARTIALLY_SUPPORTED — boas intenções, unit proof limita F5/F6 | UNKNOWN — local stack não define blast radius de fatos | PARTIALLY_SUPPORTED — depends_on/bounds desenhados, storage precisa trabalho |
| Dead code | SUPPORTED — negativos sintéticos existentes; sem universalidade | UNKNOWN — matching/queries CFG ainda indisponíveis | PARTIALLY_SUPPORTED — outcomes explícitos favorecem prova; backend/gaps importam |
| Determinism | SUPPORTED — hashes de repetição W6/W7 em corpus qualificado | UNKNOWN — contrato de IDs possível, implementação não medida | PARTIALLY_SUPPORTED — IDs/provenance estáveis desenhados; exigir permutation |
| Performance | PARTIALLY_SUPPORTED — cold pruning/linear chain bons; live DAG expande | UNKNOWN — sem solver/benchmark real | UNKNOWN — frontend finito; backend A herda expansão |
| IR size | PARTIALLY_SUPPORTED — 20.507 blocos/30 source no live DAG12 | PARTIALLY_SUPPORTED — compartilhamento reduz grafo estático; custo solver deslocado | UNKNOWN — fonte O(S+E), AIR depende do backend escolhido |
| Implementation complexity | PARTIALLY_SUPPORTED — cinco mudanças comuns, dívida de whitelists | UNKNOWN — mudança de solver/matching pode dominar economia lower | PARTIALLY_SUPPORTED — módulos e responsabilidades definidos; sem esforço estimado fictício |
| Number of repos touched | SUPPORTED — mínimo frontend/lower; sem nova AIR no desenho | PARTIALLY_SUPPORTED — frontend/lower/CFG e auditoria air-java/IR | SUPPORTED — mínimo frontend/lower + contrato SP/evidência; backend inicial existente |
| Migration risk | PARTIALLY_SUPPORTED — menor mudança wire se versionada, alto risco semântico | UNKNOWN — nova capability consumer e matching não qualificados | PARTIALLY_SUPPORTED — schema coordenado e dual-read; mais trabalho upfront |
| Legacy compatibility | PARTIALLY_SUPPORTED — preservar decode/golden antigos é factível | PARTIALLY_SUPPORTED — capability opt-in; consumers antigos não suportam | PARTIALLY_SUPPORTED — versão explícita + fixtures antigas sem reinterpretar |
| Testability | SUPPORTED — harness/context negativos já existem | PARTIALLY_SUPPORTED — oracles locais previstos, sem consumer implementado | PARTIALLY_SUPPORTED — edge inventory e proof dependencies testáveis por fronteira |
| Future COBOL constructs | PARTIALLY_SUPPORTED — só generaliza se eliminar exceções por verbo | PARTIALLY_SUPPORTED — stack resolve calls; não conhece todos os constructs | PARTIALLY_SUPPORTED — extensões em outcomes/regions na autoridade única |
| Corporate generalization | UNKNOWN — sintéticos/CardDemo não certificam código bancário | UNKNOWN — não avaliado fora de desenho contratual | UNKNOWN — melhor separação é argumento causal, não benchmark corporativo |

Fontes: D0_F2_DEEP_DIVE (46/14 witnesses), D0_F3_COMPARISON, D0_F7_IR_INTEGRITY, D0_COMPLETENESS_BLAST_RADIUS; W6_R1_PERFORMANCE, W6_E2E, W7_PERFORMANCE e contrato AIR §05.7. Hashes históricos verificados em probes/historical-evidence-hashes.json. Nenhuma célula é um PASS de implementação D0.

A não satisfaz sustentabilidade geral demonstrada. B não satisfaz custo/precisão total de CFG/dataflow demonstrados. C satisfaz os critérios de reorganização de autoridade por evidência causal e desenho concreto; sua operação/escala exigem gates futuros. Não se criou uma arquitetura D artificial.
