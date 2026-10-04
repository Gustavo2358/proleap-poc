# Remoção da via legada de gaps — validação

## Resultado

Em 2026-10-03, os **73 fontes e variantes do CardDemo** completaram as quatro
etapas frontend → lower → CFG → dependencies: **292 execuções, zero falhas**.
Os gaps publicados passaram de **38.824 para 28.050**. Foram retiradas **10.774
ocorrências redundantes**, sem perda de dependências de nenhuma categoria no
corpus, nem alteração dos candidatos, suportes, proveniência, precisão,
remainder ou estado: **65 PARTIAL e 8 COMPLETE** em ambos os lados.

| Obrigação satisfeita por fatos atuais | Ocorrências retiradas |
| --- | ---: |
| Restrições antigas de isolamento/corpo linear de PERFORM | 5.514 |
| Containment já provado pela topologia atual | 3.993 |
| NO_OP com controle local fechado | 1.142 |
| Predicado textual já publicado | 125 |

Os **13 casos sem prova estrutural suficiente** continuam publicados, agora como
`CONTROL_MEMBERSHIP_NOT_PROVEN`. Limitações reais de efeitos, entrada ausente,
CICS, layout físico, valores e runtime permanecem explícitas. Este resultado não
é uma afirmação de completude para todo COBOL ou de ausência de outras futuras
melhorias nos diagnósticos.

## Comparação independente

O oracle de inventário foi a avaliação anterior, fixada em
`3a46c24bd96949573740e9cae7ed485797cce904`: cada gap então considerado pendente deve
continuar presente. Cada ocorrência removida precisa corresponder a uma
obrigação antes comprovadamente satisfeita. Nenhum output anterior foi alterado.

O [verificador reproduzível](../../scripts/verify-active-gaps.py) verifica:

- Inventário, fatos tipados, topologia, storage, valores nominais, dependências
  de fonte, entradas e cobertura do SP. Apenas a readiness local antes limitada
  exclusivamente pelo containment redundante pode passar de PARTIAL a SUFFICIENT;
  as demais propriedades e a cobertura permanecem iguais.
- Toda a estrutura executável, valores, efeitos e cobertura AIR; todas as
  origens são resolvidas até sua localização física e cadeia de derivação.
- CFG completo e fatos, derivações, candidatos e fronteiras do produto de fonte
  qualificada.
- Dependencies completo, incluindo programas, arquivos e demais categorias,
  candidatos, suportes, origens, precisão, motivos e estados.
- `observed-dependencies.json.zst` byte a byte; fingerprints do SP e AIR
  conferidos contra os bytes JSON descomprimidos.
- Igualdade exata entre os gaps do painel e os gaps canônicos de cada SP.

As identidades da publicação mudam porque seu conteúdo diagnóstico mudou.
IDs locais de incertezas/origens são comparados por conteúdo resolvido: a
renumeração pode reutilizar um ID antigo para outro gap. Retiram-se da comparação
somente os diagnósticos comprovadamente redundantes, seus vínculos e os campos de
métricas operacionais. Os 13 diagnósticos estruturais renomeados são verificados
explicitamente. A redação genérica dos statements observados é normalizada, sem
ignorar seu código, escopo ou proveniência. A forma JSON de origens do transporte
de dependencies é convertida para a mesma forma lógica da AIR.

## Gates e testes

| Validação local | Resultado |
| --- | --- |
| Frontend `lean.py fast` | PASS; 783 testes, zero falhas, erros ou skips |
| Frontend `lean.py qualification-local` | PASS; Maven 1.354 testes, zero falhas/erros, um skip condicional; regressão de normalização e artefatos também passou |
| Lower `lean.py fast` | PASS; inclui fixture real SP 2.63, AIR válido e seis mutações negativas |
| Lower `lean.py qualification-local` | PASS; suites semânticas, capacidade, desempenho e arquitetura |
| CardDemo, runtime final | 73 fontes, 292 etapas, zero falhas |
| Comparação final do corpus | 73/73 PASS |
| Painel em navegador | Total canônico, filtro, detalhe/proveniência e navegação para resolução verificados; zero erros no console |

O único skip é o teste de descoberta condicionado por `semantic.condition.required`
em `SemanticConditionContextDiscoveryTest`. Ele não foi convertido em PASS.
O lower recebeu posteriormente uma restrição adicional contra dispensar gaps de
statements genéricos; FAST e todo o corpus final foram executados após essa
restrição. O full local anterior continua evidência das partes não alteradas.

## Pins e reprodução

[Totais, resultado por fonte e hashes](active-gaps-carddemo.json) registram os
SHAs exatos da baseline e os digests dos artefatos finais. O frontend base é
`c43b1410ac9235d906ccc90392d5f3bf82399ac1`; o lower base é
`19cf1fe9a59997f48868a51332487417753e6fce`. AIR Java
`7d77330099f46117281fdcbb08304e20d68f5672`, AIR spec
`fc229ef64eadf26c9ca093a544dad2928ae17dc2` e CFG
`ae3b23d9e853f15fff64ebb49fa40be9391670ba` permanecem fixos no E2E.
O CardDemo está em `59cc6c2fd7ebd7ef7925cad552a01a4b8b6e4d5e`.

O FAST do lower usa seu pin de biblioteca próprio, preservado no source lock:
`air-java@851931b68f420cba09fdcaca1cbdebffeba7189a`. Isso é distinto do runtime
E2E acima. O novo pin do produtor SP fica registrado no PR consumidor.

Os comandos exatos, logs, outputs brutos e runtimes congelados permanecem no
workspace em `.gap-reconciliation-20261003/active-evidence/corpus-final/`.
`results.json` registra os quatro comandos e códigos de saída por fonte.
O runner reexecuta os comandos da pesquisa anterior com apenas frontend/lower
substituídos, conserva as opções do analisador e verifica o hash de cada fonte.
A avaliação anterior permanece em `.gap-reconciliation-20261003/evidence/programs/`.
Com esses artefatos preservados, a comparação pode ser repetida usando:

```bash
python3 scripts/verify-active-gaps.py \
  --run ../active-evidence/corpus-final/results.json \
  --assessment-root ../evidence/programs \
  --output ../active-evidence/review-verification.json
```

Não foi necessário executar novamente o scanner: seus resultados da descoberta
anterior são baseline; a comparação desta mudança verifica todos os produtos
atuais diretamente contra a baseline do analisador. O script de comparação
portável foi verificado nos três fontes de smoke após parametrização; a lógica
idêntica executou os 73 fontes na comparação final.

## Integração

O SP passa para **2.63.0**, pois a obrigação de diagnóstico localizado mudou.
O decoder do lower admite essa versão exata e continua rejeitando versões futuras.
A integração deve coordenar o consumidor e o produtor; disponibilizar primeiro o
lower capaz de admitir 2.63 evita rejeição durante a troca do frontend.
Os PRs permanecem para revisão, sem merge automático.
