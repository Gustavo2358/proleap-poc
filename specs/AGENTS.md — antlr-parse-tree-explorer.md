# AGENTS.md

## Scope

These instructions apply to the entire `antlr-parse-tree-explorer/` directory.

This project is a semantic COBOL analysis tool. Correctness is defined by the COBOL language semantics and by the explicit architectural contracts of this project — **not by the currently visible corpus, fixtures, snapshots, or tests**.

The primary pipeline is:

```text
COBOL source
    ↓
normalization / preprocessing
    ↓
ANTLR parse tree
    ↓
AST
    ↓
symbol tables / semantic entities
    ↓
reference occurrences
    ↓
reference resolution
    ↓
future analyses such as CFG and dataflow
```

Preserve the boundaries between these stages.

---

# 1. Core Correctness Principle

> Tests are evidence of correctness. Tests are not the definition of correctness.

Never derive production behavior merely from the examples that currently exist in:

- `corpus/`;
- `src/test/resources/`;
- existing unit tests;
- generated `dist*` artifacts;
- a single production example supplied during a task.

A solution is acceptable only if its behavior follows from one of:

1. the COBOL language or configured dialect semantics;
2. an explicit project specification;
3. an architectural invariant documented by the project;
4. an explicitly approved product requirement.

If the required behavior cannot be established from one of these sources, treat the requirement as uncertain instead of inventing a rule from examples.

---

# 2. Specification Before Implementation

For every non-trivial semantic change, do not begin by editing code.

First determine:

1. **What semantic rule is being implemented?**
2. **What is the domain of valid inputs?**
3. **What different semantic classes of inputs exist?**
4. **What assumptions are required?**
5. **What algorithm implements the rule generally?**
6. **Is the algorithm exact?**
7. **If not exact, is it sound, complete, or neither?**
8. **What evidence will demonstrate correctness beyond the reported example?**

For language-semantics questions, prefer authoritative documentation for the configured COBOL dialect. Existing project specifications and previous semantic-hardening reports are also authoritative within their documented scope.

Do not infer a COBOL rule solely by observing how one fixture parses.

---

# 3. Explicit Assumption Audit

Before implementing a non-trivial algorithm, identify assumptions that affect correctness.

Classify each important assumption as one of:

```text
SPECIFICATION_GUARANTEED
LANGUAGE_GUARANTEED
ARCHITECTURE_GUARANTEED
OBSERVED_IN_CURRENT_CORPUS_ONLY
UNCERTAIN
```

`OBSERVED_IN_CURRENT_CORPUS_ONLY` is not sufficient justification for production logic.

`UNCERTAIN` assumptions must not silently become implementation rules.

Example of an invalid inference:

```text
All current CALL examples use literal targets.
Therefore CALL targets can be treated as literals.
```

Correct interpretation:

```text
The current corpus contains literal CALL targets,
but COBOL also permits dynamic targets.
The AST must preserve that distinction.
```

---

# 4. Exact Algorithms Over Heuristics

Prefer established, semantics-driven algorithms over ad-hoc heuristics.

The order of preference is:

```text
1. Exact algorithm
2. Formally characterized conservative approximation
3. Explicitly documented heuristic
```

A heuristic is a last resort.

Do not introduce a heuristic merely because it makes a failing fixture pass.

Examples of prohibited substitutions include:

- regex instead of structural parsing when grammar information exists;
- text scanning instead of AST traversal;
- selecting the first symbol with a matching name instead of applying COBOL resolution rules;
- choosing the nearest declaration because it works for the current program;
- hardcoded handling of identifiers appearing in fixtures;
- special casing file names or program names from the corpus;
- assuming a single program unit, section, paragraph, scope, or declaration shape because current fixtures happen to have one;
- inferring runtime values from syntax when dataflow analysis is required.

If an exact solution is computationally expensive, optimize the exact algorithm using appropriate data structures, indexes, caching, memoization, worklists, or precomputation.

Do not silently replace semantic correctness with a cheaper heuristic.

---

# 5. Heuristic Policy

If an exact implementation is impossible or impractical, do not hide that fact.

A heuristic implementation must explicitly document:

- why an exact solution is unavailable or impractical;
- the input domain for which the heuristic is expected to work;
- whether the result is sound;
- whether the result is complete;
- possible false positives;
- possible false negatives;
- how uncertainty is represented.

A heuristic must never masquerade as an exact semantic result.

Prefer:

```text
UNKNOWN
UNRESOLVED
UNSUPPORTED
AMBIGUOUS
INCOMPLETE
```

or another explicit conservative result over a fabricated answer.

When static analysis discovers several semantically valid possibilities, preserve the set of possibilities rather than arbitrarily selecting one.

---

# 6. Fail Closed on Semantic Uncertainty

This project favors conservative analysis.

Never translate:

```text
"I could not understand this construct"
```

into:

```text
"This construct has no semantic effect."
```

Examples:

- missing COPYs are not equivalent to COPYs with no dependencies;
- unsupported syntax is not equivalent to an empty statement;
- an unresolved reference is not equivalent to a nonexistent dependency;
- an opaque embedded language statement is not equivalent to a statement without dependencies;
- an ambiguous symbol is not equivalent to the first matching symbol;
- a dynamic CALL is not equivalent to no CALL;
- a nominal binding to a variable is not equivalent to knowing the variable's runtime value.

When analysis is incomplete, make incompleteness observable.

Diagnostics are part of semantic correctness.

---

# 7. Respect Analysis Boundaries

Do not solve a later analysis phase by smuggling heuristics into an earlier phase.

## Parse tree

Represents grammar-recognized syntax.

Do not attach semantic conclusions that belong to later stages.

## AST

The AST is a semantic structural model.

It must remain independent from:

- symbol tables;
- reference-resolution results;
- CFG;
- reaching definitions;
- constant propagation;
- dependency facts.

Do not add semantic bindings or dataflow state directly to AST nodes merely because doing so simplifies one implementation.

## Symbol tables

Symbol tables model declarations, scopes, namespaces, identity, and language-defined visibility.

They must not infer runtime values.

## Reference resolution

Reference resolution binds nominal occurrences according to COBOL semantic rules.

It must not perform dataflow analysis.

For example:

```cobol
CALL WS-CALL-TARGET
```

Nominal reference resolution may determine which declaration
`WS-CALL-TARGET` refers to.

It must **not** claim to know the possible program names contained in
`WS-CALL-TARGET`.

That belongs to future CFG/dataflow/value-resolution analysis.

## Embedded languages

`EXEC SQL`, `EXEC CICS`, `EXEC SQLIMS`, and other embedded languages may remain opaque until their dedicated analysis exists.

Do not extract semantic dependencies from them using opportunistic regexes and present those results as complete semantic analysis.

---

# 8. COBOL Semantics Must Drive Resolution

COBOL name resolution is not equivalent to a generic lexical-scope lookup.

Do not assume modern-language rules automatically apply.

Resolution logic must consider, where relevant:

- program units and nested programs;
- COBOL namespaces;
- scope of names;
- `GLOBAL`;
- `EXTERNAL`;
- qualification with `OF` / `IN`;
- shadowing;
- group hierarchy;
- FILE declarations;
- condition names;
- index names;
- procedure sections and paragraphs;
- ambiguity rules;
- case-insensitive canonical names;
- dialect-specific behavior.

Do not reduce these rules to:

```text
walk parents until a matching string is found
```

unless the language rule for that exact reference category actually reduces to that algorithm.

The semantic category of a candidate matters.

The point at which shadowing occurs also matters.

Qualification order matters.

Program-unit visibility matters.

When these interact, derive the algorithm from the COBOL rule rather than from the organization of the current Java classes.

---

# 9. Do Not Confuse Structural Scope with Complete Visibility

The structural scope tree is useful infrastructure.

It is not, by itself, the complete visibility relation of COBOL.

Language features may make entities visible outside simple parent-child lexical traversal.

Keep these concepts separate:

```text
structural containment
lexical organization
namespace
language visibility
name resolution
semantic ownership
```

Do not distort the structural scope tree merely to make one lookup operation easier.

Encode language visibility in the appropriate semantic algorithm.

---

# 10. Parse Structure Before Text Heuristics

When ANTLR grammar structure or AST structure can answer a question, use it.

Prefer:

```text
grammar rule
    ↓
parse-tree structure
    ↓
typed AST representation
```

over:

```text
source substring
    ↓
regex
    ↓
guess
```

Regex is acceptable for genuinely lexical or formatting-oriented operations when its domain is explicit and bounded.

Regex must not become a substitute for parsing or semantic analysis.

Never use source-text patterns to re-derive information that is already structurally represented by the parser or AST.

---

# 11. Corpus Is Evidence, Not Specification

The corpus exists to exercise the implementation.

It does not define COBOL.

Never write logic such as:

```java
if (programName.equals("COACTUPC")) { ... }
```

or logically equivalent behavior.

Avoid assumptions derived from observations such as:

```text
"The corpus never contains..."
"All current programs..."
"The example always..."
"This fixture happens to..."
```

unless the restriction is an explicit scope decision documented by the project.

When a new production program exposes a previously unseen language form, determine the general semantic category that the program represents and implement that category.

Do not patch only the observed instance.

---

# 12. No Fixture-Driven Production Logic

Never inspect fixture-specific details and encode them into production logic.

Red flags include:

- test literal strings appearing in production algorithms;
- conditions checking test resource names;
- special cases for current corpus program names;
- numeric constants derived from one fixture;
- positional assumptions derived from one parse tree;
- logic that recognizes only the exact syntax used by the failing test;
- branches introduced solely to satisfy one assertion without a semantic explanation.

Every production branch introduced for a bug fix should be explainable using a general semantic rule.

A useful review question is:

> What class of valid COBOL programs does this branch represent?

If the answer is only:

> The program from the failing test.

the implementation is overfitted.

---

# 13. Tests Must Be Derived From the Rule

When fixing a semantic bug, derive tests independently from the implementation.

Preferred sequence:

```text
semantic rule
     ↓
equivalence classes
     ↓
adversarial examples
     ↓
expected semantic behavior
     ↓
implementation
```

Avoid:

```text
implementation
     ↓
tests that reproduce implementation behavior
```

For semantic changes, include tests that would fail plausible incorrect implementations.

A test that only proves the happy path is insufficient when the bug concerns resolution, ambiguity, visibility, scope, parsing, or control flow.

---

# 14. Equivalence-Class Testing

Identify semantic classes relevant to the rule.

For reference resolution this may include, depending on the feature:

```text
no declaration
one valid declaration
multiple valid declarations
local declaration
ancestor declaration
GLOBAL declaration
LOCAL declaration
same name / different namespace
same name / different symbol kind
qualified reference
unqualified reference
valid qualification
invalid qualification
shadowed declaration
nested program
multiple nesting levels
ambiguous declaration
case variation
unsupported construct
```

Do not create dozens of nearly identical examples while leaving entire semantic classes untested.

---

# 15. Adversarial Tests Are Required for Semantic Fixes

A semantic bug fix should include at least one adversarial case designed to break the obvious naive solution.

Example:

If the bug is:

```text
GLOBAL symbol in parent program is not visible in child
```

do not test only:

```text
parent GLOBAL + child reference
```

Also consider:

```text
parent GLOBAL + child local homonym
parent LOCAL + child reference
GLOBAL at multiple nesting levels
qualification selecting between candidates
same spelling in incompatible namespace
```

The objective is not maximum test count.

The objective is to distinguish the correct algorithm from plausible incorrect algorithms.

---

# 16. Property-Based Testing

When a behavior can be expressed as a general invariant, prefer property-based testing in addition to example tests.

Useful properties may include:

### Name canonicalization

For identifiers where COBOL semantics are case-insensitive:

```text
resolve("customer-id") == resolve("CUSTOMER-ID")
```

provided the transformation does not alter literals or case-sensitive external data.

### Determinism

Given the same semantic input and policy:

```text
analyze(program) == analyze(program)
```

### AST immutability

Analysis phases must not mutate the AST.

### Resolution identity

A resolved occurrence must point to a semantic entity that actually exists in the appropriate compilation-unit model.

### Provenance

Every semantic node or occurrence claiming source provenance must map to a valid source location.

Use a property-testing library such as jqwik if the benefit justifies the dependency.

Do not add dependencies merely to satisfy this guideline when ordinary generated tests provide equivalent value.

---

# 17. Metamorphic Testing

Use metamorphic relations when the exact expected output is cumbersome but semantic invariance is known.

Examples potentially useful in this project:

### Case transformation

Changing the case of COBOL identifiers should preserve semantic binding where COBOL rules define names as case-insensitive.

### Irrelevant sequence-area changes

Changing sequence-area contents should not alter semantic AST behavior when those columns are semantically irrelevant under the configured source-format policy.

### Comments

Adding a semantically irrelevant comment should not change semantic dependencies.

### Explicit unambiguous qualification

Adding a qualifier that uniquely identifies the entity already selected by an unqualified reference should preserve the selected semantic entity when the language rules permit it.

### Renaming unrelated declarations

Renaming an unrelated identifier should not change resolution of an independent reference.

When creating metamorphic tests, state the semantic relation explicitly.

Do not assume a transformation is semantics-preserving without checking COBOL rules.

---

# 18. Differential and Reference-Oracle Testing

When a trustworthy oracle exists, use it.

Possible oracles include:

- authoritative compiler/parser behavior;
- language specification examples;
- a simpler exact implementation;
- an exhaustive algorithm restricted to small inputs;
- an independently implemented semantic model.

For complicated optimized algorithms, a slow but obviously correct implementation for small cases is valuable test infrastructure.

Example pattern:

```text
optimized(input) == referenceExact(input)
```

for generated small inputs.

Do not treat the current implementation as its own oracle.

---

# 19. Mutation Testing and Test Strength

Coverage alone does not demonstrate semantic correctness.

A test suite with high line coverage can still accept an incorrect resolver.

When practical, assess whether tests detect mutations such as:

- changing lookup order;
- changing `GLOBAL` to `LOCAL`;
- selecting the first candidate;
- ignoring qualifiers;
- dropping ambiguity;
- changing a boundary comparison;
- treating `UNSUPPORTED` as `RESOLVED`;
- removing an ancestor search;
- swapping namespace filters.

For critical semantic algorithms, mutation testing tools such as PIT may be introduced when explicitly useful.

Do not optimize for a coverage percentage at the expense of meaningful assertions.

---

# 20. Independent Challenge Pass

After implementing a non-trivial semantic change, perform a separate adversarial review.

Do not ask only:

```text
Why is this implementation correct?
```

Also ask:

```text
How could this implementation be wrong?
```

Try to construct a valid input that violates its assumptions.

Review specifically for:

- corpus-specific assumptions;
- unhandled nesting;
- hidden shadowing interactions;
- namespace collisions;
- ambiguous references;
- unsupported syntax converted into successful analysis;
- order dependence;
- incomplete candidate sets;
- accidental text-based heuristics;
- algorithmic shortcuts justified only by current tests.

If a counterexample is found, fix the general algorithm and add the counterexample as a regression test.

---

# 21. Soundness and Completeness

For semantic analyses, reason explicitly about soundness and completeness when relevant.

## Soundness

Ask:

> Can the analysis report a semantic fact that is not actually possible?

## Completeness

Ask:

> Can the analysis fail to report a semantic fact that is possible?

The desired tradeoff depends on the analysis.

For dependency discovery, silently losing a possible dependency can be more dangerous than conservatively reporting multiple possible targets.

Preserve uncertainty instead of collapsing it prematurely.

When an analysis is intentionally incomplete, expose that incompleteness in its result or diagnostics.

---

# 22. Correctness Argument for Non-Trivial Algorithms

For complex semantic algorithms, provide a short correctness argument before considering the task complete.

It should explain:

### Input domain

What inputs the algorithm handles.

### Invariant

What remains true during processing.

### Soundness

Why returned candidates/results are semantically valid.

### Completeness

Why required valid candidates/results are not accidentally discarded, or where completeness intentionally ends.

### Termination

Why processing terminates.

### Complexity

Time and memory complexity at a useful level.

This does not need to be a formal proof.

It must be strong enough to reveal hidden fixture-specific assumptions.

---

# 23. Performance Must Not Change Semantics

Performance matters for this project because real COBOL programs can be very large.

However:

> Performance optimization must preserve the semantic algorithm.

Prefer improvements such as:

- indexing;
- maps keyed by canonical identifiers;
- immutable precomputed indexes;
- caching;
- memoization;
- worklists;
- avoiding repeated AST traversals;
- avoiding unnecessary allocations;
- using appropriate graph algorithms;
- computing facts once per program unit;
- lazy materialization when semantics permit it.

Do not optimize by:

- limiting analysis to a fixed number of candidates;
- ignoring uncommon constructs;
- scanning only nearby lines;
- stopping search because the current corpus never needs deeper traversal;
- replacing grammar/AST analysis with regex;
- discarding ambiguity;
- dropping `UNKNOWN` possibilities.

If a performance optimization changes the mathematical or semantic problem being solved, it is not merely an optimization and must be treated as a design decision.

Measure before introducing semantic compromises.

---

# 24. Large-Input Complexity Review

For algorithms that traverse ASTs, symbol tables, future CFGs, or dataflow structures, explicitly inspect complexity.

Be suspicious of accidental:

```text
O(nodes × symbols)
O(references × all declarations)
O(blocks²)
O(paths)
```

behavior on large programs.

Prefer indexed lookup and monotone/worklist algorithms where appropriate.

Do not accept exponential path enumeration when a standard dataflow formulation can solve the same semantic problem.

When a known compiler/static-analysis algorithm exists, prefer the established algorithm over a custom traversal heuristic.

---

# 25. Future CFG and Dataflow Work

When CFG construction is introduced, build actual control-flow semantics.

Do not approximate control flow from textual order alone.

The CFG must eventually account for relevant COBOL constructs such as:

- sequential flow;
- `IF`;
- `EVALUATE`;
- `PERFORM`;
- `GO TO`;
- `GO TO ... DEPENDING ON`;
- paragraph/section transfers;
- sentence boundaries;
- termination constructs;
- fallthrough where semantically valid.

Likewise, value resolution should use an explicit dataflow formulation rather than arbitrary backward text scanning when multiple control-flow paths matter.

If a simpler analysis is intentionally used, characterize its soundness and completeness explicitly.

---

# 26. Static Dependency Resolution

When future analyses resolve dynamic dependency targets, preserve all statically possible results.

Conceptually prefer:

```text
possibleTargets = {A, B, C}
dynamicRemainder = true
```

over:

```text
target = A
```

when the analysis cannot prove `A` is the only possibility.

Unknown information must not erase known information.

A partially resolved dependency is preferable to either:

- inventing certainty; or
- discarding all known possibilities.

---

# 27. Preserve Provenance

Source provenance is part of the analysis contract.

Transformations must not silently detach semantic results from their source origin.

When changing:

- normalization;
- COPY expansion;
- preprocessing;
- AST construction;
- occurrence collection;
- semantic resolution;

verify that source mappings remain coherent.

Do not rebuild an identity source map in the middle of a transformed pipeline to make location handling easier.

If provenance becomes approximate rather than exact, that distinction must remain observable.

---

# 28. Preserve Ambiguity

Ambiguity is information.

Do not resolve ambiguity by:

- taking the first candidate;
- sorting candidates and selecting one;
- preferring whichever declaration appears first in the corpus;
- choosing the closest candidate without a language rule requiring it.

If COBOL semantics do not uniquely select a candidate, return an explicit ambiguous result and preserve the candidate set.

---

# 29. Generated Artifacts and Fixtures

Do not modify:

- corpus inputs;
- existing fixtures;
- grammar files;
- generated outputs;

merely to make a production implementation pass tests.

A fixture may be changed only when the fixture itself is proven inconsistent with the intended semantic rule.

A grammar may be changed only when the grammar is the actual source of the semantic/syntactic defect.

Generated artifacts should reflect production behavior, not drive it.

Never weaken a test because a new implementation fails unless the previous expectation is proven semantically incorrect.

---

# 30. Bug-Fix Workflow

For semantic bugs, use this sequence.

## Step 1 — Reproduce

Create the smallest useful example that demonstrates the semantic category.

## Step 2 — Identify the rule

Write down the COBOL/project semantic rule that determines the expected behavior.

## Step 3 — Generalize

Identify neighboring semantic classes and likely counterexamples.

## Step 4 — Write tests

Tests should distinguish the correct algorithm from likely incorrect shortcuts.

## Step 5 — Design

Describe the general algorithm and where in the pipeline it belongs.

## Step 6 — Implement

Make the smallest architectural change that correctly implements the general rule.

## Step 7 — Challenge

Try to falsify the implementation using adversarial inputs.

## Step 8 — Regress

Run the complete relevant test suite and existing regression gates.

---

# 31. When a Test Fails

Do not immediately modify production code.

First determine which of these is true:

```text
A. production violates the specification;
B. test expectation violates the specification;
C. fixture is invalid;
D. grammar does not represent the intended syntax;
E. architecture does not yet support the required semantic phase;
F. requirement is ambiguous.
```

Only case `A` directly implies a production bug.

Passing the test is not itself evidence that the chosen fix is correct.

---

# 32. Minimal Fix Does Not Mean Narrow Fix

Prefer minimal changes in architectural surface area.

Do not interpret "minimal" as:

> Handle only the failing example.

A good minimal fix changes the smallest amount of code necessary to implement the **general semantic rule**.

A narrow special case is not a minimal fix if it leaves the underlying semantic defect intact.

---

# 33. Avoid Premature Abstraction and Premature Generalization

The anti-overfitting policy does not mean building speculative infrastructure for every possible future COBOL feature.

Implement the general rule for the explicitly supported semantic domain.

It is acceptable to return `UNSUPPORTED` outside that domain.

Prefer:

```text
small exact supported domain + explicit unsupported boundary
```

over:

```text
large claimed domain + heuristic behavior
```

Do not implement CFG, SQL analysis, CICS analysis, reaching definitions, or other future phases merely because they might eventually be necessary, unless the current task explicitly requires them.

---

# 34. Architecture Before Convenience

Do not cross architectural boundaries simply because another layer already has convenient information.

Examples:

- `SymbolTableBuilder` should not depend on ANTLR parser classes if the AST already defines its input contract.
- reference-resolution results should not be written into AST nodes;
- rendering DTOs should not become semantic domain models;
- frontend needs should not dictate semantic identity;
- parser token positions should not replace source provenance abstractions;
- future CFG/dataflow state should not leak into nominal reference resolution.

Prefer adding an explicit domain abstraction over coupling semantic phases together.

---

# 35. Diagnostics Are First-Class Results

Diagnostics should help a developer determine:

- what construct was encountered;
- where it came from;
- why analysis could not proceed exactly;
- which semantic rule was involved;
- which candidates were considered where relevant;
- whether the result is ambiguous, unsupported, unresolved, or incomplete.

Avoid vague diagnostics such as:

```text
Could not resolve reference
```

when the system knows more.

Prefer something conceptually equivalent to:

```text
Reference FOO could not resolve as DATA because the nearest
visible declaration with that name belongs to an incompatible
semantic namespace.
```

Keep diagnostics deterministic and testable.

---

# 36. Comments Must Explain Semantics, Not History

Useful comments explain:

- COBOL semantic rules;
- algorithm invariants;
- non-obvious architectural boundaries;
- reasons for conservative behavior;
- reasons a tempting shortcut is invalid.

Avoid comments such as:

```text
Special case for test X
Fix for CBSTM03D
This made the failing test pass
```

Production code should describe the semantic rule, not the history of the bug report.

---

# 37. Code Review Red Flags

Treat the following patterns as requiring additional scrutiny:

```text
contains(...)
startsWith(...)
endsWith(...)
substring(...)
regex
first()
findFirst()
limit(...)
magic integer
magic program name
magic paragraph name
special-case branch
catch-and-ignore
fallback to empty collection
fallback to first candidate
```

These operations are not inherently wrong.

They become suspicious when used to replace a semantic rule.

For every such usage in semantic code, ask:

> What language rule justifies this operation?

---

# 38. Definition of Done for Semantic Changes

A semantic change is complete only when all applicable items below are satisfied:

- [ ] The governing semantic rule is identified.
- [ ] The implementation is not derived solely from current fixtures.
- [ ] Important assumptions have been audited.
- [ ] The algorithm's supported domain is clear.
- [ ] Exact behavior is preferred where practical.
- [ ] Any approximation is explicitly characterized.
- [ ] `UNKNOWN` / `UNSUPPORTED` / `UNRESOLVED` states remain observable where appropriate.
- [ ] Architectural phase boundaries are preserved.
- [ ] At least one regression test covers the original failure.
- [ ] Adversarial cases cover plausible incorrect implementations.
- [ ] Relevant equivalence classes were considered.
- [ ] Ambiguity is preserved rather than arbitrarily collapsed.
- [ ] Existing tests continue to pass.
- [ ] Existing regression gates continue to pass where applicable.
- [ ] Performance complexity is reasonable for large COBOL programs.
- [ ] No corpus-specific production rule was introduced.
- [ ] No fixture or grammar was weakened merely to make the implementation pass.
- [ ] Diagnostics remain meaningful.
- [ ] Provenance remains valid.
- [ ] A challenge pass attempted to find a counterexample.

---

# 39. Final Self-Review

Before finishing a task involving semantic production code, ask:

```text
1. Did I solve the semantic problem or only the reported example?

2. What valid COBOL input would most likely break this implementation?

3. Did any behavior come from observing the fixture rather than
   from the specification?

4. Did I replace a formal language/analysis problem with a regex,
   text scan, nearest-match rule, or other heuristic?

5. Am I claiming knowledge that belongs to a later analysis phase?

6. If the analysis cannot know something exactly, do I preserve
   uncertainty explicitly?

7. Can I explain why the algorithm is sound?

8. Can I explain where it is complete and where it is intentionally
   incomplete?

9. Would the implementation still make sense if every current
   fixture were replaced tomorrow?

10. Would a COBOL expert recognize the algorithm as an implementation
    of the language rule rather than an implementation of our tests?
```

If any answer exposes fixture-specific reasoning, hidden uncertainty, or a semantic shortcut, revisit the implementation before declaring the task complete.

---

# Guiding Principle

When forced to choose between:

```text
a simple implementation that is semantically wrong
```

and:

```text
an explicit unsupported/incomplete result
```

choose the explicit unsupported/incomplete result.

When forced to choose between:

```text
a heuristic that passes the current corpus
```

and:

```text
a principled algorithm that implements the supported semantic domain
```

choose the principled algorithm.

The purpose of this project is not to recognize the programs we already have.

The purpose is to model COBOL semantics well enough that programs we have never seen before behave according to the same rules.