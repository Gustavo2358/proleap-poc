# Preprocessing

## Propósito e escopo

`PreprocessorEngine` aplica a política fechada do frontend preprocessador, expande COPYs e transporta o `SourceMap` até o lexer COBOL.

## Entradas e saídas

- **Entrada:** fonte normalizada e mapeada, binding das gramáticas e `CopybookLibrary`.
- **Saída:** `Outcome` imutável com texto, mapa composto, diagnostics, contagens, compiler options e modos `PGMNAME`, `DYNAM` e `DLL`.

## Políticas atuais

Cada alternativa top-level de `CobolPreprocessor.startRule` possui classificação explícita:

- `COPY` é expandido, incluindo nesting e `REPLACING` suportado;
- `EJECT`, `SKIP1`, `SKIP2`, `SKIP3` e `TITLE` são branqueados preservando quebras de linha;
- compiler options são extraídas e transportadas para a policy de resolução;
- EXECs são preservados por fronteira opaca para o parser COBOL;
- texto COBOL comum é mantido;
- `REPLACE` top-level e `REPLACE OFF` permanecem `UNSUPPORTED` e falham antes do parser COBOL.

COPY ausente, cíclico ou com erro de I/O produz placeholder mapeado e diagnostic. Ausência de copybook mantém a execução observável como incompleta; não equivale a COPY vazio.

## Provenance e determinismo

Expansões compõem os segmentos existentes e acrescentam `CopyFrame`; `REPLACING`
preserva origem aproximada. O resultado não recria identity map. Edições
top-level não sobrepostas são aplicadas em lote, mantendo a ordem estrutural do
fonte e dos diagnostics sem reconstruir o mapa a cada edição.

## Complexidade

O processamento percorre a parse tree e resolve COPYs pelo repositório configurado. Ciclos são detectados pela cadeia ativa; expansão não deve revarrer a codebase inteira.

## Fronteiras explícitas

O preprocessor não resolve símbolos COBOL, não interpreta payload de SQL/CICS/SQLIMS e não inventa configuração ausente. Modos não especificados permanecem valores explícitos na policy posterior.

## Evidência executável

`PreprocessorEnginePolicyTest`, `SourceNormalizationPreprocessingIntegrationTest`, `SourceProvenanceTest` e regressão do normalizador.

## Relações

Evals: EVAL-PRE-001, EVAL-PRE-002 e EVAL-PROV-001. Invariantes: INV-PROV-001, INV-PROV-002, INV-COV-001 e INV-COV-002. ADRs: ADR-0002, ADR-0007, ADR-0008 e ADR-0009.
