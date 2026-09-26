# Arquitetura C — frontend como autoridade de ControlTopology

**Arquitetura recomendada para a próxima fase, sujeita a revisão.** É uma decisão de autoridade e contrato, não uma declaração de implementação pronta ou de 73 PASS. Manter inicialmente AIR atual como backend evita acoplar essa mudança a um solver control.local ainda não suportado.

Os quatro witnesses comparados em A/B/C são os mesmos: (W1) CBACT01C PERFORM89/EXIT158; (W2) COACTUPC PERFORM858 THRU/SET1284/EXIT1312; (W3) CBEXPORT PERFORM94→1000 com PERFORM103 aninhado→1050/STRING111; (W4) CBPAUP0C EVALUATE58/SET62→boundary2000. São modelos hipotéticos, sem execução/fix de produto. Edges de linguagem estão em D0_F2_DEEP_DIVE e D0_F3_COMPARISON.

## Modelo proposto

Uma topologia finita de SourceOccurrence/Region/Boundary, com outcomes tipados, identidade estável, origem e proof dependencies. Exemplo de vocabulário: `NORMAL`, `BRANCH`, `EXPLICIT_TRANSFER`, `LOCAL_INVOKE`, `COMPLETE(region)`, `PROGRAM_RETURN`, `UNKNOWN_LOCAL(bound)`. A palavra LOCAL_COMPLETE isolada não basta: identificar a região/porta e a regra que a consome é necessário.

`Normal` interno a uma região pode chegar a `Complete(R)`. Composição propaga Complete(arm) ao join/fim do pai; Complete(paragraph) encontra uma boundary com default ordinário e binding contextual de invocação. IF/EVALUATE transportam grouping/entries/exit relations mesmo quando predicates/values são unknown. Exceções/FILE handlers pertencem ao mesmo inventário de outcomes. Availability de um edge depende de provas específicas, não da precisão de efeitos do statement inteiro.

| Witness | Fatos frontend propostos | Tradução lower genérica |
| --- | --- | --- |
| W1 | Invoke89(entry40, end=P0000, resume90); EXIT158 Normal→Complete(P0000); boundary default44 | especializar regra de boundary no contexto89, sem reconhecer palavra EXIT |
| W2 | Invoke858(range PYYYY..PYYYY_EXIT, resume859); EVAL arm SET1284→Complete(arm)→Complete(PYYYY); default1312; EXIT1312→Complete(PYYYY_EXIT), default819 | binder de range/context resolve primeira boundary por default e última por resume; não reconstrói THRU |
| W3 | Invoke94(P1000,resume95); Invoke103(P1050,resume104); STRING111→Complete(P1050); DISPLAY106→Complete(P1000) | empilhar bindings genéricos ou especializar grafo tipado; dados mantêm identidade compartilhada |
| W4 | Branch/Evaluate58 com arm GB entry62; SET62→Complete(armGB)→Complete(EVAL58)→Complete(P2000), boundary default66 | nenhuma comparação ad hoc entre filho.normal e pai.normal; apenas tradução de outcomes |

## O que desaparece e o que permanece

Podem ser substituídas quatro reconciliações semânticas: (1) whitelist/fallback de ordinary no projector, (2) exceção CALL vs outros completions na admissão, (3) reconstrução de arm exit por igualdade com successor pai, (4) closures independentes para statement versus FILE handler. `ProcedurePerformSemantics` deixa de reprovar a existência de uma topology por whole-body/value profile. A lógica de reconhecimento COBOL continua no frontend, onde já está a gramática e o resolver; não é transferida ao CFG.

Não estimar “linhas apagadas” sem implementação. Lower continua traduzindo operações/values/storage, materializando contextos de um grafo tipado quando necessário, gerindo IDs e verificando integridade/capabilities. O ganho é remover **decisões de semântica COBOL**, não eliminar todo processamento de grafo. Projector faz transporte e checagem, sem inventar edges.

SP pode transportar partiality porque já tem availability, proveniência e inventories; precisa uma versão coordenada para topology e depends_on. Fatos desconhecidos devem identificar a região potencialmente afetada; se bound não é provado, não estreitá-lo artificialmente. Completeness de referência/topologia publicada e completude semântica da fonte são propriedades diferentes.

## Custo, escopo e limites

Mínimo inicial: frontend + SP schema/transport + lower; contratos/evidência E2E. AIR/CFG podem continuar usando operações existentes se a tradução materializada expressar corretamente os outcomes; isso requer roundtrip/validator/quality gates. Não exige nova IR antes dessa prova. Se UNKNOWN_LOCAL ou matching necessário não tiver representação honesta no backend, essa parte precisa capability/contrato explícito, não fallback inventado.

Módulos separados para grammar regions, targets resolvidos, topological invariants e dependencies de prova impedem que frontend vire um emulador de COBOL. Valores/effects não precisam ser completos; podem refinar outcomes/condições depois. Proveniência de boundary aponta ao término sintático e ao parágrafo/range; edges derivados citam premissas.

C **não elimina por si só** o custo de activation cloning: com backend A, herda seu limite. O grafo fonte pode ser O(S+E+boundaries), mas a AIR especializada depende do número de contextos. Por isso um benchmark real de COACTUPC e CBACT04C e o live shared DAG são critérios de go/no-go da migração, junto de zero cross-return. B é alternativa futura de backend, não parte aprovada desta decisão.

## Por que o custo é justificável

F2/F3 atingem 47 first blockers por perda de uma abstração comum; F7 é uma segunda falha de inventário de edges. Os probes mostram que parte da informação correta **já existe no AST** e é degradada na publicação/reconciliação. A mudança concentra autoridade existente e reduz caminhos divergentes, em vez de acrescentar interpretações em cada consumer. F5/F6 demandam a disciplina complementar de fact dependencies; C oferece o transporte, mas não resolve storage automaticamente.

Critérios C: duplicação real demonstrada; representação parcial desenhada sobre quatro witnesses; simplificação conceitual do lower identificada; custo de dois repos/contrato justificável; escopo frontend limitado à linguagem e à prova de topology. Confirmação operacional depende da futura implementação e seus gates. Nenhum fix foi feito em D0.
