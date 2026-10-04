# Logical numeric values and integer DISPLAY MOVE

Checkpoint 2, SP 2.65.0. Status: IN_PROGRESS.

## Rule and invariant

IBM Enterprise COBOL 6.4 [elementary MOVE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=statement-elementary-moves) aligns numeric operands at the decimal point; the receiver determines capacity. This cut proves unsigned, unedited integer DISPLAY receivers and integral fixed-point literals that fit, or integer DATA sources whose capacity fits. No byte encoding is inferred. Unsigned DISPLAY pictures have 1–31 digits under the documented IBM 6.4 profile; floating literals and decimal-comma syntax without a dialect proof remain unavailable. Signed fields, scale, overflow/truncation, edited pictures, binary/packed storage, subscripts and reference modification remain explicit.

A numeric transfer requires exact source syntax, a unique whole-item binding and an independently proved local cell. VALUE clauses and enclosing groups do not by themselves invalidate the cell. COPY/model, alias and allocation obligations remain causal. Numeric literals have a numeric value independently of MOVE readiness.

The frontend publishes per-receiver integer transfers on the existing MOVE fact. The consumer validates each certificate at both JSON and in-memory boundaries, and emits AIR INT assignments through the existing MOVE handler. A complete certificate set discharges only MOVE value/whole-item gaps. Unsupported peers retain conservative effects.

## Algorithm and oracle

One declaration index, one reference index, then one pass over MOVE receivers; no range enumeration, power-of-ten allocation or Boolean expansion. Capacity is compared by digit count. Expected examples: MOVE 12 TO PIC 9(3) writes INT 12; PIC 9(2) to PIC 9(4) preserves the value; MOVE 123 TO PIC 99 remains partial. Literal transfers to several receivers are independent; DATA transfers are admitted through the prefix that preserves the source value; later reads after an unsupported peer retain uncertainty.

Validate parser parity, literals, local/nested cells, alias/input rejection, multiple receivers, hostile certificates, large PIC counts, AIR validation and all 73 CardDemo sources against checkpoint 1. Preserve program/file/source dependency sites, candidates and remainder.

Primary references: [IBM numeric literals](https://www.ibm.com/docs/en/cobol-zos/6.3.0?topic=literals-numeric), [IBM 6.4 Language Reference](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf), elementary MOVE and alignment rules.

[IBM 6.4 USAGE](https://www.ibm.com/docs/en/cobol-zos/6.4.0?topic=entry-usage-clause) also applies group usage to elementary descendants. A memoized ancestor pass rejects inherited non-DISPLAY usage and unmodeled ancestor clauses (including GROUP-USAGE), even when the child has PIC 9. An explicit DISPLAY child cannot override an incompatible group usage.
