# Avaliação — WORK-HARNESS-IMPACT-001

## O que prova corretude

Esta avaliação prova que o harness possui uma taxonomia fechada e legível, que
um registro documental exige uma classe primária e justificativa mínima, e que
os exemplos orientam decisões conservadoras. Não prova a existência nem a
correção de Semantic Product, Cobol Lower, IR, CFG ou dataflow.

O oracle executável é `HarnessDocsTest`: ele verifica a existência da fonte
canônica, o conjunto exato de sete classes, as guardas normativas e os campos
do bloco YAML simples. O teste não classifica findings automaticamente e não
varre o backlog para preencher registros ausentes.

## Classes positivas

- `BLOCKS_SEMANTIC_PRODUCT`: frontend comprovadamente não entrega informação
  exigida por um contrato de Semantic Product já definido.
- `BLOCKS_IR`: Semantic Product correto e IR/lowering comprovadamente incapaz de
  representar a semântica necessária.
- `BLOCKS_CFG`: Semantic Product e IR corretos, com arestas CFG incorretas sob
  contrato de CFG definido.
- `BLOCKS_DATAFLOW`: produtos anteriores corretos, mas storage identity,
  aliases, effects ou program points insuficientes para o contrato de dataflow.
- `REDUCES_PRECISION`: output sound/conservador confirmado, com perda de
  informação útil sem afirmação falsa.
- `NOT_APPLICABLE`: finding exclusivamente documental/licença/organização sem
  consumidor semântico afetado, demonstrado positivamente.
- `UNASSESSED`: finding real com fronteira downstream ainda não determinável,
  com rationale, evidência e gatilhos de reassessment.

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
2. **Precisão versus soundness:** `{PGM1, PGM2, UNKNOWN}` reduzido a
   `{UNKNOWN}` pode ser `REDUCES_PRECISION`; excluir target possível e declarar
   conjunto completo exige classificar a primeira incorreção.
3. **Sem evidência:** “provavelmente quebra CFG” → `UNASSESSED`.
4. **Prioridade:** `BLOCKS_DATAFLOW` fora do roadmap não recebe prioridade
   implícita; `REDUCES_PRECISION` não recebe prioridade baixa automaticamente.
5. **Múltiplas consequências:** IR, CFG e dataflow afetados, primeira falha na
   IR → `BLOCKS_IR`.
6. **Fora da arquitetura:** licença sem consumidor semântico →
   `NOT_APPLICABLE`; desconhecimento sobre impacto → `UNASSESSED`.

## Casos de regressão

- O bloco F-01 em `docs/work/backlog.md` e o exemplo correspondente na fonte
  canônica devem permanecer `UNASSESSED` e conter evidence/reassessment; a
  classificação `CONFIRMED_KNOWN_BUG` do finding não muda.
- O validator deve aceitar as sete classes canônicas e rejeitar um valor fora
  delas em qualquer bloco documental `downstream_impact` no formato definido.
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
