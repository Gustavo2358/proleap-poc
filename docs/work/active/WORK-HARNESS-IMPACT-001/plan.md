# Plano — WORK-HARNESS-IMPACT-001

## Fatiamento

1. Criar a fonte canônica da taxonomia, incluindo classe primária única,
   `earliest broken layer wins`, as oito classes incluindo
   `BLOCKS_DEPENDENCY_FACTS`, evidência, `UNASSESSED`, `NOT_APPLICABLE`,
   separação de prioridade e orientação de reassessment.
2. Integrar a regra ao roteamento do harness e ao protocolo de work items sem
   alterar contratos de produção ou exigir migração dos findings existentes.
3. Estender o `HarnessDocsTest`, que já valida documentação e work-item YAML,
   para fechar o vocabulário, validar conteúdo mínimo dos blocos
   `downstream_impact` e detectar inconsistências locais de lifecycle/routing.
4. Arquivar o `WORK-COND-007` já mergeado, preservando um resumo histórico
   curto e retirando o diretório e o link do roteamento ativo.
5. Aplicar o contrato ao único finding F-01 já confirmado, registrando somente
   seu impacto `UNASSESSED`, executar a self-validation A–H e os gates antes do
   review humano; não arquivar este work item.

## Dependências

- `docs/engineering/work-item-protocol.md` e o validator existente
  `HarnessDocsTest`.
- `docs/architecture/pipeline.md`, invariantes e política de análise semântica.
- Evidência já registrada para F-01 em `BACKLOG-RES-003` e
  `WORK-COND-007`; nenhuma nova investigação desse finding é necessária.
- PR #21 mergeado em `main`, confirmado no GitHub antes do arquivamento de
  `WORK-COND-007`.

## Superfície arquitetural provável

A mudança fica em documentação de `docs/engineering`, roteamento de
`docs/index.md`, `AGENTS.md`, índice de work items e o teste documental
`HarnessDocsTest`. Não há incisão em `src/main`, grammar, AST, symbols,
occurrences, resolver ou qualquer camada downstream.

## Migrações requeridas

Não há migração em massa. A única atualização retroativa é o bloco de impacto
`UNASSESSED` do F-01 já confirmado em `docs/work/backlog.md`; ele não modifica a
classificação do finding, sua disposição de remediação ou a autorização
existente.

## Artefatos esperados

- `docs/engineering/downstream-impact-classification.md` como fonte canônica.
- Extensão de roteamento e protocolo para uso futuro.
- Validação documental mínima de vocabulário e campos.
- Self-validation executável de lifecycle, completude de fronteiras, campos
  vazios, usabilidade de schema e stale routing.
- `docs/work/active/WORK-HARNESS-IMPACT-001/` completo conforme o protocolo.
- Evidência de gates `docs`, `fast`, `semantic` e `full`, sem merge automático.
