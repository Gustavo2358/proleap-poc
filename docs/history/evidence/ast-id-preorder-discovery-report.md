# WORK-AST-003 — Discovery de IDs, ordering e reachability da AST

Identidade conceitual do incidente: `BUG-AST-PREORDER-001`.

Base reproduzível: `c6d9b6e1b597f34db06b41f4e8e04cdcf1d68a3a`, merge do PR #10 em `main`. Branch de Discovery: `discovery/work-ast-003-preorder-invariant`.

## A. Executive Summary

O bug está **confirmado** e deve bloquear WORK-AST-002 Slice 2. A hipótese inicial explica o trigger reportado, mas era incompleta: a auditoria encontrou duas violações confirmadas da mesma classe sistêmica.

1. `buildPerform` aloca o subtree de controle antes de `fromReference`/`throughReference` no ramo procedure, enquanto `Ast.children(PerformStatement)` declara referências antes de controles.
2. O helper compartilhado `declarationVisibility`, alcançado pelos call sites de `FileDescription` e `DataEntry`, chama `meta(context)` para um diagnostic que não é `Ast.Node`; essa metadata consome `nextId` e cria um gap na árvore final.

A severidade é alta para a fronteira AST: fonte aceito pelo parser produz AST imutável alcançável, mas a apresentação falha fechada antes de símbolos/resolução. Além do incidente visível, qualquer chamada nova a `meta()` fora de um nó ou qualquer inversão entre builder e `Ast.children` pode repetir a classe.

O pre-order não é exigência semântica do COBOL nem necessidade matemática de CFG/dataflow. Ele é, porém, um contrato atual intencional da representação: IDs são locais por program unit, começam em zero, são determinísticos e contíguos, e correspondem à posição na traversal canônica. `AstSnapshot` o valida explicitamente; a história de WORK-RES-004 registra uma decisão anterior tomada para preservá-lo. Esse detalhe está subdocumentado: `semantic-ast.md` diz somente “ordem determinística de construção” e INV-DET-001 exige determinismo, sem nomear pre-order/contiguidade.

Recomendação para a Fase 2: preservar a política atual, corrigir as duas fontes comprovadas de alocação divergente e promover um oracle estrutural genérico `O(nodes)` para o gate normal. Não há justificativa proporcional para reconstruir toda a AST após o build ou migrar snapshots/consumidores para IDs não posicionais.

Nenhum código de produção foi alterado neste Discovery.

## B. Reprodução

### Pipeline e fixtures

As quatro fixtures passam por leitura, normalização fixed-format, preprocessing, lexer, parser e `AstBuilder`. Os dois casos originais têm zero erro de preprocessing, zero COPY ausente, zero erro léxico e zero erro sintático. `PerformStatement`, `ProcedureReference` e `controlExpressions` estão presentes e alcançáveis.

| Fixture | Classe | Resultado |
| --- | --- | --- |
| `ast-preorder-perform-until.cbl` | procedure `PERFORM TARGET-PARA UNTIL` com `OR` | AST construída; snapshot falha em `ProcedureReference`, esperado 14, real 21 |
| `ast-preorder-perform-thru.cbl` | procedure `PERFORM FIRST-PARA THRU LAST-PARA UNTIL` | AST construída; snapshot falha em `ProcedureReference`, esperado 14, real 17 |
| `ast-preorder-conflicting-visibility.cbl` | metadata diagnóstica sem nó | AST construída; snapshot falha em `Division`, esperado 8, real 9 |
| `ast-preorder-consistent-controls.cbl` | procedure sem controle + inline com controle | snapshot e oracle estrutural passam |

Ponto exato: `AstSnapshot.from(ast)` chama `flatten`; antes de adicionar cada nó ao output, compara `ast.meta().id()` a `output.size()`. A exceção é `IllegalStateException` com mensagem determinística.

### Sequências observadas

Caso A:

```text
alocação Java:
PerformStatement 13
  control OperationExpression 14
  descendentes do controle 15..20
  from ProcedureReference 21

Ast.children / snapshot:
PerformStatement 13
  from ProcedureReference 21   <- posição esperada 14
  control OperationExpression 14
  descendentes 15..20
```

Caso B:

Forma COBOL caracterizada: procedure `PERFORM FIRST-PARA THRU LAST-PARA` com controle `UNTIL`. Não há `VARYING` nesta fixture.

```text
alocação Java:
PerformStatement 13
  control OperationExpression 14
  descendentes do controle 15..16
  from ProcedureReference 17
  through ProcedureReference 18

Ast.children / snapshot:
PerformStatement 13
  from ProcedureReference 17   <- posição esperada 14
  through ProcedureReference 18
  control OperationExpression 14
  descendentes 15..16
```

O offset não é constante: ele varia com a shape da expressão. Isso rejeita qualquer remendo baseado em `expected/got` específico.

Os números `21, 14` e `17, 18, 14` acima são fotografias reproduzíveis do comportamento defeituoso na base do Discovery. Eles não são valores de aceite da implementação futura e não devem ser simplesmente atualizados após uma correção. A regressão definitiva da Fase 2 deve derivar a sequência pela traversal canônica de `Ast.children` e exigir, para cada nó, `meta.id == posição esperada no pre-order`.

No caso diagnóstico, os IDs alcançáveis são `0..7, 9..12`; o diagnostic `CONFLICTING_DECLARATION_VISIBILITY` possui `Meta.id=8`. O primeiro nó posterior, a `Division` de procedure, recebe 9 e falha na posição 8.

### Quatro ordens distintas

- **Ordem textual COBOL:** tokens conforme escritos; procedure name precede `UNTIL` no caso reportado.
- **Ordem da parse tree:** filhos ANTLR conforme a grammar; é provenance sintática, não identidade AST.
- **Ordem de construção Java:** sequência temporal de `meta()`/`syntheticMeta()` e helpers executados.
- **Ordem estrutural AST:** pre-order de `Ast.Program` usando somente `Ast.children`.

O contrato não exige igualdade geral entre fonte, parse tree e AST: a AST elimina wrappers, cria paragraph sintético, agrupa DATA por level e preserva algumas formas como nós tipados. A relação exigida hoje é somente: para a árvore final de cada program unit, **ordem de alocação dos IDs de nó = pre-order de `Ast.children`**, sem alocações externas intercaladas.

## C. Contrato de `Ast.Meta.id`

### Documentado explicitamente

- `semantic-ast.md`: IDs seguem ordem determinística de construção.
- INV-DET-001: mesma entrada/policy produz IDs e saída na mesma ordem.
- WORK-AST-002/backlog: IDs são locais ao program unit, joins usam `(ProgramUnitId, astNodeId)` e não há persistência resistente a edição.
- ADR-0003/0005: produtos separados se combinam por identidade local namespaced.

### Executável e historicamente intencional

- `AstBuilder.buildProgramUnit` reinicia `nextId=0` por unit.
- `meta()` e `syntheticMeta()` incrementam o mesmo contador.
- `AstSnapshot.flatten` exige `Meta.id == posição pre-order` e, por consequência, unicidade e contiguidade `0..N-1`.
- `CompilationUnitModelTest` exige que cada unit comece em zero.
- WORK-RES-004 registra que uma shape alternativa foi rejeitada porque quebrava pre-order.

### Contrato emergente atual

Para cada `ProgramUnitId`, os `Ast.Node` finais alcançáveis por `Ast.children` devem possuir IDs:

1. locais ao program unit;
2. únicos;
3. determinísticos para a mesma entrada/policy;
4. contíguos no intervalo `0..nodeCount-1`;
5. iguais à posição na traversal pre-order canônica;
6. independentes de metadata que não pertence a um `Ast.Node`.

Não há contrato de igualdade com source order nem parse-tree order. Também não há estabilidade prometida após editar o fonte.

### Necessário versus incidental

- **Necessário arquiteturalmente aos produtos atuais:** unicidade, determinismo, namespace por unit, reachability exata e integridade dos foreign keys.
- **Contrato de representação atual, necessário à compatibilidade:** contiguidade e pre-order, porque snapshot/UI usam a posição serializada como o mesmo ID da AST.
- **Não necessário em abstrato a CFG/dataflow:** pre-order. Essas análises poderiam usar qualquer chave local única/determinística, mas mudar agora exigiria migração explícita sem benefício demonstrado.
- **Suposição incidental que deve ser removida:** metadata diagnóstica poder consumir o contador de nós apenas porque usa o mesmo record `Ast.Meta`.

Finding documental: pre-order/contiguidade são intencionais e executáveis, mas não possuem statement canônico próprio. A Fase 2 deve promover o texto após review.

## D. Causa raiz

### Trigger

- Primário: procedure `PERFORM` com ao menos uma expressão de controle materializada.
- Secundário descoberto: qualquer call site atual de `declarationVisibility` que receba simultaneamente `external=true` e `global=true`.

### Causa imediata

- `buildPerform` calcula `controls` antes de construir referências, invertendo a ordem declarada por `Ast.children`.
- Os call sites de `FileDescription` e `DataEntry` delegam o conflito ao mesmo `declarationVisibility`; o helper chama `meta(context)` para diagnostic sem nó e deixa um gap.

### Causa sistêmica

`nextId` mistura três responsabilidades: ordem temporal do builder, identidade do nó e posição estrutural. A correção depende hoje de disciplina manual distribuída entre `AstBuilder` e `Ast.children`; não existe oracle normal que atravesse a superfície adversarial e compare os dois mecanismos.

## E. Auditoria global `AstBuilder × Ast.children`

Status usa `CONSISTENTE`, `VIOLAÇÃO CONFIRMADA` e `NÃO APLICÁVEL`. Nenhum tipo permaneceu apenas suspeito após reconstrução dos helpers.

| Node type | Builder/helper | Ordem de criação × `Ast.children` | Status / evidência |
| --- | --- | --- | --- |
| `Program` | `visitProgramUnit` | meta; divisions × divisions | CONSISTENTE |
| `Division` | `buildIdentification/Environment/Data/Procedure` | meta; children em ordem publicada × children | CONSISTENTE |
| `Section` | `buildData`, `buildProcedureSection` | meta; entries/paragraphs × children | CONSISTENTE |
| `FileBinding` | `buildEnvironment` | leaf | NÃO APLICÁVEL |
| `FileDescription` | `buildData` | meta; entries × entries | CONSISTENTE entre nós; o call site de `declarationVisibility` pode intercalar metadata não-node quando FD/SD combina `EXTERNAL` e `GLOBAL` |
| `DataEntry` | `buildDataEntry`, `buildDataHierarchy`, `freezeDataDraft` | meta; clauses; entradas subsequentes em source pre-order; freeze reutiliza meta × clauses; nested entries | CONSISTENTE entre nós; objetos draft não alocam ID, mas o call site de visibility pode intercalar metadata não-node no format 1 |
| `PictureClause` | `mapDataClause` | leaf | NÃO APLICÁVEL |
| `UsageClause` | `mapDataClause` | leaf | NÃO APLICÁVEL |
| `ValueClause` | `mapDataClause` | leaf | NÃO APLICÁVEL |
| `RedefinesClause` | `mapDataClause` | meta; target × target | CONSISTENTE |
| `RenamesClause` | `mapDataClause` | meta; from; through × from; through | CONSISTENTE |
| `OccursClause` | `mapDataClause` | meta; min; max; depending; keys; indexes × mesma ordem | CONSISTENTE |
| `PreservedDataClause` | `mapDataClause` | meta; nearest references em parse order × recognizedReferences | CONSISTENTE |
| `ProcedureSignature` | `buildProcedureSignature` | meta; parameters; returning × mesma ordem | CONSISTENTE |
| `ProcedureParameter` | `buildProcedureSignature` | meta; reference × reference | CONSISTENTE |
| `Paragraph` | `buildParagraphGroup`, `buildParagraph` | meta real/sintética; sentences × sentences | CONSISTENTE |
| `Sentence` | `buildSentence` | meta; statements × statements | CONSISTENTE |
| `CallStatement` | `buildCall` | meta; target; arguments; returning; exception flow × mesma ordem | CONSISTENTE |
| `CallArgument` | `buildCall` | meta; value × value | CONSISTENTE |
| `IfStatement` | `buildIf` | meta; condition; then; else × mesma ordem | CONSISTENTE |
| `EvaluateStatement` | `buildEvaluate` | meta; subjects; branches × mesma ordem | CONSISTENTE |
| `EvaluateBranch` | `buildEvaluate` | meta; selector expressions; statements × mesma ordem | CONSISTENTE; `EvaluateSelector` não é Node e sua expression é alcançável |
| `PerformStatement` | `buildPerform` | procedure: meta; controls; references × references; controls | **VIOLAÇÃO CONFIRMADA** quando control não vazio; inline e procedure sem control são consistentes |
| `GoToStatement` | `buildGoTo` | meta; targets; dependingOn × mesma ordem | CONSISTENTE |
| `MoveStatement` | `buildMove` | meta; source; targets × mesma ordem | CONSISTENTE |
| `EmbeddedLanguageStatement` | `buildEmbedded` | leaf | NÃO APLICÁVEL |
| `NextSentenceStatement` | visitor/`statementsInside` | leaf | NÃO APLICÁVEL |
| `StatementOperand` | `collectStatementOperands` | meta; value × value | CONSISTENTE |
| `StatementClause` | `buildStatementClause` | meta; recognizedNodes; nested × mesma ordem | CONSISTENTE; recognized vazio hoje |
| `ModeledStatement` | `buildStructuredStatement` | meta; operands; clauses × mesma ordem | CONSISTENTE |
| `PreservedStatement` | `buildStructuredStatement` | meta; operands; clauses × mesma ordem | CONSISTENTE |
| `UnsupportedStatement` | nenhum builder atual | traversal definida, sem materialização em produção | NÃO APLICÁVEL |
| `LiteralExpression` | `literalExpression` | leaf | NÃO APLICÁVEL |
| `DataQualifier` | `buildQualifiers` | meta; reference × reference | CONSISTENTE; fallback reutiliza a meta somente quando não cria meta filha |
| `SubscriptGroup` | `tableReference` | meta; subscripts × subscripts | CONSISTENTE |
| `ReferenceModification` | `referenceModification` | meta; offset; length × mesma ordem | CONSISTENTE |
| `DataReference` | `dataReference`, `tableReference` | meta; qualifiers; subscript groups; modifier × mesma ordem | CONSISTENTE |
| `OperationExpression` | expression helpers | meta; operands em parse order × operands | CONSISTENTE |
| `FunctionExpression` | `functionExpression` | meta; arguments; modifier × mesma ordem | CONSISTENTE |
| `SpecialRegisterExpression` | `specialRegisterExpression` | meta; operands × operands | CONSISTENTE |
| `ProcedureQualifier` | `procedureReference` | leaf | NÃO APLICÁVEL |
| `ProcedureReference` | `procedureReference` | meta; qualifier × qualifier | CONSISTENTE internamente; pai `PerformStatement` é a inversão |
| `FileReference` | visitors/helpers | leaf | NÃO APLICÁVEL |
| `ProgramReference` | `buildCall` | leaf | NÃO APLICÁVEL |
| `IndexReference` | visitors/`mapDataClause` | leaf | NÃO APLICÁVEL |
| `NamedReference` | `statementOperand`/`nominalReference` | leaf | NÃO APLICÁVEL |
| `PreservedExpression` | `expression`, `preservedExpression` | meta; recognized operands em nearest parse order × operands | CONSISTENTE |
| `RawExpression` | `expression(null)` | synthetic meta; leaf | NÃO APLICÁVEL |

### Fontes indiretas e alocações sem nó

- Streams preservam encounter order nas chamadas relevantes; nenhum `unordered`, set collector ou hash iteration alimenta `Ast.children`.
- `nearestDescendants` percorre filhos em ordem e para de descer ao encontrar um match, evitando duplicação ancestor/descendant da mesma categoria.
- `directRuleChildren` preserva ordem ANTLR.
- `DataDraft/freeze` reconstrói records finais sem chamar `meta`; IDs e filhos não são duplicados.
- `syntheticMeta` só é usado por `RawExpression` e portanto materializa nó.
- A meta “sintética” do paragraph de entry usa `meta(context)` e materializa `Paragraph`.
- **Call sites atuais:** a busca global encontra exatamente duas chamadas a `declarationVisibility`.
  - `buildData` chama o helper ao construir `FileDescription`. Ele deriva os flags de `externalClause` e `globalClause`; como `fileDescriptionEntryClause*` admite ambas as alternativas na mesma FD/SD, esse caminho pode produzir `CONFLICTING_DECLARATION_VISIBILITY`.
  - `buildDataEntry` chama o helper ao construir `DataEntry`. No `dataDescriptionEntryFormat1`, as listas `dataExternalClause()` e `dataGlobalClause()` podem ser simultaneamente não vazias porque a grammar repete a união de cláusulas. Formats 2, 3 e EXEC SQL mantêm ambos os flags falsos e não produzem esse conflito.
- **Violação extra confirmada no nível correto:** em qualquer um desses dois caminhos conflitantes, o helper compartilhado cria `SemanticCoverage.Diagnostic` com `meta(context)`. O diagnostic não é `Ast.Node`, mas a chamada avança `nextId`; portanto, a cadeia causal é `declarationVisibility/call site → metadata diagnóstica não-AST → consumo indevido do contador estrutural → gap nos IDs alcançáveis`.
- A fixture `01 CONFLICTING-ITEM EXTERNAL GLOBAL` é uma reprodução pelo call site `DataEntry`, não o limite do defeito. O call site `FileDescription` é igualmente vulnerável pela análise conjunta do builder e da grammar.
- Essa é a única alocação confirmada sem nó na auditoria de todas as chamadas atuais a `meta`/`syntheticMeta`.

## F. Auditoria de consumidores

Classificações: A unicidade; B estabilidade/determinismo; C contiguidade; D pre-order explícito; E ID sem dependência ordinal; F sem dependência semântica; G ambíguo.

| Consumidor | Uso | Classe | Risco |
| --- | --- | --- | --- |
| `AstSnapshot` | compara ID com `output.size`, serializa id/parent por posição | B/C/D | falha imediata; é o consumidor que torna o bug visível |
| `CoverageSnapshot` | percorre `Ast.children`, publica IDs em findings/gaps | B/E | ordem determinística e joins; não exige valor=posição por si só |
| `CompilationUnitModel` | namespacing da raiz por unit; testes exigem raiz 0 | B/E | mudança de policy afeta contrato local por unit |
| `SymbolTableBuilder` | grava IDs de declaration/owner em scopes, symbols e relations | A/B/E | foreign keys devem continuar apontando ao mesmo nó |
| `SymbolTable` | valida IDs locais não negativos; armazena foreign keys | A/E | não usa ordenação |
| `SymbolTableSnapshot` | serializa ast IDs | B/E | churn aparece nos artefatos |
| `AstScopeIndex` | indexa todo nó alcançável por ID e rejeita duplicata | A/B/E | reachability/uniqueness essenciais; não exige contiguidade |
| `ReferenceOccurrenceCollector` | percorre estrutura, rejeita referência alcançada duas vezes e grava ast ID | A/B/E | compartilhamento/duplicação falha; ordem da traversal define occurrence IDs |
| `ReferenceOccurrences` | rejeita dois usos com o mesmo referenceAstNodeId | A/E | não exige pre-order |
| `CobolReferenceResolver` | mapa ast ID → node e joins de occurrences | A/B/E | IDs devem ser únicos/estáveis na análise |
| `DataAndIndexReferenceResolver` | mapa ast ID → node, scope e spans | A/B/E | ordering vem de spans/collections, não do número do ID |
| `CicsIntrinsicClassifier` | mapas e cobertura de subtree por ast ID | A/B/E | identidade e reachability essenciais |
| `ExternalClassification` | guarda rootAstNodeId/covered occurrence IDs | B/E | serialização e composição, sem requisito ordinal |
| `SemanticCoverage` | um finding concreto por astNodeId | A/B/E | não valida existência nem contiguidade global |
| `ResolutionAnalysisReport` | reconcilia occurrence/classification IDs e publica gaps | B/E | não depende de pre-order; inconsistência de FK continua risco |
| `ResolutionSnapshot` | serializa astNodeId e relations | B/E | churn visível, sem requisito ordinal |
| diagnostics semânticos | usam span/provenance; `Meta.id` não governa decisão | F | alocação pelo contador é efeito incidental e defeituoso |
| UI web AST/resolution | links `ast.html#node=<astNodeId>` e usa snapshot posicional | C/D/E | remover pre-order exige migrar lookup/anchors e artefatos |
| testes AST/compilation/snapshot | flatten, IDs locais e snapshots | A/B/C/D/E | proteção distribuída, mas sem oracle adversarial global normal |
| futuros CFG/dataflow | esperados como consumidores de node identity/reachability | A/B/E | pre-order não é essencial, mas churn/missing node compromete joins |

Conclusão: `AstSnapshot`/UI são os únicos consumidores atuais que dependem explicitamente de pre-order/contiguidade. Quase todos os demais exigem unicidade, determinismo, namespace e reachability. Isso não torna o requisito incidental: ele é parte observável da representação publicada e foi preservado deliberadamente, mas sua motivação é compatibilidade/estrutura, não semântica COBOL.

## G. Reachability

Auditoria estática dos fields de todos os 48 tipos e execução do oracle sobre fixtures focais e `ast-cfg-boundary`, `statements`, `declarations`, `expressions` e `references` produziram:

- todo filho `Ast.Node` armazenado nos records finais possui caminho em `Ast.children`;
- expressions dentro de `EvaluateSelector` são alcançadas por `EvaluateBranch.selectorExpressions()`;
- nenhum filho final é exposto duas vezes;
- nenhuma instância final é compartilhada entre pais nos caminhos exercitados;
- nenhum ciclo ou filho nulo foi encontrado;
- `UnsupportedStatement` não é materializado pelo builder atual;
- `DataEntry` draft é objeto intermediário substituído no freeze, com a mesma meta e sem nova alocação; não é um nó final omitido;
- diagnostics são produto separado e não devem ser alcançados como AST.

Limite do oracle: traversal a partir da raiz pode provar duplicação/ciclo e consistência `IDs ↔ nodeCount`, mas não consegue observar um objeto Java abandonado sem instrumentar o builder. A auditoria estática dos `new Ast.*`, draft/freeze e de todas as chamadas a `meta` cobre esse complemento.

## H. Alternativas arquiteturais

### Alternativa A — alocar durante construção e preservar pre-order

Benefícios: menor incisão; mantém imutabilidade, provenance, snapshots, UI e todos os foreign keys; nenhum passe extra. Custo: builders e `Ast.children` continuam acoplados. Risco: recorrência se o contrato continuar apenas manual. Mitigação necessária: invariant documentado e oracle genérico normal, além de proibir alocação do contador AST para metadata sem nó.

Compatibilidade: máxima. Performance/memória: sem mudança assintótica. CFG/dataflow recebem IDs atuais estáveis e árvore validada.

### Alternativa B — construir e reindexar em traversal posterior

Benefícios: torna a estrutura, não a execução temporal, autoridade final; elimina a classe de inversão em builders.

Custo/risco: `Ast` possui 48 record types imutáveis com `Meta` embutida. Reindexar exige reconstruir toda a árvore ou introduzir mutabilidade/drafts genéricos; coverage findings e diagnostics capturam metadata durante o build e também precisariam ser reconciliados. Há risco alto de perder provenance, compartilhar nó, mudar shape, duplicar alocação e aumentar memória. O passe é `O(nodes)`, mas a complexidade de implementação e migração é desproporcional às duas violações encontradas.

Compatibilidade: potencial churn amplo de IDs/snapshots para caminhos hoje válidos se qualquer ordem temporal latente divergir. Não recomendada agora.

### Alternativa C — manter IDs de construção e remover pre-order dos consumidores

Benefícios: builders não precisam acompanhar `Ast.children`; IDs continuam únicos se nenhuma alocação externa ocorrer.

Custo/risco: não resolve o gap causado por diagnostic sem nó se contiguidade ainda importar. `AstSnapshot` precisa separar snapshot position de astNodeId ou usar mapas; UI/anchors e artefatos precisam migrar. A mesma AST teria traversal order e identity order diferentes, aumentando custo cognitivo para CFG/debug/navigation. Testes e snapshots mudariam sem ganho semântico.

Compatibilidade: baixa em apresentação; demais produtos tolerariam IDs não ordinais, mas não há demanda que justifique a migração. Rejeitada.

### Alternativa D — derivar IDs de parse-tree/source order

Rejeitada. Parse tree não é 1:1 com AST; wrappers são eliminados, synthetic nodes existem, preserved nodes agregam subárvores e DATA é reestruturada. Isso confundiria provenance com identidade e produziria gaps/colisões por desenho.

## I. Solução recomendada para Fase 2

Sem implementar nesta fase:

1. Explicitar no domínio/invariant que, dentro de cada program unit, a traversal pre-order de `Ast.children` possui exatamente IDs `0..N-1`.
2. No ramo procedure de `buildPerform`, construir `fromReference` e `throughReference` antes de `controlExpressions`; manter inline na ordem atual. A ordem textual/parse tree não é a justificativa: a justificativa é a ordem estrutural publicada.
3. Impedir metadata não estrutural de avançar o contador AST em ambos os call sites de `declarationVisibility`. Para `CONFLICTING_DECLARATION_VISIBILITY`, reutilizar a meta do nó declarativo (`FileDescription` ou `DataEntry`) ao qual o diagnostic pertence ou passar explicitamente essa anchor meta ao helper; não inventar ID sentinela silencioso.
4. Promover o oracle opt-in para teste normal. Ele deve validar em um passe: instância alcançada uma vez, ausência de ciclos/nulos, IDs únicos, ID igual à posição, faixa contígua e `nodeCount == maxId+1`.
5. Manter regressões específicas para os dois `PERFORM`, o diagnostic gap e os controles negativos. As sequências quebradas deste Discovery permanecem evidência histórica; o aceite futuro deve ser a propriedade estrutural `id == posição no pre-order de Ast.children`, não novos hardcodes numéricos.

Essa solução corrige a causa imediata nas duas fontes conhecidas e protege a causa sistêmica por um oracle independente de cada builder. Ela não trata apenas `expected 14` nem altera semântica COBOL.

## J. Escopo proposto da implementação

Provavelmente mudar:

- `AstBuilder.java` — ordem do ramo procedure e metadata do diagnostic;
- `AstPreorderInvariantCharacterizationTest.java` — promover required oracle e expectativas verdes;
- fixtures focais deste Discovery;
- `semantic-ast.md`, `architecture/invariants.md` e catálogo de evals para tornar o contrato canônico/executável;
- `state.md` de WORK-AST-003 e, no encerramento, resumo histórico conforme protocolo.

Provavelmente não mudar:

- `Ast.java`, inclusive `Ast.children` e record shapes;
- `Ast.Meta` e `AstSnapshot`;
- grammar/parser/preprocessor;
- symbol tables, scopes, occurrences, resolvers, coverage taxonomy, report composition e UI;
- WORK-AST-002 e seus oráculos Slice 2;
- snapshots/baselines de corpus, salvo diff explicado que revele que uma entrada existente antes não chegava ao snapshot.

Produtos que devem permanecer invariantes para entradas anteriormente válidas: parse tree, AST shape, spans/provenance, coverage, símbolos, occurrences, candidates/status de resolução, external classification e `ResolutionAnalysisReport`.

## K. Riscos

- **Churn de IDs:** reordenar filhos do `PERFORM` muda IDs somente para inputs atualmente rejeitados pelo snapshot; confirmar em teste. Reutilizar meta diagnóstica remove gap em input hoje rejeitado.
- **Snapshots/order:** entradas válidas existentes devem permanecer byte/semanticamente estáveis; não atualizar baseline para esconder diferença.
- **Symbol table/scopes/occurrences/resolution:** recebem IDs diferentes nos triggers hoje falhos quando o pipeline puder avançar; joins precisam ser reconciliados, não comparados a baselines inexistentes.
- **SemanticCoverage:** findings capturam a meta dos nodes e precisam continuar 1:1 após reorder; diagnostic deve apontar para a meta do declaration node correto.
- **Provenance:** reutilizar anchor meta precisa preservar span/regra/fonte coerentes com o diagnóstico; não criar identity map nem meta sintética imprecisa.
- **ResolutionAnalysisReport:** passa a receber os triggers depois do snapshot; gaps semânticos legítimos continuam observáveis.
- **Compatibilidade com PR #10:** nenhuma mudança pode alterar coverage taxonomy/cardinalidade do Slice 1.
- **CFG/dataflow futuros:** a proteção reduz risco de foreign keys órfãs e traversal inconsistente; não autoriza implementar essas fases.
- **Oracle incompleto:** somente fixtures não provam todos os branches. Por isso o review estático de todas as alocações e o oracle estrutural devem coexistir.

## L. Critérios de aceite da Fase 2

- os três triggers hoje vermelhos passam por `AstSnapshot`;
- os dois casos mínimos mantêm `PerformStatement`, referências e controles estruturados;
- o diagnostic conflitante permanece observável com span/provenance corretos sem consumir ID de nó;
- procedure sem controle e inline com controle permanecem verdes;
- oracle estrutural normal passa na superfície representativa e nos casos adversariais;
- todos os IDs por unit são únicos, determinísticos, contíguos e iguais ao pre-order canônico;
- nenhuma duplicação de instância, ciclo, filho nulo ou gap de reachability;
- AST shape, provenance, coverage, símbolos, occurrences, resolução e relatórios de entradas previamente válidas permanecem invariantes;
- nenhum snapshot/baseline é relaxado ou atualizado sem diff causal aprovado;
- gates `fast`, `semantic` e `full` verdes; `performance` não é obrigatório se a solução permanecer sem passe novo no caminho de produção;
- required oracles anteriores de WORK-AST-002 preservam seu estado esperado;
- nenhuma mudança semântica COBOL, CFG, dataflow ou Slice 2 misturada.

## Evidência executável do Discovery

Teste normal:

```bash
mvn -q -Dtest=AstPreorderInvariantCharacterizationTest test
```

Oracle required deliberadamente vermelho nesta fase:

```bash
mvn -q -Dast.preorder.required=true \
  -Dtest=AstPreorderInvariantCharacterizationTest#everyAstNodeIdMatchesCanonicalPreOrder test
```

Falha esperada atual:

```text
canonical pre-order mismatch at ProcedureReference: expected 14 but got 21
```

O oracle não faz parte dos gates normais até a Fase 2 aprovada.

## Gates executados

- Baseline anterior aos novos artefatos, em `c6d9b6e`: `check-full.sh` passou.
- Focal normal: 6 testes, 5 verdes e 1 required oracle skipped por opt-in.
- Required oracle opt-in: vermelho esperado em `ProcedureReference`, `expected 14 but got 21`.
- `check-fast.sh`: passou.
- `check-semantic.sh`: passou.
- `check-full.sh`: passou, incluindo regressão E2E estruturada e naming.
- `git diff --check`: passou.
- `check-performance.sh`: não aplicável; Discovery não altera caminho de produção nem índices.
