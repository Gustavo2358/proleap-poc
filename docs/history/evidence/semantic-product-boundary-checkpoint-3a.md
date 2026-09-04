# Semantic Product Boundary — Checkpoint 3A

## Hipótese

Um consumer independente consegue interpretar um fato de `CALL` literal somente
por uma boundary COBOL-specific experimental: estado A2 materializado, próprio e
imutável, exposto por um port B read-only. Depois da publicação, o consumer não
precisa manter parser, resolver ou qualquer provider vivo do frontend.

## Experimento executado

O adapter test-only analisou `call-linkage-unspecified.cbl`, fez o join tipado
entre `CallStatement`, `ProgramReference` e ocorrência/resolução pelo ID
namespaced do nó, e publicou apenas `observedTarget`, sintaxe, status/reason,
linkage, unit, policy, readiness de CALL, completude do binding nominal,
uncertainty localizada e provenance nos tipos próprios da boundary. O estado de
completude foi projetado dos gaps já produzidos por `ResolutionAnalysisReport`:
`CALL_LINKAGE_UNKNOWN` reduz somente a dependency/call readiness; não reduz o
binding nominal conhecido. O `CALL CALL-NAME` foi mantido fora do slice literal.

O consumer, em pacote separado, recebeu somente o `Port`. Um segundo cenário
construiu o estado boundary diretamente, sem frontend, texto ou metadata da
grammar. A análise foi construída duas vezes para comparar valores
determinísticos; isso não é tratado como identidade de publicação. O 3A não
mantém `analysisGeneration`: coerência entre publicações diferentes permanece
explicitamente não provada. Bytecode de todas as classes boundary/consumer foi
inspecionado contra dependências de frontend e ANTLR.

## Oracles e resultado

| Oracle | Resultado |
| --- | --- |
| Closure após liberar a análise frontend | PASS |
| No leakage de tipos frontend/ANTLR no estado, port e consumer | PASS |
| No semantic reparsing de `writtenText`/`grammarRule` | PASS |
| Estado/port imutáveis e query-only | PASS |
| `CALL 'TARGET-A'` preserva `EXTERNAL_OBSERVED` e `LITERAL_EXTERNAL_PROGRAM` | PASS |
| `CALL_LINKAGE_UNKNOWN` preserva binding nominal `COMPLETE` | PASS |
| Readiness de dependency/CALL fica `INCOMPLETE`, com uncertainty tipada e localizada no call site | PASS |
| Linkage sem options permanece `UNKNOWN`; target de runtime permanece desconhecido | PASS |
| Unit namespaced, provenance e policy `UNSPECIFIED` são publicados | PASS |
| Valores publicados repetidamente são determinísticos, sem claim de identidade cross-publication | PASS |
| Oracle rejeita claim de `analysisGeneration` no contrato 3A | PASS |
| Teste focalizado (`SemanticProductBoundaryCheckpoint3ATest`, 7 testes) | PASS |
| `check-fast.sh` | PASS |
| `check-semantic.sh` | PASS |
| `check-full.sh` | PASS |
| `git diff --check` | PASS |

## Dependências observadas

O adapter conhece os internals do frontend necessários à análise: normalização,
preprocessing, ANTLR, AST, tabelas, índice de scopes, occurrences e resolver.
Essas dependências ficaram restritas ao teste adapter. A boundary e o consumer
usam somente tipos próprios e Java; o port retém apenas o estado materializado.
Não foram usados snapshots, `ExplorerMain`, `writtenText`, `grammarRule` ou
reparse no caminho consumido.

## Limitações/findings

O resultado é limitado a `CALL` literal em uma compilation unit. Não prova
interchange, serializer, round-trip, persistência, custo, outros constructs ou
um Semantic Product de produção. A ausência de compiler options reduz a
precisão do linkage e foi preservada como uncertainty, sem inferir valor de
runtime; isso confirma a limitação já classificada como `F-SP-006`, sem criar
finding semântico novo. O 3A não prova `analysisGeneration` nem rejeição de uma
mistura entre publicações distintas; persistência cross-run/cross-version e
coerência de geração continuam fora deste slice. Nenhum gap do frontend foi
corrigido.

## Veredito do checkpoint

**PASS — para o slice `CALL` literal.** As propriedades adversariais foram
provadas executavelmente para a seam A2+B: o consumer interpreta o fato apenas
pela boundary fechada, distingue binding nominal conhecido de readiness
incompleta, preserva uncertainty localizada, não encontra leakage de frontend e
não faz reparsing semântico. A igualdade determinística entre duas execuções é
somente uma propriedade de valor; a identidade/coerência cross-publication de
generation permanece não provada e não é reivindicada. Este resultado não
autoriza implementação de produção nem qualquer checkpoint além do 3A.
