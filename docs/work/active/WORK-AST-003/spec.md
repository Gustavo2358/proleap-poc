# WORK-AST-003 — Discovery da consistência global entre IDs e traversal da AST

## Problema

`AstSnapshot.from` rejeita ASTs construídas sem erro para `PERFORM` procedure com condição, porque a sequência de `Ast.Meta.id` diverge do pre-order definido por `Ast.children`. A hipótese inicial aponta para `buildPerform`, mas o shared counter de metadata pode permitir outras violações e precisa ser auditado antes de qualquer correção.

## Objetivo

Reproduzir o sintoma no pipeline real, descobrir o contrato atual de identidade/order/reachability, classificar todos os consumidores, auditar todos os tipos de `Ast.Node` e todas as fontes de alocação, desenhar um oracle genérico e recomendar a menor correção arquitetural. Esta execução termina em relatório, testes de Discovery, commit e PR; produção permanece inalterada.

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
- `UNCERTAIN`: a documentação canônica diz apenas “ordem determinística de construção”; o status normativo de contiguidade/pre-order precisa de review e promoção explícita.
- `OBSERVED_IN_CURRENT_CORPUS_ONLY`: o corpus principal passar no baseline não prova a propriedade para caminhos não exercitados.

## Comportamento esperado

1. O Discovery distingue sucesso de preprocessing, lexer, parser e AST da falha posterior de apresentação.
2. Cada discrepância registra trigger, nó, ID esperado, ID real e cadeia causal.
3. Todos os 48 tipos de `Ast.Node` são classificados contra seu builder e `Ast.children`, e todos os call sites de helpers que podem consumir o contador são auditados.
4. Consumidores são classificados por unicidade, determinismo, contiguidade, pre-order ou uso não ordinal.
5. Reachability, duplicação e ciclos são auditados sem confundir objetos intermediários do builder com a árvore final.
6. Oráculos intencionalmente vermelhos ficam opt-in; gates normais continuam verdes.
7. Nenhuma hipótese vira correção de produção nesta fase.
8. IDs quebrados específicos permanecem evidência observacional; o aceite futuro é derivado do pre-order de `Ast.children`, com `id == posição esperada`, e não de hardcodes substitutos.

## Comportamento diante de incerteza

Ausência de contrato explícito é finding, não licença para escolher uma política. Tipos sem caminho atual de materialização ficam `NÃO APLICÁVEL`; condicionais não reproduzidas ficam `SUSPEITO`. A recomendação deve declarar quais garantias são necessárias aos produtos atuais e quais são apenas conveniência de representação.

## Fora de escopo

- Alterar `AstBuilder`, `Ast.children`, `Ast.Meta`, `AstSnapshot` ou qualquer consumidor de produção.
- Corrigir `PERFORM`, gaps diagnósticos ou outros findings.
- Iniciar WORK-AST-002 Slice 2.
- Alterar grammar, parser, coverage taxonomy, símbolos, occurrences, binding, CFG ou dataflow.
- Tornar IDs persistentes entre edições do fonte.

## Regras de domínio relacionadas

`docs/domain/semantic-ast.md`, `docs/domain/compilation-units.md`, `docs/domain/symbol-model.md`, `docs/domain/reference-resolution.md` e `docs/domain/provenance.md`.

## ADRs/invariantes relacionados

ADRs 0002, 0003, 0005 e 0008. INV-AST-001, INV-AST-002, INV-PROV-002, INV-COV-001 e INV-DET-001. O Discovery avalia a necessidade de um invariant AST adicional, mas não o cria antes do review.
