# Política inicial de resolução de referências COBOL

## Identidade

- policy ID: `cobol-explorer/ibm-enterprise-compatible`;
- versão: `1.0.0`;
- grammar frontend: `Cobol.g4` e `CobolPreprocessor.g4` versionadas no projeto;
- `QUALIFY` default: `UNSPECIFIED` — nenhuma variante será presumida.

Esta política é um contrato de classificação da Fase 1. Ela ainda não executa
binding por si própria. As regras DATA/CONDITION/INDEX passaram a ser
implementadas por TDD na Fase 4; os demais namespaces permanecem em fases
separadas.

## Fontes semânticas primárias

- IBM Enterprise COBOL — [Qualification](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=reference-qualification);
- IBM Enterprise COBOL — [References to PROCEDURE DIVISION names](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=reference-references-procedure-division-names);
- IBM Enterprise COBOL — [Scope of names](https://www.ibm.com/docs/en/cobol-zos/6.4?topic=programs-scope-names);
- IBM Enterprise COBOL — [Calling nested COBOL programs](https://www.ibm.com/docs/en/cobol-aix/5.1.0?topic=subprograms-calling-nested-cobol-programs).

## Regras aprovadas para implementação futura

1. Nomes são comparados de forma case-insensitive por forma canônica, mantendo
   a grafia original para diagnóstico e navegação.
2. Qualificadores DATA seguem ancestry estrutural na ordem de dentro para fora;
   `IN` e `OF` são semanticamente equivalentes.
3. Qualificação por FILE consulta o namespace/entidade FILE.
4. `QUALIFY(EXTEND)` somente é aplicado quando configurado explicitamente.
5. Procedure names são locais ao program unit; paragraph pode ser qualificado
   somente por section.
6. GLOBAL, COMMON, nesting e shadowing seguem busca por program unit, nunca uma
   busca global conveniente sobre toda a codebase.
7. Programa externo somente é ligado quando um catálogo explícito é fornecido.
8. Forma aceita pela grammar, mas ainda sem regra segura, resulta em
   `UNSUPPORTED`; input ausente resulta em motivo conservador próprio.

## Semântica de QUALIFY implementada na Fase 4

- `STANDARD`: todos os candidatos que correspondem à sequência ordenada de
  qualifiers permanecem válidos; mais de um candidato resulta em `AMBIGUOUS`.
- `EXTEND`: quando exatamente um candidato corresponde de forma totalmente
  qualificada e os demais somente de forma parcial, o candidato totalmente
  qualificado é único. A regra equivalente para um único nível 01 também é
  aplicada.
- `UNSPECIFIED`: se STANDARD e EXTEND produziriam decisões diferentes, o
  resultado é `UNSUPPORTED/UNSUPPORTED_DIALECT_OPTION`; a implementação não
  escolhe uma opção silenciosamente.
- `IN` e `OF` continuam equivalentes, preservando-se a grafia na ocorrência.
- qualifiers podem omitir níveis intermediários, mas precisam respeitar a
  ordem estrutural de dentro para fora.

Essas regras seguem a documentação IBM de
[Qualification](https://www.ibm.com/docs/en/cobol-zos/6.3?topic=reference-qualification),
inclusive a distinção de `QUALIFY(EXTEND)` para match totalmente qualificado.

## Namespaces implementados na Fase 5

- PROCEDURE é estritamente local ao program unit. Paragraph qualificado precisa
  pertencer à section escrita; referências não atravessam programas.
- FILE aponta para a entidade formada pelas declarações SELECT e FD/SD. Uma
  entidade conserva todas as declarações e o texto de ASSIGN sem produzir
  ambiguidade artificial SELECT-versus-FD.
- PROGRAM consulta primeiro programas internos visíveis: filhos diretos e
  siblings/descendentes permitidos por COMMON. Outros top-level e siblings não
  COMMON não são escolhidos por busca global.
- Programa externo só é candidato quando `ExternalProgramCatalog` é fornecido.
  Catálogo ausente resulta em `EXTERNAL_CATALOG_NOT_PROVIDED`; catálogo presente
  e vazio resulta em `DECLARATION_NOT_FOUND`.
- CALL dinâmico mantém kind DATA e liga somente sua variável. Não há resolução
  de constante, reaching definitions ou inferência de programas possíveis.
- Contextos gramaticais que admitem mais de um namespace preservam
  `admissibleKinds`. Por exemplo, parâmetro BY REFERENCE de CALL pode admitir
  DATA e FILE; candidatos de ambos são combinados, sem preferência por nome ou
  corpus.

## Guarda não heurística

`ReferenceResolutionManifest` expande uma entrada para cada uma das 628 regras
do manifesto do frontend. Origens de referência, qualifiers, built-ins e
relações declarativas possuem overrides exatos. As demais regras herdam apenas
uma classificação conservadora de container, lacuna, boundary ou
não-referência. Nenhuma classificação consulta arquivos do corpus ou procura
palavras no texto COBOL.
