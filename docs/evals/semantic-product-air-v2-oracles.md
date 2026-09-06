# Oracles futuros — Semantic Product para AIR 2.0.0

Plano de falsificação derivado do [audit bilateral](../architecture/semantic-product-air-v2-audit.md),
não evidência executável nem implementação concluída. EVAL-SP-001/002/003
provam somente seus contratos atuais. Cada checkpoint deve promover os casos
pertinentes para testes e catálogo, sem alterar baselines para acomodar erro.
IDs `AIR-AUDIT-*` são locais deste plano, não EVALs implementados.

O prerequisite frontend de AIR-AUDIT-07 foi promovido parcialmente para
EVAL-SP-004 em WORK-SEMANTIC-PRODUCT-004: entry primária/start e saída local
GOBACK pelo Semantic Product/JSON. Oracles AIR/lowering/CFG continuam futuros;
esse teste local não certifica retorno/halt de runtime nem perfis AIR.

Fontes fixadas: [invariantes I-01–I-54](https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/conformidade/01-invariantes.md),
[oracles O-01–O-85](https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/conformidade/02-oraculos.md)
e [perfis @2](https://github.com/Gustavo2358/analysis-ir/blob/0b2fbce7046010b22b32efa8cbc3e75ccba09442/especificacao/10-perfis-de-conformidade.md).
STRUCT não exige RD/PV; SCALAR/REGION exigem seus resultados. Validator não
certifica verdade de premissa externa apenas porque seu escopo é bem formado.

## Checkpoints e falsificações

Owners: A/frontend e Semantic Product; B/contrato AIR e CobolLower; C/consumers.

| ID | Checkpoint / owner | Oracle positivo | Falsificação que deve falhar | AIR |
| --- | --- | --- | --- | --- |
| AIR-AUDIT-01 | Contrato/validator e lowerer, B | Publicação fechada recebendo exclusivamente port; producer descartável | Lowerer consulta AST, symbols, occurrences, resolution, report, SourceMap, source, parser, JSON, snapshots, consumer CP6 ou projector para completar facts | PROD-01/04; O-66 |
| AIR-AUDIT-02 | Validator, B | unknown_type fecha sobre TYPE_UNKNOWN com escopo/origem; tipos conhecidos preservados | Escolher text/int/bool ou opaque_type("unknown"); remover gap ou trocar código | I-49/50; O-69–O-76-STRUCT |
| AIR-AUDIT-03 | Validator e MOVE, A/B | Cópia com sameDomain e semântica de conversão/armazenamento estabelecida | Usar MOVE, nome, região ou lacuna igual como prova; literal unknown_type; assign preciso sem compatibilidade | I-08/52/53; O-77/O-82–O-84-STRUCT |
| AIR-AUDIT-04 | Validator, B | Prova com sujeitos/site e interseção finita; positivo de alias exato | entry cobre corpo; invocation de não-invoke; prova de k usada em k2; interseção vazia ampliada; activation(...); ciclo sem base; int/text unidos via unknown | O-82–O-85-STRUCT |
| AIR-AUDIT-05 | Lowerer/storage declarativo, A/B | DataItemId correlaciona ObjectId, associação aberta sem Cell inventada | DataItemId vira StorageId; criar células privadas por nome; span igual prova alias | I-12; O-12/O-14/O-71/O-78 |
| AIR-AUDIT-06 | Inventário/fallback, B; CFG, C | Toda occurrence tem origem/coverage e opaque com três envelopes | ObservedStatement vira nop, é ignorado ou executa parent+children duplicados; desconhecido vira vazio | O-33/O-34/O-47-STRUCT |
| AIR-AUDIT-07 | Entrada/terminal, A/B/C | Entrada publicada; GOBACK com saída de escopo correto e ausência sustentada de successor local | Primeiro root vira Entry; EOF/continuation ausente vira return; terminal genérico ganha fallthrough; STOP RUN vira return | I-05/18/21/22; O-04/O-18/O-19/O-34-STRUCT |
| AIR-AUDIT-08 | Ordem/identidade, B/C | Permutar sequences/dividir com jump preserva pontos/fluxo sob correlação | Ordem física, número de statement ou traversal determina aresta | O-09/O-10/O-29–O-31-STRUCT |
| AIR-AUDIT-09 | IF, A/B/C | Unknown bool puro mantém reads e ambas alternativas; variante impura usa envelope | unknown_type usado como bool; perde falso; terminal reconverge; remainingReads none sem prova | O-02/O-03/O-04/O-74-STRUCT; O-27 |
| AIR-AUDIT-10 | Acesso MOVE/CALL, A/B | Bare/qualified/subscript/reference-modified preservam shape e address reads | CALL WS-X(IX) tratado como acesso inteiro WS-X; IX some sem gap; literal typing esconde perda | I-11/29; O-39/O-43/O-72/O-78 |
| AIR-AUDIT-11 | CALL, A/B | Interpretação textual estabelecida produz unknown text com dependencies de tipo desconhecido | Role promove DATA a text; PGMNAME vira contrato completo; assinatura ausente vira zero; only-normal sem autoridade | O-21/O-23/O-79-STRUCT; I-08/23/25 |
| AIR-AUDIT-12 | CALL outcomes, A/B/C | Result target escrito só no normal; target/args observados antes dos efeitos | Resultado normal aplicado em exception; argumentos perdidos; assinatura parcial vira transmissão precisa | O-20/O-21/O-79/O-83 |
| AIR-AUDIT-13 | Provenance/gaps, B | Origens de uso/declaration distintas, derivações explícitas; include chain/exact/convenções mantidos | AIR ID apaga origin SP; gap unit some; clone recebe mesmo ID; span derivado inventado | O-41–O-44/O-71-STRUCT |
| AIR-AUDIT-14 | Pluralidade/input, A/B | N DATA/M MOVE/K CALL/IF, N≠M≠K e zero; input missing aberto | Primeiro/último/prefixo publicado; unavailable vira zero complete; desconhecidos removidos para melhorar claim | O-32/O-45-STRUCT; O-49/O-50 |
| AIR-AUDIT-15 | CFG aberto, C | any_control influencia todos os labels/saídas/controle externo admitidos; fronteira compacta | Fronteira só no fim/aparente next; reachability precisa após controle aberto | O-34-STRUCT; I-47 |
| AIR-AUDIT-16 | CALL literal/dependency, A/B/C | Nome/namespace/site OBSERVED sem RD/PV; input parcial conserva observação | Nome vira artifact confirmado; inatingível apagado; OPEN vira todas as dependências; trim/case implícitos | O-24/O-45/O-46/O-64–O-68 |
| AIR-AUDIT-17 | MOVE scalar/effects/RD/PV, A/B/C | Cópia captura antes de write; sameDomain conserva relação sob unknown_type; must/may distintos | Cópia válida vira havoc só por tipo; conversão implícita; source reavaliado tarde; valor morto permanece corrente | O-08/O-16/O-25/O-26/O-77/O-80/O-82–O-84-SCALAR |
| AIR-AUDIT-18 | RD/PV/dynamic CALL, C | Diamond dá {B,C} só sob premissas escalares; literal + input mantém candidato e resto | Dynamic CALL perde remainder; último MOVE textual escolhido; efeitos da chamada afetam seu próprio target antes da interação | O-02/O-23/O-36; I-24/33/48 |
| AIR-AUDIT-19 | PERFORM local, A/B/C | Dois sites compartilham memória com respectivos resumes; completion/default/top corretos | Retornos cruzados, reinicialização, busca frame inferior, unwind implícito, semântica IBM incompatível com local.boundary | O-56–O-60 |
| AIR-AUDIT-20 | GO TO/ALTER, A/B/C | DEPENDING preserva default fora da faixa; indirect CFG cobre S antes de PV | Número vira label; S vem do corpus; alvo fora de S descartado; revisões misturadas | O-05-STRUCT; O-61–O-63 |
| AIR-AUDIT-21 | Regiões/storage, A/B/C | Offset/codec/overlap/fragmentos explícitos | Alias possível vira exato; codec vira ASCII; caracteres viram bytes; escrita parcial mata objeto inteiro | O-37–O-40/O-51–O-55/O-81-REGION |

## Primeiro oracle executável a promover

Contrato/validator começa com publicação manual independente de COBOL:
entrada, terminal, origins e uncertainty explícitos. O checkpoint frontend
publica os mesmos fatos via port para AIR-FIRST/GOBACK, com contexto e escopo
de saída. Lowerer recebe só port; CFG recebe só AIR. Comparar observações sob
correlação de IDs, permitindo quantidade de labels diferente por decomposição.

Contracasos: entrada ausente; assinatura unknown apresentada vazia; GOBACK
genérico interpretado pelo nome; terminal com next espúrio; outra operação após
a saída preservada no inventário; contexto de chamada distinguindo return/halt.
Memória/dependências abertas não impedem controle fechado quando o terminal
específico o garante. Testar separadamente saída raiz, saída chamada e escopo de
término; nenhum deles é inferido do fato de haver somente um statement.

O smoke test com entrada abstrata/any_control é opcional e testa a própria
incompletude. Não substitui o oracle de entrada real. Se o contrato concreto
não expressar entry inventory indisponível sem overclaim, recusar corpo
executável e manter inventário observado; não escolher primeiro statement.

## Progressão de precisão e escala

Linear/IF entram após continuidade/avaliação e contracasos; depois sites
literais e fluxo escalar. N occurrences cobre múltiplos statements e zero
matches sem limitar a capability às quantidades do fixture. Fronteiras máximas
podem ser compactas, sem materializar todos os pares de successors.

Para PRECISE_FOR_PROFILE, inventariar todos os oracles do papel/perfil @2 e
extensões requeridas. Verde focal não prova conformidade total. Limites
operacionais exigem ANALYSIS_LIMIT, scope e remainder, sem truncar fatos.
