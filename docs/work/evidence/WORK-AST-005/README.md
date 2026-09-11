# CP6 W1A — evidência e handoff

W1A somente no frontend. Baseline obrigatório `8722945cc4cd2052c6091533f6ee6989278aa2f8`,
tree `63eba137162aa47e38d4858ff522058a8678a8dd`; discovery aprovado em
`analysis-cfg@c39a92f930b1c693857a0b30a1f5155f3f81520c`. Branch
`feat/cp6-w1a-call-semantic-product`, work item WORK-AST-005.

O [contrato canônico](../../../domain/call-semantic-product.md) documenta autoridade,
algoritmo, shape JSON, readiness, provenance, exclusões e a evolução interna 1.3.0.
O recibo remoto final registra HEAD/tree/base, runs push/PR e checkout real; é
produzido após o commit, sem alterar recursivamente a árvore validada.

## RED → implementação → GREEN

[Dynamic X8 baseline](red/dynamic-x8.json) e [literal baseline](red/literal.json)
são outputs brutos da autoridade congelada. Os [fontes X8](red/dynamic-x8.cbl) e
[literal](red/literal.cbl) não foram simplificados. `red/test.log.gz` conserva o
RED comportamental: ausência de acesso/continuação/valor ajustado no primeiro e
perda do target literal tipado no segundo.

`CallCheckpointW1ATest` contém 11 testes de produto/JSON: X8 recebe FITTED_TEXT,
RIGHT_PAD_SPACE e `PROGA   `; X5 mantém FULL_IDENTITY; CALL DATA preserva binding,
acesso provado e GOBACK como sucessor condicional; CALL LITERAL publica PROGA.
USING/RETURNING/GIVING/handlers têm presença explícita; ausência conhecida difere
de input incompleto. Efeitos UNKNOWN e outcomes OPEN persistem. Runtime DATA
continua UNKNOWN com ou sem MOVE anterior. Ambiguidade/candidatos e provenance
permanecem locais. Refmod/subscript, storage excluído, literal não básico,
truncamento e fronteiras estruturais falham de modo fechado.

O teste adicional de CALL aninhado em handler revelou apropriação indevida de seus
argumentos pelo CALL externo. `red/nested-handler-valid-source.log.gz` é o RED
semântico válido; a coleta agora parte apenas do USING direto. A tentativa anterior
`red/nested-handler.log.gz` ultrapassava coluna 72 e é registrada como erro de
formatação de fixture, **não** como prova semântica.

## Adversariais e regressão

[Resultados W1A](challenges/results.json): oito mutações compiláveis detectadas,
restauração byte-exact por arquivo e segundo GREEN. Foram desafiados literal perdido,
acesso apagado/fabricado, sucessor errado, USING/RETURNING ocultados, efeitos NONE
e padding apagado. Logs brutos preservados em gzip; erro de compilação não conta.

`validation/maven-clean-verify-online.log.gz`: BUILD SUCCESS, 579 testes, zero
failures/errors, um skip preexistente. A tentativa offline anterior, com 578 testes
verdes, falhou no packaging por dependências ausentes no cache; seu log permanece
em `validation/maven-clean-verify.log.gz` e **não** é declarado verify PASS.

[Resultados scalar MOVE](scalar-challenges/results.json): 13 mutações detectadas e
segundo GREEN. O primeiro runner parou antes de executar o challenge 06 porque a
âncora passou a ocorrer também no CALL; `scalar-initial-partial/runner.log.gz`
registra essa execução parcial, sem claim de PASS. As âncoras do harness foram
restringidas à ramificação MOVE e a execução completa passou. Na tentativa parcial,
os logs individuais green-before/01 foram sobrescritos ao reiniciar o harness
legado após falha ao mover seu diretório entre filesystems; seus hashes permanecem
no recibo, mas não são reivindicados como outputs brutos disponíveis. O runner e
os logs 02–05 foram conservados. A execução completa possui todos os logs.

O fechamento também executa full (docs, architecture, semantic, regressão E2E do
normalizador e naming), performance e challenges scalar MOVE. Não existem gates
separados chamados integration/scope/manifest: integração participa da suíte e do
E2E; manifestos participam do HarnessDocsTest; scope é conferido por diff explícito.
Os recibos finais são listados em `validation/results.json`.

## CLI, determinismo e consumer congelado

[Recibo CLI](cli/result.json) conserva duas execuções reais por fonte e hashes dos
bytes publicados. [Dynamic X8](cli/dynamic-x8-1.json) e [literal](cli/literal-1.json)
são os produtos correntes; os arquivos `-2` são a segunda execução independente.

| Produto | SHA-256 nas duas execuções |
| --- | --- |
| Dynamic X8 | e45c6fe191c6e9be4b35da793663fc908b0b600c848a236d797170e286614010 |
| Literal | e88bb1029fd184e3dabefd28bf37cdea99193630a9f5caeeebc1bd00e56870a1 |

[CP5 corrente](cli/cp5.json) foi comparado estruturalmente ao golden do lower
congelado: somente versão 1.2.0→1.3.0 e novo MOVE.textAdjustment=null foram excluídos
da comparação. Todo o restante é idêntico, incluindo fatos, IDs, gaps e provenance.
Bytes anteriores `468e3207f578e428ace89a311eadbd6e27c670331b739675b761479352adc7af`,
atuais `a0cb9191fc515cd8605fc50132273bb97e93b92772016fdb1435e506f7a1fcf4`.

O decoder extraído de `cobol-lower@18016f16b4f63149eb1bb4ca13db7e12593d8909`,
compilado isoladamente fora dos siblings, recebeu os outputs CLI CP5/X8/literal e
rejeitou os três com **UNSUPPORTED_CONTRACT**. [Log](cli/frozen-lower.log) e pacote
`cli/reproduction.tar.gz` preservam comandos, fontes do probe/decoder e hashes.

**EXPECTED INTERNAL INCOMPATIBILITY**, conforme INTERNAL-CONTRACT-DEV-001 aprovado:
um único writer 1.3.0, sem adapter/downgrade/legacy. W1C deve atualizar consumer e
pin. Não há integração CP5/CP6 verde com esses pins incompatíveis.

## Escopo e limite de autorização

Sete arquivos produtivos: Ast, AstBuilder, ScalarMoveSemantics, core SP, projector,
writer e consumer audit do próprio frontend. Grammar/vendor, parser externo,
semântica IF e NEXT SENTENCE não relacionada: delta zero. Nenhuma dependência AIR,
Invoke, lowering, CFG, solver, dataflow ou resolver de dependências adicionada.
Nenhuma interpretação/canonicalização do conteúdo runtime de DATA.

WORK-AST-004 foi retirado de active após confirmação remota de merge do PR #33;
o histórico e o routing foram atualizados. WORK-AST-005 permanece ativo para review.
Siblings são read-only; o diretório agregador não participa de operações Git.

Entrega: W1A IMPLEMENTED / AWAITING_HUMAN_REVIEW, PR OPEN / DRAFT, sem merge.
W1B/W1C/W1D/W2 NOT_STARTED / NOT_AUTHORIZED.

## Integridade da evidência

`log-hashes.json` lista hashes dos logs descomprimidos e dos gzip determinísticos.
Não há reescrita de linhas, supressão de falhas ou substituição do RED por GREEN.
Caches, toolchains e builds reproduzíveis não são versionados.
