# 05 — Semântica de controle e invocações

**Analysis IR 2.0.0 — Normativo**

## 1. Modelo de transição

Uma execução entra pelo label de uma `Entry`. Operações comuns da sequência são executadas em ordem; seu terminador determina a transição seguinte. Transferências diretas usam labels da mesma unidade. Uma passagem para outra unidade exige `invoke`; contenção lexical não a autoriza.

A ordem física de sequências é irrelevante. Não há successor criado apenas por proximidade de linhas ou IDs. Ciclos são expressos por transferências para labels anteriores ou já visitados; não existe pressuposto de aciclicidade, aninhamento estruturado ou reducibilidade do grafo.

## 2. Bifurcação e reconvergência

`branch(p,t,f)` exige `TypeRef=known(bool)` e pode produzir `t` quando `p=true` e `f` quando `p=false`. Se o valor de `p` é abstratamente desconhecido, ambos são admitidos; `unknown_type` não é um predicado booleano válido. A reunião de caminhos ocorre porque suas transferências alcançam uma continuação comum, não porque a IR armazena um “join calculado”.

Um ramo vazio deve transferir para a continuação apropriada. Ramo que retorna, termina ou diverge não ganha aresta artificial de reconvergência. Uma construção estruturada pode ser normalizada em múltiplas sequências, preservando em origem o vínculo entre elas.

Apenas uma prova semântica aplicável pode eliminar um destino de um CFG derivado. O produtor não pode inventar valor de predicado para melhorar a aparência da análise.

## 3. Seletores e laços

`dispatch` possui casos exaustivamente enumerados mais default. Seu seletor é avaliado uma vez. Faixas, prioridade e predicados combinados requerem normalização que preserve ordem e avaliação.

Um laço de pré-teste entra no teste antes do corpo. Um laço de pós-teste entra no corpo antes do teste. Contagem, inicialização, atualização e condições secundárias são operações e transferências explícitas. Uma lista de expressões sem esses papéis não é representação suficiente.

A IR não exige um nó “loop”. Um consumidor pode identificar ciclos e componentes fortemente conectados como resultado derivado. Laços com múltiplas entradas não são inválidos por serem não estruturados.

## 4. Resultados de invocação

Uma interação declara alternativas de controle:

| Alternativa | Significado |
| --- | --- |
| `normal(label)` | Retorno normal para o label local |
| `exception(tag, label)` | Exceção identificada encaminhada ao label local |
| `exception(tag, propagate)` | Exceção propagada para fora da unidade |
| `any_exception(label ou propagate)` | Encaminhamento de tags não explicitamente enumeradas |
| `halt` | Possibilidade de terminar a execução |
| `diverge` | Possibilidade de não produzir próximo estado observável |
| `open(scope)` | Outros comportamentos de controle dentro do escopo |

Tags explícitas são únicas. `any_exception` vale apenas para tags restantes e não compete com uma tag específica. A publicação deve declarar se as alternativas são exaustivas. Sem `any_exception` ou `open`, a ausência de uma exceção é uma garantia semântica que exige suporte.

Uma chamada externa DEVE publicar os resultados da abstração suportada. Não conhecer seu corpo ou faltar implementação não cria exceção, término, divergência, reentrada ou salto arbitrário. Um outcome aberto requer uma regra semântica positiva e conserva seu escopo. Continuação normal reconhecida é preservada; uma forma modelada sem retorno não recebe retorno normal por causa de cobertura parcial. O núcleo não impõe um outcome único para toda chamada.

Uma lista vazia de alternativas só pode representar divergência se ela estiver explicitamente declarada como tal. A ausência de informação não é divergência comprovada.

`InvocationOutcomes` não é o `ControlEnvelope` genérico. Possui no máximo um `normal`, uma alternativa por tag e um `any_exception`; `halt` e `diverge` são possibilidades independentes, e `open` conserva seu escopo. Destinos locais pertencem à unidade chamadora. Saída normal da unidade e salto ordinário não são retornos normais de uma interação; outros comportamentos possíveis de chamada ficam no restante de controle quando não delimitados pelo catálogo.

## 5. Retornos válidos

Quando o consumidor analisa o corpo de uma entrada chamada, o retorno normal deve corresponder à invocação que iniciou aquela ativação. Retornar para a continuação de qualquer chamada com o mesmo target gera caminhos espúrios e não é precisão exata.

Uma análise insensível a contexto pode usar sobreaproximação explícita, desde que não a apresente como pareamento exato e preserve os limites nas consultas. O perfil de controle local exige os oráculos de pareamento definidos em [conformidade](../conformidade/02-oraculos.md).

O uso de resumo ou corpo deve preservar as distinções entre estado de entrada, efeitos antes do retorno, resultados normais e efeitos de saídas excepcionais. Um resumo ausente não equivale a identidade de estado.

## 6. Fronteiras desconhecidas

`ControlScope` pode designar conjunto explícito de labels da unidade, todas as entradas/labels admissíveis de uma unidade, saídas normais/excepcionais, término, divergência e controle externo não enumerado. `any_control` é a união máxima de comportamento não delimitável no escopo da publicação e ambiente.

Um consumidor pode representar a fronteira aberta de forma compacta. Contudo, ela deve afetar toda consulta cuja alcançabilidade ou estado possa depender dela. Conectar uma fronteira apenas ao fim da unidade, ignorando possíveis saltos para o interior, é incorreto quando o escopo admite esses saltos.

Se não for possível produzir um CFG finito fechado com a precisão declarada, o consumidor deve publicar CFG parcial mais fronteira explícita, ou resultado indisponível para aquele escopo. Não pode criar fallthrough ordinário por conveniência.

## 7. `control.local@1`: invocação de trecho com memória compartilhada

Essa extensão representa chamadas locais que compartilham a ativação de memória da unidade. Ela não cria nova ativação, nem inicializa parâmetros de unidade. Usa uma pilha semântica de continuações locais; não prescreve sua implementação.

Um frame contém identidade da operação invocadora, destino de retorno e conjunto de portas de conclusão. Uma `CompletionPortId` pertence à unidade e pode ser referida por vários frames. Uma porta é um evento de conclusão, não um endereço de memória ou rótulo de exibição. Portas são declaradas no namespace da unidade. Mais de uma operação `local.boundary` pode sinalizar a mesma porta; as operações mantêm identidades de ocorrência distintas. Referir porta não declarada é falha de fechamento.

### 7.1 `local.invoke`

```text
local.invoke(entry: LabelId, completionPorts: CompletionPortId[], resume: LabelId)
```

Empilha frame e transfere para `entry`. Não cria aresta de execução direta para `resume`. O conjunto de portas pode ser vazio quando o trecho usa `local.resume` explícito. Entradas, continuações e portas devem estar fechadas sobre a mesma unidade.

### 7.2 `local.boundary`

```text
local.boundary(port: CompletionPortId, default: LabelId)
```

Se o frame do topo inclui `port`, remove esse frame e transfere à sua continuação. Caso contrário, transfere a `default` sem alterar a pilha. A regra também se aplica quando a pilha está vazia. Não busca frames abaixo do topo.

Essa operação permite que o mesmo trecho tenha continuação sequencial ordinária quando não invocado e retorno dinâmico quando executado sob um contexto local apropriado. Portas em fronteiras diferentes podem delimitar trechos que compartilham operações. A extensão não infere intervalos a partir de nomes ou ordem de declarações.

### 7.3 `local.resume`

```text
local.resume()
```

Remove o frame do topo e transfere à continuação nele registrada. Pilha vazia produz saída excepcional `invalid_local_return`, não fallthrough nem retorno normal da unidade. O consumidor deve conservar esse resultado se não houver prova que o exclua.

### 7.4 `local.unwind`

```text
local.unwind(count: nonnegative int, destination: LabelId)
```

Remove exatamente `count` frames e transfere a `destination`. Contagem superior à profundidade corrente produz saída excepcional `invalid_local_unwind`. Um `jump` ordinário não remove frames. Se uma linguagem exige abandono de contextos ao transferir, seu produtor deve explicitá-lo; a IR não adivinha essa regra.

`return`, `raise` e `halt` encerram a ativação pertinente e descartam seus frames locais. Invocações normais de outra unidade têm contextos separados. Recursão local é admissível, sem limite semântico fixo.

### 7.5 Conservadorismo e conformidade

O envelope de `local.invoke` inclui entrada e efeitos potenciais do trecho, não uma chamada externa por nome. O fallback de uma fronteira deve admitir todos os retornos correspondentes a frames possíveis e seu default. Um consumidor sem a extensão pode conservá-los como alternativas conservadoras, mas não alegar pareamento preciso.

O produtor só pode usar essa extensão quando sua semântica de conclusão corresponde à regra de topo definida aqui. Um comportamento-fonte diferente requer outra extensão ou abstração, não mudança contextual do significado de `local.boundary`.

## 8. `control.indirect@1`: destinos de controle como valores

O tipo `label(S)` contém exatamente os labels de um conjunto finito não vazio `S`, todos da unidade proprietária. Um literal `label(l,S)` exige `l ∈ S`. O valor pode residir em célula, ser lido, atribuído, passado por valor e comparado por igualdade. Não há coerção com inteiro, endereço ou nome externo.

```text
indirect.jump(target: Expression<label(S)>, within: S)
```

Avalia `target` e transfere ao label resultante. O limite `S` é parte do tipo e do contrato, não um resultado inferido de dataflow. Na ausência de valor refinado, o CFG admite todos os labels de `S`. A operação não modifica memória; a leitura do target é identificável.

Essa assinatura exige `known(label(S))`. `unknown(known(label(S)),...)` mantém o universo `S`; `unknown_type(u)` não estabelece esse universo e não permite `indirect.jump` preciso. O limite `within` sozinho não converte um operando de tipo desconhecido em label.

Uma análise posterior pode reduzir os destinos por valores possíveis, desde que preserve o restante desconhecido e a consistência da revisão de CFG utilizada. O CFG conservador inicial não depende de um cálculo prévio de reaching definitions, evitando dependência circular.

Uma possibilidade de destino fora de `S` impede usar esse tipo como representação exata. O produtor deve ampliar `S`, usar fronteira aberta ou preservar a operação como opaca. Não se pode fechar um conjunto de targets apenas a partir dos destinos já observados em um corpus.


A extensão opcional [target.possibilities@1](14-possibilidades-de-target.md) admite
ComputedTarget com domínio desconhecido para preservar alternativas textuais comprovadas.
Sem essa capability, permanece a precondição do núcleo de expressão `known(text)`.
