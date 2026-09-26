# Predicados lógicos de texto

Problema demonstrado: a topologia preserva os dois ramos de IF, mas o produto
perde o operador e o literal. O lower só consegue produzir um booleano
indeterminado; a análise posterior não consegue descartar um ramo incompatível
com os valores disponíveis.

A fonte publica uma árvore canônica de igualdade de texto, figurativos,
AND/OR/NOT. A gramática fornece operadores e figurativos; a análise nominal
fornece a identidade do operando. Abreviações herdam somente o sujeito e o
operador previstos pela estrutura da condição; grupos fecham esse contexto.
Formas não admitidas continuam sem árvore executável.

O lower exige leitura integral e tipo lógico TEXT com extensão conhecida. A
comparação alfanumérica preenche o menor operando com espaços. SPACES corresponde
a espaços por toda a extensão. LOW/HIGH-VALUES dependem da sequência de
ordenação e não são convertidos em bytes por suposição: a tradução usa a
condição necessária de todos os caracteres serem iguais, em conjunção com uma
comparação ainda desconhecida. Isso permite excluir um texto não uniforme sem
inventar sua codificação. A negação recai sobre a conjunção completa.

A análise de valores usa apenas os destinos já publicados. Um ramo é excluído
somente se sua condição for demonstravelmente incompatível com o estado; uma
parte desconhecida continua permitindo os dois resultados. A união de análises
não pode repor candidatos produzidos por uma análise que ignora esse filtro.

Fontes: [IBM condições abreviadas](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=expressions-abbreviated-combined-relation-conditions),
[figurativos](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=strings-figurative-constants).
Algoritmo linear na árvore; avaliação finita com os limites de conjunto já
existentes na análise. Oracles: igualdade com espaços, OR abreviado, NOT,
figurativo com texto uniforme/não uniforme, tipo não textual, símbolo ambíguo,
e árvore nova sob versão antiga do contrato.
