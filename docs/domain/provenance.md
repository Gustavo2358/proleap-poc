# Provenance

## Propósito e escopo

Provenance é parte do contrato de análise. O `SourceMap` nasce no arquivo físico antes de normalização, preprocessing ou expansão de COPY e é composto por todas as transformações posteriores.

## Entradas e saídas

- **Entrada:** texto físico e nome do arquivo; depois, operações de slice, replacement e expansão.
- **Saída:** texto corrente, segmentos para arquivos originais, include chains e flag de exatidão.

## Regras atuais

- Conteúdo não transformado e seus terminadores de linha mantêm segmentos exatos.
- Conteúdo normalizado, expandido ou reescrito mantém a origem, mas é marcado como aproximado quando não há correspondência física exata.
- Nenhuma etapa pode criar um mapa de identidade sobre o texto já transformado para simplificar posições.
- COPYs, inclusive aninhados, preservam origem e cadeia de inclusão.
- Diagnostics, nós semânticos e occurrences que declararem origem devem apontar para localização válida e coerente com o `SourceMap`.

Uma perda de input, COPY ausente ou construção opaca continua visível na cobertura e nos diagnostics. Não é permitido converter perda de provenance em ausência de efeito semântico.

## Fronteiras e complexidade

O parser pode oferecer posições de token, mas elas não substituem a abstração de provenance. Source format, normalização e preprocessing são responsáveis por compor o mapa; AST, símbolos e resolução o consomem sem reconstruí-lo. Segmentos adjacentes compatíveis são mesclados; consultas localizam os segmentos que cobrem o intervalo solicitado.

## Evidência executável

`SourceProvenanceTest`, `SourceNormalizerTest`, `SourceNormalizationPreprocessingIntegrationTest` e o cenário E2E do normalizador.

## Relações

Invariantes: INV-PROV-001, INV-PROV-002 e INV-COV-001. ADR: ADR-0002.
