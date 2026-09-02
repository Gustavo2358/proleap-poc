# Evals

Evals tornam oráculos semânticos localizáveis por capacidade. O teste executável continua sendo o lugar do assert; o catálogo explicará qual capacidade ele prova, qual fixture usa e quais regras/invariantes estão relacionadas.

Nesta versão, o catálogo será canônico em Markdown. Não haverá manifesto YAML duplicado enquanto nenhum gate precisar consumi-lo.

O [catálogo de evals semânticos](semantic-eval-catalog.md) registra IDs estáveis, capacidade provada, oracle executável, fixtures, tier e o tipo de implementação ingênua rejeitada. Lacunas reais permanecem no [backlog](../work/backlog.md), sem serem apresentadas como cobertura existente.

Os [oracles normativos `COND-*`](conditional-expression-oracles.md) preservam as classes de condições combinadas/abreviadas fechadas no Slice 1. Eles orientam testes futuros, mas permanecem separados do catálogo executável até que um slice autorizado materialize os asserts.
