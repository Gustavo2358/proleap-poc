# Relatório de investigação — resolução de referências DATA/INDEX

Data: 2026-08-28

## Resultado

| Hipótese do handoff | Veredito | Evidência |
| --- | --- | --- |
| A — índice fora de subscrito é rejeitado | Confirmada | `INDEXED BY TABLE-IDX` resolve como subscrito, mas `SET TABLE-IDX TO 1` e `IF TABLE-IDX > 0` retornam `UNRESOLVED/INVALID_NAMESPACE_FOR_CONTEXT`. |
| B1 — alvo de `REDEFINES` é lookup nominal, não estrutural | Confirmada para `REDEFINES` | Dois `X` no mesmo nível fazem o alvo de `Y REDEFINES X` retornar dois candidatos e `AMBIGUOUS`. A regra exige o item imediatamente precedente. |
| B1 — `FILLER` entra como candidato | Refutada | O construtor não cria símbolo para `FILLER`; ele não pode entrar no pool nominal. |
| B2 — duplicata DATA sem qualificação deve escolher o escopo mais próximo | Refutada | No mesmo programa, uma referência DATA deve ser única ou qualificada. A ambiguidade atual é correta; o escopo de programas aninhados já é tratado separadamente. |
| B1 — `RENAMES` deve usar a mesma regra posicional de `REDEFINES` | Não confirmada | `RENAMES` é uma faixa estrutural, não o mesmo contrato posicional de `REDEFINES`; não há reprodução válida de ambiguidade artificial neste recorte. |

## Reproduções antes da correção

As fixtures versionadas são `index-name-value-context.cbl` e `redefines-positional-target.cbl`. Os testes de expectativa final ficam em `DataAndIndexReferenceResolverTest`; nesta branch de correção, eles estão ativos.

1. A fixture de índice produz seis referências: a declaração, o item de tabela, o subscrito e o destino do `MOVE` resolvem; `SET TABLE-IDX TO 1` e `IF TABLE-IDX > 0` produzem dois gaps `REFERENCE_UNRESOLVED_INVALID_NAMESPACE_FOR_CONTEXT`.
2. A fixture de `REDEFINES` é sintaticamente válida e produz uma referência `REDEFINES_TARGET` a `X`. Ela recebe os dois símbolos `X` como candidatos e termina `AMBIGUOUS/MULTIPLE_VALID_CANDIDATES`.

Os dois comandos de reprodução usados foram:

```text
mvn -q exec:java -Dexec.args='--source /tmp/index-namespace-probe.cbl --copybooks /tmp --output /tmp/index-namespace-output'
mvn -q exec:java -Dexec.args='--source /tmp/redefines-position-probe.cbl --copybooks /tmp --output /tmp/redefines-position-output'
```

Em ambos os casos o parser reportou zero erros. A suíte existente passou com `mvn -q test` antes de introduzir qualquer correção.

## Evidência de implementação

### A — admissibilidade de `INDEX`

`ReferenceOccurrenceCollector.visitStatementOperands` classifica todos os operandos de statements genéricos que não são FILE como `CONTEXT_DEPENDENT`. Em seguida, `addDataReference` atribui `DATA` e, sem override, apenas `{DATA}` como `admissibleKinds`. `SET` entra nessa rota genérica. Já o caminho de subscrito é a exceção que concede `{DATA, INDEX}`.

`DataAndIndexReferenceResolver.resolveDataOccurrence` passa esses kinds para `compatibleCandidates`. Como `INDEX_NAME` só é compatível com `INDEX`, nenhum candidato sobrevive; havendo homônimo no caminho de busca, o resultado é `INVALID_NAMESPACE_FOR_CONTEXT`.

### B1 — `REDEFINES`

`resolveRedefines` usa `lookupLocal(owner.scopeId(), DATA, relation.writtenTarget())` e seleciona todo `DATA_ITEM` homônimo. Ele não consulta ordem de declaração, predecessora imediata nem a posição estrutural do alvo. Por isso a reprodução termina ambígua.

`SymbolTableBuilder.collectDataEntries` já exclui `FILLER` da tabela de símbolos (`symbolId = -1`), portanto essa parte da hipótese não é a causa.

### B2 — por que não corrigir

O teste estabelecido `resolvesSimpleDuplicateMissingAndIncompatibleNames` já declara correta a ambiguidade de `DUPLICATE-ITEM` sem qualificação. A política do projeto também preserva todos os candidatos que atendem à qualificação. Alterar isso para uma preferência por proximidade quebraria tanto o contrato existente quanto a regra COBOL de unicidade de referência.

## Regra canônica e correção proposta

IBM Enterprise COBOL documenta que index-names são criados por `INDEXED BY`, podem ser inicializados/modificados por `SET` e usados em condições relacionais. Logo, A deve ser corrigido por role semântico, não liberando `INDEX` para qualquer `DATA` bare: a coleta deve distinguir alvo/operando de `SET` para índices e operando de condição relacional, atribuindo `{DATA, INDEX}` apenas onde a gramática e a semântica permitem ambos. Se a AST genérica de `SET` não conservar papel suficiente, deve-se modelar esses operandos antes de mudar o resolver.

Para B1, o resolver de relações declarativas deve usar uma estratégia distinta para `REDEFINES`: localizar o predecessor imediato elegível no mesmo pai estrutural e validar o mesmo nível estrutural. A busca nominal não é suficiente. A modelagem publicada pode permanecer: a `DeclarationRelationResolution.Entry` continua apontando para um único candidato, mas a origem da decisão muda de nominal para estrutural.

Não recomendo uma correção B2. Também não recomendo combinar `RENAMES` com a estratégia de predecessor de `REDEFINES` sem uma reprodução válida e uma regra específica: os contratos semânticos são diferentes.

Fontes canônicas: [INDEXED BY phrase](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=clause-indexed-by-phrase), [Indexing](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=table-indexing), [Relation conditions](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=expressions-relation-conditions), [REDEFINES clause](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-redefines-clause), and [Uniqueness of reference](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=names-uniqueness-reference).

## Resultado da correção

Os artefatos gerados em diretórios temporários separados confirmam o delta esperado:

| Fixture | Antes | Depois |
| --- | ---: | ---: |
| `index-name-value-context.cbl` | 2 gaps; 4 resolvidas | 0 gaps; 6 resolvidas |
| `redefines-positional-target.cbl` | 2 gaps; 1 referência ambígua | 0 gaps; 1 referência resolvida e 1 relação resolvida |

AST, contagem de símbolos e vocabulário publicado de roles não mudaram nos dois cenários. A admissão de `INDEX` para condições relacionais é preservada internamente pelo coletor; a ocorrência publicada continua como `VALUE_READ`.
