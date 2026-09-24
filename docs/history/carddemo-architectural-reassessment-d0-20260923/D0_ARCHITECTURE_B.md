# Arquitetura B — control.local@1

**PARTIALLY_SUPPORTED no contrato AIR; UNSUPPORTED no consumer CFG atual. Não recomendar adoção imediata.** Pode reduzir representação estática e clonagem, mas matching de contexto transfere custo ao solver.

Os quatro witnesses comparados em A/B/C são os mesmos: (W1) CBACT01C PERFORM89/EXIT158; (W2) COACTUPC PERFORM858 THRU/SET1284/EXIT1312; (W3) CBEXPORT PERFORM94→1000 com PERFORM103 aninhado→1050/STRING111; (W4) CBPAUP0C EVALUATE58/SET62→boundary2000. São modelos hipotéticos, sem execução/fix de produto. Edges de linguagem estão em D0_F2_DEEP_DIVE e D0_F3_COMPARISON.

## Contrato existente, sem extensão inventada

Fonte normativa local: `analysis-ir/especificacao/05-controle-e-invocacoes.md`, seção control.local; compromisso do consumer: `analysis-cfg/docs/domain/local-control.md`. `local.invoke` empilha entry/ports/resume sem bypass e compartilha memória; `local.boundary(port,default)` só casa com o frame do topo, desempilha se casar e segue default sem alterar stack caso contrário. `local.resume` exige frame ativo; `local.unwind(n,dest)` remove exatamente n; `jump` não desempilha. Não procurar um frame antigo por baixo do topo.

Pseudocódigo de operações, não AIR JSON executável:

```text
W1 CBACT01C:
  s89: local.invoke(entry=s40, ports={end_P0000}, resume=s90)
  s158: no-op ; jump boundary_P0000
  boundary_P0000: local.boundary(end_P0000, default=s44)
  stack vazia: s158 → s44
  topo=s89: s158 → s90, pop

W2 COACTUPC:
  s858: local.invoke(entry=s1283, ports={end_PYYYY_EXIT}, resume=s859)
  arm/SET1284: jump boundary_PYYYY
  boundary_PYYYY: local.boundary(end_PYYYY, default=s1312)
  s1312: no-op ; jump boundary_PYYYY_EXIT
  boundary_PYYYY_EXIT: local.boundary(end_PYYYY_EXIT, default=s819)
  s858 ativo: primeira boundary não casa; segunda casa e retorna
  eventual PERFORM só PYYYY: primeira boundary casa
  stack vazia: ambas usam default

W3 CBEXPORT, nested:
  s94: local.invoke(entry=s102, ports={end_P1000}, resume=s95)
  s103: local.invoke(entry=s107, ports={end_P1050}, resume=s104)
  STRING111 → local.boundary(end_P1050, default=s112)
  DISPLAY106 → local.boundary(end_P1000, default=s107)
  stack [s94,s103] → [s94] → []; values/storage permanecem compartilhados

W4 CBPAUP0C:
  EVAL58 → arm(GB) → SET62 → complete_EVAL58 → boundary_P2000
  boundary_P2000: local.boundary(end_P2000, default=s66)
  PERFORM cujo último parágrafo=P2000 registra essa porta;
  THRU até P2000_EXIT registra a porta do EXIT, alcançada depois de s66.
```

Entries numéricos foram conferidos no SP dos witnesses; os identificadores de boundary são novos nomes conceituais. Para F3 B ainda precisa corrigir a publicação/composição de EVALUATE. Introduzir stack sozinho não recupera arms descartados nem resolve o contrato contraditório.

Múltiplos callers usam o mesmo body e distintos resumes no frame. Repetition pode ser wrapper de teste/incremento que invoca o body e recebe completion por iteração; condições unknown mantêm ambos os outcomes admissíveis. `local.resume` só serve a uma saída incondicional do frame comprovada; nunca mapear plain EXIT para resume, pois a entrada ordinária precisa continuar.

## GO TO, nesting e recursão

GO TO para fora do intervalo textual não autoriza `unwind(1)` automaticamente: o contrato jump conserva stack e o frontend precisa provar a disciplina COBOL da transferência, inclusive possível retorno ao range. Transfers de programa/ABEND não equivalem a completion normal. Special EXIT exige regra própria; EXIT PARAGRAPH/SECTION não é unwind genérico. Ranges ativos sobrepostos/cruzados precisam obedecer às restrições da linguagem e não mudar a semântica top-only para “fazer funcionar”. AIR permitir uma pilha recursiva não prova que todas as formas de PERFORM recursivo sejam válidas/suportadas.

## Custo e decisão

Body estático tende a O(S+E+callsites+boundaries), com compartilhamento, ao contrário da soma por contexto de A. A análise ainda deve resolver caminhos realizáveis: um grafo que liga cada boundary a todos os resumes cria cross-return e contamina targets. CallsiteId no edge sem matching nas consultas é insuficiente. São candidatos a estudar pushdown/summaries/context states; nenhum foi implementado ou medido aqui. Aliases/effects compartilhados e widening de valores podem aumentar complexidade mesmo quando o grafo estático é pequeno.

O CFG atual declara explicitamente control.local não suportado (BACKLOG-CFG-013); portanto não afirmar que B reduz complexidade **total** nem que melhora dataflow. Migração envolveria frontend, lower, air-java (modelo LocalInvoke/LocalBoundary, checks e binding reader já existem; confirmar qualificação integrada), analysis-cfg e alinhamento normativo analysis-ir. O contrato já existir não certifica todos os componentes.

B tem mérito para experimento de backend depois de estabilizar C: os quatro witnesses, shared live DAG, negative cross-return, ordinary entry e GO TO escape devem passar, com custo medido. Até lá, critérios B de ausência de piora CFG/dataflow e redução total de complexidade são **UNKNOWN**, impeditivos de recomendação imediata.
