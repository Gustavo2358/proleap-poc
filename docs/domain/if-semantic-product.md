# Fatos de IF simples — CP6 W2A

Contrato corrente SP **1.4.0**. W2A entrega fatos COBOL para uma tradução futura.
Não implementa AIR, lowering, CFG, análise de fluxo ou IF COBOL geral.

## Autoridade e domínio

Requisito de produto: CP6 W2 discovery aprovado e autorização humana específica
para W2A. Regras de linguagem: IBM Enterprise COBOL for z/OS 6.4,
[IF](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statements-if-statement),
[comparações alfanuméricas](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=conditions-alphanumeric-comparisons),
[WORKING-STORAGE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=overview-working-storage-section)
e [REDEFINES](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=clause-redefines-considerations).
A referência consolidada é o [Language Reference 6.4](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf),
consultado em 12/09/2026: WORKING-STORAGE p. 164 (PDF 192), comparações
alfanuméricas p. 276–277 (PDF 304–305), IF p. 348–349 (PDF 376–377).

LANGUAGE_GUARANTEED: IF seleciona um braço segundo uma condição; a conclusão
normal do braço retoma a sequência após o IF. Comparação alfanumérica simples
produz uma condição verdadeira ou falsa sem modificar seus operandos.
WORKING-STORAGE admite itens independentes; redefinições e outros mecanismos
de compartilhamento exigem exclusão ou prova própria.
ARCHITECTURE_GUARANTEED: parser/AST, símbolos, occurrences, resolução, coverage
e provenance são produtos canônicos. SPECIFICATION_GUARANTEED: a admissão abaixo
é deliberadamente conservadora. Nenhuma regra depende dos nomes FLAG/WS-PGM.

## PredicateGuarantee

`ConditionSurface.predicate` publica availability, profile, knownReads,
provenance, gapCodes e garantias tipadas. Sob `KNOWN / SCALAR_TEXT_EQUALITY`:

- resultDomain = BOOLEAN;
- evaluation = PURE;
- normalCompletion = TOTAL;
- readsCompleteness = COMPLETE;
- truthValue = UNKNOWN.

**predicate truth value is NOT evaluated**. Nem o valor inicial de FLAG, nem
MOVEs anteriores, nem a grafia do literal determinam uma branch tomada.
O produto não publica expressão normalizada, resultado literal booleano ou
qualquer classe AIR. As garantias são propriedades desta classe de avaliação;
a totalidade não afirma que um programa inteiro termine.

Forma admitida: relação escrita completa, operador de igualdade positivo,
subject `Ast.DataReference` escalar textual inteiro e object literal básico
com `logicalText` já interpretado pelo builder. `=` e as alternativas positivas
EQUAL/IS EQUAL TO da gramática recebem `RelationOperator.EQUAL` por tokens tipados.
NOT, desigualdades, formas combinadas/abreviadas, grupos, aritmética, funções,
chamadas, literais numéricos/national/hex e demais formas permanecem fora.

Exige occurrence VALUE_READ única RESOLVED com um candidate selecionado, shape
STRUCTURED, sem qualifier, subscript ou refmod; declaração do profile scalarText
com coverage MODELED e provenance exata da declaração e cláusulas. Statement,
condição, referência, literal e occurrence também exigem origem exata. Input
incompleto/recovery impede promoção. Cobertura canônica é esparsa: declarações,
cláusulas e statements possuem findings obrigatórios; containers e expressões
simples tipadas não possuem findings próprios. Para esses nós, a prova combina
shape tipado, proveniência exata e integridade do input/statement. Um finding
parcial presente nunca é ignorado.

`knownReads` referencia OperandIds da própria condition surface, preservando
ordem e identidade de **todas** as referências DATA publicadas. A ocorrência
positiva tem READ, binding.selected = wholeItemAccess.data = DataItemId de FLAG.
O core valida closure, papel, cardinalidade e origem; nome não é identidade.
Fora da slice, knownReads permanece, mas completude é PARTIAL e domínio,
evaluation e normalCompletion são UNKNOWN, com gap PREDICATE_NOT_PROVEN.
Unresolved com alternativas DATA/INDEX não recebe identidade DATA fabricada:
a surface histórica conserva CONDITION_REFERENCE_KIND_NOT_PROJECTED; nenhuma
lista vazia nessa situação afirma ausência de leitura.

## Completion e arms

`AstBuilder` conserva `Division.normalContinuations` usando as listas diretas
de statements de cada região de sentences. Percorre cada lista em ordem reversa,
com sucessor herdado e stack de regiões. IFs internos mantêm listas THEN/ELSE
separadas. A última instrução de cada braço herda o sucessor de seu IF, enquanto
as anteriores continuam para o próximo sibling direto. O próprio IF aponta para
o sucessor externo. Um statement não materializado quebra a relação, nunca é
pulado. Fim de paragraph/section/procedure e declaratives permanecem indisponíveis.
A extensão W2A cobre MOVE e IF nos braços; CALL conserva o escopo W1A de região
superior sem handlers. Outros controles não ganham completion universal.

No SP, `IfFact.normalContinuation` usa a mesma disponibilidade explícita de
MOVE/CALL: KNOWN com StatementId publicado ou UNAVAILABLE. Nunca NONE. Significa
**após conclusão normal**, não reachability, seleção de braço ou garantia de
retorno. `IfFact.continuation` conserva seu significado estrutural anterior;
quando completion executável é conhecida, ambas precisam concordar.
Não se deriva successor de StatementId, ProgramPoint, linha ou posição global.

`thenArm` e `elseArm` publicam separadamente:

- presence = PRESENT / ABSENT / UNKNOWN;
- contentAvailability = KNOWN / PARTIAL / UNAVAILABLE / INPUT_MISSING;
- entry = availability + StatementId opcional;
- provenance e gapCodes.

Presence vem de `ifElse()!=null`, preservada na AST sem adicionar nós/IDs.
Provenance do braço presente ancora seu primeiro token escrito (THEN, quando
existe, primeira instrução ou ELSE); o owner e cada filho preservam suas origens
completas. A ausência reutiliza a origem do IF íntegro. Isso evita um novo scan
dos segmentos de origem de todo braço aninhado por braço; a âncora isolada não
certifica conteúdo, cuja prova exige também owner e filhos completos.
ELSE ausente tem ABSENT, conteúdo KNOWN e nenhuma entry. ELSE presente vazio
continua PRESENT/PARTIAL, sem entry. Input incompleto produz UNKNOWN/INPUT_MISSING.
Um arm parcial pode conservar sua entrada observada conhecida sem certificar
o conteúdo inteiro; consumers devem ler os eixos separadamente.

`IfProfile.SIMPLE_TEXT_EQUALITY` exige IF root explicitamente terminado,
predicate KNOWN, completion KNOWN e braços completamente admitidos. THEN contém
um ou mais MOVEs diretos com cópia/fitting e completion provados; ELSE pode ser
ABSENT ou ter a mesma classe de conteúdo. Cada MOVE deve continuar para seu
sibling ou completion do IF. Nested IF é preservado com membership/entries e
completion corretos, mas owner e filho ficam OUTSIDE_SLICE para admissão simples.
NEXT SENTENCE, DISPLAY, demais statements e recovery permanecem positivos/parciais.

Readiness lowering/CFG é SUFFICIENT apenas para os fatos completos do profile
simples; effects/dataflow continua PARTIAL. As claims estruturais antigas fora
desse profile não certificam controle executável/AIR. Input/inventário e entry
readiness conservam seus limites W1; não se fecha a publicação inteira.

## IndependentStorageSet — decisão A

`State.storageIndependence` e `port.storageIndependence()` publicam um fact
próprio, separado de scalarText: availability, members (DataItemIds), provenance,
gapCodes, rule INDEPENDENT_WORKING_STORAGE_ROOTS e authority
IBM_ENTERPRISE_COBOL_6_4_WORKING_STORAGE.

**storage independence is source-derived evidence, not inferred from distinct IDs**.
KNOWN prova independência entre os storages representados por todos os membros,
na ProgramUnit publicada. Não afirma que inexistam aliases fora desse conjunto,
nem publica layout/offsets/Cells. O lower futuro pode traduzir a prova sem
realizar nova análise semântica; não pode ampliar seus membros por igualdade
ou diferença de IDs.

A emissão exige uma única seção WORKING-STORAGE, input íntegro, origem exata
da seção e inventário **inteiro** de roots elegíveis. Todos os roots devem ser
DATA_ITEM locais, elementares standalone nível 01/77, com scalarText já provado,
símbolo único por nó, coverage MODELED e provenance exata da declaração/cláusulas.
São programas comuns, sem INITIAL/RECURSIVE/COMMON/LIBRARY/DEFINITION, conforme
as provas [scalarText](scalar-text-move.md). Somente PICTURE X com extensão
positiva e USAGE DISPLAY opcional; sem children, FILLER, VALUE, OCCURS,
REDEFINES, RENAMES, EXTERNAL/GLOBAL ou cláusulas adicionais/preservadas.
Qualquer root não elegível impede **todo** o conjunto, inclusive quando as duas
declarações usadas seriam escalares. Não há seleção conveniente de subconjunto.
FILE/LINKAGE/LOCAL-STORAGE e overlays implícitos de outras estruturas não entram.
São necessários ao menos dois membros; sem prova: membros vazios e gap explícito.

Opção B foi rejeitada: scalarText declara expressamente que sua garantia lógica
não promete disjunção universal. A regra W2A usa a autoridade de WORKING-STORAGE,
a revisão de inventário inteiro e os requisitos adicionais acima; identidade
nominal e exclusões de implementação isoladas não bastam. Não há alias analysis geral.

## Arquitetura, complexidade e evolução

`IfSemantics` é snapshot imutável pós-binding, composto junto ao snapshot
`ScalarMoveSemantics` antes do projector. Indexa resolution e declarações uma vez;
preserva facts por ProgramUnit/AST node. Projector traduz os facts por índices;
writer só lê o port. O core valida invariantes/closure em passes indexados,
incluindo ancestry por intervalos da árvore de containment. Não gera CFG.
Tempo/espaço O(S + R + D + nós de expressões/cláusulas), além do custo usual de
hashing, texto e serialização. Braços são visitados uma vez, storage usa O(D)
membros sem pares O(D²); contadores N/2N medem trabalho, não tempo de hardware.

1.3.0 → **1.4.0** é evolução minor aditiva: campos existentes mantêm significado,
novas provas e disponibilidades são explícitas. Fatos de completion antes
indisponíveis podem ser fortalecidos pela nova autoridade e alterar readiness
local. A policy INTERNAL-CONTRACT-DEV-001 mantém um writer corrente, sem negociação,
downgrade ou reader produtivo local. Consumers fechados em 1.3 devem rejeitar
1.4 até migração própria. Os construtores Java legados deixam provas indisponíveis.
Goldens e receipts 1.1/1.2/1.3 não são reescritos. Nenhum pin ou consumer sibling
foi alterado e nenhum E2E downstream W2 é reivindicado.

INV-SP-010 e EVAL-SP-008 protegem o contrato. [Work item](../work/history/WORK-AST-006.md).
