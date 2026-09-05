# Invariantes arquiteturais

IDs neste documento são estáveis. `AUTOMATED` indica proteção executável atual; `PARTIALLY_AUTOMATED` combina testes com review; `REVIEW` depende hoje de inspeção semântica.

## AST e fases

### INV-AST-001 — AST sem produtos de análise

- **Statement:** a AST não contém bindings, symbol tables, CFG ou resultados de dataflow.
- **Rationale:** mantém estrutura semântica reutilizável e evita ciclos entre fases.
- **Scope:** `Ast`, `AstBuilder` e consumidores.
- **Related ADRs:** ADR-0003.
- **Enforcement:** `AUTOMATED` — `ArchitectureBoundaryTest` bloqueia dependências diretas da AST/construção para símbolos, resolução e apresentação; testes de AST completam o contrato comportamental.
- **Known exceptions:** nenhuma.

### INV-AST-002 — Estrutura deriva da gramática

- **Statement:** estrutura semântica é extraída de parse contexts e tokens; texto achatado não é reparsed para substituir a gramática.
- **Rationale:** evita heurística e perda de qualificadores, spans e alternativas.
- **Scope:** construção da AST e coleta de referências.
- **Related ADRs:** ADR-0009.
- **Enforcement:** `PARTIALLY_AUTOMATED` — manifestos/testes de cobertura e review.
- **Known exceptions:** operações genuinamente lexicais com domínio fechado.

### INV-AST-003 — IDs seguem o pre-order canônico

- **Statement:** dentro de cada `ProgramUnit`, cada instância de `Ast.Node` é alcançada exatamente uma vez pelo pre-order de `Ast.children` e possui `Meta.id` igual à sua posição nessa traversal; os IDs são determinísticos e formam o intervalo contíguo `0..N-1`. Metadata não-AST não consome esse namespace estrutural.
- **Rationale:** snapshots e joins entre produtos separados precisam de identidade local reproduzível sem gaps, ciclos, filhos nulos ou instâncias compartilhadas; a posição canônica é contrato de representação, não regra COBOL.
- **Scope:** `Ast`, `AstBuilder`, metadata diagnóstica ancorada em nodes e consumidores de identidade AST.
- **Related ADRs:** ADR-0003 e ADR-0005.
- **Enforcement:** `AUTOMATED` — `AstPreorderInvariantTest` percorre `Ast.children` em `O(nodes)` nos casos adversariais e na superfície representativa; `AstSnapshot` mantém o fail-closed posicional.
- **Known exceptions:** nenhuma; metadata de produto que não é `Ast.Node` fica fora do namespace, ainda que reutilize a `Meta` de um node como anchor.

### INV-SYM-001 — Símbolos não executam binding

- **Statement:** symbol tables modelam declarações, escopos, namespaces, entidades e relações declarativas; usos continuam `NOT_PERFORMED` até a resolução.
- **Rationale:** separa inventário nominal de decisão de lookup.
- **Scope:** builders e modelos de símbolos.
- **Related ADRs:** ADR-0003.
- **Enforcement:** `AUTOMATED` — `ArchitectureBoundaryTest` bloqueia dependência direta de construção de símbolos para ANTLR/resolução; testes de symbol table, occurrences e compilation units completam o contrato comportamental.
- **Known exceptions:** nenhuma.

## Semantic Product e lowering

### INV-SP-001 — Slice limita capability, não cardinalidade

- **Statement:** para cada `ProgramUnit`, o Semantic Product publica todas as ocorrências observadas das capabilities que declara cobrir; fixture mínima, primeiro match ou quantidade fixa não podem limitar DATA, statements ou relações. Novas famílias entram por coleções/facts tipados, não por campos singleton no envelope.
- **Rationale:** cobertura incremental define quais formas são compreendidas, enquanto cardinalidade pertence ao programa; confundi-las trunca silenciosamente código suportado e impede evolução aditiva.
- **Scope:** tipos, ports, projectors, adapters de transporte e consumers do Semantic Product.
- **Related ADRs:** ADR-0005, ADR-0008 e ADR-0013.
- **Enforcement:** `REVIEW` — o work item ativo exige oracle de multiple occurrences e container extensível antes da remediation de produção.
- **Known exceptions:** a implementação inicial de `CobolSemanticProduct`/`CobolSemanticPort` e do projector focalizado ainda materializa um único `MOVE` e um único `CALL`; `WORK-SEMANTIC-PRODUCT-002` registra essa dívida como estado implementado, não como direção arquitetural.

### INV-SP-002 — Incompletude não vira omissão silenciosa

- **Statement:** constructs observados que estejam `UNKNOWN`, `PARTIAL`, `UNSUPPORTED`, `AMBIGUOUS` ou `INPUT_MISSING` permanecem facts, coverage ou gaps localizados; sua ausência não pode ser interpretada como ausência de código, e uma claim agregada não pode exceder a completude de seus componentes.
- **Rationale:** downstream precisa distinguir inexistência provada de incapacidade, falta de input ou suporte parcial para permanecer conservador.
- **Scope:** Semantic Product, projeção, lowering-readiness, transporte e futuros consumers CFG/effects.
- **Related ADRs:** ADR-0008 e ADR-0013.
- **Enforcement:** `PARTIALLY_AUTOMATED` — INV-COV-001/003 protegem os produtos atuais; `WORK-SEMANTIC-PRODUCT-002` acrescentará oracles de inventário e no-silent-omission.
- **Known exceptions:** o projector inicial falha fora do fixture estreito e ainda não publica coverage/incompleteness de todos os statements observados; essa lacuna permanece explícita no work item ativo.

### INV-SP-003 — Boundary é suficiente para lowering sem frontend

- **Statement:** cada construct marcado como suportado ou ready preserva surface, identity, structure, nominal binding, unknowns, provenance e coverage necessárias para que um consumer do port determine o lowering declarado sem consultar AST, symbols, occurrences, resolver, report ou presentation. CFG/effects readiness são dimensões separadas; anchor/program point estrutural não afirma execution order, reachability ou edge.
- **Rationale:** retornar ao frontend duplicaria interpretação COBOL no downstream; confundir readiness estrutural, de controle e de efeitos criaria claims falsas.
- **Scope:** Semantic Product, seu port, `CobolLower` futuro e contratos de readiness por construct.
- **Related ADRs:** ADR-0003, ADR-0004 e ADR-0013.
- **Enforcement:** `REVIEW` — consumer independente e matriz de readiness são requisitos do work item ativo; architecture gate já protege parte do leakage direto.
- **Known exceptions:** a boundary inicial prova somente o fixture linear DATA/MOVE/CALL e ainda não publica IF/ELSE nem a cobertura completa da unit.

### INV-SP-004 — Projector não cria nova análise semântica

- **Statement:** projectors/adapters do Semantic Product apenas traduzem e reconciliam fatos das autoridades canônicas; não reparseiam `writtenText`, resolvem por nome, recalculam gaps/readiness, escolhem candidates, inferem runtime values ou usam snapshot/HTML como fonte. Informação necessária ausente produz partial/unsupported/gap, nunca heurística escondida.
- **Rationale:** uma segunda engine no adapter divergiria dos produtos canônicos e romperia a separação de fases.
- **Scope:** projeção frontend → Semantic Product e adapters de transporte.
- **Related ADRs:** ADR-0003, ADR-0004, ADR-0008, ADR-0009 e ADR-0013.
- **Enforcement:** `PARTIALLY_AUTOMATED` — testes de boundary/leakage e review das autoridades; o work item ativo exige que report e demais produtos permaneçam autoridades dos fatos que publicam.
- **Known exceptions:** o adapter inicial já evita reparse e nova resolução, mas ainda cria localmente parte do status/gap do slice em vez de projetar o report canônico; a correção pertence ao checkpoint futuro do projector.

### INV-SP-005 — Identidade nominal não é identidade de storage

- **Statement:** handles nominais de DATA identificam declarations/bindings, não storage físico independente. Nenhum Semantic Product, lowerer, IR ou dataflow pode assumir `DataItemId == StorageId`; layout, aliases e overlap de `REDEFINES`/`RENAMES` exigem Storage Semantics explícita.
- **Rationale:** nomes distintos podem compartilhar regiões de memória e uma análise por nome isolado produziria reads/writes, kills e reaching definitions incorretos.
- **Scope:** Semantic Product, lowering, futura Analysis IR, Storage Semantics, effects e dataflow.
- **Related ADRs:** ADR-0003, ADR-0004 e ADR-0013.
- **Enforcement:** `REVIEW` — AST/symbol model preservam relações nominais sem alegar layout; o backlog downstream deve manter a dependência de Storage Semantics.
- **Known exceptions:** Storage Semantics ainda não existe; sua ausência permanece explícita e não autoriza aliases ou regiões sintéticas.

### INV-SP-006 — Determinismo de transporte não é identidade persistente

- **Statement:** execuções equivalentes — mesma entrada normalizada/preprocessada, policy/configuração efetiva, analyzer version e contract version — reproduzem handles, facts e ordem transportáveis, sem timestamp, object identity ou map order incidental. Essa propriedade não preserva os mesmos IDs após edição estrutural ou mudança de versão e não constitui identidade persistente.
- **Rationale:** transporte e evals precisam de reprodutibilidade, enquanto estabilidade longitudinal requer contrato de identidade/migração próprio.
- **Scope:** Semantic Product, ports, projectors e adapters de transporte.
- **Related ADRs:** ADR-0005 e ADR-0013.
- **Enforcement:** `PARTIALLY_AUTOMATED` — INV-DET-001 protege a ordem dos produtos atuais; o work item ativo exige testes específicos antes do JSON determinístico.
- **Known exceptions:** persistência e migração de identity permanecem fora do contrato; o transporte JSON ainda não foi implementado.

## Condições contextuais

### INV-COND-001 — Significado dependente de binding permanece aberto até a resolução

- **Statement:** a AST de superfície preserva losslessly a condition sequence, o contexto de abbreviation e todas as alternativas semanticamente admissíveis; branch da grammar, `grammarRule`, grafia ou ordem não podem fechar um bare nominal tail como CONDITION ou object abreviado antes do binding.
- **Rationale:** declaration kind, qualification e scope são necessários para distinguir condition-name de DATA/INDEX em abbreviated combined relation conditions.
- **Scope:** lowering de condições, AST, coleta de occurrences e manifestos relacionados.
- **Related ADRs:** ADR-0003 e ADR-0012.
- **Enforcement:** `AUTOMATED` — `ContextualConditionOccurrenceTest`, `SemanticConditionContextDiscoveryTest`, `ReferenceResolutionManifestTest` e regressões de AST/resolution.
- **Known exceptions:** `ConditionSemantics` e `ConditionValidation` ainda não existem; a especialização pós-binding permanece futura. A policy de occurrence, contudo, já é contextual e shape-sensitive desde `WORK-COND-005`: `grammarRule` permanece apenas metadata, uma referência escrita gera uma occurrence, e subject/operator/`NOT` herdados não geram occurrences sintéticas.

### INV-COND-002 — Predicate normalizado é produto pós-binding separado

- **Statement:** quando a especialização/expansão de condição contextual for materializada, ela ocorre em produto imutável pós-binding com identidade própria por `ProgramUnit`; ela não muta AST, occurrences ou resolution, não cria occurrences para subject/operator omitidos e distingue provenance `WRITTEN` de `INHERITED` sem inventar source span. A projeção materializa a relation normalizada sem afirmar admissibilidade type-sensitive: type compatibility não é name binding e pertence a etapa posterior conceitual (`ConditionValidation`), ainda inexistente.
- **Rationale:** consumidores futuros precisam de um predicate único sem duplicar o state machine IBM nem falsificar identidade, uso escrito ou provenance; validade type-sensitive exige declaração/tipo e contratos IBM que o projector não possui.
- **Scope:** futuros `ConditionSemantics` e `ConditionValidation`, joins entre produtos, snapshots e consumidores CFG/predicate/dataflow.
- **Related ADRs:** ADR-0002, ADR-0003, ADR-0005 e ADR-0012.
- **Enforcement:** `REVIEW` — nenhum projector ou validador de produção existe neste checkpoint; a futura implementação deve adicionar oracles de identidade, ambiguity, provenance e escala.
- **Known exceptions:** `ConditionSemantics` e `ConditionValidation` ainda não existem; a ausência continua boundary/incompletude e não autoriza normalização em outra fase nem declaração de validade type-sensitive pelo binding.

## Provenance

### INV-PROV-001 — Sem falsa identidade

- **Statement:** uma fase transformada não recria `SourceMap.identity` sobre seu próprio texto.
- **Rationale:** posições precisam permanecer ligadas ao fonte físico.
- **Scope:** normalização, preprocessing, COPY e AST.
- **Related ADRs:** ADR-0002.
- **Enforcement:** `PARTIALLY_AUTOMATED` — testes de provenance e futuro check arquitetural.
- **Known exceptions:** fixtures que constroem deliberadamente fonte já normalizado como entrada inicial.

### INV-PROV-002 — Exatidão observável

- **Statement:** provenance aproximada permanece distinguível de provenance exata.
- **Rationale:** consumidores não podem atribuir precisão que a transformação perdeu.
- **Scope:** segmentos, diagnostics, AST e snapshots.
- **Related ADRs:** ADR-0002.
- **Enforcement:** `AUTOMATED` — `SourceProvenanceTest` e testes de normalização.
- **Known exceptions:** nenhuma.

## Resolução

### INV-RES-001 — Ambiguidade preservada

- **Statement:** uma decisão ambígua conserva todos os candidatos válidos e não expõe candidato selecionado.
- **Rationale:** ordem ou conveniência não é regra COBOL.
- **Scope:** resolvers, produto e snapshots.
- **Related ADRs:** ADR-0003.
- **Enforcement:** `AUTOMATED` — contrato de `ReferenceResolution.Entry` e testes adversariais.
- **Known exceptions:** nenhuma.

### INV-RES-002 — Binding nominal não infere valores

- **Statement:** resolução associa nomes a entidades, mas não deduz valores de runtime ou targets dinâmicos finais.
- **Rationale:** essa informação pertence a CFG/dataflow futuros.
- **Scope:** resolução DATA, PROGRAM e CALL.
- **Related ADRs:** ADR-0004.
- **Enforcement:** `AUTOMATED` — testes de CALL dinâmico e contrato de occurrences/resolution.
- **Known exceptions:** literal de programa continua sendo sintaxe explícita, não inferência de valor.

### INV-RES-003 — Dependência externa literal termina no artefato

- **Statement:** `CALL` literal sem program unit interno visível resulta em `EXTERNAL_OBSERVED`, sem candidate externo e sem scan fora da compilation unit.
- **Rationale:** o literal prova a dependência nominal, mas não autoriza inventar identidade ou análise interartefatos.
- **Scope:** resolução PROGRAM/CALL externo.
- **Related ADRs:** ADR-0010.
- **Enforcement:** `AUTOMATED` — testes de resolução PROGRAM/CALL externo e motivo `LITERAL_EXTERNAL_PROGRAM`.
- **Known exceptions:** opções de linkage inválidas ou ausentes continuam incertezas separadas.

## Extensões de plataforma

### INV-EXT-001 — Semântica COBOL não incorpora interpretação de plataforma

- **Statement:** o frontend pode reconhecer tokens/productions de plataforma e a AST pode preservar payload e language tag em `EmbeddedLanguageStatement`; essa representação sintática não autoriza interpretar semântica de plataforma nos nós COBOL canônicos, symbol table, collector ou resolver nominal, nem fazê-los depender de extensões concretas.
- **Rationale:** preservar sintaxe reconhecida mantém fidelidade e provenance; interpretá-la nas fases COBOL redefiniria silenciosamente a linguagem e forçaria mudanças transversais para cada tecnologia.
- **Scope:** frontend/AST como fronteira de preservação, produtos semânticos COBOL e futuras extensões.
- **Related ADRs:** ADR-0003, ADR-0007 e ADR-0011.
- **Enforcement:** `PARTIALLY_AUTOMATED` — `ArchitectureBoundaryTest` bloqueia dependências reversas do core COBOL para classificação concreta; `ExternalCicsCharacterizationTest` e `CicsIntrinsicClassifierTest` protegem a separação comportamental; interpretação de plataforma adicional continua sujeita a review.
- **Known exceptions:** nenhuma; identificação opaca de SQL/CICS/SQLIMS é parte permitida do statement, não interpretação semântica externa.

### INV-EXT-002 — Evidência COBOL precede classificação externa inferida

- **Statement:** uma construção explicada pela resolução COBOL não pode ser reclassificada como externa apenas por grafia; classifier inferido só atua após o fracasso da explicação COBOL do construct inteiro.
- **Rationale:** o modo real de compilação pode estar ausente e nomes de plataforma também podem ser nomes COBOL válidos.
- **Scope:** classifiers pós-resolução e composição de resultados.
- **Related ADRs:** ADR-0011.
- **Enforcement:** `AUTOMATED` — EVAL-EXT-001 em `CicsIntrinsicClassifierTest` cobre precedência COBOL, estados `AMBIGUOUS`/`UNSUPPORTED`, filho unresolved e relações metamórficas de declaração.
- **Known exceptions:** metadata futura de compilação, quando explícita e confiável, poderá governar outro caminho mediante work item e policy próprios.

### INV-EXT-003 — Classificação externa inferida preserva incerteza e provenance

- **Statement:** classificação sem contexto confiável de compilação permanece distinguível de binding e de evidência externa comprovada, com tecnologia, kind, motivo, certeza, construct, source span, origem física e include chain observáveis.
- **Rationale:** hipótese plausível não equivale a verdade sintática nem pode elevar a claim de completude.
- **Scope:** produto de classificação externa, relatórios, snapshots e diagnostics.
- **Related ADRs:** ADR-0002, ADR-0008 e ADR-0011.
- **Enforcement:** `AUTOMATED` — `ExternalClassificationProductTest` e `ExternalClassificationProjectionTest` verificam imutabilidade, certeza inferida, motivo, span, origem, include chain serializável e claim conservadora; INV-PROV-002 e INV-COV-001 continuam aplicáveis.
- **Known exceptions:** nenhuma.

### INV-EXT-004 — Classificação externa possui fronteira de construct

- **Statement:** uma classificação externa referencia o nó-raiz e todas as occurrences artificiais pertencentes à interpretação COBOL intermediária do mesmo subtree; a projeção agregada não mantém esses componentes como gaps COBOL independentes nem os apaga sem fato substituto.
- **Rationale:** classificar somente o nome-base deixaria argumentos/subscripts como falsos defeitos de binding; removê-los silenciosamente perderia auditabilidade.
- **Scope:** classifiers, composição do relatório e apresentação.
- **Related ADRs:** ADR-0003, ADR-0008 e ADR-0011.
- **Enforcement:** `AUTOMATED` — `CicsIntrinsicClassifierTest` e `ExternalClassificationProjectionTest` verificam coverage determinística do subtree, substituição dos gaps cobertos e preservação dos gaps externos ao construct.
- **Known exceptions:** occurrences externas ao subtree ou não cobertas explicitamente continuam com seu resultado COBOL original.

## Cobertura e linguagens embarcadas

### INV-COV-001 — Incompletude bloqueia completude

- **Statement:** cada fronteira semântica materializada aprovada possui exatamente um finding concreto; `UNSUPPORTED`, `INPUT_MISSING` ou `DEPENDENCY_UNKNOWN` impedem alegação de cobertura completa mesmo sem occurrence nominal.
- **Rationale:** construção não compreendida não equivale a efeito vazio.
- **Scope:** AST coverage e relatório de resolução.
- **Related ADRs:** ADR-0008.
- **Enforcement:** `AUTOMATED` — `SemanticCoverage`, `AstSemanticBoundaryRequiredOracleTest` e testes de relatório/snapshot.
- **Known exceptions:** `NOT_DEPENDENCY_BEARING` somente quando classificado explicitamente.

### INV-COV-003 — Incompletude preserva fatos independentes

- **Statement:** input externo ausente adiciona incerteza e bloqueia claims incompatíveis de completude, mas não elimina fatos semânticos independentemente sustentados pelos inputs e produtos estruturalmente disponíveis; uma fase só deixa de produzir o resultado afetado quando faltam seus pré-requisitos estruturais.
- **Rationale:** ausência de input e corrupção interna têm consequências epistemológicas distintas; transformar ambas em produto vazio apaga conhecimento válido, enquanto ignorar a ausência inventa certeza.
- **Scope:** composição de produtos parciais, gaps, classifiers pós-resolução e apresentação.
- **Related ADRs:** ADR-0008 e ADR-0011.
- **Enforcement:** `PARTIALLY_AUTOMATED` — EVAL-COV-003 cobre identidade tipada de COPY COBOL não resolvido independente do wording, `CopyInputCompleteness` separado da integridade estrutural, projeção externa conservadora, fail-closed estrutural e relações metamórficas; outras categorias de input externo permanecem sujeitas a review e work item próprio.
- **Known exceptions:** quando ausência ou corrupção impede construir coerentemente o produto exigido, a fase afetada continua fail-closed; o slice automatizado atual não cobre parser recovery, lexer errors, preprocessing inválido, CFG ou dataflow.

### INV-COV-002 — Cobertura gramatical fechada

- **Statement:** toda regra do frontend versionado possui classificação explícita; regra nova sem política falha.
- **Rationale:** evita suporte implícito por fallback.
- **Scope:** gramáticas, manifesto e preprocessor.
- **Related ADRs:** ADR-0009.
- **Enforcement:** `AUTOMATED` — `GrammarCoverageManifestTest`, `ReferenceResolutionManifestTest` e policy do preprocessor.
- **Known exceptions:** nenhuma.

### INV-EMB-001 — Linguagem embarcada permanece opaca

- **Statement:** payload SQL/CICS/SQLIMS é preservado, mas dependências não são extraídas por regex no core COBOL.
- **Rationale:** análise completa exige parser/analisador dedicado.
- **Scope:** AST, coverage e futuras extensões.
- **Related ADRs:** ADR-0007.
- **Enforcement:** `PARTIALLY_AUTOMATED` — testes de AST/coverage e review.
- **Known exceptions:** nenhuma no core atual.

## Determinismo e desempenho

### INV-DET-001 — Ordem semântica determinística

- **Statement:** mesma entrada e policy produzem IDs e saída semântica na mesma ordem.
- **Rationale:** snapshots, diagnostics e evals precisam ser reproduzíveis.
- **Scope:** units, símbolos, occurrences, resolution e snapshots.
- **Related ADRs:** ADR-0005.
- **Enforcement:** `AUTOMATED` — construtores validam IDs contíguos e testes de determinismo/escala.
- **Known exceptions:** métricas operacionais de tempo não pertencem ao snapshot determinístico.

### INV-PERF-001 — Otimização preserva semântica

- **Statement:** lookup nominal usa índices pertinentes e não reduz candidatos ou profundidade para ganhar desempenho.
- **Rationale:** desempenho não pode alterar o problema semântico.
- **Scope:** resolvers e futuras análises.
- **Related ADRs:** ADR-0003 e ADR-0005.
- **Enforcement:** `PARTIALLY_AUTOMATED` — métricas/teste de escala e review de complexidade.
- **Known exceptions:** nenhuma redução semântica implícita.
