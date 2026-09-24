# D0 — proposta de roadmap por mecanismo

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
