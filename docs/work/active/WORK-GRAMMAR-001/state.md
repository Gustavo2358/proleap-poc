## Onde estamos

Fases 1 a 5 concluídas; regressão ampla executada com dois baselines estruturais antigos deliberadamente mantidos inalterados.

## Verde conhecido

`CicsExpressionGrammarInvariantTest`: 66/66; suíte sem `SemanticModelBaselineCharacterizationTest`: 224/224; gates docs, architecture e fast: verdes; COACTUPC mantém zero parser errors, tokens, profundidade, símbolos e diagnostics.

## Restante

Decidir em trabalho posterior se os baselines de contagem existentes podem ser atualizados; isso foi proibido no escopo atual. O gate `full` permanece vermelho somente por esses dois asserts.

## Descobertas que afetam o plano

O arquivo local anterior era byte a byte idêntico ao `Cobol.g4` upstream atual. A raiz comum é a inclusão de `DFHRESP` e `DFHVALUE` em `cobolWord`, que alimenta `qualifiedDataName -> tableCall -> identifier`. Há 50 decisões diretas `identifier | literal` ou equivalentes; todas compartilham esse vazamento nominal. A matriz pré-correção executou 65 casos: 48 falharam nos 12 hosts com `identifier` antes de `literal`, quatro CICS passaram no controle `literal`-first de `enableStatement` e todos os 13 table calls reais passaram. A correção retirou somente os dois tokens de `cobolWord`. Em COACTUPC isso remove 20 `DataReference`/occurrences espúrias, 80 wrappers de parse tree e 20 gaps; os dois baselines falhos congelam as contagens anteriores.
