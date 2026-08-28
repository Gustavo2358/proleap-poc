# Source format e normalização

## Propósito e escopo

`SourceNormalizer` converte registros COBOL físicos para a entrada do preprocessor sem perder provenance. O domínio suportado atual é fixed-format com policy explícita para debug lines; outros formatos não são aceitos implicitamente.

## Entradas e saídas

- **Entrada:** texto bruto, nome do arquivo, `SourceFormat.FIXED` e `DebugLinePolicy`.
- **Saída:** `SourceNormalizer.Result` com texto normalizado, `SourceMap`, diagnostics e formato efetivo.

O mapa é originado no texto bruto. A normalização preserva terminadores LF, CRLF e CR por registro; separadores Unicode de linha e caracteres/tabs ambíguos na área compilável falham localmente.

## Contrato fixed-format atual

- colunas 1–6: sequence area;
- coluna 7: indicator area;
- colunas 8–72: program text;
- conteúdo posterior: identification area, fora do texto compilável.

A indicator area possui catálogo fechado: blank, comentário `*`, page-eject `/`, continuação `-` e debug `D`/`d`. Indicador desconhecido não recebe fallback.

Continuação é resolvida por estado lexical para literal com aspas simples, literal com aspas duplas ou palavra. Continuação órfã, após registro incompatível ou fora das categorias suportadas produz diagnóstico/erro localizado; paridade simples de aspas não é o algoritmo.

Comment entries são derivadas dos owners reconhecidos pela gramática e pelas fronteiras de Area A. Pontos dentro do conteúdo não encerram a entrada; `END-REMARKS` é fronteira explícita quando aplicável. Consulte [ADR-0001](../architecture/decisions/0001-comment-entry-normalization.md).

## Incerteza e diagnostics

Formato, caractere, indicador ou continuação não suportados falham na fronteira correspondente. A normalização não tenta reparar entrada de modo silencioso. Conteúdo transformado conserva origem com `exact=false`.

## Complexidade e provenance

O scanner percorre os registros e mantém estado limitado, com custo linear no tamanho da entrada. Nenhuma linha física é inserida para facilitar o parser. O contrato completo de origem está em [provenance](provenance.md).

## Evidência executável

`SourceNormalizerTest`, `SourceProvenanceTest`, fixtures em `src/test/resources/cobol/source-format/` e `scripts/source-normalizer-regression.sh`.

## Relações

Evals: EVAL-SRC-001, EVAL-SRC-002, EVAL-PRE-002 e EVAL-PROV-002. Invariantes: INV-AST-002, INV-PROV-001, INV-PROV-002 e INV-COV-002. ADRs: ADR-0001, ADR-0002 e ADR-0009.
