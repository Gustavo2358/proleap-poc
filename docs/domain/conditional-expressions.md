# Expressões condicionais combinadas e abreviadas

## Propósito e autoridade

Este documento registra o contrato normativo usado pelo projeto para condições combinadas e abbreviated combined relation conditions. Ele descreve a regra COBOL que as representações internas precisam preservar; não afirma que o frontend atual já materializa todas essas formas corretamente.

A autoridade é [IBM Enterprise COBOL for z/OS 6.4 Language Reference, SC27-8713-03, atualização de 28 de junho de 2024](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf). As seções mínimas são **User-defined words** (pp. 12–13), **Scope of names** e **Referencing data names...** (pp. 63–72), **RENAMES clause** (pp. 228–230) e **Conditional expressions** (pp. 268–289, especialmente 283–289).

As páginas IBM Docs equivalentes podem facilitar navegação, mas a edição e a data acima fixam a fonte normativa. O [relatório histórico do PR #14](../history/evidence/semantic-condition-context-discovery-report.md) permanece evidência da implementação observada, não especificação da linguagem. Os casos mínimos que instanciam este contrato estão no [catálogo de oracles `COND-*`](../evals/conditional-expression-oracles.md).

## Estado semântico corrente

Após a primeira relation-condition de uma sequência abreviável, o último subject e o último relational operator escritos formam o estado corrente. Uma relation completa posterior substitui ambos; uma relation com somente o subject omitido substitui o operator e conserva o subject. Subject e operator omitidos são semanticamente inseridos enquanto nenhum terminador ocorrer.

Consequências:

- `A = B OR C OR D` equivale a `(A = B) OR (A = C) OR (A = D)`;
- `A = B OR C = D OR E` equivale a `(A = B) OR (C = D) OR (C = E)`, pois a relation completa `C = D` redefine o estado corrente;
- `A = B OR < C` conserva `A` e passa a usar o relational operator explicitamente escrito na relation abreviada.

A inserção herdada termina quando outra simple condition é encontrada. Condition-name é uma simple condition, mas não é o único terminador: class condition e outras simple conditions também encerram a sequência. Portanto `A = B OR C IS NUMERIC OR D` não autoriza interpretar `D` como `A = D`.

Um bare nominal tail como `C` em `A = B OR C` não possui classe semântica final apenas pela grafia ou pelo ramo escolhido pela grammar:

- se o binding identifica DATA, INDEX ou um level-66 RENAMES usado como data-name, `C` pode ser o object abreviado; a admissibilidade type-sensitive da comparação é verificada em etapa posterior, não no binding;
- se o binding identifica um condition-name nível 88, `C` inicia nova simple condition e encerra a inserção;
- se binding, qualification, scope ou opção de dialeto não permitem decisão única, a incerteza permanece explícita.

## Parênteses, distribuição e precedência

O `)` correspondente a um `(` situado à esquerda do subject corrente encerra a sequência herdada. Assim, em `(A = B OR C) AND D`, `C` ainda pode herdar `A =`, mas `D` precisa iniciar condição válida por si.

Quando `(` aparece imediatamente após o relational operator, o operator é distribuído pelos objects internos. Ao fechar esse grupo, subject e operator continuam correntes: em `A = (B OR C) AND D`, a igualdade alcança `B` e `C` e `D` ainda pode ser continuação abreviada. Dentro desse scope distribuído não se admite nova simple condition, outro relational operator nem logical `NOT` imediatamente após o `(`.

Sem agrupamento explícito, a precedência lógica é `NOT`, depois `AND`, depois `OR`. Conectores mistos não podem ser reduzidos a uma lista plana sem preservar essa estrutura.

## `NOT` relacional e lógico

`NOT` que integra o relational operator pertence a esse operator e pode ser herdado. Por isso `A NOT = B OR C` equivale a `(A NOT = B) OR (A NOT = C)`.

Nos demais pontos, `NOT` é lógico e nega somente a relation-condition imediatamente seguinte. Em `A = B OR NOT C OR D`, quando `C` e `D` são objects relacionais admissíveis, a expansão é `(A = B) OR NOT (A = C) OR (A = D)`: o `NOT` não vira parte do operator nem se propaga a `D`.

## Classes nominais, qualification e scope

- Data-name pertence à classe DATA. Um level-66 `RENAMES` declara data-name e não cria namespace nominal adicional.
- Index-name declarado por `INDEXED BY` pertence à classe INDEX e pode participar de relation-condition somente nas combinações admitidas pela IBM; essa admissibilidade é verificada em validação type-sensitive posterior, nunca pelo binding nominal. Um data item com `USAGE INDEX` continua DATA.
- Condition-name nível 88 pertence à classe CONDITION e referencia sua conditional variable.
- Condition-name precisa ser único ou tornado único por qualification e, quando aplicável, usa os mesmos subscripts da conditional variable. `IN` e `OF` são equivalentes.
- DATA, CONDITION e INDEX são locais por padrão nas declarações pertinentes. `GLOBAL` torna as declarações elegíveis nos programas contidos segundo as regras IBM; nomes locais e de programas contendo são considerados por nível e scope, sem lookup global irrestrito.
- Uma user-defined word pertence a um único set de nomes dentro do mesmo programa. DATA e CONDITION com a mesma palavra no mesmo programa formam source IBM inválido, não uma ambiguidade de binding a ser resolvida por precedência conveniente.

Quando qualification ou scope não tornam uma referência única, o source não possui binding COBOL único. O analisador conserva candidates e estado `AMBIGUOUS` para diagnóstico; não seleciona por ordem. Se o resultado depender de uma opção de qualification não conhecida, o estado é `UNCERTAIN`/`UNSUPPORTED`, conforme o produto aplicável.

## Fronteira de representação

A parse tree registra a estrutura reconhecida pela grammar, mas não prova declaration kind. Desde `WORK-COND-003`, a AST de superfície preserva a sequência, precedência, parênteses, distribuição e abbreviations escritas sem fechar o significado binding-dependent; subject/operator omitidos permanecem omitidos e bare tails ficam contextuais. Desde `WORK-COND-004` (Slice 4), a condition-name reference escrita é um `DataReference` nominal estruturalmente lossless: nome base, qualification `IN`/`OF` em ordem escrita e subscripts como `SubscriptGroup` tipado, construídos dos children diretos do context, sem lookup e sem reparse textual. O último qualifier é `UNSPECIFIED` na surface (a parse tree não distingue DATA/FILE/MNEMONIC atrás de `inData`); o resolver o consome por mapeamento compatibility-preserving `{DATA}`, com candidate universe inalterado — a ampliação `{DATA, FILE}` pertence a `BACKLOG-RES-004` (IBM resolution-of-names step 3). A especialização e a validação continuam pós-binding, conforme a fronteira abaixo.

A arquitetura aceita em ADR-0012 (status `Accepted`), guardada por INV-COND-001 e INV-COND-002, exige que a evolução que venha a ser autorizada:

- preservar na AST de superfície somente estrutura derivada dos contexts/tokens, incluindo todos os conectores, parênteses, `NOT`, operands e a alternativa contextual ainda não especializada;
- manter nomes escritos como occurrences únicas e tipadas, sem criar occurrence sintética para subject/operator herdados;
- tratar a especialização DATA/INDEX/CONDITION como binding-dependent e exclusiva do pós-binding: o binding nominal decide a classe, `ConditionSemantics` materializa a relation e nenhuma fase anterior fecha o meaning;
- usar binding nominal para especializar a alternativa CONDITION versus DATA/INDEX, sem confundir type compatibility com name binding: resolver `N → DATA` e `IDX → INDEX` não prova que `N = IDX` é admitido pela regra IBM para index-name;
- projetar depois do binding um produto semântico separado, com árvore de predicates normalizada, identidade própria e provenance que diferencia elemento escrito de herdado; `ConditionSemantics` produz a relation normalizada sem afirmar validade de tipos ainda não verificada;
- delegar a admissibilidade type-sensitive a etapa posterior conceitual, `ConditionValidation`, que consome `ConditionSemantics`, informação de declaração/tipo e os contratos IBM aplicáveis e distingue pelo menos relation semanticamente válida, semanticamente inválida e validade ainda não verificável/incompleta, sem alterar AST, occurrences, resolution ou `ConditionSemantics`;
- manter o ownership de `COND-P06`, `COND-N04` e `COND-A06`: binding identifica INDEX; `ConditionSemantics` materializa a relation; `ConditionValidation` verifica a admissibilidade type-sensitive;
- não acrescentar `PIC`/`USAGE` checking ao resolver nem type checking ao `AstBuilder`; sem reparse textual ou heurística, e sem inventar `VALID` quando a informação de tipo ainda não estiver materializada;
- preservar `AMBIGUOUS`, `UNRESOLVED`, `UNSUPPORTED` e input inválido sem reparse textual, spelling heuristic ou escolha do primeiro candidate.

Essa decisão não implementa `ConditionSemantics`, `ConditionValidation` nem autoriza CFG, predicate analysis ou dataflow. Ela define a fronteira que esses consumidores deverão usar quando os slices correspondentes forem aprovados. O pipeline conceitual é `Surface AST → ReferenceOccurrences → ReferenceResolution → ConditionSemantics → ConditionValidation → CFG/predicate/dataflow`; `ConditionSemantics` e `ConditionValidation` ainda não existem em produção e terão API/schema decididos em slice futuro autorizado.

## Relações

Domínios: [AST semântica](semantic-ast.md), [modelo de símbolos](symbol-model.md), [resolução de referências](reference-resolution.md) e [provenance](provenance.md). ADRs: ADR-0002, ADR-0003, ADR-0005, ADR-0008, ADR-0009 e ADR-0012. Invariantes: INV-AST-001 a INV-AST-003, INV-SYM-001, INV-PROV-002, INV-RES-001, INV-COND-001, INV-COND-002 e INV-DET-001.
