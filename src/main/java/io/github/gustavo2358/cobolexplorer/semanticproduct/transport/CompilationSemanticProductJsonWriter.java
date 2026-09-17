package io.github.gustavo2358.cobolexplorer.semanticproduct.transport;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.gustavo2358.cobolexplorer.semanticproduct.CompilationSemanticProduct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
/** Versioned envelope, preserving each existing unit document verbatim in shape. */
public final class CompilationSemanticProductJsonWriter {
    private static final ObjectMapper JSON=JsonMapper.builder().enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY).build();
    private CompilationSemanticProductJsonWriter(){}
    public static byte[] serialize(CompilationSemanticProduct product)throws IOException{return JSON.writeValueAsBytes(document(product));}
    public static void write(CompilationSemanticProduct product,Path path)throws IOException{try(var out=Files.newOutputStream(path)){JSON.writeValue(out,document(product));}}
    private static Document document(CompilationSemanticProduct p){return new Document("cobol-semantic-compilation","1.0.0",p.inventoryStatus().name(),p.unitInventory().stream().map(SemanticProductJsonWriter::unitValue).toList(),p.units().stream().map(u->new UnitDocument(SemanticProductJsonWriter.documentValue(u.product()),u.parent().map(SemanticProductJsonWriter::unitValue).orElse(null),u.ownedData().stream().map(SemanticProductJsonWriter::dataValue).toList(),u.globalData().stream().map(SemanticProductJsonWriter::dataValue).toList(),u.dataCaptures().stream().map(c->new Capture(SemanticProductJsonWriter.dataValue(c.localData()),SemanticProductJsonWriter.unitValue(c.sourceData().unit()),SemanticProductJsonWriter.dataValue(c.sourceData()))).toList(),u.fileCaptures().stream().map(f->new FileCapture(SemanticProductJsonWriter.unitValue(f.unit()),"file:"+f.localId())).toList())).toList());}
    private record Document(String schema,String contractVersion,String inventoryStatus,List<Object> unitInventory,List<UnitDocument> units){}
    private record UnitDocument(Object product,Object parent,List<String> ownedData,List<String> globalData,List<Capture> dataCaptures,List<FileCapture> fileCaptures){}
    private record Capture(String localDataId,Object sourceUnit,String sourceDataId){}
    private record FileCapture(Object owner,String id){}
}
