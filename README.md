# COBOL Structure Atlas

Explorador visual da jornada `parse tree → AST → tabela de símbolos → resolução de referências` para programas COBOL. O projeto gera páginas estáticas que podem ser abertas localmente, sem servidor nem dependências web externas.

Esta página é uma porta de entrada de uso. Os contratos semânticos, as políticas de engenharia e as decisões arquiteturais estão na [documentação canônica](docs/index.md).

## Requisitos

- JDK 17 ou superior;
- Maven 3.9 ou superior.

## Gerar e abrir

Para gerar a jornada do programa padrão:

```bash
./run.sh
```

Abra os artefatos em `dist/`:

- `index.html`: parse tree;
- `ast.html`: AST semântica;
- `symbols.html`: tabela de símbolos;
- `resolution.html`: bindings nominais e cobertura conservadora.

As páginas funcionam via `file://`. A navegação permite seguir um elemento semântico até sua origem na parse tree.

Também é possível executar diretamente pelo Maven:

```bash
mvn compile exec:java
```

Para escolher fonte, copybooks e diretório de saída:

```bash
mvn exec:java \
  -Dexec.args="--source corpus/cbl/COACTUPC.cbl --copybooks corpus/cpy,corpus/cpy-bms --output dist"
```

`--copybooks` aceita uma lista de diretórios separada por vírgulas. Quando um
copybook existe em mais de um diretório, o primeiro diretório informado tem
precedência.

## Exemplos

`COACTUPC.cbl` é o caso canônico do corpus e a melhor referência para navegar pela jornada completa.

Para gerar uma visualização independente do programa com muitos `CALL`s estáticos:

```bash
mvn compile exec:java \
  -Dexec.args="--source corpus/cbl/CBSTM03A.CBL --copybooks corpus/cpy --output dist-cbstm03a"
```

`CBSTM03D.CBL` é uma variante didática com `CALL`s dinâmicos. O resolvedor associa o uso à variável-alvo; a descoberta dos valores possíveis em runtime é intencionalmente uma fronteira futura de análise de fluxo. Veja o [contrato de resolução](docs/domain/reference-resolution.md).

```bash
mvn compile exec:java \
  -Dexec.args="--source corpus/cbl/CBSTM03D.CBL --copybooks corpus/cpy --output dist-cbstm03d"
```

## Logging

A execução normal registra apenas o lifecycle essencial. Para diagnosticar as fases do pipeline, habilite `DEBUG`:

```bash
mvn exec:java -DANALYZER_LOG_LEVEL=DEBUG
```

As políticas de observabilidade e os demais níveis de logger estão em [Observabilidade](docs/engineering/observability-policy.md).

## Verificação

O harness organiza as verificações por velocidade e profundidade:

```bash
./scripts/harness/check-fast.sh
./scripts/harness/check-semantic.sh
./scripts/harness/check-full.sh
```

Consulte [Gates de verificação](docs/engineering/gates.md) para escopo, ordem e comandos legados.

## Documentação

- [Índice da documentação](docs/index.md): ponto de partida para pessoas.
- [Arquitetura](docs/architecture/index.md): pipeline e fronteiras entre camadas.
- [Contratos de domínio](docs/domain/index.md): AST, símbolos, resolução, source map e cobertura.
- [Engenharia](docs/engineering/index.md): invariantes, testes, logging e gates.
- [Catálogo de evals](docs/evals/index.md): evidências executáveis.
- [Trabalho ativo](docs/work/index.md): estado e escopo da implementação em curso.
- [Instruções para agentes](AGENTS.md): roteamento de contexto para mudanças no repositório.

## Estrutura do repositório

```text
corpus/                  programas COBOL e copybooks do corpus
src/main/antlr4/         gramáticas ANTLR
src/main/java/           pipeline, domínio semântico e exportadores
src/main/resources/web/  interface estática da jornada visual
docs/                    documentação canônica
scripts/harness/         gates de verificação
dist/                    saída gerada, pronta para abrir
```
