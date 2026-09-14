package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Typed source effect proof, independent of value transforms. References are the
 * existing AST occurrences; physical addresses remain owned by storage analysis. */
public record StatementEffectSummary(List<Ast.DataReference> knownReads,
        List<Ast.DataReference> mayWrites, List<Ast.DataReference> mustOverwrite,
        List<Ast.DataReference> exposedRegions, Bound unknownReadBound,
        Bound unknownWriteBound, Bound unknownExposureBound, Environment environment,
        ValueTransform values, Proof proof) {
    public enum Bound { NONE, ALL }
    public enum Environment { OUTPUT, INPUT, UNKNOWN }
    public enum ValueTransform { NONE, UNKNOWN }
    public enum Proof { DISPLAY_SIMPLE }
    public StatementEffectSummary {
        knownReads=List.copyOf(knownReads);mayWrites=List.copyOf(mayWrites);
        mustOverwrite=List.copyOf(mustOverwrite);exposedRegions=List.copyOf(exposedRegions);
        Objects.requireNonNull(unknownReadBound);Objects.requireNonNull(unknownWriteBound);
        Objects.requireNonNull(unknownExposureBound);Objects.requireNonNull(environment);
        Objects.requireNonNull(values);Objects.requireNonNull(proof);
        if(!mayWrites.containsAll(mustOverwrite))throw new IllegalArgumentException("MUST requires a write occurrence");
    }
    public boolean completeMutationBound() {return unknownWriteBound==Bound.NONE&&unknownExposureBound==Bound.NONE;}
    public static Optional<StatementEffectSummary> of(Ast.Statement statement) {
        if(statement instanceof Ast.ModeledStatement s)return s.effects();
        if(statement instanceof Ast.PreservedStatement s)return s.effects();
        return Optional.empty();
    }
}
