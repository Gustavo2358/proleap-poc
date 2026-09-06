# Plano

## Fatiamento

Um único slice: RED de entry/GOBACK/JSON e corrupções; fatos canônicos no
frontend; core/port/projector; transporte e consumers independentes; docs e gates.

## Dependências

Baseline main/origin main `7a376f33f55127f53c63b86d3228671b9c6a348d`, merge
do PR #30. WORK-SEMANTIC-PRODUCT-003 arquivado. Nenhuma dependência downstream.

## Superfície arquitetural provável

Ast.GobackStatement e metadata não-node de entrada na PROCEDURE DIVISION;
EntryFact/EntryInventory e GobackFact na boundary; índices e fechamento de
referências; DTO JSON; consumer CP6 e probe CP8 somente pela boundary.

## Migrações requeridas

JSON 1.0.0 → 1.1.0. Consumers devem reconhecer GOBACK tipado e novos campos;
campos e handles existentes mantêm semântica. Testes de classificação genérica
de GOBACK migram para a capacidade dedicada, preservando os outros terminais.
O runner passa a escrever `cobol-semantic-product.json`, preservando o nome
anterior `semantic-product.json` como alias de bytes idênticos. Essa correção
de composição é necessária para concretizar a boundary solicitada.

## Artefatos esperados

Fixture AIR-FIRST, oracles positivos/negativos/adversariais, documentação
durável e PR contra main. Commit/push autorizados; parar para review sem merge.
