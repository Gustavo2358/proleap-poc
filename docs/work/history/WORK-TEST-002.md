# WORK-TEST-002 — Substituir cardinalidades globais por oráculos semânticos

## Resultado

Os baselines Java e E2E do COACTUPC deixaram de congelar totais globais de parse
tree, AST, profundidade, scopes, símbolos, referências e gaps. Esses valores
continuam publicados pela aplicação como telemetria, mas o gate valida fatos
semânticos e operacionais revisáveis.

## Evidência durável

`SemanticModelBaselineCharacterizationTest` preserva formas de CALL, statements
estruturados, estado do frontend e fidelidade da grafia. A regressão E2E preserva
parser/lexer, COPYs não resolvidos, coverage, inventário de occurrences,
resolução, provenance e readiness sem acoplamento à cardinalidade incidental dos
produtos internos. `assert-semantic-artifacts.mjs` carrega os snapshots sem
executá-los e reconcilia IDs, contagens derivadas, relações entre produtos e
sentinelas conhecidas por nome de campo, independentemente da ordem serializada.

Os baselines textuais e os mapas observados de quantidade de statements foram
removidos. Contagens pequenas permanecem somente onde são fatos diretamente
derivados do fonte, como os CALLs explícitos das fixtures. A política em
`docs/engineering/semantic-testing.md` distingue contagem derivada de fixture
pequena, consistência interna de inventários e total global observado em corpus.

## Verificação

Os cinco testes Java focalizados e os três testes fail-closed do runner Node
passaram; `fast`, `semantic` e a regressão E2E também passaram. O agregado
`full` executou essas fases com sucesso e terminou vermelho somente no check de
naming por um identificador legado preexistente em
`docs/work/history/WORK-TEST-001.md`, fora do escopo desta mudança.
