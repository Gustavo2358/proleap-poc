# CardDemo: reavaliação focal dos 6 blockers históricos pré-SP

Data: 2026-09-14 (America/Sao_Paulo). Escopo executado: exatamente os seis
sources solicitados, no CardDemo `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`.

## Conclusão

Os quatro blockers históricos `PREPROCESSOR_EXEC_POLICY_MISSING` estão
resolvidos pelo merge existente de EXEC DLI. Cada programa chegou a SP, AIR,
CFG (porque o wrapper atual o inclui) e `dependencies.json`, sempre com exit 0.
Não surgiu blocker downstream; os produtos continuam honestamente `PARTIAL`.

`COTRTLIC.cbl` e o `CBSTM03A.cbl` do ZIP continuam reproduzíveis. Ambos apontam
para uma única causa conceitual: tabs físicos não possuem uma política de avanço
de coluna no fixed-format. O primeiro é recusado explicitamente por ter tab na
área de programa; o segundo deixa um tab inicial atravessar a validação e passa
a ler `0` como o indicador da coluna 7 ao normalizar o copybook `CUSTREC`.

## SHAs executados

Todos os quatro checkouts originais foram atualizados com `fetch`, `switch main`
e `pull --ff-only`; depois da execução, `HEAD == main == origin/main` e as árvores
continuavam limpas.

| Repositório | SHA |
| --- | --- |
| proleap-poc | `a5a06ce6d5eb416b40cc35ce6b6b7e58ee8f72f5` |
| cobol-lower | `46828657c02f6c01cd4cea258b1f4cc1cab4352c` |
| analysis-cfg | `b5c98d01589c82b769abe1be83da0e9b5ed0a4e4` |
| air-java | `eaf83c6233d347348a3927b5983de03cde62554a` |
| CardDemo | `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e` |

SP executado: `2.15.0`; AIR: `2.0.0`; dependency result: binding atual do
`analysis-cfg`. Os builds isolados foram feitos a partir desses commits locais;
não são gates de qualificação.

## Resultado dos 6 programas

| Program | Historical blocker | Current result | Final stage | Classification |
| --- | --- | --- | --- | --- |
| CBPAUP0C | `PREPROCESSOR_EXEC_POLICY_MISSING` | DLI aceito; SP/AIR/CFG/dependency produzidos (`PARTIAL`) | dependency | `RESOLVED_BY_EXISTING_MERGE` |
| COPAUA0C | `PREPROCESSOR_EXEC_POLICY_MISSING` | DLI aceito; SP/AIR/CFG/dependency produzidos (`PARTIAL`) | dependency | `RESOLVED_BY_EXISTING_MERGE` |
| COPAUS0C | `PREPROCESSOR_EXEC_POLICY_MISSING` | DLI aceito; SP/AIR/CFG/dependency produzidos (`PARTIAL`) | dependency | `RESOLVED_BY_EXISTING_MERGE` |
| COPAUS1C | `PREPROCESSOR_EXEC_POLICY_MISSING` | DLI aceito; SP/AIR/CFG/dependency produzidos (`PARTIAL`) | dependency | `RESOLVED_BY_EXISTING_MERGE` |
| COTRTLIC | `FIXED_FORMAT_TAB` | exit 1; tab recusado antes do preprocessing/parser; sem SP | frontend/normalization | `STILL_REPRODUCIBLE` |
| CBSTM03A (ZIP) | `NORMALIZATION_REJECTED` | exit 1 ao normalizar `COPY CUSTREC`; sem SP | frontend/preprocessing | `STILL_REPRODUCIBLE` |

Não houve casos `NEW_DOWNSTREAM_BLOCKER` nem `NOT_REPRODUCIBLE`.

## Matriz por source

| Program | Frontend exit | SP? | Frontend blocker/reason | Unresolved COPYs | AIR? | Lower blocker/reason | dependencies.json? |
| --- | ---: | --- | --- | --- | --- | --- | --- |
| CBPAUP0C | 0 | sim, `PARTIAL` | nenhum blocker; gaps explícitos | nenhum | sim, `PARTIAL` | nenhum; exit 0 | sim, `PARTIAL` |
| COPAUA0C | 0 | sim, `PARTIAL` | nenhum blocker; input incompleto explícito | `CMQODV` ×2, `CMQMDV` ×2, `CMQV`, `CMQTML`, `CMQPMOV`, `CMQGMOV` | sim, `PARTIAL` | nenhum; exit 0 | sim, `PARTIAL` |
| COPAUS0C | 0 | sim, `PARTIAL` | nenhum blocker; input incompleto explícito | `DFHAID`, `DFHBMSCA` | sim, `PARTIAL` | nenhum; exit 0 | sim, `PARTIAL` |
| COPAUS1C | 0 | sim, `PARTIAL` | nenhum blocker; input incompleto explícito | `DFHAID`, `DFHBMSCA` | sim, `PARTIAL` | nenhum; exit 0 | sim, `PARTIAL` |
| COTRTLIC | 1 | não | `NORMALIZATION_GAP/FIXED_FORMAT_TAB`; `IllegalArgumentException` | não alcançado | não | não alcançado | não |
| CBSTM03A (ZIP) | 1 | não | `NORMALIZATION_GAP/NORMALIZATION_REJECTED`; `IllegalArgumentException` durante preprocessing | expansão interrompida em `CUSTREC` | não | não alcançado | não |

## EXEC DLI e dependency

As contagens abaixo são de statements SP `OBSERVED/PARTIAL` com
`observedShape=OPAQUE_DLI`. O AIR contém a mesma quantidade de operações opacas
com `observedKind=OPAQUE_DLI`; nenhuma semântica IMS foi inferida.

| Program | EXEC DLI preservados no SP / AIR | Dependency sites | Known candidates | Remainders |
| --- | ---: | ---: | --- | --- |
| CBPAUP0C | 5 / 5 | 0 | nenhum | não aplicável |
| COPAUA0C | 8 / 8 | 4 COBOL `CALL` literais | `MQGET`, `MQOPEN`, `MQCLOSE`, `MQPUT1` | nos 4: model=false; source/interpretação/effective/control=true |
| COPAUS0C | 6 / 6 | 2 CICS `XCTL` computados | nenhum; `UNSUPPORTED_TARGET_EXPRESSION` | nos 2: model=null; source/interpretação/effective/control=true |
| COPAUS1C | 7 / 7 | 2 CICS (`LINK`, `XCTL`) computados | nenhum; `UNSUPPORTED_TARGET_EXPRESSION` | nos 2: model=null; source/interpretação/effective/control=true |

Os sites acima não são reconstruídos de dentro do DLI opaco. A partiality vem de
COPYs ausentes e de outras capacidades/incertezas publicadas; ela não interrompe
a pipeline e não constitui novo blocker pós-frontend.

## Blocker restante: COTRTLIC

- Fase exata: `NORMALIZATION`, antes de preprocessing e parser.
- Exception/reason: `IllegalArgumentException`, mensagem
  `Unsupported tab in fixed-format source at offset 75337`; classificação do
  wrapper `NORMALIZATION_GAP/FIXED_FORMAT_TAB`.
- Local: raw offset zero-based 75337 (também byte 75337 porque o prefixo é ASCII),
  linha 1811, coluna 18 (um `HT`, byte `09`).
- Contexto mínimo real:

```text
181200             AND
181300           <HT> ((:WS-EDIT-DESC-FLAG = '1'
181500                  AND TR_DESCRIPTION LIKE
```

- Menor witness executado: seis espaços, `HT`, `X`
  (`"      \tX"`) → `Unsupported tab ... at offset 6`.
- Causa: `SourceNormalizer.validateFixedCharacters` rejeita um tab a partir do
  offset 6 quando existe conteúdo posterior. É policy fixed-format/normalizer,
  não parser nem regra SQL.

## Blocker restante: CBSTM03A do ZIP

- Fase exata: frontend em `PREPROCESSING`, durante a normalização do copybook
  resolvido por `COPY CUSTREC` (COPY na linha 55 do source ZIP). O source principal
  já havia passado sua própria normalização.
- Exception/reason: `IllegalArgumentException`, mensagem
  `Unsupported fixed-format indicator '0' at line 6, column 7`; classificação do
  wrapper `NORMALIZATION_GAP/NORMALIZATION_REJECTED`.
- Contexto que dispara, em `migrated_app/cpy/CUSTREC`, linha 6:

```text
<HT><5 spaces>05  CUST-FIRST-NAME  ...
```

  Prefixo bruto: `09 20 20 20 20 20 30 35`. Como o normalizer conta o `HT`
  inicial como um único code point, o `0` vira o sétimo code point e é interpretado
  como indicador fixed-format inválido.
- Menor witness executado: `"\t     0"` → a mesma exception na linha 1,
  coluna 7.
- Diferença relevante: o programa ZIP e o nativo usam as mesmas quatro COPYs,
  mas a precedência do wrapper seleciona `migrated_app/cpy/CUSTREC` para o ZIP.
  A variante nativa seleciona `app/cpy/CUSTREC.cpy`; sua linha equivalente começa
  com dois `HT` seguidos de cinco espaços (`09 09 20 20 20 20 20 30 35`) e o
  normalizer atual a aceita. O probe confirmou: ZIP `CUSTREC` falha; o nativo passa.
  Esse PASS é acidental ao modo atual de contar tabs, não prova uma policy correta.
- A falha não é causada por CRLF nem pelo parser COBOL. O stack é
  `PreprocessorEngine.processRecursive` → `CopybookLibrary.readNormalized` →
  `SourceNormalizer.normalizeFixed` → `SourceNormalizer.indicator`.

## Root cause e eventual correção

Os dois bugs compartilham a mesma raiz: falta uma política explícita para mapear
`HT` físico a colunas fixed-format antes de interpretar sequence area, indicator,
área compilável e margem 72. Hoje tabs na área compilável são recusados; tabs no
prefixo podem atravessar a validação e deslocar silenciosamente a coluna 7.

Arquivos/classes provavelmente envolvidos:

- `proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/SourceNormalizer.java`
  (`normalizeFixed`, `validateFixedCharacters`, `indicator`, e mapeamento de origem);
- `proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/CopybookLibrary.java`
  como fronteira que aplica o mesmo normalizer aos COPYs;
- `proleap-poc/src/main/java/io/github/gustavo2358/cobolexplorer/PreprocessorEngine.java`
  para o teste de integração da expansão recursiva, não como causa primária.

Menor mudança conceitual: definir, com autoridade do dialeto/configuração, como
um `HT` avança a coluna física e aplicar essa expansão antes do recorte 1–6/7/8–72,
preservando no `SourceMap` que vários espaços normalizados vêm de um único byte.
Não basta permitir tabs nem tratar cada tab como largura 1.

Risco semântico: médio/alto no frontend. Uma escolha errada de tab stops pode
mudar indicator/comment/continuation/debug, mover texto para dentro/fora da coluna
72, ou alterar espaços dentro de literal; provenance e offsets também podem ficar
incorretos. A policy deve continuar fail-closed quando a interpretação não estiver
configurada/provada.

Testes focais esperados no eventual fix:

1. unitários do normalizer para `HT` nas áreas 1–6, coluna 7, área 8–72 e após 72,
   em posições antes/depois de um tab stop;
2. os dois witnesses mínimos deste relatório e a linha real de `COTRTLIC`;
3. integração `COPY CUSTREC` com as variantes ZIP (um tab inicial) e nativa (dois),
   verificando declaração resultante e include provenance;
4. `SourceMap` antes, dentro e depois da expansão do tab;
5. negativos para indicador realmente inválido, tab ambíguo em literal,
   continuação/comment/debug e tab que cruza a coluna 72;
6. reexecução real apenas de `COTRTLIC` e do `CBSTM03A` ZIP até dependency após o
   fix, sem stubs nem copybooks adicionais.

## Ordem proposta

1. Fechar a regra de tab stops/dialeto e seus invariantes de provenance.
2. Implementar uma única correção em `SourceNormalizer`, com os testes unitários.
3. Cobrir a fronteira de COPY recursivo para impedir que o caso ZIP seja mascarado.
4. Reexecutar primeiro os dois blockers; só então decidir se existe qualquer
   blocker downstream novo. Não há razão para tocar lower, AIR ou CFG antes disso.

## Evidência e gates

Evidência primária: `run/measurements-focal.json`. Cada diretório sob
`run/programs/` contém `measurement.json`, `frontend.stdout`, `frontend.stderr`
e, quando alcançados, os produtos e logs de lower/CFG/dependency. Inputs copiados
byte-for-byte estão em `run/inputs/`; o ZIP extraído usado na resolução está em
`run/archive-members/`. `probe.log` registra os mínimos e a comparação de
`CUSTREC`; `checkout-state.txt` registra SHAs/limpeza; `pins.json` congela o run.

Executado: preparação/build sem testes, pipeline real dos seis sources e probe
direto do normalizer. Não executados, por escopo: full frontend/lower/CFG, FAST de
qualquer repositório, corpus de 73, mutations históricas e gates de engenharia.

Nenhum código de produção foi alterado; nenhum PR, commit, stub ou copybook extra
foi criado. Nenhuma capability SET, INITIALIZE, file semantics ou outra foi iniciada.
