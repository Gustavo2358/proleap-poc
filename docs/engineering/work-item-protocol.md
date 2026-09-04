# Protocolo de work items

Work items delimitam uma mudança ativa. Eles não substituem contratos de domínio, ADRs, invariantes, testes ou o backlog; apenas roteiam a implementação atual para esses artefatos.

## Ciclo de vida

Um work item ativo fica em `docs/work/active/<WORK-ID>/` e contém exatamente `work-item.yaml`, `spec.md`, `plan.md`, `eval.md` e `state.md`. O ID segue `WORK-<ÁREA>-<NÚMERO>` e não é reutilizado.

Ao concluir, promover conhecimento durável para a família canônica correspondente, manter oráculos nos testes e no catálogo de evals, registrar um resumo curto em `docs/work/history/<WORK-ID>.md` quando ele for útil e remover o diretório ativo. Um work item concluído não se torna uma tasklist permanente.

## `work-item.yaml`

O YAML é deliberadamente um mapa curto, com valores escalares e listas simples. Os campos obrigatórios são:

```yaml
id: WORK-ÁREA-001
title: Título curto da mudança
status: active
risk: low | medium | high
goal: Resultado observável em uma frase
must_read:
  - caminho canônico ou caminho#fragmento
related_domain_rules:
  - documento de domínio aplicável
related_decisions:
  - ADR-XXXX
related_invariants:
  - INV-ÁREA-XXX
evals:
  - EVAL-ÁREA-XXX
source_scope:
  - arquivo ou diretório de produção em escopo
test_scope:
  - teste ou fixture em escopo
must_not_change:
  - fronteira fora do escopo
gates:
  - fast
```

`status` pode ser `active`, `blocked` ou `completed`; diretórios em `active/` usam somente `active` ou `blocked`. `risk` define a profundidade de contexto, não uma autorização para alterar escopo. `must_read` aponta para a menor rota canônica suficiente. Decisões, invariantes e evals devem usar IDs existentes. Os gates aceitos são `docs`, `fast`, `architecture`, `semantic`, `performance` e `full`.

## Impacto semântico downstream

Findings semânticos novos devem registrar um bloco `downstream_impact` conforme a
[taxonomia canônica](downstream-impact-classification.md). O bloco possui uma
classe primária única e os campos `class`, `rationale`, `evidence` e
`reassess_when`; a falta de evidência exige `UNASSESSED`. A classificação não é
severity, prioridade, tipo do finding ou autorização de remediação. Findings
históricos não são migrados em massa: a regra prepara os próximos registros e
deve ser aplicada quando um finding existente for reaberto ou alterado por um
work item autorizado.

Durante um Discovery, `source_scope` e `test_scope` podem reservar um arquivo novo ainda inexistente com `planned:<caminho>`. O harness aceita essa forma somente sob `src/main` ou `src/test`, exige que o diretório pai já exista e falha se o arquivo estiver presente; ao iniciar a implementação autorizada, remova o prefixo no mesmo checkpoint que cria o arquivo. Os demais campos continuam aceitando apenas caminhos existentes.

## Documentos de trabalho

`spec.md` deve conter, nesta ordem, as seções: Problema, Objetivo, Domínio de entrada suportado, Classes semânticas, Premissas, Comportamento esperado, Comportamento diante de incerteza, Fora de escopo, Regras de domínio relacionadas e ADRs/invariantes relacionados.

`plan.md` contém: Fatiamento, Dependências, Superfície arquitetural provável, Migrações requeridas e Artefatos esperados. Ele descreve slices e dependências; não repete a spec.

`eval.md` contém: O que prova corretude, Classes positivas, Classes negativas, Classes ambíguas, Casos adversariais, Casos de regressão, Propriedades/relações metamórficas e Expectativas de escala. Quando uma classe não se aplica, declarar explicitamente o motivo.

`state.md` contém: Onde estamos, Verde conhecido, Restante e Descobertas que afetam o plano. É memória curta e factual da execução, não histórico detalhado.

O [resumo de WORK-HARNESS-001](../work/history/WORK-HARNESS-001.md) registra o primeiro ciclo concluído. Templates separados ficam adiados até que um segundo work item demonstre uma repetição útil.
