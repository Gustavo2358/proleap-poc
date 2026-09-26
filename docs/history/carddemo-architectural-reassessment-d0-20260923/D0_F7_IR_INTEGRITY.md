# D0 — integridade AIR F7

**TYPE E: defeito de integridade, não partial semantics.** Profile explícito permite chegar ao assembler, que constrói um fragmento com 30 labels pendentes em cada variante CBACT04C. O validator real retorna INVALID_IR; a publicação é corretamente recusada. Não foi desligado validator nem serializada AIR inválida como saída aceita.

O [probe externo](tools/F7Probe.java) invoca decoder/admission/assembler reais, observa o registro de LocalIds por reflexão **somente leitura** e executa AirValidator. Reproduz exatamente 30 I-02, traversal completo e 19.283 obrigações semânticas no checkout. IDs e 30 contagens coincidem com o probe canônico. ZIP reproduz 30 I-02 separadamente. Obrigações semânticas não são confundidas com labels ausentes.

## Mecanismo demonstrado

Três READs têm handlers INVALID KEY. `FileControlLowering:73–76` referencia o primeiro statement do handler com `PartialProgramAssembler.label(handler.first, unit, localContext)`. `CompositionalPerformAdmission:20–38` expande closure por normal/ordinary, GO TO e braços IF/EVALUATE, mas **não visita as referências a handlers em fileInventory** quando encontra esses READs pela closure.

Em cada contexto ofensivo, o READ está no plano, o statement de handler não está. O `append` materializa todos os membros do body recebido, portanto não poderia criar a sequência que não entrou no plano. Os 11 callsites ofensivos têm `procedures=[]` e targetEntry positivo: a closure começa por entry sem membership/frontiers publicados e percorre successors ordinários. Isso explica também bodies amplos. O inventário ordinário contém o handler, com **outro contexto/LabelId**; não resolve a referência da ativação.

Não é pruning de `PerformActivationDemand`: com operações FILE, `inContext` retorna todo o inventário (scheduling eager). W6-R1 demanda não corta esses handlers. A referência foi criada por um subsistema FILE que não participa da closure genérica usada por W7/ativação. A causa é **inventário de edges dividido entre statement facts e file auxiliary facts + closure de ativação incompleta**. F7 é RELATED a F2/F3 pela autoridade fragmentada, mas não é o mesmo predicado de normalContinuation.

| READ source | Handler esperado | Linhas checkout | Ocorrências ausentes por variante |
| --- | --- | --- | --- |
| statement:215 | statement:216 DISPLAY | 373 → 375 | 9 |
| statement:222 | statement:223 DISPLAY | 394 → 397 | 10 |
| statement:229 | statement:230 DISPLAY | 416 → 418 | 11 |


Os 30 são três classes de target em 11 contextos de PERFORM. Os 11 são entry-only (`procedures=[]`), verificados em f7-input-facts.json; falta de frontier torna o fecho maior e expõe handlers fora do inventário inicial. A [matriz completa de 60 linhas](probes/f7-labels.csv) contém para **cada** erro: operation, source statement/linha, target label/statement/linha, contexto, expected materializer e razão de ausência. [Raw checkout](probes/f7-03.json), [raw ZIP](probes/f7-05.json). Os números por grupo acima são confirmados abaixo pela matriz, e devem ser lidos sem somar F7 à partição F1–F6.

## Implicação de escala e obrigação futura

O fragmento checkout já tem **23,873 sequences** para 294 statements source no inventário ordinário. Ele é inválido, não um benchmark de análise aceita; ainda assim expõe o custo de clonagem/closures largas em um caso real. Aumentar limites não conserta referência faltante.

A obrigação arquitetural é `every emitted reference → inventoried target in the same context`, derivada do **mesmo grafo tipado** usado pelo materializer. Deve valer também para FILE events, USE, SORT callbacks, exception arms e local control. Adicionar apenas esses três handlers por nome CardDemo não generaliza. A futura correção precisa oracle de handler alcançável, handler proibido no evento errado e matching de contexto, além de zero I-02.

D0 não executou um fragmento “reparado”. Portanto não há promessa de que retirar F7 leve imediatamente a dependency: outros limites/obrigações podem aparecer depois.
