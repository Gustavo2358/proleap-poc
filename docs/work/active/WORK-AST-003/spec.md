# WORK-AST-003 — Correção da consistência global entre IDs e traversal da AST

## Problema

`AstSnapshot.from` rejeita ASTs construídas sem erro para `PERFORM` procedure com condição, porque a sequência de `Ast.Meta.id` diverge do pre-order definido por `Ast.children`. O Discovery aprovado no PR #11 confirmou duas causas: ordem de alocação invertida em `buildPerform` e consumo do contador estrutural por diagnostics de `declarationVisibility`.

## Objetivo

Preservar a política atual e corrigir as duas fontes confirmadas de violação, promover o oracle estrutural para o gate normal e documentar que IDs locais de `Ast.Node` correspondem ao canonical pre-order. A Fase 2 foi autorizada somente após o merge e review independente do PR #11.

## Domínio de entrada suportado

Program units aceitos pelo frontend COBOL configurado após normalização e preprocessing, com foco em ASTs que materializam `PerformStatement`, referências de procedure, expressões de controle, metadata diagnóstica e os demais tipos retornados por `Ast.children`. Os casos mínimos são `PERFORM procedure UNTIL` simples/composto e `PERFORM THRU ... UNTIL`; controles negativos cobrem procedure sem controle e inline `PERFORM` com controle.

## Classes semânticas

- Identidade local: `Ast.Meta.id` namespaced por `ProgramUnitId`.
- Ordem: textual COBOL, parse tree, execução do builder, estrutura `Ast.children` e snapshot são dimensões distintas.
- Reachability: cada nó final semanticamente materializado deve ser alcançado uma vez pela traversal canônica.
- Metadata não estrutural: diagnostics podem carregar `Ast.Meta`, mas não são `Ast.Node`.
- Consumidores: snapshot, UI, scopes, símbolos, coverage, occurrences, resolvers, classifiers e relatórios.

## Premissas

- `ARCHITECTURE_GUARANTEED`: IDs são locais ao program unit, determinísticos e usados para joins entre produtos separados.
- `SPECIFICATION_GUARANTEED`: `Ast.children` é a traversal estrutural canônica atual e `AstSnapshot` exige igualdade entre posição pre-order e `Meta.id`.
- `SPECIFICATION_GUARANTEED`: o review do Discovery aprovou contiguidade/pre-order como contrato representacional e autorizou sua promoção canônica.
- `OBSERVED_IN_CURRENT_CORPUS_ONLY`: o corpus principal passar no baseline não prova a propriedade para caminhos não exercitados.

## Comportamento esperado

1. `buildPerform` materializa referências de procedure antes dos controles, na ordem publicada por `Ast.children`.
2. `declarationVisibility` reutiliza a metadata estrutural de `FileDescription` e `DataEntry` sem avançar `nextId`.
3. O diagnostic conflitante preserva code, presença, span, provenance e vínculo com a declaração.
4. O oracle normal verifica reachability única, ausência de ciclos/nulos e `meta.id == posição` em `O(nodes)`.
5. Os três triggers passam por `AstSnapshot`; os controles negativos permanecem verdes.
6. IDs quebrados específicos permanecem apenas na evidência histórica, sem hardcodes substitutos.
7. Produtos posteriores preservam shape, coverage, provenance e joins coerentes.
8. Gates `fast`, `semantic` e `full` permanecem verdes sem relaxar baselines.

## Comportamento diante de incerteza

Nova violação que se enquadre diretamente no invariant aprovado recebe teste mínimo e correção localizada. Caso exija política de identidade, traversal ou arquitetura adicional, a implementação para e reporta antes de ampliar escopo.

## Fora de escopo

- Alterar `Ast.children`, `Ast.Meta`, `AstSnapshot` ou a política global de IDs.
- Introduzir reindexação pós-build, ID sentinela ou segundo contador.
- Iniciar WORK-AST-002 Slice 2.
- Alterar grammar, parser, coverage taxonomy, símbolos, occurrences, binding, CFG ou dataflow.
- Tornar IDs persistentes entre edições do fonte.

## Regras de domínio relacionadas

`docs/domain/semantic-ast.md`, `docs/domain/compilation-units.md`, `docs/domain/symbol-model.md`, `docs/domain/reference-resolution.md` e `docs/domain/provenance.md`.

## ADRs/invariantes relacionados

ADRs 0002, 0003, 0005 e 0008. INV-AST-001, INV-AST-002, INV-AST-003, INV-PROV-002, INV-COV-001 e INV-DET-001.
