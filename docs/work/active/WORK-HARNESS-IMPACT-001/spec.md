# WORK-HARNESS-IMPACT-001 — Downstream semantic impact classification

## Problema

O harness já distingue tipos de finding e disposições de remediação em
Discovery e possui referências operacionais a severity, mas não possui uma
taxonomia canônica para responder qual é a primeira fronteira downstream
semanticamente incorreta. Sem essa separação,
um finding pode ser associado por intuição a CFG/dataflow, receber várias
camadas como se fossem classes primárias ou confundir impacto com prioridade.

## Objetivo

Adicionar ao harness uma disciplina documental pequena e validável para que
findings semânticos futuros registrem uma classe primária de impacto downstream,
rationale, evidência e gatilhos de reavaliação, preservando `UNASSESSED` quando
as fronteiras ainda não estiverem definidas.

## Domínio de entrada suportado

O domínio é a documentação de bugs, gaps e findings semânticos em work items,
evals, backlog ou relatórios de Discovery futuros. A classificação observa os
produtos separados do pipeline e seus contratos quando existirem; não executa
lowering, não cria produtos downstream e não altera o modelo atual de AST,
symbols, occurrences ou resolution. Findings exclusivamente não semânticos
podem usar `NOT_APPLICABLE` somente com evidência positiva de ausência de impacto
semântico.

## Classes semânticas

A fonte canônica define exatamente estas oito classes: `BLOCKS_SEMANTIC_PRODUCT`,
`BLOCKS_IR`, `BLOCKS_CFG`, `BLOCKS_DATAFLOW`, `BLOCKS_DEPENDENCY_FACTS`,
`REDUCES_PRECISION`, `UNASSESSED` e `NOT_APPLICABLE`. A classe é única e segue
`earliest broken layer wins`.

`BLOCKS_SEMANTIC_PRODUCT`, `BLOCKS_IR`, `BLOCKS_CFG`, `BLOCKS_DATAFLOW` e
`BLOCKS_DEPENDENCY_FACTS` exigem evidência dos contratos da fronteira escolhida
e das fronteiras anteriores realmente necessárias. Um semantic extractor pode
depender somente do Semantic Product ou de um subconjunto das fases anteriores.
`REDUCES_PRECISION` exige prova de soundness/conservadorismo.
`UNASSESSED` é o resultado obrigatório quando essa prova não existe.
`NOT_APPLICABLE` exige evidência positiva de ausência de consumidor semântico e
não é um substituto para desconhecimento.

## Premissas

- A taxonomia é baseada em evidência observável, não em semelhança textual,
  nome de construct, proximidade de código ou conjectura do agente.
- A primeira camada quebrada é a classe primária; consequências ficam no
  rationale/evidence.
- Impacto, tipo do finding, severity, prioridade e autorização de remediação
  são dimensões independentes.
- Semantic Product, Cobol Lower, IR, CFG e dataflow ainda não são produtos
  criados por este work item; uma fronteira inexistente não pode ser tratada
  como provada.
- `UNASSESSED` preserva incerteza e deve indicar `reassess_when`; para as demais
  classes, esse campo só aparece quando houver motivo real e, se aparecer, não
  pode estar vazio.
- Não adotar `confidence` agora é uma decisão de simplicidade: não há escala
  reproduzível nem schema de findings que justifique seus valores.

## Comportamento esperado

Todo finding semântico novo registra um bloco `downstream_impact` com `class`,
`rationale` e `evidence`. `reassess_when` é obrigatório para `UNASSESSED`,
opcional para as demais classes e, quando presente, deve ter uma entrada não
vazia. O validator documental rejeita classe fora do vocabulário canônico,
campos obrigatórios vazios, evidência sem entrada e registros `UNASSESSED` sem
gatilho; a documentação explica como justificar as fronteiras anteriores
rejeitadas.

A classificação não altera o finding original, não autoriza a remediação e não
gera prioridade implícita. O caso F-01 é usado como exemplo realista e mantém
impacto `UNASSESSED`, porque a fronteira do Semantic Product e os requisitos da
IR ainda não existem como contratos atuais.

## Comportamento diante de incerteza

Quando a evidência não identifica a primeira fronteira, a classe é
`UNASSESSED`, mesmo que o agente suspeite de CFG, dataflow ou perda de precisão.
O rationale deve explicar o produto observado, a informação perdida ou errada,
o que ainda não está definido e por que não é legítimo selecionar uma classe.
`NOT_APPLICABLE` exige o oposto: evidência de que nenhum consumidor semântico
downstream é afetado.

Reavaliações acrescentam nota datada com classe anterior, classe nova, evidência
e gatilho; não apagam a história. O work item não cria engine de eventos.

## Fora de escopo

Não fazem parte deste work item: Semantic Product, Cobol Lower, IR, CFG,
dataflow, reaching definitions, possible values, efeitos de memória, alteração
de AST/symbols/occurrences/resolver, correção de F-01, classificação automática
do backlog, migração massiva de findings, novo sistema de issue tracking,
produção COBOL, banco de dados, enum de produção ou heurística de classificação.

Também não fazem parte: transformar impacto em severity/prioridade, adotar
`confidence` sem escala demonstrável ou iniciar o Discovery arquitetural
Semantic Product → Cobol Lower → IR.

## Regras de domínio relacionadas

- A separação de produtos e a ausência atual de CFG/dataflow estão em
  `docs/architecture/pipeline.md`.
- A AST não contém produtos posteriores e a incompletude é explícita em
  `docs/architecture/invariants.md` (`INV-AST-001`, `INV-COV-001` e
  `INV-COV-003`).
- A política de análise semântica exige evidência, incerteza preservada e
  rejeição de heurísticas em `docs/engineering/semantic-analysis-policy.md`.
- O contrato operacional de impacto está em
  `docs/engineering/downstream-impact-classification.md`.

## ADRs/invariantes relacionados

`ADR-0003` mantém produtos de análise separados; `ADR-0008` torna incompletude
um resultado de primeira classe; `INV-AST-001` impede antecipar produtos
downstream na AST; `INV-COV-001` e `INV-COV-003` preservam incerteza e fatos
independentes. `EVAL-COV-003` e `EVAL-ARCH-001` fornecem os contextos
executáveis relacionados, sem serem convertidos em um novo eval semântico.
