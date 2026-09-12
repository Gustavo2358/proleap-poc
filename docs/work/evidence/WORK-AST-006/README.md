# CP6 W2A — evidência e handoff

Somente frontend. [Work item](../../active/WORK-AST-006/spec.md), branch
`feat/cp6-w2a-if-semantic-facts`. [Contrato](../../../domain/if-semantic-product.md).

## Autoridade

Baseline W1A `53d774026a1e4bcd969c7783a1d277aaa87b5f2f`, tree
`a43f0fc4ef8a227d47f012b9fcb4e410842ffcc0`. Checkout herdado `bd6dd1c` limpo e
com tree idêntica; a branch W1 permaneceu preservada. A branch nova foi rebaseada
sobre o merge W1A, confirmado pelo GitHub PR #34, sem merge local/remoto nesta
sessão. WORK-AST-005 arquivado conforme lifecycle; seus receipts não foram editados.
[Baseline e siblings](baseline.json.gz) registra HEAD/tree/status por repo e hash do
discovery formal humano em artefatos-e2e. Os arquivos preexistentes não rastreados
checkpoint-4e/cp5 de artefatos-e2e continuam fora deste trabalho.

## Decisões anteriores à implementação

SP 1.3.0 → 1.4.0, aditivo, um writer corrente conforme INTERNAL-CONTRACT-DEV-001.
Não existe reader/negociação produtivo neste repo; migração de consumer pertence
a checkpoint futuro. **Opção A**: IndependentStorageSet tipado próprio, separado
de scalarText. A autoridade da prova é a regra de WORKING-STORAGE aplicada ao
inventário inteiro de roots locais 01/77 elementares com shape/cobertura/origem
completos; não é diferença entre identidades. As exclusões e limites exatos estão
no contrato. Nenhuma análise geral de aliases foi criada.

## RED e primeiro GREEN

[Receipt RED](red/receipt.json): comando Maven com quatro testes, exit 1, quatro
falhas comportamentais e zero errors. Produção sem diff no momento do RED.
`red/test.log.gz` e os três JSONs preservam as falhas A completion, B predicate,
C ELSE, D storage. A cópia do teste antes da implementação está ao lado do receipt.

[Primeiro GREEN](green-1/receipt.json): cinco testes, exit 0, incluindo inspeção
canônica. Os JSONs closed/open/empty demonstram os fatos novos. A primeira
implementação conservadora exigia findings individuais para expressões/seção;
a inspeção mostrou que coverage é intencionalmente esparsa. A correção exige
coverage obrigatória de statement/data/cláusula, surface tipada exata e input
íntegro para os nós sem finding, sem ignorar findings parciais presentes.
Os logs da tentativa e da inspeção permanecem preservados.

## Oracles e regressão

`IfCheckpointW2ATest` usa COBOL real e JSON; `IfFactsOracle` consome apenas o port,
sem nomes, AST, fontes, IDs ordenados ou readiness textual como prova.
`IfCanonicalProofTest` retira provenance/coverage/completion canônicos e exige
falha conservadora. `IfSemanticsScaleTest` compara trabalho 64/128.

Os oráculos de contrato corrente foram atualizados explicitamente para novos
campos 1.4 e para cinco MOVEs do fixture CP7 cuja completion interna agora é
provada (MODELED 4→9; PARTIAL 10→5). O fixture/golden histórico não foi alterado.
A expectativa antiga de MOVE interno UNAVAILABLE virou assert da relação real
para GOBACK; fronteiras de paragraph/fim físico continuam UNAVAILABLE.
CALL aninhado conserva o limite W1A. O core legado continua sem provas novas
quando construído pelos construtores antigos.

As primeiras tentativas de negativos incluíram um nome reservado OTHER e uma
fronteira de paragraph sem período anterior; foram corrigidas para COBOL válido,
sem alterar gramática. Esses erros de fixture não são declarados RED semântico.
Unresolved DATA/INDEX mantém gap de kind na surface existente, sem inventar DATA.

## Escopo e estado

predicate truth value is NOT evaluated.
storage independence is source-derived evidence, not inferred from distinct IDs.

Nenhum AIR, CFG, lowering, solver/lattice, branch taken ou valor runtime produzido.
Nested ownership/completion é preservado; nested e formas gerais ficam fora da
admissão produtiva simples. W2C/W2B/W2D NOT_STARTED / NOT_AUTHORIZED.

A qualificação local abaixo foi executada. A publicação remota e seu CI pertencem
ao handoff pós-commit, que fixa PR URL, HEAD/tree final e resultado do CI sem
alterar recursivamente a árvore qualificada. O PR permanece Draft aguardando
revisão humana, sem merge/auto-merge. Nenhum gate downstream é reivindicado.

A primeira execução full passou docs/architecture/semantic e normalizador E2E,
mas falhou em naming: o checker legado rejeita o nome literal do próprio repo.
A redação autoral agora usa frontend; o receipt bruto de baseline foi comprimido
sem perda conforme a convenção dos logs, preservando exatamente seus bytes.
Não houve alteração da regra do gate nem edição de conteúdo da evidência.

## Superfície final de produção

Oito arquivos, todos no namespace `src/main/java/io/github/gustavo2358/cobolexplorer/`:

- `Ast.java`: presença lexical, origem dos braços e operador relacional tipado, sem novos nós/IDs.
- `AstBuilder.java`: índice canônico de completion com sucessor herdado e stack; origem dos braços por token de entrada.
- `IfSemantics.java`: provas imutáveis pós-binding de predicate, arms e conjunto independente, com work counts.
- `ScalarMoveSemantics.java`: composição do snapshot IF junto aos fatos W1.
- `semanticproduct/CobolSemanticProduct.java`: facts/invariantes 1.4 e validação indexada de containment.
- `semanticproduct/CobolSemanticPort.java`: exposição da prova de storage.
- `semanticproduct/projection/CobolSemanticProductProjector.java`: tradução dos fatos canônicos.
- `semanticproduct/transport/SemanticProductJsonWriter.java`: DTOs fechados e versão 1.4.0.

O profile positivo exato está no [contrato](../../../domain/if-semantic-product.md):
IF root com END-IF, igualdade positiva entre referência textual escalar inteira
local resolvida e literal básico, sem qualificadores, refmod, subscript ou outras
expressões; braços de MOVEs já provados no W1 e completion explícita. Predicate
KNOWN certifica BOOLEAN/PURE/TOTAL e todas as leituras enumeradas, sempre UNKNOWN
quanto ao valor. As entry/completions são StatementIds canônicos; não são labels.
ELSE ABSENT tem conteúdo conhecido sem entry; PRESENT vazio permanece PARTIAL.

## Handoff para possível W2C, sem autorização de início

O discovery ainda exige transporte, no checkpoint próprio, das formas AIR já
normativas Branch, Jump, Unknown com tipo BOOL conhecido e reads/origin/reason,
e da premissa DisjointStorage. W2A define a autoridade upstream e o catálogo
mínimo de fatos, mas não implementa esse codec, não gera essas formas e não
executa round-trip AIR. São necessários autorização humana própria, revisão
deste contrato e os REDs/gates de transporte previstos no discovery.

W2B também continua sem migração SP 1.4, materialização/admissão/assembler IF ou
tradução da independência. W2D continua sem a prova integrada closed/open.
Nenhum pin, solver, lattice, CFG ou relatório de valores foi modificado. Este
handoff não transforma os gaps downstream em PASS e não inicia essas slices.


## Qualificação local final

Os comandos/exits/hashes e snapshots completos estão em [qualification](archive-final.json).
Cada receipt `.json.gz` conserva o HEAD/tree base e os hashes SHA-256 de todos os
arquivos de produção antes/depois. Como a qualificação precedeu o commit, esses
HEADs são a base W1A; **não** pretendem identificar a árvore não commitada. O
snapshot `sourceFiles` do GREEN restaurado W2A cobre os 222 arquivos em `src/`.
[Scope](scope.json) confirma correspondência exata desse snapshot com a fonte
qualificada, os oito arquivos produtivos autorizados e siblings inalterados.

| Gate/comando final | Resultado observado |
| --- | --- |
| `./scripts/harness/check-full.sh` | exit 0; docs, architecture, fast, semantic, normalizador E2E e naming PASS |
| `./scripts/harness/check-performance.sh` | exit 0; probes existentes e novo IF PASS |
| `python3 scripts/harness/challenge-if-w2a.py` com diretório novo via `W2A_CHALLENGE_OUT` | 13 mutações semânticas rejeitadas, sem falha de compilação; restauração byte-exact por mutação; GREEN antes/depois |
| `python3 scripts/harness/challenge-call-w1a.py` | 8 mutações rejeitadas, restauração exata, GREEN antes/depois |
| `python3 scripts/harness/challenge-scalar-move.py` | 13 mutações rejeitadas, restauração exata, GREEN antes/depois |
| CLI Maven `exec:java --source ... --output ...` | 6 processos, todos exit 0; dois por fixture |
| Oráculo CLI independente | exit 0; fatos esperados, fitting, aliases e bytes entre processos iguais |
| Scope e integridade | exit 0; fonte qualificada idêntica, evidência antiga/gramática/resources/workflow e siblings preservados |

[Suíte completa](full-suite.json): 598 casos inventariados, 597 executados, zero
failures/errors e um skip preexistente: `SemanticConditionContextDiscoveryTest`
`requiredSemanticOraclesForFutureImplementation`, habilitado somente por
`semantic.condition.required=true`. Não foi habilitado pelo gate canônico;
não é teste W2A nem é contado como PASS. XMLs brutos comprimidos em `full-suite/`.
W2A executa 13 testes de fixture/JSON, três de insumos canônicos e um de escala;
a regressão adicional W1 compara três publicações históricas, sem alterar bytes
1.3.0. Todos passaram. Ambiente local: Temurin 25.0.4, Maven 3.9.16, Node 24.19.0,
Python 3.14.4; o workflow remoto existente usa Java 17 e Node 22.

[Work counts](work-counts.json), N=64/128: nodes 1235/2451, declarations 196/388,
references 193/385, arm members 128/256, lookups 194/386 e visitas reais de
completion 194/386 (=3N+2). Nenhum threshold de tempo é usado. A origem adicional
de braço ancora um token, evitando novo scan completo de braços aninhados.

[Determinismo CLI](cli/determinism.json): closed 14.520 bytes, SHA-256
`12d7aa24f16daf4d901a4e0df76904d0a2d525c86acc5ddd122d9e8001c5d827`;
open 12.213 bytes, `25b7a0abddc56169ff1d7329bd7e2be500245a546d08fd61ce81ebd04e32d6b1`;
empty 12.744 bytes, `b615225b893e7d9b6c3db045f8a07eaadbfde5e3ff9cdb2de42da98a9ec8e8c4`.
As duas cópias de cada produto estão preservadas. O gap dinâmico W1
`DYNAMIC_CALL_TARGET_VALUE_UNKNOWN` continua explícito nos logs; isso não é
avaliação de FLAG, análise de valores ou falha escondida de W2A.

As campanhas iniciais, as finais e os logs auxiliares de tentativas permanecem
separados. [Inventário inicial](archive-initial.json) e [final](archive-final.json)
registram hashes dos bytes brutos e gzip; compressão é sem perda. Erros iniciais
de fixture/expected/compilação e a falha de naming não são promovidos a REDs
semânticos nem escondidos pelo GREEN posterior.

A qualificação W2A de mutações é local; o workflow existente não foi refatorado.
O CI remoto executa full, performance e campanhas MOVE/CALL. Seu resultado no
SHA publicado deve constar do receipt pós-commit e do PR, separado desta execução.

**W2A IMPLEMENTED / QUALIFIED / AWAITING_HUMAN_REVIEW** (qualificação local).

**W2C NOT_STARTED / NOT_AUTHORIZED**.

**W2B NOT_STARTED / NOT_AUTHORIZED**.

**W2D NOT_STARTED / NOT_AUTHORIZED**.
