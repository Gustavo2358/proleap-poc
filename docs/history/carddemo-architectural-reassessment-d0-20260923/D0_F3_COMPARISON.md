# D0 — F3 versus F2

**SAME_MECHANISM** na perda de distinção entre conclusão local de região e sucessor ordinário. A regra de admissão que manifesta o erro é diferente.

Fonte real: `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl`. EVALUATE `statement:58`, linhas 228–241, ao fim de 2000-FIND-NEXT-AUTH-SUMMARY; EVALUATE `statement:69`, linhas 259–271, ao fim de 3000-FIND-NEXT-AUTH-DTL.

| Pai | Filho terminal | Semântica esperada | SP pai | SP filho |
| --- | --- | --- | --- | --- |
| 58 | 62: SET END-OF-AUTHDB TO TRUE, l.235 | fim do WHEN GB → fim EVALUATE → boundary do parágrafo | normal UNAVAILABLE; ordinaryContinuations 58→66 | normal KNOWN→66 |
| 69 | 71: ADD, l.262 | fim WHEN sucesso → boundary | normal UNAVAILABLE; ordinary 69→77 | normal KNOWN→77 |
| 69 | 72: SET, l.264–265 | WHEN GE/GB compartilhado → boundary | idem | normal KNOWN→77 |

66 e 77 são EXITs dos parágrafos seguintes (linhas 244 e 274). **Não são siblings do EVALUATE.** A mensagem `normal arm completion cannot enter a sibling arm` é enganosa para esses destinos.

`EvaluateAdmission` admite saída de um braço quando interna ao braço ou igual ao successor local do pai. Não considera a boundary comum cujo default ordinário aparece em `ordinaryContinuations`. O projector põe o successor ordinário nos filhos OBSERVED, enquanto o pai mantém a ausência de sucessor local. É o mesmo choque de significados visto em F2.

O probe [AST CBPAUP0C](probes/ast-control/CBPAUP0C.json) confirma os mapas separados; o SP e stderr originais ficam no diretório canônico por programa. Valores DIBSTAT desconhecidos e efeitos DLI não explicam essa contradição. Não é necessário executar IMS para reconhecer END-EVALUATE, entry dos arms e seus términos.

Esqueleto esperado: `EVAL58 → arm(GB) → SET62 → Complete(EVAL58) → Complete(P2000)`; no contexto ordinário a boundary leva a EXIT66; no PERFORM que termina em P2000 devolve ao caller; no THRU até P2000-EXIT continua a EXIT66 e conclui ali. EVAL69 é análogo. Arms com GO TO/retorno explícito não recebem conclusão normal inventada.

A regra comum proposta para F2/F3 deve compor `Complete(region)` de dentro para fora. Acrescentar “permite successor fora” ao validator ou classificar os filhos como sibling válido esconderia o erro, não modelaria a linguagem.
