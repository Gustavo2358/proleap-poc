package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageAccessTest.fixture;
import static io.github.gustavo2358.cobolexplorer.StorageAccessSemantics.*;

class StorageMetamorphicTest {
    static final String DATA="01 SOURCE-AREA.\n05 FIRST-PART PIC X(4).\n05 LAST-PART PIC X(4).\n66 SOURCE-ALIAS RENAMES FIRST-PART THRU LAST-PART.\n01 DEST-AREA.\n05 TARGET-PART PIC X(8).\n01 DEST-ALIAS REDEFINES DEST-AREA PIC X(8).";
    record Meaning(List<String> moves,List<String> calls) { }
    static Meaning meaning(String data,String code) {
        var f=fixture(data,code);var bases=new LinkedHashMap<StorageLayoutSemantics.Key,Integer>();
        java.util.function.Function<StorageLayoutSemantics.View,String> view=v->bases.computeIfAbsent(v.base(),k->bases.size())+":"+v.offset().value()+":"+v.extent().value()+":"+v.textual();
        var moves=f.effects().moves().stream().sorted(Comparator.comparingInt(m->m.statement().node())).flatMap(m->f.effects().sequence(m.statement()).stream()).map(m->m.kind()+":"+m.bytes()+":"+m.source().map(a->view.apply(a.view()))+">"+m.destination().map(a->view.apply(a.view()))+":"+m.reasons()).toList();
        var calls=f.effects().accesses().stream().filter(a->a.role()==Role.CALL_TARGET).sorted(Comparator.comparingInt(a->a.statement().node())).map(a->view.apply(a.view())).toList();
        return new Meaning(moves,calls);
    }
    @Test void renamedQualifiedAndAliasCopiesKeepExactPhysicalEffects() {
        var body="MOVE 'ABCDEFGH' TO SOURCE-AREA.\nMOVE SOURCE-AREA TO DEST-AREA.\nCALL TARGET-PART.";
        var expected=meaning(DATA,body);assertEquals(2,expected.moves().size());assertEquals(1,expected.calls().size());
        var renamedData=DATA.replace("SOURCE","INPUT").replace("DEST","OUTPUT").replace("TARGET-PART","PROGRAM-PART");
        var renamedBody=body.replace("SOURCE","INPUT").replace("DEST","OUTPUT").replace("TARGET-PART","PROGRAM-PART");
        assertEquals(expected,meaning(renamedData,renamedBody));
        assertEquals(expected,meaning(DATA,body.replace("CALL TARGET-PART","CALL TARGET-PART OF DEST-AREA")));
        assertEquals(expected,meaning(DATA,body.replace("MOVE SOURCE-AREA TO DEST-AREA","MOVE SOURCE-ALIAS TO DEST-ALIAS")));
        assertEquals(expected,meaning("01 UNRELATED-A PIC X(8).\n01 UNRELATED-B REDEFINES UNRELATED-A PIC X(8).\n"+DATA,body));
    }
    @Test void referenceSliceQualificationDoesNotChangeOneBasedByteRange() {
        var body="MOVE 'ABCDEFGH' TO SOURCE-AREA.\nCALL TARGET-PART(2:4).";
        assertEquals(meaning(DATA,body),meaning(DATA,body.replace("TARGET-PART(2:4)","TARGET-PART OF DEST-AREA(2:4)")));
    }
    @Test void qualifiedDynamicAndOutOfBoundsModifiersNeverBecomeWholeReads() {
        for(var modifier:List.of("POSITION-VAR:4","2:POSITION-VAR","8:2","0:4")) {
            var f=fixture(DATA+"\n01 POSITION-VAR PIC 9.","CALL TARGET-PART OF DEST-AREA("+modifier+").");
            assertTrue(f.effects().accesses().stream().noneMatch(a->a.role()==Role.CALL_TARGET),modifier);
        }
    }

}
