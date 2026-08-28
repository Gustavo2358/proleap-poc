# Backlog atual

Este arquivo registra trabalho futuro válido que não pertence a um work item ativo. Ordem não implica autorização para iniciar; cada item deve receber spec, eval e boundary explícitos antes de implementação.

## Arquitetura

### BACKLOG-ARCH-001 — Fronteiras por pacotes e Clean Architecture

Refatorar o package único para componentes com dependências direcionais verificáveis (frontend, AST, symbols, occurrences, resolution, presentation). Preservar APIs/produtos e usar os invariantes atuais como oracle. Depois da migração, substituir o check de bytecode por regras primariamente baseadas em package boundaries quando isso proteger o conceito sem acoplar nomes acidentais.

## CFG e efeitos semânticos

### BACKLOG-CFG-001 — CFG estrutural incremental

Introduzir produto CFG separado da AST e do binding nominal. Fatiar por fluxo linear, basic blocks, `IF`, `EVALUATE`, `GO TO`, `GO TO DEPENDING ON`, `PERFORM`, `PERFORM THRU`, `NEXT SENTENCE`, terminação e fallthrough. Cada slice precisa de oracle adversarial próprio.

### BACKLOG-CFG-002 — Statements preservados com efeito de fluxo

Modelar incrementalmente `ALTER`, `SEARCH`, `SORT`, `MERGE` e `ENTRY`, mantendo fallback conservador. Outros statements preservados (`CANCEL`, comunicação, report writer e display/exhibit) entram conforme necessidade de análise concreta.

### BACKLOG-DF-001 — StatementEffects e reaching definitions

Criar produto separado com reads, writes, partial writes, kills e unknown memory effect. Cobrir `MOVE`, group/CORRESPONDING, reference modification, `SET`, `STRING`, `UNSTRING`, `INITIALIZE`, `ACCEPT`, aritmética, operações de arquivo e parâmetros de `CALL`. Modelar aliases de `REDEFINES`/`RENAMES` como regiões, não apenas nomes.

### BACKLOG-DF-002 — Targets de CALL dinâmico

Usar CFG e dataflow para calcular conjuntos de programas possíveis sem confundir binding da variável com seu valor. Preservar targets conhecidos e remainder dinâmico. `CBSTM03D` é cenário didático, não especificação completa.

## Linguagens embarcadas e built-ins

### BACKLOG-EMB-001 — Porta para analisadores embarcados

Definir `EmbeddedLanguageAnalyzer` e plugins com parser dedicado para SQL/CICS/SQLIMS. Ligar host variables às occurrences COBOL; SQL/comando dinâmico permanece desconhecido até análise de valores. Regex não substitui parser.

### BACKLOG-DIALECT-001 — Built-ins e opções adicionais

Versionar special registers/intrinsics por compilador e ampliar opções somente com fonte semântica e configuração explícita. Modos `PGMNAME` fora de COMPAT/LONGUPPER/LONGMIXED e `CALLINTERFACE` por statement continuam não modelados.

## Codebase e dependências externas

### BACKLOG-RUNNER-001 — Runner e catálogo de codebase

Criar runner por codebase, repositório de copybooks, catálogo externo persistente e agregação paralela. Correção, aliases e completude do catálogo precisam permanecer premissas observáveis, não garantias implícitas do resolver.

### BACKLOG-DEPS-001 — Fatos finais de dependência

Produzir fatos de subprogramas/arquivos somente depois de combinar binding, coverage, CALL semantics e dataflow necessários. ASSIGN/DDNAME final e call graph externo não podem derivar apenas de literal ou candidate nominal.

## Observabilidade e apresentação

### BACKLOG-OBS-001 — Identidade e métricas operacionais

Adicionar SHA-256 do input, duração e tamanho de índices por fase fora do snapshot determinístico; transportar include chain completa também em gaps globais.

### BACKLOG-OBS-002 — Contexto concorrente

Propagar MDC explicitamente quando houver processamento assíncrono/multithread e testar isolamento entre tarefas.

### BACKLOG-OBS-003 — Diagnostics tipados

Avaliar evolução compatível de `Diagnostic` para code/severity estáveis, eliminando consumidores textuais remanescentes sem forçar breaking change transversal.

### BACKLOG-UI-001 — Navegação multi-unit

Evoluir AST/Symbol Table HTML para nested e múltiplos program units, preservando links honestos quando uma unidade não está materializada na página.
