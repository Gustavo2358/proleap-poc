package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** Typed source effect proof, independent of value transforms. References are the
 * existing AST occurrences; physical addresses remain owned by storage analysis. */
public record StatementEffectSummary(List<Ast.DataReference> knownReads,
        List<Ast.DataReference> mayWrites, List<Ast.DataReference> mustOverwrite,
        List<Ast.DataReference> exposedRegions, Bound unknownReadBound,
        Bound unknownWriteBound, Bound unknownExposureBound, Environment environment,
        ValueTransform values, Proof proof, List<Ast.DataReference> sourceTargets) {
    public enum Bound { NONE, ALL }
    public enum Environment { OUTPUT, INPUT, UNKNOWN, NONE }
    public enum ValueTransform { NONE, UNKNOWN }
    public enum Proof { NO_OP, DISPLAY_SIMPLE, INITIALIZE_TARGETS, ACCEPT_TARGET, SET_TARGETS, ARITHMETIC_TARGETS, STRING_TARGETS, UNSTRING_TARGETS, INSPECT_TARGETS }
    public StatementEffectSummary {
        knownReads=List.copyOf(knownReads);mayWrites=List.copyOf(mayWrites);
        mustOverwrite=List.copyOf(mustOverwrite);exposedRegions=List.copyOf(exposedRegions);
        sourceTargets=List.copyOf(sourceTargets);
        Objects.requireNonNull(unknownReadBound);Objects.requireNonNull(unknownWriteBound);
        Objects.requireNonNull(unknownExposureBound);Objects.requireNonNull(environment);
        Objects.requireNonNull(values);Objects.requireNonNull(proof);
        if(!mayWrites.containsAll(mustOverwrite))throw new IllegalArgumentException("MUST requires a write occurrence");
    }
    public StatementEffectSummary(List<Ast.DataReference> knownReads,
            List<Ast.DataReference> mayWrites, List<Ast.DataReference> mustOverwrite,
            List<Ast.DataReference> exposedRegions, Bound unknownReadBound,
            Bound unknownWriteBound, Bound unknownExposureBound, Environment environment,
            ValueTransform values, Proof proof) {
        this(knownReads,mayWrites,mustOverwrite,exposedRegions,unknownReadBound,
            unknownWriteBound,unknownExposureBound,environment,values,proof,mayWrites);
    }
    public boolean completeMutationBound() {return unknownWriteBound==Bound.NONE&&unknownExposureBound==Bound.NONE;}
    public static Optional<StatementEffectSummary> of(Ast.Statement statement) {
        if(statement instanceof Ast.ModeledStatement s)return s.effects();
        if(statement instanceof Ast.PreservedStatement s)return s.effects();
        return Optional.empty();
    }
}
