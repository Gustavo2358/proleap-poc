# Checkpoint 4A — evidência e handoff

Baseline main limpa/atualizada: `c8a891e0827ae1dc1140246f625fd16c2ac9bd97`.
Work item: WORK-SEMANTIC-PRODUCT-005, implementation; branch
`feat/semantic-product-scalar-move`. Escopo físico: somente este frontend COBOL.
Discovery externo foi usado read-only; CP3/PR #31 foi arquivado após confirmação
de merge no GitHub, por exigência de lifecycle do harness.
PR #32 aberto para revisão humana, com branch publicada por push normal. O CI
associado ao head executa full, performance e challenges; consultar os checks
remotos para o resultado do commit mais recente. Nenhum merge/auto-merge realizado.

Contrato final: **cobol-semantic-product 1.2.0**, minor aditiva.
[Regra, tipos públicos, exclusões e complexidade](../domain/scalar-text-move.md).
Fixture: [AIR-MOVE.cbl](../../src/test/resources/cobol/semantic/AIR-MOVE.cbl).
O oracle ScalarMoveOracle usa somente CobolSemanticPort e seus tipos públicos;
um gate de bytecode/source exclui AST, parser, símbolos, resolução, projector,
rawLexeme, PIC, nomes, programPoint e readiness. O oracle JSON usa campos tipados
mesmo após remoção de readiness/scope/detail. Reordenar statements não muda a
conclusão obtida pela referência normalContinuation.

## Input contract for Checkpoint 4C

O [SP real do fixture](evidence/checkpoint-4a/AIR-MOVE.semantic-product.json)
contém os seguintes campos (demais campos/provenance preservados no arquivo):

```json
{
  "schema": "cobol-semantic-product",
  "contractVersion": "1.2.0",
  "dataDeclarations": [{
    "id": "data:0",
    "scalarText": {
      "logicalDomain": "TEXT", "logicalExtent": 5,
      "storageClass": "WORKING_STORAGE", "declarationScope": "LOCAL"
    }
  }],
  "statements": [{
    "variant": "MOVE",
    "header": {"id": "statement:0"},
    "source": {
      "id": "operand:0:0", "kind": "ALPHANUMERIC", "value": "PROGA",
      "logicalValue": {"logicalDomain": "TEXT", "value": "PROGA", "logicalExtent": 5}
    },
    "target": {
      "id": "operand:0:1", "role": "WRITE",
      "binding": {"status": "RESOLVED", "selected": "data:0"},
      "wholeItemAccess": {"data": "data:0"}
    },
    "copySemantics": "FULL_IDENTITY",
    "normalContinuation": {"availability": "KNOWN", "statement": "statement:1"}
  }, {
    "variant": "GOBACK", "header": {"id": "statement:1"},
    "exit": "CURRENT_PROGRAM_INVOCATION", "localContinuation": "NONE"
  }]
}
```

A presença de scalarText prova o profile declarativo, e wholeItemAccess prova a
ocorrência inteira; selected apenas identifica a declaração. FULL_IDENTITY já
inclui obrigatoriedade, escrita completa e ausência de conversão/padding/truncation;
4C não precisa comparar spellings, interpretar PIC ou avaliar regras MOVE.
normalContinuation é a referência executável publicada; ordem dos arrays e
programPoint não são substitutos. Handles pertencem à unit do documento.
A entry primária aponta explicitamente para statement:0; entryInventory permanece
PRIMARY_ONLY/PARTIAL com ALTERNATE_ENTRIES_NOT_PROJECTED.

4C precisa reconhecer 1.2.0, admitir somente suas provas conhecidas e preservar
provenance e gaps independentes. Nenhum código de cobol-lower foi alterado.

O output real da CLI tem **5.177 bytes** (alias legado byte-identical).
SHA-256: `468e3207f578e428ace89a311eadbd6e27c670331b739675b761479352adc7af`.
A execução observou 403 ms no frontend/CLI, zero parser errors, um binding.

## Contracasos e regressão

Oracles protegem literal menor/maior, numérico, figurativo, national/hex/null,
destination não textual, OCCURS direto/ancestor, REDEFINES original e redefinidor,
RENAMES, group, GLOBAL/EXTERNAL, cláusula desconhecida, LINKAGE/LOCAL-STORAGE,
subscript/ref-mod, binding ambíguo/ausente e input incompleto. Os fatos MOVE e
bindings/candidates permanecem presentes. Literal não coberto conserva UNKNOWN.

Continuação no fim físico, em boundary de paragraph ou aninhada sem prova fica
UNAVAILABLE; remover metadata canônica não permite fallback pela ordem da AST.
GOBACK seguido de MOVE/GOBACK mantém NONE. Renaming e duplicação de delimitadores
são metamorfismos positivos. Construtores legados não inventam novas provas.

O [golden CP3 1.1.0](../../src/test/resources/cobol/semantic/entry-goback-sp-1.1.0.json)
foi emitido por build do baseline extraído em /tmp; oracle compara o payload
inteiro com 1.2.0 após excluir somente contractVersion. CP3 permanece obrigatório.

## Escala e falsificações

Os probes não são SLA. Os oracles verificam cardinalidade, IDs e crescimento
linear de visitas/consultas; tempos são observações incluindo frontend, projeção,
serialização e leitura JSON, sem profiler/threshold de heap.

| Probe | Linhas | DATA | MOVE | Visitas de nós | Consultas escalares | SP bytes | Tempo observado |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| facts-1500 | 3008 | 1500 | 1500 | 13508 | 1500 | 3962925 | 1394 ms |
| facts-3000 | 6008 | 3000 | 3000 | 27008 | 3000 | 7940925 | 1924 ms |
| physical | 60012 | 1 | 1 | 17 | 1 | 5243 | 193 ms |
| shared-data | 10008 | 1 | 10000 | 40013 | 10000 | 18809400 | 2078 ms |

As observações brutas, hashes, GREEN/RED/restore e resultados dos gates estão no
[manifest de evidência](evidence/checkpoint-4a/manifest.json). Outputs de escala
reproduzíveis ficam em target/checkpoint-4a; seus hashes/tamanhos são preservados.
O fixture físico usa 60.012 linhas com um DATA, MOVE e GOBACK. Massa: 1.500 e
3.000 DATA/MOVEs; compartilhamento: uma declaração e 10.000 MOVEs. Cada MOVE
resolvido faz uma consulta escalar. Nenhum novo ID expande texto.

O script challenge-scalar-move.py executa 13 falsificações para as dez classes
exigidas: categorias universais, mismatch, OCCURS/overlay/ref-mod (separados),
selected errado, nominal→inteiro, ordem→continuação, next removido, GOBACK
fallthrough, provenance literal/target (separados), DATA duplicado. Cada RED
exige falha semântica executada; erro de compilação não conta. Restauração confere
os bytes originais, seguida de segundo GREEN incluindo os probes.

## Limites e review

Não há AIR, lowering, CFG, CALL, dataflow, storage byte-level ou codecs gerais.
Qualifiers, nested elementary items, VALUE e atributos de programa adicionais
estão deliberadamente fora do profile. Overlay em qualquer ponto da seção nega
promoção na seção inteira; não é alias analysis. Continuação universal, effects,
assinaturas/entries alternativas e inventário de declaratives continuam abertos.
PR deve parar para review humano; sem merge ou auto-merge.
