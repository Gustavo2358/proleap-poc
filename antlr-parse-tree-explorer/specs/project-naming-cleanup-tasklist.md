# SDD — Limpeza de identidade e nomenclatura do projeto

## Estado

Execução concluída. O objetivo foi remover os dois identificadores
legados que apareciam no namespace, no build e na documentação, mantendo as
gramáticas COBOL intactas.

## Decisões aprovadas

- Namespace Java e `groupId` Maven:
  `io.github.gustavo2358.cobolexplorer`.
- Fontes gerados pelo ANTLR:
  `io.github.gustavo2358.cobolexplorer.antlr`.
- `artifactId`: manter `antlr-parse-tree-explorer`.
- Binding público: `Bindings.cobol()` com nome visível `COBOL grammar`.
- IDs de política: `cobol-explorer/explicit-options` e
  `cobol-explorer/ibm-enterprise-compatible`.

## Guarda absoluta das gramáticas

Estes arquivos não podem ser editados, movidos, formatados ou ter atribuições
alteradas:

- `src/main/antlr4/Cobol.g4` — SHA-256
  `77460471863292add5a113698bcaa3c7ba0e239b446063a9cccd09f9a2fb908d`;
- `src/main/antlr4/CobolPreprocessor.g4` — SHA-256
  `e78114b62294aba39f30fa1294082c6de4d099c1b521c5a8503f2b48853ed651`.

Os headers dessas gramáticas preservam atribuições da obra incorporada. Eles são
a única exceção ao gate de busca de identificadores legados.

## Escopo

- caminhos, packages, imports, Maven, ANTLR e Logback;
- binding, contratos serializados, scripts e documentação;
- relatórios e tasklists versionados;
- artefatos web `dist`, `dist-cbstm03a` e `dist-cbstm03d`;
- reconstrução limpa de `target` e gates contra regressão nominal.

## Fora do escopo

- qualquer mudança de gramática, corpus, copybook ou fonte COBOL;
- mudança funcional no parser, AST, resolução, preprocessamento ou interface;
- alteração de arquitetura além do rename aprovado;
- remoção de autoria, copyright ou atribuições das gramáticas.

## Tasklist passo a passo

### Fase 0 — Baseline e guardas

- [x] Registrar commit, status e SHA-256 das gramáticas.
- [x] Executar baseline: suíte Maven (125 testes), checks JavaScript e script
      de regressão do normalizador.
- [x] Confirmar as métricas funcionais canônicas antes do rename.
- [x] Adicionar um gate versionado que bloqueie a reintrodução dos dois
      identificadores legados fora dos arquivos protegidos.

### Fase 1 — Namespace, Maven, ANTLR e logging

- [x] Mover 37 fontes de produção e 32 testes para o namespace aprovado.
- [x] Atualizar packages, imports e package dos fontes gerados pelo ANTLR.
- [x] Atualizar `groupId`, `mainClass` e logger names.
- [x] Validar com `mvn clean test`: 125 testes verdes sem bytecode anterior.

### Fase 2 — Contratos, scripts e documentação

- [x] Migrar binding, policy IDs, asserções e snapshots versionados.
- [x] Renomear o prefixo temporário do script de regressão.
- [x] Atualizar README, specs e relatórios de modo semântico, preservando
      decisões e evidências técnicas.
- [x] Revisar o texto final da documentação e os diffs de contrato.

### Fase 3 — Artefatos e auditoria

- [x] Regenerar os três conjuntos `dist*` pelo pipeline oficial.
- [x] Comparar métricas e estrutura dos outputs com o baseline.
- [x] Executar a suíte, checks JavaScript, script de regressão e três execuções
      do explorer a partir de build limpo.
- [x] Verificar hashes das gramáticas, ausência de caminhos/classes legados e
      determinismo da regeneração.
- [x] Revisar o diff final e registrar o resultado da regressão.

## Resultado da regressão

- `mvn clean test`: 125 testes verdes.
- Checks de sintaxe JavaScript: verdes para os três conjuntos HTML.
- `scripts/source-normalizer-regression.sh naming-cleanup`: verde.
- `scripts/verify-naming.sh`: verde.
- As gramáticas conservaram exatamente os SHA-256 registrados neste documento.
- A segunda regeneração de todos os artefatos produziu os mesmos hashes da
  primeira.

## Critérios de aceite

1. As duas gramáticas continuam byte a byte idênticas aos hashes registrados.
2. Os identificadores removidos não aparecem fora das atribuições internas das
   gramáticas, nem em caminhos, classes, outputs ou documentação.
3. O código próprio e os fontes ANTLR gerados usam somente os namespaces
   aprovados.
4. Maven, logging, binding, políticas, scripts e HTMLs usam a nova identidade.
5. As métricas e o comportamento permanecem iguais, salvo campos nominais
   deliberadamente migrados.
6. A suíte e as regressões completas passam a partir de um build limpo.
7. O diff não contém mudança de gramática, corpus ou comportamento funcional.
