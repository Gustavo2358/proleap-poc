# Especificação

## Problema

O CP8 demonstrou reconstrução do port, mas não validou uma publicação AIR V2.
As claims precisam ser confrontadas com o contrato externo 2.0.0.

## Objetivo

Responder suficiência bilateral por capability, separar validade de precisão,
identificar o menor fato ausente e ordenar os próximos checkpoints.

## Domínio de entrada suportado

Código da main sincronizada, contrato canônico do Semantic Product e ZIP AIR
2.0.0 identificado no relatório. F-02 é inspecionado separadamente: PR #28
aberto, ausente da main de partida. Não integrar sua implementação neste audit.

## Classes semânticas

DATA, MOVE literal, CALL, IF, inventário observado, identidades, provenance,
controle, coverage, binding e unknowns; perfis AIR estruturais e dependências
dos perfis de fluxo/local/indireto/regiões.

## Premissas

O lowerer recebe exclusivamente CobolSemanticPort. O ZIP permanece em /tmp.
Especificação AIR e documentação IBM pertinente são autoridades externas;
código é evidência factual. Divergências permanecem findings explícitos.

## Comportamento esperado

Relatório com matriz PRECISE/CONSERVATIVE/BLOCKED, conformidade separada,
findings, fontes, primeiro slice e ordem de implementação; oracles futuros;
correções duráveis de docs/backlog; commit e novo PR contra main sem merge.

## Comportamento diante de incerteza

Envelopes máximos precisam de justificativa normativa. Readiness não substitui
fato nem prova de tipo/controle. Prerequisite remoto ausente permanece pendente.

## Fora de escopo

Toda implementação de produção, novos testes executáveis, enriquecimentos,
merge de outro PR, merge deste PR e auto-merge.

## Regras de domínio relacionadas

Contrato do Semantic Product, resolução nominal e provenance listados no YAML.

## ADRs/invariantes relacionados

ADR-0013, ADR-0012, INV-SP-001 a INV-SP-006 e INV-RES-002.
