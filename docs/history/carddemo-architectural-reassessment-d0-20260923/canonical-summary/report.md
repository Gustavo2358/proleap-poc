# Validação dos programas CardDemo — 2026-09-23

## Escopo e resultado

Foi executado **um attempt por cada uma das 73 fontes** descobertas no CardDemo público fixado em `aws-samples/aws-mainframe-modernization-carddemo@59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`: 44 arquivos do checkout e 29 membros de ZIP, incluindo `.cl2`. O runner descobriu sem whitelist, não alterou fontes ou copybooks, extraiu membros do ZIP byte a byte e verificou novamente os snapshots limpos ao fim. Não houve timeout, exceção do runner ou programa omitido. Tempo observado da rodada sequencial: 240,895 s; timeout operacional de 120 s por etapa; Java 21, `-Xmx2g`.

A validação aqui é da cadeia **fonte → Semantic Product (SP) → AIR → CFG → dependency**. Ela não compila/executa os programas COBOL em um mainframe, nem certifica equivalência de comportamento. Os pins exatos das cinco partes da cadeia e o SHA da configuração de runtime estão em [measurements.json](full/measurements.json). A [matriz por fonte](classification.csv) contém caminho lógico (inclusive variante do ZIP), SHA da fonte, primeira família bloqueadora, exit codes, estados por etapa e logs brutos relacionados.

| Etapa | `PARTIAL` com produto | `BLOCKED` | Não alcançada |
| --- | ---: | ---: | ---: |
| Frontend/SP | 71 | 2 | 0 |
| Lower/AIR | 4 | 67 | 2 |
| CFG | 4 | 0 | 69 |
| Dependency | 4 | 0 | 69 |

Os quatro que chegaram ao fim foram `CBSTM03B.CBL` (checkout), `COBSWAIT.cbl`, `CBSTM03B.cbl` (ZIP) e `SDSF.cbl` (ZIP). **Nenhum produto final foi classificado como semanticamente completo**; os quatro têm lacunas explícitas. `BLOCKED` é recusa do produto, não falha de execução COBOL.

## Famílias de problemas

Há **seis famílias de primeiro bloqueio**, distribuídas sem sobreposição nos 69 programas que não chegaram a dependency. Um probe controlado revelou uma **sétima família downstream**, latente em duas variantes já contadas na família F4. Portanto, não se deve somar 2 aos 69 bloqueados.

| Família | Programas | Primeira etapa | Erro observado e causa provável / direção de correção |
| --- | ---: | --- | --- |
| **F1 — tab em fixed-format** | **2** | Frontend | `COTRTLIC.cbl`: `Unsupported tab ... offset 75337` na normalização da fonte. `CBSTM03A.cbl` do ZIP: `Unsupported fixed-format indicator '0' ... line 6, column 7` ao normalizar o copybook `CUSTREC`. A contagem de colunas de `HT` físico não tem uma política estável para fonte e COPY; definir a política do dialeto e preservar provenance. O [relatório focal anterior](../carddemo-blockers-20260914/report.md) contém witnesses mínimos, e a rodada atual reproduz os dois erros. |
| **F2 — fronteira de parágrafo em `PERFORM`** | **46** | Lower | `INVALID_INPUT` com `normal completion cannot override explicit control` e/ou `intrinsic normal edge stays in paragraph`; cinco chegam a `IMPLEMENTATION_LIMIT` pelo volume de diagnósticos, com as mesmas violações estruturais. Em `CBACT01C.cbl`, o `PERFORM` `statement:89` publica `statement:158` como completion do parágrafo, mas esse membro também tem `normalContinuation` para `statement:44`, fora do parágrafo. Há incompatibilidade entre continuação ordinária e retorno dependente da ativação de `PERFORM`. Investigar o contrato e a projeção/admissão de fluxos contextuais, preservando ambas as possibilidades somente quando comprovadas. Não é seguro simplesmente apagar o edge. |
| **F3 — fronteira de braço em `EVALUATE`** | **1** | Lower | `CBPAUP0C.cbl`: `normal arm completion cannot enter a sibling arm` nos `statement:62`, `:71`, `:72`. O `statement:62` tem continuação para `statement:66` fora do braço, enquanto o `EVALUATE` pai publica continuação indisponível. Ajustar prova/publicação das saídas de braço ou a admissão desse estado parcial. A mensagem do lower diz “sibling arm”; o witness mostra passagem para fora do braço, e a causa exata no produtor permanece a confirmar. |
| **F4 — perfil físico de storage não selecionado** | **7** | Lower | `PROFILE_FACT: supported logical value has an unrepresentable partial storage relation`; nesses sete SP, o único gap de storage é `PROFILE_NOT_SELECTED`. Contraprova com `--storage-profile ibm-enterprise-6.4-fixed-display-1047@1`: cinco passaram a produzir AIR/CFG/dependency (`PARTIAL`); as duas variantes de `CBACT04C` chegaram ao erro F7. Selecionar perfil de forma explícita removeu a primeira recusa nos sete; a escolha de perfil precisa ser justificada pela configuração real do corpus. |
| **F5 — input de storage incompleto** | **12** | Lower | A mesma recusa `PROFILE_FACT`, mas com `INPUT_MISSING` no SP e ocorrências `DFHAID`/`DFHBMSCA` não resolvidas. Em `COADM01C.cbl`, selecionar o perfil removeu `PROFILE_NOT_SELECTED`, mas `INPUT_MISSING` e a recusa persistiram. Fornecer as dependências autênticas e validar a prova de layout/entrada; não usar stubs. Isso é hipótese de correção, ainda não uma demonstração de que todos os 12 passariam. |
| **F6 — estrutura da seção não provada** | **1** | Lower | `COPAUS2C.cbl`: `PROFILE_FACT` e `SECTION_NOT_PROVEN`. O perfil IBM explícito removeu `PROFILE_NOT_SELECTED`, mas não a recusa. O source tem `EXEC SQL INCLUDE SQLCA` e `AUTHFRDS` não resolvidos dentro da área de dados; isso pode afetar a estrutura da seção, mas a relação causal exata ainda exige investigação de preprocessing/AST/storage. |
| **F7 — rótulos AIR pendentes, latente** | **2** | Validação AIR após probe F4 | As variantes de `CBACT04C` são admitidas após selecionar o perfil, mas o lower devolve `OUTPUT_INVALID`. Um probe pela API real de `AirValidator` encontrou **30 `INVALID_IR`, regra I-02, `dangling reference: LabelId`** em cada variante. Referências de operações emitidas apontam para rótulos ausentes; investigar materialização de labels/continuations no lower. O CLI imprime apenas `OUTPUT_INVALID`, então o diagnóstico detalhado foi extraído sem modificar o produto. Esta família se sobrepõe aos sete F4 e não foi o primeiro bloqueio da rodada canônica. |

Os **67 lower exits foram código 4**: 42 `INVALID_INPUT`, 20 `BLOCKED_LOWERING` por relação de storage e cinco `IMPLEMENTATION_LIMIT` com diagnóstico estrutural da F2. As duas falhas de frontend tiveram exit 1. Os quatro que produziram AIR/CFG/dependency tiveram exit 0 nas etapas alcançadas e permaneceram `PARTIAL`.

## Lacunas transversais dos 71 SP publicados

- `entryInventory` é `INPUT_MISSING` em **40** e `PARTIAL` em **31**; todos os 71 registram `ALTERNATE_ENTRIES_NOT_PROJECTED`. Isso é limitação de cobertura publicada, não uma sétima família de crash ou recusa imediata.
- **42 programas** têm **101 ocorrências** de dependência `UNRESOLVED` (sobretudo `DFHAID` e `DFHBMSCA`, 37 cada). Os 12 F5 são um subconjunto: outras fontes com COPY ausente podem parar primeiro por F2 ou continuar com lacunas. `NOT_APPLICABLE` de includes SQL foi separado de `UNRESOLVED` na contagem.
- Os 71 SP contêm 15.973 statements observados: 598 modeled, 11.149 partial e 4.226 unsupported. Frequência de gap no corpus não prova uma única correção de capability, então esses estados não foram falsamente agrupados como uma família de fix.

## Contraprovas e limites da inferência

- [Probes dos sete F4](profile-pure-7/selection.json): cinco chegam a dependency; os dois `CBACT04C` revelam F7. Seus `case-*.log`, `measurement.json`, stdout/stderr e produtos permanecem preservados.
- [Probe F5](profile-input-probe/measurements.json): `COADM01C.cbl` segue bloqueado com `INPUT_MISSING` após selecionar o perfil.
- [Probe F6](profile-probe/measurements.json): `COPAUS2C.cbl` segue bloqueado com `SECTION_NOT_PROVEN` após selecionar o perfil.
- [Diagnóstico AIR F7](profile-pure-7/case-03.air-invalid.log): o `AirValidator` real confirmou 30 violações I-02; [mapeamento aos statements](profile-pure-7/case-03.air-invalid-mapped.log) mostra origens `statement:215`, `:222`, `:229`. A variante ZIP tem o mesmo total e a mesma regra no [log correspondente](profile-pure-7/case-05.air-invalid.log).
- F2, F3 e F6 são incompatibilidades/gaps reais observados, mas os componentes exatos a alterar e a regra semântica final devem ser confirmados antes de corrigir. Nenhuma evidência foi ajustada para transformar `PARTIAL` ou recusa em PASS.

## Provenance e reprodução

A rodada canônica está em [full/](full/): `discovery.json`, `measurements.json`, `archive-members/`, `programs/<path>/measurement.json`, produtos e stdout/stderr por etapa. A matriz e as contagens são reproduzidas por `python3 -B summarize.py` e verificam exaustividade e famílias esperadas. [summary.json](summary.json) é a agregação legível por máquina. [commands.md](commands.md) registra os comandos. [sha256sums.txt](sha256sums.txt) guarda hashes dos arquivos regulares da evidência. A versão final de `run_carddemo.py` ganhou, **depois da rodada canônica**, apenas a opção `--frontend-arg` usada nos probes; com lista vazia, seu fluxo de execução permanece o da rodada completa.

Não houve mudança nos cinco repositórios de produto nem no source CardDemo. Este relatório é uma nova evidência local; não altera a baseline histórica ou o roadmap.
