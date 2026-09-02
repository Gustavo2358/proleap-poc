# Oracles normativos de condições combinadas e abreviadas

## Status e uso

Os IDs `COND-*` são sentinelas normativos derivados do [contrato de expressões condicionais](../domain/conditional-expressions.md). Eles devem orientar os próximos slices de `BACKLOG-COND-001`, mas não alegam que a produção atual já atende às expectativas nem substituem futuros asserts executáveis.

O oracle independente primário é [IBM Enterprise COBOL for z/OS 6.4 Language Reference, SC27-8713-03, atualização de 28 de junho de 2024](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf), nas seções e páginas registradas pelo contrato de domínio. O compilador IBM é oracle adversarial desejável para inputs negativos quando estiver disponível; wording e código exatos de diagnostics não são contrato atual.

Atribuição por fase dos casos INDEX (`COND-P06`, `COND-N04`, `COND-A06`): o binding nominal identifica INDEX; `ConditionSemantics` materializa a relation abreviada; a admissibilidade type-sensitive pertence a `ConditionValidation` conceitual (futura), que consome `ConditionSemantics`, declaração/tipo e os contratos IBM. Binding nominal correto não declara a relation válida: uma implementação equivalente a `candidate.kind() == INDEX ⇒ relation válida` é proibida.

## Classes positivas

| ID | Fonte COBOL mínima | Expectativa normativa |
| --- | --- | --- |
| COND-P01 | `IF A = B OR C` | Com `A/B/C` data-names compatíveis, `C` é object abreviado: equivalente a `A = B OR A = C`. |
| COND-P02 | `IF A = B OR C OR D` | `C` e `D` herdam o último subject/operator enquanto nenhum terminador ocorre. |
| COND-P03 | `IF A = B OR < C` | O subject pode ser omitido com relational operator explícito; `A` continua subject. |
| COND-P04 | `IF A NOT = B OR C` | `NOT =` é relational operator; `C` herda esse último operator. |
| COND-P05 | `IF A = (B OR C) AND D` | `=` é distribuído a B/C; após o grupo, `A` e `=` continuam correntes e D pode continuar a abbreviation. |
| COND-P06 | `IF N = IDX OR IDX2` | `IDX`/`IDX2` podem ser index-names quando a comparação satisfaz as regras IBM aplicáveis. |
| COND-P07 | `66 R RENAMES X THRU Y.` e uso relacional válido de `R` | `R` é data-name; RENAMES não cria namespace separado. |
| COND-P08 | condition-name qualificado/subscriptado conforme sua conditional variable | A referência CONDITION preserva qualification e os subscripts exigidos pela conditional variable. |
| COND-P09 | `IF A = B OR NOT C OR D` | Com objects compatíveis, equivale a `(A = B) OR NOT (A = C) OR (A = D)`; logical `NOT` não se propaga a D. |
| COND-P10 | inner program com DATA local `C` e containing program com CONDITION `C` global, em `A = B OR C` | Scope inclui os nomes elegíveis, mas a regra de nested programs seleciona o recurso local aplicável; pré-filtro sintático CONDITION não pode saltar a DATA local. |
| COND-P11 | `IF A = B OR C = D OR E` | `C = D` redefine o estado corrente; equivale a `(A = B) OR (C = D) OR (C = E)`. |

## Classes negativas

| ID | Fonte/shape | Expectativa normativa |
| --- | --- | --- |
| COND-N01 | `(A = B OR C) AND D`, com `D` apenas data-name | O `)` encerra a inserção; `D` não vira `A = D` e o source é inválido se D não formar condição por si. |
| COND-N02 | `(A = B) OR C`, com `C` apenas data-name | A abbreviation não atravessa o `)`; `C` não herda `A =`. |
| COND-N03 | mesma user-defined word declarada como data-name e condition-name no mesmo programa | Input IBM inválido; não existe oracle de “qual candidate vence”. |
| COND-N04 | index-name comparado a data-name não numérico/incompatível | INDEX nominalmente admissível não torna a relation válida; aplicar as restrições IBM de comparação. |
| COND-N05 | shape aceita pela grammar, mas fora da sequência normativa de abbreviated condition | Aceitação sintática local não prova COBOL IBM válido; permanecer negativa/unsupported sem outra autoridade. |
| COND-N06 | `A = (B OR C = D)` | Outro relational operator dentro do scope distribuído viola a restrição IBM. |
| COND-N07 | `A = (B OR CONDITION-88)` | Nova simple condition dentro do scope distribuído não é object distribuível. |
| COND-N08 | `A = (NOT B OR C)` | Logical `NOT` imediatamente após o `(` que abre a distribuição é proibido. |
| COND-N09 | `A = B OR C IS NUMERIC OR D`, com `D` apenas data-name | A class condition encerra a inserção herdada; `D` não herda `A =`. |

## Classes ambíguas e incertas

1. Condition-names duplicados cuja referência não fica única após qualification/subscripts preservam todos os candidates e `AMBIGUOUS`; não há selected candidate.
2. Data-names duplicados com qualification insuficiente seguem a mesma postura conservadora; qualification suficiente deve desambiguar segundo a opção IBM aplicável.
3. Quando a decisão depender de policy como `QUALIFY(STANDARD)`/`QUALIFY(EXTEND)` e a configuração não for conhecida, o resultado permanece incerto/unsupported.

DATA+CONDITION homônimos no mesmo programa não pertencem a esta classe: COND-N03 os classifica como source inválido.

## Challenge pass obrigatório

- **COND-A01 — branch ANTLR enganoso:** `A = B OR C` com `C` DATA rejeita a suposição `conditionNameReference ⇒ CONDITION`.
- **COND-A02 — condition-name real termina herança:** a mesma shape com `C` condition-name rejeita a promoção universal de todo bare tail a DATA/INDEX.
- **COND-A03 — boundary versus distribuição:** `(A = B OR C) AND D` versus `A = (B OR C) AND D` rejeita uma política uniforme para todo `)`.
- **COND-A04 — `NOT` relacional versus lógico:** `A NOT = B OR C`, `A = B OR NOT C OR D` e `NOT A = B OR C` rejeitam propagação ou classificação uniforme de `NOT`.
- **COND-A05 — precedência:** `A = B OR C AND D`, `(A = B OR C) AND D` e `A = B OR (C AND D)` rejeitam flattening `MIXED_LOGICAL`.
- **COND-A06 — INDEX type-sensitive:** uma relation válida e uma incompatível rejeitam tanto `{DATA}` rígido quanto aceitação irrestrita de INDEX.
- **COND-A07 — RENAMES é DATA:** substituir data-name ordinário por level-66 válido rejeita namespace RENAMES inventado.
- **COND-A08 — qualification:** condition-name repetido e depois tornado único com `OF`/subscripts rejeita lookup apenas pelo terminal name.
- **COND-A09 — shadowing cross-set:** DATA local versus CONDITION global externa rejeita pré-filtro sintático que salta o primeiro nível nominal aplicável.
- **COND-A10 — same-program cross-set inválido:** DATA e CONDITION homônimos no mesmo programa rejeitam qualquer regra de precedência entre esses sets.
- **COND-A11 — restrições da distribuição:** inserir simple condition, outro relational operator ou logical `NOT` logo após o `(` distribuído rejeita grupo tratado como lista livre.
- **COND-A12 — atualização do estado:** `A = B OR C OR D` versus `A = B OR C = D OR E` rejeita congelar o primeiro subject/operator como estado permanente.
- **COND-A13 — término por qualquer simple condition:** condition-name e `C IS NUMERIC` rejeitam uma regra de término limitada a condition-name.

## Regressão e provenance

- `src/test/resources/cobol/resolution/abbreviated-condition-context.cbl` e `SemanticConditionContextDiscoveryTest` preservam a caracterização do PR #14; resultados atuais não são expected normativo automático.
- `COND-P01`/`COND-P02` permanecem sentinelas obrigatórios do primeiro slice executável.
- A matriz histórica de 19 shapes deve ser reclassificada por este contrato antes de promover um caso a language-positive.
- O sentinel histórico DATA+CONDITION homônimo deve permanecer teste negativo de validade/frontend, não regressão de binding desejada.
- Casos com declaration vinda de COPY precisam manter arquivo físico, include chain e exatidão de provenance; COPY não pode alterar a classe semântica.

## Propriedades e relações metamórficas

1. Expandir uma abbreviated relation válida para a forma completa equivalente preserva entidades nominais e truth structure, descontada a representação deliberadamente distinta de escrito/herdado.
2. Adicionar qualification suficiente e redundante a referência já única não troca a entidade resolvida.
3. Variar caixa de user-defined words não muda a decisão nominal.
4. Adicionar parênteses que não cruzem boundary de abbreviation nem alterem precedência preserva significado; cruzar essas fronteiras é contraexemplo, não metamorfismo.
5. Renomear consistentemente declarações/referências não relacionadas preserva as classes DATA/INDEX/CONDITION.
6. Adicionar nome não colidente em scope irrelevante não altera o resultado; homônimo relevante pode produzir shadowing/ambiguidade conforme a regra, nunca por ordem de coleção.

## Expectativas de escala

Os casos mínimos são locais e não definem cardinalidade de corpus. A implementação futura deve percorrer estrutura e joins indexados, sem scan textual/global ou complexidade `O(condition references × all declarations)`. Threshold dependente de hardware não faz parte destes oracles.
