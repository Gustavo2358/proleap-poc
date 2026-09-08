# Evidências e estratégia de testes

## O que prova corretude

O fix focal foi autorizado explicitamente em 2026-09-08 após aprovação humana
do discovery. A implementação parte do head remoto/PR #33
`64454e2b6db8040e8eee2fd2178fe92dfb8d8d00`, em worktree irmã limpa e isolada,
na mesma branch `codex/discovery-next-sentence-coverage`. Somente AstBuilder
muda em produção: `buildStatement` e a alternativa direta em `statementsInside`
terminam em `finishStatement`. O helper registra uma vez, pelo `recordCoverage`
existente, preserva logging e retorna o mesmo statement. `builtStatements`
continua restrito a StatementContexts reais; `buildCoverageReport` e manifesto
permanecem inalterados.

### Implementação e oracles promovidos

`NextSentenceCoverageDiscoveryTest` mantém o nome histórico, mas agora é
regressão obrigatória (EVAL-AST-006): removeu-se `EnabledIfSystemProperty` e
substituíram-se os asserts de crash/ausência por bijeção AST↔coverage↔inventário.
O oracle requerido original está GREEN sem opt-in. A suíte focal tem **17 testes,
zero failures/errors/skips**: matriz original de 12 cenários com 12 controles
CONTINUE, oracle agregado, boundary de período, corrupção negativa, segundo WHEN
com NEXT e duas units com namespaces independentes. Nenhum teste aceita o crash
antigo como comportamento normal.

A regressão verifica toda a matriz, não apenas os NextSentence: exatamente um
finding por Statement, mesma Meta/origem/writtenText, finding de SearchWhen
separado do filho, pre-order contíguo sem compartilhamento de instância, joins
canônicos, identidade e containment publicados, policy dos quatro contexts,
AST/coverage/JSON determinísticos e identidade JSON
`NEXT_SENTENCE` / `TYPED_NEXT_SENTENCE`. Lowering, CFG e effects-dataflow
permanecem BLOCKED tanto no port quanto no JSON. IF direto mantém PARTIAL;
normal e SEARCH mantêm UNSUPPORTED. CONTINUE permanece apenas controle de pipeline.

A captura da AST completa dos 12 cenários antes/depois foi byte-identical:
SHA-256 `657d5bca5fa15e8d3a5effb3d11e21d6a787d5894e3113dac1dab0613fdfa766`.
Isso inclui IDs/Meta, provenance, ownership, sentences/períodos e metadata de
entry/continuation. O código que materializa esses elementos não mudou.

Dois desafios semânticos focais foram executados isoladamente em AstBuilder:
remover a chamada comum da alternativa direta e duplicar coverage no caminho
normal. Ambos ficaram RED pelos contratos esperados, sem erro de compilação;
restauração byte-identical e segundo GREEN confirmados. O negativo permanente
de finding removido continua exigindo o fail-fast original do projector;
`SemanticCoverageTest` continua rejeitando duplicações.

Logs brutos, XMLs, snapshots e hashes locais estão em `target/work-ast-004/`:
`discovery-required-red.log`, `focal-green.log`, `focal-green.xml`,
`ast-before.txt`, `ast-after.txt`, `challenge-results.json` e logs dos mutantes.
As duas primeiras tentativas da ampliação do teste estão preservadas em
`focal-first.log` (helper sem declarar IOException) e `focal-second.log`
(comparação de instâncias de CompilationUnitModel em vez dos records de units).
Esses erros de teste foram corrigidos; não contam como mutantes mortos nem como
evidência do bug de produção. Não exigiram mudança no desenho aprovado.

### Evidência histórica preservada

O discovery abaixo foi executado sobre a base limpa
`2815e805fd3a9ef4762a39ab9435260fc76da0e8`, sem alteração de produção.
As referências de linhas da causa descrevem essa base. Seus resultados RED
foram reproduzidos antes do fix nesta worktree; não são mais comportamento
esperado da suíte normal.

Oracle executável: [NextSentenceCoverageDiscoveryTest](../../../../src/test/java/io/github/gustavo2358/cobolexplorer/NextSentenceCoverageDiscoveryTest.java).
O helper existente percorre normalização FIXED, preprocessing, parse sem erros,
AST, compilation units, símbolos, occurrences, resolução, validator e report.
O teste então chama o projector real e o writer JSON. Não fabrica finding para
tornar o caso positivo; o único produto adulterado pertence ao teste negativo.

### Root cause comprovada: F-01

F-01 foi remediado pelo fix focal. A classificação abaixo descreve o defeito
comprovado antes da correção; a capability de CFG continua indisponível.

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

Na base do discovery, o teste reconciliou **todos** os Statements das 12 árvores:
os únicos nodes sem finding eram NextSentence diretos. Isso complementa a inspeção de chamadas; não
certifica todo o universo COBOL. Casos anteriores de SEARCH testavam shape e
ownership, sem projetar o produto ou exigir finding para o filho.

## Classes positivas

A matriz conserva os resultados do discovery para comparação. Após o fix,
todas as doze entradas devem publicar NEXT_SENTENCE / TYPED_NEXT_SENTENCE,
com zero findings ausentes e exatamente um por Statement. As policies reais
são verificadas explicitamente: IF direto publica PARTIAL; normal/SEARCH
publicam UNSUPPORTED. Lowering/CFG/effects-dataflow continuam BLOCKED.

| Cenário | NEXT nodes / findings ausentes na base | AST → SP na base | Após o fix |
| --- | --- | --- | --- |
| IF THEN, sem THEN escrito | 1 / 1 | Exceção canônica | 0 ausentes; NEXT tipado, BLOCKED |
| IF THEN, com THEN escrito | 1 / 1 | Exceção canônica | 0 ausentes; NEXT tipado, BLOCKED |
| IF ELSE | 1 / 1 | Exceção canônica | 0 ausentes; NEXT tipado, BLOCKED |
| IF ambos os ramos | 2 / 2 | Exceção canônica | 0 ausentes; NEXT tipado, BLOCKED |
| IF terminado por período, sem END-IF | 1 / 1 | Exceção canônica | 0 ausentes; NEXT tipado, BLOCKED |
| IF aninhado | 1 / 1 | Exceção canônica | 0 ausentes; NEXT tipado, BLOCKED |
| SEARCH WHEN sequencial | 1 / 1 | Exceção canônica | 0 ausentes; NEXT tipado, BLOCKED |
| SEARCH ALL WHEN (probe estrutural) | 1 / 1 | Exceção canônica | 0 ausentes; NEXT tipado, BLOCKED |
| SEARCH com segundo WHEN independente | 1 / 1 | Exceção canônica | 0 ausentes; NEXT tipado, BLOCKED |
| statement genérico NEXT SENTENCE | 1 / 0 | NEXT_SENTENCE / TYPED_NEXT_SENTENCE, blocked | 0 ausentes; NEXT tipado, BLOCKED |
| IF com CONTINUE antes de NEXT na lista | 1 / 0 | NEXT_SENTENCE / TYPED_NEXT_SENTENCE, blocked | 0 ausentes; NEXT tipado, BLOCKED |
| EVALUATE WHEN com lista de statements | 1 / 0 | NEXT_SENTENCE / TYPED_NEXT_SENTENCE, blocked | 0 ausentes; NEXT tipado, BLOCKED |
| Cada cenário substituindo NEXT SENTENCE por CONTINUE | 0 / 0 | 12 controles publicam inventário completo | 0 NEXT; inventário completo |

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
e valida outros joins. A pipeline de teste já passava por ele antes do crash histórico.

`AstSemanticBoundaryRequiredOracleTest.everyMaterializedSemanticBoundaryHasExactlyOneFinding`
já codifica INV-COV-001 para uma fixture. A matriz de EVAL-AST-006 agora estende
essa proteção às alternativas diretas; o defeito era de enforcement, não ausência
de contrato. A corrupção negativa continua isolada em teste, sem fallback novo.

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

**Implementados neste fix:** promoção da matriz (AST/parse route, exatamente um
finding por Statement, mesma Meta, SearchWhen distinto, integração e JSON),
negativo de finding removido e duplicação existente, preservação do oracle
de sentence e controles CONTINUE. Asserts da policy real dos quatro
contexts, determinismo de build/JSON e identity/containment por unit. Reutilizam-se
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

### Validação da implementação — 2026-09-08

| Execução | Resultado real |
| --- | --- |
| Oracle requerido antes do fix | RED: 15 testes, uma failure agregando nove contracasos, zero errors/skips |
| Regressão focal promovida, sem opt-in | GREEN: 17 testes, zero failures/errors/skips |
| `check-fast.sh` | PASS (docs/lifecycle local e arquitetura) |
| `check-semantic.sh` | PASS: 568 testes, zero failures/errors, um skip preexistente |
| `check-full.sh` | PASS: fast + semantic + regressão E2E + naming |
| `check-performance.sh` | PASS: probes estruturais e de escala 4A |
| Dois desafios focais de coverage | Ambos semanticamente RED; restauração byte-identical e GREEN |
| `challenge-scalar-move.py` do workflow | 13 mutantes semanticamente RED; restauração exata e segundo GREEN |
| CLI IF e SEARCH | exit 0, JSON 1.2.0 com NEXT_SENTENCE / TYPED_NEXT_SENTENCE e três dimensões BLOCKED |

O único skip da suíte é `SemanticConditionContextDiscoveryTest`, dependente de
`semantic.condition.required`, já existente. Nenhum gate ou fixture foi
relaxado. O full preservou inclusive sua repetição interna da suíte Maven.
Performance/challenges adicionais reproduzem os entrypoints do workflow atual;
PIT permanece sob demanda, fora dos gates estáveis, conforme a política.

Comandos nesta worktree (Maven 3.9.16, JDK 25.0.4, Node 24.19.0):

```bash
mvn -o -q -Dtest=NextSentenceCoverageDiscoveryTest test
./scripts/harness/check-fast.sh
./scripts/harness/check-semantic.sh
./scripts/harness/check-full.sh
./scripts/harness/check-performance.sh
python3 scripts/harness/challenge-scalar-move.py
mvn -o -q exec:java -Dexec.args='--source src/test/resources/cobol/semantic/next-sentence-if.cbl --copybooks src/test/resources/cobol/provenance/cpy --output target/work-ast-004/cli-if'
mvn -o -q exec:java -Dexec.args='--source src/test/resources/cobol/semantic/next-sentence-search.cbl --copybooks src/test/resources/cobol/provenance/cpy --output target/work-ast-004/cli-search'
```

Foi usado o parâmetro já suportado `MAVEN_BIN` com um adapter local ignorado que
somente acrescenta `-o`: cache de dependências disponível usado offline, sem
limpeza compartilhada ou alteração do harness. Gates sequenciais, nunca dois
Maven simultâneos nesta worktree. `gate-results.json` e XMLs em
`semantic-surefire/`/`full-surefire/` preservam resultados; os challenges 4A
mantêm seus logs/hashes originais em `target/checkpoint-4a/challenges/`.
O E2E preservou outputs brutos em `/tmp/cobol-source-normalizer-full.nHoSBf`.

Os JSONs públicos do CLI são byte-identical ao alias `semantic-product.json`.
SHA-256 IF: `cdcdea5d9c430ec67db92f12a41f9f17ade6dc0477bb7aa3728df5a30101ddcb`;
SEARCH: `519ab497c9359f6bbea86c31b2727e6dedc91c550bfa092b1888ea051c819a0f`.
`cli-results.json` registra coverage PARTIAL/UNSUPPORTED respectivamente,
identidade e as três dimensões bloqueadas.

### Isolamento operacional e challenge pass

Antes de implementar: instruções do workspace/repositório lidas; worktrees
inventariadas; fetch remoto; branch local criada rastreando diretamente o head
remoto do PR #33 na worktree irmã isolada. Checkout principal ocupado:
`4345bc18917b8df56be1acd7e44118469ec3f3d3`, branch
`feat/semantic-product-scalar-move`; worktree 4E ocupada: detached
`2815e805fd3a9ef4762a39ab9435260fc76da0e8`. Ambos permaneceram somente leitura:
HEAD, branch, status limpo e os 468 hashes de arquivos rastreados de cada um
foram comparados antes/depois, sem diferenças. Inventário com paths absolutos
e hashes preservado em `occupied-before.json`/`occupied-after.json` locais.
A nova worktree usa seu próprio source/index/HEAD/target/fixtures; temporários
do harness e JUnit usam nomes únicos. Não há porta, container ou banco fixo
nos gates executados; nenhum processo global foi parado/reiniciado, nenhum
arquivo do 4E ou roadmap foi alterado. Metadados Git de registro de worktrees e
refs remotas são compartilhados por definição, sem modificar os working trees.

A revisão por falsificação cobriu bypass direto, duplicação normal, perda de
ramo/WHEN, namespaces locais repetidos, troca indevida por CONTINUE e promoção
de capability. Somente AstBuilder permanece no diff de produção; projector,
gramática, manifestos, contrato público e repositórios downstream permanecem
intocados. Nenhum novo finding exige rever o desenho aprovado. Hardening geral
e sentence target continuam não autorizados. Após commit/push, parar para
human review no PR #33 Draft; sem merge/auto-merge.

### Execução histórica do discovery

Comandos abaixo descrevem o checkpoint anterior e seu RED conhecido. O opt-in
foi removido: na implementação, o oracle requerido roda em toda suíte normal.

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
preserva um RED explícito sem deixar os gates normais vermelhos e foi removido
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
| Semantic Product | O caminho normal já expõe NEXT_SENTENCE / TYPED_NEXT_SENTENCE, id, provenance, gap e readiness bloqueada. O fix habilita o mesmo inventário conservador para os caminhos diretos. |
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
de corpus ou threshold de hardware. A implementação adiciona um registro por
ocorrência direta e preserva ordenação/complexidade existentes. Guard geral
linear e probes extensivos só se autorizados no hardening separado.
