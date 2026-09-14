package io.github.gustavo2358.cobolexplorer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.gustavo2358.cobolexplorer.StorageLayoutSemantics.*;

/** Gradual, output-aware measurements. Cardinalities are oracles; elapsed time is not an SLA. */
class StorageScaleQualificationTest {
    @Test void fixedViewsOverlaysAndRenamesScaleWithoutExtentSizedLayout() {
        for(int n:List.of(32,128,512))for(String shape:List.of("wide","overlays","renames","deep")) {
            var data=new StringBuilder();
            if(shape.equals("wide")) {data.append("01 AREA-A.\n");for(int i=0;i<n;i++)data.append("05 ITEM-").append(i).append(" PIC X(1000000000).\n");}
            else if(shape.equals("overlays")) {data.append("01 AREA-A PIC X(1000000000).\n");for(int i=1;i<n;i++)data.append("01 ALT-").append(i).append(" REDEFINES AREA-A PIC X(1000000000).\n");}
            else if(shape.equals("renames"))for(int i=0;i<n;i++)data.append("01 AREA-").append(i).append(".\n05 ITEM-").append(i).append(" PIC X(1000000000).\n66 ALIAS-").append(i).append(" RENAMES ITEM-").append(i).append(".\n");
            else {data.append("01 AREA-A.\n");for(int level=2;level<=48;level++)data.append(String.format("%02d",level)).append(" GROUP-").append(level).append(".\n");for(int i=0;i<n;i++)data.append("49 ITEM-").append(i).append(" PIC X(1000000000).\n");}
            var start=System.nanoTime();var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(data.toString(),"CONTINUE.\nGOBACK."),"scale.cbl");var parsed=System.nanoTime();
            var layout=StorageLayoutSemantics.analyze(a.build(),a.tables(),a.resolution(),a.report(),Profile.IBM_ENTERPRISE_6_4_FIXED_DISPLAY_1047);var prepared=System.nanoTime();
            var access=StorageAccessSemantics.analyze(a.build(),a.resolution(),layout);var complete=System.nanoTime();var facts=layout.layout(a.model().programUnits().get(0).id());
            assertEquals(shape.equals("renames")?n:1,facts.bases().size());assertTrue(facts.views().size()>=n);assertTrue(facts.bases().stream().allMatch(b->b.extent().value().orElseThrow().signum()>0));assertTrue(access.moves().isEmpty());
            System.out.printf(Locale.ROOT,"W8_SOURCE_SCALE shape=%s n=%d inputChars=%d bases=%d views=%d parseNs=%d layoutNs=%d accessNs=%d heapUsed=%d%n",shape,n,data.length(),facts.bases().size(),facts.views().size(),parsed-start,prepared-parsed,complete-prepared,java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed());
        }
    }
}
