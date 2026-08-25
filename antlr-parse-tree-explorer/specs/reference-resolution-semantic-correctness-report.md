# Reference-resolution semantic correctness hardening

## 1. BASELINE

### Fase 0 — 2026-08-24

#### Escopo e estado inicial

- Diretório exclusivo de trabalho: `antlr-parse-tree-explorer`.
- Commit inicial: `dd96927578cc75bc32e99375189a9d957d027ba4`.
- `git status --porcelain=v1 --untracked-files=all` antes da criação deste relatório: sem saída (worktree limpo).
- Código de produção alterado nesta fase: nenhum.
- Fixtures ou testes alterados nesta fase: nenhum.
- Único artefato criado nesta fase: este relatório.

#### Suíte Maven existente

Comando executado:

```text
/home/gustavo/.sdkman/candidates/maven/current/bin/mvn test
```

Resultado observado:

```text
Tests run: 57, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time: 52.391 s
Finished at: 2026-08-24T22:39:15-03:00
```

A execução compilou 47 fontes Java de produção e 21 fontes Java de teste. O Maven copiou 17 recursos de teste. Na primeira execução, o Surefire baixou dependências ainda ausentes do repositório Maven local; isso não alterou arquivos versionados do projeto.

#### Checks JavaScript existentes

O projeto não contém `package.json` nem scripts npm/yarn/pnpm. A verificação JavaScript estabelecida nos documentos de regressão do próprio projeto é `node --check` sobre os templates e as três jornadas versionadas.

Foram enumerados e verificados 31 arquivos:

- 4 em `src/main/resources/web`;
- 9 em `dist`;
- 9 em `dist-cbstm03a`;
- 9 em `dist-cbstm03d`.

Comando executado:

```text
find src/main/resources/web dist dist-cbstm03a dist-cbstm03d \
  -type f -name '*.js' -exec node --check '{}' ';'
```

Resultado: exit code `0`; nenhum erro de sintaxe JavaScript.

O executor acrescentou repetidamente a mensagem ambiental `Failed to create stream fd: Operation not permitted` ao início de comandos, inclusive comandos Git que terminaram com sucesso. Essa mensagem não veio do Node nem indicou falha de um arquivo: o comando agregado de `node --check` terminou com exit code `0`.

#### Evidência dos arquivos protegidos

Antes da tarefa, `git status` estava limpo. Após Maven e checks JavaScript, o comando abaixo também terminou com exit code `0`, provando ausência de diferenças versionadas nas áreas protegidas em relação ao commit inicial:

```text
git diff --quiet HEAD -- \
  corpus src/main/antlr4 src/test/resources \
  dist dist-cbstm03a dist-cbstm03d
```

Tree object IDs registrados no commit inicial:

| Categoria protegida | Caminho | Git tree object ID |
|---|---|---|
| Corpus | `corpus` | `367ec764633fa81d427998fd08f0611d36d8aa36` |
| Gramáticas | `src/main/antlr4` | `1493e1c82df6f8a8954ebc12abacb77ce78b5684` |
| Fixtures existentes | `src/test/resources` | `965b9dff6e23bb325a1a65766e1c0bea32ced122` |
| Output principal | `dist` | `c9698a0b4e3f71db595a1c9544ecffac9a9b9c54` |
| Output CBSTM03A | `dist-cbstm03a` | `791d41522daff46cd5474e136faa3fc71cbc1a5b` |
| Output CBSTM03D | `dist-cbstm03d` | `171d8a777dc8069395d092913d3f4bcc113886ac` |

Esses IDs, combinados com o `git diff --quiet` verde e o estado inicialmente limpo, formam a evidência reproduzível do conteúdo protegido anterior às futuras fixtures adversariais.

#### Diagnóstico da fase

O baseline existente está verde. A Fase 0 não auditou nenhuma das hipóteses semânticas e, portanto, não confirma nem refuta lacunas de resolução nominal. Não houve RED, diagnóstico de resolver ou correção de produção nesta fase.

## 2. HIPÓTESES AUDITADAS

### Fase 1 — GLOBAL DATA herdado por subordinados

Hipótese: a implementação preserva `GLOBAL` somente na declaração que contém a cláusula e, por isso, não torna globais os data-names subordinados, condition-names de nível 88 e index-names associados, embora esses nomes sejam globais pela linguagem.

Regra adotada: a documentação oficial do [IBM Enterprise COBOL 6.5 — GLOBAL clause](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=entry-global-clause) estabelece que data-names subordinados, condition-names e indexes associados a um nome global são também globais. A documentação de [scope of names](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=programs-scope-names) confirma que itens subordinados, condition-names e indexes tornam-se automaticamente globais e são acessíveis em programas direta ou indiretamente contidos. Portanto, as expectativas do teste não foram derivadas da implementação atual.

Fixture criada: `src/test/resources/cobol/resolution/global-subordinate-visibility.cbl`.

A fixture é sintética, possui zero erros de parsing e contém:

```text
GLOBAL-OUTER [0]
├── GLOBAL-GROUP GLOBAL
│   ├── GLOBAL-CHILD (DATA_ITEM)
│   └── STATUS-FIELD
│       └── STATUS-OK (CONDITION_NAME)
├── GLOBAL-TABLE GLOBAL
│   └── TABLE-ITEM OCCURS 10 INDEXED BY GLOBAL-IDX (INDEX_NAME)
└── GLOBAL-INNER [0,0]
    ├── MOVE GLOBAL-CHILD ...
    ├── IF STATUS-OK ...
    └── MOVE TABLE-ITEM(GLOBAL-IDX) ...
```

Teste criado: `DataAndIndexReferenceResolverTest.inheritsGlobalGroupVisibilityForDataConditionAndIndexSubordinates`.

O teste prova antes do binding que as três declarações existem somente na tabela de `GLOBAL-OUTER`, com `SymbolKind`s distintos (`DATA_ITEM`, `CONDITION_NAME`, `INDEX_NAME`), e que nenhuma declaração homônima foi criada em `GLOBAL-INNER`. Em seguida exige, para cada uso, um único candidato, o `ReferenceKind` correto e um `SemanticEntityId` pertencente ao `ProgramUnitId` do pai.

Classes e métodos suspeitos após a execução:

- `AstBuilder.declarationVisibility`: atribui visibilidade a cada `DataEntry` isoladamente;
- `SymbolTableBuilder.dataAttributes`: copia a visibilidade isolada da entrada;
- `SymbolTableBuilder.collectDataEntries`: cria condition e index symbols sem calcular GLOBAL efetivo por ancestralidade; o index recebe somente `relation=OCCURS_INDEX`;
- `DataAndIndexReferenceResolver.localOrInheritedGlobal`: aceita de ancestors apenas symbols cujo atributo individual `visibility` seja literalmente `GLOBAL`;
- `ReferenceOccurrenceCollector.addDataReference` e a forma produzida por `AstBuilder` para subscripts: o uso `GLOBAL-IDX` chegou como `qualifiedDataName`/`DATA`, apesar de a declaração correspondente ser `INDEX_NAME`.

### Fase 2 — shadowing antes da filtragem por namespace/kind

Hipótese: `DataAndIndexReferenceResolver` filtra os homônimos pelo kind exigido pelo uso antes de determinar em qual `ProgramUnit` a busca nominal deve parar. Assim, uma declaração local homônima, mas incompatível, pode ser ignorada em favor de um GLOBAL compatível do pai.

Regra adotada: a documentação oficial IBM de [scope of names](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=programs-scope-names) descreve que a busca começa no programa que contém a referência e termina no primeiro nome encontrado. Ela explicita que a busca é pelo nome global, não por um tipo particular de objeto; se o primeiro nome encontrado designa um tipo diferente do exigido, há erro. Logo, o resultado conservador adotado para o produto atual é `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`, nunca o símbolo externo compatível.

Fixture criada: `src/test/resources/cobol/resolution/nested-namespace-shadowing.cbl`.

```text
SHADOW-OUTER [0]
├── COLLIDE-CONDITION GLOBAL (DATA_ITEM)
├── COLLIDE-INDEX GLOBAL (DATA_ITEM)
└── CONTROL-GLOBAL GLOBAL (DATA_ITEM)

SHADOW-INNER [0,0]
├── COLLIDE-CONDITION (CONDITION_NAME)
├── COLLIDE-INDEX (INDEX_NAME)
└── usos DATA de COLLIDE-CONDITION, COLLIDE-INDEX e CONTROL-GLOBAL
```

Os GLOBALs do pai são level-01 e possuem a própria cláusula, isolando esta fase da herança de visibilidade testada na Fase 1. O controle sem homônimo local prova que a busca no pai está funcional.

Teste criado: `DataAndIndexReferenceResolverTest.stopsAtIncompatibleLocalNamesBeforeFilteringByReferenceKind`.

Antes do binding, o teste comprova os quatro pares nome/categoria nas tabelas dos dois programas. Depois exige que os dois homônimos locais interrompam a busca com diagnóstico conservador e que `CONTROL-GLOBAL` resolva normalmente para `SHADOW-OUTER`.

### Fase 3 — qualification antes da seleção local/GLOBAL

Hipótese: o resolver reduz o conjunto de candidatos ao `ProgramUnit` local antes de aplicar `OF/IN`, impedindo que um qualifier explícito selecione um homônimo GLOBAL do pai.

Regras adotadas:

- a documentação IBM de [resolution of names](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=names-resolution) determina que o conjunto inicial contém todos os nomes do programa contido e todos os nomes globais dos programas que o contêm; as regras normais de qualification e unicidade são aplicadas a esse conjunto antes da preferência pelo recurso local quando mais de um permanece;
- a documentação IBM de [qualification](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=reference-qualification) define `OF/IN` como qualifiers de níveis superiores da mesma hierarquia e permite qualificar um nome mesmo quando isso não seria necessário;
- a [GLOBAL clause](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=entry-global-clause) torna `VALUE-X` global por ele ser subordinado a `OUTER-GROUP GLOBAL`.

Fixture criada: `src/test/resources/cobol/resolution/nested-qualified-global.cbl`.

```text
QUALIFY-OUTER [0]
└── OUTER-GROUP GLOBAL
    └── VALUE-X

QUALIFY-INNER [0,0]
├── INNER-GROUP LOCAL
│   └── VALUE-X
└── referências:
    ├── VALUE-X
    ├── VALUE-X OF INNER-GROUP
    └── VALUE-X OF OUTER-GROUP
```

Teste criado: `DataAndIndexReferenceResolverTest.appliesQualificationAcrossLocalAndInheritedGlobalCandidatesBeforeSelectingAProgramUnit`.

Antes da resolução, o teste demonstra que existem dois DATA_ITEMs `VALUE-X`, um em cada `ProgramUnit`, e que suas hierarquias superiores são respectivamente `OUTER-GROUP GLOBAL` e `INNER-GROUP LOCAL`. As expectativas distinguem tanto o reason quanto o `SemanticEntityId.programUnitId` das três referências.

### Fase 4 — GLOBAL FILE entre programas aninhados

Hipóteses:

1. `CobolReferenceResolver.resolveFile` consulta somente o índice FILE do `ProgramUnit` do uso e não procura FILEs GLOBAL em ancestors;
2. `SymbolTableBuilder.buildFileEntities` associa SELECT + FD/SD, mas perde a visibilidade presente no `FILE_DESCRIPTION`, impedindo que a entidade semântica e seu candidato exponham `GLOBAL`/`LOCAL`.

Regra adotada: a documentação oficial [IBM Enterprise COBOL 6.5 — GLOBAL clause para file entries](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=entries-global-clause) define que a cláusula GLOBAL no FD torna global o file-name e seu file connector, disponíveis a programas direta ou indiretamente contidos. A documentação [Using data in input and output operations](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=data-using-in-input-output-operations) confirma que programas em uma estrutura contida podem acessar o mesmo arquivo por um file-name GLOBAL.

Fixture criada: `src/test/resources/cobol/resolution/nested-global-file.cbl`.

```text
FILE-OUTER [0]
├── GLOBAL-BOTH GLOBAL       (SELECT + FD; 2 declarationSymbolIds)
├── OUTER-LOCAL LOCAL        (SELECT + FD; 2 declarationSymbolIds)
├── SHADOW-FILE GLOBAL       (SELECT + FD; 2 declarationSymbolIds)
└── GLOBAL-FD-ONLY GLOBAL    (FD sem SELECT; 1 declarationSymbolId)

FILE-INNER [0,0]
├── SHADOW-FILE LOCAL        (SELECT + FD; 2 declarationSymbolIds)
└── READ GLOBAL-BOTH, OUTER-LOCAL, GLOBAL-FD-ONLY, SHADOW-FILE
```

Teste criado: `ProcedureFileProgramReferenceResolverTest.resolvesOnlyGlobalAncestorFilesAndPreservesFileEntityOwnershipAndDeclarations`.

O teste inspeciona primeiro as entidades FILE por `ProgramUnit`, depois o binding. Ele exige:

- associação SELECT + FD sem ambiguidade artificial e preservação dos dois symbol IDs;
- entidade válida com somente FD e um symbol ID;
- atributo `visibility` observável tanto na entidade quanto no candidato;
- `SemanticEntityId.programUnitId` do pai nos dois FILEs GLOBAL herdados;
- invisibilidade do FILE local do pai;
- shadowing pelo FILE local do filho.

Para permitir a inspeção pré-binding, o record de teste `Analysis` passou a carregar também `CompilationUnitSymbolTables`; essa é apenas infraestrutura de teste e não altera o produto de produção.

### Fase 5 — qualifier DATA vs FILE homônimo

Hipótese: embora `ReferenceOccurrenceCollector` preserve o kind semântico indicado pela origem AST do qualifier, `DataAndIndexReferenceResolver.applyQualification` reduz cada qualifier a uma string e compara essa lista com uma ancestry que mistura `DATA_ITEM` e `FILE_DESCRIPTION`. Assim, um qualifier DATA poderia casar indevidamente com um FILE homônimo, ou vice-versa.

Regra adotada: a documentação oficial [IBM Enterprise COBOL 6.5 — Qualification](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=reference-qualification) estabelece a equivalência lógica de `IN` e `OF`. Portanto, trocar apenas o conector em `ITEM IN QUALIFIER-X` versus `ITEM OF QUALIFIER-X` não cria, por si só, categorias semânticas diferentes. A fixture usa formas sintaticamente válidas e uma hierarquia adicional para tentar levar a grammar à distinção estrutural `inData`/`inFile`, sem depender da grafia do conector.

Fixture criada: `src/test/resources/cobol/resolution/data-file-qualifier-collision.cbl`.

```text
FILE SECTION
└── FD QUALIFIER-X
    └── FILE-RECORD
        └── ITEM

WORKING-STORAGE
└── QUALIFIER-X
    └── FILE-RECORD
        └── ITEM

referências:
├── ITEM OF QUALIFIER-X
└── ITEM OF FILE-RECORD IN QUALIFIER-X
```

Teste criado: `ProcedureFileProgramReferenceResolverTest.preservesQualifierKindWhenDataAndFileHierarchiesHaveTheSameNames`.

O teste verifica separadamente o `ReferenceOccurrence` de cada `QUALIFIER-X` e o binding da referência principal. A análise chegou às assertions com zero erros sintáticos, e os dois `ITEM` concorrentes foram comprovados estruturalmente: um sob o `FILE_DESCRIPTION` e outro sob o DATA item de working-storage.

Resultado inesperado, preservado sem adaptar a expectativa ao código: ambos os qualifiers foram produzidos como `ReferenceKind.DATA`, regra `dataName`. A alternativa geral da grammar `qualifiedDataNameFormat1 : (dataName | conditionName) (qualifiedInData+ inFile? | inFile)?`, combinada com `dataName` e `fileName` aceitando a mesma forma lexical, escolheu `inData` também para o qualifier final da segunda referência. Assim, a capacidade já demonstrada por teste antigo de produzir FILE não cobria esta forma: o caso antigo era `LINAGE-COUNTER IN BOTH-FILE`, tratado pela alternativa especial `qualifiedDataNameFormat4 : LINAGE_COUNTER inFile`.

### Fase 6 — COMMON program e descendentes

Hipótese: `CobolReferenceResolver.visibleInternalProgram` reconhece um target COMMON ao subir a cadeia de ancestors do caller, mas não exclui callers que estejam dentro do próprio subtree do target COMMON.

Regra adotada: a documentação oficial [IBM Enterprise COBOL 6.3 — Calling nested COBOL programs](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=program-calling-nested-cobol-programs) permite chamar um programa contido diretamente somente a partir de seu programa diretamente contendo, ampliando a região quando o target é COMMON. A regra mais precisa do [IBM Enterprise COBOL Language Reference 6.2, COBOL program structure](https://www.ibm.com/docs/en/SS6SG3_6.2.0/pdf/lrmvs.pdf) explicita a exclusão: um COMMON é visível ao programa que o contém e aos programas contidos nessa região, exceto aos programas contidos no próprio COMMON. O [Programming Guide 6.5](https://www.ibm.com/docs/en/SS6SG3_6.5/pdf/pgmvs.pdf) reafirma que o COMMON não pode ser chamado por programa contido em si mesmo.

Fixture criada: `src/test/resources/cobol/resolution/common-program-visibility.cbl`.

```text
COMMON-ROOT [0]
├── SIBLING [0,0]
├── COMMON-C COMMON [0,1]
│   └── DESCENDANT [0,1,0]
├── PRIVATE-SIBLING [0,2]
└── A [0,3]
    ├── B [0,3,0]
    └── DEEP-C COMMON [0,3,1]
        └── D [0,3,1,0]
```

Teste criado: `ProcedureFileProgramReferenceResolverTest.excludesCommonProgramAndItsDescendantsFromTheCommonProgramsCallingRegion`.

Antes de verificar binding, o teste comprova todos os `parentId`s da topologia e os atributos COMMON/privado no AST. Em seguida cobre separadamente:

- pai direto chamando COMMON;
- sibling chamando COMMON;
- descendente do COMMON chamando o próprio ancestor COMMON;
- sibling privado não-COMMON;
- os mesmos casos positivo e negativo em profundidade adicional;
- o programa raiz tentando chamar um COMMON que não é seu filho direto e pertence à região de `A`.

### Fase 7 — REDEFINES com nomes concorrentes

Hipótese: `DeclarationRelationResolution` reutiliza o resultado de uma ocorrência DATA genérica para `REDEFINES`, sem restringir candidatos pela posição estrutural do item que declara a relação.

Regra adotada: a documentação oficial [IBM Enterprise COBOL 6.4 — REDEFINES clause](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-redefines-clause) exige que sujeito e objeto tenham o mesmo nível na hierarquia, embora os números de nível possam diferir, e proíbe a existência de uma entrada com nível numericamente menor entre eles. As descrições alternativas da mesma área devem seguir a área redefinida sem uma entrada interveniente que defina novas posições. Além disso, a documentação de [qualification](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=reference-qualification) registra que o objeto de REDEFINES não pode ser explicitamente qualificado; portanto, a estrutura do owner deve participar da seleção em vez de exigir que o fonte desambigue com `OF/IN`.

Fixture criada: `src/test/resources/cobol/resolution/redefines-structural-binding.cbl`.

```text
GROUP-A
├── X                    <- alvo válido
└── Y REDEFINES X

GROUP-B
└── X                    <- homônimo fora do contexto

GROUP-C
├── SUBGROUP-C (05)
│   └── DEEP-X (10)
└── BAD-Y (05) REDEFINES DEEP-X  <- relação estruturalmente inválida
```

Teste criado: `DataAndIndexReferenceResolverTest.resolvesRedefinesWithinItsStructuralLevelAndRejectsInvalidHierarchyTargets`.

O teste comprova antes do binding os dois símbolos `X` distintos, seus owner scopes `GROUP-A` e `GROUP-B`, e as categorias DATA_ITEM de `Y`, `BAD-Y` e `DEEP-X`. Depois localiza cada `DeclarationRelation` pelo `ownerSymbolId`, evitando confundir relações apenas pelo texto do target.

### Fase 8 — RENAMES com nomes concorrentes

Hipótese: `RENAMES_FROM` e `RENAMES_THROUGH`, assim como REDEFINES, reutilizam bindings DATA genéricos sem consumir o logical record associado ao level-66 nem validar conjuntamente os endpoints da faixa.

Regra adotada: a documentação oficial [IBM Enterprise COBOL 6.5 — RENAMES clause](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=entry-renames-clause) exige que os nomes dos endpoints identifiquem itens elementares ou de grupo dentro do level-01 associado, que sejam distintos e que delimitem a sequência original de itens daquele record. Assim, homônimos em outro logical record não são candidatos válidos e uma faixa que cruza records é semanticamente inválida.

Fixture criada: `src/test/resources/cobol/resolution/renames-structural-binding.cbl`.

```text
RECORD-A
├── START-X                 <- FROM válido
├── END-X                   <- THROUGH válido
└── 66 RANGE-A RENAMES START-X THRU END-X

RECORD-B
├── START-X                 <- homônimo externo
└── END-X                   <- homônimo externo

RECORD-C
└── CROSS-START

RECORD-D
├── CROSS-END
└── 66 CROSS-RANGE RENAMES CROSS-START THRU CROSS-END
                              ^ range cruza RECORD-C/RECORD-D
```

Teste criado: `DataAndIndexReferenceResolverTest.resolvesRenamesWithinItsLogicalRecordAndRejectsCrossRecordRanges`.

O teste comprova os owner scopes de todos os endpoints, que `RANGE-A` pertence a `RECORD-A`, que `CROSS-RANGE` pertence a `RECORD-D`, e que ambos os owners level-66 têm `SymbolKind.RENAMES`. As quatro relações são recuperadas por `ownerSymbolId + RelationKind`, distinguindo FROM e THROUGH.

### Fase 9 — PROGRAM-NAME literal

Hipótese: a declaração literal preserva aspas em `Ast.Program.name` e, por consequência, em `ProgramUnitId.canonicalProgramName`, enquanto `CALL literal` já separa a grafia escrita da identidade semântica com `unquote`.

Regra adotada: a documentação oficial [IBM Enterprise COBOL 6.4 — COBOL program structure](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=structure-cobol-program) permite que `program-name` seja um user-defined word ou um literal alfanumérico. A comparação semântica não deve incorporar os delimitadores lexicais do literal; as aspas pertencem à representação escrita. As opções de canonicalização externas PGMNAME permanecem deliberadamente reservadas à Fase 10.

Fixture criada: `src/test/resources/cobol/resolution/literal-program-name.cbl`.

```text
PROGRAM-ID. OUTER-LITERAL.
    CALL 'CHILD'.

    PROGRAM-ID. 'CHILD'.
    END PROGRAM 'CHILD'.
END PROGRAM OUTER-LITERAL.
```

Teste criado: `ProcedureFileProgramReferenceResolverTest.separatesLiteralProgramNameSpellingFromCanonicalProgramIdentity`.

O teste compara explicitamente:

- `Ast.Program.name` e `ProgramAttributes.writtenText` da declaração;
- `ProgramUnitId.canonicalProgramName` esperado sem delimitadores;
- `Ast.ProgramReference.programName` versus `writtenText` do CALL;
- occurrence PROGRAM produzida;
- candidate final e seu `SemanticEntityId.programUnitId`.

A grammar aceitou tanto `PROGRAM-ID. 'CHILD'.` quanto `END PROGRAM 'CHILD'.` com zero erros sintáticos.

### Fase 10 — PGMNAME e política de nomes de programa

Hipótese: o frontend reconhece sintaticamente `PGMNAME`, mas a opção não chega ao produto de preprocessing nem à política de resolução. Sem esse dado, a canonicalização genérica por uppercase pode produzir bindings externos falsamente certos.

Regra adotada: a documentação oficial [IBM Enterprise COBOL 6.5 — PGMNAME](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=options-pgmname) distingue modos com identidades externas diferentes. Em síntese: `COMPAT` (default documentado) aplica uppercase, limite de oito caracteres e tradução de hífen para zero; `LONGUPPER` aplica uppercase sem truncamento; `LONGMIXED` preserva case e exige nomes definidos como literal. A sintaxe de declaração é documentada no [IBM Enterprise COBOL 6.5 — PROGRAM-ID paragraph](https://www.ibm.com/docs/en/cobol-zos/6.5.0?topic=division-program-id-paragraph). Uma análise que não recebeu a opção efetiva não pode escolher silenciosamente um desses modos quando o nome depende dela.

A grammar versionada já reconhece explicitamente `PGMNAME`/`PGMN` com `COMPAT`, `LONGMIXED`, `LONGUPPER` e aliases. O contrato da fase usa `CBL PGMNAME(LONGMIXED)` para comprovar essa capacidade separadamente do binding conservador quando nenhuma configuração é transportada.

Fixture criada: `src/test/resources/cobol/resolution/program-name-policy.cbl`.

```text
PROGRAM-ID. POLICY-CALLER.
    CALL 'SIMPLE'.
    CALL 'mixed-Child'.
    CALL 'LONG-NAME-ABC'.
END PROGRAM POLICY-CALLER.
```

Teste criado: `ProcedureFileProgramReferenceResolverTest.requiresExplicitPgmnamePolicyWhenProgramIdentityDependsOnCompilerOption`.

O teste diferencia:

- um controle convencional uppercase cuja identidade não depende, no universo do teste, da escolha de case;
- um nome mixed-case, cuja comparação diverge entre `LONGMIXED` e modos uppercase;
- um nome longo com hífens, cuja identidade externa diverge entre `COMPAT` e modos longos;
- capacidade sintática da grammar, transporte da opção pelo preprocessor e presença de modo explícito em `CobolResolutionPolicy`.

## 3. RESULTADO RED

### Fase 1

Comandos executados, sempre antes de qualquer correção de produção:

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#inheritsGlobalGroupVisibilityForDataConditionAndIndexSubordinates test
mvn -Dtest=DataAndIndexReferenceResolverTest test
mvn test
```

Resultados:

- teste específico: 1 executado, 1 falha, 0 erros;
- classe: 8 executados, 1 falha, 0 erros; os 7 testes preexistentes permaneceram verdes;
- suíte completa: 58 executados, 1 falha, 0 erros, 0 ignorados; a única falha é o novo teste adversarial;
- o teste específico agregou três grupos de falha, preservando evidência independente para DATA, CONDITION e INDEX.

| Caso | Expectativa semântica | Comportamento atual | Classificação |
|---|---|---|---|
| `GLOBAL-CHILD` | `DATA`, `RESOLVED`, um candidato em `GLOBAL-OUTER`, domínio `DATA_SYMBOL` | ocorrência corretamente `DATA`; `UNRESOLVED / DECLARATION_NOT_FOUND`; 0 candidatos | **BUG CONFIRMADO** |
| `STATUS-OK` | `CONDITION`, `RESOLVED`, um candidato em `GLOBAL-OUTER`, domínio `DATA_SYMBOL` | ocorrência corretamente `CONDITION`; `UNRESOLVED / DECLARATION_NOT_FOUND`; 0 candidatos | **BUG CONFIRMADO** |
| `GLOBAL-IDX` | `INDEX`, `RESOLVED`, um candidato em `GLOBAL-OUTER`, domínio `INDEX_SYMBOL` | declaração é `INDEX_NAME`, mas ocorrência de subscript é `DATA` (`qualifiedDataName`); `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`; 0 candidatos | **BUG CONFIRMADO**, com lacuna adicional de classificação contextual do uso |

Trechos essenciais da assertion capturada:

```text
GLOBAL visibility inherited by subordinate declarations (3 failures)
GLOBAL-CHILD: expected RESOLVED but was UNRESOLVED
  expected UNIQUE_VISIBLE_DECLARATION but was DECLARATION_NOT_FOUND
STATUS-OK: expected RESOLVED but was UNRESOLVED
  expected UNIQUE_VISIBLE_DECLARATION but was DECLARATION_NOT_FOUND
GLOBAL-IDX: expected INDEX but was DATA
  expected RESOLVED but was UNRESOLVED
  expected UNIQUE_VISIBLE_DECLARATION but was INVALID_NAMESPACE_FOR_CONTEXT
```

Os checks estruturais anteriores ao `assertAll` passaram: as declarações pertencem ao pai, seus três `SymbolKind`s estão corretos e a tabela do filho não contém declarações locais inventadas. Como nenhum candidato sobreviveu, os `SemanticEntityId`s esperados do pai não puderam ser produzidos pela implementação atual.

### Fase 2

Comandos executados antes de qualquer correção:

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#stopsAtIncompatibleLocalNamesBeforeFilteringByReferenceKind test
mvn -Dtest=DataAndIndexReferenceResolverTest test
mvn test
```

Resultados:

- teste específico: 1 executado, 1 falha agregando os 2 casos, 0 erros;
- classe: 9 executados, 2 falhas, 0 erros — exatamente os REDs das Fases 1 e 2;
- suíte completa: 59 executados, 2 falhas, 0 erros, 0 ignorados — nenhuma falha preexistente adicional;
- `CONTROL-GLOBAL` resolveu para o `ProgramUnitId` do pai e todos os checks de categorias declaradas passaram.

| Caso | Nome local | GLOBAL externo | Esperado | Resultado atual | Classificação |
|---|---|---|---|---|---|
| uso DATA de `COLLIDE-CONDITION` | `CONDITION_NAME` | `DATA_ITEM` | `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`, 0 candidatos | `RESOLVED / UNIQUE_VISIBLE_DECLARATION`; seleciona DATA do pai `[0]` | **BUG CONFIRMADO** |
| uso DATA de `COLLIDE-INDEX` | `INDEX_NAME` | `DATA_ITEM` | `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`, 0 candidatos | `RESOLVED / UNIQUE_VISIBLE_DECLARATION`; seleciona DATA do pai `[0]` | **BUG CONFIRMADO** |
| uso DATA de `CONTROL-GLOBAL` | ausente | `DATA_ITEM` | resolução para o pai | resolução para o pai | **CONTROLE POSITIVO — PASS** |

Assertion essencial capturada:

```text
an incompatible local declaration shadows an otherwise compatible outer GLOBAL (2 failures)
COLLIDE-CONDITION: expected UNRESOLVED but was RESOLVED
  candidate: SHADOW-OUTER / DATA_SYMBOL / DATA_ITEM / GLOBAL
COLLIDE-INDEX: expected UNRESOLVED but was RESOLVED
  candidate: SHADOW-OUTER / DATA_SYMBOL / DATA_ITEM / GLOBAL
```

O teste não foi adaptado ao comportamento existente: a expectativa continua baseada na regra nominal IBM e o RED permanece permanente.

### Fase 3

Comandos executados antes de qualquer correção:

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#appliesQualificationAcrossLocalAndInheritedGlobalCandidatesBeforeSelectingAProgramUnit test
mvn -Dtest=DataAndIndexReferenceResolverTest test
mvn test
```

Resultados:

- teste específico: 1 executado, 1 falha, 0 erros;
- classe: 10 executados, 3 falhas, 0 erros — exatamente os REDs acumulados das Fases 1, 2 e 3;
- suíte completa: 60 executados, 3 falhas, 0 erros, 0 ignorados;
- as verificações prévias confirmaram os dois candidatos `VALUE-X` e seus respectivos `ProgramUnitId`s/hierarquias.

| Referência | Esperado | Resultado atual | Classificação |
|---|---|---|---|
| `VALUE-X` | local de `QUALIFY-INNER`; `RESOLVED / UNIQUE_VISIBLE_DECLARATION` | local de `QUALIFY-INNER`; `RESOLVED / UNIQUE_VISIBLE_DECLARATION` | **HIPÓTESE NÃO REPRODUZIDA neste subcaso**, comportamento correto |
| `VALUE-X OF INNER-GROUP` | local de `QUALIFY-INNER`; `RESOLVED / QUALIFIED_HIERARCHY_MATCH` | local de `QUALIFY-INNER`; `RESOLVED / QUALIFIED_HIERARCHY_MATCH` | **HIPÓTESE NÃO REPRODUZIDA neste subcaso**, comportamento correto |
| `VALUE-X OF OUTER-GROUP` | herdado de `QUALIFY-OUTER`; `RESOLVED / QUALIFIED_HIERARCHY_MATCH` | `UNRESOLVED / DECLARATION_NOT_FOUND`; 0 candidatos | **BUG CONFIRMADO** |

Assertion essencial:

```text
unqualified, locally qualified and externally qualified references are distinct (1 failure)
VALUE-X OF OUTER-GROUP: expected RESOLVED but was UNRESOLVED
  expected QUALIFIED_HIERARCHY_MATCH but was DECLARATION_NOT_FOUND
```

A hipótese foi confirmada apenas na interação relevante com qualification externa. As duas formas locais passaram e não foram modificadas artificialmente para gerar RED.

### Fase 4

Comandos executados antes de qualquer correção:

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#resolvesOnlyGlobalAncestorFilesAndPreservesFileEntityOwnershipAndDeclarations test
mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
mvn test
```

O teste específico foi executado inicialmente com dois `assertAll` sequenciais. Essa primeira execução confirmou os cinco atributos `visibility=null`, mas o primeiro grupo interrompeu o método antes do grupo de binding. A única alteração subsequente no teste foi envolver os dois grupos em um `assertAll` externo, sem mudar fixture ou expectativa, para preservar todas as falhas independentes na mesma execução. A reexecução produziu o RED completo abaixo.

Resultados finais da fase:

- teste específico: 1 executado, 1 falha agregada em 2 grupos, 0 erros;
- classe: 8 executados, 1 falha, 0 erros; os 7 testes preexistentes da classe permaneceram verdes;
- suíte completa: 61 executados, 4 falhas, 0 erros, 0 ignorados — os REDs acumulados das Fases 1 a 4;
- zero erros sintáticos na fixture.

| Caso | Esperado | Resultado atual | Classificação |
|---|---|---|---|
| entidade `GLOBAL-BOTH` SELECT + FD | 2 symbol IDs; `visibility=GLOBAL` | 2 symbol IDs preservados; `visibility=null` | **BUG CONFIRMADO** para visibilidade; associação preservada |
| `READ GLOBAL-BOTH` no filho | FILE do pai, `RESOLVED`, 2 IDs, `GLOBAL` | `UNRESOLVED / DECLARATION_NOT_FOUND`, 0 candidatos | **BUG CONFIRMADO** |
| entidade `GLOBAL-FD-ONLY` | 1 symbol ID; `visibility=GLOBAL` | 1 symbol ID preservado; `visibility=null` | **BUG CONFIRMADO** para visibilidade; FD-only representado |
| `READ GLOBAL-FD-ONLY` no filho | FILE do pai, `RESOLVED`, 1 ID, `GLOBAL` | `UNRESOLVED / DECLARATION_NOT_FOUND`, 0 candidatos | **BUG CONFIRMADO** |
| `READ OUTER-LOCAL` no filho | `UNRESOLVED / DECLARATION_NOT_FOUND` | igual ao esperado | **HIPÓTESE NÃO REPRODUZIDA no controle negativo**, comportamento correto |
| `READ SHADOW-FILE` no filho | FILE local do filho, `RESOLVED`, 2 IDs, `LOCAL` | seleciona corretamente a entidade do filho e preserva 2 IDs; candidato tem `visibility=null` | **BUG CONFIRMADO** somente para observabilidade da visibilidade; shadowing correto |

Assertion essencial:

```text
GLOBAL FILE semantic product (2 failures)
FILE entities preserve association and effective visibility before binding (5 failures)
  expected GLOBAL/LOCAL but was null
nested FILE lookup applies GLOBAL visibility and local shadowing (3 failures)
  GLOBAL-BOTH: expected RESOLVED but was UNRESOLVED / DECLARATION_NOT_FOUND
  GLOBAL-FD-ONLY: expected RESOLVED but was UNRESOLVED / DECLARATION_NOT_FOUND
  SHADOW-FILE candidate: expected visibility LOCAL but was null
```

### Fase 5

Comandos executados antes de qualquer correção:

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#preservesQualifierKindWhenDataAndFileHierarchiesHaveTheSameNames test
mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
mvn test
```

A primeira execução do teste específico usava dois `assertAll` sequenciais e parou após as assertions de occurrence. A única mudança posterior foi agregá-los em um `assertAll` externo, sem alterar fixture nem expectativa, para registrar também os bindings principais na mesma execução.

Resultados finais:

- teste específico: 1 executado, 1 falha agregada em 2 grupos, 0 erros;
- classe: 9 executados, 2 falhas, 0 erros — os REDs das Fases 4 e 5; os 7 testes preexistentes permaneceram verdes;
- suíte completa: 62 executados, 5 falhas, 0 erros, 0 ignorados — exatamente os REDs acumulados das Fases 1 a 5;
- zero erros sintáticos na fixture.

| Caso | Esperado | Resultado atual | Classificação |
|---|---|---|---|
| qualifier de `ITEM OF QUALIFIER-X` | occurrence `DATA / dataName` | `DATA / dataName` | **PASS — estrutura DATA preservada** |
| binding de `ITEM OF QUALIFIER-X` | selecionar somente o ITEM de working-storage | `AMBIGUOUS / MULTIPLE_VALID_CANDIDATES`; candidatos symbol IDs 4 e 7, incluindo o ITEM do FILE | **BUG CONFIRMADO** |
| qualifier final de `ITEM OF FILE-RECORD IN QUALIFIER-X` | occurrence `FILE / fileName` | `DATA / dataName` | **FRONTEND INCAPAZ DE REPRESENTAR nesta forma / NECESSITA INVESTIGAÇÃO** |
| binding de `ITEM OF FILE-RECORD IN QUALIFIER-X` | selecionar somente o ITEM do FILE | `AMBIGUOUS / MULTIPLE_VALID_CANDIDATES`; os mesmos IDs 4 e 7 | **NECESSITA INVESTIGAÇÃO**: resultado combina a limitação do frontend com a redução string-only do resolver |

Assertions essenciais:

```text
typed DATA/FILE qualifier collision (2 failures)
grammar-derived qualifier constraints retain semantic kind (2 failures)
  expected [FILE, DATA] but was [DATA]
  expected [fileName, dataName] but was [dataName]
principal DATA binding must consume qualifier name plus kind (2 failures)
  ITEM OF QUALIFIER-X: expected RESOLVED but was AMBIGUOUS
  ITEM OF FILE-RECORD IN QUALIFIER-X: expected RESOLVED but was AMBIGUOUS
```

O primeiro subcaso isola o bug do resolver: a occurrence DATA está correta e já contém informação suficiente para excluir ancestry FILE, mas o binding conserva ambos os candidatos. O subcaso FILE não prova isoladamente o comportamento do resolver sob uma constraint FILE, porque essa constraint não chegou ao produto semântico; isso foi classificado conservadoramente, sem declarar uma certeza que a evidência não fornece.

### Fase 6

Comandos executados antes de qualquer correção:

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#excludesCommonProgramAndItsDescendantsFromTheCommonProgramsCallingRegion test
mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
mvn test
```

Resultados:

- teste específico: 1 executado, 1 falha agregando 2 subcasos, 0 erros;
- classe: 10 executados, 3 falhas, 0 erros — REDs das Fases 4, 5 e 6; os 7 testes preexistentes permaneceram verdes;
- suíte completa: 63 executados, 6 falhas, 0 erros, 0 ignorados — exatamente os REDs acumulados das Fases 1 a 6;
- zero erros sintáticos e todas as 12 assertions de topologia/categoria passaram.

| Caller → target | Regra/expectativa | Resultado atual | Classificação |
|---|---|---|---|
| `COMMON-ROOT → COMMON-C` | target diretamente contido; `RESOLVED` para `[0,1]` | igual ao esperado | **PASS — positivo do pai** |
| `SIBLING → COMMON-C` | sibling COMMON válido; `RESOLVED` para `[0,1]` | igual ao esperado | **PASS — positivo COMMON** |
| `DESCENDANT → COMMON-C` | caller está no subtree do target; invisível internamente | `RESOLVED / UNIQUE_VISIBLE_DECLARATION` para `[0,1]`, candidato `common=true` | **BUG CONFIRMADO** |
| `SIBLING → PRIVATE-SIBLING` | sibling não-COMMON invisível | `UNRESOLVED / EXTERNAL_CATALOG_NOT_PROVIDED`, 0 candidatos | **PASS — negativo privado** |
| `A → DEEP-C` | target diretamente contido; `RESOLVED` para `[0,3,1]` | igual ao esperado | **PASS — positivo profundo do pai** |
| `B → DEEP-C` | sibling COMMON válido em nível profundo; `RESOLVED` | igual ao esperado | **PASS — positivo profundo COMMON** |
| `D → DEEP-C` | caller está no subtree do target; invisível internamente | `RESOLVED / UNIQUE_VISIBLE_DECLARATION` para `[0,3,1]`, candidato `common=true` | **BUG CONFIRMADO** |
| `COMMON-ROOT → DEEP-C` | caller está fora da região contendo `A`; invisível internamente | `UNRESOLVED / EXTERNAL_CATALOG_NOT_PROVIDED`, 0 candidatos | **PASS — limite externo da região** |

Assertions essenciais:

```text
COMMON scope excludes the COMMON program subtree (2 failures)
DESCENDANT -> COMMON-C: expected UNRESOLVED but was RESOLVED
  candidate PROGRAM_UNIT [0,1], common=true
D -> DEEP-C: expected UNRESOLVED but was RESOLVED
  candidate PROGRAM_UNIT [0,3,1], common=true
```

`EXTERNAL_CATALOG_NOT_PROVIDED` é o reason contratual atual quando não existe target interno visível e nenhum catálogo externo foi fornecido. O teste exige conservadoramente ausência de candidato interno; não afirma que inexista um programa externo homônimo.

### Fase 7

Comandos executados antes de qualquer correção:

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#resolvesRedefinesWithinItsStructuralLevelAndRejectsInvalidHierarchyTargets test
mvn -Dtest=DataAndIndexReferenceResolverTest test
mvn test
```

Resultados:

- teste específico: 1 executado, 1 falha agregando os 2 casos, 0 erros;
- classe: 11 executados, 4 falhas, 0 erros — REDs das Fases 1, 2, 3 e 7; os 7 testes preexistentes permaneceram verdes;
- suíte completa: 64 executados, 7 falhas, 0 erros, 0 ignorados — exatamente os REDs acumulados das Fases 1 a 7;
- zero erros sintáticos e todos os checks estruturais anteriores ao binding passaram.

| Relação | Esperado | Resultado atual | Classificação |
|---|---|---|---|
| `GROUP-A.Y REDEFINES X` | `RESOLVED / UNIQUE_VISIBLE_DECLARATION`, somente `GROUP-A.X` (symbol ID 2) | `AMBIGUOUS / MULTIPLE_VALID_CANDIDATES`; `GROUP-A.X` ID 2 e `GROUP-B.X` ID 5 | **BUG CONFIRMADO** |
| `GROUP-C.BAD-Y REDEFINES DEEP-X` | rejeição estrutural: `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`, 0 candidatos | `RESOLVED / UNIQUE_VISIBLE_DECLARATION`; seleciona `SUBGROUP-C.DEEP-X` ID 8 | **BUG CONFIRMADO** |

Assertions essenciais:

```text
REDEFINES selection is constrained by the declaring item's structural level (2 failures)
Y REDEFINES X:
  expected RESOLVED but was AMBIGUOUS
  expected 1 candidate but was 2 (symbol IDs 2 and 5)
different-level target is rejected instead of nominally bound:
  expected UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT / 0 candidates
  but was RESOLVED / UNIQUE_VISIBLE_DECLARATION / candidate symbol ID 8
```

O caso negativo não é um erro sintático artificial: a grammar versionada aceita `REDEFINES dataName`, mas as restrições de nível/hierarquia são semânticas. O teste preserva a expectativa da linguagem e exige diagnóstico conservador em vez de binding nominal aparentemente certo.

### Fase 8

Comandos executados antes de qualquer correção:

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#resolvesRenamesWithinItsLogicalRecordAndRejectsCrossRecordRanges test
mvn -Dtest=DataAndIndexReferenceResolverTest test
mvn test
```

Resultados:

- teste específico: 1 executado, 1 falha agregando 4 grupos, 0 erros;
- classe: 12 executados, 5 falhas, 0 erros — REDs das Fases 1, 2, 3, 7 e 8; os 7 testes preexistentes permaneceram verdes;
- suíte completa: 65 executados, 8 falhas, 0 erros, 0 ignorados — exatamente os REDs acumulados das Fases 1 a 8;
- zero erros sintáticos e todos os checks de logical-record ownership passaram.

| Relação | Esperado | Resultado atual | Classificação |
|---|---|---|---|
| `RANGE-A RENAMES START-X` | FROM resolve somente para `RECORD-A.START-X` ID 2 | `AMBIGUOUS`; IDs 2 e 6 (`RECORD-B.START-X`) | **BUG CONFIRMADO** |
| `RANGE-A ... THRU END-X` | THROUGH resolve somente para `RECORD-A.END-X` ID 3 | `AMBIGUOUS`; IDs 3 e 7 (`RECORD-B.END-X`) | **BUG CONFIRMADO** |
| `CROSS-RANGE RENAMES CROSS-START` | range cruzado diagnosticado; FROM `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT` | `RESOLVED / UNIQUE_VISIBLE_DECLARATION`; ID 9 em `RECORD-C` | **BUG CONFIRMADO** |
| `CROSS-RANGE ... THRU CROSS-END` | toda a relação inválida deve permanecer conservadora; THROUGH sem binding certo | `RESOLVED / UNIQUE_VISIBLE_DECLARATION`; ID 11 em `RECORD-D` | **BUG CONFIRMADO** |

Assertions essenciais:

```text
RENAMES endpoints are selected and validated within the owner logical record (4 failures)
valid FROM: expected RESOLVED/1 but was AMBIGUOUS/2 (IDs 2, 6)
valid THROUGH: expected RESOLVED/1 but was AMBIGUOUS/2 (IDs 3, 7)
cross-record FROM: expected UNRESOLVED/INVALID_NAMESPACE_FOR_CONTEXT/0
  but was RESOLVED/UNIQUE_VISIBLE_DECLARATION/ID 9
cross-record THROUGH: expected UNRESOLVED/INVALID_NAMESPACE_FOR_CONTEXT/0
  but was RESOLVED/UNIQUE_VISIBLE_DECLARATION/ID 11
```

O caso negativo foi aceito sintaticamente porque a grammar representa a forma da cláusula; a validade de ambos os endpoints dentro do mesmo logical record é uma restrição semântica. Marcar apenas o endpoint local como resolvido esconderia a invalidade do range completo, por isso o teste exige resultado conservador para as duas relações derivadas da cláusula inválida.

### Fase 9

Comandos executados antes de qualquer correção:

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#separatesLiteralProgramNameSpellingFromCanonicalProgramIdentity test
mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
mvn test
```

Resultados:

- teste específico: 1 executado, 1 falha agregada em 2 grupos, 0 erros;
- classe: 11 executados, 4 falhas, 0 erros — REDs das Fases 4, 5, 6 e 9; os 7 testes preexistentes permaneceram verdes;
- suíte completa: 66 executados, 9 falhas, 0 erros, 0 ignorados — exatamente os REDs acumulados das Fases 1 a 9;
- zero erros sintáticos na fixture.

| Produto | Esperado | Resultado atual | Classificação |
|---|---|---|---|
| grafia declarada | `'CHILD'` preservado como representação escrita | `Ast.Program.name='CHILD'`; `ProgramAttributes.writtenText` contém `'CHILD'` | **PASS — grafia preservada** |
| identidade do programa declarado | `ProgramUnitId.canonicalProgramName=CHILD` | `canonicalProgramName='CHILD'` | **BUG CONFIRMADO** |
| target do CALL | `programName=CHILD`, `writtenText='CHILD'` | igual ao esperado | **PASS — referência separa semântica/grafia** |
| occurrence | PROGRAM, `writtenText='CHILD'` | igual ao esperado | **PASS** |
| binding | `RESOLVED / UNIQUE_VISIBLE_DECLARATION`, candidato PROGRAM_UNIT do filho | `UNRESOLVED / EXTERNAL_CATALOG_NOT_PROVIDED`, 0 candidatos | **BUG CONFIRMADO** |

Assertions essenciais:

```text
literal program name representation and binding (2 failures)
written spelling is distinct from semantic identity:
  expected canonical CHILD but was 'CHILD'
CALL literal binds to the literal PROGRAM-ID semantic identity:
  expected RESOLVED / UNIQUE_VISIBLE_DECLARATION / 1 candidate
  but was UNRESOLVED / EXTERNAL_CATALOG_NOT_PROVIDED / 0 candidates
```

O reason externo não significa que a declaração nested não exista: significa que o lookup interno não a encontrou pela chave canônica divergente e seguiu para o fallback de catálogo ausente.

### Fase 10

Comandos executados antes de qualquer correção:

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#requiresExplicitPgmnamePolicyWhenProgramIdentityDependsOnCompilerOption test
mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
mvn test
```

A primeira tentativa do teste específico encontrou um parêntese excedente no próprio teste e falhou em `testCompile`. Esse evento foi classificado como **TESTE INVÁLIDO temporário**, corrigido somente no código de teste e não usado como evidência RED semântica. A revisão do desenho também substituiu o controle inicial `CONVENTIONAL` (mais de oito caracteres e, portanto, não neutro sob `COMPAT`) por `SIMPLE`; isso ocorreu antes das execuções finais abaixo e preservou as expectativas adversariais. Após essas correções exclusivamente de teste, os resultados reproduzíveis foram:

- teste específico: 1 executado, 1 falha agregada em 3 grupos, 0 erros;
- classe: 12 executados, 5 falhas, 0 erros — REDs das Fases 4, 5, 6, 9 e 10; os 7 testes preexistentes permaneceram verdes;
- suíte completa: 67 executados, 10 falhas, 0 erros, 0 ignorados — exatamente os REDs acumulados das Fases 1 a 10;
- `CBL PGMNAME(LONGMIXED)` foi aceito pela grammar do preprocessor com zero erros e árvore contendo tanto `PGMNAME` quanto `LONGMIXED`.

| Contrato/caso | Esperado | Resultado atual | Classificação |
|---|---|---|---|
| grammar do preprocessor | reconhecer `PGMNAME(LONGMIXED)` | reconhece sem erro | **PASS** |
| transporte da opção | `PreprocessorEngine.Outcome` expõe compiler option/PGMNAME | `Outcome` não possui componente correspondente | **LACUNA DE FRONTEND / INFORMAÇÃO PERDIDA** |
| política nominal | `CobolResolutionPolicy` possui modo PGMNAME explícito | record contém somente `policyId`, `version`, `QualifyMode` | **LACUNA DE DIALETO / BUG CONFIRMADO** |
| `SIMPLE` | resolução permitida no controle option-independent | `RESOLVED`, candidato externo `SIMPLE` | **PASS** |
| `mixed-Child` sem modo conhecido | `UNSUPPORTED / UNSUPPORTED_DIALECT_OPTION`, sem candidato | `RESOLVED`, candidato externo fabricado sob `MIXED-CHILD` | **BUG CONFIRMADO** |
| `LONG-NAME-ABC` sem modo conhecido | `UNSUPPORTED / UNSUPPORTED_DIALECT_OPTION`, sem candidato | `RESOLVED`, candidato externo sob o nome longo intacto em uppercase | **BUG CONFIRMADO** |

Assertions essenciais:

```text
PGMNAME is an explicit input to program identity (3 failures)
PreprocessorEngine.Outcome must transport parsed compiler options:
  expected true but was false
CobolResolutionPolicy must expose an explicit PGMNAME mode:
  expected true but was false
mixed-Child: expected UNSUPPORTED but was RESOLVED (MIXED-CHILD)
LONG-NAME-ABC: expected UNSUPPORTED but was RESOLVED (LONG-NAME-ABC)
```

O teste não presume que a opção possa ser inferida da unidade analisada. Ele exige configuração/transporte explícito e, na ausência do dado, recusa conservadora apenas para nomes cuja identidade realmente depende do modo.

### Fase 11 — consolidação do RED baseline

Comando executado novamente, ainda antes de qualquer correção de produção:

```text
mvn test
```

Resultado reproduzido em 2026-08-24:

- 67 testes executados;
- 10 falhas;
- 0 erros;
- 0 ignorados;
- 57 testes preexistentes verdes;
- 10 testes adversariais RED, um por fase de 1 a 10.

Os relatórios Surefire confirmam os dez métodos falhos abaixo, sem falhas adicionais:

```text
inheritsGlobalGroupVisibilityForDataConditionAndIndexSubordinates
stopsAtIncompatibleLocalNamesBeforeFilteringByReferenceKind
appliesQualificationAcrossLocalAndInheritedGlobalCandidatesBeforeSelectingAProgramUnit
resolvesOnlyGlobalAncestorFilesAndPreservesFileEntityOwnershipAndDeclarations
preservesQualifierKindWhenDataAndFileHierarchiesHaveTheSameNames
excludesCommonProgramAndItsDescendantsFromTheCommonProgramsCallingRegion
resolvesRedefinesWithinItsStructuralLevelAndRejectsInvalidHierarchyTargets
resolvesRenamesWithinItsLogicalRecordAndRejectsCrossRecordRanges
separatesLiteralProgramNameSpellingFromCanonicalProgramIdentity
requiresExplicitPgmnamePolicyWhenProgramIdentityDependsOnCompilerOption
```

Não houve alteração de código de produção entre a primeira evidência RED de cada fase e esta consolidação.

## 4. CAUSA RAIZ

### Diagnóstico preliminar da Fase 1

Para DATA e CONDITION, o caminho incorreto é reproduzível:

1. o pai `GLOBAL-GROUP` recebe `visibility=GLOBAL`;
2. `GLOBAL-CHILD`, `STATUS-FIELD` e `STATUS-OK` conservam sua visibilidade sintática individual `LOCAL`;
3. `SymbolTableBuilder.dataAttributes` materializa esse `LOCAL` individualmente;
4. `DataAndIndexReferenceResolver.compatibleCandidates` encontra o nome e o kind no ancestor;
5. `localOrInheritedGlobal` descarta o candidato porque verifica apenas `owner.symbol().attributes().get("visibility") == "GLOBAL"`, sem considerar os scopes/owners ancestrais;
6. o resultado torna-se `DECLARATION_NOT_FOUND`.

Para INDEX existem duas decisões independentes:

1. `SymbolTableBuilder.collectDataEntries` cria corretamente `GLOBAL-IDX` como `INDEX_NAME`, mas não registra visibilidade efetiva herdada no símbolo;
2. no uso `TABLE-ITEM(GLOBAL-IDX)`, a grammar aceita o token pelo ramo `qualifiedDataName` do `subscript`; o AST/collector então fixa `ReferenceKind.DATA` antes de qualquer consulta às declarações. O resolver encontra um nome no namespace DATA, mas rejeita `INDEX_NAME` como incompatível com o kind DATA e retorna `INVALID_NAMESPACE_FOR_CONTEXT`.

Os testes antigos não detectavam o problema porque `nested-data-visibility.cbl` cobre apenas um level-01 que contém a própria cláusula `GLOBAL`, e `data-binding.cbl` cobre `TABLE-IDX` no próprio programa por meio da ocorrência declarativa `OCCURS_INDEX`. Nenhum deles combina declaração GLOBAL no grupo, nome subordinado e uso em programa contido.

Nenhuma correção foi implementada nesta fase. O diagnóstico não prescreve ainda onde materializar a visibilidade efetiva nem como representar a admissibilidade DATA/INDEX de subscripts; essas decisões ficam reservadas à Fase 12 após a consolidação RED.

### Diagnóstico preliminar da Fase 2

O caminho algorítmico incorreto está em `DataAndIndexReferenceResolver.resolveDataOccurrence`:

```text
compatibleCandidates(startingUnitId, canonical, occurrence.kind())
→ localOrInheritedGlobal(startingUnitId, compatible)
```

`compatibleCandidates` percorre filho e ancestors, mas adiciona somente symbols aprovados por `compatible(symbol, kind)`. Para um uso DATA:

- o `CONDITION_NAME` ou `INDEX_NAME` local é descartado imediatamente;
- o `DATA_ITEM GLOBAL` do pai permanece;
- `localOrInheritedGlobal` recebe uma lista que já perdeu o homônimo local;
- como não vê candidato local compatível, escolhe o GLOBAL externo e declara certeza.

`hasSameNameInSearchPath` não evita o problema porque só participa do ramo em que a lista final de candidatos está vazia. Aqui existe um candidato externo compatível, portanto o resolver nunca considera que o nome local deveria ter encerrado a busca.

Os testes antigos exercitavam shadowing somente entre dois DATA_ITEMs compatíveis (`OUTER-LOCAL`) e, por isso, o candidato local sobrevivia ao filtro antecipado. Eles não continham colisões entre `DATA_ITEM`, `CONDITION_NAME` e `INDEX_NAME` em ProgramUnits diferentes.

Nenhuma correção foi realizada. A correção futura deverá preservar lookup indexado por nome/unidade e separar a decisão de nível nominal da validação de kind, sem scan global por ocorrência.

### Diagnóstico preliminar da Fase 3

O caminho atual em `DataAndIndexReferenceResolver.resolveDataOccurrence` é:

```text
compatibleCandidates
→ localOrInheritedGlobal
→ applyQualification
```

Para as três referências, `compatibleCandidates` encontra os dois `VALUE-X`, pois ambos são DATA_ITEMs e os índices por nome/unidade estão funcionando. Em seguida:

- `localOrInheritedGlobal` detecta o candidato do filho e retorna imediatamente somente esse candidato;
- `VALUE-X` permanece corretamente local;
- `VALUE-X OF INNER-GROUP` casa com a ancestry local;
- para `VALUE-X OF OUTER-GROUP`, `applyQualification` recebe apenas o candidato local, rejeita sua ancestry e produz lista vazia;
- o candidato externo que casaria com `OUTER-GROUP` já foi descartado e não pode ser recuperado.

A Fase 1 acrescenta uma segunda condição necessária para o GREEN futuro: a visibilidade efetiva de `VALUE-X` precisa ser herdada de `OUTER-GROUP GLOBAL`. Mesmo após isso, a ordem atual continuaria descartando o candidato externo antes da qualification; portanto, a Fase 3 confirma uma decisão algorítmica distinta, embora os dois bugs interajam na mesma referência.

Os testes antigos verificavam qualification entre homônimos dentro do mesmo `ProgramUnit` ou GLOBAL sem homônimo local. Nenhum combinava `OF`, candidato local e candidato GLOBAL em outro `ProgramUnit`.

Nenhuma correção foi realizada nesta fase.

### Diagnóstico preliminar da Fase 4

O frontend preserva `Ast.FileDescription.visibility`, e `SymbolTableBuilder.collectDataDivision` também a coloca no symbol `FILE_DESCRIPTION`. A perda ocorre depois:

1. `SymbolTableBuilder.buildFileEntities` agrupa todas as declarações FILE pelo canonicalName e preserva associação, assignments, `hasSelect`, `hasDescription` e `declarationSymbolIds`;
2. porém, o método não deriva nem copia `visibility` do `FILE_DESCRIPTION` para `SymbolTable.Entity.attributes`;
3. `CobolReferenceResolver.fileCandidate` copia apenas os atributos da entidade, logo candidatos locais também não expõem visibilidade.

A busca falha por uma decisão separada:

1. `CobolReferenceResolver.resolveFile` obtém somente `units.get(occurrence.programUnitId())`;
2. consulta apenas `unit.files().get(canonicalName)`;
3. não percorre a cadeia de `parentId` nem pode filtrar ancestors por visibilidade;
4. tanto SELECT + FD quanto FD-only GLOBAL do pai ficam invisíveis ao filho.

O shadowing local funciona incidentalmente porque a entidade local está no único índice consultado. O FILE local do pai também fica corretamente invisível, mas pelo motivo amplo de nenhum FILE ancestral ser consultado, não por aplicação explícita da regra LOCAL/GLOBAL.

Os testes antigos cobriam associação SELECT + FD, SELECT-only e FD-only somente dentro de um único `ProgramUnit`; não exigiam visibilidade na entidade nem busca entre programas aninhados.

Nenhuma correção foi realizada. Uma correção futura deve continuar usando índices FILE por nome/unidade e percorrer somente a cadeia de units pertinente, sem scan global por referência.

### Diagnóstico preliminar da Fase 5

Há duas camadas distintas:

1. `AstBuilder.buildQualifiers` preserva a origem de cada `Ast.DataQualifier`, e `ReferenceOccurrenceCollector.qualifierKind` consulta o manifesto para mapear `inData` a DATA e `inFile` a FILE;
2. para a forma geral exercitada, a parse tree produz `dataName` nos dois `QUALIFIER-X`, de modo que o segundo qualifier chega ao collector como DATA. A ambiguidade lexical/alternativa da grammar impede que o frontend represente a constraint FILE pretendida nesse caso, embora formas especiais como `LINAGE_COUNTER inFile` consigam fazê-lo.

Mesmo quando o frontend fornece a informação correta — o primeiro qualifier é inequivocamente DATA — o resolver a perde:

```text
Ast.DataQualifier(name, origin semântica)
→ applyQualification: map(Ast.DataQualifier::name)
→ List<String>
→ orderedSubsequence(qualifiers, ancestry)
```

`DataAndIndexReferenceResolver.ancestry` também retorna `List<String>` e inclui, no mesmo domínio nominal, ancestors `DATA_ITEM` e `FILE_DESCRIPTION`. Como ambos se chamam `QUALIFIER-X`, os dois ITEMs sobrevivem e o resultado conservador atual é `AMBIGUOUS`.

Logo, a Fase 5 confirma que o resolver precisa preservar conceitualmente `canonicalName + semanticKind` durante qualification. Isso não foi implementado. A direção de correção não autoriza heurística textual nem inferência pelo conector `IN`/`OF`; a limitação do frontend deverá ser tratada separadamente antes de afirmar suporte completo ao lado FILE.

Os testes antigos não detectavam o problema porque não combinavam ancestry DATA e FILE com os mesmos nomes. O teste que observava um qualifier FILE usava a alternativa especial de `LINAGE-COUNTER`, não a forma geral adversarial.

### Diagnóstico preliminar da Fase 6

`CobolReferenceResolver.visibleInternalProgram` implementa três permissões:

```text
caller == target
OU target.parentId == caller.id
OU existe ancestorParent do caller tal que
   target.parentId == ancestorParent E target.common
```

A terceira condição identifica corretamente siblings COMMON, inclusive em níveis profundos, mas não delimita o subtree proibido. Para `DESCENDANT → COMMON-C`:

1. a subida começa em `COMMON-C`, parent direto do caller;
2. não há match porque `COMMON-C.parentId` é `COMMON-ROOT`;
3. a subida continua até `COMMON-ROOT`;
4. agora `target.parentId == COMMON-ROOT` e `target.common == true`;
5. o método retorna `true`, embora a subida tenha atravessado o próprio target.

O mesmo acontece em `D → DEEP-C` ao atravessar `DEEP-C` e alcançar `A`. A informação estrutural necessária já existe em `ProgramUnit.parentId`; não há lacuna de frontend ou dialeto.

Os testes antigos cobriam apenas `PRIVATE-CHILD → COMMON-CHILD`, um sibling COMMON positivo, e um sibling privado negativo. Não havia caller dentro do target COMMON nem repetição da regra em profundidade maior.

Nenhuma correção foi realizada. A correção futura deve usar somente a cadeia indexada de `ProgramUnitId`s e interromper/rejeitar quando o caller estiver no subtree do target, sem scan global por ocorrência.

### Diagnóstico preliminar da Fase 7

O contexto estrutural existe na tabela, mas não participa da resolução da relação:

1. `SymbolTableBuilder.collectDeclarationRelations` registra `RelationKind.REDEFINES`, `ownerSymbolId` e `referenceAstNodeId` corretamente;
2. `ReferenceOccurrenceCollector` emite para o target uma ocorrência DATA com role `REDEFINES_TARGET`;
3. `DataAndIndexReferenceResolver.resolveDataOccurrence` procura todos os DATA_ITEMs homônimos no índice por nome do ProgramUnit, aplicando somente visibilidade e qualification genéricas;
4. como a sintaxe de REDEFINES não admite qualification explícita, os dois `X` permanecem e o resultado fica artificialmente `AMBIGUOUS`;
5. `resolveRelations` apenas recupera a `ReferenceResolution.Entry` por `referenceAstNodeId` e copia status, reason e candidates para `DeclarationRelationResolution`; `relation.ownerSymbolId` não influencia a seleção.

No caso negativo, a mesma reutilização encontra o único `DEEP-X` nominal e o declara resolvido, sem comparar o scope/nível do owner `BAD-Y` com o target. Isso confirma que não é apenas um problema de ambiguidade: falta validação estrutural mesmo quando o nome é único.

Os testes antigos continham somente `REDEFINING-ITEM REDEFINES REDEFINED-ITEM` com nomes globalmente únicos no ProgramUnit e verificavam apenas que todas as relações REDEFINES estavam `RESOLVED`. Eles não afirmavam o symbol ID selecionado nem apresentavam homônimos ou relação inválida.

Nenhuma correção foi realizada. Uma correção futura deve resolver REDEFINES a partir do owner e de índices locais/estruturais já existentes, preservando ambiguidade quando a estrutura não bastar e sem scan global por relação.

### Diagnóstico preliminar da Fase 8

A informação necessária é preservada antes do resolver:

1. `AstBuilder.buildDataHierarchy` trata level 66 separadamente e o anexa ao draft do level-01 associado;
2. `SymbolTableBuilder.collectDataEntries` materializa o RENAMES no scope desse logical record;
3. `SymbolTableBuilder.collectDeclarationRelations` preserva o mesmo `ownerSymbolId` para `RENAMES_FROM` e `RENAMES_THROUGH`.

Depois disso, o caminho é o mesmo binding DATA genérico diagnosticado na Fase 7:

```text
ReferenceOccurrence DATA por endpoint
→ resolveDataOccurrence por canonicalName no ProgramUnit
→ resolveRelations copia status/reason/candidates por referenceAstNodeId
```

O owner level-66 nunca limita o índice nominal ao logical record associado. Por isso os homônimos de RECORD-B causam ambiguidade artificial. Além disso, FROM e THROUGH são tratados independentemente: nenhuma etapa verifica que ambos pertencem ao mesmo level-01 do owner nem que formam uma faixa estrutural válida. Consequentemente, os dois nomes únicos do range cruzado parecem resolvidos isoladamente e a invalidade da cláusula desaparece do produto semântico.

Os testes antigos usavam nomes únicos e apenas afirmavam que todas as relações RENAMES estavam `RESOLVED`; não verificavam os `SemanticEntityId.localId`s. A fixture `data-binding.cbl` também contém uma forma cujo range envolve itens level-01, mas nenhum teste anterior validava sua conformidade estrutural — prova adicional de que “status RESOLVED” não demonstrava correção semântica.

Nenhuma correção foi realizada. Uma correção futura deve selecionar endpoints usando o owner logical record e validar FROM/THROUGH como uma única cláusula antes de publicar as duas entradas, usando scopes/índices existentes e sem scan global por ocorrência.

### Diagnóstico preliminar da Fase 9

O frontend trata declaração e referência de forma assimétrica:

1. `AstBuilder.buildProgramUnit` constrói `Ast.Program.name` com `clean(sourceText(programName))`; `clean` apenas compacta whitespace e preserva aspas;
2. `collectProgramUnits` cria o `ProgramUnitId` aplicando `SymbolTable.canonical`, que somente converte para uppercase, resultando em `'CHILD'`;
3. `CobolReferenceResolver.buildIndexes` indexa o filho em `programsByName` sob a chave `'CHILD'`;
4. `AstBuilder.buildCall` usa `unquote` no literal e produz `ProgramReference.programName=CHILD`, preservando separadamente `writtenText='CHILD'`;
5. `resolveProgram` procura a chave `CHILD`, não encontra o filho indexado como `'CHILD'` e cai no catálogo externo ausente.

A informação escrita não precisa ser perdida: `ProgramAttributes.writtenText`, spans e provenance já preservam o fonte. A correção futura deve definir uma identidade semântica comum para declaração e referência, sem executar remoção global de aspas em nomes ou textos de origem. A política de case e caracteres dependente de PGMNAME não é inferida nesta fase.

Os testes antigos continham apenas `PROGRAM-ID` em forma de cobolWord e CALLs literais; portanto, declaração e referência convergiam após uppercase. Nenhum declarava um nested program com literal.

Nenhuma correção foi realizada.

### Diagnóstico preliminar da Fase 10

O caminho de perda e falsa certeza foi confirmado em quatro camadas:

1. `CobolPreprocessor.g4` representa `compilerOption` e enumera os modos de `PGMNAME`;
2. `PreprocessorEngine.Collector` torna apenas `COPY` e `EXEC` acionáveis, enquanto `PreprocessorEngine.Outcome` não transporta compiler options; a informação reconhecida pela grammar desaparece antes da análise semântica;
3. `CobolResolutionPolicy` modela somente `policyId`, `version` e `QualifyMode`, embora o policy ID declare genericamente `proleap-cobol/ibm-enterprise-compatible`;
4. `SymbolTable.canonical` e `CobolReferenceResolver.resolveProgram` reduzem nomes a uppercase e consultam o catálogo com essa identidade, independentemente de truncamento, tradução de hífen ou preservação de case.

Assim, `mixed-Child` é transformado em `MIXED-CHILD`, e `LONG-NAME-ABC` permanece longo e hifenizado; ambos são entregues ao catálogo como se a identidade estivesse determinada. Isso conflita respectivamente com `LONGMIXED` e `COMPAT` e transforma ausência de configuração em certeza.

Os testes anteriores só usavam nomes convencionais para os quais uppercase simples era suficiente e não inspecionavam o transporte de compiler options. Por isso o policy ID amplo não tinha seu contrato confrontado com modos IBM distintos.

Nenhuma correção foi implementada. Uma correção futura deve transportar ou receber explicitamente o modo PGMNAME, separar grafia de identidade externa e retornar `UNSUPPORTED_DIALECT_OPTION` quando o binding depender de uma configuração ausente. Não se deve assumir a opção de uma instalação/invocação externa nem implementar uma remoção/tradução indiscriminada de caracteres.

## 5. CORREÇÃO

### Tasklist executada na Fase 12

Cada item abaixo foi tratado como grupo TDD independente: teste adversarial RED preservado, causa raiz delimitada, correção mínima, teste específico, teste do grupo e suíte completa. O checkpoint de autorização entre grupos foi dispensado pelo usuário para esta sessão longa; nenhuma correção de grupo posterior foi antecipada.

- [x] GLOBAL DATA/CONDITION/INDEX subordinados;
- [x] shadowing nominal antes de namespace/kind;
- [x] qualification antes da seleção local/GLOBAL;
- [x] GLOBAL FILE entre programas aninhados;
- [x] qualifier DATA vs FILE tipado;
- [x] COMMON program e descendentes;
- [x] REDEFINES estrutural;
- [x] RENAMES estrutural;
- [x] identidade semântica de PROGRAM-ID literal;
- [x] PGMNAME explícito e binding conservador;
- [x] regressão Maven, JavaScript, performance e integridade dos arquivos protegidos.

### Fase 12 — Grupo 1: GLOBAL DATA/CONDITION/INDEX subordinados

Causa raiz tratada: `SymbolTableBuilder` materializava `visibility` somente a partir da cláusula da declaração corrente, e o collector classificava um `qualifiedDataName` em posição de subscript somente como DATA.

Arquivos de produção modificados:

- `SymbolTableBuilder.java`: propaga a visibilidade GLOBAL efetiva durante a travessia hierárquica de data entries e a grava também em CONDITION e INDEX subordinados;
- `ReferenceOccurrenceCollector.java`: posição de subscript passa a ter INDEX como kind contextual primário e `{DATA, INDEX}` como kinds admissíveis;
- `DataAndIndexReferenceResolver.java`: seleção usa `admissibleKinds` e deriva kind/domínio do candidato a partir do `SymbolKind` declarado.

A mudança é geral porque segue a hierarquia já representada no AST/tabela e o conjunto de kinds do contrato de occurrences. Não depende dos nomes da fixture, não cria símbolos de uso e não introduz scan global: os candidatos continuam vindo do índice `byName` por ProgramUnit.

Nenhuma correção dos demais grupos foi incluída.

### Fase 12 — Grupo 2: shadowing nominal antes de namespace/kind

Causa raiz tratada: `compatibleCandidates` continuava consultando ancestors depois de encontrar declarações homônimas no `ProgramUnit` corrente, porque o critério de parada era aplicado implicitamente somente após o filtro de kind.

Arquivo de produção modificado: `DataAndIndexReferenceResolver.java`. A busca indexada por nome agora para no primeiro `ProgramUnit` com qualquer declaração homônima; somente as declarações desse nível nominal são avaliadas contra `admissibleKinds`. Nome local incompatível produz `INVALID_NAMESPACE_FOR_CONTEXT`, sem selecionar silenciosamente um GLOBAL externo.

A mudança mantém lookup por `byName`, inspeciona apenas o bucket do nome por unidade e não altera qualification explícita, reservada ao Grupo 3.

### Fase 12 — Grupo 3: qualification antes da seleção local/GLOBAL

Causa raiz tratada: a pipeline reduzia candidatos ao `ProgramUnit` local antes de aplicar `OF/IN`, tornando impossível selecionar um homônimo GLOBAL explicitamente qualificado.

Arquivo de produção modificado: `DataAndIndexReferenceResolver.java`. Referências não qualificadas continuam parando no primeiro nível nominal. Referências qualificadas consultam os buckets indexados do nome no caller e ancestors, preservam somente declarações locais ou GLOBAL herdadas e aplicam a hierarquia de qualifiers antes da decisão final.

A solução combina shadowing, GLOBAL e qualification sem scan de tabelas completas e sem usar ordem de declaração.

### Fase 12 — Grupo 4: GLOBAL FILE entre programas aninhados

Causas raiz tratadas:

- `buildFileEntities` não transferia a visibilidade da FILE DESCRIPTION para a entidade associada;
- `resolveFile` consultava somente o índice FILE do caller.

Arquivos de produção modificados:

- `SymbolTableBuilder.java`: a entidade FILE passa a expor visibilidade derivada das declarações FILE DESCRIPTION, preservando associação, SELECT/FD, FD-only e todos os declaration IDs;
- `CobolReferenceResolver.java`: lookup FILE caminha pelos `ProgramUnitId`s via buckets `files[canonicalName]`, escolhe primeiro a unidade nominal relevante, permite somente entidade GLOBAL herdada e preserva shadowing local.

Conflitos de visibilidade são materializados como `CONFLICTING`, nunca escolhidos como GLOBAL. Não há scan global por referência.

### Fase 12 — Grupo 5: qualifier DATA vs FILE tipado

O diagnóstico RED mostrou que a expectativa original de obter FILE na forma genérica era parcialmente inválida: `IN` e `OF` são equivalentes, e `dataName`/`fileName` compartilham a mesma forma lexical em `qualifiedDataNameFormat1`. O teste foi corrigido por fundamento de linguagem, não para concordar com a implementação: o qualifier genérico final agora exige `{DATA, FILE}` e ambiguidade conservadora quando ambos existem. Foi acrescentado o controle válido e inequívoco `LINAGE-COUNTER IN fileName`, que a grammar representa como `inFile`.

Antes da produção, o teste revisado permaneceu RED porque os qualifiers genéricos ainda continham somente `{DATA}`.

Arquivos de produção modificados:

- `Ast.java`/`AstBuilder.java`: `DataQualifier` preserva `QualifierTarget` (`DATA`, `FILE`, `DATA_OR_FILE`) derivado da estrutura da grammar;
- `ReferenceOccurrenceCollector.java`: transporta o target como primary/admissible kinds da occurrence;
- `DataAndIndexReferenceResolver.java`: usa `QualifierConstraint(canonicalName, admissibleKinds)` e ancestry tipada DATA/FILE em qualification normal e EXTEND.

Nenhum qualifier é inferido por texto, nome aparente ou consulta antecipada à tabela. Incerteza sintática permanece ambígua; `inFile` inequívoco não pode casar com ancestral DATA homônimo.

### Fase 12 — Grupo 6: COMMON program e descendentes

Causa raiz tratada: `visibleInternalProgram` subia pelos parents do caller e voltava a incluir o COMMON quando o caller pertencia à própria subárvore desse target.

Arquivo de produção modificado: `CobolReferenceResolver.java`. Antes de avaliar a calling region COMMON, o resolver agora rejeita callers que são descendentes do target. A verificação percorre somente a cadeia precomputada de `ProgramUnitId` parents; pai, sibling, sibling profundo, programa privado e região externa mantêm seus comportamentos próprios.

### Fase 12 — Grupo 7: REDEFINES estrutural

Causa raiz tratada: `resolveRelations` copiava o binding DATA genérico do target e ignorava `ownerSymbolId`, parent scope e level.

Arquivo de produção modificado: `DataAndIndexReferenceResolver.java`. Relations REDEFINES agora consultam `lookupLocal(owner.scopeId, DATA, writtenTarget)`, exigem `DATA_ITEM` do mesmo level e publicam decisão própria. Target homônimo em outro record não participa; nome presente somente em nível/hierarquia inválidos produz `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`.

O lookup é indexado por scope+namespace+nome e não usa ordem arbitrária nem scan global. As outras relations ainda seguem o caminho anterior até seus cards próprios.

### Fase 12 — Grupo 8: RENAMES estrutural

Causa raiz tratada: FROM e THROUGH eram copiados de bindings DATA independentes, sem logical-record boundary nem validação conjunta do range.

Arquivo de produção modificado: `DataAndIndexReferenceResolver.java`. Relations RENAMES são agrupadas por `ownerSymbolId`; cada endpoint consulta o índice DATA pelo nome e é filtrado à subárvore do scope owner. Se qualquer endpoint estiver fora do logical record, ou se um range único estiver invertido, FROM e THROUGH são ambos `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`. Ambiguidade interna permanece preservada.

A regressão revelou que o teste antigo `resolvesOrderedQualificationConditionIndexAndRelations` exigia `RESOLVED` para um level-66 que atravessa dois level-01 em `data-binding.cbl`. A fixture não foi alterada; a expectativa antiga foi corrigida para o diagnóstico estrutural conservador, enquanto OCCURS e REDEFINES continuam resolvidos. Essa mudança corrige uma asserção semanticamente inválida, não mascara regressão.

O lookup usa `lookupAll(DATA, name)` e filtra somente o bucket homônimo pela cadeia de scopes; não varre todos os símbolos por occurrence.

### Fase 12 — Grupo 9: identidade semântica de PROGRAM-ID literal

Causa raiz tratada: `AstBuilder.collectProgramUnits` aplicava uppercase diretamente a `Ast.Program.name`, preservando indevidamente delimitadores do literal na chave `ProgramUnitId`.

Arquivo de produção modificado: `AstBuilder.java`. Somente a construção da identidade semântica remove aspas antes da canonicalização atual. `Ast.Program.name`, `ProgramAttributes.writtenText`, spans, provenance e `ProgramReference.writtenText` permanecem inalterados.

A correção não remove aspas globalmente nem antecipa política PGMNAME externa; apenas alinha declaração literal e CALL literal na mesma identidade interna.

### Fase 12 — Grupo 10: PGMNAME explícito e conservador

Causa raiz tratada: a grammar reconhecia a compiler option, mas `PreprocessorEngine.Outcome` a descartava; `CobolResolutionPolicy` não possuía modo PGMNAME e `resolveProgram` aplicava uppercase simples, produzindo certeza indevida para nomes cuja identidade depende da opção.

Arquivos de produção modificados:

- `PreprocessorEngine.java`: coleta estruturalmente `compilerOption`, preserva `CompilerOption` e deriva `PgmnameMode` sem regex sobre o fonte;
- `ResolutionContracts.java`: adiciona `PgmnameMode` (`COMPAT`, `LONGUPPER`, `LONGMIXED`, `UNSPECIFIED`), configuração explícita e policy ID que não promete genericamente compatibilidade IBM;
- `ExplorerMain.java`: transporta o modo reconhecido para a análise;
- `CobolReferenceResolver.java`: canonicaliza o nome externo conforme o modo e retorna `UNSUPPORTED_DIALECT_OPTION` quando a configuração ausente realmente altera a identidade;
- `ResolutionSnapshot.java`: torna o modo efetivo observável no produto serializado.

`COMPAT` aplica uppercase, tradução de hífen para zero e limite de oito caracteres; `LONGUPPER` preserva o comprimento e aplica uppercase; `LONGMIXED` preserva case. Com `UNSPECIFIED`, nomes convencionais independentes da escolha continuam resolvíveis, enquanto mixed-case e nomes longos/hifenizados dependentes da opção permanecem conservadoramente unsupported. Modos não reconhecidos não são adivinhados.

Os testes legados que exercitam catálogo externo agora fornecem `LONGUPPER` explicitamente em seu helper, deixando clara a política sob a qual suas expectativas são válidas. O teste adversarial continua usando `UNSPECIFIED` para comprovar o comportamento conservador e também executa o preprocessor real com `CBL PGMNAME(LONGMIXED)` para provar o transporte ponta a ponta.

## 6. RESULTADO GREEN

O baseline preexistente possuía 57 testes Maven e 31 checks JavaScript verdes. Na Fase 11, os 57 testes preexistentes continuaram passando e os dez novos testes adversariais ficaram RED. As subseções seguintes preservam a progressão até o GREEN final.

### Fase 12 — Grupo 1 GREEN

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#inheritsGlobalGroupVisibilityForDataConditionAndIndexSubordinates test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=DataAndIndexReferenceResolverTest test
  12 testes, 4 falhas adversariais restantes, 0 erros

mvn test
  67 testes, 9 falhas adversariais restantes, 0 erros
```

O teste específico cobre DATA, CONDITION level-88 e INDEX, incluindo ownership pelo `ProgramUnitId` pai, domínios semânticos distintos e ausência de declarações locais inventadas. Os 57 testes preexistentes permaneceram verdes.

### Fase 12 — Grupo 2 GREEN

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#stopsAtIncompatibleLocalNamesBeforeFilteringByReferenceKind test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=DataAndIndexReferenceResolverTest test
  12 testes, 3 falhas adversariais restantes, 0 erros

mvn test
  67 testes, 8 falhas adversariais restantes, 0 erros
```

Os controles `CONTROL-GLOBAL` e as categorias locais CONDITION/INDEX também permaneceram corretos; os 57 testes preexistentes seguem verdes.

### Fase 12 — Grupo 3 GREEN

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#appliesQualificationAcrossLocalAndInheritedGlobalCandidatesBeforeSelectingAProgramUnit test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=DataAndIndexReferenceResolverTest test
  12 testes, 2 falhas adversariais restantes, 0 erros

mvn test
  67 testes, 7 falhas adversariais restantes, 0 erros
```

As três formas (`VALUE-X`, `VALUE-X OF INNER-GROUP`, `VALUE-X OF OUTER-GROUP`) agora permanecem semanticamente distintas. Os 57 testes preexistentes continuam verdes.

### Fase 12 — Grupo 4 GREEN

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#resolvesOnlyGlobalAncestorFilesAndPreservesFileEntityOwnershipAndDeclarations test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
  12 testes, 4 falhas adversariais restantes, 0 erros

mvn test
  67 testes, 6 falhas adversariais restantes, 0 erros
```

Passaram as variações SELECT+FD GLOBAL, FD-only GLOBAL, FILE local invisível, FILE homônimo local no filho, ownership, visibilidade e declaration IDs. Os 57 testes preexistentes seguem verdes.

### Fase 12 — Grupo 5 GREEN

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#preservesQualifierKindWhenDataAndFileHierarchiesHaveTheSameNames test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
  12 testes, 3 falhas adversariais restantes, 0 erros

mvn test
  67 testes, 5 falhas adversariais restantes, 0 erros
```

O teste cobre dois qualifiers genéricos ambíguos, um `inFile` inequívoco, resolução da occurrence FILE e seleção da referência principal por constraints tipadas. Os 57 testes preexistentes continuam verdes.

### Fase 12 — Grupo 6 GREEN

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#excludesCommonProgramAndItsDescendantsFromTheCommonProgramsCallingRegion test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
  12 testes, 2 falhas adversariais restantes, 0 erros

mvn test
  67 testes, 4 falhas adversariais restantes, 0 erros
```

Passaram os oito callers/topologias distintos, incluindo os dois descendentes negativos. Os 57 testes preexistentes permanecem verdes.

### Fase 12 — Grupo 7 GREEN

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#resolvesRedefinesWithinItsStructuralLevelAndRejectsInvalidHierarchyTargets test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=DataAndIndexReferenceResolverTest test
  12 testes, 1 falha adversarial restante, 0 erros

mvn test
  67 testes, 3 falhas adversariais restantes, 0 erros
```

O caso válido seleciona somente `GROUP-A.X`; o target de nível diferente é rejeitado. Os 57 testes preexistentes continuam verdes.

### Fase 12 — Grupo 8 GREEN

```text
mvn -Dtest=DataAndIndexReferenceResolverTest#resolvesRenamesWithinItsLogicalRecordAndRejectsCrossRecordRanges test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=DataAndIndexReferenceResolverTest#resolvesOrderedQualificationConditionIndexAndRelations test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=DataAndIndexReferenceResolverTest test
  12 testes, 0 falhas, 0 erros

mvn test
  67 testes, 2 falhas adversariais restantes, 0 erros
```

FROM/THROUGH válidos selecionam RECORD-A; o range RECORD-C/RECORD-D e o range level-01 legado são diagnosticados. Os 57 comportamentos do baseline continuam verdes, com a expectativa inválida explicitamente corrigida.

### Fase 12 — Grupo 9 GREEN

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#separatesLiteralProgramNameSpellingFromCanonicalProgramIdentity test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
  12 testes, 1 falha adversarial restante, 0 erros

mvn test
  67 testes, 1 falha adversarial restante, 0 erros
```

O nested program literal agora possui canonical identity `CHILD` e o CALL resolve para seu `SemanticEntityId`, enquanto a grafia `'CHILD'` continua observável. O restante do baseline permanece verde.

### Fase 12 — Grupo 10 GREEN

```text
mvn -Dtest=ProcedureFileProgramReferenceResolverTest#requiresExplicitPgmnamePolicyWhenProgramIdentityDependsOnCompilerOption test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=ResolutionSnapshotTest#writesDeterministicTraceableAndConservativeBrowserProjection test
  1 teste, 0 falhas, 0 erros

mvn -Dtest=ProcedureFileProgramReferenceResolverTest test
  12 testes, 0 falhas, 0 erros

mvn test
  67 testes, 0 falhas, 0 erros, 0 ignorados
```

O teste específico comprova `Outcome.pgmnameMode=LONGMIXED`, resolução explícita LONGMIXED, canonicalização COMPAT e `UNSUPPORTED_DIALECT_OPTION` sem opção. A primeira suíte completa revelou apenas o contrato serializado antigo; o snapshot foi atualizado para expor `pgmnameMode`, seu teste ficou verde e a suíte completa subsequente terminou com exit code 0 e 67 relatórios Surefire verdes.

### Regressão final

```text
mvn -q test
  67 testes, 0 falhas, 0 erros, 0 ignorados

mvn -q -Dtest=DataAndIndexReferenceResolverTest#usesPrebuiltNameIndexesInsteadOfScanningAllSymbolsPerReference test
  exit code 0

find src/main/resources/web dist dist-cbstm03a dist-cbstm03d \
  -type f -name '*.js' -exec node --check '{}' ';'
  31 arquivos, exit code 0

git diff --check
  exit code 0
```

O teste de performance confirma que a resolução continua usando índices de nome preconstruídos. `git diff` contra o commit inicial confirmou ausência de alterações em `corpus`, `src/main/antlr4`, `dist`, `dist-cbstm03a`, `dist-cbstm03d` e em fixtures previamente versionadas; somente as dez fixtures adversariais novas foram adicionadas. Não houve alteração em CFG, def-use, reaching definitions ou value resolution.

## 7. COBERTURA ADVERSARIAL FINAL

### RED baseline consolidado — Fase 11

Legenda: **FAIL** confirma divergência semântica; **PASS** registra controle positivo/negativo que já funciona; **LIMITAÇÃO** identifica informação que o frontend ou a configuração atual não consegue transportar com fidelidade suficiente.

| Caso | Teste | Esperado semanticamente | Resultado atual | Estado |
|---|---|---|---|---|
| GLOBAL child DATA | `inheritsGlobalGroupVisibilityForDataConditionAndIndexSubordinates` | `RESOLVED`, DATA do programa pai | `UNRESOLVED / DECLARATION_NOT_FOUND` | **FAIL — BUG CONFIRMADO** |
| GLOBAL level-88 | mesmo | `RESOLVED`, CONDITION do programa pai | `UNRESOLVED / DECLARATION_NOT_FOUND` | **FAIL — BUG CONFIRMADO** |
| GLOBAL index | mesmo | `RESOLVED`, INDEX do programa pai | occurrence DATA; `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT` | **FAIL — BUG CONFIRMADO** |
| shadowing por CONDITION local incompatível | `stopsAtIncompatibleLocalNamesBeforeFilteringByReferenceKind` | busca para no nome local; resultado conservador | salta para DATA GLOBAL externo e resolve | **FAIL — BUG CONFIRMADO** |
| shadowing por INDEX local incompatível | mesmo | busca para no nome local; resultado conservador | salta para DATA GLOBAL externo e resolve | **FAIL — BUG CONFIRMADO** |
| `VALUE-X` sem qualifier | `appliesQualificationAcrossLocalAndInheritedGlobalCandidatesBeforeSelectingAProgramUnit` | DATA local | DATA local | **PASS** |
| `VALUE-X OF INNER-GROUP` | mesmo | DATA local qualificado | DATA local qualificado | **PASS** |
| `VALUE-X OF OUTER-GROUP` | mesmo | DATA GLOBAL externo qualificado | `UNRESOLVED / DECLARATION_NOT_FOUND` | **FAIL — BUG CONFIRMADO** |
| GLOBAL FILE SELECT+FD | `resolvesOnlyGlobalAncestorFilesAndPreservesFileEntityOwnershipAndDeclarations` | FILE do pai, 2 declaration IDs | entidade/IDs existem; binding nested não encontra | **FAIL — BUG CONFIRMADO** |
| FILE FD-only GLOBAL | mesmo | FILE do pai, 1 declaration ID | entidade existe; binding nested não encontra | **FAIL — BUG CONFIRMADO** |
| FILE local no pai | mesmo | invisível ao filho | `UNRESOLVED` | **PASS** |
| FILE local homônimo no filho | mesmo | shadowing local | resolve entidade local | **PASS**, mas atributo visibility ausente |
| visibility da entidade FILE | mesmo | `GLOBAL`/`LOCAL` observável | atributo `null` em todas as variações | **FAIL — BUG CONFIRMADO** |
| DATA qualifier homônimo | `preservesQualifierKindWhenDataAndFileHierarchiesHaveTheSameNames` | qualifier DATA restringe ancestry DATA | principal fica `AMBIGUOUS` entre DATA/FILE hierarchies | **FAIL — BUG CONFIRMADO** |
| FILE qualifier homônimo | mesmo | occurrence FILE e ancestry FILE | grammar/manifest produzem somente occurrence DATA na forma geral exercitada; principal `AMBIGUOUS` | **LIMITAÇÃO DE FRONTEND + BUG STRING-ONLY CONFIRMADO** |
| pai chama COMMON filho | `excludesCommonProgramAndItsDescendantsFromTheCommonProgramsCallingRegion` | permitido | resolvido | **PASS** |
| sibling chama COMMON | mesmo | permitido | resolvido | **PASS** |
| descendente chama próprio ancestor COMMON | mesmo | invisível | resolvido indevidamente | **FAIL — BUG CONFIRMADO** |
| sibling profundo chama COMMON | mesmo | permitido | resolvido | **PASS** |
| descendente profundo chama próprio ancestor COMMON | mesmo | invisível | resolvido indevidamente | **FAIL — BUG CONFIRMADO** |
| sibling privado | mesmo | invisível | não resolvido internamente | **PASS** |
| REDEFINES válido com homônimos externos ao owner | `resolvesRedefinesWithinItsStructuralLevelAndRejectsInvalidHierarchyTargets` | `GROUP-A.X` único | `AMBIGUOUS` entre `GROUP-A.X` e `GROUP-B.X` | **FAIL — BUG CONFIRMADO** |
| REDEFINES em nível/hierarquia inválidos | mesmo | diagnóstico conservador, 0 candidatos | `RESOLVED` para target nominal único | **FAIL — BUG CONFIRMADO** |
| RENAMES FROM válido com homônimo | `resolvesRenamesWithinItsLogicalRecordAndRejectsCrossRecordRanges` | endpoint no logical record do owner | `AMBIGUOUS` entre records | **FAIL — BUG CONFIRMADO** |
| RENAMES THROUGH válido com homônimo | mesmo | endpoint no logical record do owner | `AMBIGUOUS` entre records | **FAIL — BUG CONFIRMADO** |
| RENAMES cruzando logical records | mesmo | ambos endpoints diagnosticados conservadoramente | ambos `RESOLVED` nominalmente | **FAIL — BUG CONFIRMADO** |
| PROGRAM-ID literal — grammar/grafia | `separatesLiteralProgramNameSpellingFromCanonicalProgramIdentity` | forma aceita e grafia `'CHILD'` preservada | zero syntax errors; grafia preservada | **PASS** |
| PROGRAM-ID literal — identidade | mesmo | canonical `CHILD` | canonical `'CHILD'` | **FAIL — BUG CONFIRMADO** |
| CALL para PROGRAM-ID literal | mesmo | filho nested resolvido | fallback externo; `UNRESOLVED / EXTERNAL_CATALOG_NOT_PROVIDED` | **FAIL — BUG CONFIRMADO** |
| PGMNAME na grammar | `requiresExplicitPgmnamePolicyWhenProgramIdentityDependsOnCompilerOption` | reconhecer `PGMNAME(LONGMIXED)` | reconhecido sem syntax errors | **PASS** |
| transporte/configuração PGMNAME | mesmo | opção em `Outcome` e modo na policy | ausente em ambos | **LIMITAÇÃO DE FRONTEND/DIALETO CONFIRMADA** |
| nome curto `SIMPLE` | mesmo | controle resolvido | resolvido | **PASS** |
| nome mixed-case sem modo | mesmo | `UNSUPPORTED_DIALECT_OPTION` | uppercase simples e `RESOLVED` | **FAIL — BUG CONFIRMADO** |
| nome longo/hifenizado sem modo | mesmo | `UNSUPPORTED_DIALECT_OPTION` | nome longo intacto em uppercase e `RESOLVED` | **FAIL — BUG CONFIRMADO** |

Resumo das hipóteses:

- confirmadas integralmente: GLOBAL subordinado, shadowing antes de kind, qualification antes de descarte de GLOBAL, GLOBAL FILE, COMMON-descendant, REDEFINES estrutural, RENAMES estrutural e PROGRAM-ID literal;
- confirmada com limitação adicional: qualifier DATA/FILE — o resolver realmente reduz constraints a nome, e a forma geral exercitada também revelou que o frontend não produz o qualifier FILE esperado;
- confirmada como lacuna de configuração mais falsa certeza: PGMNAME — a grammar conhece a opção, mas ela não é transportada/modelada e nomes dependentes do modo são resolvidos mesmo assim;
- hipóteses parcialmente refutadas/controles verdes: associação SELECT+FD e declaration IDs já são preservados; FD-only já forma entidade; shadowing FILE local funciona; casos positivos de COMMON funcionam; grammar aceita PROGRAM-ID literal; grafia do CALL literal já é separada; grammar do preprocessor já reconhece PGMNAME.

### Matriz GREEN final — Fase 12

| Caso adversarial | Resultado final | Estado |
|---|---|---|
| GLOBAL child DATA | DATA do programa pai, ownership correto | **GREEN** |
| GLOBAL level-88 | CONDITION do programa pai | **GREEN** |
| GLOBAL index | INDEX do programa pai; occurrence admite INDEX | **GREEN** |
| shadowing por CONDITION/INDEX local incompatível | busca para no primeiro ProgramUnit nominal; não salta ao GLOBAL | **GREEN** |
| `VALUE-X` sem qualifier | DATA local | **GREEN** |
| `VALUE-X OF INNER-GROUP` | DATA local qualificado | **GREEN** |
| `VALUE-X OF OUTER-GROUP` | DATA GLOBAL externo qualificado | **GREEN** |
| GLOBAL FILE SELECT+FD | FILE GLOBAL do pai; 2 declaration IDs | **GREEN** |
| GLOBAL FILE FD-only | FILE GLOBAL do pai; 1 declaration ID | **GREEN** |
| FILE local no pai / homônimo no filho | invisibilidade e shadowing local preservados | **GREEN** |
| qualifier genérico DATA/FILE homônimo | constraints `{DATA, FILE}` e ambiguidade conservadora | **GREEN CONSERVADOR** |
| qualifier `inFile` inequívoco | occurrence FILE; ancestry DATA homônima excluída | **GREEN** |
| COMMON chamado por pai/sibling | permitido | **GREEN** |
| COMMON chamado pelo próprio descendente | invisível | **GREEN** |
| sibling privado e região externa | invisíveis quando aplicável | **GREEN** |
| REDEFINES válido com homônimos | target no owner e mesmo level | **GREEN** |
| REDEFINES estruturalmente inválido | `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT` | **GREEN** |
| RENAMES válido com homônimos | FROM/THROUGH no logical record do owner | **GREEN** |
| RENAMES cruzando records/range inválido | ambos endpoints diagnosticados conservadoramente | **GREEN** |
| PROGRAM-ID literal | grafia preservada, canonical identity sem aspas, CALL interno resolvido | **GREEN** |
| PGMNAME explícito | COMPAT/LONGUPPER/LONGMIXED aplicados conforme a policy | **GREEN** |
| PGMNAME ausente e nome dependente | `UNSUPPORTED_DIALECT_OPTION` | **GREEN CONSERVADOR** |
| lookup por referência | índice por nome/scope/unidade; teste anti-scan verde | **GREEN** |

## 8. LACUNAS RESTANTES

- **Bug conhecido dentro do escopo auditado:** nenhum permanece reproduzível pelos testes adversariais desta tarefa.
- **Limitação do frontend:** na forma geral `qualifiedDataNameFormat1`, `IN` e `OF` são equivalentes e `dataName`/`fileName` compartilham forma lexical; quando a árvore não distingue o namespace do qualifier final, o AST preserva `DATA_OR_FILE` e o resolver retorna ambiguidade em vez de inventar certeza. Formas estruturalmente inequívocas como `inFile` são preservadas e resolvidas como FILE.
- **Dialeto não suportado:** opções de program name além de `COMPAT`, `LONGUPPER` e `LONGMIXED` não foram implementadas. Valor ausente/desconhecido que seja necessário ao binding resulta conservadoramente em `UNSUPPORTED_DIALECT_OPTION`.
- **Informação de compilação ausente:** uma análise invocada sem a compiler option efetiva usa `PgmnameMode.UNSPECIFIED`; somente identidades independentes da opção podem ser resolvidas com certeza.
- **Feature deliberadamente fora de escopo:** CFG, def-use, reaching definitions, constant propagation e resolução de valores não foram introduzidos.
- **Fronteira da evidência:** o GREEN demonstra as regras nominais e topologias cobertas pelas fixtures sintéticas; não constitui afirmação de suporte integral a todo dialeto IBM Enterprise COBOL.

## 9. IMPACTO NO PRÓXIMO PASSO

**A) Reference resolution is sufficiently trustworthy as input for CFG/def-use.**

Essa conclusão é limitada ao binding nominal representável pelo frontend e coberto nesta auditoria. As falhas confirmadas de GLOBAL, shadowing, qualification, FILE, qualifier tipado, COMMON, REDEFINES, RENAMES, literal PROGRAM-ID e PGMNAME foram corrigidas por regras gerais e possuem regressões permanentes. Ownership por `ProgramUnitId`, kinds e `SemanticEntityId`s foram verificados; ambiguidade e opção de dialeto ausente permanecem conservadoras; o teste anti-scan confirma lookup indexado. A suíte final contém 67 testes Maven verdes e os 31 checks JavaScript continuam verdes.

Isso torna `ReferenceResolution` uma fundação suficientemente confiável para iniciar CFG/def-use, mas não autoriza inferir valores nem converter resultados `AMBIGUOUS`, `UNSUPPORTED` ou `UNRESOLVED` em bindings. Essas decisões conservadoras devem ser propagadas pelo próximo estágio.

## Second semantic correctness hardening

### Fase 0 — baseline

- SHA inicial: `58aea33e0280cfe2bf430017762127644d2ebef1`.
- Estado inicial versionado: limpo. A única entrada de `git status --short` era a tasklist desta rodada, fornecida como arquivo ainda não rastreado em `specs/reference-resolution-semantic-correctness-hardening-II-tasklist.txt`.
- Suíte Maven: `Tests run: 67, Failures: 0, Errors: 0, Skipped: 0`; `BUILD SUCCESS`.
- Checks JavaScript: 31 arquivos em `src/main/resources/web`, `dist`, `dist-cbstm03a` e `dist-cbstm03d`; todos passaram em `node --check` (exit code 0).
- `git diff --check`: exit code 0.
- O baseline não alterou código de produção, gramáticas, corpus, fixtures existentes nem outputs versionados. `git diff --quiet HEAD -- corpus src/main/antlr4 src/test/resources dist dist-cbstm03a dist-cbstm03d` terminou com exit code 0 antes da criação das novas fixtures.
- Escopo confirmado: esta rodada termina no hardening nominal, testes, review e decisão de readiness. CFG, def-use, reaching definitions, constant propagation e value resolution não serão implementados.

### Fase 1 — GLOBAL DATA através de ancestor LOCAL

- Regra: uma declaração LOCAL de um programa ancestral não pertence à região de visibilidade do programa indiretamente contido e, portanto, não pode bloquear um homônimo GLOBAL de um ancestral mais externo.
- Fixture: `nested-global-through-local-data.cbl`, com quatro topologias independentes e controles para nome local no programa da referência, GLOBAL no ancestor mais próximo e ausência de GLOBAL elegível.
- Teste: `skipsInvisibleLocalDataInIntermediateProgramsWhenLookingForGlobalData`.
- RED observado: esperado `RESOLVED → OUTER-A.X`; observado `UNRESOLVED / DECLARATION_NOT_FOUND`, zero candidates. **BUG CONFIRMADO**.
- Causa raiz: `DataAndIndexReferenceResolver.compatibleCandidates` interrompia `stopAtFirstNominalLevel` para qualquer homônimo em qualquer ancestor, antes de excluir declarações LOCAL invisíveis.
- Correção geral mínima: o programa da referência continua aplicando shadowing a qualquer homônimo; em ancestors, somente declarações GLOBAL participam do corte nominal e da seleção. A consulta permanece no índice `byName` por unidade.
- GREEN: teste adversarial `1/1`, grupo `DataAndIndexReferenceResolverTest` `13/13` e suíte completa `68/68`, sem falhas, erros ou testes ignorados.

### Fase 2 — GLOBAL FILE através de ancestor LOCAL

- Regra: FILE local de um programa ancestral não é visível ao programa indiretamente contido; somente FILE GLOBAL participa da busca externa, e o GLOBAL elegível mais próximo vence.
- Fixture: `nested-global-through-local-file.cbl`, com FILE GLOBAL externo através de FILE LOCAL intermediário e controles para FILE GLOBAL intermediário, FILE LOCAL no programa da referência e ausência de GLOBAL externo.
- Teste: `skipsInvisibleLocalFilesInIntermediateProgramsWhenLookingForGlobalFiles`.
- RED observado: esperado `RESOLVED → FILE-OUTER-A.FILE-X`; observado `UNRESOLVED / DECLARATION_NOT_FOUND`, zero candidates. **BUG CONFIRMADO**.
- Causa raiz: `CobolReferenceResolver.resolveFile` encontrava o bucket FILE do ancestor, filtrava suas entidades LOCAL e retornava imediatamente uma decisão vazia, sem continuar ao próximo ancestor.
- Correção geral mínima: retornar no primeiro bucket que possua entidades efetivamente visíveis; um bucket ancestral contendo apenas FILEs LOCAL é ignorado e a cadeia estrutural prossegue. Os buckets continuam preindexados por nome e unidade.
- GREEN: teste adversarial `1/1`, grupo `ProcedureFileProgramReferenceResolverTest` `13/13` e suíte completa `69/69`, sem falhas, erros ou testes ignorados. `nested-global-file.cbl` e shadowing FILE local permaneceram verdes.

### Fase 3 — FD GLOBAL propaga visibilidade aos records

- Regra: a cláusula GLOBAL de uma file description torna globais os record-names associados e os data-names, condition-names e index-names subordinados.
- Fixture: `global-fd-record-visibility.cbl`, com `CUSTOMER-FILE GLOBAL`, record, data item, level-88 e `INDEXED BY`; um segundo FD LOCAL controla que a herança não seja aplicada indiscriminadamente.
- Teste: `propagatesGlobalFileDescriptionVisibilityToItsRecordHierarchy`.
- RED observado: a entidade FILE possuía `visibility=GLOBAL`, mas `CUSTOMER-RECORD`, `CUSTOMER-ID`, `CUSTOMER-OK` e `CUSTOMER-IDX` possuíam `visibility=LOCAL`; o teste falhou nas quatro asserções estruturais antes de chegar aos bindings. **BUG CONFIRMADO**.
- Causa raiz: `SymbolTableBuilder.collectDataDivision` chamava `collectDataEntries(file.entries(), fileScope, false)`, descartando a visibilidade do `FileDescription` na raiz da hierarquia.
- Correção geral mínima: a raiz recebe `file.visibility() == GLOBAL` como `inheritedGlobal`; a recursão existente de `effectiveGlobal` continua sendo a única regra de propagação para DATA, CONDITION e INDEX.
- GREEN: teste adversarial `1/1` e suíte completa `70/70`, sem falhas, erros ou testes ignorados. O controle FD LOCAL permaneceu `UNRESOLVED`; os testes anteriores de GLOBAL FILE e hierarquia DATA GLOBAL continuaram verdes.

### Fase 4 — shadowing nominal entre namespaces em contexto FILE

- Regra: a busca identifica primeiro o nome na região nominal visível; se o primeiro nível elegível contém o nome em namespace incompatível com FILE, o resultado deve ser conservador, sem continuar até um FILE GLOBAL externo.
- Fixture: `file-namespace-shadowing.cbl`, com FILEs GLOBAL externos homônimos a DATA, CONDITION e INDEX locais; FILE local e FILE GLOBAL sem colisão são controles.
- Teste: `stopsFileLookupAtIncompatibleLocalNominalDeclarations`.
- RED observado: os três casos incompatíveis foram `RESOLVED` para `FILE-SHADOW-OUTER` em vez de `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`. **BUG CONFIRMADO**.
- Causa raiz: `CobolReferenceResolver.resolveFile` consultava exclusivamente o índice de entidades FILE; declarações DATA/CONDITION/INDEX homônimas no programa da referência eram invisíveis ao algoritmo.
- Correção geral mínima: cada `UnitIndex` passa a possuir um bucket nominal preconstruído por canonical name. `resolveFile` determina o primeiro nível nominal visível (qualquer nome local; somente GLOBAL em ancestors) e só então filtra entidades FILE. Nome incompatível produz `INVALID_NAMESPACE_FOR_CONTEXT`; o lookup continua proporcional aos buckets homônimos.
- GREEN: teste adversarial `1/1`, grupo `ProcedureFileProgramReferenceResolverTest` `14/14` e suíte completa `71/71`, sem falhas, erros ou testes ignorados. FILE GLOBAL, FILE local e `CALL ARGUMENT DATA_OR_FILE` permaneceram verdes.

### Fase 5 — canonicalização externa PGMNAME

- Regra documentada: [IBM Enterprise COBOL PGMNAME](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=options-pgmname) especifica que COMPAT aplica uppercase, truncamento em oito, tradução de hífen para `0` e converte primeiro caractere `1`-`9` em `A`-`I` e demais não alfabéticos, exceto `_`, em `J`; LONGUPPER aplica uppercase e as traduções sem truncar; LONGMIXED preserva o nome sem truncamento, tradução ou case folding.
- Fixture: `external-program-name-canonicalization.cbl`, exercitando `PROG-A`, `LONG-NAME-ABC`, `1PROG`, `$PROG`, `-PROG` e `mixed-Child` pelo `ExternalProgramCatalog`.
- Testes: `ProgramNameCanonicalizerTest.appliesTheDocumentedIbmExternalProgramNameTransformations` e `ProcedureFileProgramReferenceResolverTest.appliesPgmnameCanonicalizationBeforeExternalCatalogLookup`.
- RED unitário: COMPAT produziu `1PROG`, `$PROG` e `0PROG`, em vez de `APROG`, `JPROG` e `JPROG`. RED end-to-end: além desses casos, LONGUPPER produziu `PROG-A` e `-PROG`, em vez de `PROG0A` e `JPROG`. **BUG CONFIRMADO**.
- Causa raiz: `CobolReferenceResolver.externalProgramCanonical` implementava somente uppercase/truncamento/hífen para COMPAT e somente uppercase para LONGUPPER; conversão do primeiro caractere e traduções LONGUPPER estavam ausentes.
- Correção geral mínima: `ProgramNameCanonicalizer.external` centraliza a chave externa determinística e é testada isoladamente; o resolver continua bloqueando nomes dependentes da policy quando o modo é `UNSPECIFIED`.
- Regressão intermediária: a primeira suíte completa após a correção teve `73` testes e uma falha no fake catalog legado, que ainda indexava `EXTERNAL-ONE`/`EXTERNAL-MANY` sob LONGUPPER. O helper foi alinhado à chave IBM correta `EXTERNAL0ONE`/`EXTERNAL0MANY`; a grafia COBOL e as expectativas de binding não foram alteradas.
- GREEN: testes adversariais unitário e end-to-end `2/2`; suíte completa `73/73`, sem falhas, erros ou testes ignorados. Testes anteriores de PGMNAME explícito, ausente e catálogo externo permaneceram verdes.

### Fase 6 — LONGMIXED em nested program binding

- Regra: PGMNAME controla também referências a nested programs. LONGMIXED preserva case; LONGUPPER faz case folding, mas nested names não usam as traduções externas; COMPAT preserva a compatibilidade case-insensitive existente.
- Fixture: `longmixed-nested-program.cbl`, com declaração literal `'mixed-Child'` e chamadas `'mixed-Child'`/`'MIXED-CHILD'`.
- Teste: `honorsLongmixedCaseWhenBindingNestedPrograms`, com controles LONGUPPER e COMPAT e asserção de estabilidade do `ProgramUnitId`.
- RED observado: sob LONGMIXED, ambas as grafias resolveram para o mesmo `SemanticEntityId` do filho; a chamada uppercase deveria permanecer sem target interno. **BUG CONFIRMADO**.
- Causa raiz: `programsByName` era indexado por `ProgramUnitId.canonicalProgramName` e a consulta usava `SymbolTable.canonical`, ambos invariavelmente uppercase.
- Correção geral mínima: `ProgramNameCanonicalizer.nested` produz a chave de lookup dependente da policy para declaração, chamada e `Candidate.canonicalName`. `ProgramUnitId` não foi alterado e continua sendo identidade estrutural estável.
- GREEN: teste adversarial `1/1`, grupo de program names/resolver `17/17` e suíte completa `74/74`, sem falhas, erros ou testes ignorados. PROGRAM-ID literal, COMMON, visibilidade privada, ambiguidade e catálogo externo permaneceram verdes.

### Fase 7 — REDEFINES usa nível estrutural

- Regra: o target de REDEFINES deve pertencer ao mesmo nível estrutural do owner; igualdade textual dos level-numbers não substitui a relação de siblings representada pelos scopes.
- Fixture: `redefines-different-level-number.cbl`, com `ITEM-A` level 05 e `ITEM-B REDEFINES ITEM-A` level 04 sob o mesmo parent; `OTHER-X`/`BAD-X` têm ambos level 05, mas parents distintos.
- Teste: `usesStructuralSiblingScopeInsteadOfTextualLevelNumberForRedefines`, que verifica explicitamente `owner.scopeId == target.scopeId` no caso válido e desigualdade no controle.
- RED observado: o caso estruturalmente válido resultou `UNRESOLVED / INVALID_NAMESPACE_FOR_CONTEXT`, zero candidates, apesar do mesmo `scopeId`. **BUG CONFIRMADO**.
- Causa raiz: `DataAndIndexReferenceResolver.resolveRedefines` já fazia `lookupLocal(owner.scopeId(), ...)`, mas aplicava depois igualdade de `attributes["level"]`, rejeitando siblings válidos com grafias de level diferentes.
- Correção geral mínima: remover a comparação textual e conservar o lookup indexado no scope estrutural do owner; targets de outros grupos continuam fora do bucket local.
- GREEN: teste adversarial `1/1`, grupo `DataAndIndexReferenceResolverTest` `15/15` e suíte completa `75/75`, sem falhas, erros ou testes ignorados. Os testes anteriores de REDEFINES e targets fora do grupo permaneceram verdes.
