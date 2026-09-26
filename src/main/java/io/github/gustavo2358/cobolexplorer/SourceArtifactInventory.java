package io.github.gustavo2358.cobolexplorer;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Explicit input authority; filenames and SQL INCLUDE syntax never infer DCLGEN. */
final class SourceArtifactInventory {
    enum Kind { DCLGEN, SQL_INCLUDE }
    record Entry(String name, Kind kind, String artifact) {}
    record Document(String version, List<Entry> artifacts) {}
    private final Map<String,Entry> entries;
    private final Path directory;
    private SourceArtifactInventory(Map<String,Entry> entries,Path directory) {
        this.entries=Map.copyOf(entries);this.directory=directory;
    }
    static SourceArtifactInventory empty(){return new SourceArtifactInventory(Map.of(),Path.of("."));}
    static SourceArtifactInventory read(Path path)throws IOException {
        var mapper=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).build();
        var document=mapper.readValue(path.toFile(),Document.class);
        if(!document.version().equals("1.0.0"))throw new IllegalArgumentException("Unsupported source artifact inventory version");
        var entries=new HashMap<String,Entry>();
        for(var entry:document.artifacts()) {
            String name=SourceDependencyFact.canonical(entry.name());
            var artifact=Path.of(entry.artifact());
            if(name.isBlank()||entry.artifact().isBlank()||artifact.isAbsolute()||artifact.normalize().startsWith(".."))
                throw new IllegalArgumentException("Inventory needs nominal name and relative artifact");
            if((name.equals("SQLCA")||name.equals("SQLDA"))&&entry.kind()==Kind.DCLGEN)
                throw new IllegalArgumentException("SQLCA/SQLDA cannot be declared DCLGEN");
            if(entries.put(name,entry)!=null)throw new IllegalArgumentException("Duplicate source inventory name");
        }
        return new SourceArtifactInventory(entries,path.toAbsolutePath().getParent());
    }
    SourceDependencyFact classify(SourceDependencyFact fact) {
        var entry=entries.get(fact.name());
        if(entry==null||fact.authority().equals("BUILTIN_SQL_INCLUDE"))return fact;
        boolean resolved=Files.isRegularFile(directory.resolve(entry.artifact()));
        return new SourceDependencyFact(entry.kind()==Kind.DCLGEN?SourceDependencyFact.Kind.DCLGEN:SourceDependencyFact.Kind.SQL_INCLUDE,
            fact.name(),fact.qualification(),resolved?SourceDependencyFact.Resolution.RESOLVED:SourceDependencyFact.Resolution.UNRESOLVED,
            resolved?entry.artifact():"",entry.kind()==Kind.DCLGEN?"CONFIGURED_DCLGEN":"CONFIGURED_SQL_INCLUDE",fact.provenance(),fact.rootSite());
    }
}
