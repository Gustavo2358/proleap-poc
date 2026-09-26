# D0 — matriz de responsabilidade

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
