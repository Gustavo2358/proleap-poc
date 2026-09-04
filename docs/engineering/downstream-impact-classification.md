# Classificação de impacto semântico downstream

## Finalidade e escopo

Esta é a fonte canônica para classificar o impacto downstream de bugs, gaps e
findings semânticos. Ela responde a uma pergunta específica:

> Qual é a primeira fronteira arquitetural downstream que deixa de cumprir seu
> contrato semântico por causa deste finding?

A classificação é uma dimensão do finding. Ela não é severity, prioridade,
status, tipo do finding ou autorização para implementar uma correção. Também
não cria Semantic Product, Cobol Lower, IR, CFG ou dataflow.

O vocabulário vale para a cadeia conceitual:

```text
frontend COBOL / semantic analysis
  → Semantic Product
  → Cobol Lower
  → IR
  → CFG
  → Dataflow / Possible Values
  → Dependency Facts / semantic extractors
```

Quando uma fronteira ainda não tem contrato definido, não se deve inferir seu
impacto a partir do nome do construct, da área do código ou de uma consequência
que apenas parece provável.

## Contrato normativo

A classificação segue estas regras:

1. **Classe primária única:** cada finding possui no máximo uma classe primária
   de impacto downstream.
2. **Earliest broken layer wins:** quando várias camadas ficam incorretas, a
   classe é a primeira fronteira que não cumpre seu contrato. As demais são
   consequências e permanecem descritas no rationale/evidence, sem virar uma
   segunda classificação.
3. **Evidência antes de inferência:** a classificação deve apontar a informação
   errada ou ausente, o produto em que isso foi observado, as camadas anteriores
   que continuam corretas e o contrato da primeira camada quebrada. Também deve
   explicar no rationale por que as classes anteriores foram rejeitadas.
4. **Incerteza explícita:** `UNASSESSED` é obrigatório quando a primeira
   fronteira ainda não pode ser determinada. Falta de evidência nunca autoriza
   inferir outra classe.
5. **Soundness antes de precisão:** `REDUCES_PRECISION` só pode ser usado quando
   há evidência de que o resultado continua conservador/sound e apenas contém
   menos informação útil. Uma afirmação incorreta não é um caso de precisão
   reduzida.

Em termos normativos: **“Downstream impact classification is evidence-based.
Lack of evidence results in `UNASSESSED`, never in inferred impact.”**

## Taxonomia canônica

| Classe | Usar quando | Evidência mínima exigida |
| --- | --- | --- |
| `BLOCKS_SEMANTIC_PRODUCT` | O frontend não consegue produzir, preservar ou expor informação exigida pelo contrato do Semantic Product. | O contrato do Semantic Product está definido e a informação já está demonstrada como perdida, errada ou indisponível na fronteira do frontend. |
| `BLOCKS_IR` | O Semantic Product é semanticamente correto, mas Cobol Lower/IR não representa a semântica necessária. | Semantic Product correto, contrato de IR definido e falha observada no lowering ou na representação da IR. |
| `BLOCKS_CFG` | Semantic Product e IR são corretos, mas o CFG não consegue construir as arestas ou pontos exigidos. | Produtos anteriores corretos, contrato de CFG definido e construção incorreta/impossível demonstrada. |
| `BLOCKS_DATAFLOW` | Semantic Product, IR e CFG são corretos, mas a análise de fluxo/efeitos não cumpre seu contrato. | Produtos anteriores corretos, contrato de dataflow definido e falha demonstrada em identity, aliases, effects, program points ou análise equivalente. |
| `BLOCKS_DEPENDENCY_FACTS` | Os produtos anteriores necessários estão corretos, mas a transformação/composição que produz fatos finais de dependência ou fatos semânticos downstream viola seu contrato. | Cada produto anterior realmente exigido pelo extractor está correto; o contrato do fato está definido; e um fato final incorreto, unsound ou impossível de produzir é demonstrado. |
| `REDUCES_PRECISION` | A análise continua sound e conservadora, mas retorna menos informação útil do que o contrato permite. | Um oracle ou argumento independente mostra que nenhum fato falso é afirmado e que a perda é somente de precisão. |
| `UNASSESSED` | Ainda não há evidência suficiente para identificar a primeira fronteira ou distinguir incorreção de perda de precisão. | Rationale explícito sobre a lacuna de evidência e `reassess_when` indicando o que precisa ser definido ou observado. |
| `NOT_APPLICABLE` | O finding não tem impacto semântico downstream dentro do escopo analisado. | Evidência de que o finding é exclusivamente documental, de licença, de organização ou de outra natureza sem consumidor semântico afetado. |

Este é um vocabulário fechado de exatamente oito classes. Nenhum sinônimo ou
classe adicional deve ser inventado em um registro.

## Completude das fronteiras downstream

Antes de considerar a taxonomia fechada, cada fronteira conhecida deve ter uma
classe inequívoca para o caso em que ela seja a primeira a violar seu contrato:

| Fronteira | Classe de bloqueio |
| --- | --- |
| `Semantic Product` | `BLOCKS_SEMANTIC_PRODUCT` |
| `IR` | `BLOCKS_IR` |
| `CFG` | `BLOCKS_CFG` |
| `Dataflow / Possible Values` | `BLOCKS_DATAFLOW` |
| `Dependency Facts` | `BLOCKS_DEPENDENCY_FACTS` |

Se nenhuma fronteira estiver incorreta e somente houver perda conservadora de
informação, a classe é `REDUCES_PRECISION`. Se a fronteira ainda não puder ser
identificada, a classe é `UNASSESSED`.

`BLOCKS_SEMANTIC_PRODUCT` não significa “todo bug do frontend é grave” e não
é o default. Só é válido quando a fronteira e o contrato do produto semântico
forem conhecidos e a falha nessa fronteira estiver demonstrada. Antes disso,
use `UNASSESSED`.

`BLOCKS_DEPENDENCY_FACTS` é uma fronteira posterior a dataflow, não um sinônimo
de `BLOCKS_DATAFLOW`. Um extractor pode depender somente do Semantic Product ou
de um subconjunto dos produtos anteriores; a evidência deve demonstrar apenas
as precondições realmente exigidas por aquele extractor, sem pressupor CFG ou
dataflow quando eles não participam da transformação.

Para essa classe, o registro deve dizer explicitamente que o Semantic Product
está correto quando aplicável, que a IR está correta, e que CFG e
dataflow/possible-values estão corretos quando forem precondições do extractor.
Também deve apontar o contrato do Dependency Fact e o fato final incorreto,
ausente de forma unsound ou impossível de produzir.

## Registro mínimo de um finding

Findings semânticos novos devem registrar a classificação em um bloco YAML
simples, dentro da documentação do finding. O bloco não é um novo schema de
issue tracking; seus campos são uma disciplina documental mínima:

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    Explique a informação errada ou ausente, o produto onde ela foi observada,
    as camadas anteriores ainda corretas, a primeira fronteira que falha ou por
    que ela ainda não pode ser determinada, e por que classes anteriores foram
    rejeitadas.
  evidence:
    - Produto, fixture, teste, contrato ou observação reproduzível.
  reassess_when:
    - semantic-product-contract-defined
```

`class`, `rationale` e `evidence` são obrigatórios para todo registro.
`rationale` deve conter texto não vazio e `evidence` deve possuir pelo menos
uma entrada não vazia e verificável. `reassess_when` é obrigatório somente
para `UNASSESSED`; para as demais classes ele é opcional e só deve aparecer
quando existir um gatilho legítimo, como `contract-version-changed`,
`semantic-product-boundary-redefined` ou `new-downstream-oracle-added`. Se
aparecer, não pode estar vazio. Consequências posteriores podem ser descritas
no rationale/evidence, mas não adicionam classes ao finding.

Não há `confidence` neste contrato. Hoje não existe escala de evidência ou
schema de findings que torne `LOW`, `MEDIUM` ou `HIGH` reproduzível; introduzir
esses valores acrescentaria um número arbitrário e poderia mascarar a regra de
`UNASSESSED`. Uma futura necessidade de confidence deve ser demonstrada por
uso repetido e tratada em work item próprio. `UNASSESSED` significa “a camada
não pode ser determinada”; não significa “a classificação escolhida tem baixa
confiança”.

## Reassessment sem apagar história

Um finding pode começar como `UNASSESSED`. Quando um contrato ou evidência nova
permitir uma classificação, deve-se acrescentar uma nota datada com a classe
anterior, a nova classe, a evidência e o gatilho de reavaliação. O estado atual
do bloco pode refletir a classe mais recente, mas a justificativa anterior não
deve ser apagada.

Reassessment é disciplina documental; não há engine automática de eventos. Um
finding sem fronteira definida deve apontar, por exemplo, para:

```yaml
reassess_when:
  - semantic-product-contract-defined
  - ir-requirements-defined
  - cfg-contract-defined
```

## Aplicação cuidadosa a F-01

`F-01` de `BACKLOG-RES-003` é um bug confirmado na cadeia occurrence →
resolution: em selectors combinados de `EVALUATE TRUE`, uma condition-name pode
chegar como `DATA/{DATA}` e terminar `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`.
Isso confirma a incorreção nominal já registrada, mas não identifica sozinho a
primeira fronteira downstream quebrada.

O pipeline atual ainda não define o contrato do Semantic Product nem os
requisitos da IR. Portanto, o registro de impacto de F-01 deve permanecer:

```yaml
downstream_impact:
  class: UNASSESSED
  rationale: >
    O defeito foi observado nos produtos de occurrences/resolution, mas a
    fronteira do Semantic Product e os requisitos da IR ainda não estão
    definidos. Não há evidência suficiente para dizer se a primeira fronteira
    quebrada é Semantic Product, IR, CFG, dataflow ou somente precisão; essas
    classes são rejeitadas por falta de contrato downstream, não por evidência
    de que estejam corretas. CFG e dataflow também não podem ser classificados
    como consequência presumida.
  evidence:
    - BACKLOG-RES-003 e a caracterização de WORK-COND-007 reproduzem 34 ocorrências com a cadeia occurrence/resolution incorreta.
    - docs/architecture/pipeline.md declara Semantic Product, IR, CFG, dataflow e Dependency Facts como fronteiras futuras ou ainda não materializadas.
  reassess_when:
    - semantic-product-contract-defined
    - ir-requirements-defined
```

Este bloco é um exemplo de impacto downstream e não altera a classificação de
F-01 como `CONFIRMED_KNOWN_BUG`, sua disposição de remediação ou a autorização
de qualquer implementação.

## Casos positivos e adversariais

### Primeira camada, não propagação

Se o frontend perde a identidade de um target `PERFORM`, o Semantic Product não
consegue expor essa informação e a classe é `BLOCKS_SEMANTIC_PRODUCT`. Mesmo que
o lowering, o CFG e o dataflow também fiquem errados, eles são consequências;
não se registram quatro classes.

Se Semantic Product e IR representam corretamente uma transferência de
controle, mas a construção das arestas do CFG está errada, a classe é
`BLOCKS_CFG`. Se a IR já falha, a classe anterior é `BLOCKS_IR`.

### Precisão versus incorreção

Um conjunto de targets `{PGM1, PGM2, UNKNOWN}` reduzido conservadoramente a
`{UNKNOWN}` pode ser `REDUCES_PRECISION` se a análise continua sound. Se a
análise exclui um target possível e afirma que o conjunto é completo, isso é
comportamento unsound: deve-se localizar a primeira camada onde a afirmação
incorreta ocorre e classificá-la como bloqueio dessa camada, não como mera
precisão reduzida.

### Dependency Facts versus precision

Quando o contrato de um fato final está definido, os produtos anteriores
necessários estão corretos e o defeito aparece na transformação/composição do
fato, a classe é `BLOCKS_DEPENDENCY_FACTS`.

- **Caso conservador:** o conjunto esperado `{PGM1, PGM2, UNKNOWN}` é publicado
  como `{UNKNOWN}`. Se o resultado continua sound, trata-se de
  `REDUCES_PRECISION`.
- **Caso incorreto:** o target correto é `PGM-A`, mas o extractor publica
  `PGM-B`, embora as precondições usadas pelo extractor estejam corretas. Isso
  é `BLOCKS_DEPENDENCY_FACTS`, não `BLOCKS_DATAFLOW` nem `REDUCES_PRECISION`.
- **Caso de perda unsound:** o conjunto completo esperado `{PGM1, PGM2}` é
  publicado como `{PGM1}`. Isso não é apenas perda de precisão: localize a
  primeira fronteira que perdeu `PGM2`. Se os produtos anteriores ainda contêm
  `PGM2` e a perda ocorre ao produzir o fato final, use
  `BLOCKS_DEPENDENCY_FACTS`; se uma fronteira anterior já o perdeu, vale a
  classe anterior.

Um extractor literal que consome somente Semantic Product pode, portanto,
ser `BLOCKS_DEPENDENCY_FACTS` sem qualquer afirmação sobre dataflow. A classe
descreve a primeira fronteira demonstradamente incorreta, não todas as fases
que poderiam existir no roadmap.

### Sem evidência

“Este gap provavelmente quebra CFG” não é evidência. Sem contrato CFG, produto
observado e arestas incorretas demonstradas, a classe é `UNASSESSED`.

### Impacto versus prioridade

Um finding `BLOCKS_DATAFLOW` pode ficar fora do roadmap imediato. Um finding
`REDUCES_PRECISION` pode afetar a dependência mais importante do produto e
receber prioridade maior. Nenhuma classe implica P0/P1/P2, severity ou ordem de
implementação.

### Múltiplas consequências

Quando um único defeito afeta IR, CFG e dataflow, mas a primeira falha observada
é na IR, a classe primária é `BLOCKS_IR`. CFG e dataflow aparecem como
consequências documentadas.

### Fora da arquitetura semântica

Um finding exclusivamente de licença ou de organização documental pode ser
`NOT_APPLICABLE` quando não há consumidor semântico afetado. Um problema de
provenance não é automaticamente `NOT_APPLICABLE`: se ele impede a identidade
ou rastreabilidade exigida por um produto semântico, a evidência deve ser
analisada e a classe pode ser outra ou continuar `UNASSESSED`.

## Checklist para agentes e revisores

Antes de aceitar uma classe diferente de `UNASSESSED`, confirme:

- qual informação está errada ou ausente;
- em qual produto isso foi observado;
- quais fronteiras anteriores continuam semanticamente corretas;
- qual contrato é o primeiro que não pode ser cumprido;
- por que as classes anteriores foram rejeitadas;
- qual evidência reproduzível sustenta a decisão.

Para `UNASSESSED`, confirme também por que ainda não é possível escolher a
primeira fronteira e registre ao menos um gatilho em `reassess_when`. Para uma
classe determinada, não invente `reassess_when`: omita-o quando não houver
motivo real. Em todos os casos, um campo obrigatório vazio invalida o registro.

Se qualquer resposta depender de “provavelmente”, de nome, regex, proximidade,
regra gramatical isolada ou de uma camada ainda não definida, use
`UNASSESSED`. `NOT_APPLICABLE` exige a demonstração positiva de que não existe
impacto semântico downstream, e não apenas ausência de investigação.
