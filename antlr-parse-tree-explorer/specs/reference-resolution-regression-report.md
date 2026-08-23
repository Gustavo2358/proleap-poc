# Relatório de regressão — Resolução de referências

## Estado

Em construção. Iniciado na Fase 0 de
`reference-resolution-tasklist.md` e destinado a ser encerrado na Fase 8.

## Baseline pós-hardening

- commit inicial: `c08e3f269ea2571afd17c44be0d95f26a82feb97`;
- testes Maven antes desta etapa: 23, todos verdes;
- Java de produção, SHA-256 agregado: `af7b168665151b1d8fc5d8ad19b431a33ccbe4871fce7227e949f3af4784a833`;
- gramáticas, SHA-256 agregado: `4be036fc47a73e32dd68de6a3ab0008ef81895f52372046f6a35e986e92b202e`;
- corpus interno, SHA-256 agregado: `f89cf02782dc26f3a4a79e37fc98c35112f0003d36423e674785251152b4b10e`;
- outputs versionados, SHA-256 agregado: `04fb0d56bb4ba9a7c4c6b430253bba8b6b1526b36d85a00b76a28e2774dd742b`;
- tasklist aprovada, SHA-256:
  `cd2c3c978c54c648b898ec2330e5ecdf68aa85c7c4128dda0914d1bcbbd0ce9c`.

Os agregados foram calculados ordenando os caminhos completos relativos à raiz
do repositório, aplicando SHA-256 a cada arquivo e depois ao conjunto ordenado.

Fontes principais:

| Fonte | SHA-256 |
|---|---|
| `../cbl/CBSTM03A.CBL` | `23c8753b6b4e0c24d4560c83861fe8162626bab195faec0fe88cf80b8bf432b5` |
| `corpus/cbl/CBSTM03D.CBL` | `d75535258cb80c8777993b6662146ed7f2f8cc5888a34d648b5ea68c310a7fac` |
| `corpus/cbl/COACTUPC.cbl` | `b5bb7d6ccad022e0fc91b4dd1e971f49d184adf89b56abdce14eccff35b39396` |

## Métricas semânticas iniciais

| Programa | Parse tree | AST | Profundidade AST | CALL estático | CALL dinâmico | Escopos | Símbolos | Diagnósticos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| COACTUPC | 57.227 | 9.189 | 11 | 1 | 0 | 651 | 853 | 2 |
| CBSTM03A | 11.795 | 2.740 | 11 | 14 | 0 | 219 | 209 | 0 |
| CBSTM03D | 11.916 | 2.752 | 11 | 0 | 14 | 221 | 211 | 0 |

Todos têm zero erros léxicos/sintáticos. COACTUPC mantém três COPYs ausentes e
14 CICS opacos. CBSTM03D mantém 14 targets `WS-CALL-TARGET` e dois MOVEs de
literal para essa variável.

## Caracterização anterior à resolução

- a gramática aceita múltiplos e nested `programUnit`, mas `AstBuilder` retorna
  somente o primeiro encontrado;
- SELECT e FD/SD homônimos são hoje dois símbolos FILE independentes;
- DATA, PROCEDURE, FILE, PROGRAM e INDEX já possuem referências estruturadas;
- AST e Symbol Table não contêm resultado de binding;
- nenhuma referência possui `symbolId`, candidatos ou status de resolução.

Esses fatos são protegidos por
`ReferenceResolutionBaselineCharacterizationTest`; mudanças futuras deverão ser
intencionais, TDD-first e explicadas aqui.

## Resultados por fase

| Fase | Estado | Evidência | Diferenças esperadas | Pendências |
|---|---|---|---|---|
| 0 — baseline | aprovada | 26 testes verdes; três caracterizações novas | nenhuma mudança de produção | nenhuma |
| 1 — matriz/contratos | aprovada | 28 testes e todos os JS verdes; guarda das 628 regras | manifesto/policy/contratos imutáveis, sem binding | regras conservadoras serão refinadas por TDD nas fases de resolução |
| 2 — compilation unit | pendente | — | todos os programUnit e visibilidade | — |
| 3 — entidades/coleta | pendente | — | entidades, scope index e occurrences | — |
| 4 — DATA/INDEX | pendente | — | resolução local estruturada | — |
| 5 — PROCEDURE/FILE/PROGRAM | pendente | — | demais namespaces e catálogo plugável | — |
| 6 — cobertura/escala | pendente | — | completude e métricas | — |
| 7 — HTML | pendente | — | nova jornada visual | — |
| 8 — regressão final | pendente | — | — | — |

## Evidência TDD

### Fase 0

Testes de caracterização registram comportamento existente; por definição não
há RED funcional nesta fase. Eles serão os oráculos que ficarão RED quando as
mudanças intencionais das Fases 2 e 3 começarem.

A suíte completa terminou com 26 testes, zero falhas, zero erros e zero testes
ignorados. Nenhum arquivo de produção, grammar, corpus ou output foi alterado.

### Fase 1

1. RED: `ReferenceResolutionManifestTest` falhou na compilação porque
   `ResolutionContracts` e `ReferenceResolutionManifest` ainda não existiam.
2. GREEN: os contratos passaram a expor UnitId namespaced, kinds, roles, policy
   versionada, `QUALIFY` explícito, completude conservadora, os quatro status e
   reasons estáveis, sem implementar resolver ou candidatos.
3. GREEN: o manifesto de resolução passou a expandir deterministicamente as
   mesmas 628 chaves do manifesto das grammars. Origens DATA/CONDITION/INDEX,
   PROCEDURE/FILE/PROGRAM, qualifiers, relações e built-ins usam overrides
   exatos; as demais regras herdam somente classificação conservadora.
4. REFACTOR: lookup do manifesto passou a usar índice imutável O(1), e uma
   guarda falha se um override deixar de existir após mudança da grammar.
5. GUARD: a suíte completa terminou com 28 testes verdes e todos os JavaScripts
   passaram em `node --check`. AST, Symbol Table, corpus, grammars e outputs não
   foram modificados; nenhuma referência foi ligada a símbolo nesta fase.

## Checklist final de regressão

- [ ] suíte Maven completa;
- [ ] sintaxe de todos os JavaScripts;
- [ ] matriz das 628 regras e 50 statements;
- [ ] três programas sem novos erros;
- [ ] fatos CALL/MOVE de CBSTM03D;
- [ ] fatos essenciais da Symbol Table;
- [ ] fixtures gramaticais e semânticas;
- [ ] determinismo de duas gerações;
- [ ] navegação HTML;
- [ ] fixture de escala;
- [ ] hashes e fronteiras de escopo.
