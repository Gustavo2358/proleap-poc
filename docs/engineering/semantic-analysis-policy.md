# Política de análise semântica

Esta política governa mudanças não triviais em análise semântica. Ela não substitui a regra COBOL aplicável nem o contrato de domínio do subsistema.

## Fonte de corretude

Testes, corpus, snapshots e artefatos gerados são evidência de corretude, não a definição isolada da linguagem. Uma mudança precisa derivar de ao menos uma fonte verificável: semântica do dialeto COBOL configurado, especificação do projeto, invariante arquitetural ou requisito de produto aprovado.

Antes de alterar produção, identificar a regra, domínio de entradas, classes semânticas, premissas, algoritmo, exatidão e evidência que diferencie a solução correta de um atalho plausível. Classificar premissas relevantes como `SPECIFICATION_GUARANTEED`, `LANGUAGE_GUARANTEED`, `ARCHITECTURE_GUARANTEED`, `OBSERVED_IN_CURRENT_CORPUS_ONLY` ou `UNCERTAIN`. As duas últimas categorias não podem virar regra de produção silenciosa.

## Algoritmos, incerteza e completude

Ordem de preferência:

1. algoritmo semântico exato;
2. aproximação conservadora formalmente caracterizada;
3. heurística explicitamente documentada.

Uma heurística deve declarar por que o algoritmo exato não é viável, domínio de uso, soundness, completeness, falsos positivos/negativos e representação de incerteza. Ela nunca deve se apresentar como resultado exato.

Quando a análise não pode concluir, preservar estados como `UNKNOWN`, `UNRESOLVED`, `UNSUPPORTED`, `AMBIGUOUS` ou `INCOMPLETE`. Uma construção que não foi compreendida não equivale a ausência de efeito, referência, dependência ou declaração. Informação parcialmente conhecida não pode apagar informação já conhecida.

Em resolução futura de dependências dinâmicas, conservar o conjunto de targets estaticamente possíveis e indicar separadamente o remainder dinâmico quando ele existir; não colapsar o resultado em um único target nem descartar os targets conhecidos.

Para algoritmos não triviais, registrar de forma curta: domínio de entrada, invariante, argumento de soundness, limite de completeness, terminação e ordem de complexidade. O objetivo é revelar premissas ocultas, não exigir prova formal.

## Fronteiras de análise

Preservar a separação descrita no [pipeline arquitetural](../architecture/pipeline.md):

- parse tree representa sintaxe reconhecida pela gramática;
- AST é estrutura semântica, sem bindings, resultados de resolução, CFG ou dataflow;
- tabelas de símbolos representam declarações, escopos, namespaces e visibilidade; não inferem valores de runtime;
- resolução nominal associa ocorrências a entidades conforme regras COBOL; não calcula reaching definitions ou valores possíveis;
- linguagens embarcadas permanecem opacas até análise dedicada, sem regex oportunista vendida como análise completa.

Não usar a conveniência de uma camada para antecipar conhecimento que pertence a uma fase posterior. Em particular, `CALL WS-TARGET` pode fazer binding nominal de `WS-TARGET`; nomes de programas que a variável poderá conter dependem de CFG e dataflow futuros.

## Regras de implementação

- Preferir estrutura de grammar, parse tree e AST a substring, regex ou texto achatado quando a informação já existe estruturalmente.
- Não confundir containment estrutural, organização lexical, namespace, visibilidade COBOL, resolução e ownership semântico.
- Não transformar corpus ou fixture em lógica de produção. Cada branch deve representar uma classe geral de programas COBOL válidos.
- Não enfraquecer fixture, gramática ou artefato gerado apenas para tornar uma implementação verde. Primeiro determinar se o defeito é de produção, teste, fixture, gramática, fronteira arquitetural ou requisito ambíguo.
- Uma correção mínima reduz a superfície arquitetural alterada, mas implementa a regra geral; não trata somente o exemplo que falhou.
- Fora do domínio explicitamente suportado, preferir fronteira pequena e exata com `UNSUPPORTED` a cobertura ampla e heurística.

## Provenance, ambiguidade e diagnósticos

Transformações não podem recriar provenance de identidade sobre texto já transformado. Resultados aproximados devem continuar distinguíveis de resultados exatos. Mudanças em normalização, COPY, preprocessing, AST, ocorrências ou resolução precisam preservar localização e cadeia de origem coerentes.

Ambiguidade é informação: não selecionar primeiro candidato, candidato ordenado ou candidato conveniente sem uma regra da linguagem que o determine.

Diagnósticos são resultados semânticos. Eles devem comunicar a construção, origem, regra ou motivo, candidatos conhecidos quando aplicável e se o estado é ambíguo, não suportado, não resolvido ou incompleto. Devem ser determinísticos e testáveis.

## Revisão semântica

Antes de concluir, desafiar a solução: qual entrada COBOL válida quebra a premissa? A regra veio de especificação ou do corpus? Alguma incerteza virou ausência? A solução usa regex, nearest-match ou ordem de declaração no lugar de semântica? Está reivindicando conhecimento de uma fase futura? O algoritmo continuaria correto se todos os fixtures atuais fossem trocados?

Comentários em produção devem explicar regra, invariante ou motivo da postura conservadora; não o nome do teste ou a história de um incidente.

## Guardas do repositório

As gramáticas vendorizadas preservam autoria, copyright e atribuições. Alterá-las só é correto quando a gramática é a origem comprovada do defeito ou quando a superfície suportada muda deliberadamente; fixtures e grammar não são ajustadas apenas para satisfazer produção. `scripts/verify-naming.sh` protege a identidade atual sem remover referências exigidas pelas atribuições das gramáticas e notices, nem reescrever fontes históricas ou transitórias em `docs/history/` e `specs/`. O gate inspeciona somente arquivos rastreados ou novos não ignorados; metadados locais não fazem parte da identidade do projeto.
