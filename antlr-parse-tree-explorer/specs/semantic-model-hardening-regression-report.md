# Relatório de regressão — Semantic Model Hardening

## Estado

Em construção. Este documento foi iniciado na Fase 0 e será fechado somente na
Fase 8 da tasklist `semantic-model-hardening-tasklist.md`.

## Baseline registrado

- commit inicial: `ffa053f08884fa6c4913bf493cc8d4829272ca01`;
- Java de produção, agregado SHA-256: `222dd3f511b0cbe8fe4f591b94052775d1dee42f015cb2a00e173ca15f9442e0`;
- gramáticas, agregado SHA-256: `4be036fc47a73e32dd68de6a3ab0008ef81895f52372046f6a35e986e92b202e`;
- corpus interno, agregado SHA-256: `f89cf02782dc26f3a4a79e37fc98c35112f0003d36423e674785251152b4b10e`;
- outputs versionados, agregado SHA-256: `4a61cb0c2fe06f51f49c9cf4769131657ca9ba3fd7796e24f22cd1048c91bd80`;
- plano de resolução suspenso SHA-256:
  `eaf75ef6344812fb585de3ecffbf927df052563b1e8e5f5b8ae296e8034f2674`.

Fontes COBOL principais:

| Fonte | SHA-256 |
|---|---|
| `../cbl/CBSTM03A.CBL` | `23c8753b6b4e0c24d4560c83861fe8162626bab195faec0fe88cf80b8bf432b5` |
| `corpus/cbl/CBSTM03D.CBL` | `d75535258cb80c8777993b6662146ed7f2f8cc5888a34d648b5ea68c310a7fac` |
| `corpus/cbl/COACTUPC.cbl` | `b5bb7d6ccad022e0fc91b4dd1e971f49d184adf89b56abdce14eccff35b39396` |

## Métricas semânticas atuais

| Programa | AST | Profundidade | CALL estático | CALL dinâmico | Embedded | Unsupported | Escopos | Símbolos | Diagnósticos da tabela |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| COACTUPC | 4.100 | 8 | 1 | 0 | 14 | 411 | 651 | 853 | 2 |
| CBSTM03A | 1.250 | 8 | 14 | 0 | 0 | 268 | 219 | 209 | 0 |
| CBSTM03D | 1.260 | 8 | 0 | 14 | 0 | 268 | 221 | 211 | 0 |

Frontend atual:

- os três programas têm zero erros sintáticos;
- `COACTUPC` tem três COPYs ausentes;
- `CBSTM03A` e `CBSTM03D` não têm COPYs ausentes;
- `CBSTM03D` tem 14 targets `DataReference("WS-CALL-TARGET")` e uma única
  declaração DATA correspondente;
- o baseline anterior achatava, entre outros exemplos,
  `ACCTSIDI OF CACTUPAI` em `ACCTSIDIOFCACTUPAI` e uma reference modification
  completa em `DFHCOMMAREA(1:LENGTHOFCARDDEMO-COMMAREA)`.

Esses fatos estão protegidos por `SemanticModelBaselineCharacterizationTest`.
Contagens poderão mudar ao introduzir novos nós semânticos; os fatos essenciais
deverão permanecer e toda diferença será explicada neste relatório.

## Resultados por fase

| Fase | Estado | Evidência | Diferenças esperadas | Pendências |
|---|---|---|---|---|
| 0 — baseline | aprovada | 5 testes Maven verdes; baseline e hashes registrados | nenhuma mudança de produção | nenhuma |
| 1 — cobertura | aprovada | 11 testes Maven verdes; guarda das 628 regras | `AstBuilder` retorna AST + cobertura + diagnósticos; nós AST inalterados | findings cobrem statements nesta fase |
| 2 — proveniência | aprovada | 13 testes Maven verdes; fixtures de COPY aninhado, REPLACING e COPY ausente | `Meta` e snapshot expõem arquivo/linha/include chain/exatidão; `writtenName` mantém separadores | contextos que cruzam fronteiras são marcados `exact=false`; COPY ausente continua diagnóstico explícito |
| 3 — referências/expressões | pendente | — | — | — |
| 4 — procedure references | pendente | — | — | — |
| 5 — declarações | pendente | — | — | — |
| 6 — statements | pendente | — | — | — |
| 7 — observabilidade | pendente | — | — | — |
| 8 — regressão final | pendente | — | — | — |

## Evidência TDD da Fase 1

1. RED: `GrammarCoverageManifestTest` e `SemanticCoverageTest` falharam na
   compilação porque manifesto, taxonomia e `AstBuildResult` ainda não existiam.
2. GREEN: os cinco testes desses contratos passaram após a implementação
   mínima e a inclusão das 628 linhas explícitas do manifesto.
3. RED: `AstBuildCoverageTest` falhou porque `AstBuilder.build` ainda retornava
   somente `Ast.Program`.
4. GREEN: o builder passou a retornar produtos separados e o teste confirmou
   findings determinísticos para `MOVE`, `SET` e `GOBACK`.
5. REFACTOR/GUARD: a suíte completa terminou com 11 testes, zero falhas e sem
   alteração das métricas da AST ou da Symbol Table.

## Evidência TDD da Fase 2

1. RED: `SourceProvenanceTest` falhou na compilação pela ausência de source map,
   proveniência em `Meta` e construtor do builder que a recebesse.
2. GREEN: o preprocessador passou a manter segmentos imutáveis com arquivo,
   offsets originais, cadeia de COPY e flag de exatidão; COPY aninhado,
   REPLACING e ausência passaram nos testes focados.
3. RED: a verificação do snapshot falhou na compilação porque a origem ainda
   não era observável na representação achatada.
4. GREEN: o snapshot passou a expor arquivo/linha, profundidade de include e
   exatidão sem regenerar os artefatos HTML da fase 7.
5. GUARD: a suíte completa terminou com 13 testes e zero falhas. As métricas
   semânticas e da tabela de símbolos permaneceram iguais; qualificadores e
   reference modification deixaram de perder espaços por `getText()`.

Lacunas de input continuam conservadoras: COPY ausente gera diagnóstico
`unresolved_copy`, texto sintético rastreável ao statement original e
`exact=false`. Um contexto gramatical que cruza segmentos/arquivos também não
é declarado como slice exato.

## Checklist final de regressão

- [ ] suíte Maven completa;
- [ ] sintaxe de todos os JavaScripts;
- [ ] três programas regenerados sem novos erros léxicos/sintáticos;
- [ ] fatos de CALL/MOVE de CBSTM03D preservados;
- [ ] fatos essenciais da Symbol Table preservados;
- [ ] mudanças de nós, IDs e profundidade explicadas e determinísticas;
- [ ] fixtures sintéticas completas;
- [ ] navegação HTML validada;
- [ ] completude semântica conservadora validada;
- [ ] fontes, baselines e plano suspenso byte a byte inalterados;
- [ ] ausência de resolver, CFG, dataflow, SQL e fatos finais confirmada.
