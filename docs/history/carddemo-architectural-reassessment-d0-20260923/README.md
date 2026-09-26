# CardDemo — architectural reassessment D0

**READY_FOR_REVIEW.** Publicação documental da investigação de 2026-09-23, com
20 relatórios, matriz F2, scripts, probes, medições históricas e evidência canônica
dos 73 programas. A recomendação é a arquitetura **C**, ainda sujeita a revisão.
O baseline observado continua **4/73 reach dependency**, todos `PARTIAL`.

Comece pelo [resumo executivo](D0_EXECUTIVE_SUMMARY.md), pela
[recomendação](D0_RECOMMENDATION.md) e pelo [índice de evidências](D0_EVIDENCE_INDEX.md).
Para a família dominante, veja o [deep dive F2](D0_F2_DEEP_DIVE.md) e a
[matriz de 1.964 relações site/parágrafo](D0_F2_CLUSTER_MATRIX.csv).
As alternativas estão no [scorecard A/B/C](D0_ARCHITECTURE_SCORECARD.md), e a
proposta de sequência está no [roadmap](D0_ROADMAP_PROPOSAL.md).

## Escopo e autoridade

Este PR publica o discovery mediante solicitação posterior do usuário. As frases
“não há commit/PR”, “PRs modified: NO” e equivalentes nos relatórios congelados
descrevem a execução original do D0, anterior a esta publicação.

Os relatórios são evidência e proposta, não um ADR aceito ou autorização de
implementação. Não há alteração de código de produto, contrato, baseline,
workflow ou gate. W8 permanece pausada e W9 não foi iniciada. O PR parte de
`main`; os cinco pins analisados estão em
[baseline/initial.json](probes/baseline/initial.json), separados do código da
branch base deste PR.

CardDemo: `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`.
Os caminhos absolutos em logs, comandos e metadados preservam a proveniência do
workspace original. Os scripts originais exigem aquele layout ou adaptação
explícita para nova execução; a verificação da publicação abaixo é portátil.

## Conteúdo publicado

| Pacote | Conteúdo | Arquivos |
| --- | --- | ---: |
| [d0-evidence.tar.gz](archives/d0-evidence.tar.gz) | Relatórios originais, matriz, tools, probes e todas as 60 execuções históricas do D0, com manifesto original | 1.080 |
| [canonical-carddemo-73.tar.gz](archives/canonical-carddemo-73.tar.gz) | Evidência canônica integral dos 73 programas, cenários auxiliares, logs, outputs e manifesto original | 4.788 |
| [supporting-references.tar.gz](archives/supporting-references.tar.gz) | Snapshots de código/contratos citados, documentos históricos citados e descritores de runtime | 43 |

Os arquivos compactados somam aproximadamente 76,5 MiB. O
[inventário de publicação](PUBLICATION_INVENTORY.json) registra caminho, tamanho,
SHA-256 e localização de cada um dos **5.911 arquivos** arquivados.
[ARCHIVES.sha256](ARCHIVES.sha256) verifica os três arquivos compactados.
Todos os outputs analíticos presentes nos manifestos originais foram preservados.

As cópias navegáveis nesta pasta incluem os 20 relatórios e os probes derivados.
Nos 20 relatórios, somente destinos de links locais foram ajustados para as
cópias publicadas. O texto original e os hashes originais permanecem dentro de
`d0-evidence.tar.gz`. Nas duas tabelas CSV navegáveis, CRLF foi convertido em LF sem alteração
de células; no dossiê COBOL navegável, apenas espaços ao fim das linhas de
exibição foram removidos. Os bytes originais, inclusive esses espaços, permanecem
no arquivo compactado. As demais cópias navegáveis são byte-identical; o inventário
registra cada ajuste de link e formatação. Logs/diffs brutos ficam no arquivo
compactado quando sua formatação original conflita com a higiene Git. Resultados volumosos por programa estão nos
arquivos compactados, nos caminhos originais relativos ao workspace.

O [resumo canônico](canonical-summary/report.md), a
[classificação dos 73 programas](canonical-summary/classification.csv) e os
[comandos](canonical-summary/commands.md) também estão disponíveis sem extração.

## Referências externas e exclusões explícitas

O PDF e a extração textual integral do manual IBM são referências normativas de
terceiro, disponíveis pela [URL oficial](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf),
e não foram republicados no repositório público. Seus dois hashes originais
estão no inventário e no manifesto D0; os [metadados de origem](probes/official/source.json)
e todas as análises/citações foram preservados. Assim, o manifesto original D0
contém **1.081 entradas: 1.079 arquivos publicados e duas referências externas**.
Não se declara que esses dois arquivos externos estão contidos no tarball.

Classes compiladas, caches e dependências/toolchains reproduzíveis ficam fora
do pacote, como já ficavam fora dos manifestos originais. Os 144 symlinks de
recursos web do D0 e da validação canônica foram registrados no inventário, sem
desreferenciar ou copiar árvores externas. Não são evidências analíticas.

A auditoria D0 dos 22.567 hashes de waves anteriores está preservada. Os arquivos
originais dessas waves não foram republicados recursivamente; os documentos
citados e descritores necessários estão no pacote de referências, e os outputs
das 60 novas execuções históricas feitas no D0 estão integralmente publicados.

## Verificação e extração

A partir desta pasta, a verificação portátil não executa nenhum produto:

```bash
python3 -B verify-publication.py
sha256sum -c ARCHIVES.sha256
```

Ela valida os hashes e a enumeração de todos os membros dos arquivos compactados,
o manifesto canônico de 4.787 entradas, a cobertura das 1.081 entradas do manifesto
D0, as cópias navegáveis, a preservação do texto dos relatórios e seus links locais.
Para inspecionar os arquivos originais, extraia para uma pasta nova:

```bash
mkdir unpacked
tar -xzf archives/d0-evidence.tar.gz -C unpacked
tar -xzf archives/canonical-carddemo-73.tar.gz -C unpacked
tar -xzf archives/supporting-references.tar.gz -C unpacked
cd unpacked
sha256sum -c artefatos-e2e/carddemo-validation-20260923/sha256sums.txt
```

Os caminhos restauram a estrutura relativa do workspace, sem extrair caminhos
absolutos ou symlinks. A verificação integral do manifesto D0 com `sha256sum -c`
exigiria também obter o PDF oficial e reproduzir a extração textual com os hashes
registrados; `verify-publication.py` distingue explicitamente essas referências.

A [auditoria original D0](probes/delivery-validation.json) registra o resultado
da investigação. A [verificação de publicação](PUBLICATION_VALIDATION.json)
registra a integridade deste pacote. Testes de produto e full73 não foram
reexecutados para esta publicação documental; nenhum novo PASS semântico é alegado.
