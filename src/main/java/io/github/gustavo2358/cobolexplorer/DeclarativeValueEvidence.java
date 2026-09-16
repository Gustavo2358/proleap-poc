package io.github.gustavo2358.cobolexplorer;

import java.math.BigInteger;
import java.util.*;

/** Recognized declaration value support. Never proves lifetime, allocation or alias separation. */
final class DeclarativeValueEvidence {
    record Fact(List<Integer> bytes) {
        Fact { bytes=List.copyOf(bytes); }
    }
    private DeclarativeValueEvidence() { }

    static Optional<Fact> extract(Ast.DataEntry declaration, StorageLayoutSemantics.Profile profile,
            Map<Integer,SemanticCoverage.Finding> coverage, Optional<BigInteger> groupExtent) {
        // A repeated element's VALUE is not the logical value of its containing aggregate.
        // This is positive multiplicity evidence, not an unknown-layout recall gate.
        if(declaration.clauses().stream().anyMatch(Ast.OccursClause.class::isInstance))return Optional.empty();
        var values=declaration.clauses().stream().filter(Ast.ValueClause.class::isInstance).map(Ast.ValueClause.class::cast).toList();
        if(values.size()!=1||!modeled(values.get(0),coverage))return Optional.empty();
        var pictures=declaration.clauses().stream().filter(Ast.PictureClause.class::isInstance).map(Ast.PictureClause.class::cast).toList();
        Optional<BigInteger> extent=Optional.empty();
        if(pictures.size()==1&&modeled(pictures.get(0),coverage))extent=pictures.get(0).textExtent().map(BigInteger::valueOf);
        else if(pictures.isEmpty()&&!declaration.children().isEmpty())extent=groupExtent;
        if(extent.isEmpty()||extent.get().signum()<=0)return Optional.empty();
        // A positively known non-DISPLAY representation is not text-byte evidence.
        // An unrelated or uninterpreted clause does not decide candidate visibility.
        if(declaration.clauses().stream().filter(Ast.UsageClause.class::isInstance).map(Ast.UsageClause.class::cast).anyMatch(u->!u.display()))return Optional.empty();
        var encoded=values.get(0).logicalText().flatMap(t->StorageAccessSemantics.encode(t.value(),profile));
        if(encoded.isEmpty()||BigInteger.valueOf(encoded.get().size()).compareTo(extent.get())>0)return Optional.empty();
        var fitted=new ArrayList<>(encoded.get());int length=extent.get().intValueExact();
        while(fitted.size()<length)fitted.add(0x40);
        return Optional.of(new Fact(fitted));
    }

    private static boolean modeled(Ast.Node node,Map<Integer,SemanticCoverage.Finding> coverage) {
        var finding=coverage.get(node.meta().id());
        return finding!=null&&finding.coverage()==SemanticCoverage.ConstructionCoverage.MODELED;
    }
}
