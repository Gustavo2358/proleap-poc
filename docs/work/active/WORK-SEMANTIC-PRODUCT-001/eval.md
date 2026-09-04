# Eval do Discovery

## O que prova corretude

O Checkpoint 1 é correto quando o relatório reconstrói o fluxo executável pelos produtores e consumidores reais, distingue produtos de domínio de projections de presentation/observability, não perde os joins necessários, identifica os namespaces de identidade, mostra provenance e análise parcial com evidência, e transforma R1–R7 em requisitos observáveis sem desenhar uma IR. O relatório também precisa separar explicitamente estado atual, futuro documentado, hipótese e unknown.

## Classes positivas

- Pipeline e ownership confirmados por `ExplorerMain` e tipos concretos.
- AST, compilation units, symbol tables, occurrences, resolução, classificação externa, coverage, diagnostics e snapshots reconciliados com seus testes.
- CALL literal externo separado de CALL variável e de valor de runtime.
- Source principal, COPY e COPY aninhado rastreáveis por provenance materializada.
- Resolução `RESOLVED`, `AMBIGUOUS`, `UNRESOLVED`, `UNSUPPORTED` e `EXTERNAL_OBSERVED` preservada sem colapsar incerteza.
- R1–R7 e a matriz de control-flow ancorados em AST, occurrences, binding e gaps existentes.

## Classes negativas

- Criar um objeto de produção, serializer, JSON schema, IR, CFG ou consumer.
- Inferir possible-values ou target de runtime a partir de binding nominal.
- Tratar snapshot HTML, string formatado ou ID local isolado como contrato estável.
- Promover `ConditionSemantics`, `ConditionValidation`, CFG ou dataflow documentados para produtos existentes.
- Corrigir F-01 ou lacunas de statements durante o Discovery.

## Classes ambíguas

- O conjunto fechado de campos de uma futura boundary.
- Necessidade de atravessar o `SourceMap` inteiro versus provenance localizada.
- Persistência e determinismo de IDs entre gerações, versões e compilation units.
- Forma record, facade ou envelope; extensão/versionamento; relação exata com futuros produtos pós-binding.
- Suficiência de statements preservados para lowering futuro.

## Casos adversariais

- COPY ausente com fatos conhecidos e gaps simultâneos.
- COPY aninhado com include chain.
- Referência unresolved, ambígua e candidate cross-unit.
- `CALL 'XPTO'` externo e `CALL WS-PGM` variável.
- Nested programs, scopes, shadowing e program visibility.
- `EVALUATE TRUE` com condição nominal simples e com `FLAG-ON AND OTHER-ON` (F-01 sem correção).
- `ALTER`, `SEARCH`, `GO TO DEPENDING ON`, `PERFORM THRU`, `NEXT SENTENCE` e terminais genéricos.
- `PERFORM TIMES`, `PERFORM UNTIL`, `PERFORM WITH TEST BEFORE` e `PERFORM WITH TEST AFTER`: count, predicate e test mode precisam ser desafiados separadamente; `writtenControl`/posição não provam papéis tipados.
- `PERFORM VARYING`, incluindo `AFTER` aninhado: lista ordenada de expressions não prova papéis de variável, inicialização, passo, predicado, níveis ou cadeia `AFTER`.
- `EXIT PERFORM`, `EXIT PARAGRAPH` e `EXIT SECTION`, separados de `EXIT PROGRAM`: ausência de node/kind dedicado ou de suporte pela grammar não pode ser relatada como estrutura suficiente.
- compiler options ausentes: `PgmnameMode`, `DynamMode` e `DllMode` ficam explicitamente `UNSPECIFIED`, enquanto `CALL 'XPTO'` continua publicando o target nominal observado e localiza somente linkage como `UNKNOWN`.
- `EXEC CICS`, `EXEC SQL` e `EXEC SQLIMS` com payload opaco.

## Casos de regressão

Os gates existentes devem continuar verdes para AST/pre-order, symbols, occurrences, resolução, CALL semantics, coverage, provenance, classificação externa, snapshots e architecture boundary. A lista de evals no `work-item.yaml` é a seleção canônica desta investigação.

## Propriedades/relações metamórficas

- Mesma entrada normalizada e mesma policy produzem a mesma ordem de produtos e identidades locais.
- Alterar apenas a declaração que dá contexto a uma condition-name pode mudar binding sem alterar a surface AST lossless.
- Substituir um COPY disponível por um ausente preserva fatos independentes e torna a incompletude explícita.
- Inserir uma unidade aninhada não pode permitir join por ID local sem `ProgramUnitId`.
- Projeções de snapshot podem omitir detalhes, mas não devem ser tratadas como fonte para reconstruir o domínio.
- Um construct só é estruturalmente suficiente para lowering quando seus papéis semânticos podem ser reconstruídos sem parse tree, grammar contexts, conhecimento implícito da ordem da grammar ou reparse de strings.

## Expectativas de escala

Nenhuma propriedade algorítmica nova é proposta. O Discovery registra apenas que a futura composição precisa considerar cardinalidade de unidades, nós, símbolos, occurrences, entries, candidates, diagnostics e provenance; qualquer limite ou otimização será decisão posterior e só poderá ser introduzido com preservação semântica.
