# WORK-EXT-001 — Eval oracle-first

## O que prova corretude

EVAL-EXT-001 prova que o pipeline distingue binding COBOL de classificação externa, dá precedência à explicação COBOL, reconhece somente a shape estreita autorizada, preserva incerteza/provenance e agrega todas as occurrences artificiais do mesmo construct. O oracle compara separadamente AST, `ReferenceResolution`, classification product e report projection; uma contagem global de corpus não substitui essas relações.

## Classes positivas

### COBOL válido

Cada fonte deve ter zero parser errors, declaração/símbolo esperado, binding `RESOLVED` e zero classification CICS:

```cobol
01 DFHRESP PIC 9.
MOVE DFHRESP TO X.
```

```cobol
01 DFHVALUE PIC 9.
MOVE DFHVALUE TO X.
```

```cobol
01 DFHRESP OCCURS 10 TIMES PIC 9.
01 IDX PIC 9.
MOVE DFHRESP(IDX) TO X.
```

Repetir a table call para `DFHVALUE`. Declarar uma tabela `DFHRESP` e deixar somente `IDX` não resolvido deve preservar a raiz COBOL resolvida e não classificar o construct como CICS.

### Possível CICS

Em hosts válidos como condição relacional, sem declaração compatível de base:

```cobol
IF WS-RESP = DFHRESP(NORMAL)
    CONTINUE
END-IF.
```

e a variante `DFHVALUE(SOME-NAME)` devem manter a entry raiz `UNRESOLVED` no binding, produzir exatamente uma classification `CICS/POSSIBLE_INTRINSIC/INFERRED`, preservar meta/provenance e enumerar as occurrence identities do subtree. A projeção não pode manter `DFHRESP`/`NORMAL` ou `DFHVALUE`/argumento como gaps COBOL independentes do mesmo construct.

## Classes negativas

- `MY-TABLE(IDX)` sem declaração continua unresolved comum.
- `DFHOTHER(X)` não é classificado por prefixo.
- `DFHRESP`/`DFHVALUE` sem parênteses continuam nomes COBOL resolved ou unresolved comuns.
- Base qualificada, múltiplos grupos parenthesized e reference modification não entram no slice.
- `AMBIGUOUS`, `UNSUPPORTED`, parser error ou produto inconsistente não são convertidos em hipótese CICS.
- A ausência/configuração vazia do classifier produz o comportamento anterior byte a byte nos campos preexistentes, salvo metadado aditivo explicitamente versionado.

## Classes ambíguas

- Com declaração COBOL compatível e nenhum modo de compilação conhecido, prevalece `RESOLVED` COBOL mesmo que a grafia seja `DFHRESP(...)`.
- Sem declaração e sem metadata de compilação, o resultado é `POSSIBLE_INTRINSIC/INFERRED`, nunca CICS comprovado.
- Múltiplos candidates COBOL mantêm `AMBIGUOUS` e não acionam o classifier restrito a `UNRESOLVED`.
- Base unresolved com argumento que coincide com declaração COBOL ainda pode formar construct externo possível; o binding do argumento não é mutado, mas sua occurrence é associada ao subtree para evitar interpretação agregada contraditória.

## Casos adversariais

- Variar argumentos entre identifier declarado, identifier ausente, literal, keyword admitida e nomes não presentes no corpus; a decisão depende da base/shape, não do argumento.
- Colocar `DFHRESP(...)` em mais de um host gramatical que efetivamente produza a mesma `DataReference`; a decisão deve depender do contrato AST, não do texto do host.
- Usar `DFHRESP(IDX)(OTHER)`, `DFHRESP(IDX)(1:2)`, qualificação e reference modification para matar uma detecção baseada apenas em `baseName`.
- Criar constructs vizinhos e aninhamento de statements para provar que occurrence IDs cobertos não vazam para outro subtree ou program unit.
- Fazer o argumento resolver, ficar unresolved ou ambíguo; somente gaps pertencentes à classification root podem ser agregados.
- Provar que nenhuma classe CICS aparece nas dependências diretas de AST, symbol table, collector ou resolvers COBOL.

## Casos de regressão

- Os três characterization cases que demonstraram a regressão da remoção de `DFHRESP`/`DFHVALUE` de `cobolWord` permanecem verdes.
- EVAL-AST-002 mantém table calls estruturadas; EVAL-RES-DATA-001 mantém lookup DATA/INDEX e candidates; EVAL-RES-REPORT-001 mantém todos os gaps não cobertos e readiness conservadora.
- Snapshots conservam entries e diagnostics de `ReferenceResolution`; classification é coleção ortogonal e aditiva.
- Nenhuma gramática, manifest ou fixture existente é enfraquecida para selecionar uma derivação conveniente.

## Propriedades/relações metamórficas

1. **Disponibilidade de declaração:** para o mesmo host `H[DFHRESP(IDX)]`, adicionar uma declaração COBOL compatível para `DFHRESP` muda apenas binding/classification relacionados: sem declaração há hipótese externa; com declaração há COBOL e nenhuma hipótese. A AST host e a estrutura do construct permanecem equivalentes salvo IDs derivados da declaração adicionada.
2. **Case-insensitivity:** mudar apenas a caixa de `DFHRESP`, `DFHVALUE` e seus argumentos não altera decisão nem coverage, desconsiderada a grafia preservada.
3. **Independência do argumento:** renomear o argumento ou trocar um identifier por outro aceito pela mesma shape não altera a classificação da base unresolved.
4. **Declaração não relacionada:** adicionar símbolo fora do nome/visibilidade relevante não altera a classificação.
5. **Determinismo:** repetir análise e composição produz mesma ordem, IDs, coverage de occurrences, motivo e certeza.

## Expectativas de escala

O classifier deve percorrer entries/AST indexada uma vez ou usar índices por `(ProgramUnitId, astNodeId)`, com tempo e espaço auxiliares `O(nodes + resolution entries + covered occurrences)`. É proibido, para cada unresolved, varrer todas as declarações, todos os nós ou reparsear texto. O slice não cria gate de performance dedicado; métricas/casos sintéticos só serão acrescentados se a implementação introduzir percurso potencialmente multiplicativo.
