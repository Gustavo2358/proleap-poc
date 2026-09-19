package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** A proved preprocessing occurrence, captured before the directive is replaced. */
public record SourceDependencyFact(Kind kind, String name, String qualification,
        Resolution resolution, String artifact, String authority,
        Ast.SourceProvenance provenance, Ast.SourceLocation rootSite) {
    public enum Kind { COPYBOOK, DCLGEN, SQL_INCLUDE }
    public enum Resolution { RESOLVED, UNRESOLVED, CYCLIC, IO_ERROR }
    public SourceDependencyFact {
        Objects.requireNonNull(kind); Objects.requireNonNull(resolution);
        name=canonical(name); qualification=canonical(qualification);
        Objects.requireNonNull(artifact); Objects.requireNonNull(authority);
        Objects.requireNonNull(provenance); Objects.requireNonNull(rootSite);
        // This directive no longer exists after expansion. Name its local normalized
        // snapshot explicitly, rather than claiming a position in the final global text.
        var local=provenance.expanded();
        provenance=new Ast.SourceProvenance(new Ast.SourceLocation("preprocessing:"+provenance.original().file(),
            local.startLine(),local.startColumn(),local.endLine(),local.endColumn()),provenance.original(),provenance.includeChain(),provenance.exact());
        if(name.isBlank()||authority.isBlank())throw new IllegalArgumentException("source dependency needs name and authority");
        if((kind==Kind.DCLGEN)!=authority.equals("CONFIGURED_DCLGEN"))throw new IllegalArgumentException("DCLGEN requires positive inventory authority");
        if((resolution==Resolution.RESOLVED)==artifact.isBlank())throw new IllegalArgumentException("resolved dependency needs artifact");
    }
    static String canonical(String value){return Objects.requireNonNull(value).toUpperCase(Locale.ROOT);}
}
