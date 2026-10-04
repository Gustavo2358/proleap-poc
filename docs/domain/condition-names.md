# Condições de nível 88 e SET

Status: IN_PROGRESS — checkpoint 1 do PR #86.

## Regra e escopo

Um nível 88 não aloca um booleano. Ele associa um conjunto de valores (inclusive
intervalos fechados) à variável condicional imediatamente anterior. Ler o nome
testa a variável; SET TO TRUE atribui o primeiro valor declarado. SET TO FALSE
atribui exclusivamente o valor declarado em WHEN SET TO FALSE; a ausência dessa
cláusula não autoriza inventar um complemento. Vários destinos são processados
na ordem escrita. Qualificação e subscritos pertencem ao acesso à variável.

Fontes normativas IBM Enterprise COBOL:
- [Condition-name condition](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=expressions-condition-name-condition)
- [SET, format 4](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statement-format-4-set-condition-names)
- [VALUE, format 2](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=vc-format-2)

## Invariantes e algoritmo

- Extrair valores e destinos na fronteira sintática; nenhum reparsing no projector
  ou no lower. Resolver uma vez a declaração 88 e seu pai pelos índices nominais.
- Preservar listas e intervalos, sem enumerar elementos de intervalos e sem
  distribuir AND/OR em produtos cartesianos. Custo proporcional aos nós e aos
  valores declarados/expressões publicados, além da ordenação determinística.
- Preservar a ordem das atribuições SET, inclusive destinos com o mesmo pai.
- Incerteza de binding, índice, collation, armazenamento ou input não autoriza
  apagar candidatos de dependência. Prova nominal não implica alocação física.
- Usar os mesmos fatos canônicos em IF, EVALUATE, PERFORM e SET; não manter uma
  interpretação paralela para contagem de gaps.

## Oracle e qualificação

Testes com pai observado após SET, listas cujo primeiro valor difere dos demais,
intervalos extensos, FALSE explícito/ausente, negação, combinação, qualificação,
subscritos e colisão de destinos. Negativos de contrato removem ou contradizem a
prova publicada. Comparar os 73 fontes CardDemo por ocorrência e por dependência,
com baseline imutável ed4830e4 (28.050 gaps). O inventário inicial tem 3.807 usos:
1.819 SET TRUE, 1.465 IF, 481 EVALUATE e 42 PERFORM. A categoria só está fechada
quando cada ocorrência dispõe de semântica publicada e consumida.

Uma cauda contextual cujo binding resolve para CONDITION é um teste completo de
88, mesmo depois de uma relação numérica. A inserção de sujeito/operador abreviado
termina ao encontrar o condition-name: IBM Enterprise COBOL 6.4,
[Language Reference, abbreviated combined relation conditions](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf).
O nó desconhecido da relação vizinha permanece; NOT e a precedência AND/OR do AST
não são distribuídos nem reinterpretados. O nível 88 não aloca storage e não
converte o item elementar associado em um grupo.

## Contrato SP 2.64

`conditionNames` é um fato canônico opcional. A ausência não prova semântica de
88. A publicação contém:

- `definitions`: identidade da condição, pai DATA (ou pai FILLER anônimo), domínio,
  lista de valores/intervalos, valor falso opcional e provenance de ambos.
- `uses`: ocorrência, statement, definição, operando DATA com papel READ/WRITE,
  índices tipados e provenance. O operando liga a variável associada ao 88.
- `assignments`: destinos SET com ordinal contíguo, verdade solicitada e valor
  declarado. A ordem publicada é a ordem de execução, inclusive sobre um mesmo pai.
- `predicates`: árvores TEST/NOT/AND/OR/UNKNOWN em IF, EVALUATE e PERFORM UNTIL.
  TEST aponta para um uso READ do próprio statement. UNKNOWN preserva expressões
  vizinhas cuja semântica ainda não está disponível.

A validação fecha identidades, papéis, statement proprietário, binding ao pai,
provenance da variável e valor de SET. A árvore não aceita um uso WRITE como teste.
Um SET só satisfaz a obrigação de capability quando todos os seus destinos e sua
ordem correspondem às referências publicadas; input e controle têm provas próprias.
O produtor escreve uma única versão atual, 2.64.0, sem selecionar versões antigas
por combinação de capabilities.

EVALUATE publica sua estrutura tipada também com input parcial. Seus seletores
mantêm gaps de binding e seus braços mantêm gaps de entrada/continuação quando
faltam provas. Isto torna explícitas obrigações antes escondidas pelo diagnóstico
genérico de statement observado. Não se completa controle a partir do predicado.

A cláusula FALSE aceita as palavras opcionais previstas pela IBM; a separação
sintática impede que FALSE seja consumido como mais um valor verdadeiro.
[APAR PI97160](https://www.ibm.com/support/pages/apar/PI97160).

## Limites preservados

Os 88 deixam de ser uma categoria sem modelagem de origem. Isso não fornece
valores de entrada, codecs físicos, collation, índices concretos nem localização
para LINKAGE. Um predicado misto mantém UNKNOWN para seus outros termos. O lower
emite comparações/atribuições quando suas provas de acesso permitem; nas demais
situações publica incerteza localizada, mantendo os fatos de origem e dependências.

[Qualificação do checkpoint](../work/condition-names-qualification.md).
