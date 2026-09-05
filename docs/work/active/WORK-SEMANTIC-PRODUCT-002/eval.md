# Eval da implementação do primeiro Semantic Product

## O que prova corretude

O trabalho é correto quando, para a classe de entrada suportada, um adapter
COBOL-specific constrói uma publicação A2 fechada e um consumer independente
reconstrói o slice somente pela facade B. A publicação deve preservar a
identidade namespaced da DATA, o `PIC`, o literal do `MOVE`, a ligação do target
do `MOVE` com o operando do `CALL`, o ordering observado, provenance, policy e
status de análise. Deve afirmar nominal binding conhecido somente onde há
candidate único e deve publicar `UNKNOWN`/uncertainty para o target de runtime.

O oracle principal já foi executado no Checkpoint 3B e está em
`docs/history/evidence/semantic-product-boundary-checkpoint-3b.md`; a
implementação de produção precisa reproduzir sua propriedade sem copiar a
boundary test-only como API. Os evals existentes listados no `work-item.yaml`
continuam sendo os contratos executáveis das fases de entrada.

## Oracle de review humano

O reviewer deve tratar cada resposta como PASS/FAIL e exigir evidência
localizável no diff ou nos testes. O slice só passa quando todas forem PASS:

1. Os tipos no port pertencem à boundary e não são aliases, wrappers
   transparentes ou referências retidas de AST, symbols, occurrences,
   resolution, ANTLR, snapshots ou composition root.
2. A construção é materializada antes de o consumer consultar o port; nenhuma
   query executa análise lazy, faz reparse ou depende da ordem de queries.
3. O join comum entre `MOVE` e `CALL` usa identidade namespaced e candidate
   DATA, e não nome escrito, linha, posição, primeiro candidato ou `Ast.Meta.id`
   isolado.
4. O reviewer consegue apontar onde o contrato distingue binding nominal,
   ordering observado e target de runtime. Ver `PGMA` como target concreto é
   FAIL, mesmo para a fixture linear.
5. Unknown e partial permanecem informação positiva e localizada: nenhum
   campo ausente é reinterpretado como lista vazia, nenhum ambiguity é
   colapsado e nenhum gap é escondido para elevar readiness.
6. Provenance/exactness/include chain e policy `UNSPECIFIED` continuam
   observáveis sem exigir o `SourceMap` inteiro ou options raw inexistentes.
7. O diff não antecipa `CobolLower`, IR, CFG, dataflow, possible-values,
   dependency extraction, serializer/JSON, outra construção ou outra linguagem.

Uma resposta “não demonstrado” é finding, não aprovação condicional. Se surgir
um finding semântico novo, o reviewer deve classificá-lo pela taxonomia
`downstream_impact` antes de propor qualquer correção; este work item não
autoriza expandir seu escopo para resolvê-lo.

## Classes positivas

- O consumer obtém uma unit namespaced e um `DataItemId` boundary-owned; o
  mesmo handle aparece no target do `MOVE` e no operando do `CALL`.
- `WS-PGM`, `PIC X(8)`, literal `PGMA`, provenance da declaração/literal/
  statements e exactness são publicados por fatos tipados.
- A sintaxe `IDENTIFIER_OR_EXPRESSION` do `CALL` atravessa como chamada
  variável; binding DATA é `COMPLETE`, mas `runtimeTarget` é `UNKNOWN`.
- `DYNAMIC_CALL_TARGET_VALUE_UNKNOWN` permanece localizado no `CALL`, sem
  reduzir ou apagar o binding nominal conhecido.
- `MOVE` precede `CALL` por ordering/program points explícitos. O consumer não
  precisa de parse tree, grammar metadata, snapshots ou texto semântico.
- O estado é construído uma vez, todas as coleções expostas são imutáveis, o
  port é query-only e o consumer continua operando depois de o frontend ser
  liberado.
- Execuções repetidas com a mesma entrada/policy produzem os mesmos valores e
  a mesma ordem; isso não vira promessa de identidade cross-run.
- A policy normalizada mantém opções ausentes como `UNSPECIFIED` e não trata a
  falta delas como licença para inventar linkage ou target.
- O estado A2, a facade B e o consumer não carregam referências de frontend,
  ANTLR, presentation, serializer ou provider lazy.

## Classes negativas

- Publicar `PGMA` como `runtimeTarget` do `CALL WS-PGM`, ou criar uma lista de
  possible-values a partir do `MOVE` e da ordem linear.
- Juntar referências por nome escrito, nome terminal, linha, posição ou
  `Ast.Meta.id` sem namespace; escolher o primeiro candidate; descartar
  candidates em ambiguity.
- Reparsear `writtenText`, `grammarRule`, HTML/JS ou snapshots para recuperar
  literal, papel ou ordering.
- Expor `Ast`, `ParseTree`, `SymbolTable`, `ReferenceResolution`,
  `ResolutionAnalysisReport`, `SourceMap` ou índices no port, ou manter um
  desses objetos vivo para responder query.
- Representar `UNKNOWN`, input missing, unsupported, unresolved ou ambiguity
  por lista vazia, exception genérica ou claim `COMPLETE`.
- Anotar/mutar produtos anteriores, criar serializer/JSON, alterar grammar,
  implementar lowerer, CFG, dataflow ou generalizar o produto para outros
  constructs/linguagens.

## Classes ambíguas

- Nome e organização dos arquivos Java da boundary podem variar, mas a
  ownership, os papéis e as dependências do contrato não.
- A implementação pode usar um root com records aninhados ou tipos separados;
  ambos são válidos somente se o port expuser tipos próprios, imutáveis e
  namespaced.
- Uma API pode devolver fatos diretamente ou views tipadas; não é válido decidir
  essa forma adicionando internals, reflection ou bag dinâmico.
- O `ProgramPoint` do slice pode ser chamado de ordinal/source order, desde que
  não seja apresentado como ordem de execução ou CFG.
- A policy e a claim global podem ser expostas como core ou capability do
  estado, desde que a ausência seja explícita e fique coerente na publicação.
- Os tipos `ExperimentalCobolMoveCallBoundary` e
  `ExperimentalCobolMoveCallConsumer` servem como oracles de Discovery; não
  são uma especificação de nomes nem uma autorização para reutilizá-los em
  produção.

## Casos adversariais

- A fixture `MOVE 'PGMA' TO WS-PGM` / `CALL WS-PGM` deve falhar uma mutação que
  converta o literal em target final, mesmo com ordering linear.
- Construir o estado diretamente, sem frontend, e consumi-lo pela facade deve
  produzir o mesmo shape e deve continuar válido após liberar referências ao
  frontend.
- Tentar mutar as listas retornadas por state e port deve falhar; chamar as
  queries em ordens diferentes não pode mudar valores, ordering ou status.
- Repetir a análise deve preservar a ordem e os joins; não deve comparar ou
  reivindicar IDs estáveis entre execuções independentes.
- Alterar apenas a policy ausente deve manter o binding DATA e tornar somente
  facts dependentes da policy `UNSPECIFIED`/`UNKNOWN`.
- Montar candidate ambiguity, unresolved, unsupported ou input missing para um
  uso do slice deve preservar status/reason/candidates e claim incompleta, sem
  selecionar ou fabricar handle.
- Remover a provenance exata ou trocar COPY disponível por input ausente deve
  ser observável no status/anchor; fatos independentes não podem desaparecer.
- Inspecionar bytecode e fonte da boundary/consumer deve detectar qualquer
  dependência direta de frontend, ANTLR, `writtenText` ou `grammarRule`.
- Uma entrada fora do slice, como `CALL 'PGMA'`, outro statement ou outra unit,
  deve permanecer explicitamente fora do domínio/unsupported; sua aceitação não
  pode surgir por fallback genérico.

## Casos de regressão

Os gates existentes devem continuar verdes para AST, pre-order, symbols,
occurrences, resolução nominal, CALL semantics, coverage, provenance,
classificação externa, snapshots, performance e architecture boundary. O
consumer independente novo deve ser coberto por testes de contrato e não deve
ser acoplado aos testes de presentation.

Evals canônicos relacionados:

- `EVAL-AST-001`, `EVAL-AST-004`, `EVAL-AST-005` para surface, separação e
  identidade estrutural;
- `EVAL-PROV-001`, `EVAL-PROV-002`, `EVAL-COV-003` para provenance e análise
  parcial;
- `EVAL-UNIT-001`, `EVAL-SYM-001`, `EVAL-SYM-002` e
  `EVAL-RES-DATA-001/002/003` para namespaces, declarations e joins;
- `EVAL-RES-CALL-001/002` e `EVAL-RES-REPORT-001` para sintaxe, binding,
  unknown e readiness;
- `EVAL-RES-DET-001` e `EVAL-ARCH-001` para determinismo e leakage.

## Propriedades/relações metamórficas

- Mesma entrada normalizada e mesma policy produzem a mesma ordem de facts,
  mesma relação `MOVE` → `CALL` e os mesmos valores; a análise não depende da
  ordem de chamadas à facade.
- Trocar o literal escrito do `MOVE` altera o literal e sua provenance, mas não
  transforma nem torna conhecido o target de runtime do `CALL`.
- Renomear consistentemente a DATA e os dois usos altera somente a identidade/
  grafia projetada; o join comum e o status nominal permanecem.
- Alterar uma declaração não relacionada não muda o `DataItemId`/binding quando
  a resolução canônica continua única.
- Substituir uma option ausente por outra policy explícita altera somente facts
  policy-dependent; não pode reclassificar `CALL WS-PGM` como target concreto.
- Tornar um input estruturalmente ausente preserva facts independentes e muda
  a claim/readiness para incompleta, em vez de produzir sucesso vazio.

## Expectativas de escala

Não há threshold de hardware novo nem autorização para otimização semântica.
Para o slice pequeno, a projeção deve percorrer a surface da unit de forma
determinística, fazer joins por índices/identidades existentes e evitar
`O(references × all declarations)`. A boundary não pode reduzir candidates,
provenance ou estados para ganhar memória/tempo. Qualquer custo adicional de
materialização deve ser medido somente em checkpoint autorizado posterior,
preservando os contratos semânticos.
