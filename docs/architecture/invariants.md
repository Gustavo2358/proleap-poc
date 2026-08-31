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

### INV-SYM-001 — Símbolos não executam binding

- **Statement:** symbol tables modelam declarações, escopos, namespaces, entidades e relações declarativas; usos continuam `NOT_PERFORMED` até a resolução.
- **Rationale:** separa inventário nominal de decisão de lookup.
- **Scope:** builders e modelos de símbolos.
- **Related ADRs:** ADR-0003.
- **Enforcement:** `AUTOMATED` — `ArchitectureBoundaryTest` bloqueia dependência direta de construção de símbolos para ANTLR/resolução; testes de symbol table, occurrences e compilation units completam o contrato comportamental.
- **Known exceptions:** nenhuma.

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

- **Statement:** `UNSUPPORTED`, `INPUT_MISSING` ou `DEPENDENCY_UNKNOWN` impedem alegação de cobertura completa.
- **Rationale:** construção não compreendida não equivale a efeito vazio.
- **Scope:** AST coverage e relatório de resolução.
- **Related ADRs:** ADR-0008.
- **Enforcement:** `AUTOMATED` — `SemanticCoverage` e testes de relatório/snapshot.
- **Known exceptions:** `NOT_DEPENDENCY_BEARING` somente quando classificado explicitamente.

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
