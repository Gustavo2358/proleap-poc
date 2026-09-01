# Avaliação — WORK-COND-001

## O que prova corretude

Corretude neste Discovery significa que cada expectativa abaixo deriva de regra IBM Enterprise COBOL identificável e diferencia a semântica da linguagem do comportamento atualmente observado no frontend/pipeline. Nenhum oracle pode depender de cardinalidade de corpus, `grammarRule`, ordem de candidates ou resultado atual do resolver. Os IDs `COND-*` são oracles normativos documentais deste Slice 1; os testes opt-in do PR #14 continuam sendo caracterização/futuros requisitos e não são rebatizados como prova verde.

O oracle independente primário é [IBM Enterprise COBOL for z/OS 6.4 Language Reference, SC27-8713-03, atualização de 28 de junho de 2024](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf). Âncoras: **User-defined words** pp. 12–13; **Scope of names** e **Referencing data names...** pp. 63–72; **RENAMES clause** pp. 228–230; **Conditional expressions** pp. 268–289. O compilador IBM é um oracle adversarial desejável quando disponível, especialmente para inputs negativos; o wording exato de diagnostics não é contrato.

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
| COND-P09 | `IF A = B OR NOT C OR D` | Com objects compatíveis, equivale a `(A = B) OR NOT (A = C) OR (A = D)`; logical `NOT` não vira parte do operador nem se propaga a D. |
| COND-P10 | inner program com DATA local `C` e containing program com CONDITION `C` global, em `A = B OR C` | O conjunto de scope inclui ambos; a regra de nested programs seleciona o recurso local aplicável, portanto `C` é object abreviado DATA, não CONDITION externo escolhido por pré-filtro sintático. |

## Classes negativas

| ID local | Fonte/shape | Expectativa normativa |
| --- | --- | --- |
| COND-N01 | `(A = B OR C) AND D`, com `D` apenas data-name | O `)` encerra a inserção herdada; `D` não vira `A = D`. Se não formar condition válida por si, o source é inválido nessa posição. |
| COND-N02 | `(A = B) OR C`, com `C` apenas data-name | A abbreviation não atravessa o `)`; `C` não herda `A =`. |
| COND-N03 | mesma user-defined word declarada como data-name e condition-name no mesmo programa | Input IBM inválido: a palavra não pode pertencer simultaneamente a dois sets de user-defined words. Não existe oracle de “qual candidato vence”. |
| COND-N04 | index-name comparado a data-name não numérico/incompatível | A presença nominal de INDEX não torna a relation válida; aplicar a matriz IBM de comparações. |
| COND-N05 | forma aceita pela grammar que viola a sequência normativa de abbreviated condition, como uma segunda relational operator sem conector admissível | Aceitação sintática local não é prova de COBOL IBM válido; deve permanecer negativa/unsupported até fonte normativa demonstrar o contrário. |
| COND-N06 | `A = (B OR C = D)` | Outro relational operator dentro do scope distribuído viola a restrição IBM de distribuição. |
| COND-N07 | `A = (B OR CONDITION-88)` | Uma simple condition/condition-name dentro do scope distribuído não é object distribuível e torna essa forma inválida. |
| COND-N08 | `A = (NOT B OR C)` | Logical `NOT` imediatamente após o `(` que abre a distribuição é proibido; não confundir com `A = (B OR NOT C)`, cuja validade segue a sequência permitida. |

## Classes ambíguas

1. **Condition-names duplicados e referência insuficientemente qualificada/subscriptada**: as declarações podem ser individualmente admissíveis, mas a referência que não fica única é source inválido. O produto de análise deve preservar todos os candidates e `AMBIGUOUS` para diagnóstico conforme INV-RES-001; não escolher por ordem.
2. **Data-names duplicados e qualification insuficiente**: a referência não única é inválida; qualification suficiente deve desambiguar conforme IBM e a option configurada. Antes disso, preservar candidates/ambiguidade é postura conservadora do projeto, não alegação de execução COBOL válida.
3. **Compiler option de qualification não conhecida**: quando o resultado depender de policy como STANDARD/EXTEND, o oracle deve carregar a configuração; sem ela, estado `UNCERTAIN`, não sucesso presumido.

O caso DATA+CONDITION no mesmo programa não pertence a esta seção: é negativo de validade, não ambiguidade.

## Casos adversariais

- **A1 — branch ANTLR enganoso**: `A = B OR C` com `C` DATA; matar implementação que interpreta `conditionNameReference` como prova de CONDITION.
- **A2 — condition-name real termina herança**: mesma shape textual, mas `C` é condition-name válido; matar implementação que transforma todo bare tail em DATA/INDEX abreviado.
- **A3 — boundary vs distribuição**: comparar `(A = B OR C) AND D` com `A = (B OR C) AND D`; matar implementação que trata todo `)` como terminador ou todo grupo como distribuição.
- **A4 — NOT relacional vs lógico**: comparar `A NOT = B OR C`, `A = B OR NOT C OR D` e `NOT A = B OR C`; matar implementação que propaga logical NOT, exige condition-name autônomo após ele ou trata todo NOT como parte do operador.
- **A5 — precedence**: comparar `A = B OR C AND D` com `(A = B OR C) AND D` e `A = B OR (C AND D)` usando conditions que tornam as árvores semanticamente distinguíveis; matar flattening `MIXED_LOGICAL` sem precedência.
- **A6 — INDEX admissível mas type-sensitive**: um index-name em relation válida e uma variante incompatível; matar `{DATA}` rígido e também aceitação irrestrita de INDEX.
- **A7 — RENAMES é DATA**: trocar um data-name ordinário por level-66 RENAMES semanticamente válido; matar namespace RENAMES inventado ou exclusão por grammar origin.
- **A8 — qualification**: condition-name repetido sob conditional variables distintas, depois tornar uma referência única com `OF` e subscripts; matar lookup só pelo terminal name.
- **A9 — shadowing cross-set em nested programs**: DATA local `C` versus CONDITION global/visível `C` externo na posição contextual de bare tail; matar lookup que pula o recurso local aplicável porque pré-filtrou CONDITION pela grammar.
- **A10 — same-program cross-set invalid**: fixture com DATA `C` e 88 `C` no mesmo programa; matar qualquer oracle que aceite e selecione um deles como regra IBM.
- **A11 — restrições da distribuição**: inserir, separadamente, simple condition, outro relational operator e logical `NOT` imediatamente após o `(` distribuído; matar implementação que trata qualquer grupo após operador como lista livre de objects.

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
