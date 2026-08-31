# Preprocessing

## Propósito e escopo

`PreprocessorEngine` aplica a política fechada do frontend preprocessador, expande COPYs e transporta o `SourceMap` até o lexer COBOL.

## Entradas e saídas

- **Entrada:** fonte normalizada e mapeada, binding das gramáticas e `CopybookLibrary`.
- **Saída:** `Outcome` imutável com texto, mapa composto, diagnostics tipados, contagens derivadas, compiler options e modos `PGMNAME`, `DYNAM` e `DLL`.

## Políticas atuais

Cada alternativa top-level de `CobolPreprocessor.startRule` possui classificação explícita:

- `COPY` é expandido, incluindo nesting e `REPLACING` suportado;
- `EJECT`, `SKIP1`, `SKIP2`, `SKIP3` e `TITLE` são branqueados preservando quebras de linha;
- compiler options são extraídas e transportadas para a policy de resolução;
- EXECs são preservados por fronteira opaca para o parser COBOL;
- texto COBOL comum é mantido;
- `REPLACE` top-level e `REPLACE OFF` permanecem `UNSUPPORTED` e falham antes do parser COBOL.

COPY ausente, cíclico ou com erro de I/O produz placeholder mapeado e diagnostic. Para membro não encontrado, `Diagnostic.Code.UNRESOLVED_COPY` é a identidade semântica estruturada; `Outcome.unresolved()` é derivado desses fatos, que preservam nome solicitado e localização em ordem determinística. A mensagem humana continua útil, mas seu wording não participa de contagem, composição ou geração de gaps. Ausência de copybook mantém a execução observável como incompleta; não equivale a COPY vazio nem exige interromper fases posteriores quando o placeholder ainda permite construir seus produtos coerentemente. COPY cíclico e falha de I/O conservam a política anterior e não pertencem a esse fallback.

## Provenance e determinismo

Expansões compõem os segmentos existentes e acrescentam `CopyFrame`; `REPLACING`
preserva origem aproximada. O resultado não recria identity map. Edições
top-level não sobrepostas são aplicadas em lote, mantendo a ordem estrutural do
fonte e dos diagnostics sem reconstruir o mapa a cada edição.

## Complexidade

O processamento percorre a parse tree e resolve COPYs pelo repositório configurado. Ciclos são detectados pela cadeia ativa; expansão não deve revarrer a codebase inteira.

## Fronteiras explícitas

O preprocessor não resolve símbolos COBOL, não interpreta payload de SQL/CICS/SQLIMS, não busca membros fora dos diretórios configurados e não inventa configuração ausente. Modos não especificados permanecem valores explícitos na policy posterior. A composição posterior, não o preprocessor, decide quais fases possuem pré-requisitos estruturais sob input incompleto.

## Evidência executável

`PreprocessorEnginePolicyTest`, `SourceNormalizationPreprocessingIntegrationTest`, `SourceProvenanceTest` e regressão do normalizador.

## Relações

Evals: EVAL-PRE-001, EVAL-PRE-002, EVAL-PROV-001 e EVAL-COV-003. Invariantes: INV-PROV-001, INV-PROV-002, INV-COV-001, INV-COV-002 e INV-COV-003. ADRs: ADR-0002, ADR-0007, ADR-0008 e ADR-0009.
