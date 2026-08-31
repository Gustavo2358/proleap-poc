# WORK-EXT-001 — Plano

## Fatiamento

1. **Oracle vermelho:** adicionar fixtures mínimas e testes de characterization para nomes COBOL válidos, table call resolvido, possíveis constructs CICS, negative controls, shapes adversariais e agrupamento do subtree. Confirmar a parse/AST real de cada host antes de definir a factory do classifier.
2. **Produto ortogonal mínimo:** introduzir o menor modelo imutável de classificação externa e um classifier CICS restrito a `DFHRESP`/`DFHVALUE`. Indexar AST/entries por identidade composta uma vez; não alterar resolver, occurrences ou AST.
3. **Composição auditável:** integrar uma chamada explícita e única após a resolução no composition root atual, passar o produto ao relatório/snapshot e projetar um fato por construct com occurrence IDs cobertos. Execução sem classifier deve preservar a saída anterior.
4. **Challenge pass:** tentar falsificar precedência COBOL, limite de shape, independência de argumento, case-insensitivity, determinismo, provenance e ausência de gaps internos artificiais.
5. **Encerramento:** executar gates, promover EVAL-EXT-001 para cobertura executável, atualizar enforcement factual e arquivar o work item sem iniciar os backlogs de extensibilidade.

## Dependências

- ADR-0011 e INV-EXT-001 a INV-EXT-004 já definem a fronteira normativa.
- AST estruturada, occurrence identities e resolução nominal atuais são entradas obrigatórias.
- Nenhuma dependência de `BACKLOG-EXT-001` ou das futuras capacidades é necessária; este slice cria somente a abstração mínima demonstrada pelo primeiro caso.
- Metadata CICS, External Symbol Providers, embedded analyzers, CFG e dataflow permanecem ausentes e não podem ser simulados.

## Superfície arquitetural provável

- Novo produto imutável para classifications e coverage de occurrences, no package atual enquanto `BACKLOG-ARCH-001` não for executado.
- Classifier CICS pequeno que consome AST indexada e `ReferenceResolution`, sem dependência reversa das fases canônicas.
- Uma composição pós-resolução em `ExplorerMain`, atual composição raiz, sem registry genérico.
- `ResolutionAnalysisReport` e `ResolutionSnapshot` recebem o novo produto como entrada de projeção; apresentação não recomputa classificação.
- `ArchitectureBoundaryTest` deve bloquear dependências de AST/símbolos/resolver para classifiers concretos.

Nomes finais de tipos e assinatura são decisões da implementação. A modelagem deve tornar impossível confundir classification status com `ResolutionStatus`.

## Migrações requeridas

- Não há migração de gramática, AST, symbol table, occurrences ou `ReferenceResolution`.
- O schema do snapshot poderá ganhar uma coleção aditiva de classifications e contagens próprias; baselines só mudam quando o diff for explicado pelo oracle.
- Gaps cobertos mudam de projeção `REFERENCE_BINDING` por occurrence para um fato externo inferido por construct; a claim continua conservadora.
- EVAL-EXT-001 sai de `planned` somente quando teste e fixture executáveis existirem.

## Artefatos esperados

- Produto e classifier mínimos de classificação externa.
- Fixtures focadas para COBOL válido, CICS possível, negative controls e shapes adversariais.
- Testes de produto, classifier, composição de relatório, snapshot, provenance, determinismo e boundary.
- Snapshot/UI capaz de exibir classificação, motivo, certeza e occurrences cobertas sem ocultar o binding original.
- Documentação canônica atualizada apenas se a implementação revelar diferença material em relação ao contrato já aceito.
