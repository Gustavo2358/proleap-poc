# Política de testes semânticos

Esta política descreve como produzir oráculos que distinguem a implementação semântica correta de alternativas plausíveis, porém erradas.

## Derivação do oracle

Para uma mudança semântica, seguir esta ordem:

```text
regra semântica → classes de equivalência → casos adversariais → comportamento esperado → implementação
```

Não derivar o teste a partir do comportamento que a implementação acabou de produzir. Criar a menor fixture útil para reproduzir a classe de problema e preservar também regressão do exemplo original quando ela tiver valor.

## Classes de equivalência e adversariais

Cobrir classes que alteram a decisão: inexistência, candidato único, múltiplos candidatos, namespace incompatível, qualificação válida/inválida, shadowing, aninhamento, variação de caixa, input ausente, construção não suportada e ambiguidade. Para resolução, considerar também visibilidade COBOL, `GLOBAL`, `COMMON`, FILE, índices e nomes de procedimento conforme o domínio suportado.

Todo conserto semântico deve incluir pelo menos um caso adversarial contra o atalho óbvio. Por exemplo, um teste de visibilidade `GLOBAL` deve tentar homônimos locais, múltiplos níveis de nesting e candidatos incompatíveis, não somente o caminho feliz.

## Propriedades e metamorfismo

Quando houver uma propriedade geral, complementar exemplos com testes gerados ou loops geradores locais, se isso trouxer valor proporcional. Propriedades relevantes incluem canonicalização case-insensitive, determinismo, imutabilidade da AST, identidade de entidade resolvida e provenance válida.

Relações metamórficas são úteis quando um snapshot inteiro é caro: alterar caixa de identificadores, área de sequência irrelevante, comentário semanticamente irrelevante, qualificação explicitamente não ambígua ou declaração não relacionada não deve mudar a decisão que a regra COBOL preserva. Declarar a relação antes de implementá-la como teste.

## Oráculos independentes e força do teste

Quando disponível, usar compilador/documentação confiável, algoritmo exato mais lento em entradas pequenas ou modelo independente. A implementação otimizada não é seu próprio oracle.

Alta cobertura não prova semântica. Para algoritmos críticos, verificar se a suíte detectaria mutações como trocar ordem de lookup, ignorar qualifier, colapsar ambiguidade, remover busca de ancestor ou filtrar namespace errado. Ferramenta de mutation testing só deve ser adicionada se trouxer benefício real.

## Challenge pass

Depois da implementação, fazer revisão independente orientada à falsificação:

- procurar premissa de corpus, nesting ou shadowing não coberto;
- verificar colisões de namespace e conjunto de candidatos incompleto;
- procurar forma não suportada convertida em sucesso;
- verificar dependência da ordem, heurística textual ou atalho algorítmico;
- confirmar que fixture prova uma classe e não apenas a instância reportada.

Se houver contraexemplo, corrigir a regra geral e adicioná-lo à regressão. Evals duráveis serão catalogados em `docs/evals/` sem duplicar seus asserts.
