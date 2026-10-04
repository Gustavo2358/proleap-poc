# Gaps ativos — Semantic Product 2.63

O produtor publica uma única lista em `Semantic Product.gaps`. O painel `gaps.html`
apresenta essa mesma lista por unidade, com frequência, busca e proveniência.
`gaps-data.js` usa os mesmos DTOs do transporte do SP; não interpreta provas nem
classifica registros como superados. Não há artefato separado de reconciliação.

## Obrigações atuais

- As quatro restrições de isolamento/corpo linear de PERFORM deixaram de ser
  emitidas como gaps. A elegibilidade das especializações continua explícita em
  seus fatos, independentemente de uma lista de diagnósticos estar vazia.
- A posição estrutural usa a ocorrência e a região da topologia atual. Quando
  essa prova falta, o produtor publica `CONTROL_MEMBERSHIP_NOT_PROVEN`.
- Um statement observado com efeito `NO_OP` e controle local fechado não precisa
  do diagnóstico genérico de semântica ausente. Outros efeitos conservam o gap.
- `textPredicate` validado satisfaz a obrigação de semântica do predicado. Isso
  não fecha o perfil completo do IF, seus braços, storage ou valores de runtime.
- PERFORM sem outros diagnósticos e sem prova atual de invocação/retomada recebe
  `PERFORM_CONTROL_NOT_PROVEN`.

O fechamento de uma prova verifica suas dependências transitivas. Uma prova
`PARTIAL_UNKNOWN`, `CONTROL_POSSIBILITY`, ausente ou pertencente a outra
publicação não satisfaz a obrigação. O índice é local à unidade.

## Contrato e limites

SP **2.63.0** mantém o formato dos fatos e muda a obrigação dos gaps localizados:
provas positivas atuais podem satisfazê-la. O consumidor precisa admitir essa
versão e validar as provas; a ausência de gap não certifica suporte.

`coverage` e readiness de fatos continuam descrevendo suas capacidades locais.
Uma especialização parcial pode coexistir com controle composicional provado.
O contador de gaps não determina o estado final da análise. Input ausente,
operandos desconhecidos, layout físico, resultados CICS e runtime continuam
explícitos nos contratos correspondentes. Candidatos conhecidos permanecem
preservados quando há remainder desconhecido.

A tela de resolução nominal mantém referências, candidatos e classificações
externas. Ela deixa de publicar o contador agregado antigo e a afirmação global
`dependencyAnalysisReady`. `inputDiagnostics` conserva os erros reais de entrada,
incluindo cada COPY ausente; esses registros não são reclassificados como gaps
semânticos. A AST continua mostrando a cobertura da construção da AST.

O audit antigo de readiness saiu da aplicação e permanece somente como fixture
de teste da fronteira. `ResolutionAnalysisReport` ainda fornece fatos de entrada
e binding ao SP; suas limitações de efeitos continuam locais ao contrato.

Leitores históricos fechados e especializações que ainda produzem fatos válidos
continuam disponíveis. A remoção abrange a via ativa de diagnósticos duplicados.

## Regressões

`ActiveSemanticGapsTest` verifica ausência dos códigos retirados, conservação de
limitações reais e rejeição quando a prova é removida. `ZstdArtifactTest` compara
os gaps do painel com o SP. O consumidor executa `ActiveGapsSuite` com um produto
real 2.63, validação AIR e mutações negativas.

[Validação no CardDemo](../validation/active-gaps.md).
