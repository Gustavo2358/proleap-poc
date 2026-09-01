# Estado — WORK-COND-001

## Onde estamos

Slice 1 de `BACKLOG-COND-001` promovido para work item ativo de risco `high`, limitado ao contrato normativo IBM de condições combinadas/abreviadas. Contrato e matriz de oracles documentais estão no checkpoint de Discovery anterior a qualquer decisão arquitetural ou mudança de produção.

Base: `main` no merge do PR #14 (`b44bfa40862a8c930c3b012f1dff9fc8676ffabb`). Branch: `discovery/work-cond-001-slice-1-normative-contract`.

## Verde conhecido

- PR #14 está mergeado na `main` e a branch deste work item foi criada diretamente desse merge commit.
- IBM confirma herança do último subject e último relational operator em abbreviated combined relation conditions.
- IBM confirma os terminadores da inserção: nova simple condition, condition-name e `)` correspondente a `(` situado à esquerda do subject.
- IBM diferencia o boundary parentético anterior da distribuição causada por `(` imediatamente após relational operator; nesse segundo caso subject/operator continuam correntes após o `)`.
- Distribuição não admite simple condition, outro relational operator nem logical `NOT` imediatamente após o `(` que abre seu scope.
- IBM diferencia `NOT` integrante de relational operator de logical NOT; logical NOT nega só a relation seguinte, que ainda pode ser abreviada, e não se propaga à continuação.
- IBM define precedência `NOT` > `AND` > `OR` entre operadores lógicos, salvo parênteses.
- IBM admite index-name em relation conditions sob regras próprias de comparação.
- Level-66 `RENAMES` declara um data-name; não constitui namespace nominal concorrente.
- Condition-name exige uniqueness/qualification e, quando aplicável, os mesmos subscripts da conditional variable; `IN` e `OF` são equivalentes.
- Scope IBM inclui nomes locais e globais elegíveis de programas contendo e aplica preferência ao programa interno/containing mais próximo quando mais de um recurso permanece identificado.
- O caso histórico DATA + CONDITION com a mesma user-defined word no mesmo programa foi fechado: é source IBM inválido, pois uma user-defined word pertence a somente um set de nomes. Não há precedência de binding a descobrir para esse caso.
- A distinção entre regra normativa e comportamento observado no PR #14 está explicitada em `spec.md` e `eval.md`.
- `eval.md` define 10 classes positivas, 8 negativas, 3 ambíguas/incertas e 11 adversariais. Os testes/fixtures do PR #14 permanecem caracterização; nenhum resultado atual foi promovido a regra IBM.
- `./scripts/harness/check-fast.sh`, `./scripts/harness/check-semantic.sh` e `./scripts/harness/check-full.sh` passaram em 2026-09-01; o `full` incluiu regressão E2E, invariantes estruturados e naming.
- Nenhum arquivo de produção, grammar, arquitetura, fixture/teste ou histórico foi alterado por este Slice 1.

## Restante

- Review humano do contrato e dos oracles.
- Oracle de compilador IBM não foi executado porque `cob2` não está disponível no ambiente. Isso não reabre as regras documentadas, mas mantém `UNCERTAIN` o wording/código exato dos diagnostics negativos.
- Somente após merge/review e nova autorização, promover o Slice 2 para comparar alternativas arquiteturais.

## Descobertas que afetam o plano

1. O sentinel “DATA + CONDITION homônimos no mesmo programa” do relatório histórico não é ambiguidade COBOL. Ele deve ser usado como oracle negativo de validade/frontend, não para decidir precedence entre namespaces.
2. `RENAMES` deve ser tratado no contrato nominal como DATA (level-66 data-name); criar categoria nominal RENAMES para resolver bare tails seria modelagem incorreta.
3. Parênteses exigem dois oracles opostos: `(A = B OR C) AND D` encerra a herança em `D`, enquanto `A = (B OR C) AND D` distribui o operador no grupo e mantém subject/operator correntes depois dele.
4. Logical `NOT` em `A = B OR NOT C OR D` nega a relation abreviada de `C`, mas não se propaga a `D`; tratar `C` como condition-name autônomo seria um falso requisito.
5. Referência que não fica única após qualification/scope é source inválido; preservar `AMBIGUOUS` e os candidates é postura diagnóstica conservadora do projeto, não semântica de execução válida.
6. A escolha entre AST contextual, normalização no lowering e produto semântico pós-binding continua `UNCERTAIN` e deliberadamente fora deste slice.
7. Shapes aceitas pela grammar vendorizada não ganham status de COBOL válido sem sustentação IBM; o corpus e o parser permanecem evidência, não especificação.
