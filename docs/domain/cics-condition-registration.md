# HANDLE CONDITION: ordinary registration

A HANDLE CONDITION updates CICS handler state and returns to the next statement.
It does not call the registered paragraph at registration time and does not write
COBOL application data. Future exception dispatch remains a separate open fact.

Authority: [IBM CICS TS programming guide](https://www.ibm.com/docs/en/SSJL4D_6.x/pdf/application-programming_pdf.pdf),
[IBM CICS primer](https://www.ibm.com/docs/SSJL4D_6.x/pdf/cics-primer.pdf),
and the [condition catalog](https://www.ibm.com/docs/en/cics-ts/6.x?topic=programs-mapping-between-cics-conditions-jcics-exceptions).
The documented maximum of 16 conditions is a syntax constraint. A condition
without a label restores its default action. Duplicate/unknown options and bad
label syntax are unsupported. Procedure grammar parses labels; nominal resolution
must identify every supplied paragraph before ordinary completion is published.

Source effects carry CICS_CONDITION_REGISTRATION: no application memory reads,
writes or escapes; external environment remains UNKNOWN. The existing canonical
topology publishes only ordinary completion. Lowering consumes that proof and
retains the external effect. No ABEND state, handler dispatch or runtime outcome
is inferred. SP 2.46 admits this proof; earlier versions reject it.

Work is linear in command size and label count using the existing symbol index.
Synthetic tests cover multiple conditions/default restoration, unknown labels,
malformed/duplicate options, external effects and absence of immediate dispatch.
