package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** A proved preprocessing occurrence, captured before the directive is replaced. */
public record SourceDependencyFact(Kind kind, String name, String qualification,
        Resolution resolution, String artifact, String authority,
        Ast.SourceProvenance provenance, Ast.SourceLocation rootSite, String operation, String access) {
    public SourceDependencyFact(Kind kind,String name,String qualification,Resolution resolution,String artifact,String authority,Ast.SourceProvenance provenance,Ast.SourceLocation rootSite){this(kind,name,qualification,resolution,artifact,authority,provenance,rootSite,"NONE","NONE");}
    public enum Kind { COPYBOOK, DCLGEN, SQL_INCLUDE, DB2_TABLE }
    public enum Resolution { RESOLVED, UNRESOLVED, CYCLIC, IO_ERROR, NOT_APPLICABLE }
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
        Objects.requireNonNull(operation);Objects.requireNonNull(access);
        if((kind==Kind.DB2_TABLE)!=authority.equals("STATIC_SQL_TABLE_POSITION")||(kind==Kind.DB2_TABLE)!=(resolution==Resolution.NOT_APPLICABLE))throw new IllegalArgumentException("DB2 source authority/resolution");
        if(name.isBlank()||authority.isBlank())throw new IllegalArgumentException("source dependency needs name and authority");
        if((kind==Kind.DCLGEN)!=authority.equals("CONFIGURED_DCLGEN"))throw new IllegalArgumentException("DCLGEN requires positive inventory authority");
        if((resolution==Resolution.RESOLVED)==artifact.isBlank())throw new IllegalArgumentException("resolved dependency needs artifact");
    }
    static String canonical(String value){return Objects.requireNonNull(value).toUpperCase(Locale.ROOT);}
}
