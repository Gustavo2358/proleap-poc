# Plano e decisão técnica

## Fatiamento

1. **Concluído — Discovery:** higiene Git/lifecycle, fonte normativa, inventário
   de caminhos, reprodução AST/coverage/port/CLI e oracle requerido RED.
2. **Concluído — Gate humano do discovery:** desenho e critérios aprovados
   explicitamente em 2026-09-08.
3. **Implementado — Fix focal, novo human review pendente:** na mesma
   branch/work item/PR #33 Draft e em
   worktree isolada do Checkpoint 4E; finalização comum de Statement no AstBuilder,
   promoção dos testes e gates, commit/push e novo human review. Sem merge.
4. **Separado e não autorizado — Hardening:** validação geral antecipada de
   AST↔coverage, novas políticas de origem semântica, sentence/target públicos
   ou qualquer etapa downstream exigem decisão e escopo próprios.

## Dependências

Não há bloqueio de implementação por tecnologia externa; a aprovação humana
do discovery foi recebida. O próximo bloqueio é review da implementação antes
de qualquer merge. WORK-AST-002 já fornece a invariante e o oracle
de unicidade em uma fixture; seu Slice 3 não foi iniciado. Work items e backlog
adjacentes não autorizam ampliar este bug. A higiene exigida pelo protocolo
arquiva WORK-SEMANTIC-PRODUCT-005 após confirmar PR #32 mergeado em
2026-09-08T15:15:05Z (`2815e805fd3a9ef4762a39ab9435260fc76da0e8`).

## Superfície arquitetural provável

**Decisão aprovada:** extrair de `buildStatement` uma finalização interna de
`Ast.Statement` que recebe o nó já tipado e o contexto/texto real, registra uma
vez pelo `recordCoverage` existente e retorna o mesmo nó. O caminho normal
continua `visit(StatementContext) → finalização`; `statementsInside` encaminha
o nó da alternativa direta à mesma finalização. O visitor normal não deve
registrar outra vez. O índice `builtStatements`, usado por entry/MOVE, continua
indexado somente pelos StatementContexts reais; não fabricar contextos ou keys
para as alternativas diretas. Logging pode permanecer na finalização comum.

Não é necessário um factory global nem alterar Ast/SemanticCoverage/projector.
`buildCoverageReport` já ordena drafts e aplica o manifesto fechado. Cada
ocorrência gera um draft; complexidade assintótica permanece a atual: coleta
linear, ordenação de findings `O(F log F)`, espaço `O(F)`.

### Origem versus policy: limite deliberado do fix

O nó especial hoje usa `meta(context)`: `ifThen` (inclui THEN quando escrito),
`ifElse` (inclui ELSE) ou `searchWhen` (inclui condição). Não há context
`nextSentenceStatement` nesses branches. O finding deve compartilhar essa Meta
real e seguir a policy atual:

| Origem | ConstructionCoverage | DependencyKnowledge |
| --- | --- | --- |
| nextSentenceStatement | MODELED | NOT_DEPENDENCY_BEARING |
| ifThen / ifElse | PRESERVED_UNINTERPRETED | DEPENDENCY_UNKNOWN |
| searchWhen | MODELED | REFERENCE_READY |

Logo, o fix de integridade pode tornar IF direto `PARTIAL` no inventário do SP,
enquanto o caminho normal permanece `UNSUPPORTED`; ambos mantêm a mesma identidade
NEXT_SENTENCE e readiness bloqueada. Isso é conservador e respeita ADR-0009.
Não fingir origem nextSentenceStatement nem promover todo ifThen/ifElse para
MODELED. Se o reviewer exigir policy uniforme por tipo semântico, será preciso
revisar o desenho para separar explicitamente a chave de policy da origem
gramatical e adicionar oracles dessa distinção antes de implementar. Esse
refinamento não é necessário para reparar o crash e não está implicitamente
autorizado pelo discovery.

### Alternativas avaliadas

| Alternativa | Decisão e razão |
| --- | --- |
| Adicionar somente recordCoverage no if de statementsInside | Repararia presença, mas deixa finalização distribuída e a mesma classe de regressão fácil de repetir; preferir o helper comum. |
| Registrar no visitor NextSentence | Não alcança tokens diretos e duplica o finding do caminho normal se buildStatement continuar registrando. |
| Factory comum para todos os statements | Refactor maior sem necessidade comprovada; há duas entradas relevantes e finalização existente reutilizável. |
| Uniformizar policies agora ou mudar gramática para sempre produzir StatementContext | Amplia contrato/escopo; gramática aceita a entrada e não é a causa do crash. |
| Sintetizar Meta/context nextSentenceStatement | Falsifica provenance; nenhum context assim existiu na árvore direta. |
| Criar finding ou fallback no projector | Viola INV-SP-004 e esconde corrupção interna. |
| Usar CONTINUE, generic modeled ou fallthrough | Perde identidade/transferência; contraria a regra IBM e o objetivo explícito. |
| Implementar targets de sentence/CFG neste fix | Nova capability/contrato downstream; desnecessário para restaurar inventário íntegro e conservador. |

## Migrações requeridas

Neste checkpoint: promover o oracle requerido para execução normal e migrar
os asserts de bug para regressão positiva explicitamente;
nenhuma alteração de versão pública é prevista. Finding IDs subsequentes podem
mudar porque faltavam findings; são determinísticos por execução, não IDs
persistentes. AST IDs e Meta permanecem iguais. Coverage/report/JSON poderão
exibir os novos findings/gaps antes ausentes; revisar esses efeitos sem relaxar
baseline, sem atribuir runtime ou controle conhecido.

## Artefatos esperados

Finalização comum no AstBuilder, regressões obrigatórias em
`NextSentenceCoverageDiscoveryTest`, [eval](eval.md), EVAL-AST-006 promovido
e os cinco arquivos deste item. Preservar os dois fixtures e a documentação
canônica do discovery. Capturar RED anterior, GREEN, logs/hashes e comparação
de AST e dos checkouts ocupados em outputs locais ignorados.
Commit focalizado, push normal e Draft PR com o gate exato. Pedir review humano
e parar. A especificação de sentence/target necessária a lowering preciso é
uma lacuna documentada, sem criar automaticamente outro trabalho.
