# D0 — índice de evidência

Pacote de investigação somente leitura. Data nominal solicitada: 2026-09-23; timestamps reais dos probes são preservados. Não há commit/PR ou implementação de produto.

## Roteiro de revisão

1. [Resumo executivo](D0_EXECUTIVE_SUMMARY.md) → [recomendação](D0_RECOMMENDATION.md).
2. [F2 deep dive](D0_F2_DEEP_DIVE.md), matriz completa e dossiê dos 14 witnesses.
3. F3/F7 e storage para verificar que a decisão abrange os mecanismos distintos.
4. Arquiteturas, scorecard, processo/gates/roadmap.

## Entregáveis

- [D0_ARCHITECTURE_A.md](D0_ARCHITECTURE_A.md)
- [D0_ARCHITECTURE_B.md](D0_ARCHITECTURE_B.md)
- [D0_ARCHITECTURE_C.md](D0_ARCHITECTURE_C.md)
- [D0_ARCHITECTURE_SCORECARD.md](D0_ARCHITECTURE_SCORECARD.md)
- [D0_BASELINE.md](D0_BASELINE.md)
- [D0_CARDEMO_GATE_PROPOSAL.md](D0_CARDEMO_GATE_PROPOSAL.md)
- [D0_COMPLETENESS_BLAST_RADIUS.md](D0_COMPLETENESS_BLAST_RADIUS.md)
- [D0_EVIDENCE_INDEX.md](D0_EVIDENCE_INDEX.md)
- [D0_EXECUTIVE_SUMMARY.md](D0_EXECUTIVE_SUMMARY.md)
- [D0_F1_FRONTEND.md](D0_F1_FRONTEND.md)
- [D0_F2_DEEP_DIVE.md](D0_F2_DEEP_DIVE.md)
- [D0_F3_COMPARISON.md](D0_F3_COMPARISON.md)
- [D0_F7_IR_INTEGRITY.md](D0_F7_IR_INTEGRITY.md)
- [D0_FAILURE_TAXONOMY.md](D0_FAILURE_TAXONOMY.md)
- [D0_MINIMUM_SEMANTIC_COMPLETENESS.md](D0_MINIMUM_SEMANTIC_COMPLETENESS.md)
- [D0_PROCESS_POSTMORTEM.md](D0_PROCESS_POSTMORTEM.md)
- [D0_RECOMMENDATION.md](D0_RECOMMENDATION.md)
- [D0_RESPONSIBILITY_MATRIX.md](D0_RESPONSIBILITY_MATRIX.md)
- [D0_ROADMAP_PROPOSAL.md](D0_ROADMAP_PROPOSAL.md)
- [D0_STORAGE_PARTIALITY.md](D0_STORAGE_PARTIALITY.md)
- [D0_F2_CLUSTER_MATRIX.csv](D0_F2_CLUSTER_MATRIX.csv)

## Evidence → claim

| Evidência | O que demonstra / limite |
| --- | --- |
| [baseline inicial](probes/baseline/initial.json), [preservação final](probes/baseline/final-preservation.json) | Pins W7-R1/W8 separados; HEAD/branch/status/diff inalterados |
| [hashes canônicos finais](probes/baseline/canonical-final-hash-validation.json) | 4.787 hashes originais preservados; validação inicial adjacente |
| [fontes finais](probes/baseline/final-source-hashes.json) | 73 fontes reais byte-identical; pin corpus verificado |
| [inventário integral](probes/all-programs-analysis.json) | 73 rows; estados/gaps/dependencies; não novo oracle |
| [F2 programs](probes/f2-programs.json), [clusters](probes/f2-clusters.json), [sites](probes/f2-details.json) | 46 programas, 1.512 sites, 2.170 predicados; cinco caps de stderr explícitos |
| [24 fronteiras / 14 programas](probes/f2-witnesses.json), [COBOL lido](probes/f2-source-dossier.md) | source/AST/SP/consumer comparados; expected edges no deep dive são derivações manuais |
| [probe AST](tools/AstControlProbe.java), [runner](tools/run_ast_probe.py), probes/ast-control/ | Relações local e ordinária observadas diretamente no AST canônico; 16 programas; F6 adicional |
| [F3 AST](probes/ast-control/CBPAUP0C.json) | dois EVALUATEs terminais; raw SP/stderr na evidência canônica |
| [F6 roots](probes/ast-control/COPAUS2C-storage.json) | unresolved COPY count=0; SQL roots 70/71 → −1; structureProven=false |
| [blast radius](probes/storage-blast-radius.json), [resolução F5](probes/f5-resolution.json) | declarations, COPY positions, todos os bindings dos 12 F5; não inventa vendor declarations |
| [profile probes](probes/profile-probe-analysis.json) | leitura integral dos 9 cenários auxiliares (7+1+1); 5 produtos adicionais PARTIAL |
| [F1 bytes](probes/f1-raw-bytes.json) | offset HT e prefixo físico CUSTREC; sem editar input |
| [F7 labels](probes/f7-labels.csv), [input facts](probes/f7-input-facts.json) | cada uma das 60 referências e os 11 contextos entry-only sem range |
| [F7 checkout](probes/f7-03.json), [F7 ZIP](probes/f7-05.json), [probe](tools/F7Probe.java) | validator real + registro de IDs observado; fragmento inválido não aceito |
| [commands F7](probes/diagnostic-commands.json) | JVM/classpaths/compile/execution; todos outputs em D0 |
| [histórico runtime/pins](probes/history/builds.json), [60 medições](probes/history/measurements.json), [qualidade](probes/history-quality.json) | dez fontes × seis runtimes; estados antigos PARTIAL não são oracle |
| [22.567 hashes históricos](probes/historical-evidence-hashes.json) | manifests W5/W6/W6-R1/W7/W7-R1 sem divergência |
| [código lido e comparação W7-R1](probes/code-evidence.json) | caminho/hash de cada autoridade causal; projector possui delta W8 preservado |
| [validação da entrega](probes/delivery-validation.json) | contagens, AST joins, 60 labels, F6, links e preservação; não substitui gates de produto |

## Código causal no checkout atual

- [proleap-poc/AstBuilder.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/AstBuilder.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/Ast.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/Ast.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/ProcedurePerformSemantics.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/ProcedurePerformSemantics.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/EvaluateSemantics.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/EvaluateSemantics.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/IfSemantics.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/IfSemantics.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/PerformSemantics.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/PerformSemantics.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/StorageLayoutSemantics.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/StorageLayoutSemantics.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/StorageComponents.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/StorageComponents.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/ScalarMoveSemantics.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/ScalarMoveSemantics.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/ResolutionAnalysisReport.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/ResolutionAnalysisReport.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/CicsProgramControlAnalyzer.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/CicsProgramControlAnalyzer.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/SourceNormalizer.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/SourceNormalizer.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/CopybookLibrary.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/CopybookLibrary.java) — byte-identical ao W7-R1 aprovado.
- [proleap-poc/CobolSemanticProductProjector.java](references/.positive-memory-topology/proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/semanticproduct/projection/CobolSemanticProductProjector.java) — delta W8: consultar diff capturado; não confundir com baseline aprovado.
- [cobol-lower/ProcedurePerformAdmission.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/ProcedurePerformAdmission.java) — byte-identical ao W7-R1 aprovado.
- [cobol-lower/EvaluateAdmission.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/EvaluateAdmission.java) — byte-identical ao W7-R1 aprovado.
- [cobol-lower/PartialProgramAdmission.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/PartialProgramAdmission.java) — delta W8: consultar diff capturado; não confundir com baseline aprovado.
- [cobol-lower/CompositionalPerformAdmission.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/CompositionalPerformAdmission.java) — byte-identical ao W7-R1 aprovado.
- [cobol-lower/PartialProgramAssembler.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/PartialProgramAssembler.java) — byte-identical ao W7-R1 aprovado.
- [cobol-lower/PerformActivationDemand.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/PerformActivationDemand.java) — byte-identical ao W7-R1 aprovado.
- [cobol-lower/FileControlLowering.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/FileControlLowering.java) — byte-identical ao W7-R1 aprovado.
- [cobol-lower/LocalIds.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/LocalIds.java) — byte-identical ao W7-R1 aprovado.
- [cobol-lower/PartialProgramLowerer.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/PartialProgramLowerer.java) — byte-identical ao W7-R1 aprovado.
- [cobol-lower/OutputAssessment.java](references/.positive-memory-topology/cobol-lower/core/src/main/java/io/github/gustavo2358/lower/application/OutputAssessment.java) — byte-identical ao W7-R1 aprovado.

## Evidência histórica e contratos

- [w5/W5_CONTRACT.md](references/.positive-memory-topology/continuacao-w4-w9/evidence/w5/W5_CONTRACT.md).
- [w5/W5_EVIDENCE.md](references/.positive-memory-topology/continuacao-w4-w9/evidence/w5/W5_EVIDENCE.md).
- [w5/W5_NON_PERFORM_AUDIT.md](references/.positive-memory-topology/continuacao-w4-w9/evidence/w5/W5_NON_PERFORM_AUDIT.md).
- [w6/W6_E2E.md](references/.positive-memory-topology/continuacao-w4-w9/evidence/w6/W6_E2E.md).
- [w6/W6_DESIGN_DISCOVERY.md](references/.positive-memory-topology/continuacao-w4-w9/evidence/w6/W6_DESIGN_DISCOVERY.md).
- [w6-r1/W6_R1_DESIGN.md](references/.positive-memory-topology/continuacao-w4-w9/evidence/w6-r1/W6_R1_DESIGN.md).
- [w6-r1/W6_R1_PERFORMANCE.md](references/.positive-memory-topology/continuacao-w4-w9/evidence/w6-r1/W6_R1_PERFORMANCE.md).
- [w7/W7_PERFORMANCE.md](references/.positive-memory-topology/continuacao-w4-w9/evidence/w7/W7_PERFORMANCE.md).
- [w7-r1/W7_R1_DISCOVERY.md](references/.positive-memory-topology/continuacao-w4-w9/evidence/w7-r1/W7_R1_DISCOVERY.md).
- [AIR controle local](references/.positive-memory-topology/analysis-ir/especificacao/05-controle-e-invocacoes.md).
- [CFG capacidade local declarada](references/.positive-memory-topology/analysis-cfg/docs/domain/local-control.md).

## Autoridade externa

- [IBM Enterprise COBOL 6.4 Language Reference](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf): cap.6 pp.55–61; transfer of control p.79; implicit scope terminators p.294; EVALUATE pp.339–342; EXIT pp.342–345 e extensão p.740; PERFORM pp.413–424. PDF oficial preservado em probes/official; texto extraído para busca, sem edição.
- [IBM z/OS expand](https://www.ibm.com/docs/en/zos/2.5.0?topic=descriptions-expand-expand-tabs-spaces) e [IBM Linux source conversion](https://www.ibm.com/docs/en/cobol-linux-x86/1.1.0?topic=scu-source-conversion-utility-options): precedentes de import policy HT, não regra automática Enterprise z/OS para o corpus.

## Reprodutibilidade e limites

Scripts somente sob tools/ e outputs sob probes/. `analyze_corpus.py` inventaria evidência; `enrich_evidence.py` cruza diagnósticos; `run_history.py` recusa sobrescrever output histórico; `verify_history_and_resolution.py` confere manifests; `verify_delivery.py` é auditoria somente leitura dos repos/corpus. `write_reports.py` gera relatórios e `index_evidence.py` este índice. Classes de probe são builds diagnósticos locais, não commits de produto.

SHA256SUMS fecha o pacote de relatórios/scripts/probes (exclui a si próprio, classes compiladas e caches reproduzíveis). PDF oficial tem seu próprio hash em official/source.json. Logs de falha de compilação inicial não foram transformados em evidência PASS; os resultados usados são as execuções finais bem-sucedidas dos probes, com validator sem relaxamento.

Nenhum fix hipotético A/B/C foi executado. Não há novo full73 após fix, análise corporativa externa ou certificação de performance COACTUPC completo. SOURCE_DEPENDENCY_OWNER_UNPROVED limita sua comparação histórica. Perfis/tab stops autênticos e vendor includes permanecem questões explícitas, sem impedir a decisão sobre autoridade de controle.

**STOPPED BEFORE CODE: YES.**
