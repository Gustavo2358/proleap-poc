# Estado

## Onde estamos

4A implementado na branch feat/semantic-product-scalar-move. Baseline main limpa
e atualizada: c8a891e0827ae1dc1140246f625fd16c2ac9bd97. Contrato SP 1.2.0.
Somente este frontend mudou; discovery e repos irmãos permaneceram read-only.

## Verde conhecido

E1–E4, fixture AIR-MOVE e contracasos passam pelo oracle público e JSON.
CP3 preserva payload integral 1.1.0 (exceto versão). Full/fast/architecture/semantic,
performance e naming verdes: 549 testes, zero falhas/erros, um skip preexistente.
13 challenges RED, restauração exata e segundo GREEN no código final.
Probes: 60.012 linhas; 1.500/3.000 DATA+MOVE; 10.000 MOVEs/1 DATA. Consultas
lineares, 1 por MOVE. SP CLI: 5.177 bytes. Evidência em docs/evals/checkpoint-4a.md.
Diff integral revisado; git diff --check verde; sem alterações de grammar/resolver.

## Restante

Handoff remoto por PR e CI do head, seguido de revisão humana. Sem merge,
auto-merge ou início de 4C. O item permanece ativo até closure autorizado.

## Descobertas que afetam o plano

Registry é docs/work/index.md/active/history; 005 era o próximo ID. O lifecycle
004 foi arquivado após confirmação remota de merge do PR #31. Baseline não tinha
GitHub Actions: workflow novo executa full/performance/challenges com actions pinadas.
Overlays são excluídos conservadoramente por seção; sequência entre regiões e
storage geral permanecem abertos. Logs preservam tentativas e resultados finais.
