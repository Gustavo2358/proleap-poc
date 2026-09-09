# Integridade de canonical coverage para NEXT SENTENCE

## Problema

`IF ... NEXT SENTENCE ELSE CONTINUE END-IF` constrói AST tipada, mas a publicação
do Semantic Product lança `IllegalArgumentException: typed AST node has no
canonical coverage finding`. O mesmo defeito afeta alternativas diretas de
`ifElse` e `searchWhen`. Root cause, matriz e comandos estão em [eval](eval.md).

## Objetivo

Discovery aprovado por autorização humana explícita em 2026-09-08. O slice
atual implementa o reparo focal do registro canônico no AstBuilder, compartilhando
a finalização de statements. A continuação permanece na branch
`codex/discovery-next-sentence-coverage`, neste work item e no PR #33 Draft.
Após implementação, gates e commit/push, exigir novo review humano sem merge.

## Domínio de entrada suportado

Fonte FIXED aceita pelo frontend atual. Casos normativos centrais: IF THEN/ELSE,
com ou sem END-IF, e SEARCH sequencial com WHEN NEXT SENTENCE. A fixture SEARCH
tem OCCURS, INDEXED BY e SET do índice. Probes SEARCH ALL e listas genéricas
caracterizam a gramática/AST, sem certificar todas as restrições IBM dessas formas.
Nenhum compilador IBM foi executado; a autoridade normativa é a documentação.

## Classes semânticas

- Alternativa direta: `ifThen`, `ifElse`, `searchWhen`; tokens sem StatementContext.
- Caminho normal: `statement → nextSentenceStatement`; finding presente.
- Controle de pipeline: troca por CONTINUE; não é equivalência de execução.
- Corrupção de produto: finding ausente/duplicado; falha fechada obrigatória.
- Incompletude legítima: NEXT_SENTENCE observado com controle ainda bloqueado.

## Premissas

- `LANGUAGE_GUARANTEED`: NEXT SENTENCE transfere para depois do próximo período
  separador, inclusive com END-IF/END-SEARCH; CONTINUE é no-op. Regra e fontes em
  [AST semântica](../../../domain/semantic-ast.md#next-sentence).
- `ARCHITECTURE_GUARANTEED`: INV-COV-001 exige exatamente um finding por boundary;
  INV-SP-004 proíbe recuperar coverage por nova análise no projector.
- `SPECIFICATION_GUARANTEED`: a aprovação humana autoriza o fix focal, testes/docs e
  commit/push no PR #33 Draft; exige novo review humano antes de merge.
- `OBSERVED_IN_CURRENT_CORPUS_ONLY`: os resultados da matriz são evidência da
  base identificada, não especificação isolada da linguagem.
- `UNCERTAIN`: representação futura de sentence/target na boundary ainda não
  decidida. Ordem de IDs ou provenance não pode substituir essa decisão.

## Comportamento esperado

Acceptance criteria do fix aprovado:

1. Toda ocorrência NEXT SENTENCE das três alternativas diretas e do caminho
   normal permanece `Ast.NextSentenceStatement`; cardinalidade/ownership/IDs,
   Meta, origem real e boundaries de sentence não mudam.
2. Cada Statement alcançável na matriz possui exatamente um finding por
   `(ProgramUnitId, astNodeId)`, com a mesma Meta/provenance e writtenText do
   contexto de construção. SearchWhen conserva seu próprio finding distinto.
3. O registro passa por finalização comum, reutilizando `recordCoverage` e
   `buildCoverageReport`; não há finding ad hoc no projector, parse sintético,
   reparsing, cobertura dupla no visitor ou fallback silencioso.
4. Preservar a policy do manifesto para a **origem gramatical real**. Equivalência
   entre caminhos significa presença/unicidade/identidade do finding, não
   igualdade artificial de readiness. O [plano](plan.md) explicita a diferença
   entre `ifThen`, `ifElse`, `searchWhen` e `nextSentenceStatement`.
5. Pipeline, port e JSON publicam todas as ocorrências como `ObservedStatement`
   com `NEXT_SENTENCE` / `TYPED_NEXT_SENTENCE`, provenance, gap e readiness
   lowering/CFG/effects-dataflow bloqueada; nunca como CONTINUE ou fallthrough.
6. Remover finding de um statement normal continua falhando fechado. Os novos
   oracles positivos e os negativos de duplicação não permitem relaxar o contrato.
7. Promover o oracle opt-in para execução normal após a autorização e substituir
   as expectativas de bug da caracterização pelos oracles positivos. Documentar
   a migração; não manter teste exigindo o crash depois do fix.
8. Passar fast, semantic e full; revisar o diff e parar para review no mesmo PR.

## Comportamento diante de incerteza

Coverage ausente é corrupção interna, distinta de capability unsupported. A
falha do projector permanece. O reparo produz um finding pela policy
canônica já existente; não poderá promover a semântica de transferência para
READY. A AST retém informação para evolução futura, mas o port 1.2.0 ainda não
é suficiente para calcular o destino preciso de NEXT SENTENCE sem evoluir seu
contrato; essa limitação deve permanecer explícita.

## Fora de escopo

Hardening geral AST↔coverage; mudança de gramática/manifestos; estreitamento
de span de wrappers; factory global de todos os Ast.Nodes; validator novo em
produção; publicação de sentence IDs/targets; interpretação de condições/SEARCH
ALL; AIR/IR, CobolLower, CFG, dataflow, runtime e efeitos. Nenhum merge, retirada
de Draft ou segundo PR para implementação. A higiene adjacente já concluída no discovery foi o archive
documental obrigatório de WORK-SEMANTIC-PRODUCT-005, cujo PR #32 já foi mergeado.

## Regras de domínio relacionadas

[AST semântica](../../../domain/semantic-ast.md),
[Semantic Product](../../../domain/cobol-semantic-product.md),
[condições e SEARCH](../../../domain/conditional-expressions.md) e
[pipeline cross-repo](../../../architecture/pipeline.md).

## ADRs/invariantes relacionados

ADR-0002, ADR-0008, ADR-0009, ADR-0013; INV-AST-001/002/003,
INV-PROV-002, INV-COV-001/002, INV-SP-001/002/003/004 e INV-DET-001.
