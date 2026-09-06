# WORK-SEMANTIC-PRODUCT-004 — Entry primária e GOBACK

## Problema

A boundary preserva GOBACK genérico e anchors estruturais, mas não uma entrada
executável. F-AIR-04/05 e AIR-AUDIT-07 identificam esse prerequisite local.

## Objetivo

Promover somente Entry primária/início de BACKLOG-SP-005, coordenado somente
com a saída local GOBACK de BACKLOG-SP-003. O JSON deve permitir observar
AIR-FIRST → entry primária → StatementId GOBACK → nenhuma continuação local.

## Domínio de entrada suportado

ProgramUnits COBOL com PROCEDURE DIVISION e GOBACK reconhecido pelo contexto
tipado. O início usa a relação canônica do corpo não declarativo; quando a
primeira construção não tiver representação executável comprovada, permanece
indisponível. Assinatura sem cláusulas é conhecida no escopo da interface
escrita; cláusulas presentes ou fonte incompleta conservam disponibilidade.

## Classes semânticas

Entry primária disponível/indisponível; início conhecido/desconhecido;
assinatura conhecida sem cláusulas/parcial/indisponível; GOBACK tipado;
demais terminais observados; inventário de entries limitado à primária.

## Premissas

- LANGUAGE_GUARANTEED: GOBACK sai da invocação corrente, sem próximo statement
  local; retorno ao chamador/ambiente depende do contexto de execução IBM 6.4.
- ARCHITECTURE_GUARANTEED: IDs/ownership e estrutura vêm do frontend canônico;
  projector e JSON apenas traduzem fatos. ProgramPoint não decide entrada.
- SPECIFICATION_GUARANTEED: este slice não publica efeitos de lifecycle,
  storage, valores de retorno nem classificação main/subprogram por filename.
- UNCERTAIN: contexto runtime, assinaturas fora da capacidade e inventário
  completo de ENTRY alternativo; não transformados em ausência conhecida.

## Comportamento esperado

Entries e GOBACK conservam identidade, provenance, coverage e readiness.
Start conhecido referencia statement publicado da mesma unit; identidade
pendente é rejeitada. GOBACK substitui sua observação genérica, sem duplicação;
statements físicos posteriores continuam no inventário, sem fallthrough.
JSON 1.1.0 adiciona surface versionada, sem mudar o significado dos campos
existentes; variantes são a extensão aditiva prevista pelo contrato.

## Comportamento diante de incerteza

Signature desconhecida não vira lista vazia nem contagem zero. Entry/início
ausentes e superfície parcial têm gaps e readiness rebaixada. Inventário de
entries não alega completude além da entry primária. EOF não significa retorno.

## Fora de escopo

AIR/air-java/CobolLower/CFG; sequência universal, reachability/basic blocks;
IF/MOVE/CALL/GO TO/PERFORM/ALTER/SEARCH; demais terminais precisos; storage,
effects, RD, Possible Values, dependências; assinatura completa/ENTRY alternativo.

## Regras de domínio relacionadas

[Contrato do Semantic Product](../../../domain/cobol-semantic-product.md),
[AST](../../../domain/semantic-ast.md), [units](../../../domain/compilation-units.md).
Autoridade: IBM Enterprise COBOL for z/OS 6.4, Language Reference, GOBACK,
CALL, PROCEDURE DIVISION e transferência de controle.

## ADRs/invariantes relacionados

ADR-0013, ADR-0005; INV-SP-001 a INV-SP-007 e INV-AST-003.
