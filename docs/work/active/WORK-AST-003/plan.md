# Plano

## Fatiamento

1. Confirmar `main` no merge do PR #10, executar baseline `full` e criar branch exclusiva.
2. Reproduzir os casos mínimos pelo pipeline real e registrar sequências de IDs.
3. Auditar contrato, história estritamente relevante, consumidores, 48 tipos de nó, fontes de alocação e reachability.
4. Versionar fixtures, testes de caracterização, oracle opt-in vermelho e relatório completo.
5. Executar gates, revisar que produção permaneceu intocada, criar commit e PR de Discovery.
6. Hard stop. A eventual implementação é outro commit e outro PR após autorização explícita.

## Dependências

- Base `c6d9b6e1b597f34db06b41f4e8e04cdcf1d68a3a`, merge do PR #10.
- Contratos, policies e gates listados em `work-item.yaml`.
- Evidência histórica de WORK-RES-004 somente porque registra uma decisão anterior explicitamente motivada por pre-order.
- Review independente do PR de Discovery antes de qualquer Fase 2.

## Superfície arquitetural provável

O Discovery lê `Ast`, `AstBuilder`, `AstSnapshot` e consumidores de IDs/traversal. A implementação futura provavelmente será localizada em `AstBuilder`, testes e documentação/invariant; qualquer mudança em `Ast`, `AstSnapshot` ou consumidores exigirá justificativa adicional contra a alternativa recomendada.

## Migrações requeridas

Nenhuma no Discovery. A recomendação preserva IDs e snapshots atuais para entradas já válidas. A Fase 2 deverá comparar AST, coverage, símbolos, occurrences, resolução, provenance e relatórios antes/depois e explicar qualquer churn.

## Artefatos esperados

- Quatro fixtures focais: dois triggers mínimos, um trigger diagnóstico independente e um controle negativo.
- Testes verdes de caracterização e reachability/pre-order sobre a superfície representativa existente.
- Oracle estrutural opt-in deliberadamente vermelho.
- Relatório histórico reproduzível com auditorias completas, alternativas, recomendação, risco e aceite futuro.
- Work item, commit e PR exclusivos de Discovery.
