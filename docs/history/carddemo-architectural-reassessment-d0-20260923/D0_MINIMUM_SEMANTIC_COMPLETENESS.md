# MINIMUM SEMANTIC COMPLETENESS

**Nossa abordagem de não modelar tudo está errada? PARTIALLY.** O erro é tratar dimensões com obrigações diferentes como igualmente dispensáveis e deixar provas globais apagarem fatos independentes. D0 não sustenta a necessidade de um emulador COBOL.

| Camada | Mínimo para dependency extraction útil | Partiality aceitável | Limite demonstrado |
| --- | --- | --- | --- |
| L0 lexical/preprocessing | caracteres, colunas, tokens, COPY ownership e proveniência corretos | input ausente explicitado com impacto delimitado ou unknown | F1 impede até observar o programa; normalização errada muda linguagem |
| L1 control skeleton | cobertura muito alta de entries, boundaries, outcomes, transfers, returns e arms/handlers presentes | controle unknown com bound justificado, nunca tratado como impossibilidade | 47 F2/F3; F7 prova que edge não inventariado quebra integridade |
| L2 storage identity/layout | identidades, regiões, possíveis aliases e relações necessárias às transferências conhecidas | offsets/layout desconhecidos por região; logical views quando provados | F4/F5/F6 mostram gate global excessivo; separar células sem prova de alias seria incorreto |
| L3 value transformations | transferências relevantes a targets e overwrites seguros; distinguir valor conhecido de escrita conhecida | COMP-3/arithmetic/INITIALIZE values podem ser unknown | unknown assignment não preserva automaticamente um literal antigo; must-overwrite depende de prova |
| L4 effects/environment | leituras/escritas/exposures e eventos/controle com bounds honestos | CICS/IMS/DB2/MQ efeitos parciais, com provenance e remainders | execução normal não se presume de CALL/ABEND/READ; handlers têm edges próprios |
| L5 exact runtime behavior | não necessário para o produto proposto | valores concretos de condição, número exato de iterações, bytes runtime podem faltar | alta cobertura de skeleton não exige executar IMS nem prever dados reais |

“Control quase completo” significa cobrir os constructs efetivamente encontrados e as suas combinações, não provar todos os caminhos possíveis nem a terminação de programas arbitrários. GO TO dinâmico/ALTER, exceptions e special EXIT fora da capacidade precisam de representação explícita de desconhecimento; um gap de controle pode ampliar muito o resultado e precisa ser visível ao consumidor.

## Evidência a favor e contra H3

Contra necessidade de semântica integral: quatro produtos canônicos e cinco cenários adicionais de profile chegam a dependency com gaps; o runtime anterior levava nove/dez da amostra até o fim mesmo sem PERFORM tipado. Isso demonstra **representabilidade e viabilidade parcial**, não prova automaticamente soundness de cada resultado antigo. F2/F3 podem ser explicados sem conhecer o resultado de ADD, STRING, SET ou DLI: a fonte e a regra de término de região bastam.

A favor de limites à partiality: alias desconhecido pode afetar qualquer target compartilhado; controle unknown pode tornar indecidível um negativo de reachability; effects unknown podem invalidar candidates baseados em valor anterior. Portanto não se pode prometer extração exaustiva ou negatives precisos sem os fatos mínimos relevantes. O Gate B precisa distinguir may-candidate, suporte no modelo e remainder da fonte.

Não há prova em D0 de que todo programa empresarial possa ser analisado com alta precisão sem novas capacidades. Há evidência suficiente para refutar a tese de que implementar todos os valores/effects seja pré-requisito para resolver os 47 primeiros bloqueios de controle.

## Control graph antes da análise de valores?

**YES, como autoridade estrutural parcial tipada.** Construir a topologia fonte antes de escolher precisão dos valores evita que ausência de DFHAID apague END-EVALUATE e evita que um FILE handler seja invisível ao inventário. Depois refinar predicates/values sem mudar silenciosamente a existência de boundaries. Custo: contrato SP versionado, frontend com uma disciplina de regiões, validação cruzada e migração coordenada. Não é exportar um CFG fonte monolítico com todos os contextos ou um interpretador completo; contexto continua uma dimensão do consumidor de topologia.
