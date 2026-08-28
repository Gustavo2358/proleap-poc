# Invariantes arquiteturais

IDs neste documento são estáveis. `AUTOMATED` indica proteção executável atual; `PARTIALLY_AUTOMATED` combina testes com review; `REVIEW` depende hoje de inspeção semântica.

## AST e fases

### INV-AST-001 — AST sem produtos de análise

- **Statement:** a AST não contém bindings, symbol tables, CFG ou resultados de dataflow.
- **Rationale:** mantém estrutura semântica reutilizável e evita ciclos entre fases.
- **Scope:** `Ast`, `AstBuilder` e consumidores.
- **Related ADRs:** ADR-0003.
- **Enforcement:** `PARTIALLY_AUTOMATED` — testes de AST/ocorrências e futuro check de bytecode.
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
- **Enforcement:** `AUTOMATED` — testes de symbol table, occurrences e compilation units.
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

### INV-RES-003 — Programa externo exige catálogo explícito

- **Statement:** ausência do catálogo externo resulta em incerteza própria; não prova inexistência do programa.
- **Rationale:** falta de input não é fato negativo.
- **Scope:** resolução PROGRAM/CALL externo.
- **Related ADRs:** ADR-0006.
- **Enforcement:** `AUTOMATED` — testes de resolução de programas e motivo `EXTERNAL_CATALOG_NOT_PROVIDED`.
- **Known exceptions:** catálogo fornecido e vazio pode provar ausência dentro do catálogo configurado.

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
