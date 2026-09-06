# Plano

## Fatiamento

1. Higiene Git e identidade das duas fontes; verificar prerequisite F-02.
2. Leitura bilateral e contracasos sobre a capability declarada.
3. Relatório, oracles futuros e correções de conhecimento durável/backlog.
4. Self-validation, gates, revisão integral do diff, commit, push e novo PR.

## Dependências

F-02 não está mergeado na main de partida. Isso não impede o Discovery;
conclusões de integridade de produção são condicionais ao merge e revalidação.

## Superfície arquitetural provável

Somente documentos listados no YAML. Código e testes atuais são inputs de
leitura; experimento descartável em /tmp não é implementação do lowerer.

## Migrações requeridas

Reavaliar claims do CP8 contra TypeRef, unknown_type, sameDomain, envelopes e
perfis @2. Não migrar API, baselines ou histórico concluído em massa.

## Artefatos esperados

- [Audit bilateral](../../../architecture/semantic-product-air-v2-audit.md).
- [Oracles futuros](../../../evals/semantic-product-air-v2-oracles.md).
- Domínio, pipeline, invariantes e backlog coerentes com os findings.
- Novo PR com revisão pendente; encerrar o item somente conforme lifecycle.
