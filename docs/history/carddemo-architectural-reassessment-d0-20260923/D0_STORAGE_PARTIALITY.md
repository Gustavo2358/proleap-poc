# D0 — storage partiality (F4/F5/F6)

F4 é **MIXED**. F5 combina input autêntico ausente com prova excessivamente global. F6 tem causa de representação de seção demonstrada. Selecionar profile não resolve a arquitetura de partiality.

## F4: configuração e arquitetura são perguntas diferentes

O checkout tem contexto IBM mainframe, CICS/IMS/JCL e `scripts/compile_batch.jcl.template` invocando IGYCRCTL com **IGY.SIGYCOMP.V63**. Isso sustenta uma família de dialeto IBM, mas **não prova Enterprise 6.4 + codepage 1047 para os 73**, especialmente o ZIP `UniKix_CardDemo_runtime_v1`. O corpus também contém script GnuCOBOL `--std=ibm-strict`. UTF-8 dos arquivos do Git não identifica o encoding runtime dos campos.

Q1: **INSUFFICIENT_EVIDENCE para seleção automática desse profile exato como verdade do corpus inteiro**. É legítimo executar um cenário explícito de análise IBM 6.4/1047, com premissa registrada e digest de configuração, como o probe fez. Para promovê-lo a configuração normativa, obter configurações/listings autênticos por variante ou aprovação explícita da premissa. Não excluir variantes UniKix silenciosamente.

Q2: sem perfil físico não se pode inventar offsets, bytes, tamanhos dependentes de encoding, SYNC ou aliases independentes. Porém nominal identity, boundaries de controle e alguns logical views não dependem dessa escolha. Hoje `StorageLayoutSemantics` adiciona PROFILE_NOT_SELECTED ao conjunto global de reasons; o predicado `environment` deixa extents/bases sem prova; depois a admissão global do lower recusa até valores textuais locais para os quais não possui uma representação regional admissível.

| Programa F4 | Declarações que expõem a barreira | Probe IBM explícito | Classificação |
| --- | --- | --- | --- |
| app/app-authorization-ims-db2-mq/cbl/DBUNLDGS.CBL | WS-PGMNAME, WS-ERR-FLG, WS-END-OF-AUTHDB-FLAG, WS-MORE-AUTHS-FLAG, WS-END-OF-ROOT-SEG, WS-END-OF-CHILD-SEG, FUNC-GU, FUNC-GHU, FUNC-GN, FUNC-GHN, FUNC-GNP, FUNC-GHNP, FUNC-REPL, FUNC-ISRT, FUNC-DLET | dependency PARTIAL | F configuração física + D/A representação lógica limitada |
| app/app-authorization-ims-db2-mq/cbl/PAUDBLOD.CBL | WS-PGMNAME, WS-ERR-FLG, WS-END-OF-AUTHDB-FLAG, WS-MORE-AUTHS-FLAG, END-ROOT-SEG-FILE, END-CHILD-SEG-FILE, QUAL-SSA-SEG-NAME, QUAL-SSA-KEY-FIELD, QUAL-SSA-REL-OPER, FUNC-GU, FUNC-GHU, FUNC-GN, FUNC-GHN, FUNC-GNP, FUNC-GHNP, FUNC-REPL, FUNC-ISRT, FUNC-DLET | dependency PARTIAL | F configuração física + D/A representação lógica limitada |
| app/app-authorization-ims-db2-mq/cbl/PAUDBUNL.CBL | WS-PGMNAME, WS-ERR-FLG, WS-END-OF-AUTHDB-FLAG, WS-MORE-AUTHS-FLAG, WS-END-OF-ROOT-SEG, WS-END-OF-CHILD-SEG, FUNC-GU, FUNC-GHU, FUNC-GN, FUNC-GHN, FUNC-GNP, FUNC-GHNP, FUNC-REPL, FUNC-ISRT, FUNC-DLET | dependency PARTIAL | F configuração física + D/A representação lógica limitada |
| app/cbl/CBACT04C.cbl | FD-DIS-ACCT-GROUP-ID, TRAN-TYPE-CD, TRAN-SOURCE, DB2-REST, WS-FIRST-TIME | OUTPUT_INVALID / 30 I-02 (F7) | F configuração física + D/A representação lógica limitada |
| app/cbl/CSUTLDTC.cbl | WS-RESULT | dependency PARTIAL | F configuração física + D/A representação lógica limitada |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/CBACT04C.cbl | FD-DIS-ACCT-GROUP-ID, TRAN-TYPE-CD, TRAN-SOURCE, DB2-REST, WS-FIRST-TIME | OUTPUT_INVALID / 30 I-02 (F7) | F configuração física + D/A representação lógica limitada |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/CSUTLDTC.cbl | WS-RESULT | dependency PARTIAL | F configuração física + D/A representação lógica limitada |

Os cinco PARTIAL adicionais são DBUNLDGS, PAUDBLOD, PAUDBUNL e duas CSUTLDTC. Não são cinco suportes completos, nem um novo full 9/73 canônico. O profile-input-probe mantém COADM01C bloqueado; o profile-probe mantém COPAUS2C bloqueado. [Medições comparadas](probes/profile-probe-analysis.json).

## F5: 12 unidades, não 12 provas de impossibilidade

São seis fontes checkout e seis `.cl2` do ZIP. Todas têm COPY DFHAID e DFHBMSCA não resolvidos. A localização dos COPYs, cada declaration rejeitada, referências resolvidas/não resolvidas e sites observados estão em [blast radius](probes/storage-blast-radius.json) e [resolução integral](probes/f5-resolution.json).

| Programa físico | Bindings resolvidos | Não resolvidos | Nomes sem binding | Declarações locais rejeitadas |
| --- | --- | --- | --- | --- |
| app/cbl/COADM01C.cbl | 156 | 8 | DFHENTER, DFHGREEN, DFHPF3, EIBAID, EIBCALEN | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| app/cbl/COMEN01C.cbl | 180 | 11 | DFHENTER, DFHGREEN, DFHPF3, DFHRED, DFHRESP(NORMAL), EIBAID, EIBCALEN, EIBRESP, NORMAL | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| app/cbl/COSGN00C.cbl | 122 | 5 | DFHENTER, DFHPF3, EIBAID, EIBCALEN | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| app/cbl/COUSR01C.cbl | 171 | 14 | DFHENTER, DFHGREEN, DFHPF3, DFHPF4, DFHRESP(DUPKEY), DFHRESP(DUPREC), DFHRESP(NORMAL), DUPKEY, DUPREC, EIBAID, EIBCALEN, NORMAL | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| app/cbl/COUSR02C.cbl | 263 | 20 | DFHENTER, DFHGREEN, DFHNEUTR, DFHPF12, DFHPF3, DFHPF4, DFHPF5, DFHRED, DFHRESP(NORMAL), DFHRESP(NOTFND), EIBAID, EIBCALEN, NORMAL, NOTFND | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| app/cbl/COUSR03C.cbl | 194 | 19 | DFHENTER, DFHGREEN, DFHNEUTR, DFHPF12, DFHPF3, DFHPF4, DFHPF5, DFHRESP(NORMAL), DFHRESP(NOTFND), EIBAID, EIBCALEN, NORMAL, NOTFND | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COADM01C.cl2 | 151 | 7 | DFHENTER, DFHGREEN, DFHPF3, EIBAID, EIBCALEN | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COMEN01C.cl2 | 164 | 7 | DFHENTER, DFHGREEN, DFHPF3, EIBAID, EIBCALEN | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COSGN00C.cl2 | 122 | 5 | DFHENTER, DFHPF3, EIBAID, EIBCALEN | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COUSR01C.cl2 | 171 | 14 | DFHENTER, DFHGREEN, DFHPF3, DFHPF4, DFHRESP(DUPKEY), DFHRESP(DUPREC), DFHRESP(NORMAL), DUPKEY, DUPREC, EIBAID, EIBCALEN, NORMAL | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COUSR02C.cl2 | 263 | 20 | DFHENTER, DFHGREEN, DFHNEUTR, DFHPF12, DFHPF3, DFHPF4, DFHPF5, DFHRED, DFHRESP(NORMAL), DFHRESP(NOTFND), EIBAID, EIBCALEN, NORMAL, NOTFND | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COUSR03C.cl2 | 194 | 19 | DFHENTER, DFHGREEN, DFHNEUTR, DFHPF12, DFHPF3, DFHPF4, DFHPF5, DFHRESP(NORMAL), DFHRESP(NOTFND), EIBAID, EIBCALEN, NORMAL, NOTFND | WS-PGMNAME, WS-TRANID, WS-MESSAGE, WS-USRSEC-FILE, WS-ERR-FLG |

Não atribuir EIBCALEN/EIBAID/EIBRESP ou DFHRESP automaticamente aos dois COPYs: há também ambiente/tradução CICS, e o conteúdo autêntico ausente não foi fornecido. A atribuição exata declaration→COPY não pode ser provada a partir do nome. DFHENTER/PF* e constantes de atributo DFH* são consumidores nominais concretos a validar quando as bibliotecas autênticas estiverem disponíveis. Nenhum stub foi fornecido.

Fatos independentes observáveis: WS-PGMNAME/WS-TRANID e literais estão no próprio fonte antes dos COPYs; existem identidades e bindings locais; nomes de COPY e nomes escritos de recursos CICS podem ser preservados. Em COADM01C, WS-PGMNAME linha 36, WS-TRANID 37, XCTL computados linhas 146 e 169; MOVE WS-PGMNAME→CDEMO-FROM-PROGRAM linha 143 possui operandos declarados. Os valores DFH* em condições/cores não são premissa lexical desses nomes/targets escritos.

Isso **não prova uma célula física independente**: COPY textual arbitrário poderia mudar estrutura, colisões de nomes ou relações de storage. O fato que sobrevive deve declarar suas premissas: trecho fechado, namespace, boundary de seção e ausência de relação potencial relevante. A proposta é conservar conhecimento nominal/controle local e localizar incerteza; não assumir que todo COPY ausente contém só constantes, nem que um gap antes de outro trecho não o afete.

Hoje a existência de sourceText com base não provada e sem logical/scalar alternativa admissível gera `BLOCKED_LOWERING` para toda a unidade (`PartialProgramAdmission:96–106`). O lower não calcula que apenas DFHENTER está sem prova; bloqueia WS-PGMNAME, WS-TRANID etc. A separação fact-level é necessária; apenas adicionar copy path/profile não corrige esse desenho.

## F6: primeiro fato causal em COPAUS2C

Nas linhas 65–70, `EXEC SQL INCLUDE SQLCA` e `EXEC SQL INCLUDE AUTHFRDS` são preservados como **Ast.DataEntry com level="SQL", DataLevelKind.OPAQUE**. Não viram declarações COBOL expandidas. Em `StorageComponents.analyze:75–87`, cada filho Working-Storage deve ser DataEntry raiz nível 1/77; `level(SQL)` retorna **−1** (`:179`). Assim `structureProven=false` já na coleta de roots. Esse é o primeiro fato estrutural ausente.

`StorageLayoutSemantics:73` converte isso em SECTION_NOT_PROVEN global; extents/allocation se perdem; lower recusa data:1 WS-PGMNAME e data:13 WS-ERR-FLG. `sourceDependencies` registra SQLCA/AUTHFRDS UNRESOLVED, mas esse status nominal **não é a condição que produz structureProven=false**. É a representação opaca dos includes na seção. Mesmo com profile IBM, o bloqueio permanece. [Probe da prova de seção](probes/ast-control/COPAUS2C-storage.json).

Portanto os includes são causalmente relevantes pela falta de expansão/tipagem de declarations; não são mera coincidência. Entretanto “achar SQLCA no copy path” não está demonstrado como suficiente: o caminho EXEC SQL INCLUDE e a integração com declarations devem produzir uma estrutura tipada. O índice nominal de dependência não substitui esse caminho.

Classificação: **producer/storage model gap + global proof problem**; input autêntico é necessário para provar os campos incluídos, mas a perda de toda a seção é amplificação arquitetural. Preservar unknown region para o include e provas locais dos roots conhecidos depende de provar isolamento, não de apagar os nós SQL. Não houve alteração no fonte ou SP para testar um falso PASS.
