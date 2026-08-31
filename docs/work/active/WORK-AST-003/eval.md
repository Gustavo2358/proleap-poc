# Eval

## O que prova corretude

O Discovery é correto se reproduz a falha depois da construção da AST, discrimina as ordens relevantes, encontra todas as fontes comprovadas de divergência, classifica todos os tipos/consumidores com evidência e mantém gates normais verdes sem corrigir produção. O oracle opt-in deve falhar no primeiro nó divergente usando exclusivamente `Ast.children`.

## Classes positivas

- Procedure `PERFORM TARGET-PARA UNTIL` com condição composta.
- Procedure `PERFORM FIRST-PARA THRU LAST-PARA UNTIL` com condição simples.
- Declaração aceita pela grammar com `EXTERNAL GLOBAL`, que produz diagnostic metadata sem nó.
- Superfície representativa existente em `ast-cfg-boundary.cbl`, `statements.cbl`, `declarations.cbl`, `expressions.cbl` e `references.cbl`.

## Classes negativas

- Procedure `PERFORM` sem controle: referências são construídas na ordem estrutural e o snapshot passa.
- Inline `PERFORM UNTIL`: controles precedem body tanto na construção quanto em `Ast.children` e o snapshot passa.
- Metadata reutilizada por findings não cria novo ID; somente uma chamada alocadora extra pode gerar gap.

## Classes ambíguas

- `UnsupportedStatement` possui traversal declarada, mas não tem builder de produção atual; classificação `NÃO APLICÁVEL`.
- Pre-order é intencional no snapshot/história e necessário à compatibilidade atual, mas ainda não possui statement canônico próprio; promoção normativa depende de review.
- Futuras análises precisam de identidade única/determinística/reachable, não inerentemente de pre-order.

## Casos adversariais

- A profundidade diferente das duas condições altera o offset real (`21` versus `17`), rejeitando correção por constante.
- `THRU` adiciona duas referências estruturalmente anteriores ao controle.
- Metadata diagnóstica consome o mesmo contador sem materializar nó, provando uma segunda forma da classe de defeito.
- O oracle verifica também duplicação de instância, ciclos e filhos nulos nos caminhos exercitados.

## Casos de regressão

- Baseline `full` na base do PR #10.
- Fixtures semânticas representativas já existentes.
- WORK-RES-004, consultado somente como evidência histórica de preservação deliberada do pre-order.

## Propriedades/relações metamórficas

- Aumentar a profundidade da condição pode mudar o offset da falha, mas não o primeiro tipo estrutural divergente: `ProcedureReference`.
- Trocar procedure `PERFORM` controlado por inline mantém a mesma expressão, mas remove a inversão porque a ordem estrutural muda.
- Adicionar metadata não estrutural não deveria alterar IDs dos nós posteriores; o comportamento atual viola essa propriedade.
- Para uma AST válida, traversal canônica produz IDs únicos exatamente no intervalo `0..nodeCount-1`, sem ciclos nem instância compartilhada.

## Expectativas de escala

O oracle percorre cada nó e aresta uma vez, com sets por identidade/ID: tempo e espaço `O(nodes)`. Nenhum scan cruzado de símbolos, occurrences ou declarations é necessário.
