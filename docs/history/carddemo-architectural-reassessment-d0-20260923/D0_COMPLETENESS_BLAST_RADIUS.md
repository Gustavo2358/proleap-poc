# D0 — completeness blast radius e fact-level dependencies

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

## Contraprova à tese de apagamento absoluto

O próprio produto preserva alguns fatos positivos: COSGN00C publica XCTL com literais COADM01C e COMEN01C, apesar de INPUT_MISSING. Em COADM01C o target CDEMO-TO-PROGRAM permanece resolvido como data:17. Isso mostra que partiality local já é possível, embora o gate de storage impeça sua chegada ao fim. O uso de inputComplete no CicsProgramControlAnalyzer é restrito à prova opcional NEW_LOGICAL_LEVEL; não deve ser descrito como supressão de todos os fatos CICS e não é o modo ativo no run canônico.

A tabela distingue o fato de target preservado de sua alcançabilidade/valor computado ainda não provados. Nos alvos unavailable, a expressão escrita continua inspecionável, mas nenhum target conhecido é inventado.

| Programa físico F5 | Sites tipados e target preservado no SP |
| --- | --- |
| app/cbl/COADM01C.cbl | statement:65@145: target unavailable; statement:68@168: data data:17 |
| app/cbl/COMEN01C.cbl | statement:77@156: target unavailable; statement:80@184: target unavailable; statement:82@201: data data:17 |
| app/cbl/COSGN00C.cbl | statement:66@231: literal COADM01C; statement:67@236: literal COMEN01C |
| app/cbl/COUSR01C.cbl | statement:81@175: data data:13 |
| app/cbl/COUSR02C.cbl | statement:128@258: data data:14 |
| app/cbl/COUSR03C.cbl | statement:94@205: data data:14 |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COADM01C.cl2 | statement:62@142: target unavailable; statement:65@165: data data:17 |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COMEN01C.cl2 | statement:69@152: target unavailable; statement:72@175: data data:17 |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COSGN00C.cl2 | statement:66@231: literal COADM01C; statement:67@236: literal COMEN01C |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COUSR01C.cl2 | statement:81@175: data data:13 |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COUSR02C.cl2 | statement:128@258: data data:14 |
| samples/m2/unikix/UniKix_CardDemo_runtime_v1.zip!/migrated_app/cbl/COUSR03C.cl2 | statement:94@205: data data:14 |
