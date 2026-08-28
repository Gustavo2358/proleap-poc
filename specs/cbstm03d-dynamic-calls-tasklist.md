# SDD — Variante de CBSTM03A com CALLs dinâmicos

## Objetivo

Criar uma variante isolada de `CBSTM03A.CBL` para exercitar resolução futura de
alvos de `CALL` por reaching definitions. O programa original e os demais
artefatos existentes devem permanecer intactos.

## Decisões propostas

- Novo fonte: `corpus/cbl/CBSTM03D.CBL` (`D` de dynamic).
- Novo `PROGRAM-ID`: `CBSTM03D`.
- Fonte de origem: `../cbl/CBSTM03A.CBL`, copiado sem alterar o baseline.
- Uma única variável de alvo: `WS-CALL-TARGET PIC X(8) VALUE SPACES`.
- Todos os 14 statements usarão `CALL WS-CALL-TARGET`.
- Os targets reais já existentes serão preservados:
  - 13 chamadas para `CBSTM03B`;
  - 1 chamada para `CEE3ABD`.
- Um `MOVE` será inserido somente quando o valor necessário diferir do valor
  conhecido no fluxo linear anterior. Na sequência atual, isso significa:
  - `MOVE 'CBSTM03B' TO WS-CALL-TARGET` antes do primeiro CALL;
  - nenhum MOVE antes das 12 repetições seguintes de `CBSTM03B`;
  - `MOVE 'CEE3ABD' TO WS-CALL-TARGET` antes do CALL final.
- Nova saída visual: `dist-cbstm03d/`.

## Fora do escopo

- Alterar `CBSTM03A.CBL` original.
- Criar CFG, reaching definitions, constant resolution ou bindings.
- Mudar a ordem dos CALLs ou inventar novos subprogramas.
- Alterar argumentos, tratamento de retorno ou comportamento adjacente.
- Modificar AST ou tabela de símbolos, salvo se um defeito real impedir o
  parsing da sintaxe dinâmica já suportada.

## Tasklist

- [x] Copiar `../cbl/CBSTM03A.CBL` para `corpus/cbl/CBSTM03D.CBL`.
- [x] Alterar somente o `PROGRAM-ID` da variante para `CBSTM03D`.
- [x] Declarar `WS-CALL-TARGET` vazia na `WORKING-STORAGE SECTION`.
- [x] Substituir os 14 alvos literais por `WS-CALL-TARGET`, preservando cada
      cláusula `USING` e a pontuação original.
- [x] Inserir os dois MOVEs mínimos definidos acima, sem MOVEs redundantes.
- [x] Confirmar por inspeção que o original permaneceu byte a byte inalterado
      (`SHA-256 23c8753b...432b5`).
- [x] Executar o explorer com a variante, copybooks internos e saída isolada:

      ```bash
      mvn compile exec:java \
        -Dexec.args="--source corpus/cbl/CBSTM03D.CBL --copybooks corpus/cpy --output dist-cbstm03d"
      ```

- [x] Validar zero erros léxicos e sintáticos.
- [x] Validar na AST exatamente `0` CALLs estáticos e `14` CALLs dinâmicos.
- [x] Validar que todos os 14 `CallStatement` apontam para a mesma
      `DataReference` escrita como `WS-CALL-TARGET`.
- [x] Validar na tabela de símbolos uma única declaração DATA canônica
      `WS-CALL-TARGET`, com valor inicial `SPACES` preservado na declaração.
- [x] Abrir/inspecionar os HTMLs gerados e conferir a navegação entre Parse
      Tree, AST e Symbol Table.
- [x] Atualizar o README com o propósito da variante e o comando reproduzível.
- [x] Executar a suíte Maven e as verificações de sintaxe JavaScript.
- [x] Revisar o diff para garantir que nenhuma etapa futura foi antecipada.
- [x] Criar um commit isolado para a variante e seus artefatos gerados.

## Critérios de aceite

1. `CBSTM03A.CBL` e os artefatos anteriores não são modificados.
2. A variante contém 14 CALLs pela mesma variável e somente 2 MOVEs de target.
3. A sequência de programas chamados continua sendo 13 × `CBSTM03B`, seguida
   por 1 × `CEE3ABD`.
4. O parser termina sem erros e os três HTMLs funcionam via `file://`.
5. A AST reporta 14 CALLs dinâmicos e nenhum CALL estático.
6. Não há implementação de CFG, dataflow ou resolução de constantes neste passo.
