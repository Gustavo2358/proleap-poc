# Storage Semantics — perfil fixo ST-W0–ST-W5

Campanha `ST-20260913-01`, autorizada pelo usuário em 2026-09-13. Este documento
fecha o contrato de implementação; W0 não afirma que o produtor já o emite.
O registro local é WORK-STORAGE-FRONTEND-001, promoção dos fatos fonte de
BACKLOG-SP-008/BACKLOG-DF-001 e do contexto de BACKLOG-EXT-002. RD/values
continuam nos consumidores. Review humano ocorre após W5; nenhum merge autorizado.

## Autoridade e ambiente

As regras fonte vêm do Enterprise COBOL for z/OS 6.4:
[REDEFINES](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-redefines-clause),
[MOVE de grupo](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=items-assigning-values-group-data-move)
e [PICTURE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=clause-data-categories-picture-rules),
verificados em 2026-09-13. REDEFINES compartilha a área; grupo MOVE transfere a
representação sem interpretar separadamente seus filhos. Para alocações locais,
uma redefinição pode ser maior. A extensão do componente é o máximo dos seus
descritivos, nunca a soma. A implementação só concede essa prova após verificar
nível, parent, encadeamento e ausência de declaração interposta que aloque bytes.

Perfil `ibm-enterprise-6.4-fixed-display-1047@1`: WORKING-STORAGE de programa
ordinário; grupos fixos aninhados; folhas PIC X ou repetição positiva de X;
USAGE DISPLAY explícito ou implícito; FILLER físico; REDEFINES local de tamanho
conhecido, inclusive menor/maior e encadeado. A seleção de perfil é um fato
explícito da invocação/configuração do produtor. Não é deduzida do corpus,
da extensão do arquivo, do sistema operacional nem do charset Java.

Fonte e JSON usam suas codificações declaradas de transporte. Texto lógico
Unicode continua distinto de bytes de execução. A code page runtime escolhida
é **IBM1047**, com correspondência Unicode conforme a
[tabela IBM registrada na IANA, versão 1.00](https://data.iana.org/archive/ietf-charsets/msg01226.html).
O nome AIR proposto é `text.ebcdic.ibm1047@1`, com domínio TEXT e um octeto por
caractere representável. W1 registra e qualifica essa extensão em analysis-ir e
air-java. Exemplos independentes: espaço = `40`, `ABC` = `c1c2c3`,
`PGM00001` = `d7c7d4f0f0f0f0f1`. Nenhuma substituição por `?` é permitida.
ASCII é um perfil separado de engenharia AIR-only, `text.ascii@1`; seu sucesso
não certifica execução IBM1047. Sem ambiente, ou com metadados conflitantes,
publicar contexto desconhecido/diagnóstico e negar efeitos regionais precisos.

## Regras de layout e efeito

| Regra | Prova e resultado | Contraprova obrigatória |
| --- | --- | --- |
| Grupo fixo | offsets acumulados pela ordem estrutural; grupos incluem seus filhos | inventário SP permutado não altera layout |
| FILLER | nó físico próprio, sem exigir símbolo nominal; consome extensão provada | SQL opaco marcado filler não vira PIC X |
| Filho desconhecido | extensão desconhecida e motivo; offsets posteriores dependentes ficam desconhecidos | unknown nunca é zero |
| REDEFINES | mesma base e mesmo início do item redefinido; views com tipos/codecs próprios | não alocar nova Cell nem somar novamente seu tamanho |
| Overlays aninhados | footprint de cada conjunto = máximo das alternativas provadas | campo seguinte respeita a maior alternativa |
| Independência | roots ordinários locais e componentes bem formados sustentam disjunção rastreável | nomes/IDs diferentes não provam separação |
| MOVE literal | bytes codificados sob perfil declarado, extensão exata, destino exato | não ajustar/truncar silenciosamente |
| MOVE DATA→DATA | captura dos bytes anteriores, extensões iguais e faixas comprovadamente disjuntas | overlap fonte COBOL conserva fallback |
| Operação não admitida | footprint obrigatório conhecido ou envelope conservador com motivo | nunca Nop nem desaparecimento do statement |

Semânticas já entregues de MOVE escalar/fitting continuam com o contrato legado.
O novo perfil não concede `wholeItemAccess` a filhos ou grupos; concede um acesso
regional distinto. Qualificação nominal usa os resultados canônicos do resolver.
Origins de copybooks são preservados, inclusive em nós anônimos.

OCCURS/ODO, RENAMES, VALUE/inicialização regional, reference modification,
MOVE CORRESPONDING, NATIONAL/DBCS/UTF-8, uso numérico/COMP, SIGN/SYNC/JUSTIFIED,
GLOBAL/EXTERNAL, LINKAGE/LOCAL-STORAGE e programas INITIAL/RECURSIVE ficam fora
da prova precisa. Cláusula preservada/sem interpretação não é inocente. Uma raiz
desconhecida só permite precisão em outra raiz quando a prova de alocação separada
é independente da cláusula desconhecida. Caso contrário, o efeito fica aberto.

## Produto canônico e SP 2.7

Um cálculo pós-binding produz fatos imutáveis. Projectors apenas os transportam:

- Contexto: perfil/versão, runtime codec, autoridade, origem e lacunas.
- Nó físico: ID próprio, símbolo DATA opcional, parent/order estrutural, espécie
  grupo/folha/filler/opaco, origem e extent conhecido ou desconhecido com motivo.
- Base: ID físico, duração, visibilidade, extent e prova de alocação.
- Vista: nó/objeto, base, offset, extent, domínio, codec e prova; offset/extent
  desconhecido têm representação explícita e nunca são inferidos pelo lower.
- Acesso: ocorrência de referência, vista/intervalo, read/write e origem.
- Efeito MOVE: statement, fonte literal/cópia capturada, destinos, footprint,
  MUST/MAY, regra de ajuste explicitamente admitida e lacunas.
- Separação: conjuntos de bases disjuntas com autoridade e provenance.

SP **2.7.0** é o único writer corrente. A família de fatos regionais tem
schema fechado e versão explícita; a integração do reader 2.7 no lower preserva
os leitores SP 1.x e 2.0–2.6. O lower valida closure, concordância das provas e bounds também na
porta em memória. Nenhum campo físico é reconstruído de `picture`, nomes ou
ordem de inventário. GEN/KILL e resultados de análise não pertencem ao SP.

Exemplo canônico: AREA tem dois filhos X(4), base R extent 8, views em [0,4) e
[4,8). `MOVE 'ABCDEFGH' TO AREA` escreve `c1c2c3c4c5c6c7c8` em R[0,8).
Ler o segundo filho decodifica EFGH sob IBM1047. Um overlay sobre AREA reutiliza
R; não muda a alocação por possuir outro nome. Sem o perfil explícito, esses
mesmos nomes não autorizam codificação física precisa.

## Algoritmo e qualificação

Indexar declarações/refs uma vez e visitar a estrutura em pós-ordem, com pilha
explícita. Calcular componentes de REDEFINES e footprints antes de offsets; a
prova não depende da ordem de consulta. A terminação segue da estrutura finita
e das relações verificadas; ciclos inválidos conservam diagnóstico. Custo
pretendido O(n + e), além da ordenação canônica de saída, sem pares de nomes.

W0 exige oracle byte-exato independente e mutantes; W3 exige goldens de layout,
closure SP/lower, grupos 1/2/5/N, filler/opacos/qualifiers, E2E group→child→CALL;
W4 acrescenta compartilhamento bidirecional, overlays tardios e disjunção local.
W5 qualifica os valores compostos downstream. Cada wave mantém FAST e regressões
anteriores. Escopo de suporte só muda após as provas da respectiva wave.

## Implementação W3 em andamento

`StorageLayoutSemantics` prepara o layout canônico antes do projector. A entrada
contém uma seleção explícita de Profile; ausência não autoriza o perfil IBM1047.
Três visitas por declaração calculam estrutura, extents em pós-ordem e offsets
em pré-ordem. FILLER possui identidade mesmo sem símbolo. Os números usam
BigInteger, e um prefixo desconhecido propaga UNKNOWN_OFFSET aos sucessores.
Só níveis 01/77 podem iniciar alocação ordinária; níveis internos seguem a AST.
W3 ainda mantém a recusa conservadora dos overlays, a ser substituída em W4.

A prova exige preprocessing completo e cobertura semântica MODELED para a
declaração e suas cláusulas tipadas admitidas. `SourceProvenance.exact` descreve
fidelidade de mapeamento do texto original, não certeza do layout da AST expandida:
COPY, intervalos atravessando arquivos e REPLACING podem alterar esse indicador.
O cálculo preserva integralmente origem, includeChain e exact; não fabrica um
mapeamento exato para publicar layout conhecido. A autoridade semântica é a AST
produzida pelo preprocessing concluído. Cláusulas não interpretadas e input ausente
continuam negando prova. O significado de wholeItemAccess escalar não muda.

`StorageLayoutTest` possui oráculos de grupo aninhado, FILLER, PIC desconhecido,
ambiente ausente, cláusulas excluídas, níveis inválidos, programas não ordinários,
COPY aninhado/REPLACING, DISPLAY herdado e multiplicidade 1/2/5/40/256. O teste de
origem compara o fato publicado ao canônico, inclusive quando exact é false.
O primeiro teste de COPY assumia incorretamente exatidão do mapeamento; o contrato
de provenance existente mostrou a distinção, registrada nas tentativas W3.
Ainda não há afirmação de SP 2.7 ou M1 apenas por estes testes de layout.

`StorageAccessSemantics` recebe esse snapshot e o mesmo binding nominal, com
validação de ownership por identidade. As chaves publicadas continuam compostas;
IDs iguais em outro snapshot não permitem reutilizar as provas. Referências
integrais resolvidas admitem qualificadores, sem fabricar wholeItemAccess legado.
São publicados ponto/ocorrência, objeto nominal, base/view, role e origem.
CALL precisa de uma folha textual dentro do perfil; a resolução não calcula
nome de programa nem estado de runtime.

MOVE exato publica LITERAL_BYTES ou COPY_BYTES; este último exige faixas disjuntas
provadas e comprimentos iguais. Destino obrigatório conhecido com fonte fora do
perfil produz MUST_UNKNOWN, e destino não provado permanece UNAVAILABLE. Não há
fitting/truncamento implícito. Provas escalares de fitting já existentes continuam
disponíveis separadamente para a tradução compatível. O encoder usa a tabela
IBM/IANA fixada, rejeita texto não representável e não consulta charset da JVM.
O teste compara os 256 mapeamentos ao arquivo derivado da fonte IANA de W1,
incluindo a distinção LF/NEL. Captura aqui significa um fato de leitura anterior
à escrita; valores de runtime e efeitos posteriores continuam no consumidor AIR.


## Fronteira SP 2.7 implementada em W3

A CLI aceita `--storage-profile ibm-enterprise-6.4-fixed-display-1047@1`.
O default `unspecified` não concede bytes, extents conhecidos, codecs ou
independência física; outro identificador é erro de configuração. A composition
root executa os dois produtos canônicos e o projector recebe o snapshot pronto.
O ownership é verificado também no pacote de entrada do projector.

`CobolSemanticPort.storage()` expõe StorageInventory. No JSON, `storage` possui
`version=1.0.0`, `profile`, `profileId`, `runtimeCodec`, `nodes`, `bases`, `views`
e `gapCodes`. O identificador versionado seleciona a autoridade IBM/IANA já
fixada neste contrato; a seleção de ambiente provém da invocação, não de um
intervalo fictício de source. `storage-node:<id>` e `storage-base:<id>` têm
namespaces diferentes dos handles DATA. Um nó inclui parent/order/filler/kind,
DATA opcional, extent e provenance. Base inclui extent, allocation e provenance;
view inclui node/base/offset/extent/codec/provenance. Offset e extent têm
`value` decimal não negativo como **string** ou null acompanhado de `gapCodes`.

DataReference acrescenta `regionalAccess: {view}` ou null. A ocorrência,
role, binding e origem continuam nos mesmos campos do operand. MoveFact
acrescenta `regionalMove: {kind, bytes, gapCodes}` ou null; bytes são octetos
inteiros 0–255, exclusivamente para LITERAL_BYTES. COPY_BYTES captura a fonte
antes de escrever; MUST_UNKNOWN conserva o footprint obrigatório; UNAVAILABLE
não autoriza uma escrita precisa. Os construtores anteriores continuam
publicando ausência explícita desses fatos. WholeItemAccess e fitting legado
mantêm sua semântica. Referências de IF/EVALUATE/PERFORM/observed também podem
transportar acesso regional quando o produto canônico o conhece; isso sozinho
não promove predicate, controle nem efeitos da construção.

State verifica índices fechados, pais acíclicos, ordem entre irmãos única,
base explicitamente referenciada, view por nó, bounds, concordância nominal dos acessos,
codec/ambiente explícitos, extensão de literal e disjunção de cópia. A validação
é iterativa O(n + referências), sem reconstruir PICTURE nem recalcular offsets.
A correspondência textual completa do payload de octetos é provada no encoder
canônico contra os 256 mapeamentos IANA; o lower deve validá-la independentemente
pelo codec AIR ao receber uma publicação não confiável.

`StorageProductTest` verifica o port fechado, JSON determinístico, namespace,
FILLER, qualificações, cópias 1/2/5/40, lacunas, permutação de inventário físico,
closure inválida, bounds, seleção nominal, payload de extensão contraditória,
cópia sobreposta/sem separação e snapshot estrangeiro. O FAST passou com 168
métodos. As comparações históricas preservam os arquivos brutos e todos os fatos
anteriores, descontando somente os campos aditivos provados ausentes e a versão.
A qualificação M1 depende ainda do decoder/lowering e de values/dependências.

A identidade da base é opaca: renomeá-la coerentemente nas views não altera o
layout. Closure exige que toda base seja referenciada; não compara seu número
local ao número de um nó físico. Um oracle de renomeação detectou e eliminou essa
restrição indevida do primeiro validador SP 2.7, sem alterar o writer.

Após a correção de identidade opaca da base, FAST: 169 métodos, zero skips.
A execução Maven completa anterior: 659 descobertos, zero falhas, um skip
herdado em SemanticConditionContextDiscoveryTest por configuração; não é contado
como teste executado. Os produtos CLI SP 2.7 foram capturados separadamente.
