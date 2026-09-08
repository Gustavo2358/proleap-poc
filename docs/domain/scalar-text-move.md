# Profile elementar textual de MOVE — Checkpoint 4A

Este contrato COBOL-specific complementa o [Semantic Product](cobol-semantic-product.md).
A versão pública é **1.2.0**. Não define AIR, lowering, CFG, efeitos globais ou dataflow.

## Regra e autoridade

Autoridade: Enterprise COBOL for z/OS 6.4, consultada em 2026-09-08.
O frontend aplica as regras de [MOVE elementar](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=items-assigning-values-elementary-data-move):
no envio de caracteres, um receptor menor exige truncamento e um receptor maior
exige preenchimento. O profile de comprimentos iguais dispensa ambos.
[Categorias de PICTURE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=clause-data-categories-picture-rules)
e [símbolos de PICTURE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=clause-symbols-used-in-picture)
estabelecem categoria alfanumérica, DISPLAY e uma posição lógica por X.
A [referência da linguagem 6.4](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf),
seções Basic alphanumeric literals, MOVE statement e Sequence of execution,
fundamenta delimitadores duplicados, escrita no receptor e sequência normal.

Premissas LANGUAGE_GUARANTEED: regras acima. ARCHITECTURE_GUARANTEED: AST,
resolução e provenance canônicas. SPECIFICATION_GUARANTEED: o profile positivo
restrito abaixo. Nenhuma premissa depende de spellings ou estatísticas de corpus.

## Domínio admitido e invariantes

`DataDeclaration.scalarText` é opcional. Sua presença prova um item local,
standalone, elementar, nível 01 ou 77, em WORKING-STORAGE de programa comum
(sem INITIAL, RECURSIVE, COMMON, LIBRARY ou DEFINITION), com apenas PICTURE X
(e repetição positiva), e USAGE DISPLAY opcional. Extensão positiva conhecida;
sem filhos, FILLER, VALUE ou cláusulas adicionais. GLOBAL/EXTERNAL não entram.

O frontend examina a seção canônica inteira uma vez: qualquer REDEFINES,
RENAMES ou cláusula DATA preservada impede promoção na seção, inclusive no
item original redefinido por uma declaração posterior. OCCURS no item ou em
ancestor não entra: somente roots elementares são admitidos. Essa exclusão
conservadora não implementa alias analysis nem promete disjunção universal.
Campos ausentes no SP não fornecem prova sobre construções COBOL ausentes.

`LiteralSource.logicalValue` é opcional e contém `TextValue(value)`, domínio
TEXT e extensão lógica. O builder interpreta a alternativa básica do token
NONNUMERICLITERAL, normaliza delimitadores duplicados e admite somente os
caracteres básicos U+0020–U+007E, com conteúdo não vazio. O token também cobre
hex, national/DBCS e null-terminated: essas alternativas continuam UNKNOWN;
números, figurativos e CICS não viram TEXT. `value` conserva o valor normalizado
preexistente e coincide com logicalValue.value quando a prova existe. Texto
lógico não define encoding físico EBCDIC, ASCII ou UTF-8.

`DataReference.wholeItemAccess` é opcional. Presença prova que **essa ocorrência**
acessa diretamente o valor escalar completo do `data` referenciado. Exige
resolução única VALUE_WRITE, referência STRUCTURED, sem qualifiers, subscripts
ou reference modification, e declaração elegível. O core exige concordância
entre `wholeItemAccess.data`, `binding.selected` e a declaração com scalarText.
Binding sozinho e construtores legados não criam essa prova.

`MoveFact.copySemantics=FULL_IDENTITY` é contrato tipado: escrita obrigatória
do receptor inteiro por cópia lógica de identidade, sem conversão, padding ou
truncation. Exige MOVE não CORRESPONDING, uma origem literal provada, exatamente
um destino elegível e extensões lógicas iguais. `UNAVAILABLE` não faz essa claim.
O core rejeita prova sem literal lógico, acesso inteiro ou extensão compatível.
Remover readiness/scope textual não remove nenhum desses fatos.

`MoveFact.normalContinuation` possui availability KNOWN, UNAVAILABLE ou NONE,
statement opcional e provenance. KNOWN exige StatementId existente na mesma unit;
os demais estados não possuem statement. O builder publica relações entre
statements diretos consecutivos, dentro da mesma região de sentences (corpo sem
paragraph, ou um paragraph). Relação entre sentences da mesma região é conhecida.
Fim de paragraph/section/procedure, contexto aninhado, declaratives ou metadata
não fornecida ficam UNAVAILABLE. Este profile nunca emite NONE para MOVE.
Ausência física não prova término. A relação não é reachability: um MOVE
fisicamente posterior a GOBACK pode ter continuação própria, mas GOBACK mantém
CURRENT_PROGRAM_INVOCATION/NONE. Nenhum outro controle foi implementado.

## Autoridades, complexidade e integridade

AstBuilder estabelece LogicalText, PictureClause.textExtent, UsageClause.display
e Division.normalContinuations a partir dos contextos canônicos; as relações
não-node reutilizam IDs sem alterar traversal ou duplicar statements.
`ScalarMoveSemantics.analyze` produz um snapshot pós-binding imutável na
composição do frontend, antes da projeção. O projector apenas traduz enums,
valores e referências por identidade. O writer lê somente o port.

O enriquecimento usa um walk da AST, uma inspeção adicional limitada aos nós da
seção DATA para exclusões, um passe de símbolos e um passe de resoluções.
Índices temporários: node ID → shape elegível e (unit, target node ID) → MOVE.
Índices do produto: SemanticEntityId → ScalarText e (unit, MOVE node ID) → fatos.
Cada referência examinada faz no máximo uma consulta escalar; nenhum MOVE
percorre DATA. Esses índices têm ownership diferente dos índices de fechamento
do projector e não duplicam declarações por ocorrência. Contadores incluem
visitas das exclusões/cláusulas, símbolos, resoluções, MOVEs e consultas escalares.

Custo O(N + R + L), onde L é o texto de literals/PICTURE realmente interpretado;
memória O(N + R) mais valores publicados. Repetições PIC são somadas com overflow
check, nunca expandidas. Não há novo namespace nem identidade textual/hexadecimal.
Valores de literal são referenciados; não há cópia de source/provenance para joins.
O core reutiliza seus índices existentes para validar acesso, extensão e continuação.

Input incompleto impede provas escalares, cópia e continuação. Cases fora do
profile preservam MOVE/operands/binding/provenance e gaps
SCALAR_WHOLE_ITEM_NOT_PROVEN, MOVE_IDENTITY_NOT_PROVEN e
NORMAL_CONTINUATION_NOT_AVAILABLE conforme a prova ausente; literal não coberto
mantém LITERAL_KIND_NOT_PUBLISHED. Report/containment gaps independentes permanecem.

Readiness lowering fortalece apenas a cópia provada; cfg apenas a relação
canônica conhecida; effects/dataflow continua PARTIAL e GOBACK continua BLOCKED
nessa dimensão. EntryInventory PRIMARY_ONLY/PARTIAL não é fechado. Capability
precisa local não é publicação globalmente completa nem certificação AIR.

## Transporte e compatibilidade

1.2.0 é evolução minor aditiva: `scalarText`, `logicalValue`, `wholeItemAccess`,
`copySemantics`, `normalContinuation` são explícitos no JSON; opcionais não provados
são null. Fields antigos mantêm significado. ALPHANUMERIC só substitui UNKNOWN
onde há interpretação canônica positiva. IDs e ordem continuam determinísticos,
sem promessa de persistência entre versões/edições.

Não existe reader de produção neste repo: o adapter é writer. Test readers
ObjectMapper e os contract gates passam a reconhecer o shape 1.2.0. Consumers
externos fechados em 1.1.0 devem reconhecer 1.2.0 ou rejeitar explicitamente.
Os construtores Java anteriores preservam ausência das novas provas. O golden
[CP3 1.1.0](../../src/test/resources/cobol/semantic/entry-goback-sp-1.1.0.json)
foi produzido do baseline c8a891e0827ae1dc1140246f625fd16c2ac9bd97, sem mudanças.
Seu payload Entry/GOBACK permanece idêntico sob 1.2.0 (exceto contractVersion).
Nenhuma migração foi feita em cobol-lower; o próximo 4C deve admitir a nova versão.

## Evals

EVAL-SP-005 cobre E1–E4, oracle independente, contracasos e falsificações.
EVAL-SP-006 cobre escala física, massa de fatos e cardinalidade por referência.
INV-SP-008 fixa a prova positiva. [Evidência e handoff 4C](../evals/checkpoint-4a.md).
