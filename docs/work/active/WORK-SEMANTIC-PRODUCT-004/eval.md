# Avaliação

## O que prova corretude

Port e JSON permitem reconstruir unit/entry/start/GOBACK/ausência de successor
sem frontend. Integridade rejeita targets ausentes e namespaces cruzados.

## Classes positivas

AIR-FIRST mínimo; assinatura sem cláusulas; provenance e coverage positivas;
program point estrutural; múltiplas ocorrências GOBACK; units top-level/nested.

## Classes negativas

Entry aponta para statement ausente/de outra unit; remover GOBACK conservando
coverage MODELED; perder gap de entry/start/signature; duplicação de fact.

## Classes ambíguas

Procedure ausente, signature com cláusulas, input missing, início não coberto;
inventário alternativo indisponível permanece diferente de zero conhecido.

## Casos adversariais

GOBACK seguido de CONTINUE; STOP RUN/EXIT PROGRAM/GOBACK; ENTRY alternativo;
ordem e IDs que não autorizam entrada/fallthrough; declaratives fora do slice.

## Casos de regressão

Suíte semântica completa, contratos JSON existentes, consumer e probe,
architecture boundary, E2E do normalizador e naming.

## Propriedades/relações metamórficas

Serialização repetida/análises equivalentes byte-identical; acrescentar
statements após GOBACK não modifica seu término; IDs locais iguais em units
distintas não misturam ownership; escala plural não trunca inventário.

## Expectativas de escala

Construção da relação de entrada e tradução dos fatos com percursos lineares
e joins por identidade; sem pares de successors ou análise de reachability.
Executar performance como regressão e registrar gates e totais no state.

Evidência TDD: antes de produção, `mvn -q -Dtest=SemanticProductEntryGobackTest test`
falhou em testCompile pelas APIs Entry/GOBACK ausentes. O oracle adicional
`compositionPublishesCanonicalBoundaryFilename` falhou com NoSuchFile antes
da correção do runner, e passou depois. Regressões antigas migraram somente
as expectativas de GOBACK genérico e envelope JSON/State, mantendo fixtures,
grammar, manifestos e guardas de cardinalidade/metadata.

Limitação confirmada de declaratives (remainder de BACKLOG-SP-005): o builder
não materializa essas regiões. A nova boundary conserva gap, inventário parcial
e start indisponível; modelar as regiões exige outro slice autorizado.

```yaml
downstream_impact:
  class: REDUCES_PRECISION
  rationale: A boundary conserva a região indisponível explicitamente e não afirma controle fechado; falta precisão de entrada/regiões, não foi implementado nem refutado um CFG downstream.
  evidence:
    - AstBuilder.buildProcedure omite procedureDeclaratives do inventário AST.
    - SemanticProductEntryGobackTest.declarativesKeepInventoryAndStartOpen exige gap e inventário parcial.
```
