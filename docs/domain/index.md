# Domínio

Documentos de domínio descrevem como cada subsistema funciona semanticamente hoje. Cada um deve explicitar entradas, saídas, superfície suportada, fronteiras não suportadas, incerteza, provenance, custo esperado e links para invariantes, ADRs e evals relevantes.

Domínios disponíveis, na ordem do pipeline:

1. [source format e normalização](source-format-and-normalization.md);
2. [preprocessing](preprocessing.md);
3. [provenance](provenance.md), transversal às transformações;
4. [AST semântica](semantic-ast.md);
5. [expressões condicionais combinadas e abreviadas](conditional-expressions.md), contrato transversal ainda parcialmente não materializado;
6. [compilation units](compilation-units.md);
7. [modelo de símbolos](symbol-model.md);
8. [resolução de referências](reference-resolution.md).

CFG, dataflow, interpretação de linguagens embarcadas e descoberta final de dependências ainda não possuem contrato implementado; seu trabalho permanece no backlog. Não criar documentação especulativa para apresentá-los como domínio atual.
