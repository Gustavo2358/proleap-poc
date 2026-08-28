# Provenance

Provenance é parte do contrato de análise. O `SourceMap` nasce no arquivo físico antes de normalização, preprocessing ou expansão de COPY e é composto por todas as transformações posteriores.

## Regras atuais

- Conteúdo não transformado e seus terminadores de linha mantêm segmentos exatos.
- Conteúdo normalizado, expandido ou reescrito mantém a origem, mas é marcado como aproximado quando não há correspondência física exata.
- Nenhuma etapa pode criar um mapa de identidade sobre o texto já transformado para simplificar posições.
- COPYs, inclusive aninhados, preservam origem e cadeia de inclusão.
- Diagnostics, nós semânticos e occurrences que declararem origem devem apontar para localização válida e coerente com o `SourceMap`.

Uma perda de input, COPY ausente ou construção opaca continua visível na cobertura e nos diagnostics. Não é permitido converter perda de provenance em ausência de efeito semântico.

## Fronteiras

O parser pode oferecer posições de token, mas elas não substituem a abstração de provenance. Source format, normalização e preprocessing são responsáveis por compor o mapa; AST, símbolos e resolução o consomem sem reconstruí-lo.

Os invariantes com IDs e os evals associados serão indexados nas fases de arqueologia e catálogo.
