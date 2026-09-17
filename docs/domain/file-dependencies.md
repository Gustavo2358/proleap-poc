# FILE-DEPENDENCIES — frontend / SP

H4 aprovado; core N+C autorizado em 2026-09-16. W0 em implementação; W10 não autorizado.
[Campanha canônica](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/README.md)
(workspace: `../analysis-cfg/docs/product/file-dependencies/README.md`).
Comece pelo brief e [W0](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/work/active/FD-W0.yaml).

## Pontos de intervenção W0

- `AstBuilder.buildEnvironment/buildData` e `Ast.FileBinding/FileDescription`:
  ASSIGN hoje textual, FD/SD sem discriminante. Conservar cláusulas tipadas.
- `SymbolTableBuilder.buildFileEntities` já une FILE_CONTROL/FILE_DESCRIPTION.
  `ProcedureFileProgramReferenceResolverTest` já cobre SELECT+FD, GLOBAL, ancestral
  e shadowing. Reutilizar identidade canônica; não criar outro índice nominal.
- `ReferenceOccurrenceCollector` e resolução canônica resolvem por papel/escopo;
  WRITE/REWRITE necessitam record→owner, não lookup de filename por nome do record.
- `CobolSemanticProduct.StatementFact`/projector/writer/integrity validator são
  inventários fechados. Hoje não têm facts nativos I/O. `ObservedStatement`
  conserva observação, não certifica semântica de arquivo.
- Storage usa `ibm-enterprise-6.4-fixed-display-1047@1`; múltiplos layouts de FD
  exigem relação física provada em W3. Não modelar cada 01 como base independente.

Contrato SP de W0: declaração/owner/FD-SD/assignment variante e alvo source-level,
opcionalidade, organização/acesso/keys/status, record ownership, visibilidade,
origens e disponibilidade por dimensão. ASSIGN IBM publica assignment-name/
external file name conhecido (sourceKind ASSIGNMENT_NAME), sem inferir DD allocation
ou criar bindingMechanism, inclusive UNKNOWN; definição canônica no brief/contratos.
Sem uso não há READ inventado. Tipos e
versão final seguem [política semântica](../engineering/semantic-analysis-policy.md)
e evolução coordenada do decoder lower; nenhum cálculo de valor no projector.

## Semântica a consultar

O [perfil e as fontes](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/profiles.md)
selecionam IBM z/OS 6.4 + CICS TS 5.6 como core obrigatório; subset GnuCOBOL 3.2
é extensão posterior por decisão humana H4, sem bloquear W0 ou W11. Cobertura, non-goals,
casos e invariantes ficam somente no brief/matriz canônicos. Não importar regra
de DYNAMIC para assignment-name IBM. Origem COPY inclui expansão e include site.

W0 protege CALL, CICS Program Control, resolução nominal e storage existentes.
Gates focais: `mvn -B -ntp -Dtest=ProcedureFileProgramReferenceResolverTest,SemanticProductIntegrityValidatorTest,SemanticProductStatementInventoryTest,SemanticProductMoveCallContractTest test`.
Depois de estabilizar produção: `python3 -B scripts/harness/lean.py fast`;
qualification-local no checkpoint semântico conforme a estratégia central.
Nesta preparação documental: `python3 -B scripts/harness/lean.py docs`.

## FD-W0 — contrato declarativo em implementação

Autorização core N+C recebida em 2026-09-16 após aprovação H4; W10 não autorizado.
Writer corrente **SP 2.21.0**, com `fileInventory@1.0.0` obrigatório. A porta em
memória representa inventário ausente como UNAVAILABLE, distinto de KNOWN vazio.
Conector usa a entidade FILE canônica existente, owner completo e origens SELECT
+ FD/SD. `records` referencia identidades DATA publicadas; chaves/status carregam
binding nominal completo, incluindo candidatos e ambiguidade. Visibilidade e
organização/acesso escritos são tipados; UNSPECIFIED não inventa cláusula.
A disponibilidade do inventário é distinta dos gaps em cada declaração/ASSIGN.

Autoridade N-LR: IBM Enterprise COBOL 6.4, SC27-8713-03, **28 April 2026 update**;
PDF oficial baixado nesta execução, SHA-256
`22b5b8875041300484ac48cd16d8db6191fe4f17424cfe2f3a93392fb2aac0f6`.
Seções SELECT e ASSIGN (pp. 142–143 desta revisão), FILE SECTION e FD/SD.
ASSIGN interpreta apenas a assignment-name IBM: label documental terminada em
hífen e componente terminal de 1–8 caracteres, inicial alfabética, uppercase.
Palavra permite letras/dígitos; alphanumeric literal IBM também permite @/#/$.
O literal IBM é uma grafia da assignment-name N-LR (N02), não o filename/path de
outro dialeto em N04. Não admite DYNAMIC, EXTERNAL ou path literal de D/W10.
Nome de SD é comentário e nunca alvo externo. Não existe bindingMechanism.

Premissas: LANGUAGE_GUARANTEED para interpretação N-LR; ARCHITECTURE_GUARANTEED
para identidade nominal e projection de AST tipada. O projector apenas associa
entidades/declarações/referências já resolvidas; não reinterpreta fonte nem resolve
nomes. Custo linear no inventário de símbolos, declarações e referências; sem
cutoff. Registros não nomeáveis e declarações ausentes/duplicadas conservam gaps.
Declarações não geram statements ou execução de I/O. Efeitos/áreas compartilhadas
pertencem às próximas waves, assim como captures multi-unit de W9.

Oráculos: `FileDeclarationContractTest` (N-LR, SD, chaves/status, nomes homônimos,
owner, COPY/REPLACING e negativos), mais F-DECL e preservação histórica de CALL.
RED inicial registrado em `.harness-results/fd-w0/red.log`; gates/checkpoint final
ficam no estado canônico e na evidência E2E, sem claim antecipado de qualificação.

`record-name` de WRITE/REWRITE usa a referência DATA qualificada canônica,
permitindo associar o registro ao conector sem introduzir efeitos de execução.

## FD-W1 — fatos de uso para o slice estático

SP2.22.0/fileInventory1.1.0 acrescenta operations com disponibilidade própria.
AstBuilder conserva FileIoSurface tipada a partir dos contextos OPEN/READ/CLOSE;
projection publica statement/ordinal, comando, modo, perfil, binding/candidatos
com owners completos e origem. O lower não precisa ler grammarRule/texto COBOL.
Declarações e operações têm inventários independentes. W1 publica operações
PARTIAL (FILE_OPERATIONS_W1_SUBSET), sem afirmar efeitos/controle: cada uso tem
FILE_EFFECTS_CONTROL_PARTIAL. READ com cláusula readWith e CLOSE port I/O são
fora de N-LR; não promovidos a core. ASSIGN DYNAMIC continua fora do perfil N-LR.

N-LR: SC27-8713-03, 2026-04-28, seções OPEN/READ/CLOSE no PDF pinado em W0.
Neste slice só identificação nominal/ação; efeitos/status/outcomes aguardam W3/W4.
Oráculos FileOperationSliceTest: sequência estática, multi-file/modes, missing
file, CALL dentro de AT END, declaração sem uso e negativo de extensão dialectal.

## FD-W2 — regra e oracle antes da implementação

Entrada: W1 qualificado, SP2.22 e consumer2.0; ampliação coordenada SP2.23 /
fileInventory1.2 para família nativa N19–N25. Não há mudança AIR prevista.
Autoridade lida no PDF SC27-8713-03, revisão 2026-04-28: CLOSE pp328–329,
DELETE pp332–333, OPEN pp413–415, READ pp429–434, REWRITE pp437–439,
START pp460–461, WRITE pp476–480. Premissas LANGUAGE_GUARANTEED abaixo.

WRITE/REWRITE usam record-name qualificado de FD; FROM pode ser registro de outro
FD e não seleciona o arquivo alvo. DELETE remove RECORD de arquivo indexed ou
relative, sem semântica de remoção física de recurso. START posiciona; não lê
conteúdo. READ NEXT/KEY/INTO e WRITE FROM/ADVANCING têm papéis sintáticos distintos.
FROM literal aceito pela gramática multi-dialeto não pertence à sintaxe N-LR
consultada. READ com lock/WAIT e CLOSE port I/O continuam fora de N-LR.

AST conserva opções por arquivo em OPEN/CLOSE, papel dos operandos, relação KEY,
terminador explícito e corpos de AT END/INVALID KEY/EOP e suas formas NOT.
Nenhuma cópia de AST, parsing de grammarRule no lower ou inferência por FROM.
Resolver DATA/FILE canônico governa binding; projector associa record DATA ao
inventário FD existente. Se o owner não for provado, conserva gap, sem escolher
primeiro candidato. Tipo de referência não afirma MUST nem ordem de efeitos.

Oracles planejados FileNativeOperationTest: sete verbos, WRITE/REWRITE com FROM
em outro FD, ambiguidade/WS sem FD, NEXT/INTO/KEY, START relação, OPEN/CLOSE opções,
DELETE único uso e handlers com CALL inventariado uma vez. Negativos incluem
FROM literal, READ lock e CLOSE port I/O. Lower exercitará associação, targets,
composição existente e callbacks não incondicionais. W3/W4 fecharão efeitos e
outcomes; a representação W2 deve conservar essa estrutura com incompletude.
Algoritmo: índice record-id→file-id e associação de ocorrências resolvidas, O(fatos + operandos), com ordenação O(operandos log operandos)
por posição sintática; traversal de cada corpo uma vez, sem cutoff.

Checkpoint produtor W2: 12 oráculos nativos novos; F-DECL ampliado 147 testes
PASS, zero skips; FAST fixo 335 PASS. SP2.23/fileInventory1.2 exportado e fixtures
native/delete-handlers idênticas aos bytes consumidos pelo lower. Efeitos e
controle continuam explicitamente parciais; integração CLI qualificada no
checkpoint canônico cross-repo, sem antecipar W3/W4.
