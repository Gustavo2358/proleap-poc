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

## ST-W6.3 — MOVE textual ajustado e múltiplo

Autoridade IBM, consultada 2026-09-14:
[MOVE](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=statements-move-statement),
[ajuste textual](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=items-assigning-values-elementary-data-move).
Subset fixo textual sem JUSTIFIED/conversão: alinhamento à esquerda, descarte
à direita e padding SPACE (1047=0x40). Tamanho igual permite CopyBytes;
tamanho diferente de DATA exige transformação FitText explícita. Literal
é ajustado pelo frontend antes da publicação de bytes. Receivers seguem a
ordem lexical. Captura segura exige que nenhum receiver possa alterar a
origem: todos disjuntos da origem provada. Sobreposição torna TODOS os valores
da sequência desconhecidos, inclusive quando um receiver anterior altera a
origem; não se inventa resultado determinístico para comportamento imprevisível
na regra IBM. Overlap entre receivers é permitido e preserva a ordem. Referências
modificadas admitidas têm posições/length constantes, portanto avaliação do
endereço é invariável. Sem limite artificial de receivers; uma visita por
receiver e consulta O(1) ao layout. Oracle de padding: AB→4 = C1 C2 40 40.

## ST-W6.4 — CORRESPONDING textual fixo

Autoridade: IBM Enterprise COBOL 6.4 Language Reference, CORRESPONDING phrase
(https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf), índice oficial consultado
em 2026-09-14; página IBM Docs 6.4 indisponível (403). A regra exige nomes e
qualificadores iguais até excluir os dois grupos raiz, identificação única e
exclui FILLER e descrições subordinadas REDEFINES/RENAMES/OCCURS/index/pointer.
Raiz pode redefinir; referência modificada como raiz não é admitida.

Subset: ambos os grupos e todos os pares selecionados precisam de layout fixo
textual; par grupo/elementar, numérico, qualificação ambígua ou FILLER agrupador
mantém fallback explícito. Não se descartam pares fora do perfil para certificar
os restantes. O frontend indexa descendentes por caminho qualificado, seleciona
pares e os ordena pela declaração fonte. Efeitos usam as regras W6.3. Grupos que
podem se sobrepor produzem valores desconhecidos para todos os pares, impedindo
captura tardia inventada; pares de grupos fisicamente disjuntos são independentes.
Custo linear no inventário e no tamanho dos caminhos, sem produto cartesiano.
SP2.11 já transporta a sequência de pares como source/target/effect, com IDs e
provenance de declaração e statement. Lower não faz matching nem cópia de grupo.
Oracle: SRC A(2),FILLER(1),B(2),ONLY-S(1); DST B(1),ONLY-D(3),A(4)
produz apenas A→A (2→4) e B→B (2→1), nessa ordem; bytes de ONLY-D preservados.

## ST-W7.1 — VALUE e perfil explícito de entrada

Autoridade: IBM Enterprise COBOL6.4 VALUE format1 e WORKING-STORAGE
(https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=vc-format-1;
https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=overview-working-storage-section),
conteúdo indexado IBM consultado2026-09-14; páginas diretas403. WORKING-STORAGE
persiste no último estado; VALUE assegura a primeira inicialização, não todo
reingresso. Não se infere primeira chamada de Entry PRIMARY.

Perfil explícito --entry-storage-state initial|preserved|unknown, default unknown.
INITIAL é condição de execução selecionada pelo produtor/usuário, não fato
inferido da grafia do programa. Literais alfanuméricos simples no layout fixo1047
podem fornecer condição simultânea inicial; texto curto recebe SPACE à direita;
literal maior que a área é inválido para VALUE e não é truncado como MOVE.
PRESERVED publica preservação; UNKNOWN mantém razão explícita. Sem VALUE, bytes
permanecem abertos. VALUE não altera tamanho/layout e nunca gera MOVE de fluxo.
VALUE subordinado a VALUE ou a REDEFINES é excluído pela regra fonte. OCCURS,
NATIONAL, numérico, figurativos e programas especiais continuam fora do subset.

Algoritmo: uma visita indexada a declarações, carregando flags ancestrais VALUE/
REDEFINES; mapa de views físicas preparado. Emitir condições por declaração, sem
varrer pares/bytes desconhecidos. Literal1047 provado e padding explícito apenas
para faixa conhecida; lower recebe fatos e modo, sem parsingdeVALUE. Origem inclui
cláusula VALUE e declaração. AIR EntryState existente é a fronteira canônica;
contradição entre condições regionais é erro, não last-write-wins.

W7.3: fixture misto combina escalar inteiro usado por controle, grupo com VALUE e
REDEFINES, OCCURS e NATIONAL. Os dois últimos mantêm extensão/codec desconhecidos,
sem ampliar o perfil. Prova de alocação local separa componentes, enquanto EXTERNAL
remove a prova; CALL literal permanece publicado em ambos os casos.
