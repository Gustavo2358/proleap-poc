# D0 — taxonomia causal

**73 descobertos; SP 71 PARTIAL + 2 BLOCKED; AIR 4 PARTIAL + 67 BLOCKED + 2 não alcançados; CFG/dependency 4 PARTIAL. 69/73 não chegam ao fim.**

| Família | N | Tipo causal | Primeira perda e interpretação |
| --- | ---: | --- | --- |
| F1 | 2 | F + robustez de normalização; G condicionado ao dialeto/import policy | HT sem política uniforme de colunas. Não é partiality de valores. |
| F2 | 46 | C; A/D contribuem em K4 | Completion de região e fallthrough ordinário colidem na interpretação de normalContinuation; prova global ainda apaga algumas frontiers. |
| F3 | 1 | C | Continuação de braço é ordinária, consumer compara apenas com continuação local do EVALUATE pai. |
| F4 | 7 | F + D/A: MIXED | Perfil físico ausente é configuração real; bloquear valores lógicos independentes revela granularidade insuficiente. |
| F5 | 12 | F + D, com A localizado | COPYs autênticos ausentes; inputComplete(unit) amplia a perda para fatos sem dependência demonstrada desses COPYs. |
| F6 | 1 | A/producer gap + D | EXEC SQL INCLUDE vira DataEntry opaco level SQL; structureProven é falso para a seção inteira. |
| F7 | 2 sobrepostos a F4 | E | Closure da ativação omite handlers FILE que o emissor referencia. 30 I-02 por variante. |

A/B/C/D/E/F/G seguem a tipologia do pedido. B é comportamento desejável e já existe em fatos parciais preservados; não é sinônimo de blocker. Nenhuma família é genericamente rebatizada UNSUPPORTED. F7 não aumenta 69.

42 programas têm 101 ocorrências sourceDependencies UNRESOLVED; esses gaps transversais não são novas famílias first-loss. Os quatro produtos finais não demonstram suporte completo. Cinco saídas adicionais PARTIAL dos probes F4 tampouco mudam o baseline canônico: usaram configuração diferente.

O inventário físico e as classificações por programa permanecem no [CSV canônico](canonical-summary/classification.csv). D0 não exclui qualquer caso. Não foi comprovada corrupção de ZIP ou invalidade COBOL que autorize exclusão; missing input e contexto UniKix exigem classificação/configuração explícita, não remoção tácita.
