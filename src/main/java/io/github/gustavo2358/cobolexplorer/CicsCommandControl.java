package io.github.gustavo2358.cobolexplorer;

import java.util.*;

/** IBM command-condition interpretation; destinations remain grammar-owned by ControlTopology. */
final class CicsCommandControl {
    record Qualification(String ordinaryProof,List<String> unresolvedConditions) {
        Qualification {unresolvedConditions=List.copyOf(unresolvedConditions);}
    }
    static Optional<Qualification> qualify(CicsCommandSemantics.Fact fact) {
        if(!fact.supported()||fact.command()==CicsCommandSemantics.Kind.SEND_TERMINAL)return Optional.empty();
        // SP2.44 publishes operands only; terminal execution remains unqualified.
        boolean local=fact.options().stream().anyMatch(o->o.name().equals("RESP")||o.name().equals("NOHANDLE"));
        var remainder=new ArrayList<String>();
        if(!local)remainder.add("handler-or-default-condition");
        // SEND MAP OVERFLOW is explicitly exempt from generic RESP/NOHANDLE rules.
        if(fact.command()==CicsCommandSemantics.Kind.SEND_MAP)remainder.add("overflow");
        String command=switch(fact.command()) {case SYNCPOINT->"syncpoint";case RECEIVE_MAP->"receive-map";case SEND_MAP->"send-map";case SEND_TERMINAL->throw new IllegalStateException("terminal qualification unavailable");};
        return Optional.of(new Qualification("cics-command-"+command+"-ordinary-return",remainder));
    }
}
