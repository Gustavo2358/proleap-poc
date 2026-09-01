# Avaliação — WORK-COND-001

## O que prova corretude

Corretude neste Discovery significa que cada expectativa abaixo deriva de regra IBM Enterprise COBOL identificável e diferencia a semântica da linguagem do comportamento atualmente observado no frontend/pipeline. Nenhum oracle pode depender de cardinalidade de corpus, `grammarRule`, ordem de candidates ou resultado atual do resolver.

O oracle independente primário é a IBM Enterprise COBOL Language Reference. O compilador IBM é um oracle adversarial desejável quando disponível, especialmente para inputs negativos; o wording exato de diagnostics não é contrato.

## Classes positivas

| ID local | Fonte COBOL mínima | Expectativa normativa |
| --- | --- | --- |
| COND-P01 | `IF A = B OR C` | Com `A/B/C` data-names compatíveis, `C` é object abreviado: equivalente a `A = B OR A = C`. |
| COND-P02 | `IF A = B OR C OR D` | `C` e `D` herdam último subject/operator enquanto nenhum terminador ocorre. |
| COND-P03 | `IF A = B OR < C` | O subject pode ser omitido com relational operator explícito; `A` continua subject. |
| COND-P04 | `IF A NOT = B OR C` | `NOT =` é relational operator; `C` herda esse último relational operator. |
| COND-P05 | `IF A = (B OR C) AND D` | `=` é distribuído a B/C; ao fechar o grupo, `A` e `=` continuam correntes e D pode continuar a abbreviation. |
| COND-P06 | `IF N = IDX OR IDX2` | `IDX`/`IDX2` podem ser index-names quando a comparação satisfaz as regras IBM para index-name. |
| COND-P07 | `66 R RENAMES X THRU Y.` e uso relacional válido de `R` | `R` é data-name; RENAMES não cria um namespace nominal separado. |
| COND-P08 | condition-name qualificado/subscriptado conforme sua conditional variable | A referência CONDITION preserva qualification e a mesma combinação de subscripts exigida pela conditional variable. |

## Classes negativas

| ID local | Fonte/shape | Expectativa normativa |
| --- | --- | --- |
| COND-N01 | `(A = B OR C) AND D`, com `D` apenas data-name | O `)` encerra a inserção herdada; `D` não vira `A = D`. Se não formar condition válida por si, o source é inválido nessa posição. |
| COND-N02 | `(A = B) OR C`, com `C` apenas data-name | A abbreviation não atravessa o `)`; `C` não herda `A =`. |
| COND-N03 | mesma user-defined word declarada como data-name e condition-name no mesmo programa | Input IBM inválido: a palavra não pode pertencer simultaneamente a dois sets de user-defined words. Não existe oracle de “qual candidato vence”. |
| COND-N04 | index-name comparado a data-name não numérico/incompatível | A presença nominal de INDEX não torna a relation válida; aplicar a matriz IBM de comparações. |
| COND-N05 | forma aceita pela grammar que viola a sequência normativa de abbreviated condition, como uma segunda relational operator sem conector admissível | Aceitação sintática local não é prova de COBOL IBM válido; deve permanecer negativa/unsupported até fonte normativa demonstrar o contrário. |

## Classes ambíguas

1. **Dois condition-names válidos com o mesmo nome que permanecem elegíveis após qualification/scope**: a resolução deve preservar ambiguidade conforme INV-RES-001; o Slice 1 não escolhe candidate.
2. **Dois data-names qualificáveis com mesmo nome**: qualification insuficiente é ambígua; qualification suficiente deve desambiguar conforme IBM e policy configurada.
3. **Homônimo em nested programs de sets diferentes**: é permitido que programas distintos definam a mesma grafia. A elegibilidade depende de local/global scope e não autoriza filtrar primeiro pelo namespace que a grammar sugeriu. Oracle adversarial central: inner program possui DATA `C`; containing program expõe CONDITION `C` quando aplicável. A declaração local não deve ser ignorada por conveniência sintática.
4. **Compiler option de qualification não conhecida**: quando o resultado depender de policy como STANDARD/EXTEND, o oracle deve carregar a configuração; sem ela, estado `UNCERTAIN`, não sucesso presumido.

O caso DATA+CONDITION no mesmo programa não pertence a esta seção: é negativo de validade, não ambiguidade.

## Casos adversariais

- **A1 — branch ANTLR enganoso**: `A = B OR C` com `C` DATA; matar implementação que interpreta `conditionNameReference` como prova de CONDITION.
- **A2 — condition-name real termina herança**: mesma shape textual, mas `C` é condition-name válido; matar implementação que transforma todo bare tail em DATA/INDEX abreviado.
- **A3 — boundary vs distribuição**: comparar `(A = B OR C) AND D` com `A = (B OR C) AND D`; matar implementação que trata todo `)` como terminador ou todo grupo como distribuição.
- **A4 — NOT relacional vs lógico**: comparar `A NOT = B OR C` com `A = B OR NOT C`; matar implementação que herda logical NOT ou que trata todo NOT como boolean negation.
- **A5 — precedence**: comparar `A = B OR C AND D` com `(A = B OR C) AND D` e `A = B OR (C AND D)` usando conditions que tornam as árvores semanticamente distinguíveis; matar flattening `MIXED_LOGICAL` sem precedência.
- **A6 — INDEX admissível mas type-sensitive**: um index-name em relation válida e uma variante incompatível; matar `{DATA}` rígido e também aceitação irrestrita de INDEX.
- **A7 — RENAMES é DATA**: trocar um data-name ordinário por level-66 RENAMES semanticamente válido; matar namespace RENAMES inventado ou exclusão por grammar origin.
- **A8 — qualification**: condition-name repetido sob conditional variables distintas, depois tornar uma referência única com `OF` e subscripts; matar lookup só pelo terminal name.
- **A9 — shadowing cross-set em nested programs**: DATA local `C` versus CONDITION global/visível `C` externo; matar lookup que pula o nível local porque pré-filtrou CONDITION.
- **A10 — same-program cross-set invalid**: fixture com DATA `C` e 88 `C` no mesmo programa; matar qualquer oracle que aceite e selecione um deles como regra IBM.

## Casos de regressão

- `src/test/resources/cobol/resolution/abbreviated-condition-context.cbl` e `SemanticConditionContextDiscoveryTest` preservam caracterização do PR #14, mas seus resultados atuais são evidência de implementação, não expected normativo automático.
- O caso original `IF A = B OR C OR D` deve permanecer sentinel obrigatório para o Slice 2.
- A matriz histórica de 19 shapes deve ser reclassificada contra estes oracles antes de qualquer alteração de produção; shapes aceitas pela grammar e não sustentadas pela IBM não entram como language-positive.
- O antigo sentinel “DATA + CONDITION homônimos no mesmo programa resolve CONDITION” deve ser explicitamente tratado como frontend characterization de source IBM inválido, nunca como regressão semântica desejada.

## Propriedades/relações metamórficas

1. **Expansão de abbreviation**: substituir uma abbreviated relation válida pela forma completa semanticamente equivalente não deve alterar o conjunto de entidades nominais referidas nem o truth structure, descontada a representação sintática deliberadamente distinta.
2. **Qualification preservadora**: adicionar qualification suficiente e semanticamente redundante a uma referência já única não deve trocar a entidade resolvida.
3. **Case-insensitive**: variar caixa de user-defined words não muda a decisão nominal COBOL.
4. **Parênteses semanticamente redundantes**: adicionar parênteses que não cruzem boundary de abbreviation nem alterem precedência não muda o significado; parênteses que cruzem essas fronteiras são deliberadamente não metamórficos e servem como contraexemplo.
5. **Renomeação alfa**: renomear consistentemente declarações e referências não relacionadas não muda a classificação DATA/INDEX/CONDITION dos operands sob teste.
6. **Declaração não relacionada**: adicionar um nome não colidente em scope irrelevante não pode alterar o resultado; adicionar homônimo relevante pode alterar para ambiguous/shadowed conforme regra, nunca por hash/order.

## Expectativas de escala

Este Slice 1 não altera algoritmo nem impõe threshold de performance. Os oracles devem ser fixtures mínimas e locais. O único requisito de escala é que a futura solução do Slice 2 derive de contexto estrutural/nominal e não de scan textual/global; qualquer propriedade algorítmica será especificada no slice que autorizar produção.
