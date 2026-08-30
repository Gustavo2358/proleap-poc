# WORK-TEST-002 — Substituir cardinalidades globais por oráculos semânticos

## Resultado

Os baselines Java e E2E do COACTUPC deixaram de congelar totais globais de parse
tree, AST, profundidade, scopes, símbolos, referências e gaps. Esses valores
continuam publicados pela aplicação como telemetria, mas o gate valida fatos
semânticos e operacionais revisáveis.

## Evidência durável

`SemanticModelBaselineCharacterizationTest` preserva formas de CALL, statements
estruturados, estado do frontend e fidelidade da grafia. A regressão E2E preserva
parser/lexer, COPYs não resolvidos, coverage e readiness sem acoplamento à
cardinalidade incidental dos produtos internos. A política em
`docs/engineering/semantic-testing.md` distingue contagem derivada de fixture
pequena de total global observado em corpus.

## Verificação

O teste focalizado passou com 2 testes; `fast`, `semantic` e a regressão E2E
passaram. O agregado `full` executou essas fases com sucesso e terminou vermelho
somente no check de naming por um identificador legado preexistente em
`docs/work/history/WORK-TEST-001.md`, fora do escopo desta mudança.
