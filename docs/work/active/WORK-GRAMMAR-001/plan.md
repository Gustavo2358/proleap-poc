## Fatiamento

1. Mapear derivações, decisões compartilhadas, comportamento ANTLR e comparação upstream sem alterar produção.
2. Criar testes sintéticos de caracterização da parse tree e demonstrar as falhas pré-correção.
3. Emitir veredito sobre generalidade e prosseguir somente se positivo.
4. Aplicar a menor correção localizada e validar parse tree, AST e ocorrências.
5. Executar gates e regressões disponíveis; consolidar conhecimento durável e encerrar o work item.

## Dependências

Gramática ANTLR vendorizada, frontend de testes existente, AST e pipeline de referências.

## Superfície arquitetural provável

Uma regra compartilhada de expressão/valor em `Cobol.g4`, condicionada à demonstração da análise transitiva.

## Migrações requeridas

Nenhuma prevista; geração ANTLR ocorre pelo build Maven.

## Artefatos esperados

Teste parametrizado sintético, evidência pré/pós-correção, alteração gramatical mínima se aprovada e resultados dos gates.
