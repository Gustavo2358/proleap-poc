# Política de testes semânticos

Esta política descreve como produzir oráculos que distinguem a implementação semântica correta de alternativas plausíveis, porém erradas.

## Derivação do oracle

Para uma mudança semântica, seguir esta ordem:

```text
regra semântica → classes de equivalência → casos adversariais → comportamento esperado → implementação
```

Não derivar o teste a partir do comportamento que a implementação acabou de produzir. Criar a menor fixture útil para reproduzir a classe de problema e preservar também regressão do exemplo original quando ela tiver valor.

## Baselines de corpus e cardinalidade

Totais globais de nós, profundidade, scopes, símbolos, referências ou gaps em um
programa grande são telemetria e detectores de mudança, não oráculos semânticos
isolados. Eles podem permanecer visíveis nos relatórios, mas não devem bloquear
uma mudança apenas por congelarem a forma incidental de uma representação.

Contagem exata é apropriada quando decorre diretamente do contrato de uma fixture
pequena e controlada, como a quantidade explícita de CALLs ou ocorrências nela.
Em corpus amplo, preferir categorias semânticas, relações, estados de coverage,
readiness, provenance e propriedades de consistência. Qualquer migração de
baseline continua exigindo diff explicado e revisão da evidência.

Snapshots de corpus devem ser lidos de forma estruturada. Texto de console
destinado a humanos é telemetria, não interface semântica, e fragmentos da
serialização não são oráculos: ambos confundem apresentação com contrato e podem
aceitar substrings ou rejeitar apenas uma reordenação de campos. Para artefatos
JS/JSON, validar campos pelo nome, reconciliar contagens publicadas com os
inventários efetivos e verificar chaves estrangeiras entre os produtos do
pipeline. O loader deve falhar fechado diante de wrapper, JSON ou schema
inesperado, sem executar o conteúdo do artefato.

Uma chave local nunca deve ser tratada como identidade global: toda reconciliação
entre produtos usa a identidade declarada pelo artefato. Em particular,
occurrences de resolução são identificadas por `(unitId, occurrenceId)`, pois o
contador local reinicia em cada `ProgramUnit`.

Contagens derivadas do próprio inventário, como `entries.length` igual a
`collectedReferences` ou a soma dos estados igual ao total de occurrences, são
propriedades de consistência e não baselines de cardinalidade: permanecem válidas
quando o corpus cresce ou a representação ganha nós legítimos.

## Classes de equivalência e adversariais

Cobrir classes que alteram a decisão: inexistência, candidato único, múltiplos candidatos, namespace incompatível, qualificação válida/inválida, shadowing, aninhamento, variação de caixa, input ausente, construção não suportada e ambiguidade. Para resolução, considerar também visibilidade COBOL, `GLOBAL`, `COMMON`, FILE, índices e nomes de procedimento conforme o domínio suportado.

Todo conserto semântico deve incluir pelo menos um caso adversarial contra o atalho óbvio. Por exemplo, um teste de visibilidade `GLOBAL` deve tentar homônimos locais, múltiplos níveis de nesting e candidatos incompatíveis, não somente o caminho feliz.

## Propriedades e metamorfismo

Quando houver uma propriedade geral, complementar exemplos com testes gerados ou loops geradores locais, se isso trouxer valor proporcional. Propriedades relevantes incluem canonicalização case-insensitive, determinismo, imutabilidade da AST, identidade de entidade resolvida e provenance válida.

Relações metamórficas são úteis quando um snapshot inteiro é caro: alterar caixa de identificadores, área de sequência irrelevante, comentário semanticamente irrelevante, qualificação explicitamente não ambígua ou declaração não relacionada não deve mudar a decisão que a regra COBOL preserva. Declarar a relação antes de implementá-la como teste.

## Oráculos independentes e força do teste

Quando disponível, usar compilador/documentação confiável, algoritmo exato mais lento em entradas pequenas ou modelo independente. A implementação otimizada não é seu próprio oracle.

Alta cobertura não prova semântica. Para algoritmos críticos, verificar se a suíte detectaria mutações como trocar ordem de lookup, ignorar qualifier, colapsar ambiguidade, remover busca de ancestor ou filtrar namespace errado. Ferramenta de mutation testing só deve ser adicionada se trouxer benefício real.

O perfil Maven `mutation-adversarial` executa PIT de forma deliberadamente focalizada sobre `AstBuilder` e `ReferenceOccurrenceCollector`, usando os oráculos de traversal tipado, expressão estruturada e resolução DATA/INDEX. Ele é uma avaliação sob demanda, não um threshold global nem parte dos gates estáveis:

```bash
mvn -Pmutation-adversarial test-compile org.pitest:pitest-maven:mutationCoverage
```

O resultado relevante é se os mutantes nas decisões sob investigação são mortos. Sobreviventes em código fora do slice indicam lacunas futuras, mas não devem ser ocultados nem convertidos em score de vaidade.

## Challenge pass

Depois da implementação, fazer revisão independente orientada à falsificação:

- procurar premissa de corpus, nesting ou shadowing não coberto;
- verificar colisões de namespace e conjunto de candidatos incompleto;
- procurar forma não suportada convertida em sucesso;
- verificar dependência da ordem, heurística textual ou atalho algorítmico;
- confirmar que fixture prova uma classe e não apenas a instância reportada.

Se houver contraexemplo, corrigir a regra geral e adicioná-lo à regressão. Evals duráveis serão catalogados em `docs/evals/` sem duplicar seus asserts.
