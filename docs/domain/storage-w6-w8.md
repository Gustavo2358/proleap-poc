# Storage Semantics ST-W6–ST-W8

Escopo autorizado: RENAMES fixo, slices constantes, MOVE textual ajustado/múltiplo,
CORRESPONDING textual, VALUE regional, escopos e perfil misto. Desenvolvimento
G0/G1 focal; FAST e witness apenas no fechamento de W6/W7; qualificação em W8.

## ST-W6.1 — prova de RENAMES

Autoridade: IBM Enterprise COBOL Language Reference, seção RENAMES, confrontada
com a [página IBM](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=entry-renames-clause)
em 2026-09-14. A página 6.4 retornou 403; a referência 6.4 continua sendo a
autoridade do perfil, e a regra compartilhada foi verificada na publicação IBM.

Premissas LANGUAGE_GUARANTEED: nível 66 não aloca storage; endpoints são itens
02–49 do registro 01 associado; THROUGH inclui a área entre extremos e não
admite extremo final subordinado ao inicial. Sem THROUGH herda categoria/extensão.
Premissas de perfil ARCHITECTURE_GUARANTEED: layout fixo DISPLAY/IBM1047 preparado,
resolução nominal única, nenhuma ocorrência/tamanho dinâmico admitido.

Algoritmo: separar descrições de aliases das alocações; preparar layout uma vez;
indexar entidades/views/ancestria; provar registro, ordem e limites. O perfil
inicial exige registro físico inteiro conhecido, garantindo também os nós
intermediários e ancestrais. Publicar alias como vista na mesma base, conservando
endpoints/origem em relação própria. Falha mantém alias desconhecido, sem Cell
substituta. Índices e floresta finitos asseguram terminação; preparação linear,
prova por alias limitada à profundidade de ancestrais, sem tabela de pares.

Oracle independente: registro 2+FILLER1+3 tem extent6; alias do último campo tem
offset3/extent3; alias THROUGH tem offset0/extent6; uma única base. Endpoints
invertidos, raiz, mesmo item, outro registro e intermediário desconhecido recusam.

## ST-W6.2 — reference modification constante

[Regra IBM de reference modification](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=reference-modification), verificada em 2026-09-14: posição é ordinal a partir de1;
DISPLAY usa um byte por caractere. Perfil exige posição p e length n literais
inteiros positivos, ambos presentes, p+n−1 ≤ extent. Sem suporte a ZLEN ou
comprimento omitido nesta extensão. Oracle: texto em offset2, p=6,n=1 → [7,8).
Algoritmo O(1) por acesso após layout: offset absoluto view.offset+p−1, extent n.
Prova exige codec/perfil1047 e bounds do item; origem da ocorrência preservada.
Dinâmica e parâmetros não provados não recebem acesso inteiro substituto.
SP2.10/storage1.2 acrescenta slice nullable em regionalAccess; versões anteriores
continuam fechadas no reader. AIR usa RegionSlice existente, com codec explícito.
