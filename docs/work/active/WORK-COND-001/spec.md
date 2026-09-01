# WORK-COND-001 — Contrato normativo de condições combinadas e abreviadas

## Problema

O Discovery mergeado no PR #14 demonstrou que formas como `IF A = B OR C OR D` podem chegar ao pipeline atual com bare tails classificados sintaticamente como `conditionNameReference`, congelando `C` e `D` em namespace CONDITION antes do binding. O mesmo relatório encontrou perdas adicionais em abbreviations, parênteses, precedência, subscripts e SEARCH. Essa evidência comprova comportamento da implementação atual; ela não define a linguagem.

Antes de qualquer correção é necessário fechar a menor regra normativa IBM capaz de distinguir: continuação abreviada de relation-condition, nova simple condition/condition-name, boundaries parentéticos, `NOT` relacional/lógico, precedência, qualification e os conjuntos nominais relevantes. Este work item promove somente o Slice 1 de `BACKLOG-COND-001`.

## Objetivo

Estabelecer um contrato revisável, derivado de IBM Enterprise COBOL, e uma matriz de oracles que permita ao Slice 2 comparar alternativas arquiteturais sem inferir semântica a partir da parse tree atual ou do corpus. Nenhuma representação interna é escolhida aqui.

## Domínio de entrada suportado

Este Slice 1 cobre condições IBM Enterprise COBOL que combinam relation-conditions com `AND`, `OR`, `NOT` e parênteses, incluindo abbreviated combined relation conditions; referências nominais usadas como operandos ou simple conditions; qualification/subscripts apenas no grau necessário para determinar a classe nominal; e scope/visibility de nested programs quando altera quais nomes são elegíveis.

Uma única publicação oficial é autoridade suficiente e evita misturar releases: [IBM Enterprise COBOL for z/OS 6.4 Language Reference, SC27-8713-03, atualização de 28 de junho de 2024](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf). As menores seções necessárias são:

| Regra | Seção e páginas impressas |
| --- | --- |
| conjuntos nominais e unicidade entre sets | **User-defined words**, pp. 12–13 |
| local/global, nested programs, uniqueness, qualification, condition-name e index-name | **Scope of names** e **Referencing data names...**, pp. 63–72 |
| level-66 como data-name | **RENAMES clause**, pp. 228–230 |
| simple/general relation/complex/abbreviated conditions | **Conditional expressions**, pp. 268–289, especialmente 283–289 |

As páginas IBM Docs equivalentes podem facilitar navegação, mas não acrescentam uma segunda autoridade. Este contrato usa a edição/data acima para que review posterior não dependa de conteúdo continuamente atualizado sem versão.

A evidência histórica local permanece `docs/history/evidence/semantic-condition-context-discovery-report.md`; ela é comparativa, não normativa.

## Classes semânticas

1. **Relation completa**: sujeito e relational operator escritos explicitamente.
2. **Abbreviated relation por sujeito omitido**: operador explícito reutiliza o último sujeito.
3. **Abbreviated relation por sujeito e operador omitidos**: um object após `AND`/`OR` reutiliza o último sujeito e o último relational operator enquanto a sequência de inserção permanecer aberta.
4. **Simple condition nova**: encerra a inserção herdada; um condition-name é uma simple condition e não um object abreviado.
5. **Parêntese de boundary**: o `)` que corresponde a um `(` situado à esquerda do sujeito corrente encerra a sequência herdada.
6. **Parêntese de distribuição**: `(` imediatamente após um relational operator distribui esse operador pelos objects internos; ao fechar `)`, sujeito e operador continuam correntes.
7. **`NOT` relacional**: quando integra um relational operator, pertence ao operador e pode ser o último operador herdável.
8. **`NOT` lógico**: nega apenas a relation-condition imediatamente seguinte e não é propagado com sujeito/operador; a relation negada ainda pode ser abreviada, como no object de `A = B OR NOT C`.
9. **Conectores mistos**: `AND` possui precedência sobre `OR`, salvo agrupamento explícito.
10. **DATA**: data-name é o conjunto nominal de dados. Um level-66 `RENAMES` declara um data-name; RENAMES não cria namespace nominal concorrente próprio.
11. **INDEX**: index-name declarado por `INDEXED BY` pertence a conjunto nominal distinto e pode ser operando de relation-condition dentro das restrições IBM de comparação. Um index data item (`USAGE INDEX`) continua sendo data-name e não é index-name.
12. **CONDITION**: condition-name nível 88 pertence a conjunto nominal próprio, referencia sua conditional variable e pode exigir qualification/subscripts correspondentes.
13. **Scope/visibility**: DATA, CONDITION e INDEX são locais por padrão nas declarações relevantes; `GLOBAL` torna DATA/CONDITION e o index associado elegíveis em programas contidos. Em nested programs, IBM reúne nomes do programa interno e nomes globais dos programas contendo, aplica qualification/uniqueness e prefere o recurso declarado no programa interno, ou depois no programa contendo mais próximo, quando mais de um recurso permanece identificado.
14. **Homônimo DATA + CONDITION no mesmo programa**: não é uma classe de binding válida. IBM determina que uma user-defined word pertence a apenas um conjunto de nomes; declarar a mesma palavra simultaneamente como data-name e condition-name no mesmo programa é input COBOL inválido. Homônimos em programas distintos permanecem sujeitos às regras de scope.

## Premissas

| Premissa | Classificação | Consequência neste slice |
| --- | --- | --- |
| Após a primeira relation-condition, subject ou subject+operator podem ser omitidos e são semanticamente inseridos a partir dos últimos explicitados | `LANGUAGE_GUARANTEED` | Oracle de expansão semântica não depende do ramo ANTLR escolhido. |
| Uma simple condition e um condition-name encerram a sequência de inserção herdada | `LANGUAGE_GUARANTEED` | Bare word não pode ser congelada como CONDITION antes de decidir se ocupa a posição de object abreviado ou inicia simple condition. |
| O `)` correspondente a `(` à esquerda do sujeito encerra a inserção | `LANGUAGE_GUARANTEED` | `(A = B OR C) AND D` não herda `A =` em `D`. |
| `(` imediatamente após relational operator cria distribuição e, após o `)`, subject/operator continuam correntes | `LANGUAGE_GUARANTEED` | `A = (B OR C) AND D` pertence a classe diferente do boundary anterior. |
| Distribuição proíbe simple condition, outro relational operator e logical `NOT` imediatamente após o `(` dentro de seu scope | `LANGUAGE_GUARANTEED` | Oracles negativos não podem aceitar qualquer expressão parentética como lista de objects distribuídos. |
| `NOT` ligado ao relational operator é relacional; nas demais posições é logical NOT da condição seguinte e não é herdado | `LANGUAGE_GUARANTEED` | Oracles devem separar `A NOT = B OR C` de `A = B OR NOT C`. |
| `AND` precede `OR` sem parênteses | `LANGUAGE_GUARANTEED` | Estrutura semântica futura não pode preservar `MIXED_LOGICAL` plano como significado final. |
| General relation aceita identifier/literal/arithmetic expression/index-name conforme as regras de compatibilidade IBM | `LANGUAGE_GUARANTEED` | INDEX não pode ser descartado por origem sintática `conditionNameReference`; compatibilidade de tipos continua regra própria. |
| Level-66 RENAMES declara `data-name` | `LANGUAGE_GUARANTEED` | RENAMES entra pela classe DATA quando a referência ao data-name for válida; não criar namespace RENAMES contextual. |
| Condition-name deve ser único ou tornado único por qualification/subscripting; subscripts acompanham a conditional variable | `LANGUAGE_GUARANTEED` | Qualification/subscripts fazem parte da identidade da referência, não de heurística textual. |
| `IN` e `OF` são equivalentes e a sequência de qualifiers deve tornar a referência única conforme a hierarquia e a option `QUALIFY` | `LANGUAGE_GUARANTEED` | Oracle deve carregar qualifiers escritos e a option aplicável; terminal name isolado não basta. |
| A mesma user-defined word pertence a um único set de nomes dentro do programa | `LANGUAGE_GUARANTEED` | DATA + CONDITION homônimos no mesmo programa viram oracle negativo de validade, não ambiguidade de binding. |
| Nomes locais e globais de nested programs são resolvidos pelo conjunto e pela preferência de nível definidos pela IBM | `LANGUAGE_GUARANTEED` | O oracle cross-program deve aplicar scope antes de filtrar por uma categoria nominal sugerida pela grammar. |
| Parse tree é evidência sintática; AST, symbols, occurrences e resolution são produtos separados | `ARCHITECTURE_GUARANTEED` | Nenhum resultado observado no parser/AST pode ser promovido a regra COBOL. |
| Qual representação deve carregar contexto de abbreviation | `UNCERTAIN` | Deve permanecer aberta para Slice 2: AST contextual, lowering normalization ou produto pós-binding são alternativas, não decisões. |
| Wording exato do diagnostic IBM para declaração cross-set inválida | `UNCERTAIN` | Oracle exige rejeição/invalidade, não texto específico do compilador. |
| Frequência e shapes vistas em COACTUPC/CBSTM03A/CBSTM03D generalizam para outros sistemas | `OBSERVED_IN_CURRENT_CORPUS_ONLY` | Nunca usar frequência como semântica. |

## Comportamento esperado

O contrato normativo de equivalência usado pelos oracles é:

- `A = B OR C OR D` é interpretado, enquanto não houver terminador, como `A = B OR A = C OR A = D`.
- `A = B OR < C` mantém `A` como subject e passa a usar o relational operator explicitado para a relation abreviada; a sequência posterior usa o último operator válido conforme a regra IBM.
- `(A = B OR C) AND D` encerra a inserção no `)`; `D` precisa formar uma condição válida por si, não `A = D` por herança.
- `A = (B OR C) AND D` distribui `=` sobre `B` e `C`; ao fechar o grupo, `A` e `=` continuam correntes, permitindo a continuação abreviada `A = D`.
- `A NOT = B OR C` trata `NOT =` como relational operator e equivale a `(A NOT = B) OR (A NOT = C)`; `A = B OR NOT C` usa `NOT` lógico e equivale a `(A = B) OR NOT (A = C)` quando `C` é object relacional admissível. O `NOT` lógico não é propagado para uma continuação posterior.
- `A = B OR C AND D` respeita `AND` antes de `OR`; a futura representação não pode interpretar simplesmente pela ordem achatada.
- DATA, INDEX e CONDITION são distinguidos pelo contexto normativo e pelas declarações admissíveis; RENAMES permanece DATA. A origem `conditionNameReference` na grammar atual não é prova de declaration kind.
- Qualification e scope são aplicados pelas regras IBM antes de qualquer seleção conveniente de candidato. `IN` e `OF` são equivalentes; condition-name usa a hierarquia da conditional variable e exige os mesmos subscripts dela. Referência não tornada única é source inválido, mas a análise deve preservar os candidatos para diagnosticar ambiguidade em vez de escolher um.

### Regra COBOL versus observação atual

| Tema | Regra normativa | Observação do PR #14 |
| --- | --- | --- |
| `A = B OR C` com `C` data-name | `C` pode ser object abreviado e herdar subject/operator | grammar escolhe `conditionNameReference`; collector fecha `{CONDITION}` e gera falso gap |
| INDEX em bare tail | index-name pode participar de relation conforme compatibilidade | origem sintática pode excluir INDEX antes do resolver |
| RENAMES | nome de nível 66 é data-name | occurrence atual pode excluir DATA por contexto CONDITION |
| AND/OR | AND precede OR | AST observada pode achatar em `MIXED_LOGICAL` |
| Parênteses | boundary e distribuição têm efeitos diferentes | lowering atual não materializa integralmente essa distinção |
| Homônimo DATA+CONDITION no mesmo programa | declaração cross-set é inválida | fixture histórica podia produzir false-success CONDITION; isso caracteriza tolerância do frontend, não COBOL válido |

## Comportamento diante de incerteza

Quando a fonte IBM não autorizar uma conclusão, o oracle deve declarar `UNCERTAIN` ou input fora do domínio, nunca escolher candidate pela ordem, grammar rule ou corpus. Aceitação pela grammar vendorizada não prova validade IBM. Em particular, formas que a grammar aceite mas não caibam na sequência normativa de abbreviated conditions devem ser tratadas como adversariais de frontend e não como nova regra de linguagem.

Qualquer diferença dependente de compiler option, como políticas ampliadas de qualification já modeladas pelo projeto, deve ser preservada como configuração explícita e não resolvida neste slice.

## Fora de escopo

- qualquer alteração de produção, grammar, `Ast`, `AstBuilder`, `ReferenceOccurrenceCollector`, resolver ou arquitetura;
- decidir entre AST contextual, normalização no lowering ou produto semântico pós-binding;
- materializar subject/operator herdados em um modelo de produção;
- corrigir precedence, truncamento de `abbreviation(0)`, condition-name subscriptado, SEARCH ou occurrences;
- promover slices 2+ de `BACKLOG-COND-001`;
- inventar regra para inputs aceitos pela grammar mas não demonstrados como COBOL IBM válido;
- alterar comportamento de qualification/scope já implementado fora do necessário para definir o oracle.

## Regras de domínio relacionadas

- `docs/domain/reference-resolution.md`: binding nominal, qualification, visibility e admissible kinds permanecem produtos posteriores à AST.
- `docs/engineering/semantic-analysis-policy.md`: fonte oficial do dialeto prevalece sobre corpus e comportamento atual.
- `docs/engineering/semantic-testing.md`: oracles derivam da regra antes da implementação e devem atacar atalhos plausíveis.

## ADRs/invariantes relacionados

- ADR-0003: separação dos produtos semânticos.
- INV-AST-001: AST não incorpora resultados posteriores.
- INV-AST-002: estrutura deriva da grammar/parse contexts, sem reparse textual.
- INV-SYM-001: symbol table não executa binding.
- INV-RES-001: ambiguidade real é preservada.
- INV-DET-001: decisões e ordem permanecem determinísticas.

Nenhum ADR novo é criado neste Slice 1 porque a decisão de representação pertence explicitamente ao Slice 2.
