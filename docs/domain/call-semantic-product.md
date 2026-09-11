# CALL no Semantic Product — CP6 W1A

Contrato corrente SP **1.3.0**. Esta capability publica fatos COBOL, não AIR,
lowering, CFG, resolução de dependências ou valores de runtime.

## Autoridade e algoritmo

IBM Enterprise COBOL for z/OS 6.4, Language Reference SC27-8713-03, edição de
28/06/2024, verificada em 11/09/2026: [CALL, EXIT PROGRAM e MOVE/alinhamento](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf).
O retorno de programa chamado retoma o ponto seguinte à chamada; dados compartilhados
podem ter mudado. Alinhamento alfanumérico sem JUSTIFIED preenche à direita quando
o receptor é maior. Essas regras não provam que uma chamada retorne nem pureza.

O builder conserva presença das cinco cláusulas pelos contextos diretos CALL.
Sua relação `Division.normalContinuations` cobre MOVE e CALL sem handlers nas
listas canônicas de statements diretos da mesma região de sentences. Não atravessa
fim de paragraph/section, declaratives, statement não materializado nem controle
aninhado. Handler presente torna essa relação indisponível: não se confunde a
continuação externa da chamada com o corpo de NOT ON EXCEPTION.

`ScalarMoveSemantics` indexa os targets CALL durante o mesmo walk que já identifica
declarações escalares. Uma ocorrência CALL_TARGET, STRUCTURED, resolvida unicamente,
sem qualifiers/subscripts/refmod, ligada à declaração escalar admitida, recebe
whole-item proof. Reutiliza índices de símbolos/resolução; não procura MOVEs
anteriores. O projector traduz identities, cláusulas, provas e relações canônicas.

Tempo O(N + R + L + P), memória O(N + R + P): N estruturas/declarations, R referências,
L texto realmente interpretado, P caracteres de resultados ajustados publicados.
Passes finitos, sem limite semântico de cardinalidade e sem scan global por CALL.
Não há alias analysis geral; as exclusões conservadoras do profile escalar permanecem.

## Target e identidade

`CallTarget` é uma soma fechada de `DataReference` e `LiteralCallTarget`. O JSON usa
`target.kind=DATA` com `reference`, ou `target.kind=LITERAL` com `id`, `text`,
`writtenText`, `logicalValue` e `provenance`. Syntax é derivada dessa variante.

DATA mantém OperandId, role CALL_TARGET, binding status/reason/candidates/selected
e DataItemId. A prova wholeItemAccess aponta para a mesma seleção; ausência da
prova não apaga binding ou target. O profile mantém fora aliases/views, tabelas,
refmod, declaração não escalar e referências qualificadas. Ambiguidade/unresolved
não selecionam um candidato e nunca recebem acesso inteiro por conveniência.

Literal básico usa a interpretação canônica já usada para literals MOVE; conserva
caixa, espaços e delimitadores duplicados normalizados. WrittenText é somente
grafia/provenance, nunca fallback semântico. Literals fora desse subconjunto mantêm
sua variante/superfície e `logicalValue=null`, com gap e readiness insuficiente.
Nenhum catálogo de programas nem presença do callee no corpus é necessário.

`runtimeTarget=UNKNOWN` descreve a chamada runtime ainda não resolvida, inclusive
quando uma referência literal fonte é conhecida. Não é um valor do item DATA.
Literal conserva seu próprio valor; DATA não publica nome ou valor derivado de MOVE.

## Surface, continuação e incerteza

`surface` contém USING, RETURNING/GIVING, ON EXCEPTION, NOT ON EXCEPTION e ON OVERFLOW
como ABSENT/PRESENT/UNKNOWN. USING ausente exige `argumentCount=0`; presente exige
count positivo; desconhecido mantém count null. Input incompleto impede afirmações
de ausência, acesso e continuação. Bodies excepcionais ainda são achatados na AST;
W1A publica a distinção de presença, não inventa partições/branches dos bodies.

`normalContinuation=KNOWN` aponta para um StatementId real na mesma unit e descreve
somente o sucessor após retorno normal. UNAVAILABLE não significa NONE. A definição
permite não retorno, exceção, término, divergência e controle aberto.

`effects=UNKNOWN` e `outcomes=OPEN` são campos obrigatórios do writer e valores
fixados pelo fact; não existe variante NONE neste contrato. Ausência de USING
não afirma ausência de efeitos externos/compartilhados.

Lowering readiness SUFFICIENT exige target literal lógico provado ou DATA escalar
com acesso inteiro, origem exata do statement e target, continuação conhecida e
todas as cláusulas explicitamente ausentes. É suficiência dos fatos de fonte da
primeira slice, não certificação AIR. CFG readiness só descreve a relação local;
effects/dataflow permanece PARTIAL. Containment e gaps independentes são preservados.

## MOVE e provenance

O [profile escalar](scalar-text-move.md) conserva FULL_IDENTITY quando as extensões
são iguais. Se a origem básica é menor que o receptor elegível, publica FITTED_TEXT
e `textAdjustment(rule=RIGHT_PAD_SPACE, receiverExtent, result, provenance)`.
Resultado de PROGA para X8 é `PROGA   `. X5 continua identidade e omite adjustment.
Truncamento continua UNAVAILABLE. Sem declaração/categoria/extensão/acesso provados
não há adjustment; o lower não precisa reler PIC, recontar caracteres ou aplicar
regra COBOL para obter o resultado.

Origem de statement, literal e destino permanecem separadas. Adjustment tem regra
tipada e provenance derivada do MOVE; sua evidência são os joins do próprio fact
com source lógico, acesso do target e declaração scalarText. O core valida extensão,
resultado, binding e fechamento. Provenance do CALL e de seu target é independente.

## Contrato interno e handoff

INTERNAL-CONTRACT-DEV-001 foi aprovado para este workspace em desenvolvimento:
mudança necessária de shape/significado recebe bump explícito e uma versão corrente;
o consumer interno pode ser atualizado em checkpoint posterior. Não há dual writer,
negociação, downgrade, projeção legacy ou manutenção paralela de contratos.

1.2.0 → 1.3.0 altera o shape CALL, acrescenta adjustment MOVE e amplia as capabilities.
O lower congelado `18016f16b4f63149eb1bb4ca13db7e12593d8909` rejeita 1.3.0 com
UNSUPPORTED_CONTRACT, como esperado. W1C deverá reconhecer o contrato, atualizar
o pin e restabelecer integração. Não declarar CP5/CP6 E2E verde com pins incompatíveis.
Entry/GOBACK e demais fatos CP5 não relacionados continuam sujeitos aos oracles
de equivalência, independentemente dessa recusa de versão.

W1A DOES NOT resolve dynamic target values. W1A DOES NOT interpret runtime program
names. W1A DOES NOT emit AIR. Não aplica trim/canonicalização de nomes runtime.
W1B/W1C/W1D/W2 continuam fora da autorização W1A.

EVAL-SP-007 e INV-SP-009 protegem essa fronteira, incluindo os challenges focais.
