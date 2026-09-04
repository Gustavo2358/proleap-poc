# Semantic Product Boundary Discovery

## Problema

O frontend COBOL já produz vários artefatos semânticos separados, mas não existe ainda uma decisão sobre o estado semântico que deverá atravessar a futura fronteira para lowering. O nome `Cobol Semantic Product` é apenas uma hipótese de trabalho. A investigação precisa partir dos produtores e consumidores reais, preservando a distinção entre domínio, observabilidade e transporte.

Este work item é um Discovery arquitetural de alto risco, composto por uma
baseline factual (Checkpoint 1) e pelo desenho/suficiência da boundary
(Checkpoint 2). O Checkpoint 1 está concluído; o Checkpoint 2 está concluído e
em review humano; o Checkpoint 3A foi autorizado para falsificação test-only.
A implementação de produção do produto, de `Cobol Lower`, da IR, de CFG, de
dataflow, de possible-values e de dependency facts está fora do escopo.

## Objetivo

Produzir, ao longo do lifecycle completo de três checkpoints de Discovery,
evidência suficiente para que um review humano decida se a menor boundary
semântica COBOL-specific é suficiente para um futuro `CobolLower`. O Checkpoint 1
estabelece a baseline factual do frontend; o Checkpoint 2 define a boundary e
testa sua suficiência; e o Checkpoint 3A executa a falsificação executável da
seam aprovada, sem prova de interchange. No estado atual, somente o Checkpoint
3A está autorizado. Nenhum checkpoint desenha a IR ou implementa o
produto/lowerer.

## Domínio de entrada suportado

O domínio observado é a análise de uma compilation unit COBOL, incluindo unidades de programa aninhadas, formato de fonte normalizado, preprocessing/COPY, AST semântica, tabelas de símbolos, occurrences, resolução nominal e a classificação externa CICS focalizada que já existe. Fixtures de COPY ausente, condições, CALLs, procedures, nested programs e statements de controle constituem evidência do estado atual; não ampliam o dialeto suportado.

## Classes semânticas

O Discovery distingue fatos estruturados do frontend, referências nominais e seus resultados, relações declarativas, cobertura/completude, diagnostics, provenance e classificação externa. Não trata binding nominal como valor de runtime, e não promove snapshots ou strings de apresentação a classes de domínio.

## Premissas

- Corpus, fixtures e testes são evidência de comportamento, não especificação isolada.
- Os produtos AST, símbolos, occurrences e resolução permanecem conceitualmente separados e imutáveis conforme as autoridades vigentes.
- `ExternalClassification` é um produto pós-binding atual; `ConditionSemantics` e `ConditionValidation` são somente produtos futuros documentados.
- A neutralidade entre linguagens, se necessária, começa no futuro consumidor downstream; este item não cria um produto universal.
- A futura boundary deve preservar incerteza, incompletude e provenance observáveis.

## Comportamento esperado

O relatório do Checkpoint 1 deve descrever o que o frontend realmente
estabelece ao terminar sua análise, como os produtos fazem joins e quais fatos
um futuro lowerer precisará receber para os slices R1–R7. O relatório do
Checkpoint 2 deve definir e comparar candidatas de boundary, incluindo a forma
mais forte plausível de um modelo materializado próprio e de uma facade
query-oriented, e testar a suficiência do contrato sem expor internals. Cada
conclusão arquitetural importante deve indicar `evidence_status` entre
`PROVEN`, `STRONGLY_SUPPORTED`, `PLAUSIBLE`, `UNKNOWN` e `REFUTED`.

O plano contém três checkpoints independentes, cada um terminando em review
humano. O Checkpoint 1 foi concluído e serviu de baseline factual para o
Checkpoint 2; o Checkpoint 2 foi concluído e aguarda review. O Checkpoint 3A
tenta falsificar a candidata escolhida em código test-only e termina em review
humano. Implementação de produção será outro work item.

## Comportamento diante de incerteza

O relatório deve usar os contratos atuais: cobertura `MODELED`, `PRESERVED_UNINTERPRETED`, `UNSUPPORTED` e `INPUT_MISSING`; conhecimento de dependência `REFERENCE_READY`, `DEPENDENCY_UNKNOWN` e `NOT_DEPENDENCY_BEARING`; statuses de resolução; diagnostics; gaps e claim `COMPLETE`/`INCOMPLETE`. Ausência de um contrato downstream deve permanecer `UNASSESSED` na taxonomia de impacto do harness, com evidência e condição de reavaliação.

## Fora de escopo

- Criar ou implementar `CobolSemanticProduct`.
- Implementar ou promover a decisão de record, facade, envelope, modelo
  materializado, serializer, JSON, bundle ou schema a contrato aceito.
- Implementar `Cobol Lower`, IR, CFG, dataflow, reaching definitions, possible-values ou dependency extraction.
- Corrigir F-01, ALTER, SEARCH ou qualquer lacuna oportunista de AST/resolver.
- Alterar AST para carregar resolução, introduzir IDs de candidates na AST, refatorar `ExplorerMain`, criar parser embedded ou framework de plugins.
- Executar qualquer parte do Checkpoint 3 além do slice 3A autorizado neste estado do work item.

## Regras de domínio relacionadas

- A AST é uma superfície semântica separada, derivada de contextos tipados, sem binding, tabelas de símbolos, CFG ou dataflow.
- Compilation units, scopes, symbols, entities, occurrences e relações têm namespaces e lifetimes próprios; joins usam identidades compostas.
- Resolução nominal preserva ambiguidade, unresolved e external literal observado; não resolve valores de runtime.
- Provenance começa na fonte física e acompanha normalização, preprocessing e COPY, incluindo include chain.
- Incompletude e coverage são observáveis e não podem ser substituídas por coleções vazias aparentemente completas.
- Classificação externa é ortogonal ao binding COBOL e só é aplicada depois de uma falha nominal elegível.

## ADRs/invariantes relacionados

ADR-0002, ADR-0003, ADR-0004, ADR-0005, ADR-0008, ADR-0009, ADR-0010, ADR-0011 e ADR-0012; INV-AST-001/002/003, INV-SYM-001, INV-COND-001/002, INV-PROV-001/002, INV-RES-001/002/003, INV-EXT-001/002/003/004, INV-COV-001/002/003, INV-EMB-001 e INV-DET-001.
