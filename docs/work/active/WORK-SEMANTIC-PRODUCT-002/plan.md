# Plano

## Fatiamento

1. **Checkpoint 1 — Contrato executável e work item (este PR).** Registrar a
   decisão A2+B, o domínio exato `MOVE` literal → `CALL` variável, os tipos e
   invariantes da boundary, o oracle de review e a decomposição abaixo. Este
   checkpoint altera somente documentação e o índice de trabalho; não cria
   tipo, adapter, consumer ou integração de produção.
2. **Review humano obrigatório.** Confirmar que o contrato deste work item é
   suficiente e que o slice não está usando o literal do `MOVE` como análise de
   runtime. Nenhum checkpoint de implementação começa sem este review.
3. **Checkpoint 2 — Core A2 + port B, sem frontend.** Criar os tipos
   boundary-owned, imutáveis e namespaced para o estado mínimo e a facade
   read-only. O core deve poder ser construído diretamente em teste, sem
   parser, AST, symbols, resolver, report, ANTLR ou composition root. Validar
   closure, imutabilidade, invariantes de ordering e representação explícita de
   `UNKNOWN`/`INCOMPLETE`.
4. **Review humano obrigatório.** Verificar bytecode/dependências, ownership,
   ausência de collections mutáveis e que o port só seleciona fatos já
   materializados.
5. **Checkpoint 3 — Adapter de projeção e joins do slice.** Implementar o
   adapter COBOL-specific que consome os produtos atuais, localiza as shapes
   tipadas e reconcilia `ProgramUnitId` + node/occurrence IDs + candidate
   `DATA_SYMBOL`. Publicar declaração, literal, facts `MOVE`/`CALL`, ordering,
   policy, provenance e uncertainty sem reparse ou alteração nos produtos de
   entrada. Manter o adapter separado do package de tipos da boundary.
6. **Review humano obrigatório.** Comparar o resultado com o oracle 3B,
   desafiar joins por nome/ID local, ausência de options e qualquer tentativa de
   inferir runtime value. O review pode rejeitar uma projeção que carregue
   internals mesmo que os asserts felizes passem.
7. **Checkpoint 4 — Publicação no composition root e consumer independente.**
   Acoplar a menor chamada necessária no ponto em que AST, units, symbols,
   occurrences, resolution, report e provenance já estão coerentes. Publicar
   um único estado A2 em memória e permitir que um consumer de teste opere
   somente pelo port depois de o frontend ser liberado. Não alterar snapshots,
   CLI ou contratos existentes de apresentação, nem gerar
   `semantic-product.json` neste checkpoint. Exercitar determinismo por valor,
   ordering, provenance, policy e fechamento do lifecycle.
8. **Review humano obrigatório.** Verificar o consumer independente, o
   fechamento do state e do port e a separação explícita entre produto tipado e
   qualquer projeção de inspeção. O review não autoriza generalização,
   interchange, CFG/dataflow ou capability posterior.
9. **Checkpoint 5 — Inspection adapter determinístico.** Criar o adapter
   separado que consome somente `CobolSemanticState`/`CobolSemanticPort` e produz
   `semantic-product.json` para inspeção. A projeção deve ser determinística para
   a mesma entrada/análise, incluindo ordem observável de campos e coleções,
   sem timestamp, identidade incidental, metadata de ambiente ou dependência de
   frontend. Este checkpoint não cria serializer/framework genérico, schema
   público de interchange, round-trip, persistência ou abstração universal.
10. **Review humano obrigatório e encerramento do slice.** Executar os gates do
    contrato, revisar o diff completo, classificar qualquer finding novo pela
    taxonomia downstream e decidir se o produto está pronto para outro work
    item. O encerramento não autoriza generalização, interchange, CFG/dataflow
    ou qualquer capability posterior.

Cada checkpoint de implementação deve ser pequeno o suficiente para ser
revertido/revisado sozinho. A ordem impede que o adapter defina implicitamente a
API e impede que a integração no composition root esconda dependências de
lifecycle.

## Dependências

- O contrato aprovado nos Checkpoints 2, 3A e 3B de
  `WORK-SEMANTIC-PRODUCT-001`, preservado no [resumo histórico](../../history/WORK-SEMANTIC-PRODUCT-001.md),
  especialmente a recomendação H e o oracle
  `SemanticProductBoundaryCheckpoint3BTest`.
- AST, compilation units, symbol tables, occurrences, resolução nominal,
  `ResolutionAnalysisReport`, policy e provenance existentes na `main`.
- ADRs/invariantes e evals listados no `work-item.yaml`; os testes atuais são
  evidência e não substituem a regra do dialeto nem o contrato desta spec.
- Review/autorização de cada checkpoint anterior. Nenhum trabalho de
  WORK-AST-002, F-01, CFG, dataflow ou backlog paralelo é dependência implícita.

## Superfície arquitetural provável

```text
ExplorerMain / composition root
  ├─ mantém internals do frontend até a análise terminar
  └─ chama CobolMoveCallAdapter
           │
           ▼
  CobolSemanticProduct (A2: estado próprio e imutável)
           │
           ▼
  CobolSemanticPort (B: facade read-only)
           │
           ▼
  consumer independente do slice
```

O adapter de inspeção é uma saída downstream separada:

```text
CobolSemanticState / CobolSemanticPort
           │
           ▼
inspection adapter (somente no Checkpoint 5)
           │
           ▼
semantic-product.json
```

Os nomes são uma superfície de implementação focalizada, não uma promessa de
um Semantic Product universal. O package A2/B não deve importar o frontend; o
adapter é o único tradutor que conhece `Ast`, `CompilationUnitModel`,
`SymbolTable`, `ReferenceResolution`, report e seus índices. O composition root
não deve virar consumer nem transferir ownership dos providers vivos para o
port.

O adapter de inspeção do Checkpoint 5 é downstream separado: recebe somente o
state/port tipado e produz o artefato de inspeção. Um futuro `CobolLower` também
receberá somente o `CobolSemanticPort`; nenhum deles consome internals do
frontend e o lowerer não consome JSON.

O estado deve ser fechado por uma única construção. A ausência de um
`analysisGeneration` público não pode ser compensada misturando objetos de
análises distintas; nesta primeira versão, a coerência mínima é garantida pelo
mesmo objeto A2 contendo core, policy, status, provenance e facts do slice.
Identidade persistente entre publicações continua fora do contrato provado.

## Migrações requeridas

Nenhuma migração de AST, symbols, occurrences, resolução, grammar, fixture,
baseline ou snapshot é necessária. O Checkpoint 1 desta branch não implementa
nenhuma migração nem altera `src/main/**`.

Nos checkpoints posteriores, a única integração permitida é materializar o
produto após os produtos de análise necessários existirem. A implementação deve
preservar os produtos de entrada, manter a apresentação/inspeção como adapter
separado e evitar tornar o produto dependente do lifecycle do `ExplorerMain`. O
Checkpoint 5 não pode ser antecipado e não transforma `semantic-product.json` em
domínio. Não haverá serializer/framework genérico, schema público de
interchange, round-trip, persistência ou refatoração transversal como
pré-requisito oculto.

## Artefatos esperados

Neste checkpoint:

- `work-item.yaml`, `spec.md`, `plan.md`, `eval.md` e `state.md` deste item;
- entrada no `docs/work/index.md`;
- nenhum arquivo de produção, grammar, fixture ou teste alterado.

Nos checkpoints posteriores, somente quando autorizados:

- tipos A2/B em `src/main/java/.../semanticproduct/`;
- adapter de projeção do slice e a menor integração de publicação;
- testes de contrato, adapter, closure/lifecycle e consumer independente;
- em checkpoint posterior próprio, inspection adapter e
  `semantic-product.json`, com teste de determinismo para a mesma
  entrada/análise;
- eventual promoção de oracle/eval durável, se o review demonstrar repetição
  útil, sem copiar asserts para documentação canônica.
