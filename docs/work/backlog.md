# Backlog atual

Este arquivo registra trabalho futuro válido que não pertence a um work item ativo. Ordem não implica autorização para iniciar; cada item deve receber spec, eval e boundary explícitos antes de implementação.

## Arquitetura

### BACKLOG-ARCH-001 — Fronteiras por pacotes e Clean Architecture

Refatorar o package único para componentes com dependências direcionais verificáveis (frontend, AST, symbols, occurrences, resolution, presentation). Preservar APIs/produtos e usar os invariantes atuais como oracle. Depois da migração, substituir o check de bytecode por regras primariamente baseadas em package boundaries quando isso proteger o conceito sem acoplar nomes acidentais.

## Extensibilidade de plataforma

Os itens abaixo estão ordenados por dependência arquitetural, não por autorização. `WORK-EXT-001` é o único slice ativo e cobre somente `DFHRESP`/`DFHVALUE`; nenhum item desta seção foi iniciado.

```text
BACKLOG-EXT-001 infraestrutura de composição
  ├── BACKLOG-EXT-002 contexto confiável de compilação
  ├── BACKLOG-EXT-003 external symbols
  ├── BACKLOG-EXT-004 semantic extractors
  │     └── BACKLOG-EXT-005 protocolo GRBE
  └── BACKLOG-EXT-006 control-flow semantics ── depende de BACKLOG-CFG-001

BACKLOG-CFG-001
  ↓
BACKLOG-DF-001 statement effects + reaching definitions
  ↓
BACKLOG-DF-003 constant/possible-values
  ├── BACKLOG-DF-002 targets de CALL dinâmico
  ├── BACKLOG-EXT-004 extractors com operands dinâmicos
  └── BACKLOG-EXT-005 GRBE
```

### BACKLOG-EXT-001 — Infraestrutura mínima de extensibilidade do pipeline

#### Motivação

Capacidades de plataforma podem contribuir antes da resolução, depois do binding COBOL, durante CFG ou depois de dataflow. Hoje `ExplorerMain` compõe diretamente symbol tables, occurrences, `CobolReferenceResolver`, `ResolutionAnalysisReport` e snapshots. Repetir `if (cics)`, `new Ims...` ou branches equivalentes nessa classe e nas fases canônicas transformaria cada tecnologia em mudança transversal.

O classifier mínimo de `WORK-EXT-001` é um primeiro caso focalizado, não a autorização nem o desenho completo dessa infraestrutura.

#### Resultado esperado

Criar o menor mecanismo explícito de composição/injeção capaz de oferecer extension points independentes para unresolved reference classifiers, external symbol providers, semantic extractors e control-flow semantics providers.

Uma tecnologia implementa apenas as capacidades pertinentes. Adicionar implementação futura deve exigir predominantemente o novo componente mais registro/composição/configuração, sem reestruturar repetidamente `ExplorerMain`, o resolver, o futuro CFG builder ou outras fases canônicas.

#### Princípios e incisão controlada

- Aplicar Clean Architecture: contratos estáveis ficam do lado do core/composição; módulos concretos dependem desses contratos, nunca o inverso.
- Aplicar Open/Closed Principle: fases canônicas permanecem fechadas a branches por tecnologia e abertas a contribuições pelos extension points.
- Aplicar Dependency Inversion somente onde houver dependência real demonstrada; interfaces devem ser pequenas, coesas e orientadas à capacidade.
- Localizar precisamente uma incisão pequena no composition root atual e os pontos semânticos em que cada capability recebe/produz artefatos.
- Manter execução sem extensões como configuração válida e semanticamente equivalente ao pipeline atual.
- Compor explicitamente ordem, identidade, determinismo, falha fechada, provenance, diagnostics e conflitos entre múltiplas contribuições.
- Classificação externa continua produto ortogonal conforme ADR-0011; AST e resolver COBOL não passam a conhecer o mecanismo concreto de plugins.

O work item futuro deve comparar composição explícita, registry pequeno, injection por constructor/factory e mecanismos equivalentes. Não está decidido usar `PluginRegistry`, `ServiceLoader`, reflection, magic discovery ou framework de DI.

#### Capacidades, fronteiras e rejeições

O desenho precisa admitir momentos distintos da pipeline sem criar uma interface `PlatformPlugin` gigante com métodos opcionais. Não criar `CicsPlugin` monolítico, service locator global, API antecipada de CFG/dataflow, framework externo de DI ou packages definitivos sem evidência. Providers/extractors não mutam produtos canônicos já produzidos. Um External Symbol Provider emite contribuições externas explícitas, compostas em uma visão/inventário consumido pela resolução sem alterar silenciosamente a symbol table COBOL original. Novos produtos usam identidades compostas, provenance e certeza explícita.

Uma configuração incompatível, contribuição duplicada ou dependência ausente deve falhar fechada ou produzir incompletude tipada conforme o contrato da capability. Ordem de registro não pode selecionar silenciosamente um fato sem regra de precedência.

#### Dependências, fora de escopo e promoção

- Pré-condições: encerrar `WORK-EXT-001` e inspecionar o ponto único de composição que ele deixou; coordenar com BACKLOG-ARCH-001 sem exigir sua execução se boundaries equivalentes puderem ser protegidas no package atual.
- Fora de escopo: implementar providers/extractors concretos, CFG, dataflow, catálogos extensos ou discovery dinâmico.
- Risco principal: overengineering antes do segundo caso concreto; a abstração deve ser a menor que suporte as quatro famílias previstas sem exigir implementação de todas por cada módulo.
- Implementação ingênua rejeitada: condicionais/classes CICS/IMS/DB2/GRBE espalhados pelo `main` ou uma interface monolítica que apenas desloca esses condicionais.
- Promover quando ao menos uma segunda capability concreta exigir composição além do classifier mínimo. O work item deve mapear incisão, dependency direction, execução vazia, conflito/determinismo e testes arquiteturais antes de produção.

Relações: ADR-0003, ADR-0011, INV-EXT-001 a INV-EXT-004, EVAL-ARCH-001 e EVAL-EXT-001.

### BACKLOG-EXT-002 — Contexto explícito de tradução e compilação

#### Problema e resultado esperado

O source isolado não prova `CICS`/`NOCICS` nem outras opções de plataforma. Evidência como `EXEC CICS` pode provar necessidade local, mas sua ausência não prova modo negativo; usar o próprio `DFHRESP(...)` para inferir modo seria circular.

Definir uma entrada/policy versionada para metadata confiável oriunda de JCL/procedure, build, compiler options ou configuração fornecida pelo usuário. O resultado deve distinguir modo explícito, evidência preservada no source, inferência e desconhecido, e aplicar a precedência da ADR-0011 sem transformar ausência em `NOCICS`.

#### Fronteiras e dependências

- Depende de BACKLOG-EXT-001 para composição e provavelmente de BACKLOG-RUNNER-001 para ingestão por codebase; não autoriza parser de JCL completo.
- Pode futuramente habilitar lowering/classificação mais forte quando o modo CICS for comprovado, mas não reescreve AST retroativamente nem muda grammar por default.
- Provenance da metadata, autoridade, escopo por artifact/unit e conflitos entre fontes devem permanecer observáveis.
- Não inferir tecnologia por prefixos, frequência de corpus ou ausência de statements.

Promover somente com uma fonte concreta de metadata e casos contraditórios. O oracle deve cobrir `CICS`, `NOCICS`, desconhecido, fontes conflitantes e equivalência do pipeline sem metadata. Relações: ADR-0011, INV-EXT-002, INV-EXT-003, INV-COV-001 e BACKLOG-RUNNER-001.

### BACKLOG-EXT-003 — External Symbol Providers e origem de símbolos

#### Problema e resultado esperado

Permitir que ambientes contribuam símbolos reais ausentes do source/copybooks entregues, começando apenas por um caso CICS comprovado como `EIBCALEN` ou `EIBAID`. O resolver COBOL continua genérico e consome um inventário composto antes do lookup; novos providers entram por BACKLOG-EXT-001, sem branches por plataforma.

`bindingStatus` e `symbolOrigin` são dimensões ortogonais: um símbolo provido pode terminar `RESOLVED` com origem `CICS_EXTERNAL`. Uma declaração real expandida de copybook, como constantes de `DFHAID`/`DFHBMSCA`, tem precedência e não pode receber duplicata sintética. Ausência ou falha de `COPY` permanece input/provenance própria, não justificativa automática para inventar catálogo.

#### Fronteiras, dependências e promoção

- Depende de BACKLOG-EXT-001; integração com repositórios/copybooks pode depender de BACKLOG-RUNNER-001.
- Precisa fechar namespace, kind, scope/visibility, owner unit, identity, atributos, provenance da definição externa, conflito entre providers e precedência com declarações reais.
- Não criar catálogo amplo antecipadamente, não usar `EXTERNAL` como status de binding e não fazer o resolver chamar classes CICS.
- O provider não interpreta statements, não classifica unresolved shapes e não deduz valores de runtime.
- OCP/DIP exigem que um novo provider seja implementação mais composição, mantendo o core fechado a nomes concretos.

Promover com documentação oficial da plataforma e primeiro símbolo necessário. Oráculos mínimos: com provider resolve/origin explícita; sem provider fica unresolved; declaração real vence; colisão/contribuição duplicada falha de forma determinística; programa sem providers permanece inalterado. Relações: ADR-0003, ADR-0011, INV-EXT-001, INV-PROV-002, INV-RES-001 e BACKLOG-EXT-001.

### BACKLOG-EXT-004 — Semantic Extractors para CICS, DB2 e IMS

#### Problema e resultado esperado

Interpretar significado de plataforma depois que o frontend e o core fornecerem estrutura canônica suficiente. Slices concretos previstos incluem `EXEC CICS LINK/START/XCTL`, `EXEC SQL CALL` e tabelas SQL, além de comandos/chamadas IMS preservados. Cada extractor emite facts tipados de dependência/negócio com technology, kind, operand, certainty e provenance; não altera AST, binding ou CFG.

Extractors são capacidades independentes compostas por BACKLOG-EXT-001. Adicionar CICS, DB2 ou IMS deve exigir implementação mais registro/injeção, sem branches novos no pipeline principal. A interface não pode assumir que toda extração roda no mesmo ponto: facts literais podem depender apenas da AST/analisador embutido, enquanto operands variáveis exigem resolução e possible-values em um program point.

#### Dependências e fronteiras

- Depende de BACKLOG-EXT-001 e BACKLOG-EMB-001 para payloads `EXEC` com parser dedicado; regex no raw payload é rejeitada.
- Facts com operandos dinâmicos dependem explicitamente de BACKLOG-DF-003, que por sua vez consome CFG, statement effects e reaching definitions canônicos. O extractor consulta `possibleValues` no program point; não percorre texto para trás nem implementa mini-CFG, reaching definitions ou propagation própria.
- Fatos finais de dependência devem respeitar BACKLOG-DEPS-001: targets conhecidos e remainder dinâmico/incerto permanecem separados.
- `XCTL` pode emitir dependência aqui, mas sua ausência de fallthrough pertence a BACKLOG-EXT-006; extractor e control-flow provider não se fundem.
- Comando preservado, parser dedicado ausente ou operand dinâmico sem dataflow continua `UNKNOWN/INCOMPLETE`, nunca coleção vazia.

#### Promoção incremental

Promover por tecnologia e comando concreto, com fonte oficial e oracle independente. Começar por literal estático, depois host variable nominal, e somente depois possible-values. Testes devem cobrir comando conhecido/desconhecido, literal/variável, payload inválido, COPY provenance, múltiplos targets, remainder dinâmico, execução sem extractor e ordem determinística entre extractors. Rejeitar um extractor universal por regex ou um `CicsPlugin` que também constrói CFG.

Relações: ADR-0007, ADR-0008, ADR-0011, INV-EMB-001, INV-EXT-001, INV-COV-001, BACKLOG-EXT-001, BACKLOG-EMB-001, BACKLOG-DF-003 e BACKLOG-DEPS-001.

### BACKLOG-EXT-005 — Semantic Extractor organizacional GRBE

#### Problema e resultado esperado

Modelar o protocolo organizacional exemplificado por `CALL MONITOR USING PARM1` sem contaminar a semântica COBOL. O core continua responsável pelo `CALL`, binding do target/argumento, layout futuro, CFG e dataflow; o extractor GRBE reconhece somente o protocolo autorizado e transforma valores possíveis dos campos/bytes relevantes em facts de negócio/dependência auditáveis.

#### Dependências e fronteiras

- Depende de BACKLOG-EXT-001, do modelo de regiões/efeitos de BACKLOG-DF-001, de possible-values canônico em BACKLOG-DF-003 e de BACKLOG-DEPS-001; não deve ser promovido antes de esses contratos cobrirem o parâmetro necessário.
- Deve declarar versão do protocolo, layouts suportados, precondições, targets conhecidos, remainder desconhecido, provenance e confiança.
- Não fazer busca textual, backtracking pelo source, inferência por posição observada no corpus, parser COBOL paralelo nem reaching definitions próprio.
- OCP/DIP exigem implementação organizacional injetável, sem referência GRBE no resolver, AST, CFG builder ou `ExplorerMain`.

Promover com especificação organizacional autorizada, fixture mínima e um caso real anonimizado/reprodutível. Oráculos devem cobrir protocolo válido, target literal/variável, múltiplas reaching definitions, alias/partial write, layout incompatível e ausência da extensão. Relações: ADR-0004, ADR-0011, INV-RES-002, INV-EXT-001, BACKLOG-EXT-001, BACKLOG-DF-001, BACKLOG-DF-003 e BACKLOG-DEPS-001.

### BACKLOG-EXT-006 — Control-Flow Semantics Providers para statements externos

#### Problema e resultado esperado

Permitir que constructs externos informem ao CFG efeitos como `RETURNING_TRANSFER`, `NON_RETURNING_TRANSFER`, `PROGRAM_EXIT`, `CONDITIONAL_TRANSFER` e `UNKNOWN_EXTERNAL_EFFECT`. Casos iniciais candidatos: `EXEC CICS XCTL` sem fallthrough e `EXEC CICS RETURN` como saída do programa; IMS entra somente quando houver comando e fonte oficial concretos.

Essa capability afeta topologia/semântica do CFG e permanece distinta de Semantic Extractor. O futuro CFG builder ou uma fase explícita de enriquecimento consulta contratos genéricos compostos por BACKLOG-EXT-001; adicionar tecnologia deve ser implementação mais registro, não branch no builder ou no `main`.

#### Dependências, fronteiras e promoção

- Depende obrigatoriamente de BACKLOG-CFG-001, BACKLOG-EXT-001 e, para `EXEC`, BACKLOG-EMB-001. A interface final só pode ser fechada quando o modelo de nodes/edges/program points do CFG existir.
- Provider não emite facts de dependência como substituto do extractor, não executa dataflow e não transforma efeito desconhecido em fallthrough.
- Múltiplos providers, ausência de provider, conflito de effects e statements opacos precisam de regra determinística e conservadora.
- Não antecipar enum/API definitiva, reescrever CFG inteiro ou criar hook genérico de mutação de grafo.

Promover após o CFG estrutural estar verde e com primeiro statement externo cujo efeito mude um oracle de topologia. Testes mínimos: `XCTL`, `RETURN`, statement externo desconhecido, execução sem provider, conflito e inexistência de fallthrough indevido. Relações: ADR-0007, ADR-0008, ADR-0011, INV-EMB-001, INV-EXT-001, INV-COV-001, BACKLOG-EXT-001, BACKLOG-EMB-001 e BACKLOG-CFG-001.

## AST e cobertura preparatória para dataflow

### BACKLOG-AST-001 — Hardening da fronteira AST para CFG e dataflow

Este item fecha as garantias de preservação, coverage e rastreabilidade das fases existentes antes de `BACKLOG-CFG-001` e `BACKLOG-DF-001`. Ele não implementa CFG, layout de memória, efeitos de statements, reaching definitions, propagação de valores ou targets dinâmicos de `CALL`.

#### Resultado esperado

Ao concluir, um consumidor posterior deve poder assumir, para cada program unit reconhecido e dentro da superfície gramatical suportada, que:

1. cada statement e declaração DATA relevante possui representação AST única, determinística e alcançável;
2. toda construção semanticamente não interpretada que possa afetar dependências produz incompletude observável, mesmo quando não contém referência nominal;
3. cada referência DATA coletada pode ser ligada, sem busca textual, ao AST node, scope, resultado nominal e declaração candidata;
4. hierarquia de grupos, FILLERs e endpoints de `REDEFINES`/`RENAMES` permanecem disponíveis para um futuro modelo de regiões;
5. `CALL` literal e `CALL` por identifier/expression permanecem sintática e semanticamente separados;
6. todas as junções entre produtos usam identidade composta por program unit e ID local, com provenance e exatidão preservadas.

Essas garantias valem dentro de uma geração determinística da análise. IDs não se tornam chaves persistentes resistentes a edição do fonte.

#### Autoridade e restrições

- Regras canônicas: `docs/domain/semantic-ast.md`, `docs/domain/symbol-model.md`, `docs/domain/reference-resolution.md` e `docs/architecture/pipeline.md`.
- Invariantes aplicáveis: INV-AST-001, INV-AST-002, INV-SYM-001, INV-PROV-002, INV-COV-001, INV-COV-002, INV-RES-001, INV-DET-001 e INV-PERF-001.
- Evals a fortalecer: EVAL-AST-001 a EVAL-AST-004, EVAL-COV-001, EVAL-COV-002, EVAL-SYM-001, EVAL-SYM-002, EVAL-RES-REL-001, EVAL-RES-COV-001, EVAL-RES-REPORT-001 e EVAL-RES-DET-001.
- Não alterar gramática para acomodar o builder. Mudança de gramática exige defeito comprovado na gramática ou ampliação deliberada da superfície suportada.
- Não reparsear `writtenText`, declaration text ou snapshot para recuperar estrutura disponível nos contexts ANTLR.
- Não introduzir valores de runtime, aliases de memória, offsets, kills ou targets dinâmicos em AST, symbol table ou resolução nominal.
- FILLER continua sem símbolo nominal; sua futura identidade de storage deve nascer de `DataEntry` e não de símbolo sintético.
- Construção desconhecida não pode ser convertida em ausência de declaração, referência ou efeito.

#### Domínio de entrada e fora de escopo

O domínio inclui fonte aceito pela gramática configurada após normalização e preprocessing, todos os top-level e nested program units reconhecidos, statements, data description entries, clauses DATA, expressões/referências relevantes e containers de linguagem embarcada já reconhecidos pelo frontend.

Parser errors, COPYs ausentes e falhas de preprocessing continuam input incompleto e não entram na garantia de representação exata. SQL, CICS e SQLIMS permanecem payloads opacos. Semântica de layout de `PICTURE`/`USAGE`, conversões de `MOVE`, regiões sobrepostas, efeitos de chamadas e fluxo de controle ficam fora deste item.

#### Decisões de desenho a fechar na promoção

O work item promovido deve resolver estas decisões antes da primeira alteração de produção:

1. **Unidade de coverage:** findings representam fronteiras semânticas materializadas, não todas as 628 regras-wrapper. No mínimo: statement, data description entry, data clause e fallback/preserved expression capaz de afetar dependências.
2. **Cardinalidade:** cada fronteira encontrada possui exatamente um finding e um `astNodeId` válido; não registrar o mesmo context por wrappers intermediários.
3. **Duas dimensões independentes:** `ConstructionCoverage.MODELED` significa estrutura construída; `DependencyKnowledge.DEPENDENCY_UNKNOWN` continua permitido quando efeitos, valores ou storage ainda não foram interpretados. Modelagem estrutural não deve declarar readiness de dataflow.
4. **Falha interna versus incompletude:** produto estrutural inconsistente deve falhar fechado por invariant/exception; sintaxe válida porém ainda não interpretada deve produzir finding/gap, não exception.
5. **Identidade:** chaves de junção usam `(ProgramUnitId, astNodeId)` e `(ProgramUnitId, domain, localId)`. `astNodeId` ou `symbolId` isolado não é identidade entre units.
6. **Escopo da claim:** `dependencyAnalysisReady` continua significando readiness segundo as capacidades implementadas e versionadas; não pode ser apresentado como prova de call graph dinâmico antes de `BACKLOG-DF-002`.

#### Fase 0 — Baseline, matriz de superfície e oráculos inicialmente vermelhos

Objetivo: transformar as seis garantias em expectativas executáveis antes de mudar produção.

1. Inventariar os pontos em que `AstBuilder` cria `Statement`, `DataEntry`, `DataClause`, `PreservedExpression`, `RawExpression` e `EmbeddedLanguageStatement`.
2. Construir uma matriz versionada em teste que relacione cada fronteira com: context ANTLR, AST node esperado, coverage esperado, dependency knowledge esperado e se deve produzir occurrence.
3. Registrar o comportamento atual dos casos que revelaram a lacuna: `VALUE` sem referência nominal, clause DATA preservada sem referência, `FILLER REDEFINES`, range `RENAMES`, grupo com filhos e expressão preservada.
4. Adicionar oráculos inicialmente falhos que provem cardinalidade exata, não somente presença mínima.
5. Confirmar que os baselines existentes falham apenas pelas novas observações de coverage esperadas; não aceitar alteração incidental de AST, símbolos ou resolução.

Artefatos prováveis:

- novo `AstSemanticBoundaryCoverageTest` ou nome equivalente;
- fixtures focadas sob `src/test/resources/cobol/semantic/`;
- helpers de teste que indexem parse contexts e AST nodes sem depender de texto achatado.

Gate da fase: testes focados compilam e os casos vermelhos correspondem somente às garantias ainda ausentes.

#### Fase 1 — Preservação fechada de statements e declarações

Objetivo: provar ausência de perda e duplicação nas fronteiras estruturais relevantes.

1. Para cada `StatementContext` reconhecido, exigir exatamente um `Ast.Statement`, incluindo statements aninhados em flow clauses.
2. Para cada `DataDescriptionEntryContext` reconhecido, exigir exatamente um `Ast.DataEntry`; níveis 01–49, 66, 77, 88, FILLER e entrada SQL opaca devem permanecer distinguíveis.
3. Para cada data clause direta, exigir um `DataClause` tipado ou `PreservedDataClause` com grammar rule, texto, meta e referências reconhecidas.
4. Provar que ordem e containment seguem a estrutura gramatical: grupos contêm filhos; nível 88 pertence ao item anterior; nível 66 preserva seu owner/range; FILLER não toma emprestado o nome de outra clause.
5. Provar que `Ast.children` alcança todos esses nodes exatamente uma vez e que `AstScopeIndex` mapeia cada node alcançável.
6. Preservar `ValueClause.values` e lexemas sem interpretá-los como valores de runtime. Qualquer tipagem adicional deve derivar do context ANTLR e manter o lexema escrito.

Casos adversariais mínimos:

- keywords usadas como nomes;
- `IF`/`EVALUATE` aninhados e clauses de fluxo aninhadas;
- grupo com FILLER antes, entre e depois de itens nomeados;
- `FILLER REDEFINES target`;
- `RENAMES from THRU through` no mesmo registro e ranges inválidos;
- `VALUE` simples, intervalo, figurative constant e múltiplos valores aceitos pela gramática;
- subscript e reference modification em source e target de `MOVE`;
- linguagem embarcada entre statements ou declarações reconhecidas.

Gate da fase: oráculos exatos de parse tree → AST verdes; EVAL-AST-001 a EVAL-AST-004 continuam verdes sem mudança de binding.

#### Fase 2 — Coverage fechado nas fronteiras semânticas

Objetivo: impedir que uma construção desconhecida desapareça da claim apenas porque não contém referência nominal.

1. Generalizar o registro hoje concentrado em `buildStatement()` para as fronteiras aprovadas na Fase 0.
2. Garantir finding determinístico por ocorrência concreta, com `grammarRule`, `astNodeId`, meta, provenance, coverage, dependency knowledge e rationale.
3. Revisar `grammar-rule-manifest.tsv` contra a AST tipada atual. Exemplos que exigem decisão explícita:
   - `dataValueClause`: estrutura/lexema preservado versus semântica de valor ainda desconhecida;
   - `dataRedefinesClause` e `dataRenamesClause`: endpoints estruturados, porém alias/storage desconhecido;
   - `qualifiedDataName`, `tableCall` e reference modifier: estrutura nominal conhecida versus efeitos parciais futuros;
   - statements genéricos: referências prontas não significam writes/kills conhecidos;
   - SQL/CICS/SQLIMS: preservado e dependency unknown até analisador dedicado.
4. Não gerar finding para wrappers que apenas encaminham integralmente um filho já contabilizado.
5. Fazer `ResolutionAnalysisReport` bloquear readiness quando qualquer finding concreto tiver `UNSUPPORTED`, `INPUT_MISSING` ou `DEPENDENCY_UNKNOWN`, inclusive sem occurrence nominal.
6. Manter `PRESERVED_REFERENCE_CONTAINER` como evidência complementar para referências dentro de containers opacos, sem duplicar ou substituir o gap de frontend.

Oráculos obrigatórios:

- programa contendo apenas `VALUE` semanticamente não interpretado não pode obter claim incompatível com essa incerteza;
- clause preservada sem referências ainda produz gap de frontend;
- clause estruturada com dependency knowledge conhecido não cria gap apenas por existir;
- wrappers não multiplicam findings;
- repetir a análise produz findings e IDs na mesma ordem.

Gate da fase: EVAL-COV-001, EVAL-COV-002, EVAL-RES-COV-001 e EVAL-RES-REPORT-001 verdes.

#### Fase 3 — Integridade das junções entre produtos

Objetivo: tornar as pré-condições de consumidores posteriores verificáveis sem lookup textual.

Implementar validação linear, em teste ou produto interno pequeno, para provar por program unit:

1. todos os AST node IDs alcançáveis são únicos dentro da unit;
2. todo AST node alcançável possui exatamente um scope em `AstScopeIndex`;
3. todo símbolo aponta para `declarationAstNodeId` existente na mesma unit;
4. toda declaration relation aponta para owner symbol e reference AST node existentes;
5. toda occurrence aponta para AST node e scope existentes e aparece em exatamente uma resolution entry;
6. todo candidate DATA/INDEX/PROCEDURE aponta para símbolo existente no domínio e unit declarados;
7. todo candidate PROGRAM aponta para `ProgramUnitId` existente;
8. todos os IDs locais são interpretados somente dentro de sua identidade composta;
9. provenance da occurrence é a do AST node correspondente e continua distinguindo exato de aproximado.

Estado impossível deve falhar fechado com mensagem que identifique produto, unit e ID. `UNRESOLVED`, `AMBIGUOUS`, `UNSUPPORTED` e `EXTERNAL_OBSERVED` válidos não são falhas de integridade.

Casos negativos devem construir produtos corrompidos controlados ou usar factories de teste para provar rejeição de: node duplicado, scope ausente, candidate apontando para outro domínio, relation órfã e ocorrência sem resolution entry.

Complexidade esperada: `O(AST nodes + scopes + symbols + relations + occurrences + entries + candidates)` em tempo e espaço auxiliar linear. É proibido validar cada referência varrendo todas as declarações.

Gate da fase: EVAL-SYM-001, EVAL-SYM-002, EVAL-RES-DET-001 e EVAL-RES-PERF-001 verdes; `check-performance.sh` somente se a validação entrar no caminho de produção.

#### Fase 4 — Contrato explícito de prontidão para storage/dataflow

Objetivo: fechar a fronteira sem antecipar a fase seguinte.

1. Adicionar uma fixture integrada contendo grupo, FILLER, `VALUE`, `REDEFINES`, `RENAMES`, `OCCURS`, `MOVE` simples/CORRESPONDING, `CALL` literal e `CALL` variável em fluxo aninhado.
2. Provar que o frontend entrega todos os fatos estruturais necessários, mas não contém CFG, reaching definitions, valores possíveis ou aliases de memória.
3. Provar que `FILLER` possui `DataEntry`, meta, scope e posição estrutural mesmo sem symbol.
4. Provar que endpoints de `REDEFINES`/`RENAMES` possuem binding nominal quando válido, mantendo explícito que o semantic scope é apenas estrutural.
5. Provar que `CALL` literal usa `ProgramReference` e identifier/expression usa `DataReference` ou expressão preservada, sem converter binding DATA em target PROGRAM.
6. Documentar para `BACKLOG-DF-001` o contrato de entrada do futuro `StorageRegionModel`: percorrer `DataEntry`, usar símbolos apenas para nomes, e consumir declaration relations resolvidas sem tratá-las como layout.
7. Documentar que definições iniciais de `VALUE`, writes, partial writes e unknown effects serão produtos posteriores; sua ausência atual não deve virar valor vazio.

Gate da fase: `check-fast.sh` e `check-semantic.sh` verdes; nenhuma nova dependência arquitetural reversa.

#### Fase 5 — Regressão, migração documental e encerramento

1. Atualizar baselines apenas para diferenças explicadas pela nova coverage. Cardinalidades de AST, símbolos, occurrences e resolução devem permanecer estáveis salvo defeito comprovado por oracle.
2. Atualizar `docs/domain/semantic-ast.md`, `docs/domain/symbol-model.md` e, se a composição mudar, `docs/domain/reference-resolution.md`.
3. Atualizar o catálogo de evals com os testes finais; criar novo ID somente se o contrato não couber honestamente nos evals existentes.
4. Reforçar INV-COV-001/INV-COV-002 ou seu enforcement se os testes demonstrarem que o texto atual não exige findings concretos por fronteira.
5. Registrar no work item a matriz final de garantias, oracle e evidência; promover somente conhecimento durável aos documentos canônicos ao encerrar.
6. Executar `check-fast.sh`, `check-semantic.sh` e `check-full.sh`. Executar `check-performance.sh` se houver validação nova no caminho normal ou alteração dos índices.

#### Critérios finais de aceitação

O hardening só pode ser considerado concluído quando todos os itens abaixo forem verdadeiros:

- nenhum statement/data entry/data clause da matriz suportada desaparece ou é duplicado;
- qualquer fallback relevante possui node preservado e finding concreto;
- um unknown sem referência nominal bloqueia a claim apropriada;
- todo finding aponta para AST node existente, exceto input missing explicitamente sem node;
- todas as junções AST → scope → occurrence → resolution → candidate → declaration são válidas por identidade composta;
- FILLER e hierarquia DATA continuam disponíveis sem criar símbolos nominais falsos;
- `REDEFINES`/`RENAMES` continuam somente binding estrutural, sem falsa alegação de alias/layout;
- `CALL` variável continua gap de valor e `CALL` literal externo continua observação, não candidate fabricado;
- resultados repetidos e paralelos mantêm ordem/IDs determinísticos;
- custo permanece linear nas cardinalidades dos produtos;
- documentação e snapshots não apresentam readiness de CFG/dataflow inexistente;
- gates declarados no work item promovido estão verdes.

#### Promoção e dependências

Antes de implementar, promover este backlog para um novo work item de risco médio ou alto seguindo `docs/engineering/work-item-protocol.md`. O `source_scope` mínimo esperado inclui `AstBuilder`, `SemanticCoverage`, `GrammarCoverageManifest`, `ResolutionAnalysisReport` e apenas os modelos/validators cuja necessidade for demonstrada pelos testes vermelhos. `Ast`, symbol table e contratos de resolução não devem mudar sem uma lacuna concreta da matriz.

`BACKLOG-CFG-001` e `BACKLOG-DF-001` podem iniciar somente após as garantias estruturais e de coverage relevantes estarem verdes. `BACKLOG-DF-001` continua responsável por storage regions e efeitos de memória; `BACKLOG-DF-002` continua responsável pela interpretação final de targets dinâmicos.

## Resolução nominal

### BACKLOG-RES-003 — Classificar condition-names em `EVALUATE TRUE ... WHEN`

#### Evidência e defeito

Após `WORK-RES-003`, COACTUPC ainda possui 108 occurrences `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`. Todas têm `role=VALUE_READ`, `grammarRule=qualifiedDataName` e ocorrem como selector de `WHEN` sob `EVALUATE TRUE`, por exemplo:

```cobol
    EVALUATE TRUE
       WHEN CCARD-AID-PFK03
       WHEN ACUP-CHANGES-OKAYED-AND-DONE
```

Esses nomes são condition-names de nível 88 e devem ser resolvidos no namespace CONDITION. A gramática aceita `evaluateCondition` tanto como `condition` quanto como `evaluateValue`; para um identificador isolado, o parse atual percorre `evaluateValue → identifier`. `AstBuilder.buildEvaluate` preserva somente uma lista de `Expression`, e `ReferenceOccurrenceCollector` visita cada selector como `VALUE_READ` genérico. O contexto `EVALUATE TRUE` se perde, fazendo o coletor emitir DATA e produzindo o falso gap.

Este é um defeito distinto de `SET condition-name TO TRUE/FALSE`: a regra não pode ser ampliada por nome, regex ou pela suposição de que todo `WHEN identifier` é CONDITION. `EVALUATE data-item WHEN identifier` continua comparação de valor e o identifier pode ser DATA/INDEX; `WHEN` também aceita literals, intervalos, `NOT`, condições completas e múltiplos `ALSO`.

#### Resultado esperado

No domínio COBOL explicitamente suportado, cada selector nominal de `WHEN` correspondente a um subject booleano `TRUE` ou `FALSE` deve carregar contexto CONDITION e admitir somente `ReferenceKind.CONDITION`. A resolução deve selecionar a declaração nível 88 visível, preservar ambiguidade entre condition-names reais e continuar reportando namespace incompatível quando não houver condition-name admissível.

Selectors de `EVALUATE` cujo subject correspondente não for booleano, ou cuja forma ainda não tiver regra exata, permanecem DATA/INDEX conforme o contrato atual ou conservadoramente preservados/unsupported; o item não pode converter incerteza em sucesso.

#### Proposta de implementação

1. Antes de alterar produção, criar fixtures adversariais para `EVALUATE TRUE` e `EVALUATE FALSE` com `WHEN condition-name`, `WHEN NOT condition-name`, múltiplos `WHEN` e correspondência posicional entre `EVALUATE ... ALSO ...` e `WHEN ... ALSO ...`.
2. Criar contracasos: `EVALUATE data-item WHEN data-item`, `WHEN literal`, `WHEN value THRU value`, condition-name homônimo de DATA e condition-name ausente. Os oráculos precisam distinguir kind da occurrence, conjunto admissível, status e candidato selecionado.
3. Evoluir a AST para preservar o contexto semântico de cada selector de `EVALUATE` (por exemplo, node/record `EvaluateSelector` com expression e contexto), derivado dos contexts `evaluateSelect`, `evaluateCondition`, `evaluateValue` e `booleanLiteral`. Não inferir a decisão pelo texto ou pela grafia de `TRUE`.
4. No collector, usar o contexto do selector e o subject correspondente para emitir CONDITION somente na classe comprovada; manter a cardinalidade e a ordem determinísticas dos selectors/occurrences.
5. Atualizar snapshot, catálogo de evals e baseline de COACTUPC somente para diferenças explicadas. A contagem de 108 é evidência de corpus, não especificação da regra.
6. Rodar PIT focalizado sobre o lowering de `EVALUATE` e o collector. Os novos testes devem matar, no mínimo, mutações que removam a classificação CONDITION, ignorem o subject booleano, troquem a correspondência de `ALSO` ou classifiquem todo selector como CONDITION.

#### Autoridade, restrições e gates

- Regras canônicas: `docs/domain/semantic-ast.md`, `docs/domain/reference-resolution.md` e IBM Enterprise COBOL para `EVALUATE` e condition-names.
- Invariantes: INV-AST-001, INV-AST-002, INV-RES-001, INV-RES-002, INV-COV-001 e INV-DET-001.
- Evals a ampliar: EVAL-AST-004, EVAL-RES-DATA-001, EVAL-RES-COV-001 e EVAL-MUT-001.
- Fora de escopo: avaliação de valores de runtime, CFG/dataflow, CALL dinâmico e resolução de statements sem uma regra gramatical/semântica específica.
- Promover para work item de risco médio antes de produção; gates mínimos: `fast`, `semantic` e `full`.

### BACKLOG-RES-004 — IBM resolution-of-names: precedência de programa local após qualification

#### Evidência e defeito

O contracaso do round 2 do Discovery de `WORK-COND-004` (fato `qualifiedLocalDataNameCollidesWithOuterGlobalFileNameAcrossPrograms`) reproduz a colisão: OUTER declara `FD Q IS GLOBAL` com record `OUTER-REC` e `88 C`; INNER (contido) declara `01 Q` com `88 C` e escreve `IF C OF Q`. Hoje a condition surface fecha o qualifier como `DATA` e o resolver devolve `RESOLVED` (candidate local) — mas por exclusão acidental de namespace, não pela regra da linguagem. A referência DATA equivalente (`MOVE CUST-STATUS OF Q TO X`) já devolve `UNSUPPORTED_DIALECT_OPTION` com 2 candidates: o mesmo defeito vive hoje nos paths de data, independentemente de condition-name.

A regra IBM (Enterprise COBOL for z/OS 6.4 LR, cap. 7 "Resolution of names — Names within programs", pp. 63–66) determina: (1) o recurso referenciado é identificado aplicando qualification e demais regras de unicidade ao conjunto {nomes definidos no programa B} ∪ {nomes GLOBAL definidos em A e nos programas que contêm A}; (2) se mais de um recurso for identificado, no máximo um pode ter nome local a B; se zero ou um é local a B, o recurso declarado em B vence e, na ausência de declaração local, vence o do programa contendo mais próximo. A precedência por programa é aplicada DEPOIS da qualification, vale igualmente para condition-name, data-name, file-name e record-name e independe de `QUALIFY(STANDARD/EXTEND)`.

O resolver atual não possui esse passo: trata a divergência pós-qualification como divergência de qualify mode (`UNSUPPORTED_DIALECT_OPTION` ou `AMBIGUOUS`) em vez de aplicar a precedência de programa local. Por isso a surface NÃO pode ampliar `DATA → DATA_OR_FILE` para condition-names antes desta regra existir: a ampliação adicionaria o candidate GLOBAL externo e converteria `RESOLVED` em `UNSUPPORTED_DIALECT_OPTION`/`AMBIGUOUS` (regressão caracterizada no round 2 do Discovery).

#### Resultado esperado

Após a qualification identificar mais de um recurso, quando zero ou um deles é local ao programa da referência, selecionar o local (ou o do contendo mais próximo, na ausência de local); ambiguidade entre dois ou mais recursos locais ao MESMO programa permanece `AMBIGUOUS` (INV-RES-001). O passo aplica-se a DATA/CONDITION (e futuramente FILE/record) sem alterar candidate filtering por namespace, scope walk ou admissibleKinds das occurrences. O mapeamento do alvo de qualifier não classificado (`QualifierTarget.UNSPECIFIED`, introduzido pelo Slice 4 de `BACKLOG-COND-001`) poderá então ser ampliado de `{DATA}` para `{DATA, FILE}` sem regressão, destravando a resolução de condition-names qualificadas por file-name (hoje `DECLARATION_NOT_FOUND`).

#### Proposta de implementação

1. Discovery com os oracles do contracaso: local DATA vs outer GLOBAL FILE; local DATA vs outer GLOBAL DATA; file-only sem colisão; dupla localidade ambígua no mesmo programa; nesting de 3 níveis.
2. Modelar o passo 3 como etapa do resolver entre `applyQualification` e a decisão de qualify mode — ou como pré-filtro de candidates por programa de declaração — com invariante próprio a definir na promoção e registro em `docs/domain/reference-resolution.md`.
3. Somente depois, ampliar o mapeamento conservador `UNSPECIFIED → {DATA}` para `{DATA, FILE}` no resolver.
4. Regressão: nenhum caso `RESOLVED` atual muda, exceto os que a nova regra torna `RESOLVED` (local) a partir de `UNSUPPORTED_DIALECT_OPTION`/`AMBIGUOUS`/`DECLARATION_NOT_FOUND`.

#### Autoridade, restrições e gates

- Autoridade: IBM Enterprise COBOL for z/OS 6.4 LR, cap. 7 "Resolution of names" e cap. 8 "Scope of names"/"Qualification".
- Invariantes: INV-RES-001, INV-DET-001, INV-PERF-001; a promoção deverá definir invariante próprio para a precedência de programa local após qualification.
- Evals a ampliar: EVAL-RES-DATA-001 (colisão local/GLOBAL), EVAL-RES-DET-001; os oracles FACT de `WORK-COND-004` (`ConditionNameSurfaceDiscoveryTest.qualifiedLocalDataNameCollidesWithOuterGlobalFileNameAcrossPrograms`) permanecem como caracterização.
- Fora de escopo: SPECIAL-NAMES/mnemonic, resolução de qualifier occurrences de file-names, CFG/dataflow.
- Promover para work item de risco alto quando autorizado; gates mínimos: `fast`, `semantic`, `full`. Bloqueia apenas a ampliação do mapeamento de `QualifierTarget.UNSPECIFIED` no resolver — não bloqueia o Slice 4 estrutural de `BACKLOG-COND-001`.

## Condições e predicados

### BACKLOG-COND-001 — Contextualizar condições combinadas e referências nominais

Progresso: os Slices 1–6 foram concluídos pelos PRs #15–#20 e arquivados. O Slice 6 (`SEARCH WHEN`) materializa a boundary AST/occurrence aprovada, sem validar semanticamente `SEARCH ALL`; o Slice 7 (regressão de corpus) permanece pendente. A dependência `BACKLOG-RES-004` permanece separada e destravará `{DATA, FILE}` no resolver.

#### Evidência e problema

O [Discovery de contextualização semântica de condições](../history/evidence/semantic-condition-context-discovery-report.md) reproduziu uma classe em que a categoria sintática escolhida pela grammar é fechada como categoria nominal antes de haver contexto suficiente. Em `A = B OR C`, a parse tree atual pode representar C como `conditionNameReference`; o lowering preserva essa `grammarRule`, o collector emite `CONDITION/{CONDITION}` e o resolver rejeita uma declaração DATA, INDEX ou RENAMES homônima com `INVALID_NAMESPACE_FOR_CONTEXT`.

A escolha só pode ser especializada com a regra COBOL e o binding nominal:

- se C nomeia DATA, INDEX ou RENAMES admissível como objeto relacional, a condição abrevia sujeito e operador e equivale semanticamente a `A = C`; quando C é index-name e A é data-name comum, A precisa ser numérico inteiro;
- se C nomeia um condition-name nível 88, C inicia uma simple condition e encerra a inserção herdada;
- em `(A = B OR C) AND D`, C ainda pode herdar `A =`, mas o `)` correspondente a um `(` à esquerda do sujeito encerra a herança antes de D; D precisa iniciar uma simple condition válida;
- em `(A = B) OR C`, C também está fora da sequência herdada;
- quando `(` aparece imediatamente após o relational operator, o operador é distribuído e sujeito/operador permanecem correntes depois do `)` que encerra a distribuição. A futura regra não pode generalizar todo parêntese como término idêntico.

O Discovery encontrou ainda lacunas relacionadas e reproduzíveis: `abbreviation`/`relationCombinedComparison` sem sujeito e operador herdados materializados, AND/OR mistos achatados como `MIXED_LOGICAL`, seleção exclusiva de `abbreviation(0)`, perda de condition references em `SEARCH WHEN` e corrupção estrutural de condition-name subscriptado. O oracle SEARCH usa um nível 88 cuja conditional variable está fora da tabela, para não depender de subscript; o caso subscriptado permanece isolado em fixture própria. Não há finding de defeito no candidate filtering do resolver para a occurrence que ele recebe.

#### Resultado esperado

Representar condições combinadas de forma lossless e semanticamente contextual, preservando conectores, precedência, parênteses, NOT, sujeito/operador escritos ou herdados e o boundary de inserção. A incerteza entre condition-name e objeto abreviado deve permanecer explícita até que declaration kind e visibility permitam especialização, sem reparse de texto, spelling heuristics ou exceção por parent textual.

Occurrences devem derivar `kind` e `admissibleKinds` do contexto semântico aprovado, não tratar `Meta.origin.grammarRule()` como verdade nominal. Condition-name real no mesmo bare tail deve continuar CONDITION; DATA, INDEX e RENAMES usados como objetos de relação abreviada devem permanecer admissíveis. O resolver pode selecionar entre kinds semanticamente admissíveis, mas não deve reconstruir sujeito, operador, NOT ou precedência.

#### Fatiamento e dependências para promoção

Promover somente um slice revisável por vez, nesta ordem:

1. **concluído no PR #15:** fechar o contrato IBM de inserção/término, operador distribuído, qualification, scope e homônimos DATA/CONDITION com oracles adversariais;
2. **concluído no PR #16:** decidir em ADR/invariant se a representação será contextual na AST, normalizada no lowering ou projetada em produto pós-binding, incluindo IDs, provenance e occurrences sintéticas;
3. **ativo em WORK-COND-003:** tornar a condition sequence lossless, preservando todos os connectors/children, NOT, parênteses, `relationCombinedComparison` e precedência sem alterar o resolver no mesmo slice;
4. corrigir a estrutura de condition-name, qualification e subscripts com oracles próprios;
5. **concluído no PR #19:** projetar occurrences contextuais shape-sensitive, remover o acoplamento semântico ao nome da grammar rule, tipar controls de PERFORM, atualizar o manifesto e cobrir a regressão WAUX-like local;
6. **concluído no PR #20:** materializar `SEARCH WHEN` com boundary AST tipada, routing explícito de condition/varying, preservação de `NEXT SENTENCE` e prova de que nenhuma referência é duplicada ou desaparece;
7. **pendente:** executar regressão de corpus e promover somente diferenças justificadas, incluindo o fonte WAUX quando ele estiver disponível.

O slice arquitetural precede mudanças transversais de lowering/occurrence. Mudança no resolver só entra em escopo se o contrato aprovado demonstrar necessidade depois que a occurrence estiver semanticamente correta. SEARCH e condition-name subscriptado podem virar work items separados se seus `source_scope` e riscos não couberem no mesmo slice.

#### Restrições, evals e promoção

- Autoridade: IBM Enterprise COBOL para abbreviated combined relation conditions, complex conditions, condition-name e index-name.
- Regras canônicas a revisar quando autorizado: `docs/domain/semantic-ast.md`, `docs/domain/reference-resolution.md` e `docs/domain/symbol-model.md`.
- Invariantes relacionados: INV-AST-001, INV-AST-002, INV-AST-003, INV-SYM-001, INV-RES-001, INV-RES-002, INV-PROV-002, INV-COV-001 e INV-DET-001.
- Evals a ampliar ou criar na promoção: conditions positivas/negativas/ambíguas, DATA/CONDITION/INDEX/RENAMES, qualification, COPY, nested scope, AND/OR/NOT/parênteses, SEARCH e subscript.
- Fora de escopo: inferir valores de runtime, CFG, dataflow, constant propagation ou targets dinâmicos; esses consumidores apenas motivam uma representação correta.
- Proibido iniciar implementação a partir deste backlog. Criar work item de risco alto conforme `docs/engineering/work-item-protocol.md`; gates mínimos previstos: `fast`, `semantic`, `performance` quando houver nova propriedade algorítmica, e `full`.

## CFG e efeitos semânticos

### BACKLOG-CFG-001 — CFG estrutural incremental

Introduzir produto CFG separado da AST e do binding nominal. Fatiar por fluxo linear, basic blocks, `IF`, `EVALUATE`, `GO TO`, `GO TO DEPENDING ON`, `PERFORM`, `PERFORM THRU`, `NEXT SENTENCE`, terminação e fallthrough. Cada slice precisa de oracle adversarial próprio.

### BACKLOG-CFG-002 — Statements preservados com efeito de fluxo

Modelar incrementalmente `ALTER`, `SEARCH`, `SORT`, `MERGE` e `ENTRY`, mantendo fallback conservador. Outros statements preservados (`CANCEL`, comunicação, report writer e display/exhibit) entram conforme necessidade de análise concreta.

### BACKLOG-DF-001 — StatementEffects e reaching definitions

Criar produto separado com reads, writes, partial writes, kills e unknown memory effect. Cobrir `MOVE`, group/CORRESPONDING, reference modification, `SET`, `STRING`, `UNSTRING`, `INITIALIZE`, `ACCEPT`, aritmética, operações de arquivo e parâmetros de `CALL`. Modelar aliases de `REDEFINES`/`RENAMES` como regiões, não apenas nomes.

### BACKLOG-DF-003 — Propagação conservadora de valores possíveis

#### Problema e resultado esperado

Reaching definitions informa quais definições podem alcançar um ponto, mas consumidores como CALL dinâmico, CICS e GRBE precisam dos valores literais que essas definições podem carregar. Essa capacidade pertence ao core semântico e não pode nascer dentro de cada consumidor.

Produzir um artefato imutável e consultável conceitualmente como:

```text
possibleValues(variableOrRegion, programPoint)
    → knownValues { "PROGA", "PROGB" }
      + unknownRemainder
```

O resultado conserva simultaneamente o conjunto finito de valores estaticamente demonstráveis e um remainder explícito quando writes, aliases, inputs, chamadas ou construções não suportadas impedirem completude. Incerteza não colapsa cedo para `NOT_CONST` nem apaga valores conhecidos.

#### Dependências e propriedades

- Depende de BACKLOG-CFG-001 e BACKLOG-DF-001 para program points, regiões, statement effects e reaching definitions; não reimplementa binding nominal nem layout.
- Transfer functions entram incrementalmente por classe semântica comprovada. Efeito desconhecido acrescenta remainder incerto em vez de produzir conjunto vazio.
- Joins unem valores conhecidos e propagam o unknown remainder. Loops exigem argumento explícito de fixpoint e terminação. Widening ou limites de cardinalidade só entram se o domínio abstrato escolhido os exigir; qualquer perda de precisão permanece observável e um conjunto truncado nunca é apresentado como completo.
- Resultado, ordem, provenance das evidências e diagnostics são determinísticos. A claim deve distinguir conjunto completo, parcial e desconhecido.
- Complexidade e memória precisam ser caracterizadas pelas cardinalidades de CFG, regions, definitions e valores; threshold dependente de hardware não é oracle.

#### Consumidores, fora de escopo e promoção

- BACKLOG-DF-002 consulta este produto para targets de CALL dinâmico.
- BACKLOG-EXT-004 e BACKLOG-EXT-005 consultam o mesmo serviço para operands CICS/DB2/IMS e campos do protocolo GRBE; nenhum extractor implementa dataflow próprio.
- Fora de escopo: resolver CALL, emitir facts finais de dependência, interpretar protocolos externos ou afirmar valores exatos de runtime.
- Implementação ingênua rejeitada: procurar `MOVE` anterior por texto, escolher uma reaching definition, descartar valores conhecidos ao encontrar um caminho incerto ou confundir binding DATA com valor.

Promover depois que CFG e o slice pertinente de StatementEffects/reaching definitions estiverem verdes. O work item deve começar por literals e `MOVE` simples, depois joins/loops e unknown effects, com oráculos para múltiplos valores, known-plus-unknown, aliases/partial writes, determinismo e terminação. Relações: ADR-0003, ADR-0004, ADR-0008, INV-RES-002, INV-COV-001, INV-DET-001, BACKLOG-CFG-001 e BACKLOG-DF-001.

### BACKLOG-DF-002 — Targets de CALL dinâmico

Consumir BACKLOG-DF-003 para calcular conjuntos de programas possíveis sem confundir binding da variável com seu valor. Preservar targets conhecidos e remainder dinâmico. `CBSTM03D` é cenário didático, não especificação completa; este item resolve CALL, não fornece possible-values genérico aos demais consumidores.

## Linguagens embarcadas e built-ins

### BACKLOG-EMB-001 — Porta para analisadores embarcados

Definir a fronteira entre `EmbeddedLanguageStatement` opaco e analisadores dedicados para SQL, CICS e SQLIMS. Cada analyzer deve consumir payload/provenance preservados, produzir AST/facts próprios tipados e ligar host variables às occurrences COBOL por identidade estrutural. Payload inválido, comando não suportado e SQL/comando dinâmico permanecem desconhecidos até parser/análise de valores adequados; regex não substitui parser.

Depende de BACKLOG-EXT-001 para composição sem branches tecnológicos no pipeline. Serve de pré-condição a BACKLOG-EXT-004 e BACKLOG-EXT-006, mas não implementa por si só dependências de programa, efeitos de CFG ou dataflow. O work item futuro deve fechar ownership do parser dedicado, versão/dialeto, provenance através de host variables, coverage e falha fechada; execução sem analyzers preserva o payload opaco atual conforme ADR-0007 e INV-EMB-001.

### BACKLOG-DIALECT-001 — Built-ins e opções adicionais

Versionar special registers/intrinsics por compilador e ampliar opções somente com fonte semântica e configuração explícita. Modos `PGMNAME` fora de COMPAT/LONGUPPER/LONGMIXED e `CALLINTERFACE` por statement continuam não modelados.

## Codebase e dependências externas

### BACKLOG-RUNNER-001 — Runner e catálogo de codebase

Criar runner por codebase, repositório de copybooks, catálogo externo persistente e agregação paralela. Correção, aliases e completude do catálogo precisam permanecer premissas observáveis, não garantias implícitas do resolver.

### BACKLOG-DEPS-001 — Fatos finais de dependência

Produzir fatos de subprogramas/arquivos somente depois de combinar binding, coverage, CALL semantics e dataflow necessários. ASSIGN/DDNAME final e call graph externo não podem derivar apenas de literal ou candidate nominal.

## Observabilidade e apresentação

### BACKLOG-OBS-001 — Identidade e métricas operacionais

Adicionar SHA-256 do input, duração e tamanho de índices por fase fora do snapshot determinístico; transportar include chain completa também em gaps globais.

### BACKLOG-OBS-002 — Contexto concorrente

Propagar MDC explicitamente quando houver processamento assíncrono/multithread e testar isolamento entre tarefas.

### BACKLOG-OBS-003 — Diagnostics tipados

Avaliar evolução compatível de `Diagnostic` para code/severity estáveis, eliminando consumidores textuais remanescentes sem forçar breaking change transversal.

### BACKLOG-UI-001 — Navegação multi-unit

Evoluir AST/Symbol Table HTML para nested e múltiplos program units, preservando links honestos quando uma unidade não está materializada na página.

## Evals e desempenho

### BACKLOG-EVAL-001 — Propriedades metamórficas semânticas

Adicionar geradores e transformações controladas para provar invariância a case, comentários, sequence area e renome de símbolos não relacionados. Cada propriedade deve declarar precondições e comparar o produto semântico apropriado, não snapshots textuais acidentais.

### BACKLOG-EVAL-002 — Oracle diferencial de referência

Criar um resolver de referência lento e obviamente correto para topologias pequenas, usado somente em testes. Compará-lo ao resolver indexado por geração de casos para proteger futuras otimizações contra perda de candidatos, alteração de visibilidade ou desempate indevido.

### BACKLOG-EVAL-003 — Mutation testing focalizado

Estender o perfil focalizado introduzido por EVAL-MUT-001 aos contratos de maior risco ainda não cobertos — ambiguidade, GLOBAL, qualification, incompletude e CALL — com orçamento e conjunto de mutantes explícitos. Não transformar score global em métrica de vaidade nem introduzir gate instável.

### BACKLOG-PERF-001 — Escala das fases pré-resolução

Adicionar cenários sintéticos grandes para normalização, preprocessing, AST, compilation units e símbolos. Verificar cardinalidades, limites de recursão e ausência de scans multiplicativos por métricas determinísticas, sem threshold de tempo dependente de hardware.
