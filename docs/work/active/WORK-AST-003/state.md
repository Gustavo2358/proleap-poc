# Estado

## Onde estamos

Discovery concluído e validado sobre a base `c6d9b6e1b597f34db06b41f4e8e04cdcf1d68a3a` em `discovery/work-ast-003-preorder-invariant`. O refinamento para review corrigiu a descrição do Caso B, explicitou a função apenas observacional dos IDs quebrados e ampliou a auditoria de `declarationVisibility`; nenhuma correção de produção foi iniciada.

## Verde conhecido

- Baseline anterior às fixtures: gate `full` verde.
- Teste focal normal: 6 testes, 5 verdes e 1 required oracle skipped por opt-in.
- Gates finais `fast`, `semantic` e `full` verdes; `git diff --check` verde.
- Oracle required vermelho esperado em `ProcedureReference: expected 14 but got 21`.
- Dois casos mínimos chegam a AST e falham em `AstSnapshot` em `ProcedureReference`: `expected 14 got 21` e `expected 14 got 17`.
- Reachability exercitada não mostrou ciclo, instância duplicada nem filho nulo.

## Restante

- O checkpoint foi publicado no PR #11 a partir do commit `f6bd0d2`.
- Aguardar review independente. Fase 2 e WORK-AST-002 Slice 2 permanecem sem autorização.

## Descobertas que afetam o plano

- `buildPerform` é violação confirmada somente no ramo procedure quando há expressão de controle materializada; procedure sem controle e inline com controle são consistentes.
- Há segunda violação confirmada: `declarationVisibility` aloca `Meta` diagnóstico pelo contador AST e cria gap sem nó (`expected 8 got 9`).
- Os dois call sites atuais, `FileDescription` e `DataEntry`, podem produzir o conflito; a fixture `01 ... EXTERNAL GLOBAL` exercita apenas o segundo e não delimita o defeito.
- Pre-order é contrato atual intencional de representação/snapshot e histórico, mas está subdocumentado no domínio/invariants.
- A recomendação é preservar a política atual, corrigir as duas fontes na Fase 2 e promover um oracle estrutural genérico; reindexação pós-build e remoção do requisito têm custo e risco maiores.
- Os IDs quebrados exatos continuam documentados somente como evidência; a regressão futura deverá exigir `id == posição` no pre-order de `Ast.children`, sem substituir hardcodes por novos números.
