package io.github.gustavo2358.cobolexplorer;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConditionNameSyntaxTest {
    @Test void preservesOrderedRangesAndExplicitFalse() {
        var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program(
            "01 FLAG-A PIC 9(9).\n88 ACTIVE-A VALUE 7 20 THRU 999999999 WHEN SET TO FALSE IS 0.",
            "SET ACTIVE-A TO TRUE.\nSET ACTIVE-A TO FALSE.\nGOBACK."),"condition-name.cbl");
        var nodes=new ArrayList<Ast.Node>();var todo=new ArrayDeque<Ast.Node>();todo.add(a.model().programUnits().get(0).program());
        while(!todo.isEmpty()){var n=todo.removeFirst();nodes.add(n);todo.addAll(Ast.children(n));}
        var value=nodes.stream().filter(Ast.ValueClause.class::isInstance).map(Ast.ValueClause.class::cast).findFirst().orElseThrow();
        assertEquals(2,value.ranges().size());
        assertEquals("7",value.ranges().get(0).first().value());
        assertEquals("999999999",value.ranges().get(1).last().orElseThrow().value());
        assertEquals("0",value.falseValue().orElseThrow().value());
        var sets=nodes.stream().filter(Ast.ModeledStatement.class::isInstance).map(Ast.ModeledStatement.class::cast)
            .flatMap(s->s.conditionSet().stream()).toList();
        assertEquals(List.of(true,false),sets.stream().flatMap(s->s.assignments().stream()).map(Ast.ConditionSetTarget::truth).toList());
    }
    @Test void falseClauseOptionalWordsDoNotBecomeTrueValues() {
        for(int mask=0;mask<16;mask++) {
            String clause=((mask&1)!=0?"WHEN ":"")+((mask&2)!=0?"SET ":"")+((mask&4)!=0?"TO ":"")+"FALSE "+((mask&8)!=0?"IS ":"")+"0";
            var a=AstBoundaryTestSupport.analyze(ScalarMoveCheckpoint4ATest.program("01 FLAG-A PIC 9.\n88 ACTIVE-A VALUE 7 "+clause+".","SET ACTIVE-A TO FALSE.\nGOBACK."),"false.cbl");
            var facts=ConditionNameSemantics.analyze(a.build(),a.tables(),a.resolution());var unit=a.model().programUnits().get(0).id();
            assertEquals(1,facts.sets(unit).size(),clause);
            var assignment=facts.sets(unit).values().iterator().next().get(0);
            assertEquals("0",assignment.value().value(),clause);assertEquals(1,assignment.use().declaration().ranges().size(),clause);
        }
    }
}
