# SQL SELECT INTO — normal completion

IBM Db2 for z/OS [SELECT INTO](https://www.ibm.com/docs/en/db2-for-zos/12.0.0?topic=statements-select-into) assigns a returned row to host variables. [WHENEVER](https://www.ibm.com/docs/en/db2-for-zos/12.0.0?topic=statements-whenever) redirects exception conditions, not successful completion without a warning.

The canonical frontend recognizes a closed syntax profile: SELECT scalar columns, integer or character literals INTO the same number of simple host identifiers FROM one qualified or unqualified relation, optionally FETCH FIRST n ROW/ROWS ONLY. The parser consumes the entire retained EXEC SQL frame, without catalog or runtime assumptions. Other SQL remains unsupported. This rule licenses only the conditional ordinary successor for successful completion, accompanied by an unknown local outcome for errors, warnings, missing rows and omitted handling. It does not claim that the statement succeeds, that a row exists, or that WHENEVER handlers execute.

Memory, environment, result values and exception dispatch remain unknown under the existing observed-statement summary. No source relation name or catalog content becomes a program target. Procedure-input isolation remains required. Parsing is linear in payload length and list length, with bounded cursor progress and no recursive expression grammar.

Oracle: synthetic SQL followed by an actual CALL, with explicit ordinary and unknown outcomes; malformed SQL, count mismatch, unsupported expressions, trailing content and missing procedure input must not acquire this proof. The downstream test checks actual qualified source and AIR, never an injected name.
