# D0 — recomendação arquitetural

**Architectural decision: C — topologia de controle com autoridade no frontend, transportada pelo SP.** Preservar inicialmente o backend AIR existente como estratégia de migração, com limites explícitos de especialização. Não escolher B antes de demonstrar matching/custo no CFG/dataflow. W8 continua PAUSED e W9 NOT STARTED.

## Root causes

1. **Conclusão de região foi representada de forma desigual entre classes de statement.** AST já distingue local/ordinary; projector mistura ordinary no normal de OBSERVED; frontier global/conditional é consumida como successor exclusivo pelo lower. Isso explica todos os 46 F2 e a contradição análoga de F3, sem exigir conhecimento integral de valores.
2. **A autoridade dos edges está dividida.** Resolver/AST/semantics/projector/admission/assembler reconciliam parte da mesma linguagem; FILE inventaria referências por canal separado da closure. F7 é prova de integridade desse segundo problema, não um gap aceitável.
3. **As premissas de prova têm granularidade insuficiente.** inputComplete(unit), environment/layout e structureProven propagam perda a fatos locais. F5/F6 não justificam zerar todos os fatos independentes. Profile físico é necessidade real para certos fatos, não para todos.
4. **A campanha mediu responsabilidade local e oracles estreitos, sem viabilidade transversal obrigatória.** A amostra histórica mostra oito regressões de pipeline já visíveis até W5; controles anteriormente opacos passaram a tipados e revelaram contradições, mas o corpus qualificado não capturou essas combinações.

## Hipóteses tentadas, evidência e resultado

| Hipótese | Resultado D0 | Evidência/refutação |
| --- | --- | --- |
| H1 Positive Memory correta, controle global incompleto | SUPPORTED com correção | não há literalmente um único mapa AST; há re-colapso no contrato/consumer. CBACT01C e COACTUPC mostram dois outcomes legítimos |
| H2 skeleton insuficiente é central | SUPPORTED | 47 first blockers de controle e 60 labels de handlers em duas variantes; topology independe de ADD/DLI runtime |
| H3 precisa COBOL quase integral | NOT ESTABLISHED; refutada como pré-requisito para estes blockers | outputs PARTIAL atravessam; falta de valor não explica fronteiras. Precisão corporativa universal não foi provada |
| H4 autoridade duplicada é problema | SUPPORTED | cinco lugares reinterpretam completion/ordinary; FILE closure usa inventário diferente do emissor |
| H5 activation specialization é abstração errada | PARTIALLY | não é semanticamente inválida por definição; matching passa nos sintéticos. Live DAG e CBACT04C impedem certificação de escala geral |
| H6 granularidade de partiality | SUPPORTED | INPUT_MISSING bloqueia WS-PGMNAME; F6 level SQL derruba seção inteira; K4 perde EVALUATE frontier |
| H7 é principalmente processo, não arquitetura | PARTIALLY, explicação exclusiva refutada | processo deixou escapar falhas concretas de contrato/integridade; mais testes detectariam cedo, mas não substituem correção da abstração |

## Why

CBACT01C exige EXIT158→44 em execução ordinária e →90 no PERFORM89. COACTUPC exige SET1284→fronteira intermediária→1312 e EXIT1312→859 no THRU858; sua condição DFHAID pode continuar unknown. CBEXPORT exige nested returns sem clonar semântica COBOL em cada consumer. CBPAUP0C exige saída de WHEN para fim do EVALUATE/parágrafo, não sibling. CBACT04C exige que o mesmo inventário que emite edges contenha handlers em cada contexto.

C concentra a interpretação no estágio que já possui a gramática, a estrutura AST e a proveniência disponível, inclusive suas lacunas explícitas. Lower traduz regras tipadas de região/contexto; não redescobre significado de parágrafo. Isso elimina duplicação demonstrada. Não é argumento de elegância: são mecanismos observados em fonte e executáveis reais.

A corrigida poderia expressar os mesmos fatos, mas conservar whitelists/controle por campo repetiria o erro; sua sustentabilidade operacional ainda falta. B expressa matching e ordinary defaults de modo promissor, mas consumer CFG atual não suporta control.local e custo total é UNKNOWN. C não promete resolver cloning: esse é um gate separado de backend.

## What we should stop doing

- Adicionar exceção de completion por verbo ou relaxar a admissão de successors externos.
- Tratar whole-unit completeness como premissa implícita de todo fato local.
- Fazer lower reconstruir fallthrough/arm completion a partir de campos heterogêneos.
- Manter inventários separados de referências que o assembler cria e de alvos que a closure materializa.
- Fechar wave arquitetural apenas por corpus sintético/green CI ou por queda de um first-loss.
- Usar “mais candidates”, saída antiga ou arquivos existentes como oracle de correção.

## What we should preserve

Positive memory e fatos positivos independentes; gaps/remainders honestos; provenance tipada; contratos versionados; validator estrito; negativos de cross-return/dead code/overwrite; pins, hashes, raw logs e baselines; inventário físico de 73; distinção entre viabilidade PARTIAL e suporte completo.

## Migration path

1. Revisão humana de D0 e contrato de region outcomes/reference inventory, com os quatro modelos de A/B/C e os 14 witnesses reais. D0 termina aqui.
2. Futura wave R1 produz ControlTopology versionada, separando skeleton de valores; lower consome o grafo tipado por um backend de tradução genérico. Decoder legado conserva semântica anterior. Todos os tipos de edges emitidos participam da closure, incluindo FILE.
3. Qualificar A-backend transitório em shared DAG vivo, COACTUPC e CBACT04C; se budgets/contextos inviáveis, registrar limite e bloquear fechamento dessa dimensão. Abrir estudo de B somente com contrato/solver/oracles e orçamento próprios; não introduzir stack sem consumer.
4. Fact dependencies e storage unknown por região, com inputs autênticos/configurações explícitas e prova de isolamento. L0 normalization policy separada.
5. Toda wave executa CardDemo 73 FULL e Gate B, redistribui blockers e só então fecha.

## Expected CardDemo effect

Mecanismo de região/contexto tem alvo direto **F2 46 + F3 1**; reference closure inclui **F7 2** quando F4 for ultrapassado. Nenhuma dessas contas é promessa de chegar a 51/73 ou 53/73: resumes inline, effects, storage e budgets seguintes podem bloquear. Provas locais atacam F5 12 + F6 1 e a porção arquitetural de F4 7; configuração autêntica trata sua outra porção. Normalização trata F1 2. F7 continua sobreposição, sem inflar o denominador.

## Respostas finais obrigatórias

**Nossa abordagem de não modelar tudo está errada? PARTIALLY.** NO como filosofia para valores/effects/runtime exato; PARTIALLY para cobertura de controle e granularidade storage: skeleton/outcomes e referência de targets precisam de cobertura muito maior e integridade total do que é publicado. Unknown é permitido; contradição e AIR inválida não são partiality.

**Construir um control graph mais próximo do COBOL antes de valores? YES**, finito, parcial e tipado, sem emulação completa. Benefício: uma autoridade de outcomes/regions e bounds; custo: migração SP/frontend/lower e validação/escala. L0 correto; L1 alta cobertura; L2 parcial localizada com alias proof; L3/L4 parciais com effects/remainders; L5 não necessário.

Não implementar ainda: fixes dos sete erros, nova capability local no CFG, mudança de oracle/validator, stubs/copybooks artificiais, profile global automático ou tab policy implícita. Não retomar W8/W9 nem mudar PRs. **STOPPED BEFORE CODE: YES.**
