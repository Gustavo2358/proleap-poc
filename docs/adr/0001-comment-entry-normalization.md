# ADR 0001: normalização fechada de comment entries COBOL

- Status: aceito
- Data: 2026-08-25

## Contexto

A gramática COBOL recebe comment entries por meio do token sintético
`COMMENTENTRYLINE` (`*>CE`). O fonte fixed-format, porém, expressa essas entradas
como texto posterior aos parágrafos que admitem `commentEntry`. Portanto, essa
conversão precisa ocorrer antes do lexer COBOL.

A implementação anterior procurava alguns headers com regex, mantinha um booleano
`inEntry`, encerrava a entrada pela presença de ponto no texto e chegava a inserir
uma linha física. Esse modelo não correspondia à gramática, confundia pontuação do
comentário com fronteira sintática e podia deslocar todo o `SourceMap`.

## Decisão

Usar um scanner determinístico de registros fixed-format, depois da classificação
exaustiva da área indicadora e antes do parser COBOL.

O catálogo fechado de proprietários de comment entry é:

- `programIdParagraph`;
- `authorParagraph`;
- `installationParagraph`;
- `dateWrittenParagraph`;
- `dateCompiledParagraph`;
- `securityParagraph`;
- `remarksParagraph`.

Um teste de contrato compara esse catálogo com todos os contexts gerados pelo
ANTLR que expõem diretamente `commentEntry()`. Uma mudança na gramática sem a
política correspondente falha no teste.

Headers só são reconhecidos quando começam na Area A e possuem o separador
`DOT_FS`; comparações de palavras e delimitadores são explícitas, sem regex ou
prefix matching permissivo. O texto inline e os registros seguintes em Area B
são marcados com `*>CE`. Pontos dentro deles são apenas dados. A entrada termina
na próxima construção em Area A; `remarksParagraph` também possui a fronteira
explícita `END-REMARKS` declarada pela gramática.

`programIdParagraph` possui estado próprio porque o nome do programa e sua
cláusula opcional pertencem ao parágrafo antes do comment entry. As alternativas
`COMMON`, `INITIAL`, `LIBRARY`, `DEFINITION` e `RECURSIVE`, com `IS` e `PROGRAM`
opcionais conforme a gramática, são enumeradas deliberadamente.

Cada transformação conserva o terminador do registro original. Nenhuma linha é
inserida ou removida, e todo texto marcado recebe `exact=false`; conteúdo não
transformado e seus terminadores mantêm seus segmentos exatos.

## Consequências

- O número de linhas e os line endings permanecem estáveis para o `SourceMap`.
- Pontuação e comentários comuns dentro de uma entrada não alteram o estado.
- Novos pontos da gramática que aceitem `commentEntry` exigem política e teste.
- Formas que não satisfaçam as fronteiras enumeradas não ganham fallback
  silencioso nem são promovidas heuristicamente a comment entry.
