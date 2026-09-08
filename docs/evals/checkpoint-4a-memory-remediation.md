# Checkpoint 4A — remediação da escrita em arquivo

Work item WORK-SEMANTIC-PRODUCT-005, mesmo PR #32. Head revisado pelo usuário:
6de80966b6d275e6398280964e45f7b672cfb5e5; baseline original main permanece
c8a891e0827ae1dc1140246f625fd16c2ac9bd97. O review confirmou semântica E1–E4,
negativos, CP3, complexidade do enrichment, mutações e CI exato como PASS.
O finding era de alocação na publicação, sem mudança de significado downstream.

## Correção e limites

Antes: write → serialize → DTOs → JSON integral em byte[] → Files.write.
Agora: write → DTOs → JSON.writeValue(OutputStream, DTOs). O stream é fechado
pelo adapter, inclusive quando a escrita falha. A CLI já chama write e mantém
o alias por Files.copy. serialize continua disponível para bytes em memória.

O único arquivo de produção alterado na remediação é SemanticProductJsonWriter.
Modelo, analyzer, projector, regras de continuação, profiles e mutation script
permanecem intactos. O contrato e shape JSON continuam **1.2.0**, byte-idênticos.
Nenhum código de AIR, CFG, lowering ou repositório irmão foi alterado.

Isso retira a retenção do byte[] com todo o JSON no caminho de produção. Não
elimina o Semantic Product nem a árvore DTO, e não constitui serialização
incremental de facts. Não se afirma redução de heap medida nem novo SLA.
Os probes ainda materializam uma árvore JSON test-only para seu oracle de
cardinalidade; a produção não faz essa leitura.

## Provenance de normalContinuation

Foi avaliada e preservada nesta remediação. No corpus de 10.000 MOVEs, cada
normalContinuation.provenance é igual à header.provenance de seu MOVE. Remover
somente esses membros do JSON economizaria 2.405.672 dos 18.809.400 bytes
(12,79%). É uma dívida real, separada do byte[] integral removido da escrita.

A decisão mantém o record público e seu oracle de provenance já validados, sem
revisar agora o shape 1.2.0. Uma eventual remoção exigirá explicitar a origem
herdada do header na boundary e nos consumers; ela não foi tratada como redução
silenciosa de payload. O restante da verbosidade de transporte continua aberto.

## Oracles

SemanticProductFileWriteTest compara write com serialize em fixtures pequenos:
AIR-MOVE positivo, CP3, padding/continuação desconhecida e literal numérico.
Também verifica determinismo na repetição e truncamento de arquivo anterior.
O oracle já passava no código antigo: igualdade de bytes não prova alocação.

Um guard arquitetural focalizado rejeita as chamadas de materialização integral
no método de publicação em arquivo. Ele ficou RED com o caminho antigo e GREEN
com a escrita direta. É uma proteção estrutural contra a regressão observada,
não um profiler nem uma prova geral de alocação transitiva da biblioteca JSON.

Os probes existentes agora chamam write(port, file), medem Files.size e leem o
JSON do arquivo. Mantêm integralmente seus oracles de visitas, consultas, IDs,
cardinalidade e crescimento. Os 13 challenges semânticos não foram modificados.

## Resultado local

Full (fast, architecture, semantic, E2E e naming) e performance passaram.
Os 13 challenges ficaram RED, com restauração exata e segundo GREEN. O finding
de materialização integral no write está corrigido dentro do limite acima.
O review remoto do novo head continua sendo a próxima etapa; sem merge.

| Probe de arquivo | DATA | MOVE | Visitas | Lookups | JSON bytes | Tempo observado |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| facts-1500 | 1500 | 1500 | 13508 | 1500 | 3962925 | 1494 ms |
| facts-3000 | 3000 | 3000 | 27008 | 3000 | 7940925 | 2021 ms |
| physical (60012 linhas) | 1 | 1 | 17 | 1 | 5243 | 197 ms |
| shared-data | 1 | 10000 | 40013 | 10000 | 18809400 | 2164 ms |

Tempos incluem frontend, projeção, escrita em arquivo e oracle JSON; não são
comparações de velocidade com a medição anterior, que tinha outro caminho I/O.
Todos os outputs de escala mantêm os SHA-256 da evidência original. A CLI emite
5177 bytes e mantém o alias idêntico, com 414 ms observados; SHA-256:
468e3207f578e428ace89a311eadbd6e27c670331b739675b761479352adc7af.

O [manifest da remediação](evidence/checkpoint-4a/memory-remediation/manifest.json)
contém hashes de fontes/outputs e logs brutos comprimidos, sem substituir a
evidência anterior. O input contract de 4C permanece exatamente o publicado no
[handoff original](checkpoint-4a.md#input-contract-for-checkpoint-4c).
