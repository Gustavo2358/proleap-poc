# Eval

## O que prova corretude

A implementação é correta se os triggers atravessam `AstSnapshot`, o oracle normal percorre exclusivamente `Ast.children` e prova `id == posição` sem ciclos, filhos nulos ou instâncias duplicadas, e diagnostics conflitantes continuam ancorados às declarações sem consumir o contador estrutural.

## Classes positivas

- Procedure `PERFORM TARGET-PARA UNTIL` com condição composta.
- Procedure `PERFORM FIRST-PARA THRU LAST-PARA UNTIL` com condição simples.
- Os dois call sites atuais de `declarationVisibility`: `FileDescription` e `DataEntry`. Ambos podem receber `external=true` e `global=true` por construções aceitas pela grammar e então produzir diagnostic metadata sem nó.
- Superfície representativa existente em `ast-cfg-boundary.cbl`, `statements.cbl`, `declarations.cbl`, `expressions.cbl` e `references.cbl`.

## Classes negativas

- Procedure `PERFORM` sem controle: referências são construídas na ordem estrutural e o snapshot passa.
- Inline `PERFORM UNTIL`: controles precedem body tanto na construção quanto em `Ast.children` e o snapshot passa.
- Metadata reutilizada por findings não cria novo ID; somente uma chamada alocadora extra pode gerar gap.

## Classes ambíguas

- `UnsupportedStatement` possui traversal declarada, mas não tem builder de produção atual; classificação `NÃO APLICÁVEL`.
- Estabilidade de IDs entre edições do fonte não faz parte do contrato e não deve ser inferida do pre-order.
- Futuras análises precisam de identidade única/determinística/reachable, não inerentemente de pre-order.

## Casos adversariais

- A profundidade diferente das duas condições altera o offset real (`21` versus `17`), rejeitando correção por constante.
- `THRU + UNTIL` adiciona duas referências estruturalmente anteriores ao controle; a fixture não contém `VARYING`.
- Metadata diagnóstica consome o mesmo contador sem materializar nó em qualquer call site conflitante de `declarationVisibility`, provando uma segunda forma da classe de defeito.
- O oracle verifica também duplicação de instância, ciclos e filhos nulos nos caminhos exercitados.

## Casos de regressão

- Baseline `full` do Discovery na base do PR #10 e gates finais sobre a base do PR #11.
- Fixtures semânticas representativas já existentes.
- WORK-RES-004, consultado somente como evidência histórica de preservação deliberada do pre-order.

## Propriedades/relações metamórficas

- Aumentar a profundidade da condição pode mudar o offset da falha, mas não o primeiro tipo estrutural divergente: `ProcedureReference`.
- Trocar procedure `PERFORM` controlado por inline mantém a mesma expressão, mas remove a inversão porque a ordem estrutural muda.
- Adicionar metadata não estrutural não deveria alterar IDs dos nós posteriores; o comportamento atual viola essa propriedade.
- Para uma AST válida, traversal canônica produz IDs únicos exatamente no intervalo `0..nodeCount-1`, sem ciclos nem instância compartilhada.
- Os IDs quebrados exatos são observações de Discovery. A regressão futura deriva posições pelo pre-order de `Ast.children` e exige `id == posição`; ela não troca os hardcodes antigos por números novos.

## Expectativas de escala

O oracle percorre cada nó e aresta uma vez, com sets por identidade/ID: tempo e espaço `O(nodes)`. Nenhum scan cruzado de símbolos, occurrences ou declarations é necessário.
