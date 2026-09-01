# Estado — WORK-COND-001

## Onde estamos

Slice 1 de `BACKLOG-COND-001` promovido para work item ativo de risco `high`, limitado ao contrato normativo IBM de condições combinadas/abreviadas. Discovery parado no checkpoint anterior a qualquer decisão arquitetural ou mudança de produção.

Base: `main` no merge do PR #14 (`b44bfa40862a8c930c3b012f1dff9fc8676ffabb`). Branch: `discovery/work-cond-001-slice-1-normative-contract`.

## Verde conhecido

- PR #14 está mergeado na `main` e a branch deste work item foi criada diretamente desse merge commit.
- IBM confirma herança do último subject e último relational operator em abbreviated combined relation conditions.
- IBM confirma os terminadores da inserção: nova simple condition, condition-name e `)` correspondente a `(` situado à esquerda do subject.
- IBM diferencia o boundary parentético anterior da distribuição causada por `(` imediatamente após relational operator; nesse segundo caso subject/operator continuam correntes após o `)`.
- IBM diferencia `NOT` integrante de relational operator de logical NOT; logical NOT não é herdado.
- IBM define precedência `NOT` > `AND` > `OR` entre operadores lógicos, salvo parênteses.
- IBM admite index-name em relation conditions sob regras próprias de comparação.
- Level-66 `RENAMES` declara um data-name; não constitui namespace nominal concorrente.
- Condition-name exige uniqueness/qualification e, quando aplicável, os mesmos subscripts da conditional variable.
- O caso histórico DATA + CONDITION com a mesma user-defined word no mesmo programa foi fechado: é source IBM inválido, pois uma user-defined word pertence a somente um set de nomes. Não há precedência de binding a descobrir para esse caso.
- A distinção entre regra normativa e comportamento observado no PR #14 está explicitada em `spec.md` e `eval.md`.
- Nenhum arquivo de produção, grammar, arquitetura ou histórico foi alterado por este Slice 1.

## Restante

- Executar `./scripts/harness/check-fast.sh`, `./scripts/harness/check-semantic.sh` e `./scripts/harness/check-full.sh` em ambiente com checkout/JDK/Maven/Node. O ambiente de ferramentas desta sessão permite leitura/escrita GitHub, mas não fornece execução de shell sobre o repositório; a tentativa de clone no container não teve acesso de rede ao GitHub. Portanto os gates não são falsamente marcados como executados.
- Review humano do contrato e dos oracles.
- Somente após merge/review e nova autorização, promover o Slice 2 para comparar alternativas arquiteturais.

## Descobertas que afetam o plano

1. O sentinel “DATA + CONDITION homônimos no mesmo programa” do relatório histórico não é ambiguidade COBOL. Ele deve ser usado como oracle negativo de validade/frontend, não para decidir precedence entre namespaces.
2. `RENAMES` deve ser tratado no contrato nominal como DATA (level-66 data-name); criar categoria nominal RENAMES para resolver bare tails seria modelagem incorreta.
3. Parênteses exigem dois oracles opostos: `(A = B OR C) AND D` encerra a herança em `D`, enquanto `A = (B OR C) AND D` distribui o operador no grupo e mantém subject/operator correntes depois dele.
4. A escolha entre AST contextual, normalização no lowering e produto semântico pós-binding continua `UNCERTAIN` e deliberadamente fora deste slice.
5. Shapes aceitas pela grammar vendorizada não ganham status de COBOL válido sem sustentação IBM; o corpus e o parser permanecem evidência, não especificação.
