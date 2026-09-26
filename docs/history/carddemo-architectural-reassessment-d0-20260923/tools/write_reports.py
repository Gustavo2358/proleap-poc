from pathlib import Path
import json,csv,collections
D=Path(__file__).resolve().parents[1];P=D/'probes';R=D.parents[1];E=R/'artefatos-e2e/carddemo-validation-20260923';BASE=R/'.positive-memory-topology';IBM='https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf'
def put(name,text): (D/name).write_text(text.strip()+'\n')
def table(headers,rows):return '| '+' | '.join(headers)+' |\n| '+' | '.join('---' for _ in headers)+' |\n'+'\n'.join('| '+' | '.join(str(v).replace('|',' / ').replace('\n',' ') for v in row)+' |' for row in rows)+'\n'
def link(label,path):return f'[{label}]({path})'
b=json.loads((P/'baseline/initial.json').read_text());programs=json.loads((P/'f2-programs.json').read_text());details=json.loads((P/'f2-details.json').read_text());ws=json.loads((P/'f2-witnesses.json').read_text());clusters=json.loads((P/'f2-clusters.json').read_text());blast=json.loads((P/'storage-blast-radius.json').read_text());ms=json.loads((E/'full/measurements.json').read_text())['programs'];by={x['path']:x for x in ms}
put('D0_BASELINE.md',f'''# D0 — baseline e preservação

Estado: W5 CLOSED; W6/R1 e W7/R1 CLOSED **sob corpus qualificado**; W8 PAUSED; W9 NOT STARTED. D0 é investigação, sem implementação, commit, mudança de branch ou operação de PR.

## APPROVED_W7_R1_BASELINE versus CURRENT_W8_WORKTREE_STATE

{table(['Repositório','W7-R1 aprovado','HEAD W8 observado'],[(r,b['approvedW7R1'][r],v['head']) for r,v in b['currentW8'].items()])}
Os cinco worktrees estavam limpos, na branch `feat/positive-memory-topology`. A captura anterior aos probes, status e diffs integrais estão em [baseline/initial.json](probes/baseline/initial.json) e arquivos adjacentes. A verificação posterior está em `probes/baseline/final-preservation.json`. A raiz agregadora não foi usada como repositório Git.

O resultado canônico **4/73** usa CURRENT_W8_WORKTREE_STATE, não o pin aprovado. W8 contém mudanças parciais de dependências nominais no frontend, transfers/effects no lower e extração no CFG. O diff capturado permite revisar exatamente esse delta. Nenhuma dessas mudanças foi apagada.

## Corpus e evidência reutilizada

CardDemo `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`: 44 fontes checkout + 29 membros ZIP, incluindo `.cl2`, total físico **73**. Sem whitelist, deduplicação ou exclusões. Os hashes dos 73 fontes foram conferidos individualmente contra as medições. Antes da análise, os **4.787** arquivos do manifesto canônico foram verificados, sem divergência: [verificação](probes/baseline/evidence-hash-validation.json).

Foram lidos o report, classification CSV, summary, commands, as medições integrais e os produtos SP/AST dos 71 fontes que os publicam. Também foram lidos os três conjuntos de probes de profile e seus resultados. [Projeção verificável](probes/all-programs-analysis.json), [probes de profile](probes/profile-probe-analysis.json).

O full canônico durou 240,895 s, sem timeout/falha de runner. Comandos usam o executável absoluto Java 21; a string de Java 25 do ambiente default não identifica a JVM realmente invocada. Não houve ajuste de heap/timeout como solução semântica em D0.

## Experimentos novos

1. Inventário externo dos 46 F2: 1.512 sites PERFORM, 1.964 pares site/parágrafo. Replica dois predicados de rejeição para localizar diagnósticos, **não constitui oracle semântico**.
2. Probe AST canônico sobre 14 programas F2, CBPAUP0C e COPAUS2C: preserva fontes e mede separadamente relações local/ordinária.
3. Probe F7 usa decoder, admission, montagem e validator reais, sem relaxar regras. Aumenta apenas a retenção de diagnósticos para 30.000, como o probe canônico, permitindo ver os 30 I-02. Fragmento inválido não é publicado como AIR aceita.
4. Amostra histórica estratificada de dez checkout sources × seis runtimes, 60 execuções. As medições canônicas W8 completam a comparação. [Builds e pins](probes/history/builds.json), [medições](probes/history/measurements.json). Sem checkout, rebuild ou alteração dos produtos.

A amostra histórica não certifica todos os 73 em cada wave. Os runtimes são snapshots congelados; hashes atuais e manifests históricos são registrados. Quaisquer discrepâncias de evidência histórica são explícitas em [verificação](probes/historical-evidence-hashes.json). A execução grava apenas o diretório D0. Não foram repetidos FAST/full de produto: não há alteração de produto a qualificar; os experimentos respondem a perguntas causais distintas.
''')
put('D0_FAILURE_TAXONOMY.md','''# D0 — taxonomia causal

**73 descobertos; SP 71 PARTIAL + 2 BLOCKED; AIR 4 PARTIAL + 67 BLOCKED + 2 não alcançados; CFG/dependency 4 PARTIAL. 69/73 não chegam ao fim.**

| Família | N | Tipo causal | Primeira perda e interpretação |
| --- | ---: | --- | --- |
| F1 | 2 | F + robustez de normalização; G condicionado ao dialeto/import policy | HT sem política uniforme de colunas. Não é partiality de valores. |
| F2 | 46 | C; A/D contribuem em K4 | Completion de região e fallthrough ordinário colidem na interpretação de normalContinuation; prova global ainda apaga algumas frontiers. |
| F3 | 1 | C | Continuação de braço é ordinária, consumer compara apenas com continuação local do EVALUATE pai. |
| F4 | 7 | F + D/A: MIXED | Perfil físico ausente é configuração real; bloquear valores lógicos independentes revela granularidade insuficiente. |
| F5 | 12 | F + D, com A localizado | COPYs autênticos ausentes; inputComplete(unit) amplia a perda para fatos sem dependência demonstrada desses COPYs. |
| F6 | 1 | A/producer gap + D | EXEC SQL INCLUDE vira DataEntry opaco level SQL; structureProven é falso para a seção inteira. |
| F7 | 2 sobrepostos a F4 | E | Closure da ativação omite handlers FILE que o emissor referencia. 30 I-02 por variante. |

A/B/C/D/E/F/G seguem a tipologia do pedido. B é comportamento desejável e já existe em fatos parciais preservados; não é sinônimo de blocker. Nenhuma família é genericamente rebatizada UNSUPPORTED. F7 não aumenta 69.

42 programas têm 101 ocorrências sourceDependencies UNRESOLVED; esses gaps transversais não são novas famílias first-loss. Os quatro produtos finais não demonstram suporte completo. Cinco saídas adicionais PARTIAL dos probes F4 tampouco mudam o baseline canônico: usaram configuração diferente.

O inventário físico e as classificações por programa permanecem no [CSV canônico](../carddemo-validation-20260923/classification.csv). D0 não exclui qualquer caso. Não foi comprovada corrupção de ZIP ou invalidade COBOL que autorize exclusão; missing input e contexto UniKix exigem classificação/configuração explícita, não remoção tácita.
''')
# Detailed witness-by-witness expected graph: all edge values retained, manual exceptions for unavailable producer resume.
manual={
'CBACT01C':'EXIT é neutro. A fronteira pertence ao término de 0000-ACCTFILE-OPEN; entrar ordinariamente e executar sob PERFORM exige destinos diferentes. CALLs de erro só retomam se retornarem; não criar bypass.',
'CBACT02C':'Mesmo mecanismo com ADD no parágrafo seguinte: a aritmética desse ADD não é premissa para reconhecer o término do parágrafo chamado.',
'COACTUPC':'O THRU cruza fronteiras intermediárias por default; só a última fronteira devolve ao caller. DFHAID ausente impede provar valores de WHEN, mas não apaga a delimitação explícita END-EVALUATE. IF final tem frontier composta; STRING em um WHEN não entra no WHEN seguinte. O copybook CSUTLDPY fornece um range de 12 parágrafos, não uma única operação.',
'COCRDUPC':'O GO TO 1230-EDIT-NAME-EXIT é transferência explícita e deve ser preservado. O SET final chega ao mesmo EXIT por conclusão normal. Esses dois motivos não são intercambiáveis; no THRU a fronteira intermediária continua até o EXIT final.',
'CBEXPORT':'DISPLAY, STRING e ADD completam normalmente sem exigir seus valores exatos. PERFORM 133 está dentro de PERFORM UNTIL: seu retorno de linguagem é o PERFORM 2100-READ-CUSTOMER-RECORD da linha 251, apesar de o resume concreto estar UNAVAILABLE no SP. O laço testa novamente sua condição; não retorna para 3000-EXPORT-ACCOUNTS a cada ADD.',
'COACCT01':'No WHEN MQCC-OK, SET completa o EVALUATE e o parágrafo. O ADD no THEN final de 3000-GET-REQUEST também completa o parágrafo; a ausência dos COPYs MQ não transforma um branch em salto ao próximo WHEN. Há chamada aninhada de 4000 e repetição no programa.',
'COPAUS0C':'SET no ELSE final de PROCESS-PF7-KEY e CONTINUE no WHEN OTHER de POPULATE-AUTH-LIST completam regiões distintas. O caller 300 está no IF dentro de PERFORM UNTIL: após retorno, a linguagem executa COMPUTE WS-IDX = WS-IDX + 1 (linha 432), depois o restante do corpo/teste. O SP não publica esse resume.',
'COPAUS1C':'WHEN SEGMENT-NOT-FOUND / WHEN END-OF-DB compartilham um corpo; não são dois arms com queda entre eles. SET do WHEN STATUS-OK sai do EVALUATE terminal e retorna ao caller se esse parágrafo foi performed.',
'COBIL00C':'O CICS READ precede EVALUATE WS-RESP-CD. Mesmo com efeito externo parcial, o CONTINUE do WHEN NORMAL termina o braço; a sucessão estrutural não exige conhecer RESP runtime.',
'COTRTUPC':'SET no THEN do IF terminal e SET/nested IF do ELSE convergem no término do primeiro parágrafo do THRU, depois o EXIT final retorna ao caller. A condição composta não precisa de valor exato para preservar os dois outcomes.',
'COPAUA0C':'THRU 1000-INITIALIZE/1000-EXIT contém PERFORMs aninhados de MQ; o EXIT final devolve ao caller externo, não reinicia 1100. A fronteira do EVALUATE após CICS READ e STRING terminal de 6000 têm a mesma regra contextual, com efeitos parciais.',
'CBTRN02C':'CONTINUE do THEN do IF terminal completa 2700-A; no ELSE os PERFORMs de erro precisam completar antes da mesma fronteira. WRITE e seus resultados desconhecidos não autorizam eliminar o braço nem inventar retorno de ABEND.'}
manual.update({'CBTRN01C':'READ terminal tem INVALID KEY e NOT INVALID KEY. DISPLAY130 termina o handler de sucesso; a conclusão do READ125 já está publicada como frontier, mas a do filho não é composta pelo consumer. Em PERFORM107 o retorno de linguagem é IF WS-XREF-READ-STATUS = 0, linha 173, dentro do laço inline; SP resume UNAVAILABLE. Não há INPUT_MISSING: é limite de composição FILE.', 'CBTRN03C':'O IF externo termina por ponto na linha 243, enquanto apenas o IF interno tem END-IF. DISPLAY169 no THEN termina a região externa e o parágrafo. ProcedurePerformSemantics exige explicitlyTerminated para coletar essa frontier e a omite; não há INPUT_MISSING. O retorno de PERFORM143 é statement144; ordinary segue statement173.'})
f2='''# D0 — F2 deep dive

## Conclusão e teste da hipótese

Os **46 F2** são explicados por uma abstração de conclusão de região que não atravessa uniformemente o contrato SP/lower. Há duas manifestações: frontier publicada conflitando com continuação ordinária dentro do statement, e frontier omitida enquanto a mesma continuação ordinária ainda é publicada. Não são 46 constructs desconhecidos e não são 46 defeitos independentes.

A hipótese literal “o frontend só tem um successor” é **refutada**: `Ast.Division` já guarda `normalContinuations`, `ordinaryContinuations` e `normalCompletionStatements`. O problema real é que a publicação/consumo volta a exigir um único significado para `normalContinuation`, misturando saída da região com destino de execução. `SourceOccurrence × ExecutionContext → OutcomeSet` descreve melhor a regra necessária, mas a topologia fonte pode permanecer finita e compartilhar os nós: contexto parametriza a resolução das fronteiras, não a identidade da ocorrência.

Uma fronteira é condicional à conclusão normal. Ela não prova terminação, não sobrepõe GO TO/GOBACK e não cria um edge que pula CALL/FILE/CICS. A mesma fronteira tem default ordinário, conclusão de range ativo ou continuação interna do THRU. A ausência de um valor exato não implica ausência desses outcomes estruturais.

## Cobertura empírica dos 46

A [matriz exigida](D0_F2_CLUSTER_MATRIX.csv) tem 1.964 pares site/parágrafo de 1.512 sites PERFORM em todos os 46 programas. [Detalhes por site](probes/f2-details.json) preservam fonte, AST, membership, frontier, target e regra. Nenhum programa foi escolhido por whitelist para esse inventário. A amostra profunda contém **14 programas reais / 24 casos de fronteira**. São 356 sites de range/THRU e 1.156 single-paragraph; dois têm repetição out-of-line explícita. Repetição inline envolvendo esses callers é dimensão adicional, presente nos witnesses CBEXPORT/COPAUS0C/CBTRN01C.

Dois predicados reais de `ProcedurePerformAdmission` explicam 2.170 ocorrências de rejeição enumeradas (uma ocorrência pode repetir pelo caller). Deduplicando programa/statement/regra, são 1.645: 593 “completion cannot override” e 1.052 “intrinsic stays in paragraph”. Todas as ocorrências ofensivas são `OBSERVED`: EXIT 1.174; SET 707; CONTINUE 215; ADD 30; DISPLAY 26; STRING 18. Isso não significa ausência de MOVE/CALL/CICS nos bodies; significa que essas variantes não são os offenders desses dois predicados.

Em 41 programas, a contagem coincide exatamente com stderr. Nos outros cinco o lower interrompe retenção após 100 diagnósticos (`IMPLEMENTATION_LIMIT`); a enumeração externa mostra o restante do mesmo predicado, não afirma que o lower tenha emitido esses diagnósticos. [Conferência](probes/f2-programs.json).

Clusters causais se sobrepõem; não somar as colunas como partição:

'''
f2+=table(['Cluster','Programas','Mecanismo','Witness'],[
('K1_ROOT_EXIT',28,'EXIT raiz é frontier e recebe fallback ordinário em normalContinuation','CBACT01C, CBACT02C'),('K2_ROOT_SEQUENTIAL',8,'SET/ADD/DISPLAY/STRING/CONTINUE raiz terminal idem','CBEXPORT, COCRDUPC'),('K3_COMPOSED_FRONTIER',17,'Filho de IF terminal é frontier e também aponta fora do parágrafo','COACTUPC, CBTRN02C'),('K4_UNPUBLISHED_FRONTIER',32,'Falta frontier do filho/região; EVALUATE, IF implícito ou FILE handler ainda carrega edge ordinário','COPAUS1C, COBIL00C, COACCT01')])
f2+='\n'+table(['Programa físico','Sites','THRU/range','Repetidos','Clusters'],[(x['path'],x['performSites'],x['rangeSites'],x['repetitionSites'],', '.join(clusters['membership'][x['path']])) for x in programs])
f2+='''
## Cadeia causal, por autoridade

1. `AstBuilder.completionRelations` constrói sucessor local por parágrafo e sucessor ordinário entre parágrafos; propaga saídas de IF/EVALUATE para seus filhos. Plain EXIT/CONTINUE e statements sequenciais têm conclusão normal reconhecida.
2. `ProcedurePerformSemantics` publica membership e completions. A extração de frontier em IF depende de explicit termination; em EVALUATE depende de `structureKnown`, que hoje também exige `inputComplete(unit)`. Mistura-se forma/control com disponibilidade de valores/entrada.
3. `CobolSemanticProductProjector.observedContinuation` usa o mapa local e, quando falta, faz fallback para ordinary em modeled/preserved statements não FILE. Assim EXIT, SET e outros OBSERVED recebem `normalContinuation=próximo parágrafo`.
4. A coleção separada `ordinaryContinuations` tem whitelist MOVE/IF/EVALUATE/PERFORM/procedure-PERFORM e é suprimida quando inputComplete é falso. MOVE terminal já tem tratamento específico de completion; CALL tem tratamento próprio. Essas exceções explicam por que witnesses sintéticos centrados em MOVE/CALL passam.
5. `ProcedurePerformAdmission` exige que uma completion não possua successor concreto, salvo CALL para fora; exige que toda continuação “intrinsic” fique no parágrafo, salvo o mesmo CALL. Portanto interpreta o fallback ordinário de OBSERVED como controle explícito incompatível com completion. A mensagem “explicit control” não transforma EXIT ou SET em transferência explícita.
6. O assembler pode substituir uma frontier pelo retorno da ativação, mas a admissão já recusou a unidade. Quando a frontier nem foi publicada (K4), relaxar só a admissão permitiria escape ordinário indevido. Portanto eliminar checks/aceitar qualquer successor não é solução.

Fontes de código: `proleap-poc/.../AstBuilder.java:530–657`, `ProcedurePerformSemantics.java:58,239`, `EvaluateSemantics.java:33`, `semanticproduct/projection/CobolSemanticProductProjector.java:218,469`; `cobol-lower/core/.../ProcedurePerformAdmission.java:42–55`, `PartialProgramAdmission.java:20–32,263`, `PartialProgramAssembler.java:40–43,95–129`. Todos sob `.positive-memory-topology`, pins em D0_BASELINE. O probe externo conserva IDs AST separados dos StatementIds SP; o join usa proveniência expandida, nunca igualdade de ID numérico.

## Semântica fonte usada no raciocínio independente

'''
f2+=f'''IBM Enterprise COBOL 6.4, [Language Reference]({IBM}), “Transfer of control” p.79, “Implicit scope terminators” p.294, “PERFORM” pp.413–424, “EVALUATE” pp.339–342 e “EXIT” pp.342–345. Execução ordinária segue a ordem fonte; término de procedimento performed retoma após o caller; THRU termina no último procedimento. EXIT simples é neutro. Um WHEN selecionado termina seu próprio escopo, sem queda para o WHEN seguinte. O apêndice A p.740 permite EXIT simples junto de outros statements; não excluir CBACT01C por aplicar a restrição do padrão antigo.

As tabelas abaixo são **esqueletos esperados derivados da fonte**, não CFG executado nem outputs corrigidos. “ctx” significa a ativação do caller indicado, condicionado a conclusão normal. Os trechos COBOL lidos e os fatos completos estão no [dossiê de fontes](probes/f2-source-dossier.md) e [JSON](probes/f2-witnesses.json). SP atual bloqueia antes de AIR nesses casos.
'''
for name,desc in manual.items():
 selected=[w for w in ws if Path(w['program']).stem==name]
 f2+=f'\n### {name}\n\n{desc}\n\n'
 rows=[]
 for w in selected:
  t=w['terminal'];a=w['astControl'];nextid=t['next']['statement'];ctx=w['expectedContextCompletion'];ctx=ctx or ('PERFORM 2100-READ-CUSTOMER-RECORD, fonte linha 251' if name=='CBEXPORT' else 'IF WS-XREF-READ-STATUS, fonte linha 173' if name=='CBTRN01C' else 'COMPUTE WS-IDX, fonte linha 432');loc=t['source'];rows.append([w['caller']['id']+' → entry '+w['range'][0]['entry'],f"{t['id']} / {loc['file']}:{loc['startLine']} / {t['surface'][:95]}",f"{t['id']} → {nextid}",f"{t['id']} → boundary → {ctx}",f"local={a['paragraphLocalNext']}; ordinary AST={a['ordinaryNext']}; completes={a['normalCompletionRecognized']}",f"frontier={t['id'] in w['producerCompletions']}; next={nextid}"])
 f2+=table(['Invoke edge esperado','Ocorrência real','Edge ordinário esperado','Edge em ctx esperado','AST observado','SP / conflito lower'],rows)
 f2+='\nO lower rejeita o edge externo como intrinsic; quando frontier=true, também rejeita a possibilidade de conclusão contextual. Quando false, falta a conclusão do filho/região no contrato consumido, apesar de o AST reconhecer a conclusão normal do statement; CBTRN01C já publica a frontier do READ pai, mas o consumer não compõe seu handler.\n'
f2+='''
## Um desenho que evita confundir successors

```mermaid
flowchart LR
  C[PERFORM P] --> E[entry P]
  E --> S[último statement; execução de seus efeitos]
  S --> B[fronteira de P]
  B -->|sem conclusão do frame ativo| N[próximo parágrafo]
  B -->|P encerra range do frame ativo| R[resume do caller]
  B -->|P intermediário em THRU| N
```

Não são três edges incondicionais em um CFG comum. A regra de resolução depende do contexto e precisa ser consumida com matching, seja por especialização explícita ou por mecanismo local suportado. IF/EVALUATE terminais apontam para a mesma boundary pelos outcomes de conclusão de seus braços; GO TO continua tendo destino explícito.

## Falsificadores e alcance

- CALL/MOVE terminal são controles de contraste: o desenho atual já os trata de forma especial; isso refuta que todo statement seja incapaz de retorno contextual.
- THRU de COACTUPC/COCRDUPC refuta uma correção “todo EXIT retorna sempre”. Entrada ordinária de CBACT01C refuta “sempre remover fallthrough”.
- CBEXPORT e COPAUS0C mostram resumes subordinados que continuam ausentes no SP; zerar os 2.170 predicados não garante execução fiel nem produto final.
- Quatro programas K4 (CBTRN01C e CBTRN03C, checkout+ZIP) não têm INPUT_MISSING: FILE handler e IF de terminação implícita são lacunas independentes. Nos outros 28 K4 existe INPUT_MISSING; isso não autoriza atribuir todo gap do programa apenas ao COPY.
- COPYs incompletos podem esconder estrutura: só publicar o que tiver prova local e explicitar bounds; não inferir frontier de ProgramPoint, regex ou posição física no lower.
- A presença de CICS, FILE, CALL, IF, GO TO, repetition e callers múltiplos foi inventariada na matriz completa. Eles compõem as regiões, não são todos a origem do primeiro erro F2.

A solução comum é uma autoridade de **outcomes tipados e fronteiras de região**, com proveniência e dependências de prova. A/B/C são comparadas sobre os mesmos witnesses nos três documentos de arquitetura. Não foi implementado fix nem demonstrado PASS após mudança hipotética. O conhecimento atual basta para rejeitar novas exceções por verbo como estratégia arquitetural.
'''
put('D0_F2_DEEP_DIVE.md',f2)
put('D0_F3_COMPARISON.md','''# D0 — F3 versus F2

**SAME_MECHANISM** na perda de distinção entre conclusão local de região e sucessor ordinário. A regra de admissão que manifesta o erro é diferente.

Fonte real: `app/app-authorization-ims-db2-mq/cbl/CBPAUP0C.cbl`. EVALUATE `statement:58`, linhas 228–241, ao fim de 2000-FIND-NEXT-AUTH-SUMMARY; EVALUATE `statement:69`, linhas 259–271, ao fim de 3000-FIND-NEXT-AUTH-DTL.

| Pai | Filho terminal | Semântica esperada | SP pai | SP filho |
| --- | --- | --- | --- | --- |
| 58 | 62: SET END-OF-AUTHDB TO TRUE, l.235 | fim do WHEN GB → fim EVALUATE → boundary do parágrafo | normal UNAVAILABLE; ordinaryContinuations 58→66 | normal KNOWN→66 |
| 69 | 71: ADD, l.262 | fim WHEN sucesso → boundary | normal UNAVAILABLE; ordinary 69→77 | normal KNOWN→77 |
| 69 | 72: SET, l.264–265 | WHEN GE/GB compartilhado → boundary | idem | normal KNOWN→77 |

66 e 77 são EXITs dos parágrafos seguintes (linhas 244 e 274). **Não são siblings do EVALUATE.** A mensagem `normal arm completion cannot enter a sibling arm` é enganosa para esses destinos.

`EvaluateAdmission` admite saída de um braço quando interna ao braço ou igual ao successor local do pai. Não considera a boundary comum cujo default ordinário aparece em `ordinaryContinuations`. O projector põe o successor ordinário nos filhos OBSERVED, enquanto o pai mantém a ausência de sucessor local. É o mesmo choque de significados visto em F2.

O probe [AST CBPAUP0C](probes/ast-control/CBPAUP0C.json) confirma os mapas separados; o SP e stderr originais ficam no diretório canônico por programa. Valores DIBSTAT desconhecidos e efeitos DLI não explicam essa contradição. Não é necessário executar IMS para reconhecer END-EVALUATE, entry dos arms e seus términos.

Esqueleto esperado: `EVAL58 → arm(GB) → SET62 → Complete(EVAL58) → Complete(P2000)`; no contexto ordinário a boundary leva a EXIT66; no PERFORM que termina em P2000 devolve ao caller; no THRU até P2000-EXIT continua a EXIT66 e conclui ali. EVAL69 é análogo. Arms com GO TO/retorno explícito não recebem conclusão normal inventada.

A regra comum proposta para F2/F3 deve compor `Complete(region)` de dentro para fora. Acrescentar “permite successor fora” ao validator ou classificar os filhos como sibling válido esconderia o erro, não modelaria a linguagem.
''')
resolution=json.loads((P/'f5-resolution.json').read_text())
storage='''# D0 — storage partiality (F4/F5/F6)

F4 é **MIXED**. F5 combina input autêntico ausente com prova excessivamente global. F6 tem causa de representação de seção demonstrada. Selecionar profile não resolve a arquitetura de partiality.

## F4: configuração e arquitetura são perguntas diferentes

O checkout tem contexto IBM mainframe, CICS/IMS/JCL e `scripts/compile_batch.jcl.template` invocando IGYCRCTL com **IGY.SIGYCOMP.V63**. Isso sustenta uma família de dialeto IBM, mas **não prova Enterprise 6.4 + codepage 1047 para os 73**, especialmente o ZIP `UniKix_CardDemo_runtime_v1`. O corpus também contém script GnuCOBOL `--std=ibm-strict`. UTF-8 dos arquivos do Git não identifica o encoding runtime dos campos.

Q1: **INSUFFICIENT_EVIDENCE para seleção automática desse profile exato como verdade do corpus inteiro**. É legítimo executar um cenário explícito de análise IBM 6.4/1047, com premissa registrada e digest de configuração, como o probe fez. Para promovê-lo a configuração normativa, obter configurações/listings autênticos por variante ou aprovação explícita da premissa. Não excluir variantes UniKix silenciosamente.

Q2: sem perfil físico não se pode inventar offsets, bytes, tamanhos dependentes de encoding, SYNC ou aliases independentes. Porém nominal identity, boundaries de controle e alguns logical views não dependem dessa escolha. Hoje `StorageLayoutSemantics` adiciona PROFILE_NOT_SELECTED ao conjunto global de reasons; o predicado `environment` deixa extents/bases sem prova; depois a admissão global do lower recusa até valores textuais locais para os quais não possui uma representação regional admissível.

'''
storage+=table(['Programa F4','Declarações que expõem a barreira','Probe IBM explícito','Classificação'],[(x['program'],', '.join(d['name'] for d in x['rejectedDeclarations']), 'OUTPUT_INVALID / 30 I-02 (F7)' if 'CBACT04C' in x['program'] else 'dependency PARTIAL','F configuração física + D/A representação lógica limitada') for x in blast if 'F4' in x['family']])
storage+='''
Os cinco PARTIAL adicionais são DBUNLDGS, PAUDBLOD, PAUDBUNL e duas CSUTLDTC. Não são cinco suportes completos, nem um novo full 9/73 canônico. O profile-input-probe mantém COADM01C bloqueado; o profile-probe mantém COPAUS2C bloqueado. [Medições comparadas](probes/profile-probe-analysis.json).

## F5: 12 unidades, não 12 provas de impossibilidade

São seis fontes checkout e seis `.cl2` do ZIP. Todas têm COPY DFHAID e DFHBMSCA não resolvidos. A localização dos COPYs, cada declaration rejeitada, referências resolvidas/não resolvidas e sites observados estão em [blast radius](probes/storage-blast-radius.json) e [resolução integral](probes/f5-resolution.json).

'''
storage+=table(['Programa físico','Bindings resolvidos','Não resolvidos','Nomes sem binding','Declarações locais rejeitadas'],[(x['program'],x['counts']['status']['RESOLVED'],x['counts']['status']['UNRESOLVED'],', '.join(sorted({e['writtenText'] for e in x['unresolved']})),', '.join(d['name'] for d in next(b for b in blast if b['program']==x['program'])['rejectedDeclarations'][:5])) for x in resolution])
storage+='''
Não atribuir EIBCALEN/EIBAID/EIBRESP ou DFHRESP automaticamente aos dois COPYs: há também ambiente/tradução CICS, e o conteúdo autêntico ausente não foi fornecido. A atribuição exata declaration→COPY não pode ser provada a partir do nome. DFHENTER/PF* e constantes de atributo DFH* são consumidores nominais concretos a validar quando as bibliotecas autênticas estiverem disponíveis. Nenhum stub foi fornecido.

Fatos independentes observáveis: WS-PGMNAME/WS-TRANID e literais estão no próprio fonte antes dos COPYs; existem identidades e bindings locais; nomes de COPY e nomes escritos de recursos CICS podem ser preservados. Em COADM01C, WS-PGMNAME linha 36, WS-TRANID 37, XCTL computados linhas 146 e 169; MOVE WS-PGMNAME→CDEMO-FROM-PROGRAM linha 143 possui operandos declarados. Os valores DFH* em condições/cores não são premissa lexical desses nomes/targets escritos.

Isso **não prova uma célula física independente**: COPY textual arbitrário poderia mudar estrutura, colisões de nomes ou relações de storage. O fato que sobrevive deve declarar suas premissas: trecho fechado, namespace, boundary de seção e ausência de relação potencial relevante. A proposta é conservar conhecimento nominal/controle local e localizar incerteza; não assumir que todo COPY ausente contém só constantes, nem que um gap antes de outro trecho não o afete.

Hoje a existência de sourceText com base não provada e sem logical/scalar alternativa admissível gera `BLOCKED_LOWERING` para toda a unidade (`PartialProgramAdmission:96–106`). O lower não calcula que apenas DFHENTER está sem prova; bloqueia WS-PGMNAME, WS-TRANID etc. A separação fact-level é necessária; apenas adicionar copy path/profile não corrige esse desenho.

## F6: primeiro fato causal em COPAUS2C

Nas linhas 65–70, `EXEC SQL INCLUDE SQLCA` e `EXEC SQL INCLUDE AUTHFRDS` são preservados como **Ast.DataEntry com level="SQL", DataLevelKind.OPAQUE**. Não viram declarações COBOL expandidas. Em `StorageComponents.analyze:75–87`, cada filho Working-Storage deve ser DataEntry raiz nível 1/77; `level(SQL)` retorna **−1** (`:179`). Assim `structureProven=false` já na coleta de roots. Esse é o primeiro fato estrutural ausente.

`StorageLayoutSemantics:73` converte isso em SECTION_NOT_PROVEN global; extents/allocation se perdem; lower recusa data:1 WS-PGMNAME e data:13 WS-ERR-FLG. `sourceDependencies` registra SQLCA/AUTHFRDS UNRESOLVED, mas esse status nominal **não é a condição que produz structureProven=false**. É a representação opaca dos includes na seção. Mesmo com profile IBM, o bloqueio permanece. [Probe da prova de seção](probes/ast-control/COPAUS2C-storage.json).

Portanto os includes são causalmente relevantes pela falta de expansão/tipagem de declarations; não são mera coincidência. Entretanto “achar SQLCA no copy path” não está demonstrado como suficiente: o caminho EXEC SQL INCLUDE e a integração com declarations devem produzir uma estrutura tipada. O índice nominal de dependência não substitui esse caminho.

Classificação: **producer/storage model gap + global proof problem**; input autêntico é necessário para provar os campos incluídos, mas a perda de toda a seção é amplificação arquitetural. Preservar unknown region para o include e provas locais dos roots conhecidos depende de provar isolamento, não de apagar os nós SQL. Não houve alteração no fonte ou SP para testar um falso PASS.
'''
put('D0_STORAGE_PARTIALITY.md',storage)
put('D0_COMPLETENESS_BLAST_RADIUS.md','''# D0 — completeness blast radius e fact-level dependencies

## Mapa de usos relevantes

Caminhos abaixo são do frontend em `.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer`, salvo indicação. Referências exatas estão nos fontes nos pins congelados; números de linha servem à revisão do checkout atual.

| Uso | Premissa/efeito atual | Downstream | Escopo proposto |
| --- | --- | --- | --- |
| ResolutionAnalysisReport:231–241 | inputComplete false por qualquer gap INPUT aplicável à unit/ancestral | boolean reutilizado em provas heterogêneas | manter inventário global informativo; prova factual separada |
| projector:204 | inventário vira INPUT_MISSING | readiness/entry | não converter em proibição universal |
| projector:220 | suprime toda ordinaryContinuations | controle ordinário perde fatos | dependência do edge/region, não de valores alheios |
| IfSemantics:63 | input participa de qualificação | precisão/controle do IF | separar delimitação e predicate proof |
| EvaluateSemantics:33 | structureKnown exige input completo | K4 perde frontier/containment | preservar grouping/ends exatos; condição unknown explícita |
| PerformSemantics:49 | input completo na qualificação inline | body/resume/repetition | separar range, completion, predicate/count/value |
| ProcedurePerformSemantics:58 | perfil completo depende do input | gaps/eligibilidade, com fatos W5 preservados em parte | não tornar prova integral gate dos positivos |
| ScalarMoveSemantics:105,186 | input na elegibilidade/data e semântica CALL | disponibilidade de scalar/target/surface | binding e storage/effect específicos |
| CicsProgramControlAnalyzer:65 | em NEW_LOGICAL_LEVEL, omite prova de defaults do prefixo para unit incompleta; facts sintáticos são coletados antes | perde default-entry proof, não todos os LINK/XCTL | separar prova de ambiente de opcode/target; manter premissa quando necessária |
| StorageLayoutSemantics:66–95 | INPUT_MISSING, PROFILE_NOT_SELECTED, SECTION_NOT_PROVEN contaminam environment | todos extents/bases ficam sem prova física | regions/roots + shared relation dependencies |
| projector:291,305 | file inventories/auxiliary recebem missing | capacidades FILE/remainders | fact dependencies por use, decl e handler |
| projector:1379 | CALL surface condicionada a semantic.inputComplete | argumentos/surface podem ficar unavailable | conservar sintaxe independente de binding |
| lower PartialProgramAdmission:96–106 | sourceText representável exige base/extent ou alternativa admitida | um fato de data recusa unidade inteira | suportar fato desconhecido com bound real, sem IR inválida |

Este mapa cobre usos causais relevantes, não afirma que toda consulta do boolean seja incorreta. Uma COPY ausente em PROCEDURE DIVISION ou no meio de uma declaração pode afetar legitimamente estruturas posteriores. O defeito é usar o mesmo boolean sem registrar **qual prova depende de qual input**.

## Proposta de contrato, não implementação

`FactId → {kind, availability, value?, provenance, depends_on[], uncertainty_bound?}`. Dependencies podem referir source slice autenticado, COPY occurrence/content digest, binding proof, closed region proof, alias/layout/profile premise ou outro FactId. Falta de uma dependência degrada esse fato e seu fecho transitivo; não cria substituto positivo.

Exemplo COADM01C: token literal do nome do programa depende do slice local; identidade de data depende da declaração e do domínio de resolução; transferência lógica depende também dos dois views e da regra MOVE; offset físico depende de layout/profile/overlays; predicate DFHENTER depende da declaração/vendor input. Não unir essas cinco perguntas em `unit complete`.

Para missing COPY, emitir uma ocorrência de input ausente com posição, owner, tipo de região potencialmente afetada e incerteza sobre conteúdo. Se não for possível limitar o impacto estrutural, o bound deve permanecer amplo/unknown e o resultado perde conclusões correspondentes. Fatos realmente fechados podem sobreviver. Gaps locais não significam inventar AllControl/AllMemory como resposta universal nem prometer exaustividade de candidates.

## Sites que podem sobreviver versus precisão indisponível

Os 12 F5 têm 122–263 bindings resolvidos nos seis checkout sources, apesar dos COPYs ausentes. Os seis ZIPs têm inventários próprios no JSON, sem colapsar variantes. Os nomes de dependências fonte e os sites CICS observados sobrevivem como fatos nominais; CICS PROGRAM computado depende adicionalmente de values/views e do controle que alcança o site. Não rotular todo observed CICS como dependency CALL: SEND/RETURN/READ possuem operações diferentes. O inventário `dependencyStatements` é um conjunto amplo de superfícies candidatas para inspeção, não uma contagem final de dependency sites.

Reter nome fonte de um target é distinto de provar um candidate alcançável. Gate B deve exigir provenance/support, remainders de source/model/control e negativos por site. Nem `INPUT_MISSING` implica NO CFG, nem CFG parcial autoriza “nenhuma dependência existe”.

A análise completa de declarações/refs está em [storage-blast-radius.json](probes/storage-blast-radius.json) e [f5-resolution.json](probes/f5-resolution.json). Declarações internas de DFHAID/DFHBMSCA são **UNKNOWN por ausência de entrada autêntica**, explicitamente sem catálogo inventado.
''')
put('D0_F1_FRONTEND.md',f'''# D0 — F1 isolado de partial semantics

Dois bloqueios de frontend, reproduzidos na evidência canônica hash-verificada. São manifestações distintas da mesma ausência de política de colunas para HT.

| Caso | Local bruto | Comportamento atual | Causa |
| --- | --- | --- | --- |
| checkout COTRTLIC.cbl | offset zero-based 75337, linha 1811, coluna 18, byte 09, dentro de SQL | rejeita antes do preprocessing: Unsupported tab | validateFixedCharacters rejeita HT a partir de offset 6 com conteúdo posterior |
| ZIP CBSTM03A.cbl / COPY CUSTREC | COPY fonte linha 55; CUSTREC linha 6 começa `09 20 20 20 20 20 30 35` | conta HT como 1 caractere; lê `0` na coluna 7 e rejeita | HT inicial passa pela validação sem expansão |

O CUSTREC nativo equivalente tem dois HT antes dos cinco espaços e é aceito pelo contador atual. Isso não prova normalização correta. Não é CRLF, parser SQL ou ausência de COPY: o COPY do segundo caso foi encontrado.

## Autoridade e política necessária

IBM 6.4 [Language Reference, cap.6 pp.55–61]({IBM}) define áreas fixas 1–6, indicador 7 e área de programa 8–72. A referência consultada **não estabelece tab stops para importar bytes HT destes arquivos**. Portanto não afirmar que “IBM COBOL aceita tab com largura 8” como regra do compilador.

IBM documenta uma política de conversão em [z/OS `expand`](https://www.ibm.com/docs/en/zos/2.5.0?topic=descriptions-expand-expand-tabs-spaces): stops default de oito colunas, configuráveis. Também há opções de tab na [source conversion utility Linux](https://www.ibm.com/docs/en/cobol-linux-x86/1.1.0?topic=scu-source-conversion-utility-options). São precedentes de **import/conversion policy**, não prova da intenção original do fonte z/OS/UniKix.

Resposta “Tab é permitido?”: permitido como entrada de conversão com policy declarada; não há prova suficiente para admiti-lo silenciosamente como caractere literal de coluna no dialeto fixo pretendido. Sem policy, diagnóstico explícito é correto; aceitar alguns prefixos acidentalmente é inconsistente. Tipo F (configuração/import), com robustez G a confirmar contra policy/dialeto declarado, não invalid COBOL demonstrado.

Proposta: escolher tab stops explícitos, expansão até o próximo stop (não “substituir cada tab por 8 espaços”), antes de interpretar sequence/indicator/content/margem. Aplicar **por arquivo físico**, source e copybook, antes da expansão COPY; COPY reutiliza o mesmo normalizer e preserva cadeia de proveniência. A política de comentários, debug, continuação, literais e caracteres após coluna 72 precisa ser a mesma nos dois caminhos. Mapear todos os espaços gerados ao byte HT de origem, sem adulterar o corpus.

A futura wave deve provar os dois casos reais, testes de fronteira de coluna e negativos para mudança de indicador/literal. Expandir apenas depois de COPY já perde as colunas do copybook; aceitar HT como largura 1 mantém o erro. Nenhum normalization fix foi aplicado em D0.

A investigação anterior [report 2026-09-14](../carddemo-blockers-20260914/report.md) contém witness mínimo e stack; a conclusão D0 acima se apoia também nos bytes/logs canônicos atuais. Nenhum programa foi retirado dos 73 por esse motivo.
''')
f7=json.loads((P/'f7-03.json').read_text());f7b=json.loads((P/'f7-05.json').read_text());fr=list(csv.DictReader((P/'f7-labels.csv').open()))
put('D0_F7_IR_INTEGRITY.md',f'''# D0 — integridade AIR F7

**TYPE E: defeito de integridade, não partial semantics.** Profile explícito permite chegar ao assembler, que constrói um fragmento com 30 labels pendentes em cada variante CBACT04C. O validator real retorna INVALID_IR; a publicação é corretamente recusada. Não foi desligado validator nem serializada AIR inválida como saída aceita.

O [probe externo](tools/F7Probe.java) invoca decoder/admission/assembler reais, observa o registro de LocalIds por reflexão **somente leitura** e executa AirValidator. Reproduz exatamente 30 I-02, traversal completo e 19.283 obrigações semânticas no checkout. IDs e 30 contagens coincidem com o probe canônico. ZIP reproduz 30 I-02 separadamente. Obrigações semânticas não são confundidas com labels ausentes.

## Mecanismo demonstrado

Três READs têm handlers INVALID KEY. `FileControlLowering:73–76` referencia o primeiro statement do handler com `PartialProgramAssembler.label(handler.first, unit, localContext)`. `CompositionalPerformAdmission:20–38` expande closure por normal/ordinary, GO TO e braços IF/EVALUATE, mas **não visita as referências a handlers em fileInventory** quando encontra esses READs pela closure.

Em cada contexto ofensivo, o READ está no plano, o statement de handler não está. O `append` materializa todos os membros do body recebido, portanto não poderia criar a sequência que não entrou no plano. Os 11 callsites ofensivos têm `procedures=[]` e targetEntry positivo: a closure começa por entry sem membership/frontiers publicados e percorre successors ordinários. Isso explica também bodies amplos. O inventário ordinário contém o handler, com **outro contexto/LabelId**; não resolve a referência da ativação.

Não é pruning de `PerformActivationDemand`: com operações FILE, `inContext` retorna todo o inventário (scheduling eager). W6-R1 demanda não corta esses handlers. A referência foi criada por um subsistema FILE que não participa da closure genérica usada por W7/ativação. A causa é **inventário de edges dividido entre statement facts e file auxiliary facts + closure de ativação incompleta**. F7 é RELATED a F2/F3 pela autoridade fragmentada, mas não é o mesmo predicado de normalContinuation.

{table(['READ source','Handler esperado','Linhas checkout','Ocorrências ausentes por variante'], [('statement:215','statement:216 DISPLAY','373 → 375',9),('statement:222','statement:223 DISPLAY','394 → 397',10),('statement:229','statement:230 DISPLAY','416 → 418',11)])}

Os 30 são três classes de target em 11 contextos de PERFORM. Os 11 são entry-only (`procedures=[]`), verificados em f7-input-facts.json; falta de frontier torna o fecho maior e expõe handlers fora do inventário inicial. A [matriz completa de 60 linhas](probes/f7-labels.csv) contém para **cada** erro: operation, source statement/linha, target label/statement/linha, contexto, expected materializer e razão de ausência. [Raw checkout](probes/f7-03.json), [raw ZIP](probes/f7-05.json). Os números por grupo acima são confirmados abaixo pela matriz, e devem ser lidos sem somar F7 à partição F1–F6.

## Implicação de escala e obrigação futura

O fragmento checkout já tem **{len(f7['materializedLabels']):,} sequences** para 294 statements source no inventário ordinário. Ele é inválido, não um benchmark de análise aceita; ainda assim expõe o custo de clonagem/closures largas em um caso real. Aumentar limites não conserta referência faltante.

A obrigação arquitetural é `every emitted reference → inventoried target in the same context`, derivada do **mesmo grafo tipado** usado pelo materializer. Deve valer também para FILE events, USE, SORT callbacks, exception arms e local control. Adicionar apenas esses três handlers por nome CardDemo não generaliza. A futura correção precisa oracle de handler alcançável, handler proibido no evento errado e matching de contexto, além de zero I-02.

D0 não executou um fragmento “reparado”. Portanto não há promessa de que retirar F7 leve imediatamente a dependency: outros limites/obrigações podem aparecer depois.
''')
put('D0_MINIMUM_SEMANTIC_COMPLETENESS.md','''# MINIMUM SEMANTIC COMPLETENESS

**Nossa abordagem de não modelar tudo está errada? PARTIALLY.** O erro é tratar dimensões com obrigações diferentes como igualmente dispensáveis e deixar provas globais apagarem fatos independentes. D0 não sustenta a necessidade de um emulador COBOL.

| Camada | Mínimo para dependency extraction útil | Partiality aceitável | Limite demonstrado |
| --- | --- | --- | --- |
| L0 lexical/preprocessing | caracteres, colunas, tokens, COPY ownership e proveniência corretos | input ausente explicitado com impacto delimitado ou unknown | F1 impede até observar o programa; normalização errada muda linguagem |
| L1 control skeleton | cobertura muito alta de entries, boundaries, outcomes, transfers, returns e arms/handlers presentes | controle unknown com bound justificado, nunca tratado como impossibilidade | 47 F2/F3; F7 prova que edge não inventariado quebra integridade |
| L2 storage identity/layout | identidades, regiões, possíveis aliases e relações necessárias às transferências conhecidas | offsets/layout desconhecidos por região; logical views quando provados | F4/F5/F6 mostram gate global excessivo; separar células sem prova de alias seria incorreto |
| L3 value transformations | transferências relevantes a targets e overwrites seguros; distinguir valor conhecido de escrita conhecida | COMP-3/arithmetic/INITIALIZE values podem ser unknown | unknown assignment não preserva automaticamente um literal antigo; must-overwrite depende de prova |
| L4 effects/environment | leituras/escritas/exposures e eventos/controle com bounds honestos | CICS/IMS/DB2/MQ efeitos parciais, com provenance e remainders | execução normal não se presume de CALL/ABEND/READ; handlers têm edges próprios |
| L5 exact runtime behavior | não necessário para o produto proposto | valores concretos de condição, número exato de iterações, bytes runtime podem faltar | alta cobertura de skeleton não exige executar IMS nem prever dados reais |

“Control quase completo” significa cobrir os constructs efetivamente encontrados e as suas combinações, não provar todos os caminhos possíveis nem a terminação de programas arbitrários. GO TO dinâmico/ALTER, exceptions e special EXIT fora da capacidade precisam de representação explícita de desconhecimento; um gap de controle pode ampliar muito o resultado e precisa ser visível ao consumidor.

## Evidência a favor e contra H3

Contra necessidade de semântica integral: quatro produtos canônicos e cinco cenários adicionais de profile chegam a dependency com gaps; o runtime anterior levava nove/dez da amostra até o fim mesmo sem PERFORM tipado. Isso demonstra **representabilidade e viabilidade parcial**, não prova automaticamente soundness de cada resultado antigo. F2/F3 podem ser explicados sem conhecer o resultado de ADD, STRING, SET ou DLI: a fonte e a regra de término de região bastam.

A favor de limites à partiality: alias desconhecido pode afetar qualquer target compartilhado; controle unknown pode tornar indecidível um negativo de reachability; effects unknown podem invalidar candidates baseados em valor anterior. Portanto não se pode prometer extração exaustiva ou negatives precisos sem os fatos mínimos relevantes. O Gate B precisa distinguir may-candidate, suporte no modelo e remainder da fonte.

Não há prova em D0 de que todo programa empresarial possa ser analisado com alta precisão sem novas capacidades. Há evidência suficiente para refutar a tese de que implementar todos os valores/effects seja pré-requisito para resolver os 47 primeiros bloqueios de controle.

## Control graph antes da análise de valores?

**YES, como autoridade estrutural parcial tipada.** Construir a topologia fonte antes de escolher precisão dos valores evita que ausência de DFHAID apague END-EVALUATE e evita que um FILE handler seja invisível ao inventário. Depois refinar predicates/values sem mudar silenciosamente a existência de boundaries. Custo: contrato SP versionado, frontend com uma disciplina de regiões, validação cruzada e migração coordenada. Não é exportar um CFG fonte monolítico com todos os contextos ou um interpretador completo; contexto continua uma dimensão do consumidor de topologia.
''')
put('D0_RESPONSIBILITY_MATRIX.md','''# D0 — matriz de responsabilidade

SP é transporte tipado, não uma segunda autoridade semântica. CFG/dataflow interpretam AIR, não devem reconstruir parágrafos COBOL.

| Conceito | Quem interpreta hoje | Problema concreto | Autoridade proposta |
| --- | --- | --- | --- |
| paragraph boundaries | AstBuilder, ProcedurePerformSemantics, projector, admission/assembler | fronteira vira caso por variante | frontend ControlTopology: boundary nomeada e origem |
| ordinary fallthrough | AstBuilder + fallback projector + ordinaryNext lower | OBSERVED usa campo local para edge ordinário | frontend outcome NORMAL para boundary/default |
| PERFORM entry | resolver/ProcedurePerformSemantics; lower reconcilia | perfis de corpo confundidos com existência do entry | frontend LOCAL_INVOKE entry proof |
| PERFORM completion | semântica frontend + global completion set + lower overrides | source-global frontier consumida como edge exclusivo | frontend Complete(region); runtime context aplica binding |
| THRU sequencing | range frontend + mapper lower | membership usado também como fecho de referência | frontend ordem/boundaries; lower tradução genérica de regions |
| IF arms | AstBuilder, IfSemantics, projector, admission, assembler | arm exit depende de successor pai/conhecimento integral | frontend branch entries e region exits independentes de values |
| EVALUATE outcomes | AstBuilder, EvaluateSemantics, projector, EvaluateAdmission | F3; K4 omite topology por input/value proof | frontend grouping/ordering/normal exit; predicate separado |
| GO TO | resolver/semântica frontend; closure/admission/assembler | múltiplas listas de referências | frontend EXPLICIT_TRANSFER; validator genérico verifica alvo |
| local return | frontend frontier; lower specialization | matching embutido em cloning | regra de contexto tipada; backend genérico, sem rederivar COBOL |
| special EXIT | AstBuilder exclui do neutro; capabilities parciais | risco de tratamento por verbo/relaxamento | frontend opcode/outcome específico, UNKNOWN_LOCAL onde não provado |
| unknown control | readiness/gaps em várias camadas | ausência de edge confundida com unknown completion | frontend bound e reason tipados; consumers preservam remainder |
| FILE/USE/SORT handlers | inventário frontend + FILE lowering paralelo à closure | F7 targets emitidos sem membership contextual | um inventário topológico de referências para todos os outcomes |

Validação lower continua necessária: identidade, versão, integridade, existência de targets, compatibilidade de capability e proveniência. O que sai do lower é decidir semântica COBOL por whitelists, ordem textual ou repetição de regras de término. Tradução de topologia em AIR e gestão de orçamento/contexto continuam suas responsabilidades.

A proposta não cria um mega-frontend de valores: separar módulos grammar-to-regions, resolution-to-targets, topology validation e fact-dependency tracking, com um único contrato de saída. Storage/value/effects refinam facts sem reescrever o skeleton por status global.
''')
common='''Os quatro witnesses comparados em A/B/C são os mesmos: (W1) CBACT01C PERFORM89/EXIT158; (W2) COACTUPC PERFORM858 THRU/SET1284/EXIT1312; (W3) CBEXPORT PERFORM94→1000 com PERFORM103 aninhado→1050/STRING111; (W4) CBPAUP0C EVALUATE58/SET62→boundary2000. São modelos hipotéticos, sem execução/fix de produto. Edges de linguagem estão em D0_F2_DEEP_DIVE e D0_F3_COMPARISON.
'''
put('D0_ARCHITECTURE_A.md','''# Arquitetura A — positive facts corrigida

**PARTIALLY_SUPPORTED como destino arquitetural; não recomendada isoladamente.** Preserva compatibilidade e a especialização hoje disponível, mas a sustentabilidade da expansão não foi demonstrada para o corpus real.

'''+common+'''
## Uma regra comum possível

Separar em todos os statements: sucessor dentro da região, conclusão normal da região, transferência explícita e default ordinário. A frontier não é uma alegação de que o statement termina; só define o resultado da conclusão normal. CALL/MOVE/OBSERVED/FILE deixam de precisar de exceções diferentes. O consumer não pode interpretar ordinary como explicit transfer.

Cinco mudanças conceituais necessárias: (1) semântica uniforme de outcomes; (2) composição de regiões incluindo arms e handlers; (3) prova de controle independente de valores/input global; (4) fecho de referências único para inventory e emissão; (5) contrato de especialização/matching com limite de escala explícito. Isso é mais que trocar um `if` em admission. Se A mantiver as whitelists atuais, não satisfaz o critério de “uma abstração comum”.

| Witness | Representação A corrigida | Diferença para hoje |
| --- | --- | --- |
| W1 | EXIT158 tem completion(P0000), ordinary→44; clone do caller89 resolve completion→90; ocorrência ordinária→44 | OBSERVED não publica ordinary como intrinsic local |
| W2 | SET1284 completa EVAL e PYYYY; no range858 resolve→1312; EXIT1312 completa último parágrafo→859; entrada ordinária continua fora do range | não exigir inputComplete para grouping/terminação exatos; só último endpoint retorna |
| W3 | clone de1000/caller94 contém invoke aninhado103; clone1050/103 completa STRING111→104 no contexto de94; DISPLAY106 completa1000→95 | dois contextos de completion, sem bypass; ordinary STRING111→112 permanece distinto |
| W4 | SET62 completa arm/EVAL58; frontier da região2000 chega à boundary; default66 ou retorno da ativação que termina ali | admission usa a conclusão de região; não exige igualdade com um único normal do pai |

Múltiplos callers ganham IDs/contextos distintos; THRU dá override apenas nas boundaries correspondentes; repetition devolve ao nó de teste/incremento correto. GO TO escape requer fecho de referências no mesmo contexto e regra explícita sobre contexto ativo; não se deve transformar salto em retorno local. Dead code não é materializado por demanda só quando a ausência de demanda está provada; FILE hoje força eager.

## Escala, compatibilidade e riscos

A expansão do grafo é aproximadamente soma do body materializado em cada contexto alcançável, não apenas número de callsites. Em DAG compartilhado pode crescer exponencialmente no número de níveis. Evidência W6-R1: live DAG depth12 tem **30 source statements, 20.507 blocos AIR, 24.605 operações e 8.191 contextos**; lower 3,620 s / 748.808 KiB RSS; analysis 4,121 s / 999.592 KiB RSS. Cold DAG é barato depois de R1, mas isso não elimina o caso live. Chain depth256: 262 source / 776 blocos / 257 contextos, indicando que o problema é compartilhamento de caminhos, não toda profundidade.

O CBACT04C real perfilado já monta 23.873 sequences antes de falhar em integridade. COACTUPC tem 1.415 statements SP, 61 PERFORM sites e grande reutilização de ranges; o lower bloqueia na admissão, portanto **não há medição válida de expansão final para ele**. Não extrapolar uma razão linear dos sintéticos.

A pode ser um backend transitório limitado, com budget, resultado de limite honesto e benchmark real obrigatório. Não há base para certificar hoje que seja operacionalmente aceitável como solução geral. Preservar legacy exige decode versionado; não reinterpretar JSON antigo como contrato novo. Repositórios mínimos: frontend + lower; AIR/CFG podem manter o modelo existente, desde que os grafos traduzidos passem integridade/quality.

**Critério de recomendação A não satisfeito integralmente:** abstração comum é desenhável, mas whitelists/autoridade duplicada precisam ser removidas e performance de COACTUPC/FILE permanece sem prova. A não deve justificar mais campos especiais por construct.
''')
put('D0_ARCHITECTURE_B.md','''# Arquitetura B — control.local@1

**PARTIALLY_SUPPORTED no contrato AIR; UNSUPPORTED no consumer CFG atual. Não recomendar adoção imediata.** Pode reduzir representação estática e clonagem, mas matching de contexto transfere custo ao solver.

'''+common+'''
## Contrato existente, sem extensão inventada

Fonte normativa local: `analysis-ir/especificacao/05-controle-e-invocacoes.md`, seção control.local; compromisso do consumer: `analysis-cfg/docs/domain/local-control.md`. `local.invoke` empilha entry/ports/resume sem bypass e compartilha memória; `local.boundary(port,default)` só casa com o frame do topo, desempilha se casar e segue default sem alterar stack caso contrário. `local.resume` exige frame ativo; `local.unwind(n,dest)` remove exatamente n; `jump` não desempilha. Não procurar um frame antigo por baixo do topo.

Pseudocódigo de operações, não AIR JSON executável:

```text
W1 CBACT01C:
  s89: local.invoke(entry=s40, ports={end_P0000}, resume=s90)
  s158: no-op ; jump boundary_P0000
  boundary_P0000: local.boundary(end_P0000, default=s44)
  stack vazia: s158 → s44
  topo=s89: s158 → s90, pop

W2 COACTUPC:
  s858: local.invoke(entry=s1283, ports={end_PYYYY_EXIT}, resume=s859)
  arm/SET1284: jump boundary_PYYYY
  boundary_PYYYY: local.boundary(end_PYYYY, default=s1312)
  s1312: no-op ; jump boundary_PYYYY_EXIT
  boundary_PYYYY_EXIT: local.boundary(end_PYYYY_EXIT, default=s819)
  s858 ativo: primeira boundary não casa; segunda casa e retorna
  eventual PERFORM só PYYYY: primeira boundary casa
  stack vazia: ambas usam default

W3 CBEXPORT, nested:
  s94: local.invoke(entry=s102, ports={end_P1000}, resume=s95)
  s103: local.invoke(entry=s107, ports={end_P1050}, resume=s104)
  STRING111 → local.boundary(end_P1050, default=s112)
  DISPLAY106 → local.boundary(end_P1000, default=s107)
  stack [s94,s103] → [s94] → []; values/storage permanecem compartilhados

W4 CBPAUP0C:
  EVAL58 → arm(GB) → SET62 → complete_EVAL58 → boundary_P2000
  boundary_P2000: local.boundary(end_P2000, default=s66)
  PERFORM cujo último parágrafo=P2000 registra essa porta;
  THRU até P2000_EXIT registra a porta do EXIT, alcançada depois de s66.
```

Entries numéricos foram conferidos no SP dos witnesses; os identificadores de boundary são novos nomes conceituais. Para F3 B ainda precisa corrigir a publicação/composição de EVALUATE. Introduzir stack sozinho não recupera arms descartados nem resolve o contrato contraditório.

Múltiplos callers usam o mesmo body e distintos resumes no frame. Repetition pode ser wrapper de teste/incremento que invoca o body e recebe completion por iteração; condições unknown mantêm ambos os outcomes admissíveis. `local.resume` só serve a uma saída incondicional do frame comprovada; nunca mapear plain EXIT para resume, pois a entrada ordinária precisa continuar.

## GO TO, nesting e recursão

GO TO para fora do intervalo textual não autoriza `unwind(1)` automaticamente: o contrato jump conserva stack e o frontend precisa provar a disciplina COBOL da transferência, inclusive possível retorno ao range. Transfers de programa/ABEND não equivalem a completion normal. Special EXIT exige regra própria; EXIT PARAGRAPH/SECTION não é unwind genérico. Ranges ativos sobrepostos/cruzados precisam obedecer às restrições da linguagem e não mudar a semântica top-only para “fazer funcionar”. AIR permitir uma pilha recursiva não prova que todas as formas de PERFORM recursivo sejam válidas/suportadas.

## Custo e decisão

Body estático tende a O(S+E+callsites+boundaries), com compartilhamento, ao contrário da soma por contexto de A. A análise ainda deve resolver caminhos realizáveis: um grafo que liga cada boundary a todos os resumes cria cross-return e contamina targets. CallsiteId no edge sem matching nas consultas é insuficiente. São candidatos a estudar pushdown/summaries/context states; nenhum foi implementado ou medido aqui. Aliases/effects compartilhados e widening de valores podem aumentar complexidade mesmo quando o grafo estático é pequeno.

O CFG atual declara explicitamente control.local não suportado (BACKLOG-CFG-013); portanto não afirmar que B reduz complexidade **total** nem que melhora dataflow. Migração envolveria frontend, lower, air-java (modelo LocalInvoke/LocalBoundary, checks e binding reader já existem; confirmar qualificação integrada), analysis-cfg e alinhamento normativo analysis-ir. O contrato já existir não certifica todos os componentes.

B tem mérito para experimento de backend depois de estabilizar C: os quatro witnesses, shared live DAG, negative cross-return, ordinary entry e GO TO escape devem passar, com custo medido. Até lá, critérios B de ausência de piora CFG/dataflow e redução total de complexidade são **UNKNOWN**, impeditivos de recomendação imediata.
''')
put('D0_ARCHITECTURE_C.md','''# Arquitetura C — frontend como autoridade de ControlTopology

**Arquitetura recomendada para a próxima fase, sujeita a revisão.** É uma decisão de autoridade e contrato, não uma declaração de implementação pronta ou de 73 PASS. Manter inicialmente AIR atual como backend evita acoplar essa mudança a um solver control.local ainda não suportado.

'''+common+'''
## Modelo proposto

Uma topologia finita de SourceOccurrence/Region/Boundary, com outcomes tipados, identidade estável, origem e proof dependencies. Exemplo de vocabulário: `NORMAL`, `BRANCH`, `EXPLICIT_TRANSFER`, `LOCAL_INVOKE`, `COMPLETE(region)`, `PROGRAM_RETURN`, `UNKNOWN_LOCAL(bound)`. A palavra LOCAL_COMPLETE isolada não basta: identificar a região/porta e a regra que a consome é necessário.

`Normal` interno a uma região pode chegar a `Complete(R)`. Composição propaga Complete(arm) ao join/fim do pai; Complete(paragraph) encontra uma boundary com default ordinário e binding contextual de invocação. IF/EVALUATE transportam grouping/entries/exit relations mesmo quando predicates/values são unknown. Exceções/FILE handlers pertencem ao mesmo inventário de outcomes. Availability de um edge depende de provas específicas, não da precisão de efeitos do statement inteiro.

| Witness | Fatos frontend propostos | Tradução lower genérica |
| --- | --- | --- |
| W1 | Invoke89(entry40, end=P0000, resume90); EXIT158 Normal→Complete(P0000); boundary default44 | especializar regra de boundary no contexto89, sem reconhecer palavra EXIT |
| W2 | Invoke858(range PYYYY..PYYYY_EXIT, resume859); EVAL arm SET1284→Complete(arm)→Complete(PYYYY); default1312; EXIT1312→Complete(PYYYY_EXIT), default819 | binder de range/context resolve primeira boundary por default e última por resume; não reconstrói THRU |
| W3 | Invoke94(P1000,resume95); Invoke103(P1050,resume104); STRING111→Complete(P1050); DISPLAY106→Complete(P1000) | empilhar bindings genéricos ou especializar grafo tipado; dados mantêm identidade compartilhada |
| W4 | Branch/Evaluate58 com arm GB entry62; SET62→Complete(armGB)→Complete(EVAL58)→Complete(P2000), boundary default66 | nenhuma comparação ad hoc entre filho.normal e pai.normal; apenas tradução de outcomes |

## O que desaparece e o que permanece

Podem ser substituídas quatro reconciliações semânticas: (1) whitelist/fallback de ordinary no projector, (2) exceção CALL vs outros completions na admissão, (3) reconstrução de arm exit por igualdade com successor pai, (4) closures independentes para statement versus FILE handler. `ProcedurePerformSemantics` deixa de reprovar a existência de uma topology por whole-body/value profile. A lógica de reconhecimento COBOL continua no frontend, onde já está a gramática e o resolver; não é transferida ao CFG.

Não estimar “linhas apagadas” sem implementação. Lower continua traduzindo operações/values/storage, materializando contextos de um grafo tipado quando necessário, gerindo IDs e verificando integridade/capabilities. O ganho é remover **decisões de semântica COBOL**, não eliminar todo processamento de grafo. Projector faz transporte e checagem, sem inventar edges.

SP pode transportar partiality porque já tem availability, proveniência e inventories; precisa uma versão coordenada para topology e depends_on. Fatos desconhecidos devem identificar a região potencialmente afetada; se bound não é provado, não estreitá-lo artificialmente. Completeness de referência/topologia publicada e completude semântica da fonte são propriedades diferentes.

## Custo, escopo e limites

Mínimo inicial: frontend + SP schema/transport + lower; contratos/evidência E2E. AIR/CFG podem continuar usando operações existentes se a tradução materializada expressar corretamente os outcomes; isso requer roundtrip/validator/quality gates. Não exige nova IR antes dessa prova. Se UNKNOWN_LOCAL ou matching necessário não tiver representação honesta no backend, essa parte precisa capability/contrato explícito, não fallback inventado.

Módulos separados para grammar regions, targets resolvidos, topological invariants e dependencies de prova impedem que frontend vire um emulador de COBOL. Valores/effects não precisam ser completos; podem refinar outcomes/condições depois. Proveniência de boundary aponta ao término sintático e ao parágrafo/range; edges derivados citam premissas.

C **não elimina por si só** o custo de activation cloning: com backend A, herda seu limite. O grafo fonte pode ser O(S+E+boundaries), mas a AIR especializada depende do número de contextos. Por isso um benchmark real de COACTUPC e CBACT04C e o live shared DAG são critérios de go/no-go da migração, junto de zero cross-return. B é alternativa futura de backend, não parte aprovada desta decisão.

## Por que o custo é justificável

F2/F3 atingem 47 first blockers por perda de uma abstração comum; F7 é uma segunda falha de inventário de edges. Os probes mostram que parte da informação correta **já existe no AST** e é degradada na publicação/reconciliação. A mudança concentra autoridade existente e reduz caminhos divergentes, em vez de acrescentar interpretações em cada consumer. F5/F6 demandam a disciplina complementar de fact dependencies; C oferece o transporte, mas não resolve storage automaticamente.

Critérios C: duplicação real demonstrada; representação parcial desenhada sobre quatro witnesses; simplificação conceitual do lower identificada; custo de dois repos/contrato justificável; escopo frontend limitado à linguagem e à prova de topology. Confirmação operacional depende da futura implementação e seus gates. Nenhum fix foi feito em D0.
''')
score=[
('F2 coverage','PARTIALLY_SUPPORTED — regra comum desenhada; sem fix executado','PARTIALLY_SUPPORTED — ports expressam retorno; ainda precisa corrigir frontend','PARTIALLY_SUPPORTED — quatro witnesses e 46 inventariados; futura execução'),
('F3 coverage','PARTIALLY_SUPPORTED — conclusão de região corrige contrato em desenho','PARTIALLY_SUPPORTED — stack não recupera arm topology sozinho','PARTIALLY_SUPPORTED — Complete(arm) compõe com region boundary'),
('Paragraph ordinary execution','SUPPORTED — mapa AST e ordinary relation já existem, publicação desigual','SUPPORTED — boundary default com stack vazia, contrato normativo','SUPPORTED — default fonte explícito, demonstrado no AST'),
('PERFORM contextual return','PARTIALLY_SUPPORTED — especializado funciona nos gates qualificados, falha CardDemo','SUPPORTED — invoke/boundary expressam matching no contrato; consumer falta','PARTIALLY_SUPPORTED — binding explícito, backend precisa qualificação'),
('Multiple callers','SUPPORTED — W6 context separation em corpus qualificado','SUPPORTED — body compartilhado, frame contém resume distinto','PARTIALLY_SUPPORTED — topologia compartilhada; matching depende do backend'),
('Nested PERFORM','PARTIALLY_SUPPORTED — nested gates passam; real/fanout escala aberta','PARTIALLY_SUPPORTED — top-only serve nesting bem formado; restrições COBOL permanecem','PARTIALLY_SUPPORTED — região tipada; caso CBEXPORT desenhado'),
('THRU','PARTIALLY_SUPPORTED — ranges qualificados; F2 mostra frontier inconsistente','SUPPORTED — porta final diferente de intermediárias no contrato','PARTIALLY_SUPPORTED — ordem/portas source explícitas; não implementado'),
('Repetition','PARTIALLY_SUPPORTED — wrappers W6; resume inline real pode faltar','PARTIALLY_SUPPORTED — wrapper por iteração expressável; solver sem medição','PARTIALLY_SUPPORTED — loop topology separada de count/value proof'),
('GO TO escape','PARTIALLY_SUPPORTED — closure existe; context semantics precisa prova','PARTIALLY_SUPPORTED — jump conserva stack; unwind requer prova adicional','PARTIALLY_SUPPORTED — transfer tipado não infere completion; dialeto/active range'),
('IF/EVALUATE composition','PARTIALLY_SUPPORTED — whitelists/admission contradizem F3','PARTIALLY_SUPPORTED — ainda exige regions/outcomes frontend','PARTIALLY_SUPPORTED — regra comum elimina reconciliação por successor pai'),
('Localized gaps','PARTIALLY_SUPPORTED — boas intenções, unit proof limita F5/F6','UNKNOWN — local stack não define blast radius de fatos','PARTIALLY_SUPPORTED — depends_on/bounds desenhados, storage precisa trabalho'),
('Dead code','SUPPORTED — negativos sintéticos existentes; sem universalidade','UNKNOWN — matching/queries CFG ainda indisponíveis','PARTIALLY_SUPPORTED — outcomes explícitos favorecem prova; backend/gaps importam'),
('Determinism','SUPPORTED — hashes de repetição W6/W7 em corpus qualificado','UNKNOWN — contrato de IDs possível, implementação não medida','PARTIALLY_SUPPORTED — IDs/provenance estáveis desenhados; exigir permutation'),
('Performance','PARTIALLY_SUPPORTED — cold pruning/linear chain bons; live DAG expande','UNKNOWN — sem solver/benchmark real','UNKNOWN — frontend finito; backend A herda expansão'),
('IR size','PARTIALLY_SUPPORTED — 20.507 blocos/30 source no live DAG12','PARTIALLY_SUPPORTED — compartilhamento reduz grafo estático; custo solver deslocado','UNKNOWN — fonte O(S+E), AIR depende do backend escolhido'),
('Implementation complexity','PARTIALLY_SUPPORTED — cinco mudanças comuns, dívida de whitelists','UNKNOWN — mudança de solver/matching pode dominar economia lower','PARTIALLY_SUPPORTED — módulos e responsabilidades definidos; sem esforço estimado fictício'),
('Number of repos touched','SUPPORTED — mínimo frontend/lower; sem nova AIR no desenho','PARTIALLY_SUPPORTED — frontend/lower/CFG e auditoria air-java/IR','SUPPORTED — mínimo frontend/lower + contrato SP/evidência; backend inicial existente'),
('Migration risk','PARTIALLY_SUPPORTED — menor mudança wire se versionada, alto risco semântico','UNKNOWN — nova capability consumer e matching não qualificados','PARTIALLY_SUPPORTED — schema coordenado e dual-read; mais trabalho upfront'),
('Legacy compatibility','PARTIALLY_SUPPORTED — preservar decode/golden antigos é factível','PARTIALLY_SUPPORTED — capability opt-in; consumers antigos não suportam','PARTIALLY_SUPPORTED — versão explícita + fixtures antigas sem reinterpretar'),
('Testability','SUPPORTED — harness/context negativos já existem','PARTIALLY_SUPPORTED — oracles locais previstos, sem consumer implementado','PARTIALLY_SUPPORTED — edge inventory e proof dependencies testáveis por fronteira'),
('Future COBOL constructs','PARTIALLY_SUPPORTED — só generaliza se eliminar exceções por verbo','PARTIALLY_SUPPORTED — stack resolve calls; não conhece todos os constructs','PARTIALLY_SUPPORTED — extensões em outcomes/regions na autoridade única'),
('Corporate generalization','UNKNOWN — sintéticos/CardDemo não certificam código bancário','UNKNOWN — não avaliado fora de desenho contratual','UNKNOWN — melhor separação é argumento causal, não benchmark corporativo')]
put('D0_ARCHITECTURE_SCORECARD.md','''# D0 — scorecard por critério concreto

SUPPORTED significa evidência positiva **no escopo escrito na célula**, que pode ser expressividade do contrato, não implementação end-to-end. PARTIALLY_SUPPORTED indica suporte condicionado/incompleto. UNSUPPORTED significa capacidade ausente demonstrada; UNKNOWN significa evidência insuficiente. Não há soma ou nota numérica. C é escolha de autoridade; A/B são também estratégias de execução, portanto não são alternativas perfeitamente ortogonais.

**Capacidade operacional B hoje: UNSUPPORTED no CFG** (`docs/domain/local-control.md`). A tabela distingue isso da expressividade normativa.

'''+table(['Critério','A corrigida','B control.local','C frontend topology'],score)+'''
Fontes: D0_F2_DEEP_DIVE (46/14 witnesses), D0_F3_COMPARISON, D0_F7_IR_INTEGRITY, D0_COMPLETENESS_BLAST_RADIUS; W6_R1_PERFORMANCE, W6_E2E, W7_PERFORMANCE e contrato AIR §05.7. Hashes históricos verificados em probes/historical-evidence-hashes.json. Nenhuma célula é um PASS de implementação D0.

A não satisfaz sustentabilidade geral demonstrada. B não satisfaz custo/precisão total de CFG/dataflow demonstrados. C satisfaz os critérios de reorganização de autoridade por evidência causal e desenho concreto; sua operação/escala exigem gates futuros. Não se criou uma arquitetura D artificial.
''')
history=json.loads((P/'history/measurements.json').read_text());hq=json.loads((P/'history-quality.json').read_text());hr=[]
for name in ['CBACT01C','COACTUPC','CBEXPORT','COPAUS1C','COACCT01','CBPAUP0C','COADM01C','COPAUS2C','CBACT04C','COBSWAIT']:
 row=[name]
 for wave in ['pre-positive','w5','w6','w6-r1','w7','w7-r1']:
  m=next(x for x in history if x['wave']==wave and Path(x['path']).stem==name);s=m['stages'];row.append('dependency PARTIAL' if s['dependency']['state']=='PARTIAL' else 'frontend owner failure' if s['frontend']['state']=='BLOCKED' else 'lower BLOCKED')
 row.append('F2' if name in ['CBACT01C','COACTUPC','CBEXPORT','COPAUS1C','COACCT01'] else 'F3' if name=='CBPAUP0C' else 'F5' if name=='COADM01C' else 'F6' if name=='COPAUS2C' else 'F4' if name=='CBACT04C' else 'dependency PARTIAL');hr.append(row)
put('D0_PROCESS_POSTMORTEM.md','''# D0 — postmortem crítico do processo

A metodologia foi insuficiente para qualificar viabilidade de produto. Muitos gates fortes e úteis foram aplicados a um espaço de exemplos escolhido pela própria decomposição da implementação. Fechar responsabilidade de uma wave passou a funcionar como substituto prático de medir se programas reais ainda atravessavam a pipeline. Os documentos antigos frequentemente limitam corretamente suas alegações; o erro foi permitir progressão arquitetural sem um gate adicional de viabilidade real.

## Evidência de evolução, não memória do processo

Amostra estratificada de dez fontes reais, mesmos bytes e resolução do runner, seis runtimes congelados. W8 reutiliza a medição canônica. Todos os pins/classes/commands e outputs estão em probes/history; a JVM/heap/timeouts dos probes constam nas medições. Nenhum build histórico foi refeito.

'''+table(['Programa','Pré-positive','W5','W6','W6-R1','W7','W7-R1','W8 atual'],hr)+'''
**9/10** chegavam a dependency PARTIAL antes da campanha; **1/10** desde W5 até W7-R1 e no atual. Oito regressões de viabilidade na amostra já estariam visíveis no máximo em W5; o nono blocker já existia antes. A causa exata de introdução entre o baseline pré-positive e W5 não foi isolada por bisect, portanto não atribuir tudo ao commit W5.

COACTUPC histórico falha em `SOURCE_DEPENDENCY_OWNER_UNPROVED` durante semantic product, embora o wrapper classifique o motivo como PREPROCESSING_FAILED. O stderr preservado demonstra a fase real. W8 corrige/ultrapassa essa barreira e expõe F2. Isso é melhora de uma fronteira, não regressão F2 exclusiva de W8.

CBACT01C pré-positive tinha 103 OBSERVED e **zero PERFORM_PROCEDURE**; W5 tem 69 OBSERVED e 34 PERFORM_PROCEDURE. O antigo produto chegava ao fim sem o controle positivo novo. A publicação mais expressiva expôs consumer invariants contraditórios e storage gates; voltar a OBSERVED para restaurar 9/10 esconderia os fatos. F5 já existia como gap em COADM01C pré-positive, mas não era blocker de pipeline. [Comparação de qualidade](probes/history-quality.json).

Não há evidência para afirmar que os 69 atuais bloqueariam exatamente nas mesmas famílias em W5. A amostra prova a falha sistêmica que um gate real teria sinalizado; código comum e witnesses explicam sua extensão atual. Não extrapolar contagens históricas não executadas.

## Por que as waves foram aprovadas

| Viés | Evidência / mecanismo | Intervenção concreta |
| --- | --- | --- |
| Corpus | W6 usou 39 históricos + 3 derivados, 45 sites; gates por responsabilidade | full CardDemo 73 em toda wave arquitetural, depois do corpus qualificado |
| Witness | frontiers sintéticas centradas em MOVE/CALL receberam caminhos especiais; EXIT/SET/DISPLAY real combinados com fallthrough escaparam | fonte real primeiro; derivar mínimo só depois; contraste por outcome equivalente em verbos diferentes |
| First-loss | W5 documentou dependências de W6/W7 e não exigia dependency PASS; a progressão não mediu CardDemo amplo | publicar distribuição por estágio e reexecutar full após remover mecanismo; acompanhar próximos blockers |
| Gate | FAST/full/CI verificam invariantes e oracles locais, não denominador do produto | dois gates separados: viabilidade e qualidade; green CI nunca substitui a matriz real |
| Contract | produtor preservou frontiers; consumer manteve regra incompatible para OBSERVED | testes de fronteira produzidos pelo frontend real, incluindo final statement + próximo parágrafo |
| Realism | COACTUPC combina 61 ranges, copies, IF/EVAL, calls, CICS; FILE cria edges por canal auxiliar | corpus combinado com nested/range/repetition/handlers e entradas ordinárias |
| Approval | escopo local CLOSED foi suficiente para avançar apesar da viabilidade não medida | fechamento requer impacto real medido e regressões explicitamente aceitas, além do contrato local |
| Adversarial | havia negativos sérios de cross-return/dead code e metamórficos, mas gerados em domínio estreito | auditoria independente language→edges; trocar construct terminal, misturar FILE handlers, shared DAG vivo |

Não há registro que permita inferir motivos pessoais para CardDemo não ser gate desde W5. Há evidência objetiva de que os critérios escolhidos não o exigiam. W5_EVIDENCE declara que dependency PASS não era requisito de fechamento. W6_E2E declara sucesso restrito ao corpus congelado; W7 ampliou casos de controle e performance, mas continuou sem cobertura real ampla de referência auxiliar. A decisão de governança que faltou foi uma qualificação transversal obrigatória, não simplesmente mais testes unitários.

## Performance também precisa de realismo

W6-R1 corrigiu OOM de cold DAG e melhorou cadeia profunda, mas o live DAG12 ainda gera 8.191 contextos/20.507 blocos. W7 distinguiu latência ANTLR (exemplo ~62 s frontend) de lower (~0,915 s) e preservou timeout anterior: distinção correta, insuficiente para provar escala do CardDemo. F7 agora mostra 23.873 sequences num programa real antes de validação. Medir apenas tamanho de corpo ou cold/dead code premia a otimização que não cobre todos os caminhos vivos.

## Novo protocolo de fechamento

1. Discovery real corpus primeiro, com pins, configuração e first-loss.
2. Witness real representativo e CFG/outcomes derivados da linguagem.
3. Mínimo derivado que preserve o mecanismo, mantendo o real como regressão permanente.
4. Implementação com autoridade/prova explícita.
5. Metamórficos de contexto, verbos terminais, gaps e reference closure.
6. Corpus sintético qualificado e negativos históricos inalterados.
7. CardDemo 73 FULL, sem whitelist, com budgets estáveis.
8. Redistribuição de first-loss e Gate B por sites, sem celebrar só arquivos existentes.
9. Somente então fechar wave; toda limitação/queda real deve estar visível na revisão.

CardDemo é corpus de viabilidade, não oracle absoluto de semântica. Mais candidates ou comportamento antigo não equivalem a correção. O pós-D0 precisa também de futuro corpus corporativo autorizado para testar generalização; isso não foi executado aqui.
''')
put('D0_CARDEMO_GATE_PROPOSAL.md','''# D0 — proposta de gate permanente CardDemo

Esta proposta não altera gates/roadmaps atuais. Corpus físico congelado de 73 (44 checkout + 29 ZIP), mesmo pin, inclusive .cl2. Cada linha possui source SHA, container/member SHA quando aplicável, configuração explícita de dialect/normalização/profile/copy precedence, pins dos cinco repos e hashes de runtime/output.

## Gate A — Pipeline Viability

Executar SOURCE → frontend/SP → lower/AIR → AIR validation → CFG → dataflow/dependency em todos os 73. Por estágio: status, código/diagnóstico de primeira perda, tempo, pico de memória quando medido, schema/capabilities, output hash e trilha de provenance. “Arquivo existe” não é PASS.

Exigir decodificação completa e consistência de identidade/referências; zero INVALID_IR, traversal estrutural completo e nenhum cutoff oculto. Obrigações semânticas que o contrato permite deixar explícitas podem manter PARTIAL; validação interrompida por budget/capability não pode ser rebatizada como sucesso estrutural. CFG e dependency precisam ser decodificáveis, válidos no contrato e associados à mesma publicação; remainders não desaparecem por sucesso do processo.

Métrica principal: `reach_dependency_valid / 73`, com breakdown PARTIAL/complete/blocked/timeout e first-loss por família/mecanismo. Baseline **4/73 PARTIAL**. Nova wave deve preservar os quatro e medir quantos primeiros blockers removeu, quantos seguintes revelou e quais perdas surgiram. Meta final: todos os casos estruturalmente admissíveis atravessam, com PARTIAL honesto.

Antes de tornar 73/73 um gate normativo de sucesso, registrar para cada caso possível invalid COBOL/dialeto fora/missing input obrigatório/corrupted archive. Exclusão de sucesso só com evidência e aprovação humana, sem retirar a linha dos 73 físicos. D0 não pediu nem concedeu exclusões. Missing COPY por si só não prova impossibilidade de SP/AIR/CFG parcial.

## Gate B — Semantic Quality

| Dimensão | Exigência por site/oracle |
| --- | --- |
| required candidates | cada target justificado preservado com suporte e provenance |
| forbidden candidates | nenhum target introduzido por cross-return, fallthrough incorreto ou merge de contexto |
| dead code | ausência de candidate só quando controle e bounds justificam; unknown não vira unreachable |
| support | distinguir observação fonte, modelo, interpretação e evidência de valor/controle |
| provenance | statement/copy/edge/transfer/context chain resolvível ao input autenticado |
| remainders | source/model/effective/control mantidos; melhoria de uma dimensão não limpa todas |
| storage/effects | alias e kill/overwrite corretos; valores unknown não conservam literais indevidos |
| integrity | todos os edges/tags auxiliares no inventário e materializados no contexto correto |

Oracle de linguagem/manual para witnesses reais; não transformar output de hoje em golden verdadeiro por conveniência. Um caso pode passar A e falhar B. Publicar ambos. Não premiar aumento bruto de candidates.

## Ordem, determinismo e custo

Cada wave arquitetural: witnesses mínimos + reais → metamórficos → sintéticos qualificados → **CardDemo 73 FULL** → redistribuição de first-loss → revisão. Gates de repo/campanha obrigatórios continuam vigentes. Reusar hashes/evidência somente quando as fronteiras não foram invalidadas; o full CardDemo é obrigatório após implementação arquitetural porque a proposta o estabelece expressamente.

Repetições de determinismo por amostra adversarial: inventário físico permutado, callers iguais/diferentes, THRU versus single, ordinary entry, terminal CALL/SET/EXIT/FILE, dead handler e live DAG. Preservar todos os logs/raws, inclusive timeout/OOM. Budgets por estágio são parte da configuração; mudança de budget exige justificativa de desempenho e não resolve falha semântica. Casos grandes COACTUPC/CBACT04C e DAG vivo devem ter métricas nodes/edges/contextos antes/depois, não só tempo final.

Falha nova de integridade/negative oracle impede fechamento. Redução de blockers sem melhoria de B é viabilidade parcial, não prova de qualidade. Um plateau deve disparar discovery, não alteração de oracles para voltar ao verde.
''')
put('D0_RECOMMENDATION.md','''# D0 — recomendação arquitetural

**Architectural decision: C — topologia de controle com autoridade no frontend, transportada pelo SP.** Preservar inicialmente o backend AIR existente como estratégia de migração, com limites explícitos de especialização. Não escolher B antes de demonstrar matching/custo no CFG/dataflow. W8 continua PAUSED e W9 NOT STARTED.

## Root causes

1. **Conclusão de região foi representada de forma desigual entre classes de statement.** AST já distingue local/ordinary; projector mistura ordinary no normal de OBSERVED; frontier global/conditional é consumida como successor exclusivo pelo lower. Isso explica todos os 46 F2 e a contradição análoga de F3, sem exigir conhecimento integral de valores.
2. **A autoridade dos edges está dividida.** Resolver/AST/semantics/projector/admission/assembler reconciliam parte da mesma linguagem; FILE inventaria referências por canal separado da closure. F7 é prova de integridade desse segundo problema, não um gap aceitável.
3. **As premissas de prova têm granularidade insuficiente.** inputComplete(unit), environment/layout e structureProven propagam perda a fatos locais. F5/F6 não justificam zerar todos os fatos independentes. Profile físico é necessidade real para certos fatos, não para todos.
4. **A campanha mediu responsabilidade local e oracles estreitos, sem viabilidade transversal obrigatória.** A amostra histórica mostra oito regressões de pipeline já visíveis até W5; controles anteriormente opacos passaram a tipados e revelaram contradições, mas o corpus qualificado não capturou essas combinações.

## Hipóteses tentadas, evidência e resultado

| Hipótese | Resultado D0 | Evidência/refutação |
| --- | --- | --- |
| H1 Positive Memory correta, controle global incompleto | SUPPORTED com correção | não há literalmente um único mapa AST; há re-colapso no contrato/consumer. CBACT01C e COACTUPC mostram dois outcomes legítimos |
| H2 skeleton insuficiente é central | SUPPORTED | 47 first blockers de controle e 60 labels de handlers em duas variantes; topology independe de ADD/DLI runtime |
| H3 precisa COBOL quase integral | NOT ESTABLISHED; refutada como pré-requisito para estes blockers | outputs PARTIAL atravessam; falta de valor não explica fronteiras. Precisão corporativa universal não foi provada |
| H4 autoridade duplicada é problema | SUPPORTED | cinco lugares reinterpretam completion/ordinary; FILE closure usa inventário diferente do emissor |
| H5 activation specialization é abstração errada | PARTIALLY | não é semanticamente inválida por definição; matching passa nos sintéticos. Live DAG e CBACT04C impedem certificação de escala geral |
| H6 granularidade de partiality | SUPPORTED | INPUT_MISSING bloqueia WS-PGMNAME; F6 level SQL derruba seção inteira; K4 perde EVALUATE frontier |
| H7 é principalmente processo, não arquitetura | PARTIALLY, explicação exclusiva refutada | processo deixou escapar falhas concretas de contrato/integridade; mais testes detectariam cedo, mas não substituem correção da abstração |

## Why

CBACT01C exige EXIT158→44 em execução ordinária e →90 no PERFORM89. COACTUPC exige SET1284→fronteira intermediária→1312 e EXIT1312→859 no THRU858; sua condição DFHAID pode continuar unknown. CBEXPORT exige nested returns sem clonar semântica COBOL em cada consumer. CBPAUP0C exige saída de WHEN para fim do EVALUATE/parágrafo, não sibling. CBACT04C exige que o mesmo inventário que emite edges contenha handlers em cada contexto.

C concentra a interpretação no estágio que já possui a gramática, a estrutura AST e a proveniência disponível, inclusive suas lacunas explícitas. Lower traduz regras tipadas de região/contexto; não redescobre significado de parágrafo. Isso elimina duplicação demonstrada. Não é argumento de elegância: são mecanismos observados em fonte e executáveis reais.

A corrigida poderia expressar os mesmos fatos, mas conservar whitelists/controle por campo repetiria o erro; sua sustentabilidade operacional ainda falta. B expressa matching e ordinary defaults de modo promissor, mas consumer CFG atual não suporta control.local e custo total é UNKNOWN. C não promete resolver cloning: esse é um gate separado de backend.

## What we should stop doing

- Adicionar exceção de completion por verbo ou relaxar a admissão de successors externos.
- Tratar whole-unit completeness como premissa implícita de todo fato local.
- Fazer lower reconstruir fallthrough/arm completion a partir de campos heterogêneos.
- Manter inventários separados de referências que o assembler cria e de alvos que a closure materializa.
- Fechar wave arquitetural apenas por corpus sintético/green CI ou por queda de um first-loss.
- Usar “mais candidates”, saída antiga ou arquivos existentes como oracle de correção.

## What we should preserve

Positive memory e fatos positivos independentes; gaps/remainders honestos; provenance tipada; contratos versionados; validator estrito; negativos de cross-return/dead code/overwrite; pins, hashes, raw logs e baselines; inventário físico de 73; distinção entre viabilidade PARTIAL e suporte completo.

## Migration path

1. Revisão humana de D0 e contrato de region outcomes/reference inventory, com os quatro modelos de A/B/C e os 14 witnesses reais. D0 termina aqui.
2. Futura wave R1 produz ControlTopology versionada, separando skeleton de valores; lower consome o grafo tipado por um backend de tradução genérico. Decoder legado conserva semântica anterior. Todos os tipos de edges emitidos participam da closure, incluindo FILE.
3. Qualificar A-backend transitório em shared DAG vivo, COACTUPC e CBACT04C; se budgets/contextos inviáveis, registrar limite e bloquear fechamento dessa dimensão. Abrir estudo de B somente com contrato/solver/oracles e orçamento próprios; não introduzir stack sem consumer.
4. Fact dependencies e storage unknown por região, com inputs autênticos/configurações explícitas e prova de isolamento. L0 normalization policy separada.
5. Toda wave executa CardDemo 73 FULL e Gate B, redistribui blockers e só então fecha.

## Expected CardDemo effect

Mecanismo de região/contexto tem alvo direto **F2 46 + F3 1**; reference closure inclui **F7 2** quando F4 for ultrapassado. Nenhuma dessas contas é promessa de chegar a 51/73 ou 53/73: resumes inline, effects, storage e budgets seguintes podem bloquear. Provas locais atacam F5 12 + F6 1 e a porção arquitetural de F4 7; configuração autêntica trata sua outra porção. Normalização trata F1 2. F7 continua sobreposição, sem inflar o denominador.

## Respostas finais obrigatórias

**Nossa abordagem de não modelar tudo está errada? PARTIALLY.** NO como filosofia para valores/effects/runtime exato; PARTIALLY para cobertura de controle e granularidade storage: skeleton/outcomes e referência de targets precisam de cobertura muito maior e integridade total do que é publicado. Unknown é permitido; contradição e AIR inválida não são partiality.

**Construir um control graph mais próximo do COBOL antes de valores? YES**, finito, parcial e tipado, sem emulação completa. Benefício: uma autoridade de outcomes/regions e bounds; custo: migração SP/frontend/lower e validação/escala. L0 correto; L1 alta cobertura; L2 parcial localizada com alias proof; L3/L4 parciais com effects/remainders; L5 não necessário.

Não implementar ainda: fixes dos sete erros, nova capability local no CFG, mudança de oracle/validator, stubs/copybooks artificiais, profile global automático ou tab policy implícita. Não retomar W8/W9 nem mudar PRs. **STOPPED BEFORE CODE: YES.**
''')
put('D0_ROADMAP_PROPOSAL.md','''# D0 — proposta de roadmap por mecanismo

Proposta separada; nenhum roadmap canônico foi atualizado. W8 PAUSED; W9 NOT STARTED. Base física: 73; viabilidade canônica: **4/73 PARTIAL**. Intervalos abaixo são hipóteses de planejamento sobre **remoção do primeiro blocker atual**, não resultados executados, probabilidades ou soma de futuros PASS.

| Futura wave | Mecanismo/escopo | Alvos reais | Efeito esperado condicionado ao escopo completo | Gate de saída |
| --- | --- | --- | --- | --- |
| R1 CONTROL_TOPOLOGY_AUTHORITY | outcomes/region boundaries, ordinary defaults, contextual return, arm/handler reference inventory; SP versionado; lower genérico | F2 46, F3 1; F7 quando exposto | cerca de 46–47 primeiros blockers F2/F3; até 2 F7 sobrepostos; próximos blockers desconhecidos | 14 reais + mínimos derivados + metamórficos + sintéticos + full73 A/B; zero dangling/cross-return; orçamento real |
| R2 FACT_DEPENDENCY_LOCALITY | depends_on/proof scope, declaração/região unknown, storage identity/alias e views independentes; input autenticado separado | F5 12, F6 1, parte F4 | cerca de 12–13 primeiros blockers F5/F6 se isolamento for provado; F4 arquitetural limitado pelos tipos físicos | missing input muda só fatos dependentes; negativos de alias, include com estrutura, scope aninhado; full73 |
| R3 EXPLICIT_SOURCE_ENVIRONMENT | normalização HT por arquivo, dialect/import/profile manifest por variante, source maps | F1 2, configuração F4 7 | até 2 F1 e 5–7 primeiros F4, condicionado a policy/profile justificados; F7/outros podem emergir | bytes originais intactos, provenance exata, negativos de colunas e perfil; full73 |
| R4 CONTEXT_EXECUTION_BACKEND (condicional) | se R1 falhar escala, comparar specialization bounded versus control.local/summaries com CFG matching | limites de escala/contexto, não uma família artificial nova | sem estimativa de programas; redução de contexts/IR size medida, sem perder candidates/negativos | shared live DAG, deep chain, callers/ranges reais, recursion limits, matching e budgets; full73 |

R1 é a **próxima wave de implementação recomendada**, somente depois da revisão D0. Seu contrato deve fazer F2/F3 e handlers participarem da mesma topologia; não implementar primeiro 46 patches por fonte. A porção de input completeness que apaga fronteiras sintáticas em K4 faz parte da separação de skeleton na R1; R2 aprofunda dependências de storage/value/effect. Isso evita depender de implementar semântica física completa para iniciar R1.

Os intervalos não são um compromisso de PASS. Mesmo remover o primeiro erro de todos os 47 pode deixar a viabilidade final em 4 se aparecerem outros bloqueios; relatar isso honestamente. Um lower que agora produza AIR inválida é uma regressão de integridade, não avanço celebrável. Não somar 4 + 47 + 13 + 7 + 2 nem contar F7 duas vezes.

Na revisão de cada wave, publicar matriz física 73, distribuição antes/depois, raízes causais remanescentes, limites de performance e gates A/B separados. Se algum caso precisar de exclusão normativa de sucesso, trazer evidência e aprovação humana mantendo-o na matriz. D0 não autoriza exclusões.

R4 não é pré-aprovação de B: CFG atual não a suporta. Se A transitório for suficiente dentro de limites explícitos e realistas, R4 pode permanecer pesquisa; se não for, o fechamento de escalabilidade de R1 deve ser qualificado, nunca mascarado por timeout/heap maior.
''')
put('D0_EXECUTIVE_SUMMARY.md','''# CARDEMO_ARCHITECTURAL_REASSESSMENT_D0_READY_FOR_REVIEW

**Recomendação: C — topologia de controle produzida pelo frontend, com outcomes/regions tipados no SP e tradução genérica no lower.** Preservar positive memory; localizar dependências de prova; instituir CardDemo completo como gate de viabilidade separado da qualidade semântica. Sem implementação.

Baseline canônico CURRENT_W8_WORKTREE_STATE: **4/73 chegaram a dependency, todos PARTIAL; 69/73 não chegaram**. SP: 71 PARTIAL + 2 BLOCKED. Pins W7-R1 aprovados e estado W8 estão separados em D0_BASELINE. Corpus `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`, 44 fontes checkout + 29 ZIP incluindo .cl2. Nenhuma exclusão.

## Achados decisivos

- **F2, 46 programas:** o AST conhece local continuation e ordinary fallthrough, mas o projector põe ordinary em normalContinuation de OBSERVED e o lower o interpreta como controle explícito incompatível com completion. Frontier às vezes é omitida por prova global de input/EVALUATE ou por composição incompleta de handler FILE/IF implícito. Quatro clusters sobrepostos: EXIT raiz 28; outros sequenciais raiz 8; frontier composta 17; frontier não publicada 32. Análise dos 46, 1.512 sites, 1.964 pares site/parágrafo; 14 witnesses reais/24 fronteiras aprofundadas.
- **F3: SAME_MECHANISM.** Em CBPAUP0C, targets externos são EXITs de parágrafos seguintes, não sibling arms. O pai e seus filhos usam meanings diferentes de normalContinuation.
- **F4: MIXED.** Profile explícito leva cinco casos adicionais a dependency PARTIAL e revela F7 em dois. JCL indica IBM V6.3; 6.4/1047 para todo checkout+UniKix não foi autenticado. A ausência de profile não justifica descartar conhecimento lógico independente.
- **F5:** inputComplete(unit) amplia o impacto de DFHAID/DFHBMSCA ausentes sobre storage/controle local. Não inventar conteúdo dos COPYs; preservar fatos cuja independência for provada.
- **F6:** SQL INCLUDE vira DataEntry level SQL; nível −1 reprova roots da seção e causa SECTION_NOT_PROVEN global. SQLCA/AUTHFRDS são relevantes pela expansão/tipagem ausente, não pelo status nominal em si.
- **F1:** duas manifestações da falta de policy de colunas HT: rejeição em conteúdo e contagem errada no prefixo de COPY. Normalização por arquivo antes de COPY, com stops declarados, é o mecanismo a definir.
- **F7:** 30 I-02 por variante CBACT04C, todos handlers INVALID KEY de três READs omitidos da closure de ativação. Não é pruning por demanda; FILE usa scheduling eager. 60 referências mapeadas individualmente.

A amostra histórica de dez programas mostra 9/10 chegando a dependency PARTIAL antes da campanha e 1/10 desde W5; oito regressões de viabilidade que o gate real teria detectado. Os produtos antigos tinham PERFORMs OBSERVED: não são oracle de correção. Não extrapolar esses números aos 73 históricos não executados.

## Decisão e limites

A corrigida é expressável, mas conserva risco de duplicação e não demonstrou escala: live DAG12 gera 8.191 contextos/20.507 blocos para 30 statements; CBACT04C real já monta 23.873 sequences inválidas. B é promissora no contrato control.local, mas **UNSUPPORTED no CFG atual**; custo/precisão do solver são UNKNOWN. C elimina duplicação demonstrada e pode migrar sem nova AIR inicialmente, mas herda o custo do backend escolhido e precisa gates de escala.

**Não modelar tudo está errado? PARTIALLY.** Values/effects podem continuar parciais; controle precisa cobertura muito alta e todos os edges publicados precisam ser íntegros. Storage pode ser parcial por região, com alias/identity e provas explícitas. Runtime exato não é necessário. Construir topology fonte antes de precisão de valores: **YES**, sem emulador COBOL.

Próxima wave proposta: CONTROL_TOPOLOGY_AUTHORITY, para remover o primeiro mecanismo F2/F3 e reference-closure F7 quando exposto. Não prometer `4 + 47 = 51 PASS`; novos blockers podem aparecer. Seguem fact-dependency locality e configuração/normalização explícitas, com full73 e quality gates a cada wave.

## Estado de entrega

Product repos modified: **NO**. PRs modified: **NO**. Corpus, fixtures e oracles preservados. W8 **PAUSED**; W9 **NOT STARTED**. **STOPPED BEFORE CODE: YES**.

Começar por [recomendação](D0_RECOMMENDATION.md), [F2 deep dive](D0_F2_DEEP_DIVE.md), [scorecard](D0_ARCHITECTURE_SCORECARD.md), [postmortem](D0_PROCESS_POSTMORTEM.md) e [índice de evidência](D0_EVIDENCE_INDEX.md). Limitações e decisões hipotéticas permanecem explícitas; READY_FOR_REVIEW não significa aprovação da futura implementação.
''')
# Correct and augment conclusions after all report bodies are available.
for file in D.glob('D0_*.md'):
 text=file.read_text();text=text.replace('entry=s100','entry=s102').replace('linha 250','linha 251').replace('linha 433','linha 432');file.write_text(text)
# Concrete surviving program-dependency facts in every F5, distinct from generic CICS surfaces.
site_rows=[]
for unit in blast:
 if 'F5' not in unit['family']:continue
 facts=[]
 for s in unit['dependencyStatements']:
  if s['variant'] not in ['CICS_PROGRAM_CONTROL','CALL']:continue
  t=s['surface'].get('target');desc='target unavailable'
  if t:
   if t.get('kind')=='LITERAL':desc='literal '+str(t.get('text',t.get('writtenText')))
   elif t.get('kind')=='DATA':desc='data '+str(t.get('reference',{}).get('binding',{}).get('selected'))
   else:desc=str(t.get('kind'))
  facts.append(f"{s['id']}@{s['source']['startLine']}: {desc}")
 site_rows.append([unit['program'],'; '.join(facts)])
f=D/'D0_COMPLETENESS_BLAST_RADIUS.md';f.write_text(f.read_text()+'''\n## Contraprova à tese de apagamento absoluto\n\nO próprio produto preserva alguns fatos positivos: COSGN00C publica XCTL com literais COADM01C e COMEN01C, apesar de INPUT_MISSING. Em COADM01C o target CDEMO-TO-PROGRAM permanece resolvido como data:17. Isso mostra que partiality local já é possível, embora o gate de storage impeça sua chegada ao fim. O uso de inputComplete no CicsProgramControlAnalyzer é restrito à prova opcional NEW_LOGICAL_LEVEL; não deve ser descrito como supressão de todos os fatos CICS e não é o modo ativo no run canônico.\n\nA tabela distingue o fato de target preservado de sua alcançabilidade/valor computado ainda não provados. Nos alvos unavailable, a expressão escrita continua inspecionável, mas nenhum target conhecido é inventado.\n\n'''+table(['Programa físico F5','Sites tipados e target preservado no SP'],site_rows))
