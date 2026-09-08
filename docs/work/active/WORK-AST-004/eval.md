# Evidências e estratégia de testes

## O que prova corretude

Este checkpoint prova o **defeito e seu desenho**, não a correção. Base limpa e
sincronizada em 2026-09-08: main = origin/main = HEAD
`2815e805fd3a9ef4762a39ab9435260fc76da0e8`, após fetch e fast-forward.
Branch criada antes do discovery: `codex/discovery-next-sentence-coverage`.
Nenhum arquivo de produção mudou.

Oracle executável: [NextSentenceCoverageDiscoveryTest](../../../../src/test/java/io/github/gustavo2358/cobolexplorer/NextSentenceCoverageDiscoveryTest.java).
O helper existente percorre normalização FIXED, preprocessing, parse sem erros,
AST, compilation units, símbolos, occurrences, resolução, validator e report.
O teste então chama o projector real e o writer JSON. Não fabrica finding para
tornar o caso positivo; o único produto adulterado pertence ao teste negativo.

### Root cause comprovada: F-01

| Elo da hipótese | Veredito e evidência na base |
| --- | --- |
| Gramática aceita NEXT SENTENCE sem statement | Confirmado: [Cobol.g4](../../../../src/main/antlr4/Cobol.g4), 1533–1539 (`ifThen`, `ifElse`) e 1909–1911 (`searchWhen`) contêm alternativa direta; 1163–1165/1715–1717 também oferecem o caminho normal. O teste conta contextos efetivamente selecionados. |
| AstBuilder cria NextSentence por caminho especial | Confirmado: [AstBuilder](../../../../src/main/java/io/github/gustavo2358/cobolexplorer/AstBuilder.java), 1904–1910; `statementsInside` detecta tokens diretos sem StatementContext e cria `NextSentenceStatement(meta(context))`. `buildIf` 915–918 e `buildSearchWhen` 845–850 chamam esse helper. |
| Esse caminho contorna coverage | Confirmado: registro normal em `buildStatement` 624–650, `recordCoverage` 785–788 e materialização por manifesto em `buildCoverageReport` 769–782. O retorno especial não passa por nenhum registro. SearchWhen tem finding próprio; seu astNodeId não é o do NextSentence filho. |
| Projector exige o finding e falha | Confirmado: [projector](../../../../src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/projection/CobolSemanticProductProjector.java), `plan` 284–288 → `ProjectionInputs.finding` 1403–1406. Exceção exata: `IllegalArgumentException: typed AST node has no canonical coverage finding`. |
| CONTINUE passa pelo caminho normal | Confirmado em 12 controles; `visitContinueStatement` 663 → `modeled`/`buildStructuredStatement`, com registro pelo wrapper. Isso prova contraste de pipeline, não equivalência COBOL. |

A hipótese inicial está correta para o crash, mas incompleta em três aspectos:
`ifElse` também falha; a policy do finding depende da origem real, que difere
entre branches; e restaurar coverage não torna o SP suficiente para calcular o
destino de NEXT SENTENCE. A invariante já era contratada, não precisa ser inventada.

```yaml
downstream_impact:
  class: BLOCKS_SEMANTIC_PRODUCT
  rationale: >
    AST e identidade NEXT SENTENCE existem, mas falta o finding exigido para
    publicar o inventário positivo no Semantic Product. O projector e o CLI
    falham nessa primeira fronteira downstream conhecida. IR, CFG, DATAFLOW
    e DEPENDENCY_FACTS são consequências posteriores, não a primeira falha;
    REDUCES_PRECISION não descreve a impossibilidade de publicar o produto.
  evidence:
    - NextSentenceCoverageDiscoveryTest reproduz nove variantes diretas com a exceção canônica e doze controles CONTINUE sem crash.
    - ExplorerMain com next-sentence-if.cbl falha na fase SEMANTIC_PRODUCT; o controle publica JSON 1.2.0.
    - INV-COV-001, INV-SP-001, INV-SP-002 e INV-SP-004 exigem coverage e inventário explícitos sem reparo no projector.
```

### Inventário da família e limites da investigação

Busca em todas as gramáticas vendorizadas encontrou **quatro** produções com
`NEXT SENTENCE`: nextSentenceStatement e as três alternativas diretas acima.
Busca de construtores em toda produção encontrou **dois** sites de criação de
NextSentence, ambos em AstBuilder: visitor 688 (normal) e helper 1909 (defeituoso).

Todos os demais sites de construção de Statement são internos ao AstBuilder:
GOBACK, CALL, Modeled/Preserved, SEARCH, IF, EVALUATE, PERFORM (duas formas),
GO TO, MOVE e EmbeddedLanguage. No build da ProgramUnit eles retornam pelo
wrapper `buildStatement`; listas aninhadas usam esse wrapper por
`statementsInside`, `directNestedStatements`, Evaluate e flow clauses. Nenhum
outro bypass alcançável foi identificado. `UnsupportedStatement` é permitido
pelo modelo, mas não possui construtor em produção nesta base. Visitors públicos
podem produzir nodes isolados sem report; esse uso não é o contrato build/buildCompilationUnit.

O teste reconcilia **todos** os Statements das 12 árvores: os únicos nodes sem
finding são NextSentence diretos. Isso complementa a inspeção de chamadas; não
certifica todo o universo COBOL. Casos anteriores de SEARCH testavam shape e
ownership, sem projetar o produto ou exigir finding para o filho.

## Classes positivas

Na caracterização, positivo significa reproduzir a observação conhecida:

| Cenário | NEXT nodes / findings ausentes | AST → SP na base |
| --- | --- | --- |
| IF THEN, sem THEN escrito | 1 / 1 | Exceção canônica |
| IF THEN, com THEN escrito | 1 / 1 | Exceção canônica |
| IF ELSE | 1 / 1 | Exceção canônica |
| IF ambos os ramos | 2 / 2 | Exceção canônica |
| IF terminado por período, sem END-IF | 1 / 1 | Exceção canônica |
| IF aninhado | 1 / 1 | Exceção canônica |
| SEARCH WHEN sequencial | 1 / 1 | Exceção canônica |
| SEARCH ALL WHEN (probe estrutural) | 1 / 1 | Exceção canônica |
| SEARCH com segundo WHEN independente | 1 / 1 | Exceção canônica |
| statement genérico NEXT SENTENCE | 1 / 0 | NEXT_SENTENCE / TYPED_NEXT_SENTENCE, blocked |
| IF com CONTINUE antes de NEXT na lista | 1 / 0 | NEXT_SENTENCE / TYPED_NEXT_SENTENCE, blocked |
| EVALUATE WHEN com lista de statements | 1 / 0 | NEXT_SENTENCE / TYPED_NEXT_SENTENCE, blocked |
| Cada cenário substituindo NEXT SENTENCE por CONTINUE | 0 / 0 | 12 controles publicam inventário completo |

Fixtures: [IF](../../../../src/test/resources/cobol/semantic/next-sentence-if.cbl)
e [SEARCH](../../../../src/test/resources/cobol/semantic/next-sentence-search.cbl).
O IF contém um CONTINUE depois de END-IF e antes do período; GOBACK está na
sentence seguinte. O oracle AST distingue o statement imediato do posterior
ao período, sem executar CFG. Os probes genéricos e SEARCH ALL são evidência
de caminhos da gramática, sem alegação de validade normativa irrestrita.

## Classes negativas

O teste `removingCoverageFromNormalPathStillFailsClosed` remove o único finding
do NEXT SENTENCE normal, preserva a AST e exige a mesma exceção. `SemanticCoverage.Report`
já rejeita duplicações e IDs de finding não contíguos; não consegue detectar
ausência sem receber a AST. `CompilationUnitBuildResult` verifica as keys de
units, mas não a bijeção. `SemanticProductIntegrityValidator` não recebe coverage
e valida outros joins. A pipeline de teste passa por ele antes do crash.

`AstSemanticBoundaryRequiredOracleTest.everyMaterializedSemanticBoundaryHasExactlyOneFinding`
já codifica INV-COV-001 para uma fixture. Falta incluir as alternativas diretas
nessa cobertura de entradas; o defeito é de enforcement, não ausência de contrato.

## Classes ambíguas

Parser acceptance não valida todas as regras SEARCH ALL. Report incompleto não
autoriza descartar o NextSentence nem tratar o fluxo como no-op. Span dos nodes
diretos é o wrapper inteiro, informação real porém mais ampla que os dois tokens;
este fix não promete estreitá-lo. Policy conservadora por wrapper permanece válida
para integridade, como decidido no [plano](plan.md).

## Casos adversariais

IF ELSE impede conserto somente em thenBranch; ambos os ramos impedem deduplicar
por tipo ou grammarRule; SEARCH com dois WHENs impede usar o finding do container
como finding da ação. Caminho normal impede registro em duplicidade. CONTINUE
antes do NEXT demonstra que a escolha da alternativa ANTLR muda o resultado.
Teste negativo impede fallback no projector. Oracle de sentence impede considerar
o sucessor lexical imediato como destino já provado de NEXT SENTENCE.

### Testes mínimos do fix versus hardening separado

**Necessários para este fix:** promover a matriz (AST/parse route, exatamente um
finding por Statement, mesma Meta, SearchWhen distinto, integração e JSON),
manter negativo de finding removido e duplicação existente, e preservar o oracle
de sentence e os controles CONTINUE. Acrescentar asserts da policy real dos quatro
contexts, determinismo de build/JSON e identity/containment por unit. Reutilizar
os gates existentes para pre-order/provenance/compilation units e regressões
GOBACK/MOVE. Esses oracles não exigem implementar semântica de transferência.

**Hardening separado:** validar AST↔coverage ao construir AstBuildResult e
CompilationUnitBuildResult, em `O(nodes + findings)` por unit; detectar ausentes,
órfãos, duplicates e Meta divergente com mensagem contextual. É útil para
consumers anteriores ao projector, mas altera APIs/ponto de falha e pode expor
outros bypasses. Também separar a policy semântica da origem gramatical e gerar
matrizes de todas as famílias de statements. Nenhum desses itens é autorização
para implementação agora ou requisito oculto do reparo focal.

## Casos de regressão

Comandos reproduzíveis a partir da raiz:

```bash
./scripts/harness/check-fast.sh
mvn -q -Dtest=NextSentenceCoverageDiscoveryTest test
mvn -q -Dtest=NextSentenceCoverageDiscoveryTest -Dnext.sentence.required=true test
mvn -q exec:java -Dexec.args='--source src/test/resources/cobol/semantic/next-sentence-if.cbl --copybooks src/test/resources/cobol/provenance/cpy --output target/next-sentence-discovery/if-next'
```

O segundo comando passa: 15 testes, zero failures/errors, um skip do oracle
futuro. O terceiro é **RED esperado**, 15 testes, uma failure agregando nove
cenários (`expected: <1> but was: <0>`), zero errors/skips; não é erro de build.
A tentativa inicial tinha accessor de teste incorreto (`shape` em vez de
`observedShape`); foi corrigido antes dessas evidências, sem alterar produção.

O CLI falha com exit 1, fase SEMANTIC_PRODUCT, passando por `plan` → `finding`;
não publica `cobol-semantic-product.json`. Artefatos anteriores à fase podem
existir e não significam publicação concluída. Controle reproduzível:

```bash
python3 - <<'PY'
from pathlib import Path
p = Path('target/next-sentence-discovery/if-continue.cbl')
p.parent.mkdir(parents=True, exist_ok=True)
p.write_text(Path('src/test/resources/cobol/semantic/next-sentence-if.cbl').read_text().replace('NEXT SENTENCE', 'CONTINUE'))
PY
mvn -q exec:java -Dexec.args='--source target/next-sentence-discovery/if-continue.cbl --copybooks src/test/resources/cobol/provenance/cpy --output target/next-sentence-discovery/if-continue'
```

O controle termina com exit 0 e publica JSON 1.2.0. O opt-in segue a convenção
de `SemanticConditionContextDiscoveryTest` (`semantic.condition.required`),
preserva um RED explícito sem deixar os gates normais vermelhos e será removido
na implementação aprovada. Não é exceção ao contrato de produção.

Primeira execução de full: fast/semantic e E2E passaram; naming rejeitou o
identificador legado presente no URL GitHub do resumo arquivado do item 005.
O resumo passou a usar PR #32 e SHA explícitos, como os históricos existentes;
nenhuma regra do gate foi alterada. Gates finais e self-validation ficam no
[estado](state.md).

## Propriedades/relações metamórficas

Adicionar THEN escrito, mudar ramo, aninhar IF ou adicionar um segundo WHEN não
pode perder identidade nem coverage de uma ocorrência existente. A combinação
de dois NEXT exige dois findings, nunca um por tipo. Alternar NEXT/CONTINUE
é **contraste**, não metamorfismo semântico. Builds equivalentes devem conservar
AST/coverage/JSON determinísticos; IDs são locais à unit, sem promessa após edição.

### Suficiência downstream e riscos

| Camada | Informação preservada / limite |
| --- | --- |
| AST | NextSentenceStatement distinto, membership e Sentence(PERIOD, terminatorSpan). A fixture diferencia next lexical e after-period. Não contém CFG. |
| Semantic Product | O caminho normal já expõe NEXT_SENTENCE / TYPED_NEXT_SENTENCE, id, provenance, gap e readiness bloqueada. O fix habilitará o mesmo inventário conservador para os caminhos diretos. |
| Lowering / AIR 2.0.0 | Identidade impede confusão com CONTINUE. Ainda faltam sentence boundary/target e, em SEARCH, ownership público preciso de WHEN. `collectDirectStatements` achata containers de procedure; `Branch.UNKNOWN` não identifica o WHEN. A interface atual não autoriza resolver destino por source/IDs/ordem. |
| CFG | Não pode criar fallthrough para NEXT_SENTENCE nem usar IfFact.continuation como destino. Precisa de evolução canônica no frontend/SP antes de lowering preciso; controle conservador permanece bloqueado. |
| Futuro dataflow | Successor falso propagaria valores e reachability incorretos. Gaps e readiness bloqueada impedem alegar transferências/valores conhecidos neste checkpoint. |

Portanto, a hipótese “só adicionar coverage já entrega toda a informação para
CFG preciso” é **refutada**. A direção correta agora é integridade e identidade
sem promoção de capability. A evolução futura do contrato de controle pertence
ao frontend/SP; sua tradução pertence a cobol-lower, AIR a air-java e CFG a
analysis-cfg. Nenhum desses trabalhos foi iniciado. A proposta não escolhe
sentence handles versus target explícito; essa decisão permanece aberta.

## Expectativas de escala

Doze fontes pequenas e contagens derivadas de seus inventários, sem baseline
de corpus ou threshold de hardware. A proposta adiciona um registro por
ocorrência direta e preserva ordenação/complexidade existentes. Guard geral
linear e probes extensivos só se autorizados no hardening separado.
