# Arquitetura A — positive facts corrigida

**PARTIALLY_SUPPORTED como destino arquitetural; não recomendada isoladamente.** Preserva compatibilidade e a especialização hoje disponível, mas a sustentabilidade da expansão não foi demonstrada para o corpus real.

Os quatro witnesses comparados em A/B/C são os mesmos: (W1) CBACT01C PERFORM89/EXIT158; (W2) COACTUPC PERFORM858 THRU/SET1284/EXIT1312; (W3) CBEXPORT PERFORM94→1000 com PERFORM103 aninhado→1050/STRING111; (W4) CBPAUP0C EVALUATE58/SET62→boundary2000. São modelos hipotéticos, sem execução/fix de produto. Edges de linguagem estão em D0_F2_DEEP_DIVE e D0_F3_COMPARISON.

## Uma regra comum possível

Separar em todos os statements: sucessor dentro da região, conclusão normal da região, transferência explícita e default ordinário. A frontier não é uma alegação de que o statement termina; só define o resultado da conclusão normal. CALL/MOVE/OBSERVED/FILE deixam de precisar de exceções diferentes. O consumer não pode interpretar ordinary como explicit transfer.

Cinco mudanças conceituais necessárias: (1) semântica uniforme de outcomes; (2) composição de regiões incluindo arms e handlers; (3) prova de controle independente de valores/input global; (4) fecho de referências único para inventory e emissão; (5) contrato de especialização/matching com limite de escala explícito. Isso é mais que trocar um `if` em admission. Se A mantiver as whitelists atuais, não satisfaz o critério de “uma abstração comum”.

| Witness | Representação A corrigida | Diferença para hoje |
| --- | --- | --- |
| W1 | EXIT158 tem completion(P0000), ordinary→44; clone do caller89 resolve completion→90; ocorrência ordinária→44 | OBSERVED não publica ordinary como intrinsic local |
| W2 | SET1284 completa EVAL e PYYYY; no range858 resolve→1312; EXIT1312 completa último parágrafo→859; entrada ordinária continua fora do range | não exigir inputComplete para grouping/terminação exatos; só último endpoint retorna |
| W3 | clone de1000/caller94 contém invoke aninhado103; clone1050/103 completa STRING111→104 no contexto de94; DISPLAY106 completa1000→95 | dois contextos de completion, sem bypass; ordinary STRING111→112 permanece distinto |
| W4 | SET62 completa arm/EVAL58; frontier da região2000 chega à boundary; default66 ou retorno da ativação que termina ali | admission usa a conclusão de região; não exige igualdade com um único normal do pai |

Múltiplos callers ganham IDs/contextos distintos; THRU dá override apenas nas boundaries correspondentes; repetition devolve ao nó de teste/incremento correto. GO TO escape requer fecho de referências no mesmo contexto e regra explícita sobre contexto ativo; não se deve transformar salto em retorno local. Dead code não é materializado por demanda só quando a ausência de demanda está provada; FILE hoje força eager.

## Escala, compatibilidade e riscos

A expansão do grafo é aproximadamente soma do body materializado em cada contexto alcançável, não apenas número de callsites. Em DAG compartilhado pode crescer exponencialmente no número de níveis. Evidência W6-R1: live DAG depth12 tem **30 source statements, 20.507 blocos AIR, 24.605 operações e 8.191 contextos**; lower 3,620 s / 748.808 KiB RSS; analysis 4,121 s / 999.592 KiB RSS. Cold DAG é barato depois de R1, mas isso não elimina o caso live. Chain depth256: 262 source / 776 blocos / 257 contextos, indicando que o problema é compartilhamento de caminhos, não toda profundidade.

O CBACT04C real perfilado já monta 23.873 sequences antes de falhar em integridade. COACTUPC tem 1.415 statements SP, 61 PERFORM sites e grande reutilização de ranges; o lower bloqueia na admissão, portanto **não há medição válida de expansão final para ele**. Não extrapolar uma razão linear dos sintéticos.

A pode ser um backend transitório limitado, com budget, resultado de limite honesto e benchmark real obrigatório. Não há base para certificar hoje que seja operacionalmente aceitável como solução geral. Preservar legacy exige decode versionado; não reinterpretar JSON antigo como contrato novo. Repositórios mínimos: frontend + lower; AIR/CFG podem manter o modelo existente, desde que os grafos traduzidos passem integridade/quality.

**Critério de recomendação A não satisfeito integralmente:** abstração comum é desenhável, mas whitelists/autoridade duplicada precisam ser removidas e performance de COACTUPC/FILE permanece sem prova. A não deve justificar mais campos especiais por construct.
