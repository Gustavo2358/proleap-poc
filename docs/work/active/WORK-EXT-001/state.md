# WORK-EXT-001 — Estado

## Onde estamos

Work item preparado em estado oracle-first. Nenhum código de produção foi alterado e nenhum backlog foi iniciado.

## Verde conhecido

A gramática atual mantém `DFHRESP`/`DFHVALUE` em `cobolWord`; AST table calls, occurrences por subtree, binding nominal separado e provenance já fornecem os insumos estruturais. ADR-0011 e INV-EXT-001 a INV-EXT-004 governam o slice. O relatório integral está arquivado como evidência histórica e permanece fora de `must_read`. BACKLOG-EXT-001 a BACKLOG-EXT-006 preservam, em ordem de dependência, infraestrutura, contexto de compilação, external symbols, extractors CICS/DB2/IMS, GRBE e efeitos externos de CFG. BACKLOG-DF-003 registra possible-values como capacidade canônica futura e continua fora deste slice. O gate `fast` passou nesta preparação documental.

## Restante

Na próxima sessão, implementar somente o slice descrito neste diretório: primeiro tornar EVAL-EXT-001 executável e vermelho, depois criar produto/classifier mínimo, composição de relatório/snapshot e gates declarados. Não iniciar infraestrutura genérica, providers, extractors, CFG ou dataflow.

## Descobertas que afetam o plano

`ExplorerMain` é hoje o composition root e `ResolutionAnalysisReport` cria um gap por entry não resolvida. `Ast.DataReference` preserva subscript groups e o collector cria occurrences separadas para base/subscripts; portanto o produto precisa carregar root AST identity e occurrence IDs cobertos. A derivação real de cada host `DFHRESP(...)` deve ser confirmada pelo primeiro oracle antes de fechar a factory do classifier.
