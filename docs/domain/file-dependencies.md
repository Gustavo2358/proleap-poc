# FILE-DEPENDENCIES — frontend / SP

CORE N+C qualificado; closeout/merges autorizados. W10 DEFERRED / OPTIONAL_EXTENSION,
NOT_PART_OF_CORE, requer nova autorização. [Estado canônico](https://github.com/Gustavo2358/analysis-cfg/blob/main/docs/product/file-dependencies/closeout.md).
As seções abaixo registram checkpoints históricos.

H4 aprovado; core N+C autorizado em 2026-09-16. W0–W9 qualificadas local; W11 em qualificação; W10 não autorizado.
[Campanha canônica](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/README.md)
(workspace: `../analysis-cfg/docs/product/file-dependencies/README.md`).
Comece pelo brief e pelo item atual indicado no estado canônico.

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

## FD-W3 — entrada / primeiro oracle de storage

W2 qualificada; efeitos W3 em execução. Autoridade/tabela em
`../analysis-cfg/docs/product/file-dependencies/d-effect.md` no worktree sibling.
Primeiro delta C4: FILE SECTION entra no inventário físico geral. READ p431
(SC27-8713-03, 2026-04-28) faz descrições01 do mesmo FD compartilharem área;
SAME RECORD AREA p157 une somente arquivos nominalmente resolvidos, nunca seus
ResourceIds. Footprint é máximo, não soma. SORT AREA não equivale a RECORD AREA.
WORKING-STORAGE provadamente separado conserva identidade/independência mesmo
com tamanho opaco dentro do FD. Não muda valores, MUST ou controle neste delta.
Oracle FileStorageLayoutTest antes da produção: cinco casos positivos/negativos;
RED válido por ausência dos nós FILE no inventário físico. Algoritmo: componentes
existentes + união de grupos declarados por ID; sem pares de objetos, parsing no
projector, lookup externo ou cutoff. Q-SHARED obrigatório no fechamento de W3.

W3 em progresso: `FileIoMemory` usa binding canônico e views gerais, indexados por
unidade/AST; inventário de mutações une destinos possíveis sem afirmar MUST.
`FileIoEffects` conserva FROM anterior ao I/O e efeitos condicionados ao resultado;
READ completo sem INTO (incluindo status) precede o MOVE implícito, p430. Status
primário textual exato e receptor INTO com classe/endereço/disjunção provados têm
MUST; buffer READ nunca ganha MUST pela grafia do verbo. EOF/INVALID KEY/erro não
executam INTO; REWRITE INVALID KEY conserva o MOVE FROM anterior. Endereço variável
conserva bound da área e gap, sem captura antes de READ. Formas fora de N-LR não
recebem provas fortes. Cópia FROM reaproveita a regra geral de MOVE/fit.

SP2.24/fileInventory1.3 publica esses fatos; storage1.8 acrescenta prova geral
INDEPENDENT_LOCAL_STORAGE para WS/FILE. A tabela condicional não certifica outcomes
alcançáveis nem seleção de handler (W4). Varreduras/indexação O(AST+binding+layout)
e O(destinos de efeitos), sem solver, fixpoint novo ou cutoff. Focais e REDs em
`.harness-results/fd-w3/`; qualificação/pin bilateral ainda pendentes.

Q-SHARED W3 encontrou dez falhas de expectativas também reproduzidas no HEAD W2
isolado (`git archive b559292`): gramática DLI já existente (51 alternativas/630
regras), papéis tipados SET/DISPLAY/READ INTO, facade omitindo storage/FILE e
metadata de mnemonic. Os testes agora preservam esses contratos explícitos;
o fixture de quatro estados usa QUALIFY não especificado como UNSUPPORTED real.
Não houve ampliação de dialeto/capability. O oracle futuro de condições permanece
condicionado por `semantic.condition.required`, skip histórico explícito.

A qualificação de COACTUPC também reproduziu no W2 uma contradição entre VALUE
multilinha de COPY com origem aproximada e a exigência de proveniência exata do
contrato de entrada. `StorageInitialSemantics` conserva esse item como UNKNOWN /
SOURCE_PROVENANCE_NOT_PROVEN, sem abortar a publicação nem relaxar o validador.
O contracaso do corpus e os valores com origem exata são testados conjuntamente.

O último gate de naming também bloqueava o nome real do repositório em URLs e
evidências históricas. Nenhuma evidência foi reescrita: a regra aceita somente o
identificador exato do repositório em Markdown de docs. Seis testes protegem essa
exceção, nomes parecidos, conteúdo misto, código e paths; rodam em FAST/full.

Checkpoint produtor W3: FAST fixo 336 PASS; qualification-local 887 testes,
zero falhas/erros e um skip histórico condicionado, normalizer/naming PASS.
Focal final FROM/contrato/CALL 111 PASS. FROM não resolvido preserva gap nominal
e leitura aberta; alias preserva leitura mesmo quando cópia é MAY. Logs finais:
`.harness-results/fd-w3/{fast-4,qualification-local-5,from-read-bound-green}.log`.
Pins e E-SELECTED são fechados no checkpoint canônico após commit dos produtores.


## FD-W4 — dispatch / SP2.25, fileInventory1.4

Autoridade adicional: mesmo SC27-8713-03 de 2026-04-28, Declaratives pp264–265,
status p299, INVALID KEY pp303–304, READ AT END p432, WRITE EOP p480 e USE pp714–715.
Ast.UseClause é metadata; corpo declarativo entra uma vez no inventário e nunca
como prefixo da entrada primária. FileIoControl deriva seleção por arquivo/modo,
rotas SUCCESS/END/INVALID_KEY/OTHER_ERROR/EOP e completions a partir da AST e do
binding canônico. Projector somente transporta. FILE_HANDLER separa os corpos
condicionais, inclusive IF/EVALUATE/nested I/O; período e END-* delimitam a saída.
Status precede handlers/USE; EOP utiliza efeitos de WRITE executado. Normal não
prova00. Explicit END/INVALID KEY domina USE; READ OTHER_ERROR sem USE pode alcançar
NOT AT END, sem INTO. OPEN fornece modo; depois dele não se infere modo por ordem
textual: possíveis USE por modo + alternativa sem USE, gap explícito. Binding
incerto conserva corpos plausíveis e os eventos não excluídos pela prova.

Completions reconhecem término normal; GOBACK/GO TO e outras saídas não são
forçadas a retornar. DEBUGGING é inventariado com gap, não USE de erro. Seleção
GLOBAL ancestral integra W9. Custo O(AST + bindings + operações × declarações USE
visíveis + tamanho das rotas), sem corte por quantidade; não é um solver de arquivos.
Oráculos FileControlPlanTest/ContractTest precedem a implementação; fixtures reais
cobrem seleção, status, handlers, recursão/saídas, negativos e transporte bilateral.


Checkpoint produtor W4: focal162 + GO TO7 + entrada20 PASS; FAST fixo336 PASS;
qualification-local898, zero falhas/erros e um skip histórico previsto, normalizer/
regressão E2E/naming PASS. SP2.25 control/handlers/USE exportados nos seis fixtures
bilaterais. Logs `.harness-results/fd-w4/`; E-SELECTED nos pins finais segue CFG.


## W5 — fases SORT/MERGE e SD

SP2.26/fileInventory1.5 publica roles WORK/INPUT/OUTPUT e sortPlans com endpoints,
raízes, links de parágrafos e completions canônicos. Autoridade SC27-8713-03,
update2026-04-28: MERGE pp400–404, RELEASE pp434–435, RETURN pp435–437 e SORT
pp452–459. Nenhum endpoint procedural é programa externo. USING/GIVING conservam
todos os FD e seu modo implícito, inclusive USE em erro; SD nunca ganha nome externo.
RELEASE FROM transfere antes, RETURN INTO somente em sucesso. SORT de tabela
preserva referência DATA e não publica FILE. EXIT simples/CONTINUE têm prova NO_OP
(sem memória/ambiente); EXIT PROGRAM não recebe essa prova (EXIT p343).

FileSortControl deriva ranges da AST e do resolver, com gaps para endpoints,
kind e restrições MERGE não provados. Custo O(AST + símbolos + usos + relações
publicadas dos ranges), sem corte de ocorrências. O projector apenas transporta.
Retorno contextual compartilhado é uma redução conservadora documentada no lower;
esta publicação não afirma efeitos transitivos de procedimentos como exatos.
Oráculos FileSortContractTest cobrem 13 regras/negativos, incluindo mesmo FD em
ambos os papéis, múltiplas chaves, USE implícito e PERFORM. Focal família156 +
W5 final12 PASS; FAST336 PASS; qualification-local910 (um skip histórico previsto),
normalizer/E2E/naming PASS. Logs `.harness-results/fd-w5/`; fronteira nos pins
finais será registrada pelo consumer.


A conferência da coorte W5 corrigiu um positivo de OUTPUT PROCEDURE vazio: RETURN
p436 exige ao menos um RETURN nesse procedimento. FileSortControl agora publica
FILE_OUTPUT_PROCEDURE_EMPTY para range de saída provadamente vazio; range de
entrada vazio continua conhecido. A presença de endpoints não certifica restrições
transitivas via CALL/GO TO/PERFORM. Focal13 e FAST336 PASS nesta correção;
Q-SHARED910 anterior é REUSED para semântica compartilhada que não mudou.

## FD-W6 — auxiliares N-LR / SP2.27

Entrada W5 qualificada; oracle/regra em `../analysis-cfg/docs/product/file-dependencies/w6-implementation.md`.
N-LR SC27-8713-03, 2026-04-28: RERUN/SAME/MULTIPLE/APPLY pp154–158,
RESERVE/PADDING/DELIMITER/PASSWORD pp146–152, FD auxiliares pp181–192;
LINAGE-COUNTER pp23–24. `fileInventory@1.6` conserva inventário auxiliar tipado:
kind/effect, binding de arquivo/dados, parâmetros por papel, checkpoint/trigger,
provenance/gaps. Metadata documental não cria statements nem escreve dados.

Premissa LANGUAGE_GUARANTEED; classificação/extrator canônico antecedem projection.
SAME AREA de INDEXED/RELATIVE permite união das bases pelo algoritmo existente;
SEQUENTIAL usa o campo AS- normativo para VSAM e S-/ausência para QSAM;
SAME AREA QSAM é documental. Misturas não fechadas mantêm gap. SORT variants são documentais.
PASSWORD no OPEN, LINAGE no OPEN OUTPUT/EXTEND e WRITE, comprimento DEPENDING
na saída têm leituras conservadas; nenhum deles sobrescreve parâmetros.
Contador LINAGE permanece valor não provado pelo motor geral, sem singleton antigo.
RERUN sem EVERY preserva checkpoint sem SELECT e trigger SORT_MERGE. A forma
END_VOLUME com primeiro nome também nominalmente FILE mantém target-form não
provada, sem inventar assignment-name ou promover a OUT_OF_PROFILE. Outras
restrições observáveis (recursive/contained/collision/profile) são gaps locais.

Algoritmo: traversal/índices por unidade/identidade, união de componentes existente;
sem lookup externo, parser downstream ou cutoff. Cada cláusula cresce com seus
operandos; nomes globais de ASSIGN são indexados para a restrição de checkpoint.
Oráculos FileAuxiliaryContractTest e FileStorageLayoutTest precedem produção;
REDs/focais em `.harness-results/fd-w6`. Lower valida porta memory/wire coordenada.

Checkpoint produtor W6 final: focal230, FAST336 e Q-SHARED929 PASS (um skip histórico
previsto); manifesto APPLY corrigido após primeira Q; refinamento N05 incluído na terceira Q. B-SP/E-SELECTED nos pins
finais ainda em execução downstream; não são antecipados neste checkpoint.

Refinamento N05 (mesma W6): LINE SEQUENTIAL IBM é enum core e preserva assignment-name.
LR pp138/141–143/146–147; não habilita filenames/DYNAMIC do perfil D. Método de
acesso source-level por referência auxiliar (QSAM/VSAM/LINE_SEQUENTIAL/UNKNOWN)
sustenta classificação SAME bilateral; não é bindingMechanism ou allocation.
Oracle original excessivamente aberto corrigido pela autoridade antes do delta.

## FD-W8 — entrada CICS File Control

Autoridade, decisões e oráculos em `../analysis-cfg/docs/product/file-dependencies/w8-implementation.md`.
C-FC5.6: dez comandos API e INQUIRE/SET FILE; FILE computado usa W7.
Reutilizar parser embedded/host binding, sem misturar Program Control ou inventar
MUST/footprints. Contrato SP/decoder será fechado bilateralmente antes de emissão.

### W8 — scanner e papéis C-FC (em implementação)

Regra: API CICS TS5.6 pp351–377 (READ/browse),419(RESETBR),436–437(REWRITE),
542–545(STARTBR),569–570(UNLOCK),674–677(WRITE),115–118(DELETE),149(ENDBR);
SPI5.6 pp19–21/359–368(INQUIRE),672–674(SET/aliases). PDFs/hashes no harness CFG
`w8-implementation.md`. FILE é entrada exceto INQUIRE NEXT (saída), START/END
(sem nome individual). DATASET/OBJECTNAME são aliases comprovados de SET FILE.
TOKEN é saída em READ/READNEXT/READPREV e entrada em REWRITE/DELETE/UNLOCK.
LENGTH é entrada+saída nos reads; RIDFLD é entrada+saída em browse e READ
(GENERIC/NOTFND pode limpar), saída em WRITE RBA/XRBA, entrada nas demais formas.

Algoritmo: scanner linear compartilhado para payload CICS, aspas duplicadas e
parênteses; classificadores canônicos separados para Program e File Control.
Catálogo fechado por comando, direção tipada; unknown option retém ocorrência/gap.
Projector não reinterpreta texto. Host reference passa pela gramática COBOL existente.
Sem solver, lookup ou estado externo. Tempo O(payload+opções), memória O(opções).
Oráculo independente `CicsFileControlTest`:12 comandos, negativos por comando,
papéis, aliases, browse output, preservação offsets e separação queues/journals.
RED inicial: classe ausente em testCompile, log `.harness-results/fd-w8/parser-red.log`.
Nenhuma qualificação W8 afirmada neste ponto; downstream/pins ainda W6/W7.

Checkpoint produtor W8: SP2.28 (`fileInventory1.6`/`storage1.8` inalterados), nove
oráculos FILE, Program Control10, multiplicidade1/2/5/40. FAST345 PASS; Q-SHARED938
com um skip histórico previsto e source-normalizer full PASS. 33SPs de fronteira
em fd-w8/sp-7; testes não derivam expected do consumer. Gaps por dimensão preservados.
O FAST revelou LENGTH OF descartado após alocar AST ID: contraprova mínima e correção
na bridge antes da alocação; corpusCOACTUPC voltou a passar sem enfraquecer integridade.
Qualification revelou seleção findFirst no projector: agora exige identidade única
como o contrato anterior de Program Control. Logs anteriores permanecem evidência.

## FD-W9 — compilação e capturas

CLI publica `cobol-semantic-compilation.json` (schema cobol-semantic-compilation,
contractVersion1.0.0) além dos arquivos SP2.28 unitários compatíveis. Envelope:
unitInventory de IDs estruturais; inventoryStatus COMPLETE ou INPUT_MISSING;
units com product SP, parent nullable, ownedData/globalData, dataCaptures
(localDataId/sourceUnit/sourceDataId) e fileCaptures(owner/id). Inventário de
unidades não afirma completude dos corpos/entradas PRIMARY_ONLY. COPY ausente
conserva unidades observadas e marca INPUT_MISSING. Capturas particionam DATA
local versus importado; referências FILE conservam owner original. EXTERNAL
não é GLOBAL nem autorização para fundir por nome.

Autoridade IBM6.4 SC27-8713-03 update2026-04-28: cap7pp63–66, FDpp184–185 e
DATAp197. FileScopeSemantics indexa apenas relações canônicas de símbolos/scopes;
resolver continua dono de qualificação/shadowing/ambiguidade. Análises de storage
e CICS são calculadas uma vez; projeções por unidade compartilham esse snapshot.
Memória FILE agora indexa declarações de todas as unidades antes dos usos, O(AST
+symbols+destinos). Projeção selecionada conserva limite O(unidades×snapshot)
do indexador existente; escala observada será qualificada em W11, sem SLA.

READ GLOBAL conserva bound nominal do registro capturado. Projeção unitária não
publica intervalo físico ancestral: efeitos importados permanecem MAY e gap
FILE_CAPTURE_PHYSICAL_VIEW_UNAVAILABLE; nenhum MUST é criado. Lower fecha alias
com o objeto original na composição. Recursos físicos/runtime continuam fora
do produto. Declarações COPY repetidas preservam identidade e includeChain.

Oráculos FileScopeContractTest: pai/filho/shadow, EXTERNAL, COPY/REPLACING,
qualificação/ambiguidade, input parcial e READ GLOBAL disjunto. RED de captura
DATA observada e READ com bound excessivo preservados em fd-w9. CICS contido já
passou sem delta produtivo nesse analyzer. Teste de ordem da composição foi
atualizado para a nova chamada pública, preservando validação antes de consumers.

Checkpoint produtor W9: 7 oráculos novos; focal90 (FILE memória/escopo e integridade)
e focal53 anterior (resolver/storage/input) PASS. FAST352 zero skips; qualification-local
945 testes, um skip histórico, mais normalizador/naming PASS. Logs/tentativas em
`.harness-results/fd-w9`. Integração lower/codec/consumer ainda em andamento; isto
não qualifica a wave inteira. O PRIMARY_ONLY unitário permanece explícito.

A fronteira B-SP encontrou colisão de AST localId em COPY repetido entre irmãos;
FileIoMemory e projection usam agora Key(unidade,nó) também nesses índices.
Novo negativo COPY e focal21 PASS. FAST final352 e qualification-local final945
(um skip histórico) + normalizador/naming PASS: fast-5/qualification-4. Tentativa
qualification-3 conflitou com um build focal no mesmo target; falhas brutas foram
preservadas e os gates finais repetidos sequencialmente. Somente a execução final
sustenta este checkpoint corrigido.

## FD-W11 — continuação FILE ao completar um parágrafo

Corpus CBSTM03A.CBL demonstrou duas recusas no contrato PERFORM: o fact OBSERVED
que representa o WRITE publicava ordinary como normalContinuation intrínseca.
A correção usa a relação normal canônica do AST; o plano fileInventory.control
continua publicando ordinary por outcome. Nenhum sucessor é reconstruído por
posição, texto ou nome; contrato SP2.28/compilation1.0 e capability inalterados.

Oracle reduzido: PERFORM WRITE-P THRU WRITE-END; WRITE REC FROM DEST ao fim de
WRITE-P, EXIT no parágrafo seguinte. Intrinsic não atravessa o parágrafo; ordinary
permanece explícita no plano FILE. RED antes da correção; focal32 PASS incluindo
FILE control/sort/scope. Lower tem positivo bilateral e negativo de aresta ordinary
indevidamente marcada intrinsic. Probe real CBSTM03A passa as quatro CLIs com
PARTIAL explícito. Logs fd-w11/paragraph-*. FAST/Q e pins finais no harness W11.

W11 remediação estabilizada: FAST352 PASS; qualification-local946 testes,
zero falhas e um skip histórico, normalizer/naming PASS. Evidência em
fd-w11/fast-2.log e qualification-2.log. Sem alteração de perfil/solver/efeitos.

## FD-W11 — naming gate portátil

CI FAST aaecf8c1 falhou em quatro casos do naming guard antes do Maven. A falha
foi reproduzida em PATH sem ripgrep: a process substitution Bash escondia a
ausência do comando e aceitava conteúdo proibido. O scan usa agora Python, já
requerido pelo harness, e os caminhos Git com as mesmas exclusões/exceção textual.
Symlinks não são seguidos, como no scan anterior. Dois oráculos herméticos novos
exercitam rejeição de conteúdo e aceitação da referência canônica sem ripgrep.
RED preservado; oito testes naming e FAST fixo352 PASS, zero skips (fast-3.log).
Nenhum src/pom mudou desde aaecf8c1: qualification-local946 e bundle W11 snapshot3
são REUSED por identidade do conteúdo produtivo. Esta correção não altera pins.

## FD-W11 C06 — READ DATASET autorizado individualmente

Decisão humana C06-HUMAN-20260917, canônica no harness CFG/c06-read-dataset.md:
READ DATASET é alias legado de READ FILE no perfil CICS TS5.6. A normalização
fica somente no classificador FILE do frontend; o scanner compartilhado continua
conservando nome/raw/offsets. SP2.28 publica name=DATASET/canonicalName=FILE,
mesmos operandos/efeitos/controle de READ. Sem mudança de schema ou perfil de
nome; nenhum DSNAME/lookup, nenhum alias de outro comando antecipado.
Três novos testes independentes: sintaxe literal/host, negativos de escopo e
malformação/duplicação, e produto/provenance junto com Program Control. RED
confirmado em12 testes (três falhas semânticas); restante da família preservado.

C06 produtor: F-CICS22/FAST355 PASS, zero skips. Duas fixtures SP novas preservam
DATASET/FILE e Program Control. Q compartilhada anterior REUSED: mudança restrita
a classificação/canonicalização READ; nenhum efeito/provider/codec/controle novo.
