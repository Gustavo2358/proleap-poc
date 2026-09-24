# Fact dependency locality — SP 2.40.0

Status: implemented, qualification in progress. R1/R1-R1 remains approved; W8 is
superseded and not resumed. No control.local or backend model change belongs here.

## Authority and migration

The frontend publishes `factDependencies` with authority
`FRONTEND_FACT_DEPENDENCY_LOCALITY_R2`. SP 2.40 requires this graph and the unchanged
R1 ControlTopology. SP <=2.39 preserves historical decoding and lowering and rejects
the new field. The graph is a closed typed domain value; JSON is only transport.

A fact has identity, facet, subject, region and proof dependencies. Availability is
the conjunction of local premises, prerequisite proofs and input availability.
Inputs identify missing COPY, opaque include, unlocated input and physical profile;
they carry explicit affected context/closure regions. Diagnostics are not proof.
Deleting required proof kinds, input relations, region bounds or referenced targets
rejects on wire and in memory. Lists are canonical and duplicates/cycles are invalid.

Source identity, logical TEXT suitability, nominal storage identity, exact local
cell and physical view are separate facets. Known syntax never implies known bytes,
a complete alias inventory, an expanded COPY or an exact dynamic CALL target.

## Rule and algorithm

The language oracle is the [IBM Enterprise COBOL 6.4 Language Reference](https://publibfp.dhe.ibm.com/epubs/pdf/igy6lr40.pdf):
Working-Storage pp163–164, REDEFINES pp225–227, RENAMES pp228–229 and COPY pp688ff.
COPY replaces source text; unknown content is not empty. REDEFINES shares an
explicit preceding storage description; RENAMES belongs to its described record.
A known subsequent independent record or section boundary can close the previous
record. Lexical distance and COPY/program names prove nothing.

The frontend uses the AST hierarchy, existing storage components/alias relations,
visibility, and preprocessing source-map gap occurrences. It publishes regional
context, boundary, closure, alias-inventory/closure, allocation, logical-type and
physical-profile proofs. A gap inside an open record revokes closure. A gap before
an unproved header revokes context. An unlocated parser/I/O/input gap remains
unavailable even when another COPY has a located occurrence. Opaque SQL INCLUDE
stays an unavailable input; no SQLCA expansion is fabricated.

A closed local component without unresolved alias/repetition may own independent
logical TEXT cells. Exact whole-item aliases use a shared cell only with existing
positive whole-item relation proof. Otherwise a published binding is a bounded
union of known cells and its nominal storage region, or explicitly unavailable.
Unknown extent does not erase a positively proved allocation identity/lifetime.
FILE record allocation reuses its existing positive allocation proof.

The lower validates references and proof availability, allocates IDs and translates
published bindings into existing AIR Cell/Region/UnknownBinding. It does not inspect
COPY spelling, source coordinates, COBOL hierarchy or program names to infer
independence. The new graph replaces the unit-wide partial-storage admission veto.
Legacy versions retain that veto. Existing AIR validation remains mandatory.

## Precision and limits

| Granularity | R2 treatment |
| --- | --- |
| Declaration | SOURCE_IDENTITY from local syntax; resolution ambiguity is not erased. |
| Logical value | LOGICAL_TEXT suitability is independent of physical profile; runtime values remain separate. |
| Storage identity | LOCAL_ALLOCATION plus region context; unsupported duration stays unavailable. |
| Physical view | Existing layout measures plus profile/context/closure; no default profile. |
| Alias/overlap | Existing explicit relations; exact cells need regional alias closure. |
| MOVE transfer | Existing positive transfer fact plus published destination binding; unknown does not become exact. |
| CALL target | Existing occurrence/evidence contract, bounded read or unavailable read, explicit remainders. |
| Predicate reads | Existing value capability consumes the same materialized declaration binding. |
| Section structure | Missing/opaque regions revoke their dependent proofs; source facts survive independently. |

This first slice is deliberately conservative after an unknown insertion and for
mixed alias/OCCURS components. Some existing allocation/root-relation and value
capabilities still require broader proof. R2 removes the global lower storage veto;
it does not claim that every remaining frontend semantic gate is fact-local.
Positive possible-value evidence with an unknown remainder is not an exact target.
No gap creates a write, call, control edge, AllMemory or AllControl.

Proof evaluation is a finite DAG traversal, O(proofs + dependency edges + input
relations). Canonical sorting adds sorting cost. Frontend gap/region classification
is O(inputs × regions), and descendant cell bounds cost their explicit membership
size and ancestry depth. There is no subset/path enumeration or arbitrary cutoff.
Activation specialization is unchanged and remains a measured transitional backend.

## Tests and evidence

`FactDependencyLocalityTest` is fixed FAST; it covers profile separation, closed/open
COPY regions, aliases, opaque includes, diagnostic removal, input renaming, storage
versus control, inventory permutation, causal mutations and 1/10/100/1000 local items.
`FactDependencyWireSuite` covers old/new decoding, typed roundtrip, malformed shape,
missing dependencies/bounds, alias/input negatives and deterministic AIR.

The campaign evidence is `fact-dependency-locality-r2/evidence/` in the aggregator,
including first-four and all56 storage checkpoints, full73 physical discovery,
frozen historical oracles, first-loss redistribution and raw performance logs.
