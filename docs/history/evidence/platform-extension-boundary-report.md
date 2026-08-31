# Fronteira entre Semântica COBOL e Extensões de Plataforma

## Relatório de problema e proposta arquitetural para CICS, IMS, DB2 e extensões organizacionais

## 1. Resumo executivo

O analisador COBOL do projeto precisa distinguir duas classes de conhecimento que, embora apareçam no mesmo arquivo fonte, pertencem a camadas semânticas diferentes:

1. **semântica canônica da linguagem COBOL** — nomes, declarações, referências, `CALL`, `PERFORM`, `GO TO`, expressões, table calls, escopos, resolução de referências, CFG e dataflow;
2. **semântica de extensões externas** — CICS, IMS, DB2, recursos providos por copybooks de sistema e convenções organizacionais como GRBE.

O problema que expôs essa fronteira foi `DFHRESP(...)`/`DFHVALUE(...)`. Em fonte processado pelo tradutor CICS, essas formas são intrínsecos do ambiente CICS. Porém a mesma sequência de tokens também pode ser interpretada pela gramática COBOL como um acesso subscrito normal, por exemplo `DFHRESP(IDX)`, caso exista um data-name chamado `DFHRESP`.

A investigação demonstrou que **não existe uma interpretação sintática única correta sem conhecer o modo real de compilação**. O mesmo texto pode representar COBOL puro ou uma construção CICS. Como o analisador não dispõe, de forma geral, das opções reais de compilação, não é correto tornar a gramática globalmente CICS-aware nem forçar `DFHRESP` a deixar de ser um `cobolWord`.

A proposta deste documento é manter o **core COBOL semanticamente puro** e introduzir uma camada explícita de extensões de plataforma. Essa camada deve ser composta por quatro capacidades independentes:

| Peça arquitetural | Momento da pipeline | Problema resolvido | Exemplo |
|---|---|---|---|
| **External Symbol Provider** | antes da resolução | símbolos reais fornecidos pelo ambiente e não declarados no fonte | `EIBCALEN`, `EIBAID` |
| **Unresolved Reference Classifier** | depois da resolução COBOL | construções que parecem COBOL, mas cuja interpretação COBOL falhou e possuem uma explicação externa plausível | `DFHRESP(NORMAL)`, `DFHVALUE(...)` |
| **Semantic Extractor** | após AST/resolução e, quando necessário, após dataflow | significado de statements/chamadas específicas de plataforma | `EXEC CICS LINK`, `EXEC SQL CALL`, `CALL MONITOR USING ...` |
| **Control-Flow Semantics Provider** | construção/enriquecimento do CFG | efeitos de controle de fluxo que o core COBOL não conhece | `EXEC CICS XCTL`, `EXEC CICS RETURN` |

A ideia central é simples:

> **Primeiro o analisador tenta explicar o programa usando apenas semântica COBOL canônica. Depois, módulos externos podem contribuir símbolos, reclassificar incertezas ou acrescentar semântica de plataforma sem alterar o significado do core COBOL.**

Essa organização permite tratar `DFHRESP` de forma conservadora hoje e, ao mesmo tempo, cria a base para CICS, IMS, DB2 e GRBE sem transformar o analisador em uma coleção de exceções hardcoded.

---

## 2. Contexto do analisador

A pipeline conceitual do frontend é:

```text
Source
  ↓
Source normalization / preprocessing
  ↓
ANTLR
  ↓
Parse tree
  ↓
Semantic AST
  ↓
Symbol table
  ↓
Reference resolution
  ↓
CFG
  ↓
Dataflow analyses
  ↓
Semantic/dependency facts
```

Essa pipeline possui uma propriedade arquitetural importante: **cada estágio deve acrescentar conhecimento sem misturar responsabilidades**.

A parse tree responde principalmente a perguntas sintáticas. A AST deve preservar a estrutura semântica relevante. A symbol table representa declarações e escopos. A resolução liga ocorrências a símbolos. O CFG e o dataflow tratam fluxo e valores possíveis.

O problema surge quando um mesmo arquivo COBOL contém construções cujo significado não pertence à linguagem COBOL isoladamente, mas ao ambiente em que o programa é traduzido, compilado ou executado.

CICS é o exemplo mais evidente, mas o mesmo padrão aparece em outras tecnologias:

- CICS;
- IMS;
- DB2 e SQL embarcado;
- símbolos de sistema fornecidos por copybooks IBM;
- convenções proprietárias da organização, como GRBE;
- futuras extensões de plataforma ainda não modeladas.

O objetivo não é ensinar o core COBOL a conhecer todas essas plataformas. O objetivo é criar uma fronteira onde esse conhecimento possa ser plugado de forma controlada.

---

## 3. O problema que revelou a fronteira: `DFHRESP` e `DFHVALUE`

Considere:

```cobol
IF WS-RESP-CD = DFHRESP(NORMAL)
    CONTINUE
END-IF.
```

Para CICS, `DFHRESP(NORMAL)` é uma construção conhecida do tradutor CICS.

Entretanto, do ponto de vista puramente sintático, a mesma forma possui a aparência de um acesso subscrito COBOL:

```cobol
TABLE-NAME(INDEX)
```

Por isso uma gramática COBOL pode produzir algo semanticamente equivalente a:

```text
DataReference
base = DFHRESP
subscript = NORMAL
```

A consequência é que o coletor de referências e o resolver passam a procurar símbolos COBOL chamados:

```text
DFHRESP
NORMAL
```

Se eles não existem, surgem gaps/unresolved que parecem erros de resolução, embora o programa esteja correto no contexto CICS.

### 3.1. Por que isso não é simplesmente um bug do resolver

O resolver está fazendo exatamente o que deveria fazer diante da AST que recebeu:

```text
AST diz: DataReference(DFHRESP(NORMAL))
            ↓
resolver procura DFHRESP
            ↓
não encontra
            ↓
UNRESOLVED
```

Portanto, suprimir `DFHRESP` diretamente no resolver COBOL misturaria responsabilidades e esconderia o fato de que estamos diante de uma extensão de plataforma.

### 3.2. Por que também não é simplesmente um bug da gramática

A investigação mostrou que a sequência:

```cobol
DFHRESP(IDX)
```

é estruturalmente ambígua quando o modo de compilação não é conhecido.

Em COBOL sem tradução CICS, o seguinte programa é perfeitamente explicável como COBOL:

```cobol
01 DFHRESP OCCURS 10 TIMES PIC 9.
01 IDX PIC 9.
01 X PIC 9.

MOVE DFHRESP(IDX) TO X.
```

Nesse caso:

```text
DFHRESP(IDX)
      ↓
TableCall
      ↓
resolver encontra a declaração DFHRESP
      ↓
RESOLVED
```

Já em fonte submetido ao tradutor CICS, `DFHRESP(...)` possui significado especial.

Sem saber em qual desses mundos o fonte está sendo processado, o parser não possui informação suficiente para escolher uma única interpretação universalmente correta.

---

## 4. Investigação da tentativa de correção na gramática

Uma tentativa inicial consistiu em remover os tokens `DFHRESP` e `DFHVALUE` de `cobolWord`, garantindo que o parser não pudesse mais derivá-los como data-names normais.

Essa mudança resolveu o caso CICS observado, mas criou uma regressão mais ampla.

Os seguintes casos, válidos na gramática anterior, passaram a falhar:

```cobol
01 DFHRESP PIC 9.
MOVE DFHRESP TO X.
```

```cobol
01 DFHVALUE PIC 9.
MOVE DFHVALUE TO X.
```

```cobol
01 DFHRESP OCCURS 10 TIMES PIC 9.
MOVE DFHRESP(IDX) TO X.
```

A razão é que o lexer ainda reconhece `DFHRESP` e `DFHVALUE` como tokens dedicados, porém, sem participação em `cobolWord`, eles deixam de poder atuar como nomes COBOL comuns.

A regressão foi confirmada por characterization tests executados nas duas versões:

| Caso | `main` anterior | PR com remoção de `cobolWord` |
|---|---|---|
| `DFHRESP` como data-name sem parênteses | passa | falha |
| `DFHVALUE` como data-name sem parênteses | passa | falha |
| tabela declarada `DFHRESP(IDX)` | passa | falha |

Na `main`, esses casos produzem zero erros sintáticos, declarações e referências COBOL normais e símbolos no namespace DATA. No PR, o parser passa a tratar os tokens como início obrigatório da forma CICS e espera `(`, provocando erros em declarações e usos simples.

Também foi demonstrado, chamando entry rules separadamente, que a sequência completa `DFHRESP(IDX)` pode ser aceita tanto como `tableCall` quanto como `cicsDfhRespLiteral` na gramática original. Isso confirma que o conflito não pode ser eliminado corretamente apenas tornando uma das derivações impossível de forma global.

Isso demonstrou um princípio importante:

> Corrigir `DFHRESP(...)` dizendo que `DFHRESP` nunca pode ser um nome COBOL resolve uma ambiguidade impondo uma regra mais forte do que o problema permite.

Em outras palavras:

```text
"DFHRESP(...) pode ser CICS"
```

não implica:

```text
"DFHRESP nunca pode ser COBOL"
```

A gramática deve, portanto, permanecer capaz de representar os dois casos.

---

## 5. A informação ausente: contexto real de compilação

A interpretação exata de certas construções depende da configuração com que o programa foi traduzido/compilado.

Idealmente o analisador receberia algo como:

```text
CICS
NOCICS
```

ou informações equivalentes oriundas de:

- JCL/procedure de compilação;
- opções do compilador;
- configurações da instalação;
- metadata de build;
- pipeline de transformação do mainframe.

Porém esse contexto não está disponível de forma geral para o analisador.

Isso impõe um limite epistemológico real:

> **Se duas interpretações são compatíveis com o mesmo source e a informação que as distingue não está na entrada, nenhum algoritmo pode recuperar essa informação com certeza.**

O analisador deve, portanto, representar a incerteza em vez de fingir que conhece o modo de compilação.

---

## 6. O que pode e o que não pode ser inferido a partir do source

Algumas evidências são inequívocas em uma direção.

Por exemplo, se o programa contém:

```cobol
EXEC CICS READ ...
END-EXEC
```

há evidência clara de que aquele source exige semântica CICS.

Entretanto:

```text
não existe EXEC CICS
```

não implica:

```text
NOCICS
```

Um programa pode depender de CICS sem conter `EXEC CICS`, por exemplo usando intrínsecos ou símbolos fornecidos pelo ambiente.

Também não devemos usar `DFHRESP(...)` sozinho para inferir que o modo é CICS, pois isso seria circular: justamente queremos decidir se aquela forma é um intrínseco CICS ou um table call COBOL.

Portanto, evidências do source podem enriquecer diagnósticos ou confiança, mas não devem substituir silenciosamente a configuração de compilação que não possuímos.

---

## 7. Princípio arquitetural proposto

A solução é dividir o conhecimento em camadas:

```text
Canonical COBOL semantics
        ↓
Platform semantics
        ↓
Organization-specific semantics
```

O core deve sempre tentar primeiro a explicação COBOL.

```text
O COBOL consegue explicar a construção?
        │
        ├── sim → preserve a interpretação COBOL
        │
        └── não → permita que extensões tentem explicá-la
```

Isso evita dois erros simétricos:

1. interpretar CICS como se fosse COBOL;
2. reinterpretar COBOL válido como CICS apenas porque a grafia parece especial.

O critério operacional para `DFHRESP` torna-se:

```text
DFHRESP(...) resolve como referência COBOL?
        │
        ├── sim → é COBOL para os fatos que conseguimos provar
        │
        └── não → pode ser classificado como possível intrínseco CICS
```

Essa é uma política deliberadamente conservadora.

---

## 8. Arquitetura de extensões de plataforma

A camada de extensões deve ser composta por capacidades pequenas e independentes, em vez de um único `CicsPlugin` monolítico.

A proposta possui quatro peças.

### Visão consolidada

| Peça arquitetural | Momento da pipeline | Problema resolvido | Exemplo |
|---|---|---|---|
| **External Symbol Provider** | antes da resolução | símbolos reais fornecidos pelo ambiente e não declarados no source analisado | `EIBCALEN`, `EIBAID` |
| **Unresolved Reference Classifier** | depois da resolução COBOL | referências cuja interpretação COBOL falhou e que podem pertencer a uma extensão | `DFHRESP(NORMAL)`, `DFHVALUE(...)` |
| **Semantic Extractor** | depois de AST/resolução; opcionalmente depois de dataflow | extrair fatos específicos de comandos/protocolos externos | `EXEC CICS LINK`, `EXEC SQL CALL`, GRBE `MONITOR` |
| **Control-Flow Semantics Provider** | CFG / enriquecimento do CFG | modelar efeitos de fluxo que não pertencem ao COBOL puro | CICS `XCTL`, `RETURN` |

Essas peças resolvem problemas diferentes e devem permanecer separadas.

---

## 9. Peça 1 — External Symbol Provider

### 9.1. Problema

Alguns nomes usados por um programa são símbolos reais, mas não foram declarados pelo programa nem necessariamente aparecem em copybooks presentes no corpus analisado.

Exemplo típico:

```cobol
IF EIBCALEN = 0
    ...
END-IF.
```

`EIBCALEN` pertence ao ambiente CICS. Tratar esse nome permanentemente como unresolved seria incorreto se o analisador conhece sua existência como símbolo externo.

### 9.2. Solução

Uma extensão pode contribuir símbolos antes da resolução:

```java
interface ExternalSymbolProvider {
    void contributeSymbols(SymbolRegistry registry);
}
```

Exemplo conceitual:

```text
CicsExternalSymbolProvider
    ├── EIBCALEN
    ├── EIBAID
    └── ...
```

Então a resolução normal continua funcionando:

```text
EIBCALEN
   ↓
reference resolver
   ↓
RESOLVED
origin = CICS_EXTERNAL
```

### 9.3. Propriedade importante

**Binding status e origem do símbolo devem ser dimensões diferentes.**

Por exemplo:

```text
bindingStatus = RESOLVED
symbolOrigin  = CICS_EXTERNAL
```

Isso evita usar `EXTERNAL` como sinônimo de `UNRESOLVED`.

### 9.4. Relação com copybooks de sistema

Se um copybook real estiver disponível e declarar o símbolo, a declaração normal deve vencer.

Exemplo:

```cobol
COPY DFHAID.
```

Se o copybook for expandido e declarar `DFHENTER`, o símbolo entra normalmente na symbol table. Não é necessário inventar um símbolo sintético duplicado.

O provider é mais útil quando o ambiente fornece um símbolo que não pode ser recuperado diretamente do conjunto de fontes entregue ao analisador.

---

## 10. Peça 2 — Unresolved Reference Classifier

### 10.1. Problema

Algumas construções externas possuem uma forma que a gramática COBOL consegue interpretar como uma referência nominal comum.

`DFHRESP(NORMAL)` é o caso paradigmático.

Primeiro deixamos o core tentar a interpretação COBOL:

```text
DFHRESP(NORMAL)
      ↓
DataReference / tableCall
      ↓
resolution
      ↓
UNRESOLVED
```

Somente depois do fracasso da resolução COBOL entra a extensão.

### 10.2. Contrato proposto

```java
interface UnresolvedReferenceClassifier {
    Optional<ExternalClassification> classify(
        UnresolvedReference reference,
        AnalysisContext context
    );
}
```

Primeira implementação:

```text
CicsUnresolvedReferenceClassifier
```

### 10.3. Regra para `DFHRESP`/`DFHVALUE`

A classificação deve ser estreita e explícita:

```text
resolution status == UNRESOLVED
AND
baseName ∈ { DFHRESP, DFHVALUE }
AND
syntactic shape compatível com a forma parenthesized
```

Resultado:

```text
POSSIBLE_CICS_INTRINSIC
```

Não devemos implementar regras genéricas como:

```text
name startsWith("DFH")
```

ou dependências de argumentos específicos como `NORMAL`/`NOTFND`.

### 10.4. Por que unresolved é um filtro importante

Compare dois programas.

#### Caso A — COBOL explicável

```cobol
01 DFHRESP OCCURS 10 TIMES PIC 9.
MOVE DFHRESP(IDX) TO X.
```

```text
DFHRESP(IDX)
      ↓
resolver encontra DFHRESP
      ↓
RESOLVED
```

A extensão nunca é acionada.

#### Caso B — possível CICS

```cobol
IF WS-RESP = DFHRESP(NORMAL)
```

Sem símbolo `DFHRESP`:

```text
UNRESOLVED
    +
shape conhecido
    ↓
POSSIBLE_CICS_INTRINSIC
```

Portanto a resolução COBOL atua como um filtro semântico antes da heurística externa.

### 10.5. Classificar o construct inteiro

Não basta mudar apenas o status da ocorrência `DFHRESP`.

A AST genérica pode ter produzido ocorrências internas como:

```text
DFHRESP
NORMAL
```

Ao reconhecer o construct externo, todas as ocorrências nominais artificiais pertencentes àquele subtree devem ser associadas à mesma classificação externa.

Resultado desejado:

```text
DFHRESP(NORMAL)
    → POSSIBLE_CICS_INTRINSIC
```

Não:

```text
DFHRESP → POSSIBLE_CICS_INTRINSIC
NORMAL  → UNRESOLVED
```

Essa propriedade evita manter gaps COBOL falsos causados pelo interior de uma construção CICS.

### 10.6. Não apagar a informação

A classificação externa não deve simplesmente remover o problema do relatório.

Ela deve permanecer observável:

```text
RESOLVED                     ...
UNRESOLVED                   ...
POSSIBLE_CICS_INTRINSIC       10
```

Isso preserva provenance, auditabilidade e a possibilidade de enriquecer a análise posteriormente.

---

## 11. Peça 3 — Semantic Extractor

### 11.1. Problema

Há construções que não precisam ser “resolvidas” como símbolos, mas carregam significado específico da plataforma.

Exemplo:

```cobol
EXEC CICS LINK
    PROGRAM(WS-PROGRAM)
END-EXEC
```

O core COBOL não deveria conhecer que `LINK` representa uma dependência para outro programa.

O preprocessor/parser pode preservar o bloco como construção externa, enquanto um módulo CICS interpreta seu significado.

### 11.2. Contrato proposto

```java
interface SemanticExtractor {
    Stream<SemanticFact> extract(AnalysisContext context);
}
```

### 11.3. CICS

Um `CicsSemanticExtractor` pode reconhecer:

```text
EXEC CICS LINK
EXEC CICS XCTL
EXEC CICS START
...
```

Para:

```cobol
EXEC CICS LINK PROGRAM('PROGA') END-EXEC
```

pode emitir diretamente:

```text
PROGRAM_DEPENDENCY(current, PROGA)
```

Para:

```cobol
EXEC CICS LINK PROGRAM(WS-PROGRAM) END-EXEC
```

não deve implementar sua própria análise de valores.

Ele identifica o operando:

```text
WS-PROGRAM
```

E consulta o dataflow canônico:

```text
possibleValues(WS-PROGRAM, programPoint)
    → { "PROGA", "PROGB" }
```

Depois produz:

```text
current → PROGA
current → PROGB
```

### 11.4. GRBE

O mesmo desenho serve para convenções organizacionais.

Exemplo:

```cobol
CALL MONITOR USING PARM1
```

O core COBOL entende normalmente:

```text
CALL
  target = MONITOR
  argument = PARM1
```

Um `GrbeSemanticExtractor` conhece o protocolo organizacional e pode interpretar campos/bytes do parâmetro para descobrir outro programa envolvido.

A extensão GRBE não deve implementar CFG ou reaching definitions por conta própria. Ela usa os serviços semânticos canônicos já produzidos pelo core.

### 11.5. DB2 e IMS

A mesma capacidade pode ser usada para:

- `EXEC SQL CALL ...`;
- referências a tabelas SQL;
- chamadas específicas IMS;
- comandos transacionais preservados pelo frontend.

O princípio é sempre o mesmo:

> A extensão conhece o significado da plataforma; o core fornece a estrutura COBOL, resolução e dataflow.

---

## 12. Peça 4 — Control-Flow Semantics Provider

### 12.1. Problema

Algumas extensões alteram o fluxo de controle do programa.

Exemplo CICS:

```cobol
EXEC CICS XCTL PROGRAM('PROGA') END-EXEC
DISPLAY 'AFTER'.
```

`XCTL` transfere controle para outro programa sem o comportamento de retorno de um `CALL` normal.

Um CFG puramente COBOL pode não conhecer esse efeito.

Se o builder simplesmente tratar o `EXEC CICS` como statement opaco e conectar o próximo statement, o grafo poderá conter uma aresta de fallthrough semanticamente incorreta.

### 12.2. Contrato conceitual

```java
interface ControlFlowSemanticsProvider {
    Optional<ExternalControlFlowEffect> classify(
        Ast.Statement statement,
        AnalysisContext context
    );
}
```

Possíveis efeitos:

```text
FALLTHROUGH
RETURNING_TRANSFER
NON_RETURNING_TRANSFER
PROGRAM_EXIT
CONDITIONAL_TRANSFER
UNKNOWN_EXTERNAL_EFFECT
```

### 12.3. Exemplos

```text
EXEC CICS XCTL
    → NON_RETURNING_TRANSFER
```

```text
EXEC CICS RETURN
    → PROGRAM_EXIT
```

Essas informações podem ser consultadas durante a construção do CFG ou aplicadas numa fase explícita de enriquecimento do grafo.

### 12.4. Por que isso não pertence ao Semantic Extractor

O `SemanticExtractor` emite fatos sobre significado externo.

O `ControlFlowSemanticsProvider` interfere na topologia do CFG.

Misturar os dois criaria dependência circular entre extração de fatos e análise de fluxo.

Por isso vale manter essa capacidade separada, mesmo que inicialmente ela possua poucas implementações.

---

## 13. Pipeline proposta

A arquitetura completa pode ser representada assim:

```text
COBOL source
    │
    ▼
Source normalization
    │
    ▼
Preprocessor
    │
    ▼
ANTLR
    │
    ▼
Semantic AST
    │
    ▼
Symbol Table
    │
    ├──── External Symbol Providers
    │       ├── CICS
    │       ├── IMS
    │       └── outros
    │
    ▼
Canonical COBOL Reference Resolution
    │
    ├── RESOLVED
    │
    └── UNRESOLVED
             │
             ▼
    Extension Classification
       ├── CICS unresolved classifier
       ├── IMS unresolved classifier
       └── outros
             │
             ▼
      Enriched resolution facts
             │
             ▼
         CFG construction
             │
             ├──── Control-Flow Semantics Providers
             │       ├── CICS
             │       ├── IMS
             │       └── outros
             │
             ▼
        Dataflow analyses
             │
             ▼
      Semantic Extractors
       ├── CICS
       ├── IMS
       ├── DB2
       └── GRBE
             │
             ▼
      Dependency/business facts
```

A posição exata de alguns extractors pode variar conforme a informação de que precisam. Extractors que só dependem da AST podem rodar antes do CFG; extractors que dependem de valores possíveis devem rodar depois do dataflow.

A interface deve permitir essa evolução sem obrigar todas as extensões a implementar todas as capacidades.

---

## 14. Por que não criar um único `CicsPlugin`

Um plugin monolítico tenderia rapidamente a concentrar responsabilidades demais:

```java
class CicsPlugin {
    // symbols
    // unresolved classification
    // dependency extraction
    // CFG behavior
    // dataflow hooks
    // copybook rules
    // ...
}
```

Esse formato incentiva condicionais internas e acoplamento crescente.

É preferível um módulo CICS composto por pequenas capacidades:

```text
CICS module
├── CicsExternalSymbolProvider
├── CicsUnresolvedReferenceClassifier
├── CicsSemanticExtractor
└── CicsControlFlowSemanticsProvider
```

IMS pode implementar apenas:

```text
IMS module
├── ImsExternalSymbolProvider
├── ImsSemanticExtractor
└── ImsControlFlowSemanticsProvider
```

GRBE pode começar apenas com:

```text
GRBE module
└── GrbeSemanticExtractor
```

Isso evita criar um framework de plugins maior do que o problema atual exige.

---

## 15. Categorias diferentes de problema CICS

É importante não colocar todas as construções CICS sob a mesma estratégia.

### 15.1. Intrínsecos que colidem com sintaxe COBOL

Exemplos:

```text
DFHRESP(...)
DFHVALUE(...)
```

Tratamento:

```text
COBOL resolution first
→ if unresolved
→ CICS Unresolved Reference Classifier
```

### 15.2. Símbolos externos reais

Exemplos:

```text
EIBCALEN
EIBAID
```

Tratamento:

```text
External Symbol Provider
→ normal reference resolution
```

### 15.3. Símbolos definidos por copybooks de sistema

Exemplo:

```text
DFHAID / DFHBMSCA constants
```

Tratamento preferencial:

```text
real copybook available
→ normal preprocessing
→ normal symbol table
```

Fallback futuro, se necessário:

```text
known missing system copybook
→ external symbols or explicit external classification
```

### 15.4. Statements externos

Exemplos:

```text
EXEC CICS LINK
EXEC CICS READ
EXEC CICS XCTL
```

Tratamento:

```text
preserve as external/embedded construct
→ Semantic Extractor
→ optional Control-Flow Semantics Provider
```

Essa separação é essencial para evitar uma única regra CICS genérica fazendo tarefas semanticamente diferentes.

---

## 16. A heurística de `DFHRESP` é aceitável?

Sim, desde que seja explicitamente tratada como classificação conservadora e não como verdade sintática.

A regra não é:

```text
Todo DFHRESP(...) é CICS.
```

A regra é:

```text
O core COBOL tentou resolver esta referência e falhou.
A forma observada coincide exatamente com uma construção conhecida do ambiente CICS.
Como não possuímos o contexto real de compilação, classificamos a construção como POSSIBLE_CICS_INTRINSIC.
```

Essa diferença é importante.

### 16.1. O que torna a heurística estreita

- só roda após resolução COBOL;
- exige status unresolved;
- exige nome exato `DFHRESP` ou `DFHVALUE`;
- exige shape sintático esperado;
- não depende de nomes particulares de argumentos;
- não procura prefixos genéricos;
- não reescreve a gramática;
- não altera referências COBOL que já foram resolvidas.

### 16.2. Limitação inevitável

Se um programa for realmente compilado em modo CICS e ao mesmo tempo declarar uma tabela chamada `DFHRESP`, o analisador sem contexto de compilação poderá escolher a interpretação COBOL porque ela é semanticamente explicável pela symbol table.

Isso é uma limitação inevitável da entrada disponível, não uma falha solucionável por algoritmo local.

A política proposta assume:

> **evidência COBOL concreta vence uma interpretação externa apenas possível.**

Esse comportamento é apropriado para um analisador que deve evitar inventar fatos.

---

## 17. Invariantes arquiteturais recomendadas

As seguintes invariantes devem ser preservadas por implementação e testes.

### INV-EXT-001 — Core COBOL não conhece nomes de plataforma

O resolver COBOL não deve conter condicionais específicas como:

```java
if (name.equals("DFHRESP")) ...
```

Esse conhecimento pertence a extensões.

### INV-EXT-002 — Resolução COBOL tem precedência sobre classificação externa inferida

Uma referência COBOL resolvida não pode ser reclassificada como possível CICS apenas pela grafia.

```text
RESOLVED > POSSIBLE_EXTERNAL
```

### INV-EXT-003 — Classificação externa preserva incerteza

Sem contexto de compilação, `DFHRESP(...)` unresolved deve ser classificado como possível, não como comprovadamente CICS.

### INV-EXT-004 — Construct externo absorve gaps artificiais internos

Ao reconhecer `DFHRESP(NORMAL)` como possível intrínseco, ocorrências artificiais criadas pela interpretação COBOL do subtree não devem continuar aparecendo como gaps independentes.

### INV-EXT-005 — Symbol origin é ortogonal a binding status

Exemplo válido:

```text
status = RESOLVED
origin = CICS_EXTERNAL
```

### INV-EXT-006 — Extensões reutilizam análises canônicas

CICS/IMS/GRBE não devem implementar seus próprios CFG, reaching definitions ou constant/possibility propagation.

Devem consumir serviços fornecidos pelo core.

### INV-EXT-007 — Plataforma não contamina AST COBOL sem evidência suficiente

Sem contexto de compilação, a AST não deve obrigatoriamente transformar todo `DFHRESP(...)` em um nó CICS.

A interpretação externa pode surgir posteriormente, após a resolução.

### INV-EXT-008 — Fatos externos são observáveis

Uma ocorrência classificada externamente não deve simplesmente desaparecer do relatório. Deve permanecer auditável com provenance e classificação.

---

## 18. Estratégia de testes

### 18.1. Characterization tests para o comportamento COBOL

Preservar casos como:

```cobol
01 DFHRESP PIC 9.
MOVE DFHRESP TO X.
```

```cobol
01 DFHVALUE PIC 9.
MOVE DFHVALUE TO X.
```

```cobol
01 DFHRESP OCCURS 10 TIMES PIC 9.
MOVE DFHRESP(IDX) TO X.
```

Expectativa:

```text
0 syntax errors
symbol created
reference resolved
no CICS reclassification
```

### 18.2. Testes do classifier CICS

Fonte sem declaração COBOL correspondente:

```cobol
IF WS-RESP = DFHRESP(NORMAL)
```

Expectativa:

```text
initial COBOL resolution: unresolved
extension classification: POSSIBLE_CICS_INTRINSIC
final unresolved COBOL gaps: no DFHRESP/NORMAL artifacts from the same construct
```

Repetir para `DFHVALUE(...)`.

### 18.3. Negative controls

Não classificar:

```text
MY-TABLE(IDX)
DFHOTHER(X)
DFHRESP sem parênteses quando for nome resolvido
```

### 18.4. Metamorphic property

Dado um host `H[x]`:

```text
H[MY-TABLE(IDX)]
```

se `MY-TABLE` estiver declarado, a referência permanece COBOL.

Ao substituir apenas o operando por:

```text
H[DFHRESP(NORMAL)]
```

sem declaração `DFHRESP`, a diferença deve surgir somente após a resolução/classificação externa.

O restante da estrutura do host deve permanecer equivalente.

### 18.5. Testes de External Symbol Provider

Exemplo:

```cobol
IF EIBCALEN = 0
```

Com provider habilitado:

```text
RESOLVED
origin = CICS_EXTERNAL
```

Sem provider:

```text
UNRESOLVED
```

Isso prova que o resolver permanece independente da plataforma.

### 18.6. Testes de Semantic Extractor

Para:

```cobol
EXEC CICS LINK PROGRAM('PROGA') END-EXEC
```

validar fato:

```text
PROGRAM_DEPENDENCY → PROGA
```

Para operando variável, testar integração com possible-values/dataflow.

### 18.7. Testes de Control-Flow Semantics

Para `XCTL` e `RETURN`, validar que a topologia do CFG não contém fallthrough indevido.

---

## 19. Ordem de implementação recomendada

A arquitetura não precisa ser implementada inteira de uma vez.

### Fase 1 — resolver o problema atual

1. restaurar `Cobol.g4` ao comportamento que permite `DFHRESP`/`DFHVALUE` como `cobolWord`;
2. manter characterization tests que provaram a regressão da alteração anterior;
3. introduzir a abstração `UnresolvedReferenceClassifier`;
4. implementar `CicsUnresolvedReferenceClassifier` apenas para:
   - `DFHRESP(...)`;
   - `DFHVALUE(...)`;
5. preservar a classificação externa nos resultados;
6. garantir que gaps internos artificiais sejam agrupados/suprimidos como parte do construct externo, não apagados silenciosamente.

### Fase 2 — external symbols CICS

Adicionar `ExternalSymbolProvider` quando surgir o primeiro caso concreto que justifique símbolos como `EIBCALEN`/`EIBAID`.

Não criar um catálogo amplo antecipadamente sem testes reais.

### Fase 3 — semantic extractors

Quando iniciar a extração de dependências CICS/IMS/DB2/GRBE:

- implementar extractors específicos;
- reutilizar resolução e dataflow do core;
- evitar heurísticas duplicadas dentro de cada tecnologia.

### Fase 4 — CFG extension semantics

Ao construir o CFG, adicionar `ControlFlowSemanticsProvider` somente para comandos cuja semântica afete o grafo, como `XCTL` e `RETURN`.

---

## 20. Não objetivos

Esta proposta não pretende:

- reproduzir um tradutor CICS completo;
- determinar com certeza o modo de compilação sem receber metadata de build;
- interpretar todas as tecnologias mainframe no frontend COBOL;
- substituir parsing formal por reconhecimento textual;
- mover regras CICS para o resolver COBOL;
- inferir tecnologias por prefixos genéricos;
- implementar antecipadamente um framework de plugins genérico e complexo.

O objetivo é menor e mais importante:

> criar uma fronteira estável para que semântica de plataforma possa complementar a semântica COBOL sem contaminá-la.

---

## 21. Consequências para o desenho do AST

Uma conclusão importante da investigação é que **não devemos transformar `DFHRESP(...)` incondicionalmente em um nó CICS na AST** quando não possuímos o contexto de compilação.

A AST pode continuar representando a interpretação COBOL que o parser conseguiu construir:

```text
DataReference DFHRESP(NORMAL)
```

O core tenta resolvê-la.

Somente depois:

```text
UNRESOLVED
    ↓
CicsUnresolvedReferenceClassifier
    ↓
POSSIBLE_CICS_INTRINSIC
```

Isso é mais fiel ao conhecimento realmente disponível.

No futuro, caso o analisador passe a receber metadata confiável de compilação, poderá existir um semantic lowering mais forte:

```text
known CICS mode
    +
DFHRESP(...)
    ↓
CicsExtensionExpression
```

Mas essa possibilidade futura não deve ser simulada hoje através de uma suposição silenciosa.

---

## 22. Relação com análise de CFG e dataflow futura

Essa arquitetura também prepara o projeto para as próximas fases.

O core deve produzir algoritmos canônicos:

```text
CFG
reaching definitions
constant propagation / possible-values propagation
```

As extensões passam a consumir esses resultados.

Exemplo CICS:

```cobol
MOVE 'PROGA' TO WS-PGM
...
EXEC CICS LINK PROGRAM(WS-PGM) END-EXEC
```

O CICS extractor não precisa andar para trás pelo source tentando descobrir o valor de `WS-PGM`.

Ele consulta:

```text
possibleValues(WS-PGM, LINK-point)
```

Exemplo GRBE:

```cobol
CALL MONITOR USING PARM1
```

O GRBE extractor identifica o layout/protocolo específico e consulta o dataflow para determinar quais valores podem atingir os bytes relevantes de `PARM1`.

Essa separação é fundamental para evitar que cada plugin desenvolva suas próprias heurísticas de fluxo.

---

## 23. Provenance, diagnóstico e confiança

Toda classificação externa inferida deve preservar:

- source span;
- arquivo de origem;
- cadeia de COPY, quando aplicável;
- texto original;
- motivo da classificação;
- tecnologia responsável;
- nível de certeza apropriado.

Exemplo conceitual:

```text
classification:
  namespace: CICS
  kind: POSSIBLE_INTRINSIC
  construct: DFHRESP
  rawText: DFHRESP(NORMAL)
  reason: COBOL_REFERENCE_UNRESOLVED_WITH_KNOWN_CICS_SHAPE
  confidence: INFERRED
```

Isso permite que o usuário diferencie:

```text
comprovado pelo COBOL
comprovado por símbolo externo conhecido
inferido como possível extensão
não explicado
```

Essa distinção é especialmente importante em ferramentas de análise estática cujo resultado pode alimentar grafos de dependência ou extração de regras de negócio.

---

## 24. Critério de precedência entre camadas

Uma política explícita reduz ambiguidades futuras:

```text
1. Declaração/semântica COBOL concreta
2. Símbolo externo explicitamente provido
3. Semântica externa explicitamente preservada no source (EXEC CICS/SQL/etc.)
4. Classificação externa inferida a partir de unresolved + shape conhecido
5. UNRESOLVED/UNKNOWN
```

Essa ordem reflete a força da evidência disponível.

Ela também evita que um classifier CICS sobrescreva uma referência COBOL perfeitamente resolvida.

---

## 25. Conclusão

O incidente `DFHRESP/DFHVALUE` não revelou apenas um caso especial da gramática. Ele expôs uma fronteira fundamental do analisador:

> **COBOL e ambiente de plataforma não são a mesma linguagem semântica, ainda que apareçam no mesmo source.**

Tentar resolver essa fronteira alterando globalmente `Cobol.g4` mostrou-se incorreto porque a mesma grafia pode representar COBOL normal ou uma construção CICS dependendo de informação de compilação que o analisador não possui.

A resposta arquitetural recomendada é manter o core COBOL canônico e introduzir capacidades de extensão independentes:

1. **External Symbol Provider** — ensina ao resolver sobre símbolos reais fornecidos pelo ambiente;
2. **Unresolved Reference Classifier** — explica conservadoramente referências que o COBOL não conseguiu resolver, como `DFHRESP(...)`;
3. **Semantic Extractor** — transforma constructs externos em fatos de plataforma/dependência reutilizando AST, resolução e dataflow canônicos;
4. **Control-Flow Semantics Provider** — acrescenta efeitos de controle de fluxo externos ao CFG sem hardcoding de CICS/IMS no core.

O primeiro caso de uso deve ser pequeno: restaurar a gramática e implementar `CicsUnresolvedReferenceClassifier` para `DFHRESP` e `DFHVALUE` unresolved. Entretanto, essa pequena implementação já deve nascer sob a fronteira arquitetural descrita neste documento.

Dessa forma, o projeto evita uma gambiarra isolada e transforma um bug concreto em uma estrutura reutilizável para CICS, IMS, DB2, GRBE e futuras extensões mainframe.
