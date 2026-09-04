# Avaliação — WORK-HARNESS-IMPACT-001

## O que prova corretude

Esta avaliação prova que o harness possui uma taxonomia fechada e legível, que
um registro documental exige uma classe primária e conteúdo mínimo real, que o
lifecycle ativo não contém roteamento stale e que os exemplos orientam decisões
conservadoras. Não prova a existência nem a correção de Semantic Product, Cobol
Lower, IR, CFG, dataflow ou Dependency Facts.

O oracle executável é `HarnessDocsTest`: ele verifica a existência da fonte
canônica, o conjunto exato de oito classes, as guardas normativas, o conteúdo
dos campos do bloco YAML simples, os casos vazios e a coerência local entre
active/index/history. O teste não classifica findings automaticamente, não
infere merge de todos os work items e não varre o backlog para preencher
registros ausentes.

## Classes positivas

- `BLOCKS_SEMANTIC_PRODUCT`: frontend comprovadamente não entrega informação
  exigida por um contrato de Semantic Product já definido.
- `BLOCKS_IR`: Semantic Product correto e IR/lowering comprovadamente incapaz de
  representar a semântica necessária.
- `BLOCKS_CFG`: Semantic Product e IR corretos, com arestas CFG incorretas sob
  contrato de CFG definido.
- `BLOCKS_DATAFLOW`: produtos anteriores corretos, mas storage identity,
  aliases, effects ou program points insuficientes para o contrato de dataflow.
- `BLOCKS_DEPENDENCY_FACTS`: os produtos anteriores realmente necessários e o
  contrato do fato estão corretos, mas a transformação/composição publica um
  fato final incorreto, perde informação de modo unsound ou não consegue
  produzir o fato.
- `REDUCES_PRECISION`: output sound/conservador confirmado, com perda de
  informação útil sem afirmação falsa.
- `NOT_APPLICABLE`: finding exclusivamente documental/licença/organização sem
  consumidor semântico afetado, demonstrado positivamente.
- `UNASSESSED`: finding real com fronteira downstream ainda não determinável,
  com rationale, evidência e gatilho de reassessment.

## Classes negativas

- classe inventada ou grafia diferente do vocabulário canônico;
- duas classes primárias para um mesmo finding;
- `BLOCKS_CFG` escolhido apenas porque o construct parece controle de fluxo;
- `BLOCKS_SEMANTIC_PRODUCT` usado como default para qualquer bug do frontend;
- `REDUCES_PRECISION` usado quando há resultado unsound ou fato falso;
- `NOT_APPLICABLE` usado para esconder falta de investigação;
- prioridade/severity derivada automaticamente da classe de impacto;
- classe de impacto usada como autorização de implementação.

## Classes ambíguas

F-01 de `BACKLOG-RES-003` é o caso representativo: a incorreção na cadeia
occurrence/resolution é confirmada, mas a primeira fronteira downstream não pode
ser identificada antes dos contratos de Semantic Product e IR. Seu impacto é
`UNASSESSED`; `CONFIRMED_KNOWN_BUG` continua sendo a classificação do finding,
não do impacto.

Um problema de provenance pode ser `NOT_APPLICABLE` apenas se a evidência
mostrar que nenhum produto semântico depende dela. Se a provenance é necessária
para identidade do produto, a classe precisa ser investigada separadamente.

## Casos adversariais

1. **Propagação:** Semantic Product errado e CFG também errado →
   `BLOCKS_SEMANTIC_PRODUCT`, não duas classes.
2. **Precisão conservadora:** `{PGM1, PGM2, UNKNOWN}` reduzido a
   `{UNKNOWN}` → `REDUCES_PRECISION` somente se o resultado continua sound.
3. **Dependency Fact incorreto:** com produtos anteriores corretos, target
   esperado `PGM-A` e fato publicado `PGM-B` → `BLOCKS_DEPENDENCY_FACTS`.
4. **Perda unsound:** o conjunto completo `{PGM1, PGM2}` é publicado como
   `{PGM1}`. Se `PGM2` ainda está nos produtos anteriores e a perda ocorre na
   produção do fato, → `BLOCKS_DEPENDENCY_FACTS`; se a perda ocorre antes,
   aplica-se a classe da primeira fronteira quebrada.
5. **Extractor parcial:** um extractor literal que consome somente Semantic
   Product pode ser `BLOCKS_DEPENDENCY_FACTS`; não se exige evidência de
   dataflow que ele não utiliza.
6. **Sem evidência:** “provavelmente quebra CFG” → `UNASSESSED`.
7. **Prioridade:** `BLOCKS_DATAFLOW` fora do roadmap não recebe prioridade
   implícita; `REDUCES_PRECISION` não recebe prioridade baixa automaticamente.
8. **Múltiplas consequências:** IR, CFG e dataflow afetados, primeira falha na
   IR → `BLOCKS_IR`.
9. **Fora da arquitetura:** licença sem consumidor semântico →
   `NOT_APPLICABLE`; desconhecimento sobre impacto → `UNASSESSED`.

## Casos de regressão

- O bloco F-01 em `docs/work/backlog.md` e o exemplo correspondente na fonte
  canônica devem permanecer `UNASSESSED` e conter evidence/reassessment; a
  classificação `CONFIRMED_KNOWN_BUG` do finding não muda.
- O validator deve aceitar as oito classes canônicas e rejeitar um valor fora
  delas em qualquer bloco documental `downstream_impact` no formato definido.
- O validator deve distinguir os dois registros válidos (classe determinada
  sem reassessment e `UNASSESSED` com reassessment) dos quatro contracasos:
  rationale/evidence vazios, `UNASSESSED` sem reassessment e classe desconhecida.
- O índice deve listar exatamente os diretórios em `docs/work/active`, e nenhum
  diretório ativo pode ter resumo histórico equivalente.
- O work item deve continuar sem arquivos em `src/main/**`.

## Propriedades/relações metamórficas

- Reordenar as linhas da tabela de exemplos não altera o conjunto canônico
  validado.
- Adicionar uma consequência ao rationale não cria uma segunda classe.
- Trocar “provavelmente quebra CFG” por evidência concreta de contrato CFG e
  arestas incorretas pode permitir reclassificação posterior, mas não muda
  `UNASSESSED` sem essa evidência.
- Alterar prioridade/severity de um finding não altera sua classe de impacto.
- Reavaliar F-01 deve acrescentar história, não apagar a justificativa inicial.

## Expectativas de escala

A validação percorre a fonte canônica e os documentos Markdown já considerados
pelo `HarnessDocsTest`; não analisa o corpus COBOL nem faz busca quadrática por
findings. O custo e o formato permanecem adequados ao harness documental atual.
Não há threshold de performance novo e `check-performance.sh` não é aplicável.

## Self-validation do harness

O `HarnessDocsTest` transforma em checks executáveis as partes locais e
estruturais da challenge:

- **A/G — lifecycle e stale routing:** enumera `active/`, compara os IDs com os
  links ativos do índice, compara os resumos históricos com os links de
  `history/` e rejeita sobreposição active/history; não infere merge remoto de
  itens sem metadata confiável.
- **B — completude de fronteiras:** exige no documento canônico o mapeamento de
  Semantic Product, IR, CFG, Dataflow/Possible Values e Dependency Facts, além
  de `REDUCES_PRECISION` e `UNASSESSED`.
- **C — soundness/precision:** a documentação e os casos adversariais separam
  output correto, `{UNKNOWN}` conservador e fato/target incorreto, incluindo a
  fronteira Dependency Facts.
- **D/E — usabilidade e campos vazios:** o validator exige somente
  `class`/`rationale`/`evidence` globalmente, exige `reassess_when` apenas para
  `UNASSESSED`, e executa contracasos de campos vazios.
- **F — integridade de escopo:** o review do diff classifica cada arquivo e
  confirma que nenhuma superfície de produção aparece no escopo permitido.
- **H — challenge pass:** a política e o teste verificam que não há classe
  downstream sem representação, nem mistura de impacto, prioridade,
  soundness, desconhecimento ou autorização de implementação.
