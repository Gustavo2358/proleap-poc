# D0 — F1 isolado de partial semantics

Dois bloqueios de frontend, reproduzidos na evidência canônica hash-verificada. São manifestações distintas da mesma ausência de política de colunas para HT.

| Caso | Local bruto | Comportamento atual | Causa |
| --- | --- | --- | --- |
| checkout COTRTLIC.cbl | offset zero-based 75337, linha 1811, coluna 18, byte 09, dentro de SQL | rejeita antes do preprocessing: Unsupported tab | validateFixedCharacters rejeita HT a partir de offset 6 com conteúdo posterior |
| ZIP CBSTM03A.cbl / COPY CUSTREC | COPY fonte linha 55; CUSTREC linha 6 começa `09 20 20 20 20 20 30 35` | conta HT como 1 caractere; lê `0` na coluna 7 e rejeita | HT inicial passa pela validação sem expansão |

O CUSTREC nativo equivalente tem dois HT antes dos cinco espaços e é aceito pelo contador atual. Isso não prova normalização correta. Não é CRLF, parser SQL ou ausência de COPY: o COPY do segundo caso foi encontrado.

## Autoridade e política necessária

IBM 6.4 [Language Reference, cap.6 pp.55–61](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf) define áreas fixas 1–6, indicador 7 e área de programa 8–72. A referência consultada **não estabelece tab stops para importar bytes HT destes arquivos**. Portanto não afirmar que “IBM COBOL aceita tab com largura 8” como regra do compilador.

IBM documenta uma política de conversão em [z/OS `expand`](https://www.ibm.com/docs/en/zos/2.5.0?topic=descriptions-expand-expand-tabs-spaces): stops default de oito colunas, configuráveis. Também há opções de tab na [source conversion utility Linux](https://www.ibm.com/docs/en/cobol-linux-x86/1.1.0?topic=scu-source-conversion-utility-options). São precedentes de **import/conversion policy**, não prova da intenção original do fonte z/OS/UniKix.

Resposta “Tab é permitido?”: permitido como entrada de conversão com policy declarada; não há prova suficiente para admiti-lo silenciosamente como caractere literal de coluna no dialeto fixo pretendido. Sem policy, diagnóstico explícito é correto; aceitar alguns prefixos acidentalmente é inconsistente. Tipo F (configuração/import), com robustez G a confirmar contra policy/dialeto declarado, não invalid COBOL demonstrado.

Proposta: escolher tab stops explícitos, expansão até o próximo stop (não “substituir cada tab por 8 espaços”), antes de interpretar sequence/indicator/content/margem. Aplicar **por arquivo físico**, source e copybook, antes da expansão COPY; COPY reutiliza o mesmo normalizer e preserva cadeia de proveniência. A política de comentários, debug, continuação, literais e caracteres após coluna 72 precisa ser a mesma nos dois caminhos. Mapear todos os espaços gerados ao byte HT de origem, sem adulterar o corpus.

A futura wave deve provar os dois casos reais, testes de fronteira de coluna e negativos para mudança de indicador/literal. Expandir apenas depois de COPY já perde as colunas do copybook; aceitar HT como largura 1 mantém o erro. Nenhum normalization fix foi aplicado em D0.

A investigação anterior [report 2026-09-14](references/artefatos-e2e/carddemo-blockers-20260914/report.md) contém witness mínimo e stack; a conclusão D0 acima se apoia também nos bytes/logs canônicos atuais. Nenhum programa foi retirado dos 73 por esse motivo.
