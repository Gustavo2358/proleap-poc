# WORK-EXT-001 — Especificação

## Problema

`DFHRESP(X)` e `DFHVALUE(X)` podem chegar à AST como `DataReference` estruturada de um `tableCall`. Quando a referência-base não possui declaração COBOL compatível, o binding nominal produz `UNRESOLVED`; o subscript pode produzir outra occurrence e outro gap. No contexto CICS, o construct completo possui uma explicação externa plausível, mas o pipeline não conhece em geral o modo real de tradução/compilação.

Alterar a gramática para impedir esses nomes como `cobolWord` rejeita COBOL válido. Hardcode no resolver mistura binding nominal com plataforma. Remover entries ou gaps sem fato substituto apaga incerteza e provenance.

## Objetivo

Depois da resolução COBOL, produzir um artefato imutável e ortogonal que classifique conservadoramente o construct inteiro como possível intrínseco CICS quando, e somente quando, a explicação COBOL relevante falhar. A composição de relatório deve apresentar um único fato externo inferido com as occurrences cobertas, sem converter o binding em sucesso e sem manter componentes internos artificiais como gaps COBOL independentes.

## Domínio de entrada suportado

O slice cobre somente constructs que o frontend atual materializa estruturalmente como `Ast.DataReference` originada de `tableCall` e que atendem a todas as condições:

- nome-base canonicalizado exatamente igual a `DFHRESP` ou `DFHVALUE`;
- shape parenthesized compatível com um argumento da forma conhecida, sem qualificação, grupos adicionais ou reference modification;
- entry da occurrence-raiz com status `UNRESOLVED` após a resolução COBOL canônica;
- AST, occurrences, resolução e provenance disponíveis para a mesma `ProgramUnitId`.

Argumentos aceitos pela shape não são limitados a `NORMAL`, `NOTFND`, `ERROR` ou outro catálogo de corpus. Variação de caixa segue a canonicalização COBOL. Constructs já materializados inequivocamente como literal CICS, se houver algum host atual nessa classe, não autorizam rebaixar a AST nem ampliar este classifier sem novo oracle.

## Classes semânticas

1. **COBOL resolvido:** nome simples, data item ou table call cuja referência-base possui declaração compatível. Preservar integralmente o binding COBOL e não classificar.
2. **Possível intrínseco CICS:** shape exata `DFHRESP(argument)` ou `DFHVALUE(argument)` cuja referência-base terminou `UNRESOLVED`. Emitir classificação externa `platform=CICS`, `kind=POSSIBLE_INTRINSIC`, certeza inferida e motivo estável.
3. **Semelhança superficial:** `DFHOTHER(X)`, `MY-TABLE(IDX)`, nome sem parênteses, nome qualificado, múltiplos grupos ou reference modification. Não classificar.
4. **Binding não conclusivo diferente de unresolved:** `AMBIGUOUS`, `UNSUPPORTED` ou input incompleto. Não promover para hipótese CICS; preservar o estado original.
5. **Construct inteiro:** a classificação raiz enumera deterministicamente todas as occurrence identities pertencentes à interpretação nominal intermediária do mesmo subtree, inclusive subscript interno; entries de resolução permanecem intactas.

## Premissas

- `ARCHITECTURE_GUARANTEED`: AST, occurrences e `ReferenceResolution` são produtos separados, imutáveis e ligados por `(ProgramUnitId, astNodeId/occurrenceId)`.
- `ARCHITECTURE_GUARANTEED`: `DataReference` preserva base, subscript groups, reference modification, meta e provenance sem exigir reparsing textual.
- `SPECIFICATION_GUARANTEED`: ADR-0011 dá precedência à explicação COBOL concreta sobre classificação externa inferida.
- `UNCERTAIN`: o modo real de compilação/tradução CICS não está disponível de forma geral.
- `OBSERVED_IN_CURRENT_CORPUS_ONLY`: nomes de argumentos e frequências observadas não têm força normativa e não podem governar o algoritmo.

## Comportamento esperado

- Executar somente depois de `CobolReferenceResolver`.
- Consultar estrutura AST e identidades dos produtos; não usar regex, substring ou reparsing de `writtenText`.
- Preservar `ReferenceResolution.Entry` da raiz e dos filhos com status/reason/candidates/diagnostics originais.
- Produzir classificação com ID determinístico, unit, construct AST node, technology, kind, certainty, reason, meta/provenance e occurrence IDs cobertos.
- Na projeção agregada, substituir somente gaps `REFERENCE_BINDING` cobertos pelo construct por um fato externo observável; a hipótese inferida continua bloqueando qualquer claim incompatível de completude.
- Manter occurrences não cobertas, gaps de input/frontend, ambiguidades e demais diagnostics sem alteração.
- Ausência do classifier ou conjunto vazio de classifiers mantém exatamente o comportamento atual.

## Comportamento diante de incerteza

A classificação é uma hipótese `INFERRED`, não prova de modo CICS, literal, símbolo ou binding. Falta de AST/occurrence/resolution coerente falha fechada: não classifica e preserva os gaps originais, ou rejeita produto internamente inconsistente com diagnóstico identificável. `AMBIGUOUS`, `UNSUPPORTED`, parser error e metadata de compilação ausente nunca são convertidos em certeza.

Se a raiz estiver resolvida como COBOL, nem um subscript unresolved nem a grafia CICS autorizam classificação externa. Se a raiz estiver unresolved e um componente interno tiver binding próprio, a classificação pode registrar sua pertença ao construct sem mutar esse binding; a projeção remove apenas o gap artificial que de fato estiver coberto.

## Fora de escopo

- Restaurar ou alterar `Cobol.g4`, tokens ou manifests.
- Criar nó CICS na AST ou policy baseada em modo de compilação.
- Criar catálogo de argumentos CICS ou regra `startsWith("DFH")`.
- Resolver `EIBCALEN`, `EIBAID`, copybooks de sistema ou qualquer external symbol.
- Interpretar `EXEC CICS`, `EXEC SQL`, IMS ou protocolo GRBE.
- Criar registry, discovery, DI framework ou API genérica para todas as extensões.
- Construir CFG/dataflow, inferir possible values ou targets dinâmicos.
- Alterar outros unresolved ou claims de cobertura além da composição estritamente coberta.

## Regras de domínio relacionadas

- `docs/domain/semantic-ast.md`: table calls permanecem referências estruturadas sem binding ou plataforma.
- `docs/domain/reference-resolution.md`: binding nominal é imutável e não incorpora classificação externa.
- `docs/domain/provenance.md`: toda classificação e projeção preserva origem física, exatidão e include chain.

## ADRs/invariantes relacionados

Governam o slice ADR-0011, INV-EXT-001 a INV-EXT-004, INV-AST-001, INV-AST-002, INV-PROV-002, INV-COV-001 e INV-DET-001. ADR-0003 impede mutação entre produtos; ADR-0008 impede que a hipótese externa seja apresentada como completude.
