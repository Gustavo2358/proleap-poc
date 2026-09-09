# WORK-SEMANTIC-PRODUCT-005 — Checkpoint 4A scalar textual MOVE

Concluído pelo PR #32,
mergeado em 2026-09-08T15:15:05Z, commit
`2815e805fd3a9ef4762a39ab9435260fc76da0e8`. Metadata remota conferida durante
lifecycle hygiene obrigatório de WORK-AST-004; nenhum novo slice foi iniciado.

Contrato 1.2.0: E1–E4 do MOVE textual escalar, domínio/valor, acesso inteiro,
FULL_IDENTITY e continuação canônica. Escrita de JSON diretamente em OutputStream,
sem byte[] integral; DTOs permanecem materializados. Limites de profile e memória
continuam explícitos, sem AIR, lowering, CFG ou dataflow.

Conhecimento e evidência preservados no [profile textual](../../domain/scalar-text-move.md),
[Checkpoint 4A](../../evals/checkpoint-4a.md) e
[remediação de memória](../../evals/checkpoint-4a-memory-remediation.md).
EVAL-SP-005/006 e INV-SP-008; oracles/challenges e logs permanecem nesses artefatos.
O estado integrado registrava fast/semantic/performance/full verdes, 13 challenges
RED com restauração exata e segundo GREEN. Isso é evidência do item concluído,
não resultado reexecutado pelo discovery de NEXT SENTENCE.
