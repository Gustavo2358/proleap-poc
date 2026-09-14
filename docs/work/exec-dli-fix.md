# EXEC DLI — handoff do fix

**DONE / APPROVED / MERGED** — [PR #50](https://github.com/Gustavo2358/proleap-poc/pull/50).
`EXEC DLI` deixou de bloquear o preprocessing: agora é uma embedded language
explicitamente identificada, lexicalmente delimitada e semanticamente
opaca/PARTIAL. Nenhuma semântica IMS foi implementada.

## Fechamento em 2026-09-14

- Branch de implementação: `fix/exec-dli-opaque`; base remota confirmada por fetch:
  `eb5c78101cce335dcd56cf8a8c9388f91b031917`.
- Head qualificado e aprovado externamente:
  `6dfdf3bb302873e0ff87756b476d08fd96a674eb`. A aprovação externa foi informada
  pelo responsável; não há reviews, comentários ou threads registrados no GitHub.
- Pré-merge: head e base inalterados desde a entrega, PR aberto e mergeable,
  [Fast CI #105](https://github.com/Gustavo2358/proleap-poc/actions/runs/34871451078)
  `completed/success` no head qualificado, sem novos blockers.
- Merge explícito pelo método usual do repositório, merge commit:
  `88e9493472c183357445c0be61ca80e7193b6e37`, em `2026-09-14T17:09:45Z`
  (14:09:45 UTC−03). Sem auto-merge, rebase ou resolução de conflito.
- Após fetch, `origin/main` continha o merge e os três commits do fix.
  Head e merge têm a mesma árvore Git:
  `62374b9c61a4e0c3988461076bfbd404409ce391`; `git diff --exit-code` entre
  ambos passou. Nenhum delta produtivo foi introduzido no fechamento.
- A equivalência permite reutilizar a qualificação abaixo, inclusive corpus e
  downstream. FAST/full/qualification/corpus/decoder/CFG não foram repetidos.
  O checkout principal foi preservado; a referência remota é a autoridade final.
- Não havia work item formal DLI em `active/` ou `history/`; nenhum foi inventado.
  O índice referencia este handoff concluído. Não havia blocker DLI pendente no
  backlog nem no roadmap E2E inspecionado; não há `roadmap.md` neste repositório.
- A perda descritiva do lower fica em
  [BACKLOG-LOWER-002](backlog.md#backlog-lower-002--preservar-observedshape-na-opacidade-air),
  sem implementação, repin ou blocker retroativo.

## Causa e delta desde o discovery

Faltava a alternativa explícita DLI no preprocessor; recovery deixava `EXEC`
chegar ao catálogo fechado. A causa foi confirmada na base de implementação. O discovery foi
lido integralmente da cópia na raiz agregadora, idêntica à cópia da worktree de
investigação. Seu SHA histórico não foi usado como baseline.

A main usada como base publicava SP 2.15.0, com DVI, em lugar do 2.14.0 do discovery.
`ObservedStatement` só recebe continuation executável a partir de
`Division.normalContinuations`; embedded adjacency é separada. DLI fica fora
desse mapa, portanto nenhuma mudança produtiva no projector de continuation
foi necessária. Testes verificam `UNAVAILABLE` para TERM/SCHD/GU/GNP/REPL.

## Implementação

Um token lexical DLI por região nas duas gramáticas, policy própria e scanner
linear de palavras/literais/comentários. O fechamento exige END-EXEC real;
outro EXEC, EOF, literal aberto ou payload vazio falham antes do parser.
Transporte multiline com prefixo/sufixo verificados, preservando o slice
normalizado e deixando o período fora dele. AST `EmbeddedLanguageStatement(DLI)`
sem hosts; SP `OBSERVED/OPAQUE_DLI/PARTIAL`, gap e referências não determinadas.
Nenhum parser IMS, fact IMS, normalizador, SourceMap ou renderer vizinho alterado.

R3 expôs que as gramáticas não aceitavam CR isolado, já preservado pelo
normalizador. Duas regras NEWLINE foram alinhadas com LF/CRLF/CR. Um RED adicional
prova que CR entre statements sem período causava lexer recovery; o GREEN exige
zero diagnostics lexicais. Não houve mudança em charData.

## Evidência atual

Logs, fontes, produtos brutos e comandos completos foram gerados em
`.harness-results/exec-dli/` (ignorados; resultados não editados para obter PASS).
Na retirada da worktree, foram preservados no workspace local em
`exec-dli-fix/evidence/frontend-harness-results/exec-dli/`. Os caminhos dos
comandos abaixo conservam o contexto original da execução.

- RED focal inicial: 19 testes, 8 failures, 10 errors, zero skips. Dez casos
  válidos bloqueados pela policy EXEC; oito negativos sem diagnóstico DLI local.
- G1/G2/G3 combinados: 102 testes, zero failures/errors/skips.
- DecoderSuite do lower, incluindo AdmissionAdapterSuite: 590 checks PASS.
- CLI frontend: sete runs, exit 0; mínimos 2 statements, mistos 4, corpus 193.
- CLI lower: sete runs, exit 0, AIR produzida conservadoramente.

## Corpus e limites

COPAUS1C SHA-256
`27a969cbee69426fa1056053e676041430e99399912f0e27ee1f1a454093c21e`, igual ao
discovery. Sete regiões: 439–443, 465–469, 495–498, 525–528, 575–578,
581–582, 584–587. Sentinelas MOVE 445/471/500/530/579/588 e CICS
558/566 presentes. Provenance e payloads comparados ao slice normalizado.
COPY DFHAID/DFHBMSCA continua ausente; somente cpy/cpy-bms reais foram usados.
193 statements, sete OPAQUE_DLI; gaps totais 470 (CICS unknown) e 468 (disabled).
Sem promessa de semântica completa ou pipeline sem incerteza.

## Downstream

Lower isolado em `b163cbcc1795962fac3f14ac4c29b73ae93e5f41`, com contrato
SP 2.15.0 e pin frontend `c5d2bfe4319800e6b6c4c30ffe7e4a2c2451e892`.
AIR Java `eaf83c6233d347348a3927b5983de03cde62554a`; AIR normativa
`31893d1f4d203d19a61a750e2c4220120d9dab84`, versão 2.0.0.
Build de dependência via helper FAST existente, JDK 21, sem qualificação AIR.
Nenhum pin ou fonte downstream alterado.

AIR conserva operação opaca, nenhum controle conhecido e remainder aberto,
com memória desconhecida e dependências any_resource. O lower descarta
`observedShape` no modelo interno atual e conserva `observedKind` e `gapCode`;
[BACKLOG-LOWER-002](backlog.md#backlog-lower-002--preservar-observedshape-na-opacidade-air)
registra o follow-up de fidelidade descritiva, sem inventar semântica IMS.

GU/GNP/REPL/SCHD/TERM, PCB, segmentos, status, opções, efeitos e controle IMS
continuam desconhecidos. Não se distingue COBOL acidental dentro de região
lexicalmente válida sem interpretar a DSL. Problemas históricos SQLIMS de
flatten/adjacência/recovery permanecem fora deste PR.
EOF após END-EXEC é válido para a região; a gramática COBOL continua exigindo
período de sentença. COPY REPLACING que corrompa o transporte falha no segundo
lexer, sem AST DLI recuperada.

## RED → GREEN por requisito

| ID | Witness e RED observado | Mudança e GREEN executado |
| --- | --- | --- |
| R1 | TERM/GU/GNP/REPL/SCHD, parênteses, ponto, EOF, GOBACK/MOVE: policy EXEC bloqueava o teste. | Token/policy/visitor DLI; uma região e sentença correta, rawText exato, COBOL posterior presente. EOF sem ponto validado como região de preprocessing; AST no EOF com período. |
| R2 | Par TERM/SCHD no IF, dois DLI na mesma linha, CICS/SQL/SQLIMS antes/depois: mesmo RED de preprocessing. | Um token por bloco, sem repetição agregadora; bijeção AST/SP e identidades de todas as famílias. |
| R3 | Literal com END-EXEC/aspas duplicadas/chaves/Unicode/tags e comentários em LF/CRLF/CR: preprocessing bloqueado. Depois, CR expôs erro gramatical/lexical. | Transporte sem flatten e newline CR nas duas gramáticas; igualdade com slice normalizado, zero erro lexical no adversarial CR. |
| R4 | EOF, GOBACK, MOVE, outro EXEC, literal aberto e payload vazio: erro genérico EXEC, sem identificação DLI; EXEC desconhecido já era rejeitado. | Scanner aborta localmente, sem Outcome/AST publicado e sem END-EXEC sintético; fallback EXEC continua inexistente. |
| R5 | COPY aninhado/REPLACING/continuação/Unicode: não houve RED independente pré-produção deste teste. Primeira execução corrigiu oracle de coluna final inclusiva (18, não 19). | GREEN executado com rawText normalizado, arquivo/linhas/colunas/includeChain e offsets code points; posterior MOVE/GOBACK com origem própria. |
| R6 | PROGRAM/LINK/XCTL/SQL/nomes internos: bloqueio EXEC antes da análise. | Hosts/occurrences internas vazios; CICS verdadeiro antes/depois conserva facts nos modos UNKNOWN/NEW_LOGICAL_LEVEL, e comportamento DISABLED. Oracle inicial de DISABLED foi corrigido para ausência de facts, conforme contrato vigente. |
| R7 | AST/SP inicialmente inacessíveis pelo RED EXEC; nenhuma falsa continuation foi observada na main atual. | OPAQUE_DLI/PARTIAL, gap reconciliado, sem referências/normalContinuation; JSON e consumer local bloqueados; AIR opaca com envelopes abertos. |
| R8 | Discovery registrava bloqueio do corpus; não foi repetido RED de corpus pré-fix nesta sessão. Hash e intervalos foram confirmados antes da implementação. | GREEN novo nos sete intervalos e oito sentinelas; CLI + AST/SP/provenance + AIR inspecionados. |

## Comandos e resultados

Os logs abaixo usam o diretório original `.harness-results/exec-dli/`, preservado
na localização de fechamento indicada acima. Colunas F/E/S:
failures/errors/skips de Surefire. Nenhum gate planejado é listado como executado.

| Comando efetivamente executado | Log | Exit | Testes; F/E/S |
| --- | --- | --- | --- |
| `mvn -o -B -ntp -Dtest=ExecDliOpaqueTest test` | `test-compile-first.log` | 1 | testCompile: três nomes de API do oracle ajustados; nenhum teste executado |
| mesmo comando | `red.log` | 1 | 19; 8/10/0 |
| mesmo comando | `green-first.log` | 1 | 19; 1/0/0 (CR) |
| `mvn -o -B -ntp -Dtest=ExecDliOpaqueTest,ExecDliProvenanceTest,GrammarCoverageManifestTest test` | `green-second.log` | 1 | 24; 1/0/0 (coluna inclusiva do oracle) |
| `mvn -o -B -ntp -Dtest=ExecDliOpaqueTest,ExecDliProvenanceTest test` | `green-third.log` | 1 | 24; 0/1/0 (oracle CICS DISABLED) |
| mesmo comando | `g1.log` | 0 | 24; 0/0/0 |
| G2/G3 abaixo | `g2-g3.log` | 1 | 77; 2/0/0 (inventário de statements exigiu nova alternativa e fixture) |
| `mvn -o -B -ntp -Dtest=StatementModelAstTest test` | `g2-statement-green.log` | 0 | 3; 0/0/0 |
| `mvn -o -B -ntp '-Dtest=ExecDliOpaqueTest#bareCrBetweenUnterminatedStatementsIsNotLexerRecovery' test` | `cr-lexer-red.log` | 1 | 1; 1/0/0 |
| G1/G2/G3 abaixo | `g1-g2-g3-green.log` | 0 | 102; 0/0/0 |
| `python3 -B scripts/harness/lean.py fast` | `fast.log` | 0 | 282; 0/0/0, mais 12 testes Python |
| `mvn -o -B -ntp -Dtest=ExecDliOpaqueTest test` | `adversarial.log` | 1 | 24; 1/0/0 (oracle comparava indentação física com normalizada) |
| mesmo comando | `adversarial-green.log` | 0 | 24; 0/0/0 |
| `python3 -B scripts/harness/lean.py docs` | `docs-first.log`, `docs-final.log` | 0 | 12 testes Python, zero falhas/skips |
| `git diff --check` | stdout vazio | 0 | higiene |

Comando G2/G3:

```sh
mvn -o -B -ntp -Dtest=PreprocessorEnginePolicyTest,SourceNormalizationPreprocessingIntegrationTest,SourceNormalizerTest,SourceProvenanceTest,GrammarCoverageManifestTest,AstBuildCoverageTest,AstBuilderTypedTraversalTest,StatementModelAstTest,SemanticCoverageTest,SemanticProductStatementInventoryTest,SemanticProductCheckpoint7JsonTest,SemanticProductCheckpoint8LoweringReadinessTest,CicsProgramControlTest test
```

G1/G2/G3 GREEN acrescentou `ExecDliOpaqueTest,ExecDliProvenanceTest` no início
exato desse seletor. Não se repetiu FAST após acrescentar somente o teste de tags:
produção/harness permaneceram idênticos; a suíte focal modificada foi reexecutada.
O FAST qualifica o conteúdo produtivo final; o teste adicional tem receipt próprio.

Build lower: `run.bootstrap(fast=True)` e
`run.run(run.maven('-o','-Dexec.skip=true','-DskipTests=true','install'))`, pelo
harness existente, com `LOWER_BUILD_ROOT=../build-lower` absoluto e JDK 21.
Exit 0 em `lower-build.log`; build apenas, sem claim de testes/qualificação AIR.
`java -ea -Xmx1g -cp <lower-classpath.txt> io.github.gustavo2358.lower.adapters.testing.DecoderSuite`
executou com exit 0 e `LOWER_TESTS=590`, incluindo AdmissionAdapterSuite,
sem performance (`lower-decoder.log`).

`python3 -B .harness-results/exec-dli/run-cli.py` e `run-lower.py` executaram
os sete vetores completos salvos em `cli-results.json`/`lower-results.json`.
Foram repetidos com `DLI_RECEIPT_DIR=final-cli` após o último delta produtivo:
14 processos CLI novos, todos exit 0. Logs anteriores preservados separadamente.
`python3 -B .harness-results/exec-dli/inspect-products.py` (exit 0) reconciliou
AST/SP/AIR, gaps, CICS vizinho, sentinelas e ausência de controles conhecidos por
identidade; `final-cli/oracles.json` e `final-oracles.log` guardam o resultado.

## Diff e revisão adversarial

- Produção: `src/main/antlr4/{Cobol,CobolPreprocessor}.g4`;
  `src/main/java/io/github/gustavo2358/cobolexplorer/{DliRegion,PreprocessorEngine,Ast,AstBuilder}.java`.
- Manifesto: `src/main/resources/semantic-coverage/grammar-rule-manifest.tsv`.
  Contagem calculada sobre a base: COBOL 598+1=599, preprocessor 30+1=31,
  total 630; statements 50+1=51. Igualdade de conjuntos e ausência de duplicatas
  continuam obrigatórias; nenhuma redução de assert de exaustividade.
- Testes: `ExecDliOpaqueTest` e `ExecDliProvenanceTest` novos;
  `AstBoundaryTestSupport`, `GrammarCoverageManifestTest`,
  `PreprocessorEnginePolicyTest`, `StatementModelAstTest` e fixture
  `src/test/resources/cobol/semantic/statements.cbl` ajustados.
- Harness: `scripts/harness/lean_project.py` inclui a suíte focal DLI no FAST.
- Docs/evals: domínio preprocessing/AST/SP, catálogo de evals e este handoff.

Revisão do próprio diff executada: não há fallback EXEC, uso de recovery como
validade, delimiter sintético, flatten DLI, chaves/newlines como fechamento,
blocos unidos, classificação SQLIMS/UNKNOWN, hosts/referências/facts internos,
NOP, continuation executável inventada, captura das sentinelas, período no payload,
SourceMap novo, alteração de corpus ou ajuste oportunista de exaustividade.
A revisão adicional de tags passou após corrigir somente seu oracle de
normalização; nenhuma falha produtiva permaneceu inexplicada.

Escopo C2/C3 com ajuste lexical de CR coberto pelos gates de normalização,
provenance, gramática e AST. `qualification-local`/full, corpus integral, FAST de
siblings e CFG não foram executados: nenhum normalizador/SourceMap/renderer
compartilhado ou algoritmo downstream foi alterado. Decoder/admission e envelope
AIR deram evidência direta da fronteira modificada. Não se reivindica CFG GREEN.
