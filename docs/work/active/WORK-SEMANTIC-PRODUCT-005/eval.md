# eval

## O que prova corretude

Oracle independente só pelo port e JSON: DATA textual escalar, literal lógico, target inteiro, cópia completa e MOVE → GOBACK.

## Classes positivas

Fixture AIR-MOVE e nomes/valores equivalentes; normalização de delimitadores duplicados; DISPLAY explícito.

## Classes negativas

Padding, truncation, numeric, OCCURS, REDEFINES/RENAMES, group, subscript, ref-mod, contexto/storage não coberto.

## Classes ambíguas

Binding ausente/ambíguo e relação canônica de continuação ausente não recebem prova.

## Casos adversariais

Dez challenges do pedido 4A: literal indiscriminado, mismatch, exclusões, selected, acesso nominal, ordem incidental, continuação removida, GOBACK fallthrough, provenance perdida, DATA duplicado.

## Casos de regressão

CP3 integral, oracles anteriores, gates fast/semantic/performance/full.

## Propriedades/relações metamórficas

Renaming preserva propriedades; ordem física não substitui relação canônica; 10.000 refs não duplicam DATA; determinismo de JSON.
Remediação de memória: write(port, file) é byte-idêntico a serialize(port) em
fixtures pequenos positivos, negativos e CP3, inclusive reescrita/truncamento.
Guard arquitetural rejeita a chamada de materialização integral no caminho de
arquivo; o caminho anterior deve ficar RED. Probes usam write, sem byte[] total.

## Expectativas de escala

50k–120k linhas físicas com poucos statements; massa de declarations/MOVEs; visitas e lookups lineares, bytes JSON e tempo observado sem SLA. GREEN → RED controlado → restore → segundo GREEN.
