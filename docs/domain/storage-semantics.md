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

SP **2.7.0** será o único writer corrente em W3. A família de fatos regionais
terá schema fechado e versão explícita; leitores SP 1.x e 2.0–2.6 permanecem
disponíveis. O lower valida closure, concordância das provas e bounds também na
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
