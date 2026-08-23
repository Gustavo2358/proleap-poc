# Lacunas de interpretação semântica após o name binding

## Objetivo

Este documento registra o que ainda está **preservado, mas não completamente
interpretado**, depois das etapas Parse Tree → AST → Symbol Table → Reference
Resolution.

Não são falhas silenciosas: todas essas formas permanecem no modelo ou na
cobertura com texto, regra gramatical, span, proveniência e diagnóstico. O
objetivo é indicar quando uma análise futura precisará transformar essa
informação preservada em semântica executável.

O inventário é derivado da gramática e dos manifests versionados, não do corpus.
O arquivo `grammar-rule-manifest.tsv` continua sendo a fonte exaustiva por regra.

## 1. Statements COBOL preservados estruturalmente

Existem 16 famílias COBOL em `PRESERVED_UNINTERPRETED/DEPENDENCY_UNKNOWN`:

`ALTER`, `CANCEL`, `DISABLE`, `DISPLAY`, `ENABLE`, `ENTRY`, `EXHIBIT`,
`GENERATE`, `INITIATE`, `MERGE`, `PURGE`, `RECEIVE`, `SEARCH`, `SEND`, `SORT`
e `TERMINATE`.

Elas já possuem `PreservedStatement`, texto fiel, operandos/clauses reconhecidos
e referências extraídas pela gramática. O que falta é interpretar o efeito
completo de cada statement.

### Exemplo: ALTER

```cobol
ALTER PROCESS-A TO PROCEED TO PROCESS-B
```

Hoje `PROCESS-A` e `PROCESS-B` podem ser preservados e resolvidos como
procedures. Ainda não existe a semântica que informa ao CFG que o destino de um
GO TO alterável pode mudar em runtime.

Implementação possível:

- criar um nó semântico dedicado `AlterStatement`;
- representar origem e novo destino como `ProcedureReference` independentes;
- adicionar um `AlterCfgLowerer` que produza uma aresta dinâmica conservadora;
- manter o CFG incompleto caso o dialeto/padrão de ALTER não seja suportado;
- promover o manifest para `MODELED` somente após testes positivos, negativos e
  de ambiguidade.

### Prioridade para o CFG

| Construção | Semântica futura principal | Prioridade sugerida |
|---|---|---|
| `ALTER` | destinos de fluxo mutáveis | alta |
| `SEARCH` | branches, `AT END` e iteração | alta |
| `SORT`/`MERGE` | INPUT/OUTPUT PROCEDURE e fluxo excepcional | alta |
| `ENTRY` | pontos adicionais de entrada | alta |
| `CANCEL` | ciclo de vida de subprograma | média para dependências |
| `GENERATE`/`INITIATE`/`TERMINATE` | runtime de report writer | média/baixa conforme codebase |
| `ENABLE`/`DISABLE`/`RECEIVE`/`SEND`/`PURGE` | comunicação COBOL | conforme uso do banco |
| `DISPLAY`/`EXHIBIT` | leitura/efeito externo, normalmente sem novo fluxo | baixa para dependências |

## 2. Statements estruturados, mas ainda sem efeitos de dataflow

Os statements classificados como `MODELED/REFERENCE_READY` já preservam seus
operandos e referências. Isso é suficiente para name binding, mas não significa
que reaching definitions já saiba quais operandos são leitura, escrita, escrita
parcial ou efeito desconhecido.

Os casos relevantes incluem `SET`, `STRING`, `UNSTRING`, `INITIALIZE`, `ACCEPT`,
operações aritméticas, `READ ... INTO`, group moves, `MOVE CORRESPONDING`,
reference modification e parâmetros de `CALL`.

### Exemplo: escrita parcial

```cobol
MOVE 'ABC' TO WS-TARGET(2:3)
```

O modelo preserva a variável, offset, comprimento e literal. A futura análise
precisa registrar que isso não redefine necessariamente o valor completo de
`WS-TARGET`.

Implementação possível:

- criar um produto separado `StatementEffects`, sem anotar a AST;
- produzir `reads`, `writes`, `partialWrites`, `kills` e
  `unknownMemoryEffect` por statement;
- usar um registry de lowerers por tipo/regra, com fallback conservador;
- modelar group move e aliases de `REDEFINES`/`RENAMES` como regiões de memória,
  não apenas como nomes independentes;
- fazer o CFG consumir identidades resolvidas e o dataflow consumir
  `StatementEffects`.

Exemplo conceitual:

```text
StatementEffects
├─ reads: []
├─ writes: [DATA_SYMBOL WS-TARGET]
├─ partialWrites
│  └─ offset: 2, length: 3
└─ unknownMemoryEffect: false
```

## 3. Linguagens embarcadas opacas

`EXEC SQL`, `EXEC SQLIMS` e `EXEC CICS` preservam payload e proveniência, mas o
núcleo COBOL não interpreta seu conteúdo.

### Exemplo: SQL

```cobol
EXEC SQL
    SELECT NAME
      INTO :WS-NAME
      FROM CUSTOMER
END-EXEC
```

Hoje o bloco permanece auditável, mas não produz o fato “lê tabela CUSTOMER” nem
o efeito “escreve WS-NAME”.

Implementação possível:

- definir uma porta `EmbeddedLanguageAnalyzer` independente do frontend COBOL;
- criar um plugin SQL com parser próprio para SQL estático;
- retornar tabelas, host variables, operações e diagnostics tipados;
- ligar host variables às ocorrências COBOL já resolvidas;
- manter SQL dinâmico como dependência desconhecida até o futuro dataflow
  resolver a string/comando possível;
- conservar o payload original e nunca extrair tabelas por regex.

Para CICS, a mesma porta pode ter um plugin por comando, capaz de interpretar
recursos como `PROGRAM`, `FILE`, `TRANSID`, `MAPSET` e host variables. Comando
desconhecido deve permanecer explícito, não resultar em zero dependências.

## 4. Cláusulas e expressões preservadas

A gramática contém cláusulas DATA, ENVIRONMENT, PROGRAM e componentes de
expressão que não receberam um nó semântico especializado. Por exemplo,
`BLANK WHEN ZERO` permanece como `PreservedDataClause`.

Nem toda cláusula precisa ser interpretada para descobrir dependências. A regra
é especializá-la somente quando afetar uma análise concreta:

- layout/alias de memória para def-use e group moves;
- visibilidade/qualificação para name binding;
- tamanho e representação quando influencia reference modification;
- file control quando influencia entidade FILE ou DDNAME;
- controle de fluxo quando altera o CFG.

Implementação possível:

- manter o nó preservado como fallback;
- introduzir uma cláusula AST tipada em uma fatia TDD focada;
- atualizar builder, snapshot, coverage e manifest na mesma fatia;
- provar que a informação antiga não desapareceu e que formas não suportadas
  continuam diagnosticadas.

## 5. Special registers, intrinsics e dialeto

Special registers e intrinsics são classificados explicitamente como built-ins,
sem declaração de usuário. Uma análise futura poderá precisar conhecer seus
efeitos ou valores abstratos, mas eles não devem ser inventados como símbolos
DATA comuns.

Além disso, opções como `QUALIFY(EXTEND)` precisam ser informadas pela execução.
O modo `UNSPECIFIED` permanece conservador quando STANDARD e EXTEND poderiam
produzir decisões diferentes.

Implementação possível:

- criar um `CobolBuiltinEnvironment` versionado por compilador/dialeto;
- fornecer policy explicitamente pelo runner da codebase;
- modelar valores apenas em uma camada de abstract interpretation, não na AST ou
  na Symbol Table.

## 6. Entradas externas não são perda semântica

COPY ausente e catálogo externo ausente são condições de input, não defeitos da
AST. O comportamento atual é o desejado:

- preservar nome, localização e motivo;
- marcar a execução como incompleta;
- não inventar declaração, programa ou ausência de dependência.

Implementações futuras podem fornecer um repositório de copybooks e um
`ExternalProgramCatalog` indexado pela codebase. Os contratos atuais já permitem
essa integração sem colocar símbolos globais dentro da AST.

## 7. Observabilidade operacional ainda pendente

Antes de executar em uma codebase massiva, ainda é recomendável acrescentar:

- SHA-256/identidade do input no relatório de cada execução;
- duração e tamanho de índices por etapa fora do snapshot determinístico;
- provenance/include chain completa também nos gaps globais;
- runner paralelo, catálogo persistente e agregação por codebase;
- páginas AST/Symbol Table multi-unit ou outra interface para nested programs.

Esses itens não mudam a semântica da AST/resolução, mas são necessários para
auditoria, reprodutibilidade e operação em escala.

## Ordem incremental sugerida

1. Construir o CFG para os statements de fluxo já modelados.
2. Adicionar lowerers de `ALTER`, `SEARCH`, `SORT`, `MERGE` e `ENTRY`, sempre com
   fallback conservador.
3. Criar `StatementEffects` e implementar as famílias necessárias ao reaching
   definitions de CALL dinâmico.
4. Produzir fatos de dependência de subprogramas e arquivos.
5. Adicionar plugins SQL/CICS e integrar host variables ao dataflow.
6. Criar runner/catálogo/observabilidade para a codebase massiva.

Cada fatia deve seguir TDD, atualizar o manifest e nunca converter uma lacuna em
“nenhuma dependência”.
