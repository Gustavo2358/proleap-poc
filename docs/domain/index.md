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
8. [resolução de referências](reference-resolution.md);
9. [COBOL Semantic Product e lowering readiness](cobol-semantic-product.md),
   boundary local materializada e transportada por JSON antes do `cobol-lower`
   externo; [IF W2A](if-semantic-product.md) delimita predicate, braços/completion e independência limitada.

CFG e dataflow não são domínios locais deste repositório: a ownership cross-repo
está no [pipeline](../architecture/pipeline.md). Interpretação de linguagens
embarcadas e descoberta final de dependências também não devem ser apresentadas
como domínio atual sem contrato materializado.

- Current [SP1.8 compositional/partial boundary](../architecture/compositional-partial-lowering.md) and [BASIC PERFORM](perform-basic.md).
