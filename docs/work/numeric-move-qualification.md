# Checkpoint 2 — valores numéricos e MOVE inteiro DISPLAY

Status: IN_PROGRESS, implementação qualificada para review nos PRs frontend #86 e lower #58; sem merge.

## Resultado

O contrato SP 2.65 publica o valor lógico dos literais numéricos e provas de MOVE
por receptor. O lower valida essas provas e produz atribuições AIR INT pelo
caminho atual de MOVE. A análise de inteiros substitui `NumericControlSemantics`
e é compartilhada com as provas de controle; não há um segundo algoritmo de MOVE
para preservar o comportamento anterior.

No CardDemo fixado, todos os 73 fontes foram processados. O [inventário por fonte](numeric-move-corpus.csv)
está ordenado pela redução de gaps.

| Medida | Resultado |
| --- | ---: |
| Gaps no checkpoint 1 / checkpoint 2 | 23.960 / 22.503 |
| Redução líquida neste checkpoint | 1.457 (6,08%) |
| Literais numéricos publicados | 1.132 |
| Declarações com tipo inteiro lógico | 1.098 |
| MOVEs com prova numérica / transferências | 181 / 189 |
| Transferências de literal / de DATA | 124 / 65 |
| Transferências elegíveis ausentes ou extras na auditoria | 0 / 0 |

A auditoria percorreu os 7.015 MOVEs da AST, relacionou ocorrências, bindings,
capacidade inteira e provas publicadas e conferiu cada receptor elegível ao perfil.
As 189 transferências elegíveis estão publicadas, sem gaps de identidade ou acesso
integral já satisfeitos por essas provas. O inventário registra ainda 1.191
receptores sem prova suficiente de tipo/acesso e 362 casos sem essa prova na origem.
Esses limites permanecem explícitos; não são transferências certificadas omitidas.

| Gap | Antes | Depois | Removidos |
| --- | ---: | ---: | ---: |
| LITERAL_KIND_NOT_PUBLISHED | 1.329 | 208 | 1.121 |
| SCALAR_WHOLE_ITEM_NOT_PROVEN | 4.207 | 4.046 | 161 |
| MOVE_IDENTITY_NOT_PROVEN | 5.089 | 4.928 | 161 |
| MOVE_TARGET_CARDINALITY_OUTSIDE_CAPABILITY | 161 | 147 | 14 |

## Regra, algoritmo e limites

O recorte fecha MOVE para itens locais independentes, inteiros DISPLAY sem sinal,
sem edição e com 1–31 dígitos. Aceita literal fixo integral que cabe no receptor,
inclusive ZERO e `12.00`, ou origem DATA inteira cuja capacidade cabe no destino.
Grupos, qualificadores e VALUE conhecidos podem participar quando a independência
é provada. A capacidade é comparada por contagem de dígitos, sem enumerar valores,
intervalos ou combinações. USAGE herdado é calculado uma vez por declaração.

Nos MOVEs com vários destinos, literais permitem provas independentes; DATA
preserva o prefixo de destinos provados. Um destino desconhecido encerra esse
prefixo para evitar reler como exato um valor que pode ter sido alterado. Os efeitos
dos demais destinos continuam conservadores.

Os testes negativos incluem overflow, sinal, fração, COMP-3, USAGE COMP-5 herdado,
GROUP-USAGE, alias, tabela, binding ausente, PIC enorme e certificados forjados.
O teste de herança encontrou uma premissa ausente e a correção manteve 20 gaps
que a medição intermediária havia removido. O resultado final usa essa correção.

Continuam fora deste recorte: conversão de sinal, escala e truncamento, campos
editados, representação COMP/COMP-3/COMP-5, subscritos/refmod e armazenamento sem
prova local. Literais flutuantes e vírgula decimal sem prova do dialeto não recebem
certificado de MOVE inteiro. Não se declara completude de toda a categoria numérica.
A [regra de domínio](../domain/numeric-move.md) registra as fontes normativas IBM.

## Regressão

As 3.807 ocorrências de condições 88/SET do checkpoint 1 continuam publicadas e
consumidas: 1.988 leituras e 1.819 escritas, sem ausências ou extras.

A execução final `numeric-qualified` + `pipeline-qualified` terminou 292 etapas
frontend → lower/AIR → CFG → dependencies para os 73 fontes, sem falhas.
A comparação com o checkpoint 1 preservou todas as projeções abaixo:

| Dependência | Sites | Candidatos |
| --- | ---: | ---: |
| Programas | 150 | 209 |
| Arquivos | 391 | 378 |
| Origem qualificada | 259 | 95 |

Zero sites perdidos, candidatos perdidos/adicionados ou mudanças de remainder.
`sourceDependencies` e declarações de arquivos permanecem iguais após normalizar
a identidade da publicação. `observed-dependencies` é idêntico byte a byte nos
73 fontes. Os estados permanecem **65 PARTIAL e 8 COMPLETE**. O conteúdo executável
AIR muda para incluir os novos valores e atribuições; a comparação não exige
igualdade de operações que este checkpoint modela intencionalmente.

## Gates e reprodução

- Frontend FAST: PASS, 806 testes, zero falhas, erros ou skips.
- Frontend full local: PASS, 1.377 testes, zero falhas/erros; um discovery opt-in
  (`semantic.condition.required`) permanece ignorado. Os 73 fontes foram executados
  explicitamente na rodada integrada.
- Lower full local: PASS, incluindo capacidade de 20.000 MOVEs, determinismo e arquitetura.
- Lower FAST final: PASS.
- Oráculo independente de execução numérica: PASS, valores `[12,12,12,0,0]`,
  MOVE misto, validação AIR/codec e rejeições de contratos JSON e em memória.
- Os 4.018 arquivos de classes congelados na rodada final são idênticos byte a byte
  aos builds qualificados: frontend 2.386, lower core 541, adapters 715,
  AIR model 341 e AIR JSON 35.

Snapshots de código qualificados:

- Frontend: `5cfa8d14f87826206dc03b65acaf6a2ba3ee67f5` (SP 2.65).
- Lower: `5839d5495da1a86d68c675043d5259ff8133f8f9`.
- AIR: `a4c49bcf5e07000cb78349c2cc6357ca2dfe7acd` (inalterada neste checkpoint).
- CFG: `ae3b23d9e853f15fff64ebb49fa40be9391670ba`.
- CardDemo: `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`.

Baseline aprovado: frontend `b0a387c2bcecb0621a015a231543fb7de8dc65a4`,
lower `424a678936e0a42c77dea0c590019e28e0bccb93`. Os commits seguintes acrescentam
relatórios/índices, sem alterar o código qualificado. O pin SRC-SP fixa o snapshot
de código acima; os hashes dos arquivos fixados foram conferidos.

[Contagens, pins e hashes](numeric-move-results.json). Evidência bruta local:
`/home/gustavo/workspace/teste-e2e/.gap-reconciliation-20261003/numeric-evidence/`.
Scripts: `run-frontend.py`, `audit-numeric.py`, `audit-conditions.py`,
`run-pipeline.py` e `compare-dependencies.py`. Os logs RED e intermediários
permanecem preservados; `frontend-full-reviewed.log` foi interrompido após um teste
negativo conhecido falhar e foi substituído pela execução final qualificada.
