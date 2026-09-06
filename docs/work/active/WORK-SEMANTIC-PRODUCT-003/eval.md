# Avaliação

## O que prova corretude

Cada conclusão liga fato real do port a precondição normativa AIR, explicita
fallback e owner, sem confundir resultado de audit com implementação conforme.
O relatório registra fontes fixadas e contracasos reproduzidos.

## Classes positivas

Identidades e origens preservadas, binding nominal fechado, branches visíveis,
unknown_type e opaque válidos sob os limites normativos.

## Classes negativas

Tipo fabricado, assign sem sameDomain/conversão, storage por nome, queda após
terminal desconhecido e retorno ao frontend pelo lowerer.

## Classes ambíguas

Binding ambíguo mantém candidates; tipo/valor/storage/controle unknown são
dimensões distintas; ausência de assinatura não significa zero parâmetros.

## Casos adversariais

CALL/MOVE subscripted perdem endereço no port; terminais e CONTINUE possuem
shape genérica igual. Inputs incompletos, nested units, gaps sem statement,
predicados potencialmente impuros e controle aberto limitam claims agregadas.

## Casos de regressão

Executar fast e semantic; full antes do handoff. Preservar CP8 como evidência
de reconstrução, sem anunciar que valida AIR. F-02 ausente não fica verde por
testes skipped. Sem mudanças em testes, fixtures ou baselines.

## Propriedades/relações metamórficas

Documentar permutação de sequences, multiplicidade sem truncamento, IDs por
namespace, remoção de display metadata e descarte do frontend; implementação
dos oracles AIR permanece futura.

## Expectativas de escala

Audit sem threshold de hardware. Próximos slices devem preservar N ocorrências
e fronteiras abertas compactas, sem materializar todos os pares de labels.
