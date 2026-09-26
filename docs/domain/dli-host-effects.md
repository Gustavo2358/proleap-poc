# EXEC DLI: limites de memória e continuação

Regra: GU/GN/GNP recuperam segmentos na área INTO; SCHD agenda um PSB;
TERM encerra o agendamento, não o programa COBOL. O retorno ordinário é um
resultado possível. Falhas externas permanecem como resultado desconhecido.

Fontes IBM: [GU](https://www.ibm.com/docs/en/ims/15.4.0?topic=commands-gu-command),
[GNP](https://www.ibm.com/docs/en/ims/15.4.0?topic=commands-gnp-command),
[SCHD](https://www.ibm.com/docs/en/ims/15.4.0?topic=commands-schd-command),
[TERM](https://www.ibm.com/docs/en/ims/15.4.0?topic=commands-term-command),
[DIB](https://www.ibm.com/docs/SSGMCP_6.1.0/reference-diagnostics/components/dfhs349.html).
O DIB é gerado pelo tradutor e recebe estado do ambiente; ele não é uma das
áreas privadas explicitamente declaradas em WORKING-STORAGE. Seus valores
continuam desconhecidos. Esta capacidade não materializa DIB, PCB ou bancos.

Perfil inicial: TERM sem opções; SCHD com PSB literal ou área, SYSSERVE e
NODHABEND; GU/GN/GNP com USING PCB, SEGMENT e INTO, e qualificação WHERE simples
por igualdade. Cada operando COBOL é analisado pela gramática COBOL, com origem
no payload original. Opções desconhecidas, repetidas fora da estrutura,
expressões não suportadas ou área de saída ausente não recebem prova.
SEGLENGTH e KEYLENGTH opcionais seguem o tradutor COBOL moderno; a nota para
COBOL for MVS/VM está no [manual IBM, seção de convenções](https://publibfp.boulder.ibm.com/epubs/pdf/dfsapcf6.pdf).

Algoritmo: enquadramento linear de opções → gramática dos operandos → resumo
canônico de leituras e possíveis escritas. Nenhuma escrita é obrigatória.
Efeitos externos ficam UNKNOWN. A topologia publica o retorno normal e uma
fronteira de falha; nenhuma recuperação de destino é feita no projetor.
O resumo é transportado em statementEffects, com prova DLI_HOST_OPERANDS,
restrita ao Semantic Product 2.46.0. O lower só traduz os fatos publicados e
mantém desconhecidos os acessos sem vinculação/armazenamento admissível.

Terminação: varreduras finitas do payload, dos operandos e das ocorrências.
Complexidade linear no tamanho de cada comando e seus operandos, além da
resolução nominal existente. Oracles: comandos sintéticos, região de escrita
isolada, opção desconhecida, saída incompleta, origem de COPY e preservação de
incerteza no controle. Corpus só é evidência de integração.
