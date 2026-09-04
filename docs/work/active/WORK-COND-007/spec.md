# WORK-COND-007 — Broad corpus regression for contextual conditions

## Problema

Os Slices 1–6 de `BACKLOG-COND-001` fecharam a condição de superfície, a estrutura nominal, occurrences contextuais e `SEARCH WHEN`. Falta verificar se esses contratos continuam estáveis quando aplicados a programas COBOL reais, com COPYs, tamanhos, estilos e integrações diferentes das fixtures sintéticas.

Este checkpoint é Discovery + Corpus Characterization. Um resultado inesperado é evidência para investigação, não autorização para alterar produção.

## Objetivo

Caracterizar o corpus existente e um subconjunto reproduzível do AWS Mainframe Modernization CardDemo; medir a cadeia fonte → AST → occurrence → resolution; procurar perda, duplicação, classificação indevida, ambiguidade e incerteza; e deixar findings refutáveis, oracles mínimos e recomendações para review humano.

Pergunta central: as decisões semânticas implementadas nos Slices 1–6 continuam corretas quando expostas a um corpus COBOL significativamente mais amplo e mais próximo de código real?

## Domínio de entrada suportado

O corpus é analisado pela pipeline configurada no repositório, preservando source format, preprocessing e COPY. Serão aceitos como produtos de avaliação tanto análises completas quanto análises parciais fechadas, desde que o motivo da incompletude seja registrado. Um programa só é considerado semanticamente comparável quando normalização, preprocessing, parsing, AST, occurrences e resolução terminam com os produtos correspondentes.

O corpus externo será fixado por repositório, commit SHA, data, licença e closure de COPY. A seleção não será tratada como representação estatística de todo COBOL nem do CardDemo.

## Classes semânticas

Serão inventariadas, quando presentes: conditions de relação e abreviadas; tails contextuais; condition-names standalone; qualification e subscripts; DATA, INDEX e CONDITION; `PERFORM UNTIL`; `EVALUATE`; `SEARCH`/`SEARCH ALL`/`WHEN`; `VARYING`; `NOT`, `AND`, `OR`; agrupamento; occurrences; resolução, ambiguidade, unresolved e unsupported; determinismo e provenance.

## Premissas

- Corpus, fixtures e testes são evidência; os contratos de domínio, ADRs e invariants permanecem a autoridade.
- O CardDemo será usado somente com arquivos necessários à seleção e à closure de dependências.
- Arquivos upstream selecionados permanecem byte-for-byte quando copiados; configuração local de harness é preferida a alteração de COBOL.
- COPY ausente produz `COPY_NOT_FOUND` e análise parcial, nunca stub ou declaração inventada.
- Cardinalidade será verificada por significado e cadeia de produtos; não haverá oracle global de quantidade acidental.
- WAUX real será incluído somente se estiver disponível no corpus autorizado; caso contrário, o oracle sintético existente será preservado e a ausência será explícita.

## Comportamento esperado

Cada programa selecionado terá uma matriz com parse, AST, occurrences, resolution, diagnósticos, unresolved, ambiguous, unsupported, conditions e métricas observacionais. Para cada nominal estruturalmente compreendido, a caracterização tentará preservar a relação `nominal escrito → nominal AST → occurrence → resolution entry`, mantendo qualification e subscript como estrutura própria quando aplicável.

Findings serão classificados entre `CONFIRMED_BUG`, `EXPECTED_BEHAVIOR`, `EXPECTED_UNRESOLVED`, `UNSUPPORTED`, `GRAMMAR_GAP`, `NORMATIVE_VALIDATION_GAP`, `CORPUS_INVALID`, `TEST_GAP` e `NEEDS_ARCHITECTURAL_DECISION`.

## Comportamento diante de incerteza

Unresolved, ambiguous, unsupported, dependency missing, preprocessing failure e source-format failure permanecem observáveis. Para cada finding potencialmente bug será aplicado o template BR-01..BR-15: observation; minimal reproducer; AST; occurrences; resolution; contrato normativo/arquitetural; contraexemplo adversarial; tentativa de refutação; classificação. Se a única correção depender de heurística local, o finding será parado como `HEURISTIC_FIX_REJECTED` e não haverá implementação.

## Fora de escopo

Não fazem parte deste checkpoint: correções em `src/main/**`; mudanças de gramática, AST, collector, resolver, manifesto ou baselines produtivos; validação runtime, CFG, dataflow, valores dinâmicos, targets finais de CALL, semântica completa de CICS/SQL/IMS/MQ; normalização para mascarar fonte upstream; stubs para COPY ausente; e benchmark definitivo dependente de hardware.

## Regras de domínio relacionadas

- Condition surface lossless e sem binding: `docs/domain/semantic-ast.md`.
- Occurrences shape-sensitive, namespaces admissíveis e routing de `EVALUATE`/`SEARCH WHEN`: `docs/domain/reference-resolution.md`.
- Provenance e análise parcial: `docs/domain/provenance.md`.
- Oracles de condições e WAUX-like: `docs/evals/conditional-expression-oracles.md`.
- Seleção e expansão de corpus são caracterização; não alteram a especificação da linguagem.

## ADRs/invariantes relacionados

ADR-0002, ADR-0003, ADR-0005, ADR-0007, ADR-0008, ADR-0009, ADR-0010 e ADR-0012; `INV-AST-001`, `INV-AST-002`, `INV-AST-003`, `INV-SYM-001`, `INV-RES-001`, `INV-RES-002`, `INV-PROV-002`, `INV-COV-001`, `INV-COV-002`, `INV-DET-001` e `INV-PERF-001`.

## Limite deste checkpoint

O resultado de saída é `DISCOVERY_REVIEW_READY` somente após corpus, provenance, baseline, inventory, findings e tentativas de refutação estarem documentados. Aguardará review humano antes de qualquer implementação.
