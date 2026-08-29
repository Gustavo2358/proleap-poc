# Backlog atual

Este arquivo registra trabalho futuro válido que não pertence a um work item ativo. Ordem não implica autorização para iniciar; cada item deve receber spec, eval e boundary explícitos antes de implementação.

## Arquitetura

### BACKLOG-ARCH-001 — Fronteiras por pacotes e Clean Architecture

Refatorar o package único para componentes com dependências direcionais verificáveis (frontend, AST, symbols, occurrences, resolution, presentation). Preservar APIs/produtos e usar os invariantes atuais como oracle. Depois da migração, substituir o check de bytecode por regras primariamente baseadas em package boundaries quando isso proteger o conceito sem acoplar nomes acidentais.

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

## CFG e efeitos semânticos

### BACKLOG-CFG-001 — CFG estrutural incremental

Introduzir produto CFG separado da AST e do binding nominal. Fatiar por fluxo linear, basic blocks, `IF`, `EVALUATE`, `GO TO`, `GO TO DEPENDING ON`, `PERFORM`, `PERFORM THRU`, `NEXT SENTENCE`, terminação e fallthrough. Cada slice precisa de oracle adversarial próprio.

### BACKLOG-CFG-002 — Statements preservados com efeito de fluxo

Modelar incrementalmente `ALTER`, `SEARCH`, `SORT`, `MERGE` e `ENTRY`, mantendo fallback conservador. Outros statements preservados (`CANCEL`, comunicação, report writer e display/exhibit) entram conforme necessidade de análise concreta.

### BACKLOG-DF-001 — StatementEffects e reaching definitions

Criar produto separado com reads, writes, partial writes, kills e unknown memory effect. Cobrir `MOVE`, group/CORRESPONDING, reference modification, `SET`, `STRING`, `UNSTRING`, `INITIALIZE`, `ACCEPT`, aritmética, operações de arquivo e parâmetros de `CALL`. Modelar aliases de `REDEFINES`/`RENAMES` como regiões, não apenas nomes.

### BACKLOG-DF-002 — Targets de CALL dinâmico

Usar CFG e dataflow para calcular conjuntos de programas possíveis sem confundir binding da variável com seu valor. Preservar targets conhecidos e remainder dinâmico. `CBSTM03D` é cenário didático, não especificação completa.

## Linguagens embarcadas e built-ins

### BACKLOG-EMB-001 — Porta para analisadores embarcados

Definir `EmbeddedLanguageAnalyzer` e plugins com parser dedicado para SQL/CICS/SQLIMS. Ligar host variables às occurrences COBOL; SQL/comando dinâmico permanece desconhecido até análise de valores. Regex não substitui parser.

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
