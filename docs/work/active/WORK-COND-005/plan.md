# Plano

## Fatiamento

1. Discovery: caracterizar typed positions, policies atuais, consumers e a lacuna de contexto; fechar matriz, alternativas e oracles sem mudar produção.
2. Implementation, somente após review/autorização: implementar a policy estrutural, migrar characterization tests e preservar regressões.
3. Closure: promover contrato durável, rodar gates finais e arquivar o work item.

## Dependências

ADR-0012 e os Slices 1–4 concluídos. `BACKLOG-RES-004` não bloqueia a policy do root, mas continua bloqueando ampliar o qualifier `UNSPECIFIED` para DATA/FILE. SEARCH WHEN pertence ao Slice 6.

## Superfície arquitetural provável

Discovery em andamento. O collector é o centro; `Ast`/`AstBuilder` só entram se a investigação provar que algum slot de condição não está distinguível estruturalmente.

## Migrações requeridas

Migrar somente expectativas que caracterizam o falso gap atual; preservar SET/EVALUATE, identity/pre-order, qualifier/subscript, CICS, snapshots e resolução como regressões.

## Artefatos esperados

- teste FACT `ContextualConditionOccurrenceDiscoveryTest`;
- teste planejado `ContextualConditionOccurrenceTest` após autorização;
- spec/eval/state finais com context matrix, consumer impact e semantic challenge pass;
- zero alteração em `src/main/` no checkpoint Discovery.
