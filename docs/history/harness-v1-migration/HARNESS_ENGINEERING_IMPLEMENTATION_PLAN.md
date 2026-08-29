# Plano de Implementação — Harness Engineering v1 para o COBOL Structure Atlas

**Projeto alvo:** `antlr-parse-tree-explorer/` no repositório `proleap-poc`  
**Natureza deste documento:** plano arquitetural e de migração de conhecimento  
**Objetivo:** transformar o repositório em um ambiente *agent-legible*, verificável, conservador e eficiente em contexto, preservando todo conhecimento semântico e arquitetural já adquirido pelo projeto.

---

## 1. Resumo executivo

O projeto já possui muitos dos componentes que um bom *agent harness* precisa: especificações detalhadas, TDD por fases, fixtures COBOL mínimas, regressões sobre programas reais, contratos fail-closed, preocupação explícita com provenance, determinismo, complexidade, separação de fases semânticas e políticas anti-heurística.

O problema principal não é falta de rigor. É que esse rigor está hoje distribuído de forma pouco roteável entre:

- um documento-fonte de engenharia muito extenso, ainda não usado operacionalmente pelo repositório;
- `README.md`;
- ADRs;
- tasklists concluídas ou parcialmente concluídas;
- planos de hardening;
- relatórios de regressão;
- relatórios de correctness;
- scripts específicos de regressão;
- testes Java;
- fixtures;
- código que incorpora decisões arquiteturais que nem sempre possuem um registro canônico próprio.

Esse arranjo obriga um agente novo a reconstruir parte relevante do modelo mental do projeto em cada sessão. O custo não é apenas em tokens. Ele cria risco de:

- uma decisão já encerrada ser tratada como uma escolha ainda aberta;
- um agente simplificar uma fronteira arquitetural deliberada;
- uma regra aparecer em vários documentos com pequenas divergências;
- um relatório histórico ser confundido com contrato atual;
- uma tasklist concluída continuar sendo lida como fonte normativa;
- uma implementação correta ser substituída por uma heurística local porque o agente não encontrou o racional anterior;
- o contexto necessário para uma tarefa pequena ser inflado com centenas ou milhares de linhas irrelevantes.

O Harness Engineering v1 deve converter o repositório de um conjunto de documentos ricos, porém relativamente monolíticos, para um **sistema de conhecimento roteável + contratos executáveis + oráculos claros**.

A transformação deve seguir esta ideia:

```text
ANTES

AGENTS grande
+ tasklists grandes
+ relatórios
+ README
+ código
+ testes
        ↓
agente reconstrói o que importa


DEPOIS

AGENTS pequeno e roteador
        ↓
work item ativo
        ↓
documentos canônicos específicos
        ↓
ADRs / invariantes relevantes
        ↓
código e testes relacionados
        ↓
evals e gates executáveis
```

O objetivo não é apenas produzir documentação melhor.

O objetivo é fazer o repositório responder, de forma barata e confiável, às seguintes perguntas para qualquer agente:

1. O que este sistema faz?
2. Em qual fase do pipeline estou mexendo?
3. Quais decisões já estão fechadas?
4. Quais invariantes não podem ser violados?
5. Qual documento é autoridade sobre esta questão?
6. Qual conhecimento é histórico e não deve entrar no contexto normal?
7. Quais arquivos de código são relevantes?
8. Qual é o oráculo de corretude?
9. Qual comando prova que a mudança continua válida?
10. Como representar explicitamente aquilo que a análise ainda não sabe?

---

# 2. Princípios fundamentais do Harness Engineering v1

## 2.1. O repositório é a memória externa do agente

O chat não deve ser o lugar primário onde decisões arquiteturais sobrevivem.

Conhecimento durável precisa residir no repositório em uma forma:

- versionada;
- navegável;
- canônica;
- suficientemente pequena;
- relacionada aos artefatos executáveis que a verificam;
- explicitamente separada de histórico e trabalho transitório.

O agente deve conseguir retomar o projeto sem depender de memória conversacional anterior.

---

## 2.2. `AGENTS.md` é um roteador, não uma enciclopédia

O novo `AGENTS.md` deve ter como função principal:

- estabelecer o propósito do projeto;
- mostrar o pipeline em alto nível;
- conter somente as regras realmente universais;
- ensinar onde encontrar conhecimento por tipo de tarefa;
- indicar como localizar o work item ativo;
- indicar os gates existentes;
- impedir leitura indiscriminada de histórico.

Ele não deve conter a explicação completa de:

- name resolution COBOL;
- tabela de símbolos;
- GLOBAL/COMMON/EXTERNAL;
- CFG;
- dataflow;
- provenance;
- property-based testing;
- metamorphic testing;
- regras específicas de `QUALIFY`;
- detalhes de SourceMap;
- todos os red flags de revisão;
- toda a estratégia de testes semânticos.

Esse conhecimento continua importante. Ele apenas passa a viver em documentos especializados.

---

## 2.3. Contexto deve ser carregado just-in-time

A unidade de otimização não é apenas o tamanho do prompt.

É a quantidade de **contexto irrelevante** que entra na sessão.

O harness deve promover esta hierarquia:

```text
Tier 0 — sempre
AGENTS.md

Tier 1 — tarefa atual
work-item.yaml
state.md

Tier 2 — contratos diretamente relacionados
seções específicas de docs de domínio
invariantes
ADRs

Tier 3 — implementação
arquivos de código
testes
fixtures

Tier 4 — somente quando necessário
tasklists históricas durante a migração
relatórios antigos
evidências extensas
commits antigos
artefatos gerados
```

Após a migração, o Tier 4 deve ficar fora do caminho normal de descoberta.

---

## 2.4. Conhecimento permanente, trabalho transitório e evidência histórica são categorias diferentes

O harness deve separar explicitamente:

### Conhecimento permanente

Exemplos:

- fronteiras AST → Symbol Table → Resolution → CFG/Dataflow;
- provenance;
- regras de resolução;
- política fail-closed;
- invariantes;
- decisões arquiteturais;
- supported/unsupported boundaries.

### Trabalho transitório

Exemplos:

- qual slice está sendo implementado;
- quais arquivos devem mudar;
- qual teste está vermelho;
- qual é o próximo subpasso;
- estado atual de uma implementação.

### Evidência histórica

Exemplos:

- relatório de uma regressão antiga;
- contagens antigas;
- motivação detalhada de um hardening concluído;
- resultados de uma análise comparativa;
- snapshot de uma fase anterior.

Misturar essas categorias aumenta tokens e cria ambiguidade de autoridade.

---

## 2.5. Testes são evidência; semântica e contratos definem corretude

O harness deve preservar como princípio central a distinção já presente no projeto e reforçada pelo documento-fonte importado:

> testes e corpus demonstram comportamento; eles não definem a linguagem.

Consequências:

- comportamento de produção não nasce de fixtures;
- uma regressão não deve cristalizar um bug;
- corpus não determina supported language surface;
- o agente precisa identificar a regra de linguagem/projeto que justifica uma mudança;
- evals devem distinguir a implementação correta de implementações plausíveis, porém erradas.

---

## 2.6. Heurística nunca pode se disfarçar de semântica

A política anti-overfitting já refletida nas specs/código e sistematizada no documento-fonte importado deve ser consolidada pela migração.

A ordem de preferência permanece:

```text
1. algoritmo semântico/exato;
2. aproximação conservadora formalmente caracterizada;
3. heurística explicitamente documentada.
```

Quando a análise não consegue concluir algo:

```text
UNKNOWN
UNRESOLVED
UNSUPPORTED
AMBIGUOUS
INCOMPLETE
```

ou estados equivalentes devem continuar preferíveis a uma resposta inventada.

---

## 2.7. Invariantes importantes devem migrar de prosa para enforcement quando possível

Uma regra crítica não deve depender apenas de um agente lembrar de um parágrafo.

Sempre que uma regra puder ser mecanicamente verificada sem distorcer a arquitetura, o harness deve possuir um gate correspondente.

Exemplo conceitual:

```text
Regra documental:
AST não depende de ReferenceResolution.

Melhoria de harness:
check-architecture verifica essa dependência.
```

A documentação continua existindo para explicar a regra e seu racional.

O gate reduz a necessidade de julgamento repetido.

---

## 2.8. Nenhum documento deve nascer vazio apenas para satisfazer a árvore planejada

A estrutura proposta neste plano é uma **taxonomia-alvo**, não uma obrigação de criar placeholders.

Não criar:

```text
docs/domain/cfg.md
```

apenas porque CFG existirá no futuro.

Criá-lo quando existir conhecimento atual relevante para registrá-lo.

O mesmo se aplica a:

- ADRs;
- docs de domínio;
- manifests;
- índices específicos;
- pastas de work item;
- categorias de eval.

Um arquivo vazio ou quase vazio aumenta entropia, não legibilidade.

---

# 3. Ressalva crítica sobre remoção do `AGENTS` legado e das tasklists

A estratégia proposta faz sentido com uma condição forte:

> arquivos legados só podem ser removidos depois que o conhecimento durável contido neles tiver um destino canônico verificável.

A remoção não deve ser baseada em:

- “a tarefa está concluída”;
- “os testes passam”;
- “o código já implementa isso”;
- “a informação parece repetida”.

Antes da remoção deve existir uma **Knowledge Migration Matrix**.

Cada unidade de conhecimento relevante do arquivo legado precisa ser classificada.

Categorias mínimas:

```text
DECISION
INVARIANT
DOMAIN_RULE
SUPPORTED_SCOPE
UNSUPPORTED_BOUNDARY
ARCHITECTURE_BOUNDARY
ALGORITHM_CONTRACT
TESTING_POLICY
EVAL_ORACLE
REGRESSION_BASELINE
PERFORMANCE_CONTRACT
OBSERVABILITY_CONTRACT
OPERATIONAL_PROCEDURE
HISTORICAL_EVIDENCE
ACTIVE_BACKLOG
COMPLETED_TRANSIENT_WORK
SUPERSEDED
OBSOLETE
UNCERTAIN
```

Cada item recebe um destino.

Exemplo:

| Fonte | Conhecimento | Tipo | Destino | Estado |
|---|---|---|---|---|
| documento-fonte importado `specs/AGENTS.md — antlr-parse-tree-explorer.md` §7 | AST não contém binding/dataflow | INVARIANT | `docs/architecture/invariants.md#INV-AST-001` | MIGRATED |
| documento-fonte importado `specs/AGENTS.md — antlr-parse-tree-explorer.md` §18 | usar oracle diferencial quando disponível | TESTING_POLICY | `docs/engineering/semantic-testing.md` | MIGRATED |
| reference-resolution tasklist | CALL dinâmico resolve variável, não valor | DOMAIN_RULE + DECISION | domain doc + ADR | MIGRATED |
| tasklist antiga | “criar classe X” | COMPLETED_TRANSIENT_WORK | nenhum destino durável necessário | DISCARDED_WITH_REASON |
| relatório antigo | contagem de 23 testes | HISTORICAL_EVIDENCE | history/evidence | ARCHIVED |
| item ainda não feito | suporte a forma Y | ACTIVE_BACKLOG | `docs/work/backlog.md` ou work item | MIGRATED |

A exclusão de um arquivo exige:

```text
UNMAPPED = 0
UNCERTAIN = 0
ACTIVE_BACKLOG sem destino = 0
links canônicos para o legado = 0
```

O Git preserva o histórico. Portanto, detalhes puramente operacionais de uma tasklist concluída não precisam ser duplicados para sempre.

O que precisa sobreviver é o **conhecimento que muda decisões futuras**.

---

# 4. Fontes legadas que devem entrar na arqueologia inicial

O inventário deve ser derivado do repositório no momento da implementação, e não de uma lista congelada neste plano.

Entretanto, a migração deve considerar no mínimo as categorias e arquivos atualmente existentes, incluindo:

## 4.1. Documento-fonte de engenharia a ser importado para a migração

### Status correto desta fonte

Este documento **não existe atualmente no repositório como `AGENTS.md` operacional** e nunca foi utilizado para instruir agentes no projeto.

Ele será colocado temporariamente em:

```text
antlr-parse-tree-explorer/specs/AGENTS.md — antlr-parse-tree-explorer.md
```

apenas para participar da migração de conhecimento descrita neste plano.

Portanto:

- não presumir que suas regras já governaram implementações anteriores;
- não utilizá-lo como evidência histórica de decisões do projeto;
- confrontar suas recomendações com código, specs, testes, reports, ADRs e commits antes de promovê-las a contratos canônicos;
- aproveitar seu conteúdo como uma fonte de princípios de engenharia que já foi previamente elaborada e aprovada conceitualmente pelo responsável pelo projeto;
- remover o arquivo ao final da migração, depois que todo conteúdo escolhido tiver destino canônico.


O documento hoje conhecido como:

```text
AGENTS.md — antlr-parse-tree-explorer.md
```

ou o nome equivalente usado quando ele for colocado no projeto.

Embora nunca tenha sido usado como `AGENTS.md` do repositório, ele contém material de engenharia valioso que deve ser tratado como uma das fontes de conhecimento da migração e decomposto principalmente em:

- semantic-analysis policy;
- testing/eval policy;
- performance policy;
- invariantes arquiteturais;
- regras de resolução;
- provenance;
- regras de CFG/dataflow futuras;
- Definition of Done semântico;
- red flags de review.

Durante a migração ele deve ser tratado como `IMPORTED_ENGINEERING_KNOWLEDGE_SOURCE`, sem autoridade especial sobre o estado histórico do repositório.

O arquivo será inserido temporariamente em:

```text
antlr-parse-tree-explorer/specs/AGENTS.md — antlr-parse-tree-explorer.md
```

Esse nome não representa um `AGENTS.md` operacional. O documento nunca foi usado como instrução automática do projeto e não deve ser tratado como evidência de uma política historicamente aplicada.

Sua função durante o Harness Engineering v1 é exclusivamente:

```text
fonte temporária de conhecimento de engenharia
        ↓
classificação
        ↓
redistribuição para documentos canônicos
        ↓
remoção
```

O novo `AGENTS.md` operacional será criado apenas em uma fase posterior, depois que seus destinos de roteamento existirem.

---

## 4.2. Tasklists e planos legados

O inventário deve localizar todos os arquivos com natureza de planejamento, independentemente do sufixo.

Exemplos atualmente conhecidos:

```text
docs/source-normalizer-hardening-plan.md
docs/antlr-parse-tree-explorer-logging-tasklist.md

antlr-parse-tree-explorer/specs/cbstm03d-dynamic-calls-tasklist.md
antlr-parse-tree-explorer/specs/project-naming-cleanup-tasklist.md
antlr-parse-tree-explorer/specs/semantic-model-hardening-tasklist.md
antlr-parse-tree-explorer/specs/reference-resolution-tasklist.md
antlr-parse-tree-explorer/specs/reference-resolution-semantic-correctness-hardening-II-tasklist.txt
antlr-parse-tree-explorer/specs/reference-resolution-semantic-correctness-hardening-III-tasklist.txt
```

A implementação deve enumerar o repositório inteiro e adicionar qualquer outro documento equivalente encontrado.

Arquivos `*-plan*`, `*-tasklist*`, checklists históricas ou documentos que misturem execução concluída com conhecimento permanente devem passar pela mesma classificação.

---

## 4.3. Relatórios

Relatórios não devem ser automaticamente apagados junto com tasklists.

Exemplos conhecidos:

```text
reference-resolution-regression-report.md
reference-resolution-semantic-correctness-report.md
semantic-model-hardening-regression-report.md
```

Eles são fontes de evidência e podem conter:

- contraexemplos;
- métricas;
- bugs históricos;
- justificativas;
- resultados de gates;
- limitações observadas.

O conhecimento durável deve ser extraído.

Depois disso, a política padrão deve ser movê-los para uma área de histórico/evidência fora do roteamento normal, salvo se houver motivo para removê-los completamente.

---

## 4.4. Backlogs

Arquivos como:

```text
semantic-interpretation-backlog.md
```

não podem desaparecer como efeito colateral da limpeza.

Itens ainda válidos devem migrar para uma representação de backlog atual.

Itens concluídos ou superseded devem ser classificados explicitamente.

---

## 4.5. ADRs existentes

O projeto já contém ao menos um ADR relacionado a comment-entry normalization.

ADRs existentes não devem ser descartados para “recomeçar a numeração”.

Eles devem:

- ser incorporados à nova árvore;
- manter identidade quando possível;
- ser indexados;
- ser marcados como superseded somente quando existir decisão posterior real.

---

## 4.6. README

O `README.md` possui contratos importantes hoje.

Durante a migração, ele deve perder gradualmente a função de “manual semântico total”.

Ao final:

- README = visão do produto, execução, navegação e status de alto nível;
- docs canônicos = regras semânticas e arquitetura;
- README aponta para os docs canônicos em vez de duplicá-los extensivamente.

---

## 4.7. Código, testes, fixtures e scripts

A arqueologia não deve confiar apenas na prosa.

Uma decisão vigente pode estar evidenciada por:

- tipos e APIs públicas;
- direção de dependências;
- testes de caracterização;
- fixtures;
- regression scripts;
- manifests;
- commits que introduziram a decisão.

Essas fontes são especialmente importantes para ADRs retrospectivos.

---

# 5. Modelo de autoridade do conhecimento

O harness precisa deixar claro qual fonte vence quando duas peças de informação parecem divergir.

A seguinte hierarquia é recomendada.

## 5.1. Semântica externa da linguagem

Para regras COBOL:

```text
documentação oficial do dialeto configurado
        ↓
docs/domain/*
```

O domain doc deve registrar como a semântica externa é representada pelo projeto.

Não transformar uma decisão de representação em regra da linguagem.

---

## 5.2. Decisão arquitetural interna

Fonte canônica:

```text
Accepted ADR
```

Exemplos:

- separar binding nominal de dataflow;
- SourceMap nascer no fonte físico;
- usar catálogo explícito para programas externos.

---

## 5.3. Invariante arquitetural

Fonte canônica:

```text
docs/architecture/invariants.md
```

Um invariante pode derivar:

- de um ADR;
- de um requisito essencial de corretude;
- de uma fronteira arquitetural consolidada.

Quando derivar de ADR, deve referenciá-lo.

---

## 5.4. Contrato atual de um subsistema

Fonte canônica:

```text
docs/domain/<subsystem>.md
```

Esse documento explica o estado atual implementado.

Ele não é uma tasklist futura.

---

## 5.5. Evals e testes

São:

```text
evidência executável
```

e não a definição normativa isolada.

Devem apontar para:

- domain rule;
- invariant;
- ADR;
- bug/regression class;

quando fizer sentido.

---

## 5.6. Work item ativo

Um work item representa:

```text
mudança proposta/em andamento
```

Ele pode propor alterar um ADR ou contrato.

Enquanto a alteração não estiver consolidada, o documento canônico existente continua descrevendo o estado atual.

Ao concluir a mudança, o conhecimento permanente deve ser atualizado e o work item deixa de ser fonte normativa.

---

## 5.7. Histórico

Tasklists antigas, reports e outcome summaries são:

```text
evidência histórica
```

Não devem ser usados como autoridade automática sobre comportamento atual.

---

# 6. Estrutura-alvo do repositório

A árvore abaixo é conceitual.

**Não criar arquivos vazios.**

```text
antlr-parse-tree-explorer/
│
├── AGENTS.md
├── ARCHITECTURE.md
├── README.md
│
├── docs/
│   ├── index.md
│   │
│   ├── architecture/
│   │   ├── index.md
│   │   ├── pipeline.md
│   │   ├── invariants.md
│   │   └── decisions/
│   │       ├── index.md
│   │       ├── 0001-....md
│   │       ├── 0002-....md
│   │       └── ...
│   │
│   ├── engineering/
│   │   ├── index.md
│   │   ├── semantic-analysis-policy.md
│   │   ├── semantic-testing.md
│   │   ├── performance-policy.md
│   │   ├── observability-policy.md
│   │   └── work-item-protocol.md
│   │
│   ├── domain/
│   │   ├── index.md
│   │   ├── source-format-and-normalization.md
│   │   ├── preprocessing.md
│   │   ├── provenance.md
│   │   ├── semantic-ast.md
│   │   ├── compilation-units.md
│   │   ├── symbol-model.md
│   │   ├── reference-resolution.md
│   │   └── <futuros somente quando houver conteúdo real>
│   │
│   ├── evals/
│   │   ├── index.md
│   │   └── semantic-eval-catalog.md
│   │
│   ├── work/
│   │   ├── active/
│   │   │   └── <work-item-id>/
│   │   │       ├── work-item.yaml
│   │   │       ├── spec.md
│   │   │       ├── plan.md
│   │   │       ├── eval.md
│   │   │       └── state.md
│   │   ├── history/
│   │   │   └── <work-item-id>.md
│   │   └── backlog.md
│   │
│   └── history/
│       ├── index.md
│       ├── evidence/
│       └── harness-v1-migration/
│
├── harness/
│   ├── README.md                 # somente se houver conteúdo operacional real
│   ├── evals/
│   │   └── catalog.yaml
│   ├── architecture/
│   │   └── boundaries.yaml       # opcional, se checks forem data-driven
│   └── scenarios/
│       └── ...                   # somente se substituir hardcoding útil
│
├── scripts/
│   └── harness/
│       ├── check-fast.sh
│       ├── check-semantic.sh
│       ├── check-full.sh
│       ├── check-architecture.sh
│       ├── check-docs.sh
│       └── check-performance.sh
│
└── src/
    ├── main/
    └── test/
```

Nem todos esses arquivos precisam existir no Harness v1.

A regra é:

> um arquivo nasce porque há um contrato atual a registrar, não porque existe uma linha na árvore deste plano.

---

# 7. Responsabilidade de cada família documental

## 7.1. `AGENTS.md`

Deve responder:

- o que é este projeto;
- qual é o pipeline;
- onde está o work item ativo;
- como rotear tarefas por domínio;
- quais poucas regras são universais;
- quais gates existem;
- o que não deve ser carregado por padrão.

Não deve explicar extensivamente nenhum subsistema.

---

## 7.2. `ARCHITECTURE.md`

Documento curto.

Deve conter:

- mapa dos principais componentes;
- direção das dependências;
- artefatos produzidos por cada fase;
- links para docs detalhados;
- links para invariantes e ADR index.

Deve ser possível entender a arquitetura em poucos minutos.

---

## 7.3. `docs/index.md`

É o índice navegável do conhecimento.

Deve permitir encontrar:

- arquitetura;
- domínio;
- engenharia;
- evals;
- work items;
- histórico.

Pode conter uma tabela de roteamento por assunto.

---

## 7.4. `docs/architecture/invariants.md`

Invariantes recebem IDs estáveis.

Formato conceitual:

```text
INV-AST-001
INV-PROV-001
INV-RES-001
INV-PERF-001
...
```

Cada invariante deve registrar:

```text
ID
Statement
Rationale
Scope
Related ADRs
Enforcement
Known exceptions
```

`Enforcement` pode ser:

```text
AUTOMATED
PARTIALLY_AUTOMATED
REVIEW
```

Exemplo:

```text
INV-AST-001

Statement:
AST must not contain symbol bindings, CFG state or dataflow results.

Related decisions:
ADR-000X

Enforcement:
ArchitectureBoundaryTest + semantic review.
```

---

## 7.5. ADRs

Um ADR responde:

> por que o projeto escolheu esta arquitetura, em vez de alternativas plausíveis?

Não usar ADR para registrar pura semântica COBOL.

Formato recomendado:

```text
# ADR-XXXX — Title

Status: Accepted | Superseded | Deprecated | Proposed
Type: Contemporary | Retrospective
Recorded: YYYY-MM-DD

## Context
## Decision
## Rationale
## Consequences
## Rejected alternatives
## Evidence in current implementation
## Related invariants
## Related domain docs
## Supersedes / Superseded by
```

### ADR retrospectivo

Quando a decisão antecede o documento:

```text
Type: Retrospective
Decision predates this ADR.
```

Não inventar:

- data histórica;
- participantes;
- alternativas discutidas;
- racional não sustentado.

Registrar somente o que pode ser reconstruído por evidência.

---

## 7.6. `docs/domain/*`

Um domain doc responde:

> como este subsistema funciona semanticamente hoje?

Estrutura recomendada:

```text
Purpose
Scope
Inputs
Outputs
Current supported semantics
Explicit unsupported boundaries
Algorithm / semantic model
Diagnostics and uncertainty
Complexity expectations
Provenance requirements
Related invariants
Related ADRs
Authoritative language references
Relevant eval IDs
```

Ele deve descrever estado atual, não um plano de implementação futuro.

---

## 7.7. `docs/engineering/semantic-analysis-policy.md`

Este será um dos principais destinos do `AGENTS` legado.

Conteúdo esperado:

- tests are evidence, not specification;
- assumption audit;
- exact algorithms over heuristics;
- heuristic policy;
- fail closed;
- preserve ambiguity;
- soundness/completeness;
- no fixture-driven production logic;
- general semantic fix versus narrow fix;
- architecture before convenience;
- diagnostics as semantic results;
- correctness argument;
- anti-overfitting review questions.

Ele é lido para trabalho semântico de risco relevante.

Não precisa ser lido para logging, CSS ou rename.

---

## 7.8. `docs/engineering/semantic-testing.md`

Destino das partes do AGENTS legado relacionadas a:

- equivalence-class testing;
- adversarial tests;
- property-based testing;
- metamorphic testing;
- differential/reference oracle;
- mutation testing;
- independent challenge pass;
- test strength.

Esse documento descreve **como construir oráculos fortes**.

---

## 7.9. `docs/engineering/performance-policy.md`

Deve conter:

- performance must not change semantics;
- complexity review;
- large-input concerns;
- anti-patterns como `O(references × all declarations)`;
- indexação;
- worklists;
- caching/memoization;
- medição;
- separação entre functional correctness e machine-dependent thresholds.

---

## 7.10. `docs/engineering/observability-policy.md`

Só deve existir se o conteúdo atual de logging/observability justificar um documento próprio.

Pode receber conhecimento durável do logging tasklist:

- níveis;
- contexto;
- cardinalidade;
- trace de decisões;
- performance de logging;
- diagnostics;
- lifecycle.

Detalhes temporários da implementação do logging não precisam sobreviver.

---

## 7.11. `docs/work/*`

Não é fonte permanente.

Serve para limitar o espaço de decisão da implementação atual.

### `work-item.yaml`

Roteamento e metadados.

### `spec.md`

Comportamento desejado.

### `plan.md`

Decomposição da mudança.

### `eval.md`

Oráculos necessários.

### `state.md`

Memória curta da execução.

Ao concluir:

- conhecimento durável migra;
- evals duráveis permanecem nos testes/catalog;
- decisões viram ADR/invariante quando aplicável;
- estado transitório é eliminado;
- um `history/<id>.md` curto pode registrar o resultado.

---

# 8. Modelo de IDs canônicos

IDs tornam o roteamento barato.

Sugestão:

```text
ADR-0001
INV-AST-001
INV-PROV-001
RULE-RES-DATA-001       # opcional; usar somente se ajudar
EVAL-RES-001
EVAL-PROV-001
WORK-CFG-001
```

Não criar IDs para cada parágrafo do projeto.

IDs devem existir quando ajudam:

- referência cruzada;
- rastreabilidade;
- work-item routing;
- checks automatizados;
- revisão.

---

# 9. Estratégia de roteamento de contexto

## 9.1. Rota por domínio

O novo `AGENTS.md` deve possuir uma tabela curta.

Exemplo conceitual:

| Tipo de tarefa | Contexto canônico |
|---|---|
| source format / normalization | source-format doc + provenance |
| preprocessing / COPY | preprocessing + provenance |
| AST | semantic AST + invariants |
| symbol model | symbol model + relevant ADRs |
| reference resolution | resolution + semantic policy |
| semantic algorithm | semantic policy + semantic testing |
| performance | performance policy + domain doc |
| logging | observability policy |
| rendering/frontend | frontend docs; semantic docs somente se cruzar boundary |
| future CFG/dataflow | docs criados quando o domínio for efetivamente iniciado |

---

## 9.2. Rota por risco

### LOW

Exemplos:

- rename;
- logging sem mudança semântica;
- HTML;
- apresentação de snapshot;
- pequenas ferramentas.

Contexto default:

```text
AGENTS
work item
arquivos relacionados
testes relacionados
```

### MEDIUM

Exemplos:

- refactor que cruza abstrações;
- performance interna;
- provenance implementation;
- alteração de snapshot semanticamente neutra.

Adicionar:

```text
invariantes
ADR relacionado
domain doc
```

### HIGH

Exemplos:

- parser semantics;
- AST semantics;
- symbol tables;
- reference resolution;
- CFG;
- reaching definitions;
- constant/value propagation;
- dependency inference.

Adicionar:

```text
semantic-analysis-policy
semantic-testing
domain doc
invariants
ADRs
evals
```

---

## 9.3. Rota específica pelo work item

`work-item.yaml` deve poder restringir ainda mais o contexto.

Exemplo:

```yaml
id: WORK-RES-017
title: Resolve qualified DATA reference through FILE
risk: high

goal:
  Resolve the supported qualified DATA-through-FILE form
  according to the current COBOL resolution policy.

must_read:
  - docs/domain/reference-resolution.md#data-qualification
  - docs/architecture/invariants.md#INV-RES-002
  - docs/engineering/semantic-analysis-policy.md#exact-algorithms-over-heuristics
  - docs/engineering/semantic-testing.md#adversarial-tests

related_decisions:
  - ADR-0004

source_scope:
  - src/main/java/.../DataAndIndexReferenceResolver.java
  - src/main/java/.../ReferenceResolution.java

test_scope:
  - src/test/java/.../DataAndIndexReferenceResolverTest.java
  - src/test/resources/cobol/resolution/

must_not_change:
  - grammar
  - AST binding boundary
  - CFG/dataflow scope

evals:
  - EVAL-RES-DATA-014
  - EVAL-RES-DATA-015

gates:
  - fast
  - semantic
  - full
```

O schema exato deve refletir o projeto real.

Evitar colocar explicações longas no YAML.

Ele é um mapa.

---

# 10. Política de orçamento de contexto

Não transformar números em correctness gate rígido, mas adotar metas.

## 10.1. `AGENTS.md`

Meta:

- aproximadamente 80–150 linhas;
- poucas regras universais;
- links/rotas;
- nada de tutorial extenso.

---

## 10.2. `work-item.yaml`

Meta:

- pequeno o suficiente para leitura imediata;
- lista limitada de `must_read`;
- preferir anchors/seções específicas;
- não duplicar a spec.

---

## 10.3. `state.md`

Meta:

```text
Current slice
Last known green state
Completed
Next
Known blockers
Relevant discoveries
Decisions already accepted during this work item
```

Evitar diário cronológico.

Idealmente abaixo de ~100 linhas.

---

## 10.4. Histórico

Nunca entra automaticamente.

O `AGENTS.md` deve dizer explicitamente:

```text
Do not read docs/history/ or completed work by default.
Use them only when the active task requires historical evidence.
```

---

## 10.5. Duplicação

Regra:

> uma regra normativa possui um único documento canônico.

Outros documentos apontam para ela.

Exemplo ruim:

```text
AGENTS explica GLOBAL
README explica GLOBAL
reference-resolution doc explica GLOBAL
tasklist explica GLOBAL
```

Exemplo desejado:

```text
domain/reference-resolution.md explica GLOBAL

AGENTS → link
ARCHITECTURE → link
work item → link/anchor
```

---

# 11. Evals como produto de primeira classe

O projeto já possui muitas fixtures e testes que podem se tornar a fundação do eval harness.

O objetivo é transformar:

```text
"temos muitos testes"
```

em:

```text
"temos oráculos semânticos catalogados por capacidade"
```

---

## 11.1. Catálogo de evals

Pode existir uma representação documental e, se houver valor, uma manifestação machine-readable.

Exemplo:

```yaml
- id: EVAL-RES-001
  domain: reference-resolution
  capability: unqualified-data-binding
  tier: semantic
  test:
    class: DataAndIndexReferenceResolverTest
  fixture:
    path: src/test/resources/cobol/resolution/data-binding.cbl
  proves:
    - unique DATA candidate is resolved
    - incompatible candidate kind is not selected
  related:
    invariants:
      - INV-RES-001
```

O esperado semântico deve continuar preferencialmente no teste executável.

O catálogo explica o propósito, não duplica todo o oracle.

---

## 11.2. Tipos de eval

### Contract eval

Valida API/artefato público.

### Semantic eval

Valida regra COBOL/projeto.

### Adversarial eval

Quebra shortcuts plausíveis.

### Regression eval

Preserva bug fix.

### Property eval

Valida propriedade geral.

### Metamorphic eval

Valida invariância sem exigir snapshot inteiro.

### Differential/reference eval

Compara com oracle independente.

### Determinism eval

Executa repetidamente e compara saída semântica.

### Scale eval

Verifica forma de crescimento/algoritmo.

### End-to-end corpus scenario

Executa pipeline real.

---

## 11.3. Evals não devem depender somente do corpus

Fixtures sintéticas mínimas continuam essenciais.

O catálogo deve distinguir:

```text
synthetic semantic fixture
real corpus regression
large-input benchmark
```

---

## 11.4. Um eval deve ser capaz de rejeitar uma implementação ingênua

Pergunta central:

> Qual implementação errada plausível este eval impede?

Se a resposta for “nenhuma”, o eval pode estar apenas exercitando código.

---

# 12. Pirâmide de gates

Os scripts atuais devem ser preservados inicialmente e incorporados progressivamente.

A meta é expor poucos comandos estáveis.

---

## 12.1. `check-fast`

Objetivo:

- feedback rápido durante implementação.

Pode incluir:

- compile;
- testes focados;
- checks estruturais baratos;
- doc/schema checks baratos.

Não deve executar corpus massivo.

---

## 12.2. `check-semantic`

Objetivo:

- provar contratos semânticos relevantes.

Pode incluir:

- suíte JUnit;
- grammar/coverage manifests;
- semantic fixtures;
- architecture invariants baratos;
- determinism checks selecionados;
- provenance invariants selecionados.

---

## 12.3. `check-full`

Objetivo:

- gate geral antes de considerar o estado do repositório saudável.

Deve agregar:

```text
check-fast
check-semantic
+
cenários reais
+
source normalization regression
+
artifacts
+
parser/lexer expectations
+
determinism relevante
+
doc integrity
```

Programas canônicos atuais, como os usados pelo projeto, podem permanecer como cenários.

As expectativas devem ser semânticas e deliberadas, não snapshots cegos.

---

## 12.4. `check-performance`

Separado.

Pode incluir:

- programa grande;
- fixture sintética parametrizável;
- cardinalidades;
- tempo;
- memória;
- detecção de regressão algorítmica quando viável.

Thresholds de hardware não devem virar testes funcionais frágeis.

---

## 12.5. `check-architecture`

Objetivo:

transformar fronteiras críticas em enforcement.

Possíveis checks:

- classes de AST não dependem de resolution/CFG/dataflow;
- symbol table não depende diretamente de parser se a fronteira atual exigir AST;
- rendering não vira modelo semântico;
- resolution não escreve estado em AST;
- CFG futuro não retroalimenta AST;
- dataflow futuro não invade nominal binding.

A implementação do check deve respeitar a estrutura atual de packages.

Não refatorar todo o package layout apenas para facilitar o checker.

Se classes estiverem no mesmo package, checks por dependency graph/class reference são aceitáveis.

---

## 12.6. `check-docs`

Objetivo:

proteger o sistema de conhecimento.

Pode validar:

- links internos;
- caminhos de `must_read`;
- IDs duplicados;
- ADR index;
- invariant IDs;
- eval IDs;
- work item schema;
- referências a documentos deletados;
- presença de arquivos canônicos referenciados;
- ausência de tasklists legadas após a fase de remoção.

---

# 13. Harness observability

O próprio harness deve produzir informação útil.

Quando um gate falhar, o agente deve conseguir descobrir:

```text
qual gate;
qual capability;
qual eval;
qual fixture;
qual invariant;
qual documento canônico.
```

Evitar:

```text
Harness failed.
```

Preferir saída conceitualmente equivalente a:

```text
EVAL-RES-014 failed
Capability: qualified DATA resolution through FILE
Fixture: ...
Related rule: docs/domain/reference-resolution.md#...
Related invariant: INV-RES-002
```

Isso reduz ciclos de investigação.

---

# 14. Architecture archaeology

A nova documentação arquitetural não deve ser escrita do zero com base em preferências.

Ela deve ser **extraída do estado consolidado**.

Fontes:

```text
código
testes
fixtures
README
tasklists
reports
ADRs existentes
commits relevantes
documento-fonte importado `specs/AGENTS.md — antlr-parse-tree-explorer.md`
```

---

## 14.1. Perguntas da arqueologia

Para cada subsistema:

1. Qual é sua responsabilidade atual?
2. Quais dados entram?
3. Quais dados saem?
4. Qual estado é imutável?
5. Qual fase pode depender de qual?
6. Quais comportamentos são deliberadamente unsupported?
7. Onde uncertainty aparece?
8. Quais decisões seriam tentadoras de “simplificar”?
9. Qual custo/complexidade é esperado?
10. Qual teste prova a fronteira?

---

## 14.2. Não confundir acidente com decisão

Nem todo código atual merece um ADR.

Exemplo:

```text
"classe X tem 800 linhas"
```

não é uma decisão.

Mas:

```text
"AST permanece independente de binding"
```

é uma decisão consolidada.

---

# 15. Candidatos iniciais a ADRs retrospectivos

Esta lista é um **inventário de candidatos**, não autorização para criar ADRs sem validação.

A arqueologia deve confirmar cada um.

Possíveis decisões já consolidadas:

## ADR candidato — Provenance começa no fonte físico

Decisão aparente:

```text
SourceMap nasce antes de normalization/preprocessing
e é composto através das transformações.
```

Alternativa rejeitada pela arquitetura atual:

```text
recriar identity map sobre texto já transformado.
```

---

## ADR candidato — AST é independente de produtos de análise

Decisão aparente:

```text
AST não contém symbol binding, resolution, CFG ou dataflow.
```

---

## ADR candidato — Symbol model e nominal binding são artefatos separados

Decisão aparente:

```text
Symbol Table contém declarations/scopes/entities;
ReferenceResolution contém occurrences/candidates/decisions.
```

---

## ADR candidato — Binding nominal não realiza value resolution

Decisão aparente:

```text
CALL WS-TARGET
```

resolve nominalmente `WS-TARGET`, mas seus valores pertencem a CFG/dataflow.

---

## ADR candidato — Compilation unit/program unit é fronteira de análise

Decisão aparente:

- IDs namespaced por unit;
- nesting modelado;
- sem tabela global conveniente para toda a codebase;
- futura paralelização por unit.

---

## ADR candidato — Catálogo externo explícito para programas

Decisão aparente:

- programas internos seguem visibilidade COBOL;
- externos dependem de catálogo;
- ausência de catálogo não vira “programa não existe”.

---

## ADR candidato — Embedded languages são analisadas por fronteiras dedicadas

Decisão aparente:

- EXEC SQL/CICS/SQLIMS podem permanecer opacos;
- não usar regex oportunista no core COBOL como substituto do parser dedicado.

---

## ADR candidato — Coverage/incompleteness é first-class

Decisão aparente:

- construção não compreendida bloqueia alegação de completude;
- `UNSUPPORTED`, `INPUT_MISSING`, `DEPENDENCY_UNKNOWN` etc. permanecem visíveis.

---

## ADR candidato — Grammar surface possui coverage explícito

Decisão aparente:

- manifesto/versioned coverage;
- nova grammar alternative não deve cair em suporte implícito.

---

# 16. Candidatos iniciais a invariantes

Novamente, validar contra estado real.

Possíveis famílias:

## AST

```text
INV-AST-001
AST does not contain nominal binding or dataflow results.

INV-AST-002
Semantic structure is derived from parser/grammar structure,
not reparsed from flattened text.

INV-AST-003
AST nodes preserve written form/provenance where contract requires it.
```

## Provenance

```text
INV-PROV-001
No transformed stage may recreate false identity provenance.

INV-PROV-002
Approximate provenance must remain distinguishable from exact provenance.
```

## Resolution

```text
INV-RES-001
Ambiguity is preserved.

INV-RES-002
Resolution does not select candidates by corpus order or convenience.

INV-RES-003
Nominal binding does not infer runtime values.
```

## Coverage

```text
INV-COV-001
Unsupported/incomplete construct cannot be reported as semantically empty.

INV-COV-002
Missing input blocks completeness where relevant.
```

## Performance

```text
INV-PERF-001
Optimization must preserve the semantic problem being solved.

INV-PERF-002
Nominal lookup should be indexed by relevant key rather than scan all declarations.
```

## Determinism

```text
INV-DET-001
Same semantic input/policy produces deterministically ordered semantic output.
```

---

# 17. Fases de implementação

As fases abaixo descrevem dependências e entregáveis.

Elas não prescrevem política de autonomia, commits, ritmo ou interação com o agente.

---

# Fase 0 — Baseline e inventário de conhecimento

## Objetivo

Construir uma visão completa do material que será migrado antes de alterar a organização.

## Inventário mínimo

Enumerar:

```text
AGENTS/policies legadas
README
docs/**
specs/**
*tasklist*
*plan*
*backlog*
*report*
ADRs
scripts
test classes
fixtures
grammar manifests
coverage manifests
relevant architecture-bearing source files
```

## Produzir

Uma área temporária:

```text
docs/_migration/
```

com pelo menos:

```text
source-inventory.md
knowledge-migration-matrix.md
open-conflicts.md
```

Essa área é transitória e não faz parte do knowledge route normal.

---

## `source-inventory.md`

Para cada fonte:

```text
path
document kind
status inferred: active/completed/historical/unknown
approximate responsibilities
contains durable knowledge? yes/no
contains open work? yes/no/unknown
```

---

## `knowledge-migration-matrix.md`

Granularidade por unidade de conhecimento, não necessariamente por linha.

Campos:

```text
Source
Section
Knowledge ID temporary
Classification
Summary
Current validity
Destination
Canonical ID
Migration status
Notes
```

Statuses:

```text
UNMAPPED
MIGRATED
MERGED
ARCHIVED
SUPERSEDED
OBSOLETE_WITH_REASON
TRANSIENT_NO_MIGRATION
UNCERTAIN
```

---

## `open-conflicts.md`

Registrar divergências encontradas.

Exemplo:

```text
Tasklist A says X.
Current code/test/domain behavior says Y.
Later report suggests Y superseded X.
```

Não resolver silenciosamente.

---

## Critério de saída

- todas as fontes legadas conhecidas inventariadas;
- nenhum arquivo candidato à remoção fora do inventário;
- conflitos visíveis;
- nenhuma exclusão ainda.

---

# Fase 1 — Definir a taxonomia e autoridade documental

## Objetivo

Criar somente a estrutura mínima necessária para receber conhecimento já existente.

## Criar

Conforme houver conteúdo real:

```text
docs/index.md
docs/architecture/index.md
docs/architecture/invariants.md
docs/architecture/decisions/index.md
docs/engineering/index.md
docs/domain/index.md
docs/evals/index.md
docs/history/index.md
```

Não criar docs especializados vazios.

---

## Definir em `docs/index.md`

- modelo de autoridade;
- diferença entre architecture/domain/engineering/work/history;
- regra de non-duplication;
- regra de historical docs not normative;
- como achar active work.

---

## Critério de saída

Um agente consegue descobrir qual categoria documental deve responder a uma pergunta sem precisar ler toda a árvore.

---

# Fase 2 — Decompor o AGENTS legado

## Objetivo

Transformar o documento legado de mais de mil linhas em conhecimento canônico modular.

## Migração por tema

### Para `semantic-analysis-policy.md`

Migrar:

- tests are evidence;
- specification before implementation;
- assumption audit;
- exact algorithms over heuristics;
- heuristic policy;
- fail closed;
- corpus is evidence;
- no fixture-driven production logic;
- soundness/completeness;
- correctness argument;
- minimal fix versus narrow fix;
- uncertainty;
- ambiguity;
- diagnostics;
- architecture before convenience;
- final semantic self-review.

---

### Para `semantic-testing.md`

Migrar:

- tests derived from rule;
- equivalence classes;
- adversarial tests;
- property testing;
- metamorphic testing;
- reference oracle;
- differential testing;
- mutation testing;
- independent challenge pass;
- test strength.

---

### Para `performance-policy.md`

Migrar:

- performance must not alter semantics;
- complexity review;
- graph/dataflow algorithm concerns;
- indexed lookups;
- suspicious complexity classes;
- avoid path enumeration when standard dataflow exists.

---

### Para architecture/domain docs

Migrar somente onde for conhecimento atual:

- analysis phase boundaries;
- structural scope versus visibility;
- provenance;
- reference resolution semantics;
- future CFG boundary;
- dependency possibility preservation.

---

## Deduplicação

Quando a mesma regra aparece em várias seções:

- escolher documento canônico;
- manter um único texto normativo;
- usar cross-link nos outros documentos.

---

## Critério de saída

Cada seção materialmente relevante do documento-fonte importado `specs/AGENTS.md — antlr-parse-tree-explorer.md` aparece na migration matrix como:

```text
MIGRATED
MERGED
SUPERSEDED
OBSOLETE_WITH_REASON
TRANSIENT_NO_MIGRATION
```

Nenhum `UNMAPPED`/`UNCERTAIN`.

O arquivo legado ainda pode existir até a fase final de deletion.

---

# Fase 3 — Architecture archaeology e ADRs retrospectivos

## Objetivo

Registrar decisões já tomadas e invariantes atuais antes de criar o novo roteador.

## Atividades conceituais

Para cada candidato:

1. verificar código;
2. verificar testes;
3. verificar specs;
4. verificar relatórios;
5. verificar histórico quando necessário;
6. determinar se é:
   - decisão;
   - invariante;
   - regra da linguagem;
   - detalhe acidental.

---

## Produzir

ADRs retrospectivos somente quando sustentados.

Atualizar:

```text
docs/architecture/decisions/index.md
docs/architecture/invariants.md
ARCHITECTURE.md
```

---

## Regras para ADR retrospectivo

Obrigatório:

```text
Type: Retrospective
Decision predates this ADR.
```

quando aplicável.

Não inventar narrativa histórica.

---

## Critério de saída

As principais fronteiras que um futuro agente poderia “simplificar” possuem:

- decisão ou invariante explícito;
- evidência de estado atual;
- link a domain doc quando aplicável.

---

# Fase 4 — Consolidar docs de domínio

## Objetivo

Retirar contratos semânticos duráveis de README, tasklists e reports.

## Domínios iniciais prováveis

Criar apenas os que têm material real.

Possíveis:

```text
source-format-and-normalization.md
preprocessing.md
provenance.md
semantic-ast.md
compilation-units.md
symbol-model.md
reference-resolution.md
```

---

## Para cada doc

Consolidar:

- estado atual;
- supported semantics;
- unsupported boundaries;
- algorithm/representation;
- uncertainty;
- diagnostics;
- performance contract;
- related ADRs/invariants;
- evals.

---

## Importante

Não copiar tasklists inteiras.

Transformar:

```text
narrativa de implementação passada
```

em:

```text
contrato atual compacto.
```

---

## Critério de saída

Para cada subsistema implementado, há um documento canônico capaz de responder “como funciona hoje?” sem precisar abrir a tasklist que o construiu.

---

# Fase 5 — Migrar tasklists, planos e backlog

## Objetivo

Consumir todo conhecimento durável contido nos documentos de trabalho legados.

## Classificação de cada item

### Decisão

→ ADR.

### Invariante

→ invariants.

### Regra COBOL / semântica de domínio

→ domain doc.

### Estratégia geral de testing

→ engineering.

### Eval ou counterexample durável

→ teste/fixture + eval catalog.

### Performance expectation

→ performance policy ou domain doc.

### Observability contract

→ observability policy/domain doc.

### Procedimento ainda necessário

→ current harness/workflow.

### Trabalho futuro ainda válido

→ backlog/work item.

### Resultado histórico útil

→ history/evidence.

### Passo de implementação já concluído sem valor futuro

→ `TRANSIENT_NO_MIGRATION`.

### Regra antiga substituída

→ `SUPERSEDED` com destino atual.

---

## Tratamento de checkboxes incompletos

Nenhum checkbox aberto pode desaparecer sem classificação.

Possibilidades:

```text
still required → backlog
already implemented elsewhere → completed evidence
no longer required → obsolete with rationale
superseded by different design → superseded
unclear → UNCERTAIN, blocking deletion
```

---

## Critério de saída

Cada tasklist/plan legado possui zero conhecimento sem destino.

Ainda não é obrigatório apagar os arquivos nesta fase; o objetivo é deixá-los deletáveis com segurança.

---

# Fase 6 — Elevar os testes existentes a um catálogo de evals

## Objetivo

Permitir ao agente descobrir o oracle relevante sem ler toda a suíte.

## Inventariar

Especialmente:

- AST tests;
- source normalization;
- provenance;
- compilation units;
- symbol table;
- resolution;
- call semantics;
- coverage;
- determinism;
- logging;
- end-to-end scenarios.

---

## Produzir

```text
docs/evals/semantic-eval-catalog.md
```

e, somente se for útil para tooling:

```text
harness/evals/catalog.yaml
```

---

## Não duplicar asserts

O catálogo deve apontar para os testes executáveis.

---

## Relacionar

```text
EVAL ↔ domain rule
EVAL ↔ invariant
EVAL ↔ fixture
EVAL ↔ gate tier
```

---

## Identificar lacunas

A migração pode revelar:

```text
documented invariant with no eval
critical rule tested only by corpus
important ambiguity without adversarial case
performance contract with no scale fixture
```

Essas lacunas tornam-se backlog do harness/semantic testing.

---

## Critério de saída

As capacidades semânticas críticas atuais possuem oráculos localizáveis por ID ou catálogo.

---

# Fase 7 — Unificar regression gates

## Objetivo

Transformar scripts específicos e conhecimento operacional espalhado em poucos entrypoints estáveis.

## Preservar inicialmente

Scripts atuais continuam funcionando.

O novo harness os encapsula antes de tentar simplificá-los.

---

## Criar entrypoints

Conforme aplicável:

```text
scripts/harness/check-fast.sh
scripts/harness/check-semantic.sh
scripts/harness/check-full.sh
scripts/harness/check-performance.sh
scripts/harness/check-docs.sh
scripts/harness/check-architecture.sh
```

---

## `check-full` deve reaproveitar o que já funciona

Por exemplo, regressões atuais de source-normalizer não devem ser reimplementadas apenas para aderir a uma nova pasta.

Primeiro:

```text
wrapper
```

depois, somente se houver ganho real:

```text
refactor.
```

---

## Critério de saída

Um work item pode indicar poucos nomes de gates sem precisar descrever dezenas de comandos internos.

---

# Fase 8 — Introduzir enforcement arquitetural

## Objetivo

Converter os invariantes mais importantes em checks automáticos quando possível.

## Processo de seleção

Para cada invariant:

```text
Can this be checked cheaply and deterministically?
```

Se sim:

```text
AUTOMATED.
```

Se parcialmente:

```text
PARTIALLY_AUTOMATED.
```

Se depender de semântica/julgamento:

```text
REVIEW.
```

---

## Evitar enforcement frágil

Não codificar regras como:

```text
"class name must remain X"
```

se a identidade arquitetural é:

```text
"AST cannot depend on resolution".
```

O check deve proteger o conceito, não a implementação acidental.

---

## Critério de saída

Pelo menos as fronteiras arquiteturais mais críticas deixam de depender exclusivamente de leitura de documentação.

---

# Fase 9 — Criar o protocolo de work items

## Objetivo

Substituir mega-tasklists futuras por unidades de trabalho pequenas e roteáveis.

## Criar

```text
docs/engineering/work-item-protocol.md
```

e templates somente se houver benefício real.

---

## Estrutura do work item

### `work-item.yaml`

Roteamento.

### `spec.md`

Semântica/resultado desejado.

### `plan.md`

Slices/dependências.

### `eval.md`

Oracle e counterexamples.

### `state.md`

Memória curta.

---

## Spec deve responder

```text
Problem
Goal
Supported input domain
Semantic classes
Assumptions
Expected behavior
Uncertainty behavior
Out of scope
Related domain rules
Related ADRs/invariants
```

---

## Plan deve responder

```text
Slices
Dependencies
Likely architectural surface
Required migrations
Expected artifacts
```

Não repetir toda a spec.

---

## Eval deve responder

```text
What proves correctness?
Positive classes
Negative classes
Ambiguous classes
Adversarial cases
Regression cases
Properties/metamorphic relations
Scale expectations
```

---

## State deve responder

```text
Where are we?
What is known green?
What remains?
What was discovered that affects the plan?
```

---

## Critério de saída

É possível descrever um slice semântico pequeno sem criar uma tasklist de dezenas de páginas.

---

# Fase 10 — Criar o novo `AGENTS.md`

## Objetivo

Instalar o roteador somente depois que seus destinos existirem.

## Conteúdo recomendado

### Scope

Uma frase sobre o diretório.

### Purpose

Uma frase sobre o analisador.

### Pipeline

Poucas linhas.

### Universal rules

Somente regras como:

```text
Corpus/tests are evidence, not specification.
Prefer semantics-driven algorithms over heuristics.
Fail closed on semantic uncertainty.
Preserve analysis boundaries.
Do not weaken fixtures/grammar just to make tests pass.
For non-trivial semantic work, identify the governing rule.
Use the declared harness gates.
```

### Context routing

Tabela por domínio.

### Active work routing

Onde localizar `work-item.yaml`.

### Historical context

Não carregar history/completed por default.

### Verification

Lista dos gates.

---

## O novo AGENTS não deve conter

- tutorial;
- exemplos extensos;
- full Definition of Done;
- lista enorme de edge cases;
- toda a política de testing;
- todas as regras COBOL;
- detalhes de um subsistema específico;
- histórico.

---

## Critério de saída

Um agente consegue sair do `AGENTS.md` para o conjunto correto de documentos sem fazer leitura indiscriminada.

---

# Fase 11 — Migrar README e remover duplicação

## Objetivo

Tornar README útil para pessoas sem torná-lo uma segunda spec canônica.

## Manter no README

- propósito;
- build/run;
- principais outputs;
- navegação visual;
- exemplos;
- status de alto nível;
- links.

## Mover para docs canônicos

- contratos semânticos extensos;
- detalhes de resolution;
- provenance detalhada;
- invariantes;
- reasoning arquitetural.

---

## Critério de saída

Atualizar um contrato semântico não exige editar README + AGENTS + tasklist + domain doc.

---

# Fase 12 — Verificar cobertura da migração e remover legado

## Objetivo

Eliminar as fontes que deixaram de ter função.

## Gate de remoção do documento-fonte importado `specs/AGENTS.md — antlr-parse-tree-explorer.md`

Requer:

```text
all durable sections mapped
no UNCERTAIN
all new links valid
new AGENTS exists
routing destinations exist
semantic policy exists
testing policy exists where required
architecture/domain knowledge migrated
```

Então o arquivo legado pode ser removido.

---

## Gate de remoção de cada tasklist

Requer:

```text
all durable knowledge migrated
all incomplete work migrated or intentionally retired
all current references replaced
all useful evals preserved
all historical evidence handled
no canonical link points to tasklist
```

Então a tasklist pode ser removida.

---

## Planos concluídos

Aplicar a mesma regra.

Um `*-plan.md` concluído que hoje serve como fonte normativa deve ser migrado.

Se contém somente narrativa histórica após a migração, pode ser removido ou arquivado conforme valor forense.

---

## Reports

Por padrão:

```text
archive outside normal routing
```

em vez de deletar imediatamente.

Eles têm custo de tokens apenas quando carregados.

---

## Migration matrix

Após a remoção:

- preservar uma versão final em `docs/history/harness-v1-migration/`;
- não incluir no default context routing.

Ela é a prova de que a limpeza não apagou conhecimento por acidente.

---

# Fase 13 — Validar o próprio harness

## Objetivo

Verificar se o sistema de contexto realmente reduz custo e ambiguidade.

## Criar cenários de roteamento conceituais

Exemplos:

### Cenário A — logging

Pergunta:

```text
add TRACE around resolver decisions
```

Esperado:

```text
AGENTS
work item
observability doc
resolver source
logging tests
```

Não esperado:

```text
entire semantic-testing doc
all historical reports
all source-normalizer history
```

### Cenário B — reference resolution

Esperado:

```text
AGENTS
work item
reference-resolution domain doc
semantic analysis policy
relevant invariants
relevant ADR
evals
code/tests
```

### Cenário C — frontend HTML

Esperado:

```text
AGENTS
work item
frontend/rendering context
snapshot contract if needed
```

Não carregar CFG/dataflow policy.

---

## Medidas qualitativas

- número de docs lidos antes de editar;
- duplicação de regras;
- quantidade de contexto histórico por tarefa;
- facilidade de localizar oracle;
- quantidade de decisões redescobertas;
- regressões arquiteturais detectadas automaticamente.

---

## Métricas quantitativas opcionais

Não transformar em objetivo absoluto.

Possíveis:

```text
AGENTS bytes/lines
average must_read count
active state size
number of invariants with automated enforcement
number of semantic capabilities with eval IDs
fontes temporárias/normativas antigas restantes
broken internal references
duplicate canonical IDs
```

---

# Fase 14 — Governança contínua

## Objetivo

Impedir que o harness volte a se transformar em um novo monólito documental.

## Regra de fechamento de work item

Ao encerrar uma mudança:

```text
new durable semantic rule?
    → domain doc

new architectural decision?
    → ADR

new invariant?
    → invariants

new reusable testing lesson?
    → semantic-testing

new durable eval?
    → tests/catalog

new future work?
    → backlog

implementation diary?
    → do not promote to canonical docs
```

---

## Regra de crescimento do AGENTS

Antes de adicionar uma seção:

> esta regra é universal para praticamente qualquer tarefa neste diretório?

Se não:

```text
route to a specialized document.
```

---

## Regra de crescimento dos domain docs

Quando um doc ficar grande:

dividir por responsabilidade semântica, não por tamanho arbitrário.

---

## Regra de ADR

Não usar ADR para:

- todo bug;
- toda classe;
- toda refatoração;
- pura regra COBOL;
- todo detalhe de implementação.

---

## Regra de histórico

Histórico é acessível, mas não autoritativo.

---

# 18. Estratégia para futuras fases como CFG

O Harness v1 deve existir antes da implementação ampla de CFG porque CFG será um domínio de alto risco.

Quando CFG começar, o processo esperado de conhecimento é:

```text
work item inicial
    ↓
pesquisa/semântica COBOL
    ↓
spec
    ↓
adversarial review
    ↓
decisões arquiteturais necessárias
    ↓
domain/cfg.md nasce com conhecimento real
    ↓
evals por construção
    ↓
slices pequenos
```

Não criar agora uma enciclopédia de CFG especulativa.

Quando o domínio nascer, possíveis slices poderão ser:

```text
CFG-IR
linear flow
basic-block formation
IF
EVALUATE
GO TO
GO TO DEPENDING ON
PERFORM
PERFORM THRU
NEXT SENTENCE
termination
fallthrough
```

Cada slice deve possuir oracle próprio.

---

# 19. Relação entre planning agent, implementation agent e evaluator

O harness deve suportar separação de papéis, principalmente em work items HIGH.

A arquitetura documental não precisa depender de uma tecnologia específica de multi-agent.

Conceitualmente:

```text
Problem
   ↓
Spec / planning
   ↓
Semantic challenge
   ↓
Implementation
   ↓
Independent evaluation
   ↓
Executable gates
```

A separação importante é cognitiva:

- quem propõe a solução não deve ser o único mecanismo que decide se ela está correta;
- o oracle não deve ser produzido apenas depois de ver a implementação;
- evals adversariais devem tentar falsificar shortcuts;
- o harness executável é a última autoridade operacional.

---

# 20. Modelo de review adversarial para tarefas HIGH

O conhecimento correspondente vem em grande parte do documento-fonte importado `specs/AGENTS.md — antlr-parse-tree-explorer.md` e deve residir em `semantic-testing.md`.

Perguntas mínimas:

```text
What valid COBOL input most likely breaks this?

Is any rule inferred from current corpus?

Does the solution confuse structural scope with visibility?

Does ambiguity disappear?

Does UNKNOWN become absence?

Does an earlier analysis phase claim knowledge owned by a later phase?

Is text/regex being used where grammar/AST already carries structure?

Is there an accidental O(N*M) lookup?

Is order of declarations/corpus influencing the result without language rule?

Does the fixture prove the semantic class or only the reported instance?
```

Essas perguntas não precisam morar no `AGENTS.md`.

O work item HIGH roteia para esse documento.

---

# 21. Política de source-of-truth para COBOL

Para evitar futuros conflitos:

## Regra COBOL

Doc canônico deve apontar para fonte de dialeto quando possível.

## Representação interna

ADR/domain doc explica a escolha do projeto.

Exemplo:

```text
COBOL rule:
OF/IN qualification semantics

→ domain/reference-resolution.md


Project decision:
represent written name + canonical form separately

→ ADR / domain model
```

Essa separação evita tratar convenções internas como linguagem.

---

# 22. Política de supported/unsupported boundaries

Cada domain doc deve dizer explicitamente:

```text
Supported
Unsupported
Preserved but uninterpreted
Input missing behavior
Ambiguity behavior
```

Isso reduz pressão sobre agentes para “completar” algo usando heurística.

A fronteira pequena e exata continua preferível a uma fronteira grande e heurística.

---

# 23. Política para otimização de tokens

Além da organização documental, o harness deve incentivar:

## 23.1. Pointers, não repetição

Work item referencia:

```text
INV-RES-001
ADR-0004
EVAL-RES-014
```

em vez de colar todo o texto.

---

## 23.2. Anchors

`must_read` pode apontar para seção específica.

---

## 23.3. Generated summaries somente quando deriváveis

Se um inventário pode ser produzido do código, ele não deve ser mantido manualmente em três lugares.

Exemplo:

- grammar coverage;
- test inventory;
- eval list.

Se gerado, marcar explicitamente como generated.

---

## 23.4. Historical reports fora do route

Relatório de 100+ KB pode permanecer no Git sem nunca entrar numa sessão normal.

---

## 23.5. State curto

Não acumular “diário” de dezenas de sessões.

Consolidar.

---

## 23.6. Active work único e explícito

Evitar que o agente procure entre quinze tasklists para descobrir qual é vigente.

---

# 24. Política para completude da migração das tasklists

A afirmação:

> “todo conhecimento da tasklist foi migrado”

precisa significar algo verificável.

Para cada tasklist:

## Cobertura semântica

Toda regra de domínio atual tem destino.

## Cobertura arquitetural

Toda decisão/invariante vigente tem destino.

## Cobertura de testing

Todo padrão de oracle reutilizável tem destino.

## Cobertura de backlog

Toda tarefa ainda válida tem destino.

## Cobertura histórica

Toda evidência que ainda pode ajudar investigação foi arquivada ou existe em Git de forma suficiente.

## Cobertura de references

Nenhum documento canônico aponta para o arquivo a ser removido.

---

# 25. Tratamento de informação obsoleta

Não migrar lixo apenas para alcançar “100%”.

Uma unidade pode ser:

```text
OBSOLETE_WITH_REASON
```

Exemplo:

```text
task antiga diz que builder usa apenas primeiro programUnit,
mas o hardening posterior já corrigiu isso.
```

O conhecimento útil que sobrevive pode ser:

```text
nested program support is a current invariant
```

A limitação histórica não precisa virar regra atual.

A migration matrix registra que foi superseded.

---

# 26. Tratamento de informações contraditórias

Priorizar análise, não mescla automática.

Quando houver divergência:

```text
source antiga
source nova
current code
current tests
accepted ADR
```

criar uma entrada de conflito.

Resolver somente com evidência.

Se continuar incerto:

```text
UNCERTAIN
```

e bloquear exclusão do documento fonte.

---

# 27. Não-goals do Harness Engineering v1

O harness não deve virar desculpa para:

- reescrever o analisador;
- introduzir CFG antecipadamente;
- trocar ANTLR;
- reorganizar todos os packages Java sem necessidade;
- alterar semântica de resolution;
- “limpar” testes que parecem feios;
- atualizar baselines sem justificativa;
- inventar ADRs;
- criar dezenas de docs vazios;
- criar framework genérico de agentes;
- criar orquestrador multi-agent complexo;
- introduzir infraestrutura distribuída;
- migrar todo conhecimento histórico para prosa nova sem filtragem;
- substituir testes executáveis por documentação.

A mudança principal é:

```text
knowledge architecture
+
context routing
+
verification architecture
```

---

# 28. Definition of Done do Harness Engineering v1

O Harness v1 pode ser considerado implantado quando:

## Knowledge architecture

- [ ] existe um `docs/index.md` claro;
- [ ] arquitetura/domain/engineering/history possuem papéis explícitos;
- [ ] regras normativas têm fonte canônica;
- [ ] duplicação relevante foi removida.

## Imported engineering document migration

- [ ] todas as seções duráveis foram classificadas;
- [ ] semantic policy existe;
- [ ] semantic testing policy existe;
- [ ] performance policy existe quando aplicável;
- [ ] invariantes/domain rules foram migrados;
- [ ] nenhum item UNCERTAIN permanece;
- [ ] documento-fonte importado `specs/AGENTS.md — antlr-parse-tree-explorer.md` foi removido.

## Architecture

- [ ] `ARCHITECTURE.md` oferece mapa curto;
- [ ] invariantes relevantes possuem IDs;
- [ ] ADR index existe;
- [ ] decisões retrospectivas relevantes foram registradas sem narrativa inventada.

## Domain

- [ ] subsistemas atuais possuem docs canônicos suficientes;
- [ ] supported/unsupported boundaries são visíveis;
- [ ] uncertainty permanece explícita;
- [ ] semântica não depende de tasklists históricas.

## Tasklist migration

- [ ] todas as tasklists/planos legados foram inventariados;
- [ ] conhecimento durável foi migrado;
- [ ] work futuro foi levado ao backlog/work items;
- [ ] reports foram tratados como evidência;
- [ ] tasklists concluídas foram removidas;
- [ ] nenhum link canônico aponta para elas.

## Evals

- [ ] capacidades semânticas críticas são localizáveis;
- [ ] fixtures possuem função clara;
- [ ] regressions e adversarial cases estão distinguíveis;
- [ ] evals são roteáveis por work item.

## Gates

- [ ] existe fast gate;
- [ ] existe semantic gate;
- [ ] existe full gate;
- [ ] architecture/docs checks existem conforme viabilidade;
- [ ] performance permanece separado;
- [ ] gates existentes foram reutilizados em vez de descartados.

## Context routing

- [ ] novo `AGENTS.md` é curto;
- [ ] roteia por domínio;
- [ ] roteia para active work;
- [ ] não manda ler histórico por padrão;
- [ ] não replica os docs especializados.

## Work items

- [ ] protocolo spec/plan/eval/state definido;
- [ ] work-item possui `must_read`;
- [ ] work-item referencia invariants/ADRs/evals;
- [ ] completed work não vira nova tasklist permanente.

## Harness self-check

- [ ] docs links válidos;
- [ ] IDs únicos;
- [ ] referências a documentos-fonte removidos removidas;
- [ ] migration matrix final preservada fora do default context;
- [ ] cenários de roteamento demonstram progressive disclosure.

---

# 29. Estado final desejado

Um agente recebendo uma tarefa como:

```text
corrigir resolução de DATA qualificado por FILE
```

não deveria precisar fazer:

```text
ler 1200 linhas de AGENTS
ler 35k de tasklist
ler 138k de report
ler README inteiro
grep no repo inteiro
```

O caminho esperado seria aproximadamente:

```text
AGENTS.md
    ↓
active work-item.yaml
    ↓
docs/domain/reference-resolution.md#...
    ↓
INV-RES-...
    ↓
ADR relevante
    ↓
EVAL-RES-...
    ↓
resolver + testes + fixture
    ↓
check-semantic / check-full
```

Para uma tarefa de logging:

```text
AGENTS.md
    ↓
work item
    ↓
observability policy
    ↓
classe/teste relacionado
    ↓
fast/full gate
```

Sem carregar resolução, CFG, property testing ou historical reports.

---

# 30. Princípio de encerramento

O Harness Engineering v1 deve converter conhecimento disperso em três coisas:

```text
1. conhecimento canônico e roteável;
2. invariantes/decisões explicitamente identificáveis;
3. evidência executável de corretude.
```

A meta final não é minimizar documentação.

É minimizar **redescoberta**.

Não é minimizar tokens a qualquer custo.

É fazer cada token carregado responder a uma necessidade concreta da tarefa.

Não é tornar o agente autônomo eliminando engenharia humana.

É fazer a engenharia já realizada sobreviver de forma que o agente seguinte:

- saiba quais decisões não estão abertas;
- encontre rapidamente a semântica relevante;
- não confunda corpus com especificação;
- não introduza heurística onde existe algoritmo estabelecido;
- não atravesse fronteiras do pipeline por conveniência;
- consiga localizar um oracle;
- consiga provar o resultado por gates;
- preserve explicitamente o que ainda é unknown/unsupported/incomplete.

Nesse estado, `AGENTS.md` deixa de ser o lugar onde o projeto tenta ensinar tudo.

Ele passa a ser o mapa de um repositório que sabe ensinar a si próprio.
