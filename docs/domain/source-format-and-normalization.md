# Source format e normalização

## Propósito e escopo

`SourceNormalizer` converte registros COBOL físicos para a entrada do preprocessor sem perder provenance. O domínio suportado atual é fixed-format com policy explícita para debug lines; outros formatos não são aceitos implicitamente.

## Entradas e saídas

- **Entrada:** texto bruto, nome do arquivo, `SourceFormat.FIXED` e `DebugLinePolicy`.
- **Saída:** `SourceNormalizer.Result` com texto normalizado, `SourceMap`, diagnostics e formato efetivo.

O mapa é originado no texto bruto. A normalização preserva terminadores LF,
CRLF e CR por registro e preserva conteúdo Unicode verbatim. Offsets e colunas
são medidos em code points; separadores Unicode de linha falham localmente. TAB separador segue a política
interna de importação abaixo.

## Contrato fixed-format atual

- colunas 1–6: sequence area;
- coluna 7: indicator area;
- colunas 8–72: program text;
- conteúdo posterior: identification area, fora do texto compilável.

A indicator area possui catálogo fechado: blank, comentário `*`, page-eject `/`, continuação `-` e debug `D`/`d`. Indicador desconhecido não recebe fallback.

Continuação é resolvida por estado lexical para literal com aspas simples, literal com aspas duplas ou palavra. Continuação órfã, após registro incompatível ou fora das categorias suportadas produz diagnóstico/erro localizado; paridade simples de aspas não é o algoritmo.

Comment entries são derivadas dos owners reconhecidos pela gramática e pelas fronteiras de Area A. Pontos dentro do conteúdo não encerram a entrada; `END-REMARKS` é fronteira explícita quando aplicável. Consulte [ADR-0001](../architecture/decisions/0001-comment-entry-normalization.md).

## Incerteza e diagnostics

Formato, separador de linha, indicador ou continuação não suportados falham na
fronteira correspondente. A normalização não tenta reparar entrada de modo
silencioso. Conteúdo transformado conserva origem com `exact=false`.

## Complexidade e provenance

O scanner percorre os registros e mantém estado limitado, com custo linear no tamanho da entrada. Nenhuma linha física é inserida para facilitar o parser. O contrato completo de origem está em [provenance](provenance.md).

## Evidência executável

`SourceNormalizerTest`, `SourceProvenanceTest`, fixtures em `src/test/resources/cobol/source-format/` e `scripts/source-normalizer-regression.sh`.

## Relações

Evals: EVAL-SRC-001, EVAL-SRC-002, EVAL-PRE-002 e EVAL-PROV-002. Invariantes: INV-AST-002, INV-PROV-001, INV-PROV-002 e INV-COV-002. ADRs: ADR-0001, ADR-0002 e ADR-0009.

## Importação de TAB (R3-A)

Antes de interpretar as áreas fixas, HT separador avança até a próxima parada
de oito colunas (9, 17, 25, … em coordenadas de coluna 1-based), e não adiciona
sempre oito espaços. A mesma regra é aplicada a cada fonte e COPY físico.
Não há configuração externa nova nem seleção automática de perfil físico.

Aspas simples/duplas, inclusive duplicadas e literais continuados com seu
delimitador em cada registro, protegem o payload. TAB dentro de literal,
comentário `*`/`/`, comentário inline e área de identificação é preservado.
Continuação preserva TAB de payload no fim do fragmento. Indicadores inválidos
continuam rejeitados; a margem 72 é aplicada após a expansão de separadores.
Espaços gerados apontam para o HT original com exact=false; tokens intactos
após HT conservam offsets/colunas físicos exatos, inclusive em COPY aninhado.
O scanner é linear; arquivos sem HT mantêm o caminho e os bytes anteriores.

Esta é uma política de importação do produto, não uma afirmação de que COBOL
IBM define HT de largura oito. Precedente: [IBM z/OS expand](https://www.ibm.com/docs/en/zos/3.1.0?topic=descriptions-expand-expand-tabs-spaces);
áreas e continuação: [IBM indicator area](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=format-indicator-area).
Evidência: `FixedTabNormalizationTest`, mínimos de prefixo e separador SQL,
fronteiras 7/8/72/73, Unicode, LF/CRLF/CR, payload e provenance/COPY.
