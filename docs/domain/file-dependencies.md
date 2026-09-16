# FILE-DEPENDENCIES — frontend / SP

FD-H0–H4 prepara; FD-W0 ainda TODO, condicionado à revisão humana H4.
[Campanha canônica](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/README.md)
(workspace: `../analysis-cfg/docs/product/file-dependencies/README.md`).
Comece pelo brief e [W0](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/work/file-dependencies/FD-W0.yaml).

## Pontos de intervenção W0

- `AstBuilder.buildEnvironment/buildData` e `Ast.FileBinding/FileDescription`:
  ASSIGN hoje textual, FD/SD sem discriminante. Conservar cláusulas tipadas.
- `SymbolTableBuilder.buildFileEntities` já une FILE_CONTROL/FILE_DESCRIPTION.
  `ProcedureFileProgramReferenceResolverTest` já cobre SELECT+FD, GLOBAL, ancestral
  e shadowing. Reutilizar identidade canônica; não criar outro índice nominal.
- `ReferenceOccurrenceCollector` e resolução canônica resolvem por papel/escopo;
  WRITE/REWRITE necessitam record→owner, não lookup de filename por nome do record.
- `CobolSemanticProduct.StatementFact`/projector/writer/integrity validator são
  inventários fechados. Hoje não têm facts nativos I/O. `ObservedStatement`
  conserva observação, não certifica semântica de arquivo.
- Storage usa `ibm-enterprise-6.4-fixed-display-1047@1`; múltiplos layouts de FD
  exigem relação física provada em W3. Não modelar cada 01 como base independente.

Contrato SP de W0: declaração/owner/FD-SD/assignment variante e alvo source-level,
opcionalidade, organização/acesso/keys/status, record ownership, visibilidade,
origens e disponibilidade por dimensão. Sem uso não há READ inventado. Tipos e
versão final seguem [política semântica](../engineering/semantic-analysis-policy.md)
e evolução coordenada do decoder lower; nenhum cálculo de valor no projector.

## Semântica a consultar

O [perfil e as fontes](https://github.com/Gustavo2358/analysis-cfg/blob/feat/file-dependencies/docs/product/file-dependencies/profiles.md)
selecionam IBM z/OS 6.4, CICS TS 5.6 e subset GnuCOBOL 3.2. Cobertura, non-goals,
casos e invariantes ficam somente no brief/matriz canônicos. Não importar regra
de DYNAMIC para assignment-name IBM. Origem COPY inclui expansão e include site.

W0 protege CALL, CICS Program Control, resolução nominal e storage existentes.
Gates focais: `mvn -B -ntp -Dtest=ProcedureFileProgramReferenceResolverTest,SemanticProductIntegrityValidatorTest,SemanticProductStatementInventoryTest,SemanticProductMoveCallContractTest test`.
Depois de estabilizar produção: `python3 -B scripts/harness/lean.py fast`;
qualification-local no checkpoint semântico conforme a estratégia central.
Nesta preparação documental: `python3 -B scripts/harness/lean.py docs`.
