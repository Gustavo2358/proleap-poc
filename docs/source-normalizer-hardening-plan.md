# Plano de hardening do `SourceNormalizer` e da provenance

Status: **AGUARDANDO APROVAÇÃO — NÃO INICIAR A IMPLEMENTAÇÃO AINDA**

## Objetivo

Substituir o comportamento heurístico de `SourceNormalizer` por uma fronteira de
leitura COBOL fixed-format explícita, exaustiva, fail-closed e capaz de produzir
provenance correta desde o arquivo físico. Ao final, nenhuma transformação
anterior ao parser poderá ser apresentada pelo `SourceMap` como identidade
exata sem realmente sê-lo.

Este plano cobre:

- o vazamento de estado que transforma `ENVIRONMENT DIVISION` (ou outra linha
  COBOL posterior) em `*>CE`;
- a inferência de comment entries por regex e ponto final;
- a allowlist parcial de headers de comment entry;
- inserção, remoção e fusão de linhas antes da criação do `SourceMap`;
- continuação fixed-format baseada em contagem de aspas;
- tratamento não exaustivo da indicator area;
- margens, sequence area, identification area, debug lines e page-eject;
- normalização incorreta de line endings e da quebra final;
- ausência de contrato explícito para formato/dialeto;
- ausência de testes unitários e de regressão da aplicação para essa fronteira.

## Autorização e modo de execução

Após a aprovação deste plano, o agente deve executar todas as fases abaixo do
começo ao fim, sem pedir autorização entre tasks, fases ou commits. A aprovação
autoriza as alterações locais, a criação das fixtures, a execução dos testes, a
execução da aplicação e os commits locais previstos neste documento.

O trabalho só pode ser interrompido por um bloqueio real que exija uma decisão
fora do escopo aprovado, uma operação destrutiva não prevista ou autoridade
externa nova. Dificuldade técnica, teste vermelho, regressão ou necessidade de
refatoração interna não são motivos para parar ou pedir confirmação.

Não fazer `push` como parte deste plano, salvo solicitação explícita posterior.

## Princípios obrigatórios

- A gramática e um contrato explícito de formato definem o universo aceito; o
  corpus não define suporte de linguagem.
- Toda classe de registro físico e todo indicador aceito possuem branch e
  política explícitos.
- Indicador, formato, continuação ou construção desconhecida gera diagnóstico
  localizado ou falha fail-closed; nunca cai em conteúdo normal por default.
- `ignore`, `remove`, `preserve` e `unsupported` são decisões explícitas.
- Nenhuma regex deve redescobrir uma construção que ANTLR ou o scanner de
  registros físicos já reconhece.
- Nenhuma transformação retorna apenas texto se altera offsets, colunas ou
  linhas: deve transportar `SourceMap` e diagnósticos.
- O texto bruto do arquivo permanece a fonte original armazenada no mapa.
- `exact=true` só pode ser usado para intervalos realmente lineares e idênticos
  ao arquivo físico.
- Não introduzir fallback silencioso para manter o corpus verde.
- Não gravar artefatos de regressão em `dist/` ou em outros arquivos rastreados;
  usar diretórios únicos sob `/tmp`.

## Ciclo TDD obrigatório para cada item separável

Cada checkbox funcional abaixo deve seguir este ciclo, mesmo quando vários
itens pertençam à mesma fase:

1. Criar uma fixture mínima que isole o comportamento.
2. Escrever primeiro o teste do contrato desejado.
3. Executar o teste focado e confirmar que falha pela razão esperada.
4. Implementar a solução mínima que seja geral para toda a classe sintática ou
   física, sem condição específica da fixture.
5. Executar novamente o teste focado até ficar verde.
6. Executar todos os testes relacionados à normalização, preprocessing,
   parsing, AST e provenance.
7. Executar a suíte Maven completa.
8. Executar a regressão real da aplicação descrita abaixo e avaliar seus
   artefatos, não apenas o exit code.
9. Fazer code review da fase: procurar regex sintática, estados implícitos,
   branches default, perda de mapa, `continue` silencioso e casos não cobertos.
10. Executar `git diff --check`, confirmar que apenas arquivos da fase serão
    incluídos e criar o commit indicado.

Não commitar estado vermelho, testes desabilitados, regressão não explicada ou
tasklist que declare como concluído algo ainda não verificado.

## Gate de regressão obrigatório antes de cada commit

Criar na Fase 0 um comando automatizado, reutilizado em todas as fases, que:

1. execute `mvn test` em `antlr-parse-tree-explorer`;
2. execute o `ExplorerMain` de verdade para o caso canônico:

   ```bash
   mvn -q compile exec:java \
     -Dexec.args="--source corpus/cbl/COACTUPC.cbl --copybooks corpus/cpy --output /tmp/proleap-source-normalizer/<fase>/coactupc"
   ```

3. execute o mesmo pipeline para uma fixture completa que contenha comment
   entry, comentário intermediário e `ENVIRONMENT DIVISION`;
4. verifique mecanicamente em cada saída:
   - exit code zero;
   - `index.html`, `ast.html`, `symbols.html`, `resolution.html`,
     `tree-data.js`, `ast-data.js`, `symbol-data.js` e
     `resolution-data.js` presentes e não vazios;
   - zero erros léxicos e zero erros sintáticos;
   - `PROGRAM-ID` esperado presente;
   - divisions esperadas presentes na parse tree/AST;
   - `ENVIRONMENT DIVISION` não convertido em `*>CE`;
   - nenhuma construção COBOL desaparecida por normalização;
   - SourceMap/provenance dos probes da fixture apontando para linha e coluna
     físicas corretas;
5. compare métricas semânticas canônicas com um baseline versionado. Mudanças de
   contagem são aceitas somente quando forem consequência deliberada da fase,
   cobertas por teste e registradas no commit; parser errors, perda de divisions,
   perda de statements ou degradação de provenance nunca são aceitos como
   simples atualização de snapshot.

O gate deve limpar ou sobrescrever apenas seu diretório específico em `/tmp` e
não depender de interface gráfica.

## Tasklist

### Fase 0 — Baseline observável e harness de regressão

- [x] Criar `SourceNormalizerTest` e a árvore de fixtures de source format.
- [x] Criar uma fixture completa reproduzindo `AUTHOR.` seguido de comentário
      fixed-format e `ENVIRONMENT DIVISION`.
- [x] Criar probes de provenance com posições físicas conhecidas.
- [x] Automatizar o gate de regressão da aplicação descrito acima.
- [x] Registrar baseline semântico de `COACTUPC.cbl` sem cristalizar offsets já
      sabidamente incorretos como comportamento desejável.
- [x] Caracterizar comportamentos válidos que devem sobreviver: comentários
      fixed-format, COPY, EXEC, compiler options, AST, símbolos e resolução.
- [x] Rodar teste focado, suíte completa e gate da aplicação.
- [x] Revisar o harness para garantir que uma aplicação que apenas retorna zero,
      mas perde `ENVIRONMENT DIVISION`, realmente falhe.
- [x] Commit: `test: establish source normalization regression harness`.

### Fase 1 — Fazer a provenance nascer no arquivo bruto

- [x] Introduzir um resultado imutável de normalização contendo pelo menos
      texto transformado, `SourceMap`, diagnósticos e política de formato.
- [x] Fazer o mapa armazenar o texto bruto como fonte original.
- [x] Integrar `ExplorerMain`, `PreprocessorEngine` e `CopybookLibrary` sem
      recriar `SourceMap.identity()` sobre texto já normalizado.
- [x] Preservar a cadeia de provenance em COPYs normalizados e aninhados.
- [x] Testar linha/coluna original, `exact` e conteúdo original antes e depois
      de transformação, inclusive em copybooks.
- [x] Confirmar que APIs antigas que aceitam somente `String` não conseguem
      reintroduzir identidade falsa; remover ou restringir esses caminhos.
- [x] Rodar teste focado, suíte completa e gate da aplicação.
- [x] Commit: `refactor: originate source map before normalization`.

### Fase 2 — Scanner exato de registros físicos e line endings

- [x] Substituir `split("\\R", -1)` por leitura que preserve cada separador
      físico e a presença ou ausência de terminador final.
- [x] Corrigir arquivo vazio, arquivo sem newline final, LF, CRLF e CR sem
      adicionar linha fantasma.
- [x] Modelar cada registro físico com offsets bruto inicial/final, conteúdo e
      terminador.
- [x] Testar round-trip de linhas e offsets para todos os terminadores aceitos.
- [x] Definir política explícita para separadores Unicode: suportar com mapa
      correto ou rejeitar com diagnóstico, sem normalização implícita.
- [x] Rodar teste focado, suíte completa e gate da aplicação.
- [x] Commit: `fix: preserve physical source records and line endings`.

### Fase 3 — Contrato fechado de source format e margens

- [x] Introduzir política explícita de formato, inicialmente com os formatos
      realmente suportados (`FIXED` e quaisquer outros comprovadamente
      implementados), sem autodetecção heurística silenciosa.
- [x] Modelar sequence area, indicator area, Area A/B e identification area com
      posições físicas, sem descartar sua relação com o fonte.
- [x] Definir margens/dialeto como configuração explícita quando variáveis.
- [x] Tratar linhas curtas, tabs, caracteres não ASCII e conteúdo após a margem
      direita de maneira deliberada e testada.
- [x] Preservar caracteres fora da área compilável como whitespace mapeado ou
      metadado, em vez de cortar offsets sem registro.
- [x] Rejeitar formato não suportado com diagnóstico localizado.
- [x] Rodar teste focado, suíte completa e gate da aplicação.
- [x] Commit: `refactor: define exhaustive cobol source format contract`.

### Fase 4 — Indicator area exaustiva

- [x] Criar enum/política fechada para blank, comment, page-eject comment,
      continuation e debug; listar explicitamente extensões de dialeto aceitas.
- [x] Fazer indicador desconhecido/ilegal falhar localmente, incluindo linha e
      coluna física.
- [x] Preservar comentários e page-eject sem alterar número de linhas e com
      provenance não enganosa.
- [x] Tornar debug line dependente de política explícita, sem removê-la sempre
      por default.
- [x] Criar teste de exaustividade para que um novo tipo de indicador/política
      não ganhe fallback silencioso.
- [x] Rodar teste focado, suíte completa e gate da aplicação.
- [x] Commit: `refactor: close fixed format indicator policies`.

### Fase 5 — Continuação COBOL sem contagem heurística de aspas

- [x] Remover `oddQuote()` e qualquer decisão baseada apenas em paridade de
      caracteres.
- [x] Implementar continuação a partir de categorias lexicais/sintáticas
      explícitas: literal, palavra/token e casos não suportados.
- [x] Cobrir aspas simples e duplas, aspas do outro tipo dentro do literal,
      escaping COBOL, literais hexadecimais, nacionais e null-terminated.
- [x] Cobrir continuação de palavra, espaços significativos e separação de
      tokens conforme a gramática/dialeto suportado.
- [x] Rejeitar continuação órfã, continuação após registro incompatível e formas
      reconhecidas mas ainda não implementadas.
- [x] Mapear o texto lógico resultante para todos os registros físicos que o
      originaram e marcar regiões transformadas como não exatas.
- [x] Garantir que a fusão lógica não reduza linhas reportadas no fonte original.
- [x] Rodar teste focado, suíte completa e gate da aplicação.
- [x] Commit: `fix: implement mapped cobol continuation semantics`.

### Fase 6 — Comment entries definidos pela sintaxe, sem regex global

- [ ] Remover `markCommentEntries()`, a regex de headers e o booleano
      `inEntry`.
- [ ] Escolher e registrar em uma breve ADR local a solução que mantém a
      gramática como autoridade: preferir lexer/parser ou uma gramática ANTLR
      dedicada de registros; somente usar scanner intermediário se ele tiver
      estados e fronteiras fechados derivados das regras gramaticais.
- [ ] Cobrir exaustivamente os pontos que aceitam `commentEntry`, incluindo
      `programIdParagraph`, `AUTHOR`, `INSTALLATION`, `DATE-WRITTEN`,
      `DATE-COMPILED`, `SECURITY` e `REMARKS`.
- [ ] Criar teste de contrato entre esses pontos da gramática e as políticas do
      componente responsável por comment entries.
- [ ] Determinar o fim pela fronteira sintática apropriada, nunca por
      `endsWith(".")`.
- [ ] Cobrir comment entry inline, multilinha, vazio, com pontos internos, sem
      ponto final, com linhas vazias, com comentários fixed-format intercalados,
      no EOF e imediatamente antes de outro parágrafo/division.
- [ ] Reproduzir e corrigir explicitamente o caso em que
      `ENVIRONMENT DIVISION` virava `*>CE ENVIRONMENT DIVISION`.
- [ ] Garantir que comment entries não gerem nós semânticos, CFG, dataflow ou
      dependências, mas mantenham provenance física correta.
- [ ] Rodar teste focado, suíte completa e gate da aplicação.
- [ ] Commit: `fix: derive comment entries from closed syntax`.

### Fase 7 — Integração completa com preprocessing e copybooks

- [ ] Testar normalização + COPY simples, aninhado, `REPLACING`, ausente,
      cíclico e com erro de leitura.
- [ ] Testar comment entry e continuação dentro de copybook com include chain.
- [ ] Testar `EJECT`, `SKIP1/2/3`, `TITLE`, EXECs e compiler options depois da
      nova fronteira de source format.
- [ ] Confirmar que transformações do preprocessor preservam o mapa bruto criado
      pelo normalizador, sem voltar a `identity` intermediário.
- [ ] Confirmar que diagnósticos do lexer, parser e preprocessor apontam para
      arquivo, linha e coluna físicos.
- [ ] Rodar teste focado, suíte completa e gate da aplicação.
- [ ] Commit: `test: cover normalization and preprocessing provenance`.

### Fase 8 — Fail-closed, remoção de compatibilidade heurística e review global

- [ ] Buscar e remover regexes, `startsWith`, allowlists parciais, branches
      default permissivos e `continue` silencioso na fronteira de leitura.
- [ ] Verificar exaustividade entre gramática, formatos, indicadores,
      continuações, comment entries e políticas conhecidas.
- [ ] Confirmar que toda forma reconhecida possui uma decisão: preservar,
      transformar, ignorar deliberadamente ou rejeitar localmente.
- [ ] Remover APIs/transições temporárias que permitam perder provenance.
- [ ] Atualizar README com o contrato de source format, limitações deliberadas,
      diagnósticos e procedimento de regressão.
- [ ] Executar a suíte completa pelo menos duas vezes em árvore limpa.
- [ ] Executar o gate da aplicação para o caso canônico e todas as fixtures de
      integração.
- [ ] Inspecionar o diff completo e confirmar ausência de mudanças de artefatos
      gerados ou unrelated.
- [ ] Commit: `docs: finalize source normalization hardening`.

## Critérios de aceite finais

- [ ] O caso `comment entry -> comentário -> ENVIRONMENT DIVISION` parseia sem
      comentar ou perder a Environment Division.
- [ ] Nenhum comment entry é delimitado por regex global ou ponto final.
- [ ] Nenhuma continuação usa paridade de aspas.
- [ ] O número de linhas físicas e os line endings possuem política e mapa
      verificáveis.
- [ ] Linha e coluna originais referem-se ao arquivo bruto, inclusive depois de
      comment entries, continuações e COPYs.
- [ ] `exact=true` nunca é atribuído a trecho transformado.
- [ ] Indicadores e formatos desconhecidos falham de forma localizada.
- [ ] Existe teste automático de exaustividade para cada catálogo fechado.
- [ ] Todos os testes Maven passam.
- [ ] A regressão real da aplicação passa e seus artefatos são semanticamente
      coerentes.
- [ ] Cada fase possui seu próprio commit verde e revisado.
- [ ] A árvore de trabalho termina limpa.

## Sequência esperada de commits

1. `test: establish source normalization regression harness`
2. `refactor: originate source map before normalization`
3. `fix: preserve physical source records and line endings`
4. `refactor: define exhaustive cobol source format contract`
5. `refactor: close fixed format indicator policies`
6. `fix: implement mapped cobol continuation semantics`
7. `fix: derive comment entries from closed syntax`
8. `test: cover normalization and preprocessing provenance`
9. `docs: finalize source normalization hardening`

Se uma fase precisar de mais de um commit para manter bugs realmente separáveis,
dividi-la é permitido e preferível, desde que cada commit cumpra integralmente o
ciclo TDD e o gate de regressão. Combinar fases para reduzir commits não é
permitido.
