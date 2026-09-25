# R7-R6 — typed CICS frontier facts

- id: TYPED_CICS_FRONTIER_R7_R6
- status: IN_PROGRESS
- scope: SP2.43 source facts and bounded positive control for SYNCPOINT, RECEIVE MAP, SEND MAP.
- authority: explicit R7-R6 user authorization; [contract](../domain/cics-command-facts.md).
- no executable dispatch, AIR change, dependency publication, R8/R9/ALTER or merge.

First loss is OBSERVED/OPAQUE_CICS at real command occurrences; new source facts
and command-specific qualification address it. The next unsupported family stays
opaque. Frontend source probes, COPY/identity, positive and forbidden control,
metamorphics, wrong-code mutations and full CardDemo73 replay qualify the change.
