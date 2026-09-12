# eval

## O que prova corretude

Oracles independentes via port/JSON para fixtures closed/open e negativos; RED A completion/B predicate/C ELSE/D storage antes de produção.

## Classes positivas

FLAG = literal básico; MOVE PROGA/PROGB com fitting X8; braços com mais de um statement.

## Classes negativas

Refmod/subscript/função/aritmética/relação fora do profile, input/recovery, provenance incompleta, REDEFINES/RENAMES/overlay/layout não admitido.

## Classes ambíguas

Binding ambíguo/unresolved mantém candidatos e nenhuma prova de acesso/predicate.

## Casos adversariais

Remover completion; inverter braços; fabricar ELSE; remover read/false COMPLETE; perder whole-item; admitir condição/storage indevidos; REDEFINES; flatten nested; ordenar controle por ID/point; perder provenance. Compilação quebrada não conta como mutação morta.

## Casos de regressão

W1 MOVE/fitting/literal e dynamic CALL, bindings/IDs/provenance/origins/continuation, coverage/readiness/determinismo. Golden antigo imutável.

## Propriedades/relações metamórficas

Renomear itens, mudar literal, perturbar IDs/ordem física preservando relações, dois processos frontend com bytes iguais. Nenhuma avaliação de predicate.

## Expectativas de escala

Contadores N/2N de statements/relações/declarations/reads. O(S+R+D), nenhum limiar de hardware.
